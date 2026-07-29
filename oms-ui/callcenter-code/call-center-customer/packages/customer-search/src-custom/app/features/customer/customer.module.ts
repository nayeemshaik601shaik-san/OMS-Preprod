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
import { EffectsModule } from '@ngrx/effects';

import { AddressStateModule, SharedModule } from '@call-center/customer-shared';
import { CustomerSearchComponent } from './customer-search/customer-search.component';
import { OmsiCacheService, OmsOrdersService } from '@buc/common-components';
import { IconModule, IconService } from 'carbon-components-angular';
import Add16 from "@carbon/icons/es/add/16";
import { AppCustomizationImpl } from '../../app-customization.impl';
import { ExtCustomerModule } from '../ext-customer.module';
// components

const bundles: Array<any> = [
  {
    prefix: './assets/i18n/',
    suffix: '.json'
  }
];

@NgModule({
    declarations: [
        CustomerSearchComponent,
        ...AppCustomizationImpl.components
    ],
    imports: [
        CommonModule,
        CustomerRoutingModule,
        AddressStateModule,
        TranslateModule.forChild({
            loader: {
                provide: TranslateLoader,
                useFactory: (http: HttpClient) => new BucMultiTranslateHttpLoader(http, bundles, true),
                deps: [HttpClient]
            },
            isolate: true
        }),
        SharedModule,
        EffectsModule.forFeature([

        ]),
        IconModule,
        ...AppCustomizationImpl.imports,
        ExtCustomerModule
    ],
    providers: [
        OmsiCacheService,
        OmsOrdersService,
        ...AppCustomizationImpl.providers
    ]
})
export class CustomerModule extends BucCommonClassesAllModuleClazz {
  constructor(private iconService: IconService, translateService: TranslateService) {
    super(translateService);
    iconService.registerAll([Add16]);
  }
}
