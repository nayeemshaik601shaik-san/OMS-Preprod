/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2019, 2025
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */


import { Injectable } from '@angular/core';
import { BucCommOmsMashupService, BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { TranslateService } from '@ngx-translate/core';
import { cloneDeep } from 'lodash';
import { CC_CONSTANTS } from '../../../common/constants';

@Injectable({
  providedIn: 'root',
})
export class ProductSearchService {
  private readonly ICC_GET_CATEGORIES_MASHUP_ID = 'icc.order.item-search.searchCatalogIndex';
  private readonly ICC_GET_CATEGORIES_WITHOUT_AVAILABILITY_MASHUP_ID = "icc.order.item-search.searchCatalogIndexWithoutAvailability";
  private readonly ICC_FETCH_ALL_CATEGORIES_MASHUP_ID = 'icc.order.item-search.fetchAllCategories';
  private readonly ICC_GET_SORT_OPTIONS_MASHUP_ID = 'icc.order.item-search.getSearchIndexFieldList';
  private readonly GET_RULE_DETAILS = 'icc.common.getRuleDetails';
  private readonly ICC_BUNDLE_ITEM_AVAILABILITY_ADD_TO_CART = 'icc.order.item-search.addToCartBundleItemShipInd';
  private readonly ICC_GET_PRODUCT_DETAILS = 'icc.productDetail.getProductDetails';
  private readonly GET_COUNTRY_LIST = 'icc.common-components.getCountryList';

  constructor(
    private bucCommOmsMashupService: BucCommOmsMashupService,
    private translateService: TranslateService
  ) { }

  public async getCategoriesList(inputParams, checkAvailabilityOfProducts = true) {
    const categorySearchInput: any = {
      CategorySearch: {
        CallingOrganizationCode: inputParams.enterpriseCode,
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        IgnoreOrdering: 'Y',
        PageNumber: inputParams.pageNumber || '1',
        PageSize: inputParams.pageSize || '5',
        CategoryPath: inputParams.categoryPath || '',
        Item: {
          Currency: inputParams.currency || '',
          ExcludeChildItemsOfModelItems: 'N',
          GetAvailabilityFromCache: inputParams.cacheInventoryRule ? inputParams.cacheInventoryRule : 'Y',
          IgnoreInvalidItems: 'N',
          ItemGroupCode: 'PROD',
          CustomerInformation: {
            CustomerContactID: inputParams.customerContactId || '',
            CustomerID: inputParams.customerId || ''
          }
        }
      }
    };
    if (inputParams.termVal !== null && inputParams.termVal !== undefined) {
      categorySearchInput.CategorySearch.Terms = {
        Term: {
          Condition: 'MUST',
          Value: inputParams.termVal
        }
      }
    }
    if (inputParams.inputFilters !== null && inputParams.inputFilters !== undefined) {
      inputParams.inputFilters = inputParams.inputFilters.map(el => ({ IndexFieldName: el.IndexFieldName, Value: el.Value }));
      categorySearchInput.CategorySearch.Filters = {
        Filter: inputParams.inputFilters
      }
    }
    if (inputParams.sortField !== null && inputParams.sortField !== undefined) {
      categorySearchInput.CategorySearch.SortField = inputParams.sortField.split('-')[0];
      const IsAscendOrDescend = inputParams.sortField.split('-')[1] === 'Dsc' ? 'Y' : 'N';
      categorySearchInput.CategorySearch.SortDescending = IsAscendOrDescend;
    }
    if (inputParams.zipCode !== null && inputParams.zipCode !== undefined) {
      categorySearchInput.CategorySearch.Item.ShipToAddress = { ZipCode: inputParams.zipCode };
      if (inputParams.country !== null && inputParams.country !== undefined) {
        categorySearchInput.CategorySearch.Item.ShipToAddress.Country = inputParams.country;
        categorySearchInput.CategorySearch.Item.ShipToAddress.City = inputParams.city;
        categorySearchInput.CategorySearch.Item.ShipToAddress.State = inputParams.state;
      }
    }
    if (inputParams.shipNode !== null && inputParams.shipNode !== undefined) {
      categorySearchInput.CategorySearch.Item.ShipNodes = {
        ShipNode: {
          Node: inputParams.shipNode
        }
      };
    }

    // Make request to mashup without availability
    const mashupID = checkAvailabilityOfProducts ? this.ICC_GET_CATEGORIES_MASHUP_ID : this.ICC_GET_CATEGORIES_WITHOUT_AVAILABILITY_MASHUP_ID;

    return this.bucCommOmsMashupService
      .callMashup(mashupID, categorySearchInput, { handleMashupError: true })
      .toPromise()
      .then(mashupOutput => {
        return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, mashupID);
      }, mashupError => this.handleMashupError(mashupError));
  }

  public async getSortOptionsAndCountryList(sortOptionsExist, countryExists) {
    let mashupArray = [];
    const sortOptionsInput = {
      mashupId: this.ICC_GET_SORT_OPTIONS_MASHUP_ID,
      mashupInput: {
        SearchIndexField: {
          DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
          IncludeCanUseAsFilter: 'N',
          IncludeSearchable: 'N',
          IncludeSortable: 'Y',
          MaximumRecords: ''
        }
      }
    };
    const countryListInput = {
      mashupId: this.GET_COUNTRY_LIST,
      mashupInput: {
        CommonCode: {
          DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        }
      }

    };
    // Call only required mashups based on the following conditions
    if (sortOptionsExist && countryExists) {
      return Promise.resolve({});
    } else if (sortOptionsExist && !countryExists) {
      mashupArray = [countryListInput];
    } else if (!sortOptionsExist && countryExists) {
      mashupArray = [sortOptionsInput];
    } else {
      mashupArray = [sortOptionsInput, countryListInput];
    }
    return this.bucCommOmsMashupService
      .callMashups(mashupArray)
      .toPromise()
      .then(this.handleSortOptionsAndCountryList.bind(this, sortOptionsExist, countryExists));
  }

  private handleSortOptionsAndCountryList(sortOptionsExist, countryExists, mashupOutput) {
    if (sortOptionsExist && !countryExists) {
      return {
        getCountryList: this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.GET_COUNTRY_LIST),
      };
    } else if (!sortOptionsExist && countryExists) {
      return {
        getSortOptions: this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_GET_SORT_OPTIONS_MASHUP_ID),
      };
    } else {
      return {
        getSortOptions: this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_GET_SORT_OPTIONS_MASHUP_ID),
        getCountryList: this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.GET_COUNTRY_LIST),
      };
    }
  }

  public async getProductDetailsBasedOnItemID(inputParams) {
    const input: any = {
      Item: {
        CallingOrganizationCode: inputParams.enterpriseCode,
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        IgnoreInvalidItems: 'N',
        ItemGroupCode: 'PROD',
        HideAssociationsWithoutItems: 'Y',
        ShowVisibleAssociationsOnly: 'Y',
        UnitOfMeasure: '',
        MaximumRecords: '5000',
        GetAvailabilityFromCache: inputParams.cacheInventoryRule ? inputParams.cacheInventoryRule : 'Y',
        CustomerInformation: {
          CustomerContactID: inputParams.customerContactID,
          CustomerID: inputParams.customerId,
        },
        BarCode: {
          ContextualInfo: {
            EnterpriseCode: inputParams.enterpriseCode,
            OrganizationCode: inputParams.enterpriseCode
          },
          BarCodeData: inputParams.itemId
        }
      }
    };
    if (inputParams.zipCode !== null && inputParams.zipCode !== undefined && inputParams.zipCode !== '') {
      input.Item.ShipToAddress = { ZipCode: inputParams.zipCode };
      if (inputParams.country !== null && inputParams.country !== undefined && inputParams.country !== '') {
        input.Item.ShipToAddress.Country = inputParams.country;
      }
    }
    if (inputParams.shipNode !== null && inputParams.shipNode !== undefined) {
      input.Item.ShipNodes = {
        ShipNode: {
          Node: inputParams.shipNode
        }
      };
    }
    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.ICC_GET_PRODUCT_DETAILS, input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_GET_PRODUCT_DETAILS);
  }

  public async fetchAllCategories(inputParams) {
    const categorySearchInput: any = {
      CategorySearch: {
        CallingOrganizationCode: inputParams.enterpriseCode,
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        IgnoreOrdering: 'Y',
        CategoryPath: inputParams.categoryPath || '',
        Item: {
          ExcludeChildItemsOfModelItems: 'N',
          GetAvailabilityFromCache: inputParams.cacheInventoryRule ? inputParams.cacheInventoryRule : 'Y',
          IgnoreInvalidItems: 'N',
          ItemGroupCode: 'PROD',
          CustomerInformation: {
            CustomerContactID: inputParams.customerContactId || '',
            CustomerID: inputParams.customerId || ''
          }
        }
      }
    };
    return this.bucCommOmsMashupService
      .callMashup(this.ICC_FETCH_ALL_CATEGORIES_MASHUP_ID, categorySearchInput, { handleMashupError: true })
      .toPromise()
      .then(mashupOutput => {
        return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_FETCH_ALL_CATEGORIES_MASHUP_ID);
      }, mashupError => this.handleMashupError(mashupError));
  }

  handleMashupError(error) {
    let errorMsg = '';
    let errorCode = '';
    if (error && error.mashupResponse) {
      errorMsg = error.mashupResponse.Errors.Error[0].ErrorDescription;
      errorCode = error.mashupResponse.Errors.Error[0].ErrorCode;
      const bundleKey = 'APIERROR.' + errorCode;
      if (this.translateService.instant(bundleKey) !== bundleKey) {
        errorMsg = this.translateService.instant(bundleKey);
      }
    }
    return Promise.reject({ errorMsg, errorCode });
  }

  public getBOPISandCacheInventoryRuleDetails(enterpriseCode) {
    const mashupArray = [];
    const storeRule: any = {
      mashupId: this.GET_RULE_DETAILS,
      mashupInput: {
        Rules: {
          CallingOrganizationCode: enterpriseCode,
          DocumentType: CC_CONSTANTS.SALES_ORDER_DOC_TYPE,
          DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
          RuleSetFieldName: 'YCD_STORE_ENABLED'
        }
      }
    };
    const cacheInventoryRule = cloneDeep(storeRule);
    cacheInventoryRule.mashupInput.Rules.RuleSetFieldName = 'YCD_USE_CACHE_INVENTORY_CHECK';
    mashupArray.push(storeRule, cacheInventoryRule);

    return this.bucCommOmsMashupService.callMashups(mashupArray).toPromise().then(this.handleBOPISandCacheInventoryRules.bind(this));
  }

  private handleBOPISandCacheInventoryRules(mashupOutput) {
    return {
      bopisRuleValue: this.getRulesMashupOutput(mashupOutput, 'YCD_STORE_ENABLED'),
      cacheInventoryRuleValue: this.getRulesMashupOutput(mashupOutput, 'YCD_USE_CACHE_INVENTORY_CHECK')
    };
  }

  public getRulesMashupOutput(mashupOutput, ruleName) {
    let output;
    const mashupRefs = mashupOutput.mashupResponse.controllerData.MashupRefs.MashupRef;
    if (mashupRefs && mashupRefs.length) {
      output = mashupRefs.find(el => el.Output.Rules.RuleSetFieldName === ruleName).Output;
    }
    return output;
  }


  public async addBundleShipIndItemToCart(inputParams?) {
    const input: any = {
      Item: {
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        DocumentType: CC_CONSTANTS.SALES_ORDER_DOC_TYPE,
        OrderHeaderKey: inputParams.orderHeaderKey,
        GetAvailabilityFromCache: inputParams.cacheInventoryRule ? inputParams.cacheInventoryRule : 'Y',
        IgnoreInvalidItems: 'N',
        ItemGroupCode: 'PROD',
        CallingOrganizationCode: inputParams.enterpriseCode,
        ItemID: inputParams.itemID,
        UnitOfMeasure: inputParams.uom,
        MaximumRecords: '5000',
        HideAssociationsWithoutItems: 'Y',
        ShowVisibleAssociationsOnly: 'Y',
        QuantityLimitOverridden: inputParams.quantityLimitOverridden,
        OrderedQty: inputParams.orderedQty,
        DeliveryMethod: inputParams.deliveryMethod,
        CustomerInformation: {
          CustomerContactID: inputParams.customerContactId,
          CustomerID: inputParams.customerId,
        }
      }
    };

    if (inputParams.orderLineKey !== null && inputParams.orderLineKey !== undefined) {
      input.Item.OrderLineKey = inputParams.orderLineKey;
    }
    if (inputParams.BundleTotal !== null && inputParams !== undefined) {
      input.Item.BundleTotal = inputParams.bundleTotal;
    }

    if (inputParams.zipCode !== null && inputParams.zipCode !== undefined) {
      input.Item.ShipToAddress = { ZipCode: inputParams.zipCode };
      if (inputParams.country !== null && inputParams.country !== undefined) {
        input.Item.ShipToAddress.Country = inputParams.country;
      }
    }
    if (inputParams.shipNode !== null && inputParams.shipNode !== undefined) {
      input.Item.ShipNodes = {
        ShipNode: {
          Node: inputParams.shipNode
        }
      };
    }

    const mashupOutput = await this.bucCommOmsMashupService.callMashup(this.ICC_BUNDLE_ITEM_AVAILABILITY_ADD_TO_CART, input, {}).toPromise();
    return this.bucCommOmsMashupService.getMashupOutput(mashupOutput, this.ICC_BUNDLE_ITEM_AVAILABILITY_ADD_TO_CART);
  }

  //EOMS-13713 Changes Start - Fetch Details from Common Code
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
  
   async getFulFillmentDetails(enterpriseCode: string) {
	   
    const input = {
      CommonCode: {
        CallingOrganizationCode: 'CROCS',
        CodeType: 'CROCS_FULFILMENT_DTL',
        CodeValue: enterpriseCode,
      },
    };

    try {
      const mashupOutput = await this.bucCommOmsMashupService.callMashup('icc.dataprovider.getCommonCodeDescForValue', input, {}).toPromise();

      const response = this.bucCommOmsMashupService.getMashupOutput(mashupOutput, 'icc.dataprovider.getCommonCodeDescForValue');
      return response?.CommonCodeList?.CommonCode?.[0] ?? {};
	  
    } catch (err: any) {
      // Handle only Invalid CodeType
      const error = err?.mashupResponse?.Errors?.Error?.[0];
      if (error?.ErrorCode === 'YCP0198' || error?.ErrorDescription?.includes('Invalid CodeType')) {
        console.warn(`Common Code "CROCS_FULFILMENT_DTL" not configured`);
        return {};
      }

      // throwing any other unexpected errors
      throw err;
    }
  }
    //EOMS-13713 Changes Start
}
