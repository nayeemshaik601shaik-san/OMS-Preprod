package com.crocs.oms.order.migration.sg;

import com.crocs.oms.common.util.CommonUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;

import com.crocs.oms.common.util.CrocsConstant;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import org.w3c.dom.Element;

import java.math.BigDecimal;


public class CrocsSGReturnOrderMigration implements CrocsConstant {

    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsSGReturnOrderMigration.class.getName());

    /**
     * <p> Hover over method for more details <p>
     * <p><b>Service Name:</b> CrocsReturnOrderImportAsyncServ</p>
     * <p><b>Purpose:</b> This service will consume XML from <code>CROCS_INT_RETURN_MIGRATION_QUEUE</code>
     * and perform data validation before importing the return order.</p>
     *
     * <p><b>Mandatory Attributes:</b> DocumentType, EnteredBy, EnterpriseCode, OrderNo,
     * ConditionVariable1, PipelineKey</p>
     *
     * <p><b>Input:</b> Migration Return Order sample XML</p>
     * <pre>{@code
    <Order DocumentType="0003" EnteredBy="Migration" OrderNo="AAU000088RO" EnterpriseCode="CROCS_SG" OrderDate="2025-04-07T19:21:43+00:00" EntryType="WEB" CustomerEMailID="PBAGLA@CROCS.COM" CustomerFirstName="PRIYANKA" CustomerLastName="BAGLA" CustomerPhoneNo="(403) 718-5340" OtherCharges="6.99">
	<Extn ExtnSourceCodeGrpId="CrocsClub" ExtnIsCrocsClub="N" ExtnCustId="cust123" ExtnFraudStatus="Y"/>
	<PriceInfo Currency="SGD" EnterpriseCurrency="AUD"  TotalAmount="69.13"/>
	<PersonInfoShipTo LastName="BAGLA" FirstName="PRIYANKA" AddressLine1="123 S COLLINGWOOD ST" City="PRETTY PRAIRIE" State="KS" Country="SG" ZipCode="2260" DayPhone="(333) 333-3333" EMailID="PBAGLA@CROCS.COM"/>
	<PersonInfoBillTo LastName="BAGLA" FirstName="PRIYANKA" AddressLine1="123 S COLLINGWOOD ST" City="PRETTY PRAIRIE" State="KS" Country="SG" ZipCode="2260" DayPhone="(333) 333-3333" EMailID="PBAGLA@CROCS.COM"/>
	<OrderLines>
		<OrderLine OrderedQty="2.00" PrimeLineNo="1" SubLineNo="1" DeliveryMethod="SHP" ReturnReason="301" ConditionVariable1="Migration" OtherCharges="5.0">
			<Item CostCurrency="SGD" ItemID="10006-001-M20" ItemDesc="original, classic clog, classic, crocs, jjjjound, baya" ItemShortDesc="original, classic clog, classic, crocs, jjjjound, baya" UnitOfMeasure="EACH"/>
			<LineCharges/>
			<LinePriceInfo IsPriceLocked="Y" UnitPrice="21.0" ListPrice="21.0" RetailPrice="21.0" LineTotal="37.48" ActualPricingQty="2" OrderedPricingQty="2"  TaxableFlag="Y"/>
			<DerivedFrom DocumentType="0001" EnterpriseCode="CROCS_SG" OrderNo="AULOC000015"/>
			<OrderStatuses>
				<OrderStatus Status="3950.01" StatusQty="2.00">
					<Schedule/>
				</OrderStatus>
			</OrderStatuses>
		</OrderLine>
		<OrderLine OrderedQty="1.00" PrimeLineNo="2" SubLineNo="1" DeliveryMethod="SHP" ReturnReason="301" ConditionVariable1="Migration" OtherCharges="4.5">
			<Item CostCurrency="SGD" ItemID="10006-001-M44" ItemDesc="original, classic clog, classic, crocs, jjjjound, baya" ItemShortDesc="original, classic clog, classic, crocs, jjjjound, baya" UnitOfMeasure="EACH"/>
			<LineCharges/>
			<LinePriceInfo IsPriceLocked="Y" UnitPrice="25.0" ListPrice="25.0" RetailPrice="25.0" LineTotal="24.07" ActualPricingQty="1" OrderedPricingQty="1"  TaxableFlag="Y"/>
			<DerivedFrom DocumentType="0001" EnterpriseCode="CROCS_SG" OrderNo="AULOC000015"/>
			<OrderStatuses>
				<OrderStatus Status="3950.01" StatusQty="1.00">
					<Schedule/>
				</OrderStatus>
			</OrderStatuses>
		</OrderLine>
	</OrderLines>
</Order> 
     * }</pre>
     *
     * @param env - YFSEnvironment
     * @param inDoc - Input XML
     * @return Final validated and updated XML
     **/

    /*
     * Service Name: CrocsReturnOrderImportAsyncServ</p>
     * Purpose: This service will consume XML from CROCS_INT_RETURN_MIGRATION_QUEUE and perform data validation before importing the  order
     * Mandatory Attributes: DocumentType, EnteredBy, EnterpriseCode, OrderNo, ConditionVariable1, PipelineKey
     */

    public Document migrationDataValidation(YFSEnvironment env, Document inDoc) throws Exception {
        logger.beginTimer(CrocsSGMigarationUtil.logCurrentMethod(this.getClass()));

        logger.info("Migration: OMS_UPDATE : Input Doc for migrationDataValidation: OrderNo: " + inDoc.getDocumentElement().getAttribute(A_ORDER_NO));
        try {
            logger.verbose("Input Doc for CrocsReturnOrderMigration: \n" +SCXmlUtil.getString(inDoc));

            YFCDocument importReturnOrderInYdoc = YFCDocument.getDocumentFor(inDoc);
            YFCElement importReturnOrderYdocEle = importReturnOrderInYdoc.getDocumentElement();

            logger.verbose("importReturnOrderInYdoc for CrocsReturnOrderMigration is: " + importReturnOrderInYdoc.toString());

            String [] importROMandatoryAttr= {
                    A_DOCUMENT_TYPE,A_RETURN_ORDER_DOCUMENT_TYPE,A_ENTERED_BY,
                    A_ENTERPRISE_CODE,A_ORDER_NO,A_CONDITION_VARIABLE_1,A_RO_MIGRATION_CROCS_SG_PIPELINE_PROPERTY};


            boolean isValidForImportROProcesingFlag= isValidForImportReturnOrderProcesing(importReturnOrderYdocEle,importROMandatoryAttr);
            if (isValidForImportROProcesingFlag){
                importROProcesingReadiness(env,importReturnOrderYdocEle);
            }

            logger.endTimer(CrocsSGMigarationUtil.logCurrentMethod(this.getClass()));
            logger.verbose("Final import ReturnOrder Document: " +SCXmlUtil.getString(inDoc));
            return inDoc;

        } catch (YFSException e) {
        	logger.info("Migration :OMS_UPDATE: migrationDataValidation Catch Block :"+e.getMessage()+" "+e.getErrorCode()+""+e.getErrorDescription());
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        }
    }

    /**
     * @param importOrderYdocEle - YFCElement of Order
     */
    private void importROProcesingReadiness(YFSEnvironment env, YFCElement importOrderYdocEle) throws Exception {
        logger.beginTimer(CrocsSGMigarationUtil.logCurrentMethod(this.getClass()));
        logger.info("Migration: OMS_UPDATE : importROProcesingReadiness: OrderNo: " + importOrderYdocEle.getAttribute(A_ORDER_NO));
        logger.verbose("importOrderYdocEle for importROProcesingReadiness is: " + importOrderYdocEle.toString());

        importOrderYdocEle.setAttribute(A_PROCESS_PAYMENTS_ON_RETURN_ORDER, FLAG_N);
        YFCElement priceInfoEle = importOrderYdocEle.getChildElement(E_PRICE_INFO);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal extnRefundAmount = BigDecimal.ZERO;

        String entryType = importOrderYdocEle.getAttribute(A_ENTRY_TYPE);
        YFCElement orderExtn = importOrderYdocEle.getChildElement(E_EXTN);

        if (!YFCCommon.isVoid(orderExtn)) {
            String refundAmountStr = orderExtn.getAttribute(A_EXTN_REFUND_AMOUNT);
            if (!YFCCommon.isVoid(refundAmountStr)) {
                extnRefundAmount = new BigDecimal(refundAmountStr);
            }
        }

        logger.info("extnRefundAmount:" + extnRefundAmount + " for Import RO OrderNo:"+ importOrderYdocEle.getAttribute(OrderNo));

        YFCNodeList<YFCElement> orderLineList = importOrderYdocEle.getElementsByTagName(E_ORDER_LINE);

        for (YFCElement returnOrderLine : orderLineList) {
            YFCElement itemEle = returnOrderLine.getChildElement(E_ITEM);
            String returnOrderItemId = itemEle.getAttribute(A_ITEM_ID);
            logger.verbose("returnOrderItemId is: " + returnOrderItemId);

            YFCElement orderStatusesEle = returnOrderLine.getChildElement(A_ORDER_STATUSES);
            YFCElement orderStatusEle = orderStatusesEle.getChildElement(A_ORDER_STATUS);
            orderStatusEle.setAttribute(A_STATUS, VAL_MIG_STATUS_INVOICED);
            orderStatusEle.createChild(E_SCHEDULE);

            YFCElement linePriceInfoEle = returnOrderLine.getChildElement(A_LINE_PRICE_INFO);
            String lineTotalStr = linePriceInfoEle.getAttribute(A_LINE_TOTAL);
            BigDecimal lineTotalBD = new BigDecimal(lineTotalStr);

            logger.info("lineTotal is: " + lineTotalBD);
            logger.info("totalAmount before the update is: " + totalAmount);
            totalAmount = totalAmount.add(lineTotalBD);
            logger.info("final totalAmount is: " + totalAmount);

            YFCElement derivedFromEle = returnOrderLine.getChildElement(A_DERIVED_FROM);
            String strSalesOrderNo = derivedFromEle.getAttribute(OrderNo);
            String salesOrderDocType = derivedFromEle.getAttribute(DocumentType);
            String salesOrderEnterpriseCode = derivedFromEle.getAttribute(EnterpriseCode);

            logger.info("SalesOrderNo is: " + strSalesOrderNo);

            Document getOrderListInput = SCXmlUtil.createDocument(E_ORDER);
            Element orderEle = getOrderListInput.getDocumentElement();
            orderEle.setAttribute(A_DOCUMENT_TYPE, salesOrderDocType);
            orderEle.setAttribute(A_ENTERPRISE_CODE, salesOrderEnterpriseCode);
            orderEle.setAttribute(A_ORDER_NO, strSalesOrderNo);

            logger.verbose("getOrderListInput is: " + SCXmlUtil.getString(orderEle));
            Document getOrderListOutput = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_LIST_IMPORT_RO, API_GET_ORDER_LIST, getOrderListInput);

            logger.verbose("getOrderListOutput is: " + SCXmlUtil.getString(getOrderListOutput));

            if (!CrocsSGMigarationUtil.isValidSalesOrder(getOrderListOutput)) {
                throw new YFSException("Invalid Sales Order [" + strSalesOrderNo + "] Provided for importReturn", "",
                        "Sales Order does not exist \n " + SCXmlUtil.getString(getOrderListOutput));
            } else {
                YFCNodeList<YFCElement> golOrderLines = YFCDocument.getDocumentFor(getOrderListOutput)
                        .getDocumentElement().getElementsByTagName(E_ORDER_LINE);

                for (YFCElement golOrderLine : golOrderLines) {
                    YFCElement salesOrderItemEle = golOrderLine.getChildElement(E_ITEM);
                    String salesItemId = salesOrderItemEle.getAttribute(A_ITEM_ID);

                    if (returnOrderItemId.equals(salesItemId)) {
                        derivedFromEle.setAttribute(A_PRIME_LINE_NO, golOrderLine.getAttribute(A_PRIME_LINE_NO));
                        derivedFromEle.setAttribute(A_SUB_LINE_NO, golOrderLine.getAttribute(A_SUB_LINE_NO));
                        logger.verbose("eleOrderLine after update is: " + returnOrderLine.getString());
                    }
                }
            }

            logger.verbose("eleOrderLine after all the updates is: " + returnOrderLine.toString());
        }

        logger.info("Migration: OMS_UPDATE: final totalAmount is: " + totalAmount);
        priceInfoEle.setAttribute(A_TOTAL_AMOUNT, totalAmount.toPlainString());

        // Check for BORIS orders
        if (!YFCCommon.isVoid(entryType) && entryType.equalsIgnoreCase(A_ENTRY_TYPE_STORE)) {

            // Calculate in-store exchange amount and add notes
            if (totalAmount.compareTo(extnRefundAmount) != 0) {
                BigDecimal exchangeAmount = totalAmount.subtract(extnRefundAmount);
                logger.verbose("exchangeAmount is: " + exchangeAmount);

                String noteText = String.format("$%.2f is in-store exchange or payment and $%.2f is the refund amount",
                        exchangeAmount, extnRefundAmount);

                YFCElement notesEle = importOrderYdocEle.createChild(E_NOTES);
                YFCElement noteEle = notesEle.createChild(E_NOTE);
                noteEle.setAttribute(A_NOTE_TEXT, noteText);

                logger.info("noteElement for Boris Return:" + noteEle.toString());
            }
        }

        logger.verbose("importOrderYdocEle after processing for importROProcesingReadiness is: " + importOrderYdocEle.toString());
        logger.verbose("Migration: OMS_UPDATE: importOrderYdocEle after processing for importROProcesingReadiness is: " + importOrderYdocEle.toString());
        logger.endTimer(CrocsSGMigarationUtil.logCurrentMethod(this.getClass()));
    }


    /**
     * @param importOrderYdocEle - Element of Order
     * @param importROMandatoryAttr
     */
    private boolean isValidForImportReturnOrderProcesing(YFCElement importOrderYdocEle, String[] importROMandatoryAttr) {
        logger.beginTimer(CrocsSGMigarationUtil.logCurrentMethod(this.getClass()));

        boolean isValid = false;
        for (String ImportROMandatoryAttr: importROMandatoryAttr) {
            if(CrocsSGMigarationUtil.validateMandatoryAttribute(importOrderYdocEle, ImportROMandatoryAttr))
                isValid = true;
            else
                break;
            }
        logger.endTimer(CrocsSGMigarationUtil.logCurrentMethod(this.getClass()));
        logger.verbose("isValid is: "+isValid);
        return isValid;
    }
}
