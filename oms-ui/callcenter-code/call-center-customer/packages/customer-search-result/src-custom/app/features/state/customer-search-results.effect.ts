/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2022, 2023
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

import { Injectable } from '@angular/core';
import { Constants, EntityActions, MashupActions, PaginationActions, PaginationState, paginationEffectCreator, BindHelper } from '@call-center/customer-shared';
import { Actions } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { get } from 'lodash';
import { BucBaseUtil, BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { getArray } from '@buc/common-components';
import { COMMON, SearchForm } from '@buc/common-components';
import { merge } from 'lodash';

@Injectable()
export class CustomerSearchResultsEffect {

  getReturnSearchResults$ = paginationEffectCreator(
    Constants.CUSTOMER_TABLE_COMPONENT_ID,
    this.actions$,
    this.store$,
    this.getReturnSearchResult.bind(this)
  );

  constructor(private actions$: Actions, private store$: Store) { }

  private getReturnSearchResult(payload: any, pageState: PaginationState): Action {
    const loadCriteria = get(payload, 'loadCriteria', pageState.loadCriteria);
    const sortCriteria = get(payload, 'sortCriteria', pageState.sortCriteria);

    // wait for sortCriteria and load criteria
    if (this.isValidLoadCriteria(loadCriteria) && !BucBaseUtil.isVoid(sortCriteria)) {
      const { searchCriteria, enterprise, MaximumRecords, searchText } = loadCriteria;

      const oob = searchCriteria.oob;
      let catMap: any = COMMON.toMap(oob, 'id');
      let customerType = catMap.CustomerInfo.options.filter(op => op.id === 'customerType')[0].value === 'consumer'? Constants.CUSTOMER_TYPE.consumer : Constants.CUSTOMER_TYPE.business;
      if (loadCriteria.customerType) {
        customerType = loadCriteria.customerType;
      } 

      const input = {
        Customer: {
          CallingOrganizationCode: enterprise,
          CustomerType: customerType,
          DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
          MaximumRecords,
          CustomerContactList: {
            CustomerContact: {}
          },
          ...customerType === Constants.CUSTOMER_TYPE.business && {BuyerOrganization: {}},
          OrderBy: {}
        }
      };

       // EOMS-1113
      // directly searched from home / shell (category seach) / toolbar
      if (searchText) {

        if(customerType === Constants.CUSTOMER_TYPE.consumer){
          let Exp = [];
          searchText.split(' ').forEach(element => {
            const searchAgainstFields = ['FirstName', 'LastName', 'EmailID', 'DayPhone'];
            const fields = searchAgainstFields.map(field => ({ Name: field, QryType: 'FLIKE', Value: element.toUpperCase() }));
            Exp.push(...fields);
          });
  
          merge(input.Customer.CustomerContactList.CustomerContact, {
            ComplexQuery: {
              And: {
                Or: {
                  Exp
                }
              }
            }
          });
        } else if(customerType === Constants.CUSTOMER_TYPE.business) {
          // only support organization name search for business customer.
          const orgField = {
            OrganizationName: searchText, 
            OrganizationNameQryType: 'FLIKE'
          };
  
          merge(input.Customer.BuyerOrganization, {
            ...orgField
          });
        }

      }

      const srcMap = COMMON.toMap(catMap[oob[0].id].options, 'id');
      const root = {
        BuyerOrganization: {},
        CustomerContactList: {
          CustomerContact: {
            User: {},
            CustomerAdditionalAddressList: {
              CustomerAdditionalAddress: {
                PersonInfo: {
                  ZipCode: ""
                }
              }
            }
          }
        }
      };
      BindHelper.valueLikeBinder(srcMap, 'customerId', 'CustomerID', root);
      BindHelper.valueLikeBinder(srcMap, 'organizationName', 'OrganizationName', root.BuyerOrganization);
      BindHelper.valueLikeBinder(srcMap, 'memberID', 'DisplayUserID', root.CustomerContactList.CustomerContact.User);
      BindHelper.valueLikeBinder(srcMap, 'emailID', 'EmailID', root.CustomerContactList.CustomerContact);
      BindHelper.valueLikeBinder(srcMap, 'customerTelephoneNo', 'DayPhone', root.CustomerContactList.CustomerContact);
      BindHelper.valueLikeBinder(srcMap, 'firstName', 'FirstName', root.CustomerContactList.CustomerContact);
      BindHelper.valueLikeBinder(srcMap, 'lastName', 'LastName', root.CustomerContactList.CustomerContact);
      BindHelper.valueLikeBinder(srcMap, 'postalCode', 'ZipCode',
        root.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.PersonInfo);
      merge(input.Customer, root);
      SearchForm.injectCustomizationInto(searchCriteria.custom, input.Customer, 'customers');
      // set the sort
      if (!BucBaseUtil.isVoid(sortCriteria)) {
        const { sortKey, sortOrder } = sortCriteria;
        if (!BucBaseUtil.isVoid(sortKey) && !BucBaseUtil.isVoid(sortOrder)) {
          input.Customer.OrderBy = { Attribute: { Name: sortKey, Desc: sortOrder === 'Desc' } };
        }
      }
      return MashupActions.invokePaginatedMashup({
        input: [
          {
            mashupId: Constants.MASHUP_ID_GET_PAGINATED_CUSTOMER_LIST,
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
            id: Constants.CUSTOMER_TABLE_COMPONENT_ID,
          })
        },
      });
    }

    return EntityActions.NO_OP_ACTION();
  }

  private isValidLoadCriteria(loadCriteria): boolean {
    return getArray(get(loadCriteria, 'searchCriteria.oob', [])).length > 0;
  }
}
