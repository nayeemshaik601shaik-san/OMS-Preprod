/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2022, 2024
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ChangeDetectorRef,
  TemplateRef,
  ViewContainerRef,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import {
  CCNotificationService,
  TileGroupComponent,
  getArray,
} from '@buc/common-components';
import {
  CustomerDetailsChange,
  CustomerService,
  ShippingAddressComponent,
  CustomerAddressSelectionComponent,
  PaymentDetailsComponent,
} from '@buc/cc-components';
import {
  BreadcrumbService,
  getPathFromRoot,
  Constants,
  getFullName,
} from '@call-center/customer-shared';
import { TranslateService } from '@ngx-translate/core';
import {
  BucBaseUtil,
  BucSvcAngularStaticAppInfoFacadeUtil,
  CallCenterNavigationService,
  getPostMessageDomain,
} from '@buc/svc-angular';
import {
  merge,
  mergeWith,
  cloneDeep,
  unionBy,
  remove,
  get,
  isEmpty,
} from 'lodash';
import { Subscription } from 'rxjs';
import { ExtensionConstants } from '../../extension.constants';
import { ModalService } from 'carbon-components-angular';
import { Store } from '@ngrx/store';
import * as CustomerActions from '../../state/create-customer.action';
import { skip } from 'rxjs/operators';

@Component({
  selector: 'call-center-customer-create',
  templateUrl: './customer-create.component.html',
  styleUrls: ['./customer-create.component.scss'],
})
export class CustomerCreateComponent implements OnInit, OnDestroy {
  public componentId = 'call-center-customer-create';
  @ViewChild(ShippingAddressComponent) shippingTemp: ShippingAddressComponent;
  @ViewChild(PaymentDetailsComponent)
  paymentAndBillingTemp: PaymentDetailsComponent;
  @ViewChild('tileGroup', { static: false }) tileGroupRef!: TileGroupComponent;
  shippingCanSubmit = true;
  billingCanSubmit = true;
  readonly CUSTOMER_OBJ_INIT = {
    Customer: {
      CustomerType: '02',
      CustomerContactList: {
        CustomerContact: {
          FirstName: '',
          LastName: '',
          EmailID: '',
        },
      },
      OrganizationCode: '',
      Status: Constants.CUSTOMER_STATUS.ACTIVE,
    },
  };
  collectionSub: Subscription;
  customerObj: CustomerDetailsChange = cloneDeep(this.CUSTOMER_OBJ_INIT);
  actionSub: Subscription;

  public isScreenInitialized = false;
  public isContentInitialized = false;
  public isSubmitting = false;
  public breadCrumbList: any[];

  userLocale = '';
  omsUserLoginId = '';
  openedFromSameTab = false;
  isOverrideShipTo = false;
  isOverrideBillTo = false;
  showOverrideShipToCheckbox = false;
  showOverrideBillToCheckbox = false;
  EXTENSION = {
    TOP: ExtensionConstants.CUSTOMER_CREATE_CD_TOP,
    BOTTOM: ExtensionConstants.CUSTOMER_CREATE_CD_BOTTOM,
  };
  businessEnabled: boolean = false;
  readonly resourceIds = {
    OVERRIDE_ADDRESS_VERIFICATION: 'ICC000054',
    CREATE_BUSINESS_CUSTOMER: 'ICC000055',
  };

  showSuggestedShippingAddress: boolean = false;
  showSuggestedBillingAddress: boolean = false;
  suggestedShippingAddressList = [];
  suggestedBillingAddressList = [];
  useSuggestedAddress = false;
  billingSameAsShipping = false;
  personInfoShipTo;
  personInfoBillTo;
  personInfoContact;
  parentForm: any = null;
  consumerSelected: boolean = false;
  businessSelected: boolean = false;
  customerTypeOptions = [];

  protected readonly nlsMap: any = {
    'CUSTOMER_CREATE.GENERAL.TITLE': '',
    'CUSTOMER_CREATE.GENERAL.MSG_CREATE_ERROR': '',
    'CUSTOMER_CREATE.GENERAL.MSG_ERROR_DUPLICATE_ID': '',
    'CUSTOMER_CREATE.GENERAL.LABEL_CANCEL_CREATE': '',
    'CUSTOMER_CREATE.GENERAL.MSG_CANCEL_CREATE': '',
    'CUSTOMER_CREATE.GENERAL.LABEL_CANCEL': '',
    'CUSTOMER_CREATE.GENERAL.LABEL_CONFIRM': '',
    'CUSTOMER_CREATE.GENERAL.MSG_ADDRESS_ERROR': '',
    'CUSTOMER_CREATE.GENERAL.LABEL_FULL_NAME': '',
    'CUSTOMER_CREATE.GENERAL.ERROR_SUGGESTED_ADDRESS': '',
    'CUSTOMER_CREATE.GENERAL.ERROR_SUGGESTED_ADDRESS_MSG': '',
    'CUSTOMER_CREATE.GENERAL.MODIFY_SHIPPING': '',
    'CUSTOMER_CREATE.GENERAL.MODIFY_BILLING': '',
  };
  selectedSuggestedShipAddress: any;
  selectedSuggestedBillAddress: any;
  tabNavigationData: any;

  // TemplateRefs
  @ViewChild('radioLabelTpl', { static: true }) radioLabelTpl: TemplateRef<any>;
  personInfoShipToAddressVerified: boolean = false;
  personInfoBillToAddressVerified: boolean = false;

  constructor(
    private translate: TranslateService,
    private activatedRoute: ActivatedRoute,
    private bcSvc: BreadcrumbService,
    private modalService: ModalService,
    private store$: Store,
    public notificationService: CCNotificationService,
    public ccNavigationSvc: CallCenterNavigationService,
    private cd: ChangeDetectorRef,
    private viewContainerRef: ViewContainerRef
  ) {
    const sessionId =
      BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    const uniqueId = activatedRoute.snapshot.queryParams.uniqueId;

    // Inform hub to notify when user navigates away from the tab
    window.postMessage(
      {
        action: 'NOTIFY_TAB_NAVIGATION_REQUEST',
        data: { sessionId, uniqueId },
      },
      getPostMessageDomain()
    );

    // listen to navigation message from hub and ask user for confirmation
    window.addEventListener('message', ({ data, origin }) => {
      const evtSessionId = get(data, 'data.sessionId');
      const evtUniqueId = get(data, 'data.uniqueId');
      const action = data.action;
      if (
        parseInt(evtSessionId) === parseInt(sessionId) &&
        evtUniqueId === uniqueId &&
        action === 'NOTIFY_TAB_NAVIGATION'
      ) {
        this.setTabNavigationData(data.data);
        this.cancelCreate();
      }
    });
  }

  ngOnInit() {
    this.notificationService.registerViewContainerRef(this.viewContainerRef);
    this.initialize();
    this.actionSub = this.store$
      .select(CustomerActions.selectCompleteValidationResponse)
      // Skipping initial state entry
      .pipe(skip(1))
      .subscribe((res) => {
        if (!isEmpty(res)) {
          this.handleCreateConsumerAction(res);
        }
      });
  }

  async prepareBreadcrumbList() {
    const c = this.nlsMap['CUSTOMER_CREATE.GENERAL.TITLE'];
    const r = getPathFromRoot(this.activatedRoute.snapshot);
    this.bcSvc.updateLast(c, r, c, [r], {
      queryParams: this.activatedRoute.snapshot.queryParams,
    });
    this.breadCrumbList = this.bcSvc.get();
  }

  async initialize() {
    this.userLocale = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale();
    this.businessEnabled =
      BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(
        this.resourceIds.CREATE_BUSINESS_CUSTOMER
      );
    if (this.businessEnabled) {
      this.prepareCustomerTypeOptions();
    }
    await this._initTranslations();
    await this.prepareBreadcrumbList();
    this._subscribeToCustomerChanges();
    this._prepareForm();

    // EOMS-1113
    this.textToUpperCase();
    this.openedFromSameTab =
      this.activatedRoute.snapshot.queryParams.updateCurTab ||
      this.activatedRoute.snapshot.queryParams.updateCurTab === 'true';
    this.consumerSelected = true;
    this.isScreenInitialized = true;
  }

  // EOMS-1113
  textToUpperCase() {
    Object.keys(this.parentForm.controls).forEach((controlName) => {
      const control = this.parentForm.get(controlName);
      if (control) {
        control.valueChanges.subscribe((value) => {
          if (typeof value === 'string') {
            const updatedValue = value.toUpperCase(); // Convert to uppercase
            control.setValue(updatedValue, { emitEvent: false });
          }
        });
      }
    });
  }

  setTabNavigationData(data: any) {
    if (isEmpty(data)) {
      this.tabNavigationData = undefined;
    } else {
      this.tabNavigationData = { ...this.tabNavigationData, ...data };
    }
  }

  private async _initTranslations(): Promise<any> {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translate.get(keys).toPromise();
    keys.forEach((k) => (this.nlsMap[k] = json[k]));
  }

  private async _getNls(key, params?): Promise<any> {
    return this.translate.get(key, params).toPromise();
  }

  prepareCustomerTypeOptions() {
    this.customerTypeOptions = [
      {
        id: Constants.CUSTOMER_TYPE.consumer + 'c', // need to add string to id so doesn't interfere with select customer modal radio buttons
        value: Constants.CUSTOMER_TYPE.consumer,
        template: this.radioLabelTpl,
        checked: true,
        key: 'CUSTOMER_SEARCH_MODAL.SEARCH_FORM.LABEL_CONSUMER',
      },
      {
        id: Constants.CUSTOMER_TYPE.business + 'b',
        value: Constants.CUSTOMER_TYPE.business,
        template: this.radioLabelTpl,
        checked: false,
        key: 'CUSTOMER_SEARCH_MODAL.SEARCH_FORM.LABEL_BUSINESS',
      },
    ];
  }

  onSelectCustomerTypeRadio(event) {
    this._prepareForm();
    this.consumerSelected = event.value === Constants.CUSTOMER_TYPE.consumer;
    this.businessSelected = event.value === Constants.CUSTOMER_TYPE.business;

    if (event.value === Constants.CUSTOMER_TYPE.consumer) {
      this.customerObj.Customer['CustomerType'] =
        Constants.CUSTOMER_TYPE.consumer;
    } else {
      this.customerObj.Customer['CustomerType'] =
        Constants.CUSTOMER_TYPE.business;
    }
  }

  async createProfile() {
    if (this.shippingTemp) {
      this.shippingCanSubmit = await this.shippingTemp.canCreate();
    }
    if (this.paymentAndBillingTemp) {
      this.billingCanSubmit = await this.paymentAndBillingTemp.canCreate();
    }

    if (this.isSubmitting) {
      return;
    }
    if (this.selectedSuggestedShipAddress) {
      this.personInfoShipTo = this.selectedSuggestedShipAddress;
    }
    if (this.selectedSuggestedBillAddress) {
      this.personInfoBillTo = this.selectedSuggestedBillAddress;
    }

    // prompt required fields in the parent and child group
    if (this.parentForm?.controls) {
      Object.keys(this.parentForm.controls).forEach((field) => {
        const control = this.parentForm.get(field);
        control.markAsTouched({ onlySelf: true });
      });
      if (this.parentForm.controls['enterprise']) {
        this.parentForm.controls['enterprise'].markAsDirty();
      }
    }

    if (this.parentForm?.valid) {
      if (this.shippingCanSubmit && this.billingCanSubmit) {
        this.isSubmitting = true;

        const customer = cloneDeep(this.customerObj);
        //Append or replace shipping and billing addresses in AdditionalAddressList
        let collectCustomers = unionBy(
          customer?.Customer?.CustomerContactList?.CustomerContact
            ?.CustomerAdditionalAddressList?.CustomerAdditionalAddress,
          this.shippingTemp?.customerContact.Customer?.CustomerContactList
            ?.CustomerContact?.CustomerAdditionalAddressList
            ?.CustomerAdditionalAddress,
          'CustomerAdditionalAddressID'
        );
        //Remove any empty addresses
        var removed = remove(collectCustomers, function (n) {
          return (
            n.PersonInfo?.AddressLine1 &&
            n.PersonInfo?.AddressLine1 &&
            n.PersonInfo?.ZipCode &&
            n.PersonInfo?.City &&
            n.PersonInfo?.State &&
            n.PersonInfo?.Country
          );
        });
        customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress =
          removed;

        if (this.billingSameAsShipping) {
          customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress =
            customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.filter(
              (i) => i.IsShipTo === 'Y' && i.IsBillTo !== 'Y'
            );
          const shippingAddress =
            customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.find(
              (i) => i.IsShipTo === 'Y'
            );
          if (shippingAddress) {
            shippingAddress.IsDefaultBillTo = 'Y';
            shippingAddress.IsBillTo = 'Y';
            shippingAddress.IsDefaultShipTo = 'Y';
            shippingAddress.IsShipTo = 'Y';
          }
        }

        if (
          this.personInfoShipTo &&
          Object.keys(this.personInfoShipTo).length !== 0 &&
          this.useSuggestedAddress
        ) {
          const hasShippingAddress =
            customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.some(
              (obj) => obj.CustomerAdditionalAddressID === 'ShippingAddress'
            );
          if (!hasShippingAddress) {
            delete this.personInfoShipTo.isAddressSelected;

            let personInfoShipTo = this.personInfoShipTo;
            if (this.personInfoContact && this.personInfoContact.length === 0) {
              this.personInfoContact = this.getContactInfo();
              personInfoShipTo = {
                ...this.personInfoShipTo,
                ...this.personInfoContact,
              };
            }

            customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.push(
              {
                PersonInfo: personInfoShipTo,
                CustomerAdditionalAddressID: 'ShippingAddress',
                IsDefaultShipTo: 'Y',
                IsShipTo: 'Y',
              }
            );
          } else {
            let shippingAddressIndex =
              customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.findIndex(
                (obj) => obj.CustomerAdditionalAddressID === 'ShippingAddress'
              );
            if (shippingAddressIndex !== -1) {
              customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress[
                shippingAddressIndex
              ].PersonInfo = this.personInfoShipTo;
            }
          }
        }
        if (
          this.personInfoBillTo &&
          Object.keys(this.personInfoBillTo).length !== 0 &&
          this.useSuggestedAddress
        ) {
          const hasBillingAddress =
            customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.some(
              (obj) => obj.CustomerAdditionalAddressID === 'BillingAddress'
            );
          if (!hasBillingAddress) {
            delete this.personInfoBillTo.isAddressSelected;

            let personInfoBillTo = this.personInfoBillTo;
            if (this.personInfoContact && this.personInfoContact.length === 0) {
              this.personInfoContact = this.getContactInfo();
              personInfoBillTo = {
                ...this.personInfoBillTo,
                ...this.personInfoContact,
              };
            }

            customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.push(
              {
                PersonInfo: personInfoBillTo,
                CustomerAdditionalAddressID: 'BillingAddress',
                IsDefaultBillTo: 'Y',
                IsBillTo: 'Y',
              }
            );
            this.removeDefaultBillingFlagFromShippingAddress(customer);
          } else {
            let billingAddressIndex =
              customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.findIndex(
                (obj) => obj.CustomerAdditionalAddressID === 'BillingAddress'
              );
            if (billingAddressIndex !== -1) {
              customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress[
                billingAddressIndex
              ].PersonInfo = this.personInfoBillTo;
            }
          }
        }

        this.store$.dispatch(
          CustomerActions.createConsumerProfile({
            customerObj: cloneDeep(customer),
            isOverrideShipTo: this.isOverrideShipTo,
            isOverrideBillTo: this.isOverrideBillTo,
            isShipToAddressVerified: this.personInfoShipToAddressVerified,
            isBillToAddressVerified: this.personInfoBillToAddressVerified,
          })
        );
      }
    }
  }

  removeDefaultBillingFlagFromShippingAddress(customer) {
    const shippingAddress =
      customer.Customer.CustomerContactList.CustomerContact.CustomerAdditionalAddressList.CustomerAdditionalAddress.find(
        (obj) => obj.CustomerAdditionalAddressID === 'ShippingAddress'
      );
    if (shippingAddress) {
      delete shippingAddress.IsDefaultBillTo;
      delete shippingAddress.IsBillTo;
    }
  }

  cancelCreate() {
    this.store$.dispatch(
      CustomerActions.cancelConsumerProfile({
        header: this.nlsMap['CUSTOMER_CREATE.GENERAL.LABEL_CANCEL_CREATE'],
        label: this.nlsMap['CUSTOMER_CREATE.GENERAL.MSG_CANCEL_CREATE'],
        cancel: this.nlsMap['CUSTOMER_CREATE.GENERAL.LABEL_CANCEL'],
        confirm: this.nlsMap['CUSTOMER_CREATE.GENERAL.LABEL_CONFIRM'],
        openedFromSameTab: this.openedFromSameTab,
        tabNavigationData: cloneDeep(this.tabNavigationData),
      })
    );
  }

  private _subscribeToCustomerChanges() {
    if (this.collectionSub) {
      this.collectionSub.unsubscribe();
    }
    this.customerObj = cloneDeep(this.CUSTOMER_OBJ_INIT);
    this.collectionSub = CustomerService.subscribeToCustomerDetailsChanges(
      this._collectCustomerChanges.bind(this)
    );
  }

  private _collectCustomerChanges(c: {
    change: CustomerDetailsChange;
    arrayCuster?: (o, n) => Array<any>;
  }) {
    const newCustomerObj = cloneDeep(this.customerObj);
    if (BucBaseUtil.isUndefinedOrNull(c.arrayCuster)) {
      merge(newCustomerObj, c.change);
    } else {
      mergeWith(newCustomerObj, c.change, c.arrayCuster);
    }
    this.customerObj = cloneDeep(newCustomerObj);

    // Trigger re render of UI so bucSummaryFieldRender pure pipe can pick up changes
    if ((c.change.Customer as any)?.OrganizationCode) {
      this.cd.detectChanges();
    }
  }

  private _prepareForm() {
    // this.parentForm = new UntypedFormGroup({});

    // EOMS-1113
    this.parentForm = new UntypedFormGroup({
      // Add form controls here dynamically if needed
      givenName: new UntypedFormControl('', Validators.required),
      surname: new UntypedFormControl('', Validators.required),
      email: new UntypedFormControl('', Validators.required),
    });

    
  }

  async handleCreateConsumerAction(params) {
    this.isSubmitting = false;
    if (params?.success && params?.customerDetails) {
      const customerDetails = params.customerDetails;
      const customerId = customerDetails.Customer.CustomerID;
      this.resetOverrideAddress();
      const fullName = await getFullName(
        customerDetails.Customer,
        this.translate
      );
      const prev = this.activatedRoute.snapshot.queryParams.prev;
      this.ccNavigationSvc.openUrlInSameTab(
        `${Constants.CUSTOMER_DETAILS_ROUTE}`,
        {
          enterpriseCode: customerDetails.Customer.OrganizationCode,
          title:
            customerDetails?.Customer?.CustomerType ===
            Constants.CUSTOMER_TYPE.business
              ? customerDetails.Customer?.BuyerOrganization?.OrganizationName
              : fullName
              ? fullName
              : customerId,
          customerId,
          customerKey: customerDetails.Customer.CustomerKey,
          showSuccessMsg: true,
          ...(prev ? { prev } : { external: true }),
        }
      );
    } else {
      if (
        params?.shipToVerifySuccess === false ||
        params?.billToVerifySuccess === false
      ) {
        this.notificationService.notify({
          type: 'error',
          title:
            this.nlsMap['CUSTOMER_CREATE.GENERAL.MSG_ADDRESS_ERROR'] +
            (params?.error ? ' ' + params.error : '.'),
          duration: 0, // Prevent smart closing
        });
        // Only show override checkbox if user has permission
        if (
          params.shipToVerifySuccess === false &&
          BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(
            this.resourceIds.OVERRIDE_ADDRESS_VERIFICATION
          )
        ) {
          this.showOverrideShipToCheckbox = true;
        }
        if (
          params.billToVerifySuccess === false &&
          BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(
            this.resourceIds.OVERRIDE_ADDRESS_VERIFICATION
          )
        ) {
          this.showOverrideBillToCheckbox = true;
        }
      } else if (
        params?.suggestedShipToAddressError ||
        params?.suggestedBillToAddressError
      ) {
        if (params.suggestedShipToAddressError) {
          this.showSuggestedShippingAddress = true;
          this.suggestedShippingAddressList =
            params.suggestedShippingAddressList;
          this.personInfoShipTo = params.shipTo.PersonInfo;
        }
        if (params.suggestedBillToAddressError) {
          this.showSuggestedBillingAddress = true;
          this.suggestedBillingAddressList = params.suggestedBillingAddressList;
          this.personInfoBillTo = params.billTo.PersonInfo;
        }
        this.notificationService.notify({
          type: 'error',
          title: this.nlsMap['CUSTOMER_CREATE.GENERAL.ERROR_SUGGESTED_ADDRESS'],
          message:
            this.nlsMap['CUSTOMER_CREATE.GENERAL.ERROR_SUGGESTED_ADDRESS_MSG'],
          duration: 0,
        });
      } else {
        this.resetOverrideAddress();
      }
      if (params.shipTo?.PersonInfo) {
        this.personInfoShipToAddressVerified = params.isShipAddressVerified;
      }
      if (params.billTo?.PersonInfo) {
        this.personInfoBillToAddressVerified = params.isBillAddressVerified;
      }
    }
  }

  launchAddressSelectionModal(type: 'billingAddress' | 'shippingAddress') {
    const suggestedAddress =
      type === 'billingAddress'
        ? this.suggestedBillingAddressList
        : this.suggestedShippingAddressList;
    const personInfo =
      type === 'billingAddress' ? this.personInfoBillTo : this.personInfoShipTo;
    personInfo['FirstName'] = this.parentForm.value.givenName;
    personInfo['LastName'] = this.parentForm.value.surname;
    personInfo['DayPhone'] = this.parentForm.value.dayPhone;
    personInfo['EMailID'] = this.parentForm.value.email;
    personInfo['isAddressSelected'] = true;
    const modalTitle =
      type === 'shippingAddress'
        ? this.nlsMap['CUSTOMER_CREATE.GENERAL.MODIFY_SHIPPING']
        : this.nlsMap['CUSTOMER_CREATE.GENERAL.MODIFY_BILLING'];
    type === 'billingAddress'
      ? (this.showSuggestedBillingAddress = !this.showSuggestedBillingAddress)
      : (this.showSuggestedShippingAddress =
          !this.showSuggestedShippingAddress);
    this.modalService.destroy();
    this.modalService.create({
      component: CustomerAddressSelectionComponent,
      inputs: {
        modalText: modalTitle,
        modalData: {
          addressList: [personInfo],
          EnterpriseCode: this.customerObj?.Customer['OrganizationCode'],
          customerKey: '',
          suggestedNewAddresses: suggestedAddress.length > 0,
          suggestedAddress: suggestedAddress,
          isForCustomerProfile: true,
          saveCall: (data) => this.updateAddress(data, type),
        },
      },
    });
  }

  getUserInputAddress(address) {
    const personInfo = {
      AddressID: address.find((f) => f.key === 'AddressID')?.value || 'Default',
      AddressLine1: address.find((f) => f.key === 'AddressLine1').value,
      AddressLine2: address.find((f) => f.key === 'AddressLine2').value,
      City: address.find((f) => f.key === 'City').value,
      State: address.find((f) => f.key === 'State').value,
      ZipCode: address.find((f) => f.key === 'ZipCode').value,
      Country: address.find((f) => f.key === 'Country').value,
      isOrderShippingAddress: address.isOrderShippingAddress === 'Y',
      isOrderBillingAddress: address.isOrderBillingAddress === 'Y',
    };
    return personInfo;
  }

  getContactInfo(info?) {
    let contact;
    if (info) {
      contact = {
        FirstName: info.find((f) => f.key === 'FirstName').value,
        LastName: info.find((f) => f.key === 'LastName').value,
        EMailID: info.find((f) => f.key === 'EMailID').value,
        DayPhone: info.find((f) => f.key === 'DayPhone').value,
      };
    } else {
      contact = {
        FirstName: this.parentForm.value.givenName,
        LastName: this.parentForm.value.surname,
        EMailID: this.parentForm.value.email,
        DayPhone: this.parentForm.value.dayPhone,
      };
    }

    return contact;
  }

  updateAddress(data, type) {
    const address = {};
    data.addressField?.addressFields.forEach((obj) => {
      address[obj.key] = obj.value;
    });
    const contantInfo = this.getContactInfo(data.addressField?.addressFields);
    this.personInfoContact = contantInfo;
    this.parentForm.patchValue({
      givenName: contantInfo['FirstName'],
      surname: contantInfo['LastName'],
      dayPhone: contantInfo['DayPhone'],
      email: contantInfo['EMailID'],
    });
    const propertiesToCheck = [
      { targetProperty: 'FirstName' },
      { targetProperty: 'LastName' },
      { targetProperty: 'DayPhone' },
      { targetProperty: 'EMailID' },
    ];

    propertiesToCheck.forEach((property) => {
      if (
        this.customerObj.Customer['CustomerContactList'].CustomerContact[
          property.targetProperty
        ]
      ) {
        this.customerObj.Customer['CustomerContactList'].CustomerContact[
          property.targetProperty
        ] = contantInfo[property.targetProperty];
      }
    });

    if (type === 'shippingAddress') {
      if (
        this.suggestedShippingAddressList.length > 0 &&
        data.addressField.addressFields
      ) {
        this.suggestedShippingAddressList = [];
      }
      const personInfo = this.getUserInputAddress(
        data.addressField?.addressFields
      );
      this.updatePersonInfo(personInfo, type);
    } else {
      if (this.suggestedBillingAddressList.length > 0 && data) {
        this.suggestedBillingAddressList = [];
      }
      const personInfo = this.getUserInputAddress(
        data.addressField?.addressFields
      );
      this.updatePersonInfo(personInfo, type);
    }
    type === 'billingAddress'
      ? (this.showSuggestedBillingAddress = !this.showSuggestedBillingAddress)
      : (this.showSuggestedShippingAddress =
          !this.showSuggestedShippingAddress);
    this.useSuggestedAddress = true;
    this.modalService.destroy();
  }

  updatePersonInfo(personInfo, type?) {
    if (type === 'shippingAddress') {
      for (const [key, value] of Object.entries(this.personInfoShipTo)) {
        if (!personInfo.hasOwnProperty(key)) {
          delete this.personInfoShipTo[key];
        }
      }
      for (const [key, value] of Object.entries(personInfo)) {
        if (this.personInfoShipTo.hasOwnProperty(key)) {
          this.personInfoShipTo[key] = value;
        }
      }
    } else {
      for (const [key, value] of Object.entries(this.personInfoBillTo)) {
        if (!personInfo.hasOwnProperty(key)) {
          delete this.personInfoBillTo[key];
        }
      }
      for (const [key, value] of Object.entries(personInfo)) {
        if (this.personInfoBillTo.hasOwnProperty(key)) {
          this.personInfoBillTo[key] = value;
        }
      }
    }
  }

  async selectedSuggestedAddress(selectedAddress, type) {
    const customerContacts =
      this.customerObj.Customer['CustomerContactList'].CustomerContact;
    if (customerContacts) {
      const { DayPhone, EMailID, FirstName, LastName } = customerContacts;
      selectedAddress.value = {
        ...selectedAddress.value,
        DayPhone,
        EMailID,
        FirstName,
        LastName,
      };
    }
    if (type === 'shipping') {
      this.selectedSuggestedShipAddress = selectedAddress.value;
    }
    if (type === 'billing') {
      this.selectedSuggestedBillAddress = selectedAddress.value;
    }
    this.useSuggestedAddress = true;
  }

  onOverrideAddressCheckbox(event) {
    if (event?.type === 'SHIP') {
      this.isOverrideShipTo = event.value;
    } else if (event?.type === 'BILL') {
      this.isOverrideBillTo = event.value;
    }
  }

  onSameBillingAsShipping(event) {
    this.billingSameAsShipping = event.value === 'sameAddress';
  }

  resetOverrideAddress() {
    this.showOverrideShipToCheckbox = false;
    this.showOverrideBillToCheckbox = false;
    this.isOverrideShipTo = false;
    this.isOverrideBillTo = false;
  }

  ngOnDestroy() {
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
    if (this.collectionSub) {
      this.collectionSub.unsubscribe();
    }
  }
}
