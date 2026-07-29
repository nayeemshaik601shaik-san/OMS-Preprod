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

import { Component, Injector, OnDestroy, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ApplyHoldsActionParams, BreadcrumbService, Constants, getPathFromRoot, IsModificationAllowed, OrderCommonService, CancelOrderDataService } from '@call-center/order-shared';
import { ActivatedRoute } from '@angular/router';
import { ActionProcessorService, BaseDetailsComponent, BucPageDefinitionsConfiguration, BucPageDefinitionsHelperService, CCNotificationService, DisplayRulesHelperService, TabMessageService, GiftOptionsDataService } from '@buc/common-components';
import { ModalActionParams } from '../shared/order-details-paramtypes';
import { Subscription } from 'rxjs';
import { OrderSummaryComponent } from '../order-summary/order-summary.component';
import { BucConstants, BucSvcAngularStaticAppInfoFacadeUtil, CallCenterNavigationService } from '@buc/svc-angular';
import { ExtensionConstants } from '../../extension.constants';
@Component({
  selector: 'call-center-order-details',
  templateUrl: './order-details.component.html',
  styleUrls: ['./order-details.component.scss'],

})
export class OrderDetailsComponent extends BaseDetailsComponent implements OnInit, OnDestroy {
  EXTENSION = {
    TOP: ExtensionConstants.ORDER_DETAILS_OD_TOP,
    BOTTOM: ExtensionConstants.ORDER_DETAILS_OD_BOTTOM
  };

  @ViewChild(OrderSummaryComponent) private orderSummaryComponent: OrderSummaryComponent;

  protected readonly nlsMap: any = {
    'SHARED.GENERAL.LABEL_ORDER': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_APPLY_HOLDS': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_MANAGE_HOLDS': '',
    'ORDER_SUMMARY.ADJUST_PRICING_MODAL.LABEL_ADJUST_PRICING': '',
    'ORDER_SUMMARY.ADJUST_PRICING_MODAL.LABEL_VIEW_PRICING': '',
    'ORDER_SUMMARY.APPEASE_CUSTOMER_MODEL.LABEL_APPEASE_CUSTOMER': '',
    'RESTORE_ORDER.LABEL_RESTORE_ORDER': '',
    'ORDER_SUMMARY.CANCEL_ORDER_MODAL.LABEL_CANCEL_ORDER': '',
    'ORDER_SUMMARY.ALERT.LABEL_CREATE_ALERT': '',
    'CANCEL_ORDER_MODAL.TOOLTIP_MESSAGE': '',
    'ORDER_SUMMARY.ALERT.TOOLTIP_MESSAGE': '',
    'ORDER_SUMMARY.APPEASE_CUSTOMER_MODEL.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.PRICING_SUMMARY.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.MANAGE_HOLDS.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP': '',
    'APPLY_HOLDS_MODAL.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.GIFT_OPTIONS_MODAL.LABEL_GIFT_OPTIONS': '',
    'ORDER_SUMMARY.GIFT_OPTIONS_MODAL.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.CREATE_RETURN.LABEL_CREATE_RETURN': '',
    'ORDER_SUMMARY.CREATE_RETURN.LABEL_CREATE_RETURN_EXCHANGE': '',
    'ORDER_SUMMARY.CREATE_RETURN.LABEL_CREATE_RETURN_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.CREATE_RETURN.ERR_MESSAGE_CREATE_RETURN': '',
    'SHARED.GENERAL.LABEL_COPY_ORDER': '',
    'PAYMENT.GENERAL.MSG_PAYMENT_SUCCESS': '',
    'CHANGE_CUSTOMER_OPTIONS.ACTION_CUSTOMER_OPTIONS': '',
    'CHANGE_CUSTOMER_OPTIONS.TOOLTIP_MESSAGE': '',
    'ORDER_SUMMARY.SHARED.LABEL_CHANGE_SHIPPING_ADDRESS': ""
  };

  public isScreenInitialized = false;
  public initializeSummary = false;
  public breadCrumbList: any[];
  public currentRoute = 'order-details';
  public isNotesPanelExpanded: boolean = true; // expanded by default.
  orderActionOptions;
  isOrderArchived: boolean;
  archivedOrderNotificationObj: any;
  public componentId = 'OrderDetailsComponent';
  orderDetailsData: any;
  actionSub: Subscription;
  loadedOrderLines: any[];
  isAnyLineNotEligibleToCancel = false;
  orderRuleSetsValue: any = {};
  transactionQtyRuleDetailsValue: any;
  pageDefConfig: BucPageDefinitionsConfiguration;
  orderTabs;

  readonly resourceIdsForOrderDetailsActions = {
    APPLY_HOLDS: 'ICC000011',
    MANAGE_HOLDS: 'ICC000012',
    ADJUST_PRICING_ORDER: 'ICC000003',
    ADJUST_PRICING_ORDER_LINE: 'ICC000004',
    APPEASE_CUSTOMER: 'ICC000010',
    GIFT_OPTIONS: 'ICC000009',
    CANCEL_ORDER: 'ICC000006',
    CHANGE_FULFILLMENT_METHOD: 'ICC000002',
    MANAGE_INSTRUCTIONS: 'ICC000013',
    RESHIP: 'ICC000016',
    CHANGE_SHIPPING_ADDRESS: 'ICC000018',
    CHANGE_SHIPPING_METHOD: 'ICC000002',
    CHANGE_STORE: 'ICC000002',
    CHANGE_PICKUP_RECIPIENT: 'ICC000002',
    RESTORE_ORDER: 'ICC000019',
    CREATE_RETURN: 'ICC000041',
    CREATE_ORDER: 'ICC000001',
    APPEASE_CUSTOMER_ORDER_LINE: 'ICC000022',
    APPLY_HOLDS_ORDER_LINE: 'ICC000023',
    MANAGE_HOLDS_ORDER_LINE: 'ICC000024',
    CHANGE_GIFT_OPTIONS_ORDER_LINE: 'ICC000025',
    CANCEL_ORDER_LINE: 'ICC000026',
    ADD_NOTE_ON_ORDER_LINE: 'ICC000028',
    ADD_QUANTITY: 'ICC000030',
    ADD_ORDER_LINE: 'ICC000031',
    VIEW_SHIP_NODES: 'ICC000033',
    CHANGE_CUSTOMER_OPTIONS: 'ICC000034',
    LINE_DETAILS: 'ICC000035',
    CREATE_ALERT: 'ICC000071',
    ALERT_SEARCH: 'ICC000070',
    RETURN_SEARCH_SUMMARY: 'ICC000040',
  }
  enterpriseCode: any;

  constructor(
    private translateService: TranslateService,
    private activatedRoute: ActivatedRoute,
    private bcSvc: BreadcrumbService,
    private giftOptionsDataService: GiftOptionsDataService,
    public orderCommonService: OrderCommonService,
    public cancelOrderDataService: CancelOrderDataService,
    public actionProcessorService: ActionProcessorService,
    private displayRuleServices: DisplayRulesHelperService,
    private ccNavigationSvc: CallCenterNavigationService,
    public ccNotificationService: CCNotificationService,
    private orderviewContainerRef: ViewContainerRef,
    pageDefHelperService: BucPageDefinitionsHelperService,
    private tabRefocusService: TabMessageService,
    inj: Injector
  ) { 
    super(pageDefHelperService, inj)
    const sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    const uniqueId = activatedRoute.snapshot.queryParams.uniqueId;
    this.tabRefocusService.registerTabForRefocusNotification(sessionId,uniqueId, this.onRefresh.bind(this));
  }

  ngOnInit(): void {
    this.ccNotificationService.registerViewContainerRef(this.orderviewContainerRef);
    this.initialize();
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
    this.actionSub = this.actionProcessorService.selectUpdate<any>(this.componentId).subscribe(res => {
      const action = res.params.action;
      if (action === Constants.CREATE_RETURN_ACTION) {
        if (res.params.err) {
          BucSvcAngularStaticAppInfoFacadeUtil.publishEventToShell(BucConstants.SHELL_IFRAME_EVT_PUB_POST_STATUS, {
            data: {
                statusType: 'error',
                offeringId: 'omsi',
                statusContent: this.nlsMap['ORDER_SUMMARY.CREATE_RETURN.ERR_MESSAGE_CREATE_RETURN']
            }
          });
        }
      }
      if (res.params.refresh) {
        this.initializeSummary = true;
      }
    });
  }

  setEnterpriseForDisplayRule(){
    this.enterpriseCode = this.activatedRoute.snapshot.queryParams.enterprise; 
    this.displayRuleServices.setEnterprise(this.enterpriseCode);
  }

  ngOnDestroy() {
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
  }

  async initialize(): Promise<any> {
    await this._initTranslations();
    await this.prepareBreadcrumbList();
    this.isCreateOrder();
    this.setEnterpriseForDisplayRule();
    this.isScreenInitialized = true;
  }

  protected async _initTranslations(): Promise<any> {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translateService.get(keys).toPromise();
    keys.forEach(k => this.nlsMap[k] = json[k]);
  }

  async prepareBreadcrumbList(): Promise<any> {
    const c = `${this.nlsMap['SHARED.GENERAL.LABEL_ORDER']} ${this.activatedRoute.snapshot.queryParams.orderNo}`;
    const r = getPathFromRoot(this.activatedRoute.snapshot);
    this.bcSvc.updateLast(c, r, c, [r], { queryParams: this.activatedRoute.snapshot.queryParams });
    this.breadCrumbList = this.bcSvc.get();
  }

  isCreateOrder() {
    if (this.activatedRoute.snapshot.queryParams.onPaymentSuccess) {
      const successMsg = this.nlsMap['PAYMENT.GENERAL.MSG_PAYMENT_SUCCESS'];
      this.ccNotificationService.notify({
        type: 'success',
        title: successMsg
      });
    }
  }

  getOrderHistoryStatus() {
    this.isOrderArchived = this.orderDetailsData?.isHistory === 'Y';
    if (this.isOrderArchived) {
      const translateLiteral = this.hasRestoreResourcePermission() ? 'RESTORE_ORDER.RESTORE_ORDER_NOTIF_LABEL' : 'RESTORE_ORDER.RESTORE_ORDER_NOTIF_NOT_ALLOWED_LABEL';
      this.archivedOrderNotificationObj = {
        type: 'info',
        message: this.translateService.instant(translateLiteral),
        showClose: false,
        lowContrast: true,
        actions: this.hasRestoreResourcePermission() ? [{
          text: this.translateService.instant('RESTORE_ORDER.RESTORE_ORDER_NOTIF_ACTION'),
          resourceId: this.resourceIdsForOrderDetailsActions.RESTORE_ORDER,
          click: () => {
            this.orderCommonService.openRestoreConfirmModal(this.orderDetailsData);
          }
        }] : []
      }
    } else {
      this.archivedOrderNotificationObj = null;
    }
    return this.isOrderArchived;
  }

  handleNotesPanelToggle(event) {
    this.isNotesPanelExpanded = event;
  }

  hasRestoreResourcePermission(){
    return BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions.RESTORE_ORDER);
  }

  prepareOrderActions(): any {
    this.orderActionOptions = [
      this.applyHoldsAction(),
      this.manageHoldsAction(),
      this.adjustPricingAction(),
      this.appeaseCustomerAction(),
      this.giftOptionsAction(),
      this.cancelOrderAction(),
      this.createReturn(),
      this.createAlert(),
      this.changeShippingAddress(),
    ];
    if (this.orderDetailsData.Customer?.CustomerType === Constants.CUSTOMER_TYPE.business) { //Show only for business customers
      this.orderActionOptions.push(
        this.changeCustomerOptions()
      );
    }
    if (this.orderDetailsData.isHistory === 'Y') {
      this.orderActionOptions.push({
        selected: () => {
          this.orderCommonService.openRestoreConfirmModal(this.orderDetailsData, {});
        },
        resourceId: this.resourceIdsForOrderDetailsActions.RESTORE_ORDER,
        label: this.nlsMap['RESTORE_ORDER.LABEL_RESTORE_ORDER'],
        tid: this.componentId + '-header-restore-order-action'
      });
    }

    const copyOrderAction = {
      selected: () => {
        const inputParams = {
          orderHeaderKey: this.orderDetailsData.OrderHeaderKey,
          enterpriseCode: this.orderDetailsData.EnterpriseCode,
          orderNo: this.orderDetailsData.OrderNo,
          billToId: this.orderDetailsData.BillToID || ''
        }
        this.orderCommonService.copyOrder(inputParams).then(mashupOutput => {
          if (mashupOutput && mashupOutput.Order) {
            const title = this.translateService.instant('SHARED.GENERAL.LABEL_CREATE_ORDER', { orderNo: mashupOutput.Order.OrderNo});
            const orderNo = mashupOutput.Order.OrderNo;
            const orderHeaderKey = mashupOutput.Order.OrderHeaderKey;
            const sellerEnterpriseCode = mashupOutput.Order.EnterpriseCode;
            this.ccNavigationSvc.openUrlInNewTab(`${Constants.CREATE_ORDER_ROUTE}`,
              { orderNo, orderHeaderKey, title, sellerEnterpriseCode });
          }
        });
      },
      resourceId: this.resourceIdsForOrderDetailsActions.CREATE_ORDER,
      tid: this.componentId + 'copy-order-action',
      label: this.nlsMap['SHARED.GENERAL.LABEL_COPY_ORDER']
    };
    this.orderActionOptions.push(copyOrderAction);
  }

  orderDetails(event) {
    this.orderDetailsData = event.summaryDetails;
    this.orderRuleSetsValue = event.ruleSets;
    this.getOrderHistoryStatus();
    this.prepareOrderActions();
    this.prepareCustomization();
    if (this.isOrderArchived) {
      this.disableOrderActions();
    }    
  }

  private disableOrderActions() {
    this.orderActionOptions.filter(
      action => action.label !== this.translateService.instant('RESTORE_ORDER.LABEL_RESTORE_ORDER')
    ).forEach(action => {
      action.disabled = true;
      action.showTooltip = true;
    });
  }

  private applyHoldsAction() {
    const isHoldsAllowed =
      IsModificationAllowed(this.orderDetailsData.Modifications.Modification, Constants.MOD_TYPE_HOLD);
    return {
      selected: () => {
        this.actionProcessorService.dispatch<ApplyHoldsActionParams>(Constants.APPLY_HOLDS, {
          component: this.componentId,
          data: {
            modalText: '',
            modalData: {
              showRadioButton: false,
              holdType: 'ORDER',
              orderDetails: this.orderDetailsData,
              loadedOrderLines: [this.orderDetailsData],
              size: 'lg'
            }
          }
        });
      },
      resourceId: this.resourceIdsForOrderDetailsActions.APPLY_HOLDS,
      tid: this.componentId + '-header-apply-holds-action',
      label: this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_APPLY_HOLDS'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center',
      disabled: !isHoldsAllowed,
      showTooltip: !isHoldsAllowed,
      tooltipMsg: this.nlsMap['APPLY_HOLDS_MODAL.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP']
    };
  }

  private manageHoldsAction() {
    const disabled = this.orderDetailsData.HoldFlag !== 'Y';
    return {
      selected: () => {
        this.actionProcessorService.dispatch<ModalActionParams>(Constants.MANAGE_HOLDS, {
          component: this.componentId,
          data: {
            orderDetails: this.orderDetailsData,
          }
        });
      },
      resourceId: this.resourceIdsForOrderDetailsActions.MANAGE_HOLDS,
      tid: this.componentId + '-header-manage-holds-action',
      label: this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_MANAGE_HOLDS'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center',
      disabled,
      showTooltip: disabled,
      tooltipMsg: this.nlsMap['ORDER_SUMMARY.MANAGE_HOLDS.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP']
    };
  }

  getOrderMaxStatus() {
    let maxOrderStatus = this.orderDetailsData?.MaxOrderStatus;
    if (maxOrderStatus) {
      const idx = maxOrderStatus.indexOf('.');
      if (idx !== -1) {
        maxOrderStatus = maxOrderStatus.substring(0, idx);
      }
    }
    return Number(maxOrderStatus);
  }

  isOrderCancelled(){
    return (this.orderDetailsData?.MaxOrderStatus?.indexOf('9000') > -1);
  }
  private adjustPricingAction() {
    return {
      selected: () => {
        this.actionProcessorService.dispatch<ModalActionParams>(Constants.ADJUST_PRICING, {
          component: this.componentId,
          data: {
            modalText: '',
            modalData: {
              summaryDetails: this.orderDetailsData,
              size: 'md'
            }
          }
        });
      },
      tid: this.componentId + '-header-adjust-pricing-action',
      label: BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions.ADJUST_PRICING_ORDER) ?
        this.nlsMap['ORDER_SUMMARY.ADJUST_PRICING_MODAL.LABEL_ADJUST_PRICING'] : this.nlsMap['ORDER_SUMMARY.ADJUST_PRICING_MODAL.LABEL_VIEW_PRICING'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center',
      disabled: this.isOrderCancelled(),
      showTooltip: this.isOrderCancelled(),
      tooltipMsg: this.nlsMap['ORDER_SUMMARY.PRICING_SUMMARY.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP']
    };
  }

  private changeShippingAddress() {
    const isChangeAddressAllowed =
      IsModificationAllowed(this.orderDetailsData.Modifications.Modification, Constants.MOD_TYPE_SHIPTO);
    return {
      selected: () => {
        this.actionProcessorService.dispatch<ModalActionParams>(Constants.CHANGE_ORDER_DETAILS, {
          component: this.componentId,
          data: {
            orderHeaderKey: this.orderDetailsData.OrderHeaderKey,
            orderNo: this.orderDetailsData.OrderNo,
            sellerEnterpriseCode: this.orderDetailsData.EnterpriseCode,
            openShippingAddressEditModal: true,
          }
        });
      },
      tid: this.componentId + '-header-change-shipping-address-action',
      resourceId: this.resourceIdsForOrderDetailsActions.CHANGE_SHIPPING_ADDRESS,
      label: this.nlsMap['ORDER_SUMMARY.SHARED.LABEL_CHANGE_SHIPPING_ADDRESS'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center',
      disabled: !isChangeAddressAllowed || this.getOrderMaxStatus() >= 1500 || this.isOrderCancelled(),
      showTooltip: !isChangeAddressAllowed || this.getOrderMaxStatus() >= 1500 || this.isOrderCancelled(),
      tooltipMsg: this.nlsMap['ORDER_SUMMARY.SHARED.LABEL_ADDRESS_ACTION_UNAVAILABLE_TOOLTIP']
    };
  }

  private appeaseCustomerAction() {
    //EOMS-5621, 9303 Changes Start
    const isOrderTypeMP = this.orderDetailsData?.OrderType == 'MP' || this.orderDetailsData?.EnteredBy == 'GLOBALE' || (this.orderDetailsData?.Status == 'Return Received') ; 
    //EOMS-5621, 9303 Changes End
    return {
      selected: () => {
        this.actionProcessorService.dispatch<ModalActionParams>(Constants.APPEASE_CUSTOMER, {
          component: this.componentId,
          data: {
            modalText: '',
            modalData: {
              orderDetails: this.orderDetailsData,
              orderLines: this.loadedOrderLines,
              size: 'md'
            }
          }
        });
      },
      resourceId: this.resourceIdsForOrderDetailsActions.APPEASE_CUSTOMER,
      tid: this.componentId + '-header-appease-customer-action',
      label: this.nlsMap['ORDER_SUMMARY.APPEASE_CUSTOMER_MODEL.LABEL_APPEASE_CUSTOMER'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center',
      //EOMS-5621 start
      disabled: isOrderTypeMP || this.isOrderCancelled(),
      //EOMS-5621 end
      showTooltip: this.isOrderCancelled(),
      tooltipMsg: this.nlsMap['ORDER_SUMMARY.APPEASE_CUSTOMER_MODEL.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP']
    };
  }

  private giftOptionsAction() {
    return {
      selected: () => {
        this.actionProcessorService.dispatch<ModalActionParams>(Constants.GIFT_OPTIONS, {
          component: this.componentId,
          data: {
            modalText: '',
            modalData: {
              orderDetails: this.orderDetailsData,
              orderRuleSetsValue: this.orderRuleSetsValue,
              size: 'lg'
            }
          }
        });
      },
      resourceId: this.resourceIdsForOrderDetailsActions.GIFT_OPTIONS,
      tid: this.componentId + '-header-gift-options-action',
      label: this.nlsMap['ORDER_SUMMARY.GIFT_OPTIONS_MODAL.LABEL_GIFT_OPTIONS'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center',
      tooltipMsg: this.nlsMap['ORDER_SUMMARY.GIFT_OPTIONS_MODAL.LABEL_ORDER_ACTION_UNAVAILABLE_TOOLTIP']
    };
  }

  getTransactionQtyRuleDetailsVal(evt) {
    this.transactionQtyRuleDetailsValue = evt;
  }
  private checkIfPaginatedLines() {
    return this.orderDetailsData.OrderLines.TotalNumberOfRecords > this.loadedOrderLines?.length;
  }

  private isAnyOrderLineEligibleForCancellation(orderLinesList) {
    let isLineEligibleToCancel = false;

    if (orderLinesList && orderLinesList.length) {
      for (const orderLine of orderLinesList) {
        isLineEligibleToCancel = this.cancelOrderDataService.isOrderLineEligibleForCancellation(
          orderLine, this.transactionQtyRuleDetailsValue);
        if (isLineEligibleToCancel) {
          break;
        }
      }
      for (const orderLine of orderLinesList) {
        const isLineCancellable = this.cancelOrderDataService.isOrderLineEligibleForCancellation(
          orderLine, this.transactionQtyRuleDetailsValue);
        if (!isLineCancellable) {
          this.isAnyLineNotEligibleToCancel = true;
          break;
        }
      }
    }
    return isLineEligibleToCancel;
  }

  private cancelOrderAction() {
    // Enable the cancel order header action if either cancel modification allowed on order or pagination is active
    const orderCancelModificationAllowed = IsModificationAllowed(this.orderDetailsData.Modifications.Modification, Constants.MOD_TYPE_CANCEL);
    const requiredOrderDetails =
      (({ OrderHeaderKey, EnterpriseCode, Modifications, OrderNo }) => ({ OrderHeaderKey, EnterpriseCode, Modifications, OrderNo }))(this.orderDetailsData);
    return {
      selected: () => {
        this.actionProcessorService.dispatch<ModalActionParams>(Constants.CANCEL_ORDER, {
          component: this.componentId,
          data: {
            modalText: '',
            modalData: {
              orderDetails: requiredOrderDetails,
              isAnyLineNotEligibleToCancel: this.isAnyLineNotEligibleToCancel,
              size: 'lg',
              useTransactionalQtyRuleValue: this.transactionQtyRuleDetailsValue
            }
          }
        });
      },
      resourceId: this.resourceIdsForOrderDetailsActions.CANCEL_ORDER,
      tid: this.componentId + '-header-cancel-order-action',
      label: this.nlsMap['ORDER_SUMMARY.CANCEL_ORDER_MODAL.LABEL_CANCEL_ORDER'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center',
      disabled: !orderCancelModificationAllowed || !this.checkIfPaginatedLines,
      showTooltip: !orderCancelModificationAllowed || !this.checkIfPaginatedLines,
      tooltipMsg: this.nlsMap['CANCEL_ORDER_MODAL.TOOLTIP_MESSAGE']
    };
  }

  private createAlert() {
    return {
      selected: () => { // redirect to a create alert page
        this.ccNavigationSvc.openUrlInNewTab(`${Constants.CREATE_ALERT_ROUTE}`,
        { order: this.activatedRoute.snapshot.queryParams.orderNo, selectedEnterpriseValue: this.enterpriseCode });
      },
      resourceId: this.resourceIdsForOrderDetailsActions.CREATE_ALERT,
      tid: this.componentId + '-header-create-order-action',
      label: this.nlsMap['ORDER_SUMMARY.ALERT.LABEL_CREATE_ALERT'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center',
      tooltipMsg: this.nlsMap['ORDER_SUMMARY.ALERT.TOOLTIP_MESSAGE']
    }
  }
  private changeCustomerOptions() {
    return {
      selected: () => {
        this.actionProcessorService.dispatch<ModalActionParams>(Constants.CHANGE_CUSTOMER_OPTIONS, {
          component: this.componentId,
          data: {
            modalText: '',
            modalData: {
              orderDetails: this.orderDetailsData,
              size: 'md'
            }
          }
        });
      },
      resourceId: this.resourceIdsForOrderDetailsActions.CHANGE_CUSTOMER_OPTIONS,
      tid: this.componentId + '-header-change-customer-options-action',
      label: this.nlsMap['CHANGE_CUSTOMER_OPTIONS.ACTION_CUSTOMER_OPTIONS'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center'
    }
  }

  private createReturn() {
    const isCreateReturnAllowed = this.orderDetailsData.DraftOrderFlag === Constants.CHECK_NO && this.orderDetailsData.isHistory === Constants.CHECK_NO && !this.isOrderCancelled() && !isGlobaleOrders && !isMarketPlaceOrder;
    const isReturnAndExchangeAllowed = this.displayRuleServices.getRuleValueForOrg(this.orderDetailsData.EnterpriseCode, 'ICC_ALLOW_EXCHANGE_ORDER') === Constants.CHECK_YES;
	
	//EOMS-5621 start: Disable Return Order Creation for GLOBALE & MP Order's
    const isGlobaleOrders = this.orderDetailsData?.EnteredBy == 'GLOBALE' ;
    const isMarketPlaceOrder = this.orderDetailsData?.OrderType == 'MP'; 

    var tooltipMessage =  this.nlsMap['ORDER_SUMMARY.CREATE_RETURN.LABEL_CREATE_RETURN_UNAVAILABLE_TOOLTIP']
    if (isGlobaleOrders || isMarketPlaceOrder) {
      tooltipMessage = 'Return creation from call center is not allowed for this order';
    }
    //EOMS-5621 end

    return {
      selected: this.createReturnOrder.bind(this),
      resourceId: this.resourceIdsForOrderDetailsActions.CREATE_RETURN,
      tid: this.componentId + '-header-create-return-action',
      label:  isReturnAndExchangeAllowed ? this.nlsMap['ORDER_SUMMARY.CREATE_RETURN.LABEL_CREATE_RETURN_EXCHANGE'] : this.nlsMap['ORDER_SUMMARY.CREATE_RETURN.LABEL_CREATE_RETURN'],
      tooltipPlacement: 'left',
      tooltipAlignment: 'center',
      disabled: !isCreateReturnAllowed,
      showTooltip: !isCreateReturnAllowed,
      tooltipMsg: tooltipMessage
      
    };
  }

  private createReturnOrder() {
    this.actionProcessorService.dispatch<any>(Constants.CREATE_RETURN_ACTION, {
      componentId: this.componentId,
      Order: this.orderDetailsData
    });
  }

  onLoadedOrderLines(event) {
    this.loadedOrderLines = event;
    const cancelLabel = this.translateService.instant('ORDER_SUMMARY.CANCEL_ORDER_MODAL.LABEL_CANCEL_ORDER');
    const isAnyOrderLineCancellable = this.isAnyOrderLineEligibleForCancellation(this.loadedOrderLines);
    if (this.orderActionOptions && this.orderActionOptions.length) {
      const cancelOptionFiltered = this.orderActionOptions.filter(action => action.label === cancelLabel);
      if (cancelOptionFiltered && cancelOptionFiltered.length) {
        const cancelOption = cancelOptionFiltered[0];
        cancelOption.disabled = cancelOption.showTooltip = !isAnyOrderLineCancellable;
      }
      this.orderActionOptions.forEach((item) => {
        if (item.label === this.nlsMap['ORDER_SUMMARY.GIFT_OPTIONS_MODAL.LABEL_GIFT_OPTIONS']) {
          const giftOptionsLines = this.loadedOrderLines?.filter((line) => {
            const isLineModificationAllowed = IsModificationAllowed(line.Modifications.Modification, Constants.MOD_TYPE_GIFT_OPTION);
            if (line.DeliveryMethod === 'SHP') {
              return isLineModificationAllowed && this.orderRuleSetsValue.giftShipRule;
            } else {
              return isLineModificationAllowed && this.orderRuleSetsValue.giftPickRule;
            }
          });
          item.disabled = !giftOptionsLines.length;
          item.showTooltip = !giftOptionsLines.length;
        }
      });
    }
    // reconfirm if the order is archived when orderlines are loaded
    if (this.isOrderArchived) {
      this.disableOrderActions();
    }
  }

  onToggle() {
    this.orderSummaryComponent.toggleExpand();
  }

  onRefresh() {
    this.orderSummaryComponent.initialize();
  }

  onPageActionClicked(id, event) {
    this.actionProcessorService.dispatch<any>(id, {
      component: 'order-details',
      data: this.pageData
    });
  }

  async prepareCustomization() {
    this.pageData.routeData = this.activatedRoute;
    this.pageData.pageObject = this.orderDetailsData;
    await this.initializePageDef('order-details');
    this.pageDefConfig = this.getPageDefConfiguration();
    if (this.pageDefConfig?.actions?.length > 0) {
      this.orderActionOptions = this.orderActionOptions.concat(this.pageDefConfig.actions);
    }
    if (this.pageDefConfig?.tabs?.length > 0) {
      this.orderTabs = this.pageDefConfig.tabs;
    }
  }
}
