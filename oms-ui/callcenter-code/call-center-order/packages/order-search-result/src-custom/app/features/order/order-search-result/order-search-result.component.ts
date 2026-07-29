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

import { Component, OnInit, Injector, ViewChild, TemplateRef, ViewChildren, QueryList } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalService } from 'carbon-components-angular';
import { TranslateService } from '@ngx-translate/core';
// import { CommonService } from '../shared/data-services/common-service.service';
import { BreadcrumbService, getPathFromRoot, OrderSearchDataService, Constants, OrderSearchForm, AmountRange, PaginationActions } from '@call-center/order-shared';
import {
  BucSvcAngularStaticAppInfoFacadeUtil, BucCommOmsRestAPIService, BucPageResourceMappingService, CallCenterNavigationService, BucBaseUtil
} from '@buc/svc-angular';
import {
  BucSessionService,
  BucNotificationService,
  BucTableFilterModel,
  BucTableFilterCategoryModel,
  SearchForm,
  getCurrentLocaleDateFormat,
  COMMON as BUCCOMMON,
  TableFilterComponent,
  BucFieldFilterSortPipe,
  BucTableModel,
  getArray,
  TemplateIdDirective
} from '@buc/common-components';
import { cloneDeep, defaultTo, get, isEqual } from 'lodash';
import { OrderTableComponent, SearchResultTableData } from '../order-table/order-table.component';
import { ExtensionConstants } from '../../extension.constants';
import { Store } from '@ngrx/store';

@Component({
  selector: 'call-center-order-search-result',
  templateUrl: 'order-search-result.component.html',
  styleUrls: ['order-search-result.component.scss']
})
export class OrderSearchResultComponent extends OrderSearchForm implements OnInit {
  EXTENSION = {
    TOP: ExtensionConstants.ORDER_SEARCH_RESULT_OR_TOP,
    BOTTOM: ExtensionConstants.ORDER_SEARCH_RESULT_OR_BOTTOM
  };

  @ViewChild(OrderTableComponent) orderTableComponent: OrderTableComponent;
  @ViewChild('tableFilter') tableFilter: TableFilterComponent;

  customTemplates: { [id: string]: TemplateRef<any> } = {};
  @ViewChildren(TemplateIdDirective) set _templates(a: QueryList<TemplateIdDirective>) {
    if (a) {
      a.forEach(({ id, template }) => (this.customTemplates[id] = template));
    }
  }

  componentId = "OrderSearchResult";
  public isScreenInitialized = false;
  public moreThanOneResult = false;
  public doneLoadingResults = false;

  private readonly VALID_ONE_RESULT_CTX = {
    OrderSearchComponent: true,
    OrderSearchResultComponent: true
  };

  public readonly nlsMapResult: any = {
    'ORDER_SEARCH.GENERAL.LABEL_RESULTS': '',
    'ORDER_SEARCH.GENERAL.LABEL_FILTER_TITLE': '',
    'ORDER_SEARCH.GENERAL.LABEL_DRAFT_ORDERS': '',
    'ORDER_SEARCH.GENERAL.LABEL_INCLUDED': ''
  };
  breadCrumbList: any[];
  public sessionId: any;
  public tabId: string;
  public isIVEnabled = false;
  public bucSessionStorageService: BucSessionService;
  public bucSessionStorageService2: BucSessionService;
  public searchTabSessionStorageService: BucSessionService;
  sessionPrefix = 'call-center-order-search'; // make sure matches search component
  prefix = '';
  searchCriteria: any[] = [];
  loaded = false;
  public isPageInitialized = false;
  protected _searchByCtx: any;
  protected _grpPaths: any;
  protected enterprise = {
    selectedEnterprise: '',
    selectedEnterpriseList: [],
    currency: undefined
  };
  public searchName;
  public selectedSavedSearch;
  public saveSearchPageType;
  public tableFilterModel = new BucTableFilterModel();
  protected readonly sPipe: BucFieldFilterSortPipe;
  public hasSearched = false;
  public showFilter = true;
  public i18nDatePlaceholder;
  public datePickerLabel = '';
  public datePickerRangeLabel = '';
  public datePickPlaceholder = '';
  public fromApplyFilters = false;
  public searchText = '';
  public isDirectSearch = false;
  public searchTabId: string;
  collapseIconPlacement = 'bottom';
  collapseIconAlignment = 'end';
  collapseIconContent = 'ORDER_SEARCH.GENERAL.LABEL_COLLAPSE_ICON';
  tableDatadetails;
  pageNo = 1;
  pageSize = BucTableModel.DEFAULT_PAGE_LEN;
  sortKey = 'OrderDate';
  sortOrder = 'Desc';
  maximumRecords: number = Constants.DEFAULT_SEARCH_MAX_RECORDS;
  pageModel = {};
  private _firstInit = true;
  private _filtersByFieldMap: { [id: string]: any };
  @ViewChild(TableFilterComponent, { static: false }) filterComponent: TableFilterComponent;

  constructor(
    nSvc: BucNotificationService,
    public mSvc: ModalService,
    public tSvc: TranslateService,
    public route: ActivatedRoute,
    public bSvc: BreadcrumbService,
    restSvc: BucCommOmsRestAPIService,
    public resMapSvc: BucPageResourceMappingService,
    private ccNavigationSvc: CallCenterNavigationService,
    public bucResourceMap: BucPageResourceMappingService,
    inj: Injector,
    public searchSvc: OrderSearchDataService,
    private store$: Store
  ) {
    super(mSvc, nSvc, tSvc, route, resMapSvc, restSvc, inj, searchSvc);
  }

  ngOnInit() {
    this.initialize();
  }

  protected getSession() {
    return this.bucSessionStorageService;
  }

  public getOps() {
    return getArray(this._readFromSession('ops', []));
  }

  async prepareBreadCrumbList() {
    const c = await this.tSvc.get('ORDER_SEARCH.GENERAL.LABEL_RESULTS').toPromise();
    const r = getPathFromRoot(this.route.snapshot);
    this.bSvc.updateLast(c, r, c, [r], { queryParams: this.route.snapshot.queryParams });
    this.breadCrumbList = this.bSvc.get();
  }

  protected async initialize(onFilterChange = false) {
    this.sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    this.tabId = this.route.snapshot.queryParams.uniqueId;
    this.i18nDatePlaceholder = getCurrentLocaleDateFormat();
    this.setKeyToSession(this.route.snapshot.queryParams.searchTabId, this.sessionPrefix);
    if (this._firstInit) {
      this.i18nDatePlaceholder = getCurrentLocaleDateFormat();
      await this._initTranslations();
      this.disableSearchFields();
      this.initializeSession();
      this.prepareBreadCrumbList();
    }
    this.isDirectSearch = this.initializeDirectSearchNavigation();
    this.readSessionValues(this.sessionPrefix);
    if (this.isDirectSearch && this.searchText) {
      this.fetchSessionEnterprise();
      await this._fetchFields();
      this._mergeOobAndPreferences();

      this.prepareShowDraftOrders(this.isDirectSearch);
      this.prepareOrderLineStatus();
      const p = [
        this.prepareOrderAge(),
        this.prepareEntryTypeList()
      ];
      await Promise.all(p);

      this._setSearchByForCurrent();
      await this.prepareSearch();
    } else {
      this.initializeSearchTabNavigation();
    }

    // this.prepareBreadCrumbList();
    await this.initializeCache();

    this.setReplacementMapping([{ key: 'enterprise', value: 'selectedEnterprise' }]);
    await this.loadForm();

    this.initializeFilter();
    await this.onSearch(onFilterChange);
    this.preparePageData();
    this.isScreenInitialized = true;
    this._firstInit = false;
    this.loaded = true;
  }

  async preparePageData() {
    this.datePickerLabel = this.nlsMapResult['order-search-result.SCHEMATICS.SEARCH_RESULT.DUMMY_START_DT_LABEL'];
    this.datePickerRangeLabel = this.nlsMapResult['order-search-result.SCHEMATICS.SEARCH_RESULT.DUMMY_START_DT_LABEL'];
    this.datePickPlaceholder = this.nlsMapResult['order-search-result.SCHEMATICS.SEARCH_RESULT.DUMMY_DATE_FORMAT'];
  }

  async _initTranslations() {
    const keys = Object.keys(this.nlsMapResult);
    const json = await this.tSvc.get(keys).toPromise();
    keys.forEach(k => this.nlsMapResult[k] = json[k]);
  }

  initializeDirectSearchNavigation() {
    const searchText = this.bucSessionStorageService.getItem('orderSearchText');
    const searchTextFromUrl = this.route.snapshot.queryParams.q;
    // check if portlet / category search value was never set / updated
    if (searchText !== '' && searchTextFromUrl) {
      this.searchText = searchText || searchTextFromUrl;
      this.bucSessionStorageService.setItem('orderSearchText', this.searchText);
    }
    return this.searchText ? true : false;
  }

  initializeSearchTabNavigation() {
    this.searchTabId = this.route.snapshot.queryParams.searchTabId;
    if (this.searchTabId) {
      this.searchTabSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.searchTabId}`);
      // search text could be updated via table toolbar, so check for that possibility as well
      this.searchText = defaultTo(this.bucSessionStorageService.getItem('orderSearchText'), '');
      this.bucSessionStorageService.setItem('orderSearchText', this.searchText);
    }
  }

  initializeSession() {
    this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.tabId}`);
    this.bucSessionStorageService2 = new BucSessionService(this.sessionPrefix, this.sessionId);
  }

  async initializeCache() {
    this.isPageInitialized = defaultTo(this.bucSessionStorageService.getItem('isPageInitialized'), false);
    this.enterprise = this._readFromSession('selectedEnterprise', {});
    this.searchSettings.searchBy = this._readFromSession('orderSearchBy', undefined);
    this._searchByCtx = this._readFromSession('orderSearchByCtx', undefined);
    this._grpPaths = this._readFromSession('orderGroupPaths', undefined);
    this.selectedSavedSearch = this._readFromSession('selectedSavedSearch', '');
    this.searchCriteria = this._readFromSession('orderSearchCriteria', {});
  }

  initializeFilter() {
    this._initGroupSearchCriteriaWithCustom(this._searchByCtx);
    this.tableFilterModel.title = this.nlsMapResult['ORDER_SEARCH.GENERAL.LABEL_FILTER_TITLE'];
    this.tableFilterModel.version = 'default';
    this.tableFilterModel.items = this.searchCriteria;
    this.searchCriteria.forEach(criteria => { if (!criteria.expanded) { criteria.expanded = !criteria.expanded } });
    this.updateItemList(this.searchCriteria);
    if (this.filterComponent) {
      this.filterComponent.updateCurrentAppliedFilter();
    }
  }

  toggleFilter() {
    this.showFilter = !this.showFilter;
    this.searchCriteria = this._readFromSession('orderSearchCriteria', []);
    this.initializeFilter();
  }

  onEmptySearch(filterOption) {
    const updatedOptions = filterOption.items.map(option => {
      option.selected = false;
      return option;
    });
    filterOption.items = updatedOptions;
    filterOption.value = {
      ...filterOption.value,
      value: '',
      searchValue: '',
      canReset: true,
      id: filterOption.id,
      fieldId: filterOption.id
    };
    this.tableFilter.uncheckSavedSearchDropdown();
  }

  onOrderLineStatusSelected(event, filterOption) {
    const updatedOptions = filterOption.items.map(option => {
      option.selected = false;
      if (option.value == event.item.value) {
        option.selected = true;
      };
      return option;
    });
    filterOption.items = updatedOptions;
    filterOption.value = {
      ...filterOption.value,
      value: event.item.content,
      searchValue: event.item.value,
      canReset: true,
      id: filterOption.id,
      fieldId: filterOption.id
    };
    this.tableFilter.uncheckSavedSearchDropdown();
  }

  onOrderTotalRangeSelected(event: { selected?: AmountRange, items: Array<any> }, filterOption) {
    if (event.selected) {
      filterOption.invalid = event.selected.invalid;
      filterOption.value = {
        ...filterOption.value,
        selected: event.selected,
        value: event.selected.displayLabel,
        range: event.selected.range,
      };
    } else {
      filterOption.invalid = false;
      filterOption.value = {
        ...filterOption.value,
        selected: undefined,
        value: '',
        range: {}
      };
    }
    filterOption.items = event.items;
    this.tableFilter.uncheckSavedSearchDropdown();
  }

  async updateItemList(event: Array<BucTableFilterCategoryModel>) {
    const old = cloneDeep(this.enterprise);
    const filtersOnly = event.reduce((all, { options }) => [...all, ...options], []);
    filtersOnly.forEach(o => this._updateSingle(o));
    this._filtersByFieldMap = BUCCOMMON.toMap(filtersOnly, 'fieldId');
    if (old.selectedEnterprise !== this.enterprise.selectedEnterprise) {
      await this._cascade();
    }
  }

  async onSearch(onFilterChange = false) {
      this.hasSearched = true;
      this.bucSessionStorageService.setItem('selectedEnterprise', this.enterprise);
      this.bucSessionStorageService.setItem('isPageInitialized', this.isPageInitialized);

      if (!onFilterChange) {
        this.store$.dispatch(
          PaginationActions.startPagination({
            id: Constants.ORDER_RESULTS_TABLE_COMPONENT_ID,
            loadCriteria: {
              searchCriteria: {
                oob: cloneDeep(this.searchCriteria),
                custom: cloneDeep(this.getOps())
              },
              searchText: this.searchText,
              enterprise: this.enterprise.selectedEnterprise,
              enterprises: this.selectedEnterpriseList
            },
            sortCriteria: undefined
          })
        );
      } else {
        this.store$.dispatch(
          PaginationActions.updateLoadCriteria({
            id: Constants.ORDER_RESULTS_TABLE_COMPONENT_ID,
            loadCriteria: {
              searchCriteria: {
                oob: cloneDeep(this.searchCriteria),
                custom: cloneDeep(this.getOps())
              },
              searchText: this.searchText,
              enterprise: this.enterprise.selectedEnterprise,
              enterprises: this.selectedEnterpriseList
            },
            sortCriteria: undefined
          })
        );
      }
  }



  private async _cascade() {
    const p = [
      this.prepareHoldTypeList(this.enterprise.selectedEnterprise),
      this.prepareEntryTypeList()
    ];

    await Promise.all(p);
  }

  private _getFilterItems() {
    const m = BUCCOMMON.toMap(this.searchCriteria, 'id');
    if (this._searchByCtx && this._searchByCtx.value) {
      const f = m[this._searchByCtx.value].options;
      const c = { oob: f, custom: this.getCustomFilters() };
      const u = this.sPipe.sortFilters(c, { ctx: this._searchByCtx });
      f.splice(0, f.length, ...u);
    }
  }

  private async _saveCriteriaAndFilters(allFilters: Array<BucTableFilterCategoryModel>) {
    /* Save Filter Logic goes here */
    const properFilters = await this.rebindCustomFilters(allFilters);
    await this.updateItemList(properFilters);
    const filters = SearchForm.separateFilters(properFilters);
    this.saveFilters();
    this.saveCustomization(this.bucSessionStorageService, this.getOps());
    this.bucSessionStorageService.setItem('orderSearchCriteria', filters.oob);
  }

  async applyFilters(event) {
    this.resetOrderTotalSelection(event);
    this.resetOrderLineStatusSelection();
    this.fromApplyFilters = true;
    this.hasSearched = true;
    await this._saveCriteriaAndFilters(event);
    this.bucSessionStorageService.setItem('selectedEnterprise', this.enterprise);
    this.searchCriteria = this._readFromSession('orderSearchCriteria', []);
    this.bucSessionStorageService.setItem('invocationCtx', 'OrderSearchResultComponent');
    this.orderTableComponent.search();
    this.initialize(true);
  }

  async clearFilters(event) {
    this.bucSessionStorageService.removeItem('selectedEnterprise');
    this.selectedSavedSearch = null;
    await this._saveCriteriaAndFilters(event);
    this.bucSessionStorageService.removeItem('selectedSavedSearch');
    this.initialize(true);
  }

  enterpriseChange(event) {
    if (event?.item?.value !== this.enterprise.selectedEnterprise) {
      this.updateHoldType('holdOrderType', []);
      this.updateHoldType('holdLineType', []);
      this.prepareHoldTypeList(event?.item?.value);
    }
  }

  searchResultChange(c: SearchResultTableData) {
    this.doneLoadingResults = true;
    // go directly to order if only one result and we were invoked from the search-page
    //const callerCtx = this.bucSessionStorageService2.getItem('invocationCtx');
    const pageParams = this.route.snapshot.queryParams;
    this.bucSessionStorageService2.removeItem('invocationCtx');
    if (c.count === 1 && (!pageParams.breadcrumb || this.fromApplyFilters)) {
        c.table.openOrderDetails(c.data[0], this.route.snapshot.queryParams);
    } else {
      this.moreThanOneResult = true;
    }
  }

  selectedSavedSearchChange(savedSearchKey) {
    this.selectedSavedSearch = savedSearchKey;
    this.bucSessionStorageService.setItem('selectedSavedSearch', savedSearchKey);
  }

  resetOrderTotalSelection(filters: BucTableFilterCategoryModel[]) {
    const orderTotal = this._filtersByFieldMap[this.FIELD_ID_ORDER_TOTAL];
    if (BucBaseUtil.isVoid(orderTotal.value)) {
      orderTotal.items = orderTotal.items.map(i => ({
        ...i,
        selected: false,
        displayLabel: '',
        ...(i.id === 'freeform' ? { range: {}, label: '', invalid: true } : {})
      }));
    }
  }

  resetOrderLineStatusSelection() {
    const lineStatus = this._filtersByFieldMap[this.FIELD_ID_ORDER_LINE_STATUS];
    if (BucBaseUtil.isVoid(lineStatus.value)) {
      lineStatus.items = lineStatus.items.map(i => ({
        ...i,
        selected: false
      }));
    }
  }

  protected async _updateCascade(fieldId, list) {
    if (BucBaseUtil.isUndefinedOrNull(this._filtersByFieldMap)) {
      super._updateCascade(fieldId, list);
    } else {
      const option = this._filtersByFieldMap[fieldId];
      if (option) {
        switch (option.element) {
          case 'dropdown':
            option.items = list;
            this._updateSingle(option);
            if (typeof option.value === 'string') {
              option.value = get(option.items.find(item => item.selected), 'value', '');
            } else if (typeof option.value !== 'undefined') {
              option.value = defaultTo(option.items.find(item => item.selected), {});
            }
            break;
          case 'dropdownRange':
            option.items[0] = list;
            option.items[1] = cloneDeep(list);
            this._updateSingle(option);
            option.value.from = get(option.items[0].find(e => e.selected), 'value', '');
            option.value.to = get(option.items[1].find(e => e.selected), 'value', '');
            break;
          case 'comboBox':
            option.items = list;
            this._updateSingle(option);
            option.value = list.filter(item => item.selected);
            break;
          default:
            break;
        }
      }
    }
  }

  private _updateSingle(option: any) {
    switch (option.element) {
      case 'enterprise':
        this.enterprise = cloneDeep(option.value);
        break;
      case 'dropdownQuery':
      case 'dropdownQueryNumeric':
        option.items.forEach(item => item.selected = item.value === option.value.qryType);
        break;
      case 'dropdown':
        if (typeof option.value === 'string') {
          if (option.value === '') {
            option.items.forEach(item => {
              item.selected = item.default ? true : false;
              option.value = item.default ? item.value : option.value;
            });
          } else {
            option.items.forEach(item => item.selected = item.value === option.value);
          }
        } else if (typeof option.value === 'object') {
          const objectKeys = Object.keys(option.value).filter(k => k !== 'selected');
          if (objectKeys.length === 0) {
            option.items.forEach(item => item.selected = false);
          } else {
            option.items.forEach(item => item.selected = objectKeys.every(k => option.value[k] === item[k]));
          }
        }
        break;
      case 'dropdownRange':
        option.items[0].forEach(item => item.selected = item.value === option.value.from);
        option.items[1].forEach(item => item.selected = item.value === option.value.to);
        break;
      case 'comboBox':
        if (typeof option.value === 'string') {
          option.items.forEach(item => item.selected = item.value === option.value);
        } else {
          const idList = option.value.map(v => v.id);
          option.items.forEach(item => item.selected = idList.includes(item.id));
        }
        break;
    }
  }

  protected _getFieldValue(id) {
    // if not initialized, use parent -- likely a workspace scenario
    if (BucBaseUtil.isUndefinedOrNull(this._filtersByFieldMap)) {
      return super._getFieldValue(id);
    } else {
      return this._filtersByFieldMap[id].value;
    }
  }

  protected getEntStorageVal() {
    return this._getFieldValue('enterprise');
  }

  searchTextChanged(searchValue: string) {
    this.searchText = searchValue;
    this.bucSessionStorageService.setItem('orderSearchText', this.searchText);

    this.store$.dispatch(PaginationActions.updateLoadCriteria({
      id: Constants.ORDER_RESULTS_TABLE_COMPONENT_ID,
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

  createOrder() {
    this.ccNavigationSvc.openUrlInNewTab(Constants.CREATE_ORDER_ROUTE,
      {});
  }

  /* SearchForm abstract -- extenders must implement. Remove if not extending SearchForm */
  protected prepareSearch() {
    this.updateSearchCriteria();
    this.saveForm();
    if (!this.bucSessionStorageService.getItem('orderSearchCriteria')) {
      this.bucSessionStorageService.setItem('orderSearchCriteria', this.searchCriteria);
    }
  }

  public updateSearchCriteria() {
    if (this.searchSettings.searchByItems && this.searchSettings.searchByItems.length > 0) {
      this.searchCriteria = this._initGroupSearchCriteria(true);
      this.resetHiddenFields();
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
