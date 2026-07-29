package com.crocs.oms.pricing;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * EOMS-1444 This class covers logic to
 * Make REST API call to SFCC() to fetch item price
 * It has logic to make URL and make connection with SFCC
 * and returns UnitPrice of the item
 * This is invoked from Service
 * 
 * @author IBM
 *
 */
public class CrocsGetItemPriceFromSFCC implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetItemPriceFromSFCC.class);


	/**
	 * 
	 * @param env
	 * @param indoc
	 * @return
	 * 
	 * Sample input to method
	 * 
		<?xml version="1.0" encoding="UTF-8"?>
		<ItemList CallingOrganizationCode="CROCS_CA" Currency="CAD" PriceProgramKey="" PriceProgramName="" PricingDate="">
		   <Item CanUseAsServiceTool="N" GlobalItemID="" ItemGroupCode="PROD" ItemID="40003-001-M21" 
		   ItemKey="20250131085844354631" OrganizationCode="CROCS_NA" UnitOfMeasure="EACH" />
		</ItemList>

	 */

	public Document fetchItemPriceFromSFCC(YFSEnvironment env, Document indoc) {

		logger.info("CrocsGetItemPriceFromSFCC:fetchItemPriceFromSFCC() Starts::");
		logger.verbose("Start of method fetchItemPriceFromSFCC:CrocsGetItemPriceFromSFCC() with input: "
				+ SCXmlUtil.getString(indoc));
		/**
		 * 
		 * This will be called from CrocsGetExternalPricesForItemListUEImpl
		 * class as part of User Exit YFSGetExternalPricesForItemListUE
		 * implementation Will take itemID from input here and pass it to SFCC
		 * call while updating in URL
		 *
		 */
		Element itemListEle = indoc.getDocumentElement();
		ArrayList<Element> itemList = SCXmlUtil.getChildren(itemListEle, E_ITEM);
		logger.verbose("No of Items has been pass from OMS Item search to SFCC Service: " + itemList.size());

		if (!itemList.isEmpty()) {
			HttpsURLConnection conn = null;
			try {
				logger.verbose("No of Items has been pass from OMS Item search to SFCC Service: " + itemList.size());

				for (Element item : itemList) {

					logger.verbose("Item element for which OMS will be making call to SFCC " + SCXmlUtil.getString(item));
					String itemID = item.getAttribute(A_ITEM_ID);
					logger.verbose("Item ID for which OMS is making a call to SFCC is: " + itemID);

					String strSFCItemPriceCUrl = YFSSystem.getProperty(SFCC_API_URL_TO_FETCH_ITEM_PRICE);
					//Client secert credentail
					String strSFCCClientID = YFSSystem.getProperty(SFCC_CLIENT_ID_TO_FETCH_ITEM_PRICE);
					logger.verbose("SFCC Item Price URL is: "+strSFCItemPriceCUrl);
					logger.verbose("SFCC Item Price client id is: "+strSFCCClientID);

					if(!YFCCommon.isVoid(strSFCItemPriceCUrl) && !YFCCommon.isVoid(strSFCCClientID)){

						// preparing SFCC URL by Concating Two SMA property and one constant as it was failing in prod
						String sfccURL = strSFCItemPriceCUrl  + strSFCCClientID;
						logger.verbose("SFCC Item Price URL Post Concat of URL And Client id is: "+sfccURL);

						// Replacing ITEMID from above URL with actual ITEMID
						String urlStr = sfccURL.replace("ITEMID", itemID);
						logger.verbose("Making Get Call with URL after updating ItemID: "+urlStr);

						// making connection
						URL url = new URL(urlStr);
						conn = (HttpsURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						logger.verbose("Making connection with GET method ");

						conn.connect();
						logger.verbose("conn.connect(): done");

						int status = conn.getResponseCode();
						logger.verbose("StatusCode value of the response from SFCC Item price service call is : " + status);

						// Success response from SFCC
						if (status == Integer.parseInt(A_STATUS_CODE_SUCCESS)) {
							logger.verbose("SFCC has successfully returned price response for item: " + itemID);

							// updating unit price which came from SFCC
							updateItemWithUnitPriceValue(env, item, conn);
							logger.verbose(
									"Updated item element post appending price details is:" + SCXmlUtil.getString(item));

						} else {
							logger.verbose("SFCC Item Price call has failed with status code: " + status);

							// updating unit price as blank
							updateItemUnitPricePostSFCCFailureResponse(env, item, conn);
							logger.verbose("Updated item element with blank unit price is :" + SCXmlUtil.getString(item));
						}
					}else{
						//either URL or ClientID is blank
						logger.verbose("Either SFCC Item Price URL or Client ID is blank and not configured in SMA");
					}

				}
			} catch (Exception e) {
				logger.info("Error LocalizedMessage: CrocsGetItemPriceFromSFCC:fetchItemPriceFromSFCC():: " +e.getLocalizedMessage());
				logger.info("Error Stack Trace message: CrocsGetItemPriceFromSFCC:fetchItemPriceFromSFCC():: " +e.getStackTrace());
				logger.info("Error Message: CrocsGetItemPriceFromSFCC:fetchItemPriceFromSFCC():: "+e.getMessage());
				logger.verbose("Error while making a REST API Call to Get Item price from SFCC in fetchItemPriceFromSFCC():: "
								+ e.getMessage());

			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
		}
		logger.verbose("Final output document of fetchItemPriceFromSFCC:CrocsGetItemPriceFromSFCC is: "
				+ SCXmlUtil.getString(indoc));

		return indoc;
	}



	/**
	 * When SFCC service is  returning success response
	 * in OMS while returning data, updating unit price as
	 * actual price which is present in SFCC
	 * @param env
	 * @param itemDetailsEle
	 * @param conn
	 */
	private  void updateItemWithUnitPriceValue(YFSEnvironment env, Element itemDetailsEle, HttpsURLConnection conn){

		logger.verbose("Start of method updateItemWithUnitPriceValue with success out response for item: "
				+SCXmlUtil.getString(itemDetailsEle));

		try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder stringBuilder = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				logger.verbose("reading item price service response starts");
				stringBuilder.append(line+"\n");
			}
			logger.verbose("reading item price service response Stops");

			JSONObject jsonObj = new JSONObject(stringBuilder.toString());
			logger.verbose("Item Price response from SFCC for Item is: "+jsonObj.toString());

			Object jsonObj1 =	jsonObj.get("price");
			logger.verbose("object of item price: " +jsonObj1);
			/**
			 * Sample output from SFCC
			 * {
					"_v": "21.3",
					"_type": "product",
					"price": 54.99
				}
			 */

			String unitPrice = jsonObj1.toString();
			logger.verbose("Unit price of item returned by SFCC is "+unitPrice);

			//updating Input doc Item Element with ComputedPrice Element and UnitPrice attribute
			Element computedPriceEle = SCXmlUtil.createChild(itemDetailsEle, E_COMPUTED_PRICE);
			computedPriceEle.setAttribute(A_UNIT_PRICE, unitPrice);

		}catch(Exception exception){
			logger.verbose("Error while reading SFCC Item Price output response in updateItemWithUnitPriceValue" 
					+exception.getLocalizedMessage());
		}			

	}

	/**
	 * When SFCC service is not returning success response
	 * in OMS while returning data, updating unit price as
	 * blank value
	 * @param env
	 * @param item itemdetails
	 * @param conn httpconnection
	 */
	private  void updateItemUnitPricePostSFCCFailureResponse(YFSEnvironment env, Element item, 
			HttpsURLConnection conn){

		logger.verbose("Start of method updateItemUnitPricePostSFCCFailureResponse with item element :" 
				+SCXmlUtil.getString(item));

		/**
		 * below is sample response if item is not configured in SFCC
		 * 
		 * {
			  "_v": "21.3",
			  "fault": {
			    "arguments": {
			      "productId": "ITEMID",
			      "siteId": "crocs_us"
			    },
			    "type": "ProductNotFoundException",
			    "message": "No product with ID 'ITEMID' for site 'crocs_us' could be found."
			  }
			}
		 * 
		 */

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))){
			logger.verbose("Reading of error response starts for SFCC item price");
			StringBuilder erroResStringBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				erroResStringBuilder.append(line);
			}

			logger.verbose("Reading of error response Ends for SFCC item price");

			JSONObject jsonErrorObj = new JSONObject(erroResStringBuilder.toString());
			logger.verbose("Response Error message from SFCC Item price is: "+jsonErrorObj.toString());

			JSONObject faultJsonObj = jsonErrorObj.getJSONObject("fault");
			String type = faultJsonObj.getString("type");
			String message = faultJsonObj.getString("message");

			logger.verbose("Error Type of SFCC Item Price call is: " + type);
			logger.verbose("Error Mesaage of SFCC item Price call is: " + message);

			//updating Input doc Item Element with ComputedPrice Element and UnitPrice attribute
			Element computedPriceEle = SCXmlUtil.createChild(item, E_COMPUTED_PRICE);
			//setting unitPrice as 0.00 PMR# TS019321178 
			computedPriceEle.setAttribute(A_UNIT_PRICE, A_ZERO_UNIT_PRICE);

		}catch(Exception errorJsonException){
			errorJsonException.getLocalizedMessage();
			logger.verbose("Error  while reading failure response from SFCC Item Price Service in"
					+ " updateItemUnitPricePostSFCCFailureResponse: "+errorJsonException);
		}
	}

}