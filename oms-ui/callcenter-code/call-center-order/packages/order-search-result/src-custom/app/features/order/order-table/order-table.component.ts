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
  Component, OnInit, Input, Output, EventEmitter, ViewChild, TemplateRef, OnDestroy, ViewChildren, QueryList, Renderer2
} from '@angular/core';
import {
  BucContentTemplatesComponent, BucIconTemplatesComponent, BucTableConfiguration,
  BucTableHeaderItem, BucTableHelperService, BucTableModel, BucTableTemplateMapping, BucTableToolbarActionsModel,
  BucTableToolbarModel, COMMON, BucTemplateDirective, TableComponent, removeHeaderStyling,
  getArray,
  BucSessionService,
  setIfNotEmpty,
  BaseTableComponent
} from '@buc/common-components';
import { ModalService, TableHeaderItem } from 'carbon-components-angular';
import { Observable, Subject, Subscription, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged, filter, map, catchError } from 'rxjs/operators';
import {
  OrderSearchDataService,
  Constants,
  PaginationActions,
  PaginationModel
} from '@call-center/order-shared';
import { Router } from '@angular/router';
import { BucSvcAngularStaticAppInfoFacadeUtil, BucCommOmsMashupService, BucBaseUtil, CallCenterNavigationService } from '@buc/svc-angular';
import { ExtensionConstants } from '../../extension.constants';
import { get } from 'lodash';
import { Store } from '@ngrx/store';

export interface SearchResultTableData {
  data: Array<any>;
  type: 'orders';
  count: number;
  table: OrderTableComponent;
}
@Component({
  selector: 'call-center-order-table',
  templateUrl: 'order-table.component.html',
  styleUrls: ['order-table.component.scss']
})
export class OrderTableComponent extends BaseTableComponent implements OnInit, OnDestroy {
  EXTENSION = {
    TOP: ExtensionConstants.ORDER_TABLE_OR_TOP,
    BOTTOM: ExtensionConstants.ORDER_TABLE_BOTTOM
  };

  @Input() tableDatadetails: any;
  @Input() toggleFilter: () => {};
  @Input() parentPage: any; /* Type any to be replaced by the parent component */
  @Input() searchCriteria;
  @Input() customSearchCriteria;
  @Input() selectedEnterprise;
  @Input() searchText: string;
  @Input() tabId: string;

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
    TH_ORDER_NO: 'orderNo',
    TH_CUSTOMER_NAME: 'customerName',
    TH_EMAIL_ADDRESS: 'customerEmail',
    TH_POSTAL_CODE: 'customerZipCode',
    TH_ADDRESS: 'address',
    TH_STATUS: 'status',
    TH_HOLD_STATUS: 'holdFlag',
    TH_ENTERPRISE: 'enterprise',
    TH_ORDER_DATE: 'orderDate'
  };
  /* Table Actions */
  public readonly TABLE_ACTIONS: any = {
    ACTION_DUMMY: 'DummyActionId'
  };
  public readonly DEFAULT_SORT_COLUMN = this.TABLE_HEADERS.TH_ORDER_DATE;

  protected readonly nlsMap: any = {
    'ORDER_SEARCH.GENERAL.LABEL_ORDER_NUMBER': ''
  };

  public readonly componentId = Constants.ORDER_RESULTS_TABLE_COMPONENT_ID;
  public initialLoad = true;
  maximumRecords: number = Constants.DEFAULT_SEARCH_MAX_RECORDS;
  searchValue = '';
  selectedItems: string[] = [];

  /* Table settings */
  multiModel = new BucTableModel();
  tableToolbarModel = new BucTableToolbarModel();
  cancelText: any = { CANCEL: '' };
  paginationTranslations: any;
  totalRecordsMessage = '';

  actionResourceIds: any = {};
  resourceIds: any = {};
  private _defaultActions: Array<BucTableToolbarActionsModel>;
  protected bucSessionStorageService: BucSessionService;
  protected bucSessionStorageServiceGlobal: BucSessionService;

  @ViewChild('refresh', { static: true })
  protected refresh: TemplateRef<any>;

  @ViewChild('bucSettingsIcon', { static: true })
  bucSettingsIconTemplate: TemplateRef<any>;

  @ViewChild('actionMenuTemplate', { static: false })
  public actionMenuTemplate: TemplateRef<any>;

  // @ViewChild('bucFilterIcon', { static: true })
  // bucFilterIconTemplate: TemplateRef<any>;

  @ViewChild('iconTemplates', { static: true })
  iconTemplates: BucIconTemplatesComponent;

  @ViewChild('contentTemplates', { static: true })
  contentTemplates: BucContentTemplatesComponent;

  @ViewChildren(BucTemplateDirective) templateRefs: QueryList<BucTemplateDirective>;
  groupResults: TableComponent;
  sessionPrefix: any;
  sessionId: string;
  paginationModel: any = {
    pageSize: BucTableModel.DEFAULT_PAGE_LEN,
    pageData: {}
  };
  isSearchActive: boolean = false;

  @ViewChild('groupResults', { static: false }) set content(content: TableComponent) {
    if (content) {
      this.groupResults = content;
      removeHeaderStyling(this.groupResults.elementRef.nativeElement, this.renderer2);
    }
  }

  @ViewChild('orderNoLinkTemplate', { static: true })
  orderNoLinkTemplateRef: TemplateRef<any>;
  @ViewChild('holdTemplate', { static: true })
  holdTemplateRef: TemplateRef<any>;

  private searchSubject$ = new Subject<string>();
  private subscriptions: Subscription[] = [];
  private toolbarSearchMade: boolean;
  private readonly SEARCH_DEBOUNCE_TIME_MS = 300;
  readonly resourceIdsForActions = {
    CREATE_ORDER: 'ICC000001'
  };

  constructor(
    thService: BucTableHelperService,
    modalService: ModalService,
    public translateService: TranslateService,
    protected renderer2: Renderer2,
    public orderSearchSvc: OrderSearchDataService,
    public router: Router,
    public ccNavigationSvc: CallCenterNavigationService,
    private bucCommOmsMashupService: BucCommOmsMashupService,
    private store$: Store
  ) {
    super(thService, modalService);
  }

  async ngOnInit() {
    this._init();
  }

  private async _init() {
    this.sessionId = BucSvcAngularStaticAppInfoFacadeUtil.getCurrentCCSessionId();
    this.initialLoad = true;
    this.initializeSession();
    await this.prepareResourceIds();
    await this._initTranslations();
    await this.initializeTableConfig();
    this.initialLoad = false;
    const cachedSelectedEnterprise = this.bucSessionStorageService.getItem('selectedEnterprise');
    this.selectedEnterprise = this.selectedEnterprise ? this.selectedEnterprise :
      cachedSelectedEnterprise.selectedEnterprise;
    this.initializeSubscriptions();
  }

  initializeSession() {
    this.bucSessionStorageServiceGlobal = new BucSessionService(Constants.GLOBAL_SESSION_KEY, `${this.sessionId}-${this.tabId}`);
    if (this.bucSessionStorageServiceGlobal.getItem('sessionPrefix')) {
      this.sessionPrefix = this.bucSessionStorageServiceGlobal.getItem('sessionPrefix');
    }
    this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, `${this.sessionId}-${this.tabId}`);
  }
  async prepareResourceIds() {
    const allActionIds = {};
    const objectPath = this.determinePathForResourceId();
    const errorMessage = ''; /* TODO: Relevant error message to be added */
    /* TODO: Uncomment the below function call, add the CommonService import and scope constants as 1st param */
    // allActionIds = await CommonService.getResourceIds('', objectPath, errorMessage);
    this.resourceIds = allActionIds;
  }

  determinePathForResourceId() {
    const suffix = 'Dummy Actions Resource Id'; /* TODO: Actions mapping path to be added wrt to getResourceIds-mappings.json */
    const rc = ''; /* TODO: Replace with correct constants based on Inbound or Return */
    const path = rc === '' ? suffix : rc + '.' + suffix;
    return path;
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
      templateData: { onClick: () => { this.refreshTable(); } }
    };
    templateMapping.toolbarContent[this.TC_OPEN_TABLE_FIELD_CONFIGURATION_MODAL] = {
      contentTemplate: this.bucSettingsIconTemplate
    };

    /* initialize table from configuration. */
    await this.initializeTable('order-table', templateMapping, this.templateRefs.toArray(), []);
    const tableConfiguration: BucTableConfiguration = this.getTableConfiguration();

    this.multiModel = tableConfiguration.getBucTableModel();
    this.multiModel.currentPage = 1;
    this.multiModel.isLoading = true;
    this.multiModel.pageLength = this.paginationModel.pageSize;

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

  openOrderDetails(orderData,routeQueryParams?) {
    const isCreateOrderAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForActions.CREATE_ORDER);
    if (orderData.DraftOrderFlag === 'Y' && isCreateOrderAllowed) {
      const title = this.translateService.instant('SHARED.GENERAL.LABEL_CREATE_ORDER', { orderNo: orderData.OrderNo });
      this.ccNavigationSvc.openUrlInNewTab(`${Constants.CREATE_ORDER_ROUTE}`,
        { orderNo: orderData.OrderNo, orderHeaderKey: orderData.OrderHeaderKey, title, sellerEnterpriseCode: orderData.EnterpriseCode });
    } else {
      this.ccNavigationSvc.openUrlInNewTab(`${Constants.ORDER_DETAILS_ROUTE}`,
        { orderNo: orderData.OrderNo, orderHeaderKey: orderData.OrderHeaderKey, enterprise: orderData.EnterpriseCode, title: orderData.OrderNo, ...(routeQueryParams !== undefined && routeQueryParams.prev && { prev : routeQueryParams.prev })});
        }
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
    this.multiModel.currentSortKey = sortColumn?.['sortKey'];
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
              this.totalRecordsMessage = this.translateService.instant('ORDER_SEARCH_RESULT.GENERAL.TOO_MANY_RESULTS',
                { total: pageModel.TotalNumberOfRecords, max: Constants.DEFAULT_SEARCH_MAX_RECORDS });
            } else {
              this.totalRecordsMessage = '';
            }

            this.emitTableDataChange(currentPage.pageData, 'orders', currentPage.pageData.length);

            if (!this.multiModel.totalDataLength) {
              return [];
            }

            return currentPage.pageData;
          } else if (currentPage && currentPage.error) {
            this.sendNotification('ORDER_SEARCH_RESULT.GENERAL.TOO_MANY_RESULTS');
            return [];
          }
        })
      );

  }

  protected getDataForColumn(id: string, item: any) {
    let colData: any = { data: '' };
    switch (id) {
      case this.TABLE_HEADERS.TH_ORDER_NO:
        colData = {
          data: item,
          id: item.OrderNo,
          enterprise: item.EnterpriseCode,
          template: this.orderNoLinkTemplateRef
        };
        break;
      case this.TABLE_HEADERS.TH_CUSTOMER_NAME:
        colData = { data: item.CustomerFirstName + ' ' + item.CustomerLastName };
        break;
      case this.TABLE_HEADERS.TH_HOLD_STATUS:
        colData = {
          data: {
            text: item.HoldFlag === 'N' ? this.translateService.instant('ORDER_SEARCH.GENERAL.LABEL_NO_HOLD') :
              this.translateService.instant('ORDER_SEARCH.GENERAL.LABEL_HOLD'),
            ...item
          },
          id: item.HoldFlag,
          template: this.holdTemplateRef
        };
        break;
      case this.TABLE_HEADERS.TH_ADDRESS:
        colData = { data: item && item.PersonInfoBillTo && item.PersonInfoBillTo.AddressLine1 ? item.PersonInfoBillTo.AddressLine1 : '' };
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
      this.setSort(this.getColumnById(this.DEFAULT_SORT_COLUMN));
    }
  }

  protected onToolbarActionClicked(id: string, event?: any) { }
  protected onToolbarContentClicked(id: string, event?: any) { }
  protected onOverflowMenuActionSelected(id: string) { }

  protected onTableLoadComplete(data?): void {
    this.multiModel.isLoading = data === undefined;
  }

  initializeSubscriptions() {
    this.subscriptions.push(
      this.searchSubject$.pipe(
        debounceTime(this.SEARCH_DEBOUNCE_TIME_MS),
        distinctUntilChanged()
      ).subscribe((searchValue) => {
        this.toolbarSearchMade = true;
        this.isSearchActive = true;
        this.searchText = searchValue.trim();
        this.searchTextChange.emit(this.searchText);
        this.search();
        this.toolbarSearchMade = false;
      }),
      super.loadTable().subscribe(this.onTableLoadComplete.bind(this))
    )
  }

  searchTextChanged(searchValue) {
    this.searchSubject$.next(searchValue);
  }

  searchTextCleared() {
    this.searchSubject$.next('');
  }

  ngOnDestroy() {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }
}
