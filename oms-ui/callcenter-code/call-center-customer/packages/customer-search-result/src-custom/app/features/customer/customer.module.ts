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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { TranslateModule, TranslateLoader, TranslateService } from '@ngx-translate/core';
import { BucCommonClassesAllModuleClazz, BucMultiTranslateHttpLoader } from '@buc/svc-angular';
import { CustomerRoutingModule } from './customer-routing.module';
import { CustomerSearchResultComponent } from './customer-search-result/customer-search-result.component';
import { CustomerTableComponent } from './customer-table/customer-table.component';
import { SharedModule } from '@call-center/customer-shared';
import { OmsiCacheService, OmsOrdersService } from '@buc/common-components';
import { EffectsModule } from '@ngrx/effects';
import { CustomerSearchResultsEffect } from '../state/customer-search-results.effect';
import { IconModule, IconService } from 'carbon-components-angular';
import ChevronRight16 from "@carbon/icons/es/chevron--right/16";
import Add16 from "@carbon/icons/es/add/16";
import Settings16 from "@carbon/icons/es/settings/16";
import Renew16 from "@carbon/icons/es/renew/16";
import { AppCustomizationImpl } from '../../app-customization.impl';
import { ExtCustomerModule } from '../ext-customer.module';

const bundles: Array<any> = [
  {
    prefix: './assets/i18n/',
    suffix: '.json'
  }
];

@NgModule({
  declarations: [
    CustomerSearchResultComponent,
    CustomerTableComponent,
    ...AppCustomizationImpl.components
  ],
  imports: [
    CommonModule,
    CustomerRoutingModule,
    TranslateModule.forChild({
      loader: {
        provide: TranslateLoader,
        useFactory: (http: HttpClient) => new BucMultiTranslateHttpLoader(http, bundles, true),
        deps: [HttpClient]
      },
      isolate: true
    }),
    SharedModule,
    EffectsModule.forFeature([CustomerSearchResultsEffect]),
    IconModule,
    ...AppCustomizationImpl.imports,
    ExtCustomerModule
  ],
  providers: [
    OmsiCacheService,
    OmsOrdersService,
    CustomerSearchResultsEffect,
    ...AppCustomizationImpl.providers
  ]
})
export class CustomerModule extends BucCommonClassesAllModuleClazz {
  constructor(private iconService: IconService, translateService: TranslateService) {
    super(translateService);
    iconService.registerAll([Renew16, Settings16, Add16, ChevronRight16]);
  }
}
