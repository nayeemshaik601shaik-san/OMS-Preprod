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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { TranslateModule, TranslateLoader, TranslateService } from '@ngx-translate/core';
import { BucCommonClassesAllModuleClazz, BucMultiTranslateHttpLoader } from '@buc/svc-angular';
import { OrderRoutingModule } from './order-routing.module';
import { OrderSearchResultComponent } from './order-search-result/order-search-result.component';
import { OrderSharedModule, SharedModule } from '@call-center/order-shared';
import { OrderTableComponent } from './order-table/order-table.component';
import {
  OmsiCacheService,
  OmsOrdersService
} from '@buc/common-components';
import { EffectsModule } from '@ngrx/effects';
import { OrderSearchResultsEffect } from '../state/order-search-result.effect';
import { IconModule, IconService, TooltipModule } from 'carbon-components-angular';
import Information16 from "@carbon/icons/es/information/16";
import Settings16 from "@carbon/icons/es/settings/16";
import Renew16 from "@carbon/icons/es/renew/16";
import Archive16 from "@carbon/icons/es/archive/16";
import PauseOutlineFilled16 from "@carbon/icons/es/pause--outline--filled/16";
import { AppCustomizationImpl } from '../../app-customization.impl';
import { ExtOrderModule } from '../ext-order.module';
// components

const bundles: Array<any> = [
  {
    prefix: './assets/i18n/',
    suffix: '.json'
  }
];

@NgModule({
  declarations: [
    OrderSearchResultComponent,
    OrderTableComponent,
    ...AppCustomizationImpl.components
  ],
  imports: [
    CommonModule,
    OrderRoutingModule,
        TranslateModule.forChild({
      loader: {
        provide: TranslateLoader,
        useFactory: (http: HttpClient) => new BucMultiTranslateHttpLoader(http, bundles, true),
        deps: [HttpClient]
      },
      isolate: true
    }),
    SharedModule,
    OrderSharedModule,
    EffectsModule.forFeature([OrderSearchResultsEffect]),
    IconModule,
    TooltipModule,
    ...AppCustomizationImpl.imports,
    ExtOrderModule
  ],
  providers: [
    OmsiCacheService,
    OmsOrdersService,
    OrderSearchResultsEffect,
    ...AppCustomizationImpl.providers
  ]
})
export class OrderModule extends BucCommonClassesAllModuleClazz {
  constructor(private iconService: IconService, translateService: TranslateService) {
    super(translateService);
    iconService.registerAll([
      PauseOutlineFilled16, Archive16, Renew16, Settings16, Information16
    ]);
  }
}
