/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2021, 2023
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

import { Injectable, input } from '@angular/core';
import { DisplayRulesHelperService } from '@buc/common-components';
import { BucCommOmsMashupService, BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { Constants } from '@call-center/order-shared';

@Injectable()
export class FulfillmentMethodsService {

constructor(
    private bucCommOmsMashupService: BucCommOmsMashupService,
    private displayRuleService: DisplayRulesHelperService
    ) { }
  
  private readonly ICC_GET_FULFILLMENT_SUMMARY_MASHUP_ID = 'icc.order.create-order.fulfillment-methods.getFulfillmentDetails';
  private readonly ICC_DELETE_ORDERLINE_MASHUP_ID = 'icc.order.create-order.fulfillment-methods.deleteOrderLine'; 
  private readonly MODIFY_FULFILLMENT_METHOD_SHIP_TO_PICK = 'icc.order.create-order.fulfillment-methods.modifyFulfillmentFromShipToPick';
  private readonly MODIFY_FULFILLMENT_METHOD_PICK_TO_SHIP = 'icc.order.create-order.fulfillment-methods.modifyFulfillmentFromPickToShip';
  private readonly CHECK_FOR_AVAILABILITY_OF_LINES = 'icc.order.create-order.fulfillment-methods.checkForAvailabilityOfLines';
  private readonly ICC_CHANGE_SHIPPING_ADDRESS = "icc.order.create-order.fulfillment-methods.modifyOrderShippingAddress";
  private VALIDATE_AND_RESERVE_ORDER = "icc.order.create-order.review-order.validateItemAndReserve";
  private UPDATE_ORDERLINE_QUANTITY = "icc.order.create-order.review-cart.updateOrderLine";
  
  public async getFulfillmentDetails(orderHeaderKey, includeModifications ) {
    const fulfillmentInput = {
        Order: {
          DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
          OrderHeaderKey: orderHeaderKey,
          ...includeModifications ? {Modifications: {
            Modification: [
              {
                ModificationType: 'RULE_ID'
              },
              {
                ModificationType: 'ADD_INSTRUCTION'
              },
              {
                ModificationType: 'CHANGE_INSTRUCTION'
              }
            ]
          }}: {},
          OrderLines :{
            OrderLine: {
             ...includeModifications ? {
                Modifications: {
                  Modification: [
                    { ModificationType: 'CANCEL' },
                    { ModificationType: 'ADD_INSTRUCTION' },
                    { ModificationType: 'CHANGE_INSTRUCTION' },
                    { ModificationType: 'SHIP_NODE' },
                    { ModificationType: 'SHIPTO' },
                    { ModificationType: 'CHANGE_DELIVERY_METHOD' },
                    { ModificationType: 'MARKFOR' },
                    { ModificationType: 'CARRIER_SERVICE_CODE' },
                    { ModificationType: 'REQ_SHIP_DATE' }
                  ]
                }
              } : {}
            }
          }
        }
      };
    return this.bucCommOmsMashupService.callMashup(this.ICC_GET_FULFILLMENT_SUMMARY_MASHUP_ID, fulfillmentInput, {}).toPromise()
    .then(mashupOutput => this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_GET_FULFILLMENT_SUMMARY_MASHUP_ID));
  }

  public async deleteOrderLine(deleteOrderlineInput) {
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.ICC_DELETE_ORDERLINE_MASHUP_ID, deleteOrderlineInput, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_DELETE_ORDERLINE_MASHUP_ID);
  }

  public async modifyFulfillmentMethodFromShipToPick(modifyFulfillmentInput) {
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.MODIFY_FULFILLMENT_METHOD_SHIP_TO_PICK, modifyFulfillmentInput, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.MODIFY_FULFILLMENT_METHOD_SHIP_TO_PICK);
  }

  public async modifyFulfillmentMethodFromPickToShip(modifyFulfillmentInput) {
    const input: any = {
      Order: {
        Action: 'MODIFY',
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        IgnoreOrdering: 'Y',
        ValidateItems: 'Y',
        OrderHeaderKey: modifyFulfillmentInput.orderHeaderKey,
        DocumentType: modifyFulfillmentInput.docType,
        EnterpriseCode: modifyFulfillmentInput.orgCode,
      }
    };
    let orderlinesInput = [];
    orderlinesInput = modifyFulfillmentInput.orderLines.map(line => ({
      OrderLineKey: line.OrderLineKey,
      ShipNode: ' ',
      DeliveryMethod: 'SHP',
      PersonInfoShipTo: modifyFulfillmentInput.personInfoShipTo,
      CarrierServiceCode: modifyFulfillmentInput.carrierServiceCode,
      BundleParentLine: line.BundleParentLine,
      OrderDates: {
        OrderDate: [
          {
            CommittedDate: modifyFulfillmentInput.deliveryStartDate,
            DateTypeId: 'MIN_DELIVERY'
          },
          {
            CommittedDate: modifyFulfillmentInput.deliveryEndDate,
            DateTypeId: 'MAX_DELIVERY'
          }
        ]
      }
    }));
    input.Order.OrderLines = { OrderLine: [] };
    input.Order.OrderLines.OrderLine = orderlinesInput;

    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.MODIFY_FULFILLMENT_METHOD_PICK_TO_SHIP, input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.MODIFY_FULFILLMENT_METHOD_PICK_TO_SHIP);
  }

  public async checkForAvailabilityOfLines(enterpriseCode, selectedOrderLines, orderHeaderKey, personInfoShipTo, fulfillmentParams) {
    if (personInfoShipTo.OriginalKey) {
      delete personInfoShipTo.OriginalKey;
    }
    const input: any = {
      Promise: {
        modifyFulfillmentInput: fulfillmentParams.modifyFulfillmentInput,
        orignalOrderLines: { 
          OrderLine : fulfillmentParams.orderLines 
        },
        OrganizationCode: enterpriseCode,
        IgnoreOrdering: 'Y',
        IgnoreUnpromised: 'N',
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        PromiseLines: {
          PromiseLine: []
        },
        EvaluateOrder: {
          OrderHeaderKey: orderHeaderKey
        },
        EvaluateOptions: {
          EvaluateOption: {
            ShipToAddress: personInfoShipTo
          }
        }
      }
    };

    selectedOrderLines.forEach(line => {
      input.Promise.PromiseLines.PromiseLine.push({
        RequiredQty: line.OrderedQty,
        ShipNode: ' ',
        Overrides: { IsShippingAllowed: 'Y'},
        OrderLine: { OrderLineKey: line.OrderLineKey }
      });
    });

    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.CHECK_FOR_AVAILABILITY_OF_LINES, input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.CHECK_FOR_AVAILABILITY_OF_LINES);
  }

  updateQunatity(input) {
    return this.bucCommOmsMashupService.callMashup(this.UPDATE_ORDERLINE_QUANTITY, input, {}).toPromise()
      .then(mashupOutput => this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.UPDATE_ORDERLINE_QUANTITY));
  }

  async validateAndReserveOrder(orderDetails,isLargeOrder) {
    const isStampFirstPromiseDate = this.displayRuleService.getRuleValueForOrg(orderDetails.EnterpriseCode, Constants.ICC_STAMP_FIRST_PROMISE_DATE) === Constants.CHECK_YES;
    const input = {
      Order: {
        OrderHeaderKey: orderDetails.OrderHeaderKey,
        IsLargeOrder: isLargeOrder,
        EnterpriseCode: orderDetails.EnterpriseCode,
        DocumentType: orderDetails.DocumentType,
        StampFirstPromiseDate: isStampFirstPromiseDate ? 'Y' : 'N'
      }
    };
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.VALIDATE_AND_RESERVE_ORDER, input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.VALIDATE_AND_RESERVE_ORDER);
  }

  async changeShippingAddress(orderDetails, modifiedAddress) {
    const input: any = {
      Order: {
        Action: 'MODIFY',
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        IgnoreOrdering: 'Y',
        OrderHeaderKey: orderDetails.OrderHeaderKey,
        DocumentType: orderDetails.DocumentType,
        EnterpriseCode: orderDetails.EnterpriseCode,
        ShipToKey: orderDetails.ShipToKey,
        PersonInfoShipTo: modifiedAddress,
      }
    };
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.ICC_CHANGE_SHIPPING_ADDRESS, input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_CHANGE_SHIPPING_ADDRESS);
  }

  //EOMS-1463 - Changes Start
  async changeOrderReason(orderDetails) {
    const input: any = {
      Order: {
        Action: 'MODIFY',
        OrderHeaderKey: orderDetails.orderHeaderKey,
        DocumentType: orderDetails.docType,
        EnterpriseCode: orderDetails.orgCode,
        Extn:{
          ExtnReasonCode: orderDetails?.Extn?.extnReasonCode
        },
      }
    };
    const mashupOutput = await this.bucCommOmsMashupService.callMashup("icc.order.summary.updateReasonCode", input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, "icc.order.summary.updateReasonCode");
  }

  async changeCostCenter(orderDetails) {
    const input: any = {
      Order: {
        Action: 'MODIFY',
        OrderHeaderKey: orderDetails.orderHeaderKey,
        DocumentType: orderDetails.docType,
        EnterpriseCode: orderDetails.orgCode,
        Extn:{
          ExtnCostCenter: orderDetails?.Extn?.extnCostCenter
        },
      }
    };
    const mashupOutput = await this.bucCommOmsMashupService.callMashup("icc.order.summary.updateCostCenter", input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, "icc.order.summary.updateCostCenter");
  }

  async getCostCenters(enterpriseCode: string) {
    return this.getCommonCodes(enterpriseCode, 'CROCS_COST_CENTER');
  }

  async getOrderReasons() {
    return this.getCommonCodes('CROCS', 'CROCS_ORDER_REASONS');
  }

  private async getCommonCodes(callingOrganizationCode: string, 
    codeType: string,
  ) {
    const input = {
      CommonCode: {
        CallingOrganizationCode: callingOrganizationCode,
        CodeType: codeType,
      },
    };

    const mashupOutput = await this.bucCommOmsMashupService
      .callMashup('icc.order.summary.getCommonCodeList', input, {})
      .toPromise();

    const response = this.bucCommOmsMashupService.getMashupOutput(mashupOutput, 'icc.order.summary.getCommonCodeList');

    const list = response?.CommonCodeList?.CommonCode || [];

    return list.map((el) => ({
      id: el.CodeValue,
      value: el.CodeValue,
      content: el.CodeValue,
      selected: el.CodeLongDescription?.trim().toUpperCase() === 'DEFAULT',
    }));
  }


}
