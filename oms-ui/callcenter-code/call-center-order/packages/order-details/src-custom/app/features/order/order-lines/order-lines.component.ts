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
  Component, EventEmitter, Injector, Input, OnDestroy, OnInit,
  Output, QueryList, TemplateRef, ViewChild, ViewChildren
} from '@angular/core';
import {
  BucIconTemplatesComponent, BucTableConfiguration,
  BucTableHeaderItem, BucTableHelperService, BucTableModel, BucTableTemplateMapping,
  BucTableToolbarModel, BucTemplateDirective, TableComponent,
  COMMON as BUCCOMMON,
  TemplateIdDirective, removeHeaderStyling,
  BucCommonCurrencyFormatPipe,
  fmtDate,
  TableOverflowMenuAction,
  BucTableToolbarActionsModel,
  ActionProcessorService,
  BucSessionService,
  BaseTableComponent,
  BucContentTemplatesComponent,
  CommonService,
  getArray,
  setIfNotEmpty,
  BucDateTimeHelper,
  getCurrentLocale,
  DisplayRulesHelperService,
  BucCommonQuantityFormatPipe,
  localeBuc2Angular} from '@buc/common-components';
import { ModalService, TableHeaderItem } from 'carbon-components-angular';
import { Observable, of, ReplaySubject, Subscription } from 'rxjs';
import {
  ActionParams, ApplyHoldsActionParams, Constants, IsModificationAllowed, OrderCommonService,
  paginationTranslations, regexEscape, tableCancelText, CancelOrderDataService, ViewAllNotesModalComponent, CommonCodes, formatNumber
} from '@call-center/order-shared';
import { catchError, debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { OrderSummaryComponent } from '../order-summary/order-summary.component';
import { Router } from '@angular/router';
import { BucBaseUtil, BucCommOmsMashupService, BucSvcAngularStaticAppInfoFacadeUtil, CallCenterNavigationService } from '@buc/svc-angular';
import { OrderSummaryService } from '../data-service/order-summary.service';
import { ExtensionConstants } from '../../extension.constants';
import { cloneDeep, get } from 'lodash';
import { getTypeaheadConfig } from '@buc/cc-components';

@Component({
  selector: 'call-center-order-lines',
  templateUrl: './order-lines.component.html',
  styleUrls: ['./order-lines.component.scss'],
})

export class OrderLinesComponent extends BaseTableComponent implements OnInit,OnDestroy {
  EXTENSION = {
    TOP: ExtensionConstants.ORDER_LINES_OD_TOP,
    BOTTOM: ExtensionConstants.ORDER_LINES_OD_BOTTOM
  };

  private templates: { [id: string]: TemplateRef<any> } = {};
  @Input() transactionQtyRuleDetailsValue: any;
  searchSub: Subscription;
  pageSelector: any;
  isAdjustPricingResourceAllowed: boolean;
  isAddNoteOnOrderLineResourceAllowed: boolean;
  reasonCodeList: any;
  isLoading: boolean;
  displayDoubleQty: boolean;
  lineDetails: any;
  showAdditionalLineDetails: boolean = false;
  setInputFocus: boolean;
 
  @ViewChildren(TemplateIdDirective) set _templates(a: QueryList<TemplateIdDirective>) {
    if (a) {
      a.forEach(({ id, template }) => this.templates[id] = template);
    }
  }
  @ViewChildren(BucTemplateDirective) templateRefs: QueryList<BucTemplateDirective>;
  @ViewChild('iconTemplates', { static: true }) iconTemplates: BucIconTemplatesComponent;
  @ViewChild('headerActionOverflowTriggerTemplate', { static: true }) headerActionOverflowTriggerTemplate: TemplateRef<any>;
  @ViewChild('contentTemplates', { static: true }) contentTemplates: BucContentTemplatesComponent;
  @ViewChild('groupResults', { static: false }) set content(content: TableComponent) {
    if (content) {
      this.groupResults = content;
      removeHeaderStyling(this.groupResults.elementRef.nativeElement, this.renderer2);
    }
  }
  @Input() parentContainer: OrderSummaryComponent;
  @Input() orderDetails;
  @Input() orderLineList;
  @Input() ruleSetValues;
  @Input() bopisRuleValue;
  @Input() resourceIdsForOrderDetailsActions;
  @Input() orderRuleSetsValue;
  @Input() parentPage;
  @Input() isAddLinesAllowed;

  @Input() showExpand = true;
  @Input() expandState;

  @Output() orderLinesLoaded = new EventEmitter<any[]>();
  @Output() navigateToChangeFulfillment = new EventEmitter<any>();
  @Output() navigateToAddLinesToOrder = new EventEmitter<any>();
  groupResults: TableComponent;

  actionResourceIds: any = {};
  
  private readonly SEARCH_DEBOUNCE_TIME_MS = getTypeaheadConfig();
  // Table headers
  public readonly TH_LINE = 'line';
  public readonly TH_ITEM_NAME = 'itemName';
  public readonly TH_TOTAL_AMOUNT = 'totalAmount';
  public readonly TH_LINE_QUANTITY = 'lineQuantity';
  public readonly TH_FULFILLMENT_METHOD = 'fulfilmentMethod';
  public readonly TH_EXPECTED_DATE = 'expectedDate';
  public readonly TH_TRACKING = 'tracking';
  public readonly TH_STATUS = 'status';
  public readonly TH_EVENTS = 'events';


  public readonly TH_DERIVED_ORDER = 'derivedFromOrder';
  public readonly TH_REASON_CODE = 'reasonCode';
  public readonly TH_LINE_TYPE = 'lineType';

  // Table actions.
  public readonly ACTION_MANAGE_HOLDS = 'manageHolds';
  public readonly ACTION_APPLY_HOLDS = 'applyHolds';
  public readonly ACTION_ADJUST_PRICING = 'adjustPricing';
  public readonly ACTION_APPEASE_CUSTOMER = 'appeaseCustomer';
  public readonly ACTION_GIFT_OPTIONS = 'giftOptions';
  public readonly ACTION_RESHIP_ORDER_LINE = 'reshipOrderLine';
  public readonly ACTION_CHANGE_FULFILLMENT_METHOD = 'changeFulfillmentMethod';
  public readonly ACTION_ADD_QUANTITY = 'addQuantity';

  public readonly ACTION_MANAGE_INSTRUCTIONS = 'manageInstructions';
  public readonly ACTION_CANCEL_ORDER_LINE = 'cancelOrderLine';
  public readonly ACTION_ADD_ORDER_LINE = 'addLines';
  public readonly ACTION_CANCEL_MULTIPLE_ORDER_LINES = 'cancelMultipleOrderLines';
  public readonly ACTION_ADD_OR_VIEW_NOTES = 'addOrViewNotes';
  public readonly ACTION_VIEW_LINE_DETAILS = 'viewLineDetails';

  public readonly defaultPageLength = Constants.TABLE_PAGE_LENGTH_10;

  protected bucSessionStorageService: BucSessionService;
  protected bucSessionStorageServiceGlobal: BucSessionService;

  componentId = 'order-lines';
  actionSub: Subscription;

  tableContent = {
    refresh: 'refresh'
  };
  searchValue: any;
  selected: string[] = [];
  tenantId: string;
  sessionPrefix: any;
  notesTypeList = [];
  isCarryOrder: boolean;

  protected readonly nlsMap: any = {
    'ORDER_SUMMARY.ORDER_LINES.HEADER_LINE': '',
    'ORDER_SUMMARY.SHARED.SHP': '',
    'ORDER_SUMMARY.SHARED.PICK': '',
    'ORDER_SUMMARY.SHARED.DEL': '',
    'ORDER_SUMMARY.SHARED.CARRY': '',
    'ORDER_SUMMARY.SHARED.ADD_ORDER_LINE': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_CANCEL_ORDER_LINE_INELIGIBLE_TOOLTIP': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_CANCEL_MULTIPLE_ORDER_LINE_INELIGIBLE_TOOLTIP': '',
    'ORDER_SUMMARY.APPEASE_CUSTOMER_MODEL.LABEL_ACTION_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_RESHIP_MULTIPLE_ORDER_LINE_INELIGIBLE_TOOLTIP': '',
    'APPLY_HOLDS_MODAL.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.MANAGE_HOLDS.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.APPEASE_CUSTOMER_MODEL.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.PRICING_SUMMARY.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP': '',
    'FULFILLMENT.SHIPMENT_DETAILS.LINKS.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_CHANGE_FULFILLMENT_MULTIPLE_INELIGIBLE_TOOLTIP': '',
    'ORDER_SUMMARY.GIFT_OPTIONS_MODAL.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP':'',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_CANCEL_ORDER_LINE': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_CANCEL_MULTIPLE_ORDER_LINES': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_VIEW_NOTES': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_ADD_VIEW_NOTES': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_NOTES_INELIGIBLE_TOOLTIP': '',
    'ORDER_SUMMARY.ORDER_LINES.LABEL_VIEW_NOTES_INELIGIBLE_TOOLTIP': '',
    'ORDER_SUMMARY.PRICING_SUMMARY.LABEL_ADJUST_PRICING': '',
    'ORDER_SUMMARY.PRICING_SUMMARY.LABEL_VIEW_PRICING': '',
    'ORDER_SUMMARY.PRICING_SUMMARY.LABEL_LINE_ACTION_NO_ADUSTMENTS_TOOLTIP':'',
    'ORDER_SUMMARY.ORDER_LINES.TAG_CANCELLED': '',
    'ORDER_SUMMARY.ORDER_LINES.TAG_UPDATED': '',
    'ORDER_SUMMARY.ORDER_LINES.TAG_NEW_ITEM': '',
    'ORDER_SUMMARY.ORDER_LINES.UPDATE_QUANTITY_DECREASED_TO_ZERO': '',
    'ORDER_SUMMARY.ORDER_LINES.UPDATE_QUANTITY_DECREASED': '',
    'ORDER_SUMMARY.ORDER_LINES.UPDATE_QUANTITY_INCREASED': '',
    'ORDER_LINE_DETAILS.GENERAL.INVALID_QUANTITY_MSG': '',
    'ORDER_LINE_DETAILS.GENERAL.BAD_QUANITY_NUMBER_MSG': '',
    'ORDER_SUMMARY.ADDITIONAL_LINE_DETAILS.MODAL_HEADER': '',
    'ORDER_SUMMARY.ADDITIONAL_LINE_DETAILS.CLOSE': '',
    'PRODUCT_BROWSING.TAB_TITLE': '',
    'ORDER_LINES.GENERAL.LABEL_RELATED_PRODUCTS_MODAL_HEADER': ''
  };

  orderHeaderKey = 'ORDK_20Aug01DCBPS06';
  orderlinesListModel: BucTableModel = new BucTableModel();
  loadedOrderLines: any[];
  defaultSortColumnId = this.TH_LINE;

  private searchChg = new ReplaySubject<string>(1);
  private defaultTableActions: Array<BucTableToolbarActionsModel>;
  toolbarModel: BucTableToolbarModel;
  overflowMenu: TableOverflowMenuAction[];
  ofmActions = [];
  initialLoad: boolean;
  cancelText: any = { CANCEL: '' };
  paginationTranslations: any;
  private ofmData: any;
  showManageHoldsAction = false;
  filtersExp = []; // for handling filter queries
  _isFlyoutFilterOpen = false;
  _isSearchOpen = false;
  isSearch = false;
  searchQuery: any;
  complexExpression = []; // for handling filter queries
  cancelLinesOptions: any = {}; // for passing eligible/non-eligible lines
  bundleParentFulfillmentModeMapping = {};
  paginationModel: any = {
    pageSize: this.defaultPageLength,
    pageData: {}
  };
  maximumRecords: number = Constants.DEFAULT_SEARCH_MAX_RECORDS;
  private GET_COMPLETE_ORDER_LINES_LIST_MASHUP_ID = 'icc.order.summary.getCompleteOrderLineList';
  initialTotalDataLength = 0;
  curLocale: any;
  addLineResourcePermission: any;

  private modalSvc: ModalService;
  public qtyPipe: BucCommonQuantityFormatPipe = new BucCommonQuantityFormatPipe()

  lcl = localeBuc2Angular(BucSvcAngularStaticAppInfoFacadeUtil.getUserLanguage());
  public currPipe: BucCommonCurrencyFormatPipe = new BucCommonCurrencyFormatPipe(this.lcl)

  constructor(
    private mdlService: ModalService,
    private inj: Injector,
    public router: Router,
    private orderSummaryService: OrderSummaryService,
    public cancelOrderDataService: CancelOrderDataService,
    public translate: TranslateService,
    public actionProcessorService: ActionProcessorService,
    public ccNavigationSvc: CallCenterNavigationService,
    private orderCommonService: OrderCommonService,
    private bucCommOmsMashupService: BucCommOmsMashupService,
    private displayRulesHelperService: DisplayRulesHelperService,
  ) {
    super(inj.get(BucTableHelperService), mdlService);
    this.modalSvc = inj.get(ModalService);
  }

  ngOnInit(): void {
    this.initialize();
    this.displayDoubleQty = this.displayRulesHelperService.isShowDoubleQtyDisplayRule();
    this.showAdditionalLineDetails = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions.LINE_DETAILS);
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
    this.actionSub = this.actionProcessorService.selectUpdate<any>(this.componentId).subscribe(res => {
      if (res.params.refresh) {
        this.parentPage.initialize(null,false);
        this.initialize(true);
      }
    });
  }

  ngOnDestroy() {
    if (this.actionSub) {
      this.actionSub.unsubscribe();
    }
  }

  async initialize(skipTableLoad = false): Promise<any> {
    this.tenantId = BucSvcAngularStaticAppInfoFacadeUtil.getSelectedTenantId();
    this.initialLoad = true;
    this.searchValue = '';
    this.isCarryOrder = this.orderDetails?.MaxOrderStatus?.startsWith('1100.7777');
    this.isAdjustPricingResourceAllowed =  BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions.ADJUST_PRICING_ORDER_LINE);
    this.isAddNoteOnOrderLineResourceAllowed = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.resourceIdsForOrderDetailsActions.ADD_NOTE_ON_ORDER_LINE);
    this.curLocale = getCurrentLocale();
    if (this.curLocale.startsWith('zh-')) {
      this.curLocale = 'zh';
    }
    await this._initTranslations();
    this.getCodesForNotes();
    this.initializeSession();
    this.getActionResourceIds();
    if (!skipTableLoad) {
      await this.initializeTableConfig();
      this._subscribeToTextSearch();
      const tableModel = this.getTableConfiguration().getBucTableModel();
      this.initialLoad = false;
    }
  }

  async initializeTableConfig(): Promise<any> {
    await this._initRegularTable();
  }
  protected async _initTranslations(): Promise<any> {
    const keys = Object.keys(this.nlsMap);
    const json = await this.translate.get(keys).toPromise();
    keys.forEach(k => this.nlsMap[k] = json[k]);

    this.cancelText = await tableCancelText(this.translate);
    this.paginationTranslations = await paginationTranslations(this.translate);
  }

  initializeSession(): any {
    this.bucSessionStorageServiceGlobal = new BucSessionService(Constants.GLOBAL_SESSION_KEY, this.tenantId);
    if (this.bucSessionStorageServiceGlobal.getItem('sessionPrefix')) {
      this.sessionPrefix = this.bucSessionStorageServiceGlobal.getItem('sessionPrefix');
    }
    this.bucSessionStorageService = new BucSessionService(this.sessionPrefix, this.tenantId);
  }

  protected fetchTableData(): Observable<Array<any>> {
    const currentPage = get(this.paginationModel, ['pageData', this.orderlinesListModel.currentPage]);
    if (BucBaseUtil.isVoid(currentPage)) {
      return this.fetchTableDataFromMashup();
    } else {
      return of(getArray(currentPage.data));
    }
  }

  protected fetchTableDataFromMashup(): Observable<Array<any>> {

    let pageAction = 'START';
    let pageModel = {};
    if (this.orderlinesListModel.currentPage > 1) {
      pageAction = 'NEXT';
      pageModel = { ...this.paginationModel.pageData[this.orderlinesListModel.currentPage - 1].pageModel };
    }
    if(pageAction === 'START' && this.orderLineList){
            this.loadedOrderLines = getArray(this.orderLineList.Output.OrderLineList.OrderLine);
            const total = Number(this.orderLineList.Output.OrderLineList.TotalNumberOfRecords);
            const usedTotal = total > this.maximumRecords ? this.maximumRecords : total;
            this.orderlinesListModel.totalDataLength = usedTotal;
            // For mashup pagination:
            const currentPageModel = {};
            setIfNotEmpty(this.orderLineList, 'IsFirstPage', currentPageModel);
            setIfNotEmpty(this.orderLineList, 'IsLastPage', currentPageModel);
            setIfNotEmpty(this.orderLineList, 'PageNumber', currentPageModel);
            setIfNotEmpty(this.orderLineList, 'IsValidPage', currentPageModel);
            setIfNotEmpty(this.orderLineList, 'LastRecord', currentPageModel);
            this.paginationModel.pageData[this.orderlinesListModel.currentPage] = {
              data: this.loadedOrderLines,
              pageModel: currentPageModel
            };
            this.isSearch = false;
            if (this.orderlinesListModel.totalDataLength > 0){
              this.orderLinesLoaded.emit(this.loadedOrderLines);
              return of(this.loadedOrderLines);
              
            }
            this.loadedOrderLines = [];
            this.orderLinesLoaded.emit([]);
            return of([]);

    }

    const payload = {
      orderNumber: this.orderDetails.OrderNo,
      orderHeaderKey: this.orderDetails.OrderHeaderKey,
      enterpriseCode: this.orderDetails.EnterpriseCode,
      sellerOrganizationCode: this.orderDetails.SellerOrganizationCode,
      isHistory: this.orderDetails.isHistory,
      sort: this.orderlinesListModel.currentSortKey || 'PrimeLineNo',
      by: this.orderlinesListModel.currentSortOrder,
      pageNumber: this.orderlinesListModel.currentPage,
      pageSize: this.paginationModel.pageSize
    };

    return this.orderSummaryService.getOrderLineListMashup(pageAction, pageModel, payload, this.filtersExp, this.searchQuery)
          .pipe(map((mashupOutput: any) => {
            const apiOutput = this.bucCommOmsMashupService.getPaginatedMashupOutput(
              mashupOutput, this.GET_COMPLETE_ORDER_LINES_LIST_MASHUP_ID
            );
            this.loadedOrderLines = getArray(apiOutput.Output.OrderLineList.OrderLine);
            const total = Number(apiOutput.Output.OrderLineList.TotalNumberOfRecords);
            const usedTotal = total > this.maximumRecords ? this.maximumRecords : total;
            this.orderlinesListModel.totalDataLength = usedTotal;

            // For mashup pagination:
            const currentPageModel = {};
            setIfNotEmpty(apiOutput, 'IsFirstPage', currentPageModel);
            setIfNotEmpty(apiOutput, 'IsLastPage', currentPageModel);
            setIfNotEmpty(apiOutput, 'PageNumber', currentPageModel);
            setIfNotEmpty(apiOutput, 'IsValidPage', currentPageModel);
            setIfNotEmpty(apiOutput, 'LastRecord', currentPageModel);
            this.paginationModel.pageData[this.orderlinesListModel.currentPage] = {
              data: this.loadedOrderLines,
              pageModel: currentPageModel
            };
            this.isSearch = false;
            if (this.orderlinesListModel.totalDataLength > 0){
              this.orderLinesLoaded.emit(this.loadedOrderLines);
              return this.loadedOrderLines;
            }
            this.loadedOrderLines = [];
            this.orderLinesLoaded.emit([]);
            return [];
    }),
      catchError(() => {
        this.orderlinesListModel.totalDataLength = 0;
        this.orderLinesLoaded.emit([]);
        this.loadedOrderLines = [];
        return [];
      }));
  }

  protected getDataForColumn(id: string, item: any): Promise<any> {
    let rc: any = { data: '' };
    switch (id) {
      case this.TH_LINE:
        const sdrAppliedQty = this.cancelOrderDataService.getSDRAppliedQty(item);

        // Only want it to show "New item" or "Updated line" if added later to the order, e.g. Order was created more than 10 min ago
        const lineCreateTs = BucDateTimeHelper.getMoment(item.Createts);
        const timeSinceLineCreated = BucDateTimeHelper.getMoment(null).diff(BucDateTimeHelper.getMoment(lineCreateTs), 'seconds');
        const orderCreateTs = BucDateTimeHelper.getMoment(this.orderDetails.Createts);
        const timeSinceOrderCreated = BucDateTimeHelper.getMoment(null).diff(BucDateTimeHelper.getMoment(orderCreateTs), 'seconds');
        const isNewItem = timeSinceLineCreated < Constants.TEN_MINUTES_IN_SECONDS && timeSinceOrderCreated > Constants.TEN_MINUTES_IN_SECONDS;

        let timeSinceModified;
        if (item?.Modifyts) {
          const modifyTs = BucDateTimeHelper.getMoment(item?.Modifyts)
          timeSinceModified = BucDateTimeHelper.getMoment(null).diff(BucDateTimeHelper.getMoment(modifyTs), 'seconds');
        } else {
          timeSinceModified = Constants.TEN_MINUTES_IN_SECONDS + 2;
        }
        const isUpdated = timeSinceModified < Constants.TEN_MINUTES_IN_SECONDS && timeSinceOrderCreated > Constants.TEN_MINUTES_IN_SECONDS
        const isZero = Number(item.OrderLineTranQuantity.OrderedQty) === 0;
        rc = {
          data: {
            item,
            quantityChange: !isZero && isUpdated,
            quantityZero: isZero && isUpdated,
            DisplayStatus: (isZero && isUpdated) ? this.nlsMap['ORDER_SUMMARY.ORDER_LINES.TAG_CANCELLED'] :
              isNewItem ? this.nlsMap['ORDER_SUMMARY.ORDER_LINES.TAG_NEW_ITEM'] :
              (!isZero && isUpdated) ? this.nlsMap['ORDER_SUMMARY.ORDER_LINES.TAG_UPDATED'] : '',
            sdrAppliedLabel: this.translate.instant(
              'ORDER_SUMMARY.SHARED.LABEL_STOP_DELIVERY_APPLIED',
              { requestedQty: sdrAppliedQty }
            )
          },
          searchKey: 'PrimeLineNo',
          searchData: item.PrimeLineNo,
          sortData: item.PrimeLineNo,
          numeric: true,
          template: this.templates.lineKeyLink,
          id: item.OrderLineKey
        };
        break;
      case this.TH_ITEM_NAME:
        const titleObj = item.ItemDetails.PrimaryInformation;
        titleObj.title = titleObj?.ShortDescription;
        titleObj.OrderLineKey = item.OrderLineKey;
        titleObj.ItemID = item.ItemDetails.ItemID;
        titleObj.UnitOfMeasure = item.ItemDetails.UnitOfMeasure;
        titleObj.item = item;
        titleObj.relatedLines = getArray(get(item, 'ChildOrderLineRelationships.OrderLineRelationship')).map(i => ({
          ItemDetails:  i.ChildLine.ItemDetails,
          OrderLineKey: i.ChildOrderLineKey,
          PrimeLineNo: i.ChildLine.PrimeLineNo,
        }))
        titleObj.parentLineNo = get(item, 'ParentOrderLineRelationships.OrderLineRelationship.ParentLine.PrimeLineNo', '');
        titleObj.parentLineDesc = get(item, 'ParentOrderLineRelationships.OrderLineRelationship.ParentLine.ItemDetails.PrimaryInformation.ShortDescription', '');
        rc = {
          data: titleObj || '',
          id: item.OrderLineKey,
          template: this.templates.itemDetails,
          searchData: titleObj.title
        };
        break;
      case this.TH_TOTAL_AMOUNT:
        const currencyObj = item.Order && item.Order.length && item.Order[0];
        rc = {
          id: item.OrderLineKey,
          data: {
            value: this.currPipe.transform(item.LineOverallTotals.LineTotal,
                  currencyObj && currencyObj.PriceInfo.Currency, 'symbol'),
            priceIncluded: item.ReshipParentLineKey || item.LinePriceInfo.IsLinePriceForInformationOnly === 'Y',
            id: item.OrderLineKey,
            displayIcon: Number(item.LineOverallTotals.LineTotal) > Number(item.LineOverallTotals.ExtendedPrice) || Number(item.LineOverallTotals.LineTotal) < Number(item.LineOverallTotals.ExtendedPrice)
          },
          template: this.templates.pricePaid,
          title: ''
        };
        break;
      case this.TH_LINE_QUANTITY:
        const modAllow = IsModificationAllowed(item.Modifications.Modification, Constants.MOD_ADD_QUANTITY);
        const canAccess = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.actionResourceIds[this.ACTION_ADD_QUANTITY]);
        console.log('allow : ' + modAllow + ' and access : ' + canAccess);
        rc = {
          data: {
            value: item.OrderLineTranQuantity.OrderedQty || '',
            canEdit: canAccess && modAllow && !this.isCarryOrder,
            item,
            invalid: false,
            invalidText: this.nlsMap['ORDER_LINE_DETAILS.GENERAL.INVALID_QUANTITY_MSG'],
            initialState: true,
          },
          id: item.OrderLineKey,
          template: this.templates.quantity,
          title: ''
        };
        break;
      case this.TH_EXPECTED_DATE:
        rc = {
          data: fmtDate(item.ExpectedStartDate, Constants.DATETIME_FORMAT) || '',
          id: item.OrderLineKey,
          template: this.templates.general,
          title: ''
        };
        if (BucBaseUtil.isVoid(item.ExpectedEndDate)) {
          rc['data'] = fmtDate(item.ExpectedStartDate, Constants.DATETIME_FORMAT);
        }
        break;
      case this.TH_STATUS:
        rc = {
          data: item,
          searchKey: 'DisplayStatus',
          sortData: item.DisplayStatus,
          id: item.OrderLineKey,
          template: this.templates.status
        };
        break;
      case this.TH_FULFILLMENT_METHOD:
        rc = {
          data: this.nlsMap['ORDER_SUMMARY.SHARED.' + item.DeliveryMethod],
          id: item.OrderLineKey,
          sortData: item.DeliveryMethod,
          template: this.templates.general,
          title: ''
        };
        break;
      case BucTableConfiguration.TH_OVER_FLOW_MENU_ACTION_ID:
        rc = {
          data: {
            line: item,
            offset: this.overflowMenu.length > 5 ? {x: 0, y: -100} : undefined
          },
          id: item.OrderLineKey,
          template: this.templates.overflowMenu,
        };
        break;
    }
    return rc;
  }

  showQuantity(data) {
    data.initialState = false;
    setTimeout(() => {
      data.value = cloneDeep(data.value);
    }, 1);
  }

  openPricingSummary(data){
    this.pricingSummaryModal(this.loadedOrderLines.find(item => item.OrderLineKey === data.id));

  }

  getActionResourceIds() {
    // Map actions with resource Ids
    this.actionResourceIds[this.ACTION_CHANGE_FULFILLMENT_METHOD] = this.resourceIdsForOrderDetailsActions.CHANGE_FULFILLMENT_METHOD;
    this.actionResourceIds[this.ACTION_APPLY_HOLDS] = this.resourceIdsForOrderDetailsActions.APPLY_HOLDS_ORDER_LINE;
    this.actionResourceIds[this.ACTION_MANAGE_HOLDS] = this.resourceIdsForOrderDetailsActions.MANAGE_HOLDS_ORDER_LINE;
    this.actionResourceIds[this.ACTION_MANAGE_INSTRUCTIONS] = this.resourceIdsForOrderDetailsActions.MANAGE_INSTRUCTIONS;
    this.actionResourceIds[this.ACTION_APPEASE_CUSTOMER] = this.resourceIdsForOrderDetailsActions.APPEASE_CUSTOMER_ORDER_LINE;
    this.actionResourceIds[this.ACTION_CANCEL_ORDER_LINE] = this.resourceIdsForOrderDetailsActions.CANCEL_ORDER_LINE;
    this.actionResourceIds[this.ACTION_ADD_ORDER_LINE] = this.resourceIdsForOrderDetailsActions.ADD_ORDER_LINE;
    this.actionResourceIds[this.ACTION_CANCEL_MULTIPLE_ORDER_LINES] = this.resourceIdsForOrderDetailsActions.CANCEL_ORDER_LINE;
    this.actionResourceIds[this.ACTION_GIFT_OPTIONS] = this.resourceIdsForOrderDetailsActions.CHANGE_GIFT_OPTIONS_ORDER_LINE;
    this.actionResourceIds[this.ACTION_RESHIP_ORDER_LINE] = this.resourceIdsForOrderDetailsActions.RESHIP;
    this.actionResourceIds[this.ACTION_ADD_QUANTITY] = this.resourceIdsForOrderDetailsActions.ADD_QUANTITY;
    this.actionResourceIds[this.ACTION_VIEW_LINE_DETAILS] = this.resourceIdsForOrderDetailsActions.LINE_DETAILS;
  }

  protected onOverflowMenuActionSelected(id: string): any {
    switch (id) {
      case this.ACTION_APPLY_HOLDS:
        this.applyHolds([this.ofmData.line.OrderLineKey]);
        break;
      case this.ACTION_MANAGE_HOLDS:
        this.manageHolds([this.ofmData.line.OrderLineKey]);
        break;
      case this.ACTION_ADJUST_PRICING:
        this.adjustPricing(this.ofmData);
        break;
      case this.ACTION_MANAGE_INSTRUCTIONS:
        this.manageInstruction(this.ofmData);
        break;
      case this.ACTION_APPEASE_CUSTOMER:
        this.appeaseCustomer([this.ofmData.line]);
        break;
      case this.ACTION_CANCEL_ORDER_LINE:
        this.cancelOrderLine([this.ofmData.line]);
        break;
      case this.ACTION_GIFT_OPTIONS:
        this.giftOptions([this.ofmData.line]);
        break;
      case this.ACTION_RESHIP_ORDER_LINE:
        this.reshipOrderLine(this.ofmData.line.OrderLineKey);
        break;
      case this.ACTION_CHANGE_FULFILLMENT_METHOD:
        const orderLineKeys = [this.ofmData.line.OrderLineKey];
        this.navigateToChangeFulfillment.emit({ orderLineKeys });
        break;
      case this.ACTION_ADD_OR_VIEW_NOTES:
        this.openViewAllNotesModal(this.ofmData.line);
        break;
      case this.ACTION_VIEW_LINE_DETAILS:
        this.openAdditionalLineDetailsModal(this.ofmData.line)
        break;
    }
  }

  _onClickAddLines() {
    const orderLineKeys = this.loadedOrderLines.map(line => line.OrderLineKey);
    this.navigateToAddLinesToOrder.emit({ orderLineKeys });
  }

  openViewAllNotesModal(orderLine): void {
    this.modalService.destroy();
    this.modalService.create({
      component: ViewAllNotesModalComponent,
      inputs: {
        modalData: {
          orderDetails: orderLine,
          notesList:  orderLine.Notes.Note,
          notesReason: [...this.notesTypeList],
          callBack: (noteList) => {  this.addNotesCallback(noteList, orderLine);}
        }
      }
    });
  }

  openAdditionalLineDetailsModal(orderLine) {
    this.actionProcessorService.dispatch<ActionParams>(Constants.ADDITIONAL_LINE_DETAILS, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          size: 'lg',
          orderLine
        }
      }
    });
  }

  openRelatedItemsModal(data) {
    const relatedItemData = {
      lineNo: data.item.PrimeLineNo,
      itemDesc: data.item.ItemDetails.PrimaryInformation?.ShortDescription,
      relatedLines: data.relatedLines
    }

    this.orderCommonService.openRelatedItemsModal(relatedItemData, this.templates.relatedLines, false);
  }



  async getCodesForNotes(): Promise<any> {
    const getCommonCodeListInput = {
      CommonCode: {
        CallingOrganizationCode: this.orderDetails?.EnterpriseCode,
        CodeType: CommonCodes.notesReason,
        DocumentType: this.orderDetails?.DocumentType,
        DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale()
      }
    };
    const resp = await this.orderCommonService.getCommonCodeListForNotes(getCommonCodeListInput);
    this.reasonCodeList = (resp && resp.CommonCodeList && resp.CommonCodeList.CommonCode) || [];

    if (this.reasonCodeList && this.reasonCodeList.length) {
      this.notesTypeList = this.reasonCodeList.map(item => ({
        content: item.CodeShortDescription || item.CodeLongDescription,
        selected: false,
        value: item.CodeValue
      }));
    }
    else {
      this.notesTypeList = [];
    }
  }

  addNotesCallback(updatedNoteList, orderLine)  {
    orderLine.Notes.Note = updatedNoteList;
    if (orderLine.Notes?.Note && orderLine.Notes.Note.length) {
      orderLine.HasNotes = 'Y';
      for (const lineNote of orderLine.Notes.Note) {
        const noteTime = new Date(lineNote.ContactTime).getTime();
        const codeDescription = this.reasonCodeList.filter(codeItem => codeItem.CodeValue === lineNote.ReasonCode);
        lineNote.ReasonText = codeDescription.length ? codeDescription[0].CodeShortDescription : '';
        lineNote.displayNote = true; // for search field selections
        lineNote.isFiltered = true; // for checkbox selections
        lineNote.typeFiltered = true; // for dropdown filtering
        lineNote.isNewNote = noteTime >= (Date.now() - 5000) ? true : false;
      }
      orderLine.Notes.Note.sort((a, b) => {
        const keyA = new Date(a.ContactTime).getTime();
        const keyB = new Date(b.ContactTime).getTime();
        return keyB - keyA;
      });
    } else {
      orderLine.HasNotes = 'N';
    }
  }

  applyHolds(selectedRowIds): void {
    const selectedLines = this.loadedOrderLines.filter((item) => selectedRowIds.includes(item.OrderLineKey));
    this.actionProcessorService.dispatch<ApplyHoldsActionParams>(Constants.APPLY_HOLDS, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          showRadioButton: true,
          holdType: 'ORDER_LINE',
          orderDetails: this.orderDetails,
          loadedOrderLines: selectedLines,
          size: 'lg'
        }
      }
    });
  }

  manageHolds(selected): void {
    const hasHoldData = this.loadedOrderLines.filter((item) => selected.includes(item.OrderLineKey)).filter(
      (element => element.HoldFlag === 'Y')
    );
    this.actionProcessorService.dispatch<ActionParams>(Constants.MANAGE_HOLDS, {
      component: this.componentId,
      data: {
        orderDetails: this.orderDetails,
        selectedOrderLines: selected,
        hasHold: hasHoldData
      }
    });
  }

  onSearchBarExpandToggle(event){
    this._isSearchOpen = event;
  }

  adjustPricing(selected): void {
    this.actionProcessorService.dispatch<ActionParams>(Constants.ADJUST_PRICING, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          summaryDetails: this.orderDetails,
          lineDetails: selected,
          isLineLevel: true,
          size: 'lg'
        }
      }
    });
  }
  pricingSummaryModal(selectedLine){
    this.actionProcessorService.dispatch<ActionParams>(Constants.LINE_PRICING_SUMMARY, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          component: this.componentId,
          orderLineDetails: selectedLine,
          size: 'md',
          summaryDetails: this.orderDetails,
          ruleSetValues: this.ruleSetValues,
          isAdjustPricingResourceAllowed: this.isAdjustPricingResourceAllowed
        }
      }
    });
  }

  manageInstruction(selected): void {
    this.actionProcessorService.dispatch<ActionParams>(Constants.MANAGE_INSTRUCTIONS, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          summaryDetails: this.orderDetails,
          lineDetails: selected,
          size: 'lg'
        }
      }
    });
  }
  appeaseCustomer(selected): void {
    this.actionProcessorService.dispatch<ActionParams>(Constants.APPEASE_CUSTOMER, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          orderDetails: this.orderDetails,
          orderLines: this.loadedOrderLines,
          selectedOrderLines: selected,
          isLineLevel: true,
          size: 'md'
        }
      }
    });
  }

  async cancelOrderLine(selected) {
    if (selected.length > 1) {
      // filter out only eligible lines in case table header action
      selected = selected.filter( line => !this.cancelLinesOptions.notEligibleLines.includes(line));
    }
    const requiredOrderDetails =
      (({ OrderHeaderKey, EnterpriseCode, Modifications, OrderNo }) => ({ OrderHeaderKey, EnterpriseCode, Modifications, OrderNo }))(this.orderDetails);
    this.actionProcessorService.dispatch<ActionParams>(Constants.CANCEL_ORDER, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          orderDetails: requiredOrderDetails,
          orderLines: this.loadedOrderLines,
          cancelLinesOptions: this.cancelLinesOptions,
          selectedOrderLines: selected,
          useTransactionalQtyRuleValue: this.transactionQtyRuleDetailsValue,
          isLineLevel: true,
          size: 'lg'
        }
      }
    });
  }

  async reshipOrderLine(orderLineKey) {
    this.actionProcessorService.dispatch<ActionParams>(Constants.RESHIP_ORDERLINE, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          orderDetails: this.orderDetails,
          orderLineKey,
          isShipment: false,
          size: 'lg'
        }
      }
    });
  }

  giftOptions(selected): void {
    this.actionProcessorService.dispatch<ActionParams>(Constants.GIFT_OPTIONS, {
      component: this.componentId,
      data: {
        modalText: '',
        modalData: {
          orderDetails: this.orderDetails,
          orderLines: this.loadedOrderLines,
          selectedOrderLines: selected,
          orderRuleSetsValue: this.orderRuleSetsValue,
          isLineLevel: true,
          size: 'lg'
        }
      }
    });
  }

  protected onTableLoadComplete(): void {
    this.initialLoad = false;
    this.orderlinesListModel.rowsSelected.forEach(r => r = false);

    if (this.selected.length > 0) {
      const sel = BUCCOMMON.toMap(this.selected);
      this.orderlinesListModel.data.forEach((row, i) =>
        this.orderlinesListModel.rowsSelected[i] = sel[row[0].data.OrderLineKey] ? true : false
      );
    }

    if (this.loadedOrderLines.length) {
      this.orderlinesListModel.data.forEach((row: any, i) => {
        // fetch the orderlineKey from the first fixed Line no. column
        const orderLineKey = row[0].id;
        const orderLine: any = this.loadedOrderLines.filter(line => line.OrderLineKey === orderLineKey)[0];

        // Map bundle parent orderline key and fulfillment mode
        if (orderLine.IsBundleParent === 'Y' && !this.bundleParentFulfillmentModeMapping[orderLine.OrderLineKey]) {
          this.bundleParentFulfillmentModeMapping[orderLine.OrderLineKey] = orderLine.ItemDetails.PrimaryInformation.BundleFulfillmentMode
        }
        const isItemShipTogether = orderLine.BundleParentLine && this.bundleParentFulfillmentModeMapping[orderLine.BundleParentLine.OrderLineKey] === '01';
        // Disable row selection for ship together bundle item
        if (isItemShipTogether) {
          row.disabled = true;
        }

      });
    }

    this.orderlinesListModel.isLoading = false;
    if (this.setInputFocus) {
      setTimeout(() => {
        const input: HTMLElement = document.querySelector('.order-line-table-results .cds--search-input') as HTMLElement;
        if (input) {
          input.focus();
        }
         this.setInputFocus = false;
      })
    }
  }

  protected onToolbarActionClicked(id: string, event?: any): any {
    switch (id) {
      case this.ACTION_APPLY_HOLDS:
        this.applyHolds(this.selected);
        break;
      case this.ACTION_MANAGE_HOLDS:
        this.manageHolds(this.selected);
        break;
      case this.ACTION_ADJUST_PRICING:
        this.adjustPricing({ line: this.loadedOrderLines.find(item => item.OrderLineKey === this.selected[0]) });
        break;
      case this.ACTION_MANAGE_INSTRUCTIONS:
        this.manageInstruction({ line: this.loadedOrderLines.find(item => item.OrderLineKey === this.selected[0]) });
        break;
      case this.ACTION_APPEASE_CUSTOMER:
        this.appeaseCustomer(this.loadedOrderLines.filter(item => this.selected.includes(item.OrderLineKey)));
        break;
      case this.ACTION_GIFT_OPTIONS:
        this.giftOptions(this.loadedOrderLines.filter(item => this.selected.includes(item.OrderLineKey)));
        break;
      case this.ACTION_CANCEL_MULTIPLE_ORDER_LINES:
        const selectedRowsForCancel = this.loadedOrderLines.filter(item =>
          this.selected.includes(item.OrderLineKey) &&
          this.cancelOrderDataService.isOrderLineEligibleForCancellation(item, this.transactionQtyRuleDetailsValue)
        );
        this.cancelOrderLine(selectedRowsForCancel);
        break;
      case this.ACTION_CANCEL_ORDER_LINE:
        const selectedRowForCancel = this.loadedOrderLines.filter(item =>
          this.selected.includes(item.OrderLineKey) &&
          this.cancelOrderDataService.isOrderLineEligibleForCancellation(item, this.transactionQtyRuleDetailsValue)
        );
        this.cancelOrderLine(selectedRowForCancel);
        break;
      case this.ACTION_CHANGE_FULFILLMENT_METHOD:
        const orderLineKeys = [this.selected[0]];
        this.navigateToChangeFulfillment.emit({ orderLineKeys });
        break;
      case this.ACTION_RESHIP_ORDER_LINE:
        this.reshipOrderLine(this.selected[0]);
        break;
      case this.ACTION_ADD_OR_VIEW_NOTES:
        this.openViewAllNotesModal(this.loadedOrderLines.find(item => item.OrderLineKey === this.selected[0]));
        break;
      case this.ACTION_VIEW_LINE_DETAILS:
        this.openAdditionalLineDetailsModal(this.loadedOrderLines.find(item => item.OrderLineKey === this.selected[0]));
        break;
    }
  }

  protected onToolbarContentClicked(id: string, event?: any): any {
  }

  protected updateSortCriteria(colsSorted: Array<BucTableHeaderItem>): void {
    if (colsSorted.length > 0) {
      // multi header sorting is not supported. pick the first one
      this.setSort(colsSorted[0]);
    } else {
      // pick a default fallback column to sort by.
      this.setSort(this.getColumnById(this.defaultSortColumnId));
    }
  }

  protected setSort(sortColumn: BucTableHeaderItem): void {
    this.orderlinesListModel.currentSortKey = sortColumn?.['sortKey'];
    this.orderlinesListModel.currentSortOrder = sortColumn.descending ? 'Desc' : 'Asc';
    this.resetTable();
  }

  protected applyColumnOverrides(): void {
    const colIdsToRemove = [];
    colIdsToRemove.push(this.TH_DERIVED_ORDER);
    colIdsToRemove.push(this.TH_LINE_TYPE);
    colIdsToRemove.push(this.TH_REASON_CODE);

    if (colIdsToRemove.length > 0) { this.removeTableHeaders(colIdsToRemove); }
  }

  private async _initRegularTable(): Promise<any> {
    const templateMapping: BucTableTemplateMapping = { toolbarContent: {} };
    /*     templateMapping.toolbarContent[this.CONTENT_FILTER] = { //TODO
          contentTemplate: this.bucFilterIconTemplate,
          templateData: this.toggleFilter
        }; */

    const createts = BucDateTimeHelper.getMoment(this.orderDetails.Createts);
    const orderAge = BucDateTimeHelper.getMoment(null).diff(BucDateTimeHelper.getMoment(createts), 'days');
    this.addLineResourcePermission = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.actionResourceIds[this.ACTION_ADD_ORDER_LINE]);
    templateMapping.toolbarContent[this.TC_OPEN_TABLE_FIELD_CONFIGURATION_MODAL] = {
      templateData: {
        onClick: this.openTableConfigurationModal.bind(this, false),
        addLine: {
          allowed: this.isAddLinesAllowed && !this.isCarryOrder,
          orderAgeWithinLimit: orderAge <= this.orderRuleSetsValue.orderAgeRule,
          value: 'ORDER_SUMMARY.SHARED.ADD_ORDER_LINE', 
          onClick: this._onClickAddLines.bind(this)
        }
      },
      contentTemplate: this.templates.settingsIcon
    };
    // initialize table from configuration. The table configuration is under order-table.
    await this.initializeTable('orderline-table',
      templateMapping, this.templateRefs.toArray(), []);
    const tableConfiguration: BucTableConfiguration = this.getTableConfiguration();
    this.applyColumnOverrides();
    this.setToolbarAndMenuActions();

    this.orderlinesListModel = tableConfiguration.getBucTableModel();
    this.orderlinesListModel.pageLength = this.paginationModel.pageSize;
    this.orderlinesListModel.currentPage = 1;
    this.selected = [];

    this.toolbarModel = tableConfiguration.getToolbarModel();
    const defaultActions = this.toolbarModel.actions;
    defaultActions.forEach((action: any) => {
      if (action.iconTemplate && (typeof action.iconTemplate === 'string')) {
        action.iconTemplate = this.iconTemplates.getTemplate(action.iconTemplate.toString());
      }
      if (action.contentTemplate && (typeof action.contentTemplate === 'string')) {
        action.contentTemplate = this.contentTemplates.getTemplate(action.contentTemplate.toString());
      }
    });

    // apply user preference
    await this.applyUserPreference().toPromise();
  }

  private setToolbarAndMenuActions(): any {
    const tableConfiguration: BucTableConfiguration = this.getTableConfiguration();

    const actionIds = [
      this.ACTION_APPLY_HOLDS,
      this.ACTION_MANAGE_HOLDS,
      this.ACTION_ADJUST_PRICING,
      this.ACTION_MANAGE_INSTRUCTIONS,
      this.ACTION_APPEASE_CUSTOMER,
      this.ACTION_CHANGE_FULFILLMENT_METHOD,
      this.ACTION_GIFT_OPTIONS,
      this.ACTION_CANCEL_MULTIPLE_ORDER_LINES,
      this.ACTION_RESHIP_ORDER_LINE,
      this.ACTION_CANCEL_ORDER_LINE,
      this.ACTION_ADD_OR_VIEW_NOTES,
      this.ACTION_VIEW_LINE_DETAILS
    ];

    const adjustPricingAction = this.getToolbarActionById(this.ACTION_ADJUST_PRICING);
    if (adjustPricingAction) {
      adjustPricingAction.value = this.isAdjustPricingResourceAllowed ? this.nlsMap['ORDER_SUMMARY.PRICING_SUMMARY.LABEL_ADJUST_PRICING'] : this.nlsMap['ORDER_SUMMARY.PRICING_SUMMARY.LABEL_VIEW_PRICING'];
    }

    const adjustPricingOverflow = this.getOverflowMenuActionById(this.ACTION_ADJUST_PRICING);
    if (adjustPricingOverflow) {
      adjustPricingOverflow.label = this.isAdjustPricingResourceAllowed ? this.nlsMap['ORDER_SUMMARY.PRICING_SUMMARY.LABEL_ADJUST_PRICING'] : this.nlsMap['ORDER_SUMMARY.PRICING_SUMMARY.LABEL_VIEW_PRICING'];
    }

    const addOrViewNotesAction = this.getToolbarActionById(this.ACTION_ADD_OR_VIEW_NOTES);
    if(addOrViewNotesAction){
      addOrViewNotesAction.value = this.isAddNoteOnOrderLineResourceAllowed ? this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_ADD_VIEW_NOTES'] : this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_VIEW_NOTES'];
      addOrViewNotesAction.tooltipMsg = this.isAddNoteOnOrderLineResourceAllowed ? this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_NOTES_INELIGIBLE_TOOLTIP'] : this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_VIEW_NOTES_INELIGIBLE_TOOLTIP']
    }

    const addOrViewNotesOverflow = this.getOverflowMenuActionById(this.ACTION_ADD_OR_VIEW_NOTES);
    if(addOrViewNotesOverflow){
      addOrViewNotesOverflow.label = this.isAddNoteOnOrderLineResourceAllowed ? this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_ADD_VIEW_NOTES'] : this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_VIEW_NOTES'];
      addOrViewNotesAction.tooltipMsg = this.isAddNoteOnOrderLineResourceAllowed ? this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_NOTES_INELIGIBLE_TOOLTIP'] : this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_VIEW_NOTES_INELIGIBLE_TOOLTIP']
    }

    this.setOverflowMenuResourceIds(this.actionResourceIds, actionIds);
    // action
    this.setActionResourceIds(this.actionResourceIds, actionIds);

    // overflow
    this.overflowMenu = tableConfiguration.getOverflowMenuActions();

    tableConfiguration.setActiveOverflowMenuActions(actionIds);
    // toolbar
    let toolbarActionIds = tableConfiguration
      .getToolbarModel().actions
      .sort((a, b) => {
        if(a.sequence < b.sequence) { return -1; }
        if(a.sequence > b.sequence) { return 1; }
        return 0;
      })
      .map((item) => item.id);

    if(!BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(this.actionResourceIds.cancelOrderLine)) {
        toolbarActionIds = toolbarActionIds.filter(id => id !== this.ACTION_CANCEL_MULTIPLE_ORDER_LINES);
    }
    tableConfiguration.setActiveTableToolbarActions(toolbarActionIds);
    this.defaultTableActions = this.getTableConfiguration().getToolbarModel().actions;
  }

  private _subscribeToTextSearch() {
    this.searchSub = this.searchChg.pipe(debounceTime(this.SEARCH_DEBOUNCE_TIME_MS), distinctUntilChanged())
      .subscribe((e) => this.search(e));
  }
  private resetTable() {
    this.orderlinesListModel.currentPage = 1;
    this.paginationModel.pageData = {};
  }

  search(searchValue) {
    this.searchQuery = {};
    this.setInputFocus = true;
    this.orderlinesListModel.isLoading = true;
    if (searchValue && searchValue.length > 0) {
        const searchAgainstFields = [ 'ItemDesc', 'ItemID']; // extended Description is the combination of ItemDesc + (ItemID)
        const Exp = searchAgainstFields.map(field => ({ Name: field, QryType: 'LIKE', Value: searchValue }));
        this.searchQuery = { And: { Or: { Exp } } } ;
        this.isSearch = true;
    }
    this.resetTable();
    this.loadTableAsync(true);
    this.initialLoad = false;
  }

  protected clearSearch() {
    this.loadTableAsync();
    this.search('');
    this.isSearch = false;
    this.orderlinesListModel.totalDataLength = this.initialTotalDataLength;
    this.orderlinesListModel.isLoading = false;
  }

  protected searchTable(searchCriteria: string) {
    const tableModel = this.getTableConfiguration().getBucTableModel();
    tableModel.isLoading = true;
    const searchExpression = new RegExp(regexEscape(searchCriteria), 'i');

    tableModel.isLoading = false;
  }

  onSearch($event): void {
    if (this.SEARCH_DEBOUNCE_TIME_MS) {
      this.searchChg.next($event);
    }
    this.searchValue = $event;
  }

  onSearchEnterClick(event) {
    this.searchChg.next(event);
  }

  searchTextCleared() {
    this.searchChg.next('');
  }

  disableSearch($event): void {
    // this.searchChg['_events']  for input search text
    const key = '_events';
    const inputSearchtext = this.searchChg && this.searchChg[key] && this.searchChg[key][0];
    if ($event && !inputSearchtext) {
      this._isSearchOpen = null;
    } else if (inputSearchtext) {
      this._isSearchOpen = true;
    } else {
      this._isSearchOpen = false;
    }
  }

  async onFilter(filterCriteria) {
    const filterOptions: any = filterCriteria?.options || [];
    const appliedOptions = filterOptions.filter(f => f.value && f.value !== '');
    const appliedFilters: any = [];
    appliedOptions.forEach(option => {
      appliedFilters.push((({ id, value }) => ({ id, value }))(option)); // pluck out only id and value attrs
    });
    this.filtersExp = this._formulateComplexQuery(appliedFilters);
    await this.loadTableAsync();
    this.closeFilter();
  }

  closeFilter() {
    setTimeout(() => {
      this._isFlyoutFilterOpen = true;
      setTimeout(() => this._isFlyoutFilterOpen = false, 100);
    }, 100);
  }

  openFilter(dropdownEvt) {
    setTimeout(() => {
      this._isFlyoutFilterOpen = false;
      setTimeout(() => this._isFlyoutFilterOpen = true, 100);
    }, 100);
  }

  private _formulateComplexQuery(appliedFilters) {
    const filtersExp: any = { EXP: [] };
    for (const filter of appliedFilters) {
      switch (filter.id) {
        case 'fulfilmentMethod':
          filtersExp.DeliveryMethod = filter.value;
          break;
        case 'orderLineStatus':
          filtersExp.Status = filter.value;
          break;
        case 'expectedDeliveryDate':
          if (filter.value.date) {
            filtersExp.ExpectedDate = filter.value.date;
          }
          break;
        case 'orderStartDate':
          if (filter.value.date) {
            filtersExp.FromOrderDate = filter.value.date;
          }
          break;
        case 'orderEndDate':
          if (filter.value.date) {
            filtersExp.ToOrderDate = filter.value.date;
          }
          break;
        default:
          break;
      }
    }
    return filtersExp;
  }

  tableFilterer(fullTable: Array<any>, term: string): any {
    const re = new RegExp(regexEscape(term), 'gi');
    return fullTable.filter(r => r.some(c =>
      c.searchKey ? c.data[c.searchKey].match(re) :
        (typeof c.data === 'string' ? c.data.match(re) : '')));
  }

  onSelectRow(rowIds): void {

    // update selectedRows on row selection but make sure only enabled lines are selected
    const selectedRows = this.loadedOrderLines.filter((item) => {
      return rowIds.includes(item.OrderLineKey) && !this.cancelOrderDataService.isOrderLineCanceled(item);
    });
    // filter out ineligible rows
    rowIds = selectedRows.map(line => line.OrderLineKey);
    // action
    let actions = this.defaultTableActions;

    // Apply holds
    const isApplyHoldsAllowed = selectedRows.map(
      (item) => IsModificationAllowed(item.Modifications.Modification, Constants.MOD_TYPE_HOLD))
      .some((val) => val);
    if (!isApplyHoldsAllowed) {
      actions = this.disableAction(actions, this.ACTION_APPLY_HOLDS);
    }

    // Manage holds
    const hasHold = selectedRows.filter(element => element.HoldFlag === 'Y');
    if (!hasHold.length) {
      actions = this.disableAction(actions, this.ACTION_MANAGE_HOLDS);
    }

    // Disable Change fulfillment method if more than one line is selected
    let isChangeFulfillmentDisabled =
      !(this.selected.length === 1 && selectedRows.length);
    if (this.selected.length === 1 && selectedRows.length === 1) {
      // Disable Change fulfillment method if status is above Shipped and Cancelled
      let maxLineStatus = selectedRows[0]?.MaxLineStatus;
      const idx = maxLineStatus.indexOf('.');
      if (idx !== -1) {
        maxLineStatus = maxLineStatus.substring(0, idx);
      };
      // disable change fulfillment option if order line status is greater than or equal to included in shipment
      if ( maxLineStatus >= '3350') {
        isChangeFulfillmentDisabled = true;
      }
    }
    if(this.orderDetails?.isHistory === 'Y'){
      isChangeFulfillmentDisabled = true;
    }
    if (isChangeFulfillmentDisabled) {
      actions = this.disableAction(actions, this.ACTION_CHANGE_FULFILLMENT_METHOD);
    }

    // Adjust pricing
    const isAdjustPricingDisabled =
      !(this.selected.length === 1 && selectedRows.length &&
        IsModificationAllowed(selectedRows[0].Modifications.Modification, Constants.MOD_TYPE_PRICE)) || selectedRows[0].LinePriceInfo.IsLinePriceForInformationOnly === 'Y';
    if (this.isAdjustPricingResourceAllowed && isAdjustPricingDisabled) {
      actions = this.disableAction(actions, this.ACTION_ADJUST_PRICING);
    } else if (!this.isAdjustPricingResourceAllowed && !(this.selected.length === 1 && selectedRows.length)) {
      actions = this.disableAction(actions, this.ACTION_ADJUST_PRICING);
    } else if (!this.isAdjustPricingResourceAllowed && this.selected.length === 1 && !selectedRows[0].LineCharges.LineCharges) {
      actions = this.disableAction(actions, this.ACTION_ADJUST_PRICING);
      const adjustPricingIndex = actions.findIndex((action) => action.id === this.ACTION_ADJUST_PRICING)
      actions[adjustPricingIndex].tooltipMsg = this.nlsMap['ORDER_SUMMARY.PRICING_SUMMARY.LABEL_LINE_ACTION_NO_ADUSTMENTS_TOOLTIP'];
    }

    // Manage instruction
    const isManageInstructionDisabled =
      !(this.selected.length === 1 && selectedRows.length);
    if (isManageInstructionDisabled) {
      actions = this.disableAction(actions, this.ACTION_MANAGE_INSTRUCTIONS);
    }

    // Appease customer
    if (!(selectedRows.filter(ele => ele.MaxLineStatus !== '9000').length)) {
      actions = this.disableAction(actions, this.ACTION_APPEASE_CUSTOMER);
    }
	
	 //EOMS-9303 Changes Start 
    if (selectedRows[0]?.Status === 'Return Received') {
      actions = this.disableAction(actions, this.ACTION_APPEASE_CUSTOMER);
    }
    //EOMS-9303 Changes End
	
    //EOMS-5621,1463,13713 start
    if (
      selectedRows.some((row) => row.LineType === 'MARKETPLACE') ||
      this.orderDetails?.EnteredBy == 'GLOBALE' ||
      (this.orderDetails?.EntryType == 'Call Center' && this?.orderDetails?.DocumentType == '0001' && !['REFUND', 'EXCHANGE'].includes(this.orderDetails?.OrderPurpose))
    ) {
      actions = this.disableAction(actions, this.ACTION_APPEASE_CUSTOMER);
      actions = this.disableAction(actions, this.ACTION_RESHIP_ORDER_LINE);
    }
    //EOMS-5621,1463,13713 end

    // Cancel multiple orderlines
    const cancelLines = {
      notEligibleLines: [],
      eligibleLines: []
    };
    selectedRows.forEach(row => {
      if (this.cancelOrderDataService.isOrderLineEligibleForCancellation(row, this.transactionQtyRuleDetailsValue)) {
        cancelLines.eligibleLines.push(row);
      } else {
        cancelLines.notEligibleLines.push(row);
      }
    });
    if (cancelLines.notEligibleLines && selectedRows.length === cancelLines.notEligibleLines.length) {
      actions = this.disableAction(actions, this.ACTION_CANCEL_MULTIPLE_ORDER_LINES);
    }

    // Reship orderline
    const isEligibleReshipLine = selectedRows.filter(ele => ele.MaxLineStatus.startsWith('3700')).length &&
    selectedRows.filter(ele => ['DEL', 'SHP'].indexOf(ele.DeliveryMethod) > -1).length;
    if (selectedRows.length > 1 || !isEligibleReshipLine) {
      actions = this.disableAction(actions, this.ACTION_RESHIP_ORDER_LINE);
    }

    this.cancelLinesOptions = cancelLines;

    // Gift options
    const giftOptionsLines = selectedRows.filter((line) => this.isGiftOptionAllowedForLine(line));
    if (!giftOptionsLines.length) {
      actions = this.disableAction(actions, this.ACTION_GIFT_OPTIONS);
    }

    const isNotesDisabled = this.selected.length != 1;
    if(isNotesDisabled) {
      actions = this.disableAction(actions, this.ACTION_ADD_OR_VIEW_NOTES);
    }
    const isViewLineDetailsDisabled = this.selected.length != 1;
    if (isViewLineDetailsDisabled) {
      actions = this.disableAction(actions, this.ACTION_VIEW_LINE_DETAILS);
    }

    if (this.orderCommonService.isOrderArchived(this.orderDetails)) {
      const actionsToHideOnArchivedOrder = [
        this.ACTION_APPLY_HOLDS,
        this.ACTION_MANAGE_HOLDS,
        this.ACTION_ADJUST_PRICING,
        this.ACTION_MANAGE_INSTRUCTIONS,
        this.ACTION_APPEASE_CUSTOMER,
        this.ACTION_CHANGE_FULFILLMENT_METHOD,
        this.ACTION_GIFT_OPTIONS,
        this.ACTION_CANCEL_MULTIPLE_ORDER_LINES,
        this.ACTION_RESHIP_ORDER_LINE,
        this.ACTION_CANCEL_ORDER_LINE
      ];
      actions = actions.filter(action => !actionsToHideOnArchivedOrder.includes(action.id));
    }

    //change tooltip message if  multiple lines are selected
    if (selectedRows.length > 1){
      for (let index=0;index <actions.length; index++){
        switch(actions[index].id){
          case this.ACTION_APPLY_HOLDS:
            actions[index].tooltipMsg = this.nlsMap['APPLY_HOLDS_MODAL.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP'];
            break;
          case this.ACTION_CANCEL_MULTIPLE_ORDER_LINES:
            actions[index].value = this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_CANCEL_MULTIPLE_ORDER_LINES'];
            actions[index].tooltipMsg = this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_CANCEL_MULTIPLE_ORDER_LINE_INELIGIBLE_TOOLTIP'];
            break;
          case this.ACTION_RESHIP_ORDER_LINE:
            actions[index].tooltipMsg = this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_RESHIP_MULTIPLE_ORDER_LINE_INELIGIBLE_TOOLTIP'];
            break;
          case this.ACTION_MANAGE_HOLDS:
            actions[index].tooltipMsg = this.nlsMap['ORDER_SUMMARY.MANAGE_HOLDS.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP'];
            break;
          case this.ACTION_APPEASE_CUSTOMER:
            actions[index].tooltipMsg = this.nlsMap['ORDER_SUMMARY.APPEASE_CUSTOMER_MODEL.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP'];
            break;
          case this.ACTION_ADJUST_PRICING:
            actions[index].tooltipMsg = this.nlsMap['ORDER_SUMMARY.PRICING_SUMMARY.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP'];
            break;
          case this.ACTION_MANAGE_INSTRUCTIONS:
            actions[index].tooltipMsg = this.nlsMap['FULFILLMENT.SHIPMENT_DETAILS.LINKS.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP'];
            break;
          case this.ACTION_CHANGE_FULFILLMENT_METHOD:
            actions[index].tooltipMsg = this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_CHANGE_FULFILLMENT_MULTIPLE_INELIGIBLE_TOOLTIP'];
            break;
          case this.ACTION_GIFT_OPTIONS:
            actions[index].tooltipMsg = this.nlsMap['ORDER_SUMMARY.GIFT_OPTIONS_MODAL.LABEL_LINE_ACTION_MULTIPLE_UNAVAILABLE_TOOLTIP'];
            break;
          default:
            break;
        }
      }
    } else {
      for (let index=0;index <actions.length; index++) {
        switch(actions[index].id) {
          case this.ACTION_CANCEL_MULTIPLE_ORDER_LINES:
            actions[index].value = this.nlsMap['ORDER_SUMMARY.ORDER_LINES.LABEL_CANCEL_ORDER_LINE'];
            break;
        }
      }
    }

    this.toolbarModel.actions = CommonService.addToolbarOverflowAction(actions,
      this.getTableConfiguration(), this.headerActionOverflowTriggerTemplate);
  }

  private disableAction(actions, id) {
    return actions.map((action) => action.id === id ? {...action, disabled: true, showTooltip: true} : action);
  }

  async selectPage(pageNumber): Promise<any> {
    this.selected = [];
    if (this.orderlinesListModel.pageLength !== this.paginationModel.pageSize) {
      this.resetTable();
      this.paginationModel.pageSize = this.orderlinesListModel.pageLength;
    } else {
      this.orderlinesListModel.currentPage = pageNumber;
    }
    await this.loadTableAsync();
    this.orderlinesListModel.isLoading = false;
  }

  protected async loadTableAsync(loadCheck?): Promise<any> {
    if (this.orderlinesListModel && !loadCheck) {
      this.orderlinesListModel.isLoading = true;
    }
    await this.loadTable().toPromise();
  }

  onExpand() {
    this.parentPage.toggleExpand();
  }

  onColSort(index): void {
    const header: BucTableHeaderItem = this.orderlinesListModel.getHeader(index) as BucTableHeaderItem;
    this.initialLoad = false;
    this.setSort(header);
    this.loadTableAsync();
  }

  trackShipment(url): void {
  }

  async getEventDetails(item): Promise<any> {
    const resp = await this.orderCommonService.getCompleteOrderLineDetailsMashup(item.OrderLineKey);
    return resp && resp.OrderLine;
  }

  alignTitle(title: string): any {
    return title.split('(').join('<br>(');
  }

  pageSelectorCapture(e): any {
    this.pageSelector = e;
  }

  private async _getNls(key, params?): Promise<any> {
    return this.translate.get(key, params).toPromise();
  }

  onOfmClick(data) {
    this.ofmData = data;
    const applyHolds = this.getOverflowMenuActionById(this.ACTION_APPLY_HOLDS);
    const manageHolds = this.getOverflowMenuActionById(this.ACTION_MANAGE_HOLDS);
    const manageInstruction = this.getOverflowMenuActionById(this.ACTION_MANAGE_INSTRUCTIONS);
    const adjustPricing = this.getOverflowMenuActionById(this.ACTION_ADJUST_PRICING);
    const appeaseCustomer = this.getOverflowMenuActionById(this.ACTION_APPEASE_CUSTOMER);
    const changeFulfillmentMethod = this.getOverflowMenuActionById(this.ACTION_CHANGE_FULFILLMENT_METHOD);
    const reshipOrderLine = this.getOverflowMenuActionById(this.ACTION_RESHIP_ORDER_LINE);
    const cancelOrderLine = this.getOverflowMenuActionById(this.ACTION_CANCEL_ORDER_LINE);
    const giftOptions = this.getOverflowMenuActionById(this.ACTION_GIFT_OPTIONS);
    const viewLineDetails = this.getOverflowMenuActionById(this.ACTION_VIEW_LINE_DETAILS);

    let disabled = false;
    let isItemShipTogether = false;
    if (data.line.BundleParentLine) {
      isItemShipTogether = this.bundleParentFulfillmentModeMapping[data.line.BundleParentLine.OrderLineKey] === '01';
    }
    const allTableActions = [appeaseCustomer, manageHolds, manageInstruction, adjustPricing,
      applyHolds, cancelOrderLine, reshipOrderLine, giftOptions, changeFulfillmentMethod, viewLineDetails];


      allTableActions.forEach( (a: any) => {
        //EOMS-5621, 1463, 13713 Changes Start
      const isLineTypeMP = data?.line?.LineType == 'MARKETPLACE' || this.orderDetails?.EnteredBy == 'GLOBALE';
      const isPromotionalOrder = (this.orderDetails?.EntryType == 'Call Center' &&  this?.orderDetails?.DocumentType == '0001' && !['REFUND', 'EXCHANGE'].includes(this.orderDetails?.OrderPurpose));
        //EOMS-5621, 1463, 13713 Changes end
		
      // Hide actions if resource permissions are false;
      a.hide = a.resourceId && !BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(a.resourceId);
      if (!a.hide && !isItemShipTogether) {
        switch (a.id) {
          case this.ACTION_APPEASE_CUSTOMER:
            //EOMS-5621, 9303 Changes Start
            disabled = isLineTypeMP || data.line.MaxLineStatus === '9000' || data?.line?.Status === 'Return Received' ; 
            //EOMS-5621, 9303 Changes End
            a.disabled = a.showTooltip = disabled;
            break;
          case this.ACTION_MANAGE_HOLDS:
            disabled = data.line.HoldFlag !== 'Y';
            a.disabled = a.showTooltip = disabled;
            break;
          case this.ACTION_MANAGE_INSTRUCTIONS:
            disabled = data.line.DeliveryMethod === 'CARRY';
            a.disabled = a.showTooltip = disabled;
            break;
          case this.ACTION_ADJUST_PRICING:
            disabled = this.isAdjustPricingResourceAllowed ?
              (!IsModificationAllowed(data.line.Modifications.Modification, Constants.MOD_TYPE_PRICE) || data.line.LinePriceInfo.IsLinePriceForInformationOnly === 'Y') : false;
            a.disabled = a.showTooltip = disabled;
            break;
          case this.ACTION_APPLY_HOLDS:
            disabled =
              !IsModificationAllowed(data.line.Modifications.Modification, Constants.MOD_TYPE_HOLD);
            a.disabled = a.showTooltip = disabled;
            break;
          case this.ACTION_CANCEL_ORDER_LINE:
            disabled =
              !this.cancelOrderDataService.isOrderLineEligibleForCancellation(data.line, this.transactionQtyRuleDetailsValue);
            a.disabled = a.showTooltip = disabled;
            break;
          case this.ACTION_RESHIP_ORDER_LINE:
            // EOMS-6178,1463
            disabled = !data.line.MaxLineStatus.startsWith('3700') || data.line.DeliveryMethod === 'PICK' || data.line.DeliveryMethod === 'CARRY' || isLineTypeMP || isPromotionalOrder;
            a.disabled = a.showTooltip = disabled;
            break;
          case this.ACTION_CHANGE_FULFILLMENT_METHOD:
            disabled = false;
            // Disable Change fulfillment method if status is above Shippped and canelled
            let maxLineStatus = data.line.MaxLineStatus;
            const idx = maxLineStatus.indexOf('.');
            if (idx !== -1) {
              maxLineStatus = maxLineStatus.substring(0, idx);
            };
            if (maxLineStatus >= '3350' ){
              disabled = true;
              a.tooltipMsg =  this.translate.instant('ORDER_SUMMARY.ORDER_LINES.LABEL_CHANGE_FULFILLMENT_INELIGIBLE_TOOLTIP',
              { orderStatus: this.orderDetails?.DisplayStatus });
            }
            if (this.orderDetails?.isHistory === 'Y') {
              disabled = true;
              a.tooltipMsg = 'ORDER_SUMMARY.FULFILLMENT_GROUPS.TOOLTIPS_CHANGE_FULFILLMENT.TEXT_HISTORY_ORD_NOT_ALLOWED';
            }
            a.disabled = a.showTooltip = disabled;
            break;
          case this.ACTION_GIFT_OPTIONS:
            a.disabled = a.showTooltip = !this.isGiftOptionAllowedForLine(data.line);
            break;
          default:
            break;
        }
        if (this.orderCommonService.isOrderArchived(this.orderDetails)) {
          a.disabled = a.showTooltip = true;
        }
      } else if (isItemShipTogether) {
        a.disabled = a.showTooltip = true;
      }
        if (a.id === this.ACTION_ADJUST_PRICING && !this.isAdjustPricingResourceAllowed) {
          a.disabled = a.showTooltip = !data.line.LineCharges.LineCharge;
          a.tooltipMsg = !data.line.LineCharges.LineCharge ? this.nlsMap['ORDER_SUMMARY.PRICING_SUMMARY.LABEL_LINE_ACTION_NO_ADUSTMENTS_TOOLTIP'] : a.tooltipMsg;
        }
    });
    // override disabled state for already canceled lines
    this._removeOverflowMenuActions(data.line);
  }

  _removeOverflowMenuActions(orderLine): void {
    // currently disables all other options except View line details
    const actionsAllowedOnCanceledLine = [];
    const actionsToRemove = this.ofmActions.filter( action => !actionsAllowedOnCanceledLine.includes(action.id));
    if (this.cancelOrderDataService.isOrderLineCanceled(orderLine)) {
      // // Use these methods to remove the ofm actions in the near future
      // this.getTableConfiguration().removeOverflowMenuActions(actionsToRemove);
      // this.getTableConfiguration().setActiveOverflowMenuActions(actionsAllowedOnCanceledLine);
      actionsToRemove.forEach( action => {
        action.disabled = action.showTooltip = true;
      });
    } else {
      // this.getTableConfiguration().setActiveOverflowMenuActions(this.ofmActions);
    }
  }

  getValFrmObj(obj, key): any {
    return obj[key];
  }

  goToOrderLineDetails(orderLine, shouldExpandNotes = false) {
    const selected =this.loadedOrderLines.find(item => item.OrderLineKey === orderLine.OrderLineKey)
    const navParams: any = {
      orderLineKey: orderLine.OrderLineKey, itemId: orderLine.ItemID,
      enterpriseCode: this.orderDetails.EnterpriseCode, uom: orderLine.UnitOfMeasure,
      title: orderLine.ExtendedDisplayDescription,
      shouldExpandNotes,
      modifications: selected.Modifications,
      orderNumber: this.orderDetails.OrderNo,
    };
    if (shouldExpandNotes) {
      navParams.itemId = orderLine.ItemDetails.ItemID;
      navParams.uom = orderLine.ItemDetails.UnitOfMeasure;
      navParams.title = orderLine.ItemDetails.PrimaryInformation.ExtendedDisplayDescription;
    }
    this.ccNavigationSvc.openUrlInNewTab(Constants.ORDER_LINE_DETAILS_ROUTE, navParams);
  }
  hasOrderLineInstructions(data) {
    return data?.item?.Instructions?.NumberOfInstructions !== '0'
  }

  private isGiftOptionAllowedForLine(line) {
    const isLineModificationAllowed = IsModificationAllowed(line.Modifications.Modification, Constants.MOD_TYPE_GIFT_OPTION);
    if (line.DeliveryMethod === 'SHP') {
      return isLineModificationAllowed && this.orderRuleSetsValue.giftShipRule;
    } else {
      return isLineModificationAllowed && this.orderRuleSetsValue.giftPickRule;
    }
  }

  setIconsContainerWidth(data) {
    let iconsCount = [
      this.hasOrderLineInstructions(data),
      data.item.GiftFlag === 'Y',
      data.item.StopDeliveryRequestDetails.TotalNumberOfRecords > 0,
      data.item.HasNotes === 'Y',
      data.item.HasReturnLines === 'Y',
      data.item.ReshippedQty > 0,
      data.item.ReshipParentLineKey
    ]
    iconsCount = iconsCount.filter(e => e)
    return {'max-icons-width': iconsCount.length > 2 , 'min-icons-width': iconsCount.length === 2};
  }

  onNumberInputChange($event, data) {
    if (!$event || !isNaN($event?.toString())) {
      data.invalid = false;
      if (BucBaseUtil.isUndefinedOrNull($event)) {
        data.invalid = true;
        data.invalidText = this.nlsMap['ORDER_LINE_DETAILS.GENERAL.BAD_QUANITY_NUMBER_MSG'];
      } else if (Number($event?.toString()) < 0) {
        data.invalidText = this.nlsMap['ORDER_LINE_DETAILS.GENERAL.INVALID_QUANTITY_MSG'];
        data.invalid = true;
      } else {
        this.updateQuantity($event, data)
      }
    }
  }

  async updateQuantity(event, data) {
    const val = event.toString();
    const prevVal = data?.value;
    let displayMsg;
    let noteType;
    let qtyUpdated = true;
    const noteText = await this._getNls('ORDER_SUMMARY.ORDER_LINES.UPDATE_QUANTITY_NOTE_TEXT',
      { prevQty: formatNumber(this.curLocale, prevVal), newQty: formatNumber(this.curLocale, val),
        lineNo: data?.item?.PrimeLineNo, lineKey: data?.item?.OrderLineKey,
        desc: data?.item?.ItemDetails?.PrimaryInformation?.Description });
    // const requiredOrderDetails =
    //   (({ OrderHeaderKey, EnterpriseCode, Modifications, OrderNo }) => ({ OrderHeaderKey, EnterpriseCode, Modifications, OrderNo }))(this.orderDetails);
    if (Number(val) > Number(prevVal)) {
      displayMsg = await this._getNls('ORDER_SUMMARY.ORDER_LINES.UPDATE_QUANTITY_INCREASED',
        { desc: data?.item?.ItemDetails?.PrimaryInformation?.Description, qty: val });
      noteType = CommonCodes.increaseLineQty;
    } else if ((Number(val) === 0) || (Number(val) < Number(prevVal))) {
      displayMsg = await this._getNls('ORDER_SUMMARY.ORDER_LINES.UPDATE_QUANTITY_DECREASED',
        { desc: data?.item?.ItemDetails?.PrimaryInformation?.Description, qty: val });
      noteType = CommonCodes.decreaseLineQty;
    } else if (Number(val) === Number(prevVal)) {
      qtyUpdated = false;
    }
    if (qtyUpdated) {
      data.disabled = true;
      this.actionProcessorService.dispatch<ActionParams>(Constants.UPDATE_QTY, {
        component: this.componentId,
        data: {
          data,
          value: val,
          previousValue: prevVal,
          orderLines: this.loadedOrderLines,
          displayMsg,
          noteType,
          noteText,
          errorCallback: this.updateQuantityFailure.bind(this),
        }
      });
    }
  }

  updateQuantityFailure(data) {
    const hIndex = this.orderlinesListModel.header.findIndex(x => x.id === this.TH_LINE_QUANTITY);
    this.orderlinesListModel.data.forEach(r => {
      if (r[0]['id'] === data.data.item.OrderLineKey) {
        r[hIndex].data.invalid = true;
        r[hIndex].data.invalidText = data.quantityInvalidText;
        r[hIndex].data.value = data.previousValue;
      }
    });
  }

  openProductDetails(itemId) {
    this.ccNavigationSvc.openUrlInNewTab(Constants.PRODUCT_BROWSING_ROUTE, { searchValue: itemId, 
      title: this.nlsMap['PRODUCT_BROWSING.TAB_TITLE'],
      sellerEnterpriseCode: this.orderDetails.EnterpriseCode, showDetailsPage: true, external:true  });
  }
}
