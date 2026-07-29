/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2022, 2023
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

import { Component, OnInit, Injector, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalService } from 'carbon-components-angular';
import { TranslateService } from '@ngx-translate/core';
import { BreadcrumbService, Constants, getPathFromRoot, CustomerSearchForm, PaginationActions } from '@call-center/customer-shared';
import {
  BucSvcAngularStaticAppInfoFacadeUtil,
  BucCommOmsRestAPIService,
  BucPageResourceMappingService,
  CallCenterNavigationService
} from '@buc/svc-angular';
import {
  BucSessionService,
  OmsOrganizationService,
  BucNotificationService,
  BucTableFilterModel,
  BucTableFilterCategoryModel,
  SearchForm,
  getCurrentLocaleDateFormat,
  COMMON as BUCCOMMON,
  TableFilterComponent,
  BucFieldFilterSortPipe,
  BucTableModel,
  getArray
} from '@buc/common-components';
import { CustomerTableComponent, SearchResultTableData } from '../customer-table/customer-table.component';
import { cloneDeep, defaultTo, isEqual } from 'lodash';
import { ExtensionConstants } from '../../extension.constants';
import { Store } from '@ngrx/store';

@Component({
  selector: 'call-center-customer-search-result',
  templateUrl: 'customer-search-result.component.html',
  styleUrls: ['customer-search-result.component.scss']
})
export class CustomerSearchResultComponent extends CustomerSearchForm implements OnInit {
  @ViewChild(CustomerTableComponent) customerTableComponent: CustomerTableComponent;
  public isScreenInitialized = false;
  public moreThanOneResult = false;
  public doneLoadingResults = false;

  private readonly VALID_ONE_RESULT_CTX = {
    CustomerSearchComponent: true,
    CustomerSearchResultComponent: true
  };

  public readonly nlsMapResult: any = {
    'CUSTOMER_SEARCH.GENERAL.LABEL_RESULTS': '',
    'CUSTOMER_SEARCH.GENERAL.LABEL_FILTER_TITLE': ''
  };

  breadCrumbList: any[];
  public tenantId: any;
  public sessionId: any;
  public tabId: string;
  public isIVEnabled = false;
  public bucSessionStorageService: BucSessionService;
  public bucSessionStorageServiceGlobal: BucSessionService;
  public searchTabSessionStorageService: BucSessionService;
  sessionPrefix = 'call-center-customer-search'; // make sure matches search component
  prefix = '';
  searchCriteria: any[] = [];
  selectedCustomerType;
  loaded = false;
  public isPageInitialized = false;
  protected _searchByCtx: any;
  protected _grpPaths: any;
  protected enterprise = {
    selectedEnterprise: '',
    selectedEnterpriseList: []
  };
  public searchName;
  public selectedSavedSearch;
  public saveSearchPageType;
  public tableFilterModel = new BucTableFilterModel();
  protected readonly sPipe: BucFieldFilterSortPipe;
  private _filtersByFieldMap: { [id: string]: any };
  public hasSearched = false;
  public showFilter = true;
  public i18nDatePlaceholder;
  public datePickerLabel = '';
  public datePickerRangeLabel = '';
  public datePickPlaceholder = '';
  public fromApplyFilters = false;
  public widgetSearchValue = '';
  public fromHome = false;
  public searchTabId: string;
  public searchText = '';
  public customerType = '';
  public isDirectSearch = false;
  pageNo = 1;
  pageSize = BucTableModel.DEFAULT_PAGE_LEN;
  sortKey = 'OrderDate';
  sortOrder = 'Desc';
  maximumRecords: number = Constants.DEFAULT_SEARCH_MAX_RECORDS;
  pageModel = {};
  private _firstInit = true;
  EXTENSION = {
    TOP: ExtensionConstants.CUSTOMER_SEARCH_RESULT_CD_TOP,
    BOTTOM: ExtensionConstants.CUSTOMER_SEARCH_RESULT_CD_BOTTOM
  };
  public componentId = 'call-center-customer-search-result';

  @ViewChild(TableFilterComponent, { static: false }) filterComponent: TableFilterComponent;
  @ViewChild(CustomerTableComponent, { static: false}) customerTable :CustomerTableComponent;
  constructor(
    nSvc: BucNotificationService,
    public ccNavigationSvc: CallCenterNavigationService,
    public mSvc: ModalService,
    public tSvc: TranslateService,
    public route: ActivatedRoute,
    public bSvc: BreadcrumbService,
    restSvc: BucCommOmsRestAPIService,
    public resMapSvc: BucPageResourceMappingService,
    public bucResourceMap: BucPageResourceMappingService,
    public orgSvc: OmsOrganizationService,
    inj: Injector,
    private store$: Store
  ) {
    super(mSvc, nSvc, tSvc, route, resMapSvc, restSvc, inj);
  }

  ngOnInit() {
    this.initialize();
  }

  async prepareBreadcrumbList() {
    const keys = await this.tSvc.get('CUSTOMER_SEARCH.GENERAL.LABEL_RESULTS').toPromise();
    const rootPath = getPathFromRoot(this.route.snapshot);
    this.bSvc.updateLast(keys, rootPath, keys, [rootPath], { queryParams: this.route.snapshot.queryParams });
    this.breadCrumbList = this.bSvc.get();
  }

  protected async initialize(onFilterChange = false) {
    this.tenantId = BucSvcAngularStaticAppInfoFacadeUtil.getSelectedTenantId();
    this.sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
  
    this.tabId = this.route.snapshot.queryParams.uniqueId;
    this.searchTabId = this.route.snapshot.queryParams.searchTabId;
    if (this._firstInit) {
      this.i18nDatePlaceholder = getCurrentLocaleDateFormat();
      await this._initTranslations();
      this.disableSearchFields();
      this.initializeSession();
      this.prepareBreadcrumbList();
    }
    this.isDirectSearch = this.initializeDirectSearchNavigation();
    if (this.isDirectSearch && this.searchText) {
      this.fetchSessionEnterprise();
      await this._fetchFields();
      this._mergeOobAndPreferences();
      this._setSearchByForCurrent();
      await this.prepareSearch();
    } else {
      this.initializeSearchTabNavigation();
    }
    await this.initializeCache();
    await this.checkEnterpriseList();
    this.setReplacementMapping([{ key: 'enterprise', value: 'selectedEnterprise' }]);
    await this.loadForm();
    this.initializeFilter(onFilterChange);
    await this.onSearch(onFilterChange);
    this.preparePageData();
    this._firstInit = false;
    this.isScreenInitialized = true;
    this.loaded = true;
  }


  protected getSession() {
    return this.bucSessionStorageService;
  }

  public getOps() {
    return getArray(this._readFromSession('ops', []))
  }

  async preparePageData() { }

  async _initTranslations() {
    const keys = Object.keys(this.nlsMapResult);
    const json = await this.tSvc.get(keys).toPromise();
    keys.forEach(k => this.nlsMapResult[k] = json[k]);
  }

  initializeSearchTabNavigation() {
    this.searchTabId = this.route.snapshot.queryParams.searchTabId;
    if (this.searchTabId) {
      this.searchTabSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.searchTabId}`);
    }
    this.searchText = defaultTo(this.bucSessionStorageService.getItem('customerSearchText'), '');
    this.bucSessionStorageService.setItem('customerSearchText', this.searchText);
  }

  initializeSession() {
    if (this.searchTabId) {
      this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.searchTabId}`);
    } else {
      this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.tabId}`);
    }
  }

  initializeDirectSearchNavigation() {
    const searchText = this.bucSessionStorageService.getItem('customerSearchText'); 
    const custType = this.bucSessionStorageService.getItem('selectedCustomerType'); 
    const searchTextFromUrl = this.route.snapshot.queryParams.q;
    const customerTypeFromUrl = this.route.snapshot.queryParams.c;
    const convertedCustType = customerTypeFromUrl === Constants.CUSTOMER_TYPE.business ? 'business' : 'consumer';
    
    const enterpriseCode = this.route.snapshot.queryParams.enterpriseCode;

    // check if portlet / category search value was never set / updated
    if (searchText !== '' && searchTextFromUrl) {
      this.searchText = searchText || searchTextFromUrl;
      this.bucSessionStorageService.setItem('customerSearchText', this.searchText);
    }
    if (custType !== '' && customerTypeFromUrl) {
      this.selectedCustomerType = custType || convertedCustType;
      this.bucSessionStorageService.setItem('selectedCustomerType', convertedCustType);
    }

    if (enterpriseCode) {
      this.enterprise = {
        selectedEnterprise: enterpriseCode,
        selectedEnterpriseList: []
      };
      this.bucSessionStorageService.setItem('selectedEnterprise', this.enterprise);
    }
    return this.searchText ? true : false;
  }

  initializeCache() {
    this.isPageInitialized = defaultTo(this.bucSessionStorageService.getItem('isPageInitialized'), false);
    this.enterprise = this._readFromSession('selectedEnterprise', {});
    this.searchName = this._readFromSession('searchName', undefined);
    this._searchByCtx = this._readFromSession('customerSearchByCtx', undefined);
    this._grpPaths = this._readFromSession('groupPaths', undefined);
    this.selectedSavedSearch = this._readFromSession('selectedSavedCustomerSearch', '');
    this.searchCriteria = this._readFromSession('customerSearchCriteria', {});
    this.selectedCustomerType = this._readFromSession('selectedCustomerType', 'consumer');
    if (!this.isDirectSearch) {
      const customerType = this.searchCriteria.filter(temp => temp.id === 'CustomerInfo')[0].options.filter(op => op.id === 'customerType')[0];
      this.selectedCustomerType = customerType ? customerType.value : this.selectedCustomerType;
    }
  }

  initializeFilter(onFilterChange) {
    this._initGroupSearchCriteriaWithCustom(this._searchByCtx);
    this.tableFilterModel.title = this.nlsMapResult['CUSTOMER_SEARCH.GENERAL.LABEL_FILTER_TITLE'];
    this.tableFilterModel.version = 'default';

    // display the right set of search fields in the filter
    const customerType = this.searchCriteria.filter(sc => sc.id === 'CustomerInfo')[0].options.filter(op => op.id === 'customerType')[0];
    if (this.isDirectSearch && !onFilterChange) {
      customerType.value = this.selectedCustomerType;
      customerType.items[0].checked = customerType.value === 'consumer';
      customerType.items[1].checked = customerType.value === 'business';
    }

    if (customerType.value === 'consumer'){
      this.searchCriteria.filter(sc => sc.id === 'CustomerInfo')[0].options.forEach(op => {
        if (op.fieldId === 'organizationName' || op.fieldId === 'customerId'){
          op.hide = true;
        }
      })
    }
    this.tableFilterModel.items = this.searchCriteria;  

    // TODO: add additional item for customer type selection
      // check the radioFieldGroup in buc-table-filter for the structure of the object expected.
    this.updateItemList(this.searchCriteria);
    if (this.filterComponent) {
      this.filterComponent.updateCurrentAppliedFilter();
    }
  }

  toggleFilter() {
    this.showFilter = !this.showFilter;
    this.searchCriteria = this._readFromSession('customerSearchCriteria', {});
    this.initializeFilter(false);
  }

  async updateItemList(event: Array<BucTableFilterCategoryModel>) {
    const old = cloneDeep(this.enterprise);
    const filtersOnly = event.reduce((all, { options }) => [...all, ...options], []);
    filtersOnly.forEach(o => this._updateSingle(o));
    this._filtersByFieldMap = BUCCOMMON.toMap(filtersOnly, 'fieldId');
  }

  async onSearch(onFilterChange = false) {
    this.hasSearched = true;
    this.bucSessionStorageService.setItem('customerSearchCriteria', this.searchCriteria);
    this.bucSessionStorageService.setItem('selectedCustomerEnterprise', this.enterprise);
    this.bucSessionStorageService.setItem('isPageInitializedCustomer', this.isPageInitialized);
    // Searches REQUIRE an enterprise to be selected
    if (this.enterprise.selectedEnterprise) {
      if (!onFilterChange) {
        this.store$.dispatch(
          PaginationActions.startPagination({
            id: Constants.CUSTOMER_TABLE_COMPONENT_ID,
            loadCriteria: {
              searchCriteria: {
                oob: cloneDeep(this.searchCriteria),
                custom: cloneDeep(this.getOps())
              },
              searchText: this.searchText,
              enterprise: this.enterprise.selectedEnterprise,
              customerType: this.selectedCustomerType === 'business' ? Constants.CUSTOMER_TYPE.business : Constants.CUSTOMER_TYPE.consumer
            },
            sortCriteria: undefined
          })
        );
      } else {
        this.store$.dispatch(
          PaginationActions.updateLoadCriteria({
            id: Constants.CUSTOMER_TABLE_COMPONENT_ID,
            loadCriteria: {
              searchCriteria: {
                oob: cloneDeep(this.searchCriteria),
                custom: cloneDeep(this.getOps())
              },
              searchText: this.searchText,
              enterprise: this.enterprise.selectedEnterprise
            },
            sortCriteria: undefined
          })
        );
      }
    }
  }

  private async _saveCriteriaAndFilters(allFilters: Array<BucTableFilterCategoryModel>) {
    const properFilters = await this.rebindCustomFilters(allFilters);
    await this.updateItemList(properFilters);
    const filters = SearchForm.separateFilters(properFilters);
    this.saveFilters();
    this.bucSessionStorageService.setItem('customerSearchCriteria', filters.oob);
    this.saveCustomization(this.bucSessionStorageService, this.getOps());
  }

  async applyFilters(event) {
    this.fromApplyFilters = true;
    this.hasSearched = true;
    event.forEach(({ options }) => {
      // Find index of enterprise & prevent enterprise value from being cleared
      const enterpriseIndex = options.findIndex((o) => o.fieldId === "enterprise");
      if (enterpriseIndex > -1 && !options[enterpriseIndex].value.selectedEnterprise) {
        options[enterpriseIndex].value = this.enterprise;
      }
    });
    await this._saveCriteriaAndFilters(event);
    this.bucSessionStorageService.setItem('selectedEnterprise', this.enterprise);
    this.searchCriteria = this._readFromSession('customerSearchCriteria', []);
    this.bucSessionStorageService.setItem('invocationCtx', 'CustomerSearchResultComponent');
    // this.customerTableComponent.search();
    this.initialize(true);  
  }

  customerTypeOnChange(event){
    const customerInfoSearchCriteria = this.searchCriteria.filter(cr => cr.id === 'CustomerInfo')[0];
    customerInfoSearchCriteria.options.forEach(filterItem => {
      filterItem.hide = false
      if((filterItem.id === 'organizationName' || filterItem.id === 'customerId') && event.value === 'consumer'){
        filterItem.value = ''
        filterItem.hide = true
      }
    });
    this.bucSessionStorageService.setItem('selectedCustomerType', event.value);
  }

  async clearFilters(event) {

    this.bucSessionStorageService.removeItem('selectedEnterprise');
    this.selectedSavedSearch = null;

    event.forEach(({ options }) => {
      // Find index of enterprise & prevent enterprise value from being cleared
      const enterpriseIndex = options.findIndex((o) => o.fieldId === "enterprise");
      if (enterpriseIndex > -1) {
        options[enterpriseIndex].value = this.enterprise;
      }
    });

    await this._saveCriteriaAndFilters(event);
    this.customerTableComponent.search();
    this.bucSessionStorageService.removeItem('selectedSavedCustomerSearch');
    this.initialize(true);
  }

  searchResultChange(c: SearchResultTableData) {
    this.doneLoadingResults = true;
    // go directly to order if only one result and we were invoked from the search-page
    //const callerCtx = this.bucSessionStorageService.getItem('invocationCtx');
    const pageParams = this.route.snapshot.queryParams;
    this.bucSessionStorageService.removeItem('invocationCtx');
    if (c.count === 1 && (!pageParams.breadcrumb || this.fromApplyFilters)) {
      c.table.openCustomerDetails(c.data[0],this.route.snapshot.queryParams);
    }
    else {
      this.moreThanOneResult = true;
    }
  }

  selectedSavedSearchChange(savedSearchKey) {
    this.selectedSavedSearch = savedSearchKey;
    this.bucSessionStorageService.setItem('selectedSavedCustomerSearch', savedSearchKey);
  }

  private _updateSingle(option: any) {
    switch (option.element) {
      case 'enterprise':
        this.enterprise = cloneDeep(option.value);
        break;
      case 'dropdownQuery':
      case 'dropdownQueryNumeric':
        break;
      case 'dropdown':
        break;
      case 'dropdownRange':
        break;
      case 'comboBox':
    }
  }

  searchTextChanged(searchValue: string) {
    this.searchText = searchValue;
    this.bucSessionStorageService.setItem('customerSearchText', this.searchText);

    this.store$.dispatch(PaginationActions.updateLoadCriteria({
      id: Constants.CUSTOMER_TABLE_COMPONENT_ID,
      loadCriteria: {
        searchCriteria: {
          oob: cloneDeep(this.searchCriteria),
          custom: cloneDeep(this.getOps())
        },
        searchText: this.searchText,
        enterprise: this.enterprise.selectedEnterprise
      },
      sortCriteria: undefined
    }));
  }

  createCustomer() {
    this.ccNavigationSvc.openUrlInNewTab(Constants.CUSTOMER_CREATE_ROUTE, { searchTabId: this.tabId });
  }

  async checkEnterpriseList() {
    // Enterprise is required, hence it will always be an option, but order may vary based on customization
    const enterpriseIndex = this.searchCriteria[0].options.findIndex((o) => o.fieldId === "enterprise");
    this.searchCriteria[0].options[enterpriseIndex].canReset = false;
    const omsUserLoginId = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLoginId();
    const resp = await this.orgSvc.getOrganizationListMashup(omsUserLoginId).toPromise();
    
    if (resp.Organization.length > 1) {
      this.searchCriteria[0].options[enterpriseIndex].canReset = true;
    }

    // Executing this logic only on initial render
    if (this.isDirectSearch && this.searchText && this._firstInit) {
      // Add missing data to improve UX
      const orgIndex = resp.Organization.findIndex((o) => o.OrganizationCode === this.enterprise.selectedEnterprise);
      if (orgIndex > -1) {
        this.searchCriteria[0].options[enterpriseIndex].value = { ...this.enterprise, content: resp.Organization[orgIndex].OrganizationName };
      }
    }
  }

  /* SearchForm abstract -- extenders must implement. Remove if not extending SearchForm */
  protected _prefName() { return ''; }

  protected getTenantFieldMap() { return; }

  protected prepareOptions() { return; }

  protected _savedSearchCallbacks() { return; }

  protected resultsRoute() { return; }

  protected prepareSearch() {
    this.updateSearchCriteria();
    this.saveForm();
    if (!this.bucSessionStorageService.getItem('customerSearchCriteria')) {
      this.bucSessionStorageService.setItem('customerSearchCriteria', this.searchCriteria);
    }
  }

  protected _setSearchForDefault() { return; }

  private _getFilterItems() {
    const m = BUCCOMMON.toMap(this.searchCriteria, 'id');
    if (this._searchByCtx && this._searchByCtx.value) {
      const f = m[this._searchByCtx.value].options;
      const c = { oob: f, custom: this.getCustomFilters() };
      const u = this.sPipe.sortFilters(c);
      f.splice(0, f.length, ...u);
    }
  }

  private _readFromSession(key: string, defaultValue: any) {
    const fromSearchTab = this.searchTabId ?
      defaultTo(this.searchTabSessionStorageService.getItem(key), defaultValue) : defaultValue;
    const updatedValInCurrentTab = defaultTo(this.bucSessionStorageService.getItem(key), defaultValue);
    const updatedInCurrentTab = updatedValInCurrentTab !== defaultValue && !isEqual(fromSearchTab, updatedValInCurrentTab);
    return updatedInCurrentTab ? updatedValInCurrentTab : fromSearchTab;
  }
}
