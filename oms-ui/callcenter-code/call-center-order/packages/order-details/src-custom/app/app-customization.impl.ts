import {
    BucCommonComponentsModule,
    BucFeatureComponentsModule,
    CUSTOM_FEATURE_ACTIONS,
  } from '@buc/common-components';
import { ExtnAdjustPricingActionService } from "./custom/actions/extn-adjust-pricing-action.service";
import { ExtnAdjustPricingModalComponent } from "./custom/adjust-pricing-modal/adjust-pricing-modal.component";
import { Constants } from '@call-center/order-shared';
import { AdjustPricingLineTableComponent } from './custom/adjust-pricing-modal/adjust-pricing-line-table/adjust-pricing-line-table.component';
import { AdjustPricingOrderTableComponent } from './custom/adjust-pricing-modal/adjust-pricing-order-table/adjust-pricing-order-table.component';
import { ExtnFulfillmentGroupTabComponent } from './features/order/fulfillment-group-tab/fulfillment-group-tab.component';
import { ExtnFulfillmentGroupDetailsComponent } from './features/order/fulfillment-group-tab/fulfillment-group-details/fulfillment-group-details.component';

export class AppCustomizationImpl {
    static readonly components = [
        ExtnAdjustPricingModalComponent,
        AdjustPricingLineTableComponent,
        AdjustPricingOrderTableComponent,
		ExtnFulfillmentGroupTabComponent,
		ExtnFulfillmentGroupDetailsComponent
    ];

    static readonly providers = [
        ExtnAdjustPricingActionService,
    {
      provide: CUSTOM_FEATURE_ACTIONS, 
      useValue: [
        {
          name: Constants.ADJUST_PRICING,
          action: ExtnAdjustPricingActionService
        },
      ],
      multi: true,
    },
    ];

    static readonly imports = [
        BucCommonComponentsModule,
        BucFeatureComponentsModule,
    ];

}
