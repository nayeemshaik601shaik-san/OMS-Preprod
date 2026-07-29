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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { BucBaseUtil, CallCenterNavigationService } from '@buc/svc-angular';
import { CommonCode, Constants, EntityStoreService, Order, selectCommonCode, ExchangeOrder, Return } from '@call-center/return-shared';
import { Observable, Subscription } from 'rxjs';
import { filter, map, take } from 'rxjs/operators';
import * as createReturnActions from '../../../../../../state/create-return.action';
import { ProductsToReturnState } from '../../../../../../state/create-return.model';
import { BucNotificationModel, BucNotificationService, CommonBinaryOptionModalComponent, DisplayRulesHelperService } from '@buc/common-components';
import { TranslateService } from '@ngx-translate/core';
import { ModalService } from 'carbon-components-angular';

@Component({
  selector: 'call-center-return-options',
  templateUrl: 'return-options.component.html',
  styleUrls: ['./return-options.component.scss'],
})
export class ReturnOptionsComponent implements OnInit, OnDestroy {
  @Input() originalSalesOrderHeaderKey: string;
  @Input() returnOrder: Return;
  @Input() exchangeOrder: ExchangeOrder;
  @ViewChild('radioLabelTpl', { static: true }) radioLabelTpl: TemplateRef<any>;

  componentId = 'ProductsToReturnComponent';
  selectReturnOptions = [];

  defaultReturnOption = 'return'; //EOMS-4366 defaulting return

  useDifferentReasonCode: boolean = false;
  returnReasonCodes: Array<any> = [];
  returnReasonsInvalid$: Observable<boolean> = this.entityStoreSvc
    .getStore()
    .select(createReturnActions.getProductsToReturnState)
    .pipe(
      filter((p) => !BucBaseUtil.isVoid(p)),
      map(this.isReturnReasonInvalid.bind(this))
    );

  originalSalesOrder = {
    orderNo: '-',
    orderDate: '-',
    orderDateFormat: Constants.LONG_DATETIME_FORMAT,
    grandTotal: '0',
    currencyCode: '',
    enterpriseCode: '',
    customerName: ''
  };
  private subscriptions: Array<Subscription> = [];
  exchangeTypes = [];
 

  constructor(
    private entityStoreSvc: EntityStoreService,
    private ccNavService: CallCenterNavigationService,
    private bucNotificationService: BucNotificationService,
    private modalService: ModalService,
    private displayRulesHelperService: DisplayRulesHelperService,
    private translate: TranslateService,
    private cdr: ChangeDetectorRef //added for return
  ) {}

  ngOnInit(): void {
    //EOMS-4366 Force default to "Return" by setting store value start
    this.entityStoreSvc.dispatchAction(
      createReturnActions.toggleReturnWithExchangeFlow({ returnAndExchange: false })
    );
    this.initialize();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }

  onSelectRadioChange(event: any) {
    if (event.srcElement.value === 'exchange') {
      this.showReturnAndExchange(true);
    } else if(event.srcElement.value === 'return'){
      this.openConfirmationModal();
    }
  }

  onSelectReturnReason(event: any) {
    this.entityStoreSvc.dispatchAction(
      createReturnActions.setCommonReturnReasonCode({
        reasonCode: { id: event.item.id, name: event.item.value },
      })
    );
  }

  onApplyDifferentReasonsToggle(toggled: boolean) {
    this.useDifferentReasonCode = toggled;
    this.returnReasonCodes = this.returnReasonCodes.map(rc => ({...rc, selected: false}));
    this.entityStoreSvc.dispatchAction(
      createReturnActions.setCommonReturnReasonCode(
        toggled ? { reasonCode: { id: undefined, name: undefined } } : undefined
      )
    );
  }

  isReturnReasonInvalid(productsToReturnState: ProductsToReturnState) {
    const { commonReturnReasonCode, currentSalesOrderLines, productsToReturn } =
      productsToReturnState;
    const isReturnReasonInvalid = Array.isArray(currentSalesOrderLines) &&
      currentSalesOrderLines.length > 0 &&
      currentSalesOrderLines.some(
        (ol) =>
          productsToReturn[ol] &&
          parseInt(productsToReturn[ol].returnQuantity) > 0
          && productsToReturn[ol].returnReason === undefined
      );
    return commonReturnReasonCode === undefined && isReturnReasonInvalid;
  }

  openOrderDetails() {
    this.ccNavService.openUrlInNewTab(Constants.ORDER_DETAILS_ROUTE, {
      orderNo: this.originalSalesOrder.orderNo,
      orderHeaderKey: this.originalSalesOrderHeaderKey,
      title: this.originalSalesOrder.orderNo,
      enterprise: this.originalSalesOrder.enterpriseCode,
    });
  }

  private async initialize() {
    this.subscriptions.push(
      // get RETURN_REASON common code.
      this.entityStoreSvc.getStore()
        .select(selectCommonCode(Constants.COMMON_CODE_RETURN_REASON))
        .pipe(
          filter((cc) => !BucBaseUtil.isVoid(cc)),
          take(1),
        ).subscribe(this.initializeReturnReasons.bind(this)),

      // subscribe to origin sales order entity
      this.entityStoreSvc
        .getEntityById(this.originalSalesOrderHeaderKey, 'order')
        .pipe(
          filter(o => !BucBaseUtil.isVoid(o)),
          take(1)
        ).subscribe(this.setOriginalSalesOrder.bind(this)),

      // get Return option
      this.entityStoreSvc.getStore()
      .select(createReturnActions.isReturnAndExchange)
      .pipe(
        filter((cc) => !BucBaseUtil.isVoid(cc)),
        take(1),
      ).subscribe(this.initializeReturnOptions.bind(this)),
      
    );
  }

  //EOMS-4366 start
  private initializeReturnOptions(isReturnAndExchange) {
    const defaultToReturn = !isReturnAndExchange;
    this.defaultReturnOption = defaultToReturn ? 'return' : 'exchange';

    this.selectReturnOptions = [
      {
        id: 'return',
        value: 'return',
        template: this.radioLabelTpl,
        checked: defaultToReturn,
        key: 'CREATE_RETURN.PRODUCTS_TO_RETURN.LABEL_RETURN_ONLY',
      },
      {
        id: 'exchange',
        value: 'exchange',
        template: this.radioLabelTpl,
        checked: !defaultToReturn,
        key: 'CREATE_RETURN.PRODUCTS_TO_RETURN.LABEL_RETURN_AND_EXCHANGE',
        disabled: this.displayRulesHelperService.getRuleValueForOrg(
          this.returnOrder.enterpriseCode,
          Constants.RULE_ALLOW_EXCHANGE_ORDER
        ) === Constants.CHECK_NO,
      },
    ];

    this.cdr.detectChanges();
  }
  //EOMS-4366 start

  private initializeReturnReasons(reasonCodes: CommonCode[]): void {
    if (!BucBaseUtil.isVoid(reasonCodes)) {
      this.returnReasonCodes = reasonCodes.map((rc) => ({
        id: rc.value,
        value: rc.description,
        selected: false,
        content: rc.description,
      }));
    } else {
      this.returnReasonCodes = [];
    }
    if(this.returnReasonCodes.length === 0) {
      this.bucNotificationService.send([
        new BucNotificationModel({
          statusType: 'error',
          statusContent: this.translate.instant('CREATE_RETURN.PRODUCTS_TO_RETURN.ERROR_RETURN_REASONS_NOT_CONFIGURED'),
        })
      ]);
    }
  }

  private setOriginalSalesOrder(order: Order) {
    this.originalSalesOrder.orderNo = order.orderNo;
    this.originalSalesOrder.orderDate = order.orderDate;
    this.originalSalesOrder.grandTotal = order.grandTotal.value;
    this.originalSalesOrder.currencyCode = order.grandTotal.currencyCode;
    this.originalSalesOrder.enterpriseCode = order.enterpriseCode;
    this.originalSalesOrder.customerName = order.customerFirstName + ' ' + order.customerLastName
  }

  openConfirmationModal() {
    const optionOne = {
      primary: '',
      callOnClose: true,
      callback: this._callbackOnCancel.bind(this),
      text: this.translate.instant('SHARED.GENERAL.LABEL_CANCEL'),
      tid: 'change-to-return-only-cancel-button'
    };
    const optionTwo = {
      class: {
        primary: true
      },
      callback: this._callbackOnChangeToReturn.bind(this),
      callOnClose: true,
      text: this.translate.instant('SHARED.GENERAL.LABEL_CONFIRM'),
      tid: 'change-to-return-only-yes-button',
    };
    this.modalService.destroy();
    this.modalService.create({
      component: CommonBinaryOptionModalComponent,
      inputs: {
        modalText: {
          header: this.translate.instant('CREATE_RETURN.PRODUCTS_TO_RETURN.CHANGE_RETURN_OPTION'),
          label:this.translate.instant('CREATE_RETURN.PRODUCTS_TO_RETURN.LABEL_CONFIRM_RETURN_ONLY'),
          size: 'sm'
        },
        optionOne,
        optionTwo,
      }
    });
  }

    //EOMS-4366 start
  _callbackOnChangeToReturn(): void {
    this.entityStoreSvc.dispatchAction(createReturnActions.deleteExchangeDetails());
    this.showReturnAndExchange(false);
  }
  //EOMS-4366 end

  _callbackOnCancel() {
    // on cancel change the radio option back to exchange
    this.selectReturnOptions = this.selectReturnOptions.map(i => ({...i, checked: i.id === 'exchange'}))
  }

  showReturnAndExchange(showReturnAndExchange) {
    this.entityStoreSvc.dispatchAction(
      createReturnActions.toggleReturnWithExchangeFlow({ returnAndExchange: showReturnAndExchange })
    );
  }
}
