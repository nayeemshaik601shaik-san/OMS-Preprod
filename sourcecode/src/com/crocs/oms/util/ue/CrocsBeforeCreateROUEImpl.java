package com.crocs.oms.util.ue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSBeforeCreateOrderUE;

public class CrocsBeforeCreateROUEImpl implements CrocsConstant, YFSBeforeCreateOrderUE {
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsBeforeCreateROUEImpl.class);

	@Override
	public String beforeCreateOrder(YFSEnvironment yfsEnvironment, String s) throws YFSUserExitException {
		return null;
	}

	/**
	 * This logic is invoked during Return Order creation from Call Center (CC)
	 * EOMS-8402 : for HD CC return - need to modify it to be redirected to LVDC. 
	 * 
	 * @param env
	 * @param inDoc
	 * @return inDoc
	 * @throws YFSUserExitException
	 */
	@Override
	public Document beforeCreateOrder(YFSEnvironment env, Document inDoc) throws YFSUserExitException {
		logger.beginTimer("CrocsBeforeCreateROUEImpl::beforeCreateOrder");
		logger.info("CrocsBeforeCreateROUEImpl : beforeCreateOrder : Input Document : " + XMLUtil.getXMLString(inDoc));

		Element orderEle = inDoc.getDocumentElement();
		String enterpriseCode = orderEle.getAttribute(A_ENTERPRISE_CODE);
		String entryType = orderEle.getAttribute(A_ENTRY_TYPE);
		String orderNo = orderEle.getAttribute(A_ORDER_NO);

		try {
			if(CrocsConstant.HEYDUDE_US.equalsIgnoreCase(enterpriseCode) && CrocsConstant.ENTRY_TYPE_CALL_CENTER.equalsIgnoreCase(entryType)) {
				//EOMS-8044, EOMS-8402
				/**
				 Purpose:- This Condition is to stamp the shipNode and take below decisions.
								if LVDCnode is active, LVDC ShipNode will be stamped..
								else Radial ShipNode will be stamped
								For HEYDUDE_US and HEYDUDE_US_MP, returns are expected to be redirected to LVDC when active.
							
							Another one for switch:-
							if LVDC is de-activatd, Return shipnode will always be radial, it will be accomplished just modifying the value in CommonCode.
				 
				 Operation:- If LVDCnode is active, fetch mapped ShipNode from
							  CommonCode configuration and route to LVDC.
				 **/
				
					logger.info("CrocsBeforeCreateROUEImpl : beforeCreateOrder : CallCenterReturn_GetShipNodeforReturn");
						
					String shipNode = CommonUtil.getShipNodeforReturn(env, enterpriseCode);
					orderEle.setAttribute(CrocsConstant.A_SHIP_NODE, shipNode);
					
			}
			//EOMS-10737 Start 
			/** This Condition is to stamp Singapore Maersk 3PL (3001) 
			 * when returns are created from OMS CC for CROCS_SG orders
			 **/
			else if(CrocsConstant.CROCS_SG.equalsIgnoreCase(enterpriseCode) && CrocsConstant.ENTRY_TYPE_CALL_CENTER.equalsIgnoreCase(entryType)) {								
					logger.info("CrocsBeforeCreateROUEImpl : beforeCreateOrder : CallCenterReturn_GetShipNodeforReturn");
					logger.info("order number : " + orderNo);		
					orderEle.setAttribute(CrocsConstant.A_SHIP_NODE, SG_RO_FULFILLMENT_NODE);		
			}
			//EOMS-10737 End
		} catch (Exception e) {
				throw new YFSException("CrocsBeforeCreateROUEImpl : beforeCreateOrder : Exception" + e.getMessage());
	        }
		
		 
		logger.info("CrocsBeforeCreateROUEImpl::beforeCreateOrder : Output Document: " + SCXmlUtil.getString(inDoc));
		logger.endTimer("CrocsBeforeCreateROUEImpl::beforeCreateOrder" );
		return inDoc;

	}

}