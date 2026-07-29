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
  Component, ElementRef, EventEmitter,
  Input, OnChanges, OnDestroy, OnInit,
  Output, SimpleChanges, TemplateRef,
  ViewChild, Injector
} from '@angular/core';
import { ActionProcessorService, BucCommonQuantityFormatPipe, Constants, DisplayRulesHelperService, localeBuc2Angular, TabMessageService } from '@buc/common-components';
import { BucCommonCurrencyFormatPipe } from '@buc/common-components';
import { BucNotificationModel } from '@buc/common-components';
import { BucNotificationService } from '@buc/common-components';
import { CCNotificationService } from '@buc/common-components';
import { TranslateService } from '@ngx-translate/core';
import { ProductDetailsService } from '../data-service/product-details.service';
import { isEmpty, isEqual } from 'lodash';
import { AddToCartErrorCodes, CC_CONSTANTS, ExtensionConstants, PRODUCT_RULES } from '../../../common/constants';
import { ActionParams } from '../../../common/types';
import { Subscription, Observable, of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { BucBaseUtil, BucSvcAngularStaticAppInfoFacadeUtil, getPostMessageDomain } from '@buc/svc-angular';
import { ProductTabsComponent } from '../product-tabs/product-tabs.component';
import { EditableFieldRenderer } from '@buc/common-components';
import { getArray } from '@buc/common-components';
import { StoreDataService } from '../../../store-search-result/data-service/store-data.service';
import { OrderStoreService } from '../../../common/services/order-store.service';
import { AddToOrderService } from '../../../add-to-order/add-to-order.service';
import { AddToOrderModalComponent } from '../../../add-to-order/add-to-order.component';

@Component({
  selector: 'buc-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent extends EditableFieldRenderer implements OnInit, OnChanges, OnDestroy {

  @Input() productIdentifierInfo = {
    itemID: '',
    unitOfMeasure: '',
    defaultProductClass: '',
    selectedFulfillmentMethod: 'SHP',
    shipTo: '',
    country: '',
    countries: [],
    shipNode: {} as any,
    productQtyInCart: '',
    orderLineKey: '',
    selectedTab: '',
    orderHeaderKey: '',
    enterpriseCode: '',
    isParentItemSearchResult: false,
    productBrowsingStandalone: false,
    readFromSession: false,
    selectedVariantID: '',
    isModelItem: false,
    selectedVariant: '',
    associatedItemID: '',
    associatedItemUOM: '',
    quantity: 0,
    quantityLimitOverride: false,
    hasQuantityOverridePermission: false,
    showQtyOverrideCheckbox: false,
    parentItemID: ''
  }
  @Input() currency = '';
  @Input() rulesConfig = {
    [PRODUCT_RULES.cacheInventory]: '',
    [PRODUCT_RULES.pickupStoreEnabled]: ''
  };
  @Input() isReturnOrder = false;
  @Input() isExchangeOrder = false;
  @Input() customerInformation = {
    customerId: '',
    customerContactId: ''
  };
  @Input() isParentItemSearchResult = false;
  @Input() handleAddToCartExternally = false;
  // @Input() showGoToOrder = false;
  @Output() returnToItemSearchResult: EventEmitter<any> = new EventEmitter();
  @Output() navigateToCreateOrder: EventEmitter<any> = new EventEmitter();
  @Output() setReadFromSessionFalse: EventEmitter<any> = new EventEmitter();
  @Output() updateOrderSummary: EventEmitter<any> = new EventEmitter();
  @Output() updateFulfillmentData: EventEmitter<any> = new EventEmitter();
  @Output() handleCreateOrder: EventEmitter<any> = new EventEmitter();



  //EOMS-13713 - Changes Start
  @Input() costCenterOptions: any;
  @Input() orderReasonOptions: any;
  @Input() fulfillmentDetails: any;
  //EOMS-13713 - Changes End

  FIELD_ITEM_ID = 'itemID';
  FIELD_DESC = 'itemDesc';

  isScreenInitialized = false;
  componentId = 'ProductDetailComponent'
  fulfillmentOptions: any = [];
  productDetails: any;
  variationDetails: any;
  attributeList = [];
  allowedCombinations = [];
  selectedCombination: any = {};
  imageList = [];
  selectedVariant: any;
  isSelectedCombinationInvalid = false;
  isModelItem = false;
  isBundleItemShipIndependent = false;
  isBundleCompInStock = false;
  actionSub: Subscription;
  enterpriseCode: string;
  isItemInStock = false;
  orderLineKey = '';
  selectedFulfillmentDetails: any;
  productToRemoveFromCartKey: string;
  hasPriceOverridePermission = false;
  overridePriceInfo;
  decimalDigits: any;
  displayDoublQtyRule: boolean;
  showDetailsPage = false;
  hideTable: boolean;

  EXTENSION = {
    TOP: ExtensionConstants.PRODUCT_DETAILS_CC_TOP,
    BOTTOM: ExtensionConstants.PRODUCT_DETAILS_CC_BOTTOM
  };

  quantity = {
    orderedQty: 1,
    minQuantity: 1,
    isQuantityInvalid: false,
    quantityInvalidText: '',
    quantityLimitOverride: false,
    hasQuantityOverridePermission: false,
    showQtyOverrideCheckbox: false
  }

  showGoToOrder = false

  @ViewChild('radioLabelTpl', { static: true }) radioLabelTpl: TemplateRef<any>;
  @ViewChild('productTabs') productTabs: ElementRef;
  @ViewChild('productItemID', { static: true }) productIDTpl: TemplateRef<any>;
  @ViewChild('productDescription', { static: true }) productDescTpl: TemplateRef<any>;

  @ViewChild(ProductTabsComponent, { static: false }) productTabsComponent: ProductTabsComponent;

  readonly resourceIdAddToCart = "ICC000001";
  readonly resourceIdAddToOrder = "ICC000061";

  protected readonly nlsMap: any = {
    'PRODUCT_DETAIL.RADIO_OPTIONS.LABEL_SHIPPING': '',
    'PRODUCT_DETAIL.RADIO_OPTIONS.LABEL_PICKUP': '',
    'PRODUCT_DETAIL.NOTIFICATIONS.MSG_ADDED_TO_CART': '',
    'PRODUCT_DETAIL.NOTIFICATIONS.MSG_QTY_FAILING_VALIDATION': '',
    'PRODUCT_SEARCH.MESSAGES.MSG_PRODUCT_DETAILS_NOT_FOUND': ''
  };
  selectedTab = '';

  @Output() sendCartDetails: EventEmitter<any> = new EventEmitter();
  @Output() sendCartErrorDetails: EventEmitter<any> = new EventEmitter();
  productQtyInCart: any;

  isCountryInvalid = false;
  shipNode: any;

  lcl = localeBuc2Angular(BucSvcAngularStaticAppInfoFacadeUtil.getUserLanguage());
  public currPipe: BucCommonCurrencyFormatPipe = new BucCommonCurrencyFormatPipe(this.lcl)
  public qtyPipe: BucCommonQuantityFormatPipe = new BucCommonQuantityFormatPipe();
  
  constructor(
    private translateService: TranslateService,
    private productDetailsService: ProductDetailsService,
    private actionProcessorService: ActionProcessorService,
    private activatedRoute: ActivatedRoute,
    private ccNotificationService: CCNotificationService,
    private bucNS: BucNotificationService,
    private orderStoreService: OrderStoreService,
    private storeDataService: StoreDataService,
    private displayRuleService: DisplayRulesHelperService,
    private addToOrderService: AddToOrderService,
    private route: ActivatedRoute,
    private tabMessageService: TabMessageService,
    inj: Injector
  ) {
    super(inj)
  }

  ngOnInit(): void {
    this.initialize();
    if(this.productIdentifierInfo.productBrowsingStandalone) {
      const sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
      const uniqueId = this.route.snapshot.queryParams.uniqueId;
      this.tabMessageService.registerTabForRefocusNotification(sessionId, uniqueId, this.onTabRefocus.bind(this, sessionId, uniqueId));
    }
    this.actionSub = this.actionProcessorService.selectUpdate<any>(this.componentId).subscribe(res => {
      if (res.params.selectedStore) {
        this.handleStoreSelectionResp(res.params.selectedStore);
      }
      if (res.params.overridePrice) {
        this.overridePriceInfo = res.params.overridePrice;
      }
    })
    this.displayDoublQtyRule = this.displayRuleService.isShowDoubleQtyDisplayRule();
    this.decimalDigits = this.displayRuleService.getDecimalsDigits();
  }

  onTabRefocus(sessionId, uniqueId) {
    this.checkOpenDraftOrders();
    window.postMessage({
      action: Constants.NOTIFY_TAB_REFOCUS,
      data: { sessionId, uniqueId }
    }, getPostMessageDomain());
  }

  ngOnChanges(c: SimpleChanges): void {
    if (c.productIdentifierInfo && !c.productIdentifierInfo.isFirstChange() && c.productIdentifierInfo.currentValue !== c.productIdentifierInfo.previousValue) {
      this.initialize();
    }
  }

  ngOnDestroy() {
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
  }

  async initialize(): Promise<any> {
    this.resetValues();
    const queryParams = this.activatedRoute.snapshot.queryParams;
    this.enterpriseCode = queryParams?.sellerEnterpriseCode;
    if (!this.currency) {
      const userOrgsList = this.getUserOrgsList();
      if (userOrgsList) {
        const matchingOrg = userOrgsList.find(org => org.value === this.enterpriseCode);
        this.currency = matchingOrg?.currency;
      }
    }
    this.hasPriceOverridePermission = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource('ICC000021');
    if (!this.productIdentifierInfo.orderHeaderKey) {
      this.productIdentifierInfo.orderHeaderKey = queryParams?.orderHeaderKey;
    }
    if (this.productIdentifierInfo.enterpriseCode) {
      this.enterpriseCode = this.productIdentifierInfo.enterpriseCode;
    }
    // Read item data from session to load product details if this page is redirected from product browsing
    if (this.activatedRoute.snapshot.queryParams?.fromProductBrowsing === 'Y' && this.productIdentifierInfo.readFromSession) {
      this.isModelItem = this.productIdentifierInfo.isModelItem;
      this.selectedVariant = this.productIdentifierInfo.selectedVariant;
      this.selectedTab = this.productIdentifierInfo.selectedTab;
      if (this.quantity) {
        this.quantity.orderedQty = this.productIdentifierInfo.quantity;
        this.quantity.quantityLimitOverride = this.productIdentifierInfo.quantityLimitOverride;
        this.quantity.hasQuantityOverridePermission = this.productIdentifierInfo.hasQuantityOverridePermission;
        this.quantity.showQtyOverrideCheckbox = this.productIdentifierInfo.showQtyOverrideCheckbox;
      }
    }

    this._initTranslations();
    await this.getProductDetails();
    this.shipNode = this.shipNode ?? this.productIdentifierInfo.shipNode;

    this.initFulfillmentRadioOptions();

    if (this.productIdentifierInfo?.selectedVariantID) {
      const combination = { ...this.allowedCombinations.find((item) => item.ItemID === this.productIdentifierInfo.selectedVariantID) };
      this.attributeList.forEach((attribute) => {
        const option = attribute.options.find((opt) => opt.DisplayAttributeValue === combination[attribute.id])
        this.onAttributeChange(attribute, option);
      })
      this.productToRemoveFromCartKey = this.orderLineKey;
      this.orderLineKey = '';
    }

    if (this.productIdentifierInfo.selectedFulfillmentMethod === 'PICK' && !this.shipNode) {
      this.isItemInStock = false;
    } else {
      if (this.productIdentifierInfo.selectedFulfillmentMethod === 'PICK') {
        await this.fetchPickProductAvailablity();
      }
      this.checkProductAvailablity();
    }

    if (this.productIdentifierInfo.quantity) {
      this.quantity.orderedQty = this.productIdentifierInfo.quantity;
    }

    if (this.activatedRoute.snapshot.queryParams?.fromProductBrowsing === 'Y' && this.productIdentifierInfo.readFromSession) {
      // Call add to cart on load if the current page is redirected to here from product browsing
      if (!this.productIdentifierInfo.associatedItemID) {
        this.addToCart();
      }
      this.setReadFromSessionFalse.emit();
    }
    if(this.activatedRoute.snapshot.queryParams?.showDetailsPage){
      this.showDetailsPage = true;
    }
    this.checkOpenDraftOrders();
  }

  checkOpenDraftOrders() {
    this.showGoToOrder = this.productIdentifierInfo.productBrowsingStandalone ? this.addToOrderService.hasOpenCreateOrderTabs(this.enterpriseCode) : false;
  }
  public getUserOrgsList(): any { // gets org list from sessionStorage
    const keys = Object.keys(sessionStorage);
    const key = keys.find((key) => key.includes("userOrgsList"));
    const value = key ? JSON.parse(sessionStorage.getItem(key)) : null;
    return value;
  }

  afterProductTabsLoaded() {
    if (this.activatedRoute.snapshot.queryParams?.fromProductBrowsing === 'Y' && this.productIdentifierInfo.readFromSession) {
      // Call add to cart on load if the current page is redirected to here from product browsing
      if (this.productIdentifierInfo.associatedItemID && this.productIdentifierInfo.associatedItemUOM) {
        const item = this.productTabsComponent.associatedProductsTabComponent.associatedItems.find(el => {
          return (el.Item.ItemID === this.productIdentifierInfo.associatedItemID) && (el.Item.UnitOfMeasure === this.productIdentifierInfo.associatedItemUOM)
        });
        if (item && item.Item) {
          this.productTabsComponent.associatedProductsTabComponent.addToCart(item.Item);
        }
      }
      this.setReadFromSessionFalse.emit();
    }
  }

  toLowerCase(str: string = null) {
    return str ? str.toLowerCase() : str;
  }

  onQuantityChange(event) {
    if(Object.prototype.hasOwnProperty.call(event, 'value') && !event.value){
      this.quantity.isQuantityInvalid = true;
    } else if(event?.source && Object.prototype.hasOwnProperty.call(event, 'value')) {
      this.quantity.orderedQty = event?.value;
      if(this.quantity.orderedQty > 0) {
        this.quantity.isQuantityInvalid = false;
        const transformedValue = this.qtyPipe.transform(event.value, this.displayDoublQtyRule, this.decimalDigits);
        this.quantity.orderedQty = transformedValue;
      } else {
        this.quantity.isQuantityInvalid = true;
      }
    }
  }

    assignValue(event) {
    this.quantity.orderedQty = event.srcElement.value;
  }

  protected async _initTranslations(): Promise<any> {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translateService.get(keys).toPromise();
    keys.forEach(k => this.nlsMap[k] = json[k]);
  }

  private async getProductDetails() {

    const hasRules = this.rulesConfig && this.rulesConfig[PRODUCT_RULES.pickupStoreEnabled] && this.rulesConfig[PRODUCT_RULES.cacheInventory];
    const resp = await this.productDetailsService.getProductDetails(this.prepareGetCompleteItemListApiInput(), hasRules);
    if(!resp?.productDetails?.ItemList?.Item) {
        this.hideTable = true;
    }
    
    if (!hasRules) {
      this.rulesConfig[PRODUCT_RULES.pickupStoreEnabled] = resp.pickupEnabledRule?.Rules.RuleSetValue;
      this.rulesConfig[PRODUCT_RULES.cacheInventory] = resp.cacheInventoryRule?.Rules.RuleSetValue;
    }

    if (resp?.productDetails?.ItemList?.Item?.length > 0) {
      this.productDetails = resp.productDetails.ItemList.Item[0];
      const qty = parseInt(this.productDetails.PrimaryInformation.MinOrderQuantity);
      this.quantity.orderedQty = this.quantity.minQuantity = qty > 1 ? qty : 1;
      this.isModelItem = this.productDetails.PrimaryInformation.IsModelItem === CC_CONSTANTS.CHECK_YES;

      this.setAssetList(this.productDetails.AssetList?.Asset);

      if (this.isModelItem && resp.productDetails.ItemList.ItemList?.length > 0) {
        this.variationDetails = resp.productDetails.ItemList.ItemList[0];
        this.setAttributeList();
        this.setAllowedCombinationList(resp.productDetails.ItemList.ItemList[0].AllowedCombinationList.AllowedCombination ?? []);
      }

      if(isEmpty(this.productDetails.ComputedPrice)){
        this.productDetails.ComputedPrice.UnitPrice = '0';
      }

      this.isBundleItemShipIndependent = (this.productDetails.PrimaryInformation.KitCode === "BUNDLE") && (this.productDetails.PrimaryInformation.BundleFulfillmentMode === "00");
    }
    await this.initializeFieldDetailAttributes('product-details');
    await this.loadPageAttributes().toPromise();
    this.isScreenInitialized = true;
    this.selectedTab = this.productIdentifierInfo.selectedTab;
    this.productQtyInCart = this.productIdentifierInfo.productQtyInCart;
    this.orderLineKey = this.productIdentifierInfo.orderLineKey;

    if (this.selectedTab) {
      setTimeout(() => {
        this.productTabs.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
      }, 0);
    }

  }

  protected fetchPageAttributeData(): Observable<Array<any>> {
    return of(getArray(this.productDetails));
  }

  get isAddToCartDisabled() {
    if(this.quantity.isQuantityInvalid && !BucBaseUtil.isVoid(this.quantity.isQuantityInvalid)) {
      this.quantity.quantityInvalidText = this.translateService.instant('PRODUCT_DETAIL.GENERAL.NOTIF_NULL_CANCEL_QTY_SELECTED');
    }
    return this.isReturnOrder ? (this.productDetails?.PrimaryInformation?.IsValid === 'N' || this.quantity.isQuantityInvalid) 
    : (this.isModelItem && !this.selectedVariant) || (this.isBundleItemShipIndependent && !this.isBundleCompInStock) || (!this.isBundleItemShipIndependent && !this.isItemInStock)
      || !this.quantity.orderedQty || this.quantity.isQuantityInvalid
      || (!this.isBundleItemShipIndependent && this.getNumericValue(this.selectedFulfillmentDetails?.availablity?.CurrentAvailableQty) && this.quantity.orderedQty > this.getNumericValue(this.selectedFulfillmentDetails?.availablity?.CurrentAvailableQty))
      || (!this.isBundleItemShipIndependent && this.getNumericValue(this.selectedFulfillmentDetails?.availablity?.FutureAvailableQuantity) && this.quantity.orderedQty > this.getNumericValue(this.selectedFulfillmentDetails?.availablity?.FutureAvailableQuantity))
  }


  protected getDataForAttribute(id: string, item: any): Promise<any> {
    let attr: any = {};
    let data: any = '';
    switch (id) {
      case this.FIELD_ITEM_ID:
        data = { template: this.productIDTpl, templateData: { value: this.productIdentifierInfo } };
        break;
      case this.FIELD_DESC:
        data = { template: this.productDescTpl, templateData: item };
        break;
    }
    attr = { ...attr, data };
    return attr as any;
  }

  onCountrySelectionChange(item) {
    this.isCountryInvalid = false;
    this.productIdentifierInfo.country = item?.id;
    if (!isEmpty(this.productIdentifierInfo.countries)) {
      this.productIdentifierInfo.countries.forEach(el => el.selected = el.id === item.id);
    }
  }

  onCountryValSearch(evt) {
    this.isCountryInvalid = false;
    if (evt) {
      const searchedCountry = this.productIdentifierInfo.countries.find((item) => item.content.toLowerCase() === evt.toLowerCase());
      this.isCountryInvalid = !searchedCountry;
      if (searchedCountry) {
        this.productIdentifierInfo.country = searchedCountry.id;
      }
    } else {
      this.productIdentifierInfo.countries.forEach(el => el.selected = false);
      this.isCountryInvalid = true;
      this.productIdentifierInfo.country = '';
    }
  }

  private prepareGetCompleteItemListApiInput() {
    let fulfillmentMathodValue = this.productIdentifierInfo.selectedFulfillmentMethod === 'SHP' ? this.productIdentifierInfo.shipTo : this.productIdentifierInfo.shipNode?.ShipNode;
    if (this.fulfillmentOptions?.length) {
      const selectedMethod = this.fulfillmentOptions.find(el => el.checked);
      this.productIdentifierInfo.selectedFulfillmentMethod = selectedMethod.id;
      fulfillmentMathodValue = selectedMethod.id === 'SHP' ? selectedMethod.fulfillmentValue : selectedMethod.fulfillmentValue?.ShipNode;
    }
    const input = {
      Item: {
        Currency: this.currency,
        CallingOrganizationCode: this.enterpriseCode,
        GetAvailabilityFromCache: this.productIdentifierInfo.selectedFulfillmentMethod === 'PICK' ? 'N' : this.rulesConfig[PRODUCT_RULES.cacheInventory],
        UnitOfMeasure: this.productIdentifierInfo.unitOfMeasure,
        ProductClass: this.productIdentifierInfo.defaultProductClass,
        BarCode: {
          ContextualInfo: {
            EnterpriseCode: this.enterpriseCode,
            OrganizationCode: this.enterpriseCode
          },
          BarCodeData: this.productIdentifierInfo.parentItemID ? this.productIdentifierInfo.parentItemID : this.productIdentifierInfo.itemID,
        },
        ...(this.productIdentifierInfo.selectedFulfillmentMethod === 'SHP' && fulfillmentMathodValue) && {
          ShipToAddress: {
            ZipCode: fulfillmentMathodValue,
            Country: this.productIdentifierInfo.country || '',
            City: '',
            State: ''
          }
        },
        ...(this.productIdentifierInfo.selectedFulfillmentMethod === 'PICK' && fulfillmentMathodValue) && {
          ShipNodes: {
            ShipNode: {
              Node: fulfillmentMathodValue
            }
          }
        },
        ...(this.customerInformation?.customerId || this.customerInformation?.customerContactId) && {
          CustomerInformation: {
            CustomerContactID: this.customerInformation?.customerContactId,
            CustomerID: this.customerInformation?.customerId
          }
        }
      }
    };

    return input;
  }

  private prepareGetSingleStoreAvailablity() {
    const searchStoresInput = {
      AlternateStore: {
        Mode: '02',
        OrganizationCode: this.enterpriseCode,
        NodeList: {
          Node: {
            ShipNode: this.shipNode.ShipNode,
          }
        },
        OrderLines: {
          OrderLine: {
            Item: {
              ItemID: this.isModelItem ? this.selectedVariant.ItemID : this.productDetails.ItemID,
              UnitOfMeasure: this.isModelItem ? this.selectedVariant.UnitOfMeasure : this.productDetails.UnitOfMeasure,
              ProductClass: this.isModelItem ? this.selectedVariant.PrimaryInformation.DefaultProductClass : this.productDetails.PrimaryInformation.DefaultProductClass
            }
          }
        }
      }
    };
    return searchStoresInput;
  }

  private setAttributeList() {
    this.attributeList = [];
    this.variationDetails.AttributeList?.Attribute?.map((variation) => {
      this.attributeList.push({
        id: variation.AttributeID,
        sequence: variation.SequenceNo,
        label: variation.ShortDescription,
        options: variation.AssignedValueList?.AssignedValue
      })
    })
  }

  private setAllowedCombinationList(combinations) {
    this.allowedCombinations = combinations.map((combination) =>
      combination.ItemAttributeList.ItemAttribute.reduce((acc, obj) => {
        acc[obj.ItemAttributeName] = obj.DisplayAttributeValue;
        return acc;
      }, { ItemID: combination.ItemID }));
  }

  private setAssetList(assets) {
    this.imageList = []
    if (assets?.length) {
      const firstObj = assets.find(ele => {
        return ele.Type === 'ITEM_IMAGE_1';
      });
      if (firstObj) {
        this.imageList.push({
          url: firstObj.ContentLocation + '/' + firstObj.ContentID,
          active: true
        })
      } else {
        this.imageList.push({
          url: this.productDetails.PrimaryInformation?.ImageLocation + '/' + this.productDetails.PrimaryInformation?.ImageID,
          active: true
        })
      }
      assets.forEach(ele => {
        if (ele.Type === 'ITEM_IMAGE_LRG_1') {
          this.imageList.push({
            url: ele.ContentLocation + '/' + ele.ContentID,
            active: false
          });
        }
      });
    } else {
      this.imageList.push({
        url: this.productDetails.PrimaryInformation?.ImageLocation + '/' + this.productDetails.PrimaryInformation?.ImageID,
        active: true
      })
    }
  }

  private initFulfillmentRadioOptions() {
    this.fulfillmentOptions = [
      {
        id: 'SHP',
        value: 'SHP',
        template: this.radioLabelTpl,
        checked: this.productDetails?.PrimaryInformation?.IsShippingAllowed === CC_CONSTANTS.CHECK_YES && this.productIdentifierInfo.selectedFulfillmentMethod !== 'PICK',
        disabled: this.productDetails?.PrimaryInformation?.IsShippingAllowed !== CC_CONSTANTS.CHECK_YES || this.isModelItem,
        key: 'PRODUCT_DETAIL.RADIO_OPTIONS.LABEL_SHIPPING',
        link: 'PRODUCT_DETAIL.RADIO_OPTIONS.LABEL_ENTER_POSTAL_CODE',
        showLink: this.productIdentifierInfo.shipTo ? false : true,
        showEditIcon: this.productIdentifierInfo.shipTo ? true : false,
        fulfillmentValue: this.productIdentifierInfo.shipTo,
        availablity: this.productIdentifierInfo.selectedFulfillmentMethod !== 'PICK' ? this.productDetails?.Availability : {},
        showAvailablity: this.productIdentifierInfo.selectedFulfillmentMethod !== 'PICK',
        bundleCompAvailablity: false
      },
      ...(this.rulesConfig[PRODUCT_RULES.pickupStoreEnabled] === 'Y') ? [{
        id: 'PICK',
        value: 'PICK',
        template: this.radioLabelTpl,
        checked: (this.productDetails?.PrimaryInformation?.IsPickupAllowed === CC_CONSTANTS.CHECK_YES) && (this.productIdentifierInfo.selectedFulfillmentMethod === 'PICK'),
        disabled: (this.productDetails?.PrimaryInformation?.IsPickupAllowed !== CC_CONSTANTS.CHECK_YES) || this.isModelItem,
        key: 'PRODUCT_DETAIL.RADIO_OPTIONS.LABEL_PICKUP',
        link: 'PRODUCT_DETAIL.RADIO_OPTIONS.LABEL_PICKUP_STORES',
        showLink: this.productIdentifierInfo.selectedFulfillmentMethod === 'PICK' && this.shipNode?.ShipNode ? false : true,
        showEditIcon: this.productIdentifierInfo.selectedFulfillmentMethod === 'PICK' && this.shipNode?.ShipNode ? true : false,
        fulfillmentValue: this.shipNode,
        availablity: this.productIdentifierInfo.selectedFulfillmentMethod === 'PICK' ? this.productDetails?.Availability : {},
        bundleCompAvailablity: false
      }] : []
    ];
  }
  private async fetchPickProductAvailablity() {
    this.selectedFulfillmentDetails = this.fulfillmentOptions.find((el) => el.checked);

    const resp = await this.storeDataService.getSingleStoreAvailablity(this.prepareGetSingleStoreAvailablity());
    const storeNode = resp.AlternateStores?.NodeList?.Node[0];
    if (!isEmpty(storeNode.Availability)) {
      if (storeNode.Availability.IsAvailable === 'N') {
        // Out of stock
        this.selectedFulfillmentDetails.availablity.CurrentAvailableQty = 0;
        this.selectedFulfillmentDetails.availablity.FutureAvailableQuantity = 0;
      } else if (storeNode.Availability.IsFutureAvailability === 'N') {
        // In stock
        this.selectedFulfillmentDetails.availablity.CurrentAvailableQty = resp?.AlternateStores?.NodeList?.Node[0]?.Availability?.AvailableQty ?? this.selectedFulfillmentDetails.fulfillmentValue.Availability.AvailableQty;
      } else if (storeNode.Availability.IsFutureAvailability === 'Y') {
        // Available on Future date
        this.selectedFulfillmentDetails.availablity.CurrentAvailableQty = 0;
        this.selectedFulfillmentDetails.availablity.FutureAvailableQuantity = storeNode.Availability?.AvailableQty ?? this.selectedFulfillmentDetails.fulfillmentValue.Availability.AvailableQty;
        this.selectedFulfillmentDetails.availablity.FutureAvailableDate = storeNode.Availability?.AvailableDate ?? this.selectedFulfillmentDetails.fulfillmentValue.Availability.AvailableDate;
      }
    }
  }

  private checkProductAvailablity() {
    this.selectedFulfillmentDetails = this.fulfillmentOptions.find((el) => el.checked);
    if (this.isBundleItemShipIndependent) {
      this.selectedFulfillmentDetails.bundleCompAvailablity = this.isBundleCompInStock = this.productDetails?.Components?.Component?.every((item) => item.Availability &&
        parseInt(item.Availability.CurrentAvailableQty) >= parseInt(item.Item.PrimaryInformation.MinOrderQuantity));
    } else {
      this.isItemInStock = this.selectedFulfillmentDetails?.availablity && ((parseInt(this.selectedFulfillmentDetails?.availablity?.CurrentAvailableQty) >= this.quantity.minQuantity) ||
        (parseInt(this.selectedFulfillmentDetails?.availablity?.FutureAvailableQuantity) > 0));
    }
  }

  onAttributeChange(changedAttribute, selected) {
    if (changedAttribute.selectedOption !== selected.DisplayAttributeValue) {
      this.selectedCombination[changedAttribute.id] = selected.DisplayAttributeValue;
      changedAttribute.selectedOption = selected.DisplayAttributeValue;

      this.attributeList.forEach((attribute) => {
        if (attribute.id !== changedAttribute.id) {
          attribute.invalidCombination = this.checkAttributeCombination(attribute);
        }
      })

      if (Object.keys(this.selectedCombination).length === this.attributeList.length) {
        this.setSelectedVariant();
      }
    }
  }

  private checkAttributeCombination(attribute) {
    attribute.options.forEach(variationItem => {
      variationItem.invalidCombination = true;
      const newCombination = { ...this.selectedCombination, [attribute.id]: variationItem.DisplayAttributeValue };
      if (this.checkCombination(newCombination, attribute.id, variationItem.DisplayAttributeValue)) {
        variationItem.invalidCombination = false;
      }
    });
  }

  private checkCombination(newCombination: any, id, value) {
    let filteredCombinations = [...this.allowedCombinations];
    Object.keys(newCombination).forEach((key) => {
      filteredCombinations = filteredCombinations.filter((item) => item[key] === newCombination[key]);
    })
    return filteredCombinations.find((item) => item[id] === value);
  }

  private setSelectedVariant() {
    this.selectedVariant = null;
    const isValidCombination = this.allowedCombinations.find((comb) => {
      const { ItemID, ...allowedComb } = comb;
      return isEqual(allowedComb, this.selectedCombination);
    });
    if (isValidCombination) {
      this.isSelectedCombinationInvalid = false;
      this.selectedVariant = this.variationDetails.Item.find((item) => item.ItemID === isValidCombination.ItemID);
      const qty = parseInt(this.selectedVariant.PrimaryInformation.MinOrderQuantity);
      this.quantity.minQuantity = qty > 1 ? qty : 1;
      this.setAssetList(this.selectedVariant?.AssetList?.Asset);
      this.fulfillmentOptions.forEach((item) => {
        if (item.id === 'SHP') {
          item.disabled = this.selectedVariant.PrimaryInformation.IsShippingAllowed !== CC_CONSTANTS.CHECK_YES;
          item.availablity = this.selectedVariant.Availability;
          this.checkProductAvailablity();
        } else {
          item.disabled = (this.selectedVariant.PrimaryInformation.IsPickupAllowed !== CC_CONSTANTS.CHECK_YES) || (this.rulesConfig[PRODUCT_RULES.pickupStoreEnabled] === 'N');
          item.availablity = this.selectedVariant.Availability;
          this.checkProductAvailablity();
        }
      });
    } else {
      this.isSelectedCombinationInvalid = true;
      this.fulfillmentOptions.forEach((item) => item.disabled = true);
    }
  }

  private openSelectStoreModal(item) {
    const nodeInfo = this.fulfillmentOptions.find((el) => el.id === 'PICK').fulfillmentValue;
    let shipToAddress;
    if (nodeInfo) {
      shipToAddress = nodeInfo.ShipNodePersonInfo;
    } else {
      const ordDetails = this.orderStoreService.getOrderDetails();
      shipToAddress = ordDetails?.Order?.PersonInfoShipTo;
    }
    this.actionProcessorService.dispatch<ActionParams>(CC_CONSTANTS.SELECT_STORE, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          enterpriseCode: this.enterpriseCode,
          personInfoShipTo: shipToAddress,
          orderLines: {
            RequiredQty: this.quantity.orderedQty,
            Item: {
              ItemID: this.isModelItem ? this.selectedVariant.ItemID : this.productDetails.ItemID,
              UnitOfMeasure: this.isModelItem ? this.selectedVariant.UnitOfMeasure : this.productDetails.UnitOfMeasure,
              ProductClass: this.isModelItem ? this.selectedVariant.PrimaryInformation.DefaultProductClass : this.productDetails.PrimaryInformation.DefaultProductClass
            }
          },
          selectedShipNode: nodeInfo?.ShipNode,
          size: 'lg'
        },
      }
    });
  }

  handleStoreSelectionResp(selectedStore) {
    const item = this.fulfillmentOptions.find(el => el.checked);
    this.shipNode = selectedStore ?? null;
    item.fulfillmentValue = selectedStore;
    item.showLink = false;
    item.showEditIcon = true;
    if (!isEmpty(item.fulfillmentValue.Availability)) {
      if (item.fulfillmentValue.Availability.IsAvailable === 'N') {
        // Out of stock
        item.availablity.CurrentAvailableQty = 0;
        item.availablity.FutureAvailableQuantity = 0;
      } else if (item.fulfillmentValue.Availability.IsFutureAvailability === 'N') {
        // In stock
        item.availablity.CurrentAvailableQty = item.fulfillmentValue.Availability.AvailableQty;
      } else if (item.fulfillmentValue.Availability.IsFutureAvailability === 'Y') {
        // Available on Future date
        item.availablity.CurrentAvailableQty = 0;
        item.availablity.FutureAvailableQuantity = item.fulfillmentValue.Availability.AvailableQty;
        item.availablity.FutureAvailableDate = item.fulfillmentValue.Availability.AvailableDate;
      }
      this.checkProductAvailablity();
    }
  }

  async addToCart(associatedItem?) {
    const selectedOption = this.fulfillmentOptions.find(el => el.checked);
    this.setFulfillmentData(selectedOption);
    if (this.productIdentifierInfo.productBrowsingStandalone) {
      const itemInfoObj = {
        isModelItem: this.isModelItem,
        itemID: this.productDetails.ItemID,
        showQtyOverrideCheckbox: this.quantity.showQtyOverrideCheckbox,
        hasQuantityOverridePermission: this.quantity.hasQuantityOverridePermission,
        quantityLimitOverride: this.quantity.quantityLimitOverride,
        minQuantity: this.quantity.minQuantity,
        orderedQty: this.quantity.orderedQty,
        orderLineKey: this.orderLineKey,
        selectedVariant: this.selectedVariant,
        associatedItemID: associatedItem ? associatedItem.associatedItemID : null,
        associatedItemUOM: associatedItem ? associatedItem.associatedItemUOM : null,
        deliveryMethod: selectedOption,
        shipNode: selectedOption.id === 'PICK' ? this.fulfillmentOptions.find(el => el.checked).fulfillmentValue : null,
        zipCode: selectedOption.id === 'SHP' ? this.fulfillmentOptions.find(el => el.checked).fulfillmentValue : null,
        selectedTab: this.productTabsComponent.selectedTab
      };
      this.navigateToCreateOrder.emit(itemInfoObj);
    } else {
      const ordDetails = this.orderStoreService.getOrderDetails();
      const IsPriceLocked = this.isExchangeOrder ? this.displayRuleService.getRuleValueForOrg( ordDetails.Order?.EnterpriseCode, 'ICC_EXCHANGE_PRICE_LOCKED') : 'Y';
      if (BucBaseUtil.isVoid(ordDetails.Order?.OrderHeaderKey)) {
        const itemDetails = this.getItemDetails();
        this.handleCreateOrder.emit(itemDetails);
      } else {
        const cartCount = ordDetails.Order?.TotalNumberOfParentLines;

        //EOMS-13713 - Changes Start
        const costCenter = this.costCenterOptions.find(item => item.selected)?.value;
        const orderReason = this.orderReasonOptions.find(item => item.selected)?.value;
        const isSalesOrder = ordDetails?.Order?.DocumentType === '0001';
        //EOMS-13713 - Changes End
        console.log("ordDetails?.Order?.DocumentType: ",ordDetails?.Order?.DocumentType);
        console.log("ordDetails: product-detail ", ordDetails);
        const input = {
          Order: {
            EnterpriseCode: this.enterpriseCode,
            OrderHeaderKey: this.productIdentifierInfo.orderHeaderKey,
            ...(this.hasPriceOverridePermission && this.overridePriceInfo && cartCount > 0 && { isPriceOverride: 'Y' }),

            //EOMS-13713 - Changes Start
			AllocationRuleID: this.fulfillmentDetails?.CodeShortDescription,
            ...(isSalesOrder && {
              Extn: {
                ExtnCostCenter: costCenter,
                ExtnReasonCode: orderReason,
              }
            }),
            //EOMS-13713 - Changes End
            OrderLines: {
              OrderLine: [
                {
                  OrderLineKey: this.orderLineKey,
                  DeliveryMethod: selectedOption.id,
                  ...(selectedOption.id === 'PICK') && {
                    ShipNode: selectedOption.fulfillmentValue?.ShipNode
                  },
                  OrderedQty: this.quantity.minQuantity,
                  Item: {
                    ItemID: this.isModelItem ? this.selectedVariant.ItemID : this.productDetails.ItemID,
                    UnitOfMeasure: this.isModelItem ? this.selectedVariant.UnitOfMeasure : this.productDetails.UnitOfMeasure,
                    ProductClass: this.isModelItem ? this.selectedVariant.PrimaryInformation.DefaultProductClass : this.productDetails.PrimaryInformation.DefaultProductClass
                  },
                  LinePriceInfo: {
                    ...(this.hasPriceOverridePermission && this.overridePriceInfo) ? { IsPriceLocked } : {},
                    ListPrice: this.isModelItem ? this.selectedVariant.ComputedPrice?.ListPrice : this.productDetails.ComputedPrice?.ListPrice,
                    UnitPrice: this.overridePriceInfo ? this.overridePriceInfo.price : (this.isModelItem ? this.selectedVariant.ComputedPrice?.UnitPrice : this.productDetails.ComputedPrice?.UnitPrice),
                    BundleTotal: (this.overridePriceInfo && this.productDetails.PrimaryInformation.KitCode === "BUNDLE") ? this.overridePriceInfo.price : (this.isModelItem ? this.selectedVariant.ComputedPrice?.BundleTotal : this.productDetails.ComputedPrice?.BundleTotal)
                  },
                  OrderLineTranQuantity: {
                    OrderedQty: this.quantity.orderedQty
                  },
                  ...(this.quantity.showQtyOverrideCheckbox && this.quantity.hasQuantityOverridePermission && this.quantity.quantityLimitOverride) && {
                    OrderOverride: {
                      QuantityLimitOverridden: 'Y'
                    }
                  },
                  ...(this.hasPriceOverridePermission && this.overridePriceInfo && IsPriceLocked === 'Y' && {
                    Notes: {
                      Note: {
                        ReasonCode: "YCD_NEW_ITEM_INFO",
                        NoteText: this.overridePriceInfo.note ? this.overridePriceInfo.note : this.translateService.instant('OVERRIDE_PRICE.GENERAL.MSG_PRICE_OVERRIDDEN_NOTE', { reason: this.overridePriceInfo.reason }),
                      }
                    }
                  }),
				  //EOMS-13713 Changes Start
                  FulfillmentType: this.fulfillmentDetails?.CodeLongDescription,
                  //EOMS-13713 Changes Start
                },
                ...(this.productIdentifierInfo.selectedVariantID && !this.productIdentifierInfo.readFromSession) ? [{
                  Action: "REMOVE",
                  OrderLineKey: this.productToRemoveFromCartKey
                }] : [],
              ]
            }
          }
        }
        const resp = await this.productDetailsService.addProductToCart(input);
        this.handleAddToCartResponse(resp);
      }
    }
  }

  getItemDetails() {
    const selectedOption = this.fulfillmentOptions.find(el => el.checked);
    const ordDetails = this.orderStoreService.getOrderDetails();
    const IsPriceLocked = this.isExchangeOrder ? this.displayRuleService.getRuleValueForOrg( ordDetails.Order?.EnterpriseCode, 'ICC_EXCHANGE_PRICE_LOCKED') : 'Y';
    const itemDetails = {
      ...this.productDetails,
      ItemID: this.isModelItem ? this.selectedVariant.ItemID : this.productDetails.ItemID,
      UnitOfMeasure: this.isModelItem ? this.selectedVariant.UnitOfMeasure : this.productDetails.UnitOfMeasure,
      ProductClass: this.isModelItem ? this.selectedVariant.PrimaryInformation.DefaultProductClass : this.productDetails.PrimaryInformation.DefaultProductClass,
      ComputedPrice: {
        ListPrice: this.isModelItem ? this.selectedVariant.ComputedPrice?.ListPrice : this.productDetails.ComputedPrice?.ListPrice,
        UnitPrice: this.overridePriceInfo ? this.overridePriceInfo.price : (this.isModelItem ? this.selectedVariant.ComputedPrice?.UnitPrice : this.productDetails.ComputedPrice?.UnitPrice),
        BundleTotal: (this.overridePriceInfo && this.productDetails.PrimaryInformation.KitCode === "BUNDLE") ? this.overridePriceInfo.price : (this.isModelItem ? this.selectedVariant.ComputedPrice?.BundleTotal : this.productDetails.ComputedPrice?.BundleTotal)
      },
      DeliveryMethod: selectedOption.id,
      ...(selectedOption.id === 'PICK') && {
        ShipNode: selectedOption.fulfillmentValue?.ShipNode
      },
      OrderLineTranQuantity: {
        OrderedQty: this.quantity.orderedQty
      },
      ...(this.quantity.showQtyOverrideCheckbox && this.quantity.hasQuantityOverridePermission && this.quantity.quantityLimitOverride) && {
        OrderOverride: {
          QuantityLimitOverridden: 'Y'
        }
      },
      ...(this.hasPriceOverridePermission && this.overridePriceInfo && IsPriceLocked === 'Y' && {
        Notes: {
          Note: {
            ReasonCode: "YCD_NEW_ITEM_INFO",
            NoteText: this.overridePriceInfo.note ? this.overridePriceInfo.note : this.translateService.instant('OVERRIDE_PRICE.GENERAL.MSG_PRICE_OVERRIDDEN_NOTE', { reason: this.overridePriceInfo.reason }),
          }
        }
      })
    };
   return itemDetails;
  }

  onAddToOrder(associatedItem?) {
    const selectedOption = this.fulfillmentOptions.find(el => el.checked);
    this.setFulfillmentData(selectedOption);
    const itemDetails = this.getItemDetails();
    this.modalService.destroy();
    this.modalService.create({
      component: AddToOrderModalComponent,
      inputs: {
        modalData: {
          size: 'md',
          enterpriseCode: this.enterpriseCode,
          itemDetails: associatedItem ?? itemDetails
        }
      }
    });
  }

  setFulfillmentData(selectedOption) {
    const shipNode = selectedOption.id === 'PICK' ? this.fulfillmentOptions.find(el => el.checked).fulfillmentValue : null;
    const zipCode = selectedOption.id === 'SHP' ? this.fulfillmentOptions.find(el => el.checked).fulfillmentValue : null;

    const fulfillmentItem = {
      selectedFulfillMethodItem: selectedOption.id,
      isZipCodeAvailableItem: zipCode ? true : false,
      showZipCodeInputFieldItem: zipCode ? false : true,
      pickupStoreAddrItem: shipNode,
      isPickupStoreAddrAvailableItem: shipNode ? true : false,
      zipCodeItem: zipCode,
    };
    this.updateFulfillmentData.emit(fulfillmentItem);
  }

  handleAddToCartResponse(resp) {
    if (resp?.Order) {
      if (resp.Order.OrderLines?.OrderLine?.length) {
        this.orderStoreService.setOrderDetails(resp);
        this.updateOrderSummary.emit();
        const orderLines = resp.Order.OrderLines.OrderLine;
        this.quantity.hasQuantityOverridePermission = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource('ICC000020');
        const errorArr = [];
        if (resp.Order.HasValidationErrors === CC_CONSTANTS.CHECK_YES) {
          resp.Order.ModifiedOrderLines?.OrderLine?.forEach((line) => {
            if (line.Errors?.Error?.length) {
              const error = line.Errors?.Error[0];
              this.orderLineKey = line.OrderLineKey;

              if (error.ErrorCode === AddToCartErrorCodes.MinQuantity) {
                if (line.Item.ItemID === (this.isModelItem ? this.selectedVariant.ItemID : this.productDetails.ItemID)) {
                  this.quantity.isQuantityInvalid = true;
                  const qty = error.Attribute.find((item) => item.Name === 'MinOrderQuantity').Value;
                  this.quantity.quantityInvalidText = this.translateService.instant('PRODUCT_DETAIL.GENERAL.ERR_MIN_QUANTITY_ALLOWED', { quantity: parseInt(qty) });
                  this.handleAddToCartError(error, errorArr);
                } else {
                  errorArr.push({ error, errorMsg: this.nlsMap['PRODUCT_DETAIL.NOTIFICATIONS.MSG_QTY_FAILING_VALIDATION'] });
                }
              } else if (error.ErrorCode === AddToCartErrorCodes.MaxQuantity) {
                if (line.Item.ItemID === (this.isModelItem ? this.selectedVariant.ItemID : this.productDetails.ItemID)) {
                  this.quantity.isQuantityInvalid = true;
                  const qty = error.Attribute.find((item) => item.Name === 'MaxOrderQuantity').Value;
                  this.quantity.quantityInvalidText = this.translateService.instant('PRODUCT_DETAIL.GENERAL.ERR_MAX_QUANTITY_ALLOWED', { quantity: parseInt(qty) });
                  this.handleAddToCartError(error, errorArr);
                } else {
                  errorArr.push({ error, errorMsg: this.nlsMap['PRODUCT_DETAIL.NOTIFICATIONS.MSG_QTY_FAILING_VALIDATION'] });
                }
              } else {
                this.handleAddToCartError(error, errorArr);
              }

              this.sendCartErrorDetails.emit({ itemID: (this.isModelItem ? this.selectedVariant.ItemID : this.productDetails.ItemID), orderLineKey: this.orderLineKey });
            }
          });
          this.bucNS.send(errorArr.map((err) => {
            return new BucNotificationModel({
              statusType: 'error',
              statusContent: err.errorMsg,
            });
          }));
          if (errorArr.length === 1) {
            if (errorArr[0].error.ErrorCode === AddToCartErrorCodes.MinQuantity || errorArr[0].error.ErrorCode === AddToCartErrorCodes.MaxQuantity) {
              this.quantity.showQtyOverrideCheckbox = true;
            }
          }
        } else {
          this.quantity.isQuantityInvalid = false;
          this.quantity.showQtyOverrideCheckbox = false;

          // Reset quantity and send cart details to parent on success
          this.quantity.quantityInvalidText = '';
          this.orderLineKey = '';
          this.quantity.orderedQty = this.quantity.minQuantity;
          this.sendCartDetails.emit({ itemID: (this.isModelItem ? this.selectedVariant.ItemID : this.productDetails.ItemID), orderLinesObj: resp.Order.OrderLines, resp });

          // Update product quantity in cart
          this.updateProductQtyInCart(orderLines);

          this.ccNotificationService.notify({
            type: 'success',
            title: this.translateService.instant(
              (this.isReturnOrder ? 'PRODUCT_DETAIL.NOTIFICATIONS.MSG_ADDED_TO_RETURN_CART' : 'PRODUCT_DETAIL.NOTIFICATIONS.MSG_ADDED_TO_CART'),
              { item: this.productDetails.PrimaryInformation.ShortDescription }
            )
          });
        }
      }
    }
  }

  updateProductQtyInCart(orderLines){
      // Update product quantity in cart
      const orderLinesWithCurrentItem = orderLines.filter(line => (line.Item.ItemID === (this.isModelItem ? this.selectedVariant.ItemID : this.productDetails.ItemID)) && line.Item.UnitOfMeasure === this.productDetails.UnitOfMeasure);
      const productInCartCount = orderLinesWithCurrentItem.reduce((acc, el) => {
        return acc + Number(el.OrderedQty)
      }, 0);
      this.productQtyInCart = productInCartCount;
  }

  handleAddToCartError(error, errorArr) {
    let errorMsg = '';
    if (error) {
      errorMsg = error.ErrorDescription;
      const errorCode = error.ErrorCode;
      const bundleKey = 'APIERROR.' + errorCode;
      if (this.translateService.instant(bundleKey) !== bundleKey) {
        errorMsg = this.translateService.instant(bundleKey);
      }
    }
    errorArr.push({ error, errorMsg });
  }

  getNumericValue(val) {
    if (val) {
      return parseInt(val);
    }
  }

  onOverrideCheckboxChange(value) {
    this.quantity.quantityLimitOverride = value;
    this.quantity.isQuantityInvalid = false;
  }

  onFulfillmentRadioChange(item) {
    if (item?.source?.id) {
      this.fulfillmentOptions.forEach(el => el.checked = el.id === item.source.id);
      const selected = this.fulfillmentOptions.find(el => el.id === item.source.id);
      if (this.isBundleItemShipIndependent && (!selected.fulfillmentValue || !selected.bundleCompAvailablity)) {
        this.isBundleCompInStock = false;
      } else {
        this.checkProductAvailablity();
      }
    }
  }

  dispactFullfillmentAction(item) {
    this.fulfillmentOptions.forEach(el => el.checked = el.id === item.id);
    switch (item.id) {
      case 'SHP':
        item.showLink = false;
        item.showEditIcon = false;
        break;
      case 'PICK':
        this.openSelectStoreModal(item);
        break;
    }
  }

  fulfillmentOptionSaveOrEditClick(item) {
    this.fulfillmentOptions.forEach(el => el.checked = el.id === item.id);
    switch (item.id) {
      case 'SHP':
        if (item.showEditIcon) {
          item.showEditIcon = false;
        } else {
          if (item.fulfillmentValue) {
            this.getAvailablity(item);
          }
        }
        break;
      case 'PICK':
        this.openSelectStoreModal(item);
        break;
    }
  }

  private async getAvailablity(item) {
    const hasRules = this.rulesConfig && this.rulesConfig[PRODUCT_RULES.pickupStoreEnabled] && this.rulesConfig[PRODUCT_RULES.cacheInventory];
    const resp = await this.productDetailsService.getProductDetails(this.prepareGetCompleteItemListApiInput(), hasRules);

    if (resp?.productDetails?.ItemList?.Item?.length > 0) {
      const details = resp.productDetails.ItemList.Item[0];
      item.availablity = details.Availability;
      if (this.isModelItem && resp.productDetails.ItemList.ItemList?.length > 0) {
        this.variationDetails = resp.productDetails.ItemList.ItemList[0];
        item.availablity = this.variationDetails.Item.find((item) => item.ItemID === this.selectedVariant?.ItemID).Availability;
      }
      this.checkProductAvailablity();
    }

    item.showLink = false;
    item.showEditIcon = true;
  }

  private resetValues() {
    this.isScreenInitialized = false;
    this.imageList = [];
    this.selectedVariant = null;
    this.selectedCombination = {};
    this.isSelectedCombinationInvalid = false;
    this.overridePriceInfo = null;
    this.quantity = {
      orderedQty: 1,
      minQuantity: 1,
      isQuantityInvalid: false,
      quantityInvalidText: '',
      quantityLimitOverride: false,
      hasQuantityOverridePermission: false,
      showQtyOverrideCheckbox: false
    }
  }

  openOverridePriceModal() {
    const price = !this.isModelItem ?
      (this.productDetails.ComputedPrice.BundleTotal || this.productDetails.ComputedPrice.UnitPrice) :
      (this.selectedVariant.ComputedPrice?.BundleTotal || this.selectedVariant.ComputedPrice?.UnitPrice);

    this.actionProcessorService.dispatch<ActionParams>(CC_CONSTANTS.OVERRIDE_PRICE, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          enterpriseCode: this.enterpriseCode,
          price: this.currPipe.transform(isNaN(price) ? 0 : price, this.productDetails.Currency ?? this.currency, 'symbol'),
          currency: this.productDetails.Currency ?? this.currency,
          orderLine: {
            ItemDetails: {
              PrimaryInformation: this.productDetails.PrimaryInformation,
              ItemID: this.productIdentifierInfo.itemID
            }
          }
        }
      }
    });
  }
}
