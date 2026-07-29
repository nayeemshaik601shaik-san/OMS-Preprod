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

import { Component, OnInit, Input, Output, EventEmitter, OnDestroy, ViewChild, TemplateRef, Injector, ViewContainerRef } from '@angular/core';
import { BucBaseUtil, BucCommOmsRestAPIService, BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
// import { OrderCommonService } from '../../../data-service/order-common.service';
import { OrderCommonService } from '@call-center/order-shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalService } from 'carbon-components-angular';
// import { ManageInstructionModalComponent } from '../../manage-instruction-modal/manage-instruction-modal.component';
import { ManageInstructionModalComponent } from '@call-center/order-shared';
import { ActionProcessorService, CCNotificationService, CommonBinaryOptionModalComponent, DisplayRulesHelperService, EditableFieldRenderer, PickupSlotAppointmentService, fmtDate, getArray, getMoment } from '@buc/common-components';
import { ChangePickupRecipientModalComponent, ChangeShippingOptionModalComponent, ChangeShippingOptionService } from '@buc/cc-components';
// import { Constants } from '../../../common/order.constants';
import { Constants } from '@call-center/order-shared';
// import { ActionParams } from '../../../actions/paramtypes/paramtypes';
import { ActionParams } from '@call-center/order-shared';
import { Observable, of, Subscription } from 'rxjs';
// import { TrackingNumberModalComponent } from '../../tracking-number-modal/tracking-number-modal.component';
import { TrackingNumberModalComponent } from '@call-center/order-shared';
// import { SharedExtensionConstants } from './../../../shared-extension.constants';
import { SharedExtensionConstants } from '@call-center/order-shared';
// import { getUniqueLineInstructions, getUniquegroupInstructions, getUniqueInstructionsForShipment } from '../../../utils/instruction-utils';
import { getUniqueLineInstructions,getUniquegroupInstructions, getUniqueInstructionsForShipment } from '@call-center/order-shared/lib/utils/instruction-utils';
import moment from 'moment';
// import { IsModificationAllowed } from '../../../utils/order-utils';
import { IsModificationAllowed } from '@buc/common-components';
import { get } from 'lodash';
// import { ModifyFulfillmentMethodService } from '../../../data-service/modify-fufillment-method.service';
import { ModifyFulfillmentMethodService } from '@call-center/order-shared';

@Component({
  // EOMS-6178 Update this selector value & the import statements 
  selector: 'call-center-fulfillment-group-details[extn]',
  templateUrl: './fulfillment-group-details.component.html',
  styleUrls: ['./fulfillment-group-details.component.scss'],
  providers: [ChangeShippingOptionService]
})
export class ExtnFulfillmentGroupDetailsComponent extends EditableFieldRenderer implements OnInit, OnDestroy {
  EXTENSION = {
    TOP: SharedExtensionConstants.FULFILLMENT_GROUP_DETAILS_RS_TOP,
    BOTTOM: SharedExtensionConstants.FULFILLMENT_GROUP_DETAILS_RS_BOTTOM
  };
  componentId = 'fulfillment-group-details';

  @Input() groupElement;
  @Input() orderDetails;
  @Input() instructionTypeList;
  @Input() showActionPanel;
  @Input() fulfillmentOrderDetails;
  @Input() isLargeOrder;
  @Input() isOrderLine;
  @Input() resourceIdsForOrderDetailsActions;
  @Input() maxOrderStatus;
  @Input() isTransferOrder = false;

  showLevelOfSvcNotificationErr = false;
  levelOfSvcNotificationErrNotifObj: any;

  @Output() parentPageRefresh = new EventEmitter<any>();
  @Output() navigateToChangeFulfillment = new EventEmitter<any>();
  @ViewChild('levelOfServiceModalTemplate', { static: false }) levelOfServiceModalTemplate: TemplateRef<any>;
  @ViewChild('shipNodeAddressModalTemplate', { static: false }) shipNodeAddressModalTemplate: TemplateRef<any>;
  @ViewChild('addressDisplay', { static: true }) addressDisplay: TemplateRef<any>;
  @ViewChild('labelEditTpl', { static: true }) labelEditTpl: TemplateRef<any>;
  @ViewChild('labelLinkTpl', { static: true }) labelLinkTpl: TemplateRef<any>;

  FIELD_INSTRUCTION = "manageInstruction";
  FIELD_ADDRESS = "address";
  FIELD_TRACKING_NUMBER = "trackingNumber";
  FIELD_SERVICE_LEVEL = "levelOfService"
  FIELD_STORE_ADDRESS = "storeAddress";
  FIELD_PICKUP_APPOINTMENT = "pickupAppointment";
  FIELD_FULFILL = "fulfillmentMethod";
  FIELD_SERVICE_APPOINTMENT = "serviceAppointment"
  FIELD_CHANGE_PICKUP = "changePickupRecipient"

  public nslMap = {
      'FULFILLMENT.FULFILLMENT_GROUPS.LABEL_ADDRESS_DESCRIPTION': '',
      'FULFILLMENT.FULFILLMENT_GROUPS.MSG_SUCCESS_SHIPMENTADDRESS_UPDATE': '',
      'FULFILLMENT.FULFILLMENT_GROUPS.MSG_FAILED_SHIPMENTADDRESS_UPDATE': '',
      'FULFILLMENT.FULFILLMENT_GROUPS.LABEL_MODIFY_SHIPPING_ADDRESS': '',
      'ORDER_SUMMARY.FULFILLMENT_GROUPS.HEADER_LEVEL_OF_SERVICE_MODAL': '',
      'ORDER_SUMMARY.FULFILLMENT_GROUPS.MSG_LEVEL_OF_SERVICE_SUCCESS': '',
      'SHARED.GENERAL.LABEL_CANCEL': '',
      'SHARED.GENERAL.LABEL_SAVE': '',
      'FULFILLMENT.SHIPMENT_DETAILS.LABEL_ITEMS_IN': '',
      'ORDER_SUMMARY.SHIP_NODE_ADDRESS_MODAL.LABEL_SHIP_NODE_ADDRESS': '',
      'ORDER_SUMMARY.SHIP_NODE_ADDRESS_MODAL.LABEL_CLOSE': '',
      'FULFILLMENT.SHIPMENT_DETAILS.LABEL_SHIP_NODE_DESC': '',
      'FULFILLMENT.SHIPMENT_DETAILS.LABEL_SHIP_NODE': ''
  };

  orderHeaderKey: string;
  subscriptions: Subscription[] = [];
  disableShippingOption = false;
  userLocale: string;
  defaultRuleOptimizationObj = {};
  selectedRuleDescription: string;
  selectedFulfillmentGroup: any;
  isOrderArchived: boolean;

  // Level of service modal
  carrierServiceList: any;
  initialCarrierServiceCode: any;
  levelOfServiceModalData: any;
  isCarrierSvcInvalid = false;
  levelOfServiceModal: any;
  addressObj: any;
  isEditable = false;
  progressIndicatorType = '';
  isModalLoading: boolean = false;
  isStampFirstPromiseDate: boolean;

  //EOMS-6178
  reShipAllowed: boolean = true;   

  constructor(
      inj: Injector,
      private orderCommonService: OrderCommonService,
      public translate: TranslateService,
      public modalService: ModalService,
      public bucCommOmsRestAPIService: BucCommOmsRestAPIService,
      private actionProcessorService: ActionProcessorService,
      private ccNotificationService: CCNotificationService,
      private ShippingOptionService: ChangeShippingOptionService,
      private pickupSlotAppointmentService: PickupSlotAppointmentService,
      private modifyfulfillmentMethodService: ModifyFulfillmentMethodService,
      private displayRuleHelperService: DisplayRulesHelperService,
  ) {
    super(inj);
  }

  ngOnInit(): void {

    this.initialize();
    const actionSub = this.actionProcessorService.selectUpdate<any>(this.componentId).subscribe(res => {
      this.initialize();
      if (res.params.refresh) {
        this.parentPageRefresh.emit('fulfillment-group-tab');
      }
    });
    this.subscriptions.push(actionSub);
  }

  async initialize(): Promise < any > {
    await this._initTranslations();
    this.userLocale = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale();
    this.setSelectedFulfillmentGrp();
    await this.loadOptimizationPanel();
    if(!this.isTransferOrder) {
      await this.getRuleDetaisForShippingOption();
    }
    this.checkForOrderArchived();
    this.initData();
    this.isStampFirstPromiseDate = this.displayRuleHelperService.getRuleValueForOrg(this.orderDetails?.EnterpriseCode, Constants.ICC_STAMP_FIRST_PROMISE_DATE) === Constants.CHECK_YES;
    this.checkForModificationPermission(this.groupElement)
    this.getLineType();
  }

  protected async _initTranslations(): Promise < any > {
    const keys = Object.keys(this.nslMap);
    const json = await this.translate.get(keys).toPromise();
    keys.forEach(k => this.nslMap[k] = json[k]);
  }

  private async initData() {
    if (this.groupElement.method === "SHP"){
      await this.initializeFieldDetailAttributes('shipping-fulfillment');
      this.progressIndicatorType = 'shipping';
    }
    else if (this.groupElement.method === "DEL"){
      await this.initializeFieldDetailAttributes('delivery-fulfillment');
      this.progressIndicatorType = 'delivery';
    }
    else if (this.groupElement.method === "PICK"){
      await this.initializeFieldDetailAttributes('pickup-fulfillment');
      this.progressIndicatorType = 'pick';
    }
    await this.loadPageAttributes().toPromise();
  }

  protected fetchPageAttributeData(): Observable<Array<any>> {
    return of(getArray(this.groupElement));
  }

  protected getDataForAttribute(id: string, item: any): Promise<any> {
    let attr: any = {};
    let data: any = '';
    let isResourceAllowed = false;
    switch (id) {
      case this.FIELD_INSTRUCTION:
        let insObj: any[];
        isResourceAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions?.MANAGE_INSTRUCTIONS)
        this.isEditable = this.isOrderLine ? isResourceAllowed : isResourceAllowed && this.groupElement.isInstuctionsModificationAllowed

        if (this.groupElement.isShipment) {
          insObj = getUniqueInstructionsForShipment(this.groupElement.value.Shipment?.ShipmentLines?.ShipmentLine)
        } else {
          insObj = this.isOrderLine ? getUniqueLineInstructions(this.groupElement.value) : getUniquegroupInstructions(this.groupElement.value.OrderLines.OrderLine)
        }
        data = { template: this.labelEditTpl, templateData: { value: insObj && insObj.length > 0 ? this.getInstructions(insObj) : '', isEditable: this.isEditable, id: this.FIELD_INSTRUCTION} };

        break;
      case this.FIELD_ADDRESS:
        isResourceAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions?.CHANGE_SHIPPING_ADDRESS)
        this.isEditable = this.isOrderLine ? isResourceAllowed : isResourceAllowed && this.groupElement.isShippingAddrEditAllowed;

        data = { template: this.addressDisplay, templateData: { value: this.groupElement.value.PersonInfoShipTo, isEditable: this.isEditable, id: this.FIELD_ADDRESS} };
        break;
      case this.FIELD_TRACKING_NUMBER:
        if (this.groupElement.isShipment) {
          const shipContainer = this.groupElement.value.Shipment.Containers;
          item.newValue = shipContainer.Container ? shipContainer.Container.map(i => i.TrackingNo) : '';
          this.isEditable = item.newValue;
          const url = shipContainer.Container ?  shipContainer.Container.length === 1 ? shipContainer.Container[0].TrackingURL : '' : '';
          data = { template: this.labelLinkTpl, templateData: { value: item.newValue, url: url, id: this.FIELD_TRACKING_NUMBER} };
        } else {
          this.isEditable = false;
           data = { template: this.labelLinkTpl, templateData: { value: '', id: this.FIELD_TRACKING_NUMBER} };
        }
      break;
      case this.FIELD_FULFILL:
        isResourceAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions?.CHANGE_FULFILLMENT_METHOD)
        this.isEditable = this.isOrderLine ? isResourceAllowed : isResourceAllowed && this.groupElement.isFulfillmentMethodEditAllowed;
        data = { template: this.labelEditTpl, templateData: { value: this.groupElement.fulfillmentMethod, isEditable: this.isEditable, id: this.FIELD_FULFILL} };
        break;
      case this.FIELD_SERVICE_LEVEL:
        isResourceAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions?.CHANGE_FULFILLMENT_METHOD)
        this.isEditable = this.isOrderLine ? isResourceAllowed : isResourceAllowed &&  this.groupElement.isCarrierSvcModAllowed;
        data = { template: this.labelEditTpl, templateData: { value: this.groupElement.value.CarrierServiceDesc, isEditable: this.isEditable, id: this.FIELD_SERVICE_LEVEL} };
        break;
      case this.FIELD_STORE_ADDRESS:
        isResourceAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions?.CHANGE_STORE)
        this.isEditable = this.isOrderLine ? isResourceAllowed : isResourceAllowed && this.groupElement.isChangeStoreAllowed;
        data = { template: this.addressDisplay, templateData: { value: this.groupElement.storeAddressObj, isEditable: this.isEditable, id: this.FIELD_STORE_ADDRESS } };
        break;
      case this.FIELD_PICKUP_APPOINTMENT:
        isResourceAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions?.CHANGE_STORE)
        this.isEditable = isResourceAllowed && this.groupElement.isChangePickupAppointmentAllowed;

        if (this.isOrderLine) {
          item.newValue = this.groupElement.value.ReqShipDate ? fmtDate(this.groupElement.value.ReqShipDate, Constants.LONG_DATETIME_FORMAT) : '-';
        } else {
          item.newValue = this.groupElement.value?.OrderLines?.OrderLine[0]?.ReqShipDate ?
          fmtDate(this.groupElement.value?.OrderLines?.OrderLine[0]?.ReqShipDate, Constants.LONG_DATETIME_FORMAT): '-';
        }

        data = { template: this.labelEditTpl, templateData: { value: item.newValue ===  '-' ? '' : this.groupElement.pickupAppointment ?
          fmtDate(this.groupElement.pickupAppointment, Constants.LONG_DATETIME_FORMAT) : '-', isEditable: this.isEditable, id: this.FIELD_PICKUP_APPOINTMENT} };
        break;
      case this.FIELD_SERVICE_APPOINTMENT:
        this.isEditable = !this.isOrderLine && isResourceAllowed;
        let apptStartDate, apptEndDate;
        if (this.groupElement.isShipment) {
          apptStartDate = fmtDate(this.groupElement.value.Shipment.ServiceLine.PromisedApptStartDate, Constants.LONG_DATETIME_FORMAT);
          apptEndDate = fmtDate(this.groupElement.value.Shipment.ServiceLine.PromisedApptEndDate, Constants.LONG_DATETIME_FORMAT);
        } else {
          apptStartDate = fmtDate(this.groupElement.value.ApptStartTimestamp, Constants.LONG_DATETIME_FORMAT);
          apptEndDate = fmtDate(this.groupElement.value.ApptEndTimestamp, Constants.LONG_DATETIME_FORMAT);
        }
        item.newValue = apptStartDate.split('-')[1] + ' - ' + apptEndDate.split('-')[1] + '\n' +
        apptStartDate.split('-')[0];

        data = { template: this.addressDisplay, templateData: { value: item.newValue, isEditable: this.isEditable, id: this.FIELD_SERVICE_APPOINTMENT } };
        break;
      case this.FIELD_CHANGE_PICKUP:
        isResourceAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions?.CHANGE_PICKUP_RECIPIENT)
        this.isEditable = isResourceAllowed &&  this.groupElement.isChangePickupRecipientAllowed;
        var phone: any;
        if (this.isOrderLine) {
           item.newValue = this.orderDetails.PersonInfoMarkFor ? this.orderDetails.PersonInfoMarkFor :
           `${this.orderDetails.Order[0]?.CustomerFirstName} ${this.orderDetails.Order[0].CustomerLastName}`
        } else {
           item.newValue = this.groupElement.value?.OrderLines?.OrderLine[0]?.PersonInfoMarkFor ?
           `${this.groupElement.value?.OrderLines?.OrderLine[0]?.PersonInfoMarkFor.FirstName}
           ${this.groupElement.value?.OrderLines?.OrderLine[0]?.PersonInfoMarkFor.LastName}` : this.orderDetails.CustomerFirstName || this.orderDetails.CustomerLastName ?
           `${this.orderDetails.CustomerFirstName} ${this.orderDetails.CustomerLastName}` : '';

           phone =  this.groupElement.value?.OrderLines?.OrderLine[0]?.PersonInfoMarkFor ?
           this.groupElement.value?.OrderLines?.OrderLine[0]?.PersonInfoMarkFor.DayPhone : ""
        }
        data = { template: this.labelEditTpl, templateData: { value: item.newValue, isEditable: this.isEditable, id: this.FIELD_CHANGE_PICKUP, phone: phone} };
        break;
    }
    attr = { ...attr, data };
    return attr as any;
  }

  getInstructions(instructions) {
    const arr = [];
    let str = '';
    instructions.forEach(obj => {
      this?.instructionTypeList?.forEach(ele => {
        if (ele.CodeValue === obj.InstructionType) {
          obj.InstructionType = ele.CodeShortDescription
        }
      })
      str = obj.InstructionType + ' - ' + obj.InstructionText;
      arr.push(str);
    });
    return arr;
  }

  //Mofification permission for each action
  checkForModificationPermission(selectedGroup) {
    if (Number(selectedGroup.value.Shipment?.Status?.Status) >= 1400 || this.orderDetails?.isHistory === 'Y') {
      selectedGroup.isChangeFulfillmentMethodAllowed = false;
      return 0;
    }
    if (!this.isOrderLine) {
      // disable change fulfillment option if order line status is greater than or equal to included in shipment
      selectedGroup.isChangeFulfillmentMethodAllowed = this.getMaxLineStatus() >= '3350' ? false : true;
      selectedGroup.isCarrierSvcModAllowed = this.modifyfulfillmentMethodService.isCarrierServiceChangeAllowed(selectedGroup?.value);
      selectedGroup.isShippingAddrEditAllowed =  this.modifyfulfillmentMethodService.isShippingAddrEditAllowed(this.getMaxLineStatus(), selectedGroup?.value, this.orderDetails?.EnterpriseCode);
      selectedGroup.isChangeStoreAllowed =  this.modifyfulfillmentMethodService.isChangeStoreAllowed(selectedGroup?.value);
      selectedGroup.isChangePickupRecipientAllowed =  this.modifyfulfillmentMethodService.isChangePickRecipientAllowed(selectedGroup?.value);
      selectedGroup.isChangePickupAppointmentAllowed =  this.modifyfulfillmentMethodService.isChangePickupAppointmentAllowed(selectedGroup?.value);
      selectedGroup.isInstuctionsModificationAllowed =  this.modifyfulfillmentMethodService.isInstructionEditAllowed(selectedGroup?.value, this.fulfillmentOrderDetails);
      selectedGroup.isFulfillmentMethodEditAllowed = this.modifyfulfillmentMethodService.isChangeToShippingAllowed(selectedGroup?.value) && this.modifyfulfillmentMethodService.isChangeToPickupAllowed(selectedGroup?.value, this.orderDetails?.EnterpriseCode)
    }
  }


  getMaxLineStatus() {
    return this.groupElement.value.OrderLines?.OrderLine[0]?.MaxLineStatus || ''
  }

  setSelectedFulfillmentGrp() {
    if (this.groupElement.isShipment) {
      this.selectedFulfillmentGroup = {
        fulFillmentGroupId: this.groupElement.value.Shipment?.ShipmentKey,
        selectionKey: 'ShipmentKey'
      }
    } else {
      this.selectedFulfillmentGroup = {
        fulFillmentGroupId: this.groupElement.value?.FulfilmentGroupID,
        selectionKey: 'FulfilmentGroupID'
      }
    }

  }

  checkForOrderArchived() {
    this.isOrderArchived = this.orderCommonService.isOrderArchived(this.orderDetails);
    return this.isOrderArchived;
  }

  async loadOptimizationPanel() {
    const modificationAllowed = IsModificationAllowed(getArray(this.fulfillmentOrderDetails?.Modifications?.Modification), Constants.MOD_TYPE_RULE_ID);
    if (modificationAllowed) {
      this.disableShippingOption = false;
    } else {
      this.disableShippingOption = true;
    }
  }

  getRuleDetaisForShippingOption() {
    const ruleDataPayload = {
      Rules: {
        CallingOrganizationCode: this.orderDetails?.EnterpriseCode,
        DisplayLocalizedFieldInLocale: this.userLocale,
        RuleSetFieldName: 'YCD_SHIPMENT_OPTIMIZATION_RULE',
        CustomerID: this.orderDetails?.BillToID
      },
    };
    this.ShippingOptionService.getRuleDetaisForShippingOption( ruleDataPayload ).then(
      (mashupOutput) => {
        this.defaultOptimizationType(mashupOutput);
      }
    );
  }

  defaultOptimizationType(ruleModel) {
      const allocationRuleID = this.fulfillmentOrderDetails?.AllocationRuleID;
      const allocationRule = ruleModel?.Rules.RuleSetValue;
      let customerOptimizationType = null;
      customerOptimizationType = ruleModel?.Rules?.CustomerOptimizationType;
      let sourceModelOptimizationType = null;
      sourceModelOptimizationType = {};
      const OptKey = 'OptimizationType';
      if (allocationRuleID === allocationRule) {
        sourceModelOptimizationType[OptKey] = '02';
      } else {
          if (this.fulfillmentOrderDetails?.OptimizationType) {
            sourceModelOptimizationType[OptKey] = this.fulfillmentOrderDetails?.OptimizationType;
          } else {
              if (!customerOptimizationType) {
                sourceModelOptimizationType[OptKey] = ruleModel?.Rules?.EnterpriseOptimizationType;
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

  async goToChangeFulfillmentMethod(openShippingAddrModal?, openStoreAddrModal?) {
    let orderLineKeys = [];
    let fulfillmentObj = {
        orderLineKeys,
        openShippingAddrModal: openShippingAddrModal || false,
        openStoreAddrModal: openStoreAddrModal || false,
        isOrderLine: this.isOrderLine,
        fulfillmentGroupId: undefined
    }
    if (this.isOrderLine) {
      fulfillmentObj.orderLineKeys = [this.orderDetails.OrderLineKey];
    } else {
        fulfillmentObj.fulfillmentGroupId = this.groupElement.value.FulfilmentGroupID;
    }
    this.navigateToChangeFulfillment.emit(fulfillmentObj);
  }

  manageOrderLineInstruction(): void {
    this.actionProcessorService.dispatch<ActionParams>(Constants.MANAGE_INSTRUCTIONS, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          summaryDetails: this.orderDetails.Order[0],
          lineDetails: { line: this.orderDetails },
          isLineLevel: this.isOrderLine,
          size: 'lg',
          successCallback: () => {
            this.parentPageRefresh.emit({tabName : 'fulfillment-group-tab', selectedFulfillmentGrp : this.selectedFulfillmentGroup});
          }
        }
      }
    });
  }

  async openEditInstructionModal(): Promise<void> {
      if (this.isOrderLine) {
        this.manageOrderLineInstruction();
      } else {
        this.orderCommonService.openRestoreConfirmModal(this.orderDetails, async () => {
        this.modalService.destroy();
          this.modalService.create({
            component: ManageInstructionModalComponent,
            inputs: {
              displayData: {
                size: 'lg',
                summary: this.orderDetails,
                selectedShipmentGroup: this.groupElement.value,
                isShipment: this.groupElement.isShipment,
                shipmentDetails: this.groupElement.value.Shipment,
                fulfillmentOrderDetails: this.fulfillmentOrderDetails,
                selectedShipmentContent: this.groupElement?.content,
                successCallback: (fulFillmentGroup) => {
                  this.parentPageRefresh.emit({tabName : 'fulfillment-group-tab', selectedFulfillmentGrp : fulFillmentGroup});
                }
              }
            }
          });
        })
    }
  }

  async dispatchAction(action, url?, trackingNoList?) {
    switch (action) {
      case 'address' :
        // Go to change fulfillment options page and open edit shipping address modal
        this.orderCommonService.openRestoreConfirmModal(this.orderDetails, async () => {
          this.goToChangeFulfillmentMethod(true, false);
        });
        break;
      case 'storeAddress' :
        // Go to change fulfillment options page and open store address modal
        this.orderCommonService.openRestoreConfirmModal(this.orderDetails, async () => {
          this.goToChangeFulfillmentMethod(false, true);
        });
        break;
      case 'trackingNumber' :
        if (url) {
          window.open(url, '_blank');
        } else if (this.checkIsArray(trackingNoList)) {
          this.openTrackingModal();
        }
        break;
      case 'manageInstruction' :
        this.openEditInstructionModal();
        break;
      case 'changePickupRecipient' :
        this.openChangePickupRecipientModal();
        break;
      case 'pickupAppointment':
        this.openPickupAppointmentModal();
        break;
      case 'reship' :
        this.reshipShipment();
        break;
      case 'fulfillmentMethod':
        this.goToChangeFulfillmentMethod();
        break;
      case 'levelOfService':
        this.orderCommonService.openRestoreConfirmModal(this.orderDetails, async () => {
          await this.getCarrierServiceOptionsList();
          this.openLevelOfServiceModal();
        });
        break;
    }
  }

  async getCarrierServiceOptionsList() {
    this.isCarrierSvcInvalid = false;
    this.showLevelOfSvcNotificationErr = false;
    let orderLinesInput: any = [];

    if (this.isOrderLine) {
      orderLinesInput = [
        {
          ItemID: this.orderDetails.ItemDetails.ItemID,
          OrderLineKey: this.orderDetails.OrderLineKey,
          EarliestShipDate: this.orderDetails.EarliestShipDate,
          IsParcelShippingAllowed: this.orderDetails.ItemDetails.PrimaryInformation.IsParcelShippingAllowed || "N",
          PersonInfoShipTo: this.groupElement?.value.PersonInfoShipTo
        }
      ]
    } else {
      orderLinesInput = this.groupElement?.value.OrderLines.OrderLine.map(line => ({
        ItemID: line.ItemDetails.ItemID,
        OrderLineKey: line.OrderLineKey,
        EarliestShipDate: line.EarliestShipDate || this.groupElement?.value.ExpectedDeliveryStartDate,
        IsParcelShippingAllowed: line.ItemDetails.PrimaryInformation.IsParcelShippingAllowed || "N",
        PersonInfoShipTo: this.groupElement?.value.PersonInfoShipTo
      }));
    }


    await this.orderCommonService.getCarrierServiceOptions(this.orderDetails.OrderHeaderKey, orderLinesInput)
        .then(mashupOutput => {
          if (mashupOutput.CarrierServiceList && mashupOutput.CarrierServiceList.CarrierService?.length) {
            this.carrierServiceList = mashupOutput.CarrierServiceList?.CarrierService;
            if (this.carrierServiceList && this.carrierServiceList.length) {
              this.carrierServiceList.sort((a, b) =>
                a.CarrierServiceDesc > b.CarrierServiceDesc ? 1 : 
                a.CarrierServiceDesc < b.CarrierServiceDesc ? -1 : 0
              )
              this.carrierServiceList.forEach(el => {
                // content value is in this format : Express (2022-09-05 - 2022-10-06)
                let content;
                if (BucBaseUtil.isVoid(el.DeliveryStartDate) && BucBaseUtil.isVoid(el.DeliveryEndDate)) {
                  content = el.CarrierServiceDesc;
                } else {
                  content = `${el.CarrierServiceDesc} (${this.getDateFormat(el.DeliveryStartDate, el.DeliveryEndDate)})`;
                }
                 el.content = content;
                  el.selected = (this.groupElement?.value.CarrierServiceCode === el.CarrierServiceCode) ? true : false
                  el.id = el.value = el.CarrierServiceCode;
                if (el.selected) {
                  this.initialCarrierServiceCode = el.id;
                }
              });
            }
          }
      });
  }

  getDateFormat(date1, date2) {
    return getMoment(date1).format(Constants.SHORT_DATE_FORMAT) + ' - ' + getMoment(date2).format(Constants.SHORT_DATE_FORMAT);
  }


  openLevelOfServiceModal() {
    this.modalService.destroy();
    this.levelOfServiceModalData = {
      modalText: {
        header: this.nslMap['ORDER_SUMMARY.FULFILLMENT_GROUPS.HEADER_LEVEL_OF_SERVICE_MODAL'],
        label: undefined,
        template: this.levelOfServiceModalTemplate,
        className: 'levelOfServiceModal'
      },
      optionOne: {
        text: this.nslMap['SHARED.GENERAL.LABEL_CANCEL'],
        tid: 'level-of-service-modal-cancel',
      },
      optionTwo: {
        text: this.nslMap['SHARED.GENERAL.LABEL_SAVE'],
        callback: this.onSaveLevelOfService.bind(this),
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

  onSelectCarrierService(evt) {
    this.isCarrierSvcInvalid = false;
    if (evt.length === 0 ) {
      if (this.initialCarrierServiceCode) {
        this.levelOfServiceModalData.optionTwo.disabled = false;
      }
    } else {
      this.carrierServiceList.forEach(el => el.selected = el.id === evt.item.id);
      if (this.initialCarrierServiceCode !== evt.item.id) {
        this.levelOfServiceModalData.optionTwo.disabled = false;
      } else {
        this.levelOfServiceModalData.optionTwo.disabled = true;
      }
    }
  }

  onCarrierSvcValSearch(evt) {
    this.isCarrierSvcInvalid = false;
    if (evt) {
      this.isCarrierSvcInvalid = this.carrierServiceList.some(el => el.content !== evt);
    }
  }

  async onSaveLevelOfService() {
      if (this.isCarrierSvcInvalid) {
        return;
      }
      let orderLines = []
      if (this.isOrderLine) {
        this.orderDetails.DocumentType = this.orderDetails.Order[0].DocumentType;
        this.orderDetails.EnterpriseCode = this.orderDetails.Order[0].EnterpriseCode;
        orderLines = [
          {
            OrderLineKey: this.orderDetails.OrderLineKey,
            ShipNode: ' ',
            DeliveryMethod: 'SHP',
            Notes: this.orderDetails.Notes.NumberOfNotes > 0 ? this.orderDetails.Notes : undefined,
          }
        ];
      } else {
        orderLines = this.groupElement?.value.OrderLines.OrderLine.map(i =>
          ({
            OrderLineKey: i.OrderLineKey
          })
        );
      }
      const input: any = {
          orderHeaderKey: this.orderDetails.OrderHeaderKey,
          isLargeOrder: this.isLargeOrder,
          docType: this.orderDetails.DocumentType,
          orgCode: this.orderDetails.EnterpriseCode,
          //carrierServiceCode: this.carrierServiceList?.find(el => el.selected)?.CarrierServiceCode || '',
          orderLines
      };

      const selectedCarrierService = this.carrierServiceList?.find(el => el.selected);
      if(selectedCarrierService){
        input['carrierServiceCode'] = selectedCarrierService.CarrierServiceCode;
        input['deliveryStartDate'] = selectedCarrierService.DeliveryStartDate;
        input['deliveryEndDate'] = selectedCarrierService.DeliveryEndDate;
      }
      await this.orderCommonService.saveLevelOfServiceForSelectedGroup(input)
        .then(mashupOutput => {
          if (mashupOutput) {
            this.ccNotificationService.notify({
              type: 'success',
              title: this.nslMap['ORDER_SUMMARY.FULFILLMENT_GROUPS.MSG_LEVEL_OF_SERVICE_SUCCESS'],
              message: ''
            });
            this.levelOfServiceModal.instance.closeModal();
            // Refresh order summary
            this.parentPageRefresh.emit({tabName : 'fulfillment-group-tab', selectedFulfillmentGrp : this.selectedFulfillmentGroup});
          }
        }, mashupError => {
          this.showLevelOfSvcNotificationErr = true;
          this.levelOfSvcNotificationErrNotifObj = {
            type: 'error',
            title: mashupError,
            showClose: true,
            lowContrast: true
          };
        });
  }

  async reshipShipment() {
    if (this.groupElement.isShipment && this.groupElement.value.Shipment?.Status?.Status >= 1400) {
      this.actionProcessorService.dispatch<ActionParams>(Constants.RESHIP_ORDERLINE, {
        component: this.componentId,
        data: {
          modalText: '',
          modalData: {
            orderDetails: this.orderDetails,
            documentType: this.groupElement.value.Shipment?.DocumentType,
            hasMultipleOrderInShipment: this.groupElement.value.Shipment?.DisplayOrderNo?.includes('|'),
            isShipment: this.groupElement.value.Shipment?.ShipmentKey,
            size: 'lg',
            callback: () => {
              this.parentPageRefresh.emit({tabName : 'fulfillment-group-tab', selectedFulfillmentGrp : this.selectedFulfillmentGroup});
            }
          }
        }
      });
    }
  }

  openTrackingModal() {
    this.modalService.destroy();
    this.modalService.create({
        component: TrackingNumberModalComponent,
        inputs: {
          shipmentDetails: {
                size: 'lg',
                shipmentData: this.groupElement.value.Shipment
            }
        }
    });
  }

  checkIsArray(trackings){
    return Array.isArray(trackings) ? trackings.length > 0 : false;
  }

  openChangePickupRecipientModal(){
    const status = (this.groupElement.isShipment || this.groupElement.value.Shipment?.Status?.Status.startsWith('3700'));
    if ( !status ) {
      this.orderCommonService.openRestoreConfirmModal(this.orderDetails, async () => {
        this.modalService.destroy();
        this.modalService.create({
         component: ChangePickupRecipientModalComponent,
          inputs: {
            modalData: {
                size: 'sm',
                orderHeaderKey: this.orderDetails.OrderHeaderKey,
                selectedGroup: this.groupElement.value,
                isShipment: this.groupElement.isShipment,
                shipmentDetails: this.groupElement.value.Shipment,
                isOrderLine: this.isOrderLine,
                successCallback: (fulFillmentGroup) => {
                    this.parentPageRefresh.emit({tabName : 'fulfillment-group-tab', selectedFulfillmentGrp : fulFillmentGroup});
                }
            }
          }
       });
      })
    }
  }

  openChangeShippingOptionModal(){
    if (!this.disableShippingOption) {
    this.orderCommonService.openRestoreConfirmModal(this.orderDetails, async () => {
      this.modalService.destroy();
      this.modalService.create({
          component: ChangeShippingOptionModalComponent,
          inputs: {
            orderData: {
                  size: 'md',
                  orderDetails: this.orderDetails,
                  fulfillmentOrderDetails: this.fulfillmentOrderDetails,
                  defaultRuleOptimizationObj: this.defaultRuleOptimizationObj,
                  StampFirstPromiseDate: this.isStampFirstPromiseDate ? 'Y' : 'N',
                  successCallback: () => {
                    this.parentPageRefresh.emit({tabName : 'fulfillment-group-tab', selectedFulfillmentGrp : this.selectedFulfillmentGroup});
                }
              }
          }
      });
      })
    }
  }

  async openPickupAppointmentModal() {
    let earliestShipDate = "", reqShipDate = "";
    if (this.isOrderLine) {
      this.orderDetails.EnterpriseCode = this.orderDetails.Order[0].EnterpriseCode;
      earliestShipDate = this.orderDetails.EarliestShipDate;
      reqShipDate = this.orderDetails.ReqShipDate;
    } else {
      earliestShipDate = this.groupElement.value?.OrderLines?.OrderLine[0]?.EarliestShipDate;
      reqShipDate = this.groupElement.value?.OrderLines?.OrderLine[0]?.ReqShipDate;
    }
    // TODO: Display only date picker modal if shipNode not allowed slot booking
    try {
      const appResp = await this.pickupSlotAppointmentService.getPickupCalenderSlots(this.groupElement.value.Shipnode.ShipNode, this.orderDetails.EnterpriseCode);
      const slotList = appResp.Calendar.EffectivePeriods.EffectivePeriod[0].Shifts.Shift;
      this.orderCommonService.openRestoreConfirmModal(this.orderDetails, () => {
        this.actionProcessorService.dispatch<ActionParams>(Constants.PICKUP_SLOT_APPOINTMENT, {
          component: this.componentId,
          data: {
            modalText: '',
            modalData: {
              orderHeaderKey: this.orderDetails.OrderHeaderKey,
              isLargeOrder: this.isLargeOrder,
              enterpriseCode: this.orderDetails.EnterpriseCode,
              selectedGroup: this.groupElement.value,
              orgCode: this.groupElement.value.Shipnode.ShipNode,
              timeSlotList: slotList,
              earliestAvailableDate: earliestShipDate,
              selectedAppointmentDate: reqShipDate,
              saveWithinModal: true,
              isOrderLine: this.isOrderLine,
              StampFirstPromiseDate: this.isStampFirstPromiseDate ? 'Y' : 'N'
            }
          }
        });
      });
    } catch (err) {
      this.orderCommonService.openRestoreConfirmModal(this.orderDetails, () => {
        this.actionProcessorService.dispatch<any>(Constants.PICKUP_APPOINTMENT, {
          component: this.componentId,
          data: {
            modalText: '',
            modalData: {
              orderHeaderKey: this.orderDetails.OrderHeaderKey,
              isLargeOrder: this.isLargeOrder,
              enterpriseCode: this.orderDetails.EnterpriseCode,
              selectedGroup: this.groupElement.value,
              earliestAvailableDate: earliestShipDate,
              selectedAppointmentDate: reqShipDate,
              isOrderLine: this.isOrderLine,
              StampFirstPromiseDate: this.isStampFirstPromiseDate ? 'Y' : 'N'
            }
          }
        });
      });
    }
  }

  ngOnDestroy(){
    this.subscriptions.forEach((subs: Subscription) => {
      subs.unsubscribe();
    });
  }

  //EOMS-6178, 13713 - Changes Start
  getLineType() {
    if (
		this.orderDetails.OrderType === 'MP' || 
		this.orderDetails?.EnteredBy == 'GLOBALE' || 
		(this.orderDetails?.EntryType === 'Call Center' && this.orderDetails?.DocumentType === '0001' && !['REFUND', 'EXCHANGE'].includes(this.orderDetails?.OrderPurpose))
    ) {
      this.reShipAllowed = false;
    }
  }
  //EOMS-6178, 13713 - Changes End

}
