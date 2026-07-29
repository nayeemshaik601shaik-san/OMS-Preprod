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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, ViewContainerRef } from '@angular/core';
import { ActionProcessorService, BucCommonCurrencyFormatPipe, CCNotificationService, CommonBinaryOptionModalComponent, getCurrentLocale, localeBuc2Angular } from '@buc/common-components';
import { BucBaseUtil, BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { ActionParams, OrderCommonService } from '@call-center/order-shared';
import { Constants } from '@call-center/order-shared/lib/common/order.constants';
import { PaymentDataService } from '@call-center/order-shared/lib/data-service/payment-data-service';
import { TranslateService } from '@ngx-translate/core';
import { ModalService } from 'carbon-components-angular';
import { isEmpty, omit } from 'lodash';
import { Observable, Subscription } from 'rxjs';
import { ExtensionConstants } from '../../../extension.constants';
import { getCurrencySymbol } from '@angular/common';

@Component({
  selector: 'call-center-payment-details',
  templateUrl: './payment-details.component.html',
  styleUrls: ['./payment-details.component.scss'],
})
export class PaymentDetailsComponent implements OnInit, OnDestroy {
  EXTENSION = {
    TOP: ExtensionConstants.PAYMENT_DETAILS_CO_TOP,
    BOTTOM: ExtensionConstants.PAYMENT_DETAILS_CO_BOTTOM
  };
  @Input() orderHeaderKey;
  @Input() isLoader;

  isScreenInitialized = false;

  displayValue = [];
  paymentInfo: boolean;
  inputCharge: any = {};
  paymentMethods = [];
  isPaymentTypeDisabled = [];
  paymentMethodItems = [];
  isSelected = [];
  selectedPaymentKeys = [];
  capturePaymentList: any = [];
  isAmountExceeded = [false];
  exceedAmountError = [false];
  origRequestedAmount = {};
  requestedAmount: any;
  amountDueText: any;
  enterpriseCode: any;
  currency: any;
  orderNo: any;
  chargeTransactionDetails: any;
  pendingAmount: any;
  isAmountDue: boolean;
  amountDue: any;
  summaryDetails: any;
  isErrorInPayment = false;
  isInitial = true;
  actionSub: Subscription;
  displayBalanceLink = [false];
  isResourceAllowedForModifyPaymentMethod: boolean;
  isResourceAllowedForRemovePaymentMethod: boolean;
  curLocale: any;
  private eventsSubscription: Subscription;

  //EOMS-1463
  isAddPaymentDisabled: boolean = false;

  @Input() events: Observable<void>;
  @Input() isAddLines: any;
  lcl = localeBuc2Angular(BucSvcAngularStaticAppInfoFacadeUtil.getUserLanguage());
  public currPipe: BucCommonCurrencyFormatPipe = new BucCommonCurrencyFormatPipe(this.lcl)

  componentId = 'payment-details';

  readonly resourceIdsForActions = {
    ADD_MODIFY_PAYMENT_METHOD : 'ICC000007',
    REMOVE_PAYMENT_METHOD: 'ICC000008'
  }

  nlsMap: any = {
    'PAYMENT.GENERAL.LABEL_DUE': '',
    'PAYMENT.GENERAL.LABEL_SUFFICIENT_FUNDS_CREATE_ORDER': '',
    'PAYMENT.GENERAL.MSG_INVALID_ENTRY_IN_AMOUNT_TO_CHARGE': '',
    'PAYMENT.GENERAL.MSG_CANNOT_ADD_MORE_THAN_FUNDS_AVALABLE': '',
    'PAYMENT.GENERAL.MSG_CANNOT_PAY_MORE_THAN_DUE_AMOUNT': '',
    'PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.BUTTON_CANCEL': '',
    'PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.BUTTON_YES': '',
    'PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.LABEL_HEADER_REMOVE': '',
    'PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.LABEL_HEADER_SUSPEND': '',
    'PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.LABEL_CONFIRM_REMOVE_TEXT': '',
    'PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.LABEL_CONFIRM_SUSPEND_TEXT': '',
    'PAYMENT.GENERAL.MSG_INVALID_ENTRY_FOR_PAYMENT': '',
    'PAYMENT.GENERAL.MSG_ERROR_GETTING_FUNDS_AVAILABLE_NO_CUSTOMER': '',
    'PAYMENT.GENERAL.MSG_UNABLE_TO_GET_FUNDS': '',
    'PAYMENT.GENERAL.MSG_NO_FUNDS_AVAILABLE': '',
    'PAYMENT.GENERAL.MSG_ERROR_GETTING_FUNDS_AVAILABLE_NO_CUST_ACCOUNT': '',
    'PAYMENT_SUMMARY.PAYMENT_METHODS.LABEL_ENDING_WITH_TITLE': '',
    'PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.MESSAGE_REMOVE_PAYMENT_METHOD_SUCCESS': '',
    'PAYMENT_SUMMARY.ADD_PAYMENT_METHOD.MSG_PAYMENT_ADD_SUCCESS':''
  };

  constructor(public translate: TranslateService,
    private paymentDataService: PaymentDataService,
    public actionProcessorService: ActionProcessorService,
    public modalService: ModalService,
    public ccNotificationService: CCNotificationService,
    private orderCommonService: OrderCommonService,
    private ref: ChangeDetectorRef,
    ) { }

  ngOnInit(): void {
    
    this.initialize();
    this.eventsSubscription = this.events.subscribe(() => this.getCompleteOrderInfo());
  }

  async initialize(): Promise<any> {
    this.curLocale = getCurrentLocale();
    if (this.curLocale.startsWith('zh')) {
        this.curLocale = 'zh';
    }
    await this._initTranslations();
    await this.getCompleteOrderInfo();
    this.checkAllowedModAndResourceId();
    await this.getPaymentMethodList();
    this.isScreenInitialized = true;
    if (this.paymentMethods.length) {
      this.initializeAccordion();
    }
  }

  async getCompleteOrderInfo(): Promise<any> {
    await this.orderCommonService.getCompleteOrderDetails(this.orderHeaderKey).then(response => {
      this.summaryDetails = response.Order;
      this.enterpriseCode = this.summaryDetails.EnterpriseCode;
      this.currency = this.summaryDetails.PriceInfo.Currency;
      this.orderNo = this.summaryDetails.OrderNo;
      this.chargeTransactionDetails = this.summaryDetails.ChargeTransactionDetails;
      this.pendingAmount = this.chargeTransactionDetails.FundsRequiredOnOrder;

      //EOMS-1463
      this.isAddPaymentDisabled = Number(this.summaryDetails?.PriceInfo?.TotalAmount) === 0;
    });
    if (!isEmpty(this.inputCharge)) {
      this.calculateDueAmount(this.inputCharge);
    }
  }

  initializeAccordion() {
    setTimeout(() => {
      let openAccordionIndex = -1;
      for (const index in this.paymentMethods) {
        const idx = Number(index);
        const paymentMethod = this.paymentMethods[idx];

        if(paymentMethod.SuspendAnyMoreCharges && paymentMethod.SuspendAnyMoreCharges !== Constants.CHECK_YES) {
          if ((paymentMethod.ChargeUpToAvailable === Constants.CHECK_YES || paymentMethod.PaymentTypeGroup === Constants.CUSTOMER_ACCOUNT) && !Number(paymentMethod.FundsAvailable)) {
            this.isSelected[idx] = false;
            this.isPaymentTypeDisabled[idx] = true;
            continue;
          } else {
            if (paymentMethod.IsDefaultMethod === 'Y') {
              this.isSelected[idx] = true;
              this.isPaymentTypeDisabled[idx] = false;
              openAccordionIndex = idx;
            }
            this.isPaymentTypeDisabled[idx] = false;
            this.isSelected[idx] = true;
            this.openAccordion(idx);
            if (openAccordionIndex === -1) {
              openAccordionIndex = idx;
            }
          }
        } else {
          this.isSelected[idx] = false;
          this.isPaymentTypeDisabled[idx] = true;
        }
      }
      if (openAccordionIndex !== -1) {
        if(this.isInitial) {
          this.capturePaymentList.push(...this.paymentMethods);
        }
        this.isSelected[openAccordionIndex] = true;
        this.isPaymentTypeDisabled[openAccordionIndex] = false;
        this.openAccordion(openAccordionIndex);
      }
      this.isInitial = false;
    });
  }

  protected async _initTranslations(): Promise<any> {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translate.get(keys).toPromise();
    keys.forEach(k => this.nlsMap[k] = json[k]);
  }

  /**
   * To fetch the list of payment methods
   */
   async getPaymentMethodList() {
    const customerPaymentMethodList = await this.getCustomerPaymentMethods();
    const paymentMethodsObj = this.paymentDataService.prepareDisplayPaymentMethods(this.summaryDetails, customerPaymentMethodList);
    if (paymentMethodsObj.paymentMethodsAvailable) {
      this.paymentMethods = paymentMethodsObj.paymentDetails?.paymentDetail;
      this.checkBalanceLink(this.paymentMethods);
      this.managePaymentMethod(this.paymentMethods);
      await this._populateTitles();
    } else {
      const fundsRequiredOnOrder = Number(this.summaryDetails?.ChargeTransactionDetails?.FundsRequiredOnOrder);
      await this.preparePaymentBanner(fundsRequiredOnOrder);
    }
  }

  private async _populateTitles(){
    for (let i = 0; i < this.paymentMethods.length; i++) {
      this.paymentMethods[i]['paymentTypeHeading'] = this.paymentMethods[i].PaymentTypeDescription ? await this._getNls('PAYMENT_SUMMARY.PAYMENT_METHODS.LABEL_ENDING_WITH_TITLE', {title: this.paymentMethods[i].PaymentTypeDescription}): "";
      this.paymentMethods[i]['creditCardTypeHeading'] = this.paymentMethods[i].CreditCardType ? await this._getNls('PAYMENT_SUMMARY.PAYMENT_METHODS.LABEL_ENDING_WITH_TITLE', {title: this.paymentMethods[i].CreditCardType}): this.paymentMethods[i]['paymentTypeHeading'];
    }
  }


  async getCustomerPaymentMethods() {
    const input = {
      Customer: {
        CustomerID: this.summaryDetails.BillToID,
        OrganizationCode: this.summaryDetails.EnterpriseCode,
        CustomerContact: {
          CustomerContactID: this.summaryDetails.CustomerContactID
        }
      }
    };
    try {
      const resp = await this.paymentDataService.getCustomerPaymentMethodList(input);
      const customerPaymentMethodList = resp.CustomerPaymentMethodList.CustomerPaymentMethod;
      return customerPaymentMethodList;
    } catch (err) {
      console.error(err);
    }
  }

  checkBalanceLink(paymentMethods) {
    paymentMethods.forEach((element, i) => {
      if ((element.ChargeUpToAvailable === 'Y' || element.PaymentTypeGroup === Constants.CUSTOMER_ACCOUNT) 
        && (BucBaseUtil.isVoid(element.FundsAvailable) || Number(element.FundsAvailable) === 0)
      ) {
        this.displayBalanceLink[i] = true;
      } else {
        this.displayBalanceLink[i] = false;
      }
    });
  }

  checkAllowedModAndResourceId() {
    // Resource permission check from within component ts
    this.isResourceAllowedForModifyPaymentMethod =
      BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForActions.ADD_MODIFY_PAYMENT_METHOD);
    this.isResourceAllowedForRemovePaymentMethod =
      BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForActions.REMOVE_PAYMENT_METHOD);
  }

  async callAddNewPaymentMethod(data) {
    // make sure value is formatted to two decimal places
    if (data.MaxChargeLimit) {
      data.MaxChargeLimit = (Math.round(data.MaxChargeLimit * 100) / 100).toFixed(2)
    }
    const addedPaymentMethod = await this.paymentDataService.prepareNewPaymentObject(data);
    const newPaymentMethod = addedPaymentMethod?.newPaymentMethodDetails;
    if (newPaymentMethod) {
      newPaymentMethod.PaymentKey = this.paymentMethods.length;
      let updatedValue;
      const currSymbol = getCurrencySymbol(this.currency, 'narrow', this.curLocale);
      if (newPaymentMethod.ChargeUpToAvailable === Constants.CHECK_YES || newPaymentMethod.PaymentTypeGroup === Constants.CUSTOMER_ACCOUNT) {
        if(data.FundsAvailable) {
          newPaymentMethod.FundsAvailable = data.FundsAvailable;
        } else {
          const availableFunds = await this.getBalance(newPaymentMethod);
           newPaymentMethod.FundsAvailable = availableFunds;
        }
        if (Number(newPaymentMethod.FundsAvailable) === 0 || (newPaymentMethod.FundsAvailable) === undefined) {
          this.inputCharge[newPaymentMethod.PaymentKey] = '';
          this.displayValue[newPaymentMethod.PaymentKey] = '';
          newPaymentMethod.RequestedAmount = '';
          this.preparePaymentBanner(this.pendingAmount);
        } else {
          if (newPaymentMethod.FundsAvailable && Number(newPaymentMethod.FundsAvailable) < Number(this.pendingAmount)) {
            this.inputCharge[newPaymentMethod.PaymentKey] = newPaymentMethod.FundsAvailable;
            updatedValue = this.currPipe.transform(newPaymentMethod.FundsAvailable, this.currency, 'symbol');
            updatedValue = updatedValue.replace(currSymbol, '').trim();
            this.displayValue[newPaymentMethod.PaymentKey] = new String(updatedValue);
            newPaymentMethod.RequestedAmount = newPaymentMethod.FundsAvailable;
          } else {
            newPaymentMethod.RequestedAmount = data.MaxChargeLimit ? data.MaxChargeLimit : this.pendingAmount;
            this.inputCharge[newPaymentMethod.PaymentKey] = newPaymentMethod.RequestedAmount;
            updatedValue = this.currPipe.transform(newPaymentMethod.RequestedAmount, this.currency, 'symbol');
            updatedValue = updatedValue.replace(currSymbol, '').trim();
            this.displayValue[newPaymentMethod.PaymentKey] = new String(updatedValue);
          }
        }
      } else {
        newPaymentMethod.RequestedAmount = data.MaxChargeLimit ? data.MaxChargeLimit : this.pendingAmount;
        this.inputCharge[newPaymentMethod.PaymentKey] = newPaymentMethod.RequestedAmount;
        updatedValue = this.currPipe.transform(newPaymentMethod.RequestedAmount, this.currency, 'symbol');
        updatedValue = updatedValue.replace(currSymbol, '').trim();
        this.displayValue[newPaymentMethod.PaymentKey] = new String(updatedValue);
      }
      this.calculateDueAmount(this.inputCharge, newPaymentMethod, this.inputCharge[newPaymentMethod.PaymentKey]);
      newPaymentMethod['paymentTypeHeading'] = newPaymentMethod['paymentTypeHeading'] ||
        newPaymentMethod.PaymentTypeDescription ? await this._getNls('PAYMENT_SUMMARY.PAYMENT_METHODS.LABEL_ENDING_WITH_TITLE', {title: newPaymentMethod.PaymentTypeDescription}): "";
      newPaymentMethod['creditCardTypeHeading'] = newPaymentMethod['creditCardTypeHeading'] ||
        newPaymentMethod.CreditCardType ? await this._getNls('PAYMENT_SUMMARY.PAYMENT_METHODS.LABEL_ENDING_WITH_TITLE', {title: newPaymentMethod.CreditCardTypeDesc || newPaymentMethod.CreditCardType}): "";
      this.paymentMethods.push(newPaymentMethod);
      setTimeout(() => {
        const lastItemIndex = this.paymentMethods.length - 1;
        const lastItem = this.paymentMethods[lastItemIndex];
        if ((lastItem.ChargeUpToAvailable === Constants.CHECK_YES || lastItem.PaymentTypeGroup === Constants.CUSTOMER_ACCOUNT) && !Number(lastItem.FundsAvailable)) {
          this.isSelected[lastItemIndex] = false;
          this.isPaymentTypeDisabled[lastItemIndex] = true;
        } else {
          this.isSelected[lastItemIndex] = true;
          this.isPaymentTypeDisabled[lastItemIndex] = false;
          this.openAccordion(lastItemIndex);
        }
      });
    }
  }

  async getFundsAvailable(paymentMethod) {
    let availableFunds;
    await this.paymentDataService.getFundsAvailable(paymentMethod, this.summaryDetails.DocumentType, this.summaryDetails.enterpriseCode).then(
      mashupOutput => {
        availableFunds = mashupOutput.InvokeUE.XMLData.PaymentMethod.FundsAvailable;
      }
    );
    return availableFunds;
  }

  cancelAddPayment(context: any) {
    this.paymentInfo = context.paymentInfo;
  }

  /**
   * To open the accordion when user checks the checkbox
   */
  toggleAccordian(event, index, item) {
    const div = document.getElementsByClassName('cds--form-item cds--checkbox-wrapper');
    const element = div[index].closest('.accordion');
    const panel = element.firstChild.nextSibling as HTMLElement;
    this.isSelected[index] = event.checked;
    if (event.checked) {
      this.selectedPaymentKeys.push(item.PaymentKey);
      panel.style.maxHeight = (panel.scrollHeight + 16) + 'px';
    } else {
      const keyIndex = this.selectedPaymentKeys.indexOf(item.PaymentKey);
      if (keyIndex > -1) {
        this.selectedPaymentKeys.splice(keyIndex, 1);
      }
      panel.style.maxHeight = null;
      this.inputCharge[item.PaymentKey] = '';
      this.calculateDueAmount(this.inputCharge, item, this.inputCharge[item.PaymentKey]);
    }
  }

  managePaymentMethod(paymentMethods) {
    for(const paymentMethod of paymentMethods){
      if (paymentMethod.MaxChargeLimit && !this.isAddLines) {
        this.inputCharge[paymentMethod.PaymentKey] = paymentMethod.MaxChargeLimit;
      } else if (this.isAddLines) {
        this.inputCharge[paymentMethod.PaymentKey] = '';
      }
    }
    this.calculateDueAmount(this.inputCharge);
  }

  private calculateDueAmount(inputCharge, item?, changedValue?) {
    const fundsRequiredOnOrder = this.summaryDetails?.ChargeTransactionDetails?.FundsRequiredOnOrder;
    const totalEnteredAmount =  Object.values(inputCharge).map(a => Number(a)).reduce((a,b) => a + b, 0);
    this.pendingAmount = parseFloat(fundsRequiredOnOrder) === 0 ? 0 : this.subtractValues(fundsRequiredOnOrder, totalEnteredAmount);

    if (item) {
      if (this.pendingAmount < 0) {
        this.isAmountExceeded[item.PaymentKey] = true;
        this.isErrorInPayment = true;
        this.isAmountDue = false;
        this.exceedAmountError[item.PaymentKey] = this.nlsMap['PAYMENT.GENERAL.MSG_INVALID_ENTRY_IN_AMOUNT_TO_CHARGE'];
        this.preparePaymentBanner(this.pendingAmount, item);
      } else {
        if ((Number(changedValue) > Number(item.FundsAvailable))
          && (item.ChargeUpToAvailable === Constants.CHECK_YES || item.PaymentTypeGroup === Constants.CUSTOMER_ACCOUNT)
        ) {
          this.amountDueText = this.nlsMap['PAYMENT.GENERAL.MSG_CANNOT_ADD_MORE_THAN_FUNDS_AVALABLE'];
          this.isErrorInPayment = true;
          this.isAmountExceeded[item.PaymentKey] = true;
          this.isErrorInPayment = true;
          this.isAmountDue = true;
          this.exceedAmountError[item.PaymentKey] = this.nlsMap['PAYMENT.GENERAL.MSG_INVALID_ENTRY_IN_AMOUNT_TO_CHARGE'];
        } else {
          this.isAmountExceeded[item.PaymentKey] = false;
          if (!changedValue) {
            this.origRequestedAmount[item.PaymentKey] = '';
          } else {
            this.origRequestedAmount[item.PaymentKey] = this.setRequestedAmount(item.PaidAmount, changedValue);
          }
          this.preparecapturePaymentInput(item, this.origRequestedAmount);
          this.amountDue = this.currPipe.transform(Number(this.pendingAmount), this.currency, 'symbol');
          this.preparePaymentBanner(this.pendingAmount, item);
        }
      }
    } else {
      this.amountDue = this.currPipe.transform(this.pendingAmount, this.currency, 'symbol');
      this.preparePaymentBanner(this.pendingAmount, item);
    }
  }

  async preparePaymentBanner(fundsRequiredOnOrder, item?) {
    if (Number(fundsRequiredOnOrder) > 0) {
      this.amountDue = this.currPipe.transform(fundsRequiredOnOrder, this.currency, 'symbol');
      this.amountDueText = this.amountDue + ' ' + this.nlsMap['PAYMENT.GENERAL.LABEL_DUE'];
      this.isAmountDue = true;
      this.isErrorInPayment = false;
    } else if (Number(fundsRequiredOnOrder) === 0) {
      this.amountDueText = this.nlsMap['PAYMENT.GENERAL.LABEL_SUFFICIENT_FUNDS_CREATE_ORDER'];
      this.isAmountDue = false;
      this.isErrorInPayment = false;
      if (item) {
        let updatedValue = this.displayValue[item.PaymentKey];
        updatedValue = this.currPipe.transform(this.inputCharge[item.PaymentKey], this.currency, 'symbol');
        const currSymbol = getCurrencySymbol(this.currency, 'narrow', this.curLocale);
        updatedValue = updatedValue.replace(currSymbol, '').trim();
        this.displayValue[item.PaymentKey] = new String(updatedValue);
      }
    } else if (fundsRequiredOnOrder < 0) {
      this.amountDueText = await this._getNls('PAYMENT.GENERAL.MSG_CANNOT_PAY_MORE_THAN_DUE_AMOUNT',
        { amount: this.currPipe.transform(this.summaryDetails?.ChargeTransactionDetails?.FundsRequiredOnOrder, this.currency, 'symbol') })
      this.isAmountDue = false;
      this.isErrorInPayment = true;
    }
  }

  /**
   * To open the accordion if checbox is already checked by default
   */
   openAccordion(index) {
    const div = document.getElementsByClassName('cds--form-item cds--checkbox-wrapper');
    const element = div[index].closest('.accordion');
    const panel = element.firstChild.nextSibling as HTMLElement;
    panel.style.maxHeight = (panel.scrollHeight + 16) + 'px';
  }

  /**
   * To calculate the correct requested amount for capture payment input
   */
   private setRequestedAmount(paidAmount, enteredValue) {
    if (paidAmount > 0) {
      this.requestedAmount = parseFloat(paidAmount) + parseFloat(enteredValue);
    } else {
      this.requestedAmount = parseFloat(enteredValue);
    }
    return this.requestedAmount;
  }

  preparecapturePaymentInput(item, origRequestedAmount) {
    let capturePaymentObj: any = {};
    if (item.isCustom) {
      capturePaymentObj = item;
    } else {
      capturePaymentObj.PaymentReference1 = item.PaymentReference1;
      capturePaymentObj.PaymentReference2 = item.PaymentReference2;
      capturePaymentObj.PaymentReference3 = item.PaymentReference3;
      capturePaymentObj.CustomerAccountNo = item.CustomerAccountNo;
      capturePaymentObj.CreditCardNo = item.CreditCardNo;
      capturePaymentObj.CreditCardExpDate = item.CreditCardExpDate;
      capturePaymentObj.CreditCardType = item.CreditCardType;
      capturePaymentObj.DebitCardNo = item.DebitCardNo;
      capturePaymentObj.SvcNo = item.SvcNo;
      capturePaymentObj.DisplayCreditCardNo = item.DisplayCreditCardNo;
      capturePaymentObj.DisplayDebitCardNo = item.DisplayDebitCardNo;
      capturePaymentObj.DisplaySvcNo = item.DisplaySvcNo;
      capturePaymentObj.PaymentType = item.PaymentType,
      capturePaymentObj.IsDefaultMethod = item.IsDefaultMethod;
      capturePaymentObj.CheckNo =  item.CheckNo,
      capturePaymentObj.CheckReference = item.CheckReference;
      capturePaymentObj.RequestedAmount = origRequestedAmount[item.PaymentKey];
      capturePaymentObj.IsCustomMethod = item.IsCustomMethod || false;
      capturePaymentObj.PaymentKey = item.PaymentKey;
      capturePaymentObj = Object.assign(capturePaymentObj, this.getDisplayAccountNo(item));
    }

    const newObjIndex = this.capturePaymentList.findIndex(el => el.PaymentKey === item.PaymentKey);
    if (newObjIndex > -1) {
      this.capturePaymentList.splice(newObjIndex, 1, capturePaymentObj);
    } else {
      this.capturePaymentList.push(capturePaymentObj);
    }
  }

  async onDisplayChange($event, item) {
    if ($event) {
      this.displayValue[item.PaymentKey] = $event;
    }
  }
  /**
   * When customer edits the amount to charge field, this method would be called
   */
  async onAmountToPayChange(changedValue, item) {
    if (!changedValue || !isNaN(changedValue?.toString())) {
      this.inputCharge[item.PaymentKey] = changedValue ? Number(changedValue) : undefined;
      this.calculateDueAmount(this.inputCharge, item, this.inputCharge[item.PaymentKey]);
    }
  }

  focusOut(event, item) {
    const changedValue = Number(event.target.value);
    if (!isNaN(changedValue)) {
      this.inputCharge[item.PaymentKey] = changedValue.toFixed(2);
    }
  }

  onAddRemainder(item) {
    const origPendingAmount = isEmpty(this.inputCharge[item.PaymentKey]) ? this.pendingAmount : this.addValues(this.pendingAmount, this.inputCharge[item.PaymentKey]);
    if ((Number(item.FundsAvailable) < Number(origPendingAmount) && (item.ChargeUpToAvailable === Constants.CHECK_YES || item.PaymentTypeGroup === Constants.CUSTOMER_ACCOUNT))) {
      if (isEmpty(this.inputCharge)) {
        this.inputCharge[item.PaymentKey] = item.FundsAvailable;
      } else {
        const amountToBeAdded = this.subtractValues(item.FundsAvailable, this.inputCharge[item.PaymentKey]);
        if (isEmpty(this.inputCharge[item.PaymentKey])) {
          this.inputCharge[item.PaymentKey] = item.FundsAvailable;
        } else {
          this.inputCharge[item.PaymentKey] = this.addValues(amountToBeAdded, this.inputCharge[item.PaymentKey]);
        }
      }
    } else {
      this.inputCharge[item.PaymentKey] = origPendingAmount;
    }
    this.calculateDueAmount(this.inputCharge, item, this.inputCharge[item.PaymentKey]);
  }

  /**
   * To display the payment activity modal
   */
   showPaymentActivity(selectedPaymentMethod) {
    this.actionProcessorService.dispatch<ActionParams>(Constants.PAYMENT_ACTIVITY, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          summaryDetails: this.summaryDetails,
          paymentMethodDetails: selectedPaymentMethod
        }
      }
    });
    this.actionSub = this.actionProcessorService.selectUpdate<any>(this.componentId).subscribe(res => {
      if (res.params.refresh) {
      }
    });
  }

  /**
   * This will be called to remove a payment method from an order
   */
   removePaymentMethod(event) {
    const optionOne = {
      primary: '',
      callOnClose: true,
      callback: this._callbackOnCancelModal.bind(this),
      text: this.nlsMap['PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.BUTTON_CANCEL']
    };
    const optionTwo = {
      class: {
        primary: true
      },
      callback: this._callbackOnRemovePaymentMethod.bind(this, event),
      callOnClose: true,
      text: this.nlsMap['PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.BUTTON_YES']
    };

    this.modalService.destroy();
      this.modalService.create({
        component: CommonBinaryOptionModalComponent,
        inputs: {
          modalText: {
            header: event.IsCustomMethod || BucBaseUtil.isVoid(event.PaymentKey) ? this.nlsMap['PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.LABEL_HEADER_REMOVE'] : this.nlsMap['PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.LABEL_HEADER_SUSPEND'],
            label: event.IsCustomMethod || BucBaseUtil.isVoid(event.PaymentKey) ? this.nlsMap['PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.LABEL_CONFIRM_REMOVE_TEXT'] : this.nlsMap['PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.LABEL_CONFIRM_SUSPEND_TEXT'],
            size: 'sm'
          },
          optionOne,
          optionTwo,
        }
      });
  }

  private _callbackOnCancelModal() {
    return true;
  }

  _callbackOnRemovePaymentMethod(selectedPaymentMethod) {
    if (selectedPaymentMethod.IsCustomMethod || BucBaseUtil.isVoid(selectedPaymentMethod.PaymentKey)) {
      this.removePaymentMethodFromArray(selectedPaymentMethod);
    } else if (selectedPaymentMethod.PaidAmount === 0) {
      this.removeSavedPaymentMethod(selectedPaymentMethod);
    }
  }

  removePaymentMethodFromArray(selectedPaymentMethod) {
    const index = this.paymentMethods.findIndex(el => el.PaymentKey === selectedPaymentMethod.PaymentKey);
    if (index > -1) {
      this.inputCharge[selectedPaymentMethod.PaymentKey] = '';
      this.calculateDueAmount(this.inputCharge);
      const successMsg = this.nlsMap['PAYMENT_SUMMARY.REMOVE_PAYMENT_METHOD.MESSAGE_REMOVE_PAYMENT_METHOD_SUCCESS'];
      this.ccNotificationService.notify({
        type: 'success',
        title: successMsg
      });
      this.paymentMethods.splice(index, 1);
    }
  }

  removeSavedPaymentMethod(selectedPaymentMethod) {
    const paymentMethodsInput = {
      Action: 'REMOVE',
      PaymentKey: selectedPaymentMethod.PaymentKey
    };
    this.paymentDataService.removePaymentMethodFromOrder(this.orderHeaderKey, paymentMethodsInput).then(
      mashupOutput => {
        if (mashupOutput.Order) {
          this.removePaymentMethodFromArray(selectedPaymentMethod);
        }
      }
    );
  }

  async onCheckBalance(item) {
    const index = this.paymentMethods.findIndex(el => el.PaymentKey === item.PaymentKey);
    this.displayBalanceLink[index] = false;
    this.ref.detectChanges();
    await this.getBalance(item);
  }

  private async getBalance(paymentMethod) {
    let availableFunds;
    if (paymentMethod.ChargeUpToAvailable === 'Y') {
      availableFunds = await this.getFundsAvailable(paymentMethod);
      if (Number(availableFunds) > 0) {
        const index = this.paymentMethods.findIndex(el => el.PaymentTypeGroup === paymentMethod.PaymentTypeGroup && el.PrimaryAccountNo === paymentMethod.PrimaryAccountNo && el.PaymentType === paymentMethod.PaymentType);
        if (index > -1) {
          this.paymentMethods[index].FundsAvailable = availableFunds;
          this.isSelected[index] = false;
          this.isPaymentTypeDisabled[index] = false;
        }
      } else {
        const errorMsg = this.nlsMap['PAYMENT.GENERAL.MSG_NO_FUNDS_AVAILABLE'];
        this.ccNotificationService.notify({
          type: 'error',
          title: errorMsg
        });
      }
    } else if (paymentMethod.PaymentTypeGroup === 'CUSTOMER_ACCOUNT') {
      if (!this.summaryDetails.BillToID) {
        const errorMsg = this.nlsMap['PAYMENT.GENERAL.MSG_ERROR_GETTING_FUNDS_AVAILABLE_NO_CUSTOMER'];
        this.ccNotificationService.notify({
          type: 'error',
          title: errorMsg
        });
      } else {
        availableFunds = await this.callGetCustomerAccountBalance(paymentMethod);
      }
    }
    return availableFunds;
  }

  private async callGetCustomerAccountBalance(paymentMethod) {
    let availableFunds;
    const customerPaymentMethodList = await this.getCustomerPaymentMethods();
    const index = customerPaymentMethodList.findIndex(el => (el.PaymentTypeGroup === paymentMethod.PaymentTypeGroup) &&
      (el.PaymentType === paymentMethod.PaymentType) && (el.PrimaryAccountNo === paymentMethod.CustomerAccountNo));
    if (index > -1) {
      availableFunds = customerPaymentMethodList[index].AvailableAccountBalance;
      if (Number(availableFunds) > 0) {
        this.paymentMethods[index].FundsAvailable = availableFunds;
        this.isSelected[index] = false;
        this.isPaymentTypeDisabled[index] = false;
      } else {
        const errorMsg = this.nlsMap['PAYMENT.GENERAL.MSG_NO_FUNDS_AVAILABLE'];
        this.ccNotificationService.notify({
          type: 'error',
          title: errorMsg
        });
      }
    } else {
      const errorMsg = this.nlsMap['PAYMENT.GENERAL.MSG_ERROR_GETTING_FUNDS_AVAILABLE_NO_CUST_ACCOUNT'];
      this.ccNotificationService.notify({
        type: 'error',
        title: errorMsg
      });
    }
    return availableFunds;
  }

  savePaymentMethods() {
    this.prepareSavePaymentMethodInput(this.paymentMethods)
    return true;
  }

  prepareSavePaymentMethodInput(paymentMethods) {
    const paymentMethodsInput = {
      PaymentMethod: []
    };

    for(const paymentMethod of paymentMethods) {
        let paymentMethodObj = {};
        if (paymentMethod) {
          paymentMethodObj = paymentMethod.isCustom
            ? paymentMethod
            : (typeof paymentMethod.PaymentKey === 'string'
                ? this.prepareSaveExistingPaymentMethodInput(paymentMethod)
                : this.prepareSaveNewPaymentMethodInput(paymentMethod)
            )
        }
        paymentMethodsInput.PaymentMethod.push(paymentMethodObj);
    }
    if (paymentMethodsInput.PaymentMethod.length > 0) {
      this.paymentDataService.addPaymentMethodToOrder(this.orderHeaderKey, paymentMethodsInput);
    }
  }

  async payDueAmount() {
    let paymentSuccessful = false;
    if (Number(this.pendingAmount) === 0) {
      // There is no amount owed, so it payment can be initialized to successful
      paymentSuccessful = true;
      for(const capturePayment of this.capturePaymentList) {
        const selectedIndex = this.paymentMethods.findIndex(el => (el.PaymentKey === capturePayment.PaymentKey) || (el.IsDefaultMethod === capturePayment.IsDefaultMethod));
        if (this.isSelected[selectedIndex] && !Number(capturePayment.RequestedAmount)) {
          this.isErrorInPayment = true;
          this.isAmountDue = true;
          this.isAmountExceeded[capturePayment.PaymentKey] = true;
          this.exceedAmountError[capturePayment.PaymentKey] = this.nlsMap['PAYMENT.GENERAL.MSG_INVALID_ENTRY_IN_AMOUNT_TO_CHARGE'];
          this.amountDueText = this.nlsMap['PAYMENT.GENERAL.MSG_INVALID_ENTRY_FOR_PAYMENT'];
         }
      }
      if (this.isErrorInPayment) {
        return;
      }
      const paymentMethodsInput = {
        PaymentMethod: []
      };

      for(const capturePayment of this.capturePaymentList) {
        const selectedIndex = this.paymentMethods.findIndex(el => (el.PaymentKey === capturePayment.PaymentKey) || (el.IsDefaultMethod === capturePayment.IsDefaultMethod));
        if (this.isSelected[selectedIndex]) {
          let paymentMethodObj = {};
          if (capturePayment.isCustom) {
            // omit fields that are not supported by API
            paymentMethodObj = omit(capturePayment, ['ChargeUpToAvailable', 'FundsAvailable', 'MaxChargeLimit'] );
          } else if ((capturePayment.IsCustomMethod) || (capturePayment.IsDefaultMethod === Constants.CHECK_YES)) {
            paymentMethodObj = {
              Operation: 'Manage',
              PaymentReference1: capturePayment.PaymentReference1,
              PaymentReference2: capturePayment.PaymentReference2,
              PaymentReference3: capturePayment.PaymentReference3,
              CustomerAccountNo: capturePayment.CustomerAccountNo,
              PaymentType: capturePayment.PaymentType,
              CheckNo: capturePayment.CheckNo,
              CheckReference: capturePayment.CheckReference,
              CreditCardNo: capturePayment.CreditCardNo,
              CreditCardExpDate: capturePayment.CreditCardExpDate,
              CreditCardType: capturePayment.CreditCardType,
              DebitCardNo: capturePayment.DebitCardNo,
              SvcNo: capturePayment.SvcNo,
              ...this.getDisplayAccountNo(capturePayment),
              RequestedAmount: capturePayment.RequestedAmount
            }
          } else {
            paymentMethodObj = {
              IsCorrection: this.isAddLines ? 'N' : 'Y',
              Operation: 'Manage',
              PaymentKey: capturePayment.PaymentKey,
              RequestedAmount: capturePayment.RequestedAmount
            }
            if (capturePayment.ToResume === 'Y') {
              Object.assign(paymentMethodObj, {ResetSuspensionStatus : 'Y'});
            }
          }
          paymentMethodsInput.PaymentMethod.push(paymentMethodObj);
        }
      }
      if (paymentMethodsInput.PaymentMethod.length > 0) {
        await this.paymentDataService.payDueAmount(this.orderHeaderKey, this.enterpriseCode, paymentMethodsInput).then(
          mashupOutput => {
            if (mashupOutput.Order.ErrorFound !== Constants.CHECK_YES) {
              paymentSuccessful = true;
            } else {
              paymentSuccessful = false;
            }
          }
        );
      }
    } else {
      this.isErrorInPayment = true;
      this.isAmountDue = Number(this.pendingAmount) < 0 ? false : true;
    }
    return paymentSuccessful;
  }

  addValues(a, b) {
    return (parseFloat(a) + parseFloat(b)).toFixed(2);
  }

  subtractValues(a, b) {
    return (parseFloat(a) - parseFloat(b)).toFixed(2);
  }

  private getDisplayAccountNo(paymentMethod) {
    const data: any = {};
    if (paymentMethod.PaymentTypeGroup === Constants.DEBIT_CARD){
      data.DisplayPrimaryAccountNo = data.DisplayDebitCardNo = (paymentMethod.DisplayPrimaryAccountNo || paymentMethod.DebitCardNo || '').substr(-4);
    } else if (paymentMethod.PaymentTypeGroup === Constants.CREDIT_CARD) {
      data.DisplayPrimaryAccountNo = data.DisplayCreditCardNo = (paymentMethod.DisplayPrimaryAccountNo || paymentMethod.CreditCardNo || '').substr(-4);
    } else if (paymentMethod.PaymentTypeGroup === Constants.STORED_VALUE_CARD) {
      data.DisplayPrimaryAccountNo = data.DisplaySvcNo = (paymentMethod.DisplayPrimaryAccountNo || paymentMethod.SvcNo || '').substr(-4);
    } else if (paymentMethod.PaymentTypeGroup === Constants.CUSTOMER_ACCOUNT) {
      data.DisplayPrimaryAccountNo = data.DisplayCustomerAccountNo = (paymentMethod.DisplayPrimaryAccountNo || paymentMethod.CustomerAccountNo || '').substr(-4);
    } else {
      data.DisplayPrimaryAccountNo = (paymentMethod.DisplayPrimaryAccountNo || paymentMethod.PaymentReference1 || '').substr(-4);
    }
    return data;
  }

  private async _getNls(key, params?): Promise<any> {
    return this.translate.get(key, params).toPromise();
  }

  ngOnDestroy() {
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
    if(this.eventsSubscription){
      this.eventsSubscription.unsubscribe();
    }
  }

  prepareSaveExistingPaymentMethodInput(paymentMethod: any): {} {
    return {
      PaymentKey: paymentMethod.PaymentKey,
      MaxChargeLimit: this.inputCharge[paymentMethod.PaymentKey]
    }
  }

  prepareSaveNewPaymentMethodInput(paymentMethod: any): any {
    return {
      PaymentReference1: paymentMethod.PaymentReference1,
      PaymentReference2: paymentMethod.PaymentReference2,
      PaymentReference3: paymentMethod.PaymentReference3,
      CustomerAccountNo: paymentMethod.CustomerAccountNo,
      PaymentType: paymentMethod.PaymentType,
      CheckNo: paymentMethod.CheckNo,
      CheckReference: paymentMethod.CheckReference,
      CreditCardNo: paymentMethod.CreditCardNo,
      CreditCardExpDate: paymentMethod.CreditCardExpDate,
      CreditCardType: paymentMethod.CreditCardType,
      DebitCardNo: paymentMethod.DebitCardNo,
      SvcNo: paymentMethod.SvcNo,
      ...this.getDisplayAccountNo(paymentMethod),
      MaxChargeLimit: this.inputCharge[paymentMethod.PaymentKey]
    }
  }
}

