package com.crocs.oms.common.util;
import java.rmi.RemoteException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.crocs.oms.util.restapi.CrocsRestConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public final class CommonUtil implements CrocsConstant{
    private Properties properties;
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    public CommonUtil()
    {
        // prevents access default paramater-less constructor
    }

    private static YFCLogCategory logger = YFCLogCategory.instance(CommonUtil.class);


    /**
     * Instance of YIFApi used to invoke Sterling Commerce APIs or services.
     */
    private static YIFApi api;

    static {
        try {
            CommonUtil.api = YIFClientFactory.getInstance().getLocalApi();
        } catch (Exception e) {

            logger.error("Error Message is:"+ e.getMessage());
        }
    }


    /**
     * Invokes a Sterling Commerce API.
     *
     * @param env
     *            Sterling Commerce Environment Context.
     * @param templateName
     *            Name of API Output Template that needs to be set
     * @param apiName
     *            Name of API to invoke.
     * @param inDoc
     *            Input Document to be passed to the API.
     * @throws java.lang.Exception
     *             Exception thrown by the API.
     * @return Output of the API.
     */
    public static Document invokeAPI(YFSEnvironment env, String templateName,
                                     String apiName, Document docIn) throws Exception {
        env.setApiTemplate(apiName, templateName);
        Document returnDoc = CommonUtil.api.invoke(env, apiName, docIn);
        env.clearApiTemplate(apiName);
        return returnDoc;
    }

    /**
     * Invokes a Sterling Commerce Service.
     *
     * @param env
     *            Sterling Commerce Environment Context.
     * @param serviceName
     *            Name of Service to invoke.
     * @param inDoc
     *            Input Document to be passed to the Service.
     * @throws java.lang.Exception
     *             Exception thrown by the Service.
     * @return Output of the Service.
     */
    public static Document invokeService(YFSEnvironment env,
                                         String serviceName, Document docIn) throws RemoteException {
        return CommonUtil.api.executeFlow(env, serviceName, docIn);
    }

    /**
     * This method will create exception with Error Code.
     *
     * @param strErrorCode
     */
    public static void throwError(final String strErrorCode) {
        final YFSException yfsException = new YFSException();
        yfsException.setErrorCode(strErrorCode);
        throw yfsException;
    }

    /**
     * This method will create exception with Error Code with Error Message.
     *
     * @param strErrorCode
     * @param strErrorMessage
     */
    public static void throwError(final String strErrorCode, final String strErrorMessage) {
        final YFSException yfsException = new YFSException();
        yfsException.setErrorCode(strErrorCode);
        yfsException.setErrorDescription(strErrorMessage);
        throw yfsException;
    }

    /**
     * This method will create exception with Error Code and append any
     * attributes that are set in Map object.
     *
     * @param strErrorCode
     * @param mapErrorAttributes
     *            - Map Object with attributes to be set in error.
     */
    public static void throwError(final String strErrorCode, final Map<String, String> mapErrorAttributes) {
        final YFSException yfsException = new YFSException();
        yfsException.setErrorCode(strErrorCode);
        for (final Entry<String, String> mapErrorAttributesEntry : mapErrorAttributes.entrySet()) {
            yfsException.setAttribute(mapErrorAttributesEntry.getKey(), mapErrorAttributesEntry.getValue());
        }
        throw yfsException;
    }

    /**
     * This method will create exception with Error Code with Error Message and
     * append any attributes that are set in Map object.
     *
     * @param strErrorCode
     *            - Error code of an error.
     * @param strErrorMessage
     *            - Message for error.
     * @param mapErrorAttributes
     *            - Map Object with attributes to be set in error.
     */
    public static void throwError(final String strErrorCode, final String strErrorMessage,
                                  final Map<String, String> mapErrorAttributes) {
        final YFSException yfsException = new YFSException();
        yfsException.setErrorCode(strErrorCode);
        yfsException.setErrorDescription(strErrorMessage);
        for (final Entry<String, String> mapErrorAttributesEntry : mapErrorAttributes.entrySet()) {
            yfsException.setAttribute(mapErrorAttributesEntry.getKey(), mapErrorAttributesEntry.getValue());

        }
        throw yfsException;
    }

    /**
     * This method will print xml in log if debug is enabled
     *
     * @param LOGGER
     *            - YFCLogCategory object
     * @param docPrintXML
     *            - XML to be printed in log file
     */
    public static void debug(final YFCLogCategory LOGGER, final Document docPrintXML) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(SCXmlUtil.getString(docPrintXML));
        }

    }

    /**
     * This method will print xml in log if debug is enabled
     *
     * @param LOGGER
     *            - YFCLogCategory object
     * @param docPrintXML
     *            - XML element to be printed in log file
     */
    public static void debug(final YFCLogCategory LOGGER, final Element elePrintXML) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(SCXmlUtil.getString(elePrintXML));
        }

    }

    /**
     * This method will print XML along with message in log file.
     *
     * @param LOGGER
     *            - YFCLogCategory object
     * @param docPrintXML
     *            - XML element to be printed in log file
     * @param message
     *            - Message to be printed before XML
     */
    public static void debug(final YFCLogCategory LOGGER, final String message, final Document docPrintXML) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message + SCXmlUtil.getString(docPrintXML));
        }

    }

    /**
     * This method will print XML along with message in log file.
     *
     * @param LOGGER
     *            - YFCLogCategory object
     * @param elePrintXML
     *            - XML element to be printed in log file
     * @param message
     *            - Message to be printed before XML
     */
    public static void debug(final YFCLogCategory LOGGER, final String message, final Element elePrintXML) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message + SCXmlUtil.getString(elePrintXML));
        }

    }

    /**
     * This method will concatenate n strings and log it as one message.
     * @param LOGGER - YFCLogCategory object
     * @param message - String Object
     */
    public static void debug(final YFCLogCategory LOGGER, final String... message) {
        if (LOGGER.isDebugEnabled()) {
            String strCombinedMessage = "";
            for (final String strInputMsg : message) {
                strCombinedMessage += strInputMsg;
            }

            LOGGER.debug(strCombinedMessage);
        }
    }
    /**
     * This method will print XML along with message in log file.
     * @param LOGGER - YFCLogCategory object
     * @param elePrintXML - XML element to be printed in log file
     * @param message - Message to be printed before XML
     */
    public static void debug(final YFCLogCategory LOGGER,
                             final String message,final YFCElement elePrintXML) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message+elePrintXML.toString());
        }

    }

    /**
     * This method will print XML along with message in log file.
     * @param LOGGER - YFCLogCategory object
     * @param message - Message to be printed before XML
     * @param addMessage - message to be appended
     */
    public static void debug(final YFCLogCategory LOGGER,
                             final String message, final String addMessage) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message + addMessage);
        }

    }
    /**
     * This method will print XML along with message in log file.
     * @param LOGGER - YFCLogCategory object
     * @param docPrintXML - XML element to be printed in log file
     * @param message - Message to be printed before XML
     */
    public static void debug(final YFCLogCategory LOGGER,
                             final String message,final YFCDocument docPrintXML) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message+SCXmlUtil.getString(docPrintXML.getDocument()));
        }

    }
    /**
     * This method will print XML along with message in log file.
     * @param LOGGER - YFCLogCategory object
     * @param message - Message to be printed before XML
     * @param addMessage - integer value to be appended
     */
    public static void debug(final YFCLogCategory LOGGER,
                             final String message, final int addMessage) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message + addMessage);
        }
    }
    /**
     * This method will print XML along with message in log file.
     * @param LOGGER - YFCLogCategory object
     * @param message - Message to be printed before XML
     * @param addMessage - Boolean value to be appended
     */
    public static void debug(final YFCLogCategory LOGGER,
                             final String message, final boolean addMessage) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message + addMessage);
        }
    }
    /**
     * This method will print XML along with message in log file.
     * @param LOGGER - YFCLogCategory object
     * @param message - Message to be printed before XML
     * @param addMessage - Boolean value to be appended
     */
    public static void debug(final YFCLogCategory LOGGER,
                             final String message, final long addMessage) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message + addMessage);
        }
    }

    /**
     * Invokes a Sterling Commerce API.
     *
     * @param env      Sterling Commerce Environment Context.
     * @param template Output template document for the API
     * @param apiName  Name of API to invoke.
     * @param inDoc    Input Document to be passed to the API.
     * @throws java.lang.Exception Exception thrown by the API.
     * @return Output of the API.
     */
    public static Document invokeAPI(YFSEnvironment env, Document template, String apiName, Document inDoc)
            throws RemoteException {
        env.setApiTemplate(apiName, template);
        Document returnDoc = CommonUtil.api.invoke(env, apiName, inDoc);
        env.clearApiTemplate(apiName);
        return returnDoc;
    }
    
    /**
     * This method forms the input for the CustomIVInvokeRestAPI api component.
	 * Which is used to call IV api's from OMS.
	 * 
	 * docIVInputSkeleton formed as below :
	 * <InventoryVisibilityAPI Content-Type="application/json" 
	 * 		HTTPMethod="POST" URL="https://api.watsoncommerce.ibm.com/catalog/us-4*****77/v1/items">
	 * 			<Input>
	 * 				<Sample Input />
	 * 			</Input>
	 * </InventoryVisibilityAPI>
     * 
     * @param strContentType
     * @param strHttpMethod
     * @param strURL
     * @param strJsonInput
     * @return
     */
    public static Document formIVInput(String strContentType, String strHttpMethod, String strURL, String strJsonInput) {
		logger.verbose("CommonUtil : formIVInput : Start");
		String strTenantId=YFCConfigurator.getInstance().getProperty(CrocsConstant.IV_TENANT_ID);
		logger.verbose("CommonUtil : SMA property strTenantId :"+strTenantId);
		
        // Replace placeholder in URL with actual tenant ID
        strURL = strURL.replace("{tenantId}", strTenantId);
        
        // Create the root document for InventoryVisibilityAPI
        Document docOutput = SCXmlUtil.createDocument(CrocsXmlConstants.E_INV_VISIBILITY_API);
        Element eleOut = docOutput.getDocumentElement();
        eleOut.setAttribute(CrocsRestConstants.CONTENT_TYPE, strContentType);
        eleOut.setAttribute(CrocsConstant.HTTP_METHOD, strHttpMethod);
        eleOut.setAttribute(CrocsConstant.URL, strURL);

        // Append <Input> element
        Element eleInput = docOutput.createElement(CrocsXmlConstants.E_INPUT);
        
        // Set the JSON string directly as text content of the <Input> element
        eleInput.setTextContent(strJsonInput);
        eleOut.appendChild(eleInput);
        
        logger.verbose("CommonUtil : formIVInput : docOutput : End : " + SCXmlUtil.getString(docOutput));
        return docOutput;
	}
    
    /**
     * This method forms the input for the CustomIVInvokeRestAPI api component.
	 * Which is used to call IV api's from OMS.
	 * 
	 * docIVInputSkeleton formed as below:
	 * <InventoryVisibilityAPI Content-Type="application/json" 
	 * 		HTTPMethod="POST" URL="https://api.watsoncommerce.ibm.com/configuration/{tenantId}/v1/nodes/{nodeId}">
	 * 	 <Input>
	 * 	 <Sample Input />
	 * 	</Input>
	 * </InventoryVisibilityAPI>
     * 
     * @param strContentType
     * @param strHttpMethod
     * @param strURL
     * @param strJsonInput
     * @param strNodeId
     * @return
     */    
    public static Document formIVInputForNode(String strContentType, String strHttpMethod, String strURL, String strJsonInput, String strNodeId) {
  		logger.verbose("CommonUtil : formIVInput : Start");
  		String strTenantId=YFCConfigurator.getInstance().getProperty(CrocsConstant.IV_TENANT_ID);
  		logger.verbose("CommonUtil : SMA property strTenantId :"+strTenantId);
  		
          // Replace placeholder in URL with actual tenant ID
          strURL = strURL.replace("{tenantId}", strTenantId);
          
          // Replace placeholder in URL with actual nodeId
          strURL = strURL.replace("{nodeId}", strNodeId);
          
          // Create the root document for InventoryVisibilityAPI
          Document docOutput = SCXmlUtil.createDocument(CrocsXmlConstants.E_INV_VISIBILITY_API);
          Element eleOut = docOutput.getDocumentElement();
          eleOut.setAttribute(CrocsRestConstants.CONTENT_TYPE, strContentType);
          eleOut.setAttribute(CrocsConstant.HTTP_METHOD, strHttpMethod);
          eleOut.setAttribute(CrocsConstant.URL, strURL);
    
          // Append <Input> element
          Element eleInput = docOutput.createElement(CrocsXmlConstants.E_INPUT);
          
          // Set the JSON string directly as text content of the <Input> element
          eleInput.setTextContent(strJsonInput);
          eleOut.appendChild(eleInput);
          
        
          logger.verbose("CommonUtil : formIVInput : docOutput : End : " + SCXmlUtil.getString(docOutput));
          return docOutput;
  	}
    public static Document formGetIVInput(String strContentType, String strHttpMethod, String strURL,String itemId,String unitOfMeasure, String shipNode) {
        logger.verbose("CommonUtil : formIVInput : Start");
        String strTenantId=YFCConfigurator.getInstance().getProperty(CrocsConstant.IV_TENANT_ID);
        logger.verbose("CommonUtil : SMA property strTenantId :"+strTenantId);

        // Replace placeholder in URL with actual tenant ID
        strURL = strURL.replace("{tenantId}", strTenantId);

        // Replace placeholder in URL with actual itemId
        strURL = strURL.replace("{itemId}", itemId);

        // Replace placeholder in URL with actual unitOfMeasure
        strURL = strURL.replace("{unitOfMeasure}", unitOfMeasure);

        // Replace placeholder in URL with actual shipNode
        strURL = strURL.replace("{shipNode}", shipNode);

        // Create the root document for InventoryVisibilityAPI
        Document docOutput = SCXmlUtil.createDocument(CrocsXmlConstants.E_INV_VISIBILITY_API);
        Element eleOut = docOutput.getDocumentElement();
        eleOut.setAttribute(CrocsRestConstants.CONTENT_TYPE, strContentType);
        eleOut.setAttribute(CrocsConstant.HTTP_METHOD, strHttpMethod);
        eleOut.setAttribute(CrocsConstant.URL, strURL);

        logger.verbose("CommonUtil : formIVInput : docOutput : End : " + SCXmlUtil.getString(docOutput));
        return docOutput;
    }
    
    /**
	 * Prorating the Line Charges and Line Taxes for each Order line based on their Return Qty
	 * 
	 * Chareperline = $10
	 * order qty -> 3
	 * per qty charge-> ($10/3)= 3.33
	 * 1st qty refund -> 3.33
	 * 2nd qty refund -> 3.33
	 * 3rd qty refund -> 3.34
	 * 
	 * 
	 * @param intChargeAmount
	 * @param intSalesReturnablQty
	 * @param intSalesOrderedQty
	 * @param intOrderedQty
	 * @return
	 */
	public static double prorateLineChargesAndLineTaxes(boolean bLastReturnQty, double intChargeAmount,
			double intSalesOrderedQty, double intOrderedQty, double dTotalReturnedQty) {

		logger.verbose("CrocsProratingChargesUtil : prorateLineChargesAndLineTaxes: START");
		double dChargeAmount;
		try {
			if (bLastReturnQty) {
				dChargeAmount = intChargeAmount / intSalesOrderedQty;
				dChargeAmount = Math.round(dChargeAmount * 100.0) / 100.0;
				dChargeAmount = dChargeAmount * (dTotalReturnedQty - intOrderedQty);
				dChargeAmount = intChargeAmount - dChargeAmount;
			} else {
				dChargeAmount = intChargeAmount / intSalesOrderedQty;
				dChargeAmount = Math.round(dChargeAmount * 100.0) / 100.0;
				dChargeAmount = dChargeAmount * intOrderedQty;
			}
		} catch (Exception e) {
			throw new YFSException(
					"CrocsProratingChargesUtil.prorateLineChargesAndLineTaxes :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProratingChargesUtil : prorateLineChargesAndLineTaxes: END");
		return dChargeAmount;
	}

	/**
	 * Description: This below Method helps to get Common Code List 
	 * based on the CodeType and CodeValue if exist.
	 * 
	 * @param env
	 * @param strCodeType
	 * @param strCodeValue
	 * @return
	 */
	public static Document getCommonCodeList(YFSEnvironment env, String strOrganaizationCode , String strCodeType, String strCodeValue) {

		logger.verbose("CommonUtil : getCommonCodeList: START");
		
		Document getCommonCodeListsDoc = null;
		try {
			Document getCommonCodeIndoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_COMMON_CODE);
			Element getCommonCodeEle = getCommonCodeIndoc.getDocumentElement();
			
			getCommonCodeEle.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, strOrganaizationCode);

			if(!YFCCommon.isVoid(strCodeType)){
				getCommonCodeEle.setAttribute(CrocsXmlConstants.A_CODE_TYPE, strCodeType);
			}
			if (!YFCCommon.isVoid(strCodeValue)){
				getCommonCodeEle.setAttribute(CrocsXmlConstants.A_CODE_VALUE, strCodeValue);
			}

			//calling getCommonCodeList API
			getCommonCodeListsDoc = CommonUtil.invokeService(env, CrocsConstant.CROCS_GET_COMMON_CODE_LIST_SERV, getCommonCodeIndoc);

		} catch (Exception e) {

			logger.verbose("Error in getCommonCodeList API call in method getCommonCodeList: "
					+ e.getMessage());
		}

		logger.verbose("CommonUtil : getCommonCodeList: END");
		
		return getCommonCodeListsDoc;
	}
	
	/**
	 * Gives current UTC SFCC-compliant date-time format
	 * 
	 * Output format: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} in UTC (e.g., "2025-07-15T14:25:30.000Z")
	 * 
	 * @return formatted date string in SFCC UTC format
	 */
	public static String convertSysDateIntoSFCCFormat() {
		logger.verbose("CommonUtil : convertSysDateIntoSFCCFormat : Start");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String systemDate = OffsetDateTime.now(ZoneOffset.UTC).format(formatter);
		logger.verbose("CommonUtil : convertSysDateIntoSFCCFormat : End : "+systemDate);
		return systemDate;
	}
	
	/**
	 * Converts a string date input into SFCC-compliant UTC date-time format
	 * 
	 * Supported inputs : 
	 * yyyy-MM-dd (e.g., "2024-07-15")
	 * yyyy-MM-dd HH:mm:ss (e.g., "2025-07-15 14:25:30")
	 * yyyy-MM-dd'T'HH:mm:ss (e.g., "2025-07-15T14:25:30")
	 * 
	 * Output format: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} in UTC (e.g., "2025-07-15T14:25:30.000Z")
	 * 
	 * @param strInput
	 * @return formatted date string in SFCC UTC format
	 */
	public static String convertInputStringDateIntoSFCCFormat(String strInput) {
	logger.verbose("CommonUtil : convertInputStringDateIntoSFCCFormat : Start");

        LocalDateTime localDateTime;

        //EOMS-5000 ExtnShipmentDate changes start
        if (strInput.endsWith("Z")) {
            // Format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z' or yyyy-MM-dd'T'HH:mm:ss'Z'
            Instant instant = Instant.parse(strInput);
            localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            //EOMS-5000 ExtnShipmentDate changes end
        } else if (strInput.contains("T")) {
            // Format: yyyy-MM-dd'T'HH:mm:ss
        	DateTimeFormatter isoFormatter =null;
        	//EOMS-5166-START
        	if(strInput.endsWith("+0000"))
        		isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        	else
        		isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        	//EOMS-5166-END
            localDateTime = LocalDateTime.parse(strInput, isoFormatter);
        } else if (strInput.length() > 10) {
			//EOMS-5166-START        	
        	DateTimeFormatter dtFormatter =null;
        	
        	if (strInput.endsWith(".0"))
        		dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.0");
        	else 
        	// Format: yyyy-MM-dd HH:mm:ss
             dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        	//EOMS-5166-END
            localDateTime = LocalDateTime.parse(strInput, dtFormatter);
        } else {
            // Format: yyyy-MM-dd
            LocalDate localDate = LocalDate.parse(strInput, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            localDateTime = localDate.atStartOfDay(); // 00:00:00
        }

        String systemDate = localDateTime
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        logger.verbose("CommonUtil : convertInputStringDateIntoSFCCFormat : End : " + systemDate);
        return systemDate;
	}

    /**this method is used to print info statements for confirm shipment and Return receipt*/
     /**Sample input XML for ShipConfirmation
     * <ASN>
     *   <ASNID>1009702OCCASHIP1</ASNID>
     *   <BillOfLadingNumber>BL12345678</BillOfLadingNumber>
     *   <ShipVia>UE90</ShipVia>
     *   <LPN>
     *     <TrackingNbr>1Z999AA10123456784</TrackingNbr>
     *     <LPNID>LPN0001</LPNID>
     *     <LPNDetail>
     *       <ItemSequenceNbr>1</ItemSequenceNbr>
     *       <LPNDetailQuantity>
     *         <Quantity>4</Quantity>
     *       </LPNDetailQuantity>
     *     </LPNDetail>
     *     <LPNDetail>
     *       <ItemSequenceNbr>2</ItemSequenceNbr>
     *       <LPNDetailQuantity>
     *         <Quantity>4</Quantity>
     *       </LPNDetailQuantity>
     *     </LPNDetail>
     *   </LPN>
     *   <ASNDetail>
     *     <ItemName>50009-001-M24</ItemName>
     *     <ASNDetailQuantity>
     *       <ShippedQuantity>4</ShippedQuantity>
     *     </ASNDetailQuantity>
     *     <PurchaseOrderLineItemID>1</PurchaseOrderLineItemID>
     *   </ASNDetail>
     *   <ASNDetail>
     *     <ItemName>50009-001-M24</ItemName>
     *     <ASNDetailQuantity>
     *       <ShippedQuantity>4</ShippedQuantity>
     *     </ASNDetailQuantity>
     *     <PurchaseOrderLineItemID>2</PurchaseOrderLineItemID>
     *   </ASNDetail>
     * </ASN> */
    /**
     * Sample Input for returnReceipt
     * <Envelope>
     *     <Source>3PLUPSCA</Source>
     *     <SourceDescription>UPS 3PL CA – crocs</SourceDescription>
     *     <ActionType>UPDATE</ActionType>
     *     <SourceMessageID>71402022092300005</SourceMessageID>
     *     <SourceDateTime>20250730 130514</SourceDateTime>
     *   </Envelope>
     *   <GoodsReceipt>
     *     <DocumentId>71402022092300005</DocumentId>
     *     <DocumentDate>20250730</DocumentDate>
     *     <GoodsMovementCode>VL02N</GoodsMovementCode>
     *     <Plant>1032</Plant>
     *     <DeliveryDocument>7006060013</DeliveryDocument>
     *     <ReceiptLine>
     *       <ItemName>10001-2Y2-M9W11</ItemName>
     *       <Style>10001-2Y2</Style>
     *       <Color>2Y2</Color>
     *       <Size>M9W11</Size>
     *       <GridItem>Y</GridItem>
     *       <StockType>1</StockType>
     *       <PurchaseOrder>0067749622</PurchaseOrder>
     *       <PurchaseOrderLineId>900002</PurchaseOrderLineId>
     *       <Quantity>
     *         <Qty>1</Qty>
     *       </Quantity>
     *       <StorageLocation>0001</StorageLocation>
     *       <ReasonCode>900</ReasonCode>
     *     </ReceiptLine>
     *     <ReceiptLine>
     *       <ItemName>10001-2Y2-M8W10</ItemName>
     *       <Style>10001-2Y2</Style>
     *       <Color>2Y2</Color>
     *       <Size>M8W10</Size>
     *       <GridItem>Y</GridItem>
     *       <StockType>1</StockType>
     *       <PurchaseOrder>0067749622</PurchaseOrder>
     *       <PurchaseOrderLineId>900001</PurchaseOrderLineId>
     *       <Quantity>
     *         <Qty>1</Qty>
     *       </Quantity>
     *       <StorageLocation>0001</StorageLocation>
     *       <ReasonCode>900</ReasonCode>
     *     </ReceiptLine>
     *   </GoodsReceipt>
     * @param doc
     */
    public void receivedXML(Document doc) {
        String methodName = properties.getProperty(V_ARGUMENT_NAME, V_DEFAULT);
        try {
            switch (methodName) {
                case V_3PL_SHIPMENT_CONFIRMATION:
                    handleShipmentConfirmation(doc);
                    break;

                case V_3PL_RETURN_RECEIPT_MESSAGE:
                    handleReturnReceipt(doc);
                    break;

                default:
                    logger.info("xml:for"+methodName + SCXmlUtil.getString(doc));
                    logger.info("ClassName: CommonUtil | Method: receivedXML | Unexpected ArgumentName configured.\n" +
                            "Expected ArgumentName is [3PLShipmentConfirmation, 3PLReturnReceiptMessage] " +
                            "and received ArgumentName is: " + methodName);
                    break;
            }

        } catch (Exception e) {
            logger.verbose("Exception Details for receiveJson for ConfirmationShipment or ReturnReceipt : "+ e.getMessage());
        }
    }

    /**
     * Handler for 3PL Shipment Confirmation
     */
    private void handleShipmentConfirmation(Document doc) {
        String orderNo="";
        try {
            Element confirmShipmentIndoc = doc.getDocumentElement();
            Element asnID = SCXmlUtil.getChildElement(confirmShipmentIndoc, "ASNID");
            if (!YFCCommon.isVoid(asnID)) {
                String asnIDValue = asnID.getTextContent();
                int shipIndex = asnIDValue.indexOf("SHIP");
                if (shipIndex != -1) {
                    // Extract the parts using substring
                   orderNo = asnIDValue.substring(0, shipIndex);  // Everything before "SHIP"
                    logger.verbose("OrderNo: " + orderNo);
                }
            }
            logger.info("OMS_Update : CommonUtil : handleShipmentConfirmation 3PL received for orderNo " + orderNo + "\n" + "Final I/O Document for ShipmentConfirmation :" + SCXmlUtil.getString(doc));
            logger.verbose("Received xml:for handleShipmentConfirmation 3PL  "+ SCXmlUtil.getString(doc));
        }catch (YFSException e) {
            logger.info("OMS_Update : CommomUtil : handleShipmentConfirmation : in catch block  : " + orderNo +
                    "\n" + e.getMessage() +"\n" +e.getErrorDescription()+"\n"+e.getErrorCode());
            throw new YFSException(e.getMessage(),e.getErrorDescription(),e.getErrorCode());
        }

    }

    /**
     * Handler for 3PL Return Receipt Message
     */
    private void handleReturnReceipt(Document doc) {
        String orderNo="";
        try {
            Element confirmShipmentIndoc = doc.getDocumentElement();
            Element goodsReceiptEle = SCXmlUtil.getChildElement(confirmShipmentIndoc, E_GOODS_RECEIPT);
            Element firstReceiptLine = SCXmlUtil.getChildElement(goodsReceiptEle, E_RECEIPT_LINE);
            Element purchaseOrder = SCXmlUtil.getChildElement(firstReceiptLine, E_PURCHASE_ORDER);
            orderNo = purchaseOrder.getTextContent();
            logger.info("OMS_Update : CommonUtil : handleReturnReceipt : in catch block : " + orderNo + "\n" + "Final I/O Document for ReturnReceipt :" + SCXmlUtil.getString(doc));
            logger.verbose("Received xml:for handleReturnReceipt 3PL return receipt "+ SCXmlUtil.getString(doc));
        }
        catch (YFSException e) {
            logger.info("OMS_Update : CommomUtil : handleReturnReceipt : in catch block  : " + orderNo +
                    "\n" + e.getMessage() +"\n" +e.getErrorDescription()+"\n"+e.getErrorCode());
            throw new YFSException(e.getMessage(),e.getErrorDescription(),e.getErrorCode());
        }


    }
    
    /**
     * OMS-5182 :: URL Changes as per Locale. 
     * based on SCAC value and Locale , we are forming Tracking URL as per Locale.
     * 
     * @param primaryURL
     * @param customerLocale
     * @param scac
     * @param orderNo
     * @return
     */
    public static String updatePrimaryURLAsPerLocale(String primaryURL,String customerLocale, String scacValue,String orderNo) {
    	
		try {
			String lang = customerLocale.split("_")[0];

			switch (scacValue) {
			case STR_SCAC_PUROLATOR:
				primaryURL = primaryURL.replace("/en/", "/" + lang + "/");
				primaryURL = primaryURL.replace("shipping/tracker", "expedition/faire-le-suivi-dun-envoi");
				break;
			case STR_SCAC_UPSC:
				primaryURL = primaryURL.replace(STR_EN_CA, customerLocale);
				break;
			case STR_SCAC_FLEET_OPTICS:
				primaryURL = primaryURL.replace("/?", "/" + lang + "/?");
				break;
			default:
				logger.verbose("OMS_UPDATE : SCAC " + scacValue + " was not configured in OMS, hence Tracking_Url for this order :"
						+ orderNo + " would be defaulted to blank in EXTN_TRACKING_URL on yfs_order_line");
                 logger.info("OMS_UPDATE : SCAC "+ scacValue +" was not configured in OMS for this order :"+orderNo+".Formation of Tracking URL Failed");
				break;
			}

		} catch (Exception e) {
			/** 
			 * Flow is not broken intentionally, because impact would be only on preparing and persisting the Tracing URL.
			 * Rather we will capture this exception as part of logger.info in case backtrack is needed.
			 *
			 **/
			logger.info("CommonUtil : updatePrimaryURLAsPerLocale Exception while prepraing the Tracking URL locale:"
					+ customerLocale + " for order:"+orderNo+ e.getMessage());
			logger.verbose("CommonUtil : updatePrimaryURLAsPerLocale Exception while prepraing the Tracking URL locale:"
					+ customerLocale + " for order: "+orderNo + e.getMessage());
		}
		logger.info("OMS_UPDATE:order:"+orderNo+ "Tracking URL:"+primaryURL+" is formed for locale:"+customerLocale);
		return primaryURL;
    }
    /**Purpose:-
     * 	logs the given XML Document at the INFO level based on the argument name configured in properties.
     *  "Argument name" is configurable and if condition satisfied, DOC,flow and etc gets logged.
     * 	There is a generic condition available too for configuring wherever required for above purpose dynamically.
     * @param doc
     */
    public void printInfoStatement(Document doc) {
        String flowName = properties.getProperty(V_FLOW_NAME, V_DEFAULT);
        try {
            YFCDocument flowDetailsYDoc= YFCDocument.createDocument(E_FLOW_DETAILS);
            switch (flowName) {
                case V_CROCS_CA_SEND_RELMSG_TO_UPS_ON_SUCCESS_SERV:
                case V_CROCS_CA_SEND_RELMSG_ON_UPDATE_SERV:
                case V_CROCS_CA_SHIPMENT_UPDATES_CREATE_SYNC_SERV:
                case V_CROCS_CA_SHIPMENT_UPDATES_PACKED_SYNC_SERV:
                case V_CROCS_CA_PUBLISH_SHIP_DTLS_TO_NARVAR_ASYNC_SERV:
                case V_CROCS_CA_NARVAR_RETURN_LABEL_POST_TO_QUEUE_SYNC_SERV:
                case V_CROCS_CA_ORDER_UPDATE_TO_FORTER_POST_TO_QUEUE:
                case V_GENERIC_FLOW_FOR_INFO_STMT:  //Generic case to print Info Statements

                    logWithFlowDetails(doc, flowName, flowDetailsYDoc);
                    break;

                default:
                    logger.info("Logging info as Unexpected FlowName passed. document:for"+ flowName + SCXmlUtil.getString(doc));
                    logger.info("CommonUtil | Method: logInfo | Unexpected ArgumentName configured.\n" +
                            "Expected FlowName is [CrocsCASendRelMsgToUPSOnSuccessServ,CrocsCASendRelMsgOnUpdateServ,CrocsCAShipmentUpdatesPackedSyncServ,CrocsCAShipmentUpdatesCreateSyncServ\n" +
                            "CrocsCAPublishShipDtlsToNarvarAsyncServ,CrocsCANarvarLabelPostToQueueSyncServ,CrocsCAOrderUpdateToForterPostToQueue] " +
                            "and received ArgumentName is: " + flowName);
                    break;
            }

        } catch (Exception e) {
            logger.verbose("Exception Details for logInfo method in commonUtil : "+ e.getMessage());
        }
    }

    /** Retrieves flow details and populates the provided YFCDocument with those details
     * and than call logInfoMessage to print Info statement
     *
     * @param doc doc
     * @param flowName flow/service name
     * @param flowDetailsYDoc flowdetails Document
     */
    private void logWithFlowDetails(Document doc, String flowName, YFCDocument flowDetailsYDoc) {
        getFlowDetails(flowDetailsYDoc);
        logInfoMessage(doc, flowName, flowDetailsYDoc, properties.getProperty(V_SEARCH_TERM, V_DEFAULT));
    }

    /**
     * PURPOSE:- The method sets the flow details  on the document's root element
     * XML:-     <FlowDetails FlowName="CrocsCASendRelMsgOnUpdateServ" transactionId="ChangeOrder" Enterprise_Key="CROCS_CA"/>
     * @param flowDetailsYDoc
     */
    private void getFlowDetails(YFCDocument flowDetailsYDoc) {
        YFCElement flowDetailsYDocEle= flowDetailsYDoc.getDocumentElement();
        flowDetailsYDocEle.setAttribute(V_TRANSACTION_ID,properties.getProperty(V_TRANSACTION_ID, V_DEFAULT));
        flowDetailsYDocEle.setAttribute(V_FLOW_NAME,properties.getProperty(V_FLOW_NAME, V_DEFAULT));
        flowDetailsYDocEle.setAttribute(A_ENTERPRISE_KEY,properties.getProperty(A_ENTERPRISE_KEY, V_DEFAULT));
        flowDetailsYDocEle.setAttribute(V_SEARCH_TERM,properties.getProperty(V_SEARCH_TERM, V_DEFAULT));

    }
    /**
     * Logs the given XML Document at the INFO level along with the current timestamp and method name..
     *
     * @param doc XML Document to be logged
     * @param flowName method name
     */
    private void logInfoMessage(Document doc ,String flowName, YFCDocument flowDetailsYDoc,String logSearchTerm) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logger.info("[" + timestamp + "] OMS_Update :  CommonUtil :" + logSearchTerm + "\n" + "flowDetailsYDoc :" +"\n" +flowDetailsYDoc.toString()+"\n" + "Final I/O Document for Flow name : " + flowName + "\n" + SCXmlUtil.getString(doc));
    }
    
    /**
     * This method fetches mapped return ShipNode from CommonCode configuration
     * @param env
     * @param shipNode
     * @param enterpriseCode
     * @throws Exception 
     */
    public static String getShipNodeforReturn(YFSEnvironment env, String enterpriseCode) throws Exception {

    	logger.beginTimer("CrocsShipNodeUtil : getShipNodeforReturn");
    	logger.verbose("CrocsShipNodeUtil : getShipNodeforReturn Input EnterpriseCode: " + enterpriseCode);
    	logger.info("CrocsShipNodeUtil : getShipNodeforReturn : Input EnterpriseCode" + enterpriseCode);
    	String shipNode="";
    	try {
	    	Document inDoc = SCXmlUtil.createDocument(E_COMMON_CODE);
	
	        Element commonCodeEle = inDoc.getDocumentElement();
	        commonCodeEle.setAttribute(A_CODE_TYPE, VAL_HDUS_GET_ACTIVE_NODE);
	        commonCodeEle.setAttribute(A_CODE_VALUE, VAL_IS_LVDC_ACTIVE);
	        commonCodeEle.setAttribute(A_ENTERPRISE_CODE, enterpriseCode);

	        logger.info("getShipNodeforReturn : getCommonCodeList Input : " + SCXmlUtil.getString(inDoc));
	        
	        Document outDoc = CommonUtil.invokeAPI(env, "", CrocsAPIConstants.API_GET_COMMON_CODE_LIST, inDoc);
	        
	        logger.info("getShipNodeforReturn : getCommonCodeList Input : " + SCXmlUtil.getString(outDoc));	
	        String newShipNode = SCXmlUtil.getXpathAttribute(outDoc.getDocumentElement(), "//CommonCodeList/CommonCode[1]/@CodeLongDescription");
	        
	        if(!YFCCommon.isVoid(newShipNode)) {
	        	shipNode= newShipNode;
	        }
	        
    	} catch (YFSException e) {
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        }
            
        logger.info("CrocsShipNodeUtil : getShipNodeforReturn : Output ShipNode: " + shipNode);
        logger.endTimer("CrocsShipNodeUtil : getShipNodeforReturn");

        return shipNode;
    }
    
    /**
     * EOMS-10554: Checking the AddressLine1, AddressLine2 And AddressLine3
     * whether PO BOX is present or not.
     * 
     * @param env
     * @return
     * @throws Exception
     */
    public static boolean validatePOBoxAddress(Document inDoc) throws YFSException
	{
		logger.beginTimer("CommonUtil : validatePOBoxAddress: START");
		logger.verbose("CommonUtil : validatePOBoxAddress :Input Document:" + XMLUtil.getXMLString(inDoc));

		boolean isPOBox = false;
		try {

			Element orderEle = inDoc.getDocumentElement();			
			Element personInfoShipToEle = SCXmlUtil.getChildElement(orderEle, E_PERSON_INFO_SHIP_TO);
			if (!YFCCommon.isVoid(personInfoShipToEle)
					&& (!YFCCommon.isVoid(personInfoShipToEle.getAttribute(A_ADDRESS_LINE_1))
							|| !YFCCommon.isVoid(personInfoShipToEle.getAttribute(A_ADDRESS_LINE_2))
							|| !YFCCommon.isVoid(personInfoShipToEle.getAttribute(CrocsConstant.A_ADDRESS_LINE_3)))) {

				String poBoxPatterns = YFSSystem.getProperty(CrocsConstant.STR_CROCS_PO_BOXES_LIST);
				
				if (YFCCommon.isVoid(poBoxPatterns)) {
		            return false;
		        }

				String strAddressLine1 = personInfoShipToEle.getAttribute(A_ADDRESS_LINE_1);
				String strAddressLine2 = personInfoShipToEle.getAttribute(A_ADDRESS_LINE_2);
				String strAddressLine3 = personInfoShipToEle.getAttribute(CrocsConstant.A_ADDRESS_LINE_3);

				String[] patterns = poBoxPatterns.split(";");
				
				for (String pattern : patterns) {

					if (strAddressLine1.contains(pattern) || strAddressLine2.contains(pattern)
							|| strAddressLine3.contains(pattern)) {

						isPOBox = true;
						break;
					}
				}

			}

		} catch (YFSException e) {
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}
		logger.verbose("CommonUtil : validatePOBoxAddress : "+ isPOBox);
		logger.endTimer("CommonUtil : validatePOBoxAddress: END");
		return isPOBox;
	}

/**
	 * Description: This below Method helps to get changeOrder Input  
	 * based on the input values passed - EnterpriseCode, DocumentType and Order Number.
	 * 
	 * @param enterpriseCode
	 * @param documentType
     * @param orderNo

	 * @return
	 */
	public static Document getChangeOrderDocInput( String enterpriseCode, String documentType, String orderNo) {

		logger.verbose("CommonUtil : getChangeOrderDocInput : START");
		Document changeOrderInDoc = null;
		try {
			changeOrderInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
			Element orderEle = changeOrderInDoc.getDocumentElement();
			
			
            if(!YFCCommon.isVoid(enterpriseCode) && !YFCCommon.isVoid(orderNo) && !YFCCommon.isVoid(documentType) ){      
			    orderEle.setAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE, enterpriseCode);
                orderEle.setAttribute(CrocsXmlConstants.A_DOCUMENT_TYPE, documentType);
                orderEle.setAttribute(CrocsXmlConstants.A_ORDER_NO, orderNo);
                orderEle.setAttribute(A_OVERRIDE, FLAG_Y);
                orderEle.setAttribute(A_SELECT_METHOD, VAL_WAIT);
            }else {
                throw new YFSException("Invalid EnterpriseCode / Document Type / Order Number","YFS:Invalid Order","YFS:Invalid Order");
            }
			
		} catch (YFSException e) {

			logger.verbose("Error in CommonUtil in method getChangeOrderDocInput: "	+ e.getMessage());
            throw new YFSException(e.getMessage(),e.getErrorCode(),e.getErrorDescription());
		}

		logger.verbose("CommonUtil : getCommonCodeList: END");
		
		return changeOrderInDoc;
	}
    
/**
	 * Description: This below Method helps to get changeOrder Input  
	 * based on the input values passed - OrderHeaderKey
	 * 
	 * @param orderHeaderKey
	 * @return
	 */
	public static Document getChangeOrderDocInput(String orderHeaderKey) {

		logger.verbose("CommonUtil : getChangeOrderDocInput : START");
		Document changeOrderInDoc = null;
		try {
			 changeOrderInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
			Element orderEle = changeOrderInDoc.getDocumentElement();
			
			if(!YFCCommon.isVoid(orderHeaderKey)){
				orderEle.setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY, orderHeaderKey);
                orderEle.setAttribute(A_OVERRIDE, FLAG_Y);
                orderEle.setAttribute(A_SELECT_METHOD, VAL_WAIT);
            }
            else {        
                throw new YFSException("OrderHeaderKey is Null/Blank","YFS:Invalid Order","YFS:Invalid Order");
            }
            			
		} catch (YFSException e) {
			logger.verbose("Error in CommonUtil in method getChangeOrderDocInput: "	+ e.getMessage());
            throw new YFSException(e.getMessage(),e.getErrorCode(),e.getErrorDescription());
		}

		logger.verbose("CommonUtil : getChangeOrderDocInput : END");
		
		return changeOrderInDoc;
	}
}
