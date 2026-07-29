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

import { Injectable } from '@angular/core';
import { BucCommOmsMashupService, BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';


@Injectable()
export class ChangeFulfillmentMethodService {

  private readonly GET_COMPLETE_ORDER_LINES_LIST_MASHUP_ID = 'icc.order.summary.getCompleteOrderLineList';

  private readonly ICC_GET_FULFILLMENT_SUMMARY_MASHUP_ID = 'icc.order.change-fulfillment-method.getFulfillmentDetails';
  private readonly ICC_GET_ORDER_LINE = 'icc.order.change-fulfillment-method.getOrderLine';
  private readonly GET_BOPIS_RULE_DETAILS = 'icc.order.change-fulfillment-method.getBOPISruleValue';
  private readonly GET_COUNTRY_LIST = 'icc.order.change-fulfillment-method.getCountryList';
  private readonly GET_STATE_LIST = 'icc.common-components.getStateRegionList';
  private readonly GET_ALTERNATE_STORE_AVAILABLITY = 'icc.order.change-fulfillment-method.getAlternateStoreAvailability'
  private readonly CHECK_FOR_AVAILABILITY_OF_LINES = 'icc.order.change-fulfillment-method.checkForAvailabilityOfLines';
  private readonly GET_CARRIER_SERVICE_OPTIONS = 'icc.order.change-fulfillment-method.getCarrierServiceOptions';
  private readonly MODIFY_FULFILLMENT_METHOD_PICK_TO_SHIP = 'icc.order.change-fulfillment-method.modifyFulfillmentFromPickToShip';
  private readonly MODIFY_FULFILLMENT_METHOD_SHIP_TO_PICK = 'icc.order.change-fulfillment-method.modifyFulfillmentFromShipToPick';
  private GET_COMMON_CODE_LIST_MASHUP_ID = 'icc.order.summary.getCommonCodeList';


  // TODO: Add back when address is needed from customer details
  // private readonly GET_SHIPPING_ADDRESS_MASHUP_ID = 'icc.order.change-fulfillment-method.getShippingAddress';

  

  constructor(
    private bucCommOmsMashupService: BucCommOmsMashupService
  ) { }

  public getOrderLineListMashup(pageAction, pageModel, data, filtersExp: any = {}) {
    const PAGE_SIZE = data.pageSize;
    const mashupInput: any = {
      OrderLine: {
        BundleParentOrderLineKeyQryType: 'ISNULL',
        CallingOrganizationCode: data.enterpriseCode,
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        DisplayTransactionPrice: 'Y',
        IgnoreOrdering: 'N',
        OrderHeaderKey: data.orderHeaderKey,
        DeliveryMethod: filtersExp.DeliveryMethod,
        Status: filtersExp.Status,
        StatusQryType: filtersExp.StatusQryType,
        Order: {
          FromOrderDate: filtersExp.FromOrderDate,
          OrderDateQryType: 'BETWEEN',
          ToOrderDate: filtersExp.ToOrderDate,
          EnterpriseCode: data.enterpriseCode,
          OrderNo: data.orderNumber,
          SellerOrganizationCode: data.sellerOrganizationCode
        },
        ComplexQuery: {
          And: {
            Exp: filtersExp.EXP ?? []
          }
        },
        OrderBy: {
          Attribute: [
            {
              Name: 'GroupSequenceNumber',
              Desc: data.by === 'Desc'
            },
            {
              Name: data.sort,
              Desc: data.by === 'Desc'
            }]
        },
        Modifications: {
          Modification: [
            { ModificationType: 'PRICE' },
            { ModificationType: 'CANCEL' },
            { ModificationType: 'HOLD' },
            { ModificationType: 'SHIP_NODE' },
            { ModificationType: 'SHIPTO' },
            { ModificationType: 'CHANGE_DELIVERY_METHOD' },
            { ModificationType: 'MARKFOR' },
            { ModificationType: 'CARRIER_SERVICE_CODE' },
            { ModificationType: 'REQ_SHIP_DATE' }
          ]
        },
      }
    };
    // Check for History order
    if (data.isHistory && data.isHistory === 'Y') {
      mashupInput.OrderLine.ReadFromHistory = 'Y';
    }
    return this.bucCommOmsMashupService.callPaginatedMashup(this.GET_COMPLETE_ORDER_LINES_LIST_MASHUP_ID, mashupInput, pageAction,
      PAGE_SIZE, pageModel, {}).toPromise().then(mashupOutput => {
        if (pageAction === 'START') {
          return this.bucCommOmsMashupService.getPaginatedMashupOutput(mashupOutput, this.GET_COMPLETE_ORDER_LINES_LIST_MASHUP_ID);
        } else {
          const apiOutput = this.bucCommOmsMashupService.getPaginatedMashupOutput(
            mashupOutput, this.GET_COMPLETE_ORDER_LINES_LIST_MASHUP_ID
          );
          pageModel.IsFirstPage = apiOutput.IsFirstPage;
          pageModel.IsLastPage = apiOutput.IsLastPage;
          pageModel.PageNumber = apiOutput.PageNumber;
          pageModel.IsValidPage = apiOutput.IsValidPage;
          pageModel.LastRecord = apiOutput.LastRecord;
          // const appendModel = pageModel.Output.OrderLineList.OrderLine;
          pageModel.Output.OrderLineList.OrderLine = apiOutput.Output.OrderLineList.OrderLine;
          return pageModel;
        }
      });
  }

  public async getFulfillmentDetails(orderHeaderKey, IsLargeOrder) {
    const fulfillmentInput = {
      Order: {
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        OrderHeaderKey: orderHeaderKey,
        IsLargeOrder,
        OrderLines: {
          OrderLine : [
            {
              Modifications: {
                Modification: [
                  { ModificationType: 'SHIP_NODE' },
                  { ModificationType: 'SHIPTO' },
                  { ModificationType: 'CHANGE_DELIVERY_METHOD' },
                  { ModificationType: 'MARKFOR' },
                  { ModificationType: 'CARRIER_SERVICE_CODE' },
                  { ModificationType: 'REQ_SHIP_DATE' },
                ]
              }
            }
          ]
        }

      }
    };
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.ICC_GET_FULFILLMENT_SUMMARY_MASHUP_ID, fulfillmentInput, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_GET_FULFILLMENT_SUMMARY_MASHUP_ID);
  }

  public  async getBOPISRuleDetails(enterpriseCode) {
    const input: any = {
      Rules: {
        CallingOrganizationCode: enterpriseCode,
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        RuleSetFieldName: 'YCD_STORE_ENABLED'
      }
    };
    return this.bucCommOmsMashupService.callMashup(this.GET_BOPIS_RULE_DETAILS, input, {}).toPromise()
    .then(mashupOutput => this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.GET_BOPIS_RULE_DETAILS));
  }

  public async checkForAvailabilityOfLines(enterpriseCode, selectedOrderLines, orderHeaderKey, personInfoShipTo) {
    if (personInfoShipTo.OriginalKey) {
      delete personInfoShipTo.OriginalKey;
    }
    if (personInfoShipTo.isUseSuggested) {
      delete personInfoShipTo.isUseSuggested;
    }
    const input: any = {
      Promise: {
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

  public async getCarrierServiceOptions(orderHeaderKey, orderLines ) {
    const input: any = {
      Order: {
        OrderHeaderKey: orderHeaderKey,
        IgnoreOrdering: 'Y',
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        OrderLines : {
          OrderLine : orderLines
        }
      }
    };

    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.GET_CARRIER_SERVICE_OPTIONS, input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.GET_CARRIER_SERVICE_OPTIONS);
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
        IsLargeOrder: modifyFulfillmentInput.isLargeOrder
      }
    };
    let orderlinesInput = [];
    orderlinesInput = modifyFulfillmentInput.orderLines.map(line => ({
      OrderLineKey: line.OrderLineKey,
      ShipNode: ' ',
      DeliveryMethod: 'SHP',
      Notes: line.Notes,
      PersonInfoShipTo: modifyFulfillmentInput.personInfoShipTo,
      CarrierServiceCode: modifyFulfillmentInput.carrierServiceCode,
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

  public async modifyFulfillmentMethodFromShipToPick(modifyFulfillmentInput) {
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.MODIFY_FULFILLMENT_METHOD_SHIP_TO_PICK, modifyFulfillmentInput, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.MODIFY_FULFILLMENT_METHOD_SHIP_TO_PICK);
  }

  public getFulfillmentAndOrderLineDetails(orderHeaderKey, orderLineKey, isLargeOrder) {
    const mashupArray = [];
    mashupArray.push({
      mashupId: this.ICC_GET_FULFILLMENT_SUMMARY_MASHUP_ID,
      mashupInput: {
        Order: {
          DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
          OrderHeaderKey: orderHeaderKey,
          IsLargeOrder: isLargeOrder
        }
      }
    });
    mashupArray.push({
      mashupId: this.ICC_GET_ORDER_LINE,
      mashupInput: {
        OrderLine: {
          OrderLineKey: orderLineKey,
          DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale()
        }
      }
    });
    return this.bucCommOmsMashupService.callMashups(mashupArray).toPromise().then(this.handleFulfillmentAndOrderLine.bind(this));
  }

  private handleFulfillmentAndOrderLine(mashupOutput) {
    return {
      getFulfillmentDetails: this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_GET_FULFILLMENT_SUMMARY_MASHUP_ID),
      getOrderLine: this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_GET_ORDER_LINE)
    };
  }

  public async getCountryList(enterpriseCode) {
    const input = {
      Rules: {
        CallingOrganizationCode: enterpriseCode,
      }
    };
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.GET_COUNTRY_LIST, input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.GET_COUNTRY_LIST);
  }

  public async getStateList(country, enterprise?) {
    const input = {
      Region : {
        Country: country,
        OrganizationCode: enterprise
      }
    };
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.GET_STATE_LIST, input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.GET_STATE_LIST);
  }

  public getAlternateStoreAvailablity(pageAction, pageModel, pageSize, mashupInput) {
    return this.bucCommOmsMashupService.callPaginatedMashup(this.GET_ALTERNATE_STORE_AVAILABLITY, mashupInput, pageAction,
      pageSize, pageModel, {}).toPromise().then(mashupOutput => {
            if (pageAction === 'START') {
                return this.bucCommOmsMashupService.getPaginatedMashupOutput(mashupOutput, this.GET_ALTERNATE_STORE_AVAILABLITY);
            } else {
                const apiOutput = this.bucCommOmsMashupService.getPaginatedMashupOutput(
                    mashupOutput, this.GET_ALTERNATE_STORE_AVAILABLITY
                );
                pageModel.IsFirstPage = apiOutput.IsFirstPage;
                pageModel.IsLastPage = apiOutput.IsLastPage;
                pageModel.PageNumber = apiOutput.PageNumber;
                pageModel.IsValidPage = apiOutput.IsValidPage;
                pageModel.LastRecord = apiOutput.LastRecord;
                pageModel.Output.AlternateStores.NodeList.Node = apiOutput.Output.AlternateStores.NodeList.Node;
                return pageModel;
            }
        });
  }

  public async getNotesReasonCodesForNotes(enterpriseCode, docType, notesReason) {
    const notesReasonCodesInput = {
      CommonCode: {
        CallingOrganizationCode: enterpriseCode,
        CodeType: notesReason,
        DocumentType: docType,
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
      }
    };
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.GET_COMMON_CODE_LIST_MASHUP_ID, notesReasonCodesInput, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.GET_COMMON_CODE_LIST_MASHUP_ID);
  }



 // TODO: Add back when address is needed from customer details
 // Calls getCompleteOrderDetails to fetch Shipping address
 // Calls getCustomerDetails in custom mashup if address doesn't exist in getCompleteOrderDetails response
  // getShippingAddress(orderHeaderKey) {
  //   const input = {
  //     Order: {
  //       DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
  //       IgnorePendingChange: 'N',
  //       RetrieveDefaultCustomerPaymentMethod: 'Y',
  //       OrderHeaderKey: orderHeaderKey,
  //       Modifications: {
  //         Modification: [
  //           {
  //             ModificationType: 'PRICE'
  //           },
  //           {
  //             ModificationType: 'CHANGE_PROMOTION'
  //           },
  //           {
  //             ModificationType: 'REMOVE_PROMOTION'
  //           },
  //           {
  //             ModificationType: 'PAYMENT_METHOD'
  //           }
  //         ]
  //       },
  //     }
  //   };
  //   return this.bucCommOmsMashupService.callMashup(this.GET_SHIPPING_ADDRESS_MASHUP_ID, input, {}).toPromise()
  //     .then(mashupOutput => this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.GET_SHIPPING_ADDRESS_MASHUP_ID));
  // }

}
