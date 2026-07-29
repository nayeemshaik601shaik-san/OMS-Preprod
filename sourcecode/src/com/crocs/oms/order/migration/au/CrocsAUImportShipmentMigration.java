package com.crocs.oms.order.migration.au;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.shipment.CrocsShipmentPackedToShippedStatusFromWMS;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsAUImportShipmentMigration implements CrocsConstant{
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsAUImportShipmentMigration.class.getName());


	/**
	 * <p> Hover over method for more details <p>
	  <p><b>Service Name:</b> CrocsImportShipmentAsync</p>
      <p><b>Purpose:</b> This service will consume XML from <code>CROCS_IMPORT_SHIPMENT_MIGRATION_QUEUE</code> 
      and perform data validation before importing the order shipment.</p>
      
      <p><b>Mandatory Attributes:</b> DocumentType, EnteredBy, EnterpriseCode, OrderNo, 
       PipelineKey, ItemID, ToAddress</p>
      
      <p><b>Input:</b> Migration Sales Order Shipment sample XML</p>
      <pre>{@code
	    
	    <?xml version="1.0" encoding="UTF-8"?>
<Shipment ActualDeliveryDate=""
    ActualShipmentDate="2025-04-05T08:15:47.000+0000"
    BillToCustomerId="" BuyerOrganizationCode=""
    CarrierServiceCode="Economy" Currency="USD" DeliveryTS=""
    DocumentType="0001" EnteredBy="Migration" EnterpriseCode="CROCS_AU"
    ExpectedDeliveryDate=""
    ExpectedShipmentDate="2025-04-05T08:15:47.000+0000" History="N"
    OrderNo="AULOC000016" PipelineKey="202504031215582621376"
    ReceivingNode="" ReleaseNo="" SellerOrganizationCode="CROCS_AU"
    ShipDate="2025-04-05" ShipNode="3011" ShipToCustomerId=""
    ShipmentKey="" ShipmentNo="AULOC000016_1" Status="1400" TrackingNo="1Z6F857YYW260869975|UPS">
    <FromAddress AddressLine1="484 NW 53rd Ave" AddressLine2=""
        City="Miami" Country="US" EMailID="nannasolano@gmail.com"
        FirstName="PRIYANKA" LastName="BAGLA"
        MobilePhone="(178) 626-2445" State="FL" ZipCode="33126-5040"/>
    <ToAddress AddressLine1="123 S COLLINGWOOD ST" AddressLine2=""
        City="PRETTY PRAIRIE" Country="US"
        EMailID="PRIYANKA.BAGLA@IBM.COM" FirstName="PRIYANKA"
        LastName="BAGLA" OtherPhone="(333) 333-3333" PersonID=""
        State="KS" ZipCode="67570-8923"/>
    <ShipmentLines>
        <ShipmentLine
            ItemDesc="original, classic clog, classic, crocs, jjjjound, baya"
            ItemID="10006-001-M20" NetWeight="50" NetWeightUom="LB"
            OrderHeaderKey="" OrderLineKey="" OrderNo="AULOC000016"
            Quantity="2" ShipmentLineNo="1" UnitOfMeasure="EACH"/>
        <ShipmentLine
            ItemDesc="original, classic clog, classic, crocs, jjjjound, baya"
            ItemID="10006-001-M44" NetWeight="50" NetWeightUom="LB"
            OrderHeaderKey="" OrderLineKey="" OrderNo="AULOC000016"
            Quantity="1" ShipmentLineNo="2" UnitOfMeasure="EACH"/>
    </ShipmentLines>
</Shipment>
     * }
	 * @param env - YFSEnvironment
	 * @param importShipmentInDoc - Document
	 * @return - Output of importShipment Api.
	 * @throws Exception 
	 */
	
	
    /*
     * Service Name: CrocsSalesOrderImportAsync</p>
     * Purpose: This service will consume XML from CROCS_IMPORT_SHIPMENT_MIGRATION_QUEUE and perform data validation before importing the order
     * Mandatory Attributes: DocumentType, EnteredBy, EnterpriseCode, OrderNo, PipelineKey, ItemID, ToAddress
     */
       
	
	public Document importShipment(YFSEnvironment env, Document importShipmentInDoc) throws Exception {
		
		logger.beginTimer(CrocsAUMigarationUtil.logCurrentMethod(this.getClass()));
		try {
			YFCDocument importShipmentYdoc = YFCDocument.getDocumentFor(importShipmentInDoc);
			YFCElement importShipmentYdocEle = importShipmentYdoc.getDocumentElement();

			String importShipmentMandatoryAttr[] = { CrocsConstant.A_DOCUMENT_TYPE,
					CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER, CrocsConstant.A_ENTERED_BY,
					CrocsConstant.A_ENTERPRISE_CODE, CrocsConstant.A_ORDER_NO,
					CrocsConstant.A_SO_SHIPMENT_MIGRATION_CROCS_AU_PIPELINE_PROPERTY };

			boolean isValidForImportOrderProcesingFlag = isValidForImportShipmentProcesing(importShipmentYdocEle,
					importShipmentMandatoryAttr);
			
			if (isValidForImportOrderProcesingFlag) {
				
				CrocsAUMigarationUtil.updateCarrierServiceCode(env,importShipmentYdocEle,importShipmentYdocEle.getAttribute(CrocsConstant.EnterpriseCode));
				importSOProcesingReadiness(env,importShipmentYdocEle);
			}
			
			logger.verbose("CrocsImportShipmentMigration :: importShipment :: Input Doc for importShipment: \n"
					+ importShipmentYdoc.toString());
			
		}catch(YFSException e) {
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}

		logger.endTimer(CrocsAUMigarationUtil.logCurrentMethod(this.getClass()));
		return importShipmentInDoc;
	}

	private void importSOProcesingReadiness(YFSEnvironment env, YFCElement importShipmentYdocEle) throws Exception {
		try {
			String strOrderNo = importShipmentYdocEle.getAttribute(CrocsConstant.A_ORDER_NO);
			int shipmentLineNo = 1;
			String customerLocale="";
			
			Document getOrderListOutput = CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_GET_ORDER_LIST_ORDER_EVENT_UPDATES,
					CrocsConstant.API_GET_ORDER_LIST,
					SCXmlUtil.createFromString("<Order OrderNo='" + strOrderNo + "'/>"));
			
			if (!CrocsAUMigarationUtil.isValidSalesOrder(getOrderListOutput)) {
				throw new YFSException("Invalid Sales Order [" + strOrderNo + "] Provided for importShipment", "",
						"Sales Order does not exist \n " + SCXmlUtil.getString(getOrderListOutput));
			} else {
				YFCNodeList<YFCElement> golOrderLines = YFCDocument.getDocumentFor(getOrderListOutput)
						.getDocumentElement().getElementsByTagName(CrocsConstant.E_ORDER_LINE);

				YFCNodeList<YFCElement> shipmentLines = importShipmentYdocEle.getChildElement("ShipmentLines")
						.getElementsByTagName(CrocsConstant.E_SHIPMENT_LINE);
				
				//EOMS-5182 :: URL Changes : START
				Element orderListOutEle = getOrderListOutput.getDocumentElement();
				Element orderDetails = SCXmlUtil.getChildElement(orderListOutEle, E_ORDER);
				if(SCXmlUtil.getChildElement(orderDetails, E_EXTN)!=null) {
					Element eleOrderExtn = SCXmlUtil.getChildElement(orderDetails, E_EXTN);
					customerLocale = eleOrderExtn.getAttribute(EXTN_CUSTOMER_LOCALE);
				}
				//EOMS-5182 :: URL Changes : END
				
				YFCDocument organizationYDoc = YFCDocument.createDocument(E_ORGANIZATION);
				YFCElement organizationYDocEle = organizationYDoc.getDocumentElement();

				for (YFCElement shipmentLine : shipmentLines) {
					String shipmentLineitemId = shipmentLine.getAttribute(CrocsConstant.A_ITEM_ID);
					
					for (YFCElement golOrderLine : golOrderLines) {
						if (shipmentLineitemId.equals(golOrderLine.getChildElement("Item").getAttribute(CrocsConstant.A_ITEM_ID))) {

							String strOrderLineKey = golOrderLine.getAttribute(CrocsConstant.A_ORDER_LINE_KEY);
							shipmentLine.setAttribute(CrocsConstant.A_ORDER_LINE_KEY, strOrderLineKey);
							String strOrderHeaderKey = golOrderLine.getAttribute(CrocsConstant.A_ORDER_HEADER_KEY);
							shipmentLine.setAttribute(CrocsConstant.A_ORDER_HEADER_KEY, strOrderHeaderKey);
							
							shipmentLine.setAttribute(CrocsConstant.A_SHIPMENT_LINE_NO, shipmentLineNo++);
							shipmentLine.setAttribute(CrocsConstant.OrderNo, strOrderNo);
							shipmentLine.setAttribute(CrocsConstant.A_UNIT_OF_MEASURE, "EACH");
							
							
							/*Appending Container Details to shipment*/
							
							YFCElement containers = importShipmentYdocEle.createChild(CrocsConstant.E_CONTAINERS);
							YFCElement container =  containers.createChild(CrocsConstant.E_CONTAINER);
							
							String containerNo = importShipmentYdocEle.getAttribute(CrocsConstant.OrderNo).concat(shipmentLine.getAttribute(CrocsConstant.A_SHIPMENT_LINE_NO));
							container.setAttribute(CrocsXmlConstants.A_CONTAINER_NO,containerNo);
							
							String trackingNo = importShipmentYdocEle.getAttribute(CrocsConstant.A_TRACKING_NO);
							
							/** As we have observed there could be two pattren for trackingNo
							 * pattern 01 : &#xa; 1Z6F857YYW86116475|UPS&#xa;
							 * Pattern 02 : &#xa; 1Z6F857YYW86116475|UPS&#xa; 1Z6F857YYW86143767|UPS&#xa;
							 * Pattern 03 : &#xA; 9200190383434300018479&#xA; 1Z6F857YYN36114041&#xA;
							 * Pattern 04 : &#10; 803356510503233324&#10; 9200190383434300020854&#10;
							 * Pattern 05 : &#xa; 920043015566603 &#xa;
							 * In pattern 02,03,04 data in the column is exceeding the set limit, so it needs to be trimmed. 
							 * 
							 * As we are extracting the tracking no and scac using split by "|" , below line will help delealing with all above pattern 
							 * trackingNo.trim().split("\\s")[0];
							 * 
							 * In all above patern we will consider the first set of data.
							 * In pattern 03,04 and 05,we are receiving tracking no without scac and "|",so this is handled in else loop
							 * 
							 **/
							 
							if (!YFCCommon.isVoid(trackingNo)) {	
								trackingNo = trackingNo.trim().split("\\s")[0];
								importShipmentYdocEle.setAttribute(CrocsConstant.A_TRACKING_NO,trackingNo);
							}
							
							String scac = "", strPrimaryUrl="";
							container.setAttribute(CrocsConstant.A_TRACKING_NO,importShipmentYdocEle.getAttribute(CrocsConstant.A_TRACKING_NO));
							container.setAttribute(CrocsConstant.A_SCAC,"");
							
							if (!YFCCommon.isVoid(trackingNo)) {
								
								if (trackingNo.contains("|")) {
									String[] trackingNoSplits = trackingNo.split("\\|");

									scac = trackingNoSplits[1].trim();
									trackingNo = trackingNoSplits[0].trim();
									
									container.setAttribute(CrocsConstant.A_TRACKING_NO, trackingNo);
									container.setAttribute(CrocsConstant.A_SCAC,scac);

									CrocsShipmentPackedToShippedStatusFromWMS obj = new CrocsShipmentPackedToShippedStatusFromWMS();
									Document OrgCode = obj.updateSCAC(env, SCXmlUtil.createDocument(A_COMMON_CODE), trackingNoSplits[1]);
									Element outEleCC = OrgCode.getDocumentElement();
									String scacValue = SCXmlUtil.getXpathAttribute(outEleCC,
											XPATH_CODE_SHORT_DESCRIPTION);
									String strSCACandService = SCXmlUtil.getXpathAttribute(outEleCC,
											XPATH_CODE_LONG_DESCRIPTION);

									organizationYDocEle.setAttribute(A_ORGANIZATION_CODE, scacValue);
									logger.verbose(
											"Calling getOrganizationList with input: " + organizationYDoc.toString());
									Document getOrganizationListOutDoc = CommonUtil.invokeAPI(env,
											TEMPLATE_GET_ORGANIZATION_LIST, API_GET_ORGANIZATION_LIST,
											organizationYDoc.getDocument());
									logger.verbose("Output returned from getOrganizationList: "
											+ SCXmlUtil.getString(getOrganizationListOutDoc));

									strPrimaryUrl = SCXmlUtil.getXpathAttribute(
											getOrganizationListOutDoc.getDocumentElement(), XPAH_PRIMARY_URL);
									
									//EOMS-5182 :: URL Changes as per Locale : START
//									if (!YFCCommon.isVoid(customerLocale) && STR_FR_CA.equalsIgnoreCase(customerLocale)) {
//										strPrimaryUrl = CommonUtil.updatePrimaryURLAsPerLocale(strPrimaryUrl,customerLocale,scacValue,strOrderNo);
//									}
									//EOMS-5182 :: URL Changes as per Locale : END
									
									if (!YFCCommon.isVoid(strPrimaryUrl))
									strPrimaryUrl = strPrimaryUrl.replaceAll(A_TRACKING_NO, trackingNoSplits[0].trim().replaceAll("&", "&amp;"));
									
									YFCElement extnContainer = container.createChild(E_EXTN);
									extnContainer.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
								}else{
									logger.verbose("trackingNo in else block is: " +trackingNo);
									container.setAttribute(CrocsConstant.A_TRACKING_NO, trackingNo);
								}
								
							}
							
							YFCElement containerDetails = container.createChild(CrocsConstant.E_CONTAINER_DETAILS);
							YFCElement containerDetail = container.createChild(CrocsConstant.E_CONTAINER_DETAIL);
							
							containerDetail.setAttribute(CrocsConstant.A_QUANTITY,shipmentLine.getAttribute(CrocsConstant.A_QUANTITY));
							
							YFCElement containerShipmentLine = containerDetail.createChild(CrocsConstant.E_SHIPMENT_LINE);
							containerShipmentLine.setAttribute(CrocsConstant.A_QUANTITY, shipmentLine.getAttribute(CrocsConstant.A_QUANTITY));
							containerShipmentLine.setAttribute(CrocsConstant.A_SHIPMENT_LINE_NO, shipmentLine.getAttribute(CrocsConstant.A_SHIPMENT_LINE_NO));
						}
					}
				}
			}

		} catch (YFSException e) {
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}
	}

	/**
     * @param importOrderYdocEle - YFCElement of Order
     */
	private boolean isValidForImportShipmentProcesing(YFCElement importShipmentYdocEle,
			String[] importShipmentMandatoryAttr) {

			logger.beginTimer(CrocsAUMigarationUtil.logCurrentMethod(this.getClass()));

			boolean isValid = false;
			for (String ImportOrderMandatoryAttrtribute : importShipmentMandatoryAttr) {

				if (CrocsAUMigarationUtil.validateMandatoryAttribute(importShipmentYdocEle,
						ImportOrderMandatoryAttrtribute))
					isValid = true;
				else
					break;
			}
			logger.endTimer(CrocsAUMigarationUtil.logCurrentMethod(this.getClass()));
			return isValid;
		}
}
