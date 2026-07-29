package com.crocs.oms.order.receipt;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * 
 * @author IBM
 * This class covers logic to update
 * receipt's quantity in OMS once received
 * from WMS and close the receipt in OMS
 *
 */
public class CrocsReceiveReturnReceiptFromWMS implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsReceiveReturnReceiptFromWMS.class);




	public void receiveReturnReceiptInOMS(YFSEnvironment env, Document indoc){

		/**
		 * Sample input XML coming from WMS via OIC
		 * 
		 * 
		 * <Receipt  ReceivingNode="1032" >
    		<Shipment  OrderNo="Y100002100" ReceivingNode="1032"/>
        	<ReceiptLines>
            <ReceiptLine OrderNo="Y100002100" PrimeLineNo="1" Quantity="1" ItemID="40003-001-M22"/>
        	</ReceiptLines>
			</Receipt>
		 * 
		 */

		/**
		 * 
		 * Expected indoc in OMS
		 * 
		 * 
		 * <Receipt DocumentType="0003" ReceivingNode="1032" >
    		<Shipment DocumentType="0003" EnterpriseCode="CROCS_CA" OrderNo="Y100002100" 
    			ReceivingNode="1032"/>
        		<ReceiptLines>
            		<ReceiptLine OrderNo="Y100002100" PrimeLineNo="1" SubLineNo="1" Quantity="1"
            		 ItemID="40003-001-M22"/>
        		</ReceiptLines>
			</Receipt>
		 * 
		 */

		Document receiveOrderOutDoc = null;
		/**
		 * EOMS-4393 changes
		 * preparing receiveOrder API Input and 
		 * checking if there are receipt lines to process which has quantity greater then zero
		 */

		if(prepareInputForReceiveOrderAPI(env, indoc)){

			try {
				logger.verbose("calling receiveOrder API with input: "+SCXmlUtil.getString(indoc));
				receiveOrderOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_RECEIVE_ORDER_FOR_RETURN, API_RECEIVE_ORDER, indoc);

				//calling closeReceipt API
				closeReceiptInOMS(env, receiveOrderOutDoc);


			} catch (Exception e) {
				logger.verbose("CrocsReceiveReturnReceiptFromWMS.receiveReturnReceiptInOMS: Exception while calling receiveOrderAPI "+ e.getMessage());
				logger.info("CrocsReceiveReturnReceiptFromWMS.receiveReturnReceiptInOMS: Exception while calling receiveOrderAPI "+e.getMessage());
				//CommonUtil.throwError(ERROR_CODE_RECEIVE_RETURN_ERROR, ERROR_DESC_FOR_RECEIVE_RETURN);
				throw new YFSException(e.getMessage());

			}
		}else{
			logger.verbose("CrocsReceiveReturnReceiptFromWMS.receiveReturnReceiptInOMS: There are no receipt lines having Quantity as non zero so skipping receive order API call");
			logger.info("CrocsReceiveReturnReceiptFromWMS.receiveReturnReceiptInOMS: There are no receipt lines having Quantity as non zero so skipping receive order API call");
		}

	}

	/**
	 * 
	 * @param env
	 * @param indoc
	 * @return
	 */
	private boolean prepareInputForReceiveOrderAPI(YFSEnvironment env, Document indoc){

		logger.verbose("Starts of method prepareInputForReceiveOrderAPI with input: "
				+SCXmlUtil.getString(indoc));

		//to skip API calls if there are no lines
		boolean hasValidLine = false;
		String receivingNode = "";
		String enterpriseCode = "";
		Element receiptEle = indoc.getDocumentElement();
		receiptEle.setAttribute(A_DOCUMENT_TYPE, A_RETURN_ORDER_DOCUMENT_TYPE);
		receivingNode = receiptEle.getAttribute(A_RECEIVING_NODE);

		Element shipmentEle = SCXmlUtil.getChildElement(receiptEle, E_SHIPMENT);
		shipmentEle.setAttribute(A_DOCUMENT_TYPE, A_RETURN_ORDER_DOCUMENT_TYPE);
		enterpriseCode = shipmentEle.getAttribute(A_ENTERPRISE_CODE);

		//Safety check as for APTOS returns while preparing message we will stamp EnterpriseCode
		if (YFCCommon.isVoid(enterpriseCode) && !YFCCommon.isVoid(receivingNode)) {
			//setting enterprise code
		    String enterpriseCodeToUpdate = null;

		    switch (receivingNode) {
		        case A_OHIO_DC_VALUE:
		            enterpriseCodeToUpdate = CROCS_US;
		            break;
		        case A_UPS_SCS_VALUE:
		            enterpriseCodeToUpdate = CROCS_CA;
		            break;
		        case A_RADIAL_SHIPNODE:
		            enterpriseCodeToUpdate = HEYDUDE_US;
		            break;
		        default:
		        	enterpriseCodeToUpdate = "";
		            break;
		    }
		    shipmentEle.setAttribute(A_ENTERPRISE_CODE, enterpriseCodeToUpdate);
		}
		
		

		/**
		 * EOMS-4393 changes
		 * Filter out Receipt Lines with Quantity as zero 
		 * and set flag to skip api call processing if no lines
		 */
		Element receiptLines = SCXmlUtil.getChildElement(receiptEle, E_RECEIPT_LINES);
		ArrayList<Element> receiptLineList = SCXmlUtil.getChildren(receiptLines, E_RECEIPT_LINE);
		for(Element receiptLine : receiptLineList){
			logger.verbose("Iterating Receipt Line : "+SCXmlUtil.getString(receiptLine));
			String quantity = receiptLine.getAttribute(A_QUANTITY);
			if(quantity.equalsIgnoreCase(VAL_ZERO)){
				logger.verbose("Receipt Line Quantity is zero, hence removing this line");
				SCXmlUtil.removeNode(receiptLine);	
			}else{
				//if there is atleast one receipt line, below flag is set as true
				hasValidLine = true;
			}

		}

		if(!hasValidLine) {
			
			logger.info("CrocsReceiveReturnReceiptFromWMS.prepareInputForReceiveOrderAPI: Value of hasValidLine flag is : "+hasValidLine);
			logger.info("CrocsReceiveReturnReceiptFromWMS.prepareInputForReceiveOrderAPI: Ends of method prepareInputForReceiveOrderAPI with updated input: "
					+SCXmlUtil.getString(indoc));
		}
		logger.verbose("CrocsReceiveReturnReceiptFromWMS.prepareInputForReceiveOrderAPI: Ends of method prepareInputForReceiveOrderAPI with updated input: "
				+SCXmlUtil.getString(indoc));
		logger.verbose("CrocsReceiveReturnReceiptFromWMS.prepareInputForReceiveOrderAPI: Value of hasValidLine flag is : "+hasValidLine);

		return hasValidLine;

	}

	/**
	 * 
	 * @param env
	 * @param receiveOrderOutDoc
	 */
	private void closeReceiptInOMS(YFSEnvironment env, Document receiveOrderOutDoc){

		logger.verbose("CrocsReceiveReturnReceiptFromWMS.closeReceiptInOMS: Starts of method closeReceiptInOMS with input: "
				+SCXmlUtil.getString(receiveOrderOutDoc));

		Element receiceOrderOutEle = receiveOrderOutDoc.getDocumentElement();
		String receiptHeaderKey = receiceOrderOutEle.getAttribute(A_RECEIPT_HEADER_KEY);

		Document closeReceiptIndoc = SCXmlUtil.createDocument(E_RECEIPT);
		Element closeReceiptEle = closeReceiptIndoc.getDocumentElement();
		closeReceiptEle.setAttribute(A_DOCUMENT_TYPE, A_RETURN_ORDER_DOCUMENT_TYPE);
		closeReceiptEle.setAttribute(A_RECEIPT_HEADER_KEY, receiptHeaderKey);

		try {
			Document closeReceiptOutDoc = CommonUtil.invokeAPI(env, "", API_CLOSE_RECEIPT, 
					closeReceiptIndoc);

			logger.verbose("CrocsReceiveReturnReceiptFromWMS.closeReceiptInOMS: Close receipt API call is successfully completed " +SCXmlUtil.getString(closeReceiptOutDoc));
		} catch (Exception e) {
			logger.verbose("CrocsReceiveReturnReceiptFromWMS.closeReceiptInOMS: Error in catch block of close receipt API: "+e.getLocalizedMessage());
			logger.info("CrocsReceiveReturnReceiptFromWMS.closeReceiptInOMS: Exception while calling CloseReceiptApi "+e.getMessage());
			throw new YFSException(e.getMessage());

		}

	}


}