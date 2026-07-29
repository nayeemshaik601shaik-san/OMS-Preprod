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
import { Constants, EntityActions, MashupActions, PaginationActions, PaginationState, paginationEffectCreator, BindHelper } from '@call-center/order-shared';
import { Actions } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { BucBaseUtil, BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import {
  COMMON,
  SearchForm,
  getArray,
  makeUnique
} from '@buc/common-components';
import { get, isArray, isEmpty, merge, mergeWith, mapValues, keyBy } from 'lodash';

@Injectable()
export class OrderSearchResultsEffect {

  constructor(private actions$: Actions, private store$: Store) { }

  getOrderSearchResults$ = paginationEffectCreator(
    Constants.ORDER_RESULTS_TABLE_COMPONENT_ID,
    this.actions$,
    this.store$,
    this.getOrderSearchResult.bind(this)
  );

  private isValidLoadCriteria(loadCriteria): boolean {
    return getArray(get(loadCriteria, 'searchCriteria.oob', [])).length > 0;
  }

  private _orderTotalQuery(gMap): any {
    const c = { value: undefined };
    if (BindHelper.findValue(gMap, 'orderTotal', 'value.range', c) && c.value) {
      const query = [];

      if (!BucBaseUtil.isVoid(c.value.min)) {
        query.push({
          Or: {
            Exp: { Name: 'TotalAmount', Value: String(c.value.min), QryType: 'GE' }
          }
        });
      }
      if (!BucBaseUtil.isVoid(c.value.max)) {
        query.push({
          Or: {
            Exp: { Name: 'TotalAmount', Value: String(c.value.max), QryType: 'LE' }
          }
        })
      };

      return query.length > 0 ? query : undefined;
    }
  }

  private _orderHoldQuery(cMap, def = ''): any {
    const orderHolds = { value: [] };
    const lineHolds = { value: [] };
    BindHelper.findValue(cMap, 'holdOrderType', 'value', orderHolds);
    BindHelper.findValue(cMap, 'holdLineType', 'value', lineHolds);
    if ((orderHolds.value.length > 0) || (lineHolds.value.length > 0)) {
      return {
        HoldFlag: 'Y',
        ...orderHolds.value.length > 0 ? {OrderHoldType: {
          ComplexQuery: {
            And: {
              Or: {
                Exp: orderHolds.value.map(holdType => (
                  { Name: 'HoldType', QryType: 'EQ', Value: holdType.id }))
              }
            }
          },
          Status: '1300',
          StatusQryType: 'NE'
        } } : {},
        ...lineHolds.value.length > 0 ? {OrderLine: {
          OrderHoldType: {
            ComplexQuery: {
              And: {
                Or: {
                  Exp: lineHolds.value.map(holdType => (
                    { Name: 'HoldType', QryType: 'EQ', Value: holdType.id }))
                }
              }
            },
            Status: '1300',
            StatusQryType: 'NE'
          }
        }} : {}
      };
    } else {
      return {
        HoldFlag: def
      };
    }
  }


  private getOrderSearchResult(payload: any, pageState: PaginationState): Action {
    const loadCriteria = get(payload, 'loadCriteria', pageState.loadCriteria);
    const sortCriteria = get(payload, 'sortCriteria', pageState.sortCriteria);

    // wait for sortCriteria and load criteria
    if (this.isValidLoadCriteria(loadCriteria) && !BucBaseUtil.isVoid(sortCriteria)) {
      const { searchCriteria, enterprise, MaximumRecords, searchText } = loadCriteria;

      const oob = searchCriteria.oob;
      const catMap: any = COMMON.toMap(oob, 'id');
      const gMap = catMap.OrderInfo ? COMMON.toMap(getArray(catMap.OrderInfo.options), 'id'): {};
      const c = { value: undefined };

      const ordersRaw = BindHelper.findValue(gMap, 'orderNo', 'value', c) ? c.value : '';

      const draftOrderValues = mapValues(keyBy(getArray(get(gMap, 'isDraftOrder.items')), 'id'), 'value');
      const Order = {
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        DocumentType: '0001',
        DraftOrderFlag: BindHelper.findValue(gMap, 'isDraftOrder', 'value', c) ?  draftOrderValues[c.value]  : 'N',
        ReadFromHistory: BindHelper.findValue(gMap, 'orderAge', 'value', c) ? c.value : 'N',
        EntryType: BindHelper.findValue(gMap, 'entryType', 'value', c) ? c.value : '',
        MaximumRecords,
        OrderStatus: {
          Status: BindHelper.findValue(gMap, 'orderLineStatus', 'value', c) ? c.value.searchValue : '',
        },
        OrderPurpose: BindHelper.findValue(gMap, 'orderPurpose', 'value', c) ? c.value : '',
        OrderBy: {}
      };

      if (!isEmpty(enterprise)) {
        const Exp = [enterprise].map(Value => ({ Name: 'EnterpriseCode', QryType: 'EQ', Value }));
        merge(Order, {
          ComplexQuery: { And: { And: { And: { Or: { Exp } } } } },
        });
      }

      const orderTotalQuery = this._orderTotalQuery(gMap);
      if (orderTotalQuery) {
        mergeWith(Order, {
          ComplexQuery: {
            And: {
              And: {
                And: orderTotalQuery
              }
            }
          }
        }, (obj, src) => isArray(obj) ? obj.concat(src) : undefined)
      }

      if (ordersRaw) {
        const Name = 'OrderNo';
        const QryType = 'LIKE';
        const Exp = makeUnique<string>(ordersRaw.split(',').map(o => o.trim())).filter(Boolean)
          .map(Value => ({ Name, QryType, Value }));

        if (!isEmpty(Exp)) {
          merge(Order, {
            ComplexQuery: {
              And: {
                And: {
                  Or: {
                    Exp
                  }
                }
              }
            }
          });
        }
      }

      //EOMS-2409 start
      // directly searched from home / shell (category seach) / toolbar
      if (searchText) {
        const searchAgainstFields = ['OrderNo', 'CustomerEMailID', 'CustomerPhoneNo'];
        const Exp = searchAgainstFields.map(field => ({ Name: field, QryType: 'FLIKE', Value: searchText.toUpperCase() }));
        merge(Order, { ComplexQuery: { And: { Or: { Exp } } } });
      }
      //EOMS-2409 end

      const ohQ = this._orderHoldQuery(gMap);
      const srcMap = COMMON.toMap(catMap[oob[1].id].options, 'id');
      const root = {};
      BindHelper.valueLikeBinder(gMap, 'customerPONo', 'CustomerPONo', root);
      BindHelper.dateTimeRangeBinder(gMap, 'orderDateRange', 'OrderDate', root);
      BindHelper.valueLikeBinder(srcMap, 'emailID', 'CustomerEMailID', root);
      BindHelper.valueLikeBinder(srcMap, 'customerTelephoneNo', 'CustomerPhoneNo', root);
      BindHelper.valueLikeBinder(srcMap, 'firstName', 'CustomerFirstName', root);
      BindHelper.valueLikeBinder(srcMap, 'lastName', 'CustomerLastName', root);
      BindHelper.valueLikeBinder(srcMap, 'postalCode', 'CustomerZipCode', root);
      BindHelper.valueLikeBinder(gMap, 'orderName', 'OrderName', root);
      
      //EOMS-3890 start
      const input = { Order: {} };

      merge(Order, root, ohQ);
      input.Order = Order; // assign after merging

      const fieldsToUppercase = ['CustomerEMailID', 'CustomerFirstName', 'CustomerLastName'];

      // Handling fields like CustomerFirstName,CustomerLastName and CustomerEMailID
          fieldsToUppercase.forEach(field => {
         const val = input.Order?.[field];
           if (typeof val === 'string') {
         input.Order[field] = val.toUpperCase();
             }
});

/**const order = input.Order as any;
 const expList = order?.ComplexQuery?.And?.Or?.Exp;

      if (Array.isArray(expList)) {
      expList.forEach(condition => {
      if (
      fieldsToUppercase.includes(condition.Name) &&
      typeof condition.Value === 'string'
       ) {
          condition.Value = condition.Value.toUpperCase();
         }
     });
   }**/
  //EOMS-3890 end

    if (!BucBaseUtil.isVoid(sortCriteria)) {
        const { sortKey, sortOrder } = sortCriteria;
        if (!BucBaseUtil.isVoid(sortKey) && !BucBaseUtil.isVoid(sortOrder)) {
          Order.OrderBy = { Attribute: { Name: sortKey, Desc: sortOrder === 'Desc' ? 'Y' : 'N' } };
        }
      }

    
      SearchForm.injectCustomizationInto(searchCriteria.custom, Order, 'orders');

      return MashupActions.invokePaginatedMashup({
        input: [
          {
            mashupId: Constants.MASHUP_ID_GET_PAGINATED_ORDER_LIST,
            mashupInput: input
          }
        ],
        pageAction: pageState.pageNumber === 1 ? 'START' : 'NEXT',
        PageNumber: pageState.pageNumber - 1,
        pageSize: pageState.pageSize,
        LastRecord: get(pageState, ['lastRecord', pageState.pageNumber - 1]),
        options: {
          pageId: pageState.id,
          handleMashupError: true,
          onFailureAction: PaginationActions.pageLoadFailed({
            id: Constants.ORDER_RESULTS_TABLE_COMPONENT_ID,
          })
        },
      });
    }

    return EntityActions.NO_OP_ACTION();
  }
}
