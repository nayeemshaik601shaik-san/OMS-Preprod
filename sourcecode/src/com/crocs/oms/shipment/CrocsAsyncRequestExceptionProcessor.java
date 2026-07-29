package com.crocs.oms.shipment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;

public class CrocsAsyncRequestExceptionProcessor implements CrocsConstant{
    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsAsyncRequestExceptionProcessor.class);

    /**
     * Sample Input:: 
     * <?xml version="1.0" encoding="UTF-8"?>
		<AsyncronousRequest ErrorCount="1"
		                    Message="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?>&#xa;&lt;Shipment BackOrderNonShippedQuantity=&quot;&quot; CancelNonShippedQuantity=&quot;Y&quot;&#xa;    IsAsyncProcess=&quot;Y&quot; OrderHeaderKey=&quot;20251024143752436468&quot;&#xa;    OrderNo=&quot;Y100002344&quot; ReleaseNo=&quot;1&quot; ShipmentNo=&quot;&quot; WMSCode=&quot;2&quot;>&#xa;    &lt;Containers>&#xa;        &lt;Container ContainerNo=&quot;&quot; SCAC=&quot;FE20&quot; TrackingNo=&quot;1Z6F857Y100002344&quot;>&#xa;            &lt;ContainerDetails>&#xa;                &lt;ContainerDetail Quantity=&quot;2&quot;>&#xa;                    &lt;ShipmentLine Quantity=&quot;2&quot; ShipmentLineNo=&quot;2&quot;/>&#xa;                &lt;/ContainerDetail>&#xa;            &lt;/ContainerDetails>&#xa;        &lt;/Container>&#xa;    &lt;/Containers>&#xa;    &lt;ShipmentLines>&#xa;        &lt;ShipmentLine ItemID=&quot;10002-002-M18&quot; OrderNo=&quot;Y100002344&quot;&#xa;            Quantity=&quot;2&quot; ReleaseNo=&quot;1&quot; ShipmentLineNo=&quot;2&quot; UnitOfMeasure=&quot;EACH&quot;/>&#xa;    &lt;/ShipmentLines>&#xa;&lt;/Shipment>&#xa;">
			<Errors>
				<Error ErrorCode="EXTN_005"
				       ErrorDescription="Confirm Shipment update with WMSCode = 2 was not processed for this order"
				       ErrorRelatedMoreInfo="">
					<Attribute Name="ErrorCode"
					           Value="EXTN_005"/>
					<Attribute Name="ErrorDescription"
					           Value="Confirm Shipment update with WMSCode = 2 was not processed for this order"/>
				</Error>
			</Errors>
		</AsyncronousRequest>
     */
    /**
     * Processes an exception when a WMSCode = 2 is retried. If the retry count reaches the maximum error count, an alert is triggered.
     * An alert is raised by creating a new document containing details like the OrderNo and OrderHeaderKey.
     * 
     * @param env
     * @param exceptionDocument		The input XML document containing the exception details, including current error count and message.
     * @return						A document containing order details required for raising an alert:
	 *								<Order OrderHeaderKey="" OrderNo="" />
     */
    public Document prepareDocumentForRaisingAlert(Document exceptionDocument) {
        logger.info("CrocsProcessAsyncReqException: Start of method prepareDocumentForRaisingAlert: : ");
        logger.info("CrocsProcessAsyncReqException: prepareDocumentForRaisingAlert: exceptionDocument: " + SCXmlUtil.getString(exceptionDocument));

        Element exceptionEle = exceptionDocument.getDocumentElement();

		String message = exceptionEle.getAttribute(A_Message);
        logger.info("CrocsProcessAsyncReqException: prepareDocumentForRaisingAlert: Message: " + message);

		// convert string msg to doc 
		Document inDocAsReceivedFromWMS = SCXmlUtil.createFromString(message);
        logger.info("CrocsProcessAsyncReqException: prepareDocumentForRaisingAlert: inDocAsReceivedFromWMS: "+ SCXmlUtil.getString(inDocAsReceivedFromWMS));

		Element inDocEle = inDocAsReceivedFromWMS.getDocumentElement();

		// prepare document for alert with OrderNo & OrderHeaderKey
		Document orderDoc = SCXmlUtil.createDocument(E_ORDER);
		Element orderEle = orderDoc.getDocumentElement();

		orderEle.setAttribute(A_ORDER_NO, inDocEle.getAttribute(A_ORDER_NO));
		orderEle.setAttribute(A_ORDER_HEADER_KEY, inDocEle.getAttribute(A_ORDER_HEADER_KEY));

		logger.info("CrocsProcessAsyncReqException: End of method prepareDocumentForRaisingAlert with orderDoc: " + SCXmlUtil.getString(orderDoc));

		return orderDoc;
    }
}