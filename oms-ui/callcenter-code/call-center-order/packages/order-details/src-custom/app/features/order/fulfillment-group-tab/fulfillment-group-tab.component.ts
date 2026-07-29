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

import { Component, OnInit, Input, Output, EventEmitter, SimpleChanges } from '@angular/core';
import { BucCommOmsRestAPIService, BucSvcAngularStaticAppInfoFacadeUtil, CallCenterNavigationService } from '@buc/svc-angular';
// import { OrderCommonService } from '../../data-service/order-common.service';
import { OrderCommonService } from '@call-center/order-shared';
import { TranslateService } from '@ngx-translate/core';
// import { ManageInstructionModalService } from '../manage-instruction-modal/manage-instruction-modal.service';
import { ManageInstructionModalService } from '@call-center/order-shared/lib/components/manage-instruction-modal/manage-instruction-modal.service';
import { ActionProcessorService, fmtDate } from '@buc/common-components';
// import { TrackOrderConfig, ShipmentType, Constants, DEL_METHOD } from '../../common/order.constants';
import { TrackOrderConfig, ShipmentType,Constants,DEL_METHOD } from '@call-center/order-shared';
// import { IsModificationAllowed } from '../../utils/order-utils';
import { IsModificationAllowed } from '@call-center/order-shared';
// import { getUniqueLineInstructions, getUniquegroupInstructions, getUniqueInstructionsForShipment } from '../../utils/instruction-utils';
import { getUniqueLineInstructions, getUniquegroupInstructions, getUniqueInstructionsForShipment } from '@call-center/order-shared/lib/utils/instruction-utils';
import moment from 'moment';
import { cloneDeep } from 'lodash';
// import { SharedExtensionConstants } from '../../shared-extension.constants';
import { SharedExtensionConstants } from '@call-center/order-shared';
// import { ActionParams } from '../../actions/paramtypes/paramtypes';
import { ActionParams } from '@call-center/order-shared';

@Component({
  // EOMS-6178 Update this selector value & the import statements
  selector: 'call-center-fulfillment-group-tab[extn]',
  templateUrl: './fulfillment-group-tab.component.html',
  styleUrls: ['./fulfillment-group-tab.component.scss'],
  providers: [ManageInstructionModalService]
})
export class ExtnFulfillmentGroupTabComponent implements OnInit {
  EXTENSION = {
    TOP: SharedExtensionConstants.FULFILLMENT_GROUP_TAB_RS_TOP,
    BOTTOM: SharedExtensionConstants.FULFILLMENT_GROUP_TAB_RS_BOTTOM
  };

  componentId = 'fulfillment-group-tab';

  @Input() orderDetails;
  @Input() fulfillmentGroupList;
  @Input() showActionPanel = true; // to show/hide action panel on the right
  @Input() fulfillmentOrderDetails;
  @Input() selectedFulfillmentGrp;
  @Input() isLargeOrder;
  @Input() bopisRuleValue;
  @Input() resourceIdsForOrderDetailsActions;
  @Input() isTransferOrder = false;

  @Output() parentPageRefresh = new EventEmitter<any>();
  @Output() navigateToChangeFulfillment = new EventEmitter<any>();


  public nslMap = {
    'FULFILLMENT.SHIPMENT_DETAILS.LABEL_ORDER_FULFILLED_IN': '',
    'FULFILLMENT.TRACK_SHIPMENT.ORDER_PLACED': '',
    'FULFILLMENT.TRACK_SHIPMENT.AWAITING_ROUTING': '',
    'FULFILLMENT.TRACK_SHIPMENT.SHIPMENT_CREATED': '',
    'FULFILLMENT.TRACK_SHIPMENT.READY_FOR_PICKING': '',
    'FULFILLMENT.TRACK_SHIPMENT.IN_PROGRESS': '',
    'FULFILLMENT.TRACK_SHIPMENT.PICKING_IN_PROGRESS': '',
    'FULFILLMENT.TRACK_SHIPMENT.READY_FOR_PACKING': '',
    'FULFILLMENT.TRACK_SHIPMENT.PACKING_IN_PROGRESS': '',
    'FULFILLMENT.TRACK_SHIPMENT.PACKED': '',
    'FULFILLMENT.TRACK_SHIPMENT.SHIPPING': '',
    'FULFILLMENT.TRACK_SHIPMENT.SHIPPED_FROM_STORE': '',
    'FULFILLMENT.TRACK_SHIPMENT.SHIPPED_FROM_WAREHOUSE': '',
    'FULFILLMENT.TRACK_SHIPMENT.SHIPPED_FROM_CUSTOM': '',
    'FULFILLMENT.TRACK_SHIPMENT.READY_FOR_CUSTOMER_PICKUP': '',
    'FULFILLMENT.TRACK_SHIPMENT.PICKUP_COMPLETE': '',
    'FULFILLMENT.TRACK_SHIPMENT.PICKED_UP_BY_CUSTOMER': '',
    'FULFILLMENT.TRACK_SHIPMENT.SHIPMENT_CANCELLED': '',
    'FULFILLMENT.TRACK_SHIPMENT.CARRIER': '',
    'FULFILLMENT.SHIPMENT_DETAILS.LABEL_AND': '',
    'FULFILLMENT.SHIPMENT_DETAILS.LABEL_PRODUCT': '',
    'FULFILLMENT.SHIPMENT_DETAILS.LABEL_PRODUCTS': '',
    'ORDER_SUMMARY.FULFILLMENT_GROUPS.TOOLTIPS_CHANGE_FULFILLMENT.TEXT_HISTORY_ORD_NOT_ALLOWED': '',
    'ORDER_SUMMARY.FULFILLMENT_GROUPS.TOOLTIPS_CHANGE_FULFILLMENT.TEXT_AFTER_RELEASED_NOT_ALLOWED': '',
    'ORDER_SUMMARY.FULFILLMENT_GROUPS.TOOLTIPS_CHANGE_FULFILLMENT.TEXT_BOPIS_N_NOT_ALLOWED': '',
    'ORDER_SUMMARY.FULFILLMENT_GROUPS.TOOLTIPS_CHANGE_FULFILLMENT.TEXT_MOD_DEL_METHOD_NOT_ALLOWED': '',
    'ORDER_SUMMARY.FULFILLMENT_GROUPS.TOOLTIPS_CHANGE_FULFILLMENT.TEXT_SHIPPING_NOT_ALLOWED': '',
    'ORDER_SUMMARY.FULFILLMENT_GROUPS.TOOLTIPS_CHANGE_FULFILLMENT.TEXT_PICKUP_NOT_ALLOWED': '',
    'FULFILLMENT.SHIPMENT_DETAILS.WARNING_UNAVAILABLE_LINES': '',
    'FULFILLMENT.SHIPMENT_DETAILS.LINKS.LABEL_CHANGE_FULFILLMENT_METHOD': '',
    'FULFILLMENT.SHIPMENT_DETAILS.WARNING_ALL_UNAVAILABLE_LINES': ''
  };

  orderHeaderKey: string;
  orderFulfilledHeading: string;
  isLoader = false;

  // Added for Orderline support
  orderLineKey = '';
  isOrderLine = false;
  fulfillmentGroups = [];
  dropdownShipToDisplaySuffixCounter = 0; // Suffix for dropdown label for ffms w/o shipment number
  maxOrderStatus: any;
  instructionTypeList = [];
  isDisableChangeShippingAddr = false;
  fulfillmentMethodType: any;
  emptyBlockMessage: string = '';
  hasUnavailableLines: boolean;
  unavailableNotification;

  constructor(
    private orderCommonService: OrderCommonService,
    public translate: TranslateService,
    public bucCommOmsRestAPIService: BucCommOmsRestAPIService,
    private manageInstructionModalService: ManageInstructionModalService,
    private ccNavigationSvc: CallCenterNavigationService,
    public actionProcessorService: ActionProcessorService,
  ) {
  }

  ngOnInit(): void {
    this.initialize();
  }

  async initialize(): Promise<any> {
    this.isLoader = true;
    if(!this.isTransferOrder) {
      await this.getInstructionCodeTypesList();
    }
    await this._initTranslations();
    this.orderLineKey = this.orderDetails?.OrderLineKey || '';
    this.isOrderLine = this.orderLineKey !== '';
    this.fulfillmentMethodType = ShipmentType;
    this.fetchMaxOrderStatus();
    await this.prepareFulfillmentDetails();
  }

  protected async _initTranslations(): Promise<any> {
    const keys = Object.keys(this.nslMap);
    const json = await this.translate.get(keys).toPromise();
    keys.forEach(k => this.nslMap[k] = json[k]);
  }

  private async _getNls(key, params?): Promise<any> {
    return this.translate.get(key, params).toPromise();
  }

  async getInstructionCodeTypesList() {
    await this.manageInstructionModalService.getCommonCodeListForInstructions(this.orderDetails)
      .then(mashupOutput => {
        this.instructionTypeList = mashupOutput.CommonCodeList?.CommonCode || [];
      });
  }

  private fetchMaxOrderStatus() {
    this.maxOrderStatus = this.orderDetails?.MaxOrderStatus;
    if (this.maxOrderStatus) {
      const idx = this.maxOrderStatus.indexOf('.');
      if (idx !== -1) {
        this.maxOrderStatus = this.maxOrderStatus.substring(0, idx);
      };
    }
  }


  async getFulfillmentDetails(selectedGroup) {
    if (!selectedGroup.expanded) {
      if (selectedGroup.isShipment) {
        selectedGroup.value.Shipment.ExpectedDeliveryDate = selectedGroup.value.Shipment.ExpectedDeliveryDate;
        selectedGroup.value.Shipment.ExpectedShipmentDate = selectedGroup.value.Shipment.ExpectedShipmentDate;
        const curNodeType = selectedGroup.value.Shipment?.ShipNode?.NodeType;
        let curNodeDescription;
        if (!Constants.DEFAULT_NODE_TYPES.includes(curNodeType)) {
          const nodeList = await this.orderCommonService.getNodeTypeList(curNodeType);
          const target = nodeList?.NodeTypes?.NodeType?.find(n => n.NodeTypeID === curNodeType);
          if (target) {
            curNodeDescription = target?.NodeTypeDescription;
          }
        }
        if (this.isOrderLine) {
          const sl = selectedGroup.value.Shipment?.ShipmentLines?.ShipmentLine?.find(l => l?.OrderLine?.OrderLineKey === this.orderLineKey);
          if (sl) {
            selectedGroup.qtyInShipment = sl.Quantity;
          }
        }
      } else {
        selectedGroup.value.Shipment = {};
        selectedGroup.value.currency = this.fulfillmentGroupList.currency;
        selectedGroup.expectedDate = selectedGroup.value.ExpectedDeliveryEndDate;
        if (this.isOrderLine) {
          selectedGroup.qtyInShipment = selectedGroup.value.Quantity;
        }
      }
    }
    selectedGroup.expanded = !selectedGroup.expanded;
  }

   prepareFulfillmentDetails() {
    if (this.fulfillmentGroupList?.groupsData.length) {
      this.fulfillmentGroups = this.fulfillmentGroupList.groupsData.map(ele => ({
        content: this.getShipmentContent(ele),
        address: this.getFulfillmentGroupAddress(ele),
        productCountLabel: this.getProductCountLabel(ele),
        hasReshipLine: this.hasReshipLine(ele),
        value: ele,
        method: ele.DeliveryMethod,
        isShipment: ele.Shipment?.ShipmentKey ? true : false,
        status: ele.Shipment?.Status?.Status,
        expanded: false,
        itemsExpanded: false,
        maxOrderStatus: this.maxOrderStatus,
        fulfillmentMethod: ShipmentType[ele.DeliveryMethod],
        storeAddressObj: this.getStoreAddress(ele),
        pickupAppointment: this.getPickupAppointment(ele)
      }));
    }
    this.setOrderFulfilledHeading(this.fulfillmentGroups);
    if (this.fulfillmentGroups.length === 1 || this.selectedFulfillmentGrp?.fulFillmentGroupId && this.selectedFulfillmentGrp?.selectionKey) {
      this.setSelectedGroup(this.selectedFulfillmentGrp);
    }

    if (this.fulfillmentGroups.length === 0) {
      if (this.fulfillmentGroupList?.hasUnavailableLines) {
        this.hasUnavailableLines = true;
        this.setUnavailableNotification(this.nslMap['FULFILLMENT.SHIPMENT_DETAILS.WARNING_ALL_UNAVAILABLE_LINES']);
      } else if (this.fulfillmentOrderDetails?.Error) {
        this.emptyBlockMessage = this.fulfillmentOrderDetails.Error;
      } else if (this.fulfillmentOrderDetails?.NoShipmentsFound === 'Y') {
        this.emptyBlockMessage = 'FULFILLMENT.SHIPMENT_DETAILS.LABEL_UNAVAILABLE_SHIPMENTS_MESSAGE';
      } else {
        this.emptyBlockMessage = 'FULFILLMENT.SHIPMENT_DETAILS.LABEL_UNAVAILABLE_LINES_MESSAGE';
      }
    } else if (this.fulfillmentGroupList?.hasUnavailableLines) {
      this.hasUnavailableLines = true;
      this.setUnavailableNotification(this.nslMap['FULFILLMENT.SHIPMENT_DETAILS.WARNING_UNAVAILABLE_LINES']);
    }
    this.isLoader = false;
  }

  getPickupAppointment(ele) {
    if (ele.Shipment?.ShipmentKey) {
      if (ele.DeliveryMethod == 'PICK' && ele.Shipment.Status.Status >= '1400') {
        return ele.Shipment.ActualShipmentDate ? ele.Shipment.ActualShipmentDate : '';
      } else {
        return ele.Shipment.ExpectedShipmentDate;
      }
    } else {
      if (this.isOrderLine) {
        return ele.ReqShipDate ? ele.ReqShipDate : ele.EarliestShipDate;
      } else {
        return ele.OrderLines?.OrderLine[0]?.ReqShipDate ? ele.OrderLines?.OrderLine[0]?.ReqShipDate : (ele.OrderLines?.OrderLine[0]?.EarliestShipDate ? ele.OrderLines?.OrderLine[0]?.EarliestShipDate : ele.ExpectedDeliveryStartDate);
      }
    }
  }

  setUnavailableNotification(title) {
    this.unavailableNotification = {
      type: 'warning',
      title,
      showClose: false,
      lowContrast: true,
      ...BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions?.CHANGE_FULFILLMENT_METHOD) ? {
        actions: [{
          text: this.nslMap['FULFILLMENT.SHIPMENT_DETAILS.LINKS.LABEL_CHANGE_FULFILLMENT_METHOD'],
          click: this.goToChangeFulfillmentOptions.bind(this)
        }]
      } : {}
    }
  }

  goToChangeFulfillmentOptions() {
    this.actionProcessorService.dispatch<ActionParams>(Constants.CHANGE_ORDER_DETAILS, {
      component: this.componentId,
      data: {
        orderHeaderKey: this.orderDetails.OrderHeaderKey,
        orderNo: this.orderDetails.OrderNo,
        sellerEnterpriseCode: this.orderDetails.EnterpriseCode
      }
    });
  }

  getStoreAddress(element: any) {
    let storeAddress = {};
    if (element.Shipnode) {
      storeAddress = { ...element?.Shipnode?.ShipNodePersonInfo, FirstName: element?.Shipnode?.Description, LastName: '' }
    } else if (element.ShipNode) {
      storeAddress = { ...element?.ShipNode?.ShipNodePersonInfo, FirstName: element?.ShipNode?.Description, LastName: '' }
    }
    return storeAddress;
  }

  async setOrderFulfilledHeading(shipments) {
    const shipmentCount = {
      Delivery: 0,
      Shipping: 0,
      Pickup: 0
    };
    let shipmentParts = '';
    shipments.forEach(ele => {
      shipmentCount[ShipmentType[ele.method]] += 1;
    });
    for (const key in shipmentCount) {
      if (shipmentCount[key] > 0) {
        if (shipmentParts) {
          shipmentParts += ' ' + this.nslMap['FULFILLMENT.SHIPMENT_DETAILS.LABEL_AND'] + ' ' + key + ' (' + shipmentCount[key] + ').';
        }
        else {
          shipmentParts = key + ' (' + shipmentCount[key] + ')';
        }
      }
    }
    this.orderFulfilledHeading = await this._getNls('FULFILLMENT.SHIPMENT_DETAILS.LABEL_ORDER_FULFILLED_IN',
      { count: shipments.length }) + shipmentParts;
  }

  getShipmentContent(ele) {
    let content = '';
    if (ele.Shipment?.ShipmentKey) {
      content = ShipmentType[ele.DeliveryMethod] + ' ' + ele.Shipment.ShipmentNo + ' - ';
    } else {
      content = ShipmentType[ele.DeliveryMethod] + ' - ';
    }
    return content;
  }

  getFulfillmentGroupAddress(ele) {
    let address = '';
    switch (ele.DeliveryMethod) {
      case 'SHP':
        address = this.getCombinedAddress(ele.PersonInfoShipTo);
        break;
      case 'PICK':
      case 'CARRY':
        if (ele.Shipnode) {
          address = ele.Shipnode.Description + ', ' + this.getCombinedAddress(ele.Shipnode.ShipNodePersonInfo);
        } else if (ele.ShipNode) {
          address = ele.ShipNode.Description + ', ' + this.getCombinedAddress(ele.ShipNode.ShipNodePersonInfo);
        }
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
    if (!this.isOrderLine) {
      let productCountLabel = "", orderLines = [];
      if (ele.Shipment?.ShipmentKey) {
        orderLines = ele.Shipment?.DocumentType === '0001' ? ele.Shipment?.ShipmentLines?.ShipmentLine.filter(obj => {
          return obj.OrderHeaderKey === this.orderDetails.OrderHeaderKey
        }) : ele.Shipment?.ShipmentLines?.ShipmentLine;

      } else {
        orderLines = ele.OrderLines.OrderLine;
      }
      productCountLabel = orderLines.length + ' ' + (orderLines.length > 1 ? this.nslMap['FULFILLMENT.SHIPMENT_DETAILS.LABEL_PRODUCTS'] :
        this.nslMap['FULFILLMENT.SHIPMENT_DETAILS.LABEL_PRODUCT']);
      return productCountLabel;
    }
  }

  hasReshipLine(ele) {
    if (!this.isOrderLine) {
      let hasReshipLine = false, orderLines = [];
      if (ele.Shipment?.ShipmentKey) {
        ele.Shipment?.ShipmentLines?.ShipmentLine.forEach(orderln => {
          orderLines.push(orderln.OrderLine);
        });

      } else {
        orderLines = ele.OrderLines.OrderLine;
      }
      hasReshipLine = orderLines.some(i => i.ReshipParentLineKey)
      return hasReshipLine;
    }
  }

  setSelectedGroup(fulfillmentGrp) {
    if (this.fulfillmentGroups.length === 1) {
      this.getFulfillmentDetails(this.fulfillmentGroups[0]);
    } else {
      const selectedGroup = this.fulfillmentGroups.filter(ele => {
        return ele.value[fulfillmentGrp.selectionKey] === fulfillmentGrp.fulFillmentGroupId;
      });
      this.getFulfillmentDetails(selectedGroup[0]);
      this.selectedFulfillmentGrp = null;
    }
  }

  getInstructions(instructions) {
    const arr = [];
    let str = '';
    instructions.forEach(obj => {
      this.instructionTypeList.forEach(ele => {
        if (ele.CodeValue === obj.InstructionType) {
          obj.InstructionType = ele.CodeShortDescription
        }
      })
      str = obj.InstructionType + ' - ' + obj.InstructionText;
      arr.push(str);
    });
    return arr;
  }

  getExpectedDate() {
    if (this.orderDetails.OrderDates?.OrderDate) {
      let date;
      this.orderDetails.OrderDates.OrderDate.forEach(obj => {
        if (obj.DateTypeId === 'MAX_DELIVERY') {
          date = fmtDate(obj.ExpectedDate, Constants.LONG_DATETIME_FORMAT);
        }
      });
      return date;
    }
  }

}
