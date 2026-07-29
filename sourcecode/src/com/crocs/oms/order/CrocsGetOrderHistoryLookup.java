package com.crocs.oms.order;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.crocs.oms.common.util.CommonUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/*This class reads the input from SFCC 
* expected input date range and customer email id
* EOMS-1313 Order history lookup
* Checks null and not null check for email id and accordingly
* display the error message with 200 status
* calls get order list api
* if no order available for the customer number and it should display as 0 order message
*/
public class CrocsGetOrderHistoryLookup implements CrocsConstant {

   private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetOrderHistoryLookup.class);

    public Document processMessage(YFSEnvironment env, Document orderQueryRequestXML) throws FactoryConfigurationError, Exception {
        Document orderQueryResponseXML = null;
        String strCustomerNo = null;

        try {
            if (logger.isVerboseEnabled()) {
                logger.verbose("Request XML for order query:START" + XMLUtil.getXMLString(orderQueryRequestXML));
            }

            Element reqRootOrder = orderQueryRequestXML.getDocumentElement();
            Element eleOrder = SCXmlUtil.getChildElement(reqRootOrder, E_ORDER);
            
            String documentType = eleOrder.getAttribute(A_DOCUMENT_TYPE);
            System.out.println("documentType : " + documentType);
            
            String enterpriseCode = eleOrder.getAttribute(A_ENTERPRISE_CODE);
            System.out.println("enterpriseCode:" + enterpriseCode);
            
            // Fetching customerNo from the input, as per the updated requirement
            if(eleOrder.getElementsByTagName(E_EXTN).getLength()>0) {
            	Element eleExtn = SCXmlUtil.getChildElement(eleOrder, E_EXTN);
                if(eleExtn.hasAttributes()) {
                	strCustomerNo = eleExtn.getAttribute(A_CUSTOMER_NO);
                    System.out.println("Request XML strCustomerNo: " + strCustomerNo);
                }
            }

            eleOrder.setAttribute(A_READ_FROM_HISTORY, "B");
            eleOrder.setAttribute(A_DRAFT_ORDER_FLAG, "N");

            // Validate if customerNo is available or not
            if (YFCCommon.isVoid(strCustomerNo)) {
                Document error = SCXmlUtil.createDocument(CrocsConstant.A_ERROR);
                error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_DESCRIPTION, CrocsConstant.V_DESCRIPTION);
                error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_CODE, CrocsConstant.V_CODE);
                logger.verbose("OrderQueryResponseXML Error:" + XMLUtil.getXMLString(error));
                return error;
            } else {
            	if (YFCCommon.isVoid(documentType) || YFCCommon.isVoid(enterpriseCode)) {
            		Document error = SCXmlUtil.createDocument(CrocsConstant.A_ERROR);
                    error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_DESCRIPTION, CrocsConstant.V_DESCRIPTION);
                    error.getDocumentElement().setAttribute(CrocsConstant.A_ERROR_CODE, CrocsConstant.V_CODE);
                    System.out.println("error: " + XMLUtil.getXMLString(error));
                    return error;
            	}else {
            		DocumentBuilder dbdr = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document inDocGetOrderList = dbdr.newDocument();
                    Node nodeImp = inDocGetOrderList.importNode(eleOrder, true);
                    inDocGetOrderList.appendChild(nodeImp);
                    logger.verbose("inDocGetOrderList:" + XMLUtil.getXMLString(inDocGetOrderList));

                    // Call the getOrderList API
                    orderQueryResponseXML = callGetOrderList(env, inDocGetOrderList);

                    // If customer number is valid but no orders are found, return a message indicating no orders
                    Element eleOutput = orderQueryResponseXML.getDocumentElement();
                    String totalOrderList = eleOutput.getAttribute("TotalOrderList");
                    if (strCustomerNo != null && totalOrderList.equals("0")) {
                        Document errorDocument = SCXmlUtil.createDocument(CrocsConstant.A_ERROR);
                        errorDocument.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_CODE, CrocsConstant.A_STATUS_ORDER_CODE);
                        errorDocument.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_MESSAGE, CrocsConstant.A_STATUS_ORDER_MESSAGE);
                        logger.verbose("OrderQueryResponseXML error Document:A_0_ORDER_MESSAGE:" + XMLUtil.getXMLString(errorDocument));
                        return errorDocument;
                    }
            	}
            }
        } catch (Exception e) {
            logger.error("Error processing order query request", e);

        }

        logger.verbose("OrderQueryResponseXML for order query:END" + XMLUtil.getXMLString(orderQueryResponseXML));
        return orderQueryResponseXML;
    }

    /* Method to call the getOrderList API using template */
    public Document callGetOrderList(YFSEnvironment env, Document orderQueryRequest) throws Exception {
        Document getOrderListOutput = null;
        try {
            logger.verbose("Before calling Get Order List API: request" + XMLUtil.getXMLString(orderQueryRequest));
            getOrderListOutput = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_HISTORY, API_GET_ORDER_LIST, orderQueryRequest);
            logger.verbose("After calling Get Order List: response" + XMLUtil.getXMLString(getOrderListOutput));
        } catch (Exception e) {
            logger.error("Error calling Get Order List API", e);
        }
        return getOrderListOutput;
    }
}
