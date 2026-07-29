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

import { Component, OnInit, ViewChild, TemplateRef, OnDestroy, Output, EventEmitter, Input, ViewContainerRef } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BucDateTimeHelper, fmtDate, getMoment, GiftOptionsDataService,
  ActionProcessorService, CCNotificationService, CommonBinaryOptionModalComponent, BucCommonCurrencyFormatPipe, getArray,
  BucSessionService,
  localeBuc2Angular} from '@buc/common-components';
import { ChangeShippingOptionModalComponent, ChangeShippingOptionService, OrderStoreService } from '@buc/cc-components';
import { BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { ModalService } from 'carbon-components-angular';
import { FulfillmentMethodsService } from '../../data-service/fulfillment-methods.service';
import { CreateOrderService } from '../create-order.service';
import { OrderCommonService, Constants, ActionParams, ModifyCustomerAddressParams,
   ViewAllNotesModalComponent, IsModificationAllowed} from '@call-center/order-shared';
import { FulfillmentMethodItemsComponent } from './fulfillment-method-items/fulfillment-method-items.component';
import { Observable, Subscription } from 'rxjs';
import { get } from 'lodash';
import { ExtensionConstants } from '../../../extension.constants';
import { skip } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';


@Component({
  selector: 'call-center-fulfillment-methods',
  templateUrl: './fulfillment-methods.component.html',
  styleUrls: ['./fulfillment-methods.component.scss'],
  providers: [FulfillmentMethodsService, ChangeShippingOptionService]
})
export class FulfillmentMethodsComponent implements OnInit, OnDestroy {
  EXTENSION = {
    TOP: ExtensionConstants.FULFILLMENT_METHODS_CO_TOP,
    BOTTOM: ExtensionConstants.FULFILLMENT_METHODS_CO_BOTTOM
  };
  componentId = 'FulfillmentMethodsComponent';

  @Input() hideLoader = false;
  OrderHeaderKey: any;
  isLargeOrder: any = 'N';
  isLoader = false;
  actionSub: Subscription;
  orderRuleSetsValue: any = {};
  selectedItemsFromTable: any;
  inputOrderLines: any;

  //Fulfillment Details
  orderDetails: any;
  fulfillmentGroups: any = [];
  unAvailableGroups: any = [];
  isCartEmpty = false;
  fieldItems: any;
  selectedFmtGroup: any;
  view: string = 'FulfillmentView';
  // Level of service modal
  levelOfServiceModalData: any;
  levelOfServiceModal: any;

  //Pickup Date
  i18nDatePlaceholder;
  curLocale;
  flatpickrDateFormat;
  appointmentDate: string;

  //Shipping option
  userLocale: string;
  defaultRuleOptimizationObj = {};
  selectedRuleDescription: string;
  bundleParentKeyMapper = {};
  notesTypeList: any;
  readonly resourceIdsForOrderActions = {
    GIFT_OPTIONS: 'ICC000025',
    CHANGE_FULFILLMENT_METHOD: 'ICC000002',
    MANAGE_INSTRUCTIONS: 'ICC000013',
    CHANGE_SHIPPING_ADDRESS: 'ICC000018',
    CHANGE_SHIPPING_METHOD: 'ICC000002',
    CHANGE_STORE: 'ICC000002',
    CHANGE_PICKUP_RECIPIENT: 'ICC000002',
    ADJUST_PRICING_ORDER_LINE: 'ICC000004',
    OVERRIDE_PRICE: 'ICC000021'
  }


  @Input() orderLinesForChangeFulfillment: any = [];
  @Output() updateOrderSummary: EventEmitter<any> = new EventEmitter();
  @Output() sendUnavailableGroups: EventEmitter<any> = new EventEmitter();

  @Input() events: Observable<void>;
  private eventsSubscription: Subscription;

  @ViewChild('levelOfServiceModalTemplate', { static: false }) levelOfServiceModalTemplate: TemplateRef<any>;
  @ViewChild(FulfillmentMethodItemsComponent) private fulfillmentItemsTable: FulfillmentMethodItemsComponent;

  editShippingOption = true;
  // Translation
  protected readonly nslMap = {
    'FULFILLMENT.SHIPMENT_DETAILS.LABEL_PRODUCTS' : '',
    'FULFILLMENT.SHIPMENT_DETAILS.LABEL_PRODUCT' : '',
    'FULFILLMENT_METHODS.LABEL_SHP' : '',
    'FULFILLMENT_METHODS.LABEL_PICK' : '',
    'FULFILLMENT_METHODS.LABEL_CARRY' : '',
    'FULFILLMENT.PICKUP_APPOINTMENT.MSG_PICKUP_APPOINTMENT_UPDATED': '',
    'FULFILLMENT_METHODS.MSG_STORE_ADDRESS_UPDATED': '',
    'FULFILLMENT_METHODS.MSG_PICKUP_RECIPIENT_UPDATED': '',
    'FULFILLMENT_METHODS.MSG_ORDER_LINE_REMOVED': '',
    'FULFILLMENT_METHODS.MSG_ORDER_LINES_REMOVED': '',
    'FULFILLMENT_METHODS.MSG_LEVEL_OF_SERVICE_SUCCESS': '',
    'FULFILLMENT.SHIPMENT_DETAILS.LABEL_CHANGE_LEVEL_OF_SERVICE':'',
    'FULFILLMENT_METHODS.MSG_SHIPPING_ADDRESS_SUCCESS': '',
    'FULFILLMENT.FULFILLMENT_GROUPS.LABEL_MODIFY_SHIPPING_ADDRESS':'',
    'FULFILLMENT_METHODS.FULFILLMENT_PICK_SUCCESS': '',
    'FULFILLMENT_METHODS.MSG_ORDER_LINES_FROM_DEFFERENT_FULFILLMENT_GROUP': '',
    'FULFILLMENT_METHODS.ORDER_LINES_NOT_AVAILABLE_FOR_SHIPPING': '',
    'SELECT_STORE.GENERAL.LABEL_SELECT_STORE': '',
    'FULFILLMENT_METHODS.MSG_INVALID_DATE': '',
    'FULFILLMENT_METHODS.LABEL_UNAVAILABLE_SHIPPING_LINES': '',
    'FULFILLMENT_METHODS.LABEL_UNAVAILABLE_PICKUP_LINES': '',
    'SHARED.GENERAL.LABEL_CANCEL':'',
    'SHARED.GENERAL.LABEL_SAVE':''
  };
  isAdjustPricingResourceAllowed: boolean;
  subscriptions: Subscription[] = [];
  protected bucSessionStorageService: BucSessionService;
  lcl = localeBuc2Angular(BucSvcAngularStaticAppInfoFacadeUtil.getUserLanguage());
  public currPipe: BucCommonCurrencyFormatPipe = new BucCommonCurrencyFormatPipe(this.lcl)
  isCreateOrder: boolean;

  constructor(
    public translate: TranslateService,
    private orderCommonService: OrderCommonService,
    private fulfillmentService: FulfillmentMethodsService,
    private actionProcessorService: ActionProcessorService,
    private createOrderService: CreateOrderService,
    private orderStoreService: OrderStoreService,
    private ccNotificationService: CCNotificationService,
    private giftOptionDataService: GiftOptionsDataService,
    private shippingOptionService: ChangeShippingOptionService,
    private modalService: ModalService,
    public activatedRoute: ActivatedRoute,) {

  }

   ngOnInit(): void {

    this.initialize();
    this.getSubscribeData();
    // this.eventsSubscription = this.events.subscribe(() => this.getFulfillmentData());
  }

  selected(event): void{
   console.log(event);
   if(event.name =='FulfillmentView'){
      this.view = 'FulfillmentView';
   }else if(event.name =='OrderLinesView'){
      this.view = 'OrderLinesView';
   }
  }


  async initialize() {
    this.isLoader = true;
    await this._initTranslations();
    this.initializeSession();
    this.OrderHeaderKey = this.orderStoreService.getOrderDetails()?.Order?.OrderHeaderKey;
    this.userLocale = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale();
    this.isAdjustPricingResourceAllowed =  BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderActions.ADJUST_PRICING_ORDER_LINE);
    await this.refreshFulfillmentData();
    this.getOrderRuleSetsValue();
    this.initializeNotes();
    this.getRuleDetaisForShippingOption();
    this.subscriptions.push(
      this.createOrderService.getMessage().subscribe(message => {
        if (message && message.refreshParentComponent === true) {
          this.refreshFulfillmentData();
        }
      }),
      // skip previous refresh notification and refresh changes only on the current page
      this.orderCommonService.refreshParent.pipe(skip(1)).subscribe(data => {
        if (data) {
          this.refreshFulfillmentData();
        }
      })
    )
  }

  initializeSession() {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    const uniqId = queryParams.uniqueId;
    const sessionPrefix = 'create-order-' + uniqId;
    const sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    this.bucSessionStorageService = new BucSessionService(sessionPrefix, sessionId);
    const isAddLinesWizard = queryParams?.addLines ? JSON.parse(queryParams?.addLines) : false;
    const isChangeFulfillmentOptionsWizard = queryParams?.changeFulfillment ? JSON.parse(queryParams?.changeFulfillment) : false;
    this.isCreateOrder = !isAddLinesWizard && !isChangeFulfillmentOptionsWizard;

}


  async refreshFulfillmentData() {
    await this.getFulfillmentData();
  }

  getSubscribeData() {
    let successMsg : any = '';
    this.actionSub = this.actionProcessorService.selectUpdate<any>(this.componentId).subscribe(res => {
      if (res.params.selectedStore) {
        this.isLoader = true;
        successMsg = this.nslMap['FULFILLMENT_METHODS.MSG_STORE_ADDRESS_UPDATED'];
        this.savePickupDetails(res.params, successMsg);
      }
      else if (res.params.pickupByDetail) {
        this.isLoader = true;
        successMsg = this.nslMap['FULFILLMENT_METHODS.MSG_PICKUP_RECIPIENT_UPDATED'];
        this.savePickupDetails(res.params.pickupByDetail, successMsg);
      }
      else if (res.params.selectedAppointmentDate) {
        this.isLoader = true;
        successMsg = this.nslMap['FULFILLMENT.PICKUP_APPOINTMENT.MSG_PICKUP_APPOINTMENT_UPDATED'];
        this.savePickupDetails(res.params, successMsg);
      }
      if ([Constants.MANAGE_INSTRUCTIONS, Constants.GIFT_OPTIONS, Constants.ADJUST_PRICING,Constants.OVERRIDE_PRICE,Constants.ACTION_ADD_NOTE,Constants.CHANGE_PRODUCT_VARIATION].indexOf(res.params.action) > -1) {
        this.createOrderService.sendMessage(true);
      }
    });
  }

  private async _initTranslations(): Promise<any> {
    const keys = Object.keys(this.nslMap);
    const json = await this.translate.get(keys).toPromise();
    keys.forEach(k => this.nslMap[k] = json[k]);
  }

  private async _getNls(key, params?): Promise<any> {
    return this.translate.get(key, params).toPromise();
  }

  async getFulfillmentData(): Promise<any> {
    await this.fulfillmentService.getFulfillmentDetails(this.OrderHeaderKey, !this.isCreateOrder).then(async mashupOutput => {
      if (mashupOutput.Order) {
        this.prepareFulfillmentMethodsDetails(mashupOutput.Order);
      }
    });
  }

  async prepareFulfillmentMethodsDetails(fulfillmentDetails) {
    this.orderDetails = fulfillmentDetails;
    const shippingGroups = get(this.orderDetails, 'ShippingGroups.ShippingGroup', []);
    const pickupGroups = get(this.orderDetails, 'PickupGroups.PickupGroup', []);
    let unAvailablePickupGroup = get(this.orderDetails, 'UnavailableLines.PickupLinesWithNoShipnode.PickupGroup', []);
    if (pickupGroups.length) { pickupGroups.forEach(object => { object.DeliveryMethod = 'PICK'; }); }
    if (typeof unAvailablePickupGroup === 'object' && Object.entries(unAvailablePickupGroup).length) {
      unAvailablePickupGroup.DeliveryMethod = 'PICK';
      unAvailablePickupGroup.isStoreUnavailable = true;
      unAvailablePickupGroup = [unAvailablePickupGroup];
    }
    let unAvailableShippingLines = get(this.orderDetails, 'UnavailableLines.ShippingLines', []);
    let unAvailablePickupLines = get(this.orderDetails, 'UnavailableLines.PickupLines', []);
    if (typeof unAvailableShippingLines === 'object' && Object.entries(unAvailableShippingLines).length) {
      unAvailableShippingLines.DeliveryMethod = 'SHP';
      unAvailableShippingLines.currency = this.orderDetails.PriceInfo?.Currency;
      unAvailableShippingLines.OrderLines = {
        OrderLine : getArray(unAvailableShippingLines.OrderLine)
      }
      delete unAvailableShippingLines.OrderLine;
      unAvailableShippingLines = [unAvailableShippingLines];
    }
    if (typeof unAvailablePickupLines === 'object' && Object.entries(unAvailablePickupLines).length) {
      unAvailablePickupLines.DeliveryMethod = 'PICK';
      unAvailablePickupLines.currency = this.orderDetails.PriceInfo?.Currency;
      unAvailablePickupLines.OrderLines = {
        OrderLine : getArray(unAvailablePickupLines.OrderLine)
      }
      delete unAvailableShippingLines.OrderLine;
      unAvailablePickupLines = [unAvailablePickupLines];
    }

    this.prepareFulfillmentDetails([...shippingGroups, ...pickupGroups, ...unAvailablePickupGroup]);
    this.prepareUnAvailableGroups([...unAvailableShippingLines, ...unAvailablePickupLines]);

    this.checkShippingOptionAllowed();
    this.isLoader = false;
  }

  checkShippingOptionAllowed() {
    const modificationAllowed = IsModificationAllowed(getArray(this.orderDetails?.Modifications?.Modification), Constants.MOD_TYPE_RULE_ID);
    this.editShippingOption = this.isCreateOrder ? true : modificationAllowed
  }

  async getOrderRuleSetsValue() {
    await this.giftOptionDataService.getGiftRuleSetsValue(this.orderDetails.EnterpriseCode).then(
      resp => {
        this.orderRuleSetsValue.giftShipRule = resp.giftShipRule?.Rules.RuleSetValue === Constants.CHECK_YES;
        this.orderRuleSetsValue.giftPickRule = resp.giftPickRule?.Rules.RuleSetValue === Constants.CHECK_YES;
        this.orderRuleSetsValue.giftWrapRule = resp.giftWrapRule?.Rules.RuleSetValue === Constants.CHECK_YES;
      }
    );
  }

  getRuleDetaisForShippingOption() {
    this.isLoader = true;
    const ruleDataPayload = {
      Rules: {
        CallingOrganizationCode: this.orderDetails?.EnterpriseCode,
        DisplayLocalizedFieldInLocale: this.userLocale,
        RuleSetFieldName: 'YCD_SHIPMENT_OPTIMIZATION_RULE',
        CustomerID: this.orderDetails?.BillToID
      },
    };
    this.shippingOptionService.getRuleDetaisForShippingOption( ruleDataPayload ).then(
      (mashupOutput) => {
        this.defaultOptimizationType(mashupOutput);
        this.isLoader = false;
      }
    );
  }

  defaultOptimizationType(ruleModel) {
      const allocationRuleID = this.orderDetails?.AllocationRuleID;
      const allocationRule = ruleModel.Rules.RuleSetValue;
      let customerOptimizationType = null;
      customerOptimizationType = ruleModel.Rules?.CustomerOptimizationType;
      let sourceModelOptimizationType = null;
      sourceModelOptimizationType = {};
      const OptKey = 'OptimizationType';
      if (allocationRuleID === allocationRule) {
        sourceModelOptimizationType[OptKey] = '02';
      } else {
          if (this.orderDetails?.OptimizationType) {
            sourceModelOptimizationType[OptKey] = this.orderDetails?.OptimizationType;
          } else {
              if (!customerOptimizationType) {
                sourceModelOptimizationType[OptKey] = ruleModel.Rules?.EnterpriseOptimizationType;
              } else {
                sourceModelOptimizationType[OptKey] = customerOptimizationType;
              }
          }
      }
      this.defaultRuleOptimizationObj = {
        OptimizationType: sourceModelOptimizationType.OptimizationType,
        ruleDesc: ruleModel.Rules.RuleSetValueDescription
      };
      const shippingOptionsList = [
        {
          value: '03',
          key: 'FULFILLMENT.SHIPMENT_DETAILS.SHIPPING_METHOD_03',
        },
        {
          value: '01',
          key: 'FULFILLMENT.SHIPMENT_DETAILS.SHIPPING_METHOD_01',
        },
        {
          value: '02',
          key: 'FULFILLMENT.SHIPMENT_DETAILS.SHIPPING_METHOD_02'
        }
      ];
      shippingOptionsList.forEach(rule => {
        if (rule.value === sourceModelOptimizationType.OptimizationType){
            this.selectedRuleDescription = rule.key;
        }
      });
  }

  openChangeShippingOptionModal() {
    this.orderCommonService.openRestoreConfirmModal(this.orderDetails, async () => {
      this.modalService.destroy();
      this.modalService.create({
          component: ChangeShippingOptionModalComponent,
          inputs: {
            orderData: {
              size: 'md',
              fulfillmentOrderDetails: this.orderDetails,
              defaultRuleOptimizationObj: this.defaultRuleOptimizationObj,
              successCallback: () => {
               this.onSuccessOfChangeShippingOption();
              }
            }
          }
      });
    })
  }

  async onSuccessOfChangeShippingOption(){
    await this.refreshFulfillmentData();
    await this.getRuleDetaisForShippingOption();
  }

  async deleteOrderLine(orderLineKeys) {
    this.isLoader = true;
    const deleteOrderLinePayload = {
       Order: {
          OrderHeaderKey: this.orderDetails.OrderHeaderKey,
          EnterpriseCode: this.orderDetails.EnterpriseCode,
          OrderLines: {
            OrderLine: orderLineKeys.map((item) => ({
              OrderLineKey: item,
              Action: !this.isCreateOrder ? 'CANCEL' : 'REMOVE'
            }))
          }
       }
    };
    await this.fulfillmentService.deleteOrderLine(deleteOrderLinePayload).then(mashupOutput => {
      if (mashupOutput.Order.OrderHeaderKey) {
        const successMsg = orderLineKeys.length > 1 ? this.nslMap['FULFILLMENT_METHODS.MSG_ORDER_LINES_REMOVED'] :
        this.nslMap['FULFILLMENT_METHODS.MSG_ORDER_LINE_REMOVED'];
        this.orderStoreService.setOrderDetails(mashupOutput);
        // this.updateOrderSummary.emit();
        this.createOrderService.sendMessage(true);
        this.displayMessage('success', successMsg);
      }
    });
  }

  getDateFormat(date1, date2) {
    return getMoment(date1).format(Constants.SHORT_DATE_FORMAT) + ' - ' + getMoment(date2).format(Constants.SHORT_DATE_FORMAT);
  }

  async getFulfillmentDetails(selectedGroup) {
    if (!selectedGroup.expanded) {
      selectedGroup.value.currency = this.orderDetails.PriceInfo?.Currency;
    }
    selectedGroup.expanded = true;
  }

  prepareUnAvailableGroups(groupsData) {
    this.unAvailableGroups = [];
    if (groupsData.length) {
      this.unAvailableGroups = groupsData.map(ele => ({
        content: ele.DeliveryMethod === 'SHP' ? this.nslMap['FULFILLMENT_METHODS.LABEL_UNAVAILABLE_SHIPPING_LINES'] :
        this.nslMap['FULFILLMENT_METHODS.LABEL_UNAVAILABLE_PICKUP_LINES'],
        value: ele,
        method: ele.DeliveryMethod,
        isUnAvailable: true
      }));
    }
    this.sendUnavailableGroups.emit(this.unAvailableGroups);
  }

  prepareFulfillmentDetails(groupsData) {
    if (groupsData.length) {
      this.fulfillmentGroups = groupsData.map((ele, index) => ({
        content: this.nslMap['FULFILLMENT_METHODS.LABEL_'+ ele.DeliveryMethod],
        address: this.getFulfillmentGroupAddress(ele),
        productCountLabel: this.getProductCountLabel(ele),
        storeAvailableCheck: ele.DeliveryMethod === 'SHP'|| (ele.DeliveryMethod === 'PICK' && !ele.isStoreUnavailable),
        fulFillmentGroupId: ele?.FulfilmentGroupID,
        value: ele,
        method: ele.DeliveryMethod,
        expanded: this.fulfillmentGroups[index]?.expanded ? this.fulfillmentGroups[index].expanded : false
      }));
      this.setSelectedGroup();
      this.isCartEmpty = false;
    } else {
      this.fulfillmentGroups = [];
      this.isCartEmpty = true;
    }
  }

  checkSelectedLinesFromMultiGroups(group, groupMethods) {
    if (this.orderLinesForChangeFulfillment.length) {
      const orderLines = group.value?.OrderLines.OrderLine;
      let checkGroupLines = false;
      orderLines.forEach(ele => {
        if (this.orderLinesForChangeFulfillment.indexOf(ele.OrderLineKey) > -1) {
          if(!checkGroupLines) {
            groupMethods.push(group.method);
            checkGroupLines = true;
          }
        }
      });
    }
  }

  checkSelectedLinesGroup(group) {
    if (this.orderLinesForChangeFulfillment.length) {
      const orderLines = group.value?.OrderLines.OrderLine;
      return orderLines.filter(ele => { return ele.OrderLineKey === this.orderLinesForChangeFulfillment[0] }).length;
    }
    return false;
  }

  setSelectedGroup() {
    if (this.fulfillmentGroups.length === 1 || this.selectedFmtGroup?.fulFillmentGroupId || this.orderLinesForChangeFulfillment.length) {
      if (this.fulfillmentGroups.length === 1) {
        this.getFulfillmentDetails(this.fulfillmentGroups[0]);
      } else {
        let methods = [];
        const selectedGroup = this.fulfillmentGroups.filter(ele => {
          this.checkSelectedLinesFromMultiGroups(ele, methods);
          return ele.fulFillmentGroupId === this.selectedFmtGroup?.fulFillmentGroupId || this.checkSelectedLinesGroup(ele);
        });
        if (methods.length > 1) {
          const infoMessage = this.nslMap["FULFILLMENT_METHODS.MSG_ORDER_LINES_FROM_DEFFERENT_FULFILLMENT_GROUP"];
          this.displayMessage("info", infoMessage);
        }
        if (selectedGroup.length) {
          this.getFulfillmentDetails(selectedGroup[0]);
        }
        this.selectedFmtGroup = null;
      }
    }
  }


  getFulfillmentGroupAddress(ele) {
    let address = '';
    switch(ele.DeliveryMethod) {
      case 'SHP':
        address = this.getCombinedAddress(ele.PersonInfoShipTo);
        break;
      case 'PICK':
      case 'CARRY':
        address = ele.isStoreUnavailable ? this.nslMap['SELECT_STORE.GENERAL.LABEL_SELECT_STORE'] : ele.Shipnode.Description + ', ' +
        this.getCombinedAddress(ele.Shipnode.ShipNodePersonInfo);
        break;
    }
    return address;
  }

  getCombinedAddress(addressObj) {
    let address = addressObj.AddressLine1 ? addressObj.AddressLine1 + ', ' : '';
    address += addressObj.City ? addressObj.City + ' ' : '';
    address += addressObj.ZipCode ? addressObj.ZipCode : '';
    return address;
  }

  getProductCountLabel(ele) {
    let productCountLabel = "", orderLines = [];
    orderLines = ele.OrderLines?.OrderLine;
    productCountLabel = orderLines.length + ' ' + (orderLines.length > 1 ? this.nslMap['FULFILLMENT.SHIPMENT_DETAILS.LABEL_PRODUCTS'] :
    this.nslMap['FULFILLMENT.SHIPMENT_DETAILS.LABEL_PRODUCT']);
    return productCountLabel;
  }


  setPickupAppointment(event){
    this.setPickupAppointmentDate(event.value, event.group)
  }

  setPickupAppointmentDate(value, group) {
    this.selectedFmtGroup = group;
    const earliestAvailableDate = group.value?.OrderLines?.OrderLine[0]?.EarliestShipDate;
    const tz = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserTimeZone();
    const availableDate = fmtDate( BucDateTimeHelper.getMomentWithTimezone(earliestAvailableDate, tz), Constants.MOMENT_DATE_FORMAT);
    this.appointmentDate = value;
    if (getMoment(value).isSameOrAfter(availableDate)) {
      this.isLoader = true;
      this.savePickupDate(group);
    }
  }

  savePickupDate(groupElement) {
    const changePickupSlotPaylod: any = {
      Order: {
        OrderHeaderKey: this.orderDetails.OrderHeaderKey,
        EnterpriseCode: this.orderDetails.EnterpriseCode,
        OrderLines: {
          OrderLine: groupElement.value.OrderLines?.OrderLine?.map((item) => ({
            OrderLineKey: item.OrderLineKey,
            DeliveryMethod: 'PICK',
            ReqShipDate: this.appointmentDate
          }))
        }
      }
    };
    this.fulfillmentService.modifyFulfillmentMethodFromShipToPick(changePickupSlotPaylod).then(
      mashupOutput => {
        if (mashupOutput.Order) {
          this.displayMessage('success', this.nslMap['FULFILLMENT.PICKUP_APPOINTMENT.MSG_PICKUP_APPOINTMENT_UPDATED']);
          this.prepareFulfillmentMethodsDetails(mashupOutput.Order);
        }
      });
  }

  displayMessage(messageType, message) {
    this.ccNotificationService.notify({
      type: messageType,
      title: message,
    });
  }

  openLevelOfServiceModal() {
    this.modalService.destroy();
    this.levelOfServiceModalData = {
      modalText: {
        header: this.nslMap['FULFILLMENT.SHIPMENT_DETAILS.LABEL_CHANGE_LEVEL_OF_SERVICE'],
        label: undefined,
        template: this.levelOfServiceModalTemplate,
        className: 'levelOfServiceModal'
      },
      optionOne: {
        text: this.nslMap['SHARED.GENERAL.LABEL_CANCEL'],
      },
      optionTwo: {
        text: this.nslMap['SHARED.GENERAL.LABEL_SAVE'],
        callback: this.onSaveCarrierService.bind(this),
        tid: 'level-of-service-modal-save',
        class: { primary: true },
        disabled: true,
        ckCb: true
      }
    };
    // Utils Binary option modal with Cancel and Save text for buttons
    this.levelOfServiceModal = this.modalService.create({
      component: CommonBinaryOptionModalComponent,
      inputs: this.levelOfServiceModalData
    });
  }

  onCarrierSvcValSearch(evt) {
    this.selectedFmtGroup.isCarrierSvcInvalid = false;
    if (evt) {
      this.selectedFmtGroup.isCarrierSvcInvalid = this.selectedFmtGroup.carrierServiceList.some(el => el.content !== evt);
    }
  }

  onSelectCarrierService(evt) {
    this.selectedFmtGroup.isCarrierSvcInvalid = false;
    if (evt.length === 0 ) {
      this.levelOfServiceModalData.optionTwo.disabled = true;
    } else {
      this.selectedFmtGroup.carrierServiceList.forEach(el => el.selected = el.id === evt.item.id);
      if (this.selectedFmtGroup.initialCarrierServiceCode !== evt.item.id) {
        this.levelOfServiceModalData.optionTwo.disabled = false;
      } else {
        this.levelOfServiceModalData.optionTwo.disabled = true;
      }
    }
  }

  onSaveCarrierService() {
    this.selectedFmtGroup.isCarrierSvcInvalid = false;
    const levelOfService = this.selectedFmtGroup.carrierServiceList?.find(el => el.selected)?.CarrierServiceCode;
    this.saveLevelOfService(levelOfService);
  }

  async saveLevelOfService(carrierServiceEvt) {
    if ((carrierServiceEvt?.length || carrierServiceEvt?.item?.CarrierServiceCode) && !this.selectedFmtGroup.isCarrierSvcInvalid) {
      this.isLoader = true;
      const carrierServiceCode = carrierServiceEvt?.item?.CarrierServiceCode ? carrierServiceEvt?.item?.CarrierServiceCode : carrierServiceEvt;
      const orderLines = this.inputOrderLines;
      const input: any = {
          orderHeaderKey: this.orderDetails.OrderHeaderKey,
          docType: this.orderDetails.DocumentType,
          orgCode: this.orderDetails.EnterpriseCode,
          personInfoShipTo: this.selectedFmtGroup?.value.PersonInfoShipTo,
          carrierServiceCode: carrierServiceCode,
          orderLines
      };
      if(carrierServiceCode){
        input['deliveryStartDate'] = carrierServiceEvt?.item?.DeliveryStartDate;
        input['deliveryEndDate'] = carrierServiceEvt?.item?.DeliveryEndDate;
      }
      await this.fulfillmentService.modifyFulfillmentMethodFromPickToShip(input).then(mashupOutput => {
          if (mashupOutput.Order) {
            this.displayMessage('success', this.nslMap['FULFILLMENT_METHODS.MSG_LEVEL_OF_SERVICE_SUCCESS']);
          }
        }).catch(error => this.isLoader = false);
    }
  }

  async getCarrierServiceOptionsList(groupElement,selected?) {
    const selectedKeys = new Set(selected);
    groupElement.isCarrierSvcInvalid = false;
    let orderLinesInput: any = [];
    orderLinesInput = groupElement?.value.OrderLines.OrderLine
    .filter(line => selectedKeys.has(line.OrderLineKey))
    .map(line => ({
      ItemID: line.ItemDetails.ItemID,
      OrderLineKey: line.OrderLineKey,
      EarliestShipDate: line.EarliestShipDate,
      IsParcelShippingAllowed: line.ItemDetails.PrimaryInformation.IsParcelShippingAllowed,
      PersonInfoShipTo: groupElement?.value.PersonInfoShipTo
    }));
    await this.orderCommonService.getCarrierServiceOptions(this.orderDetails.OrderHeaderKey, orderLinesInput)
        .then(mashupOutput => {
          if (mashupOutput.CarrierServiceList && mashupOutput.CarrierServiceList.CarrierService?.length) {
            groupElement.carrierServiceList = mashupOutput.CarrierServiceList?.CarrierService;
            if (groupElement.carrierServiceList && groupElement.carrierServiceList.length) {
              groupElement.carrierServiceList.sort((a, b) =>
                a.CarrierServiceDesc > b.CarrierServiceDesc ? 1 : 
                a.CarrierServiceDesc < b.CarrierServiceDesc ? -1 : 0
              )
              groupElement.carrierServiceList.forEach(el => {
                // content value is in this format : Express (2022-09-05 - 2022-10-06)
                const content = `${el.CarrierServiceDesc}`+ ' (' + this.getDateFormat(el.DeliveryStartDate, el.DeliveryEndDate) + ')';
                el.content = content;
                el.selected = (groupElement?.value.CarrierServiceCode === el.CarrierServiceCode) ? true : false
                el.id = el.value = el.CarrierServiceCode;
                if (el.selected) {
                  groupElement.initialCarrierServiceCode = el.id;
                  groupElement.expectedDate = this.getDateFormat(el.DeliveryStartDate, el.DeliveryEndDate);
                }
                this.selectedFmtGroup = groupElement;
              });
            }
          }
      });
  }

  async getSelectedItemsFromTable(event) {
    if (event.action === 'giftOptions') {
      this.giftOptions(event);
    } else if (event.action === 'manageInstructions') {
      this.manageInstruction(event);
    } else if(event.action === 'adjustPricing'){
      this.adjustPricing(event);
    } else if(event.action === 'overridePrice'){
      this.openOverridePriceModal(event);
    } else if(event.action === 'notes'){
      this.openViewAllNotesModal(event.selected[0].line);
    } else if(event.action === 'showLinePricingSummary'){
      this.openLinepricingSummaryModal(event.selectedLine);
    } else if (event.action === 'showViewAllNotesModal') {
      this.openViewAllNotesModal(event.selectedLine)
    } else {
      this.selectedItemsFromTable = [];
      this.selectedItemsFromTable = event.selected;
      this.dispatchAction(event.action, event.groupElement, true,event);
    }
  }

  deliveryAction(event){
    this.dispatchAction(event.action, event.group, false, event);
  }

  async dispatchAction(action, group, isActionFromTable=false, event?) {
    this.selectedFmtGroup = group;
    this.inputOrderLines = group.value.OrderLines?.OrderLine;
    if (isActionFromTable) {
      this.inputOrderLines = this.inputOrderLines.filter(line => this.selectedItemsFromTable.indexOf(line.OrderLineKey) !== -1);
    }
    switch (action) {
      case 'changeStoreAddress' :
        this.openSelectStoreModal();
        break;
      case 'changePickupRecipient' :
        this.openChangePickupRecipientModal();
        break;
      case 'changePickupDate':
        if (isActionFromTable) {
          this.openPickupAppointmentModal();
        }
        break;
      case 'changeShippingAddress':
        this.openEditShippingAddressModal();
        break;
      case 'changeLevelOfService':
        if(isActionFromTable) {
          this.getCarrierServiceOptionsList(group,event.selected);
          this.openLevelOfServiceModal();
        } else {
          this.saveLevelOfService(event.value);
        }
        break;
      case 'changeToShipping':
          this.openEditShippingAddressModal();
          break;
      case 'changeToPickup':
          this.openSelectStoreModal();
          break;
      //EOMS-1463  Changes Start
      case 'orderReason':
        this.saveOrderReason(event?.value);
        break;
      //EOMS-1463  Changes End
      //EOMS-13713  Changes Start
      case 'costCenter':
        this.saveCostCenter(event?.value);
        break;
      //EOMS-13713  Changes End
    }
  }

  giftOptions(ev): void {
    this.selectedFmtGroup = ev.groupElement;
    this.actionProcessorService.dispatch<ActionParams>(Constants.GIFT_OPTIONS, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          orderDetails: this.orderDetails,
          orderLines: ev.loadedItems,
          selectedOrderLines: ev.selected,
          orderRuleSetsValue: this.orderRuleSetsValue,
          checkWithoutModificationRule: true,
          isLineLevel: true,
          size: 'lg'
        }
      }
    });
  }


  adjustPricing(ev): void {
    this.actionProcessorService.dispatch<ActionParams>(Constants.ADJUST_PRICING, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          summaryDetails: this.orderStoreService.getOrderDetails().Order,
          lineDetails: ev.selected[0],
          skipModificationPermissionCheck:true,
          isLineLevel: true,
          size: 'lg',
          component: this.componentId
        }
      }
    });
  }

  openOverridePriceModal(event) {
    //const row = this.orderlinesListModel.data.find((curRow: any) => curRow.find((item) => item.id === selected.OrderLineKey));
    // const priceColIndex = this.orderlinesListModel.header[0].find((item) => item.id === this.TH_TOTAL_AMOUNT)?.sequence - 1;
    this.actionProcessorService.dispatch<ActionParams>(Constants.OVERRIDE_PRICE, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          enterpriseCode: this.orderDetails.EnterpriseCode,
          price: event.selected[0].line.LineOverallTotals.UnitPrice,
          saveInsideModal: true,
          orderHeaderKey: this.orderDetails.OrderHeaderKey,
          orderLine: event.selected[0].line,
          bundleComponent:  event.selected[0].line.BundleParentLine ? this.bundleParentKeyMapper[event.selected[0].BundleParentLine.OrderLineKey] : null,
          currency: this.orderDetails.PriceInfo?.Currency
        }
      }
    });
  }

  manageInstruction(ev): void {
    this.selectedFmtGroup = ev.groupElement;
    this.actionProcessorService.dispatch<ActionParams>(Constants.MANAGE_INSTRUCTIONS, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          summaryDetails: this.orderDetails,
          lineDetails: ev.selected,
          isLineLevel: true,
          isActionFromCreateOrder: true,
          size: 'lg'
        }
      }
    });
  }

  getStoreAddressToSend() {
     let addressToSend;
     if (this.selectedFmtGroup.isUnAvailable) {
        addressToSend = (this.selectedFmtGroup.method === 'SHP' || this.inputOrderLines.length > 1) ? this.orderDetails.PersonInfoShipTo :
        this.inputOrderLines[0].Shipnode?.ShipNodePersonInfo;
     } else {
       addressToSend = this.selectedFmtGroup.method === 'PICK' ? this.selectedFmtGroup.value.Shipnode?.ShipNodePersonInfo :
       this.selectedFmtGroup?.value.PersonInfoShipTo;
     }
     return addressToSend;
  }

  openSelectStoreModal() {
    const addressToSend = this.getStoreAddressToSend();
    this.actionProcessorService.dispatch<ActionParams>(Constants.SELECT_STORE, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          orderHeaderKey: this.orderDetails.OrderHeaderKey,
          enterpriseCode: this.orderDetails.EnterpriseCode,
          personInfoShipTo: addressToSend,
          orderLines : this.inputOrderLines.map((item) => ({
            OrderLineKey: item.OrderLineKey,
            RequiredQty: item.OrderedQty,
            Item: {
              ItemID: item.ItemDetails.ItemID,
              UnitOfMeasure: item.ItemDetails.UnitOfMeasure,
              ProductClass: item.ItemDetails.PrimaryInformation.DefaultProductClass
            }
          })),
          selectedShipNode: this.selectedFmtGroup.value.ShipNode,
          size: 'lg'
        }
      }
    });
  }

  getShippingAddressToSend() {
    let addressToSend;
     if (this.selectedFmtGroup.isUnAvailable) {
        addressToSend = (this.selectedFmtGroup.method === 'SHP' && this.inputOrderLines.length === 1 && this.inputOrderLines[0]?.PersonInfoShipTo) ?
        this.inputOrderLines[0].PersonInfoShipTo : this.orderDetails.PersonInfoShipTo;
     } else {
       addressToSend = this.selectedFmtGroup.method === 'SHP' ? this.selectedFmtGroup?.value.PersonInfoShipTo :
       this.orderDetails.PersonInfoShipTo;
     }
     return addressToSend;
  }

  // Edit shipping address modal
  async openEditShippingAddressModal(): Promise<void> {
    const addressToSend = this.getShippingAddressToSend();
    this.actionProcessorService.dispatch<ModifyCustomerAddressParams>(Constants.MODIFY_CUSTOMER_ADDRESS, {
      component: this.componentId,
      data: {
        modalText: this.nslMap['FULFILLMENT.FULFILLMENT_GROUPS.LABEL_MODIFY_SHIPPING_ADDRESS'],
        modalData: {
          orderDetails: this.orderDetails,
          EnterpriseCode: this.orderDetails.EnterpriseCode,
          currentShipment: {
            PersonInfoShipTo: addressToSend
          },
          customerId: this.orderDetails.ShipToID,
          isUnavailableMultiLines: this.selectedFmtGroup.isUnAvailable && this.inputOrderLines.length > 1,
          saveCall: this.updateAddress.bind(this)
        }
      }
    });
  }

  updateAddress(params) {
    this.isLoader = true;
    const input: any = {
      Order: {
        Action: 'MODIFY',
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
        IgnoreOrdering: 'Y',
        ValidateItems: 'Y',
        OrderHeaderKey: this.orderDetails.OrderHeaderKey,
        DocumentType: this.orderDetails.DocumentType,
        EnterpriseCode: this.orderDetails.EnterpriseCode,
        isOrderShippingAddress: params.isOrderShippingAddress,
        isOrderBillingAddress: params.isOrderBillingAddress,
        PersonInfoShipTo: params.modifiedAddress,
        OrderLines: {
          OrderLine : this.inputOrderLines.map(line => ({
            OrderLineKey: line.OrderLineKey,
            ShipNode: ' ',
            DeliveryMethod: 'SHP',
            PersonInfoShipTo: params.modifiedAddress,
            CarrierServiceCode: this.selectedFmtGroup.carrierServiceList?.find(el => el.selected)?.CarrierServiceCode || '',
          }))
        }
      }
    };
    const fulfillmentParams = { modifyFulfillmentInput: input, orderLines: this.inputOrderLines }
    this.fulfillmentItemsTable.checkForShippingLinesAvailability(params.modifiedAddress, params.addressField, fulfillmentParams);
  }


  async refreshFulfillmentDetails(checkAvailability) {
    if (checkAvailability.isLinesAvailable === 'N') {
      this.ccNotificationService.notify({
        type: 'error',
        title: this.nslMap['FULFILLMENT_METHODS.ORDER_LINES_NOT_AVAILABLE_FOR_SHIPPING'],
      });
      this.isLoader = false;
      return 0;
    }
    let successMsg = "";
    if (this.selectedFmtGroup.method === 'PICK') {
      successMsg = await this._getNls('FULFILLMENT_METHODS.FULFILLMENT_SHIP_SUCCESS', { count: this.selectedItemsFromTable.length });
    } else {
      successMsg = this.nslMap['FULFILLMENT_METHODS.MSG_SHIPPING_ADDRESS_SUCCESS'];
    }
    if (checkAvailability.fulfillmentDetails) {
      this.displayMessage('success', successMsg);
      this.orderLinesForChangeFulfillment = [];
      this.prepareFulfillmentMethodsDetails(checkAvailability.fulfillmentDetails);
    }
  }

  createInputForPickupDetails(pickupDetails) {
    const input: any = {
      Order: {
        OrderHeaderKey: this.orderDetails.OrderHeaderKey,
        DocumentType: this.orderDetails.DocumentType,
        EnterpriseCode: this.orderDetails.EnterpriseCode,
        OrderLines: {
          OrderLine: this.inputOrderLines.map((item) => ({
            OrderLineKey: item.OrderLineKey,
            DeliveryMethod: 'PICK',
            ...(pickupDetails.isPickedByCustomer && { MarkForKey: ' ' }),
            ...(pickupDetails.selectedStore) && { ShipNode: pickupDetails.selectedStore.ShipNode },
            ...(pickupDetails.selectedAppointmentDate && { ReqShipDate: pickupDetails.selectedAppointmentDate }),
            ...(!pickupDetails.isPickedByCustomer && pickupDetails.pickedByInfo &&
              { PersonInfoMarkFor: pickupDetails.pickedByInfo })
          }))
        }
      }
    };
    return input;
  }

  async savePickupDetails(pickupDetails, successMsg) {
    if (this.selectedFmtGroup.method === 'SHP') {
      successMsg = await this._getNls('FULFILLMENT_METHODS.FULFILLMENT_PICK_SUCCESS', { count: this.selectedItemsFromTable.length });
    }
    const pickupDetailsPayload = this.createInputForPickupDetails(pickupDetails);
    this.fulfillmentService.modifyFulfillmentMethodFromShipToPick(pickupDetailsPayload).then(
      mashupOutput => {
        if (mashupOutput.Order) {
          this.displayMessage('success', successMsg);
          this.orderLinesForChangeFulfillment = [];
          this.prepareFulfillmentMethodsDetails(mashupOutput.Order);
        }
      });
  }

  async openPickupAppointmentModal() {
    let earliestShipDate = "", reqShipDate = "";
    earliestShipDate = this.selectedFmtGroup.value?.OrderLines?.OrderLine[0]?.EarliestShipDate;
    reqShipDate = this.selectedFmtGroup.value?.OrderLines?.OrderLine[0]?.ReqShipDate;
    this.orderCommonService.openRestoreConfirmModal(this.orderDetails, () => {
      this.actionProcessorService.dispatch<any>(Constants.PICKUP_APPOINTMENT, {
        component: this.componentId,
        data: {
          modalText: '',
          modalData: {
            orderHeaderKey: this.orderDetails.OrderHeaderKey,
            isLargeOrder: this.isLargeOrder,
            enterpriseCode: this.orderDetails.EnterpriseCode,
            selectedGroup: this.selectedFmtGroup.value,
            earliestAvailableDate: earliestShipDate,
            selectedAppointmentDate: reqShipDate,
            getDataWithoutSave: true
          }
        }
      });
    });
  }

  openChangePickupRecipientModal() {
    this.actionProcessorService.dispatch<ActionParams>(Constants.PICKUP_RECIPIENT, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          size: 'sm',
          orderHeaderKey: this.orderDetails.OrderHeaderKey,
          selectedGroup: this.selectedFmtGroup.value,
          getDataWithoutSave: true,
          orderLineKey: !this.selectedFmtGroup.FulfilmentGroupID ? this.inputOrderLines[0].OrderLineKey : null,
        }
      }
    });

  }

  private openViewAllNotesModal(event): void {
    const notesModalInput = event;
    notesModalInput.OrderHeaderKey=this.orderDetails.OrderHeaderKey;
    if (notesModalInput) {
        this.modalService.create({
          component: ViewAllNotesModalComponent,
          inputs: {
            modalData: {
              orderDetails: notesModalInput,
              notesList: notesModalInput.Notes.Note,
              notesReason: this.notesTypeList,
              callBack: (noteList) => {
                this.addNotesCallback(noteList, notesModalInput);
              }
            }
          }
        });
    }
  }

  addNotesCallback(updatedNoteList, notesModalInput)  {
    notesModalInput.Notes.Note = updatedNoteList;
    if (notesModalInput.Notes?.Note && notesModalInput.Notes.Note.length) {
      notesModalInput.HasNotes = 'Y';
      for (const lineNote of notesModalInput.Notes.Note) {
        const noteTime = new Date(lineNote.ContactTime).getTime();
        const codeDescription = this.notesTypeList.filter(codeItem => codeItem.value === lineNote.ReasonCode);
        lineNote.ReasonText = codeDescription.length ? codeDescription[0].content : '';
        lineNote.displayNote = true; // for search field selections
        lineNote.isFiltered = true; // for checkbox selections
        lineNote.typeFiltered = true; // for dropdown filtering
        lineNote.isNewNote = noteTime >= (Date.now() - 5000) ? true : false;
      }
      notesModalInput.Notes.Note.sort((a, b) => {
        const keyA = new Date(a.ContactTime).getTime();
        const keyB = new Date(b.ContactTime).getTime();
        return keyB - keyA;
      });
      notesModalInput.Notes.NumberOfNotes = updatedNoteList.length;
    } else {
      notesModalInput.HasNotes = 'N';
      notesModalInput.Notes.NumberOfNotes = 0;
    }
  }

  async initializeNotes() {
    let reasonCodeList;
    if(this.orderCommonService.getReasonCodeListForNotes().length > 0) {
      reasonCodeList = this.orderCommonService.getReasonCodeListForNotes()
    } else {
      const getCommonCodeListInput = {
        CommonCode: {
          CallingOrganizationCode: this.orderDetails && this.orderDetails.EnterpriseCode,
          CodeType: 'NOTES_REASON',
          DocumentType: this.orderDetails && this.orderDetails.DocumentType,
          DisplayLocalizedFieldInLocale: this.userLocale
        }
      };
      const resp = await this.orderCommonService.getCommonCodeListForNotes(getCommonCodeListInput);
      reasonCodeList = resp && resp.CommonCodeList && resp.CommonCodeList.CommonCode;
      if(reasonCodeList?.length > 0) {
        this.orderCommonService.setReasonCodeListForNotes(reasonCodeList);
      }
    }
    if (reasonCodeList && reasonCodeList.length) {
      this.notesTypeList = reasonCodeList.map(item => ({
        content: item.CodeShortDescription || item.CodeLongDescription,
        selected: false,
        value: item.CodeValue
      }));
    }
    else {
      this.notesTypeList = [];
    }
  }

  openLinepricingSummaryModal(selectedLine) {
    this.actionProcessorService.dispatch<ActionParams>(Constants.LINE_PRICING_SUMMARY, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          component: this.componentId,
          orderLineDetails: selectedLine,
          size: 'md',
          skipModificationPermissionCheck:true,
          summaryDetails: this.orderDetails,
          ruleSetValues: this.createOrderService.getAllRuleSetValues(),
          isAdjustPricingResourceAllowed: this.isAdjustPricingResourceAllowed
        }
      }
    });
  }

  validateAndReserveOrder(){
   return this.fulfillmentItemsTable.validateAndReserveOrder();
  }


  ngOnDestroy() {
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  //EOMS-1463 - Changes Start
  async saveOrderReason(orderReasonEvt) {
    if ((orderReasonEvt?.length || orderReasonEvt?.item?.value)) {
      this.isLoader = true;
      const orderReason = orderReasonEvt?.item?.value
      const input: any = {
        orderHeaderKey: this.orderDetails.OrderHeaderKey,
        docType: this.orderDetails.DocumentType,
        orgCode: this.orderDetails.EnterpriseCode,
        Extn:{
          extnReasonCode: orderReason,
        }
      };

      await this.fulfillmentService.changeOrderReason(input).then(mashupOutput => {
        if (mashupOutput.Order) {
          this.isLoader = false
          this.displayMessage('success', 'Updated Order Reason Code!');
        }
      }).catch(error => this.isLoader = false);
    }
    //EOMS-1463 - Changes End

  //EOMS-13713 - Changes Start
  async saveCostCenter(costCenterEvt) {
    if (costCenterEvt?.length || costCenterEvt?.item?.value) {
      this.isLoader = true;
      const costCenter = costCenterEvt?.item?.value;
      const input: any = {
        orderHeaderKey: this.orderDetails.OrderHeaderKey,
        docType: this.orderDetails.DocumentType,
        orgCode: this.orderDetails.EnterpriseCode,
        Extn: {
          extnCostCenter: costCenter,
        },
      };

      await this.fulfillmentService
        .changeCostCenter(input)
        .then((mashupOutput) => {
          if (mashupOutput.Order) {
            this.isLoader = false;
            this.displayMessage('success', 'Updated Cost Center!');
          }
        })
        .catch((error) => (this.isLoader = false));
    }
    //EOMS-13713 - Changes End
  }

}
