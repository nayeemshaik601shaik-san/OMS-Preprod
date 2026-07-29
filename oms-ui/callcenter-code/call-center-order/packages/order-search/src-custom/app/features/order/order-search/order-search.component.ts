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

import { AfterViewInit, Component, Injector, OnInit, QueryList, TemplateRef, ViewChild, ViewChildren } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BreadcrumbService, getPathFromRoot, OrderListDataService, OrderSearchDataService, OrderSearchForm } from '@call-center/order-shared';
import { ActivatedRoute, Router } from '@angular/router';
import {
  BucSessionService, getCurrentLocale, getCurrentLocaleDateFormat, getFlatPickrDateFormat,
  SelectEnterpriseComponent, EnterpriseItem, BucFieldRenderPipe, getMoment,
  BucNotificationService, BucTableFilterCategoryModel, BucDateTimeHelper,
  TemplateIdDirective
} from '@buc/common-components';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BucCommOmsMashupService, BucCommOmsRestAPIService, BucPageResourceMappingService, BucSvcAngularStaticAppInfoFacadeUtil, CallCenterNavigationService } from '@buc/svc-angular';
import { ModalService } from 'carbon-components-angular';
import { Constants } from 'packages/order-shared/src/lib/common/order.constants';
import { cloneDeep } from 'lodash';
import { ExtensionConstants } from '../../extension.constants';
import { AmountRange } from '@call-center/order-shared';
@Component({
  selector: 'call-center-order-search',
  templateUrl: './order-search.component.html',
  styleUrls: ['./order-search.component.scss']
})
export class OrderSearchComponent extends OrderSearchForm implements OnInit {
  EXTENSION = {
    TOP: ExtensionConstants.ORDER_SEARCH_OS_TOP,
    BOTTOM: ExtensionConstants.ORDER_SEARCH_OS_BOTTOM
  };

  componentId = "OrderSearch";
  static readonly DEFAULT_QRY_TYPE = 'LIKE';
  static readonly DEFAULT_NUMERIC_QRY_TYPE = 'EQ';
  static readonly DEFAULT_TIME_PERIOD = 'AM';
  defaultLabels = {
    searchByLabel: '',
    customizationLabel: '',
    pageHeaderDes: ''
  };
  protected renderingPath = ['searchBy'];
  public searchDirection: string;
  public isScreenInitialized = false;
  public breadCrumbList: any[];
  public currentRoute = 'order-list';
  readonly nlsMap: any = {
    'ORDER_SEARCH.GENERAL.LABEL_ADVANCED_SEARCH': '',
    'ORDER_SEARCH.GENERAL.LABEL_QUERY_is': '',
    'ORDER_SEARCH.GENERAL.LABEL_QUERY_starts_with': '',
    'ORDER_SEARCH.GENERAL.LABEL_QUERY_contains': '',
  };

  private customTemplates: { [id: string]: TemplateRef<any> } = {};
  @ViewChildren(TemplateIdDirective) set _templates(a: QueryList<TemplateIdDirective>) {
    if (a) {
      a.forEach(({ id, template }) => (this.customTemplates[id] = template));
    }
  }

  tenantId = '';
  sessionId: string;
  tabId: string;
  sectionTitle = '';
  public pageHeaderDes: SafeHtml;
  protected readonly sanitizer: DomSanitizer;
  protected bucSessionStorageService: BucSessionService;
  protected bucSessionStorageService2: BucSessionService;
  protected bucSessionStorageServiceGlobal: BucSessionService;
  prefix = '';
  sessionPrefix = 'call-center-order-search';
  searchCriteria = [];
  currLocale;
  i18nDatePlaceholder: string;
  flatpickrDateFormat: string;
  protected stringQueryOptions = [];
  protected numericQueryOptions = [];
  userData;
  staticUserData;
  isPageInitialized = false;
  selectedSavedSearch;
  protected readonly rPipe: BucFieldRenderPipe;
  searchFilters: any = [];
  selectedSavedSearchCriteria;
  pageModel = {};
  constructor(
    public router: Router,
    public translate: TranslateService,
    protected dataSvc: OrderListDataService,
    public searchSvc: OrderSearchDataService,
    public bucCommOmsRestAPIService: BucCommOmsRestAPIService,
    public bcSvc: BreadcrumbService,
    public ccNavigationSvc: CallCenterNavigationService,
    public modalSvc: ModalService,
    public bucNS: BucNotificationService,
    public route: ActivatedRoute,
    resMapSvc: BucPageResourceMappingService,
    restSvc: BucCommOmsRestAPIService,
    private bucCommOmsMashupService: BucCommOmsMashupService,
    inj: Injector
  ) {
    super(modalSvc, bucNS, translate, route, resMapSvc, restSvc, inj, searchSvc);
    this.sanitizer = inj.get(DomSanitizer);
    this.rPipe = inj.get(BucFieldRenderPipe);
  }

  protected getSession() {
    return this.bucSessionStorageService;
  }

  ngOnInit() {
    this.initialize();
  }

  async prepareBreadcrumbList() {
    const c = await this.translate.get('ORDER_SEARCH.GENERAL.LABEL_ADVANCED_SEARCH').toPromise();
    const r = getPathFromRoot(this.route.snapshot);
    this.bcSvc.updateLast(c, r, c, [r], { queryParams: this.route.snapshot.queryParams });
    this.breadCrumbList = this.bcSvc.get();
  }

  async initialize(clearForm = false) {
    let p;
    // this.cascading = false;
    this.sectionTitle = await this.translate.get('ORDER_SEARCH.GENERAL.LABEL_SEARCH_BY_ORDER_INFO').toPromise();
    this.tenantId = BucSvcAngularStaticAppInfoFacadeUtil.getSelectedTenantId();
    this.sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    this.tabId = this.route.snapshot.queryParams.uniqueId;
    await this._initTranslations();

    // async that do not require a wait
    (async () => this.setReplacementMapping([
      { key: 'enterprise', value: 'oobFieldsMap.enterprise.storage.value.selectedEnterprise' }
    ]))();

    this.initializeSession();
    this.fetchSessionEnterprise();
    this.readSessionValues(this.sessionPrefix, clearForm);
    await this._fetchFields();
    this.currLocale = getCurrentLocale();
    if (this.currLocale.startsWith('zh-')) {
      this.currLocale = 'zh';
    }
    this.i18nDatePlaceholder = getCurrentLocaleDateFormat();
    this.flatpickrDateFormat = getFlatPickrDateFormat();
    p = [
      this.getCurrentUser(),
      this.loadForm()
    ];
    await Promise.all(p);
    await this.prepareSearchFields();
    this.fieldsContainer.custom = this.getCustomFields();
    this.initCustomTemplates(this.customTemplates);
    this.isPageInitialized = true;
    this._mergeOobAndPreferences();
    await this.updateGeneralUI();
    this.prepareHoldTypeList(this.getEntStorageVal().selectedEnterprise);
    this._setSearchByForCurrent();
    await this.updateUI();
    if (clearForm) {
      this.clearCustomFields();
    }
    else {
      this.prepareBreadcrumbList();
    }
    this.isScreenInitialized = true;
  }

  async prepareSearchFields(){
    this.prepareOrderLineStatus();
    this.prepareShowDraftOrders(false);
    const p = [
      this.prepareOrderAge(),
      this.prepareEntryTypeList(),
    ];
    await Promise.all(p);
  }

  protected async _initTranslations() {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translate.get(keys).toPromise();
    keys.forEach(k => this.nlsMap[k] = json[k]);
  }

  initializeSession() {
    this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.tabId}`);
    // need session storage not associated with tabId for redirecting to order details when it's only 1 result
    this.bucSessionStorageService2 = new BucSessionService(this.sessionPrefix, this.sessionId);
    this.bucSessionStorageService.setItem('activeTab', 'orders');
    this.bucSessionStorageService.setItem('orderPageNo', 1);
    this.searchCriteria = this.bucSessionStorageService.getItem('orderSearchCriteria') || [];
    this.selectedSavedSearch = this.bucSessionStorageService.getItem('selectedSavedSearch');
  }

  async getCurrentUser() {
    this.userData = await this.dataSvc.getCurrentUser(this.tenantId);
    this.staticUserData = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentUser();
  }

  public updateSearchCriteria() {
    if (this.searchSettings.searchByItems.length > 0) {
      this.searchCriteria = this._initGroupSearchCriteria(true);
      this.resetHiddenFields();
    }
  }

  prepareSearch() {
    // save filter to cache for order list to render
    this.bucSessionStorageService.setItem('appliedFiltersList', this.searchFilters);
    this.updateSearchCriteria();
    this.bucSessionStorageService.setItem('orderSearchCriteria', this.searchCriteria);
    this.bucSessionStorageService.setItem('orderSearchBy', this.searchSettings.searchBy);
    this.bucSessionStorageService.setItem('orderSearchByCtx', this.searchSettings.searchByCtx);
    this.bucSessionStorageService.setItem('isPageInitialized', true);
    this.bucSessionStorageService2.setItem('invocationCtx', 'OrderSearchComponent');
    this.bucSessionStorageService.setItem('orderGroupPaths', this.grpHier.paths);
    const e = this.getEntStorageVal();
    this.bucSessionStorageService.setItem('selectedEnterprise', {
      selectedEnterprise: e.selectedEnterprise,
      selectedEnterpriseList: e.selectedEnterpriseList
    });
    if (this.selectedSavedSearch) {
      this.bucSessionStorageService.setItem('selectedSavedSearch', this.selectedSavedSearch);
    } else {
      this.bucSessionStorageService.removeItem('selectedSavedSearch');
    }
    this.saveCustomization(this.bucSessionStorageService, this.getOps());
    this.saveForm();
  }

  querySearch() {
    let invalid = false;
    this.searchCriteria
      .forEach(({ options }) => invalid = invalid || options.some(o => this.getOobField(o.fieldId).storage.invalid));
    if (invalid) {
      return;
    }
    this.prepareSearch();
    this.navigateToResults();
  }

  protected resultsRoute() {
    return Constants.ORDER_SEARCH_RESULT_ROUTE;
  }

  navigateToResults() {
    this.ccNavigationSvc.openUrlInSameTab(this.resultsRoute(), { searchTabId: this.tabId });
  }

  clearForm() {
    this.bucSessionStorageService.removeItem('orderSearchCriteria');
    this.bucSessionStorageService.setItem('orderSearchCriteria', []);
    this.bucSessionStorageService.removeItem('hasSearched');
    this.bucSessionStorageService.removeItem('selectedSavedSearch');
    this.clearSearchCriteria();
    this.onEntClear();
    this.initialize(true);
  }

  prepForSave() {
    this.updateSearchCriteria();

    // bind a category for custom-fields
    const options = this.fieldsContainer.custom;
    const custom = Object.assign(new BucTableFilterCategoryModel({ title: '', id: '', options, expanded: true }), { type: 'custom', });
    // save
    this.savedSearchCriteria = cloneDeep([... this.searchCriteria, custom]);
  }

  raisedDateChange(event, option) {
    option.storage.value.date = event[0] ? BucDateTimeHelper.getMoment(event[0]).endOf('day').toISOString() : '';
    this.uncheckSavedSearchDropdown();
  }

  onTimeChange(event, option) {
    option.storage.value.time = event.time ? event.time : '';
    if (event.timePeriod) {
      option.storage.value.period = event.timePeriod;
    }
    this.uncheckSavedSearchDropdown();
  }

  onDateChange(event, option) {
    let value = event;
    // If event is { value: [], option: [] }
    if (typeof event === 'object') {
      value = event.value;
      option.storage.value.option = event.option;
    }
    if (value === undefined || value.length === 0) {
      option.storage.value.from = '';
      option.storage.value.to = '';
    } else if (value.length === 1) {
      option.storage.value.from = getMoment(value[0]).toISOString();
      option.storage.value.to = '';
    } else if (value.length === 2) {
      option.storage.value.from = getMoment(value[0]).toISOString();
      option.storage.value.to = getMoment(value[1]).toISOString();
    }
    this.uncheckSavedSearchDropdown();
  }

  onOrderLineStatusSelected(event, f){
    f.storage.items.forEach(option => {
      option.selected = false;
      if(option.value == event.item.value){
        option.selected = true;
      };
    });
    f.storage.value = {
      ...f.storage.value,
      searchValue: event.item.value, // for search
      value: event.item.content // for display
    };
    this.uncheckSavedSearchDropdown();
  }

  onEmptySearch(event, f){
    if(event == ''){
      f.storage.items.forEach(option => {
        option.selected = false;
      });
      f.storage.value = {
        ...f.storage.value,
        searchValue: '', // for search
        value: '' // for display
      };
      this.uncheckSavedSearchDropdown();
    }
  }

  onOrderTotalRangeSelected(event: {selected?: AmountRange, items: Array<any>}) {
    const storage = this.getOobField(this.FIELD_ID_ORDER_TOTAL).storage;
    if (event.selected) {
      storage.invalid = event.selected.invalid;
      storage.selected = event.selected;
      storage.content = event.selected.displayLabel;
      storage.value = event.selected.range;
    } else {
      storage.invalid = false;
      storage.selected = undefined;
      storage.value = {};
      storage.content = '';
    }
    storage.items = event.items;
    this.uncheckSavedSearchDropdown();
  }

  async selectEnterprise(event: { item: EnterpriseItem }, field, ref: SelectEnterpriseComponent, update = true) {
    let changed = false;
    if (event.item) {
      if (field.storage.value.selectedEnterprise !== event.item.value) {
        this.onEntSelReInit(event, field, ref, update);
        changed = true;
      }
    } else {
      Object.assign(field.storage.value, { content: '', selectedEnterpriseList: [], selectedEnterprise: '' });
      changed = true;
    }

    if (update && changed) {
      this._updateByFiltersWKO(this.searchCriteria);
      this.updateSearchCriteria();
      this.updateHoldType('holdOrderType', []);
      this.updateHoldType('holdLineType', []);
      if (event.item) {
        this.prepareHoldTypeList(event.item.value);
        this.updateEntryType(event.item.value);
      } else {
        this.updateEntryType();
      }
    }
    this.processForm();
  }

  onEntSearch(e, f, ref: SelectEnterpriseComponent) {
    f.storage.invalid = false;
    if(e !== '') {
      if (!f.storage.isReset) {
        const c = { value: undefined };
        const invalid = !this._onSearchValid(e, ref.list, c) || !c.value;
        if (!invalid) {
          f.storage.onSearch = true;
          this.selectEnterprise({ item: c.value }, f, ref);
        } else {
          f.storage.invalid = true;
        }
      }
    }
  }

   //EOMS-2409
   onInputChange(e: any, f: any){
    if(f.id === 'orderNumber' || f.id === 'firstName' || f.id === 'lastName' ||
      f.id === 'postalCode' || f.id === 'email'){
        if(f.storage?.value)
          f.storage.value = f.storage.value.toUpperCase();
      }
  }
}
