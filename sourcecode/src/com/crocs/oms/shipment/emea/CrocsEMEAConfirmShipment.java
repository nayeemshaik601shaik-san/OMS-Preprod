package com.crocs.oms.shipment.emea;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-10448 & EOMS-10915 
 * CrocsEMEAConfirmShipment is responsible for processing shipment confirmations
 * received from WMS for EMEA.
 *
 * It handles multiple WMS codes and supports:
 * <ul>
 * <li>Shipment processing based on WMS code rules (1, 2, 3, 5)</li>
 * <li>Brand-based shipment splitting logic</li>
 * <li>Short shipment handling</li>
 * <li>Container and tracking information updates</li>
 * <li>Order line tracking updates post shipment confirmation</li>
 * </ul>
 *
 * This class extends CrocsEMEAShipmentUtil to reuse shared shipment utilities
 * such as API invocation, XML manipulation, and shipment transformation logic.
 */
public class CrocsEMEAConfirmShipment extends CrocsEMEAShipmentUtil implements CrocsConstant {
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsEMEAConfirmShipment.class);

	/**
	 * Entry point for shipment confirmation processing.
	 *
	 * @param env                  YFSEnvironment object
	 * @param confirmShipmentInDoc Incoming shipment confirmation update
	 * @param docOrderReleaseList  Order release details
	 * @return Processed shipment output document
	 */
	public Document processShipmentUpdate(YFSEnvironment env, Document confirmShipmentInDoc, Document docOrderReleaseList) {
		logger.verbose("CrocsEMEAConfirmShipment: Start of method processShipmentUpdate with confirmShipmentInDoc " + SCXmlUtil.getString(confirmShipmentInDoc));
		logger.verbose("CrocsEMEAConfirmShipment: Start of method processShipmentUpdate with docOrderReleaseList " + SCXmlUtil.getString(docOrderReleaseList));

		Document confirmShipmentOutDoc = processShipmentBasedOnWMSCode(env, confirmShipmentInDoc, docOrderReleaseList);

		logger.info("CrocsEMEAConfirmShipmentRefactored: End of processShipmentUpdate: with output confirmShipmentOutDoc: " + SCXmlUtil.getString(confirmShipmentOutDoc));
		return confirmShipmentOutDoc;
	}

	/**
	 * Routes shipment processing logic based on WMS code received in the
	 * confirmation input.
	 *
	 * @param env                  YFSEnvironment object
	 * @param confirmShipmentInDoc input shipment confirmation document
	 * @param docOrderReleaseList  order release details document
	 * @return processed shipment output document
	 * @throws YFSException if WMS code is invalid or processing fails
	 */
	private Document processShipmentBasedOnWMSCode(YFSEnvironment env, Document confirmShipmentInDoc, Document docOrderReleaseList) {
		logger.verbose("CrocsEMEAConfirmShipment: Start of method processShipmentUpdate with confirmShipmentInDoc " + SCXmlUtil.getString(confirmShipmentInDoc));
		logger.verbose("CrocsEMEAConfirmShipment: Start of method processShipmentUpdate with docOrderReleaseList " + SCXmlUtil.getString(docOrderReleaseList));

		try {
			// Validate mandatory attributes
			validateRequiredAttributes(confirmShipmentInDoc);

			Element confirmShipmentEle = confirmShipmentInDoc.getDocumentElement();
			String strWMSCode = confirmShipmentEle.getAttribute(CrocsXmlConstants.A_WMS_CODE);

			Document confirmShipmentOutDoc = null;

			// Route processing based on WMS Code
			switch (strWMSCode) {
			case CrocsConstant.VAL_ONE:
				logger.verbose("Processing flow selected: WMSCode = 1");
				confirmShipmentOutDoc = processShipmentForWMSCode1(env, confirmShipmentInDoc, docOrderReleaseList);
				break;

			case CrocsConstant.VAL_TWO, CrocsConstant.VAL_THREE, CrocsConstant.VAL_FIVE:
				logger.verbose("Processing flow selected: WMSCode = " + strWMSCode);
				confirmShipmentOutDoc = processShipmentForWMSCode235(env, confirmShipmentInDoc, docOrderReleaseList);
				break;

			default:
				logger.error("Invalid WMS Code received: " + strWMSCode);
				throw new YFSException("Invalid WMSCode Value", "YSC84_0006", "Received WMSCode '" + strWMSCode + "', Valid values are: [1,2,3,5]");
			}

			return confirmShipmentOutDoc;
		} catch (Exception e) {
			logger.error("Exception in processShipmentBasedOnWMSCode: " + e.getMessage(), e);
			throw new YFSException("Error in method CrocsEMEAConfirmShipment.processShipmentBasedOnWMSCode ", "", e.getMessage());
		}
	}

	/**
	 * Processes shipment confirmation for WMS Code 1 flow.
	 *
	 * <p>
	 * Flow Overview:
	 * <ul>
	 *     <li>Reads shipment confirmation input</li>
	 *     <li>Fetches shipment details using order/release information</li>
	 *     <li>Groups shipment lines by brand</li>
	 *     <li>Splits shipment brand-wise</li>
	 *     <li>Confirms each split shipment</li>
	 *     <li>Updates tracking details at order line level</li>
	 * </ul>
	 *
	 * @param env YFS Environment object
	 * @param docConfirmShipment Input shipment confirmation document
	 * @param docOrderReleaseList Order release list document
	 * @return Document containing list of confirmed shipments
	 */
	private Document processShipmentForWMSCode1(YFSEnvironment env, Document docConfirmShipment, Document docOrderReleaseList) {
		logger.verbose("CrocsEMEAConfirmShipment: Start of method processShipmentBasedOnWMSCode");
		logger.verbose("Input confirmShipmentInDoc: " + SCXmlUtil.getString(docConfirmShipment));
		logger.verbose("docOrderReleaseList: " + SCXmlUtil.getString(docOrderReleaseList));

		try {
			Element eleConfirmShipment = docConfirmShipment.getDocumentElement();

			String strOrderNo = eleConfirmShipment.getAttribute(CrocsXmlConstants.A_ORDER_NO);
			String strReleaseNo = eleConfirmShipment.getAttribute(CrocsXmlConstants.A_RELEASE_NO);

			// Get Order Release details
			Element eleOrderReleaseList = docOrderReleaseList.getDocumentElement();
			Element eleOrderRelease = SCXmlUtil.getChildElement(eleOrderReleaseList, CrocsXmlConstants.E_ORDER_RELEASE);

			String strEnterpriseCode = eleOrderRelease.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);

			String strOrderHeaderKey = eleOrderRelease.getAttribute(A_ORDER_HEADER_KEY);
			eleConfirmShipment.setAttribute(A_ORDER_HEADER_KEY, strOrderHeaderKey);

			// fetch shipment details
			Document docGetShipmentListForOrder = invokeGetShipmentListForOrder(env, strOrderNo, strReleaseNo, strEnterpriseCode);

			// update shipment attributes in the input document - ShipmentNo, ShipNode, etc
			updateShipmentAttributes(docConfirmShipment, docGetShipmentListForOrder);

			// prepare a map to have shipment lines grouped by Brand
			Map<String, List<Element>> brandToLinesMap = groupShipmentLinesByBrand(env, docConfirmShipment);

			// Output document containing all confirmed shipments
			Document listOfShipment = SCXmlUtil.createDocument(E_SHIPMENT_LIST);
			Element eleListOfShipment = listOfShipment.getDocumentElement();

			// Map used for tracking shipment line to tracking number
			Map<String, Element> shipmentLineToTrackingNoMap = new HashMap<>();

			for (Map.Entry<String, List<Element>> entry : brandToLinesMap.entrySet()) {
				logger.verbose("CrocsEMEAConfirmShipment: processShipmentForWMSCode1: Currently processing for brand: " + entry.getKey());

				List<Element> brandLines = entry.getValue();

				Element inDocCopyEle = SCXmlUtil.getCopy(eleConfirmShipment);
				Document inDocCopyDoc = inDocCopyEle.getOwnerDocument();

	            // Update container details for current brand
				updateContainerDetailsAsPerBrand(inDocCopyDoc, brandLines);

	            // Split shipment
				Document splitShipmentOutDoc = splitShipment(env, inDocCopyDoc);

				String newShipmentNo = SCXmlUtil.getXpathAttribute(splitShipmentOutDoc.getDocumentElement(), XPATH_TARGET_SHIPMENT_NO);
				inDocCopyEle.setAttribute(A_SHIPMENT_NO, newShipmentNo);

				// PrimeLineNo -> ShipmentLine mapping 
				Map<String, String> primeLineToShipmentLineMap = buildPrimeLineToShipmentLineMap(splitShipmentOutDoc);

				// Updates SCAC and tracking details at the container level
				updateContainerDetailsForFlagN(env, inDocCopyDoc, primeLineToShipmentLineMap, shipmentLineToTrackingNoMap, docOrderReleaseList);

				logger.info("CrocsEMEAConfirmShipment: processShipmentForWMSCode1: Input to confirm shipment api: " + SCXmlUtil.getString(inDocCopyDoc));
				Document confirmShipmentOutDoc = invokeConfirmShipmentAPI(env, inDocCopyDoc);

	            // Add confirmed shipment response to final output document
				SCXmlUtil.importElement(eleListOfShipment, confirmShipmentOutDoc.getDocumentElement());
			}

			// Update tracking details at Order Line level
			updateTrackingDetailsAtOrderLine(env, docOrderReleaseList, shipmentLineToTrackingNoMap);

			return listOfShipment;
			
		} catch (YFSException e) {
	        logger.verbose("Exception occurred in processShipmentForWMSCode1 " + e.getMessage());
			throw new YFSException(e.getMessage());
		}

	}

	/**
	 * Processes shipment confirmation flow for WMS Codes 2, 3 and 5.
	 *
	 * <p>
	 * Processing Flow:
	 * <ul>
	 *     <li>Reads shipment and order release details</li>
	 *     <li>Fetches shipment list for the order</li>
	 *     <li>Handles WMS Code 2 specific validations</li>
	 *     <li>Updates short shipped lines</li>
	 *     <li>Groups shipment lines by brand</li>
	 *     <li>Performs shipment split for each brand</li>
	 *     <li>Confirms shipment for each processed split shipment</li>
	 *     <li>Updates tracking information at order line level</li>
	 * </ul>
	 *
	 * @param env YFS Environment object
	 * @param docConfirmShipment Input ConfirmShipment document
	 * @param docOrderReleaseList OrderReleaseList document
	 * @return List of confirmed shipments
	 */
	private Document processShipmentForWMSCode235(YFSEnvironment env, Document docConfirmShipment, Document docOrderReleaseList) {
		logger.verbose("CrocsEMEAConfirmShipment: Start of method processShipmentForWMSCode235");
		logger.verbose("Input confirmShipmentInDoc: " + SCXmlUtil.getString(docConfirmShipment));
		logger.verbose("Input docOrderReleaseList: " + SCXmlUtil.getString(docOrderReleaseList));
		
		try {
			Element eleConfirmShipment = docConfirmShipment.getDocumentElement();
			String strWMSCode = eleConfirmShipment.getAttribute(CrocsXmlConstants.A_WMS_CODE);

			// Get Order Release details
			Element eleOrderReleaseList = docOrderReleaseList.getDocumentElement();
			Element eleOrderRelease = SCXmlUtil.getChildElement(eleOrderReleaseList, CrocsXmlConstants.E_ORDER_RELEASE);

			String strOrderHeaderKey = eleOrderRelease.getAttribute(A_ORDER_HEADER_KEY);
			eleConfirmShipment.setAttribute(A_ORDER_HEADER_KEY, strOrderHeaderKey);

			// PrimeLineNo -> OrderLine mapping
			Map<String, Element> primeNoToLineMap = buildPrimeLineToOrderLineMap(docOrderReleaseList);

			// Resolve shipment list for current order - whether to create shipment or to get the details of existing shipment
			Document docGetShipmentListForOrder = resolveShipmentList(env, docOrderReleaseList, docConfirmShipment);

			// Check if update with WMSCode = 2 can be processed or not
			if (VAL_TWO.equals(strWMSCode) && validateAndHandleWMSCode2Processing(env, eleConfirmShipment, docGetShipmentListForOrder)) {
				return docConfirmShipment;
			}

			// Update input document with short shipped lines for the lines which are not present in the shipment update
			updateInDocToHaveShortShipLines(docConfirmShipment, primeNoToLineMap);

			// Update shipment attributes like ShipmentNo, ShipNode etc.
			updateShipmentAttributes(docConfirmShipment, docGetShipmentListForOrder);

			// prepare a map to have shipment lines grouped by Brand
			Map<String, List<Element>> brandToLinesMap = groupShipmentLinesByBrand(env, docConfirmShipment);
			
			// Output document containing all confirmed shipments
			Document listOfShipment = SCXmlUtil.createDocument(E_SHIPMENT_LIST);
			Element eleListOfShipment = listOfShipment.getDocumentElement();
	
			// Map used for tracking shipment line to tracking number
			Map<String, Element> shipmentLineToTrackingNoMap = new HashMap<>();
			
			int processedBrands = 0;
			int totalBrands = brandToLinesMap.size();
			
			for (Map.Entry<String, List<Element>> entry : brandToLinesMap.entrySet()) {
				logger.verbose("CrocsEMEAConfirmShipment: processShipmentForWMSCode235: Currently processing for brand: " + entry.getKey());

				processedBrands++;
				boolean isLastBrand = (processedBrands == totalBrands);

				Element inDocCopyEle = SCXmlUtil.getCopy(eleConfirmShipment);
				Document inDocCopyDoc = inDocCopyEle.getOwnerDocument();

	            // Update container details for current brand
				updateContainerDetailsAsPerBrand(inDocCopyDoc, entry.getValue());

				if (!isLastBrand) {
					// For non-last brands - Split shipment and confirm separately
					
		            // Split shipment
					Document splitShipmentOutDoc = splitShipment(env, inDocCopyDoc);
					
					String newShipmentNo = SCXmlUtil.getXpathAttribute(splitShipmentOutDoc.getDocumentElement(), XPATH_TARGET_SHIPMENT_NO);
					inDocCopyEle.setAttribute(A_SHIPMENT_NO, newShipmentNo);

					// PrimeLineNo -> ShipmentLine mapping 
					Map<String, String> primeLineToShipmentLineMap = buildPrimeLineToShipmentLineMap(splitShipmentOutDoc);

					// Updates SCAC and tracking details at the container level for Flag N
					updateContainerDetailsForFlagN(env, inDocCopyDoc, primeLineToShipmentLineMap, shipmentLineToTrackingNoMap, docOrderReleaseList);
				} else
					// Updates SCAC and tracking details at the container level for Flag Y
					updateContainerDetailsForFlagY(env, inDocCopyDoc, shipmentLineToTrackingNoMap, docOrderReleaseList);

				// Update short shipped lines before confirmation 
				updateShortShipLines(inDocCopyDoc);

				logger.info("CrocsEMEAConfirmShipment: processShipmentForWMSCode235: Input to confirm shipment api: " + SCXmlUtil.getString(inDocCopyDoc));
				Document confirmShipmentOutDoc = invokeConfirmShipmentAPI(env, inDocCopyDoc);

	            // Add confirmed shipment response to final output document
				SCXmlUtil.importElement(eleListOfShipment, confirmShipmentOutDoc.getDocumentElement());
			}

			// Update tracking details at Order Line level
			updateTrackingDetailsAtOrderLine(env, docOrderReleaseList, shipmentLineToTrackingNoMap);
			
			return listOfShipment;

		} catch (YFSException e) {
	        logger.verbose("Exception occurred in processShipmentForWMSCode235 " + e.getMessage());
			throw new YFSException(e.getMessage());
		}
	}
}
