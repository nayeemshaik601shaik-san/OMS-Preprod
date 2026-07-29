package com.crocs.oms.shipment.emea;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsErrorConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-10448 & EOMS-10915 
 * Utility class for EMEA shipment processing in OMS.
 *
 * Handles shipment creation, confirmation, split shipment processing,
 * short shipment logic, container updates, and integration with OMS APIs.
 * Works primarily on Sterling OMS XML (DOM) structures.
 */
public class CrocsEMEAShipmentUtil implements CrocsConstant{
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsEMEAShipmentUtil.class);

	/**
     * Categorizes shipment lines into groups based on their associated brand.
     * The brand is determined by looking up item details via the getItemList API 
     * and resolving the 'Department' attribute.
     * 
     * @param env YFSEnvironment object.
     * @param shipmentDoc The Input Document containing ShipmentLine elements.
     * @return A Map where keys are Brand names and values are Lists of shipment line Elements.
     */
    public Map<String, List<Element>> groupShipmentLinesByBrand(YFSEnvironment env, Document shipmentDoc) {
        logger.verbose("CrocsEMEAShipmentUtil: Start of method groupShipmentLinesByBrand: with input shipmentDoc: " + SCXmlUtil.getString(shipmentDoc));
       
        Document docGetItemListInput = formGetItemListInput(shipmentDoc);
        
        logger.verbose("Invoking getItemList to resolve brand departments.");
        Document docGetItemListOutput = invokeGetItemListAPI(env, docGetItemListInput);
        Element eleGetItemListOutput = docGetItemListOutput.getDocumentElement();

        // Initialize the grouping map
        Map<String, List<Element>> brandToLinesMap = new HashMap<>();
        
        // XPath template to locate the PrimaryInformation of a specific ItemID within the API output
        String itemLookupPath = "Item[@ItemID='%s']/PrimaryInformation";

        Element eleShipment = shipmentDoc.getDocumentElement();
        Element eleShipmentLines = SCXmlUtil.getChildElement(eleShipment, E_SHIPMENT_LINES);
        List<Element> eleListOfShipmentLine = SCXmlUtil.getChildrenList(eleShipmentLines);

        logger.verbose("Processing " + eleListOfShipmentLine.size() + " shipment lines for grouping brand");

        // Iterate through each shipment line to resolve its brand
        for (Element eleShipmentLine : eleListOfShipmentLine) {
            String strItemIdAtShipmentLine = eleShipmentLine.getAttribute(A_ITEM_ID);

            // Construct specific XPath for the current item
            String strXpathToGetPrimaryInfo = String.format(itemLookupPath, strItemIdAtShipmentLine);
            Element elePrimaryInfo = SCXmlUtil.getXpathElement(eleGetItemListOutput, strXpathToGetPrimaryInfo);

            if (!YFCCommon.isVoid(elePrimaryInfo)) {
                // Extract Department and map it to a Brand 
                String strDepartment = elePrimaryInfo.getAttribute(A_DEPARTMENT);
                String brand = null;

                if (!YFCCommon.isVoid(strDepartment)) {
                    String department = strDepartment.toLowerCase();

                    if (department.contains(STR_HEYDUDE_BRAND)) {
                        brand = STR_HEYDUDE_BRAND;
                    } else if (department.contains(STR_CROCS_BRAND)) {
                        brand = STR_CROCS_BRAND;
                    }                    
                }
                
                logger.verbose("Item ID: " + strItemIdAtShipmentLine + " resolved to Brand: " + brand);
                brandToLinesMap.computeIfAbsent(brand, brandKey -> new ArrayList<>()).add(eleShipmentLine);
            } 
//            else {
//                // Default Case: If item info is missing, default the line to the core 'Crocs' brand
//                logger.verbose("Item details not found for ItemID: " + strItemIdAtShipmentLine + ". Defaulting to " + STR_CROCS_BRAND);
//                brandToLinesMap.computeIfAbsent(STR_CROCS_BRAND, brandKey -> new ArrayList<>()).add(eleShipmentLine);
//            }
        }
        
        logger.verbose("CrocsEMEAShipmentUtil: End of method groupShipmentLinesByBrand. Total brands identified: " + brandToLinesMap.keySet().size());
        return brandToLinesMap;
    }
	
	/**
	 * Determines the brand based on the provided department name.
	 * 
	 * If department is null or empty, defaults to CROCS brand
	 * If department contains HEYDUDE keyword, returns HEYDUDE brand
	 * Otherwise, defaults to CROCS brand
	 * 
	 * @param department the department name used to identify the brand
	 * @return the resolved brand name (either HEYDUDE or CROCS)
	 */
	private static String resolveBrand(String department) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method resolveBrand with department: " + department);

	    // Return default brand when input is missing or invalid
//	    if (YFCCommon.isVoid(department)) {
//	    	logger.verbose("CrocsEMEAShipmentUtil: End of method resolveBrand: Department is void. Defaulting brand to: " + STR_CROCS_BRAND);
//	        return STR_CROCS_BRAND;
//	    }

	    // for case-insensitive comparison
//	    String departmentLowerCase  = department.toLowerCase();

	    // Check if department belongs to HEYDUDE brand
//	    if (departmentLowerCase.contains(STR_HEYDUDE_BRAND)) {
//	    	logger.verbose("CrocsEMEAShipmentUtil: End of method resolveBrand: Department '" + department + "' contains keyword '" + STR_HEYDUDE_BRAND + "'. Resolving to HEYDUDE.");
//	        return STR_HEYDUDE_BRAND;
//	    }
	    
	    String resolvedBrand = department.toLowerCase().contains(STR_HEYDUDE_BRAND) ? STR_HEYDUDE_BRAND : STR_CROCS_BRAND;

	    logger.verbose("CrocsEMEAShipmentUtil: End of method resolveBrand with final brand as '" + resolvedBrand);
	    return resolvedBrand;
	}
	
	/**
     * Prepares a mapping of Prime Line Numbers to their corresponding Order Line elements.
     * 
     * @param orderReleaseRoot The Document containing Order Release and Order Line details.
     * @return A Map where the key is the PrimeLineNo and the value is the OrderLine Element.
     */
    protected Map<String, Element> buildPrimeLineToOrderLineMap(Document orderReleaseRoot) {
        logger.verbose("CrocsEMEAShipmentUtil: Start of method buildPrimeLineToOrderLineMap with input: " + SCXmlUtil.getString(orderReleaseRoot));

        // Initialize map to store PrimeLineNo as key 
        Map<String, Element> primeLineMap = new HashMap<>();

        Element eleOrderRelease = orderReleaseRoot.getDocumentElement();
        Element orderRelease = SCXmlUtil.getChildElement(eleOrderRelease, E_ORDER_RELEASE);
        List<Element> listOfOrderLine = SCXmlUtil.getChildren(orderRelease, E_ORDER_LINE);

        // Iterate through each order line
        if (!YFCCommon.isVoid(listOfOrderLine)) {
        	
            for (Element orderLine : listOfOrderLine) {
                String primeLineNo = orderLine.getAttribute(A_PRIME_LINE_NO);
                
                // Only map lines with a valid Prime Line Number to avoid null keys
                if (!YFCCommon.isVoid(primeLineNo)) {
                    primeLineMap.put(primeLineNo, orderLine);
                }
            }
        }

        logger.verbose("CrocsEMEAShipmentUtil: End of method buildPrimeLineToOrderLineMap with primeLineMap as : ");
		primeLineMap.forEach((prime, orderLine) -> logger.verbose("PrimeLineNo: " + prime + " => OrderLine: " + SCXmlUtil.getString(orderLine)));
		
        return primeLineMap;
    }

	protected void validateRequiredAttributes(Document confirmShipmentInDoc) throws YFSException {
	    if (YFCCommon.isVoid(confirmShipmentInDoc)) {
	        throw new YFSException("Input document is null", "YDM00001", "The input XML document is null or empty. Cannot process shipment confirmation");
	    }

	    Element confirmShipmentEle = confirmShipmentInDoc.getDocumentElement();

	    // Mandatory attributes
	    String[] mandatoryAttrs = { A_ORDER_NO, A_RELEASE_NO, A_WMS_CODE };
	    StringBuilder missingAttrs = new StringBuilder();

	    for (String attr : mandatoryAttrs) {
	        if (YFCCommon.isVoid(confirmShipmentEle.getAttribute(attr))) {
	            if (!missingAttrs.isEmpty()) {
	                missingAttrs.append(", ");
	            }
	            missingAttrs.append(attr);
	        }
	    }

	    if (!missingAttrs.isEmpty()) {
	        throw new YFSException("Missing required attribute", "PLT3004", "The following required attribute(s) is missing or empty in Shipment confirmation XML: " + missingAttrs );
	    }

	    // At least one of the two flags should be present
	    String cancelNonShippedQuantity = confirmShipmentEle.getAttribute(A_CANCEL_NON_SHIPPED_QUANTITY);
	    String backOrderNonShippedQuantity = confirmShipmentEle.getAttribute(A_BACK_ORDER_NON_SHIPPED_QUANTITY);

	    if (YFCCommon.isVoid(cancelNonShippedQuantity) && YFCCommon.isVoid(backOrderNonShippedQuantity)) {
	        throw new YFSException("Missing required flags", "YFS10460", "At least one of the attributes 'CancelNonShippedQuantity' or 'BackOrderNonShippedQuantity' must be present.");
	    }
	}

	/**
     * Prepares the input XML for the getItemList API call.
     * 
     * This method extracts ItemIDs from the ShipmentLines and constructs a ComplexQuery 
     * to fetch item details in a single API call.
     * 
     * @param shipmentDoc The input Shipment document containing line items.
     * @return A Document formatted for getItemList with a ComplexQuery of ItemIDs.
     */
    private Document formGetItemListInput(Document shipmentDoc) {
        logger.verbose("CrocsEMEAShipmentUtil: Start of method formGetItemListInput: with input shipmentDoc: " + SCXmlUtil.getString(shipmentDoc));

        if (YFCCommon.isVoid(shipmentDoc)) {
            logger.verbose("Input shipmentDoc is null or void. Returning original document.");
            return shipmentDoc;
        }

        Element shipmentEle = shipmentDoc.getDocumentElement();

        // Create the root getItemList input element for the Item entity
        Document getItemListInputDoc = SCXmlUtil.createDocument(E_ITEM);
        Element getItemListInputEle = getItemListInputDoc.getDocumentElement();
        getItemListInputEle.setAttribute(A_ORGANIZATION_CODE, STR_CROCS_NA);

        // Build the ComplexQuery 
        Element complexQueryEle = SCXmlUtil.createChild(getItemListInputEle, E_COMPLEX_QUERY);
        complexQueryEle.setAttribute(A_OPERATOR, S_AND);

        Element andEle = SCXmlUtil.createChild(complexQueryEle, E_AND);
        Element orEle = SCXmlUtil.createChild(andEle, E_OR);

        // Retrieve ShipmentLines to extract individual Item IDs
        Element shipmentLinesEle = SCXmlUtil.getChildElement(shipmentEle, E_SHIPMENT_LINES);

        if (!YFCCommon.isVoid(shipmentLinesEle)) {
            List<Element> shipmentLinesList = SCXmlUtil.getChildrenList(shipmentLinesEle);
            
            logger.verbose("Building ComplexQuery for " + (shipmentLinesList != null ? shipmentLinesList.size() : 0) + " shipment lines.");

            if (shipmentLinesList != null) {
                for (Element shipmentLine : shipmentLinesList) {
                    String itemIdAtShipmentLine = shipmentLine.getAttribute(A_ITEM_ID);

                    if (!YFCCommon.isVoid(itemIdAtShipmentLine)) {
                        Element expEle = SCXmlUtil.createChild(orEle, A_EXP);
                        expEle.setAttribute(A_NAME, A_ITEM_ID);
                        expEle.setAttribute(A_VALUE, itemIdAtShipmentLine);
                        expEle.setAttribute(A_QRY_TYPE, EQ);
                    }
                }
            }
        }

        logger.verbose("CrocsEMEAShipmentUtil: End of method formGetItemListInput: with output getItemListInputDoc: " + SCXmlUtil.getString(getItemListInputDoc));
        return getItemListInputDoc;
    }
    
    /**
     * Invokes the 'CrocsEMEAGetItemList' service to retrieve item details.
     * 
     * @param env YFSEnvironment object.
     * @param getItemListInput input for the getItemList service.
     * @return The output Document from the service containing item attributes.
     * @throws YFSException if the service invocation fails.
     */
    private Document invokeGetItemListAPI(YFSEnvironment env, Document getItemListInput) {
        logger.verbose("CrocsEMEAShipmentUtil: Start of method invokeGetItemListAPI: with input getItemListInput: " + SCXmlUtil.getString(getItemListInput));
        
        try {
            // Invoke the configured service via the Common Utility
            Document getItemListOutput = CommonUtil.invokeService(env, SERVICE_CROCS_EMEA_GET_ITEM_LIST_SHIPMENT, getItemListInput);
            
            logger.verbose("CrocsEMEAShipmentUtil: End of method invokeGetItemListAPI: successfully retrieved getItemListOutput: " + SCXmlUtil.getString(getItemListOutput));
            return getItemListOutput;
            
        } catch (Exception e) {
            // Log the stack trace/message for troubleshooting before throwing exception
            logger.verbose("CrocsEMEAShipmentUtil: Error encountered while invoking getItemList Service: " + e.getMessage());
            
            // Re-throw as YFSException to ensure the Sterling transaction is handled correctly
            throw new YFSException("Error invoking getItemList Service: " + e.getMessage());
        }
    }
	
    /**
     * Retrieves Order Release details by invoking the getOrderReleaseList service.
     * 
     * This method prepares the input XML using the OrderNo and ReleaseNo, for 
     * 
     * @param env       The YFSEnvironment object
     * @param orderNo   The Order Number 
     * @param releaseNo The specific Release Number
     * @return A Document containing the Order Release list output.
     * @throws YFSException if the service call fails.
     */
    protected Document invokeGetOrderReleaseList(YFSEnvironment env, String orderNo, String releaseNo) {
        logger.verbose("CrocsEMEAShipmentUtil: Start of method invokeGetOrderReleaseList: with OrderNo: " + orderNo + " & ReleaseNo: " + releaseNo);

        // Initialize the OrderRelease document and set the ReleaseNo attribute
        Document docGetOrderReleaseListInput = SCXmlUtil.createDocument(E_ORDER_RELEASE);
        Element eleGetOrderReleaseListInput = docGetOrderReleaseListInput.getDocumentElement();
        eleGetOrderReleaseListInput.setAttribute(A_RELEASE_NO, releaseNo);

        // Add the Order element to the input to filter by OrderNo and DocumentType
        Element eleOrder = SCXmlUtil.createChild(eleGetOrderReleaseListInput, E_ORDER);
        eleOrder.setAttribute(A_ORDER_NO, orderNo);
        eleOrder.setAttribute(A_DOCUMENT_TYPE, CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER);
        
        try {
            // Log the complete input XML before service invocation for troubleshooting
            logger.verbose("CrocsEMEAShipmentUtil: invokeGetOrderReleaseList: Calling getOrderReleaseList api with input: " + SCXmlUtil.getString(docGetOrderReleaseListInput));
            
            // Invoke the specific service defined for US Order Release lookups
            Document docOrderReleaseListOutput = CommonUtil.invokeService(env, SERVICE_GET_ORDER_RELEASE_LIST_US, docGetOrderReleaseListInput);
            
            logger.verbose("CrocsEMEAShipmentUtil: End of method invokeGetOrderReleaseList: successfully retrieved docOrderReleaseListOutput: " + SCXmlUtil.getString(docOrderReleaseListOutput));
            return docOrderReleaseListOutput;
            
        } catch (Exception e) {
            // Capture exception details in the verbose logs
            logger.verbose("CrocsEMEAShipmentUtil: Error invoking getOrderReleaseList Service: " + e.getMessage());
            
            // Wrap and throw as YFSException for consistent Sterling error handling
            throw new YFSException("Error invoking getOrderReleaseList Service: " + e.getMessage());
        }
    }
	
    /**
     * Retrieves a list of shipments associated with a specific order and release.
     * 
     * This method constructs the input XML for the getShipmentListForOrder service
     * 
     * @param env            The YFSEnvironment object
     * @param orderNo        The Order Number to filter the shipments.
     * @param releaseNo      The Release Number associated with the order.
     * @param enterpriseCode The Enterprise Code for the transaction context.
     * @return A Document containing the list of shipments for the specified order.
     * @throws YFSException if the service invocation fails.
     */
    protected Document invokeGetShipmentListForOrder(YFSEnvironment env, String orderNo, String releaseNo, String enterpriseCode) {
        logger.verbose("CrocsEMEAShipmentUtil: Start of method invokeGetShipmentListForOrder: with OrderNo: " + orderNo + " & ReleaseNo: " + releaseNo + " & EnterpriseCode: " + enterpriseCode);

        // Prepare input for the getShipmentListForOrder input
        Document docGetShipmentListForOrderInput = SCXmlUtil.createDocument(E_ORDER);
        Element eleGetShipmentListInput = docGetShipmentListForOrderInput.getDocumentElement();
        
        eleGetShipmentListInput.setAttribute(A_ORDER_NO, orderNo);
        eleGetShipmentListInput.setAttribute(A_ENTERPRISE_CODE, enterpriseCode);
        eleGetShipmentListInput.setAttribute(A_RELEASE_NO, releaseNo);
        eleGetShipmentListInput.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);

        try {
            // Log the prepared input XML before calling the service
            logger.verbose("CrocsEMEAShipmentUtil: invokeGetShipmentListForOrder: Calling getShipmentListForOrder api with input: " + SCXmlUtil.getString(docGetShipmentListForOrderInput));
            
            // Invoke the specific service to fetch shipment details
            Document docGetShipmentListForOrder = CommonUtil.invokeService(env, SERVICE_GET_SHIPMENT_LIST_FOR_ORDER, docGetShipmentListForOrderInput);
            
            logger.verbose("CrocsEMEAShipmentUtil: End of method invokeGetShipmentListForOrder: successfully retrieved docGetShipmentListForOrderOutput: " + SCXmlUtil.getString(docGetShipmentListForOrder));
            return docGetShipmentListForOrder;
            
        } catch (Exception e) {
            // Log the error with method context before throwing the YFSException
            logger.error("CrocsEMEAShipmentUtil: Error invoking getShipmentListForOrder Service: " + e.getMessage());
            
            // Re-throw as YFSException for standard Sterling error propagation
            throw new YFSException("Error invoking getShipmentListForOrder Service: " + e.getMessage());
        }       
    }
	
	/**
     * Updates the input document with required shipment attributes.
     * 
     * @param env                     The YFSEnvironment object
     * @param inDoc                   The Input Document to be updated.
     * @param docShipmentListForOrder The source Document containing candidate shipments.
     * @throws YFSException if no shipment in the required statuses is found.
     */
	protected void updateShipmentAttributes(Document inDoc, Document docShipmentListForOrder) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateShipmentAttributes.");
		logger.verbose("Input inDoc: " + SCXmlUtil.getString(inDoc));
		logger.verbose("Input docShipmentListForOrder: " + SCXmlUtil.getString(docShipmentListForOrder));

		Element eleShipmentList = docShipmentListForOrder.getDocumentElement();
		
		// take shipment which is in packed status 
		Element eleShipment = SCXmlUtil.getXpathElement(eleShipmentList, XPATH_SHIPMENT_STATUS_PACKED);	
		
		// otherwise take shipment which is created
		if(YFCCommon.isVoid(eleShipment)) 
			eleShipment = SCXmlUtil.getXpathElement(eleShipmentList, XPATH_SHIPMENT_STATUS_CREATED);	
		

		if(YFCCommon.isVoid(eleShipment)) {
			logger.verbose("CrocsEMEAShipmentUtil: No shipment found in created or packed status Input docShipmentListForOrder: " + SCXmlUtil.getString(docShipmentListForOrder));
			throw new YFSException("No shipment found in created or packed status " + SCXmlUtil.getString(docShipmentListForOrder));
		}
		
		Element eleInput = inDoc.getDocumentElement();
		eleInput.setAttribute(A_SHIPMENT_NO, eleShipment.getAttribute(A_SHIPMENT_NO));
		eleInput.setAttribute(A_OVERRIDE_MODIFICATION_RULES, FLAG_Y);
		eleInput.setAttribute(A_ENTERPRISE_CODE, eleShipment.getAttribute(A_ENTERPRISE_CODE));
		eleInput.setAttribute(A_DOCUMENT_TYPE, eleShipment.getAttribute(A_DOCUMENT_TYPE));
		eleInput.setAttribute(A_SHIP_NODE, eleShipment.getAttribute(A_SHIP_NODE));
		eleInput.setAttribute(A_SELLER_ORGANIZATION_CODE, eleShipment.getAttribute(A_SELLER_ORGANIZATION_CODE));

		logger.verbose("CrocsEMEAShipmentUtil: End of method updateShipmentAttributes. Updated output: " + SCXmlUtil.getString(inDoc));	
	}
	
	/**
     * Updates the shipment and container details in the input document 
     * based on the provided brand-specific shipment lines.
     * 
     * The method performs the following actions:
     * <ul>
     *   <li>Replaces existing ShipmentLines in the input document with brand lines.</li>
     *   <li>Filters ContainerDetail entries so that only shipment lines belonging 
     *       to the selected brand remain associated with containers.</li>
     *   <li>Removes containers that no longer contain any shipment lines after filtering.</li>
     * </ul>
     * 
     * @param inDoc The input Shipment Document to be updated.
     * @param brandLines List of ShipmentLine elements associated with a specific brand.
     */
	protected void updateContainerDetailsAsPerBrand(Document inDoc, List<Element> brandLines) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateContainerDetailsAsPerBrand with input document: " + SCXmlUtil.getString(inDoc));
		
		// Set used to store ShipmentLineNo values for quick lookup
		Set<String> setOfItemLineNo = new HashSet<>();

		// Remove existing ShipmentLines so that only brand-specific shipment lines are updated
		Element inDocEle = inDoc.getDocumentElement();
		inDocEle.removeChild(SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES));
		Element eleShipmentLines = SCXmlUtil.createChild(inDocEle, E_SHIPMENT_LINES);

		String strShipmentLineNo = "";
		
		// Import each brand-specific shipment line into the updated ShipmentLines element
		for (Element eachShipmentLine : brandLines) {
			logger.verbose("Processing brandLine:: " + SCXmlUtil.getString(eachShipmentLine));
			SCXmlUtil.importElement(eleShipmentLines, eachShipmentLine);
			
			// ShipmentLineNo for container filtering
			strShipmentLineNo = eachShipmentLine.getAttribute(A_SHIPMENT_LINE_NO);
			
			if (!YFCCommon.isVoid(strShipmentLineNo))
				setOfItemLineNo.add(strShipmentLineNo);
		}
		
		// Update container details to retain only shipment lines that belong to the current brand
		Element containerEle = SCXmlUtil.getChildElement(inDocEle, E_CONTAINERS);
		List<Element> listOfContainer = SCXmlUtil.getChildrenList(containerEle);

		for (Element eachContainer : listOfContainer) {
			logger.verbose("Processing eachContainer:: " + SCXmlUtil.getString(eachContainer));
			
			Element containerDetail = SCXmlUtil.getXpathElement(eachContainer, "ContainerDetails/ContainerDetail");
			List<Element> listOfShipmentLine = SCXmlUtil.getChildrenList(containerDetail);

			for (Element eachShipment : listOfShipmentLine) {

				String shipmentLineNo = eachShipment.getAttribute(A_SHIPMENT_LINE_NO);
				if (!setOfItemLineNo.contains(shipmentLineNo)) {
					containerDetail.removeChild(eachShipment);
				}
			}

			// Remove container completely if no shipment lines remain
			int noOfShipmentLineAtContainer = SCXmlUtil.getChildrenList(containerDetail).size();
			if (noOfShipmentLineAtContainer == 0)
				containerEle.removeChild(eachContainer);
		}
		logger.verbose("CrocsEMEAShipmentUtil: End of method updateContainerDetailsAsPerBrand with updated document: " + SCXmlUtil.getString(inDoc));
	}
	
	/**
     * Invokes the splitShipment API to split shipment lines shipment input document.
     * 
     * @param env YFSEnvironment object.
     * @param inDoc The input Shipment Document used for shipment split processing.
     * @return Document containing the splitShipment API response.
     * @throws YFSException if any exception occurs during shipment split processing.
     */
	protected Document splitShipment(YFSEnvironment env, Document inDoc) throws YFSException {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method splitShipment with input: " + SCXmlUtil.getString(inDoc));

		// Prepare input document for splitShipment API invocation
		Document splitShipmentInDoc = prepareInputForSplitShipment(inDoc);

		logger.verbose("Prepared splitShipment API input: " + SCXmlUtil.getString(splitShipmentInDoc));

		// Invoke splitShipment API
		logger.verbose("Invoking splitShipment API.");
		Document splitShipmentOutDoc = invokeSplitShipmentAPI(env, splitShipmentInDoc);

		logger.verbose("CrocsEMEAShipmentUtil: End of method splitShipment with output: " + SCXmlUtil.getString(splitShipmentOutDoc));
		return splitShipmentOutDoc;
	}
	
	/**
     * Prepares the input document required for invoking the splitShipment API.
     *  
     * @param inDoc The input Shipment Document containing shipment and shipment line details.
     * @return Document formatted as input for the splitShipment API.
     */
	private Document prepareInputForSplitShipment(Document inDoc) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method prepareInputForSplitShipment with input: " + SCXmlUtil.getString(inDoc));

		Element inDocEle = inDoc.getDocumentElement();

		// Fetch shipment line elements from input document
		Element inDocShipmentLinesEle = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
		List<Element> inDocShipmentLineListEle = SCXmlUtil.getChildrenList(inDocShipmentLinesEle);

		// Create root SplitShipment
		Document splitShipmentInDoc = SCXmlUtil.createDocument(E_SPLIT_SHIPMENT);
		Element splitShipmentInDocEle = splitShipmentInDoc.getDocumentElement();

		// Create Source Shipment 
		Element sourceEle = SCXmlUtil.createChild(splitShipmentInDocEle, E_SOURCE);
		Element shipmentEle = SCXmlUtil.createChild(sourceEle, E_SHIPMENT);

		// Fetch shipment level attributes
		String sellerOrganizationCode = inDocEle.getAttribute(A_SELLER_ORGANIZATION_CODE);
		String shipNode = inDocEle.getAttribute(A_SHIP_NODE);

		// Populate source shipment details
		shipmentEle.setAttribute(A_SELLER_ORGANIZATION_CODE, sellerOrganizationCode);
		shipmentEle.setAttribute(A_SHIP_NODE, shipNode);
		shipmentEle.setAttribute(A_SHIPMENT_NO, inDocEle.getAttribute(A_SHIPMENT_NO));

		logger.verbose("Source Shipment prepared with ShipmentNo: " + inDocEle.getAttribute(A_SHIPMENT_NO));

		Element shipmentLinesEle = SCXmlUtil.createChild(shipmentEle, E_SHIPMENT_LINES);

		// Populate shipment line details 
		for (Element eachShipmentLine : inDocShipmentLineListEle) {

			Element shipmentLineEle = SCXmlUtil.createChild(shipmentLinesEle, E_SHIPMENT_LINE);

			String shipmentLineNo = eachShipmentLine.getAttribute(A_SHIPMENT_LINE_NO);
			String itemId = eachShipmentLine.getAttribute(A_ITEM_ID);

			shipmentLineEle.setAttribute(A_ITEM_ID, itemId);
			shipmentLineEle.setAttribute(A_ORDER_NO, eachShipmentLine.getAttribute(A_ORDER_NO));
			shipmentLineEle.setAttribute(A_QUANTITY, eachShipmentLine.getAttribute(A_QUANTITY));
			shipmentLineEle.setAttribute(A_RELEASE_NO, eachShipmentLine.getAttribute(A_RELEASE_NO));
			shipmentLineEle.setAttribute(A_SHIPMENT_LINE_NO, shipmentLineNo);
			shipmentLineEle.setAttribute(A_UNIT_OF_MEASURE, eachShipmentLine.getAttribute(A_UNIT_OF_MEASURE));
			shipmentLineEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);
		}

		// Create Target Shipment 
		Element targetEle = SCXmlUtil.createChild(splitShipmentInDocEle, E_TARGET);
		Element targetShipmentEle = SCXmlUtil.createChild(targetEle, E_SHIPMENT);

		// Populate target shipment details
		targetShipmentEle.setAttribute(A_SELLER_ORGANIZATION_CODE, sellerOrganizationCode);
		targetShipmentEle.setAttribute(A_SHIP_NODE, shipNode);
		targetShipmentEle.setAttribute(A_SHIPMENT_NO, "");

		logger.verbose("CrocsEMEAShipmentUtil: End of method prepareInputForSplitShipment with output: " + SCXmlUtil.getString(splitShipmentInDoc));
		return splitShipmentInDoc;
	}
	

	/**
     * Invokes the splitShipment API using the provided input document.
     * 
     * @param env YFSEnvironment object.
     * @param splitShipmenInput The prepared input document for the splitShipment API.
     * @return Document containing the splitShipment API response.
     * @throws YFSException if an error occurs while invoking the splitShipment API.
     */
	private Document invokeSplitShipmentAPI(YFSEnvironment env, Document splitShipmenInput) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method invokeSplitShipmentAPI with input splitShipmenInput: " + SCXmlUtil.getString(splitShipmenInput));

		try {
			// Invoke splitShipment API		
			Document splitShipmentOutDoc = CommonUtil.invokeAPI(env, "", API_SPLIT_SHIPMENT, splitShipmenInput);			
			logger.verbose("CrocsEMEAShipmentUtil: End of method invokeSplitShipmentAPI with output splitShipmentOutDoc: " + SCXmlUtil.getString(splitShipmentOutDoc));
			return splitShipmentOutDoc;

		} catch (Exception e) {
			logger.error("CrocsEMEAShipmentUtil: Error invoking splitShipment API. Exception message: " + e.getMessage(), e);
			throw new YFSException("Error invoking splitShipment api: " + e.getMessage());
		}
	}
	
	/**
     * Builds a mapping between PrimeLineNo and ShipmentLineNo  from the splitShipment API output document.
     * 
     * @param splitShipmentOutDoc The output document returned by the splitShipment API.
     * @return A Map containing PrimeLineNo to ShipmentLineNo mappings.
     */
	protected static Map<String, String> buildPrimeLineToShipmentLineMap(Document splitShipmentOutDoc) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method buildPrimeLineToShipmentLineMap with input: " + SCXmlUtil.getString(splitShipmentOutDoc));

		// Initialize map to store PrimeLineNo -> ShipmentLineNo mapping
		Map<String, String> primeLineMap = new HashMap<>();

		Element eleSplitShipment = splitShipmentOutDoc.getDocumentElement();

		// Fetch ShipmentLines from Target Shipment section
		Element eleShipmentLines = SCXmlUtil.getXpathElement(eleSplitShipment, "/SplitShipment/Target/Shipment/ShipmentLines");

		if (!YFCCommon.isVoid(eleShipmentLines)) {
			List<Element> listOfShipmentLine = SCXmlUtil.getChildrenList(eleShipmentLines);

			// Iterate through each shipment line and build PrimeLineNo mapping
			for (Element eachShipment : listOfShipmentLine) {

				String primeLineNo = eachShipment.getAttribute(A_PRIME_LINE_NO);
				String shipmentLineNo = eachShipment.getAttribute(A_SHIPMENT_LINE_NO);

				// Only add valid PrimeLineNo and ShipmentLineNo mappings
				if (!YFCCommon.isVoid(primeLineNo) && !YFCCommon.isVoid(shipmentLineNo)) {
					logger.verbose("Mapping PrimeLineNo: " + primeLineNo + " to ShipmentLineNo: " + shipmentLineNo);

					primeLineMap.put(primeLineNo, shipmentLineNo);
				}
			}
		}

		logger.verbose("Final PrimeLineNo to ShipmentLineNo mappings:");
		primeLineMap.forEach((prime, shipLine) -> logger.verbose("PrimeLineNo: " + prime + " => ShipmentLineNo: " + shipLine));

		logger.verbose("CrocsEMEAShipmentUtil: End of method buildPrimeLineToShipmentLineMap");
		return primeLineMap;
	}
		
	/**
     * Updates container and shipment line details for shipments processed under Flag 'N'.
     * 
     * The method performs the following actions:
     * <ul>
     *   <li>Updates SCAC and tracking details at the container level.</li>
     *   <li>Builds tracking information mapping for shipment lines.</li>
     *   <li>Recreates ContainerDetail entries using updated ShipmentLineNo mappings.</li>
     *   <li>Updates ShipmentLineNo values in ShipmentLines using PrimeLine mappings.</li>
     * </ul>
     * 
     * The PrimeLineNo to ShipmentLineNo mapping is typically generated 
     * after shipment split processing.
     * 
     * @param env YFSEnvironment object.
     * @param inDoc Shipment document whose container details need to be updated.
     * @param mapPrimeLineNo Map containing PrimeLineNo to ShipmentLineNo mappings.
     * @param shipmentLineToTrackingNoMap Map storing ShipmentLineNo to OrderLine tracking details.
	 * @param docOrderReleaseList 
     * @return Updated shipment document with container and shipment line details.
     */
	protected Document updateContainerDetailsForFlagN(YFSEnvironment env, Document inDoc, Map<String, String> mapPrimeLineNo, Map<String, Element> shipmentLineToTrackingNoMap, Document docOrderReleaseList) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateContainerDetailsForFlagN with input: " + SCXmlUtil.getString(inDoc));

		Element inDocEle = inDoc.getDocumentElement();

		// Fetch all containers from shipment
		Element containersEle = SCXmlUtil.getChildElement(inDocEle, E_CONTAINERS);
		List<Element> containerListEle = SCXmlUtil.getChildrenList(containersEle);

		// Generate current shipment timestamp in SFCC format
		OffsetDateTime currentDate = OffsetDateTime.now(ZoneOffset.UTC);
		String strCurrentDate = CommonUtil.convertInputStringDateIntoSFCCFormat(currentDate.toString());

		// Iterate through each container
		for (Element eachContainer : containerListEle) {
			String containerNo = eachContainer.getAttribute(A_CONTAINER_NO);

			logger.verbose("Processing Container: " + SCXmlUtil.getString(eachContainer));

			// Update SCAC details at container level
			updateSCACDetailsAtContainer(env, eachContainer, docOrderReleaseList);

			// Fetch tracking details from container
			String strTrackingNoAtContainer = eachContainer.getAttribute(A_TRACKING_NO);
			String strScacAtContainer = eachContainer.getAttribute(A_SCAC);

			String strTrackingUrlAtContainer = SCXmlUtil.getChildElement(eachContainer, E_EXTN).getAttribute(A_EXTN_TRACKING_URL);

			logger.verbose("Container Tracking Details - TrackingNo: " + strTrackingNoAtContainer + ", SCAC: " + strScacAtContainer);

			// Fetch existing container details
			Element containerDetails = SCXmlUtil.getChildElement(eachContainer, E_CONTAINER_DETAILS);
			Element containerDetail = SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL);
			List<Element> shipmentList = SCXmlUtil.getChildrenList(containerDetail);

			logger.verbose("Shipment lines identified under container " + containerNo + ": " + shipmentList.size());

			// Remove existing container detail node for reconstruction
			containerDetails.removeChild(SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL));

			// Rebuild container detail entries
			for (Element shipmentLine : shipmentList) {
				String strShipmentLineNo = shipmentLine.getAttribute(A_SHIPMENT_LINE_NO);

				logger.verbose("Processing ShipmentLineNo: " + strShipmentLineNo + " for Container: " + containerNo);

				// Create OrderLine tracking information only once per ShipmentLineNo
				if (!YFCCommon.isVoid(strShipmentLineNo) && !shipmentLineToTrackingNoMap.containsKey(strShipmentLineNo)) {

					logger.verbose("Creating tracking mapping for ShipmentLineNo: " + strShipmentLineNo);

					Document docOrderLine = SCXmlUtil.createDocument(E_ORDER_LINE);

					Element eleOrderLine = docOrderLine.getDocumentElement();

					eleOrderLine.setAttribute(A_ACTION, VAL_MODIFY);
					eleOrderLine.setAttribute(A_PRIME_LINE_NO, strShipmentLineNo);
					eleOrderLine.setAttribute(A_SUB_LINE_NO, VAL_SUB_LINE_NO_1);

					// Populate tracking details under Extn
					Element extnEle = SCXmlUtil.createChild(eleOrderLine, E_EXTN);
					extnEle.setAttribute(A_EXTN_TRACKING_URL, strTrackingUrlAtContainer);
					extnEle.setAttribute(A_EXTN_SHIPMENT_DATE, strCurrentDate);
					extnEle.setAttribute(EXTN_TRACKING_NO, strTrackingNoAtContainer);
					extnEle.setAttribute(EXTN_SHIP_CARRIER, strScacAtContainer);

					shipmentLineToTrackingNoMap.put(strShipmentLineNo, eleOrderLine);
				}

				// Recreate ContainerDetail entry
				Element containerDetailEle = SCXmlUtil.createChild(containerDetails, E_CONTAINER_DETAIL);
				containerDetailEle.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));

				Element containerShipmentLine = SCXmlUtil.createChild(containerDetailEle, E_SHIPMENT_LINE);
				containerShipmentLine.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));

				// Update ShipmentLineNo using PrimeLine mapping
				String mappedValue = mapPrimeLineNo.get(strShipmentLineNo);

				if (mappedValue != null) {
					logger.verbose("Mapped old ShipmentLineNo: " + strShipmentLineNo + " to new ShipmentLineNo: " + mappedValue);
					containerShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, mappedValue);
				}
			}
		}

		// Update ShipmentLineNo values under ShipmentLines section
		Element shipmentLines = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
		List<Element> shipmentLineList = SCXmlUtil.getChildrenList(shipmentLines);

		for (Element eachShipmentLine : shipmentLineList) {
			String existingShipmentLineNo = eachShipmentLine.getAttribute(A_SHIPMENT_LINE_NO);
			String mappedValue = mapPrimeLineNo.get(existingShipmentLineNo);

			if (mappedValue != null) {
				logger.verbose("Updating ShipmentLineNo from " + existingShipmentLineNo + " to " + mappedValue);
				eachShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, mappedValue);
			}
		}

		logger.verbose("CrocsEMEAShipmentUtil: End of method updateContainerDetailsForFlagN with output: " + SCXmlUtil.getString(inDoc));
		return inDoc;
	}
	

	/**
     * Updates SCAC, carrier service, and tracking URL details 
     * at the container level.
     * 
     * The method performs the following actions:
     * <ul>
     *   <li>Stores the original SCAC value in ExternalReference1.</li>
     *   <li>Fetches SCAC configuration details using CommonCode </li>
     *   <li>Updates the container with OMS-configured SCAC value.</li>
     *   <li>Retrieves and imports SCAC and carrier service information.</li>
     *   <li>Updates tracking URL details at the container level.</li>
     * </ul>
     * 
     * @param env YFSEnvironment object.
     * @param eachContainer Container element whose SCAC details need to be updated.
	 * @param strCustomerLocale 
	 * @param orderReleaseList 
     * @throws YFCException if any exception occurs while updating SCAC details.
     */
	private void updateSCACDetailsAtContainer(YFSEnvironment env, Element eachContainer, Document orderReleaseList) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateSCACDetailsAtContainer with container detail as ." + SCXmlUtil.getString(eachContainer));

		try {

			// Fetch current SCAC value from container
			String scac = eachContainer.getAttribute(A_SCAC);
			// Preserve original SCAC value in ExternalReference1
			eachContainer.setAttribute(A_EXTERNAL_REFERENCE_1, scac);

			// Fetch SCAC configuration details from CommonCode
			Document docScacDetails = invokeCommonCodeListForSCACDetail(env, scac);
			Element eleScacDetails = docScacDetails.getDocumentElement();

			// Fetch OMS-configured SCAC value and SCAC-Service mapping
			String scacValue = SCXmlUtil.getXpathAttribute(eleScacDetails, XPATH_CODE_SHORT_DESCRIPTION);
			String strSCACandService = SCXmlUtil.getXpathAttribute(eleScacDetails, XPATH_CODE_LONG_DESCRIPTION);

			logger.verbose("Resolved OMS SCAC value: " + scacValue);
			logger.verbose("Resolved SCAC and Service mapping: " + strSCACandService);

			// Update SCAC at container level with OMS-configured value
			eachContainer.setAttribute(A_SCAC, scacValue);

			// Fetch SCAC and carrier service details
			Element docScacAndService = getScacAndServiceList(env, scacValue, strSCACandService);

			// Import SCAC and service details into container
			SCXmlUtil.importElement(eachContainer, docScacAndService);

			String strSCAC = eachContainer.getAttribute(A_SCAC);
			// Update tracking URL details at container level
			updateTrackingURLDetailsAtContainer(env, strSCAC, eachContainer, orderReleaseList);

			logger.verbose("CrocsEMEAShipmentUtil: End of method updateSCACDetailsAtContainer.");

		} catch (Exception e) {
			logger.error("CrocsEMEAShipmentUtil: Exception occurred while updating SCAC details. Exception message: " + e.getMessage(), e);
			throw new YFCException("getMessage:" + e.getMessage(), e.toString());
		}
	}
	
	
	/**
     * Updates the tracking URL details at the container level based on the provided SCAC value.
     * 
     * @param env YFSEnvironment object.
     * @param strSCAC SCAC value used to retrieve carrier tracking URL configuration.
     * @param eachContainer Container element whose tracking URL needs to be updated.
	 * @param strEnterpriseCode 
	 * @param orderReleaseList 
     * @throws YFCException if any exception occurs while updating tracking URL details.
     */
	private void updateTrackingURLDetailsAtContainer(YFSEnvironment env, String strSCAC, Element eachContainer, Document orderReleaseList) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateTrackingURLDetailsAtContainer with SCAC value as " + strSCAC + " and container details as ." + SCXmlUtil.getString(eachContainer));

		try {
			// Prepare getOrganizationList input document
			Document getOrganizationListInDoc = SCXmlUtil.createDocument(E_ORGANIZATION);
			getOrganizationListInDoc.getDocumentElement().setAttribute(A_ORGANIZATION_CODE, strSCAC);

			Document getOrganizationListOutDoc;

			// Invoke getOrganizationList API
			logger.verbose("Calling getOrganizationList API with input: " + SCXmlUtil.getString(getOrganizationListInDoc));
			getOrganizationListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORGANIZATION_LIST, API_GET_ORGANIZATION_LIST, getOrganizationListInDoc);
			logger.verbose("Output returned from getOrganizationList API: " + SCXmlUtil.getString(getOrganizationListOutDoc));

			// Fetch tracking URL template from organization details
			String strPrimaryUrl = SCXmlUtil.getXpathAttribute(getOrganizationListOutDoc.getDocumentElement(), XPAH_PRIMARY_URL);
			String trackingNo = eachContainer.getAttribute(A_TRACKING_NO);

			Element extn = SCXmlUtil.createChild(eachContainer, E_EXTN);

			strPrimaryUrl = updateTrackingURLAsPerLocale(strPrimaryUrl, strSCAC, orderReleaseList);
			strPrimaryUrl = strPrimaryUrl.replaceAll(A_TRACKING_NO, trackingNo);
			logger.verbose("Final generated tracking URL: " + strPrimaryUrl);

			// Update tracking URL under Extn
			extn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
		} catch (Exception e) {
			logger.error("CrocsEMEAShipmentUtil: Exception occurred while updating tracking URL details. Exception message: " + e.getMessage(), e);
			throw new YFCException(e.getMessage());
		}
	}

	/**
     * Invokes the getCommonCodeList API to retrieve SCAC configuration details.
     * 
     * @param env YFSEnvironment object.
     * @param scac SCAC value for which configuration details need to be retrieved.
     * @return Document containing CommonCode details for the provided SCAC.
     * @throws YFCException if any exception occurs during CommonCode API invocation.
     */
	public Document invokeCommonCodeListForSCACDetail(YFSEnvironment env, String scac) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method invokeCommonCodeListForSCACDetail with SCAC: " + scac);
		Document outDoc = null;

		try {
			// Prepare input document for getCommonCodeList API
			Document docCommonCode = SCXmlUtil.createDocument(E_COMMON_CODE);
			Element eleCommonCode = docCommonCode.getDocumentElement();

			eleCommonCode.setAttribute(A_CODE_TYPE, STR_CROCS_SCAC_NAMES);
			eleCommonCode.setAttribute(A_CODE_VALUE, scac);
			eleCommonCode.setAttribute(A_ORGANIZATION_CODE, STR_CROCS);

			logger.verbose("Prepared getCommonCodeList API input: " + SCXmlUtil.getString(docCommonCode));

			// Invoke getCommonCodeList API
			logger.verbose("Invoking API: " + API_GET_COMMON_CODE_LIST);
			outDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_COMMON_CODE_LIST, API_GET_COMMON_CODE_LIST, docCommonCode);
			logger.verbose("Received getCommonCodeList API response: " + SCXmlUtil.getString(outDoc));

		} catch (Exception e) {
			logger.error("CrocsEMEAShipmentUtil: Exception occurred while invoking getCommonCodeList API for SCAC: " + scac + ". Exception message: " + e.getMessage(), e);
			throw new YFCException("getMessage:" + e.getMessage(), e.toString());
		}
		logger.verbose("CrocsEMEAShipmentUtil: End of method invokeCommonCodeListForSCACDetail.");
		return outDoc;
	}
	
	/**
     * Retrieves SCAC and service configuration details by invoking the getScacAndServiceList API.
     * 
     * @param env YFSEnvironment object.
     * @param strSCAC SCAC code used as lookup key.
     * @param strSCACandService SCAC and service mapping value.
     * @return Element containing SCAC and Service details.
     * @throws Exception if any error occurs during API invocation or parsing response.
     */
	private Element getScacAndServiceList(YFSEnvironment env, String strSCAC, String strSCACandService) throws Exception {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method getScacAndServiceList with SCAC: " + strSCAC + " and strSCACandService: " + strSCACandService);

		try {
			// Prepare input for getScacAndServiceList API
			Document getScacAndServiceInput = SCXmlUtil.createDocument(A_SCAC_AND_SERVICE);
			Element eleGetScacAndServiceInput = getScacAndServiceInput.getDocumentElement();
			eleGetScacAndServiceInput.setAttribute(A_SCAC_KEY, strSCAC);
			eleGetScacAndServiceInput.setAttribute(A_SCAC_AND_SERVICE, strSCACandService);

			logger.verbose("Prepared getScacAndServiceList API input: " + SCXmlUtil.getString(getScacAndServiceInput));

			// Invoke API
			logger.verbose("Invoking API: " + API_GET_SCAC_AND_SERVICE_LIST);
			Document getScacAndServiceOutput = CommonUtil.invokeAPI(env, TEMPLATE_GET_SCAC_AND_SERVICE_LIST, API_GET_SCAC_AND_SERVICE_LIST, getScacAndServiceInput);
			logger.verbose("Received getScacAndServiceList API response: " + SCXmlUtil.getString(getScacAndServiceOutput));

			Element scacAndServiceElement = null;

			if (!YFCCommon.isVoid(getScacAndServiceOutput)) {
				scacAndServiceElement = SCXmlUtil.getXpathElement(getScacAndServiceOutput.getDocumentElement(), "/ScacAndServiceList/ScacAndService");
			}

			logger.verbose("Extracted ScacAndService element: " + SCXmlUtil.getString(scacAndServiceElement));
			return scacAndServiceElement;

		} catch (Exception e) {
			logger.error("CrocsEMEAShipmentUtil: Exception occurred in getScacAndServiceList for SCAC: " + strSCAC + ". Exception: " + e.getMessage(), e);
			throw new YFSException("Error in getScacAndServiceList method :" + e.getMessage());
		}
	}

	protected Document invokeConfirmShipmentAPI2(YFSEnvironment env, Document inputDoc) throws YFSException {
		logger.info("CrocsShipmentPackedToShippedStatusFromWMS: Start of method invokeConfirmShipmentAPI with input: "
				+ SCXmlUtil.getString(inputDoc));

		try {
			Document result = CommonUtil.invokeAPI(env, TEMPLATE_CONFIRM_SHIPMENT, API_CONFIRM_SHIPMENT, inputDoc);
			logger.verbose("End of method invokeConfirmShipmentAPI with output: " + SCXmlUtil.getString(result));
			return result;
		} catch (Exception e) {
			logger.error("Error invoking confirmShipment API: " + e.getMessage());
			throw new YFSException("Error invoking confirmShipment API: " + e.getMessage());
		}
	}
	
	/**
     * Invokes the confirmShipment API to confirm shipment processing.
     * 
     * @param env YFSEnvironment object.
     * @param inputDoc Input document required for confirmShipment API.
     * @return Document containing confirmShipment API response.
     * @throws YFSException if an error occurs while invoking the API.
     */
	protected Document invokeConfirmShipmentAPI(YFSEnvironment env, Document inputDoc) throws YFSException {
		logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: Start of method invokeConfirmShipmentAPI with input: " + SCXmlUtil.getString(inputDoc));

		try {
			// Invoke confirmShipment API
			logger.verbose("Invoking confirmShipment API.");
			Document result = CommonUtil.invokeAPI(env, TEMPLATE_CONFIRM_SHIPMENT, API_CONFIRM_SHIPMENT, inputDoc);
			logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: End of method invokeConfirmShipmentAPI with output: " + SCXmlUtil.getString(result));
			return result;
		} catch (Exception e) {
			logger.error("CrocsShipmentPackedToShippedStatusFromWMS: Error invoking confirmShipment API. Exception: " + e.getMessage(), e);
			throw new YFSException("Error invoking confirmShipment API: " + e.getMessage());
		}
	}
	
	/**
     * Creates a shipment by invoking the createShipment API using the provided OrderRelease list.
     *  
     * @param env YFSEnvironment object.
     * @param docOrderReleaseList Input document containing order release details.
     * @return Document containing created shipment details under ShipmentList root.
     * @throws YFSException if an error occurs during shipment creation.
     */
	protected Document createShipment(YFSEnvironment env, Document docOrderReleaseList) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method createShipment with input: " + SCXmlUtil.getString(docOrderReleaseList));

		try {
			// Prepare input for createShipment API
			Document docCreateShipmentInput = formCreateShipmentInput(docOrderReleaseList);
			logger.verbose("Prepared createShipment input: " + SCXmlUtil.getString(docCreateShipmentInput));

			// Invoke createShipment API
			Document docCreateShipmentOutput = CommonUtil.invokeAPI(env, TEMPLATE_CREATE_SHIPMENT, API_CREATE_SHIPMENT, docCreateShipmentInput);
			logger.verbose("Received createShipment API response: " + SCXmlUtil.getString(docCreateShipmentOutput));

			// Create wrapper document for response
			Document shipmentListDoc = SCXmlUtil.createDocument(E_SHIPMENT_LIST);
			Element shipmentListEle = shipmentListDoc.getDocumentElement();

			// Import API response into ShipmentList
			if (!YFCCommon.isVoid(docCreateShipmentOutput)) {
				Element eleCreateShipment = docCreateShipmentOutput.getDocumentElement();
				SCXmlUtil.importElement(shipmentListEle, eleCreateShipment);
			}

			logger.verbose("CrocsEMEAShipmentUtil: End of method createShipment with output: " + SCXmlUtil.getString(shipmentListDoc));
			return shipmentListDoc;

		} catch (Exception e) {
			logger.error("CrocsEMEAShipmentUtil: Exception in createShipment. Message: " + e.getMessage(), e);
			throw new YFSException("Error while creating shipment:" + e.getMessage());
		}
	}

	/**
     * Prepares the input document required for createShipment API.
     * 
     * @param docOrderReleaseList Input document containing OrderRelease and OrderLine details.
     * @return Document for createShipment API.
     */
	private Document formCreateShipmentInput(Document docOrderReleaseList) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method formCreateShipmentInput with input: " + SCXmlUtil.getString(docOrderReleaseList));

		Element eleOrderReleaseList = docOrderReleaseList.getDocumentElement();
		Element eleOrderRelease = SCXmlUtil.getChildElement(eleOrderReleaseList, E_ORDER_RELEASE);
		Element eleOrder = SCXmlUtil.getChildElement(eleOrderRelease, E_ORDER);

		// Create Shipment root document
		Document docCreateShipment = SCXmlUtil.createDocument(E_SHIPMENT);
		Element eleCreateShipment = docCreateShipment.getDocumentElement();

		// shipment ttributes
		eleCreateShipment.setAttribute(A_CARRIER_SERVICE_CODE, eleOrderRelease.getAttribute(A_CARRIER_SERVICE_CODE));
		eleCreateShipment.setAttribute(A_ENTERPRISE_CODE, eleOrderRelease.getAttribute(A_ENTERPRISE_CODE));
		eleCreateShipment.setAttribute(A_SHIP_NODE, eleOrderRelease.getAttribute(A_SHIP_NODE));
		eleCreateShipment.setAttribute(A_DOCUMENT_TYPE, eleOrderRelease.getAttribute(A_DOCUMENT_TYPE));

		// Create ShipmentLines container
		Element eleShipmentLines = SCXmlUtil.createChild(eleCreateShipment, E_SHIPMENT_LINES);
		List<Element> listOfOrderLines = SCXmlUtil.getChildren(eleOrderRelease, E_ORDER_LINE);

		for (Element eleOrderLine : listOfOrderLines) {
			Element eleOrderStatuses = SCXmlUtil.getChildElement(eleOrderLine, E_ORDER_STATUSES);

			List<Element> listOforderStatus = SCXmlUtil.getChildrenList(eleOrderStatuses);
			// Skip cancelled lines
			if (listOforderStatus.size() == 1 && STR_STATUS_CANCELLED.equals(listOforderStatus.get(0).getAttribute(A_STATUS))) {
				logger.verbose("Skipping cancelled OrderLine PrimeLineNo: " + eleOrderLine.getAttribute(A_PRIME_LINE_NO));
				continue;
			}

			Element eleShipmentLine = SCXmlUtil.createChild(eleShipmentLines, E_SHIPMENT_LINE);
			// Set quantity based on released status only
			for (Element eleStatus : listOforderStatus) {
				if (STATUS_RELEASE.equals(eleStatus.getAttribute(A_STATUS))) {
					eleShipmentLine.setAttribute(A_QUANTITY, eleStatus.getAttribute(A_STAT_QTY));
				}
			}
			
			Element eleItem = SCXmlUtil.getChildElement(eleOrderLine, E_ITEM);

			// Populate shipment line details
			eleShipmentLine.setAttribute(A_ITEM_ID, eleItem.getAttribute(A_ITEM_ID));
			eleShipmentLine.setAttribute(A_ORDER_NO, eleOrder.getAttribute(A_ORDER_NO));
			eleShipmentLine.setAttribute(A_PRIME_LINE_NO, eleOrderLine.getAttribute(A_PRIME_LINE_NO));
			eleShipmentLine.setAttribute(A_RELEASE_NO, eleOrderRelease.getAttribute(A_RELEASE_NO));
			eleShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, eleOrderLine.getAttribute(A_PRIME_LINE_NO));
			eleShipmentLine.setAttribute(A_SUB_LINE_NO, eleOrderLine.getAttribute(A_SUB_LINE_NO));
			eleShipmentLine.setAttribute(A_UNIT_OF_MEASURE, eleItem.getAttribute(A_UNIT_OF_MEASURE));
			
			logger.verbose("Added ShipmentLine " + SCXmlUtil.getString(eleOrderLine) + " for PrimeLineNo: " + eleOrderLine.getAttribute(A_PRIME_LINE_NO));
		}
		logger.verbose("CrocsEMEAShipmentUtil: End of method formCreateShipmentInput with docCreateShipment " + SCXmlUtil.getString(docCreateShipment));
		return docCreateShipment;
	}
	
	/**
     * Updates container details for shipments processed under Flag 'Y'.
     * 
     * The method performs the following actions:
     * <ul>
     *   <li>Updates SCAC and tracking details at container level.</li>
     *   <li>Rebuilds ContainerDetail structure for each container.</li>
     *   <li>Creates tracking information mapping for shipment lines.</li>
     *   <li>Populates tracking metadata such as URL, tracking number, and carrier.</li>
     * </ul>
     * 
     * The shipmentLineToTrackingNoMap is populated using computeIfAbsent
     * to ensure one tracking record per ShipmentLineNo.
     * 
     * @param env YFSEnvironment object.
     * @param inDoc Shipment document to be updated.
     * @param shipmentLineToTrackingNoMap Map storing ShipmentLineNo to tracking OrderLine details.
	 * @param docOrderReleaseList 
     * @return Updated shipment document with refreshed container details.
     */
	protected Document updateContainerDetailsForFlagY(YFSEnvironment env, Document inDoc, Map<String, Element> shipmentLineToTrackingNoMap, Document docOrderReleaseList) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateContainerDetailsForFlagY with input: " + SCXmlUtil.getString(inDoc));

		Element inDocEle = inDoc.getDocumentElement();

		Element containersEle = SCXmlUtil.getChildElement(inDocEle, E_CONTAINERS);
		OffsetDateTime currentDate = OffsetDateTime.now(ZoneOffset.UTC);
		String strCurrentDate = CommonUtil.convertInputStringDateIntoSFCCFormat(currentDate.toString());

		// Process containers only if available
		if (containersEle.hasChildNodes()) {
			List<Element> containerListEle = SCXmlUtil.getChildrenList(containersEle);

			for (Element eachContainer : containerListEle) {

				// Update SCAC and tracking configuration
				updateSCACDetailsAtContainer(env, eachContainer, docOrderReleaseList);
				
				String strTrackingNoAtContainer = eachContainer.getAttribute(A_TRACKING_NO);
				String strScacAtContainer = eachContainer.getAttribute(A_SCAC);
				String strTrackingUrlAtContainer = SCXmlUtil.getChildElement(eachContainer, E_EXTN).getAttribute(A_EXTN_TRACKING_URL);


				// Fetch container details
				Element containerDetails = SCXmlUtil.getChildElement(eachContainer, E_CONTAINER_DETAILS);
				Element containerDetail = SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL);
				List<Element> shipmentList = SCXmlUtil.getChildrenList(containerDetail);

				// Remove existing container detail before rebuilding
				containerDetails.removeChild(SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL));

				for (Element shipmentLine : shipmentList) {
					String strShipmentLineNo = shipmentLine.getAttribute(A_SHIPMENT_LINE_NO);

					// Create tracking mapping only if not already present
					if (!YFCCommon.isVoid(strShipmentLineNo)) {
						shipmentLineToTrackingNoMap.computeIfAbsent(strShipmentLineNo, shipmentLineNo -> {
							Document docOrderLine = SCXmlUtil.createDocument(E_ORDER_LINE);
							Element eleOrderLine = docOrderLine.getDocumentElement();

							eleOrderLine.setAttribute(A_ACTION, VAL_MODIFY);
							eleOrderLine.setAttribute(A_PRIME_LINE_NO, shipmentLineNo);
							eleOrderLine.setAttribute(A_SUB_LINE_NO, VAL_SUB_LINE_NO_1);
							
							Element extnEle = SCXmlUtil.createChild(eleOrderLine, E_EXTN);
							extnEle.setAttribute(A_EXTN_TRACKING_URL, strTrackingUrlAtContainer);
							extnEle.setAttribute(A_EXTN_SHIPMENT_DATE, strCurrentDate);
							extnEle.setAttribute(EXTN_TRACKING_NO, strTrackingNoAtContainer);
							extnEle.setAttribute(EXTN_SHIP_CARRIER, strScacAtContainer);

							return eleOrderLine;
						});
					}

					// update container detail structure
					Element containerDetailEle = SCXmlUtil.createChild(containerDetails, E_CONTAINER_DETAIL);
					containerDetailEle.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));

					Element containerShipmentLine = SCXmlUtil.createChild(containerDetailEle, E_SHIPMENT_LINE);
					containerShipmentLine.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));
					containerShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, strShipmentLineNo);
				}
			}
		}

		logger.verbose("CrocsEMEAShipmentUtil: End of method updateContainerDetailsForFlagY with output: " + SCXmlUtil.getString(inDoc));
		return inDoc;
	}
	
	/**
	 * Adds shipment lines required for cancellation processing.
	 * 
	 * The method compares shipment lines in the input document with order release
	 * lines and identifies lines that are not already part of the shipment. It then
	 * prepares cancellation shipment lines with zero quantity for remaining
	 * eligible order lines.
	 * 
	 * @param docConfirmShipment   Input shipment document to be updated.
	 * @param orderReleaseListOutDoc Order release document used for cancellation
	 *                               evaluation.
	 */
	protected void addShipmentLineForCancellation(Document docConfirmShipment, Document orderReleaseListOutDoc) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method addShipmentLineForCancellation with inputDocumentInitial: "+ SCXmlUtil.getString(docConfirmShipment));
		logger.verbose("CrocsEMEAShipmentUtil: OrderRelease input: " + SCXmlUtil.getString(orderReleaseListOutDoc));

		Element inDocEle = docConfirmShipment.getDocumentElement();
		Element orderReleaseListOutEle = orderReleaseListOutDoc.getDocumentElement();

		String orderNo = inDocEle.getAttribute(A_ORDER_NO);
		String releaseNo = inDocEle.getAttribute(A_RELEASE_NO);

		Element shipmentLines = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
		List<Element> listOfShipmentLine = SCXmlUtil.getChildrenList(shipmentLines);

		Element orderRelease = SCXmlUtil.getChildElement(orderReleaseListOutEle, E_ORDER_RELEASE);
		List<Element> listOfOrderLine = SCXmlUtil.getChildren(orderRelease, E_ORDER_LINE);

		// Build set of existing shipment line numbers
		Set<String> shipmentLineSet = new HashSet<>();

		for (Element eachShipmentLine : listOfShipmentLine) {
			String shipmentLineNo = eachShipmentLine.getAttribute(A_SHIPMENT_LINE_NO);
			shipmentLineSet.add(shipmentLineNo);
		}

		logger.verbose("Existing ShipmentLineNos: " + shipmentLineSet);

		// Remove order lines already present in shipment
		Iterator<Element> orderLineIterator = listOfOrderLine.iterator();

		while (orderLineIterator.hasNext()) {
			Element orderLine = orderLineIterator.next();

			String primeLineNo = orderLine.getAttribute(A_PRIME_LINE_NO);
			if (shipmentLineSet.contains(primeLineNo)) {
				orderLineIterator.remove();
			}
		}

		// Create cancellation shipment lines for remaining order lines
		for (Element remainingOrderLine : listOfOrderLine) {
			Element orderStatusesEle = SCXmlUtil.getChildElement(remainingOrderLine, E_ORDER_STATUSES);
			List<Element> listOforderStatus = SCXmlUtil.getChildrenList(orderStatusesEle);

			// Skip cancelled order lines
			if (listOforderStatus.size() == 1 && STR_STATUS_CANCELLED.equals(listOforderStatus.get(0).getAttribute(A_STATUS))) {
				logger.verbose("Skipping cancelled PrimeLineNo: " + remainingOrderLine.getAttribute(A_PRIME_LINE_NO));
				continue;
			}

			String primeLineNo = remainingOrderLine.getAttribute(A_PRIME_LINE_NO);
			
			Element shipmentLineToCancel = SCXmlUtil.createChild(shipmentLines, E_SHIPMENT_LINE);
			shipmentLineToCancel.setAttribute(A_ITEM_ID, SCXmlUtil.getXpathAttribute(remainingOrderLine, STR_XPATH_ITEM_ID));
			shipmentLineToCancel.setAttribute(A_ORDER_NO, orderNo);
			shipmentLineToCancel.setAttribute(A_QUANTITY, VAL_ZERO);
			shipmentLineToCancel.setAttribute(A_SHIPMENT_LINE_NO, primeLineNo);
			shipmentLineToCancel.setAttribute(A_RELEASE_NO, releaseNo);
			shipmentLineToCancel.setAttribute(A_UNIT_OF_MEASURE, SCXmlUtil.getXpathAttribute(remainingOrderLine, XPATH_ITEM_UNIT_OF_MEASURE));
		}

		logger.verbose("CrocsEMEAShipmentUtil: End of method addShipmentLineForCancellation with updated document: " + SCXmlUtil.getString(docConfirmShipment));
	}

	/**
     * Updates shipment line quantities based on short ship indicators.
     * 
     * @param inDoc Shipment document containing shipment line details.
     */
	protected void updateShortShipLines(Document inDoc) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateShortShipLines with input: " + SCXmlUtil.getString(inDoc));

		Element inDocEle = inDoc.getDocumentElement();
		Element shipmentListEle = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
		List<Element> listOfShipment = SCXmlUtil.getChildrenList(shipmentListEle);

		for (Element eachShipmentLineEle : listOfShipment) {
			String shipmentLineNo = eachShipmentLineEle.getAttribute(A_SHIPMENT_LINE_NO);
			String isShortShipLineFlag = eachShipmentLineEle.getAttribute(A_IS_SHORT_SHIP_LINE);

			if (!YFCCommon.isVoid(isShortShipLineFlag)) {
				
				if (FLAG_Y.equals(isShortShipLineFlag)) {
					eachShipmentLineEle.setAttribute(A_QUANTITY, VAL_ZERO);
					
				} else {
					String strOriginalQty = eachShipmentLineEle.getAttribute(A_ORG_QUANTITY);
					eachShipmentLineEle.setAttribute(A_QUANTITY, strOriginalQty);
					eachShipmentLineEle.removeAttribute(A_ORG_QUANTITY);
				}

				// Remove temporary flag after processing
				eachShipmentLineEle.removeAttribute(A_IS_SHORT_SHIP_LINE);
				logger.verbose("Cleaned up short ship flag for ShipmentLineNo: " + shipmentLineNo);
			}
		}
		logger.verbose("CrocsEMEAShipmentUtil: End of method updateShortShipLines with output: " + SCXmlUtil.getString(inDoc));
	}
		
	/**
     * Updates the input document to include short ship shipment lines.
     * 
     * @param inDoc Shipment document to be updated.
     * @param primeNoToLineMap Map containing PrimeLineNo to ShipmentLine mapping.
     * @throws YFSException if any error occurs during processing.
     */
	protected void updateInDocToHaveShortShipLines(Document inDoc, Map<String, Element> primeNoToLineMap) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateInDocToHaveShortShipLines with input: " + SCXmlUtil.getString(inDoc));

		try {
			Element inDocEle = inDoc.getDocumentElement();
			Element shipmentLinesEle = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
			List<Element> listOfShipmentLines = SCXmlUtil.getChildrenList(shipmentLinesEle);

			// Update existing lines
			Set<String> processedLines = updateExistingShipmentLines(listOfShipmentLines, primeNoToLineMap);
			
			// Add missing lines (short shipments)
			addNewShortShipLines(inDocEle, shipmentLinesEle, primeNoToLineMap, processedLines);

		} catch (Exception e) {
			logger.error("CrocsEMEAShipmentUtil: Error in updateInDocToHaveShortShipLines: " + e.getMessage(), e);
			throw new YFSException("Error in method CrocsEMEAShipmentUtil.updateInDocToHaveShortShipLines :" + e.getMessage());
		}

		logger.verbose("CrocsEMEAShipmentUtil: End of method updateInDocToHaveShortShipLines with output: " + SCXmlUtil.getString(inDoc));
	}
	
	/**
     * Adds missing shipment lines as short ship lines into the input document.
     * 
     * @param inDocEle Root element of the input shipment document.
     * @param eleShipmentLines ShipmentLines element where new lines will be added.
     * @param lineMap Map of PrimeLineNo to OrderLine elements.
     * @param processedLines Set of already processed PrimeLineNos.
     */
	private void addNewShortShipLines(Element inDocEle, Element eleShipmentLines, Map<String, Element> lineMap, Set<String> processedLines) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method addNewShortShipLines: inDocEle " + SCXmlUtil.getString(inDocEle));
		logger.verbose("eleShipmentLines " + SCXmlUtil.getString(eleShipmentLines));

		String orderNo = inDocEle.getAttribute(A_ORDER_NO);
		String releaseNo = inDocEle.getAttribute(A_RELEASE_NO);

		for (Map.Entry<String, Element> entry : lineMap.entrySet()) {
			String primeLineNo = entry.getKey();
			Element eleOrderLine = entry.getValue();

			if (!processedLines.contains(primeLineNo) && isEligibleForShortShip(eleOrderLine)) {				
				Element eleShipmentLineToCancel = SCXmlUtil.createChild(eleShipmentLines, E_SHIPMENT_LINE);
				
				eleShipmentLineToCancel.setAttribute(A_ITEM_ID, SCXmlUtil.getXpathAttribute(eleOrderLine, STR_XPATH_ITEM_ID));
				eleShipmentLineToCancel.setAttribute(A_ORDER_NO, orderNo);
				
				String qty = getEligibleStatusQty(eleOrderLine);
				eleShipmentLineToCancel.setAttribute(A_QUANTITY, qty);
				eleShipmentLineToCancel.setAttribute(A_IS_SHORT_SHIP_LINE, FLAG_Y);
				eleShipmentLineToCancel.setAttribute(A_SHIPMENT_LINE_NO, eleOrderLine.getAttribute(A_PRIME_LINE_NO));
				eleShipmentLineToCancel.setAttribute(A_RELEASE_NO, releaseNo);
				eleShipmentLineToCancel.setAttribute(A_UNIT_OF_MEASURE, SCXmlUtil.getXpathAttribute(eleOrderLine, XPATH_ITEM_UNIT_OF_MEASURE));
			}
		}

		logger.verbose("CrocsEMEAShipmentUtil: End of method addNewShortShipLines with updated eleShipmentLines " + SCXmlUtil.getString(eleShipmentLines));
	}

	/**
     * Determines whether an order line is eligible to be treated as a short ship line. 
     * Lines with only a single status of SHIPPED or CANCELLED are excluded from short ship processing.
     * 
     * @param eleOrderLine OrderLine element to evaluate.
     * @return true if the line is eligible for short shipment, false otherwise.
     */
	private boolean isEligibleForShortShip(Element eleOrderLine) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method isEligibleForShortShip with eleOrderLine " + SCXmlUtil.getString(eleOrderLine));

		if (YFCCommon.isVoid(eleOrderLine)) {
			logger.verbose("OrderLine is void. Returning false.");
			return false;
		}

		Element orderStatusesEle = SCXmlUtil.getChildElement(eleOrderLine, E_ORDER_STATUSES);
		List<Element> listOfOrderStatus = SCXmlUtil.getChildrenList(orderStatusesEle);

		// If there is only one status and it is SHIPPED or CANCELLED,
		// it is not eligible for short ship processing
		if (listOfOrderStatus.size() == 1) {
			String status = listOfOrderStatus.get(0).getAttribute(A_STATUS); 
			
			boolean eligible = !(STR_STATUS_CANCELLED.equals(status) || STATUS_SHIPPED.equals(status));
			logger.verbose("Eligibility result (single status rule): " + eligible);
			return eligible;
		}

		return true;
	}
	
	/**
	 * Updates existing shipment lines using order line information and prepares
	 * them for short ship processing evaluation.
	 * 
	 * @param shipmentLines    List of existing ShipmentLine elements.
	 * @param primeNoToLineMap Map of PrimeLineNo to OrderLine elements.
	 * @return Set of processed PrimeLineNos.
	 */
	private Set<String> updateExistingShipmentLines(List<Element> shipmentLines, Map<String, Element> primeNoToLineMap) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateExistingShipmentLines");

		Set<String> processedLines = new HashSet<>();
		
		for (Element eachShipment : shipmentLines) {
			
			String primeLineNo = eachShipment.getAttribute(A_SHIPMENT_LINE_NO);
			processedLines.add(primeLineNo);

			Element eleOrderLine = primeNoToLineMap.get(primeLineNo);
			if (!YFCCommon.isVoid(eleOrderLine)) {
				String strShipmentLineQty = eachShipment.getAttribute(A_QUANTITY);
				String strOrderLineQty = getEligibleStatusQty(eleOrderLine);

				eachShipment.setAttribute(A_QUANTITY, strOrderLineQty);
				eachShipment.setAttribute(A_ORG_QUANTITY, strShipmentLineQty);
				eachShipment.setAttribute(A_IS_SHORT_SHIP_LINE, FLAG_N);
			}
		}
		logger.verbose("CrocsEMEAShipmentUtil: End of method updateExistingShipmentLines. Processed count: " + processedLines.size());
		return processedLines;
	}
	
	/**
	 * Retrieves the eligible quantity for an order line based on its status.
	 * 
	 * If no matching status is found or quantity is missing, it returns "0.00".
	 * 
	 * @param eleOrderLine OrderLine element to evaluate.
	 * @return Eligible quantity as String, or "0.00" if not found.
	 */
	private String getEligibleStatusQty(Element eleOrderLine) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method getEligibleStatusQty with eleOrderLine: " + SCXmlUtil.getString(eleOrderLine));

		Element orderStatusesEle = SCXmlUtil.getChildElement(eleOrderLine, E_ORDER_STATUSES);

		if (!YFCCommon.isVoid(orderStatusesEle)) {
			List<Element> listOforderStatus = SCXmlUtil.getChildrenList(orderStatusesEle);

			for (Element eleStatus : listOforderStatus) {
				String status = eleStatus.getAttribute(A_STATUS);

				// Only Released or Included In Shipment
				if (STATUS_RELEASE.equals(status) || STATUS_INCLUDED_IN_SHIPMENT.equals(status)) {
					
					String qty = eleStatus.getAttribute(A_STAT_QTY);
					if (!YFCCommon.isVoid(qty)) {
						return qty;
					}
				}
			}
		}

		logger.verbose("No eligible status quantity found. Returning 0.00");
		return "0.00";
	}
	
	/**
     * Prepares a document for creating an asynchronous request with the provided input element.
     * @param inDocEle     The input XML element to be wrapped in the async request document.
     * @return             A Document representing the async request api input.
     */
	protected Document prepareCreateAsyncRequestInput(Element inDocEle) {
		logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: Start of method prepareCreateAsyncRequestInput with inDocEle: " + SCXmlUtil.getString(inDocEle));

		// Create root async request document
		Document asyncReqDoc = SCXmlUtil.createDocument(E_CREATE_ASYNC_REQUEST);
		Element asyncReqEle = asyncReqDoc.getDocumentElement();

		// Add API element
		Element apiElement = SCXmlUtil.createChild(asyncReqEle, E_API);
		apiElement.setAttribute(A_IS_SERVICE, FLAG_Y);
		apiElement.setAttribute(A_NAME, SERVICE_CROCS_CONFIRM_SHIPMENT_SYNC_SERV);

		// Imported inDocEle element to Input
		Element inputElement = SCXmlUtil.createChild(apiElement, E_INPUT);
		SCXmlUtil.importElement(inputElement, inDocEle);

		logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: End of method prepareCreateAsyncRequestInput with asyncReqDoc: " + SCXmlUtil.getString(asyncReqDoc));
		return asyncReqDoc;		
	}

    /**
     * This function is used to invoke createAsyncRequestAPI
     * @param env
     * @param createAsyncReqDoc The input received to invoked the createAsyncRequestAPI
     * @return	The response returned by createAsyncRequestAPI
     */
	protected Document invokeCreateAsyncRequestAPI(YFSEnvironment env, Document createAsyncReqDoc) {
        logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: Start of method invokeCreateAsyncRequestAPI with input: " + SCXmlUtil.getString(createAsyncReqDoc));
    	try {
            Document result = CommonUtil.invokeAPI(env, "", API_CREATE_ASYNC_REQUEST, createAsyncReqDoc);
            logger.verbose("End of method invokeCreateAsyncRequestAPI with output: " + SCXmlUtil.getString(result));
            return result;
    	}catch (Exception e) {
    		logger.error("Error invoking createAsyncRequestAPI API: " + e.getMessage());
            throw new YFSException("Error invoking createAsyncRequestAPI API: " + e.getMessage());
		}
    }

	/**
     * If Update with WMSCode = 1 is processed then at the Shipment/@Code would be 1
	 * @param shipmentListOutput     The output Document from the getShipmentList API, containing shipment details.
     * @return						 true if any shipment has Code = 1; false otherwise.
     */
    protected boolean checkIfUpdateWithWMSCode1IsProcessed(Document shipmentListOutput) {
    	logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: Start of method checkIfUpdateWithWMSCode1IsProcessed with shipmentListOutput: " + SCXmlUtil.getString(shipmentListOutput));
    	Element shipmentListEle = shipmentListOutput.getDocumentElement();
    	List<Element> listOfShipment = SCXmlUtil.getChildrenList(shipmentListEle);

    	for(Element eachShipment : listOfShipment) {
    		if(VAL_ONE.equals(eachShipment.getAttribute(A_CODE))) {
    	    	logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: Code = 1 found in shipment Returning true");
    	    	logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: End of method checkIfUpdateWithWMSCode1IsProcessed");
    	    	return true;
    		}
    	}
        logger.verbose("No shipment found with Code = 1. Returning false.");
    	logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: End of method checkIfUpdateWithWMSCode1IsProcessed:");
    	return false;
    }
    
	/**
     * Updates tracking details at order line level by invoking ChangeOrder API.
     * 
     * @param env YFSEnvironment object.
     * @param docOrderReleaseList Order release document containing order context.
     * @param shipmentLineToTrackingNoMap Map of ShipmentLineNo to updated OrderLine elements.
     * @throws YFSException if ChangeOrder API invocation fails.
     */
	protected void updateTrackingDetailsAtOrderLine(YFSEnvironment env, Document docOrderReleaseList, Map<String, Element> shipmentLineToTrackingNoMap) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateTrackingDetailsAtOrderLine with docOrderReleaseList as: " + SCXmlUtil.getString(docOrderReleaseList));

		Element eleOrderReleaseList = docOrderReleaseList.getDocumentElement();
		Element orderRelease = SCXmlUtil.getChildElement(eleOrderReleaseList, E_ORDER_RELEASE);
		Element eleOrder = SCXmlUtil.getChildElement(orderRelease, E_ORDER);

		String strOrderHeaderKey = orderRelease.getAttribute(A_ORDER_HEADER_KEY);
		String strOrderNo = eleOrder.getAttribute(A_ORDER_NO);
		String strEnterpriseCode = eleOrder.getAttribute(A_ENTERPRISE_CODE);

		// Prepare ChangeOrder input
		Document changeOrderInDoc = SCXmlUtil.createDocument(E_ORDER);
		Element orderEle = changeOrderInDoc.getDocumentElement();

		orderEle.setAttribute(A_ACTION, VAL_MODIFY);
		orderEle.setAttribute(A_ORDER_HEADER_KEY, strOrderHeaderKey);
		orderEle.setAttribute(A_ORDER_NO, strOrderNo);
		orderEle.setAttribute(A_ENTERPRISE_CODE, strEnterpriseCode);
		orderEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);
		orderEle.setAttribute(A_OVERRIDE, FLAG_Y);

		Element orderLineListEle = SCXmlUtil.createChild(orderEle, E_ORDER_LINES);

		for (Map.Entry<String, Element> entry : shipmentLineToTrackingNoMap.entrySet()) {
			Element eleOrderLine = entry.getValue();
			SCXmlUtil.importElement(orderLineListEle, eleOrderLine);
		}

		try {
			Document changeOrderOutDoc = CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER, changeOrderInDoc);
			logger.verbose("ChangeOrder response: " + SCXmlUtil.getString(changeOrderOutDoc));

		} catch (Exception e) {
			logger.error("Error in ChangeOrder API invocation: " + e.getMessage(), e);
			throw new YFSException("Error while invoking changeOrder API:" + e.getMessage());
		}

		logger.verbose("CrocsEMEAShipmentUtil: End of method updateTrackingDetailsAtOrderLine");
	}
	
	/**
     * Validates and handles WMS Code-2 processing when it arrives before Code-1.
     * 
     * WMSCode-1 is expected before WMSCode-2.
     * If Code-2 arrives first, the system defers processing by creating an async request.
     * 
     * @param env YFSEnvironment object.
     * @param eleConfirmShipment ConfirmShipment element containing WMS request data.
     * @param docGetShipmentListForOrder Shipment list document used for validation.
     * @return true if async processing is triggered for Code-2, false otherwise.
     * @throws YFSException if duplicate async request is detected.
     */
	protected boolean validateAndHandleWMSCode2Processing(YFSEnvironment env, Element eleConfirmShipment, Document docGetShipmentListForOrder) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method validateAndHandleWMSCode2Processing with eleConfirmShipment: " + SCXmlUtil.getString(eleConfirmShipment));
		logger.verbose("docGetShipmentListForOrder: " + SCXmlUtil.getString(docGetShipmentListForOrder));

		boolean isCode1UpdateProcessed = checkIfUpdateWithWMSCode1IsProcessed(docGetShipmentListForOrder);

		logger.verbose("WMS Flow - Code1 processed flag: " + isCode1UpdateProcessed);

		if (isCode1UpdateProcessed) {
			return false;
		}
		// Code=2 arrived before Code=1
		String isAsyncProcess = eleConfirmShipment.getAttribute(A_IS_ASYNC_PROCESS);

		boolean isDuplicateAsyncRequest = !YFCCommon.isVoid(isAsyncProcess) && FLAG_Y.equals(isAsyncProcess);

		if (isDuplicateAsyncRequest) {
			logger.error("Duplicate async request detected for WMS Code-2");
			throw new YFSException(CrocsErrorConstants.VAL_ERROR_DESCRIPTION_WMSCODE, CrocsErrorConstants.VAL_ERROR_CODE_EXTN_005, CrocsErrorConstants.VAL_ERROR_DESCRIPTION_EXTN_005);
		}
		eleConfirmShipment.setAttribute(A_IS_ASYNC_PROCESS, FLAG_Y);
		invokeCreateAsyncRequestAPI(env, prepareCreateAsyncRequestInput(eleConfirmShipment));

		logger.verbose("CrocsEMEAShipmentUtil: End of method validateAndHandleWMSCode2Processing: WMS Code-2 received before Code-1. Async request created.");
		return true;
	}
	
	/**
     * Resolves whether to create a new shipment or fetch existing shipment list based on WMS code and order status.
     * 
     * @param env YFSEnvironment object.
     * @param docOrderReleaseList Order release document.
     * @param docConfirmShipment Confirm shipment request document containing WMS details.
     * @return Document either containing newly created shipment or existing shipment list.
     */
	protected Document resolveShipmentList(YFSEnvironment env, Document docOrderReleaseList, Document docConfirmShipment) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method resolveShipmentList with docConfirmShipment " + SCXmlUtil.getString(docConfirmShipment));
		logger.verbose("docOrderReleaseList " + SCXmlUtil.getString(docOrderReleaseList));

		Element eleConfirmShipment = docConfirmShipment.getDocumentElement();
		Element eleOrderReleaseList = SCXmlUtil.getChildElement(docOrderReleaseList.getDocumentElement(), E_ORDER_RELEASE);
		
		String currentOrderStatus = SCXmlUtil.getAttribute(eleOrderReleaseList, A_STATUS);
		String strOrderNo = SCXmlUtil.getAttribute(eleConfirmShipment, A_ORDER_NO);
		String strWMSCode = SCXmlUtil.getAttribute(eleConfirmShipment, A_WMS_CODE);
		String strReleaseNo = SCXmlUtil.getAttribute(eleConfirmShipment, A_RELEASE_NO);
		String strEnterpriseCode = eleOrderReleaseList.getAttribute(A_ENTERPRISE_CODE);
		
		logger.verbose("OrderNo: " + strOrderNo + ", WMSCode: " + strWMSCode + ", OrderStatus: " + currentOrderStatus);

		boolean isWMSCode3OR5 = VAL_THREE.equals(strWMSCode) || VAL_FIVE.equals(strWMSCode);
		boolean isOrderInReleasedStatus = RELEASED.equals(currentOrderStatus);

		Document docShipmentDetails;
		if (isWMSCode3OR5 && isOrderInReleasedStatus) {
			logger.verbose("Creating shipment for WMS Code 3/5 with Released order");
			docShipmentDetails = createShipment(env, docOrderReleaseList);
			logger.verbose("CrocsEMEAShipmentUtil: End of method resolveShipmentList with docShipmentDetails " + SCXmlUtil.getString(docShipmentDetails));
			return docShipmentDetails;
		}

		logger.verbose("Fetching existing shipment list for OrderNo: " + strOrderNo);
		docShipmentDetails = invokeGetShipmentListForOrder(env, strOrderNo, strReleaseNo, strEnterpriseCode);
		logger.verbose("CrocsEMEAShipmentUtil: End of method resolveShipmentList with docShipmentDetails " + SCXmlUtil.getString(docShipmentDetails));
		
		return docShipmentDetails;
	}
	
	/**
	 * Updates the tracking URL based on carrier, customer locale, enterprise,
	 * and order delivery zip code.
	 * 
	 * @param strPrimaryUrl Base tracking URL received from carrier configuration.
	 * @param strSCAC Carrier SCAC code.
	 * @param docOrderReleaseList Order release document containing locale and address details.
	 * @return Updated tracking URL based on carrier and locale configuration.
	 */
	public String updateTrackingURLAsPerLocale(String strPrimaryUrl, String strSCAC, Document docOrderReleaseList) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method updateTrackingURLAsPerLocale");
		logger.verbose("SCAC: " + strSCAC + ", PrimaryURL: " + strPrimaryUrl);
		logger.verbose("docOrderReleaseList: " + SCXmlUtil.getString(docOrderReleaseList));
		
		Element eleOrderRelease = docOrderReleaseList.getDocumentElement();
		
		String strCustomerLocale = SCXmlUtil.getXpathAttribute(eleOrderRelease, XPATH_ORDERRELEASELIST_ORDERRELEASE_ORDER_EXTNCUSTOMERLOCALE);
		String strEnterpriseCode = SCXmlUtil.getXpathAttribute(eleOrderRelease, XPATH_ORDERRELEASELIST_ORDERRELEASE_ENTERPRISECODE);
    	String strOrderZipCode = SCXmlUtil.getXpathAttribute(eleOrderRelease, XPATH_ORDER_RELEASE_LIST_ZIP_CODE);
    
    	strOrderZipCode = Objects.toString(strOrderZipCode, "");
    	String strUpdatedTrackingURL;
    	
    	logger.verbose(EXTN_CUSTOMER_LOCALE + strCustomerLocale);
    	logger.verbose(EnterpriseCode + strEnterpriseCode);
    	logger.verbose(ShipToZipCode + strOrderZipCode);
    	
	    switch (strSCAC) {
	        case STR_SCAC_UPSC:
				logger.verbose("Updating tracking URL for UPS carrier");
	            String strUPSLocale = getUPSLocale(strCustomerLocale, strEnterpriseCode);
	            strUpdatedTrackingURL = strPrimaryUrl.replace(STR_EN_CA, strUPSLocale);
	            break;
	            
	        case STR_SCAC_DPD:
				logger.verbose("Updating tracking URL for DPD carrier");
	        	strUpdatedTrackingURL = getDPDTrackingURL(strPrimaryUrl, strCustomerLocale, strEnterpriseCode, strOrderZipCode);
	        	break;

	        case STR_SCAC_GLS:
				logger.verbose("Updating tracking URL for GLS carrier");
	        	strUpdatedTrackingURL = getGLSTrackingURL(strPrimaryUrl, strCustomerLocale, strEnterpriseCode, strOrderZipCode);
	        	break;
	        	
	        default:
				logger.verbose("No carrier-specific URL update required for SCAC: " + strSCAC);
	        	strUpdatedTrackingURL = strPrimaryUrl;
	    }
	    
		logger.verbose("CrocsEMEAShipmentUtil: End of method updateTrackingURLAsPerLocale. UpdatedTrackingURL: " + strUpdatedTrackingURL);
		return strUpdatedTrackingURL;
	}
	
	/**
	 * Resolves the UPS tracking locale based on customer locale and enterprise code.
	 * 
	 * UPS URL Configured: https://www.ups.com/track?loc=en_CA&tracknum=TrackingNo
	 * 
	 * @param strCustomerLocale Customer locale from the order.
	 * @param strEnterpriseCode Enterprise code associated with the order.
	 * @return UPS locale value to be used in the tracking URL.
	 */
	private String getUPSLocale(String strCustomerLocale, String strEnterpriseCode) {
		logger.verbose("CrocsEMEAShipmentUtil: Start of method getUPSLocale");
		
    	logger.verbose(EXTN_CUSTOMER_LOCALE + strCustomerLocale);
    	logger.verbose(EnterpriseCode + strEnterpriseCode);

		String strUPSLocale;

		switch (strEnterpriseCode) {

			case CROCS_DE, HEYDUDE_DE:
				strUPSLocale = STR_DE_DE.equalsIgnoreCase(strCustomerLocale) ? STR_DE_DE : STR_EN_DE;
				break;

			case CROCS_FI:
				strUPSLocale = STR_FI_FI.equalsIgnoreCase(strCustomerLocale) ? STR_FI_FI : STR_EN_FI;
				break;

			case CROCS_FR, HEYDUDE_FR:
				strUPSLocale = STR_FR_FR.equalsIgnoreCase(strCustomerLocale) ? STR_FR_FR : STR_EN_FR;
				break;

			case CROCS_NL:
				strUPSLocale = STR_NL_NL.equalsIgnoreCase(strCustomerLocale) ? STR_NL_NL : STR_EN_NL;
				break;

			default:
				// CROCS_GB, HEYDUDE_GB, CROCS_EU, HEYDUDE_EU
				strUPSLocale = STR_EN_GB;
				break;
		}

		logger.verbose("CrocsEMEAShipmentUtil: End of method getUPSLocale with value of strUPSLocale " + strUPSLocale);
		return strUPSLocale;
	}
	
	/**
	 * Resolves the GLS tracking URL based on customer locale, enterprise code,
	 * and order zip code.
	 * 
	 * GLS URL Configured: https://gls-group.eu/GROUP/en/parcel-tracking?match=TrackingNo
	 * 
	 * @param strPrimaryUrl Default GLS tracking URL.
	 * @param strCustomerLocale Customer locale from the order.
	 * @param strEnterpriseCode Enterprise code associated with the order.
	 * @param strOrderZipCode Delivery zip code associated with the order.
	 * @return GLS tracking URL customized for the enterprise and locale.
	 */
	private String getGLSTrackingURL(String strPrimaryUrl, String strCustomerLocale, String strEnterpriseCode, String strOrderZipCode) {

		logger.verbose("CrocsEMEAShipmentUtil: Start of method getGLSTrackingURL");
		logger.verbose("CustomerLocale: " + strCustomerLocale + ", EnterpriseCode: " + strEnterpriseCode + ", ZipCode: " + strOrderZipCode);

		String strGLSTrackingURL;

		switch (strEnterpriseCode) {

			case CROCS_DE, HEYDUDE_DE:
				logger.verbose("Resolving GLS tracking URL for Germany");

				strGLSTrackingURL = STR_DE_DE.equalsIgnoreCase(strCustomerLocale)
						? GLS_TRACKING_URL_DE_DE + strOrderZipCode
						: GLS_TRACKING_URL_DE_EN + strOrderZipCode;
				break;

			case CROCS_FI:
				logger.verbose("Resolving GLS tracking URL for Finland");

				strGLSTrackingURL = STR_FI_FI.equalsIgnoreCase(strCustomerLocale)
						? GLS_TRACKING_URL_FI_FI
						: GLS_TRACKING_URL_FI_EN;
				break;

			case CROCS_NL:
				logger.verbose("Resolving GLS tracking URL for Netherlands");
				
				strGLSTrackingURL = GLS_TRACKING_URL_NL + strOrderZipCode;
				break;

			default:
				// CROCS_GB, HEYDUDE_GB, CROCS_EU, HEYDUDE_EU
				logger.verbose("Using default GLS tracking URL");

				strGLSTrackingURL = strPrimaryUrl;
				break;
		}

		logger.verbose("CrocsEMEAShipmentUtil: End of method getGLSTrackingURL. GLSTrackingURL: " + strGLSTrackingURL);

		return strGLSTrackingURL;
	}
	
	/**
	 * Resolves the DPD tracking URL based on customer locale, enterprise code,
	 * and order zip code.
	 * 
	 * DPD URL Configured: https://www.dpdgroup.com/EnterpriseCode/mydpd/my-parcels/search?parcelNumber=TrackingNo&lang=locale
	 * 
	 * @param strPrimaryUrl Default DPD tracking URL.
	 * @param strCustomerLocale Customer locale from the order.
	 * @param strEnterpriseCode Enterprise code associated with the order.
	 * @param strOrderZipCode Delivery zip code associated with the order.
	 * @return DPD tracking URL customized for the enterprise and locale.
	 */
	private String getDPDTrackingURL(String strPrimaryUrl, String strCustomerLocale, String strEnterpriseCode, String strOrderZipCode) {

		logger.verbose("CrocsEMEAShipmentUtil: Start of method getDPDTrackingURL");
		logger.verbose("CustomerLocale: " + strCustomerLocale + ", EnterpriseCode: " + strEnterpriseCode + ", ZipCode: " + strOrderZipCode + ", PrimaryURL: " + strPrimaryUrl);

		String strDPDTrackingURL;

		switch (strEnterpriseCode) {

			case CROCS_DE, HEYDUDE_DE:
				strDPDTrackingURL = strPrimaryUrl
						.replace(EnterpriseCode, STR_DE)
						.replace(N_LOCALE, STR_DE_DE.equalsIgnoreCase(strCustomerLocale) ? STR_DE : STR_EN);
				break;

			case CROCS_FI:
				strDPDTrackingURL = STR_FI_FI.equalsIgnoreCase(strCustomerLocale) ? DPD_TRACKING_URL_FI_FI : DPD_TRACKING_URL_FI_EN;
				break;

			case CROCS_FR, HEYDUDE_FR:
				strDPDTrackingURL = strPrimaryUrl
						.replace(EnterpriseCode, STR_FR)
						.replace(N_LOCALE, STR_FR_FR.equalsIgnoreCase(strCustomerLocale) ? STR_FR : STR_EN);
				break;

			case CROCS_NL:
				strDPDTrackingURL = strPrimaryUrl
						.replace(EnterpriseCode, STR_NL)
						.replace(N_LOCALE, STR_NL_NL.equalsIgnoreCase(strCustomerLocale) ? STR_NL : STR_EN);
				break;

			default:
				// CROCS_GB, HEYDUDE_GB, CROCS_EU, HEYDUDE_EU
				strDPDTrackingURL = DPD_TRACKING_URL_DEFAULT + strOrderZipCode;
				break;
		}

		logger.verbose("CrocsEMEAShipmentUtil: End of method getDPDTrackingURL with value of DPDTrackingURL: " + strDPDTrackingURL);
		return strDPDTrackingURL;
	}
	
}
