package com.crocs.oms.order;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.common.util.CommonUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;


/*This class reads the input from SFCC 
 * expected input order no, document type and enterprise code
 * customer email id for guest user and customer number for registered user
 * guest customer orders {
   "Order": {
      "Extn": {
         "ExtnCustId": ""
      },
      "OrderNo": "FraudOrder0094",
      "DocumentType": "0001",
      "EnterpriseCode": "CROCS_CA",
     "CustomerEMailID": "PETER.HOOK@CR.COM"
   }
}
 * Registered customer orders {
   "Order": {
      "Extn": {
         "ExtnCustId": "123"
      },
      "OrderNo": "FraudOrder0093",
      "DocumentType": "0001",
      "EnterpriseCode": "CROCS_CA"
   }
}
 * */
public class HeyDudeGetOrderDetailLookUp implements CrocsConstant {
    private static YFCLogCategory logger = YFCLogCategory.instance(HeyDudeGetOrderDetailLookUp.class);

    public Document processMessage(YFSEnvironment env, Document orderQueryRequestXML)
            throws FactoryConfigurationError, Exception {
        Document orderQueryResponseXML = null;
        String customerNo=null;
        try {
            logger.verbose("HeyDudeGetOrderDetailLookUp:Request XML for order query: START: " + XMLUtil.getXMLString(orderQueryRequestXML));

            Element reqRootOrder = orderQueryRequestXML.getDocumentElement();
            Element eleOrder = SCXmlUtil.getChildElement(reqRootOrder, E_ORDER);
            String orderNo = eleOrder.getAttribute(A_ORDER_NO);
            logger.verbose("orderNo: " + orderNo);
            String documentType = eleOrder.getAttribute(A_DOCUMENT_TYPE);
            logger.verbose("documentType : " + documentType);
            String enterpriseCode = eleOrder.getAttribute(A_ENTERPRISE_CODE);
            logger.verbose("enterpriseCode:" + enterpriseCode);
            String customerEmailId = eleOrder.getAttribute(A_CUSTOMER_EMAIL_ID);
            logger.verbose("customerEmailId: " + customerEmailId);

            // New attribute for registered customers
            if(eleOrder.getElementsByTagName(E_EXTN).getLength()>0) {
            	Element eleExtn = SCXmlUtil.getChildElement(eleOrder, E_EXTN);
                if(eleExtn.hasAttributes()) {
                	customerNo = eleExtn.getAttribute(A_CUSTOMER_NO);
                    logger.verbose("customerNo: " + customerNo);
                }
            }

            // Check if the customer is registered or guest
            if (YFCCommon.isVoid(customerNo)) {
                // Guest customer: use email-id for validation
                logger.verbose("Request XML for fetchGuestOrderDetails: " + XMLUtil.getXMLString(orderQueryResponseXML));
                if (YFCCommon.isVoid(orderNo) || YFCCommon.isVoid(documentType) || YFCCommon.isVoid(enterpriseCode) ||
                        YFCCommon.isVoid(customerEmailId)) {
                    Document error = SCXmlUtil.createDocument(CrocsConstant.A_ERROR);
                    error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_DESCRIPTION, CrocsConstant.V_DESCRIPTION);
                    error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_CODE, CrocsConstant.V_CODE);
                    logger.verbose("error: " + XMLUtil.getXMLString(error));
                    return error;
                } else {
                    orderQueryResponseXML = fetchGuestOrderDetails(env, orderNo, enterpriseCode, documentType, customerEmailId);
                    if(isValidResponse(orderQueryResponseXML)) {
                    	Element eleOrderList = orderQueryResponseXML.getDocumentElement();
                        Element orderLinesEle = SCXmlUtil.getXpathElement(eleOrderList, XPATH_ORDERLIST_ORDER_RETURNED_ORDER_LINES);
                    	updateIsReturnableFlag(orderQueryResponseXML);
                    	if (!YFCCommon.isVoid(orderLinesEle)) {
                    		updateReturnDetails(env, orderQueryResponseXML);
                    	}
                    }
                }
            } else {
                // Registered customer: use customerNo for validation
                logger.verbose("Request XML for fetchRegisteredOrderDetails: " + XMLUtil.getXMLString(orderQueryResponseXML));
                if (YFCCommon.isVoid(orderNo) || YFCCommon.isVoid(documentType) || YFCCommon.isVoid(enterpriseCode) ||
                        YFCCommon.isVoid(customerNo)) {
                    Document error = SCXmlUtil.createDocument(CrocsConstant.A_ERROR);
                    error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_DESCRIPTION, CrocsConstant.V_DESCRIPTION);
                    error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_CODE, CrocsConstant.V_CODE);
                    logger.verbose("error: " + XMLUtil.getXMLString(error));
                    return error;
                } else {
                    orderQueryResponseXML = fetchRegisteredOrderDetails(env, orderNo, enterpriseCode, documentType, customerNo);
                    if(isValidResponse(orderQueryResponseXML)) {
                    	Element eleOrderList = orderQueryResponseXML.getDocumentElement();
                        Element orderLinesEle = SCXmlUtil.getXpathElement(eleOrderList, XPATH_ORDERLIST_ORDER_RETURNED_ORDER_LINES);
                    	updateIsReturnableFlag(orderQueryResponseXML);
                    	if (!YFCCommon.isVoid(orderLinesEle)) {
                    		updateReturnDetails(env, orderQueryResponseXML);
                    	}
                    }
                }
            }
            logger.verbose("HeyDudeGetOrderDetailLookUp:output from orderQueryResponseXML: " + XMLUtil.getXMLString(orderQueryResponseXML));
        } catch (Exception e) {
            logger.error("Exception in processMessage method: " + e.getMessage(), e);
        }
                
        return orderQueryResponseXML;
    }

	/*Method to check fetch the guest order details based on email id and order no combination
     * if nor order found based on email id returning 0 order message*/
    public Document fetchGuestOrderDetails(YFSEnvironment env, String orderNo, String enterpriseCode, 
                                             String documentType, String customerEmailId) throws Exception {
        Document orderQueryResponseXML = null;
        try {
            logger.verbose("Inside fetchGuestOrderDetails method : " + orderNo + enterpriseCode + documentType + customerEmailId);
            Document inDocGetOrderList = SCXmlUtil.createDocument(E_ORDER);
            
            //EOMS-7577:: Changes Start
            //TODO: Add a index for below search criteria
            if(enterpriseCode.equals(HEYDUDE_CA) || enterpriseCode.equals(HEYDUDE_AU)) 
                inDocGetOrderList.getDocumentElement().setAttribute(A_CUSTOMER_PO_NO, orderNo);
            else 
                inDocGetOrderList.getDocumentElement().setAttribute(A_ORDER_NO, orderNo);
            //EOMS-7577:: Changes End
            
            inDocGetOrderList.getDocumentElement().setAttribute(A_ENTERPRISE_CODE, enterpriseCode);
            inDocGetOrderList.getDocumentElement().setAttribute(A_DOCUMENT_TYPE, documentType);
            inDocGetOrderList.getDocumentElement().setAttribute(A_CUSTOMER_EMAIL_ID, customerEmailId.toUpperCase());

            logger.verbose("Before calling getOrder list api for:inDocGetOrderList: fetchGuestOrderDetails: " + XMLUtil.getXMLString(inDocGetOrderList));
            orderQueryResponseXML = callGetOrderList(env, inDocGetOrderList);
            logger.verbose("After calling getOrder list api for fetchGuestOrderDetails: " + XMLUtil.getXMLString(orderQueryResponseXML));

            // Validate email for guest customers
            Element orderElement = SCXmlUtil.getChildElement(orderQueryResponseXML.getDocumentElement(), E_ORDER);
            String totalOrderList = orderQueryResponseXML.getDocumentElement().getAttribute("TotalOrderList");

            if (totalOrderList != null && totalOrderList.equals("0")) {
                Document errorDocument = SCXmlUtil.createDocument(CrocsConstant.A_ERROR);
                errorDocument.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_CODE, CrocsConstant.A_STATUS_ORDER_CODE);
                errorDocument.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_MESSAGE, CrocsConstant.A_STATUS_ORDER_MESSAGE);
                logger.verbose("orderQueryResponseXML Error Document:A_0_ORDER_MESSAGE:" + XMLUtil.getXMLString(errorDocument));
                return errorDocument;
            }

            String responseEmail = orderElement.getAttribute(A_CUSTOMER_EMAIL_ID);
            
            //Validate cusomer email id is matching or not. if not throw on error otherwise 
            if (!customerEmailId.equalsIgnoreCase(responseEmail)) {
                Document error = SCXmlUtil.createDocument(CrocsConstant.A_ERROR);
                error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_DESCRIPTION, CrocsConstant.VE_ERROR_DESCRIPTION);
                error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_CODE, CrocsConstant.VE_ERROR_CODE);
                logger.verbose("error: " + XMLUtil.getXMLString(error));
                return error;
            }
            logger.verbose("Final output:orderQueryResponseXML: fetchGuestOrderDetails: END:" + XMLUtil.getXMLString(orderQueryResponseXML));
        } catch (Exception e) {
            logger.error("Exception in fetchGuestOrderDetails method: " + e.getMessage(), e);
        }
        return orderQueryResponseXML;
    }

    /*Method to check fetch the registerd order details based on customer no and order no combination
     * if nor order found based on email id returning 0 order message*/
    public Document fetchRegisteredOrderDetails(YFSEnvironment env, String orderNo, String enterpriseCode, 
                                                  String documentType, String customerNo) throws Exception {
        Document orderQueryResponseXML = null;
        try {
            logger.verbose("Inside fetchRegisteredOrderDetails method : " + orderNo + enterpriseCode + documentType + customerNo);
            Document inDocGetOrderList = SCXmlUtil.createDocument(E_ORDER);
            
            //EOMS-7577:: Changes Start
            //TODO: Add a index for below search criteria
            if(enterpriseCode.equals(HEYDUDE_CA) || enterpriseCode.equals(HEYDUDE_AU)) 
                inDocGetOrderList.getDocumentElement().setAttribute(A_CUSTOMER_PO_NO, orderNo);
            else 
                inDocGetOrderList.getDocumentElement().setAttribute(A_ORDER_NO, orderNo);
            //EOMS-7577:: Changes End
            
            inDocGetOrderList.getDocumentElement().setAttribute(A_ENTERPRISE_CODE, enterpriseCode);
            inDocGetOrderList.getDocumentElement().setAttribute(A_DOCUMENT_TYPE, documentType);
            logger.verbose(":fetchRegisteredOrderDetails: inDocGetOrderList: " + XMLUtil.getXMLString(inDocGetOrderList));

            Element eExtn = inDocGetOrderList.createElement(E_EXTN);
            eExtn.setAttribute(A_CUSTOMER_NO, customerNo);
            inDocGetOrderList.getDocumentElement().appendChild(eExtn);
            logger.verbose("customerNo:" + customerNo);

            logger.verbose("Before calling getOrder list api for:inDocGetOrderList: fetchRegisteredOrderDetails: " + XMLUtil.getXMLString(inDocGetOrderList));
            orderQueryResponseXML = callGetOrderList(env, inDocGetOrderList);
            logger.verbose("After calling getOrder list api for fetchRegisteredOrderDetails: " + XMLUtil.getXMLString(orderQueryResponseXML));

            // Validate customerNo for registered customers
            Element orderElement = SCXmlUtil.getChildElement(orderQueryResponseXML.getDocumentElement(), E_ORDER);
            //String totalOrderList = orderElement.getAttribute("TotalOrderList");
            String totalOrderList = orderQueryResponseXML.getDocumentElement().getAttribute("TotalOrderList");

            if (totalOrderList != null && totalOrderList.equals("0")) {
                Document errorDocument = SCXmlUtil.createDocument(CrocsConstant.A_ERROR);
                errorDocument.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_CODE, CrocsConstant.A_STATUS_ORDER_CODE);
                errorDocument.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_MESSAGE, CrocsConstant.A_STATUS_ORDER_MESSAGE);
                logger.verbose("orderQueryResponseXML Error Document:A_0_ORDER_MESSAGE:" + XMLUtil.getXMLString(errorDocument));
                return errorDocument;
            }

            Element eleExtn = SCXmlUtil.getChildElement(orderElement, E_EXTN);
            String responseCustomerNo = eleExtn.getAttribute(A_CUSTOMER_NO);
            logger.verbose("eleExtn" + eleExtn);
            logger.verbose("responseCustomerNo" + responseCustomerNo);

            if (!customerNo.equals(responseCustomerNo)) {
                Document error = SCXmlUtil.createDocument(CrocsConstant.A_ERROR);
                error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_DESCRIPTION, CrocsConstant.V_ERROR_DESCRIPTION);
                error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_CODE, CrocsConstant.V_ERROR_CODE);
                logger.verbose(":error: fetchRegisteredOrderDetails: " + XMLUtil.getXMLString(error));
                return error;
            }
            logger.verbose("Final output for:orderQueryResponseXML: fetchRegisteredOrderDetails: " + XMLUtil.getXMLString(orderQueryResponseXML));
        } catch (Exception e) {
            logger.error("Exception in fetchRegisteredOrderDetails method: " + e.getMessage(), e);
        }
        return orderQueryResponseXML;
    }

    /* Method to call the getOrderList API using template */
    public Document callGetOrderList(YFSEnvironment env, Document orderQueryRequest) throws Exception {
        Document getOrderListOutput = null;
        try {
            logger.verbose("input to getOrderList: " + XMLUtil.getXMLString(orderQueryRequest));
            getOrderListOutput = CommonUtil.invokeService(env, SERVICE_GET_ORDER_LIST, orderQueryRequest);

            logger.verbose("output to getOrderList: END: " + XMLUtil.getXMLString(getOrderListOutput));
        } catch (Exception e) {
            logger.error("Exception in callGetOrderList method: " + e.getMessage(), e);
            throw e; // Rethrow exception to propagate to caller
        }
        return getOrderListOutput;
    }
    
    /**
     * This method calls the getOrderLineList api to get details of return order 
     * @param env
     * @param getOrderList
     * @return
     * @throws Exception
     */
    public Document callGetOrderLineList(YFSEnvironment env, Document getOrderList) throws Exception {
		logger.verbose("Start of method callGetOrderLineList with input - getOrderList: " + SCXmlUtil.getString(getOrderList));
    	Element getOrderListEle = getOrderList.getDocumentElement();
    	
    	String orderHeaderKey = SCXmlUtil.getXpathAttribute(getOrderListEle, XPATH_ORDERLIST_ORDER_ORDER_HEADER_KEY);
    	String enterpriseCode = SCXmlUtil.getXpathAttribute(getOrderListEle, XPATH_ORDERLIST_ORDER_ORDER_ENTERPRISE_CODE);
    	
    	//Creating input doc for getOrderLineList
    	Document getOrderLineListInDoc = SCXmlUtil.createDocument(E_ORDER_LINE);
    	Element getOrderLineListEle = getOrderLineListInDoc.getDocumentElement();
    	
    	getOrderLineListEle.setAttribute(A_DERIVED_FROM_ORDER_HEADER_KEY, orderHeaderKey);
    	getOrderLineListEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);// Why this is needed?
    	getOrderLineListEle.setAttribute(A_ENTERPRISE_CODE, enterpriseCode);// Why this is needed?
    	
        Document getOrderLineListOutput = null;
        try {
            logger.verbose("input to getOrderLineList: " + XMLUtil.getXMLString(getOrderLineListInDoc));
            getOrderLineListOutput = CommonUtil.invokeService(env, SERVICE_GET_ORDER_LINE_LIST, getOrderLineListInDoc);
            
            logger.verbose("output from getOrderLineList: " + XMLUtil.getXMLString(getOrderLineListOutput));
        } catch (Exception e) {
            logger.error("Exception in callGetOrderLineList method: " + e.getMessage(), e);
            throw e; // Rethrow exception to propagate to caller
        }
		logger.verbose("End of method callGetOrderLineList with input - getOrderList: " + SCXmlUtil.getString(getOrderLineListOutput));

    	return getOrderLineListOutput;
    }
    
    /**
     * This method takes the getOrderList output and updates the return order details in the getOrderList output 
     * if there is any return order associated with this order
     * @param env
     * @param getOrderList 
     * @return
     * @throws Exception
     */
    private Document updateReturnDetails(YFSEnvironment env, Document getOrderList) throws Exception {
		logger.verbose("End of method updateReturnDetails with input - getOrderList: " + SCXmlUtil.getString(getOrderList));

    	Element getOrderListEle = getOrderList.getDocumentElement();
    	// invoke getOrderLineList
    	Document getOrderLineList = callGetOrderLineList(env, getOrderList);
    	Element getOrderLineListEle = getOrderLineList.getDocumentElement();
    	
    	int totalLineList = Integer.parseInt(getOrderLineListEle.getAttribute(A_TOTAL_LINE_LIST));
    	
    	// if there is no return for the order
    	if(totalLineList == 0) {
    		logger.verbose("End of method callGetOrderLineList with no return order for this order with output - getOrderList: " + SCXmlUtil.getString(getOrderList));
    		return getOrderList;
    	} 
    	else {
	    	Element orderLinesEle = SCXmlUtil.getXpathElement(getOrderListEle, XPATH_ORDERLIST_ORDER_ORDER_ORDER_LINES);
			NodeList listOfOrderLine = orderLinesEle.getElementsByTagName(E_ORDER_LINE) ;
			
	    	for(int i=0; i<listOfOrderLine.getLength(); i++) {
	    		
	    		Element orderEle = (Element)listOfOrderLine.item(i);
	    		
	    		String salesOrderLineKey = orderEle.getAttribute(A_ORDER_LINE_KEY);
	    		
	    		if (!YFCCommon.isVoid(salesOrderLineKey) ) {
	    			
		    		NodeList listOfMatchEle = SCXmlUtil.getXpathNodes(getOrderLineListEle, "/OrderLineList/OrderLine[@DerivedFromOrderLineKey='"+salesOrderLineKey+"']");
		    		
		    		
		    		
		    		for(int index = 0; index < listOfMatchEle.getLength(); index++) {
		    			Element matchEle = (Element) listOfMatchEle.item(index);
		    			
		    			if(matchEle != null) {
		    				Element eOrderLineRerturnElement = SCXmlUtil.createChild(orderEle, E_RETURN_ORDER);
		    				SCXmlUtil.importElement(eOrderLineRerturnElement, matchEle);
		    				orderEle.appendChild(eOrderLineRerturnElement);
		    			}
		    		}
	    		}
	    	}
    	}
    	 	
		logger.verbose("End of method callGetOrderLineList return order details updated for this order with output - getOrderList: " + SCXmlUtil.getString(getOrderList));

		return getOrderList;
	}
    
    /**
     * This method takes getOrderList outDoc & 
     * if the input given to getOrderList is incorrect then this method gets an Error document
     * <Error StatusCode="NO_ORDERS_FOUND" 
     * StatusMessage="The request was processed successfully, but no orders were found matching the specified criteria."/>
     * @param getOrderListOutput
     * @return
     */
    private boolean isValidResponse(Document getOrderListOutput) {
		logger.verbose("Start of method isValidResponse with input: " + SCXmlUtil.getString(getOrderListOutput));
    	Element getOrderListOutputEle = getOrderListOutput.getDocumentElement();   	
    	// if we receive error doc then we return false
    	boolean isValidRes = !getOrderListOutputEle.getTagName().equals(E_ERROR);
		logger.verbose("End of method isValidResponse with boolean output: " + isValidRes);
		return isValidRes;    	
    }
    
    /**
     * This method takes the getOrderList output, 
     * it first checks if order is in return window (45 days) & then checks if any line has returnable quantity if yes then it updates the IsReturnable = Y
     * otherwise IsReturnable = N
     * @param orderQueryResponseXML
     */
	private void updateIsReturnableFlag(Document orderQueryResponseXML) {
		logger.verbose("Start of method updateIsReturnableFlag with input: " + SCXmlUtil.getString(orderQueryResponseXML));

		Element getOrderListEle = orderQueryResponseXML.getDocumentElement();
		Element orderEle = SCXmlUtil.getChildElement(getOrderListEle, E_ORDER);
		String orderDateStr = orderEle.getAttribute(A_ORDER_DATE);
		Element orderLinesEle = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);		
		
		ZonedDateTime orderDate = ZonedDateTime.parse(orderDateStr);
		ZonedDateTime currentSystemDate = ZonedDateTime.now();
		
		logger.verbose("Order Date: " + orderDate);
		logger.verbose("Current system Date: " + currentSystemDate);		
		
		String isReturnableFlag = FLAG_N;		
		List<Element> listOfOrderLine = SCXmlUtil.getChildrenList(orderLinesEle);
			
		for(Element eachOrderLine : listOfOrderLine) {
			
			String returnableQtyStr = eachOrderLine.getAttribute(A_RETURNABLE_QTY);
			double returnableQty = Double.parseDouble(returnableQtyStr);
			
            if (returnableQty > 0.0) {
            	String returnWindow = SCXmlUtil.getXpathAttribute(eachOrderLine, XPATH_RETURN_WINDOW_AT_ITEM_DETAILS);
            	
            	if(!returnWindow.isEmpty()) {
                 	ZonedDateTime returnWindowDate = orderDate.plusDays(Integer.parseInt(returnWindow));
            		logger.verbose("Date For Return Window at line: " + returnWindowDate);
                 	// currentSystemDate <= returnWindowDate
                 	if(!currentSystemDate.isAfter(returnWindowDate)) {
        				isReturnableFlag = FLAG_Y;
        				break;
                   	}
            	}
			}
		}
		orderEle.setAttribute(A_IS_RETURNABLE, isReturnableFlag);
		logger.verbose("End of method updateIsReturnableFlag with IsReturnable flag valus as: " + isReturnableFlag);
		logger.verbose("And orderQueryResponseXML document as: " + SCXmlUtil.getString(orderQueryResponseXML));
	}
}
