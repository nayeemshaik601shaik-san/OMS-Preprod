package com.crocs.oms.shipment;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CrocsCAShipmentConfirmationNotification implements CrocsConstant {
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsCAShipmentConfirmationNotification.class);

	/**
	 * This method will be invoke when shipment moves to shipped status
	 * (CONFIRM_SHIPMENT_ON_SUCCESS)
	 * 
	 * @param env
	 * @param inDoc
	 * @return
	 * @throws Exception 
	 */
	public Document confirmShipmentNotification(YFSEnvironment env, Document inDoc) throws Exception {
		logger.verbose("Start of method confirmShipmentNotification with input: " + SCXmlUtil.getString(inDoc));

		Element inDocEle = inDoc.getDocumentElement();
		Element shipmentLines = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
		Element shipmentLineEle = SCXmlUtil.getChildrenList(shipmentLines).get(0);

		String orderHeaderKey = shipmentLineEle.getAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY);

		Document getOrderListOutDoc = invokeGetOrderList(env, orderHeaderKey);
		Document outDoc = prepareDocForSFCC(env,inDoc, getOrderListOutDoc);

		logger.verbose("End of method confirmShipmentNotification with output: " + SCXmlUtil.getString(outDoc));

		return outDoc;
	}

	/**
	 * This method is used to prepare the document for SFCC which we will place in
	 * the Queue:: CROCS_OUT_SHIPMENT_CONFIRMATION_MSG_QUEUE It will receive the
	 * inDoc and output from the getOrderList api to form the output
	 * 
	 * @param inDoc
	 * @param getOrderListDoc
	 * @return
	 * @throws Exception 
	 */
	public Document prepareDocForSFCC(YFSEnvironment env, Document inDoc, Document getOrderListDoc) throws Exception {
		logger.verbose("Start of method prepareDocForSFCC with inDoc: " + SCXmlUtil.getString(inDoc));
		logger.verbose("Start of method prepareDocForSFCC with getOrderListDoc: " + SCXmlUtil.getString(getOrderListDoc));

		Element getOrderListEle = getOrderListDoc.getDocumentElement();
		String orderHeaderKey = SCXmlUtil.getXpathAttribute(getOrderListEle, XPATH_ORDERLIST_ORDER_ORDER_HEADER_KEY);
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(STR_DATE_FORMAT_MM_DD_YYYY);

		Element orderEle = SCXmlUtil.getChildElement(getOrderListEle, E_ORDER);
		String orderDate = SCXmlUtil.getAttribute(orderEle, A_ORDER_DATE);
		OffsetDateTime orderDateParsed = OffsetDateTime.parse(orderDate);

		orderEle.setAttribute(A_ORDER_DATE, orderDateParsed.format(dateFormatter));


		/** EOMS- 4368 Currency symbol to sent to OIC
		 * If Curreny is USD currenysymbol will be $
		 * If Curreny is CAD currenysymbol will be C$
		extract <PriceInfo> element and get Currency attribute**/
		String currencySymbol =VAL_US_CURRENCY_SYMBOL;
		Element priceInfo = SCXmlUtil.getChildElement(orderEle, E_PRICE_INFO);
		String currency = SCXmlUtil.getAttribute(priceInfo, A_CURRENCY);
		if (!YFCCommon.isVoid(currency)) {
			switch (currency.toUpperCase()) {
				case VAL_US_CURRENCY: // "USD"
					currencySymbol = VAL_US_CURRENCY_SYMBOL;
					break;
				case VAL_CA_CURRENCY: // "CAD"
					currencySymbol = VAL_CA_CURRENCY_SYMBOL;
					break;
				default:
					currencySymbol = VAL_US_CURRENCY_SYMBOL;
					break;
			}
		}
		//set the attribute CurrencySymbol="..." at header level
		getOrderListEle.setAttribute(A_CUSTOM_CURRENCY_SYMBOL, currencySymbol);

		/*EOMS- 7995 Update Shipping Charge and Discount*/

		updateOverAllCharges(getOrderListDoc, orderEle);

		//getUnique PromotionId
		getUniquePromotionId(getOrderListDoc);

		//itemLocaleFeed
		getLocalizedItemFeed(getOrderListDoc);

		Element orderShipmentsEle = SCXmlUtil.createChild(getOrderListEle, CrocsXmlConstants.E_SHIPMENTS);

		LocalDateTime currentDateTime = LocalDateTime.now();
		String systemDate = currentDateTime.format(dateFormatter);

		orderShipmentsEle.setAttribute(CrocsXmlConstants.A_SYSTEM_DATE, systemDate);

		updateShipmentStatus(getOrderListDoc);
		
		// invoking getShipmentListForOrder to get the details of all shipment
		Document getShipmentListForOrder = getShipmentListForOrder(env, orderHeaderKey);
		List<Element> listOfShipment = SCXmlUtil.getChildrenList(getShipmentListForOrder.getDocumentElement());
		
		// adding details of all shipment for this order
		for(Element eachShipment : listOfShipment) {
			
			Element containersEle = SCXmlUtil.getChildElement(eachShipment, E_CONTAINERS);
			
			String actualShipmentDate = eachShipment.getAttribute(A_ACTUAL_SHIPMENT_DATE);
			if(!actualShipmentDate.isEmpty()) {
				OffsetDateTime dateTime = OffsetDateTime.parse(actualShipmentDate);
				actualShipmentDate = dateTime.format(dateFormatter);
			}			
			
			List<Element> listOfContainer = SCXmlUtil.getChildrenList(containersEle);
			
			for(Element eachContainer : listOfContainer) {
				
				Element ship = SCXmlUtil.createChild(orderShipmentsEle, E_SHIPMENT);
				
				ship.setAttribute(A_ACTUAL_SHIPMENT_DATE, actualShipmentDate);
				ship.setAttribute(A_TRACKING_NO, eachContainer.getAttribute(A_TRACKING_NO));
				Element extnEle = SCXmlUtil.getChildElement(eachContainer, E_EXTN);
				String trackingURL = extnEle.getAttribute(A_EXTN_TRACKING_URL);
				String scacContainer = eachContainer.getAttribute(A_SCAC);
				
				ship.setAttribute(A_TRACKING_URL, trackingURL);
				ship.setAttribute(A_SCAC, scacContainer);
				if (scacContainer.isEmpty())
					ship.setAttribute(CrocsXmlConstants.A_IS_CARRIER_MISSING, CrocsXmlConstants.A_TRUE_STRING);
				else
					ship.setAttribute(CrocsXmlConstants.A_IS_CARRIER_MISSING, CrocsXmlConstants.A_FALSE_STRING);
			}
		}
		
		if(SCXmlUtil.getChildrenList(orderShipmentsEle).size() > 1)
			orderShipmentsEle.setAttribute(CrocsXmlConstants.A_SPLIT_SHIPMENT, CrocsXmlConstants.A_TRUE_STRING);
		else
			orderShipmentsEle.setAttribute(CrocsXmlConstants.A_SPLIT_SHIPMENT, CrocsXmlConstants.A_FALSE_STRING);
		
		logger.verbose("End of method prepareDocForSFCC with outDoc: " + SCXmlUtil.getString(getOrderListDoc));
		return getOrderListDoc;
	}

	/** Update the Shipping Charge in OverallTotal by recalculating it based on the
	 * Shipping Charge and Shipping Discount from OverallChargeTotals.
	 * Update the Grand Discount in OverallTotal using the Line Overall Discount from OverallChargeTotals.
	 * @param getOrderListDoc getOrderList Doc
	 * @param orderEle
	 */
	private void updateOverAllCharges(Document getOrderListDoc, Element orderEle) {
        String strOrderNo = null;
		double shippingCharge = 0.0;
		double shippingDiscount = 0.0;
        try {
            Element overAllTotals = SCXmlUtil.getChildElement(orderEle, E_OVERALL_TOTALS);
            Element overallChargeTotals = SCXmlUtil.getChildElement(overAllTotals, E_OVERALL_CHARGE_TOTALS);
            List<Element> overAllChargeTotalList = SCXmlUtil.getChildrenList(overallChargeTotals);
            strOrderNo = orderEle.getAttribute(A_ORDER_NO);

            for (Element overAllChargeTotal : overAllChargeTotalList) {
                String chargeName = SCXmlUtil.getAttribute(overAllChargeTotal, A_CHARGE_NAME);
                if (!YFCCommon.isVoid(chargeName) && A_SHIPPING_CHARGE.equals(chargeName)) {
                    shippingCharge += SCXmlUtil.getDoubleAttribute(overAllChargeTotal, A_GRAND_CHARGES, 0.0);
                }
                if (!YFCCommon.isVoid(chargeName) && A_SHIPPING_DISCOUNT.equals(chargeName)) {
                    shippingDiscount += SCXmlUtil.getDoubleAttribute(overAllChargeTotal, A_GRAND_DISCOUNT, 0.0);
                }
            }
            // Final Shipping Charge
            Double finalShippingCharges = shippingCharge - shippingDiscount;
            overAllTotals.setAttribute(A_GRAND_CHARGES, String.format("%.2f", finalShippingCharges));
            logger.verbose("End of method updateOverAllCharges with getOrderListDoc: " + SCXmlUtil.getString(getOrderListDoc));
    } catch (Exception e) {
        logger.info("OMS_Update : CrocsShipmentconfirmationNotification : updateOverAllCharges : in catch block : " + strOrderNo);
        logger.verbose(" error in CrocsShipmentconfirmationNotification:updateOverAllCharges" + e.getMessage());
        throw new YFCException("CrocsShipmentconfirmationNotification:updateOverAllCharges" + e.getMessage());

    }

	}

	/**
	 * This method will update the shipment status at each line
	 * Possible Status are: Shipped, Cancelled, Delayed and PartialShipment
	 * @param getOrderListDoc
	 */
	private void updateShipmentStatus(Document getOrderListDoc) {
		logger.verbose("Start of method updateShipmentStatus with getOrderListDoc: " + SCXmlUtil.getString(getOrderListDoc));

		Element getOrderListEle = getOrderListDoc.getDocumentElement();
		Element orderEle = SCXmlUtil.getChildElement(getOrderListEle, E_ORDER);
		Element orderLinesEle = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);
		
		List<Element> orderLineList = SCXmlUtil.getChildrenList(orderLinesEle);
		boolean isPartialCancel = false;
		int countOfCancelledLines = 0;
		
		for(Element eachLine : orderLineList) {
			Element orderStatusesEle = SCXmlUtil.getChildElement(eachLine, E_ORDER_STATUSES);
			List<Element> orderStatusList = SCXmlUtil.getChildrenList(orderStatusesEle);
			
			if(orderStatusList.size() == 1) {
				String orderStatus = orderStatusList.get(0).getAttribute(A_STATUS);
				switch (orderStatus) {
				case STATUS_SHIPPED:
					eachLine.setAttribute(A_SHIPMENT_STATUS, VAL_SHIPPED);
					break;
				case STR_STATUS_CANCELLED:
					eachLine.setAttribute(A_SHIPMENT_STATUS, STR_CANCELLED);
					isPartialCancel = true;
					countOfCancelledLines++;
					break;
				case STATUS_INCLUDED_IN_SHIPMENT:
					eachLine.setAttribute(A_SHIPMENT_STATUS, VAL_DELAYED);
					break;
				default:
					break;
				}
			}else {
				eachLine.setAttribute(A_SHIPMENT_STATUS, VAL_PARTIAL_SHIPMENT);
				// EOMS-3514 changes start
				for(Element eachOrderStatus : orderStatusList) {
					if(eachOrderStatus.getAttribute(A_STATUS).equals(STR_STATUS_CANCELLED))
						isPartialCancel = true;
				}
				// EOMS-3514 changes end
				
			}
			logger.verbose("ShipmentStatus at each line is : " + SCXmlUtil.getString(eachLine));

		}
		Element shipmentsEle = SCXmlUtil.getChildElement(getOrderListEle, E_SHIPMENTS);
		// isPartialCancel method was refactored and changes were considered here 
		shipmentsEle.setAttribute(CrocsXmlConstants.A_PARTIAL_CANCEL, isPartialCancel ? A_TRUE_STRING : A_FALSE_STRING);
		
		// short pick scenario marking the flag as Y for auth void in adyen portal
		if(countOfCancelledLines == orderLineList.size())
			getOrderListEle.setAttribute(A_AUTH_VOID_REQ, FLAG_Y);
		else
			getOrderListEle.setAttribute(A_AUTH_VOID_REQ, FLAG_N);
			
		
		logger.verbose("End of method updateShipmentStatus with getOrderListDoc: " + SCXmlUtil.getString(getOrderListDoc));
		
	}

	/**
	 * This method is used to call the getOrderList API using the orderHeaderKey
	 * @param env
	 * @param orderHeaderKey
	 * @return
	 */
	private Document invokeGetOrderList(YFSEnvironment env, String orderHeaderKey) {
		logger.verbose("Start of method invokeGetOrderList with orderHeaderKey"+orderHeaderKey);
		
		Document getOrderListInput = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
		Element orderEle = getOrderListInput.getDocumentElement();
		orderEle.setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY, orderHeaderKey);

		try {
			Document getOrderListOutput = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_LIST_ON_SHIPMENT_CONFIRM, API_GET_ORDER_LIST, getOrderListInput);
			logger.verbose("End of method invokeGetOrderList with getOrderListOutput"+SCXmlUtil.getString(getOrderListOutput));
			return getOrderListOutput;
		} catch (Exception e) {
			throw new YFSException("Error invoking getOrderList API: " + e.getMessage());
		}
	}

	/**
	 * This method is used to call the getShipmentListForOrder API using the orderHeaderKey
	 * @param env
	 * @param orderHeaderKey
	 * @return
	 * @throws Exception
	 */
	private Document getShipmentListForOrder(YFSEnvironment env, String orderHeaderKey) throws Exception {
		logger.verbose("Start of method getShipmentListForOrder with orderHeaderKey: " + orderHeaderKey);

		Document getShipmentListIndoc = SCXmlUtil.createDocument(E_ORDER);
		Element getShipmentListEle = getShipmentListIndoc.getDocumentElement();
		getShipmentListEle.setAttribute(A_ORDER_HEADER_KEY, orderHeaderKey);

		logger.debug("getShipmentListForOrder Input: " + SCXmlUtil.getString(getShipmentListIndoc));
		try {
			Document shipmentListDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_SHIPMENT_LIST_FOR_ORDER_ON_SHIPMENT_SUCCESS, API_GET_SHIPMENT_LIST_FOR_ORDER, getShipmentListIndoc);
			logger.verbose("End of method getShipmentListForOrder with output: " + SCXmlUtil.getString(shipmentListDoc));
			return shipmentListDoc;
		}catch (Exception e) {
			logger.verbose("End of method getShipmentListForOrder with output: " + e.getMessage());
			throw new YFSException("Error invoking getShipmentListForOrder API: " + e.getMessage());			
		}
	}

	/**  EOMS- 4973 this method gets all the unique promotions present in the order and assign it to 1st OrderLine
	 * @param getOrderListDoc getOrderListDoc
	 */
	public void getUniquePromotionId(Document getOrderListDoc) {
		String strOrderNo = null;
		double shippingDiscount = 0.0;
		try {
			logger.verbose("Start of method getUniquePromotionId with getOrderListDoc: " + SCXmlUtil.getString(getOrderListDoc));

			Element getOrderListEle = getOrderListDoc.getDocumentElement();
			Element orderEle = SCXmlUtil.getChildElement(getOrderListEle, E_ORDER);
			strOrderNo = orderEle.getAttribute(A_ORDER_NO);
			Element orderLinesEle = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);

			List<Element> orderLineList = SCXmlUtil.getChildrenList(orderLinesEle);
			Element overAllTotals = SCXmlUtil.getChildElement(orderEle,E_OVERALL_TOTALS);
			// Collect unique promotions using HashMap
			Map<String, String> uniquePromo = new HashMap<>();
			for (Element orderLine : orderLineList) {
				Element lineCharges = SCXmlUtil.getChildElement(orderLine, E_LINE_CHARGES);
				List<Element> lineCharge = SCXmlUtil.getChildrenList(lineCharges);
				for (Element lineChargeEle : lineCharge) {
					Element extn = SCXmlUtil.getChildElement(lineChargeEle, E_EXTN);
					if (extn != null) {
						String promoId = extn.getAttribute(E_EXTN_DWPROMOTION_ID);
						String promoText = extn.getAttribute(E_EXTN_PROMOTION_TEXT);
						// Only add if not already in map
						uniquePromo.putIfAbsent(promoId, promoText);
					}
					//Remove LineCharge element from LineCharges
					lineCharges.removeChild(lineChargeEle);
				}
				// Sum up discount amounts from lineOverallTotals
				Element lineOverAllTotals = SCXmlUtil.getChildElement(orderLine, E_LINE_OVERALL_TOTALS);
				shippingDiscount += SCXmlUtil.getDoubleAttribute(lineOverAllTotals, A_DISCOUNT, 0.0);
			}
			//Update GrandDiscount in OverAllTotals
			overAllTotals.setAttribute(A_GRAND_DISCOUNT, String.format("%.2f", shippingDiscount));

			//Assign Unique promotions to 1st OrderLine
			if (!orderLineList.isEmpty()) {
				Element firstOrderLine = orderLineList.get(0);
				Element lineCharges = SCXmlUtil.getChildElement(firstOrderLine, E_LINE_CHARGES);
				for (Map.Entry<String, String> entry : uniquePromo.entrySet()) {
					Element lineCharge = firstOrderLine.getOwnerDocument().createElement(E_LINE_CHARGE);
					Element extn = firstOrderLine.getOwnerDocument().createElement(E_EXTN);
					extn.setAttribute(E_EXTN_DWPROMOTION_ID, entry.getKey());
					extn.setAttribute(E_EXTN_PROMOTION_TEXT, entry.getValue());
					lineCharge.appendChild(extn);
					lineCharges.appendChild(lineCharge);
				}
			}
			logger.verbose("End of method getUniquePromotionId with getOrderListDoc: "+ SCXmlUtil.getString(getOrderListDoc));
		} catch (Exception e) {
			logger.info("OMS_Update : CrocsShipmentconfirmationNotification : getUniquePromotionId : in catch block : " + strOrderNo);
			logger.verbose(" error in CrocsShipmentconfirmationNotification:getUniquePromotionId" + e.getMessage());
			throw new YFCException("CrocsShipmentconfirmationNotification:getUniquePromotionId" + e.getMessage());

		}
	}

	/**This method will filter the Item details(below attributes) based on locale if present else will take it from OOB
	 * ProductUrl,ColorCode,SizeCode
	 * @param getOrderListDoc
	 */
	public void getLocalizedItemFeed(Document getOrderListDoc) {
		try{
		Element getOrderListEle = getOrderListDoc.getDocumentElement();
		Element orderEle = SCXmlUtil.getChildElement(getOrderListEle, E_ORDER);
		Element orderLinesEle = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);

		String enterpriseCode = orderEle.getAttribute(A_ENTERPRISE_CODE);


		Element extnEle = SCXmlUtil.getChildElement(orderEle,E_EXTN);
		String 	extnCustomerLocale = extnEle.getAttribute(EXTN_CUSTOMER_LOCALE);

		/**Update Locale for US : As US has 2 locales 'default' and 'en_US' so if locale is default' we are setting it to en_US to fetch data from
		 * CROCS_ITEM_ORG_DATA table
		**/

		if (!YFCCommon.isVoid(extnCustomerLocale) && V_US_DEFAULT_LOCALE.equals(extnCustomerLocale)) {
			extnCustomerLocale= V_EN_US_LOCALE;
			logger.info("Updating default locale to en_US" + extnCustomerLocale);
		}

        /* this will compare OrganizationCode at crocsItemOrgData level to EnterpriseCode and extnCustomerLocale to locale
        *  and if these values matches it will take SizeCode , Color, ProductUrl from Hang-off table(crocsItemOrgData) otherwise take from OOB */
		List<Element> orderLineList = SCXmlUtil.getChildrenList(orderLinesEle);
		for (Element orderLine : orderLineList) {
			Element itemDetailsEle = SCXmlUtil.getChildElement(orderLine, E_ITEM_DETAILS);
			Element primaryInformation = SCXmlUtil.getChildElement(itemDetailsEle,E_PRIMARY_INFORMATION );
			Element extnItemEle = SCXmlUtil.getChildElement(itemDetailsEle, E_EXTN);
			Element crocsItemOrgDataList = SCXmlUtil.getChildElement(extnItemEle,E_CROCS_ITEM_ORG_DATA_LIST);
			List<Element> crocsItemOrgData = SCXmlUtil.getChildrenList(crocsItemOrgDataList);

			Iterator<Element> crocsIterator = crocsItemOrgData.iterator();
			while (crocsIterator.hasNext()) {
				Element crocsItemOrgDataEle = crocsIterator.next();
				String organizationCode = crocsItemOrgDataEle.getAttribute(A_ORGANIZATION_CODE);
				String locale = crocsItemOrgDataEle.getAttribute(A_LOCALE);

				if ((!YFCCommon.isVoid(organizationCode) && organizationCode.equals(enterpriseCode)) && (!YFCCommon.isVoid(locale) && extnCustomerLocale.equals(locale))) {
					/** Keep and update attributes
					 * keeping the data from Locale to primary info.
					 * */
					String sizeCode= crocsItemOrgDataEle.getAttribute(A_SIZE_CODE);
					String colorCode= crocsItemOrgDataEle.getAttribute(A_COLOR);
					String productUrl= crocsItemOrgDataEle.getAttribute(A_PRODUCT_URL);

					/*Updating Sizecode*/
					if(!YFCCommon.isVoid(sizeCode)) {
						primaryInformation.setAttribute(A_SIZE_CODE, sizeCode);
						logger.info("SizeCode is obtained from the CrocsItemOrgData table");
					}
					else {
						primaryInformation.setAttribute(A_SIZE_CODE, primaryInformation.getAttribute(A_SIZE_CODE));
					}
                    /*Updating Colorcode*/
					if(!YFCCommon.isVoid(colorCode)) {
						primaryInformation.setAttribute(A_COLOR_CODE, colorCode);
						logger.info("ColorCode is obtained from the CrocsItemOrgData table");
					}
					else {
						primaryInformation.setAttribute(A_COLOR_CODE, primaryInformation.getAttribute(A_COLOR_CODE));
					}
					/*Updating ProductUrl*/
					if(!YFCCommon.isVoid(colorCode)) {
						extnItemEle.setAttribute(A_EXTN_PRODUCT_URL,productUrl);
						logger.info("ProductUrl is obtained from the CrocsItemOrgData table");
					}
					else {
						extnItemEle.setAttribute(A_EXTN_PRODUCT_URL, extnItemEle.getAttribute(A_EXTN_PRODUCT_URL));
					}
				}
					// Remove ItemOrgDataList Element
				// Safely remove the current element using the iterator
				crocsIterator.remove();

			}
		}
		logger.verbose("CrocsCAShipmentConfirmation :getLocalizedItemFeed: Itemfeed Updated: "+ SCXmlUtil.getString(getOrderListDoc));
	} catch (YFSException e) {
			logger.info("OMS_Update : CrocsCAShipmentconfirmationNotification : getLocalizedItemFeed : in catch block : " + SCXmlUtil.getString(getOrderListDoc) +
					"\n" + e.getMessage() +"\n" +e.getErrorDescription()+"\n"+e.getErrorCode());
			throw new YFSException(e.getMessage(),e.getErrorDescription(),e.getErrorCode());
		}

	}

	}