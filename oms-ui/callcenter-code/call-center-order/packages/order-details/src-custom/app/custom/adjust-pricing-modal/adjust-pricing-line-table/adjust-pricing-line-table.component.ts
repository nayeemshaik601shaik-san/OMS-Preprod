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

import { Component, Injector, Input, OnChanges, OnInit, QueryList, SimpleChanges, TemplateRef, ViewChild, ViewChildren } from '@angular/core';
import { BaseTableComponent, BucCommonCurrencyFormatPipe, BucIconTemplatesComponent, BucTableConfiguration, BucTableHeaderItem,
  removeHeaderStyling, BucTableHelperService, BucTableModel, BucTableTemplateMapping, BucTableToolbarModel,
  COMMON, TableComponent, TemplateIdDirective, BucTemplateDirective, 
  localeBuc2Angular} from '@buc/common-components';
import { TranslateService } from '@ngx-translate/core';
import { ModalService, TableHeaderItem } from 'carbon-components-angular';
import { Observable, of, ReplaySubject } from 'rxjs';
import { BucSvcAngularStaticAppInfoFacadeUtil, CallCenterNavigationService } from '@buc/svc-angular';
import { SharedExtensionConstants } from '@call-center/order-shared/lib/shared-extension.constants';
import { Constants } from '@call-center/order-shared/lib/common/order.constants';

@Component({
  selector: 'call-center-adjust-pricing-line-table:not([extn])',
  templateUrl: './adjust-pricing-line-table.component.html',
  styleUrls: ['./adjust-pricing-line-table.component.scss'],
})
export class AdjustPricingLineTableComponent extends BaseTableComponent implements OnInit, OnChanges {
  EXTENSION = {
    TOP: SharedExtensionConstants.ADJUST_PRICING_LINE_TABLE_RS_TOP,
    BOTTOM: SharedExtensionConstants.ADJUST_PRICING_LINE_TABLE_RS_BOTTOM
  };
  
  private templates: { [id: string]: TemplateRef<any> } = {};
  @ViewChildren(TemplateIdDirective) set _templates(a: QueryList<TemplateIdDirective>) {
    if (a) {
      a.forEach(({ id, template }) => this.templates[id] = template);
    }
  }
  groupResults: TableComponent;
  @ViewChild('groupResults', { static: false }) set content(content: TableComponent) {
    if (content) {
      this.groupResults = content;
      removeHeaderStyling(this.groupResults.elementRef.nativeElement, this.renderer2);
    }
  }
  @ViewChildren(BucTemplateDirective) templateRefs: QueryList<BucTemplateDirective>;
  @ViewChild('iconTemplates', { static: true }) iconTemplates: BucIconTemplatesComponent;

  @Input() selectedOrderLines = [];

  // Table headers
  public readonly TH_LINE = 'line';
  public readonly TH_ITEM_NAME = 'itemName';
  public readonly TH_LINE_QUANTITY = 'lineQuantity';
  public readonly TH_ADJUSTMENTS = 'adjustments';
  public readonly TH_ORG_TOTAL = 'originalTotal';
  public readonly TH_NEW_TOTAL = 'newTotal';
  public readonly STR_DASH = '';

  public readonly defaultPageLength = Constants.TABLE_PAGE_LENGTH_10;
  public componentId = 'adjust-pricing-orderline-table';

  model: BucTableModel = new BucTableModel();
  toolbarModel: BucTableToolbarModel;
  private searchChg = new ReplaySubject<string>(1);
  cancelText: any = { CANCEL: '' };
  sortKey = 'PrimeLineNo';
  sortOrder: 'Desc' | 'Asc' = 'Desc';
  defaultSortColumnId = this.TH_LINE;
  pageNo = 1;
  pageSize;
  initialLoad: boolean;
  paginationTranslations: any;
  searchValue: any;
  chargesChanged = false;
  selected: string[] = [];
  _orginalTotal: number;

  lcl = localeBuc2Angular(BucSvcAngularStaticAppInfoFacadeUtil.getUserLanguage());
  public currPipe: BucCommonCurrencyFormatPipe = new BucCommonCurrencyFormatPipe(this.lcl)

  constructor(
    private mdlService: ModalService,
    private inj: Injector,
    public translate: TranslateService,
    public ccNavigationSvc: CallCenterNavigationService
  ) {
    super(inj.get(BucTableHelperService), mdlService);
  }

  ngOnChanges(c: SimpleChanges) {
    if(this.chargesChanged){
      this.chargesChanged = false;
    }
    if ( c.selectedOrderLines.currentValue?.length > 0 && c.selectedOrderLines.currentValue !== c.selectedOrderLines.previousValue) {
      if(c.selectedOrderLines.currentValue[0]  && c.selectedOrderLines.previousValue[0] ){
        this.chargesChanged = true;
      }
      this.reloadTable();

    }
  }

  getOrderPricingSummaryDetails (orderSummary: any, isLine?: boolean){
    const summaryInfo = {
      shippingChargesList: [],
      headerAdjustmentList: [],
      promotionList: []
    };
    const charges= isLine? orderSummary?.LineCharges?.LineCharge : orderSummary?.HeaderCharges?.HeaderCharge;
    charges?.forEach( charge => {
      if (charge.ChargeAmount > 0){
        if (charge.ChargeCategory === 'Shipping'){
          summaryInfo.shippingChargesList.push(charge);
        }else {
          if (orderSummary.Awards?.Award ){
            const promoIndex = orderSummary.Awards.Award.findIndex(award =>
              ( charge.ChargeCategory === award.ChargeCategory && charge.ChargeName === award.ChargeName
                && award.AwardApplied === 'Y'));
            if (promoIndex === -1){
              summaryInfo.headerAdjustmentList.push(charge);
            } else{
              summaryInfo.promotionList.push(orderSummary.Awards.Award[promoIndex]);
            }
          }else {
            summaryInfo.headerAdjustmentList.push(charge);
          }
        }
      }
    });
    return summaryInfo;
  }

  ngOnInit(): void {
    this.chargesChanged = false;
    this.initialize();
  }

  async initialize(): Promise<any> {
    this.pageSize = this.defaultPageLength;
    await this.initializeTableConfig();
    this.initialLoad = true;
    this.searchValue = '';
  }

  async initializeTableConfig(): Promise<any> {
    await this._initRegularTable();
  }

  private async _initRegularTable(): Promise<any> {
    const templateMapping: BucTableTemplateMapping = { toolbarContent: {} };
    templateMapping.toolbarContent[this.TC_OPEN_TABLE_FIELD_CONFIGURATION_MODAL] = {
      contentTemplate: this.templates.settingsIcon
    };
    // initialize table from configuration. The table configuration is under order-table.
    await this.initializeTable(this.componentId,
      templateMapping, [], []);
    const tableConfiguration: BucTableConfiguration = this.getTableConfiguration();
    this.model = tableConfiguration.getBucTableModel();
    this.model.pageLength = this.pageSize;
    this.model.currentPage = this.pageNo;
    this.selected = [];

    this.toolbarModel = tableConfiguration.getToolbarModel();
    const defaultActions = this.toolbarModel.actions;
    defaultActions.forEach(action => {
      if (action.iconTemplate) {
        action.iconTemplate = this.iconTemplates.getTemplate(action.iconTemplate.toString());
      }
    });

    // apply user preference
    await this.applyUserPreference().toPromise();
  }

  async reloadTable() {
    await this.loadTableAsync();
  }

  protected fetchTableData(): Observable<Array<any>> {
    return of(this.selectedOrderLines);
  }

  protected getDataForColumn(id: string, item: any): Promise<any> {
    if(!this.chargesChanged){
      this._orginalTotal= item?.LineOverallTotals.LineTotal;
    }
    let rc: any = { data: '' };

    switch (id) {
      case this.TH_LINE:
        rc = {
          data: item.PrimeLineNo,
          searchKey: 'PrimeLineNo',
          sortData: item.PrimeLineNo,
          numeric: true,
          id: item.OrderLineKey,
        };
        break;
      case this.TH_ITEM_NAME:
        const titleObj = item.ItemDetails.PrimaryInformation;
        titleObj.title = titleObj.ShortDescription;
        titleObj.ItemID = item.ItemDetails.ItemID;
        rc = {
          data: titleObj || '',
          id: item.OrderLineKey,
          template: this.templates.itemDetails
        };
        break;
      case this.TH_LINE_QUANTITY:
        rc = {
          data: item.OrderedQty || '',
          id: item.OrderLineKey,
          template: this.templates.general
        };
        break;
      case this.TH_ADJUSTMENTS:
        const deltaAdjustment =  (Number(item?.LineOverallTotals.LineTotal)  - Number(this._orginalTotal) + Number.EPSILON) * 100 / 100;
        rc = {
          data: !this.chargesChanged ? this.STR_DASH :this.currPipe.transform(deltaAdjustment, item && item.PriceInfo?.Currency, 'symbol'),
          id: item.OrderLineKey,
          template: this.templates.general
        };
        break;
      case this.TH_ORG_TOTAL:
        const orgTotal = this._orginalTotal;
        rc = {
          data: this.currPipe.transform(orgTotal, item.PriceInfo?.Currency, 'symbol'),
          id: item?.OrderLineKey,
          template: this.templates.general
        };
      break;
      case this.TH_NEW_TOTAL:
        const newTotal = Number(item.LineOverallTotals.LineTotal);
        let promotionAmount = 0.00;
        const summaryInfo= this.getOrderPricingSummaryDetails(item, true);
        if (summaryInfo.promotionList.length > 0){
          promotionAmount = summaryInfo.promotionList.reduce((a, b) => a + (Number(b.AwardAmount) + Number.EPSILON) * 100 / 100 , 0.00);
        }
        const lineAdjustmentsWithoutShipping = (Number(item.LineOverallTotals.Charges)
        - Number(item.LineOverallTotals.Discount)- Number(item.LineOverallTotals.ShippingTotal) + Number.EPSILON) * 100 / 100;
        const adjustments = (lineAdjustmentsWithoutShipping - promotionAmount + Number.EPSILON) * 100 /100;
        rc = {
          id: item.OrderLineKey,
          data: {
            subTotal: this.currPipe.transform(item.LineOverallTotals.ExtendedPrice, item && item.PriceInfo?.Currency, 'symbol'),
            newTotal: !this.chargesChanged ? this.STR_DASH :this.currPipe.transform(newTotal, item && item.PriceInfo?.Currency, 'symbol'),
            shipping:
              this.currPipe.transform(item.LineOverallTotals.ShippingTotal, item && item.PriceInfo?.Currency, 'symbol'),
            taxes: this.currPipe.transform(item.LineOverallTotals.Tax, item && item.PriceInfo?.Currency, 'symbol'),
            adjustments:this.currPipe.transform(adjustments, item && item.PriceInfo?.Currency, 'symbol'),
            promotions:this.currPipe.transform(promotionAmount, item && item.PriceInfo?.Currency, 'symbol'),
            totAmount: this.currPipe.transform(newTotal, item && item.PriceInfo?.Currency, 'symbol'),
            hasChanges: this.chargesChanged
          },
          template: this.templates.totalAmount
        };
        break;
      case BucTableConfiguration.TH_OVER_FLOW_MENU_ACTION_ID:
        rc = {
          data: {
            line: item
          },
          id: item.OrderLine ? item.OrderLine.OrderLineKey : item.OrderHeaderKey,
          template: this.templates.overflowMenu,
        };
        break;
    }
    return rc;
  }



  onSearch($event): void {
    this.searchChg.next($event);
  }

  protected onOverflowMenuActionSelected(id: string): any { }

  alignTitle(title: string): any {
    return title.split('(').join('<br>(');
  }

  protected onTableLoadComplete(): void {
    this.initialLoad = false;
    this.model.rowsSelected.forEach(r => r = false);

    if (this.selected.length > 0) {
      const sel = COMMON.toMap(this.selected);
      this.model.data.forEach((row, i) =>
        this.model.rowsSelected[i] = sel[row[0].data.OrderLineKey] ? true : false
      );
    }

    this.model.isLoading = false;

  }

  protected onToolbarActionClicked(id: string, event?: any): any { }

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

  async selectPage(pageNumber): Promise<any> {
    this.pageNo = pageNumber;
    this.pageSize = this.model.pageLength;
    this.model.currentPage = pageNumber;
    await this.loadTableAsync();
    this.model.isLoading = false;
  }

  protected async loadTableAsync(): Promise<any> {
    if (this.model) {
      this.model.isLoading = true;
    }
    await this.loadTable().toPromise();
  }

  goToOrderLineDetails(orderLine) {
    this.ccNavigationSvc.openUrlInNewTab(Constants.ORDER_LINE_DETAILS_ROUTE,
      { orderLineKey: orderLine.OrderLineKey, itemId: orderLine.ItemID,
        enterpriseCode: this.selectedOrderLines[0].ItemDetails.OrganizationCode, uom: orderLine.UnitOfMeasure });
  }

  onColSort(index): void {
    const header: BucTableHeaderItem = this.model.getHeader(index) as BucTableHeaderItem;
    this.initialLoad = false;
    this.setSort(header);
    this.loadTableAsync();
  }

  protected setSort(sortColumn: BucTableHeaderItem): void {
    this.sortKey = sortColumn?.['sortKey'];
    this.sortOrder = sortColumn?.descending ? 'Desc' : 'Asc';
  }

  onSelectRow(rows): void {
  }


}
