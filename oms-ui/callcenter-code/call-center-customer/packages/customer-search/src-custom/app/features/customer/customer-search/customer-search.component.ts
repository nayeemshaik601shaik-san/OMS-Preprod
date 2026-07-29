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

import {
  Component,
  Injector,
  OnInit,
  TemplateRef,
  ViewChild
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  BucSessionService,
  getCurrentLocale,
  getCurrentLocaleDateFormat,
  getFlatPickrDateFormat,
  BucFieldRenderPipe,
  BucFieldFilterSortPipe,
  BucNotificationService,
  EnterpriseItem,
  SelectEnterpriseComponent,
  BucTableFilterCategoryModel
} from '@buc/common-components';
import { BucCommOmsRestAPIService, BucPageResourceMappingService, BucSvcAngularStaticAppInfoFacadeUtil, CallCenterNavigationService } from '@buc/svc-angular';
import { TranslateService } from '@ngx-translate/core';
import { ModalService } from 'carbon-components-angular';
import { cloneDeep } from 'lodash';
import { CommonService, BreadcrumbService, getPathFromRoot, CustomerListDataService, CustomerSearchForm } from '@call-center/customer-shared';

import { DomSanitizer } from '@angular/platform-browser';
import { Constants, TenantFieldMap } from '@call-center/customer-shared/lib/common/customer.constants';
import { ExtensionConstants } from '../../extension.constants';

@Component({
  selector: 'call-center-customer-search',
  templateUrl: './customer-search.component.html',
  styleUrls: ['./customer-search.component.scss']
})
export class CustomerSearchComponent extends CustomerSearchForm implements OnInit {
  tenantId = '';
  prefix = '';
  sessionPrefix = 'call-center-customer-search';
  bcList;
  userData;
  staticUserData;
  searchCriteria = [];
  i18nDatePlaceholder: string;
  flatpickrDateFormat: string;
  curLocale;
  initialized: boolean = false;
  selectedCustomerType = 'consumer';

  protected callbacks: { [fieldId: string]: () => any } = {};
  searchSettings: { searchBy: string, searchByItems: Array<any>, searchByCtx: any, customerType: string } = {
    searchBy: '',
    searchByItems: [],
    searchByCtx: {},
    customerType: ''
  };
  public renderingMap = {
    searchBy: 'searchSettings.searchBy',
    customerType: 'oobFieldsMap.customerType.storage.value'
  };
  public isScreenInitialized = false;
  protected readonly nlsMap: any = {
    'CUSTOMER_SEARCH.SEARCH.ADVANCED_LABEL_SEARCH': '',
    'CUSTOMER_SEARCH.SEARCH.DESCRIPTION': '',
    'CUSTOMER_SEARCH.SEARCH.MESSAGE_ERROR_UpdateCurrentUser': '',
    'CUSTOMER_SEARCH.SEARCH.invalidSearch': '',
    'CUSTOMER_SEARCH.SEARCH.LABEL_BUSINESS': '',
    'CUSTOMER_SEARCH.SEARCH.LABEL_CONSUMER': '',
    'CUSTOMER_SEARCH.SEARCH.LABEL_ALL': ''
  }
  readonly resourceIds = {
    CREATE_BUSINESS_CUSTOMER: 'ICC000055'
  };
  hasCreateBusinessPermission = false;
  sessionId: string;
  tabId: string;
  sectionTitle = '';

  public defaultLabels = {
    searchByLabel: '',
    customizationLabel: '',
    pageHeaderDes: ''
  };

  EXTENSION = {
    TOP: ExtensionConstants.CUSTOMER_SEARCH_CD_TOP,
    BOTTOM: ExtensionConstants.CUSTOMER_SEARCH_CD_BOTTOM
  };
  public componentId = 'call-center-customer-search';
  protected renderingPath = ['searchBy', 'customerType'];
  public searchDirection: string;
  protected bucSessionStorageService: BucSessionService;
  protected readonly rPipe: BucFieldRenderPipe;
  protected readonly sPipe: BucFieldFilterSortPipe;
  protected readonly sanitizer: DomSanitizer;
  protected readonly commonService: CommonService;
  public breadCrumbList: any[];
  tenantFieldMap: any = TenantFieldMap;
  enterpriseList
  searchFilters: any = [];
  constructor(
    private translate: TranslateService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    private bcSvc: BreadcrumbService,
    resMapSvc: BucPageResourceMappingService,
    restSvc: BucCommOmsRestAPIService,
    inj: Injector,
    public ccNavigationSvc: CallCenterNavigationService,
    protected modalSvc: ModalService,
    protected bucNS: BucNotificationService,
    protected dataSvc: CustomerListDataService
  ) {
    super(modalSvc, bucNS, translate, activatedRoute, resMapSvc, restSvc, inj);
    this.sanitizer = inj.get(DomSanitizer);
    this.rPipe = inj.get(BucFieldRenderPipe);
  }

  ngOnInit() {
    this.initialize();
  }

  protected getSession() {
    return this.bucSessionStorageService;
  }

  async initialize(clearForm = false) {
    let p;
    this.sectionTitle = await this.translate.get('CUSTOMER_SEARCH.GENERAL.LABEL_CHOOSE_CUSTOMER_TYPE').toPromise();
    this.tenantId = BucSvcAngularStaticAppInfoFacadeUtil.getSelectedTenantId();
    this.sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    this.hasCreateBusinessPermission = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIds.CREATE_BUSINESS_CUSTOMER);
    this.tabId = this.route.snapshot.queryParams.uniqueId;

    // async that do not require a wait
    (async () => this.setReplacementMapping([
      { key: 'enterprise', value: 'oobFieldsMap.enterprise.storage.value.selectedEnterprise' }
    ]))();
    this.initializeSession();
    this.getCurrentUser();

    this.disableSearchFields();

    await this._initTranslations();

    if (clearForm) {
      this.sessionEnterprise = '';
    } else {
      this.prepareBreadCrumbList();
      this.fetchSessionEnterprise();
    }

    await this._fetchFields();

    this.curLocale = getCurrentLocale();
    if (this.curLocale.startsWith('zh-')) {
      this.curLocale = 'zh';
    }
    this.i18nDatePlaceholder = getCurrentLocaleDateFormat();
    this.flatpickrDateFormat = getFlatPickrDateFormat();
    // done render -- load everything else
    this.isPageInitialized = true;
    p = [
      this.getCurrentUser(),
      this.loadForm()
    ];
    await Promise.all(p);
    this.fieldsContainer.custom = this.getCustomFields();
    await this.updateGeneralUI();

    this._setSearchByForCurrent();
    await this.updateUI(); // update the previous saved values
    if (clearForm) {
      this.clearCustomFields();
    }
    if(!this.hasCreateBusinessPermission){ // remove radio selection if no permission.
      this.getOobField('customerType').storage.hidden = true;
    } else {
      const customerType = this.getOobField('customerType').storage.value;
      this.toggleSearchFieldByCustomer(customerType ? customerType : 'consumer');
    }
    this.actionsDisabled = false;
    this.isScreenInitialized = true;
  }

  protected async _initTranslations() {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translate.get(keys).toPromise();
    keys.forEach(k => this.nlsMap[k] = json[k]);
  }

  async prepareBreadCrumbList() {
    const c = await this.translate.get('CUSTOMER_SEARCH.SEARCH.ADVANCED_LABEL_SEARCH').toPromise();
    const r = getPathFromRoot(this.route.snapshot);
    this.bcSvc.updateLast(c, r, c, [r], { queryParams: this.route.snapshot.queryParams });
    this.breadCrumbList = this.bcSvc.get();
  }

  clearForm() {
    this.bucSessionStorageService.removeItem('customerSearchCriteria');
    this.bucSessionStorageService.removeItem('hasSearched');
    this.bucSessionStorageService.removeItem('selectedSavedCustomerSearch');
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

  querySearch() {
    let invalid = false;
    this.searchCriteria
      .forEach(({ options }) => {
        invalid = options.some(o => this.getOobField(o.fieldId).storage.invalid);

        // Enterprise is required to search for customers
        options.forEach((o) => {
          if (o.fieldId === "enterprise" && !o.value.selectedEnterprise) {
            invalid = true;
            this.getOobField(o.fieldId).storage.invalid = true;
          }
        })

        // Exit foreach
        if (invalid) {
          return;
        }
      });
    if (invalid) {
      return;
    }
    this.prepareSearch();
    this.navigateToResults();
  }

  protected resultsRoute() {
    return Constants.CUSTOMER_SEARCH_RESULT_ROUTE;
  }

  navigateToResults() {
    this.ccNavigationSvc.openUrlInSameTab(this.resultsRoute(), { searchTabId: this.tabId });
  }

  initializeSession() {
    this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.tabId}`);
    this.bucSessionStorageService.setItem('activeTab', 'customer');
    this.bucSessionStorageService.setItem('customerPageNo', 1);
    this.searchCriteria = this.bucSessionStorageService.getItem('customerSearchCriteria') || [];
    this.selectedSavedSearch = this.bucSessionStorageService.getItem('selectedSavedSearch');
  }

  prepareSearch() {
    this.bucSessionStorageService.setItem('appliedFiltersList', this.searchFilters);
    this.updateSearchCriteria();
    this.bucSessionStorageService.setItem('selectedCustomerType', this.selectedCustomerType);
    this.bucSessionStorageService.setItem('customerTypeRadioOptions', this.oobFieldsMap.customerType);
    if(!this.hasCreateBusinessPermission){
      this.searchCriteria[0].options.forEach(op => {
        // if no resource permission then only keep the fields for consumer customers
        if (op.fieldId === 'customerType' || op.fieldId === 'organizationName' || op.fieldId === 'customerId'){
          op.hide = true;
        }
      });
    } 
    this.bucSessionStorageService.setItem('customerSearchCriteria', this.searchCriteria);
    this.bucSessionStorageService.setItem('isPageInitialized', true);
    this.bucSessionStorageService.setItem('invocationCtx', 'CustomerSearchComponent');
    this.bucSessionStorageService.setItem('customerGroupPaths', this.grpHier.paths);
    this.bucSessionStorageService.setItem('customerSearchByCtx', this.searchSettings.searchByCtx);

    const e = this.getEntStorageVal();
    this.bucSessionStorageService.setItem('selectedCustomerEnterprise', {
      selectedEnterprise: e.selectedEnterprise,
      selectedEnterpriseList: e.selectedEnterpriseList
    });
    if (this.selectedSavedSearch) {
      this.bucSessionStorageService.setItem('selectedSavedCustomerSearch', this.selectedSavedSearch);
    } else {
      this.bucSessionStorageService.removeItem('selectedSavedCustomerSearch');
    }
    this.saveCustomization(this.bucSessionStorageService, this.getOps());
    this.saveForm();
  }

  async getCurrentUser() {
    this.userData = await this.dataSvc.getCurrentUserAsObs(this.tenantId);
    this.staticUserData = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentUser();
  }

  async selectEnterprise(event: { item: EnterpriseItem }, field, ref: SelectEnterpriseComponent, update = true) {
    let changed = false;

    if (event.item) {
      if (field.storage.value.selectedEnterprise !== event.item.value || field.storage.value.selectedEnterprise) {
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
    }
    await this.processForm();
  }

  onEntSearch(e, f, ref: SelectEnterpriseComponent) {
    f.storage.invalid = false;
    if (e !== '') {
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

  create() {
    this.ccNavigationSvc.openUrlInNewTab(Constants.CUSTOMER_CREATE_ROUTE, { searchTabId: this.tabId });
  }

  protected _setSearchForDefault() { return; }

  toggleSearchFieldByCustomer(customerType) {
    // hide or resurface the additional field depends on the radio selection
    if (customerType == 'consumer') {
      this.getOobField('organizationName').storage.value = '';
      this.getOobField('organizationName').storage.hidden = true;
      this.getOobField('customerId').storage.hidden = true;
      this.getOobField('customerId').storage.value = '';
    } else { // business
      this.getOobField('organizationName').storage.hidden = false;
      this.getOobField('customerId').storage.hidden = false;
    }
  }

  onRadioChange(event, f){
    // make some of the fields appear or disappear depends on the selection.
    f.storage.items.forEach(cusType => {
      cusType.checked = event.value == cusType.value ? true: false;
    });
    f.storage.value = event.value;
    this.searchSettings.customerType = event.value;
    this.selectedCustomerType = event.value;

    this.toggleSearchFieldByCustomer(event.value);
  }

  //EOMS-1113 (Text visible in uppercase in input fields)
  onInputChange(e: any, f: any): void {
    if (f.storage?.value) {
      f.storage.value  = f?.storage?.value.toUpperCase();
    }
    
  }
}
