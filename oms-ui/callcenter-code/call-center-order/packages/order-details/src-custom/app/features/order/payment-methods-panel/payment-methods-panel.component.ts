/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2021, 2024
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

import { Component, Injector, Input, OnInit } from '@angular/core';
import { BucCommonCurrencyFormatPipe, EditableFieldRenderer, getArray, localeBuc2Angular } from '@buc/common-components';
import { BucBaseUtil, BucCommOmsMashupService, BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { BucMaskPipe } from '@call-center/order-shared';
import { Observable, of } from 'rxjs';
import { OrderSummaryService } from '../data-service/order-summary.service';


@Component({
  selector: 'call-center-payment-methods-panel',
  templateUrl: './payment-methods-panel.component.html',
  styleUrls: ['./payment-methods-panel.component.scss'],
})
export class PaymentMethodsPanelComponent extends EditableFieldRenderer implements OnInit {

  @Input() summaryDetails: any;
  @Input() paymentMethod: any;
  @Input() panelType: any;
  @Input() paymentIndex: any;
  givexDetails:any;

  PANEL_TYPE_COMMON = "common";
  PANEL_TYPE_METHOD = "method";

  FIELD_AMOUNT = "amount";
  FIELD_REFUND_AMOUNT = "refundAmount";
  FIELD_TOTAL_AUTHORIZED = "totalAuthorized";
  FIELD_TOTAL_CHARGED = "totalCharged";
  FIELD_PAYMENT_METHOD = "paymentMethod";
  FIELD_ACCOUNT_NUMBER = "accountNumber";
  FIELD_PAYMENT_REF_1 = "paymentRef1";
  FIELD_PAYMENT_REF_2 = "paymentRef2";
  FIELD_PAYMENT_REF_3 = "paymentRef3";
  FIELD_CHECK_ACC = "checkAccountNumber";
  FIELD_ROUTING = "routingNumber";
  FIELD_CHECK_REF = "checkRef";
  FIELD_SVC = "svcNumber";
  FIELD_CARD = "cardNumber";
  FIELD_EXPIRY = "expiryDate";
  FIELD_NAME_ON_CARD = "nameOnCard";
  
  isMasked = true;
  hasPermission:boolean;

  componentId = 'payment-method-panel';

  readonly resourceIds = {
    ICC_FULL_CARD_NO : 'ICC_FULL_CARD_NO'
  }

  maskSvcNo(svcNo: string): string {
    let maskedNo : any = this.bucMask.transform(svcNo);
    return svcNo ? maskedNo : '';
  }

  // Function to toggle between masked/unmasked
  toggleMask() {
    this.isMasked = !this.isMasked;
  }
  

  lcl = localeBuc2Angular(BucSvcAngularStaticAppInfoFacadeUtil.getUserLanguage());
  public currPipe: BucCommonCurrencyFormatPipe = new BucCommonCurrencyFormatPipe(this.lcl)
  
  constructor(
    inj: Injector,
    public bucMask: BucMaskPipe,
    private bucCommOmsMashupService : BucCommOmsMashupService  ) {
    super(inj);
  }

  ngOnInit(): void {
    this.initData()
  }

  private async initData() {
    if (this.panelType === this.PANEL_TYPE_COMMON){
      await this.initializeFieldDetailAttributes('order-payment-methods-common');
    }
    else if (this.panelType === this.PANEL_TYPE_METHOD) {
      if (this.paymentMethod.PaymentType === 'CHECK' || this.paymentMethod.PaymentType === 'REFUND_CHECK') {
          await this.initializeFieldDetailAttributes('order-payment-methods-check');
      } else if (this.paymentMethod.PaymentType === 'CUSTOMER_ACCOUNT' || this.paymentMethod.PaymentTypeGroup === 'CUSTOMER_ACCOUNT') {
          await this.initializeFieldDetailAttributes('order-payment-methods-customer-account');
      } else if (this.paymentMethod.PaymentType === 'CREDIT_CARD' || this.paymentMethod.PaymentTypeGroup === 'CREDIT_CARD') {
          await this.initializeFieldDetailAttributes('order-payment-methods-credit-card');
      } else if (this.paymentMethod.PaymentType === 'DEBIT_CARD' || this.paymentMethod.PaymentTypeGroup === 'DEBIT_CARD') {
          await this.initializeFieldDetailAttributes('order-payment-methods-debit-card');
      } else if (this.paymentMethod.PaymentType === 'SVC' || this.paymentMethod.PaymentTypeGroup === 'STORED_VALUE_CARD') {
          await this.initializeFieldDetailAttributes('order-payment-methods-svc');
      } else if (this.paymentMethod.PaymentType === 'GIFT_CARD') {
          await this.initializeFieldDetailAttributes('order-payment-methods-gift-card');
      } else {
          await this.initializeFieldDetailAttributes('order-payment-methods-other');
      }
    } 

    // EOMS-3699
    // if user has permission then only make a call to get decrypted details
    let resp:any;
    
    if (this.panelType === this.PANEL_TYPE_METHOD && this.paymentMethod?.PaymentType === 'Givex'){
      this.hasPermission = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIds.ICC_FULL_CARD_NO);
      
      if(this.hasPermission){
        await this.getGivexDecrytedDetails(this.paymentMethod).then((result) => {
          resp = {...result};
        });
      }

      this.givexDetails = {...resp};
    }
    await this.loadPageAttributes().toPromise();
  }

  protected fetchPageAttributeData(): Observable<Array<any>> {
    return of(getArray(this.paymentMethod));
  }

  protected getDataForAttribute(id: string, item: any): Promise<any> {
    let attr: any = {};
    let data: any = '';

    switch (id) {

      case this.FIELD_AMOUNT:
        if(item.PaidAmount >= 0){
          data = this.currPipe.transform(item.PaidAmount,
            this.summaryDetails.PriceInfo.Currency, 'symbol');
          attr = {
            attrTID: "payment-fields-common-" + this.paymentIndex
          };
          
        }
        else{
          this.removeAttributes([this.FIELD_AMOUNT])
        }
        break;
      case this.FIELD_PAYMENT_METHOD: 
        data = item.PaymentTypeDescription
        attr = {
          attrTID: "payment-fields-common-" + id + "-" + this.paymentIndex
        };
        break;
      case this.FIELD_REFUND_AMOUNT:
        if(item.TotalRefundedAmount >= 0){
          data = this.currPipe.transform(item.TotalRefundedAmount,
            this.summaryDetails.PriceInfo.Currency, 'symbol');
          attr = {
            attrTID: "payment-fields-common-" + id + "-" + this.paymentIndex
          };
        }
        else{
          this.removeAttributes([this.FIELD_REFUND_AMOUNT])
        }
        break;
      case this.FIELD_TOTAL_AUTHORIZED:
        data = this.currPipe.transform(item.TotalAuthorized,
          this.summaryDetails.PriceInfo.Currency, 'symbol');
        attr = {
          attrTID: "payment-fields-common-" + id + "-" + this.paymentIndex
        };
        break;
      case this.FIELD_TOTAL_CHARGED:
        data = this.currPipe.transform(item.TotalCharged,
          this.summaryDetails.PriceInfo.Currency, 'symbol');
        attr = {
          attrTID: "payment-fields-common-" + id + "-" + this.paymentIndex
        };
        break;
      case this.FIELD_ACCOUNT_NUMBER:
        data = this.bucMask.transform(item.PrimaryAccountNo);
        attr = {
          attrTID: this.componentId + "-fields-on-method-" + id + "-" + this.paymentIndex
        };
        break;
      case this.FIELD_PAYMENT_REF_1:
      case this.FIELD_CHECK_ACC:
        data = this.bucMask.transform(this.paymentMethod.PaymentReference1)
        attr = {
          attrTID: this.componentId + "-fields-on-method-" + id + "-" + this.paymentIndex
        };
        break;
      case this.FIELD_PAYMENT_REF_2:
        data = this.bucMask.transform(this.paymentMethod.PaymentReference2)
        attr = {
          attrTID: this.componentId + "-fields-on-method-" + id + "-" + this.paymentIndex
        };
        break;
      case this.FIELD_PAYMENT_REF_3:
      case this.FIELD_ROUTING:
        data = this.bucMask.transform(this.paymentMethod.PaymentReference3)
        attr = {
          attrTID: this.componentId + "-fields-on-method-" + id + "-" + this.paymentIndex
        };
        break;  

      case this.FIELD_SVC:
      case this.FIELD_CARD:
        // EOMS-3699
        // remove oob field if user has permission to view full card number
        if (this.paymentMethod?.PaymentType === 'Givex' && this.hasPermission){
          this.removeAttributes([this.FIELD_SVC])
        }else{
          data = this.bucMask.transform(this.paymentMethod.DisplayPrimaryAccountNo);
          attr = {
            attrTID: this.componentId + "-fields-on-method-" + id + "-" + this.paymentIndex
          };
        }
        break;
      case this.FIELD_CHECK_REF:
        data = this.bucMask.transform(this.paymentMethod.CheckNo)
        attr = {
          attrTID: this.componentId + "-fields-on-method-" + id + "-" + this.paymentIndex
        };
        break;
      case this.FIELD_EXPIRY:
        data = this.paymentMethod.CreditCardExpDate;
        attr = {
          attrTID: this.componentId + "-fields-on-method-" + id + "-" + this.paymentIndex
        };
        break;
      case this.FIELD_NAME_ON_CARD:
        const fullName = (item.FirstName ?? '') + ' ' + (item.LastName ?? '');
        data = BucBaseUtil.isVoid(fullName.trim()) ? '-' : fullName.trim();
        attr = {
          attrTID: this.componentId + '-fields-on-method-' + id + '-' + this.paymentIndex
        };
        break;
    }
    attr = { ...attr, data};
    return attr as any;
  }

  // EOMS-3699
  // This function will make a call to the custom mashup to get decrypted givex card number
  private async getGivexDecrytedDetails(paymentMethod) {
    const givexReq = {
      PaymentType: paymentMethod?.PaymentType,
      SvcNo: paymentMethod?.SvcNo
    };
    const input = {
      PaymentMethod: givexReq,
    };

    return this.bucCommOmsMashupService.callMashup('icc.order.summary.getDecryptedGivexDetails' , input, {})
    .toPromise()
    .then((mashupOutput => {
      const resp = this.bucCommOmsMashupService.getMashupOutput(mashupOutput, 'icc.order.summary.getDecryptedGivexDetails');
      return resp;
    }))

  }
}
