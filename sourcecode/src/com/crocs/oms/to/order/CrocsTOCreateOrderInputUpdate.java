package com.crocs.oms.to.order;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;


/**
 *Java implementation for Transfer Orders. This class customizes order
 * creation for Transfer Orders (TO) in Crocs OMS. It performs three key
 * operations: Updates Requested Delivery Date based on Common Codes Replaces
 * PersonInfoBillTo using store's Organization details Enriches OrderLine items
 * from OMS master data
 */
public class CrocsTOCreateOrderInputUpdate implements CrocsXmlConstants{

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsTOCreateOrderInputUpdate.class);


	/**
	 * Document-based override called before creating a Transfer Order.
	 *
	 * @param env   YFS environment
	 * @param inDoc Input Order Document
	 * @return Modified Order Document
	 * @throws Exception 
	 * @throws YFSUserExitException if any customization fails
	 */
	public Document  updateOrderAttributes(YFSEnvironment env, Document inDoc) {
		logger.verbose("CrocsTOCreateOrderInputUpdate  Input: " + SCXmlUtil.getString(inDoc));

		try {
			Element eleOrder = inDoc.getDocumentElement();

			// update PersonInfoBillTo on the Transfer Order
			updatePersonInfoBillTo(env, inDoc, eleOrder);

			//EOMS-12493 Stamp PersonInfoShipTo at orderline level
			updateOrderLinePersonInfoShipTo(inDoc,eleOrder);

			// Always enrich items from OMS master data
			fetchAndUpdateOrderItems(env, eleOrder);

			// As part of --EOMS-13648-- moving the transformation logic to updateReqDeliveryDate method below; this allows fetching EnterpriseCode from eleOrder after setting it in updatePersonInfoBillTo method
			//Apply ReqDeliveryDate transformations for Transfer Orders
			updateReqDeliveryDate(env, eleOrder);
		} catch (Exception e) {
			logger.error("Exception in CrocsTOCreateOrderInputUpdate", e);
			throw  new YFSException("CrocsTOCreateOrderInputUpdate  updateOrderAttributes Failed: " + e.getMessage());
		}

		logger.verbose("CrocsTOCreateOrderInputUpdate Final Output: " + SCXmlUtil.getString(inDoc));
		return inDoc;
	}

	// ================================
	// 1. Update Requested Delivery Date
	// ================================

	/**
	 * Updates Requested Delivery Date by adding delta days calculated from Common
	 * Codes.
	 *
	 * @param env      YFS environment
	 * @param eleOrder Order element
	 * @throws Exception
	 */
	private void updateReqDeliveryDate(YFSEnvironment env, Element eleOrder) throws Exception {

		// Fetch Common Codes for date calculation
		String orderType = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_NAME);
		String omsDateFormat = "yyyy-MM-dd'T'HH:mm:ss.S";
		SimpleDateFormat sdf = new SimpleDateFormat(omsDateFormat);
		Calendar cal = Calendar.getInstance();

		if (CrocsConstant.A_RUSH_TO.equalsIgnoreCase(orderType)) {
			cal.setTime(new java.util.Date());
			String rushDate = sdf.format(cal.getTime());
			eleOrder.setAttribute(CrocsXmlConstants.A_REQ_DELIVERY_DATE, rushDate);
			logger.info("RushTO → ReqDeliveryDate set to SYSDATE: " + rushDate);
		} else if (CrocsConstant.A_STANDARD_TO.equalsIgnoreCase(orderType)) {
			//EOMS-13648-START
			String strOrgCode = eleOrder.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
			//EOMS-13648-END
			YFCDocument getCCListDoc = YFCDocument.createDocument(CrocsXmlConstants.A_COMMON_CODE);
			getCCListDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_CODE_TYPE,
					CrocsConstant.A_CROCS_TO_DATECAL);
			//EOMS-13648-START
			getCCListDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE,
					strOrgCode);
			//EOMS-13648-END

			Document ccOut = CommonUtil.invokeAPI(env,
					CrocsTemplateConstants.TEMPLATE_MODIFY_GET_COMMON_CODE_LIST_FOR_TO,
					CrocsAPIConstants.API_GET_COMMON_CODE_LIST, getCCListDoc.getDocument());
			validateResponse(ccOut, CrocsAPIConstants.API_GET_COMMON_CODE_LIST);
			//EOMS-13648-START
			int daysToAdd = 23;
			if (!YFCObject.isNull(ccOut) && ccOut.getDocumentElement().getElementsByTagName(CrocsXmlConstants.A_COMMON_CODE).getLength() > 0) {
				daysToAdd = calculateDaysToAdd(ccOut);
			}
			//EOMS-13648-END
			// Add offset days
			cal.add(Calendar.DATE, daysToAdd);
			String newDate = sdf.format(cal.getTime());
			eleOrder.setAttribute(CrocsXmlConstants.A_REQ_DELIVERY_DATE, newDate);

			logger.info("CrocsTOCreateOrderInputUpdate : Updated ReqDeliveryDate to: " + newDate);
		}
	}

	/**
	 * Calculates date offset using Intransit and STANDARDTODATE values from Common
	 * Codes.
	 *
	 * @param ccOut Common Code API output
	 * @return Days to add to Requested Delivery Date
	 */
	private  int calculateDaysToAdd(Document ccOut) {
		NodeList ccList = ccOut.getDocumentElement().getElementsByTagName(CrocsXmlConstants.A_COMMON_CODE);
		int intransitDays = -1;
		int standardTODays = -1;

		// Parse Common Codes
		for (int i = 0; i < ccList.getLength(); i++) {
			Element cc = (Element) ccList.item(i);
			String value = cc.getAttribute(CrocsXmlConstants.A_CODE_VALUE);
			int days = Integer.parseInt(cc.getAttribute(CrocsXmlConstants.A_CODE_SHORT_DESCRIPTION));

			if (CrocsXmlConstants.A_INTRANSIT.equalsIgnoreCase(value)) {
				intransitDays = days;
			} else if (CrocsXmlConstants.A_STANDARD_TO_DATE.equalsIgnoreCase(value)) {
				standardTODays = days;
			}
		}
		if (intransitDays == -1 || standardTODays == -1) {
			throw new YFSException("CrocsTOCreateOrderInputUpdate : Missing Intransit/StandardDate in CommonCode");
		}
		return standardTODays - intransitDays;
	}

	// ================================
	// 2. Update PersonInfoBillTo
	// ================================

	/**
	 * Replaces PersonInfoBillTo on the order with CorporatePersonInfo from the
	 * store's Organization record.
	 * @throws Exception 
	 */
	private void updatePersonInfoBillTo(YFSEnvironment env, Document inDoc, Element eleOrder)
			throws Exception {
		Element eleOrderLine = (Element) inDoc.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE).item(0);
		if (YFCObject.isNull(eleOrderLine)) {
			throw new YFSException("CrocsTOCreateOrderInputUpdate: updatePersonInfoBillTo -OrderLine missing, cannot fetch ReceivingNode.");
		}

		String storeNo = eleOrderLine.getAttribute(CrocsXmlConstants.A_RECEIVING_NODE);
		if (YFCObject.isNull(storeNo)) {
			throw new YFSException("CrocsTOCreateOrderInputUpdate: updatePersonInfoBillTo -ReceivingNode missing in OrderLine.");
		}

		// Fetch Organization for the store
		YFCDocument orgDoc = YFCDocument.createDocument(CrocsConstant.E_ORGANIZATION);
		orgDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, storeNo);

		Document orgOut = CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_GET_ORGANIZATION_LIST_TO,
		        CrocsAPIConstants.API_GET_ORGANIZATION_LIST,
		        orgDoc.getDocument());
		validateResponse(orgOut, CrocsConstant.A_CROCS_GET_ORGANIZATION_LIST);

		Element eleOrg = (Element) orgOut.getDocumentElement().getElementsByTagName(CrocsXmlConstants.E_ORGANIZATION)
				.item(0);
		Element extnOrg = (Element) eleOrg.getElementsByTagName(CrocsXmlConstants.E_EXTN).item(0);
		if (YFCObject.isNull(eleOrg)) {
			throw new YFSException("CrocsTOCreateOrderInputUpdate : updatePersonInfoBillTo -No Organization found for store: " + storeNo);
		}

		// Replace PersonInfoBillTo with billToPersonInfo from Organization
		Element billToPersonInfo = (Element) eleOrg.getElementsByTagName(CrocsXmlConstants.E_BILLING_PERSON_INFO)
				.item(0);
		//EOMS-9293 - stamp CorporatePersonInfo if the billToPersonInfo is not present 
		if (YFCObject.isNull(billToPersonInfo)) {
			billToPersonInfo = (Element) eleOrg.getElementsByTagName(CrocsXmlConstants.E_CORPORATE_PERSONINFO)
					.item(0);
		}
		//EOMS- 9293 end
		if (!YFCObject.isNull(billToPersonInfo)) {
			Element newPIB = (Element) inDoc.importNode(billToPersonInfo, true);
			// Setting Orgname as First name to send it to WMS
			String strOrgName = extnOrg.getAttribute(CrocsXmlConstants.A_EXTN_SAP_ORGANIZATION_NAME);
			if (!YFCObject.isVoid(strOrgName)) {
				newPIB.setAttribute(CrocsXmlConstants.A_FIRST_NAME, strOrgName);
			}
			// JIRA - 6001 : Start
			String strParentOrgCode = eleOrg.getAttribute(CrocsXmlConstants.A_PARENT_ORGANIZATION_CODE);
			            
			//based on the receiving node set EnterpriseCode,BuyerOrganizationCode,SellerOrganizationCode,AllocationRuleID
			if (!YFCObject.isVoid(strParentOrgCode)) {
				eleOrder.setAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE, strParentOrgCode);
				eleOrder.setAttribute(CrocsXmlConstants.A_BUYER_ORGANIZATION_CODE, strParentOrgCode);
				eleOrder.setAttribute(CrocsXmlConstants.A_SELLER_ORGANIZATION_CODE, strParentOrgCode);
				eleOrder.setAttribute(CrocsXmlConstants.A_ALLOCATION_RULE_ID, CrocsConstant.A_CROCS_TO_SCH);
			} else {
				logger.error(
						"CrocsTOCreateOrderInputUpdate : updatePersonInfoBillTo - ParentOrganizationCode Not found on receiving node");
				throw new YFSException(
						"CrocsTOCreateOrderInputUpdate updatePersonInfoBillTo : ParentOrganizationCode Not found on receiving node");
			}
			// JIRA - 6001 : End
			inDoc.renameNode(newPIB, null, CrocsXmlConstants.E_PERSON_INFO_BILL_TO);

			Element oldPIB = (Element) eleOrder.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_BILL_TO).item(0);
			if (oldPIB != null) {
				eleOrder.removeChild(oldPIB);
			}
			eleOrder.appendChild(newPIB);
			logger.info("CrocsTOCreateOrderInputUpdate : updatePersonInfoBillTo - Updated PersonInfoBillTo from Organization");
		} else {
			logger.warn("CrocsTOCreateOrderInputUpdate: updatePersonInfoBillTo - CorporatePersonInfo missing for store: " + storeNo);
		}
	}

	// ================================
	// 3. Enrich Items from OMS
	// ================================

	/**
	 * Enriches OrderLine items by resolving UPC aliases and fetching master data
	 * from OMS.
	 * 
	 * @throws Exception
	 */
	private void fetchAndUpdateOrderItems(YFSEnvironment env, Element eleOrder) throws Exception {
		String strEnterpriseCode = eleOrder.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
		NodeList orderLines = eleOrder.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
		logger.info("Processing " + orderLines.getLength() + " OrderLines");
		
		  //  Collect all UPCs
	    Set<String> upcSet = collectUPCs(orderLines);

	    // Fetch all items from OMS using a single getItemList call
	    Document itemOut = fetchItemsFromOMS(env, upcSet);

	    //  Validate OMS response (check missing or duplicate items)
	    Map<String, Element> upcToItemMap = validateItems(upcSet, itemOut);
	    

	    //  Enrich order lines with OMS item data
	    enrichOrderLines(orderLines, upcToItemMap,strEnterpriseCode);
	}

	/**
	 * 1️⃣ Collects all UPCs from OrderLines
	 */
	private Set<String> collectUPCs(NodeList orderLines) {
	    Set<String> upcSet = new LinkedHashSet<>();

	    for (int i = 0; i < orderLines.getLength(); i++) {
	        Element eleOL = (Element) orderLines.item(i);
	        Element eleItem = (Element) eleOL.getElementsByTagName(CrocsXmlConstants.E_ITEM).item(0);

	        String upc = eleItem.getAttribute(CrocsXmlConstants.A_ITEM_ID);
	        if (YFCObject.isVoid(upc)) {
	            throw new YFSException("Transfer Order Creation Failed - Item UPC missing in OrderLine.");
	        }

	        upcSet.add(upc);
	    }

	    if (upcSet.isEmpty()) {
	        throw new YFSException("Transfer Order Creation Failed - No items found for enrichment.");
	    }

	    return upcSet;
	}
	/**
	 * 2️⃣ Builds ComplexQuery input and calls OMS getItemList once
	 */
	private Document fetchItemsFromOMS(YFSEnvironment env, Set<String> upcSet) throws Exception {
		Document itemDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ITEM);
	    Element eleItem = itemDoc.getDocumentElement();
	    eleItem.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, CrocsConstant.STR_CROCS_NA);
	    eleItem.setAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE,CrocsXmlConstants.S_EACH );

	    // Create <ItemAliasList>/<ItemAlias> hierarchy
	      Element eleItemAliasList = SCXmlUtil.createChild(eleItem, CrocsXmlConstants.E_ITEM_ALIAS_LIST);
		 Element eleItemAlias = SCXmlUtil.createChild(eleItemAliasList, CrocsXmlConstants.E_ITEM_ALIAS);
	    
	    
	 // Create ComplexQuery element with Operator="AND"
		 Element  complexQuery = SCXmlUtil.createChild(eleItemAlias,CrocsXmlConstants.E_COMPLEX_QUERY);
	    complexQuery.setAttribute(CrocsXmlConstants.A_OPERATOR, CrocsXmlConstants.S_AND);
	    
	 // Create <And> block
	    Element  andElement = SCXmlUtil.createChild(complexQuery,CrocsXmlConstants.E_AND);
	    
	 // Add <Exp Name="AliasName" Value="UPC" QryType="LIKE"/>
	    Element expAliasName = SCXmlUtil.createChild(andElement,CrocsXmlConstants.E_EXP);
	    expAliasName.setAttribute(CrocsXmlConstants.A_NAME, CrocsXmlConstants.A_ALIAS_NAME);
	    expAliasName.setAttribute(CrocsXmlConstants.S_VALUE, CrocsXmlConstants.S_UPC);
	    expAliasName.setAttribute(CrocsXmlConstants.A_QRY_TYPE, CrocsXmlConstants.S_LIKE);

	 // Create <Or> block under <And>
        Element orElement = SCXmlUtil.createChild(andElement,CrocsXmlConstants.E_OR);

	    for (String upc : upcSet) {
	        Element exp = SCXmlUtil.createChild(orElement,CrocsXmlConstants.E_EXP);
	        exp.setAttribute(CrocsXmlConstants.A_NAME, CrocsXmlConstants.A_ALIAS_VALUE);
	        exp.setAttribute(CrocsXmlConstants.S_VALUE, upc);
	        exp.setAttribute(CrocsXmlConstants.A_QRY_TYPE, CrocsXmlConstants.S_EQUAL);
	    }

	    logger.verbose("fetchAndUpdateOrderItems - getItemList Input: " +SCXmlUtil.getString(itemDoc));

		Document itemOut = CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_GET_ITEM_LIST_TO,
				CrocsAPIConstants.API_GET_ITEM_LIST, itemDoc);
		validateResponse(itemOut, CrocsAPIConstants.API_GET_ITEM_LIST);
	    return itemOut;
	}
	
	/**
	 * 3️⃣ Validates OMS response - check for missing or duplicate items
	 */
	private Map<String, Element> validateItems(Set<String> upcSet, Document itemOut) {
	    NodeList itemList = itemOut.getDocumentElement().getElementsByTagName(CrocsXmlConstants.E_ITEM);
	    if (YFCObject.isNull(itemList) || itemList.getLength() == 0) {
	        throw new YFSException("Transfer Order Creation Failed - No matching items found in OMS for provided UPCs.");
	    }

	    Map<String, Element> upcToItemMap = new HashMap<>();
	    Set<String> duplicateUPCs = new HashSet<>();

	    for (int i = 0; i < itemList.getLength(); i++) {
	        Element omsItem = (Element) itemList.item(i);
	        NodeList aliasList = omsItem.getElementsByTagName(CrocsXmlConstants.E_ITEM_ALIAS);

	        for (int j = 0; j < aliasList.getLength(); j++) {
	            Element alias = (Element) aliasList.item(j);
	            String aliasValue = alias.getAttribute(CrocsXmlConstants.A_ALIAS_VALUE);

	            if (upcSet.contains(aliasValue)) {
	                if (upcToItemMap.containsKey(aliasValue)) {
	                    duplicateUPCs.add(aliasValue);
	                } else {
	                    upcToItemMap.put(aliasValue, omsItem);
	                }
	            }
	        }
	    }

	    if (!duplicateUPCs.isEmpty()) {
	        throw new YFSException("Transfer Order Creation Failed - More than one item found in OMS for UPC(s): " + duplicateUPCs);
	    }

	    Set<String> missingUPCs = new HashSet<>(upcSet);
	    missingUPCs.removeAll(upcToItemMap.keySet());
	    if (!missingUPCs.isEmpty()) {
	        throw new YFSException("Transfer Order Creation Failed - Missing item(s) in OMS for UPC(s): " + missingUPCs);
	    }

	    return upcToItemMap;
	}

	
	/**
	 * Updates OrderLine items with OMS data
	 */
	private void enrichOrderLines(NodeList orderLines, Map<String, Element> upcToItemMap,String enterpriseCode) {
	    for (int i = 0; i < orderLines.getLength(); i++) {
	        Element eleOL = (Element) orderLines.item(i);

	        //EOMS-7881 - Create Order Updates for CA 
	        String code = enterpriseCode == null ? "" : enterpriseCode.trim();

			if (CrocsConstant.CROCS_US.equals(code)) {
				eleOL.setAttribute(CrocsXmlConstants.A_SHIP_NODE, CrocsConstant.A_OHIO_DC_VALUE);				
			} else if (CrocsConstant.CROCS_CA.equals(code)) {
				// If you truly want an empty ship node for CA
				eleOL.setAttribute(CrocsXmlConstants.A_SHIP_NODE, "");				
			}
			//EOMS - 8495 - Add FulfillmentType to Both US and CA Orders
			eleOL.setAttribute(CrocsXmlConstants.A_FULFILLMENT_TYPE, CrocsConstant.A_TO_FULFILLMENT_TYPE);
	        Element eleItemOL = (Element) eleOL.getElementsByTagName(CrocsXmlConstants.E_ITEM).item(0);
	        String upc = eleItemOL.getAttribute(CrocsXmlConstants.A_ITEM_ID);

	        Element omsItem = upcToItemMap.get(upc);
	        Element elePrimaryInfo = (Element) omsItem.getElementsByTagName(CrocsXmlConstants.E_PRIMARY_INFORMATION).item(0);

	        eleItemOL.setAttribute(CrocsXmlConstants.A_ITEM_ID, omsItem.getAttribute(CrocsXmlConstants.A_ITEM_ID));
	        eleItemOL.setAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE, omsItem.getAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE));
	        eleItemOL.setAttribute(CrocsXmlConstants.A_PRODUCT_CLASS, elePrimaryInfo.getAttribute(CrocsXmlConstants.A_DEFAULT_PRODUCT_CLASS));

	        String strExtendedDisplayDesc = elePrimaryInfo.getAttribute(CrocsXmlConstants.A_EXTENDED_DISPLAY_DESCRIPTION);
	        if (!YFCCommon.isVoid(strExtendedDisplayDesc)) {
	            eleItemOL.setAttribute(A_ITEM_DESC, strExtendedDisplayDesc);
	        }
	    }

	    logger.info("fetchAndUpdateOrderItems - Successfully enriched all order items from OMS");
	}

	/**
	 * Calls getItemList API using UPC alias to resolve actual ItemID.
	 * 
	 * @throws Exception
	 */
	
	// ================================
	// Common Utility
	// ================================

	/**
	 * Validates API response.
	 *
	 * @param doc     Response document
	 * @param apiName API name for logging
	 */
	private void validateResponse(Document doc, String apiName) {
		if (YFCObject.isNull(doc)) {
			throw new YFSException("CrocsTOCreateOrderInputUpdate validateResponse :-Null response from API: " + apiName);
		}
	}

	//START- EOMS-12493
	private void updateOrderLinePersonInfoShipTo(Document inDoc, Element eleOrder) {
		Element orderPersonInfoBillTo = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_PERSON_INFO_BILL_TO);
		if (!YFCObject.isNull(orderPersonInfoBillTo)) {
			NodeList orderLines = eleOrder.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
			for (int i = 0; i < orderLines.getLength(); i++) {
				Element orderLine = (Element) orderLines.item(i);
				Element linePersonInfoShipTo = (Element) orderPersonInfoBillTo.cloneNode(true);
				linePersonInfoShipTo = (Element) inDoc.renameNode(linePersonInfoShipTo, null, CrocsXmlConstants.E_PERSON_INFO_SHIP_TO);
				orderLine.appendChild(linePersonInfoShipTo);
			}
			logger.info("Updated PersonInfoShipTo on all OrderLines");
		}else {
			logger.info("Order level PersonInfoBillTo not found. Skipping PersonInfoShipTo update.");
		}
	}
	//END- EOMS-12493
}