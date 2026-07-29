/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2019, 2023
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

import { Injectable } from '@angular/core';
import {
  BucCommOmsMashupService,
  BucSvcAngularStaticAppInfoFacadeUtil,
} from '@buc/svc-angular';
import { CC_CONSTANTS, PRODUCT_RULES } from '../../../common/constants';

@Injectable({
  providedIn: 'root',
})
export class ProductDetailsService {
  private GET_PRODUCT_DETAILS = 'icc.productDetail.getProductDetails';
  private GET_BUNDLED_COMPONENT_DETAILS =
    'icc.productDetail.getProductDetails.bundleComponents';
  private GET_ASSOCIATED_PRODUCT_DETAILS =
    'icc.productDetail.getProductDetails.associationList';
  private GET_PICKUP_RULE =
    'icc.productDetail.getProductDetails.getPickupEnabledRule';
  private GET_CACHE_INVENTORY_RULE = 'icc.common.getRuleDetails';
  private ADD_PRODUCT_TO_CART = 'icc.order.create-order.addToCart';

  constructor(private bucCommOmsMashupService: BucCommOmsMashupService) {}

  getProductDetails(input, hasRules) {
    if (hasRules) {
      return this.bucCommOmsMashupService
        .callMashup(this.GET_PRODUCT_DETAILS, input, {})
        .toPromise()
        .then((mashupOutput) => ({
          productDetails: this.bucCommOmsMashupService.getMashupOutput(
            mashupOutput,
            this.GET_PRODUCT_DETAILS
          ),
        }));
    } else {
      const mashupArray = [];
      mashupArray.push({
        mashupId: this.GET_CACHE_INVENTORY_RULE,
        mashupInput: this.getRuleDetailsInput(
          input.Item.CallingOrganizationCode,
          PRODUCT_RULES.cacheInventory
        ),
      });
      mashupArray.push({
        mashupId: this.GET_PICKUP_RULE,
        mashupInput: this.getRuleDetailsInput(
          input.Item.CallingOrganizationCode,
          PRODUCT_RULES.pickupStoreEnabled
        ),
      });
      mashupArray.push({
        mashupId: this.GET_PRODUCT_DETAILS,
        mashupInput: input,
      });
      return this.bucCommOmsMashupService
        .callMashups(mashupArray)
        .toPromise()
        .then(this.handleGetRuleDetailsResponse.bind(this));
    }
  }

  handleGetRuleDetailsResponse(mashupOutput) {
    return {
      cacheInventoryRule: this.bucCommOmsMashupService.getMashupOutput(
        mashupOutput,
        this.GET_CACHE_INVENTORY_RULE
      ),
      pickupEnabledRule: this.bucCommOmsMashupService.getMashupOutput(
        mashupOutput,
        this.GET_PICKUP_RULE
      ),
      productDetails: this.bucCommOmsMashupService.getMashupOutput(
        mashupOutput,
        this.GET_PRODUCT_DETAILS
      ),
    };
  }

  getBundledComponents(itemID, enterpriseCode, unitOfMeasure) {
    const input = {
      Item: {
        ItemID: itemID,
        CallingOrganizationCode: enterpriseCode,
        UnitOfMeasure: unitOfMeasure,
      },
    };
    return this.bucCommOmsMashupService
      .callMashup(this.GET_BUNDLED_COMPONENT_DETAILS, input, {})
      .toPromise()
      .then((mashupOutput) =>
        this.bucCommOmsMashupService.getMashupOutput(
          mashupOutput,
          this.GET_BUNDLED_COMPONENT_DETAILS
        )
      );
  }

  getAssociatedProductDetails({
    itemID,
    enterpriseCode,
    unitOfMeasure,
    associationType,
  }) {
    const input = {
      Item: {
        ItemID: itemID,
        CallingOrganizationCode: enterpriseCode,
        UnitOfMeasure: unitOfMeasure,
        ItemAssociationTypeList: {
          ItemAssociationType: {
            Type: associationType,
          },
        },
      },
    };
    return this.bucCommOmsMashupService
      .callMashup(this.GET_ASSOCIATED_PRODUCT_DETAILS, input, {})
      .toPromise()
      .then((mashupOutput) =>
        this.bucCommOmsMashupService.getMashupOutput(
          mashupOutput,
          this.GET_ASSOCIATED_PRODUCT_DETAILS
        )
      );
  }

  getRuleDetailsInput(enterpriseCode, ruleName) {
    return {
      Rules: {
        CallingOrganizationCode: enterpriseCode,
        DocumentType: CC_CONSTANTS.SALES_ORDER_DOC_TYPE,
        DisplayLocalizedFieldInLocale:
          BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        RuleSetFieldName: ruleName,
      },
    };
  }

  addProductToCart(input) {
    input.Order.DocumentType = input?.DocumentType || '0001';
    input.Order.DisplayLocalizedFieldInLocale =
      BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale();

    // EOMS-2543 Start
    // EOMS-2543 Default LevelOfService
    // Add CarrierServiceCode to each OrderLine if DocumentType is "0001"
    if (input.Order.DocumentType === '0001') {
      const orderLines = input.Order.OrderLines?.OrderLine;
      const normalizedOrderLines = Array.isArray(orderLines)
        ? orderLines
        : orderLines
        ? [orderLines]
        : [];
      normalizedOrderLines.forEach((orderLine) => {
        //EOMS-1463 - Promotion Call Center Order - Changes Start
        orderLine.CarrierServiceCode = this.getDefaultCarrierBasedOnEnterprise(
          input.Order.EnterpriseCode,
        );
      });
      //EOMS-1463 - Promotion Call Center Order - Changes End
      // EOMS-2543 End

      return this.bucCommOmsMashupService
        .callMashup(this.ADD_PRODUCT_TO_CART, input, {})
        .toPromise()
        .then((mashupOutput) =>
          this.bucCommOmsMashupService.getMashupOutput(
            mashupOutput,
            this.ADD_PRODUCT_TO_CART,
          ),
        );
    }
  }

  //EOMS-1463 - Promotion Call Center Order - Changes Start
  getDefaultCarrierBasedOnEnterprise(enterpriseCode: string): string {
    let carrierServiceCode = '';

    switch (enterpriseCode) {
      case 'CROCS_US':
        carrierServiceCode = 'Economy';  
        break;
      case 'CROCS_CA':
        carrierServiceCode = 'Standard';
        break;
      case 'HEYDUDE_US':
        carrierServiceCode = 'Economy';
        break;
      case 'HEYDUDE_CA':
        carrierServiceCode = 'Standard';
        break;
      case 'HEYDUDE_AU':
        carrierServiceCode = 'Express 2-Day';
        break;
      case 'CROCS_AU':
        carrierServiceCode = 'AusPost';
        break;
      default:
        carrierServiceCode = 'Economy';
        break;
    }
    return carrierServiceCode;
  }
  //EOMS-1463 - Promotion Call Center Order - Changes End

}
