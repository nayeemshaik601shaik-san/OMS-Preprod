package com.crocs.oms.util.ue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.pca.ycd.japi.ue.YCDGetTrackingNumberURLUE;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;

public class CrocsGetTrackingNumberURLUEImpl implements YCDGetTrackingNumberURLUE, CrocsConstant {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetTrackingNumberURLUEImpl.class);
	/*
	 * EOMS-2301 
	 * Will get tracking url from getShipmentContainerList API
	 * 
	 * Will get Extn Tracking URl for above tracking no and use that for formation of tracking URL
	 */

	@Override
	public Document getTrackingNumberURL(YFSEnvironment arg0, Document inDoc) throws YFSUserExitException {

		logger.verbose("Input to CrocsGetTrackingNumberURLUE UE: " + SCXmlUtil.getString(inDoc));

		NodeList trackingNumbersNodeList = inDoc.getDocumentElement().getElementsByTagName(E_TRACKING_NUMBER);
		if (trackingNumbersNodeList.getLength() != 0) {
			Document urlOutDoc = SCXmlUtil.createDocument(E_TRACKING_NUMBERS);

			for (int i = 0; i < trackingNumbersNodeList.getLength(); i++) {
				Element inputTrackingNoEle = (Element) trackingNumbersNodeList.item(i);
				String trackingNo = inputTrackingNoEle.getAttribute(A_TRACKING_NO);
				try {
					if (!YFCCommon.isVoid(trackingNo)) {
						logger.verbose("trackingNo " + trackingNo);
						Document getShipmentContainerListInput = SCXmlUtil.createDocument(E_CONTAINER);
						getShipmentContainerListInput.getDocumentElement().setAttribute(A_TRACKING_NO, trackingNo);

						Document getShipmentContainerListOutput = CommonUtil.invokeAPI(arg0,
								TEMPLATE_GET_SHIPMENT_CONTAINER_LIST, API_GET_SHIPMENT_CONTAINER_LIST,
								getShipmentContainerListInput);

						String trackingUrl = SCXmlUtil.getXpathAttribute(
								getShipmentContainerListOutput.getDocumentElement(), XPAH_EXTN_TRACKING_URL);

						Element trackingNumbersEle = urlOutDoc.getDocumentElement();
						Element trackingNumberEle = SCXmlUtil.createChild(trackingNumbersEle, E_TRACKING_NUMBER);
						trackingNumberEle.setAttribute(A_REQUEST_NO, inputTrackingNoEle.getAttribute(A_REQUEST_NO));
						trackingNumberEle.setAttribute(URL, trackingUrl);
						logger.verbose("final output from UE is " + SCXmlUtil.getString(urlOutDoc));

					}
				} catch (Exception e) {
					throw new YFSException(
							"CrocsGetTrackingNumberURLUEImpl.getTrackingNumberURL :Expection"
									+ e.getMessage());
				}
			}
			return urlOutDoc;

		}
		return inDoc;
	}

}
