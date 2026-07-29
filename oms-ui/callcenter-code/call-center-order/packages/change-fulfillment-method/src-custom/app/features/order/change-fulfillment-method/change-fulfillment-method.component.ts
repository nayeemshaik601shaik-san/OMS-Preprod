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

import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewChild,
  ViewContainerRef,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
  ActionParams,
  BreadcrumbService,
  CommonCodes,
  Constants,
  getPathFromRoot,
  IsModificationAllowed,
  ModifyCustomerAddressParams,
  OrderCommonService,
  ShipmentType,
} from '@call-center/order-shared';
import { ActivatedRoute } from '@angular/router';
import {
  ActionProcessorService,
  BucDateTimeHelper,
  CCNotificationService,
  DisplayRulesHelperService,
  fmtDate,
  getCurrentLocale,
  getCurrentLocaleDateFormat,
  getFlatPickrDateFormat,
  getMoment,
  PickupSlotAppointmentService,
} from '@buc/common-components';
import { ChangeFulfillmentMethodService } from '../data-service/change-fulfillment-method.service';
import { Subscription } from 'rxjs';
import { ChangeFulfillmentLinesTableComponent } from './change-fulfillment-lines-table/change-fulfillment-lines-table.component';
import {
  BucSvcAngularStaticAppInfoFacadeUtil,
  CallCenterNavigationService,
} from '@buc/svc-angular';
import moment from 'moment';
import { ExtensionConstants } from '../../extension.constants';

@Component({
  selector: 'call-center-change-fulfillment-method',
  templateUrl: './change-fulfillment-method.component.html',
  styleUrls: ['./change-fulfillment-method.component.scss'],
})
export class ChangeFulfillmentMethodComponent implements OnInit, OnDestroy {
  EXTENSION = {
    TOP: ExtensionConstants.CHANGE_FULTILLMENT_METHOD_CF_TOP,
    BOTTOM: ExtensionConstants.CHANGE_FULTILLMENT_METHOD_CF_BOTTOM,
  };

  isScreenInitialized = false;
  accordionContent: any;
  enableSave = false;
  commentsValue = '';
  expanded = false;

  detailsChanged = {
    isShippingDetailsChanged: false,
    isPickupDetailsChanged: false,
  };

  protected readonly nlsMap = {
    'CHANGE_FULFILLMENT_METHOD.BREADCRUMB.CHANGE_FULFILLMENT_METHOD': '',
    'CHANGE_FULFILLMENT_METHOD.SELECT_FULFILLMENT_METHOD.RADIO_OPTIONS.LABEL_SHIPPING':
      '',
    'CHANGE_FULFILLMENT_METHOD.SELECT_FULFILLMENT_METHOD.RADIO_OPTIONS.LABEL_PICKUP':
      '',
    'CHANGE_FULFILLMENT_METHOD.SELECT_FULFILLMENT_METHOD.LABEL_SELECT_FULFILLMENT_METHOD':
      '',
    'CHANGE_FULFILLMENT_METHOD.SHIPPING_DETAILS.LABEL_SHIPPING_DETAILS': '',
    'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.LABEL_PICKUP_DETAILS': '',
    'CHANGE_FULFILLMENT_METHOD.DELIVERY_DETAILS.LABEL_DELIVERY_DETAILS': '',
    'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.LABEL_PICKUP_BY': '',
    'CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.BUTTONS.SHOW_LINES': '',
    'CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_NOT_ALLOWED_SHIP': '',
    'CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_NOT_ALLOWED_PICK': '',
    'CHANGE_FULFILLMENT_METHOD.SHIPPING_ADDRESS.LABEL_ADDRESS_DESCRIPTION': '',
    'CHANGE_FULFILLMENT_METHOD.SHIPPING_ADDRESS.MSG_SUCCESS_SHIPMENTADDRESS_UPDATE':
      '',
    'CHANGE_FULFILLMENT_METHOD.SHIPPING_ADDRESS.LABEL_MODIFY_SHIPPING_ADDRESS':
      '',
    'CHANGE_FULFILLMENT_METHOD.MESSAGES.CARRIER_SERVICE_OPTIONS.NOT_CONFIGURED':
      '',
  };

  public breadCrumbList: any[];

  public componentId = 'ChangeFulfillmentMethodComponent';
  orderHeaderKey: any;
  selectFulfillmentOptions: any;

  @ViewChild('radioLabelTpl', { static: true }) radioLabelTpl: TemplateRef<any>;

  @ViewChild(ChangeFulfillmentLinesTableComponent)
  private changeFulfillmentLinesTable: ChangeFulfillmentLinesTableComponent;

  accordionFlags = {
    showShippingDetails: false,
    showDeliveryDetails: false,
    showPickupDetails: false,
  };

  shipmentItems: any;
  pickupItems: any;

  loadedOrderLines: any;
  fulfillmentGroupId: any;
  isLargeOrder: any;
  fulfillmentSummaryDetails: any;
  selectedFulfillmentGroup: any;
  initialDeliveryMethod: any;
  orderLineKeys: any;
  selectedLinesFromOrderDetails = [];
  actionSub: Subscription;
  unavailableLinesNotificationObjForShip: any;
  someLinesNotAllowedNotificationObj: any;
  numberOfSelectedUnavailableLinesForShip: any;
  showNotificationForShipping = true;
  unavailableLinesNotificationObjForPick: any;
  numberOfSelectedUnavailableLinesForPick: any;
  showNotificationForPickup = true;
  pickupDetails = {
    selectedStore: null,
    pickupBy: {
      isPickedByCustomer: false,
      pickedByInfo: null,
    },
    appointmentDate: null,
    isDateInvalid: false,
    dateInvalidText: null,
  };
  carrierServiceList: any;
  pageTitle = '';
  tableTitle = '';
  initialCarrierServiceCode: any;
  selectedItemsFromTable: any;
  orderlinesListTableModelData: any;
  shippingAddr: any;

  i18nDatePlaceholder;
  curLocale;
  flatpickrDateFormat;
  carrierSvcReadOnlyValue: any;
  reasonCodeListForNotes: any;
  isCarrierSvcInvalid = false;
  // TODO: API dependency - check for calendar rule and set value based on it
  isSlotBasedAppointmentAllowed = false;
  openShippingAddrModal: boolean;
  openStoreAddrModal: boolean;
  selectedCarrierSvcCode: any;
  initialAddrParams: {
    openShippingAddrModal: boolean;
    openStoreAddrModal: boolean;
  };
  maxOrderStatus: any;
  pickupTimeSlotList;
  previousPath: any;
  enterpriseCode: any;
  private viewContainerRef: ViewContainerRef;

  constructor(
    private translateService: TranslateService,
    private activatedRoute: ActivatedRoute,
    private bcSvc: BreadcrumbService,
    public orderCommonService: OrderCommonService,
    public changeFulfillmentMethodService: ChangeFulfillmentMethodService,
    private actionProcessorService: ActionProcessorService,
    private ref: ElementRef,
    private ccNavigationSvc: CallCenterNavigationService,
    public ccNotificationService: CCNotificationService,
    private pickupSlotAppointmentService: PickupSlotAppointmentService,
    private displayRuleServices: DisplayRulesHelperService
  ) {}

  async ngOnInit() {
    this.ccNotificationService.registerViewContainerRef(this.viewContainerRef);
    this.initialize();
    this.actionSub = this.actionProcessorService
      .selectUpdate<any>(this.componentId)
      .subscribe((res) => {
        if (res.params.selectedStore) {
          this.pickupDetails.selectedStore = res.params.selectedStore;
        }
        if (res.params.pickupByDetail) {
          this.pickupDetails.pickupBy.isPickedByCustomer =
            res.params.pickupByDetail.isPickedByCustomer;
          this.pickupDetails.pickupBy.pickedByInfo =
            res.params.pickupByDetail.pickedByInfo;
        }
        if (res.params.selectedAppointmentDate) {
          this.pickupDetails.appointmentDate =
            res.params.selectedAppointmentDate;
        }
        //TODO: Save should be enabled only when unavailable lines are unselected
        if (!this.pickupDetails.isDateInvalid) {
          this.enableSave = true;
          this.detailsChanged.isPickupDetailsChanged = true;
        }
        this.initPickupDetails();
      });
  }

  setEnterpriseForDisplayRule() {
    this.enterpriseCode = this.fulfillmentSummaryDetails.Order.EnterpriseCode;
    this.displayRuleServices.setEnterprise(this.enterpriseCode);
  }

  ngOnDestroy() {
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
  }

  async initialize(): Promise<any> {
    await this._initTranslations();
    await this.getOrderDataFromParams();
    await this.prepareBreadcrumbList();
    this.i18nDatePlaceholder = getCurrentLocaleDateFormat();
    this.curLocale = getCurrentLocale();
    this.flatpickrDateFormat = getFlatPickrDateFormat();
    this.isScreenInitialized = true;
  }

  protected async _initTranslations(): Promise<any> {
    const keys = Object.keys(this.nlsMap);
    if (keys.length) {
      const json = await this.translateService.get(keys).toPromise();
      keys.forEach((k) => (this.nlsMap[k] = json[k]));
    }
  }

  private async _getNls(key, params?): Promise<any> {
    return this.translateService.get(key, params).toPromise();
  }

  async prepareBreadcrumbList(): Promise<any> {
    const crumb =
      this.nlsMap[
        'CHANGE_FULFILLMENT_METHOD.BREADCRUMB.CHANGE_FULFILLMENT_METHOD'
      ];
    const route = getPathFromRoot(this.activatedRoute.snapshot);
    this.bcSvc.updateLast(crumb, route, crumb, [route], {
      queryParams: this.activatedRoute.snapshot.queryParams,
    });
    this.breadCrumbList = this.bcSvc.get();
  }

  getOrderDataFromParams(): void {
    this.orderHeaderKey =
      this.activatedRoute.snapshot.queryParams.orderHeaderKey;
    this.isLargeOrder = this.activatedRoute.snapshot.queryParams.isLargeOrder;
    this.openShippingAddrModal =
      this.activatedRoute.snapshot.queryParams.openShippingAddrModal === 'true'
        ? true
        : false;
    this.openStoreAddrModal =
      this.activatedRoute.snapshot.queryParams.openStoreAddrModal === 'true'
        ? true
        : false;
    this.initialAddrParams = {
      openShippingAddrModal: this.openShippingAddrModal,
      openStoreAddrModal: this.openStoreAddrModal,
    };
    if (this.activatedRoute.snapshot.queryParams.orderLineKeys) {
      this.orderLineKeys = JSON.parse(
        this.activatedRoute.snapshot.queryParams.orderLineKeys
      );
    }
    this.previousPath = this.activatedRoute.snapshot.queryParams.isOrderLine
      ? Constants.ORDER_LINE_DETAILS_ROUTE
      : Constants.ORDER_DETAILS_ROUTE;
    if (
      this.activatedRoute.snapshot.queryParams.fulfillmentGroupId &&
      this.activatedRoute.snapshot.queryParams.fulfillmentGroupId !==
        'undefined'
    ) {
      this.fulfillmentGroupId =
        this.activatedRoute.snapshot.queryParams.fulfillmentGroupId;
    }
  }

  async getReasonCodesForNotes() {
    await this.changeFulfillmentMethodService
      .getNotesReasonCodesForNotes(
        this.fulfillmentSummaryDetails.Order.EnterpriseCode,
        this.fulfillmentSummaryDetails.Order.DocumentType,
        CommonCodes.notesReason
      )
      .then(async (mashupOutput) => {
        if (mashupOutput) {
          if (mashupOutput.CommonCodeList) {
            this.reasonCodeListForNotes =
              mashupOutput.CommonCodeList.CommonCode;
          }
        }
      });
  }

  toggleExpand(): void {
    this.expanded = !this.expanded;
  }

  initAccordion(lines) {
    this.accordionContent = [
      {
        expanded: false,
        title:
          lines && lines[0]?.DeliveryMethod === 'PICK'
            ? this.nlsMap[
                'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.LABEL_PICKUP_DETAILS'
              ]
            : lines && lines[0]?.DeliveryMethod === 'SHP'
            ? this.nlsMap[
                'CHANGE_FULFILLMENT_METHOD.SHIPPING_DETAILS.LABEL_SHIPPING_DETAILS'
              ]
            : this.nlsMap[
                'CHANGE_FULFILLMENT_METHOD.DELIVERY_DETAILS.LABEL_DELIVERY_DETAILS'
              ],
      },
    ];
  }

  async getLoadedOrderLinesFromTable(evt) {
    this.loadedOrderLines = evt;
  }

  async getFulfillmentSummaryDetails(evt) {
    this.carrierServiceList = null;
    this.fulfillmentSummaryDetails = evt;
    this.setEnterpriseForDisplayRule();
    if (
      this.fulfillmentSummaryDetails &&
      this.fulfillmentSummaryDetails.Order
    ) {
      this.maxOrderStatus = this.fulfillmentSummaryDetails.Order.MaxOrderStatus;
      const idx = this.maxOrderStatus.indexOf('.');
      if (idx !== -1) {
        this.maxOrderStatus = this.maxOrderStatus.substring(0, idx);
      }
      // Check for FulfilmentGroupID to load details for selected radio item
      if (this.isLargeOrder === 'N') {
        if (this.fulfillmentGroupId !== undefined) {
          if (
            this.fulfillmentSummaryDetails.Order.ShippingGroups ||
            this.fulfillmentSummaryDetails.Order.PickupGroups
          ) {
            if (this.fulfillmentSummaryDetails.Order.ShippingGroups) {
              if (
                !this.selectedFulfillmentGroup ||
                !this.selectedFulfillmentGroup.length
              ) {
                this.selectedFulfillmentGroup =
                  this.fulfillmentSummaryDetails.Order.ShippingGroups.ShippingGroup.filter(
                    (el) => el.FulfilmentGroupID === this.fulfillmentGroupId
                  );
                if (
                  this.selectedFulfillmentGroup &&
                  this.selectedFulfillmentGroup.length
                ) {
                  this.pageTitle = await this._getNls(
                    'CHANGE_FULFILLMENT_METHOD.PAGE_TITLE',
                    {
                      shipmentType: ShipmentType.SHP,
                      addr: this.selectedFulfillmentGroup[0]?.PersonInfoShipTo
                        .City,
                    }
                  );
                }
              }
            }
            if (this.fulfillmentSummaryDetails.Order.PickupGroups) {
              if (
                !this.selectedFulfillmentGroup ||
                !this.selectedFulfillmentGroup.length
              ) {
                this.selectedFulfillmentGroup =
                  this.fulfillmentSummaryDetails.Order.PickupGroups.PickupGroup.filter(
                    (el) => el.FulfilmentGroupID === this.fulfillmentGroupId
                  );
                this.pageTitle = await this._getNls(
                  'CHANGE_FULFILLMENT_METHOD.PAGE_TITLE',
                  {
                    shipmentType: ShipmentType.PICK,
                    addr: this.selectedFulfillmentGroup[0]?.Shipnode
                      .Description,
                  }
                );
              }
            }
            if (
              this.selectedFulfillmentGroup &&
              this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine
            ) {
              this.initialDeliveryMethod =
                this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0].DeliveryMethod;
              this.initSelectFulfillmentRadioOptions();
              this.initDetailsBasedOnRadioSelection(
                this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine
              );
            }
          }
        } else if (this.orderLineKeys && this.orderLineKeys.length === 1) {
          // For a single order line in a small order
          if (
            this.fulfillmentSummaryDetails.Order &&
            (this.fulfillmentSummaryDetails.Order.ShippingGroups ||
              this.fulfillmentSummaryDetails.Order.PickupGroups)
          ) {
            if (this.fulfillmentSummaryDetails.Order.ShippingGroups) {
              if (
                !this.selectedFulfillmentGroup ||
                !this.selectedFulfillmentGroup.length
              ) {
                this.selectedFulfillmentGroup =
                  this.fulfillmentSummaryDetails.Order.ShippingGroups.ShippingGroup.filter(
                    (group) =>
                      group.OrderLines.OrderLine.find(
                        (line) => line.OrderLineKey === this.orderLineKeys[0]
                      )
                  );
                this.fulfillmentSummaryDetails.Order.ShippingGroups.ShippingGroup.forEach(
                  (group) => {
                    const selectedLine = group.OrderLines.OrderLine.find(
                      (line) => line.OrderLineKey === this.orderLineKeys[0]
                    );
                    if (selectedLine) {
                      this.pageTitle = this.translateService.instant(
                        'CHANGE_FULFILLMENT_METHOD.PAGE_TITLE_LINE',
                        {
                          num: selectedLine?.PrimeLineNo,
                          desc: selectedLine?.Item?.ItemShortDesc || '',
                        }
                      );
                      this.selectedLinesFromOrderDetails.push(selectedLine);
                    }
                  }
                );
              }
            }
            if (this.fulfillmentSummaryDetails.Order.PickupGroups) {
              if (
                !this.selectedFulfillmentGroup ||
                !this.selectedFulfillmentGroup.length
              ) {
                this.selectedFulfillmentGroup =
                  this.fulfillmentSummaryDetails.Order.PickupGroups.PickupGroup.filter(
                    (group) =>
                      group.OrderLines.OrderLine.find(
                        (line) => line.OrderLineKey === this.orderLineKeys[0]
                      )
                  );
                this.fulfillmentSummaryDetails.Order.PickupGroups.PickupGroup.forEach(
                  (group) => {
                    const selectedLine = group.OrderLines.OrderLine.find(
                      (line) => line.OrderLineKey === this.orderLineKeys[0]
                    );
                    if (selectedLine) {
                      this.pageTitle = this.translateService.instant(
                        'CHANGE_FULFILLMENT_METHOD.PAGE_TITLE_LINE',
                        {
                          num: selectedLine?.PrimeLineNo,
                          desc: selectedLine?.Item?.ItemShortDesc || '',
                        }
                      );
                      this.selectedLinesFromOrderDetails.push(selectedLine);
                    }
                  }
                );
              }
            }
            if (this.selectedLinesFromOrderDetails[0]) {
              this.initialDeliveryMethod =
                this.selectedLinesFromOrderDetails[0].DeliveryMethod;
              this.initSelectFulfillmentRadioOptions();
              this.initDetailsBasedOnRadioSelection(
                this.selectedLinesFromOrderDetails
              );
            }
          }
        }
      } else if (
        this.isLargeOrder === 'Y' &&
        this.orderLineKeys &&
        this.orderLineKeys.length === 1
      ) {
        // For a single order line in a large order
        if (
          this.fulfillmentSummaryDetails.OrderLine &&
          this.fulfillmentSummaryDetails.OrderLine[0]
        ) {
          this.selectedLinesFromOrderDetails.push(
            this.fulfillmentSummaryDetails.OrderLine[0]
          );
        }
        if (this.selectedLinesFromOrderDetails[0]) {
          this.initialDeliveryMethod =
            this.selectedLinesFromOrderDetails[0].DeliveryMethod;
          await this.initSelectFulfillmentRadioOptions();
          this.initDetailsBasedOnRadioSelection(
            this.selectedLinesFromOrderDetails
          );
        }
      } else {
      }
    }

    this.getReasonCodesForNotes();
  }

  async initDetailsBasedOnRadioSelection(lines) {
    this.initAccordion(lines);
    if (lines && lines[0] && lines[0].DeliveryMethod) {
      this.initialDeliveryMethod = lines[0].DeliveryMethod;
      if (this.initialDeliveryMethod === 'SHP') {
        this.selectFulfillmentOptions.find((el) => el.id === 'SHP').checked =
          true;
        this.accordionFlags.showShippingDetails = true;
        this.accordionContent[0].title =
          this.nlsMap[
            'CHANGE_FULFILLMENT_METHOD.SHIPPING_DETAILS.LABEL_SHIPPING_DETAILS'
          ];
        this.tableTitle =
          this.nlsMap[
            'CHANGE_FULFILLMENT_METHOD.SELECT_FULFILLMENT_METHOD.RADIO_OPTIONS.LABEL_SHIPPING'
          ];
        if (this.openShippingAddrModal) {
          this.accordionContent[0].expanded = true;
          await this.initShippingDetails();
          this.openEditShippingAddressModal();
        } else {
          if (this.accordionContent[0].expanded) {
            this.initShippingDetails();
          }
        }
      } else if (this.initialDeliveryMethod === 'PICK') {
        this.selectFulfillmentOptions.find((el) => el.id === 'PICK').checked =
          true;
        this.accordionFlags.showPickupDetails = true;
        this.accordionContent[0].title =
          this.nlsMap[
            'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.LABEL_PICKUP_DETAILS'
          ];
        this.tableTitle =
          this.nlsMap[
            'CHANGE_FULFILLMENT_METHOD.SELECT_FULFILLMENT_METHOD.RADIO_OPTIONS.LABEL_PICKUP'
          ];
        if (this.openStoreAddrModal) {
          this.accordionContent[0].expanded = true;
          await this.initPickupDetails();
          this.openSelectStoreModal();
        } else {
          if (this.accordionContent[0].expanded) {
            this.initPickupDetails();
          }
        }
      }
    }
  }

  private initSelectFulfillmentRadioOptions() {
    this.selectFulfillmentOptions = [
      {
        id: 'SHP',
        value: 'SHP',
        template: this.radioLabelTpl,
        checked: false,
        key: 'CHANGE_FULFILLMENT_METHOD.SELECT_FULFILLMENT_METHOD.RADIO_OPTIONS.LABEL_SHIPPING',
      },
      {
        id: 'PICK',
        value: 'PICK',
        template: this.radioLabelTpl,
        checked: false,
        key: 'CHANGE_FULFILLMENT_METHOD.SELECT_FULFILLMENT_METHOD.RADIO_OPTIONS.LABEL_PICKUP',
      },
    ];
  }

  async getCarrierServiceOptionsList() {
    let orderLinesInput: any = [];
    orderLinesInput = this.loadedOrderLines.map((line) => ({
      ItemID: line.ItemDetails.ItemID,
      OrderLineKey: line.OrderLineKey,
      EarliestShipDate: line.EarliestShipDate,
      IsParcelShippingAllowed:
        line.ItemDetails.PrimaryInformation.IsParcelShippingAllowed,
      PersonInfoShipTo: this.shippingAddr,
    }));
    await this.changeFulfillmentMethodService
      .getCarrierServiceOptions(
        this.fulfillmentSummaryDetails.Order.OrderHeaderKey,
        orderLinesInput
      )
      .then((mashupOutput) => {
        if (
          mashupOutput.CarrierServiceList &&
          mashupOutput.CarrierServiceList.CarrierService?.length
        ) {
          this.carrierServiceList =
            mashupOutput.CarrierServiceList?.CarrierService;
          if (this.carrierServiceList && this.carrierServiceList.length) {
            this.carrierServiceList.forEach((el) => {
              // content value is in this format : Express (2022-09-05 - 2022-10-06)
              const content = `${el.CarrierServiceDesc} (${el.DeliveryStartDate} - ${el.DeliveryEndDate})`;
              el.content = content;
              if (!this.fulfillmentGroupId) {
                el.selected =
                  this.initialDeliveryMethod === 'SHP' &&
                  this.fulfillmentSummaryDetails &&
                  this.fulfillmentSummaryDetails.Order.ShippingGroups &&
                  this.fulfillmentSummaryDetails.Order.ShippingGroups
                    .ShippingGroup[0].CarrierServiceCode ===
                    el.CarrierServiceCode
                    ? true
                    : false;
              } else {
                el.selected =
                  this.initialDeliveryMethod === 'SHP' &&
                  this.fulfillmentSummaryDetails &&
                  this.fulfillmentSummaryDetails.Order.ShippingGroups &&
                  this.fulfillmentSummaryDetails.Order.ShippingGroups.ShippingGroup.filter(
                    (elem) => elem.FulfilmentGroupID === this.fulfillmentGroupId
                  )[0]?.CarrierServiceCode === el.CarrierServiceCode
                    ? true
                    : false;
              }
              el.id = el.value = el.CarrierServiceCode;
              if (el.selected) {
                this.initialCarrierServiceCode = el.id;
                this.selectedCarrierSvcCode = el.id;
              }
            });
            // Check CARRIER_SERVICE_CODE Modification permission
            if (
              this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
                ?.Modifications?.Modification
            ) {
              let isCarrierSvcModAllowed = IsModificationAllowed(
                this.selectedFulfillmentGroup[0].OrderLines.OrderLine[0]
                  .Modifications.Modification,
                Constants.MOD_TYPE_CARRIER_SERVICE_CODE
              );
              if (!isCarrierSvcModAllowed) {
                // If Carrier service modification is not allowed, show readonly value
                if (this.carrierServiceList && this.carrierServiceList.length) {
                  const selectedCarrierSvc = this.carrierServiceList.some(
                    (el) => el.selected
                  );
                  if (!selectedCarrierSvc) {
                    this.carrierSvcReadOnlyValue = '-';
                  } else {
                    this.carrierSvcReadOnlyValue = this.carrierServiceList.find(
                      (el) => el.selected
                    ).content;
                  }
                }
              }
            }
          }
        }
      });
  }

  getSelectedItemsFromTable(evt) {
    this.selectedItemsFromTable = evt;
    if (!this.selectedItemsFromTable.length) {
      this.enableSave = false;
    } else {
      const selectedOption = this.selectFulfillmentOptions?.find(
        (el) => el.checked
      ).id;
      if (this.initialDeliveryMethod !== selectedOption) {
        if (
          selectedOption === 'PICK' &&
          (this.pickupDetails.selectedStore ||
            this.pickupDetails.pickupBy.pickedByInfo ||
            this.pickupDetails.appointmentDate) &&
          !this.pickupDetails.isDateInvalid
        ) {
          this.enableSave = true;
        } else {
          this.enableSave = false;
        }
      } else {
        if (this.initialDeliveryMethod === 'SHP') {
          if (this.carrierServiceList) {
            if (
              this.initialCarrierServiceCode !==
              this.carrierServiceList?.find((el) => el.selected).id
            ) {
              this.enableSave = true;
            }
          }
          // Add similar condition for shipping address change (Enable save if shipping address is changed)
        } else {
          if (
            (this.pickupDetails.selectedStore ||
              this.pickupDetails.pickupBy.pickedByInfo ||
              this.pickupDetails.appointmentDate) &&
            !this.pickupDetails.isDateInvalid
          ) {
            this.enableSave = true;
          }
        }
      }
    }
  }

  getOrderLinesTableModelData(evt) {
    this.orderlinesListTableModelData = evt;
    if (
      this.orderlinesListTableModelData &&
      this.orderlinesListTableModelData.length
    ) {
      const someLinesNotAllowed = this.orderlinesListTableModelData.find(
        (row) => row.disabled
      );
      if (someLinesNotAllowed) {
        const selectedFulfillmentOption = this.selectFulfillmentOptions.find(
          (el) => el.checked
        );
        // Show error message if lines are not allowed for shipping or pickup
        this.someLinesNotAllowedNotificationObj = {
          type: 'error',
          title:
            selectedFulfillmentOption.id === 'SHP'
              ? this.nlsMap[
                  'CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_NOT_ALLOWED_SHIP'
                ]
              : this.nlsMap[
                  'CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_NOT_ALLOWED_PICK'
                ],
          showClose: true,
          lowContrast: true,
        };
      } else {
        this.someLinesNotAllowedNotificationObj = null;
      }
    }
  }

  onSelectCarrierService(evt) {
    this.isCarrierSvcInvalid = false;
    if (evt.length === 0) {
      if (this.initialCarrierServiceCode) {
        this.selectedCarrierSvcCode = '';
        this.detailsChanged.isShippingDetailsChanged = true;
        this.shipmentItems.find((el) => el.id === 'comments').showTile = true;
        this.enableSave = true;
      }
    } else {
      this.carrierServiceList.forEach(
        (el) => (el.selected = el.id === evt.item.id)
      );
      this.selectedCarrierSvcCode = evt.item.id;
      if (
        this.initialCarrierServiceCode !== evt.item.id &&
        this.selectedItemsFromTable?.length
      ) {
        this.detailsChanged.isShippingDetailsChanged = true;
        this.shipmentItems.find((el) => el.id === 'comments').showTile = true;
        this.enableSave = true;
      } else {
        this.enableSave = false;
      }
    }
  }

  onCarrierSvcValSearch(evt) {
    this.isCarrierSvcInvalid = false;
    if (evt) {
      this.isCarrierSvcInvalid = this.carrierServiceList.some(
        (el) => el.content !== evt
      );
    }
  }

  async onSelectFulfillmentRadioChange(item) {
    this.commentsValue = '';
    if (item.value === 'SHP') {
      this.selectFulfillmentOptions.forEach(
        (el) => (el.checked = el.id === 'SHP')
      );
      this.accordionFlags.showShippingDetails = true;
      this.accordionFlags.showPickupDetails = false;
      this.accordionFlags.showDeliveryDetails = false;
      if (this.initialDeliveryMethod === 'SHP') {
        this.enableSave = false;
        await this.onRefresh();
      } else {
        if (this.selectedItemsFromTable?.length) {
          this.enableSave = true;
        }
      }
      if (this.initialDeliveryMethod !== 'SHP') {
        this.accordionContent[0].expanded = true;
        this.changeFulfillmentLinesTable.checkForIsShippingAllowed();
        await this.changeFulfillmentLinesTable.checkForShippingLinesAvailability(
          this.shippingAddr
        );
      }
      this.accordionContent[0].title =
        this.nlsMap[
          'CHANGE_FULFILLMENT_METHOD.SHIPPING_DETAILS.LABEL_SHIPPING_DETAILS'
        ];
      if (this.accordionContent[0].expanded) {
        this.initShippingDetails();
      }
    } else if (item.value === 'PICK') {
      this.selectFulfillmentOptions.forEach(
        (el) => (el.checked = el.id === 'PICK')
      );
      this.accordionFlags.showPickupDetails = true;
      this.accordionFlags.showShippingDetails = false;
      this.accordionFlags.showDeliveryDetails = false;
      if (this.initialDeliveryMethod === 'PICK') {
        await this.onRefresh();
      } else {
        this.accordionContent[0].expanded = true;
        this.changeFulfillmentLinesTable.checkForIsPickupAllowed();
      }
      if (
        (this.pickupDetails.selectedStore ||
          this.pickupDetails.pickupBy.pickedByInfo ||
          this.pickupDetails.appointmentDate) &&
        !this.pickupDetails.isDateInvalid
      ) {
        this.enableSave = true;
      } else {
        this.enableSave = false;
      }
      this.accordionContent[0].title =
        this.nlsMap[
          'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.LABEL_PICKUP_DETAILS'
        ];
      if (this.accordionContent[0].expanded) {
        this.initPickupDetails();
      }
    }
  }

  private async initPickupDetails() {
    const shipNode = this.pickupDetails.selectedStore
      ? this.pickupDetails.selectedStore.ShipNode
      : this.selectedFulfillmentGroup[0]?.ShipNode;
    if (shipNode) {
      try {
        const appResp =
          await this.pickupSlotAppointmentService.getPickupCalenderSlots(
            shipNode,
            this.fulfillmentSummaryDetails.Order.EnterpriseCode
          );
        this.pickupTimeSlotList =
          appResp.Calendar.EffectivePeriods.EffectivePeriod[0].Shifts.Shift;
        this.isSlotBasedAppointmentAllowed = true;
      } catch (err) {
        this.isSlotBasedAppointmentAllowed = false;
      }
    }
    this.pickupItems = [
      {
        id: 'pickupAddress',
        showEditIcon: true,
        showTile:
          this.initialDeliveryMethod === 'PICK'
            ? true
            : this.pickupDetails.selectedStore
            ? true
            : false,
        value: this.pickupDetails.selectedStore
          ? {
              ...this.pickupDetails.selectedStore.ShipNodePersonInfo,
              FirstName: this.pickupDetails.selectedStore.Description,
              LastName: '',
            }
          : {
              ...this.selectedFulfillmentGroup[0]?.Shipnode?.ShipNodePersonInfo,
              FirstName:
                this.selectedFulfillmentGroup[0]?.Shipnode?.Description,
              LastName: '',
            },
        title: 'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.LABEL_PICKUP_ADDRESS',
        tooltipMsg:
          'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.TOOLTIP_EDIT_PICKUP_ADDRESS',
      },
      {
        id: 'pickupBy',
        showEditIcon: true,
        showTile:
          this.initialDeliveryMethod === 'PICK'
            ? true
            : this.pickupDetails.selectedStore
            ? true
            : false,
        value: this.getPickByDetails(),
        title: 'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.LABEL_PICKUP_BY',
        tooltipMsg:
          'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.TOOLTIP_EDIT_PICKUP_BY',
      },
      {
        id: 'pickupAppointment',
        showEditIcon: this.isSlotBasedAppointmentAllowed ? true : false,
        showTile:
          this.initialDeliveryMethod === 'PICK'
            ? true
            : this.pickupDetails.selectedStore
            ? true
            : false,
        value: this.getPickupDate(),
        title:
          'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.LABEL_PICKUP_APPOINTMENT',
        tooltipMsg:
          'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.TOOLTIP_EDIT_PICKUP_APPOINTMENT',
      },
      {
        id: 'comments',
        showEditIcon: false,
        showTile: false,
        value: '',
      },
    ];
    if (
      (this.initialDeliveryMethod !== 'PICK' &&
        this.pickupDetails?.selectedStore) ||
      this.detailsChanged.isPickupDetailsChanged
    ) {
      this.pickupItems.find((el) => el.id === 'comments').showTile = true;
    }

    // Check MARKFOR modification permission
    // Override modification permissions for released orders
    if (this.maxOrderStatus && this.maxOrderStatus !== '3200') {
      if (
        this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
          ?.Modifications?.Modification
      ) {
        let isPickupByAllowed = IsModificationAllowed(
          this.selectedFulfillmentGroup[0].OrderLines.OrderLine[0].Modifications
            .Modification,
          Constants.MOD_TYPE_MARKFOR
        );
        const pickupByItem = this.pickupItems.find(
          (item) => item.id === 'pickupBy'
        );
        if (isPickupByAllowed) {
          // Hide edit icon if pickup by permission is not allowed
          pickupByItem.showEditIcon = true;
        } else {
          pickupByItem.showEditIcon = false;
        }
      }
    }

    // Check SHIP NODE Modification permission
    // Override modification permissions for released orders
    if (this.maxOrderStatus && this.maxOrderStatus !== '3200') {
      let isShipNodeModAllowed;
      if (
        this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
          ?.Modifications?.Modification
      ) {
        isShipNodeModAllowed = IsModificationAllowed(
          this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
            ?.Modifications?.Modification,
          Constants.MOD_TYPE_SHIP_NODE
        );
      }
      const storeAddrItem = this.pickupItems.find(
        (el) => el.id === 'pickupAddress'
      );
      if (isShipNodeModAllowed) {
        storeAddrItem.showEditIcon = true;
      } else {
        storeAddrItem.showEditIcon = false;
      }
    }
  }

  private getPickByDetails() {
    const pickBy = {
      name: '',
      phone: '',
    };
    if (
      this.pickupDetails.pickupBy.pickedByInfo &&
      !this.pickupDetails.pickupBy.isPickedByCustomer
    ) {
      pickBy.name =
        this.pickupDetails.pickupBy.pickedByInfo.FirstName +
        ' ' +
        this.pickupDetails.pickupBy.pickedByInfo.LastName;
      pickBy.phone = this.pickupDetails.pickupBy.pickedByInfo.DayPhone;
    } else if (this.fulfillmentGroupId) {
      pickBy.name =
        (this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
          ?.PersonInfoMarkFor?.FirstName ||
          this.fulfillmentSummaryDetails.Order?.CustomerFirstName) +
        ' ' +
        (this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
          ?.PersonInfoMarkFor?.LastName ||
          this.fulfillmentSummaryDetails.Order?.CustomerLastName);
      pickBy.phone =
        this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]?.PersonInfoMarkFor?.DayPhone;
    } else {
      pickBy.name =
        (this.selectedLinesFromOrderDetails[0]?.PersonInfoMarkFor?.FirstName ||
          this.fulfillmentSummaryDetails.Order?.CustomerFirstName) +
        ' ' +
        (this.selectedLinesFromOrderDetails[0]?.PersonInfoMarkFor?.LastName ||
          this.fulfillmentSummaryDetails.Order?.CustomerLastName);
      pickBy.phone =
        this.selectedLinesFromOrderDetails[0]?.PersonInfoMarkFor?.DayPhone;
    }
    return pickBy;
  }

  private getPickupDate() {
    let date;
    const timezone = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserTimeZone();
    if (
      this.initialDeliveryMethod === 'PICK' &&
      !this.pickupDetails.appointmentDate
    ) {
      date = this.fulfillmentGroupId
        ? this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
            ?.ReqShipDate
        : this.selectedLinesFromOrderDetails[0]?.ReqShipDate;
    } else {
      date = this.pickupDetails.appointmentDate;
    }
    return date
      ? this.isSlotBasedAppointmentAllowed
        ? getMoment(date).tz(timezone).format(Constants.LONG_DATETIME_FORMAT)
        : getMoment(date).format(
            BucDateTimeHelper.convertFromFlatPickrDateFormat(
              this.flatpickrDateFormat
            )
          )
      : '';
  }

  private async initDeliveryDetails() {
    this.accordionContent[0].title =
      this.nlsMap[
        'CHANGE_FULFILLMENT_METHOD.DELIVERY_DETAILS.LABEL_DELIVERY_DETAILS'
      ];
  }

  private async initShippingDetails() {
    this.isCarrierSvcInvalid = false;

    // TODO: get address from getCompleteOrderDetails or getCustomerDetails API for other to SHP
    // if (this.initialDeliveryMethod !== 'SHP') {
    //   if (!this.shippingAddressDetails) {
    //     await this.getShippingAddressDetails();
    //   }
    // }
    // Fetch items from fields json
    if (this.initialDeliveryMethod === 'SHP') {
      if (this.isLargeOrder === 'N') {
        if (
          this.fulfillmentGroupId !== undefined ||
          (this.orderLineKeys && this.orderLineKeys.length === 1)
        ) {
          if (
            this.selectedFulfillmentGroup &&
            this.selectedFulfillmentGroup.length
          ) {
            if (!this.shippingAddr) {
              this.shippingAddr =
                this.selectedFulfillmentGroup[0].PersonInfoShipTo;
            }
          }
        }
      }
    }
    if (!this.shippingAddr) {
      this.shippingAddr = this.fulfillmentSummaryDetails.Order.PersonInfoShipTo;
    }
    this.shipmentItems = [
      {
        id: 'shippingAddress',
        showEditIcon: true,
        showTile: true,
        value: this.shippingAddr || '-',
        title:
          'CHANGE_FULFILLMENT_METHOD.SHIPPING_DETAILS.LABEL_SHIPPING_ADDRESS',
        tooltipMsg:
          'CHANGE_FULFILLMENT_METHOD.SHIPPING_DETAILS.TOOLTIP_EDIT_SHIPPING_ADDRESS',
      },
      {
        id: 'levelOfService',
        showEditIcon: false,
        showTile: true,
        value:
          this.nlsMap[
            'CHANGE_FULFILLMENT_METHOD.MESSAGES.CARRIER_SERVICE_OPTIONS.NOT_CONFIGURED'
          ],
        title:
          'CHANGE_FULFILLMENT_METHOD.SHIPPING_DETAILS.LABEL_LEVEL_OF_SERVICE',
      },
      {
        id: 'comments',
        showEditIcon: false,
        showTile: false,
        value: '',
      },
    ];

    // Check SHIPTO Modification permission
    // check allowed modification array for released and extended release status orders
    let isShipToModAllowed;
    if (this.maxOrderStatus.startsWith('3200')) {
      isShipToModAllowed = IsModificationAllowed(
        this.fulfillmentSummaryDetails.Order?.AllowedModifications
          ?.Modification,
        Constants.MOD_TYPE_SHIPTO
      );
    } else {
      isShipToModAllowed = IsModificationAllowed(
        this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
          ?.Modifications?.Modification,
        Constants.MOD_TYPE_SHIPTO
      );
    }
    const shipAddrItem = this.shipmentItems.find(
      (el) => el.id === 'shippingAddress'
    );
    if (isShipToModAllowed) {
      shipAddrItem.showEditIcon = true;
    } else {
      shipAddrItem.showEditIcon = false;
    }

    if (
      this.initialDeliveryMethod !== 'SHP' ||
      this.detailsChanged.isShippingDetailsChanged
    ) {
      this.shipmentItems.find((el) => el.id === 'comments').showTile = true;
    }

    if (
      this.selectedCarrierSvcCode === undefined ||
      this.selectedCarrierSvcCode === null
    ) {
      if (this.loadedOrderLines) {
        await this.getCarrierServiceOptionsList();
      }
      if (this.carrierServiceList && this.carrierServiceList.length) {
        this.carrierServiceList.forEach(
          (el) =>
            (el.selected =
              el.CarrierServiceCode === this.initialCarrierServiceCode)
        );
      }
    } else {
      if (!this.carrierServiceList) {
        if (this.loadedOrderLines) {
          await this.getCarrierServiceOptionsList();
        }
      }
      if (this.carrierServiceList && this.carrierServiceList.length) {
        this.carrierServiceList.forEach(
          (el) =>
            (el.selected =
              el.CarrierServiceCode === this.selectedCarrierSvcCode)
        );
      }
    }
  }
  // TODO: Add back shipping address from customer details if needed
  // async getShippingAddressDetails(): Promise<any> {
  //   try {
  //     await this.changeFulfillmentMethodService.getShippingAddress(this.orderHeaderKey).then(mashupOutput => {
  //       if (mashupOutput.Order) {
  //         this.shippingAddressDetails = mashupOutput.Order;
  //       } else if (mashupOutput.Customer) {
  //         if (mashupOutput.Customer.CustomerContactList[0] && mashupOutput.Customer.CustomerContactList[0].CustomerContact) {
  //           const defaultShipTo = mashupOutput.Customer.CustomerContactList.CustomerContact[0].CustomerAdditionalAddressList.CustomerAdditionalAddress.find(el => el.IsDefaultShipTo === 'Y');
  //           this.shippingAddressDetails = defaultShipTo;
  //         }
  //       }

  //     });
  //   } catch (err) {
  //     console.error(err);
  //     const errorMsg = err?.error?.errors?.[0]?.ErrorDescription;
  //     const notification = {
  //       type: 'error',
  //       title: errorMsg
  //     };
  //     this.ccNotificationService.notify(notification);
  //     return false;
  //   }
  // }

  dispatchAction(action, url?, trackingNoList?) {
    switch (action) {
      case 'shippingAddress':
        this.openEditShippingAddressModal();
        break;
      case 'pickupBy':
        this.openChangePickupRecipientModal();
        break;
      case 'pickupAddress':
        this.openSelectStoreModal();
        break;
      case 'pickupAppointment':
        if (this.isSlotBasedAppointmentAllowed) {
          this.openPickupAppointmentModal();
        }
        break;
    }
  }

  openSelectStoreModal() {
    let orderLines = this.loadedOrderLines.filter(
      (line) => this.selectedItemsFromTable.indexOf(line.OrderLineKey) !== -1
    );
    this.actionProcessorService.dispatch<ActionParams>(Constants.SELECT_STORE, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          orderHeaderKey: this.fulfillmentSummaryDetails.Order.OrderHeaderKey,
          enterpriseCode: this.fulfillmentSummaryDetails.Order.EnterpriseCode,
          personInfoShipTo: this.pickupDetails.selectedStore
            ? this.pickupDetails.selectedStore.ShipNodePersonInfo
            : this.initialDeliveryMethod === 'PICK'
            ? this.selectedFulfillmentGroup[0]?.Shipnode?.ShipNodePersonInfo
            : this.selectedFulfillmentGroup[0]?.PersonInfoShipTo,
          orderLines: orderLines?.map((item) => ({
            OrderLineKey: item.OrderLineKey,
            RequiredQty: item.AvailableQty,
            Item: {
              ItemID: item.ItemDetails.ItemID,
              UnitOfMeasure: item.ItemDetails.UnitOfMeasure,
              ProductClass:
                item.ItemDetails.PrimaryInformation.DefaultProductClass,
            },
          })),
          selectedShipNode: this.selectedFulfillmentGroup[0]?.ShipNode,
          size: 'lg',
        },
      },
    });
  }

  openChangePickupRecipientModal() {
    this.actionProcessorService.dispatch<ActionParams>(
      Constants.PICKUP_RECIPIENT,
      {
        component: this.componentId,
        data: {
          modalText: '',
          modalData: {
            orderHeaderKey: this.fulfillmentSummaryDetails.Order.OrderHeaderKey,
            selectedGroup: this.selectedFulfillmentGroup[0],
            getDataWithoutSave: true,
            orderLineKey: !this.fulfillmentGroupId
              ? this.orderLineKeys[0]
              : null,
            pickedByInfo: this.pickupDetails.pickupBy.pickedByInfo,
            isPickedByCustomer: this.pickupDetails.pickupBy.isPickedByCustomer,
            size: 'sm',
          },
        },
      }
    );
  }

  openPickupAppointmentModal() {
    const earliestDate = this.pickupDetails.selectedStore
      ? this.pickupDetails.selectedStore.Availability?.AvailableDate
      : this.fulfillmentGroupId
      ? this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
          ?.EarliestShipDate
      : this.selectedLinesFromOrderDetails[0]?.EarliestShipDate;
    this.actionProcessorService.dispatch<ActionParams>(
      Constants.PICKUP_SLOT_APPOINTMENT,
      {
        component: this.componentId,
        data: {
          modalText: '',
          modalData: {
            enterpriseCode: this.fulfillmentSummaryDetails.Order.EnterpriseCode,
            orgCode: this.pickupDetails.selectedStore
              ? this.pickupDetails.selectedStore.ShipNode
              : this.selectedFulfillmentGroup[0]?.ShipNode,
            timeSlotList: this.pickupTimeSlotList,
            earliestAvailableDate: moment(earliestDate).format(
              BucDateTimeHelper.convertFromFlatPickrDateFormat(
                this.flatpickrDateFormat
              )
            ),
            selectedAppointmentDate:
              this.initialDeliveryMethod === 'PICK' &&
              !this.pickupDetails.appointmentDate
                ? this.fulfillmentGroupId
                  ? this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
                      ?.ReqShipDate
                  : this.selectedLinesFromOrderDetails[0]?.ReqShipDate
                : this.pickupDetails.appointmentDate,
          },
        },
      }
    );
  }

  onClickAccordion(evt) {
    if (evt && evt.expanded) {
      if (this.selectFulfillmentOptions) {
        if (
          this.selectFulfillmentOptions.find((el) => el.id === 'SHP').checked
        ) {
          this.accordionContent[0].title =
            this.nlsMap[
              'CHANGE_FULFILLMENT_METHOD.SHIPPING_DETAILS.LABEL_SHIPPING_DETAILS'
            ];
          this.initShippingDetails();
        }
        if (
          this.selectFulfillmentOptions.find((el) => el.id === 'PICK').checked
        ) {
          this.accordionContent[0].title =
            this.nlsMap[
              'CHANGE_FULFILLMENT_METHOD.PICKUP_DETAILS.LABEL_PICKUP_DETAILS'
            ];
          this.initPickupDetails();
        }
      }
    }
  }

  getNumberOfSelectedUnavailableLinesForShip(evt) {
    this.numberOfSelectedUnavailableLinesForShip = evt;
    if (this.numberOfSelectedUnavailableLinesForShip > 0) {
      this.showNotificationForShipping = true;
      this.enableSave = false;
    } else if (this.numberOfSelectedUnavailableLinesForShip === 0) {
      if (this.selectedItemsFromTable?.length) {
        this.enableSave = true;
      }
    }
    // Show error notification if unavailable lines exist for shipping in selected lines
    if (this.numberOfSelectedUnavailableLinesForShip > 0) {
      this.unavailableLinesNotificationObjForShip = {
        type: 'error',
        title: this.translateService.instant(
          `CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_UNAVAILABLE_SHIP`,
          {
            count: this.numberOfSelectedUnavailableLinesForShip,
          }
        ),
        message:
          this.selectedItemsFromTable?.length &&
          this.selectedItemsFromTable.length === 1
            ? this.translateService.instant(
                `CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_SELECT_ANOTHER_ADDRESS`
              )
            : this.translateService.instant(
                `CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_SELECT_ANOTHER_ADDRESS_OR_DESELECT_MSG`
              ),
        showClose: true,
        lowContrast: true,
      };
    }
  }

  getNumberOfSelectedUnavailableLinesForPick(evt) {
    this.numberOfSelectedUnavailableLinesForPick = evt;
    // Show error notification if unavailable lines exist for pickup in selected lines
    if (this.numberOfSelectedUnavailableLinesForPick > 0) {
      this.unavailableLinesNotificationObjForPick = {
        type: 'error',
        title: this.translateService.instant(
          `CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_UNAVAILABLE_PICK`,
          {
            count: this.numberOfSelectedUnavailableLinesForPick,
          }
        ),
        message:
          this.selectedItemsFromTable?.length &&
          this.selectedItemsFromTable.length === 1
            ? this.translateService.instant(
                `CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_SELECT_ANOTHER_ADDRESS`
              )
            : this.translateService.instant(
                `CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_SELECT_ANOTHER_ADDRESS_OR_DESELECT_MSG`
              ),
        showClose: true,
        lowContrast: true,
      };
      this.showNotificationForPickup = true;
    }
  }

  showLines() {
    // Scroll to top to show lines table
    this.ref.nativeElement.scrollIntoView();
  }

  resetFields() {
    this.someLinesNotAllowedNotificationObj = null;
    this.shipmentItems = [];
    this.pickupItems = [];
    this.shippingAddr = null;
    this.loadedOrderLines = null;
    this.openShippingAddrModal = false;
    this.openStoreAddrModal = false;
    this.selectedCarrierSvcCode = null;
    Object.keys(this.detailsChanged).forEach(
      (key) => (this.detailsChanged[key] = false)
    );
    this.commentsValue = '';
  }

  private addNotesToLines(orderLines) {
    const loginUserId =
      BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLoginId();
    orderLines.forEach((line) => {
      line.Notes = {
        Note: {
          ReasonCode: CommonCodes.changeFulfillment,
          NoteText: this.commentsValue.trim(),
          Createuserid: loginUserId,
        },
      };
    });
    return orderLines;
  }

  onRefresh() {
    this.resetFields();
    this.changeFulfillmentLinesTable.initialize();
  }

  // On click of Save button
  async modifyFulfillmentMethod() {
    if (this.selectFulfillmentOptions.find((el) => el.id === 'SHP').checked) {
      if (this.isCarrierSvcInvalid) {
        return;
      }
      let orderLines = this.loadedOrderLines.filter(
        (line) => this.selectedItemsFromTable.indexOf(line.OrderLineKey) !== -1
      );
      const input: any = {
        orderHeaderKey: this.orderHeaderKey,
        isLargeOrder: this.isLargeOrder,
        docType: this.fulfillmentSummaryDetails.Order.DocumentType,
        orgCode: this.fulfillmentSummaryDetails.Order.EnterpriseCode,
        personInfoShipTo: this.shippingAddr,
        carrierServiceCode:
          this.carrierServiceList?.find((el) => el.selected)
            ?.CarrierServiceCode || '',
        orderLines,
      };
      const selectedCarrierService = this.carrierServiceList?.find(
        (el) => el.selected
      );
      if (selectedCarrierService) {
        input['carrierServiceCode'] = selectedCarrierService.CarrierServiceCode;
        input['deliveryStartDate'] = selectedCarrierService.DeliveryStartDate;
        input['deliveryEndDate'] = selectedCarrierService.DeliveryEndDate;
      }
      if (this.commentsValue) {
        input.orderLines = this.addNotesToLines(orderLines);
      }
      await this.changeFulfillmentMethodService
        .modifyFulfillmentMethodFromPickToShip(input)
        .then((mashupOutput) => {
          if (mashupOutput) {
            // changes for EOMS-687
            let IsFraudOrder = mashupOutput.Order.IsFraudOrder;
            //EOMS-4261 START
            let isValidOrder = mashupOutput.Order.IsValidOrder;
            let response=mashupOutput.Order.Response;
            let isExceptionOrder=mashupOutput.Order.IsExpcetionOrder;
            if(isValidOrder=='N')
            {
              this.ccNotificationService.notify({
                type: 'error',
                title: '',
                message:response,
              });

            }
            else if (IsFraudOrder == 'Y') {
              this.ccNotificationService.notify({
                type: 'error',
                title: '',
                message:
                  'Fraud validation failed : Please provide valid address.',
              });
            }
            else if(isExceptionOrder=='Y'){
              this.ccNotificationService.notify({
                type: 'error',
                title: '',
                message:
                  'Exception occurred during Forter request.',
              });
              //EOMS-4261 END
            } else {
              // Navigate to order-details page
              this.ccNavigationSvc.goBackToPreviousPathInSameTab(
                `${this.previousPath}`,
                this.breadCrumbList,
                {},
                this.displaySuccessMsg.bind(this)
              );
            }
          }
        });
    } else if (
      this.selectFulfillmentOptions.find((el) => el.id === 'PICK').checked
    ) {
      this.updateShipToPickFulfillmentMethod();
    } else {
    }
  }

  // On click of Cancel button
  cancelChangeFulfillment() {
    this.ccNavigationSvc.goBackToPreviousPathInSameTab(
      `${this.previousPath}`,
      this.breadCrumbList,
      {}
    );
  }

  // Display success msg after modifying fulfillment method
  async displaySuccessMsg() {
    const selectedFulfillmentOption = this.selectFulfillmentOptions.find(
      (el) => el.checked
    ).id;
    let successTitle = '';
    if (this.initialDeliveryMethod !== selectedFulfillmentOption) {
      successTitle =
        selectedFulfillmentOption === 'SHP'
          ? await this._getNls(
              'CHANGE_FULFILLMENT_METHOD.SUCCESS_MESSAGES.TITLE_SHIP_SUCCESS',
              { count: this.selectedItemsFromTable.length }
            )
          : await this._getNls(
              'CHANGE_FULFILLMENT_METHOD.SUCCESS_MESSAGES.TITLE_PICK_SUCCESS',
              { count: this.selectedItemsFromTable.length }
            );
    } else {
      successTitle = await this._getNls(
        'CHANGE_FULFILLMENT_METHOD.SUCCESS_MESSAGES.MSG_LINES_UPDATED',
        { count: this.selectedItemsFromTable.length }
      );
    }

    this.ccNotificationService.notifyShell({
      statusType: 'success',
      statusContent: successTitle,
    });
  }

  private updateShipToPickFulfillmentMethod() {
    let orderLines = this.loadedOrderLines.filter(
      (line) => this.selectedItemsFromTable.indexOf(line.OrderLineKey) !== -1
    );
    const input: any = {
      Order: {
        OrderHeaderKey: this.orderHeaderKey,
        IsLargeOrder: this.isLargeOrder,
        DocumentType: this.fulfillmentSummaryDetails.Order.DocumentType,
        EnterpriseCode: this.fulfillmentSummaryDetails.Order.EnterpriseCode,
        OrderLines: {
          OrderLine: orderLines.map((item) => ({
            OrderLineKey: item.OrderLineKey,
            DeliveryMethod: 'PICK',
            ...(this.pickupDetails.pickupBy.isPickedByCustomer && {
              MarkForKey: ' ',
            }),
            ...(this.pickupDetails.selectedStore && {
              ShipNode: this.pickupDetails.selectedStore.ShipNode,
            }),
            ...(this.pickupDetails.appointmentDate && {
              ReqShipDate: this.pickupDetails.appointmentDate,
            }),
            ...(!this.pickupDetails.pickupBy.isPickedByCustomer &&
              this.pickupDetails.pickupBy.pickedByInfo && {
                PersonInfoMarkFor: this.pickupDetails.pickupBy.pickedByInfo,
              }),
          })),
        },
      },
    };
    if (this.commentsValue) {
      input.Order.OrderLines.OrderLine = this.addNotesToLines(
        input.Order.OrderLines.OrderLine
      );
    }
    this.changeFulfillmentMethodService
      .modifyFulfillmentMethodFromShipToPick(input)
      .then((mashupOutput) => {
        if (mashupOutput) {
          this.ccNavigationSvc.goBackToPreviousPathInSameTab(
            `${this.previousPath}`,
            this.breadCrumbList,
            {},
            this.displaySuccessMsg.bind(this)
          );
        }
      });
  }

  setPickupAppointmentDate(value) {
    const tz = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserTimeZone();
    const availableDate = this.pickupDetails.selectedStore
      ? this.pickupDetails.selectedStore.Availability?.AvailableDate
      : this.fulfillmentGroupId
      ? this.selectedFulfillmentGroup[0]?.OrderLines?.OrderLine[0]
          ?.EarliestShipDate
      : this.selectedLinesFromOrderDetails[0]?.EarliestShipDate;
    const formattedDate = fmtDate(
      BucDateTimeHelper.getMomentWithTimezone(availableDate, tz),
      Constants.MOMENT_DATE_FORMAT
    );
    this.pickupDetails.appointmentDate = value;
    if (moment(value).isSameOrAfter(moment(formattedDate))) {
      this.pickupDetails.isDateInvalid = false;
      this.enableSave = true;
      this.detailsChanged.isPickupDetailsChanged = true;
      this.pickupItems.find((el) => el.id === 'comments').showTile = true;
    } else {
      this.pickupDetails.isDateInvalid = true;
      this.pickupDetails.dateInvalidText = this.translateService.instant(
        'CHANGE_FULFILLMENT_METHOD.ERROR_MESSAGES.MSG_INVALID_DATE',
        { date: formattedDate }
      );
      this.enableSave = false;
    }
  }

  // Edit shipping address modal
  async openEditShippingAddressModal(): Promise<void> {
    this.actionProcessorService.dispatch<ModifyCustomerAddressParams>(
      Constants.MODIFY_CUSTOMER_ADDRESS,
      {
        component: this.componentId,
        data: {
          modalText:
            this.nlsMap[
              'CHANGE_FULFILLMENT_METHOD.SHIPPING_ADDRESS.LABEL_MODIFY_SHIPPING_ADDRESS'
            ],
          modalData: {
            orderDetails: this.fulfillmentSummaryDetails?.Order,
            EnterpriseCode:
              this.fulfillmentSummaryDetails?.Order?.EnterpriseCode,
            currentShipment: { PersonInfoShipTo: this.shippingAddr },
            customerId: this.fulfillmentSummaryDetails?.Order.ShipToID,
            saveCall: this.updateAddress.bind(this),
          },
        },
      }
    );
  }

  updateAddress(params) {
    this.saveShippingAddress(params.modifiedAddress, params.addressField);
  }

  async saveShippingAddress(modifiedAddress, addressField) {
    addressField.successCall();
    this.shippingAddr = modifiedAddress;
    this.shipmentItems.find((el) => el.id === 'shippingAddress').value =
      this.shippingAddr;
    this.detailsChanged.isShippingDetailsChanged = true;
    this.shipmentItems.find((el) => el.id === 'comments').showTile = true;
    this.changeFulfillmentLinesTable.checkForShippingLinesAvailability(
      this.shippingAddr
    );
  }

  // To detect changes in shipping address & update shipment items array
  public trackItem(index: number, item) {
    return item.value;
  }
}
