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
  Component, EventEmitter, Input,
  OnChanges,
  OnDestroy, OnInit, Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ActionProcessorService, Constants, DisplayRulesHelperService, TabMessageService } from '@buc/common-components';
import { BucNotificationModel } from '@buc/common-components';
import { BucNotificationService } from '@buc/common-components';
import { BucSessionService } from '@buc/common-components';
import { BucTableModel } from '@buc/common-components';
import { CCNotificationService } from '@buc/common-components';
import { getArray } from '@buc/common-components';
import { ProductDetailsService } from '../product-details/data-service/product-details.service';
import { ProductSearchService } from './data-service/product-search.service';
import { ProductCommonService } from '../data-service/product.service';
import { BucBaseUtil, BucSvcAngularStaticAppInfoFacadeUtil, CallCenterNavigationService, getPostMessageDomain } from '@buc/svc-angular';
import { TranslateService } from '@ngx-translate/core';
import { cloneDeep, isEmpty } from 'lodash';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { ActionParams } from '../../common/types';
import { AddToCartErrorCodes, CC_CONSTANTS, ProductDetailTabs, PRODUCT_RULES, ExtensionConstants } from '../../common/constants';
import { OrderStoreService } from '../../common/services/order-store.service';
import { ProductDetailComponent } from '../product-details/product-detail/product-detail.component';
import { ModalService } from 'carbon-components-angular';
import { AddToOrderModalComponent } from '../../add-to-order/add-to-order.component';
import { AddToOrderService } from '../../add-to-order/add-to-order.service';

@Component({
  selector: 'buc-product-search',
  templateUrl: './product-search.component.html',
  styleUrls: ['./product-search.component.scss']
})
export class ProductSearchComponent implements OnInit, OnChanges, OnDestroy {
  componentId = 'ProductSearchComponent';

  // Search
  inputSearchValue = '';
  inputSearchValueforItem = '';
  searchPlaceholderText: any;
  searchByItemPlaceholderText: any;
  searchValue = '';
  isSearchIndexConfigured: boolean;

  // Breadcrumb
  breadCrumbStack = [];

  // Translation
  protected readonly nlsMap = {
    'PRODUCT_SEARCH.ITEM_SEARCH.TEXT_PLACE_HOLDER': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.ITEM_SEARCH_TEXT_PLACE_HOLDER': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.LABEL_PICKUP': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.LABEL_SHIPPING': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.ALL_CATEGORIES': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.LABEL_ASCENDING': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.LABEL_DESCENDING': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.MSG_BUNDLE_OUT_OF_STOCK': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.MSG_ADD_TO_CART_FAIL': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.SORT_OPTIONS.Relevance': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.SORT_OPTIONS.ItemID': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.SORT_OPTIONS.ShortDescription': '',
    'PRODUCT_SEARCH.MESSAGES.MSG_INVALID_ITEM_ID': '',
    'PRODUCT_DETAIL.NOTIFICATIONS.MSG_QTY_FAILING_VALIDATION': '',
    'PRODUCT_SEARCH.ITEM_SEARCH.EXCHANGE_TEXT_PLACE_HOLDER': ''
  };

  showDetailsPage = false;

  // Filter panel
  isCancelBtnDisabled = true;
  filterFields: any;
  inputFilters: any[];
  filterTags = [];
  isApplyBtnDisabled = true;

  // Pagination
  totalPages = 1;
  model: BucTableModel = new BucTableModel();
  pageLength = CC_CONSTANTS.ITEM_SEARCH_RESULTS_PAGE_LENGTH;
  pageNo = 1;
  itemsPerPageOptions = CC_CONSTANTS.ITEM_SEARCH_RESULTS_PER_PAGE;
  public readonly defaultPageLength = CC_CONSTANTS.ITEM_SEARCH_RESULTS_PAGE_LENGTH;

  // Sort
  sortOptions: any;
  sortField: any;

  // Zip code
  isZipCodeAvailable = true;
  showZipCodeInputField = false;
  parentZipCode;

  // Pickup
  isPickupStoreAddrAvailable = false;
  pickupStoreAddr;

  // Fulfillment method
  selectedFulfillMethod: any;
  fulfillmentMethodItemsForProduct;
  // Rules
  bopisRuleValue: any;
  cacheInventoryRuleValue: any;

  // Categories
  categoriesList = [];
  selectedCategory: any;
  inputCategoryPath = '';
  parentAndChildCategoriesLen = 0;
  showAllCategories = true;

  // Item Details
  isItemSearchPage = true;
  isItemDetailsPage = false;
  selectedItem: any;
  selectedTab = '';

  customerId = '';
  customerContactId = '';

  // Input params for product details
  productIdentifierInfo = {
    itemID: '',
    unitOfMeasure: '',
    defaultProductClass: '',
    selectedFulfillmentMethod: '',
    shipTo: '',
    country: '',
    countries: [],
    shipNode: {},
    productQtyInCart: '',
    orderLineKey: '',
    selectedTab: '',
    orderHeaderKey: '',
    enterpriseCode: '',
    productBrowsingStandalone: false,
    isParentItemSearchResult: true,
    selectedVariantID: '',
    readFromSession: false,
    isModelItem: false,
    associatedItemID: '',
    associatedItemUOM: '',
    selectedVariant: {} as any,
    quantity: 0,
    quantityLimitOverride: false,
    hasQuantityOverridePermission: false,
    showQtyOverrideCheckbox: false,
  };
  rulesConfig = {
    [PRODUCT_RULES.cacheInventory]: '',
    [PRODUCT_RULES.pickupStoreEnabled]: '',
  };
  customerInformation = {
    customerId: '',
    customerContactId: ''
  };
  EXTENSION = {
    TOP: ExtensionConstants.PRODUCT_SEARCH_CC_TOP,
    BOTTOM: ExtensionConstants.PRODUCT_SEARCH_CC_BOTTOM
  };

  @Input() productInfo;
  @Input() enterpriseCurrency;
  @Input() isExchangeOrder = false;
  @Input() isReturnOrder = false;
  @Input() checkAvailabilityOfProducts = true;
  @Input() searchInput;
  // Quantity and Add to cart
  @Output() updateOrderSummary: EventEmitter<any> = new EventEmitter();
  @Output() handleCreateOrder: EventEmitter<any> = new EventEmitter();
  @Output() handleAddToOrder: EventEmitter<any> = new EventEmitter();

  @ViewChild(ProductDetailComponent, { static: false }) productDetail: ProductDetailComponent;

  //EOMS-13713 - Changes Start
  costCenterOptions: any;
  orderReasonOptions: any;
  fulfillmentDetails: any;
  //EOMS-13713 - Changes End

  // Override quantity limit
  readonly overrideQtyResourceId = 'ICC000020';
  readonly productSearchByIdResourceId = 'ICC000062';

  // Miscellaneous
  itemsList = [];
  currency: any;
  totalHits = 0;
  isScreenInitialized = false;
  areResultsLoaded = false;
  actionSub: Subscription;
  orderDetails: any;
  selectedCategoryIdx: number;
  @Input() productBrowsingStandalone = false;
  @Input() enterpriseCode;
  @Input() addItemToOrder = false;

  // Session
  sessionId;
  sessionPrefix = 'product-browsing';
  productBrowsingUniqueId;
  protected bucSessionStorageService: BucSessionService;
  protected productBrowsingSessionStorageService: BucSessionService;
  enterpriseList: any;
  isSingleEnterprise = false;

  // Catalog index error
  isIndexError = false;
  itemDetailsBasedOnItemID: any;

  // Address
  country: string;
  city: string;
  state: string;
  countries = [];
  isCountryInvalid = false;
  showGoToOrder = false;
  isProductSearchByIdAllowed;
  
  constructor(
    private translateService: TranslateService,
    private productSearchService: ProductSearchService,
    private route: ActivatedRoute,
    private actionProcessorService: ActionProcessorService,
    private productService: ProductCommonService,
    private orderStoreService: OrderStoreService,
    private productDetailsService: ProductDetailsService,
    private ccNavigationSvc: CallCenterNavigationService,
    protected bucNS: BucNotificationService,
    private ccNotificationService: CCNotificationService,
    private modalService: ModalService,
    private addToOrderService: AddToOrderService,
    private tabMessageService: TabMessageService,
    private displayRulesHelperService: DisplayRulesHelperService,
  ) { }

  ngOnInit(): void {
    this.initialize();
    if(this.productBrowsingStandalone) {
      const sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
      const uniqueId = this.route.snapshot.queryParams.uniqueId;
      this.tabMessageService.registerTabForRefocusNotification(sessionId, uniqueId, this.onTabRefocus.bind(this, sessionId, uniqueId));
    }
    this.isProductSearchByIdAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.productSearchByIdResourceId);
  }

  onTabRefocus(sessionId, uniqueId) {
    this.checkOpenDraftOrders();
    window.postMessage({
      action: Constants.NOTIFY_TAB_REFOCUS,
      data: { sessionId, uniqueId }
    }, getPostMessageDomain());
  }

  ngOnChanges(changes: SimpleChanges): void {
      if(changes.addItemToOrder?.currentValue) {
        if(BucBaseUtil.isVoid(this.orderDetails)) {
          const ordDetails = this.orderStoreService.getOrderDetails();
          if (ordDetails && ordDetails.Order) {
            this.orderDetails = ordDetails.Order;
          }
        }
        this.handleNavigationFromAddToOrder();
        this.addItemToOrder = false;
      }
  }

  private initData() {
    this.isScreenInitialized = true;
  }

  async initialize(): Promise<any> {
    await this._initTranslations();
    this.isSearchIndexConfigured = this.displayRulesHelperService.isSearchIndexConfigured();
    this.sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    this.searchPlaceholderText = this.isExchangeOrder ? this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.EXCHANGE_TEXT_PLACE_HOLDER'] : this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.TEXT_PLACE_HOLDER'];
    this.searchByItemPlaceholderText = this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.ITEM_SEARCH_TEXT_PLACE_HOLDER'] ;
    const ordDetails = this.orderStoreService.getOrderDetails();
    if (ordDetails && ordDetails.Order) {
      this.orderDetails = ordDetails.Order;
      if (this.orderDetails.PersonInfoShipTo) {
        this.country = this.orderDetails.PersonInfoShipTo.Country;
        this.city = this.orderDetails.PersonInfoShipTo.City;
        this.state = this.orderDetails.PersonInfoShipTo.State;
        this.parentZipCode = this.orderDetails.PersonInfoShipTo.ZipCode;
        this.showZipCodeInputField = false;
        this.isZipCodeAvailable = true;
      }
      this.customerId = this.orderDetails ? this.orderDetails.BillToID : '';
      this.customerContactId = this.orderDetails ? this.orderDetails.CustomerContactID : '';
    }
    const queryParams = this.route.snapshot.queryParams;
    let storeInSessionObjSearchData: any;
    if (this.productBrowsingStandalone) {
      // Fetch enterprise list from utils if the current page is standalone product browsing
      let userOrgList = [];
      if (BucSvcAngularStaticAppInfoFacadeUtil.getAccessibleOrgListForUser()) {
        userOrgList = getArray(BucSvcAngularStaticAppInfoFacadeUtil.getAccessibleOrgListForUser().OrganizationList.Organization);
      }
      if (userOrgList && userOrgList.length) {
        userOrgList = userOrgList.filter(el => el.IsSeller === 'Y');
        userOrgList = userOrgList.map(el => ({ content: el.OrganizationName, value: el.OrganizationCode, selected: this.enterpriseCode === el.OrganizationCode }));
        this.enterpriseList = userOrgList;
        this.isSingleEnterprise = this.enterpriseList.length === 1;
      }
      if(!queryParams?.showDetailsPage){
        await this.fetchAllCategories();
      }
      await this.initializeSession();
    } else {
      if (queryParams?.fromProductBrowsing === 'Y') {
        await this.initializeProductBrowsingSession();
        storeInSessionObjSearchData = this.productBrowsingSessionStorageService.getItem('searchData');
        this.isIndexError = storeInSessionObjSearchData.isIndexError || false;
        if (!this.isIndexError) {
          await this.fetchAllCategories();
        }
      } else {
        await this.fetchAllCategories();
        await this.initializeSession();
      }
    }
    this.checkOpenDraftOrders();
    this.initializeFulfillmentMethod();
    if (this.productInfo && this.productInfo.ItemID) {
      this.inputSearchValue = this.productInfo.ItemID;
      this.inputSearchValueforItem = this.productInfo.ItemID;
      this.openItemDetails(this.productInfo);
    }
    this.initRules();
    
    if (!queryParams?.showDetailsPage && (queryParams?.searchValue || this.searchInput)) {
      this.inputSearchValue = queryParams.searchValue || this.searchInput;
      if (this.isIndexError) {
        this.isItemSearchPage = false;
        this.fetchItemDetailsBasedOnItemID(this.inputSearchValue);
      } else {
        this.selectedCategory = '';
        this.resetBreadCrumbStack();
        this.pushToBreadCrumb({ searchValue: this.inputSearchValue });
        this.fetchCategoriesBasedOnSearchTerm(this.inputSearchValue);
      }
    }
    if (queryParams?.fromProductBrowsing === 'Y') {
      // Read parent data from session storage

      if (storeInSessionObjSearchData) {
        if (storeInSessionObjSearchData.readFromSession && !storeInSessionObjSearchData.productDetailsStandalone && !storeInSessionObjSearchData.isIndexError) {
          this.enterpriseCode = storeInSessionObjSearchData.enterpriseCode;
          this.customerId = storeInSessionObjSearchData.customerId || '';
          this.customerContactId = storeInSessionObjSearchData.customerContactId || '';
          this.parentZipCode = storeInSessionObjSearchData.parentZipCode || null;
          this.isZipCodeAvailable = storeInSessionObjSearchData.isZipCodeAvailable || false;
          this.showZipCodeInputField = storeInSessionObjSearchData.showZipCodeInputField || false;
          this.selectedFulfillMethod = storeInSessionObjSearchData.selectedFulfillMethod || null;
          this.pickupStoreAddr = storeInSessionObjSearchData.pickupStoreAddr;
          this.isPickupStoreAddrAvailable = storeInSessionObjSearchData.isPickupStoreAddrAvailable || false;
          this.cacheInventoryRuleValue = storeInSessionObjSearchData.cacheInventoryRuleValue || null;
          this.inputSearchValue = storeInSessionObjSearchData.inputSearchValue || '';
          this.breadCrumbStack = storeInSessionObjSearchData.breadCrumbStack;
          this.sortField = storeInSessionObjSearchData.sortField;
          this.country = storeInSessionObjSearchData.country;
          this.filterTags = storeInSessionObjSearchData.filterTags;
          this.isItemSearchPage = storeInSessionObjSearchData.isItemSearchPage;
          this.pageNo = storeInSessionObjSearchData.pageNumber;
          this.pageLength = storeInSessionObjSearchData.pageSize;
          this.inputFilters = storeInSessionObjSearchData.inputFilters;
          // Fetch items list based on search data from session
          await this.getItemsList(storeInSessionObjSearchData.category, storeInSessionObjSearchData.inputFilters,
            storeInSessionObjSearchData.pageNumber, storeInSessionObjSearchData.pageSize);
          if (!isEmpty(this.fulfillmentMethodItemsForProduct) && this.selectedFulfillMethod) {
            this.fulfillmentMethodItemsForProduct.forEach((el, i) => el.selected = el.id === this.selectedFulfillMethod);
          }
          if (this.sortField && !isEmpty(this.sortOptions)) {
            this.sortOptions.forEach((el, i) => {
              el.selected = el.id === this.sortField
            });
          }
        }
        if (storeInSessionObjSearchData.readFromSession && !storeInSessionObjSearchData.productDetailsStandalone && storeInSessionObjSearchData.isIndexError) {
          this.itemsList = [storeInSessionObjSearchData.itemDetailsBasedOnItemID];
          this.inputSearchValue = storeInSessionObjSearchData.inputSearchValue || '';
          this.isItemSearchPage = storeInSessionObjSearchData.isItemSearchPage;
        }
        if (storeInSessionObjSearchData.productDetailsStandalone) {
          if (this.productBrowsingSessionStorageService.getItem('itemData')) {
            this.searchValue = this.inputSearchValue = this.productBrowsingSessionStorageService.getItem('itemData').ItemID;
            await this.fetchCategoriesBasedOnSearchTerm(this.inputSearchValue);
            this.isItemSearchPage = false;
          }
        }
      }

      const itemFromSession = this.productBrowsingSessionStorageService.getItem('itemData');

      // Navigate to product detail page
      if (!this.isItemSearchPage && storeInSessionObjSearchData && storeInSessionObjSearchData.readFromSession) {
        if (itemFromSession) {
          const itemIdx = this.itemsList.findIndex(el => el.ItemID === itemFromSession.ItemID);
          const itemObj = cloneDeep(this.itemsList[itemIdx]);
          itemFromSession.UnitOfMeasure = itemObj.UnitOfMeasure;
          itemObj.zipCodeItem = itemFromSession.zipCodeItem;
          if (itemObj.zipCodeItem) {
            itemObj.showZipCodeInputField = false;
            itemObj.isZipCodeAvailableItem = true;
          }
          itemObj.pickupStoreAddrItem = itemFromSession.pickupStoreAddrItem;
          if (itemObj.pickupStoreAddrItem) {
            itemObj.isPickupStoreAddrAvailableItem = true;
          }
          itemObj.selectedFulfillMethodItem = itemFromSession.selectedFulfillMethodItem;
          if (!isEmpty(itemObj.fulfillmentMethodList) && itemObj.selectedFulfillMethodItem) {
            itemObj.fulfillmentMethodList.forEach((el, i) => el.selected = el.id === itemObj.selectedFulfillMethodItem);
          }
          itemObj.orderLineKey = itemFromSession.orderLineKey;

          if (itemObj) {
            const itemType = itemObj.PrimaryInformation.KitCode === 'BUNDLE' ? 'bundle' : '';
            this.openItemDetails(itemFromSession, itemType);
          } else {
            this.openItemDetails(itemFromSession);
          }

        }
      } else {
        // Call add to cart from search page
        if (!isEmpty(this.itemsList) && storeInSessionObjSearchData && storeInSessionObjSearchData.readFromSession) {
          // Read the item data from session and update
          if (this.productBrowsingSessionStorageService.getItem('itemData')) {
            const itemFromSession = this.productBrowsingSessionStorageService.getItem('itemData');
            const itemIdx = this.itemsList.findIndex(item => item.ItemID === itemFromSession.ItemID);
            this.itemsList[itemIdx] = itemFromSession;
            // Call add to cart for the selected item
            this.onAddToCart(this.itemsList[itemIdx]);
            this.doNotAllowToReadFromSession();

          }
        }
      }

    }

    if(queryParams?.showDetailsPage && queryParams?.searchValue){
      this.showDetailsPage = queryParams?.showDetailsPage;
      this.resetBreadCrumbStack();
      this.pushToBreadCrumb({ searchValue: queryParams?.searchValue });
      this.openItemDetails({ItemID : queryParams.searchValue});
    }

    this.isScreenInitialized = true;

    //EOMS-13713 Changes Start
    this.costCenterOptions = await this.productSearchService.getCostCenters(this.enterpriseCode);

    this.orderReasonOptions = await this.productSearchService.getOrderReasons();
	
	this.fulfillmentDetails = await this.productSearchService.getFulFillmentDetails(this.enterpriseCode);
    //EOMS-13713 Changes End
  }

  initializeSession() {
    this.productBrowsingUniqueId = this.route.snapshot.queryParams.uniqueId;
    this.sessionPrefix = 'product-browsing-' + this.productBrowsingUniqueId;
    this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, this.sessionId);
  }

  initializeProductBrowsingSession() {
    this.productBrowsingUniqueId = this.route.snapshot.queryParams.productBrowsingUniqueId;
    if (this.productBrowsingUniqueId) {
      this.sessionPrefix = 'product-browsing-' + this.productBrowsingUniqueId;
      this.productBrowsingSessionStorageService = new BucSessionService(this.sessionPrefix, this.sessionId);
    }
  }

  checkOpenDraftOrders() {
    this.showGoToOrder = this.productBrowsingStandalone ? this.addToOrderService.hasOpenCreateOrderTabs(this.enterpriseCode) : false;
  }

  async handleNavigationFromAddToOrder() {
    // check if item is added from Add to order flow in product browsing
    const orderNo = this.orderStoreService.getOrderDetails().Order.OrderNo
    const sessionPrefix = 'add-to-order-' + orderNo;
    const addToOrderSessionService = new BucSessionService(sessionPrefix, '');
    const itemFromAddToOrderFlow = addToOrderSessionService.getItem('itemDetails');
    if (!BucBaseUtil.isVoid(itemFromAddToOrderFlow)) {
      await this.onAddToCart(itemFromAddToOrderFlow);
      addToOrderSessionService.removeItem('itemDetails');
      this.handleAddToOrder.emit();
    }
  }

  initializeFulfillmentMethod() {
    let fulfillmentData;
    if (this.productBrowsingStandalone) {
      fulfillmentData = this.bucSessionStorageService.getItem('fulfillmentData');
    } else {
      if (this.route.snapshot.queryParams?.fromProductBrowsing === 'Y') {
        fulfillmentData = this.productBrowsingSessionStorageService.getItem('fulfillmentData');
      } else {
        fulfillmentData = this.bucSessionStorageService.getItem('fulfillmentData');
      }
    }
    if (fulfillmentData) {
      this.parentZipCode = fulfillmentData.parentZipCode || this.parentZipCode;
      this.isZipCodeAvailable = fulfillmentData.isZipCodeAvailable || false;
      this.showZipCodeInputField = fulfillmentData.showZipCodeInputField || false;
      this.selectedFulfillMethod = fulfillmentData.selectedFulfillMethod || null;
      this.pickupStoreAddr = fulfillmentData.pickupStoreAddr;
      this.isPickupStoreAddrAvailable = fulfillmentData.isPickupStoreAddrAvailable || false;
    }
  }

  async initRules() {
    if (this.enterpriseCode) {
      await this.productSearchService.getBOPISandCacheInventoryRuleDetails(this.enterpriseCode).then(mashupOutput => {
        if (mashupOutput && mashupOutput.bopisRuleValue && mashupOutput.bopisRuleValue.Rules) {
          this.bopisRuleValue = mashupOutput.bopisRuleValue.Rules.RuleSetValue;
        }
        if (mashupOutput && mashupOutput.cacheInventoryRuleValue && mashupOutput.cacheInventoryRuleValue.Rules) {
          this.cacheInventoryRuleValue = mashupOutput.cacheInventoryRuleValue.Rules.RuleSetValue;
        }
      });
    }
  }

  async onSelectEnterprise(evt) {
    this.enterpriseCode = evt.item.value;
    this.enterpriseList.forEach(el => el.selected = el.value === evt.item.value);
    await this.fetchAllCategories();
    this.isItemSearchPage = true;
    this.isItemDetailsPage = false;
    this.isSearchIndexConfigured = this.displayRulesHelperService.isSearchIndexConfigured();
  }

  protected async _initTranslations(): Promise<any> {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translateService.get(keys).toPromise();
    keys.forEach(k => this.nlsMap[k] = json[k]);
  }

  onSelectFulfillmentMethodHeaderLevel(evt) {
    this.fulfillmentMethodItemsForProduct.forEach((el, i) => el.selected = el.id === evt.item.id);
    this.selectedFulfillMethod = this.fulfillmentMethodItemsForProduct.find(el => el.selected).id;
    if ((this.selectedFulfillMethod === 'SHP') ||
      (this.selectedFulfillMethod === 'PICK' && this.pickupStoreAddr)) {
      this.getItemsList(this.selectedCategory, this.inputFilters);
    }
  }
  onSelectFulfillmentMethodForItem(evt, item) {
    item.fulfillmentMethodList.forEach((el, i) => el.selected = el.id === evt.item.id);
    item.selectedFulfillMethodItem = item.fulfillmentMethodList.find(el => el.selected).id;
    if (item.selectedFulfillMethodItem === 'SHP') {
      this.updateItemAvailabilityForZipCode(item);
    }
    if (item.selectedFulfillMethodItem === 'PICK' && item.pickupStoreAddrItem) {
      this.updateItemAvailabilityForStore(item, item.pickupStoreAddrItem);
    }
  }



  onSelectSortOption(evt) {
    this.sortOptions.forEach((el, i) => {
      el.selected = el.id === evt.item.id;
      if (el.selected) {
        if (el.id === 'Relevance') {
          this.sortField = null;
        } else {
          this.sortField = el.id;
        }
      }
    });
    this.getItemsList(this.selectedCategory, this.inputFilters);
  }

  async fetchAllCategories() {
    this.selectedCategory = null;
    this.resetSort();
    this.resetFilters();
    if (this.isSearchIndexConfigured) {
      const inputToFetchAllCategories = {
        enterpriseCode: this.enterpriseCode,
        customerId: this.customerId,
        customerContactId: this.customerContactId,
        cacheInventoryRule: 'Y'
      };
      await this.productSearchService.fetchAllCategories(inputToFetchAllCategories).then(mashupOutput => {
        this.searchValue = this.inputSearchValue = '';
        if (mashupOutput && mashupOutput.CatalogSearch) {
          this.inputCategoryPath = '';
          this.categoriesList = mashupOutput.CatalogSearch.CategoryList.Category;
          this.getParentAndChildCategoryLength();
          this.isIndexError = false;
          this.areResultsLoaded = false;
        }
      }, err => {
        this.categoriesList = [];
        this.inputFilters = [];
        this.itemsList = [];
        this.areResultsLoaded = false;
        // Do not display error banner for no active index as it is displayed in the page's empty state already
        if (err && err.errorCode !== 'YCM85_016') {
          this.displayErrorBanner(err.errorMsg);
        }
        this.isIndexError = true;
        this.isItemSearchPage = true;
        this.isItemDetailsPage = false;
      });
    } else {
      this.categoriesList = [];
      this.inputFilters = [];
      this.itemsList = [];
      this.areResultsLoaded = false;
      this.isIndexError = true;
      this.isItemSearchPage = true;
      this.isItemDetailsPage = false;
    }
    this.resetBreadCrumbStack();
    this.resetPagination();
    this.itemsList = [];
  }

  displayErrorBanner(errMsg) {
    this.bucNS.send([
      new BucNotificationModel({
        statusType: 'error',
        statusContent: errMsg
      })
    ]);
  }

  getParentAndChildCategoryLength() {
    this.showAllCategories = true;
    if (!isEmpty(this.categoriesList)) {
      this.categoriesList.forEach(category => {
        if (!category.ChildCategoryList) category.ChildCategoryList = { Category: [] };
        category.ChildCategoryList.Category = getArray(category.ChildCategoryList.Category);
      });
      const childCategoriesLength = this.categoriesList.reduce((acc, val) => acc + val.ChildCategoryList.Category.length, 0);
      this.parentAndChildCategoriesLen = childCategoriesLength + this.categoriesList.length;
      // If total categories length is more than 10, hide some categories and show View all link
      if (this.parentAndChildCategoriesLen > 10) {
        this.showAllCategories = false;
      }
    }
  }

  resetPickupStore() {
    this.pickupStoreAddr = null;
    this.isPickupStoreAddrAvailable = false;
  }

  resetSort() {
    this.sortField = null;
    if (this.sortOptions && this.sortOptions.length) {
      this.sortOptions.forEach(el => el.selected = el.id === 'Relevance');
    }
  }

  resetFulfillmentMethod() {
    this.selectedFulfillMethod = 'SHP';
    if (this.fulfillmentMethodItemsForProduct && this.fulfillmentMethodItemsForProduct.length) {
      this.fulfillmentMethodItemsForProduct.forEach(el => el.selected = el.id === 'SHP');
    }
  }

  resetPagination() {
    this.pageNo = 1;
    this.itemsPerPageOptions = CC_CONSTANTS.ITEM_SEARCH_RESULTS_PER_PAGE;
    this.totalPages = 1;
  }

  resetZipCode() {
    if (this.orderDetails && this.orderDetails.PersonInfoShipTo) {
      this.parentZipCode = this.orderDetails.PersonInfoShipTo.ZipCode;
      this.isZipCodeAvailable = true;
      this.showZipCodeInputField = false;
    } else {
      this.parentZipCode = null;
      this.isZipCodeAvailable = false;
      this.showZipCodeInputField = false;
    }
  }

  resetFilters() {
    this.filterFields = null;
    this.filterTags = [];
    this.inputFilters = null;
    this.isApplyBtnDisabled = this.isCancelBtnDisabled = true;
  }

  // Reset Breadcrumb links
  resetBreadCrumbStack() {
    this.breadCrumbStack = [{
      id: this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.ALL_CATEGORIES'],
      content: this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.ALL_CATEGORIES']
    }];
  }

  // Reset Quantity
  resetQuantity(item) {
    const quantity = Number(item.PrimaryInformation.MinOrderQuantity);
    item.quantity = (quantity > 1) ? quantity : 1
    item.isQuantityInvalid = false;
    item.quantityInvalidText = '';
  }

  // On clicking breadcrumb link, pop the breadcrumb stack
  onBreadCrumbLinkClick(link) {
    if (link.CategoryPath) {
      this.getItemsList(link);
      const selectedLinkIdx = this.breadCrumbStack.findIndex(el => el.CategoryPath === link.CategoryPath);
      if (selectedLinkIdx !== -1) {
        this.breadCrumbStack.length = selectedLinkIdx + 1;
      }
    } else if (link.searchValue) {
      this.performSearch(link.searchValue);
    } else {
      this.fetchAllCategories();
    }
  }

  // Push the link to breadcrumb stack on selecting category or search term
  pushToBreadCrumb(category) {
    if (category.searchValue) {
      if (category.searchValue === this.breadCrumbStack[this.breadCrumbStack.length - 1].searchValue) {
        return;
      }
    }
    if (category.CategoryPath) {
      if (category.CategoryPath === this.breadCrumbStack[this.breadCrumbStack.length - 1].CategoryPath) {
        return;
      }
    }
    this.breadCrumbStack.push(category);
    if (category.searchValue) {
      this.breadCrumbStack[this.breadCrumbStack.length - 1].content = category.searchValue;
      this.breadCrumbStack[this.breadCrumbStack.length - 1].id = category.searchValue;
    } else {
      this.breadCrumbStack[this.breadCrumbStack.length - 1].content = category.ShortDescription;
      this.breadCrumbStack[this.breadCrumbStack.length - 1].id = category.ShortDescription;
    }
    if (!isEmpty(this.breadCrumbStack)) {
      const lastItem = this.breadCrumbStack[this.breadCrumbStack.length - 1];
      if (lastItem.CategoryPath) {
        // If an entry exists already in the breadcrumb, then pop the stack
        const idx = this.breadCrumbStack.findIndex(el => el.CategoryPath === lastItem.CategoryPath);
        if (idx !== -1) {
          this.breadCrumbStack.length = idx + 1;
        }
      }
    }
  }


  async performSearch(searchValue: string) {
    searchValue = searchValue.trim();
    this.searchValue = searchValue;
    if (searchValue === '') {
      this.clearSearch();
    } else {
      if (searchValue) {
        if (this.isIndexError) {
          this.fetchItemDetailsBasedOnItemID(searchValue);
        } else {
          this.selectedCategory = '';
          this.resetBreadCrumbStack();
          this.pushToBreadCrumb({ searchValue })
          this.fetchCategoriesBasedOnSearchTerm(searchValue);
        }
      }
    }
    this.initData();
  }

  async fetchItemDetailsBasedOnItemID(searchValue) {
   const input = {
      enterpriseCode: this.enterpriseCode,
      itemId: searchValue,
      cacheInventoryRule: this.cacheInventoryRuleValue,
      customerContactId: this.customerContactId || '',
      customerId: this.customerId || '',
      zipCode: this.parentZipCode || '',
      country: this.country || ''
    };
    await this.productSearchService.getProductDetailsBasedOnItemID(input).then(mashupOutput => {
      if (mashupOutput && mashupOutput.ItemList && mashupOutput.ItemList.Item) {
        const itemList = getArray(mashupOutput.ItemList.Item);
        if (itemList && itemList.length) {
          this.itemDetailsBasedOnItemID = itemList[0];
          // Navigate to product details
            this.openItemDetails(itemList[0]);
        }
      } else {
        // Invalid item id
        this.ccNotificationService.notify({
          type: 'error',
          title: this.nlsMap['PRODUCT_SEARCH.MESSAGES.MSG_INVALID_ITEM_ID']
        });
      }
    });
  }


  clearSearch() {
    this.fetchAllCategories();
  }

  async fetchCategoriesBasedOnSearchTerm(searchValue) {
    const inputToGetCategoriesList = {
      categoryPath: '',
      termVal: searchValue,
      sortField: this.sortField || null,
      cacheInventoryRule: this.cacheInventoryRuleValue,
      enterpriseCode: this.enterpriseCode,
      customerId: this.customerId || '',
      customerContactId: this.customerContactId || '',
      pageSize: this.pageLength,
      shipNode: (this.selectedFulfillMethod === 'PICK' && this.pickupStoreAddr?.ShipNode) || null,
    };

    await this.productSearchService.getCategoriesList(inputToGetCategoriesList, this.checkAvailabilityOfProducts).then(mashupOutput => {
      if (mashupOutput && mashupOutput.CatalogSearch) {
        this.inputCategoryPath = inputToGetCategoriesList.categoryPath;
        this.categoriesList = mashupOutput.CatalogSearch.CategoryList.Category;
        this.getParentAndChildCategoryLength();
        this.areResultsLoaded = true;
        this.itemsList = mashupOutput.CatalogSearch.ItemList.Item;
        this.currency = this.enterpriseCurrency || mashupOutput.CatalogSearch.ItemList.Currency;
        this.totalHits = mashupOutput.CatalogSearch.TotalHits;
        this.totalPages = mashupOutput.CatalogSearch.TotalPages;
        this.pageNo = 1;
        this.model.currentPage = this.pageNo;
        this.model.pageLength = this.pageLength;
        this.model.isLoading = true;
        this.model.totalDataLength = this.totalHits;
        if (isEmpty(mashupOutput.CatalogSearch.CategoryList)) {
          this.categoriesList = [];
        }
        if (!mashupOutput.CatalogSearch.ItemList.Item) {
          this.itemsList = [];
        }
        if (!isEmpty(this.itemsList)) {
          this.generateFulfillmentMethodListForEachItem();
          this.getZipCodeForEachItem();
          this.getStoreForEachItem();
          this.getQuantityForEachItem();
          this.displayExistingCartDetails();
        }
        this.populateFilterFields(mashupOutput, null);
        // If user is on product detail page and entered search term, open product detail directly if there's only one search result
        if (this.itemsList && Array.isArray(this.itemsList) && this.itemsList.length === 1) {
          if ((this.isItemDetailsPage && !this.isItemSearchPage) || this.route.snapshot.queryParams?.showDetailsPage) {
            const item = this.itemsList[0];
            const itemType = item.PrimaryInformation.KitCode === 'BUNDLE' ? 'bundle' : '';
            this.openItemDetails(item, itemType);
          }
        } else {
          this.isItemSearchPage = true;
          this.isItemDetailsPage = false;
        }
        this.filterTags = [];
        this.inputFilters = null;
      }
    });
    this.getFulfillmentMethodHeaderLevel();
    this.initData();
  }
  

  onOverrideCheckboxChange(value, item) {
    item.quantityLimitOverride = value;
    item.isQuantityInvalid = false;
    item.quantityInvalidText = '';
  }

  restrictNext() {
    if (this.itemsList && this.itemsList.length) {
      const isAnyItemHavingError = this.itemsList.some(item => item.isItemHavingError);
      return isAnyItemHavingError;
    }
  }

  getSubCategories(category, inputFilters, parentCategory) {
    this.getItemsList(category, inputFilters, 1, this.pageLength, parentCategory);
  }

  async getItemsList(category, inputFilters?, pageNumber?, pageSize?, parentCategory?) {
    this.selectedCategory = category;
    let termValInput;
    if (this.inputSearchValue) {
      termValInput = this.inputSearchValue
    } else {
      if (this.breadCrumbStack[this.breadCrumbStack.length - 1].searchValue) {
        termValInput = this.breadCrumbStack[this.breadCrumbStack.length - 1].searchValue;
      }
    }
    const inputToGetCategoriesList = {
      categoryPath: category && category.CategoryPath ? category.CategoryPath : '',
      termVal: termValInput,
      inputFilters,
      pageNumber: pageNumber ?? this.pageNo,
      pageSize: pageSize ?? this.pageLength,
      sortField: this.sortField,
      cacheInventoryRule: (this.selectedFulfillMethod === 'PICK' && this.isPickupStoreAddrAvailable) ? 'N' : this.cacheInventoryRuleValue,
      zipCode: (this.selectedFulfillMethod === 'SHP' && this.parentZipCode) || null,
      shipNode: (this.selectedFulfillMethod === 'PICK' && this.pickupStoreAddr?.ShipNode) || null,
      enterpriseCode: this.enterpriseCode,
      country: this.country || '',
      city: this.city || '',
      state: this.state || '',
      customerId: this.customerId,
      customerContactId: this.customerContactId,
      currency: this.enterpriseCurrency
    };
    await this.productSearchService.getCategoriesList(inputToGetCategoriesList, this.checkAvailabilityOfProducts).then(mashupOutput => {
      if (mashupOutput && mashupOutput.CatalogSearch) {
        this.inputCategoryPath = inputToGetCategoriesList.categoryPath;
        this.totalHits = mashupOutput.CatalogSearch.TotalHits;
        this.totalPages = mashupOutput.CatalogSearch.TotalPages;
        this.pageNo = pageNumber ?? this.pageNo;
        this.model.currentPage = this.pageNo;
        this.model.pageLength = this.pageLength;
        this.model.isLoading = true;
        this.model.totalDataLength = this.totalHits;
        this.categoriesList = mashupOutput.CatalogSearch.CategoryList.Category;

        // To preserve selected category
        if (category) {
          if (category.ChildCategoryList && category.ChildCategoryList.Category) {
            category.ChildCategoryList.Category = null;
          }
          // To preserve parent category
          if (parentCategory) {
            if (parentCategory.ChildCategoryList && parentCategory.ChildCategoryList.Category) {
              parentCategory.ChildCategoryList.Category = null;
            }
          }

          if (!this.categoriesList) {
            if (parentCategory) {
              this.categoriesList = [parentCategory, category];
            } else {
              this.categoriesList = [category];
            }
          } else {
            if (parentCategory) {
              this.categoriesList.unshift(parentCategory, category);
            } else {
              this.categoriesList.unshift(category);
            }
          }
        }
        this.getParentAndChildCategoryLength();
        if (this.selectedCategory) {
          const idx = this.categoriesList.findIndex(el => el.CategoryPath === this.selectedCategory.CategoryPath);
          if (idx !== -1) {
            this.selectedCategoryIdx = idx;
          }
        }
        this.areResultsLoaded = true;
        this.isIndexError = false;

        this.itemsList = mashupOutput.CatalogSearch.ItemList.Item;
        if (!isEmpty(this.itemsList)) {
          this.generateFulfillmentMethodListForEachItem();
          this.getZipCodeForEachItem();
          this.getStoreForEachItem();
          this.getQuantityForEachItem();
          this.displayExistingCartDetails();
        }
        this.currency = this.enterpriseCurrency || mashupOutput.CatalogSearch.ItemList.Currency;
        this.populateFilterFields(mashupOutput, inputFilters);
      }
    }, err => {
      this.categoriesList = [];
      this.inputFilters = [];
      this.itemsList = [];
      this.areResultsLoaded = false;
      this.isIndexError = true;
      this.isItemSearchPage = true;
      this.isItemDetailsPage = false;
      // Do not display error banner for no active index as it is displayed in the page's empty state already
      if (err && err.errorCode !== 'YCM85_016') {
        this.displayErrorBanner(err.errorMsg);
      }
    });
    await this.getFulfillmentMethodHeaderLevel();
    this.initData();
  }

  displayExistingCartDetails() {
    const ordDetails = this.orderStoreService.getOrderDetails();
    if (ordDetails && ordDetails.Order) {
      this.orderDetails = ordDetails.Order;
      if (this.orderDetails && this.orderDetails.OrderLines && this.orderDetails.OrderLines.OrderLine) {
        const orderLines = this.orderDetails.OrderLines.OrderLine;
        if (this.itemsList.length) {
          this.itemsList.forEach((item, i) => {
            // Update product qty in cart within product panel
            const orderLinesWithCurrentItem = orderLines.filter(line => line.Item.ItemID === item.ItemID && line.Item.UnitOfMeasure === item.UnitOfMeasure);
            const productQtyInCartCount = orderLinesWithCurrentItem.reduce((acc, el) => {
              return acc + Number(el.OrderedQty)
            }, 0);
            item.productQtyInCart = productQtyInCartCount;
          });
        }
      }
    }
  }

  async getFulfillmentMethodHeaderLevel() {
    if (isEmpty(this.fulfillmentMethodItemsForProduct)) {
      this.fulfillmentMethodItemsForProduct = [
        {
          id: 'SHP',
          value: 'SHP',
          selected: true,
          content: this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.LABEL_SHIPPING']
        },
        {
          id: 'PICK',
          value: 'PICK',
          selected: false,
          content: this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.LABEL_PICKUP']
        }
      ];
    }
    if (this.selectedFulfillMethod) {
      if ((this.selectedFulfillMethod === 'SHP' && this.parentZipCode) ||
        (this.selectedFulfillMethod === 'PICK' && this.pickupStoreAddr)) {
        this.fulfillmentMethodItemsForProduct.forEach(el => el.selected = el.id === this.selectedFulfillMethod);
      }
    }
    if (isEmpty(this.selectedFulfillMethod)) {
      this.selectedFulfillMethod = 'SHP';
    }
    if (isEmpty(this.sortOptions) || isEmpty(this.country)) {
      await this.getSortOptionsAndCountryList();
    }
    if (this.bopisRuleValue === 'N') {
      const pickUpValIdx = this.fulfillmentMethodItemsForProduct.findIndex(el => el.id === 'PICK');
      if (pickUpValIdx !== -1) {
        this.fulfillmentMethodItemsForProduct.splice(pickUpValIdx, 1);
      }
    }
  }

  async getSortOptionsAndCountryList() {
    await this.productSearchService.getSortOptionsAndCountryList(!isEmpty(this.sortOptions), this.country).then(mashupOutput => {
      if (mashupOutput && mashupOutput.getSortOptions && mashupOutput.getSortOptions.SearchIndexFieldList) {
        this.sortOptions = getArray(mashupOutput.getSortOptions.SearchIndexFieldList.SearchField);
        if (this.sortOptions.length > 0) {
          // Added Relevance manually
          this.sortOptions.push({ IndexFieldName: 'Relevance', Name: 'Relevance' });
          this.sortOptions.forEach(el => {
            el.DisplayName = this.translateService.instant(`PRODUCT_SEARCH.ITEM_SEARCH.SORT_OPTIONS.${el.Name}`);
            el.id = el.IndexFieldName;
            el.value = el.IndexFieldName;
            el.selected = false;
            el.content = `${el.DisplayName} (${this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.LABEL_ASCENDING']})`;
            if (el.IndexFieldName === 'Relevance') {
              // Select Relevance by default on load
              el.selected = true;
              el.content = el.DisplayName;
            }
          });

          // Make new array with descending sort option and combine it with original array
          const sortOptionArr2 = this.sortOptions.map(el => {
            return { ...el }
          })
          const relevanceIdx = sortOptionArr2.findIndex(el => el.Name === 'Relevance');
          sortOptionArr2.splice(relevanceIdx, 1);
          sortOptionArr2.forEach(el => {
            el.id = el.value = el.id + '-Dsc';
            el.content = el.DisplayName = `${el.DisplayName} (${this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.LABEL_DESCENDING']})`;
          });

          this.sortOptions = [...this.sortOptions, ...sortOptionArr2];
          this.sortOptions.sort((a, b) => a.id > b.id ? 1 : -1);
        }

      }
      if (mashupOutput && mashupOutput.getCountryList) {
        const countryList = mashupOutput.getCountryList.CommonCodeList;
        if (countryList && countryList.CommonCode) {
          this.countries = getArray(countryList.CommonCode)
            .map(({ CodeLongDescription: content, CodeValue: id }) =>
              ({ content, id, selected: id === 'US' })
            );
          this.country = 'US';
          this.isCountryInvalid = false;
        }
      }
    });
  }

  onCountrySelectionChange(item) {
    this.isCountryInvalid = false;
    this.country = item?.id;
    this.countries.forEach(el => el.selected = el.id === item.id);
    this.state = '';
    this.city = '';
  }

  onCountryValSearch(evt) {
    this.isCountryInvalid = false;
    if (evt) {
      const searchedCountry = this.countries.find((item) => item.content.toLowerCase() === evt.toLowerCase());
      this.isCountryInvalid = !searchedCountry;
      if (searchedCountry) {
        this.country = searchedCountry.id;
      }
    } else {
      this.countries.forEach(el => el.selected = false);
      this.isCountryInvalid = true;
      this.country = '';
    }
  }

  async generateFulfillmentMethodListForEachItem() {
    this.itemsList.forEach((item) => {
      if (isEmpty(item.fulfillmentMethodList)) {
        item.fulfillmentMethodList = [
          {
            id: 'SHP',
            value: 'SHP',
            selected: true,
            content: this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.LABEL_SHIPPING']
          },
          {
            id: 'PICK',
            value: 'PICK',
            selected: false,
            content: this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.LABEL_PICKUP']
          }
        ];
      }
      if (this.selectedFulfillMethod) {
        if ((this.selectedFulfillMethod === 'SHP' && this.parentZipCode) ||
          (this.selectedFulfillMethod === 'PICK' && this.pickupStoreAddr)) {
          item.fulfillmentMethodList.forEach(el => el.selected = el.id === this.selectedFulfillMethod);
          item.selectedFulfillMethodItem = this.selectedFulfillMethod;
        }
      }
      if (isEmpty(item.selectedFulfillMethodItem)) {
        item.selectedFulfillMethodItem = 'SHP';
      }
      if (this.bopisRuleValue === 'N' || item.PrimaryInformation.IsPickupAllowed !== 'Y') {
        const pickUpValIdx = item.fulfillmentMethodList.findIndex(el => el.id === 'PICK');
        if (pickUpValIdx !== -1) {
          item.fulfillmentMethodList.splice(pickUpValIdx, 1);
        }
        if (item.PrimaryInformation.IsPickupAllowed !== 'Y' && item.selectedFulfillMethodItem === 'PICK') {
          item.selectedFulfillMethodItem = 'SHP';
          item.fulfillmentMethodList.forEach(el => el.selected = el.id === item.selectedFulfillMethodItem);
          this.updateItemAvailabilityForZipCode(item);
        }
      }
    });
  }

  async getZipCodeForEachItem() {
    this.itemsList.forEach((item) => {
      if (this.parentZipCode) {
        item.isZipCodeAvailableItem = true;
        item.showZipCodeInputFieldItem = false;
        item.zipCodeItem = this.parentZipCode;
      } else {
        item.isZipCodeAvailableItem = false;
        item.showZipCodeInputFieldItem = false;
        item.zipCodeItem = null;
      }
    });
  }
  async getStoreForEachItem() {
    this.itemsList.forEach((item) => {
      if (this.pickupStoreAddr) {
        item.isPickupStoreAddrAvailableItem = true;
        item.pickupStoreAddrItem = this.pickupStoreAddr;
      } else {
        item.isPickupStoreAddrAvailableItem = false;
        item.pickupStoreAddrItem = null;
      }
    });
  }

  getQuantityForEachItem() {
    this.itemsList.forEach((item) => {
      const quantity = Number(item.PrimaryInformation.MinOrderQuantity);
      item.quantity = (quantity > 1) ? quantity : 1
      item.isQuantityInvalid = false;
      item.quantityInvalidText = '';
      item.orderLineKey = '';
      item.quantityLimitOverride = false;
      item.hasQuantityOverridePermission = false,
        item.showQtyOverrideCheckbox = false;
    });
  }

  getNumber(str = null) {
    return Number(str);
  }

  toLowerCase(str: string = null) {
    return str.toLowerCase();
  }

  onClickEnterPostalCode(item?) {
    if (!item) {
      this.showZipCodeInputField = true;
    } else {
      item.showZipCodeInputFieldItem = true;
    }
  }

  async onSaveZipCode(item?) {
    this.city = '';
    this.state = '';
    if (!item) {
      this.showZipCodeInputField = false;
      this.isZipCodeAvailable = true;
      this.getItemsList(this.selectedCategory, this.inputFilters);

    } else {
      item.showZipCodeInputFieldItem = false;
      item.isZipCodeAvailableItem = true;
      this.updateItemAvailabilityForZipCode(item);
    }
  }

  onClickSelectStore(store?, item?) {
    if (!item) {
      this.openSelectStoreModal(store);
    } else {
      this.openSelectStoreModal(store, item);
    }
  }

  async updateItemAvailabilityForZipCode(item) {
    const inputToGetCategoriesList = {
      categoryPath: '',
      termVal: item.ItemID,
      zipCode: item.zipCodeItem || null,
      country: this.country || '',
      city: this.city || '',
      state: this.state || '',
      enterpriseCode: this.enterpriseCode,
      customerId: this.customerId,
      customerContactId: this.customerContactId,
      pageSize: this.pageLength
    };
    await this.productSearchService.getCategoriesList(inputToGetCategoriesList).then(mashupOutput => {
      this.searchValue = this.inputSearchValue = '';
      if (mashupOutput && mashupOutput.CatalogSearch) {
        if (mashupOutput.CatalogSearch.ItemList && mashupOutput.CatalogSearch.ItemList.Item) {
          const outputItem = getArray(mashupOutput.CatalogSearch.ItemList.Item);
          if (outputItem && outputItem.length) {
            const itemToUpdate = this.itemsList.find(el => el.ItemID === outputItem[0].ItemID);
            itemToUpdate.Availability = outputItem[0].Availability;
            itemToUpdate.PrimaryInformation = outputItem[0].PrimaryInformation;
          }
        }
      }
    });
  }



  populateFilterFields(mashupOutput, inputFilters) {
    if (mashupOutput.CatalogSearch.FacetList && mashupOutput.CatalogSearch.FacetList.ItemAttribute) {
      this.filterFields = getArray(mashupOutput.CatalogSearch.FacetList.ItemAttribute);
      if (this.filterFields.length) {
        this.filterFields.forEach(field => {
          if (field.AssignedValueList.AssignedValue && field.AssignedValueList.AssignedValue.length) {
            field.AssignedValueList.AssignedValue.forEach(el => {
              el.id = el.value = el.Value;
              el.content = el.ShortDescription;
              el.selected = false;
              // To preserve applied filters
              if (inputFilters && inputFilters.length) {
                inputFilters.forEach(filter => {
                  if (filter.IndexFieldName === field.IndexFieldName) {
                    if (filter.Value === el.value) {
                      el.selected = true;
                    }
                  }
                })
              }
            })
          }
        });
      }
    }
  }

  onSelectPage(page) {
    this.pageNo = page;
    this.model.currentPage = page;
    this.pageLength = this.model.pageLength;
    this.getItemsList(this.selectedCategory, this.inputFilters, page, this.model.pageLength);
  }

  cancelFilter() {
    this.isApplyBtnDisabled = this.isCancelBtnDisabled = true;
    this.filterTags = [];
    this.inputFilters = [];
    if (this.filterFields.length) {
      this.filterFields.forEach(f => {
        f.AssignedValueList.AssignedValue.forEach(el => el.selected = false);
      });
      this.getItemsList(this.selectedCategory);
    }
  }

  async applyFilter() {
    if (this.filterFields.length) {
      const inputFilters = [];
      this.filterFields.forEach(f => {
        const selectedFilter = f.AssignedValueList.AssignedValue.find(el => el.selected);
        if (selectedFilter) {
          inputFilters.push({
            IndexFieldName: f.IndexFieldName, Value: selectedFilter.Value,
            AttrDesc: f.Attribute !== undefined ? f.Attribute.ShortDescription : f.ItemAttributeDescription,
            SelectedValue: selectedFilter.ShortDescription
          });
        }
      });
      if (inputFilters.length === 0) {
        this.getItemsList(this.selectedCategory);
        this.inputFilters = [];
        this.filterTags = [];
        this.isCancelBtnDisabled = true;
        this.isApplyBtnDisabled = true;
        return;
      }
      this.inputFilters = inputFilters;
      await this.getItemsList(this.selectedCategory, inputFilters);
      this.filterTags = [];
      if (inputFilters.length) {
        this.inputFilters.forEach(elem => this.filterTags.push({
          key: elem.AttrDesc, value: elem.SelectedValue,
          IndexFieldName: elem.IndexFieldName, Value: elem.Value
        }));
      } else {
        this.isCancelBtnDisabled = true;
      }
      this.isApplyBtnDisabled = true;
    }
  }

  openItemDetails(item, itemType?) {
    if (itemType) {
      this.selectedTab = itemType === 'bundle' ? ProductDetailTabs.COMPONENTS : itemType === 'substitute' ? ProductDetailTabs.ALTERNATIVE : '';
    } else {
      this.selectedTab = '';
    }
    // Passing input params for product details
    this.productIdentifierInfo.itemID = item.ItemID;
    this.productIdentifierInfo.unitOfMeasure = item.UnitOfMeasure;
    this.productIdentifierInfo.defaultProductClass = item?.PrimaryInformation?.DefaultProductClass;
    if (item.selectedFulfillMethodItem && item.selectedFulfillMethodItem === 'SHP') {
      this.productIdentifierInfo.selectedFulfillmentMethod = 'SHP';
    }
    if (item.selectedFulfillMethodItem && item.selectedFulfillMethodItem === 'PICK') {
      this.productIdentifierInfo.selectedFulfillmentMethod = 'PICK';
    }
    this.productIdentifierInfo.shipTo = item.zipCodeItem;
    this.productIdentifierInfo.country = this.country;
    this.productIdentifierInfo.countries = this.countries;
    this.productIdentifierInfo.shipNode = item.pickupStoreAddrItem;
    if (this.orderDetails) {
      this.productIdentifierInfo.orderHeaderKey = this.orderDetails.OrderHeaderKey;
    }
    this.productIdentifierInfo.enterpriseCode = this.enterpriseCode;
    this.productIdentifierInfo.productBrowsingStandalone = this.productBrowsingStandalone;
    this.productIdentifierInfo.selectedTab = this.selectedTab;
    if (!this.selectedTab) {
      this.productIdentifierInfo.selectedTab = item.selectedTab;
    }
    this.rulesConfig[PRODUCT_RULES.pickupStoreEnabled] = this.bopisRuleValue;
    this.rulesConfig[PRODUCT_RULES.cacheInventory] = this.cacheInventoryRuleValue;
    this.customerInformation.customerId = this.customerId;
    this.customerInformation.customerContactId = this.customerContactId;
    this.productIdentifierInfo.productQtyInCart = item.productQtyInCart;
    this.productIdentifierInfo.orderLineKey = item.orderLineKey;
    this.productIdentifierInfo.selectedVariantID = item.selectedVariantID;
    this.productIdentifierInfo.quantity = item.quantity;
    // from Product browsing
    if (this.route.snapshot.queryParams?.fromProductBrowsing === 'Y' && this.productBrowsingSessionStorageService &&
      this.productBrowsingSessionStorageService.getItem('searchData').readFromSession) {
      this.selectedTab = this.productBrowsingSessionStorageService.getItem('itemData').selectedTab;
      this.productIdentifierInfo.isModelItem = item.isModelItem;
      this.productIdentifierInfo.selectedVariant = item.selectedVariant;
      if (item.selectedVariant && item.selectedVariant.ItemID) {
        this.productIdentifierInfo.selectedVariantID = item.selectedVariant.ItemID;
      }
      this.productIdentifierInfo.readFromSession = true;
      this.productIdentifierInfo.quantity = item.quantity;
      this.productIdentifierInfo.showQtyOverrideCheckbox = item.showQtyOverrideCheckbox;
      this.productIdentifierInfo.hasQuantityOverridePermission = item.hasQuantityOverridePermission;
      this.productIdentifierInfo.quantityLimitOverride = item.quantityLimitOverride;
      if (item.associatedItemID && item.associatedItemUOM) {
        this.productIdentifierInfo.associatedItemID = item.associatedItemID;
        this.productIdentifierInfo.associatedItemUOM = item.associatedItemUOM;
      }

    } else {
      this.productIdentifierInfo.readFromSession = false;
    }
    this.productIdentifierInfo = cloneDeep(this.productIdentifierInfo);
    this.isItemDetailsPage = true;
    this.isItemSearchPage = false;
    this.selectedItem = item;
  }

  async bundleShipIndAddToCart(item) {
    const inputToBundleItemAddToCart = {
      enterpriseCode: this.enterpriseCode,
      orderHeaderKey: this.orderDetails.OrderHeaderKey,
      cacheInventoryRule: (this.selectedFulfillMethod === 'PICK' && this.isPickupStoreAddrAvailable) ? 'N' : this.cacheInventoryRuleValue,
      itemID: item.ItemID,
      uom: item.UnitOfMeasure,
      quantityLimitOverridden: item.quantityLimitOverridden || 'N',
      customerContactId: this.customerContactId || '',
      customerId: this.customerId || '',
      zipCode: (item.selectedFulfillMethodItem === 'SHP' && item.zipCodeItem) || null,
      country: this.country || '',
      shipNode: (item.selectedFulfillMethodItem === 'PICK' && item.pickupStoreAddrItem.ShipNode) || null,
      bundleTotal: item.ComputedPrice.BundleTotal || null,
      orderedQty: item.quantity,
      deliveryMethod: item.selectedFulfillMethodItem,
      orderLineKey: item.orderLineKey || null
    }

    await this.productSearchService.addBundleShipIndItemToCart(inputToBundleItemAddToCart).then(mashupOutput => {
      if (mashupOutput && mashupOutput.ItemList && mashupOutput.ItemList.Order) {
        const item = this.itemsList.find(el => el.ItemID === mashupOutput.ItemList.Item[0].ItemID);
        this.handleAddToCartResponse(mashupOutput.ItemList, item);
      } else {
        const errMsg = this.nlsMap['PRODUCT_SEARCH.ITEM_SEARCH.MSG_BUNDLE_OUT_OF_STOCK'];
        this.displayErrorBanner(errMsg);
      }
    });
  }

  doNotAllowToReadFromSession() {
    const storeInSessionObjSearchData = this.productBrowsingSessionStorageService.getItem('searchData');
    if (storeInSessionObjSearchData) {
      storeInSessionObjSearchData.readFromSession = false;
      this.productBrowsingSessionStorageService.setItem('searchData', storeInSessionObjSearchData);
    }
  }

  private getCurrencyForSellerOrganization() {

    let orgCurrency;
    const userOrgsList = ProductCommonService.getUserOrgsList();
    if (userOrgsList) {
      const matchingOrg = userOrgsList.find(org => org.value === this.enterpriseCode);
      orgCurrency = matchingOrg?.currency ? matchingOrg.currency : orgCurrency;
    }
    if (orgCurrency) {
      console.error('Unable to retrieve currency of the seller organization ' + this.enterpriseCode);
    }

    return orgCurrency;
  }


  onNavigateToCreateOrder(evt) {
    const item = {
      orderLineKey: evt.orderLineKey,
      ItemID: evt.itemID,
      quantity: evt.orderedQty,
      showQtyOverrideCheckbox: evt.showQtyOverrideCheckbox,
      hasQuantityOverridePermission: evt.hasQuantityOverridePermission,
      selectedFulfillMethodItem: evt.deliveryMethod?.id,
      quantityLimitOverride: evt.quantityLimitOverride,
      selectedTab: evt.selectedTab,
      associatedItemID: evt.associatedItemID,
      associatedItemUOM: evt.associatedItemUOM,
      selectedVariant: evt.selectedVariant,
      isModelItem: evt.isModelItem,
      pickupStoreAddrItem: evt.shipNode,
      zipCodeItem: evt.zipCode
    };
    this.goToCreateOrderFlow(item);

  }


  async goToCreateOrderFlow(item) {
    const storeInSessionSearchData = {
      category: this.selectedCategory,
      inputFilters: this.inputFilters,
      pageNumber: this.pageNo,
      pageSize: this.pageLength,
      selectedFulfillMethod: this.selectedFulfillMethod,
      cacheInventoryRuleValue: this.cacheInventoryRuleValue,
      parentZipCode: this.parentZipCode,
      isZipCodeAvailable: this.isZipCodeAvailable,
      showZipCodeInputField: this.showZipCodeInputField,
      pickupStoreAddr: this.pickupStoreAddr,
      isPickupStoreAddrAvailable: this.isPickupStoreAddrAvailable,
      enterpriseCode: this.enterpriseCode,
      sortField: this.sortField,
      country: this.country,
      inputSearchValue: this.inputSearchValue,
      breadCrumbStack: this.breadCrumbStack,
      customerId: this.customerId,
      customerContactId: this.customerContactId,
      isItemSearchPage: this.isItemSearchPage,
      isItemDetailsPage: this.isItemDetailsPage,
      filterTags: this.filterTags,
      isIndexError: this.isIndexError,
      itemDetailsBasedOnItemID: this.itemDetailsBasedOnItemID || null,
      readFromSession: true
    };
    this.bucSessionStorageService.setItem('searchData', storeInSessionSearchData);
    this.bucSessionStorageService.setItem('itemData', item);
    // Call create order api and get orderNo, orderHeaderKey
    const orgCurrency = this.getCurrencyForSellerOrganization();

    await this.productService.createDraftOrder(this.enterpriseCode, orgCurrency).then(mashupResponse => {
      if (mashupResponse && mashupResponse.Order) {
        const orderNo = mashupResponse.Order.OrderNo;
        const orderHeaderKey = mashupResponse.Order.OrderHeaderKey;
        const title = this.translateService.instant('PRODUCT_SEARCH.LABEL_CREATE_ORDER', { orderNo });
        // Navigate to Create order flow
        this.ccNavigationSvc.openUrlInSameTab(`${CC_CONSTANTS.CREATE_ORDER_ROUTE}`,
          {
            orderNo, orderHeaderKey, title, sellerEnterpriseCode: this.enterpriseCode, fromProductBrowsing: 'Y',
            productBrowsingUniqueId: this.productBrowsingUniqueId
          });
      }
    });
  }

  onAddToOrder(item) {
    this.setFulfillmentData(item);
    this.modalService.destroy();
    this.modalService.create({
      component: AddToOrderModalComponent,
      inputs: {
        modalData: {
          size: 'md',
          enterpriseCode: this.enterpriseCode,
          itemDetails: item
        }
      }
    });
  }

  async onAddToCart(item) {
    this.setFulfillmentData(item);
    if (this.productBrowsingStandalone) {
      this.goToCreateOrderFlow(item);

    } else {
      if (BucBaseUtil.isVoid(this.orderDetails.OrderHeaderKey)) {
        const itemDetails =  {
          ...item,
          DeliveryMethod: item.selectedFulfillMethodItem,
          ...(item.selectedFulfillMethodItem === 'PICK') && {
            ShipNode: item.pickupStoreAddrItem.ShipNode
          },
          OrderLineTranQuantity: {
            OrderedQty: item.quantity
          },
          OrderOverride: {
            QuantityLimitOverridden: item.quantityLimitOverride ? 'Y' : 'N'
          }
        }
        this.handleCreateOrder.emit(itemDetails);
      } else {
        //EOMS-13713 - Changes Start
        const costCenter = this.costCenterOptions.find(code => code.selected)?.value;
        const orderReason = this.orderReasonOptions.find(code => code.selected)?.value;
        const isSalesOrder = this.orderDetails?.DocumentType === '0001';
        //EOMS-13713 - Changes End
      const input = {
        Order: {
          EnterpriseCode: this.enterpriseCode,
          OrderHeaderKey: this.orderDetails.OrderHeaderKey,
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
            OrderLine: {
              OrderLineKey: item.orderLineKey,
              DeliveryMethod: item.selectedFulfillMethodItem,
              ...(item.selectedFulfillMethodItem === 'PICK') && {
                ShipNode: item.pickupStoreAddrItem.ShipNode
              },
              OrderedQty: Number(item.PrimaryInformation.MinOrderQuantity) > 1 ? Number(item.PrimaryInformation.MinOrderQuantity) : 1,
              Item: {
                ItemID: item.ItemID,
                UnitOfMeasure: item.UnitOfMeasure,
                ProductClass: item.PrimaryInformation.DefaultProductClass
              },
              LinePriceInfo: {
                ListPrice: item.ComputedPrice?.ListPrice,
                UnitPrice: item.ComputedPrice?.UnitPrice,
                BundleTotal: item.ComputedPrice?.BundleTotal
              },
              OrderLineTranQuantity: {
                OrderedQty: item.quantity
              },
              OrderOverride: {
                QuantityLimitOverridden: item.quantityLimitOverride ? 'Y' : 'N'
              },
			  //EOMS-13713 - Changes Start
              FulfillmentType: this.fulfillmentDetails?.CodeLongDescription,
              //EOMS-13713 - Changes End
            }
          },
        }
      }
      if (item.PrimaryInformation.KitCode === 'BUNDLE' && item.PrimaryInformation.BundleFulfillmentMode === '00') {
        this.bundleShipIndAddToCart(item);
      } else {
        const resp = await this.productDetailsService.addProductToCart(input);
          this.handleAddToCartResponse(resp, item);
        }
      }
    }

  }

  setFulfillmentData(item) {
    let sessionStorage;
    if (this.productBrowsingStandalone) {
      sessionStorage = this.bucSessionStorageService;
    } else {
      if (this.route.snapshot.queryParams?.fromProductBrowsing === 'Y') {
        sessionStorage = this.productBrowsingSessionStorageService;
      } else {
        sessionStorage = this.bucSessionStorageService;

      }
    }
    const fulfillmentData = {
      selectedFulfillMethod: item.selectedFulfillMethodItem,
      isZipCodeAvailable: item.isZipCodeAvailableItem,
      showZipCodeInputField: item.showZipCodeInputFieldItem,
      pickupStoreAddr: item.pickupStoreAddrItem,
      isPickupStoreAddrAvailable: item.isPickupStoreAddrAvailableItem,
      parentZipCode: item.zipCodeItem,
      enterpriseCode: this.enterpriseCode
    };
    if (sessionStorage) {
      sessionStorage.setItem('fulfillmentData', fulfillmentData);
    }
  }

  updateFulfillmentData(item) {
    this.setFulfillmentData(item);
    this.initializeFulfillmentMethod();
    this.fulfillmentMethodItemsForProduct.forEach((el, i) => el.selected = el.id === this.selectedFulfillMethod);
    if (!isEmpty(this.itemsList)) {
      this.generateFulfillmentMethodListForEachItem();
      this.getZipCodeForEachItem();
      this.getStoreForEachItem();
      this.getQuantityForEachItem();
      this.displayExistingCartDetails();
    }
  }

  handleAddToCartResponse(resp, item) {
    if (resp?.Order) {
      if (resp.Order.OrderLines?.OrderLine?.length) {
        this.orderStoreService.setOrderDetails(resp);
        this.updateOrderSummary.emit(item);
        item.hasQuantityOverridePermission = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.overrideQtyResourceId);
        const errorArr = [];
        if (resp.Order.HasValidationErrors === CC_CONSTANTS.CHECK_YES) {
          resp.Order.ModifiedOrderLines?.OrderLine?.forEach((line) => {
            if (line.Errors?.Error?.length) {
              const error = line.Errors?.Error[0];
              item.orderLineKey = resp.Order.ModifiedOrderLines?.OrderLine[0].OrderLineKey;
              if (error.ErrorCode === AddToCartErrorCodes.MinQuantity) {
                if (line.Item.ItemID === item.ItemID) {
                  item.isQuantityInvalid = true;
                  const qty = error.Attribute.find((item) => item.Name === 'MinOrderQuantity').Value;
                  item.quantityInvalidText = this.translateService.instant('PRODUCT_DETAIL.GENERAL.ERR_MIN_QUANTITY_ALLOWED', { quantity: parseInt(qty) });
                  this.handleAddToCartError(error, errorArr);
                } else {
                  errorArr.push({ error, errorMsg: this.nlsMap['PRODUCT_DETAIL.NOTIFICATIONS.MSG_QTY_FAILING_VALIDATION'] });
                }
              } else if (error.ErrorCode === AddToCartErrorCodes.MaxQuantity) {
                if (line.Item.ItemID === item.ItemID) {
                  item.isQuantityInvalid = true;
                  const qty = error.Attribute.find((item) => item.Name === 'MaxOrderQuantity').Value;
                  item.quantityInvalidText = this.translateService.instant('PRODUCT_DETAIL.GENERAL.ERR_MAX_QUANTITY_ALLOWED', { quantity: parseInt(qty) });
                  this.handleAddToCartError(error, errorArr);
                } else {
                  errorArr.push({ error, errorMsg: this.nlsMap['PRODUCT_DETAIL.NOTIFICATIONS.MSG_QTY_FAILING_VALIDATION'] });
                }
              } else {
                this.handleAddToCartError(error, errorArr);
              }
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
              item.showQtyOverrideCheckbox = true;
            }
          }
          item.isItemHavingError = true;
        } else {
          this.updateCartDetails({ itemID: item.ItemID, orderLinesObj: resp.Order.OrderLines, resp });
          this.ccNotificationService.notify({
            type: 'success',
            title: this.translateService.instant(
              (this.isReturnOrder ? 'PRODUCT_DETAIL.NOTIFICATIONS.MSG_ADDED_TO_RETURN_CART' : 'PRODUCT_DETAIL.NOTIFICATIONS.MSG_ADDED_TO_CART'),
              { item: item.PrimaryInformation.ShortDescription }
            ),
          });
        }
      }
    }
  }

  updateCartDetails(cartDetails) {
    // Update cart count
    const item = this.itemsList.find(el => el.ItemID === cartDetails.itemID);

    if (item) {
      const orderLines = cartDetails.orderLinesObj?.OrderLine ?? [];
      if (cartDetails.resp) {
        this.orderStoreService.setOrderDetails(cartDetails.resp);
      }
      item.isQuantityInvalid = false;
      item.showQtyOverrideCheckbox = false;
      item.isItemHavingError = false;
      this.resetQuantity(item);
      // Update product qty in cart within product panel
      const orderLinesWithCurrentItem = orderLines.filter(line => line.Item.ItemID === item.ItemID && line.Item.UnitOfMeasure === item.UnitOfMeasure);
      const productQtyInCartCount = orderLinesWithCurrentItem.reduce((acc, el) => {
        return acc + Number(el.OrderedQty)
      }, 0);
      item.productQtyInCart = productQtyInCartCount;
      item.orderLineKey = '';
      if (this.isItemDetailsPage && this.productDetail) {
        this.productIdentifierInfo.productQtyInCart = item.productQtyInCart;
        this.productDetail.updateProductQtyInCart(orderLines);
      }
    }
  }

  getCartErrorDetailsForProduct(errDetails) {
    const item = this.itemsList.find(el => el.ItemID === errDetails.itemID);
    const orderLineKey = errDetails.orderLineKey;
    item.orderLineKey = orderLineKey;
    item.isItemHavingError = true;
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

  async showItemSearchPage(evt) {
    if(this.categoriesList.length == 0){
      await this.fetchAllCategories();
    }
    this.isItemDetailsPage = false;
    this.isItemSearchPage = true;
  }


  private openSelectStoreModal(store?, item?) {
    const data: any = {
      modalText: '',
      modalData: {
        enterpriseCode: this.enterpriseCode,
        personInfoShipTo: {}
      },
    }
    if (item) {
      data.modalData.orderLines = [{
        Item: {
          ItemID: item.ItemID,
          UnitOfMeasure: item.UnitOfMeasure,
          ProductClass: item.PrimaryInformation.DefaultProductClass
        }
      }];
    }
    if (store) {
      data.modalData.personInfoShipTo = store.ShipNodePersonInfo;
      data.modalData.selectedShipNode = store.ShipNode;
    } else {
      data.modalData.personInfoShipTo = this.orderDetails.PersonInfoShipTo
    }
    this.actionProcessorService.dispatch<ActionParams>(CC_CONSTANTS.SELECT_STORE, {
      component: this.componentId,
      data
    });

    // If the modal was `closed` without saving, we need to unsubscribe since there was no changes
    // Garbage collect any existing subscriptions that were not completed
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }

    // On saving store
    this.actionSub = this.actionProcessorService.selectUpdate<any>(this.componentId).pipe(take(1)).subscribe(res => {
      if (res.params.selectedStore) {
        // If header level store is changed, then call api and update availability for all items
        // Else if item level store is changed, then update item with Node availability object.
        const selectedStore = res.params.selectedStore;
        if (!item) {
          this.isPickupStoreAddrAvailable = true;
          this.pickupStoreAddr = selectedStore
          this.getAvailabilityForStore();
        } else {
          item.isPickupStoreAddrAvailableItem = true;
          item.pickupStoreAddrItem = selectedStore;
          if ((!isEmpty(res.params.selectedStore.Availability) && !isEmpty(item.Availability)) ||
            (!isEmpty(res.params.selectedStore.Availability) && isEmpty(item.Availability))) {
            this.updateItemAvailabilityForStore(item, selectedStore);
          }
        }
      }
    });
  }

  updateItemAvailabilityForStore(item, selectedStore) {
    if (!selectedStore.Availability) {
      // Out of stock
      item.Availability = {
        CurrentAvailableQty: 0,
        FutureAvailableQuantity: 0
      };
      return;
    }
    if (selectedStore.Availability.IsAvailable === 'N') {
      // Out of stock
      item.Availability = {
        CurrentAvailableQty: 0,
        FutureAvailableQuantity: 0
      };
    } else if (selectedStore.Availability.IsFutureAvailability === 'N') {
      // In stock
      item.Availability = {
        CurrentAvailableQty: selectedStore.Availability.AvailableQty
      };
    } else if (selectedStore.Availability.IsFutureAvailability === 'Y') {
      // Available on Future date
      item.Availability = {
        CurrentAvailableQty: 0,
        FutureAvailableQuantity: selectedStore.Availability.AvailableQty,
        FutureAvailableDate: selectedStore.Availability.AvailableDate
      };
    }
  }

  async getAvailabilityForStore() {
    await this.getItemsList(this.selectedCategory, this.inputFilters);
  }

  onCloseTag(tag, i) {
    this.filterTags.splice(i, 1);
    const inputFilterIdx = this.inputFilters.findIndex(el => tag.IndexFieldName === el.IndexFieldName && tag.Value === el.Value);
    if (inputFilterIdx !== -1) {
      this.inputFilters.splice(inputFilterIdx, 1);
    }
    if (!this.filterTags.length) {
      this.isCancelBtnDisabled = true;
    }
    this.getItemsList(this.selectedCategory, this.inputFilters);
  }

  onSelectFilterValue(field, evt) {
    this.isApplyBtnDisabled = this.isCancelBtnDisabled = false;
    const selectedField = this.filterFields.find(f => f.IndexFieldName === field.IndexFieldName);
    if (evt.length === 0) {
      if (selectedField.AssignedValueList && selectedField.AssignedValueList.AssignedValue?.length) {
        selectedField.AssignedValueList.AssignedValue.forEach(el => el.selected = false);
      }
    } else {
      if (selectedField.AssignedValueList && selectedField.AssignedValueList.AssignedValue?.length) {
        selectedField.AssignedValueList.AssignedValue.forEach(el => el.selected = el.id === evt.item.id);
      }
    }
  }

  ngOnDestroy() {
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
  }
}
