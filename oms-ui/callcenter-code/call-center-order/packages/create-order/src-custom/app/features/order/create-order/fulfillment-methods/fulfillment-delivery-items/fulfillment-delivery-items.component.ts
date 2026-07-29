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
  EventEmitter,
  Injector,
  Input,
  OnInit,
  Output,
  TemplateRef,
  ViewChild,
} from '@angular/core';
import {
  BucDateTimeHelper,
  BucSessionService,
  EditableFieldRenderer,
  fmtDate,
  getArray,
  getFlatPickrDateFormat,
  getMoment,
} from '@buc/common-components';
//EOMS-1463 - Changes Start (Import Service to make mashup call)
import {
  BucSvcAngularStaticAppInfoFacadeUtil,
  BucCommOmsMashupService,
} from '@buc/svc-angular';
//EOMS-1463 - Changes End 
import {
  Constants,
  ModifyFulfillmentMethodService,
  OrderCommonService,
} from '@call-center/order-shared';
import { TranslateService } from '@ngx-translate/core';
import moment from 'moment';
import { Observable, of } from 'rxjs';
import { ExtensionConstants } from '../../../../extension.constants';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'call-center-fulfillment-delivery-items',
  templateUrl: './fulfillment-delivery-items.component.html',
  styleUrls: ['./fulfillment-delivery-items.component.scss'],
})
export class FulfillmentDeliveryItemsComponent
  extends EditableFieldRenderer
  implements OnInit {
  EXTENSION = {
    TOP: ExtensionConstants.FULFILLMENT_DELIVERY_ITEMS_CO_TOP,
    BOTTOM: ExtensionConstants.FULFILLMENT_DELIVERY_ITEMS_CO_BOTTOM,
  };

  @Input() groupElement: any;
  @Input() orderDetails: any;
  @Output() deliveryAction: EventEmitter<any> = new EventEmitter();
  @Output() setPickupAppointment: EventEmitter<any> = new EventEmitter();
  @ViewChild('addressTpl', { static: true }) addressTpl: TemplateRef<any>;
  @ViewChild('recipientTpl', { static: true }) recipientTpl: TemplateRef<any>;

  componentId = 'FulfillmentDeliveryItemsComponent';

  FIELD_SHIPPING_ADDRESS = 'changeShippingAddress';
  FIELD_SERVICE_LEVEL = 'changeLevelOfService';
  FIELD_EXPECTED_DELIVERY_DATE = 'expectedDeliveryDate';
  FIELD_STORE_ADDRESS = 'changeStoreAddress';
  FIELD_PICKUP_DATE = 'changePickupDate';
  FIELD_PICKUP_RECIPIENT = 'changePickupRecipient';

  //EOMS-1463 - Changes Start (Define the Field & Add it in form)

  //EOMS-13713 - Changes Start
  costCenterOptions: any;
  orderReasonOptions: any;
  //EOMS-13713 - Changes End

  FIELD_ORDER_REASON = 'orderReason';
  FIELD_COST_CENTER = 'costCenter';

  formOptions = {
    [this.FIELD_SHIPPING_ADDRESS]: [],
    [this.FIELD_ORDER_REASON]: [],
    [this.FIELD_COST_CENTER]: []
  };

  //EOMS-1463 - Changes End

  requestMap: { [id: string]: string } = {
    [this.FIELD_PICKUP_DATE]: 'appointmentDate',
  };

  //EOMS-1463 - Changes Start (Make it editable)
  shadow = {
    [this.FIELD_SHIPPING_ADDRESS]: { isEditable: true },
    [this.FIELD_SERVICE_LEVEL]: { isEditable: true },
    [this.FIELD_STORE_ADDRESS]: { isEditable: true },
    [this.FIELD_PICKUP_DATE]: { isEditable: true },
    [this.FIELD_PICKUP_RECIPIENT]: { isEditable: true },
    [this.FIELD_EXPECTED_DELIVERY_DATE]: { isEditable: true },
    [this.FIELD_ORDER_REASON]: { isEditable: true },
    [this.FIELD_COST_CENTER]: { isEditable: true },
  };
  //EOMS-1463 - Changes End

  colIdsToRemove = [];
  appointmentDate: any;
  isDateInvalid = false;
  dateInvalidText = '';
  sessionPrefix: string;
  public sessionId: any;
  isChangeFulfillmentMethodWizard = false;
  protected bucSessionStorageService: BucSessionService;
  isCreateOrder: boolean;

  constructor(
    inj: Injector,
    private orderCommonService: OrderCommonService,
    public translate: TranslateService,
    public activatedRoute: ActivatedRoute,
    private modifyFulfillmentMethodService: ModifyFulfillmentMethodService,
    //EOMS-1463 - Changes Start (Inject the Service through Constructor)
    private bucCommOmsMashupService: BucCommOmsMashupService,
    //EOMS-1463 - Changes End
  ) {
    super(inj);
  }

  ngOnInit(): void {
    this.initialize();
  }

  async initialize() {
    this.initializeSession();
    if (this.groupElement.value.DeliveryMethod === 'SHP') {
      await this.initializeFieldDetailAttributes('order-shipments-ship');
    } else if (this.groupElement.value.DeliveryMethod === 'PICK') {
      await this.initializeFieldDetailAttributes('order-shipments-pick');
    }
    this.initPickupDate();
    await this._initFormOptions();
    this.checkChangingFulfillmentOption();
    await this.loadPageAttributes().toPromise();
  }

  initializeSession() {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    const uniqId = this.activatedRoute.snapshot.queryParams.uniqueId;
    this.sessionPrefix = 'create-order-' + uniqId;
    this.bucSessionStorageService = new BucSessionService(
      this.sessionPrefix,
      this.sessionId,
    );

    const isAddLinesWizard = queryParams?.addLines
      ? JSON.parse(queryParams?.addLines)
      : false;
    const isChangeFulfillmentOptionsWizard = queryParams?.changeFulfillment
      ? JSON.parse(queryParams?.changeFulfillment)
      : false;
    this.isCreateOrder = !isAddLinesWizard && !isChangeFulfillmentOptionsWizard;
  }

  // EOMS-1463 - Changes Start (Add our field in editable field list)
  _isEditable(id, allowanceMap) {
    if (!this.isCreateOrder) {
      return this.shadow[id]?.isEditable;
    }
    return (
      id === this.FIELD_SERVICE_LEVEL ||
      id == this.FIELD_PICKUP_DATE ||
      id === this.FIELD_ORDER_REASON ||
      id === this.FIELD_COST_CENTER
    ); 
    // EOMS-1463 - Changes End
  }

  private async _initFormOptions() {
    if (this.groupElement.value.DeliveryMethod === 'SHP') {
      await this.getCarrierServiceOptionsList(this.groupElement);
    } else {
      this.colIdsToRemove.push(this.FIELD_SERVICE_LEVEL);
    }

    if (this.groupElement.carrierServiceList?.length > 0) {
      this.formOptions[this.FIELD_SERVICE_LEVEL] =
        this.groupElement.carrierServiceList;
    }

    //EOMS-1463 - Changes Start

    this.costCenterOptions = await this.getCostCenters(this.orderDetails.EnterpriseCode);
    this.orderReasonOptions = await this.getOrderReasons();

    if (this.orderReasonOptions?.length) {
      this.formOptions[this.FIELD_ORDER_REASON] = this.orderReasonOptions;
    }

    if (this.costCenterOptions?.length) {
      this.formOptions[this.FIELD_COST_CENTER] = this.costCenterOptions;
    }
    //EOMS-1463 - Changes End
  }

  protected fetchPageAttributeData(): Observable<Array<any>> {
    return of(getArray(this.groupElement));
  }

  protected getDataForAttribute(id: string, item: any): Promise<any> {
    let attr: any = {};
    let data: any = '';
    switch (id) {
      case this.FIELD_SHIPPING_ADDRESS:
        data = {
          template: this.addressTpl,
          templateData: {
            value: this.groupElement.value.PersonInfoShipTo,
            id: this.FIELD_SHIPPING_ADDRESS,
            disabled: !this.shadow[id].isEditable,
          },
        };
        break;
      case this.FIELD_SERVICE_LEVEL:
        data = this.formOptions[this.FIELD_SERVICE_LEVEL];
        break;
      case this.FIELD_STORE_ADDRESS:
        const value = {
          ...this.groupElement.value.Shipnode?.ShipNodePersonInfo,
          FirstName: this.groupElement.value.Shipnode?.Description,
          LastName: '',
        };
        data = {
          template: this.addressTpl,
          templateData: {
            value: value,
            id: this.FIELD_STORE_ADDRESS,
            disabled: !this.shadow[id].isEditable,
          },
        };
        break;
      case this.FIELD_PICKUP_DATE:
        data = {
          dateData: this.groupElement.appointmentDate,
          invalid: this.isDateInvalid,
          invalidText: this.dateInvalidText,
          disabled: !this.shadow[id].isEditable,
        };
        break;
      case this.FIELD_PICKUP_RECIPIENT:
        //make link
        const name = this.groupElement.value?.OrderLines?.OrderLine[0]
          ?.PersonInfoMarkFor
          ? this.groupElement.value?.OrderLines?.OrderLine[0]?.PersonInfoMarkFor
            .FirstName +
          ' ' +
          this.groupElement.value?.OrderLines?.OrderLine[0]?.PersonInfoMarkFor
            .LastName
          : this.orderDetails.CustomerFirstName +
          ' ' +
          this.orderDetails.CustomerLastName;
        const phone = this.groupElement.value?.OrderLines?.OrderLine[0]
          ?.PersonInfoMarkFor
          ? this.groupElement.value?.OrderLines?.OrderLine[0]?.PersonInfoMarkFor
            .DayPhone
          : '';

        data = {
          template: this.recipientTpl,
          templateData: {
            value: name,
            phone: phone,
            id: this.FIELD_PICKUP_RECIPIENT,
            disabled: !this.shadow[id].isEditable,
          },
        };
        break;

      //EOMS-1463 - Changes Start
      case this.FIELD_ORDER_REASON:
        data = this.formOptions[this.FIELD_ORDER_REASON];
        break;
      //EOMS-1463 - Changes End
      case this.FIELD_COST_CENTER:
        data = this.formOptions[this.FIELD_COST_CENTER];
        break;
    }

    attr = { ...attr, data };
    return attr as any;
  }

  async emitAction(id) {
    this.deliveryAction.emit({ action: id, group: this.groupElement });
  }

  onDropdown(attr, valueDesc) {
    this.deliveryAction.emit({
      action: attr.id,
      group: this.groupElement,
      value: valueDesc,
    });
  }

  async onDate(attr, valueDesc) {
    if (getArray(valueDesc).length) {
      const tz = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserTimeZone();
      const dateTimeMoment = BucDateTimeHelper.getMoment(attr.rawData);
      const newDate = BucDateTimeHelper.getMoment(
        valueDesc[0],
        Constants.DATE_FORMAT,
      ).format('YYYY-MM-DD');
      const newDateTokens = newDate.split('-');

      const earliestAvailableDate =
        this.groupElement.value?.OrderLines?.OrderLine[0]?.EarliestShipDate;
      const availableDate = fmtDate(
        BucDateTimeHelper.getMomentWithTimezone(earliestAvailableDate, tz),
        Constants.MOMENT_DATE_FORMAT,
      );
      this.appointmentDate = newDate;

      if (getMoment(newDate).isSameOrAfter(availableDate)) {
        this.isDateInvalid = false;
      } else {
        this.isDateInvalid = true;
        this.dateInvalidText = this.translate.instant(
          'FULFILLMENT_METHODS.MSG_INVALID_DATE',
          { date: availableDate },
        );

        this.getDataForAttribute(this.FIELD_PICKUP_DATE, this.groupElement);
        await this.loadPageAttributes().toPromise();
      }

      const dateTimeBeforeUTC = dateTimeMoment
        .year(Number(newDateTokens[0]))
        .month(Number(newDateTokens[1]) - 1)
        .date(Number(newDateTokens[2]))
        .hour(0)
        .minute(0);

      const localTime = `${dateTimeBeforeUTC.format('YYYY-MM-DD')}T00:00`;
      const finalUtcTime = BucDateTimeHelper.getMomentInDifferentTimezone(
        localTime,
        tz,
      ).toISOString();

      this.setPickupAppointment.emit({
        group: this.groupElement,
        value: finalUtcTime,
      });
      this.onAddChange(attr, finalUtcTime);
    }
  }

  initPickupDate() {
    if (!this.groupElement.appointmentDate) {
      const selectedAppointmentDate =
        this.groupElement.value?.OrderLines?.OrderLine[0]?.ReqShipDate;
      this.groupElement.appointmentDate = selectedAppointmentDate
        ? moment(selectedAppointmentDate).format(
          BucDateTimeHelper.convertFromFlatPickrDateFormat(
            getFlatPickrDateFormat(),
          ),
        )
        : '';
    }
  }

  async getCarrierServiceOptionsList(groupElement) {
    groupElement.isCarrierSvcInvalid = false;
    let orderLinesInput: any = [];
    orderLinesInput = groupElement?.value.OrderLines.OrderLine.map((line) => ({
      ItemID: line.ItemDetails.ItemID,
      OrderLineKey: line.OrderLineKey,
      EarliestShipDate: line.EarliestShipDate,
      IsParcelShippingAllowed:
        line.ItemDetails.PrimaryInformation.IsParcelShippingAllowed,
      PersonInfoShipTo: groupElement?.value.PersonInfoShipTo,
    }));
    await this.orderCommonService
      .getCarrierServiceOptions(
        this.orderDetails.OrderHeaderKey,
        orderLinesInput,
      )
      .then((mashupOutput) => {
        if (
          mashupOutput.CarrierServiceList &&
          mashupOutput.CarrierServiceList.CarrierService?.length
        ) {
          groupElement.carrierServiceList =
            mashupOutput.CarrierServiceList?.CarrierService;
          if (
            groupElement.carrierServiceList &&
            groupElement.carrierServiceList.length
          ) {
            groupElement.carrierServiceList.sort((a, b) =>
              a.CarrierServiceDesc > b.CarrierServiceDesc
                ? 1
                : a.CarrierServiceDesc < b.CarrierServiceDesc
                  ? -1
                  : 0,
            );
            groupElement.carrierServiceList.forEach((el) => {
              // content value is in this format : Express (2022-09-05 - 2022-10-06)
              const content =
                `${el.CarrierServiceDesc}` +
                ' (' +
                this.getDateFormat(el.DeliveryStartDate, el.DeliveryEndDate) +
                ')';
              el.content = content;
              el.selected =
                groupElement?.value.CarrierServiceCode === el.CarrierServiceCode
                  ? true
                  : false;
              el.id = el.value = el.CarrierServiceCode;
              if (el.selected) {
                groupElement.initialCarrierServiceCode = el.id;
                groupElement.expectedDate = this.getDateFormat(
                  el.DeliveryStartDate,
                  el.DeliveryEndDate,
                );
              }
              //this.selectedFmtGroup = groupElement;
            });
          }
        }
      });
  }

  getDateFormat(date1, date2) {
    return (
      getMoment(date1).format(Constants.SHORT_DATE_FORMAT) +
      ' - ' +
      getMoment(date2).format(Constants.SHORT_DATE_FORMAT)
    );
  }

  checkChangingFulfillmentOption() {
    if (!this.isCreateOrder) {
      Object.keys(this.shadow).forEach((id) => {
        let isAllowed = false;
        switch (id) {
          case this.FIELD_STORE_ADDRESS:
            isAllowed =
              this.modifyFulfillmentMethodService.isChangeStoreAllowed(
                this.groupElement.value,
              );
            break;
          case this.FIELD_PICKUP_RECIPIENT:
            isAllowed =
              this.modifyFulfillmentMethodService.isChangePickRecipientAllowed(
                this.groupElement.value,
              );
            break;
          case this.FIELD_PICKUP_DATE:
            isAllowed =
              this.modifyFulfillmentMethodService.isChangePickupAppointmentAllowed(
                this.groupElement.value,
              );
            break;
          case this.FIELD_SERVICE_LEVEL:
            isAllowed =
              this.modifyFulfillmentMethodService.isCarrierServiceChangeAllowed(
                this.groupElement.value,
              );
            break;
          case this.FIELD_SHIPPING_ADDRESS:
            isAllowed =
              this.modifyFulfillmentMethodService.isShippingAddrEditAllowed(
                this.groupElement.value.OrderLines?.OrderLine[0]
                  ?.MaxLineStatus || '',
                this.groupElement.value,
                this.orderDetails?.EnterpriseCode,
              );
            break;
        }
        this.shadow[id].isEditable = isAllowed;
      });
    }
  }

  //EOMS-1463, 13713 - Changes Start (Function to fectch the list of reasons from common code)
  async getCostCenters(enterpriseCode: string) {
    return this.getCommonCodes(enterpriseCode, 'CROCS_COST_CENTER');
  }

  async getOrderReasons() {
    return this.getCommonCodes('CROCS', 'CROCS_ORDER_REASONS');
  }

  private async getCommonCodes(callingOrganizationCode: string, 
    codeType: string,
  ) {
    const input = {
      CommonCode: {
        CallingOrganizationCode: callingOrganizationCode,
        CodeType: codeType,
      },
    };

    const mashupOutput = await this.bucCommOmsMashupService
      .callMashup('icc.order.summary.getCommonCodeList', input, {})
      .toPromise();

    const response = this.bucCommOmsMashupService.getMashupOutput(mashupOutput, 'icc.order.summary.getCommonCodeList');

    const list = response?.CommonCodeList?.CommonCode || [];

    return list.map((el) => ({
      id: el.CodeValue,
      value: el.CodeValue,
      content: el.CodeValue,
      selected: el.CodeLongDescription?.trim().toUpperCase() === 'DEFAULT',
    }));
  }
  //EOMS-1463, 13713 - Changes End
}
