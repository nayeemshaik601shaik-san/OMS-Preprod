/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2019, 2025
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

import {
  AfterViewInit,
  Component,
  Inject,
  QueryList,
  TemplateRef,
  ViewChildren,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BaseModal, ModalService } from 'carbon-components-angular';
import {
  CommonBinaryOptionModalComponent,
  CommonService,
} from '@buc/common-components';
import { BucAddressDisplayHelperService } from '../address-display/address-display-helper.service';
import { TemplateIdDirective } from '@buc/common-components';
import { isEqual, isEmpty, pick, omit, omitBy, assign, keys } from 'lodash';
import { Constants } from '@buc/common-components';
import { BucNotificationService } from '@buc/common-components';
import { BucNotificationModel } from '@buc/common-components';
import _ from 'lodash';
import { BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { isEmailValid } from '../common/functions';

@Component({
  selector: 'buc-customer-address-selection',
  templateUrl: './customer-address-selection.component.html',
  styleUrls: ['./customer-address-selection.component.scss'],
  providers: [BucAddressDisplayHelperService],
})
export class CustomerAddressSelectionComponent
  extends BaseModal
  implements AfterViewInit
{
  componentId = 'modify-shipping-address';

  // Forms
  addressFields: any[] = [];
  mandatoryFields: any = [];
  numberFields: any = [];
  personInfo: any;
  prevPersonInfo: any;

  userSelectedAddress = [];
  userSelectedAddressPersonInfoKey = '';

  // Order
  deliveryIns = '';
  addressDes = '';
  shippingGroups: any;
  modifiedAddress: any;
  orderAddressKey: any;

  // Customer information
  customerContactID: string;
  customerKey = '';
  organizationCode = '';
  isForBusinessCustomer = false;
  isForContact = false;

  // UI controls
  isInitailizedComplete = false;
  existingAddresses: any[] = [];
  disableLink = false;
  disableBtn = true;
  saveOption: any;
  allowDelete = false;
  isForCustomerProfile = false;
  notificationShown = false;
  notificationObj: any;
  editSuggestedAddress = false;
  radios: any = [];
  skipAddressCheck = false;
  showAddressBypass = false;
  //  pass necessary and accepted fields to avoid API output violation
  ignoreProps = [
    'defaultBilling',
    'default',
    'defaultSoldTo',
    'orderline',
    'OriginalKey',
    'IsContactAddress',
    'IsShipTo',
    'IsBillTo',
    'IsSoldTo',
    'CustomerAdditionalAddressID',
    'IsDefaultShipTo',
    'IsDefaultBillTo',
    'IsDefaultSoldTo',
  ];

  personInfoProps = [
    'AddressID',
    'AddressLine1',
    'AddressLine2',
    'City',
    'Country',
    'CountryDesc',
    'DayPhone',
    'EMailID',
    'EnterpriseCode',
    'IsAddressVerified',
    'FirstName',
    'LastName',
    'MobilePhone',
    'EveningPhone',
    'PersonID',
    'Company',
    'PersonInfoKey',
    'State',
    'Title',
    'TitleDesc',
    'ZipCode',
  ];

  readonly resourceIds = {
    OVERRIDE_ADDRESS_VERIFICATION: 'ICC000054',
  };

  protected readonly nlsMap: any = {
    'EDIT_ADDRESS.LABEL_MODIFY_SHIPPING_ADDRESS_CANCEL': '',
    'EDIT_ADDRESS.LABEL_MODIFY_SHIPPING_ADDRESS_SAVE': '',
    'EDIT_ADDRESS.LABEL_MODIFY_SHIPPING_ADDRESS': '',
    'EDIT_ADDRESS.MSG_SUCCESS_SHIPMENTADDRESS_UPDATE': '',
    'EDIT_ADDRESS.MSG_FAILED_SHIPMENTADDRESS_UPDATE': '',
    'EDIT_ADDRESS.ERROR_MESSAGE.PLEASE_ENTER_VALID_PHONE_NUMBER': '',
    'EDIT_ADDRESS.ERROR_MESSAGE.REQUIRED_FIELD': '',
    'EDIT_ADDRESS.LABEL_SAVE_TO_CUSTOMER_PROFILE': '',
    'EDIT_ADDRESS.LABEL_DEFAULT_ADDRESS': '',
    'EDIT_ADDRESS.LABEL_DEFAULT_BILLING_ADDRESS': '',
    'EDIT_ADDRESS.MSG_INVALID_ADDRESS': '',
    'EDIT_ADDRESS.MSG_FAILED_CUSTOMER_UPDATE': '',
    'EDIT_ADDRESS.ERROR_SUGGESTED_ADDRESS': '',
    'EDIT_ADDRESS.ERROR_SUGGESTED_ADDRESS_MSG': '',
    'EDIT_ADDRESS.LABEL_DEFAULT_SOLD_TO_ADDRESS': '',
    'EDIT_ADDRESS.CUSTOMER_DETAILS.LABEL_CONFIRM': '',
    'EDIT_ADDRESS.CUSTOMER_DETAILS.LABEL_DISMISS': '',
    'EDIT_ADDRESS.CUSTOMER_DETAILS.LABEL_CONFIRM_SAVE_ADDRESS_MODAL_HEADER': '',
    'EDIT_ADDRESS.CUSTOMER_DETAILS.SHIP_BILL': '',
    'EDIT_ADDRESS.CUSTOMER_DETAILS.SHIP_BILL_SOLDTO': '',
    'EDIT_ADDRESS.CUSTOMER_DETAILS.SHIP_SOLDTO': '',
    'EDIT_ADDRESS.CUSTOMER_DETAILS.BILL_SOLDTO': '',
    'EDIT_ADDRESS.ERROR_MESSAGE.LABEL_INVALID_FORMAT': '',
  };

  readonly selectedAddressStyle = {
    'border-left': '4px solid #0F62FE',
    background: '#E0E0E0',
    'margin-bottom': '0rem',
  };

  templates: { [id: string]: TemplateRef<any> } = {};
  public notificationService: BucNotificationService =
    new BucNotificationService();
  addressType = '';
  isViewInitialized = false;
  suggestedAddressSelected: boolean;
  @ViewChildren(TemplateIdDirective, { emitDistinctChangesOnly: false })
  set unnamed(a: QueryList<TemplateIdDirective>) {
    if (a) {
      a.forEach(({ id, template }) => (this.templates[id] = template));
    }
  }

  constructor(
    @Inject('modalText') public modalText,
    @Inject('modalData') public modalData,
    public translate: TranslateService,
    private commonService: CommonService,
    private modalService: ModalService,
    private helperService: BucAddressDisplayHelperService
  ) {
    super();
    this.orderAddressKey =
      this.modalData.currentShipment?.PersonInfoShipTo?.PersonInfoKey;
  }

  async ngAfterViewInit() {
    const p = [
      this._initTranslations(),
      this.helperService.getAddressJson(this.personInfo),
    ];
    await Promise.all(p);
    this.setSaveOptions();
    await this.getCompleteAddressList();
    this.initRadio();
    this.isViewInitialized = true;
  }

  protected async _initTranslations(): Promise<void> {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translate.get(keys).toPromise();
    keys.forEach((k) => (this.nlsMap[k] = json[k]));
  }

  setSaveOptions() {
    this.saveOption = {
      customerProfile: {
        value: false,
        key: this.nlsMap['EDIT_ADDRESS.LABEL_SAVE_TO_CUSTOMER_PROFILE'],
      },
      defaultShipping: {
        value: false,
        key: this.nlsMap['EDIT_ADDRESS.LABEL_DEFAULT_ADDRESS'],
      },
      defaultBilling: {
        value: false,
        key: this.nlsMap['EDIT_ADDRESS.LABEL_DEFAULT_BILLING_ADDRESS'],
      },
      defaultSoldTo: {
        value: false,
        key: this.nlsMap['EDIT_ADDRESS.LABEL_DEFAULT_SOLD_TO_ADDRESS'],
      },
    };
  }

  /**
   * Retrieve addresses based on order detail or use addresses passed via modal data
   */
  async getCompleteAddressList() {
    try {
      if (this.modalData.orderDetails) {
        const input = {
          OrderLine: {
            OrderHeaderKey: this.modalData.orderDetails?.OrderHeaderKey,
            BillToID: this.modalData.orderDetails?.BillToID,
            CustomerContactID: this.modalData.orderDetails.CustomerContactID,
            EnterpriseCode: this.modalData.orderDetails?.EnterpriseCode,
          },
        };
        this.customerContactID = this.modalData.orderDetails.CustomerContactID;
        this.organizationCode = this.modalData.orderDetails.EnterpriseCode;
        this.isForBusinessCustomer =
          this.modalData.isForBusinessCustomer ?? false;
        await this.commonService
          .getOrderAllAddresses(input)
          .then((mashupOutput) => {
            this.getExistingAddresses(mashupOutput?.PersonInfoList);
          });
      } else {
        this.isInitailizedComplete = true;
        this.existingAddresses = this.modalData.addressList;
        this.organizationCode = this.modalData.EnterpriseCode;
        this.customerKey = this.modalData.customerKey;
        this.customerContactID = this.modalData.customerContactID;
        this.isForBusinessCustomer =
          this.modalData.isForBusinessCustomer ?? false;
        this.isForContact = this.modalData.isForContact ?? false;
        // Set initial address to be an empty form for a new address
        if (this.modalData?.triggerNewAddress) {
          this.addNewAddress();
        }
        // Will be added to customer profile by default
        // Hence, we can hide the `For customer profile` checkbox
        if (this.modalData.isForCustomerProfile) {
          this.isForCustomerProfile = true;
          this.getSuggestedList();
        }
        // Show delete button
        if (this.modalData.enableDelete) {
          this.allowDelete = true;
        }
      }
    } catch (e) {
      console.log(`Error: `, e);
    }
  }

  getAddressFields(addressFields): void {
    this.addressFields = addressFields.addressFields;
    this.mandatoryFields = addressFields.mandatoryFields;
    this.numberFields = addressFields.numberFields;
  }

  setLowerCaseObject(addressObj) {
    if (Object.keys(addressObj).length) {
      Object.keys(addressObj).forEach((key) => {
        const isLowerCaseReq =
          addressObj[key] && typeof addressObj[key] === 'string';
        addressObj[key] = isLowerCaseReq
          ? addressObj[key].toLowerCase()
          : addressObj[key];
      });
    }
  }

  updateExistingAddresses() {
    if (this.modalData.currentShipment?.PersonInfoShipTo) {
      const currentAddress = this.modalData.currentShipment?.PersonInfoShipTo;
      this.removeDuplicateOrderLineAddress();
      this.removeDuplicateOrderAddresses(currentAddress);
      this.setExistingAddressFromCurrent(currentAddress);
    } else {
      // Person Info Ship to is missing
      // Create a dictionary using the PersonInfoKey and remove duplicates
      const dict = {};
      this.existingAddresses.forEach((existingAddress) => {
        if (existingAddress.PersonInfoKey) {
          // Merge both objects, values should be the same since they have same PersonInfoKey, but it will merge props
          // such as `isOrderAddress`
          dict[existingAddress.PersonInfoKey] = {
            ...(dict[existingAddress.PersonInfoKey]
              ? dict[existingAddress.PersonInfoKey]
              : {}),
            ...existingAddress,
          };
        } else {
          // If personInfoBillTo is missing in order, API returns an empty object
          // Select the address and let users fill out billing information
          existingAddress.isOrderAddress = 'Y';
          existingAddress.isAddressSelected = true;
          dict[''] = existingAddress;
        }
      });

      this.existingAddresses = Object.keys(dict).map(
        (addressKey) => dict[addressKey]
      );
    }
  }

  setExistingAddressFromCurrent(currentAddress) {
    this.existingAddresses.forEach((existingAddress, index) => {
      if (existingAddress.PersonInfoKey === currentAddress.PersonInfoKey) {
        existingAddress.isAddressSelected = true;
        this.existingAddresses[index] = Object.assign(
          existingAddress,
          currentAddress
        );
      }
    });
  }

  removeDuplicateOrderLineAddress() {
    const orderAddress = this.existingAddresses.filter(
      (existingAddress) =>
        existingAddress.isOrderShippingAddress === 'Y' ||
        existingAddress.isOrderBillingAddress === 'Y'
    );
    const orderLineAddressIndex = [];
    this.existingAddresses.forEach((existingAddress, index) => {
      if (
        existingAddress.orderline === 'Y' &&
        orderAddress.find(
          (i) => existingAddress.PersonInfoKey === i.PersonInfoKey
        )
      ) {
        orderLineAddressIndex.push(index);
      }
    });
    if (orderLineAddressIndex.length) {
      orderLineAddressIndex.sort((a, b) => b - a);
      orderLineAddressIndex.forEach((i) => {
        this.existingAddresses.splice(i, 1);
      });
    }
  }

  removeDuplicateOrderAddresses(currentAddress) {
    const { orderAddressIndex, orderAddresses } = this.getOrderAddresses();
    const isRemoveDuplicateAddressIndex = [];
    this.existingAddresses
      .filter((existingAddress) => existingAddress.CustomerAdditionalAddressID)
      .forEach((existingAddress) => {
        if (existingAddress.CustomerAdditionalAddressID) {
          const {
            PersonInfoKey,
            IsAddressVerified,
            isAddressSelected,
            IsDefaultShipTo,
            IsDefaultBillTo,
            IsDefaultSoldTo,
            CustomerAdditionalAddressID,
            IsContactAddress,
            ...customerAddressCheck
          } = existingAddress;
          this.setLowerCaseObject(customerAddressCheck);
          orderAddressIndex.forEach((value, index) => {
            const {
              PersonInfoKey,
              isOrderShippingAddress,
              isOrderBillingAddress,
              IsAddressVerified,
              OriginalKey,
              orderline,
              IsContactAddress,
              ...orderAddress
            } = orderAddresses[index];
            if (
              isEqual(
                omitBy(orderAddress, (o) => o === ''),
                omitBy(customerAddressCheck, (c) => c === '')
              )
            ) {
              isRemoveDuplicateAddressIndex.push(value);
              existingAddress.isOrderShippingAddress =
                this.getAddressFlagValue(
                  orderAddresses[index].isOrderShippingAddress
                ) ?? existingAddress.isOrderShippingAddress;
              existingAddress.isOrderBillingAddress =
                this.getAddressFlagValue(
                  orderAddresses[index].isOrderBillingAddress
                ) ?? existingAddress.isOrderBillingAddress;
              existingAddress.orderline = this.getAddressFlagValue(
                orderAddresses[index].orderline
              );
              existingAddress.isAddressSelected =
                currentAddress.PersonInfoKey ===
                orderAddresses[index].PersonInfoKey
                  ? true
                  : false;
            }
          });
        }
      });
    if (isRemoveDuplicateAddressIndex.length > 0) {
      isRemoveDuplicateAddressIndex.sort((a, b) => b - a);
      isRemoveDuplicateAddressIndex.forEach((i) => {
        this.existingAddresses.splice(i, 1);
      });
    }
  }

  getAddressFlagValue(flagValue) {
    if (flagValue) {
      return flagValue === 'y' ? 'Y' : 'N';
    }
    return undefined;
  }

  getOrderAddresses() {
    const orderAddressIndex: any[] = [];
    const orderAddresses: any[] = [];
    this.existingAddresses.forEach((existingAddress, index) => {
      if (
        existingAddress.isOrderShippingAddress === 'Y' ||
        existingAddress.isOrderBillingAddress === 'Y' ||
        existingAddress.orderline === 'Y'
      ) {
        orderAddressIndex.push(index);
        const orderAddress = { ...existingAddress };
        this.setLowerCaseObject(orderAddress);
        orderAddresses.push(orderAddress);
      }
    });
    return { orderAddressIndex, orderAddresses };
  }

  getSuggestedList() {
    if (this.modalData.suggestedNewAddresses) {
      const keysToRemove = [
        'PersonInfo',
        'Company',
        'EveningPhone',
        'EMailID',
        'Title',
        'PersonInfoKey',
        'PersonID',
        'FirstName',
        'LastName',
        'MobilePhone',
        'DayPhone',
        'AddressLine3',
      ];
      this.modalData.suggestedAddress = this.modalData.suggestedAddress.map(
        (obj) => {
          const {
            Company,
            EveningPhone,
            Title,
            PersonInfoKey,
            PersonID,
            AddressLine3,
            ...filteredObj
          } = obj;
          return filteredObj;
        }
      );
      if (!this.isForCustomerProfile) {
        const userSelectedAddress = this.modalData.currentShipment
          ? { ...this.modalData.currentShipment.PersonInfoShipTo }
          : { ...this.modalData.currentBilling.PersonInfoBillTo };
        keysToRemove.forEach((key) => delete userSelectedAddress[key]);
        this.userSelectedAddress = userSelectedAddress;
      } else {
        const addressObj = {
          ...this.modalData.addressList.find(
            (obj) => obj.isAddressSelected === true
          ),
        };
        delete addressObj.FirstName;
        delete addressObj.LastName;
        delete addressObj.DayPhone;
        delete addressObj.MobilePhone;
        delete addressObj.EMailID;
        this.userSelectedAddress = addressObj;
      }
      this.disableBtn = false;
      this.saveOption.customerProfile.value = false;
      this.saveOption.defaultBilling.value = false;
      this.saveOption.defaultShipping.value = false;
      this.saveOption.defaultSoldTo.value = false;
    }
  }

  getExistingAddresses(personInfoList) {
    this.existingAddresses = [];
    this.customerKey = personInfoList.CustomerKey;
    this.organizationCode = this.modalData.EnterpriseCode;
    if (personInfoList.PersonInfo) {
      this.existingAddresses = personInfoList.PersonInfo;
    }
    this.updateExistingAddresses();
    if (
      this.modalData.isUnavailableMultiLines ||
      !this.existingAddresses.find((i) => i.isAddressSelected)
    ) {
      this.addNewAddress();
    }
    this.getSuggestedList();
    this.isInitailizedComplete = true;
  }

  selectedSuggestedAddress(address) {
    this.suggestedAddressSelected = true;
    // eslint-disable-next-line no-prototype-builtins
    if (
      !address.value.hasOwnProperty('isAddressSelected') ||
      address.value.isAddressSelected !== true
    ) {
      address.value.isAddressSelected = true;
    }
    // eslint-disable-next-line no-prototype-builtins
    if (this.personInfo.hasOwnProperty('AddressID')) {
      address.value.AddressID = this.personInfo.AddressID;
    }
    for (const [key, value] of Object.entries(address.value)) {
      // eslint-disable-next-line no-prototype-builtins
      if (this.personInfo.hasOwnProperty(key)) {
        this.personInfo[key] = value;
      }
    }
    // eslint-disable-next-line no-prototype-builtins
    const hasNewAddress =
      this.personInfo.hasOwnProperty('newAddress') &&
      this.personInfo.newAddress === true;
    if (hasNewAddress) {
      for (const [key, value] of Object.entries(address.value)) {
        this.personInfo[key] = value;
      }
    }
  }

  modifyAddress() {
    this.modalData.suggestedNewAddresses = false;
    this.modalData.suggestedAddress = [];
    this.editSuggestedAddress = true;
    this.userSelectedAddress = [];
    this.suggestedAddressSelected = true;
  }

  saveButtonEnabled(personInfo) {
    if (
      personInfo.PersonInfoKey !== this.orderAddressKey ||
      personInfo.isOrderShippingAddress ||
      personInfo.isOrderBillingAddress
    ) {
      this.disableBtn = false;
    }
  }

  checkContactInfo() {
    const requiredKeys = [
      'FirstName',
      'LastName',
      'EMailID',
      'DayPhone',
      'MobilePhone',
    ];
    return _.some(requiredKeys, (key) => !_.isEmpty(this.personInfo[key]));
  }

  initRadio() {
    if (Object.keys(this.templates).length && this.existingAddresses.length) {
      this.setSelectedAddressAsFirstOption();
      this.radios = this.existingAddresses.map((existingAddress, index) => {
        if (existingAddress.isAddressSelected) {
          if (
            this.modalData.suggestedNewAddresses ||
            (this.personInfo &&
              this.checkContactInfo() &&
              this.editSuggestedAddress)
          ) {
            this.personInfo = { ...this.personInfo, ...existingAddress };
          } else {
            this.personInfo = JSON.parse(JSON.stringify(existingAddress));
          }
          this.personInfo.AddressID = this.personInfo.AddressID
            ? this.personInfo.AddressID
            : '';
          this.prevPersonInfo = JSON.parse(JSON.stringify(this.personInfo));
          this.addressType = this.personInfo.AddressID;
          if (this.personInfo.PersonInfoKey) {
            this.checkOptions();
          }
        }
        if (
          existingAddress.IsDefaultBillTo &&
          existingAddress.IsDefaultBillTo === 'Y'
        ) {
          existingAddress.defaultBilling = true;
        }
        if (
          existingAddress.IsDefaultShipTo &&
          existingAddress.IsDefaultShipTo === 'Y'
        ) {
          existingAddress.default = true;
        }
        if (
          existingAddress.IsDefaultSoldTo &&
          existingAddress.IsDefaultSoldTo === 'Y'
        ) {
          existingAddress.defaultSoldTo = true;
        }

        return {
          id: index,
          value: index,
          checked: existingAddress.isAddressSelected,
          template: this.templates['address'],
          existingAddress,
          disabled: false,
        };
      });
    }
  }

  setSelectedAddressAsFirstOption() {
    // move the selected address to the top before initialize
    if (!this.isViewInitialized) {
      const selectedAddressIndex = this.existingAddresses.findIndex(
        (i) => i.isAddressSelected
      );
      if (selectedAddressIndex > 0) {
        this.existingAddresses.unshift(
          this.existingAddresses.splice(selectedAddressIndex, 1)[0]
        );
      }
    }
  }

  checkOptions() {
    if (this.personInfo.CustomerAdditionalAddressID) {
      this.saveOption.customerProfile.value = true;
    }
    if (
      (this.personInfo.IsDefaultBillTo &&
        this.personInfo.IsDefaultBillTo === 'Y') ||
      (this.personInfo.IsDefaultShipTo &&
        this.personInfo.IsDefaultShipTo === 'Y') ||
      (this.personInfo.IsDefaultSoldTo &&
        this.personInfo.IsDefaultSoldTo === 'Y')
    ) {
      if (this.personInfo.IsDefaultShipTo === 'Y') {
        this.saveOption.defaultShipping.value = true;
      }
      if (this.personInfo.IsDefaultBillTo === 'Y') {
        this.saveOption.defaultBilling.value = true;
      }
      if (this.personInfo.IsDefaultSoldTo === 'Y') {
        this.saveOption.defaultSoldTo.value = true;
      }
    }
  }

  onRadio(event) {
    this.disableBtn = true;
    this.addressType = '';
    const selectedIndex = event.value;
    this.existingAddresses.forEach(
      (address) => (address.isAddressSelected = false)
    );
    this.existingAddresses[selectedIndex].isAddressSelected = true;
    this.personInfo = JSON.parse(
      JSON.stringify(this.existingAddresses[selectedIndex])
    );
    this.personInfo.AddressID = this.personInfo.AddressID
      ? this.personInfo.AddressID
      : '';
    this.prevPersonInfo = JSON.parse(JSON.stringify(this.personInfo));
    this.addressType = this.personInfo.AddressID;
    this.saveButtonEnabled(this.personInfo);
    this.saveOption.customerProfile.value = false;
    this.saveOption.defaultBilling.value = false;
    this.saveOption.defaultShipping.value = false;
    this.saveOption.defaultSoldTo.value = false;
    this.checkOptions();

    this.radios.forEach((radio, index) => {
      if (index === selectedIndex) radio.checked = true;
      else radio.checked = false;
    });
  }

  enableSaveButton(event) {
    if (!(event && event.key)) {
      if (
        (this.personInfo.isOrderBillingAddress ||
          this.personInfo.isOrderShippingAddress) &&
        isEqual(this.prevPersonInfo, this.personInfo) &&
        !this.saveOption.defaultShipping.value &&
        !this.saveOption.defaultBilling.value &&
        !this.saveOption.defaultSoldTo.value &&
        !this.saveOption.customerProfile.value &&
        this.addressType === this.personInfo.AddressID
      ) {
        this.disableBtn = true;
      } else {
        this.disableBtn = false;
      }
      this.clearSuggestedAddress();
    }
  }

  clearSuggestedAddress() {
    // if another address is edited, then clear suggested address of the previous address
    if (
      this.userSelectedAddress &&
      this.userSelectedAddressPersonInfoKey !== this.personInfo.PersonInfoKey
    ) {
      this.userSelectedAddress = [];
      this.userSelectedAddressPersonInfoKey = '';
      this.modalData.suggestedNewAddresses = false;
      this.modalData.suggestedAddress = [];
      this.editSuggestedAddress = false;
    }
  }

  onOverrideAddressCheckbox(event: boolean) {
    // Only show override checkbox if user has permission
    this.skipAddressCheck = event;
  }

  createVerifyAddressInput(modifiedAddress, addressFields) {
    const apiInput = {
      PersonInfo: {
        manageCustomerParams: {
          default: false,
          saveToCustomer: this.saveOption.customerProfile.value,
          isForCustomerProfile: this.isForCustomerProfile,
          Customer: {
            Operation: 'Manage',
            CustomerKey: addressFields.customerKey,
            OrganizationCode: addressFields.OrganizationCode,
            CustomerAdditionalAddressList: {},
            CustomerContactList: {
              CustomerContact: [
                {
                  CustomerContactID: this.customerContactID,
                  CustomerAdditionalAddressList: {
                    CustomerAdditionalAddress: [
                      {
                        CustomerAdditionalAddressID:
                          this.personInfo.newAddress ||
                          this.modalData.suggestedNewAddresses
                            ? ''
                            : this.personInfo.CustomerAdditionalAddressID,
                        IsShipTo: 'Y',
                        IsBillTo: 'Y',
                        IsSoldTo: 'Y',
                        IsDefaultShipTo: this.saveOption.defaultShipping.value
                          ? 'Y'
                          : 'N',
                        IsDefaultBillTo: this.saveOption.defaultBilling.value
                          ? 'Y'
                          : 'N',
                        IsDefaultSoldTo: this.saveOption.defaultSoldTo.value
                          ? 'Y'
                          : 'N',
                        PersonInfo: pick(modifiedAddress, this.personInfoProps),
                      },
                    ],
                  },
                },
              ],
            },
          },
        },
        EnterpriseCode: modifiedAddress.EnterpriseCode,
        IsAddressVerified: 'N',
        PersonInfoKey: modifiedAddress.PersonInfoKey,
      },
    };
    // Remove IsDefaultSoldTo if not an address for a business customer
    if (!this.isForBusinessCustomer) {
      delete apiInput.PersonInfo.manageCustomerParams.Customer
        .CustomerContactList.CustomerContact[0].CustomerAdditionalAddressList
        .CustomerAdditionalAddress[0].IsDefaultSoldTo;
    }
    if (this.modalData.suggestedNewAddresses) {
      delete apiInput.PersonInfo.manageCustomerParams.default;
      delete apiInput.PersonInfo.manageCustomerParams.saveToCustomer;
    }
    if (this.isForBusinessCustomer && !this.isForContact) {
      delete apiInput.PersonInfo.manageCustomerParams.Customer
        .CustomerContactList;
      apiInput.PersonInfo.manageCustomerParams.Customer.CustomerAdditionalAddressList =
        {
          CustomerAdditionalAddress: [
            {
              CustomerAdditionalAddressID:
                this.personInfo.newAddress ||
                this.modalData.suggestedNewAddresses
                  ? ''
                  : this.personInfo.CustomerAdditionalAddressID,
              IsShipTo: 'Y',
              IsBillTo: 'Y',
              IsSoldTo: 'Y',
              IsDefaultShipTo: this.saveOption.defaultShipping.value
                ? 'Y'
                : 'N',
              IsDefaultBillTo: this.saveOption.defaultBilling.value ? 'Y' : 'N',
              IsDefaultSoldTo: this.saveOption.defaultSoldTo.value ? 'Y' : 'N',
              PersonInfo: pick(modifiedAddress, this.personInfoProps),
            },
          ],
        };
    } else {
      delete apiInput.PersonInfo.manageCustomerParams.Customer
        .CustomerAdditionalAddressList;
    }
    addressFields.addressFields.forEach((field) => {
      if (field.value && !this.ignoreProps.includes(field.key)) {
        apiInput.PersonInfo = {
          ...apiInput.PersonInfo,
          [field.key]: field.value,
        };
      }
    });
    return apiInput;
  }

  createManageCustomerInputForCustomer(modifiedAddress, addressFields) {
    modifiedAddress = omit(modifiedAddress, this.ignoreProps);
    const apiInput = {
      Customer: {
        Operation: 'Manage',
        CustomerKey: addressFields.customerKey,
        OrganizationCode: addressFields.OrganizationCode,
        CustomerAdditionalAddressList: {
          CustomerAdditionalAddress: [
            {
              CustomerAdditionalAddressID: this.personInfo.newAddress
                ? ''
                : this.personInfo.CustomerAdditionalAddressID,
              IsShipTo: 'Y',
              IsBillTo: 'Y',
              IsSoldTo: 'Y',
              IsDefaultShipTo: this.saveOption.defaultShipping.value
                ? 'Y'
                : 'N',
              IsDefaultBillTo: this.saveOption.defaultBilling.value ? 'Y' : 'N',
              IsDefaultSoldTo: this.saveOption.defaultSoldTo.value ? 'Y' : 'N',
              PersonInfo: pick(modifiedAddress, this.personInfoProps),
            },
          ],
        },
      },
    };
    // Remove IsDefaultSoldTo if not an address for a business customer
    if (!this.isForBusinessCustomer) {
      delete apiInput.Customer.CustomerAdditionalAddressList
        .CustomerAdditionalAddress[0].IsDefaultSoldTo;
    }
    return apiInput;
  }

  createManageCustomerInputForContact(modifiedAddress, addressFields) {
    modifiedAddress = omit(modifiedAddress, this.ignoreProps);
    const apiInput = {
      Customer: {
        Operation: 'Manage',
        CustomerKey: addressFields.customerKey,
        OrganizationCode: addressFields.OrganizationCode,
        CustomerContactList: {
          CustomerContact: [
            {
              CustomerContactID: this.customerContactID,
              CustomerAdditionalAddressList: {
                CustomerAdditionalAddress: [
                  {
                    CustomerAdditionalAddressID: this.personInfo.newAddress
                      ? ''
                      : this.personInfo.CustomerAdditionalAddressID,
                    IsShipTo: 'Y',
                    IsBillTo: 'Y',
                    IsSoldTo: 'Y',
                    IsDefaultShipTo: this.saveOption.defaultShipping.value
                      ? 'Y'
                      : 'N',
                    IsDefaultBillTo: this.saveOption.defaultBilling.value
                      ? 'Y'
                      : 'N',
                    IsDefaultSoldTo: this.saveOption.defaultSoldTo.value
                      ? 'Y'
                      : 'N',
                    PersonInfo: pick(modifiedAddress, this.personInfoProps),
                  },
                ],
              },
            },
          ],
        },
      },
    };
    // Remove IsDefaultSoldTo if not an address for a business customer
    if (!this.isForBusinessCustomer) {
      delete apiInput.Customer.CustomerContactList.CustomerContact[0]
        .CustomerAdditionalAddressList.CustomerAdditionalAddress[0]
        .IsDefaultSoldTo;
    }
    return apiInput;
  }

  getUserInputAddress() {
    const isAddressAlreadySelected = this.existingAddresses?.some(
      (obj) => obj.isAddressSelected
    );
    const personInfo = {
      AddressID:
        this.addressFields.find((f) => f.key === 'AddressID')?.value ||
        'Default',
      AddressLine1: this.addressFields.find((f) => f.key === 'AddressLine1')
        .value,
      AddressLine2: this.addressFields.find((f) => f.key === 'AddressLine2')
        .value,
      City: this.addressFields.find((f) => f.key === 'City').value,
      State: this.addressFields.find((f) => f.key === 'State').value,
      ZipCode: this.addressFields.find((f) => f.key === 'ZipCode').value,
      Country: this.addressFields.find((f) => f.key === 'Country').value,
      FirstName: this.addressFields.find((f) => f.key === 'FirstName').value,
      LastName: this.addressFields.find((f) => f.key === 'LastName').value,
      EMailID: this.addressFields.find((f) => f.key === 'EMailID').value,
      DayPhone: this.addressFields.find((f) => f.key === 'DayPhone').value,
      isOrderShippingAddress: this.personInfo.isOrderShippingAddress === 'Y',
      isOrderBillingAddress: this.personInfo.isOrderBillingAddress === 'Y',
      isAddressSelected: !isAddressAlreadySelected,
    };
    return personInfo;
  }

  massageData(address) {
    const result = [];
    address.addressFields.forEach((item) => {
      const newItem = {
        key: item.key,
        value: address[item.key],
        isCountry: item.key === 'Country',
        isState: item.key === 'State',
        isTitle: item.key === 'Title',
        isName: item.key === 'FirstName',
        isSurName: item.key === 'LastName',
        isZipCode: item.key === 'ZipCode',
        isContactInfo: item.isContactInfo,
        errorMessage: item.errorMessage,
      };
      result.push(newItem);
    });
    return result;
  }

  async handleManageCustomerResponse(modifiedAddress, addressField, resp) {
    if (resp.Customer?.CustomerKey) {
      await this.saveModalCall(modifiedAddress, addressField, 'Y');
    } else if (resp.PersonInfoList?.Status !== Constants.STATUS_VERIFIED) {
      this.showNotification(
        this.nlsMap['EDIT_ADDRESS.MSG_FAILED_CUSTOMER_UPDATE']
      );
    }
  }

  async handleResponse(modifiedAddress, addressField, resp?) {
    const respStatus = resp?.PersonInfoList?.Status || resp?.Customer?.Status;
    const acceptedStatuses = [
      Constants.STATUS_VERIFIED,
      Constants.STATUS_UE_MISSING,
      Constants.STATUS_AVS_DOWN,
      Constants.STATUS_VERIFIED,
    ];
    if (
      resp?.Customer?.CustomerKey ||
      acceptedStatuses.some((status) => respStatus === status) ||
      this.skipAddressCheck ||
      (this.personInfo.IsAddressVerified === 'Y' &&
        this.modalData.suggestedNewAddresses)
    ) {
      if (
        (resp?.PersonInfoList?.TotalNumberOfRecords > 1 ||
          (resp?.PersonInfoList?.TotalNumberOfRecords == 1 &&
            resp?.PersonInfoList.ProceedWithSingleAVSResult === 'N') ||
          this.skipAddressCheck) &&
        respStatus === Constants.STATUS_VERIFIED
      ) {
        this.modalData.suggestedAddress = resp?.PersonInfoList.PersonInfo.map(
          (obj) => {
            const {
              Company,
              EveningPhone,
              Title,
              DayPhone,
              FirstName,
              LastName,
              MobilePhone,
              EMailID,
              PersonInfoKey,
              PersonID,
              AddressLine3,
              ...filteredObj
            } = obj;
            return filteredObj;
          }
        );

        this.modalData.suggestedNewAddresses = true;
        this.suggestedAddressSelected = false;
        if (this.userSelectedAddress.length === 0) {
          const personInfo = { ...this.personInfo };
          const keysToRemove = [
            'PersonInfo',
            'Company',
            'EveningPhone',
            'EMailID',
            'Title',
            'PersonInfoKey',
            'PersonID',
            'FirstName',
            'LastName',
            'MobilePhone',
            'DayPhone',
            'AddressLine3',
          ];
          keysToRemove.forEach((key) => delete personInfo[key]);
          this.userSelectedAddress = personInfo;
          this.userSelectedAddressPersonInfoKey = this.personInfo.PersonInfoKey;
          this.editSuggestedAddress = false;
        }
        this.showNotification(
          this.nlsMap['EDIT_ADDRESS.ERROR_SUGGESTED_ADDRESS_MSG']
        );
        this.disableBtn = false;
        this.saveOption.customerProfile.value = false;
        this.saveOption.defaultBilling.value = false;
        this.saveOption.defaultShipping.value = false;
        this.saveOption.defaultSoldTo.value = false;
        this.initRadio();
        return;
      } else if (
        resp?.PersonInfoList?.TotalNumberOfRecords == 1 &&
        resp?.PersonInfoList.ProceedWithSingleAVSResult === 'Y'
      ) {
        this.overrideAddress(modifiedAddress, addressField, resp);
      }
      await this.saveModalCall(
        modifiedAddress,
        addressField,
        respStatus
          ? respStatus === Constants.STATUS_VERIFIED
            ? 'Y'
            : 'N'
          : this.personInfo.IsAddressVerified
      );
    } else if (resp?.PersonInfoList?.Status !== Constants.STATUS_VERIFIED) {
      // Only show override checkbox if user has permission
      if (
        BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(
          this.resourceIds.OVERRIDE_ADDRESS_VERIFICATION
        )
      ) {
        this.showAddressBypass = true;
      }
      this.showNotification(this.nlsMap['EDIT_ADDRESS.MSG_INVALID_ADDRESS']);
    }
  }

  overrideAddress(modifiedAddress, addressField, resp) {
    assign(
      modifiedAddress,
      pick(resp.PersonInfoList.PersonInfo[0], keys(modifiedAddress))
    );
    addressField.addressFields
      .filter((i) => keys(modifiedAddress).includes(i.key))
      .forEach((i) => (i.value = modifiedAddress[i.key]));
  }

  //EOMS-3128 : Mandatory Fields
  async verifyShippingAddress(addressFields) {
    let canSubmit = true;
    let modifiedAddress = {};
    addressFields.addressFields.forEach((field) => {
      if (addressFields.mandatoryFields.includes(field.key) && !field.value) {
        field.errorMessage =
          this.nlsMap['EDIT_ADDRESS.ERROR_MESSAGE.REQUIRED_FIELD'];
        canSubmit = false;
      }
      if (field.key === 'EMailID') {
        if (!field.value) {
          field.errorMessage = this.translate.instant('EDIT_ADDRESS.ERROR_MESSAGE.REQUIRED_FIELD');
          canSubmit = false;
        } else if (!isEmailValid(field.value)) {
          field.errorMessage = this.translate.instant('EDIT_ADDRESS.ERROR_MESSAGE.LABEL_INVALID_FORMAT');
          canSubmit = false;
        }
      }

      if (field.key === 'FirstName' && !field.value) {
        field.errorMessage = this.translate.instant(
          'EDIT_ADDRESS.ERROR_MESSAGE.REQUIRED_FIELD'
        );
        canSubmit = false;
      }
      if (field.key === 'LastName' && !field.value) {
        field.errorMessage = this.translate.instant(
          'EDIT_ADDRESS.ERROR_MESSAGE.REQUIRED_FIELD'
        );
        canSubmit = false;
      }
      if (field.key === 'DayPhone' && !field.value) {
        field.errorMessage = this.translate.instant(
          'EDIT_ADDRESS.ERROR_MESSAGE.REQUIRED_FIELD'
        );
        canSubmit = false;
      }
      if (field.key === 'AddressLine1' && !field.value) {
        field.errorMessage = this.translate.instant(
          'EDIT_ADDRESS.ERROR_MESSAGE.REQUIRED_FIELD'
        );
        canSubmit = false;
      }
      if (field.key === 'City' && !field.value) {
        field.errorMessage = this.translate.instant(
          'EDIT_ADDRESS.ERROR_MESSAGE.REQUIRED_FIELD'
        );
        canSubmit = false;
      }
    });
    const checkIfPersonKeyToSend =
      (this.personInfo.isOrderBillingAddress ||
        this.personInfo.isOrderShippingAddress ||
        this.personInfo.orderline) &&
      this.saveOption.customerProfile.value;
    const checkIfOrderIsCustomerAddress =
      (this.personInfo.isOrderBillingAddress ||
        this.personInfo.isOrderShippingAddress) &&
      this.personInfo?.CustomerAdditionalAddressID;
    this.suggestedAddressSelected = this.modalData.suggestedNewAddresses
      ? this.suggestedAddressSelected
      : true;
    if (canSubmit && this.suggestedAddressSelected) {
      modifiedAddress = {
        EnterpriseCode: this.organizationCode,
        IsAddressVerified: !isEmpty(this.userSelectedAddress) ? 'Y' : 'N',
        AddressID: this.addressType,
        PersonInfoKey:
          (!checkIfPersonKeyToSend || checkIfOrderIsCustomerAddress) &&
          this.personInfo.PersonInfoKey
            ? this.personInfo.PersonInfoKey
            : '',
      };
      addressFields.addressFields.forEach((field) => {
        if (field.value) {
          modifiedAddress = { ...modifiedAddress, [field.key]: field.value };
        }
      });
      try {
        const { showConfirmAddress, confirmLabel } =
          this.showConfirmAddressModal();
        if (showConfirmAddress) {
          this.openConfirmSaveAddressModal(
            confirmLabel,
            modifiedAddress,
            addressFields
          );
        } else {
          this.verifyAndSaveAddress(modifiedAddress, addressFields);
        }
      } catch (e) {
        this.showNotification(
          this.nlsMap['EDIT_ADDRESS.MSG_FAILED_SHIPMENTADDRESS_UPDATE']
        );
      }
    }
    return canSubmit;
  }

  showConfirmAddressModal() {
    let confirmLabel = '';
    let showConfirmAddress = false;
    if (this.modalData.isDraftFlow) {
      if (this.modalData.addressToChange === 'billing') {
        confirmLabel =
          this.saveOption.defaultSoldTo.value === false
            ? 'SHIP_BILL'
            : this.saveOption.defaultShipping.value === false
            ? 'BILL_SOLDTO'
            : 'SHIP_BILL_SOLDTO';
        showConfirmAddress =
          this.saveOption.defaultShipping.value ||
          this.saveOption.defaultSoldTo.value;
      } else if (this.modalData.addressToChange === 'shipping') {
        confirmLabel =
          this.saveOption.defaultSoldTo.value === false
            ? 'SHIP_BILL'
            : this.saveOption.defaultBilling.value === false
            ? 'SHIP_SOLDTO '
            : 'SHIP_BILL_SOLDTO';
        showConfirmAddress =
          this.saveOption.defaultBilling.value ||
          this.saveOption.defaultSoldTo.value;
      } else if (this.modalData.addressToChange === 'soldto') {
        confirmLabel =
          this.saveOption.defaultBilling.value === false
            ? 'SHIP_SOLDTO'
            : this.saveOption.defaultShipping.value === false
            ? 'BILL_SOLDTO'
            : 'SHIP_BILL_SOLDTO';
        showConfirmAddress =
          this.saveOption.defaultBilling.value ||
          this.saveOption.defaultShipping.value;
      }
    }
    return { confirmLabel, showConfirmAddress };
  }

  public openConfirmSaveAddressModal(
    confirmLabel,
    modifiedAddress,
    addressFields
  ) {
    const label_confirm =
      this.nlsMap['EDIT_ADDRESS.CUSTOMER_DETAILS.LABEL_CONFIRM'];
    const label_dismiss =
      this.nlsMap['EDIT_ADDRESS.CUSTOMER_DETAILS.LABEL_DISMISS'];
    const modalData = {
      modalText: {
        header:
          this.nlsMap[
            'EDIT_ADDRESS.CUSTOMER_DETAILS.LABEL_CONFIRM_SAVE_ADDRESS_MODAL_HEADER'
          ],
        label: this.nlsMap[`EDIT_ADDRESS.CUSTOMER_DETAILS.${confirmLabel}`],
        size: 'xs',
      },
      optionOne: {
        primary: '',
        text: label_dismiss,
        tid: 'dismiss-cancel',
        callback: () => {
          return false;
        },
        callOnClose: true,
      },
      optionTwo: {
        class: {
          primary: true,
        },
        text: label_confirm,
        tid: 'confirm-cancel',
        callback: this.confirmSaveAddress.bind(
          this,
          modifiedAddress,
          addressFields
        ),
      },
    };
    this.modalService.create({
      component: CommonBinaryOptionModalComponent,
      inputs: modalData,
    });
  }

  confirmSaveAddress(modifiedAddress, addressFields) {
    this.verifyAndSaveAddress(modifiedAddress, addressFields);
  }

  async verifyAndSaveAddress(modifiedAddress, addressFields) {
    if (
      this.personInfo.IsAddressVerified === 'Y' &&
      (!this.hasAddressChanged() ||
        (this.hasAddressChanged() && this.modalData.suggestedNewAddresses)) &&
      this.saveOption.customerProfile.value
    ) {
      let inputData;
      if (this.personInfo.IsContactAddress === 'Y') {
        inputData = this.createManageCustomerInputForContact(
          modifiedAddress,
          addressFields
        );
      } else {
        inputData = this.createManageCustomerInputForCustomer(
          modifiedAddress,
          addressFields
        );
      }
      await this.commonService
        .manageCustomer(inputData)
        .then((mashupOutput) => {
          this.handleManageCustomerResponse(
            modifiedAddress,
            addressFields,
            mashupOutput
          );
        });
    } else if (
      this.personInfo.IsAddressVerified === 'N' ||
      this.hasAddressChanged()
    ) {
      if (
        this.skipAddressCheck ||
        (this.personInfo.IsAddressVerified === 'Y' &&
          this.modalData.suggestedNewAddresses)
      ) {
        this.handleResponse(modifiedAddress, addressFields);
      } else {
        const inputData = this.createVerifyAddressInput(
          modifiedAddress,
          addressFields
        );
        const mashupOutput = await this.commonService.verifyAddress(inputData);
        this.handleResponse(modifiedAddress, addressFields, mashupOutput);
      }
    } else {
      this.saveModalCall(
        modifiedAddress,
        addressFields,
        this.personInfo.IsAddressVerified
      );
    }
  }
  hasAddressChanged() {
    const props = [
      'Company',
      'AddressLine1',
      'AddressLine2',
      'City',
      'State',
      'ZipCode',
      'Country',
    ];
    const prevAddress = pick(this.prevPersonInfo, props);
    const address = pick(this.personInfo, props);
    return !isEqual(prevAddress, address);
  }

  async saveModalCall(modifiedAddress, addressField, IsAddressVerified) {
    const ignoreProps = [
      'defaultBilling',
      'default',
      'defaultSoldTo',
      'orderline',
      'IsContactAddress',
      ...(this.isForCustomerProfile
        ? []
        : [
            'IsShipTo',
            'IsBillTo',
            'IsSoldTo',
            'CustomerAdditionalAddressID',
            'IsDefaultShipTo',
            'IsDefaultBillTo',
            'IsDefaultSoldTo',
          ]),
    ];
    if (addressField && addressField.addressFields?.length > 0) {
      addressField.addressFields = addressField.addressFields.filter(
        (i) => !ignoreProps.includes(i.key)
      );
    }
    modifiedAddress = omit(modifiedAddress, ignoreProps);
    const saveParams = {
      modifiedAddress: { ...modifiedAddress, IsAddressVerified },
      addressField: addressField,
      isOrderBillingAddress: this.personInfo.isOrderBillingAddress === 'Y',
      isOrderShippingAddress: this.personInfo.isOrderShippingAddress === 'Y',
      successCall: this.closeModal.bind(this),
      isDefaultAddress: this.saveOption,
      isForContact: this.isForContact,
      customerContactID: this.customerContactID,
    };
    await this.modalData.saveCall(saveParams);
  }

  showNotification(errorMsg) {
    const notification = new BucNotificationModel({
      statusType: 'error',
      statusContent: errorMsg,
    });
    this.notificationService.send([notification]);
  }

  closeNotification() {
    this.notificationShown = false;
  }

  getContactInfo() {
    const contact = {
      FirstName: this.addressFields.find((f) => f.key === 'FirstName').value,
      LastName: this.addressFields.find((f) => f.key === 'LastName').value,
      EMailID: this.addressFields.find((f) => f.key === 'EMailID').value,
      DayPhone: this.addressFields.find((f) => f.key === 'DayPhone').value,
    };
    return contact;
  }

  async saveAddress() {
    let addressFields;
    if (
      this.modalData.suggestedNewAddresses &&
      !isEmpty(this.userSelectedAddress)
    ) {
      let result = [];
      const contantInfo = this.getContactInfo();
      const personInfo = { ...this.personInfo, ...contantInfo };
      Object.entries(personInfo).forEach(([key, value]) => {
        const newItem = {
          key: key,
          value: value,
          isCountry: key === 'Country',
          isState: key === 'State',
          isTitle: key === 'Title',
          isName: key === 'FirstName',
          isSurName: key === 'LastName',
          isZipCode: key === 'ZipCode',
          isContactInfo: key === 'DayPhone' || key === 'EMailID',
          errorMessage: '',
        };
        result.push(newItem);
      });
      result = _.reject(
        result,
        (obj) =>
          obj.key === 'isAddressSelected' ||
          obj.key === 'isOrderShippingAddress' ||
          obj.key === 'isOrderBillingAddress' ||
          obj.key === 'newAddress' ||
          obj.key === 'EnterpriseCode'
      );
      addressFields = result;
    }
    const params = {
      addressFields: !isEmpty(addressFields)
        ? addressFields
        : this.addressFields,
      mandatoryFields: this.mandatoryFields,
      numberFields: this.numberFields,
      addressType: this.addressType,
      successCall: this.closeModal.bind(this),
      customerKey: this.customerKey,
      OrganizationCode: this.organizationCode,
    };
    await this.verifyShippingAddress(params);
  }

  // Pass data to caller with PersonInfoKey
  deleteSelectedAddress() {
    const params = {
      addressType: this.addressType,
      successCall: this.closeModal.bind(this),
      customerKey: this.customerKey,
      OrganizationCode: this.organizationCode,
      PersonInfoKey: this.personInfo.PersonInfoKey,
      isForContact: this.isForContact,
      customerContactID: this.customerContactID,
    };
    this.modalData?.deleteCall(params);
  }

  // Updates saveOptions so they can be passed to users via params
  onCheckbox(option, event): void {
    this.saveOption[option].value = event && event.checked;
    this.enableSaveButton(event);
  }

  addNewAddress() {
    this.modalData.suggestedNewAddresses = false;
    this.modalData.suggestedAddress = [];
    if (
      !this.existingAddresses.find(
        (existingAddress) => existingAddress.newAddress
      )
    ) {
      this.disableLink = true;
      this.disableBtn = false;
      this.existingAddresses.forEach((obj) => {
        obj.isAddressSelected = false;
      });
      this.existingAddresses.unshift({
        newAddress: true,
        PersonInfoKey: '',
        isAddressSelected: true,
      });
      // Set all radio values to false
      this.saveOption.customerProfile.value = false;
      this.saveOption.defaultBilling.value = false;
      this.saveOption.defaultShipping.value = false;
      this.saveOption.defaultSoldTo.value = false;
      this.initRadio();
    }
  }

  cancel(): void {
    if (typeof this.modalData.onCancel === 'function') {
      this.modalData.onCancel();
    }
    super.closeModal();
  }
}
