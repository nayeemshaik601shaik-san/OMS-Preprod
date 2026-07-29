package com.crocs.oms.node;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * This API is been created for EOMS-555 task. API creates store and respective
 * calendar details recieved from OIC, calls the OOB manageOrganizationHirarchy,
 * createCalendar and changeCalendar API’s to create/Update node and calendar
 * details.
 */
@SuppressWarnings("unused")
public class CrocsManageStoreOrgAPI implements CrocsConstant, CrocsXmlConstants {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsManageStoreOrgAPI.class);

	public static String strOrgId = "";

	/**
	 * manageStoreOrgDetails method calls manageOrganizationHierarchy api
	 * to create the store
	 * 
	 * @param env
	 * @param inDoc
	 */
	public void manageStoreOrgDetails(YFSEnvironment env, Document inDoc) throws Exception {

		Element eleUpdatedCalendarShifts = null;
		try {

			logger.verbose("CrocsManageStoreOrgAPI manageStoreOrgDetails InDoc: : " +
					SCXmlUtil.getString(inDoc));

			/**
			 * Fix for Task#EOMS-3864 starts
			 * Removing PaymentProcessingReqd & DefaultPaymentRuleId attributes from input
			 * and updating NodeType value from STORE to Store
			 */
			
			Element organizationEle = inDoc.getDocumentElement();
			organizationEle.setAttribute(A_DEFAULT_PAYMENT_RULE_ID, "");
			organizationEle.setAttribute(A_PAYMENT_PROCESSING_REQUIRED, NO);
			
			Element shipNodeEle = SCXmlUtil.getChildElement(organizationEle, E_NODE);
           
			/** EOMS-8586: Enhance SAP org feed by deriving CapacityOrgCode from parent org code **/
			String parentOrgCode= organizationEle.getAttribute(CrocsXmlConstants.A_PARENT_ORGANIZATION_CODE);
			organizationEle.setAttribute(CrocsXmlConstants.A_CAPACITY_ORG_CODE, parentOrgCode);				
			//End
			

			shipNodeEle.setAttribute(A_NODE_TYPE, A_NODE_TYPE_STORE);
			/**
			 * Fix for Task#EOMS-3864 Ends
			 */
			

			
			strOrgId = inDoc.getDocumentElement().getAttribute(A_ORGANIZATION_CODE);
			//START-EOMS-11895
			String localeCode = organizationEle.getAttribute(CrocsXmlConstants.A_LOCALE_CODE);
			if (YFCCommon.isVoid(localeCode) || CrocsConstant.CROCS_US.equals(parentOrgCode)) {
				inDoc.getDocumentElement().setAttribute(A_LOCALE_CODE, VAL_LOCALE_MST);
			}
			//END- EOMS-11895
			Element calendarElementFromInDoc = (Element) inDoc.getDocumentElement().getElementsByTagName(E_CALENDAR)
					.item(0);
			if (!YFCObject.isVoid(calendarElementFromInDoc)) {
				Element eleShifts = (Element) calendarElementFromInDoc.getElementsByTagName(E_SHIFTS)
						.item(0);
				eleUpdatedCalendarShifts = manageCalendarShifts(env, eleShifts);

				inDoc.getDocumentElement().removeChild(calendarElementFromInDoc);
			}
			logger.verbose("manageOrganizationHierarchy indoc: : " +
					SCXmlUtil.getString(inDoc));
			// call api manageOrganizationHierarchy
			CommonUtil.invokeAPI(env, "", CROCS_MANAGE_ORG_HIERCHY_API, inDoc);
			// create or update calendar
			if (!YFCObject.isVoid(eleUpdatedCalendarShifts)) {
				manageCalendar(env, eleUpdatedCalendarShifts);
			}
			// Post Org Message to IV Queue for Sync
			CommonUtil.invokeService(env, CROCS_POST_ORG_MSG_FOR_IV_SYNC_SERV, inDoc);
		} catch (Exception e) {
			logger.verbose("Exception while creating store and calendar details" +
					e.getMessage());
			throw e;
		}
	}

	/**
	 * manageCalendar method calls createCalendar or changeCalendar api
	 * to create or update the calendar
	 * 
	 * @param env
	 * @param calendarElementFromInDoc
	 */
	public void manageCalendar(YFSEnvironment env, Element eleUpdatedCalShifts) throws Exception {

		Document getCalendarListOutDoc = null;
		Document calendarInDoc = null;
		Document newCalendarInDoc = null;
		try {
			logger.verbose("manageCalendar method inDoc: : " +
					SCXmlUtil.getString(eleUpdatedCalShifts));
			// Build New Calendar
			newCalendarInDoc = buildNewCalandar(env, eleUpdatedCalShifts);
			// Build New Calendar

			getCalendarListOutDoc = getCalendarList(env,
					newCalendarInDoc.getDocumentElement().getAttribute(A_CALENDAR_ID));
			Element eleCalendar = (Element) getCalendarListOutDoc.getElementsByTagName(E_CALENDAR).item(0);

			// call changeCalendar api if calendar already exists with latest data using
			// "ResetAll" attribute. else call createCalendar
			if (!YFCObject.isVoid(eleCalendar)) {
				logger.verbose("changeCalendar newCalendarInDoc: : " +
						SCXmlUtil.getString(newCalendarInDoc));
				// call changeCalendar
				CommonUtil.invokeAPI(env, "", CROCS_CHANGE_CALENDAR_API, newCalendarInDoc);
			} else {

				logger.verbose("createCalendar newCalendarInDoc: : " +
						SCXmlUtil.getString(newCalendarInDoc));
				// call createCalendar
				CommonUtil.invokeAPI(env, "", CROCS_CREATE_CALENDAR_API, newCalendarInDoc);
			}
		} catch (Exception e) {
			logger.verbose("Exception while creating or updating store calendar details"
					+ e.getMessage());
			throw e;
		}

	}

	/**
	 * getCalendarList method calls getCalendarList api
	 * to check if calendar is already present in system
	 * 
	 * @param env
	 * @param calendarElementFromInDoc
	 * @return getCalendarListOutDoc
	 */
	public Document getCalendarList(YFSEnvironment env, String strCalendarId) throws Exception {

		Document getCalendarListOutDoc = null;
		Document getCalendarListInDoc = null;
		try {
			// Prepare getCalendarList API Input
			getCalendarListInDoc = SCXmlUtil.createDocument(E_CALENDAR);
			Element eleIngetCalendarList = getCalendarListInDoc.getDocumentElement();
			eleIngetCalendarList.setAttribute(A_CALENDAR_ID, strCalendarId);

			logger.verbose("getCalendarList InDoc: : " +
					SCXmlUtil.getString(getCalendarListInDoc));
			// call getCalendarList
			getCalendarListOutDoc = CommonUtil.invokeAPI(env, "", CROCS_GET_CALENDAR_LIST_API, getCalendarListInDoc);
			logger.verbose("getCalendarList OutDoc: : " +
					SCXmlUtil.getString(getCalendarListOutDoc));
		} catch (Exception e) {
			logger.verbose("Exception while calling getCalendarList API" +
					e.getMessage());
			throw e;
		}
		return getCalendarListOutDoc;
	}

	/**
	 * This method will build calendar xml
	 * will all required data
	 * 
	 * @param env
	 * @param calendarElementFromInDoc
	 * @return getCalendarListOutDoc
	 */
	public Document buildNewCalandar(YFSEnvironment env, Element eleUpdatedCalShifts) throws Exception {

		Document newCalendarInDoc = null;
		try {
			newCalendarInDoc = SCXmlUtil.createDocument();
			Element calendarEle = newCalendarInDoc.createElement(E_CALENDAR);
			calendarEle.setAttribute(A_ORGANIZATION_CODE, strOrgId);
			calendarEle.setAttribute(A_CALENDAR_ID, strOrgId.concat(VAL_CALENDAR_UNDS));
			calendarEle.setAttribute(A_CAL_DESC, strOrgId.concat(VAL_CALENDAR));
			newCalendarInDoc.appendChild(calendarEle);
			Element eleEffectivePeriods = newCalendarInDoc.createElement(E_EFFECTIVE_PERIODS);
			eleEffectivePeriods.setAttribute(A_RESET_ALL, YES);
			calendarEle.appendChild(eleEffectivePeriods);
			Element eleEffectivePeriod = newCalendarInDoc.createElement(E_EFFECTIVE_PERIOD);
			eleEffectivePeriod.setAttribute(A_EFFECTIVE_FROM_DATE, VAL_EFFECTIVE_FROM_DATE);
			eleEffectivePeriod.setAttribute(A_EFFECTIVE_TO_DATE, VAL_EFFECTIVE_TO_DATE);
			eleEffectivePeriods.appendChild(eleEffectivePeriod);

			Node nodetobeimporNode = eleUpdatedCalShifts;
			Node impotedNode = newCalendarInDoc.importNode(nodetobeimporNode, true);
			eleEffectivePeriod.appendChild(impotedNode);
			logger.verbose("Build New newCalendarDoc ::" + SCXmlUtil.getString(newCalendarInDoc));
		} catch (Exception e) {
			logger.verbose("Exception while calling getCalendarList API" +
					e.getMessage());
			throw e;
		}
		return newCalendarInDoc;
	}

	/**
	 * Adjusting Calendar shifts based on unque combination of shift start and end
	 * time
	 * 
	 * @param env
	 * @param calendarElementFromInDoc
	 * @return updated calendarElementFromInDoc
	 */
	public Element manageCalendarShifts(YFSEnvironment env, Element calendarElementFromInDoc) throws Exception {

		Document outputDoc = null;
		try {

			logger.verbose("manageCalendarShifts method inDoc: : " + SCXmlUtil.getString(calendarElementFromInDoc));
			Document eleInputDoc = XMLUtil.getDocumentFromElement(calendarElementFromInDoc);
			Element shiftNode = (Element) eleInputDoc.getElementsByTagName(E_SHIFT).item(0);
			NamedNodeMap attributes = shiftNode.getAttributes();

			// Group shifts by time
			Map<String, Map<String, String>> groupedShifts = new LinkedHashMap<>();
			for (int i = 0; i < attributes.getLength(); i++) {
				Node attr = attributes.item(i);
				String name = attr.getNodeName();
				String value = attr.getNodeValue();
				if (name.endsWith(A_SHIFT_START_TIME)) {
					String day = name.replace(A_SHIFT_START_TIME, "");
					groupedShifts.putIfAbsent(
							value + "-" + attributes.getNamedItem(day + A_SHIFT_END_TIME).getNodeValue(),
							new LinkedHashMap<>());
					groupedShifts.get(value + "-" + attributes.getNamedItem(day + A_SHIFT_END_TIME).getNodeValue()).put(
							day,
							value);
				}
			}

			outputDoc = SCXmlUtil.createDocument();
			Element shiftsEle = outputDoc.createElement("Shifts");
			outputDoc.appendChild(shiftsEle);
			int count = 1;
			for (Map.Entry<String, Map<String, String>> entry : groupedShifts.entrySet()) {
				String[] times = entry.getKey().split("-");
				String startTime = times[0];
				String endTime = times[1];

				Element shiftElement = outputDoc.createElement(E_SHIFT);
				shiftElement.setAttribute(A_SHIFT_NAME, VAL_GENERAL_SHIFT + count);
				shiftElement.setAttribute(A_SHIFT_START_TIME, startTime + ":00");
				shiftElement.setAttribute(A_SHIFT_END_TIME, endTime + ":00");

				for (String day : entry.getValue().keySet()) {
					shiftElement.setAttribute(day + VAL_VALID, YES);
				}
				shiftsEle.appendChild(shiftElement);
				count++;
			}
		} catch (Exception e) {
			logger.verbose("Exception while calling manageCalendarShift API" +
					e.getMessage());
			throw e;
		}
		logger.verbose("calendarElementFromInDoc post adjusting Shifts: : " +
				SCXmlUtil.getString(calendarElementFromInDoc));
		return outputDoc.getDocumentElement();
	}
}