package com.crocs.oms.order;

import com.crocs.oms.common.util.CrocsConstant;
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

public class CrocsGetPageOrderHistory implements CrocsConstant {
    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetPageOrderHistory.class);
   
    public static Document processMessage (YFSEnvironment env, Document orderQueryRequestXML) throws FactoryConfigurationError, Exception {
        Document orderQueryResponseXML = null;
        
        CommonUtil.debug(logger, "Input document:", XMLUtil.getXMLString(orderQueryRequestXML));

        Element rootElement = orderQueryRequestXML.getDocumentElement();
        Element pageElement = SCXmlUtil.getChildElement(rootElement,"Page");
        Element eleOrder = SCXmlUtil.getXpathElement(pageElement,"//API/Input/Order");
        
        String strCustomerEmailID=null;
        
        CommonUtil.debug(logger, "Input document for Order element:", XMLUtil.getXMLString(orderQueryRequestXML));
        
        strCustomerEmailID= eleOrder.getAttribute(A_CUSTOMER_EMAIL_ID);
        
        	DocumentBuilder dbdr = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document inDocGetOrderList = dbdr.newDocument();
            Node orderNode = inDocGetOrderList.importNode(pageElement, true);
            inDocGetOrderList.appendChild(orderNode);
            orderQueryResponseXML = getPage(env, inDocGetOrderList);
        
            
        return orderQueryResponseXML;
    }

    public static Document getPage (YFSEnvironment env, Document orderQueryRequest) throws Exception {
        Document getOrderListOutput = null;
        
        getOrderListOutput = CommonUtil.invokeAPI(env, "", "getPage", orderQueryRequest);
        
        return getOrderListOutput;
    }
}

