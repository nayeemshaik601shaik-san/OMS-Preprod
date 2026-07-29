import { Injectable, OnDestroy } from '@angular/core';
import { ActionProcessorService } from '@buc/common-components';
import { ModalService } from 'carbon-components-angular';
import { Subscription } from 'rxjs';
import { Constants } from '@call-center/order-shared';
import { ActionParams } from '@call-center/order-shared';
import { ExtnAdjustPricingModalComponent } from '../../custom/adjust-pricing-modal/adjust-pricing-modal.component';

@Injectable()
export class ExtnAdjustPricingActionService implements OnDestroy {

    subscriptions: Subscription[] = [];

    constructor(private actionProcessorService: ActionProcessorService, private modalService: ModalService) {
        const sub = this.actionProcessorService.select<ActionParams>(Constants.ADJUST_PRICING).subscribe(({ params }) => {
            this.openAdjustPricingActionModal(params);
        });
        this.subscriptions.push(sub);
    }

    openAdjustPricingActionModal(params) {
        this.modalService.destroy();
        this.modalService.create({
            component: ExtnAdjustPricingModalComponent,
            inputs: {
                modalText: params.data.modalText,
                modalData: {
                    ...params.data.modalData,
                    successCallback: () => {
                        this.successCallBack(params);
                    },
                }
            }
        });
    }

    successCallBack(params) {
        this.actionProcessorService.dispatchUpdate(params.component, {
            action: Constants.ADJUST_PRICING,
            refresh: true,
            success: true
        });
    }

    ngOnDestroy() {
        this.subscriptions.forEach(s => s.unsubscribe());
    }
}