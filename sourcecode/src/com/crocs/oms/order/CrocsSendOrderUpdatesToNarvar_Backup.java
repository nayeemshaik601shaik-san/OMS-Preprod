package com.crocs.oms.order;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.math.BigDecimal;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 *
 * This class is used to send Order Updates to Narvar
 * 1. To Send order Shipped update, when order is completly shipped in OMS
 *    By invoking a sync service on confirm shipment on success event(POST)
 * Or
 * 2.Store return order is created in OMS(EntryType:Store, coming from APTOS system)
 * 	 By invoking a sync service on create return on success event(POST)
 *
 * @author IBM
 *
 */
public class CrocsSendOrderUpdatesToNarvar_Backup implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsSendOrderUpdatesToNarvar_Backup.class);


	/**
	 *
	 * @param env
	 * @param indoc : On Success event message of Either Create return or Shipment Shipped
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public  void prepareAndSendUpdatesToNarvar(YFSEnvironment env, Document indoc ){

		logger.verbose("Starts of method CrocsSendOrderUpdatesToNarvar:prepareAndSendUpdatesToNarvar: "+SCXmlUtil.getString(indoc));
		//Either Shipment Shipped, Return created

		Element inputEle = indoc.getDocumentElement();//check if it is Shipment or Order
		String rootNAme = inputEle.getNodeName();
		if(rootNAme.equalsIgnoreCase(E_ORDER)){
			logger.verbose("Order element details received");

			/**
			 * Getting return order details information
			 */
			Element orderDetails = getOrderInfo(env, inputEle,true);

			/**
			 * Preparing Json payload for Shipment update to Narvar
			 */
			JSONObject returnOrderJsonPayload = prepareReturnUpdateMessage(env, orderDetails);

			/**
			 * connecting with Narvar APIs via REST connection and sending update
			 */
			narvarRestAPICallToPostUpdate(env, orderDetails, returnOrderJsonPayload, false, true);

		}else if (rootNAme.equals(E_SHIPMENT)){
			logger.verbose("Shipment element details received");
			///Shipment/ShipmentLines/ShipmentLine/Order
			Element orderEle = SCXmlUtil.getXpathElement(inputEle, "ShipmentLines/ShipmentLine/Order");

			/**
			 * Getting sales order details information
			 */
			Element orderDetails = getOrderInfo(env, orderEle,false);

			/**
			 * Need to call below two steps only when Order is completely Shipped in OMS
			 * Checking if order is completly shipped in oms. then only send update to Narvar
			 */
			boolean isOrderFullyShipped = isOrderCompletelyShipped(env, orderDetails);

			if(isOrderFullyShipped){
				/**
				 * Preparing Json payload for Shipment update to Narvar
				 */
				JSONObject orderJson = prepareShipmentUpdateToNarvarMsg(env, orderDetails, indoc);

				logger.verbose("Final Json payload is "+orderJson.toString());
				logger.info("OMS_Update : CrocsSendOrderUpdatesToNarvar.prepareAndSendUpdatesToNarvar() : OMS to Narvar Update:" +  orderJson.toString());

				/**
				 * connecting with Narvar APIs via REST connection and sending update
				 */
				narvarRestAPICallToPostUpdate(env, orderDetails, orderJson, true, false);
			}
		}

	}


	/**
	 * Getting sales or return order details
	 * by calling getOrderList API
	 * @param env
	 * @param orderEle
	 * @param isReturnOrder
	 * @return
	 */
	private Element getOrderInfo(YFSEnvironment env, Element orderEle, boolean isReturnOrder){

		logger.verbose("method getOrderInfo starts: "+SCXmlUtil.getString(orderEle));
		Element orderInfo = null;
		Document getOrderListIndoc = SCXmlUtil.createDocument(E_ORDER);
		Element getOrderListEle = getOrderListIndoc.getDocumentElement();

		if(isReturnOrder){
			getOrderListEle.setAttribute(A_DOCUMENT_TYPE, A_RETURN_ORDER_DOCUMENT_TYPE);
		}else{
			getOrderListEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);

		}

		getOrderListEle.setAttribute(A_ORDER_NO, orderEle.getAttribute(A_ORDER_NO));
		getOrderListEle.setAttribute(A_ENTERPRISE_CODE, orderEle.getAttribute(A_ENTERPRISE_CODE));
		logger.verbose(" getOrderInfo input is: "+SCXmlUtil.getString(getOrderListIndoc));


		try {
			Document orderDetails = CommonUtil.invokeService(env, A_CROCS_GET_ORDER_LIST_FOR_NARVAR_UPDATE, getOrderListIndoc);
			logger.verbose(" getOrderInfo output is: "+SCXmlUtil.getString(orderDetails));

			Element orderList = orderDetails.getDocumentElement();
			orderInfo = SCXmlUtil.getChildElement(orderList, E_ORDER);
		} catch (Exception e) {
			e.printStackTrace();
			logger.verbose("Exception in getorderList Call "+e.getLocalizedMessage());

		}
		return  orderInfo;

	}

	/**
	 * Preparing json payload to send narvar update for Store Returns
	 * @param env
	 * @param orderDetails: return order details
	 * @return: JSON payload with shipment update details
	 */
	private JSONObject prepareReturnUpdateMessage(YFSEnvironment env, Element orderDetails){

		logger.verbose("Starts of method prepareReturnUpdateMessage:"+SCXmlUtil.getString(orderDetails));

		String enterpriseCode = "";
		enterpriseCode = orderDetails.getAttribute(A_ENTERPRISE_CODE);
		Element returnOrderLines = SCXmlUtil.getChildElement(orderDetails, E_ORDER_LINES);
		ArrayList<Element> returnOrderLineList = SCXmlUtil.getChildren(returnOrderLines, E_ORDER_LINE);

		String salesOrderNo = SCXmlUtil.getXpathAttribute(orderDetails, "OrderLines/OrderLine/DerivedFromOrder/@OrderNo");

		/**
		 *
		 * Form sales order we take only SalesOrderNo from DeriveFromOrder Element
		 OrderItems will have return order lines and quantities those are returns
		 *
		 * Json Payload to send store returns updates to Narvar
		 * {
		 "rma_number":"STORE12340987",
		 "carrier":"",
		 "order_number":"6568815CUS",
		 "locale":"en_US",
		 "type":"external_returns",
		 "attributes": {
		 "reporting_reason": "return_to_store"
		 },
		 "order_items":[
		 {
		 "quantity":"2",
		 "return_reason_code":"301",
		 "sku":"210069-4NS-M8W10",
		 "customer_comment":"test by narvar"
		 }
		 ]
		 }
		 *
		 */

		// Root JSON Object
		JSONObject orderJson = new JSONObject();

		orderJson.put("rma_number", orderDetails.getAttribute(A_ORDER_NO));
		//carrier at Return TBD
		orderJson.put("carrier", "");
		orderJson.put("order_number", salesOrderNo);

		if(enterpriseCode.equalsIgnoreCase(CROCS_US)){
			orderJson.put("locale", "en_US");
		}else if (enterpriseCode.equalsIgnoreCase(CROCS_CA)){
			orderJson.put("locale", "en_CA");
		}

		orderJson.put("locale", "en_US");
		orderJson.put("type", "external_returns");

		// Nested attributes object
		JSONObject attributes = new JSONObject();
		attributes.put("reporting_reason", "return_to_store");
		orderJson.put("attributes", attributes);

		// Array of order_items
		JSONArray orderItems = new JSONArray();
		for(Element returnOrderLine: returnOrderLineList){

			int quantity = (int) Double.parseDouble(returnOrderLine.getAttribute(A_ORDERED_QTY));
			Element returnItem = SCXmlUtil.getChildElement(returnOrderLine, E_ITEM);
			// First item
			JSONObject orderLineJson = new JSONObject();
			orderLineJson.put("quantity", quantity);
			orderLineJson.put("return_reason_code", "301");
			orderLineJson.put("sku", returnItem.getAttribute(A_ITEM_ID));
			orderLineJson.put("customer_comment", "Returned at Store");

			// Add items to array
			orderItems.put(orderLineJson);
		}

		// Add order_items array to jsonOrder
		orderJson.put("order_items", orderItems);

		return orderJson;
	}


	/**
	 * Preparing JSON payload to send order shipped update for Sales order
	 * @param env
	 * @param orderDetails
	 * @param indoc
	 * @return
	 */
	private JSONObject prepareShipmentUpdateToNarvarMsg(YFSEnvironment env, Element orderDetails, Document indoc ){
		logger.verbose("starts of method prepareShipmentUpdateToNarvarMsg() with shipmentEle:"+SCXmlUtil.getString(indoc));
		logger.verbose("starts of method prepareShipmentUpdateToNarvarMsg() with orderDetails:"+SCXmlUtil.getString(orderDetails));

		String enterpriseCode = "";
		String orderDate = "";
		String trackingNo = "";
		String carrierName = "";
		String extnCustomerLocale = "";

		Element inputEle = indoc.getDocumentElement();
		String shipDate = inputEle.getAttribute(A_ACTUAL_SHIPMENT_DATE);
		Element orderLines = SCXmlUtil.getChildElement(orderDetails, E_ORDER_LINES);
		ArrayList<Element> orderLineList = SCXmlUtil.getChildren(orderLines, E_ORDER_LINE);
		enterpriseCode = orderDetails.getAttribute(A_ENTERPRISE_CODE);
		orderDate = orderDetails.getAttribute(A_ORDER_DATE);

		//as we are saving tracking no and scac same for all the lines, taking first order line always
		Element salesOrderLine = SCXmlUtil.getXpathElement(orderDetails, "OrderLines/OrderLine");
		Element extnSalesOrderLine = SCXmlUtil.getChildElement(salesOrderLine, E_EXTN);
		if(!YFCCommon.isVoid(extnSalesOrderLine)){
			trackingNo = extnSalesOrderLine.getAttribute(EXTN_TRACKING_NO);
			carrierName = extnSalesOrderLine.getAttribute(EXTN_SHIP_CARRIER);
		}

		//Form JSON payload
		JSONObject orderJson = new JSONObject();
		JSONObject orderInfo = new JSONObject();

		orderInfo.put("order_number", orderDetails.getAttribute(A_ORDER_NO));
		orderInfo.put("order_date", orderDate);

		//EOMS-730: Order Updates to Narvar : START
		if(SCXmlUtil.getChildElement(orderDetails, E_EXTN)!=null) {
			Element eleOrderExtn = SCXmlUtil.getChildElement(orderDetails, E_EXTN);
			extnCustomerLocale = eleOrderExtn.getAttribute(EXTN_CUSTOMER_LOCALE);
		}

		if(YFCCommon.isVoid(extnCustomerLocale) || STR_DEFAULT.equalsIgnoreCase(extnCustomerLocale))
			orderInfo.put("checkout_locale", "en_US");
		else
			orderInfo.put("checkout_locale", extnCustomerLocale);
		//EOMS-730 : Order Updates to Narver : END

		/* For Exchange Order : if order purpose is EXCHANGE
		EOMS-6306 START*/
		JSONObject orderInfoAttributes = new JSONObject();
		String orderPurpose=orderDetails.getAttribute(A_ORDER_PURPOSE);
		orderInfoAttributes.put("order_purpose",orderPurpose);

		if(enterpriseCode.equalsIgnoreCase(CROCS_US)){
			orderInfo.put("currency_code", CURRENCY_USD);
			orderInfoAttributes.put("sfcc_site","crocs_us");
		}else if (enterpriseCode.equalsIgnoreCase(CROCS_CA)){
			orderInfo.put("currency_code", CURRENCY_CAD);
			orderInfoAttributes.put("sfcc_site","crocs_ca");
		}
		orderInfo.put("attributes",orderInfoAttributes);
		//EOMS-6306 END
		JSONArray itemsInfoForShipment = new JSONArray();
		JSONArray orderItemsArray = new JSONArray();
		//looping through each order line
		for(Element orderLine : orderLineList ){
			Element itemEle = SCXmlUtil.getChildElement(orderLine, E_ITEM);
			Element linePriceInfoEle = SCXmlUtil.getChildElement(orderLine, E_LINE_PRICE_INFO);
			Element itemDetailsEle = SCXmlUtil.getChildElement(orderLine, E_ITEM_DETAILS);
			Element primaryInfoEle = SCXmlUtil.getChildElement(itemDetailsEle, E_PRIMARY_INFORMATION);

			/**
			 * Calculating UnitPrice value
			 * by LineTotal / PricingQty from LineOverAllTotals Element
			 *  if orderedqty is greater than zero
			 *
			 *  As per existing impl, Narvar is not showing cancelled lines on portal
			 *  so stamping unit price in this case from LinePriceInfo Element
			 *
			 */
			String strUnitPrice = linePriceInfoEle.getAttribute(A_UNIT_PRICE);
			int qtyThreshold = 0 ;
			String orderedQty = orderLine.getAttribute(A_ORDERED_QTY);

			if(Double.parseDouble(orderedQty)> qtyThreshold){
				logger.info("OrderedQty is greater then zero");
				Element lineTotalEle = SCXmlUtil.getChildElement(orderLine, E_LINE_OVERALL_TOTALS);
				Double dLineTotal = Double.parseDouble(lineTotalEle.getAttribute(A_LINE_TOTAL));
				Double dPricingQty = Double.parseDouble(lineTotalEle.getAttribute(A_PRICING_QUANTITY));
				Double dUnitPrice = dLineTotal / dPricingQty;

				BigDecimal bdUnitPrice = new BigDecimal(dUnitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
				strUnitPrice = bdUnitPrice.toString();
			}

			logger.info("OrderNo: "+orderDetails.getAttribute(A_ORDER_NO) + " ItemID:: "+ itemEle.getAttribute(A_ITEM_ID)
					+ " UnitPrice is:: "+ strUnitPrice);

			//Defect#EOMS-4316 Fix
			Element extnItemEle = SCXmlUtil.getChildElement(itemDetailsEle, E_EXTN);
			String extnImageURL = "";
			extnImageURL = extnItemEle.getAttribute(A_EXTN_IMAGE_URL);

			int quantity = (int) Double.parseDouble(orderLine.getAttribute(A_ORDERED_QTY));
			JSONObject itemJson = new JSONObject();
			itemJson.put("item_id", itemEle.getAttribute(A_ITEM_ID)); /*EOMS-6483*/
			itemJson.put("sku", itemEle.getAttribute(A_ITEM_ID));
			itemJson.put("name", itemEle.getAttribute(A_ITEM_DESC));
			itemJson.put("description", primaryInfoEle.getAttribute(A_DESCRIPTION));
			itemJson.put("quantity", quantity);
			itemJson.put("unit_price", strUnitPrice);
			//Defect#EOMS-4316 taking it From Item/ExtnImageURL
			itemJson.put("item_image", extnImageURL);
			itemJson.put("item_url", primaryInfoEle.getAttribute(A_IMAGE_LOCATION));
			itemJson.put("fulfillment_status", A_VAL_SHIPPED);
			itemJson.put("is_final_sale", A_TRUE);
			itemJson.put("categories", new JSONArray().put("Footwear"));
			//EOMS-6306 start
			String sizeCode=primaryInfoEle.getAttribute(A_SIZE_CODE);
			String colorCode=primaryInfoEle.getAttribute(A_COLOR_CODE);

			itemJson.put("color", colorCode);
			itemJson.put("size", sizeCode);
			//EOMS-6306 END
			orderItemsArray.put(itemJson);

			//
			JSONObject itemShipJson = new JSONObject();
			itemShipJson.put("quantity", quantity);
			itemShipJson.put("sku", itemEle.getAttribute(A_ITEM_ID));
			itemsInfoForShipment.put(itemShipJson);

			//EOMS-6306 Start
			String extnSAPMaterialGroup=extnItemEle.getAttribute(EXTN_SAP_MATERIAL_GROUP);
			JSONObject attributes = new JSONObject();
			attributes.put("extn_sap_material_id", extnSAPMaterialGroup);
			itemJson.put("attributes",attributes);
			//EOMS-6306 End

		}

		orderInfo.put("order_items", orderItemsArray);

		String formattedShipDate = shipDate.substring(0, 19) + "Z"; // strip milliseconds and timezone offset
		Element personInfoShipTo = SCXmlUtil.getChildElement(orderDetails, E_PERSON_INFO_SHIP_TO);

		JSONObject shippedTo = new JSONObject();
		//keep this in address object
		JSONObject shippingAddress = new JSONObject();
		shippingAddress.put("street_1", personInfoShipTo.getAttribute(A_ADDRESS_LINE_1));
		shippingAddress.put("street_2", personInfoShipTo.getAttribute(A_ADDRESS_LINE_2));
		shippingAddress.put("city", personInfoShipTo.getAttribute(A_CITY));
		shippingAddress.put("state", personInfoShipTo.getAttribute(A_STATE));
		shippingAddress.put("zip", personInfoShipTo.getAttribute(A_ZIPCODE));
		shippingAddress.put("country", personInfoShipTo.getAttribute(A_COUNTRY));

		shippedTo.put("first_name", personInfoShipTo.getAttribute(A_FIRST_NAME));
		shippedTo.put("last_name", personInfoShipTo.getAttribute(A_LAST_NAME));
		shippedTo.put("phone", personInfoShipTo.getAttribute(A_DAY_PHONE));
		shippedTo.put("email", personInfoShipTo.getAttribute(A_EMAIL_ID));
		shippedTo.put("address", shippingAddress);

		JSONArray shipmentsArray = new JSONArray();
		JSONObject shipment = new JSONObject();

		shipment.put("ship_source", A_VAL_DC);
		shipment.put("items_info", itemsInfoForShipment);
		shipment.put("carrier", carrierName);
		shipment.put("shipped_to", shippedTo);
		shipment.put("ship_date", formattedShipDate);
		shipment.put("tracking_number",trackingNo);

		shipmentsArray.put(shipment);
		orderInfo.put("shipments", shipmentsArray);

		// form Billing Object
		Element personInfoBillTo = SCXmlUtil.getChildElement(orderDetails, E_PERSON_INFO_BILL_TO);
		if (personInfoBillTo != null) {
			JSONObject billing = new JSONObject();
			JSONObject billedTo = new JSONObject();
			JSONObject billingAddress = new JSONObject();

			billingAddress.put("street_1", personInfoBillTo.getAttribute(A_ADDRESS_LINE_1));
			billingAddress.put("street_2", personInfoBillTo.getAttribute(A_ADDRESS_LINE_2));
			billingAddress.put("city", personInfoBillTo.getAttribute(A_CITY));
			billingAddress.put("state", personInfoBillTo.getAttribute(A_STATE));
			billingAddress.put("zip", personInfoBillTo.getAttribute(A_ZIPCODE));
			billingAddress.put("country", personInfoBillTo.getAttribute(A_COUNTRY));

			billedTo.put("first_name", personInfoBillTo.getAttribute(A_FIRST_NAME));
			billedTo.put("last_name", personInfoBillTo.getAttribute(A_LAST_NAME));
			billedTo.put("phone", personInfoBillTo.getAttribute(A_DAY_PHONE));
			billedTo.put("email", personInfoBillTo.getAttribute(A_EMAIL_ID));
			billedTo.put("address", billingAddress);

			billing.put("billed_to", billedTo);
			orderInfo.put("billing", billing);
		}

		// form Customer Object from ShipTo details
		JSONObject customer = new JSONObject();
		JSONObject customerAddress = new JSONObject();

		customerAddress.put("street_1", personInfoShipTo.getAttribute(A_ADDRESS_LINE_1));
		customerAddress.put("street_2", personInfoShipTo.getAttribute(A_ADDRESS_LINE_2));
		customerAddress.put("city", personInfoShipTo.getAttribute(A_CITY));
		customerAddress.put("state", personInfoShipTo.getAttribute(A_STATE));
		customerAddress.put("zip", personInfoShipTo.getAttribute(A_ZIPCODE));
		customerAddress.put("country", personInfoShipTo.getAttribute(A_COUNTRY));

		customer.put("first_name", personInfoShipTo.getAttribute(A_FIRST_NAME));
		customer.put("last_name", personInfoShipTo.getAttribute(A_LAST_NAME));
		customer.put("phone", personInfoShipTo.getAttribute(A_DAY_PHONE));
		customer.put("email", personInfoShipTo.getAttribute(A_EMAIL_ID));
		customer.put("address", customerAddress);

		orderInfo.put("customer", customer);

		orderJson.put("order_info", orderInfo);

		logger.verbose("Final Json payload is "+orderJson.toString());

		return orderJson;
	}


	/**
	 * This method is checking if Sales order is completely Shipped
	 * @param env
	 * @param orderDetails : Sales order details
	 * @return isOrderCompletelyShipped boolean flag
	 */
	private Boolean isOrderCompletelyShipped(YFSEnvironment env, Element orderDetails){

		logger.verbose("Starts of method isOrderCompletelyShipped() with order: "+SCXmlUtil.getString(orderDetails));
		boolean isOrderFullyShipped = false;
		String maxOrderStatus = "";
		String minOrderStatus = "";

		maxOrderStatus = orderDetails.getAttribute(A_MAX_ORDER_STATUS);
		minOrderStatus = orderDetails.getAttribute(A_MIN_ORDER_STATUS);
		logger.verbose("Value of maxOrderStatus "+maxOrderStatus);
		logger.verbose("Value of minOrderStatus "+minOrderStatus);

		if(!YFCCommon.isVoid(maxOrderStatus) && !YFCCommon.isVoid(minOrderStatus)
				&& maxOrderStatus.equalsIgnoreCase(STATUS_SHIPPED) && minOrderStatus.equalsIgnoreCase(STATUS_SHIPPED)){
			isOrderFullyShipped = true;
			logger.verbose("updating isOrderFullyShipped flag as true");
		}

		logger.verbose("Value of isOrderFullyShipped is : "+isOrderFullyShipped);

		return isOrderFullyShipped;

	}

	/**
	 * Preparing Narvar connection details and calling their services
	 * via REST
	 * @param env
	 * @param orderDetails: Sales/return order details
	 * @param orderJson: Sales or return order json paylaod
	 * @param isSOShipmentUpdate: is call for Shipment update to Narvar
	 * @param isReturnUpdate: is call for Store return update to Narvar
	 */
	private void narvarRestAPICallToPostUpdate(YFSEnvironment env, Element orderDetails, JSONObject orderJson,boolean isSOShipmentUpdate,
											   boolean isReturnUpdate){

		logger.verbose("Starts of method narvarRestAPIConnection() with orderDetails: "+SCXmlUtil.getString(orderDetails));
		logger.verbose("Starts of method narvarRestAPIConnection() with json payload: "+orderJson.toString());
		logger.verbose("Starts of method narvarRestAPIConnection() with isSOShipmentUpdate: "+isSOShipmentUpdate);
		logger.verbose("Starts of method narvarRestAPIConnection(): with isReturnUpdate: " +isReturnUpdate);

		//call Rest Service
		HttpURLConnection connection = null;

		try{

			String sUrl = "";
			//Posting Sales order update to Narvar once order is fully Shipped
			if(isSOShipmentUpdate){
				// Setup URL and connection
				sUrl = YFCConfigurator.getInstance().getProperty(CROCS_NARVAR_API_FOR_SALES);
			}else if(isReturnUpdate){
				//Posting APTOS Return(Store returns) update to Narvar
				// Setup URL and connection
				sUrl = YFCConfigurator.getInstance().getProperty(CROCS_NARVAR_API_FOR_RETURN);
			}

			logger.verbose("URL is: "+sUrl);
			URI uri = new URI(sUrl);
			java.net.URL url = uri.toURL();
			connection = (HttpURLConnection) url.openConnection();
			// Dynamically fetch the API key from SMA for the user name
			String strUserName = YFCConfigurator.getInstance().getProperty(CROCS_NARVAR_USERNAME);
			String strPassword = YFCConfigurator.getInstance().getProperty(CROCS_NARVAR_PASSWORD);
			String encodedAuth = Base64.getEncoder()
					.encodeToString((strUserName + ":"+ strPassword).getBytes(StandardCharsets.UTF_8));
			connection.setRequestProperty(CrocsConstant.A_AUTHORIZATION, CrocsConstant.BASIC + encodedAuth);
			//POST Method
			connection.setRequestMethod(CrocsConstant.HTTP_POST_REQUEST);
			String strContentType=YFCConfigurator.getInstance().getProperty(A_CONTENT_TYPE);
			connection.setRequestProperty(CrocsConstant.CONTENT_TYPE, strContentType);
			//connection.setRequestProperty(CrocsConstant.API_VERSION, CrocsConstant.API_VERSION_V);
			connection.setDoOutput(true);
			connection.connect();

			if (!YFCCommon.isVoid(orderJson)) {
				String strPostBody = orderJson.toString();
				byte[] postData = strPostBody.getBytes(StandardCharsets.UTF_8);
				OutputStream outputStream = connection.getOutputStream();
				outputStream.write(postData);
				outputStream.flush();

			}

			// Get the response code and handle the response
			int responseCode = connection.getResponseCode();
			logger.verbose("responseCode of Narvar update call is: "+responseCode);
			// prepare response body
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder responseBody = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				responseBody.append(inputLine);
			}
			in.close();
			logger.verbose("responseBody of Narvar Update call is: "+responseBody);
			logger.info("OMS_Update : CrocsSendOrderUpdatesToNarvar.narvarRestAPICallToPostUpdate() : ResponseBody of Narvar Update call is:" +  responseBody);

		}catch(YFSException | URISyntaxException | IOException e){
			logger.verbose("Exception in Narvar Rest API Call for Sending updating: "+e.getLocalizedMessage());
		}finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

	}

}