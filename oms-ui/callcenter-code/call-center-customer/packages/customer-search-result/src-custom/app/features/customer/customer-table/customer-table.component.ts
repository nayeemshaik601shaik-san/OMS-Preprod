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
  Component, OnInit, Input, Output, EventEmitter, ViewChild,
  TemplateRef, OnDestroy, ViewChildren, QueryList, Renderer2, OnChanges, SimpleChanges
} from '@angular/core';
import {
  BucContentTemplatesComponent, BucIconTemplatesComponent, BucTableConfiguration,
  BucTableHeaderItem, BucTableHelperService, BucTableModel, BucTableTemplateMapping, BucTableToolbarActionsModel,
  BucTableToolbarModel, COMMON, TableOverflowMenuAction, BucTemplateDirective, TableComponent, removeHeaderStyling,
  BucSessionService, getArray
} from '@buc/common-components';
import { Observable, Subject, Subscription, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { ModalService, TableHeaderItem } from 'carbon-components-angular';
import { Constants, PaginationActions, getFullName, PaginationModel } from '@call-center/customer-shared';
import { map, debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';
import { BaseTableComponent } from '@buc/common-components';
import { BucSvcAngularStaticAppInfoFacadeUtil, BucBaseUtil, CallCenterNavigationService } from '@buc/svc-angular';
import { get } from 'lodash';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';

export interface SearchResultTableData {
  data: Array<any>;
  type: 'orders';
  count: number;
  table: CustomerTableComponent;
}
@Component({
  selector: 'call-center-customer-table',
  templateUrl: 'customer-table.component.html',
  styleUrls: ['customer-table.component.scss']
})
export class CustomerTableComponent extends BaseTableComponent implements OnInit, OnDestroy, OnChanges {

  @Input() toggleFilter: () => {};
  @Input() parentPage: any; /* Type any to be replaced by the parent component */
  @Input() searchCriteria;
  @Input() selectedEnterprise;
  @Input() tabId: string;
  @Input() searchText: string;
  @Input() customerType: string;

  @Output() resultChange: EventEmitter<SearchResultTableData> = new EventEmitter<SearchResultTableData>();
  @Output() searchTextChange: EventEmitter<string> = new EventEmitter<string>();

  /* Table Generic Constants */
  public readonly TABLE_CONSTANTS: any = {
    DEFAULT_PAGE_LENGTH: 10,
    CONTENT_FILTER: 'toggleFilter',
    CONTENT_REFRESH: 'refresh',
  };
  /* Table Headers */
  public readonly TABLE_HEADERS: any = {
    TH_CUSTOMER_ID: 'customerId',
    TH_ORGANIZATION: 'organization',
    TH_ADDRESS: 'address',
    TH_CUSTOMER_NAME: 'customerName',
    TH_EMAIL_ADDRESS: 'emailAddress',
    TH_PHONE_NUMBER: 'phone',
    TH_CUSTOMER_TYPE: 'customerType',
    TH_ORG_NAME: 'orgName',
    TH_ENTERPRISE: 'enterprise',
    TH_CUSTOMER_STATUS: 'customerStatus',
    TH_MEMBER_ID: 'memberID',
    TH_POSTAL_CODE: 'postalCode'

  };
  /* Table Actions */
  public readonly TABLE_ACTIONS: any = {
    ACTION_SAMPLE: 'SampleActionId'
  };

  public readonly defaultSortColumnId: string = this.TABLE_HEADERS.TH_CUSTOMER_ID;

  protected readonly nlsMap: any = {
    'CUSTOMER_SEARCH.GENERAL.LABEL_ACTIVE': '',
    'CUSTOMER_SEARCH.GENERAL.LABEL_HOLD': '',
    'CUSTOMER_SEARCH.GENERAL.LABEL_INACTIVE': '',
    'CUSTOMER_SEARCH.GENERAL.LABEL_BUSINESS': '',
    'CUSTOMER_SEARCH.GENERAL.LABEL_CONSUMER': ''
  };

  public componentId = Constants.CUSTOMER_TABLE_COMPONENT_ID;
  public initialLoad = true;
  public overflowMenu: TableOverflowMenuAction[];
  refreshing: boolean = false;
  searchValue: string = '';
  selectedItems: string[] = [];
  paginationModel: any = {
    pageSize: BucTableModel.DEFAULT_PAGE_LEN,
    pageData: {}
  };

  /* Table settings */
  multiModel = new BucTableModel();
  tableToolbarModel = new BucTableToolbarModel();
  cancelText: any = { CANCEL: '' };
  paginationTranslations: any;
  totalRecordsMessage = '';
  searchPlaceholderMsg = '';

  private _defaultActions: Array<BucTableToolbarActionsModel>;

  protected bucSessionStorageService: BucSessionService;
  protected bucSessionStorageServiceGlobal: BucSessionService;

  // customerType = Constants.CUSTOMER_TYPE.all;
  CONSUMER_TABLE_ID = 'consumer-table';
  BUSINESS_TABLE_ID = 'business-table';

  @ViewChild('refresh', { static: true })
  protected refresh: TemplateRef<any>;

  @ViewChild('bucSettingsIcon', { static: true })
  bucSettingsIconTemplate: TemplateRef<any>;

  @ViewChild('actionMenuTemplate', { static: false })
  public actionMenuTemplate: TemplateRef<any>;

  @ViewChild('bucFilterIcon', { static: true })
  bucFilterIconTemplate: TemplateRef<any>;

  @ViewChild('iconTemplates', { static: true })
  iconTemplates: BucIconTemplatesComponent;

  @ViewChild('contentTemplates', { static: true })
  contentTemplates: BucContentTemplatesComponent;

  @ViewChildren(BucTemplateDirective) templateRefs: QueryList<BucTemplateDirective>;
  groupResults: TableComponent;
  sessionPrefix = 'call-center-customer-search';
  tenantId: string;
  searchTabId: string;
  sessionId: string;
  pageAction: string;
  pageModel = {};
  previousLastRecord = {}
  isSearchActive: boolean = false;

  @ViewChild('groupResults', { static: false }) set content(content: TableComponent) {
    if (content) {
      this.groupResults = content;
      removeHeaderStyling(this.groupResults.elementRef.nativeElement, this.renderer2);
    }
  }
  @ViewChild('customerIdLinkTemplate', { static: true })
  customerIdLinkTemplateRef: TemplateRef<any>;

  @ViewChild('customerIdTemplate', { static: true })
  customerIdTemplateRef: TemplateRef<any>;

  private searchSubject$ = new Subject<string>();
  private searchSubscription: Subscription;
  private toolbarSearchMade: boolean;
  private reinit = false;
  private readonly SEARCH_DEBOUNCE_TIME_MS = 300;

  constructor(
    thService: BucTableHelperService,
    modalService: ModalService,
    public translateService: TranslateService,
    protected renderer2: Renderer2,
    public route: ActivatedRoute,
    public ccNavigationSvc: CallCenterNavigationService,
    private store$: Store
  ) {
    super(thService, modalService);
  }

  async ngOnInit() {
    this.reinit = true
    this.initialize();
  }

  async ngOnChanges(changes: SimpleChanges){
    if ('customerType' in changes && changes.customerType.currentValue != changes.customerType.previousValue) {
      await this.initialize();
    }
  }

  async initialize() {
    this.tenantId = BucSvcAngularStaticAppInfoFacadeUtil.getSelectedTenantId();
    this.sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    this.initialLoad = true;
    this.initializeSession();
    await this._initTranslations();
    await this.initializeTableConfig();
    this.initialLoad = false;
    this.handleToolbarSearchChange();

    super.loadTable().subscribe((data) => { 
      this.multiModel.isLoading = data === undefined; 
    });

  }

  initializeSession() {
    this.sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    this.tabId = this.route.snapshot.queryParams.uniqueId;
    this.searchTabId = this.route.snapshot.queryParams.searchTabId;

    if (this.searchTabId) {
      this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.searchTabId}`);
    } else {
      this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.tabId}`);
    }
  }

  protected async _initTranslations() {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translateService.get(keys).toPromise();
    keys.forEach(k => this.nlsMap[k] = json[k]);
    this.cancelText = await COMMON.tableCancelText(this.translateService);
    this.paginationTranslations = await COMMON.paginationTranslations(this.translateService);
  }

  async search() {
    this.resetTable();
    this.loadTable();
    this.initialLoad = false;
  }

  private resetTable() {
    this.multiModel.currentPage = 1;
    this.paginationModel.pageData = {};
  }

  async initializeTableConfig() {
    const templateMapping: BucTableTemplateMapping = { toolbarContent: {} };
    templateMapping.toolbarContent[this.TABLE_CONSTANTS.CONTENT_REFRESH] = {
      contentTemplate: this.refresh,
      templateData: {
        onClick: () => {
          this.refreshing = true;
          this.search();
        }
      }
    };
    templateMapping.toolbarContent[this.TC_OPEN_TABLE_FIELD_CONFIGURATION_MODAL] = {
      contentTemplate: this.bucSettingsIconTemplate
    };
    templateMapping.toolbarContent[this.TABLE_CONSTANTS.CONTENT_FILTER] = {
      contentTemplate: this.bucFilterIconTemplate,
      templateData: this.toggleFilter
    };

    /* initialize table from configuration. */
    const tableID = this.customerType === 'business' ? this.BUSINESS_TABLE_ID : this.CONSUMER_TABLE_ID;

    if(this.customerType === 'consumer'){
      this.searchPlaceholderMsg = this.translateService.instant('CUSTOMER_SEARCH.GENERAL.LABEL_TOOLBAR_PLACEHOLDER_CONSUMER_CUSTOMER');
    }else if(this.customerType === 'business'){
      this.searchPlaceholderMsg = this.translateService.instant('CUSTOMER_SEARCH.GENERAL.LABEL_TOOLBAR_PLACEHOLDER_BUSINESS_CUSTOMER');
    }

    await this.initializeTable(tableID, templateMapping, this.templateRefs.toArray(), [], this.reinit);
    const tableConfiguration: BucTableConfiguration = this.getTableConfiguration();

    this.multiModel = tableConfiguration.getBucTableModel();
    this.multiModel.isLoading = true;
    this.multiModel.pageLength = this.paginationModel.pageSize;
    this.multiModel.currentPage = 1;
    this.tableToolbarModel = tableConfiguration.getToolbarModel();
    this._defaultActions = this.tableToolbarModel.actions;
    this._defaultActions.forEach(action => {
      if (action.iconTemplate) {
        action.iconTemplate = this.iconTemplates.getTemplate(action.iconTemplate.toString());
      }
      if (action.contentTemplate) {
        action.contentTemplate = this.contentTemplates.getTemplate(action.contentTemplate.toString());
      }
    });
    // apply user preference
    await this.applyUserPreference().toPromise();

    this.initialLoad = false;
    this.multiModel.isLoading = false;
  }

  public loadTable(): Observable<any> {
    if (this.multiModel && !this.toolbarSearchMade) {
      this.multiModel.isLoading = true;
    }
    this.store$.dispatch(
      PaginationActions.updateSortCriteria({
        id: this.componentId,
        sortCriteria: {
          sortKey: this.multiModel.currentSortKey,
          sortOrder: this.multiModel.currentSortOrder
        },
        pageSize: this.multiModel.pageLength
      })
    );
    return of({});
  }

  public refreshTable() {
    this.multiModel.currentPage = 1;
    this.multiModel.isLoading = true;
    this.store$.dispatch(PaginationActions.refresh({ id: this.componentId }));
  }

  async selectPage(pageNumber) {
    this.multiModel.currentPage = pageNumber;
    this.store$.dispatch(PaginationActions.gotoPage({
      id: this.componentId,
      pageNumber,
      pageSize: this.multiModel.pageLength
    }));
  }

  async openCustomerDetails(customerData,routeQueryParams) {
    const fullName = await getFullName(customerData, this.translateService);
    this.ccNavigationSvc.openUrlInNewTab(
      `${Constants.CUSTOMER_DETAILS_ROUTE}`,
      {
        enterpriseCode: customerData.OrganizationCode,
        title: customerData?.CustomerType == Constants.CUSTOMER_TYPE.business ? customerData.BuyerOrganization?.OrganizationName : (fullName ? fullName : customerData.CustomerID),
        customerId: customerData.CustomerID,
        customerKey: customerData.CustomerKey,
        selectedEnterprise: this.selectedEnterprise.selectedEnterprise,
        ...(routeQueryParams !== undefined && routeQueryParams.prev && { prev : routeQueryParams.prev })
      }
    );
  }

  onColSort(index: number) {
    const header: BucTableHeaderItem = this.multiModel.getHeader(index) as BucTableHeaderItem;
    this.initialLoad = false;
    this.setSort(header);
    this.store$.dispatch(PaginationActions.updateSortCriteria({
      id: this.componentId,
      sortCriteria: {
        sortKey: this.multiModel.currentSortKey,
        sortOrder: this.multiModel.currentSortOrder
      }
    }));
  }

  protected setSort(sortColumn: BucTableHeaderItem) {
    this.multiModel.currentSortKey = sortColumn['sortKey'];
    this.multiModel.currentSortOrder = sortColumn.descending ? 'Desc' : 'Asc';
    this.resetTable();
  }

  public emitTableDataChange(data: Array<any>, type, count: number) {
    const output: SearchResultTableData = { data, type, count, table: this };
    this.resultChange.emit(output);
  }
  protected fetchTableData(): Observable<Array<any>> {
    const currentPage = get(this.paginationModel, ['pageData', this.multiModel.currentPage]);
    if (BucBaseUtil.isVoid(currentPage)) {
      return this.fetchTableDataFromMashup();
    } else {
      return of(getArray(currentPage.data));
    }
  }
  protected fetchTableDataFromMashup(): Observable<Array<any>> {
    const enterprisesForSearch = this.bucSessionStorageService.getItem('selectedEnterprise') ?? this.selectedEnterprise;

    // selectedEnterprise is required to make a search
    if (!enterprisesForSearch?.selectedEnterprise) {
      return of([]);
    }

    return this.store$
      .select(PaginationActions.selectCurrentPage(this.componentId))
      .pipe(
        filter((currentPage) => this.isSearchActive ? !BucBaseUtil.isVoid(currentPage) : true),
        map((currentPage) => {
          this.isSearchActive = false;
          if (currentPage && currentPage.pageData) {
            const pageModel: PaginationModel = currentPage.pageModel;
            this.multiModel.totalDataLength = pageModel.TotalNumberOfRecords;
            if (pageModel.TotalNumberOfRecords > Constants.DEFAULT_SEARCH_MAX_RECORDS) {
              this.totalRecordsMessage = this.translateService.instant('CUSTOMER_SEARCH_RESULT.GENERAL.TOO_MANY_RESULTS',
                { total: pageModel.TotalNumberOfRecords, max: Constants.DEFAULT_SEARCH_MAX_RECORDS });
            } else {
              this.totalRecordsMessage = '';
            }

            this.emitTableDataChange(currentPage.pageData, 'customers', currentPage.pageData.length);

            if (!this.multiModel.totalDataLength) {
              return [];
            }

            return currentPage.pageData;
          } else if (currentPage && currentPage.error) {
            this.sendNotification('CUSTOMER_SEARCH.GENERAL.MESSAGE_Fail_to_get_customers');
            return [];
          }
        })
      );

  }

  protected getDataForColumn(id: string, item: any) {
    let colData: any = { data: '' };
    const customer = 'CustomerContact' in item.CustomerContactList ? item.CustomerContactList?.CustomerContact[0] : {}
    switch (id) {
      case this.TABLE_HEADERS.TH_CUSTOMER_ID:
        colData = {
          data: item, id: item.CustomerID,
          template: this.customerIdLinkTemplateRef
        };
        break;
      case this.TABLE_HEADERS.TH_CUSTOMER_NAME:
        const fname = customer.FirstName ? customer.FirstName : '';
        const lname = customer.LastName ? customer.LastName : '';
        colData = { data: fname + ' ' + lname };
        break;
      case this.TABLE_HEADERS.TH_EMAIL_ADDRESS:
        colData = { data: customer.EmailID ? customer.EmailID : ''};
        break;
      case this.TABLE_HEADERS.TH_PHONE_NUMBER:
        colData = { data: customer.DayPhone ? customer.DayPhone : '' };
        break;
      case this.TABLE_HEADERS.TH_ENTERPRISE:
        colData = { data: item.OrganizationCode };
        break;
      case this.TABLE_HEADERS.TH_CUSTOMER_STATUS:
        const statusCode = item.Status;
        let status = '';
        switch (statusCode) {
          case '10':
            status = this.nlsMap['CUSTOMER_SEARCH.GENERAL.LABEL_ACTIVE'];
            break;
          case '20':
            status = this.nlsMap['CUSTOMER_SEARCH.GENERAL.LABEL_HOLD'];
            break;
          case '30':
            status = this.nlsMap['CUSTOMER_SEARCH.GENERAL.LABEL_INACTIVE'];
            break;
        }
        colData = { data: status };
        break;
      case this.TABLE_HEADERS.TH_MEMBER_ID:
        colData = { data: customer.User ? customer.User.DisplayUserID : '' };
        break;
      case this.TABLE_HEADERS.TH_ORGANIZATION:
        const orgName = item.BuyerOrganization?.OrganizationName ? item.BuyerOrganization?.OrganizationName : '';
        colData = { data: orgName };
        break;
      case this.TABLE_HEADERS.TH_ADDRESS:
        const city = customer?.DefaultBillToAddress?.PersonInfo?.City ? customer.DefaultBillToAddress.PersonInfo.City + ', ' : '';
        const state = customer?.DefaultBillToAddress?.PersonInfo?.State ? customer.DefaultBillToAddress.PersonInfo.State + ', ' : '';
        const zipcode = customer?.DefaultBillToAddress?.PersonInfo?.ZipCode ? customer.DefaultBillToAddress.PersonInfo.ZipCode: '';
        colData = { data: city + state + zipcode}
        break;
      case this.TABLE_HEADERS.TH_POSTAL_CODE:
        colData = { data: customer?.DefaultBillToAddress?.PersonInfo?.ZipCode ? customer?.DefaultBillToAddress?.PersonInfo?.ZipCode : '' };
        break;
    }

    return colData;
  }

  onOverflowMenuClick(data: any) { }

  protected updateSortCriteria(sortedCols: BucTableHeaderItem[]) {
    if (sortedCols.length > 0) {
      // multi header sorting is not supported. pick the first one
      this.setSort(sortedCols[0]);
    } else {
      // pick a default fallback column to sort by.
      this.setSort(this.getColumnById(this.defaultSortColumnId));
    }
  }

  protected onToolbarActionClicked(id: string, event?: any) { }
  protected onToolbarContentClicked(id: string, event?: any) { }
  protected onOverflowMenuActionSelected(id: string) { }

  protected onTableLoadComplete(): void {
    this.initialLoad = false;
    this.multiModel.isLoading = false;
  }

  handleToolbarSearchChange() {
    this.searchSubscription = this.searchSubject$.pipe(
      debounceTime(this.SEARCH_DEBOUNCE_TIME_MS),
      distinctUntilChanged()
    ).subscribe((searchValue) => {
      this.toolbarSearchMade = true;
      this.isSearchActive = true;
      this.searchText = searchValue;
      this.searchTextChange.emit(this.searchText);
      this.search();
      this.toolbarSearchMade = false;
    });
  }

  searchTextChanged(searchValue) {
    this.searchSubject$.next(searchValue);
  }

  searchTextCleared() {
    this.searchSubject$.next('');
  }

  ngOnDestroy() {
    this.searchSubscription?.unsubscribe();
  }

}
