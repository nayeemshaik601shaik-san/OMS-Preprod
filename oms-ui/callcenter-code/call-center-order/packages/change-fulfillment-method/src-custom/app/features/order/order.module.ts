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
import { ChangeFulfillmentMethodComponent } from './change-fulfillment-method/change-fulfillment-method.component';
import { SharedModule, OrderActionsModule, OrderSharedModule } from '@call-center/order-shared';



import { AccordionModule, CodeSnippetModule, DatePickerModule, DialogModule, ModalModule, TagModule, IconService, TooltipModule } from 'carbon-components-angular';
import { ModalService, IconModule } from 'carbon-components-angular';
import { AdjustPricingActionService } from '@call-center/order-shared/lib/actions/adjust-pricing-action.service';
import { ChangeFulfillmentLinesTableComponent } from './change-fulfillment-method/change-fulfillment-lines-table/change-fulfillment-lines-table.component';
import { ChangeFulfillmentMethodActionsModule } from '../../modules/change-fulfillment-method.action.module';
import { CCRightPanelComponent } from '@call-center/order-shared/lib/components/cc-right-panel/cc-right-panel.component';
import Edit16 from "@carbon/icons/es/edit/16";
import Box16 from "@carbon/icons/es/box/16";
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
        ChangeFulfillmentMethodComponent,
        ChangeFulfillmentLinesTableComponent,
        CCRightPanelComponent,
        ...AppCustomizationImpl.components
    ],
    imports: [
        CommonModule,
        OrderRoutingModule,
        ModalModule,
        TagModule,
            DialogModule,
        TranslateModule.forChild({
            loader: {
                provide: TranslateLoader,
                useFactory: (http: HttpClient) => new BucMultiTranslateHttpLoader(http, bundles, true),
                deps: [HttpClient]
            },
            isolate: true
        }),
        SharedModule,
        OrderActionsModule,
        OrderSharedModule,
        AccordionModule,
        DatePickerModule,
        ChangeFulfillmentMethodActionsModule,
        CodeSnippetModule,
        IconModule,
        TooltipModule,
        ...AppCustomizationImpl.imports,
        ExtOrderModule
    ],
    providers: [
        ModalService,
        AdjustPricingActionService,
        ...AppCustomizationImpl.providers
    ]
})
export class OrderModule extends BucCommonClassesAllModuleClazz {
  constructor(private iconService: IconService, translateService: TranslateService) {
    super(translateService);
    iconService.registerAll([Box16, Edit16]);
  }
}
