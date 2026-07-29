package com.crocs.oms.order;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.ibm.icu.text.SimpleDateFormat;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class CrocsCreateOrderReservation implements CrocsConstant {
	
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCreateOrderReservation.class);

	/**
	 * This method does below :
	 * 1. makes reserveAvailableInventory OOB api call
	 * 2. stamps the details sent by IV in above api output to the create order message.
	 * 
	 * @param env
	 * @param inDoc
	 * @return
	 * @throws Exception
	 */
	public  Document createReservation(YFSEnvironment env, Document inDoc) throws Exception {
		
		logger.beginTimer("CrocsCreateOrderReservation::createReservation: START:"+ SCXmlUtil.getString(inDoc));
		try
		{
			String strOrderNo="";
			Element inDocOrderEle = inDoc.getDocumentElement();
			//EOMS-5516 : Manage reservation for MarketPlace Orders : START
			if (inDocOrderEle.getAttribute(CrocsXmlConstants.A_ORDER_TYPE) != null
					&& CrocsConstant.ORDER_TYPE_MP.equalsIgnoreCase(inDocOrderEle.getAttribute(CrocsXmlConstants.A_ORDER_TYPE))) {
				strOrderNo = inDocOrderEle.getAttribute(CrocsXmlConstants.A_CUST_CUST_PO_NO);
			}else {
				strOrderNo = inDocOrderEle.getAttribute(CrocsXmlConstants.A_ORDER_NO);
			}
			//EOMS-5516 : Manage reservation for MarketPlace Orders : END
			inDocOrderEle.setAttribute(CrocsXmlConstants.A_VALIDATE_ITEM, "Y");
			Element eleOrderLines = (Element) inDocOrderEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINES).item(0);

			NodeList orderLineList = SCXmlUtil.getXpathNodes(eleOrderLines, CrocsXmlConstants.E_ORDER_LINE);
			for (int i = 0; i < orderLineList.getLength(); i++) {
				Element eleOrderLine = (Element) orderLineList.item(i);
                eleOrderLine.setAttribute(CrocsXmlConstants.A_RESERVATION_ID, strOrderNo);
			}
			manageReservations(env, inDoc);
		}
		catch (Exception e) {
			logger.error("Create order exception during reservation" + e.getMessage());
			throw e;
		}
		logger.endTimer("CrocsCreateOrderReservation::createReservation: END:"+ SCXmlUtil.getString(inDoc));
		return inDoc;
    }
	
	 /*
	 * This method creates the reserve available inventory api input
	 * it includes attributes for creation of input
	 * Item id, product class, quantity,fulfillment type and UOM
	 * */
	private Document manageReservations(YFSEnvironment env, Document inDoc) throws Exception {

        try {
            Element inDocOrderEle = inDoc.getDocumentElement();
            String strDocumentType = inDocOrderEle.getAttribute(CrocsXmlConstants.A_DOCUMENT_TYPE);
            String strOrderName = inDocOrderEle.getAttribute(CrocsXmlConstants.A_ORDER_NAME);
			String strOrderPurpose = inDocOrderEle.getAttribute(CrocsXmlConstants.A_ORDER_PURPOSE);
            
            String strOrganizationCode = inDocOrderEle.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
            String strOrderNo = "";
            String strAllocationRuleID = inDocOrderEle.getAttribute(CrocsXmlConstants.A_ALLOCATION_RULE_ID);
            
            //EOMS-9055 - Changes Start - Crocs CA MP
            String strOrderType = inDocOrderEle.getAttribute(CrocsXmlConstants.A_ORDER_TYPE);
            //EOMS-9055 - Changes End
            
          //EOMS-5516 : Manage reservation for MarketPlace Orders : START
            if (inDocOrderEle.getAttribute(CrocsXmlConstants.A_ORDER_TYPE) != null
					&& CrocsConstant.ORDER_TYPE_MP.equalsIgnoreCase(inDocOrderEle.getAttribute(CrocsXmlConstants.A_ORDER_TYPE))) {
				strOrderNo = inDocOrderEle.getAttribute(CrocsXmlConstants.A_CUST_CUST_PO_NO);
			}else {
				strOrderNo = inDocOrderEle.getAttribute(CrocsXmlConstants.A_ORDER_NO);
			}
          //EOMS-5516 : Manage reservation for MarketPlace Orders : END
            Document docPromise = SCXmlUtil.createDocument("Promise");
            Element elePromise = docPromise.getDocumentElement();
            elePromise.setAttribute(CrocsXmlConstants.A_DEMAND_TYPE, "RSRV_ORDER");
            elePromise.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, strOrganizationCode);
            elePromise.setAttribute(CrocsXmlConstants.A_CHECK_INVENTORY, "Y");
            
            //EOMS-8651: Changes Start: Stamping AllocationRuleID
            //EOMS-11118: Stamping AllocationRuleID for Crocs SG : START
            //EOMS-11371 : Stamping AllocationRuleID for Crocs KR : START
			if(!YFCCommon.isVoid(strOrganizationCode)){
				switch (strOrganizationCode) {
					case CROCS_CA:
					case CROCS_KR:
					case CROCS_SG:
					case HEYDUDE_US:
					case HEYDUDE_MP:
					case HEYDUDE_CA:
						elePromise.setAttribute(
								CrocsXmlConstants.A_ALLOCATION_RULE_ID,
								strAllocationRuleID
						);
						logger.info("Allocation rule ID is stamped for organization "
								+ strOrganizationCode + " and allocation rule ID is " + strAllocationRuleID);
						break;
					default:
						break;
				}
			}
            //EOMS-8651: Changes End
			//EOMS-11118: Stamping AllocationRuleID for Crocs SG : END
            
            Element eleReservationParameters = SCXmlUtil.createChild(elePromise, "ReservationParameters");
            eleReservationParameters.setAttribute(CrocsXmlConstants.A_RESERVATION_ID, strOrderNo);
            //TO Reservation adding Expiration Date
            if (CrocsConstant.A_TO_DOCUMENT_TYPE.equalsIgnoreCase(strDocumentType)) {
				String strReqDeliveryDate = inDocOrderEle.getAttribute(CrocsXmlConstants.A_REQ_DELIVERY_DATE);
				if (!YFCObject.isNull(strReqDeliveryDate)) {
					if (CrocsConstant.A_STANDARD_TO.equalsIgnoreCase(strOrderName)) {
						eleReservationParameters.setAttribute(CrocsXmlConstants.A_EXPIRATION_DATE, strReqDeliveryDate);
					} else if (CrocsConstant.A_RUSH_TO.equalsIgnoreCase(strOrderName)) {
						String omsDateFormat = "yyyy-MM-dd'T'HH:mm:ss.S";
						SimpleDateFormat sdf = new SimpleDateFormat(omsDateFormat);
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.DATE, 30);
						String newDate = sdf.format(cal.getTime());
						eleReservationParameters.setAttribute(CrocsXmlConstants.A_EXPIRATION_DATE, newDate);
						logger.verbose("Rush To Exporation Date" + newDate);
					}
				}
			}
            
			/** EOMS-6289 - adding Inventory Reservation Expiration Date for Exchanges Orders.
			 * 	Reservation will be held upto 21 days for Exchnage Order.
			 * 	if IF no inventory found while reserving , cancel the EO.
			 **/
			else if (CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER.equalsIgnoreCase(strDocumentType)
					&& CrocsConstant.VAL_ORDER_PURPOSE.equalsIgnoreCase(strOrderPurpose)) {
				
				//EOMS-10885: Change Reservation of inventory from 21 days to 30 days : START
				int iExpiryDays = 30;
				Document docGetCommonCodeList = CommonUtil.getCommonCodeList(env, STR_CROCS, STR_CROCS_EXCHANGE_RSV_DAY, STR_CROCS_EXCHANGE_RESERVATION_DAY);
				if(!YFCCommon.isVoid(docGetCommonCodeList)) {
					iExpiryDays = Integer.parseInt(SCXmlUtil.getXpathAttribute(docGetCommonCodeList.getDocumentElement(), "//CommonCodeList/CommonCode[1]/@CodeShortDescription"));
				}
				logger.info("Reservation Expiry for EO "+strOrderNo+" is defaulted to "+iExpiryDays+" Commoncode CROCS_EXCH_RSV_DAY is void");
				//EOMS-10885: Change Reservation of inventory from 21 days to 30 days : END
				String omsDateFormat = "yyyy-MM-dd'T'HH:mm:ss.S";
				SimpleDateFormat sdf = new SimpleDateFormat(omsDateFormat);
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, iExpiryDays);
				String newDate = sdf.format(cal.getTime());
				eleReservationParameters.setAttribute(CrocsXmlConstants.A_EXPIRATION_DATE, newDate);
				logger.verbose("CrocsCreateOrderReservation : Inventory Expiration Date for Exchanges Order "+strOrderNo+" is " + newDate);
				logger.info("CrocsCreateOrderReservation : Inventory Expiration Date for Exchanges Order "+strOrderNo+" is " + newDate);
			}
            Element elePromiseLines = SCXmlUtil.createChild(elePromise, "PromiseLines");
       
            NodeList orderLineList = inDocOrderEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);

            for (int i = 0; i < orderLineList.getLength(); i++) {
                Element eleOrderLine = (Element) orderLineList.item(i);
              
                Element eleItem = SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_ITEM);
                Element elePromiseLine = SCXmlUtil.createChild(elePromiseLines, CrocsXmlConstants.E_PROMISE_LINE);
                elePromiseLine.setAttribute(CrocsXmlConstants.A_LINE_ID, eleOrderLine.getAttribute(CrocsXmlConstants.A_LINE_ID));
                elePromiseLine.setAttribute(CrocsXmlConstants.A_ITEM_ID, eleItem.getAttribute(CrocsXmlConstants.A_ITEM_ID));
                elePromiseLine.setAttribute("RequiredQty", eleOrderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY));
                elePromiseLine.setAttribute(CrocsXmlConstants.A_FULFILLMENT_TYPE, eleOrderLine.getAttribute(CrocsXmlConstants.A_FULFILLMENT_TYPE));

                //EOMS-8430 - Changes Start - Cross border CA Shipping 
                //EOMS-9055 - Changes Start - Crocs CA MP
                String strCarrierServiceCode = eleOrderLine.getAttribute(CrocsXmlConstants.A_CARRIER_SERVICE_CODE);
                if(!CrocsConstant.ORDER_TYPE_MP.equalsIgnoreCase(strOrderType) && CrocsConstant.CROCS_CA.equalsIgnoreCase(strOrganizationCode) && !YFCCommon.isVoid(strCarrierServiceCode) 
                		&& CrocsConstant.VAL_EXPEDITED.equalsIgnoreCase(strCarrierServiceCode)) {
                    elePromiseLine.setAttribute(CrocsXmlConstants.A_FULFILLMENT_TYPE, CrocsConstant.CROCS_CA_FULFILLMENT_TYPE_EXPRESS);
                }
                //EOMS-8430 - Changes End
                //EOMS-9055 - Changes End
                
                //EOMS-10554 - Changes Start
                if(!CrocsConstant.ORDER_TYPE_MP.equalsIgnoreCase(strOrderType) 
                		&& CrocsConstant.CROCS_CA.equalsIgnoreCase(strOrganizationCode) 
                		&& CrocsConstant.A_SALES_ORDER_DOCUMENT_TYPE.equalsIgnoreCase(strDocumentType)
                		&& CommonUtil.validatePOBoxAddress(inDoc)) {
                	
                    elePromiseLine.setAttribute(CrocsXmlConstants.A_FULFILLMENT_TYPE, CrocsConstant.CROCS_CA_FULFILLMENT_TYPE_EXPRESS);
                }
                //EOMS-10554 - Changes End

                elePromiseLine.setAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE, eleItem.getAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE));
                elePromiseLine.setAttribute(CrocsXmlConstants.A_PRODUCT_CLASS, eleItem.getAttribute(CrocsXmlConstants.A_PRODUCT_CLASS));
                logger.verbose(" reserveAvailableInventory request XML to create reservation"+XMLUtil.getXMLString(docPromise));
               
            }
           
			Document docPromiseoutput = CommonUtil.invokeAPI(env, "", CrocsXmlConstants.A_RESERVE_AVAIALABLE_INVENTORY_API, docPromise);
			
			logger.verbose(" After calling reserveAvailableInventory for create order reservations"+XMLUtil.getXMLString(docPromiseoutput));
			
			//Start : Pass the reservation details from IV onto create order message.
			logger.verbose("***create order message before stamping reservation details : "+XMLUtil.getXMLString(inDoc));
			//fetch and store the reservation details in a Map.
			Map<String, List<Element>> reservationMap = new HashMap<>();
	        NodeList promiseLines = docPromiseoutput.getElementsByTagName(CrocsXmlConstants.E_PROMISE_LINE);
	        for (int i = 0; i < promiseLines.getLength(); i++) {
	            Element promiseLine = (Element) promiseLines.item(i);
	            String itemId = promiseLine.getAttribute(CrocsXmlConstants.A_ITEM_ID);
	            NodeList reservations = promiseLine.getElementsByTagName("Reservation");
	            List<Element> reservationList = new ArrayList<>();
	            for (int j = 0; j < reservations.getLength(); j++) {
	                reservationList.add((Element) reservations.item(j));
	            }
	            reservationMap.put(itemId, reservationList);
	        }
	        //Stamp the reservation details onto the create order message.
	        NodeList orderLines = inDoc.getElementsByTagName("OrderLine");
	        for (int i = 0; i < orderLines.getLength(); i++) {
	            Element orderLine = (Element) orderLines.item(i);
	            Element item = (Element) orderLine.getElementsByTagName("Item").item(0);
	            String itemId = item.getAttribute(CrocsXmlConstants.A_ITEM_ID);
	            if (reservationMap.containsKey(itemId)) {
	                Element orderLineReservations = inDoc.createElement("OrderLineReservations");
	                for (Element reservation : reservationMap.get(itemId)) {
	                    Element orderLineReservation = inDoc.createElement("OrderLineReservation");
	                    orderLineReservation.setAttribute(CrocsXmlConstants.A_ITEM_ID, itemId);
	                    orderLineReservation.setAttribute("Node", reservation.getAttribute("ShipNode"));
	                    orderLineReservation.setAttribute("ProductClass", reservation.getAttribute("ProductClass"));
	                    orderLineReservation.setAttribute("Quantity", reservation.getAttribute("ReservedQty"));
	                    orderLineReservation.setAttribute("ReservationID", reservation.getAttribute("ReservationID"));
	                    orderLineReservation.setAttribute("UnitOfMeasure", reservation.getAttribute("UnitOfMeasure"));
	                    orderLineReservations.appendChild(orderLineReservation);
	                }
	                orderLine.appendChild(orderLineReservations);
	            }
	        }
	        logger.verbose("***create order message after stamping reservation details : "+XMLUtil.getXMLString(inDoc));
	        //End : Pass the reservation details from IV onto create order message.
	        
	        // To invoke the alert in case of any reservation failures while creation of
			// reservations
			Element ele1 = docPromiseoutput.getDocumentElement();
			NodeList promiseLineList = ele1.getElementsByTagName(CrocsXmlConstants.E_PROMISE_LINE);
			for (int i = 0; i < promiseLineList.getLength(); i++) {
				Element elePromiseLineList = (Element) promiseLineList.item(i);
				Element pageElement1 = SCXmlUtil.getXpathElement(elePromiseLineList, "Reservations");
				String strAvailableQty = pageElement1.getAttribute("AvailableQty");
				String strTotalReservedQty = pageElement1.getAttribute("TotalReservedQty");
				String strFulfillmentType = elePromiseLineList.getAttribute("FulfillmentType");

				if (strTotalReservedQty.equals("0.00") || strAvailableQty.equals("0.00")
						|| strFulfillmentType.equals("")) {
					CommonUtil.invokeService(env, "CrocsReservationFailureAlertSyncServ", inDoc);
					CommonUtil.debug(logger, "After alert created successfully :" + XMLUtil.getXMLString(inDoc));
				}
			}
        } catch (Exception e) {
			logger.verbose("*** Catch block while fetching and stamping the reservation details ***");
			e.printStackTrace();
		}
        logger.verbose("***Final create order message : "+XMLUtil.getXMLString(inDoc));
		return inDoc;
	}
}
