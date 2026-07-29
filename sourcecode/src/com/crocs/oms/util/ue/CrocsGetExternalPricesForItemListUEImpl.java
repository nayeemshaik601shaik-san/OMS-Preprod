package com.crocs.oms.util.ue;

import java.rmi.RemoteException;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSGetExternalPricesForItemListUE;

/**
 * EOMS-1117 This class is written to cover scope for CC orders
 * This User exit is being invoked during item search on AddProducts
 * on Call center Create order screen.
 * Implementation of connecting with SFCC Item Price using REST API CALL
 * and send back item's unit price from SFCC response back to CC UI
 * 
 * 
 * @author IBM
 *
 */

public class CrocsGetExternalPricesForItemListUEImpl implements YFSGetExternalPricesForItemListUE,CrocsConstant  {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetExternalPricesForItemListUEImpl.class);

	/**
	 * This method gets invoke as part of getCompleteItemList API call
	 * when any product is searched on Call Center.
	 *  
	 *  OMS connects with SFCC item price service and get item's unit price
	 *  and send back to CC UI
	 */
	@Override
	public Document getExternalPricesForItemList(YFSEnvironment env, Document inputDoc) throws YFSUserExitException {

		logger.verbose("Input to CrocsGetExternalPricesForItemListUEImpl:CrocsGetExternalPricesForItemListUEImpl to"
				+ " is  "+SCXmlUtil.getString(inputDoc));

		Element inputElement = inputDoc.getDocumentElement();//ItemList
		ArrayList<Element> itemsList = SCXmlUtil.getChildren(inputElement, E_ITEM);
		logger.verbose("No of Items in input: "+itemsList.size());

		try {
			//Calling SFCC Item Price Service via Rest Call to fetch item's price
			Document itemPriceSfccOutDoc = CommonUtil.invokeService(env, CROCS_GET_ITEM_PRICE_FROM_SFCC_SYNC_SERV, inputDoc);

			logger.verbose("Output from SFCC Item Price Service is :"+SCXmlUtil.getString(itemPriceSfccOutDoc));
		} catch (RemoteException exc) {

			logger.verbose("Error in getExternalPricesForItemList.CrocsGetExternalPricesForItemListUEImpl "
					+ "while making REST API call to SFCC" +exc.getLocalizedMessage());

		}

		logger.verbose("output from CrocsGetExternalPricesForItemListUEImpl Userxit is "+SCXmlUtil.getString(inputDoc));
		return inputDoc;
	}
}
