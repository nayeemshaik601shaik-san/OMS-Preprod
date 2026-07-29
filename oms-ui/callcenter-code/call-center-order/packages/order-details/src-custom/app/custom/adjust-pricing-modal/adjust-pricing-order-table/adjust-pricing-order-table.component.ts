/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2021, 2024
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
import { ModalService } from 'carbon-components-angular';
import { Observable, of, ReplaySubject } from 'rxjs';
import { BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { SharedExtensionConstants } from '@call-center/order-shared/lib/shared-extension.constants';
import { Constants } from '@call-center/order-shared/lib/common/order.constants';

@Component({
  selector: 'call-center-adjust-pricing-order-table:not([extn])',
  templateUrl: './adjust-pricing-order-table.component.html',
  styleUrls: ['./adjust-pricing-order-table.component.scss'],
})
export class AdjustPricingOrderTableComponent extends BaseTableComponent implements OnInit, OnChanges {
  EXTENSION = {
    TOP: SharedExtensionConstants.ADJUST_PRICING_ORDER_TABLE_RS_TOP,
    BOTTOM: SharedExtensionConstants.ADJUST_PRICING_ORDER_TABLE_RS_BOTTOM
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

  @Input() selectedOrder = [];

  // Table headers
  public readonly TH_ORDER = 'order';
  public readonly TH_ADJUSTMENTS = 'adjustments';
  public readonly TH_ORG_TOTAL = 'originalTotal';
  public readonly TH_NEW_TOTAL = 'newTotal';
  public readonly STR_DASH = '';

  public readonly defaultPageLength = Constants.TABLE_PAGE_LENGTH_10;
  public componentId = 'adjust-pricing-order-table';

  model: BucTableModel = new BucTableModel();
  toolbarModel: BucTableToolbarModel;
  private searchChg = new ReplaySubject<string>(1);
  cancelText: any = { CANCEL: '' };
  pageNo = 1;
  pageSize;
  initialLoad: boolean;
  chargesChanged = false;
  paginationTranslations: any;
  searchValue: any;
  selected: string[] = [];
  _orginalTotal: number;

  lcl = localeBuc2Angular(BucSvcAngularStaticAppInfoFacadeUtil.getUserLanguage());
  public currPipe: BucCommonCurrencyFormatPipe = new BucCommonCurrencyFormatPipe(this.lcl)


  constructor(
    private mdlService: ModalService,
    private inj: Injector,
    public translate: TranslateService,
  ) {
    super(inj.get(BucTableHelperService), mdlService);
  }

  ngOnChanges(c: SimpleChanges) {
    if(this.chargesChanged){
      this.chargesChanged = false;
    }
    if ( c.selectedOrder.currentValue?.length > 0 && c.selectedOrder.currentValue !== c.selectedOrder.previousValue) {
      if(c.selectedOrder.currentValue[0]  && c.selectedOrder.previousValue[0] ){
        this.chargesChanged = true;
      }
      this.reloadTable();
    }
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
  async reloadTable() {
    await this.loadTableAsync();
  }

  protected fetchTableData(): Observable<Array<any>> {
      return of(this.selectedOrder);
  }

  protected getDataForColumn(id: string, item: any): Promise<any> {
    let rc: any = { data: '' };

    if(!this.chargesChanged){
      this._orginalTotal= item?.OverallTotals.GrandTotal;
    }

    switch (id) {
      case this.TH_ORDER:
        rc = {
          data: item?.OrderNo,
          searchKey: 'order',
          numeric: true,
          id: item?.OrderHeaderKey,
        };
        break;
      case this.TH_ADJUSTMENTS:
        const deltaAdjustment = ((Number(item?.OverallTotals.GrandTotal) - Number(this._orginalTotal) + Number.EPSILON) * 100) / 100;
        rc = {
          data:  !this.chargesChanged ? this.STR_DASH : this.currPipe.transform(deltaAdjustment, item?.PriceInfo.Currency, 'symbol', '1.2-2'),
          id: item?.OrderHeaderKey,
          template: this.templates.general
        };
        break;
      case this.TH_ORG_TOTAL:
        const orgTotal = this._orginalTotal;
        rc = {
          data: this.currPipe.transform(orgTotal, item?.PriceInfo.Currency, 'symbol'),
          id: item?.OrderHeaderKey,
          template: this.templates.general
        };
        break;
      case this.TH_NEW_TOTAL:
        const newTotal = item?.OverallTotals.GrandTotal;
        const summaryInfo= this.getOrderPricingSummaryDetails(item);
        let promotionAmount= 0;
        if (summaryInfo.promotionList.length > 0){
          promotionAmount = summaryInfo.promotionList.reduce((a, b) => a + ((Number(b.AwardAmount) + Number.EPSILON) * 100) / 100 , 0.00);
        }
        const hdrChargesWithoutShipping = ((Number(item?.OverallTotals.GrandCharges)- Number(item?.OverallTotals.GrandDiscount)- Number(item?.OverallTotals.GrandShippingTotal) + Number.EPSILON) * 100) / 100;
        const adjustments = ((hdrChargesWithoutShipping - promotionAmount) + Number.EPSILON) * 100 /100;

        rc = {
          id: item?.OrderHeaderKey,
          data: {
            subTotal: this.currPipe.transform(item?.OverallTotals.LineSubTotal, item?.PriceInfo.Currency, 'symbol'),
            newTotal: !this.chargesChanged ? this.STR_DASH :this.currPipe.transform(newTotal, item?.PriceInfo.Currency, 'symbol'),
            adjustments:this.currPipe.transform(adjustments, item?.PriceInfo.Currency, 'symbol'),
            promotions:this.currPipe.transform(promotionAmount, item?.PriceInfo.Currency, 'symbol'),
            shipping:
              this.currPipe.transform(item?.OverallTotals.GrandShippingTotal, item?.PriceInfo.Currency, 'symbol'),
            taxes: this.currPipe.transform(item?.OverallTotals.GrandTax, item?.PriceInfo.Currency, 'symbol'),
            totAmount: this.currPipe.transform(item?.OverallTotals.GrandTotal, item?.PriceInfo.Currency, 'symbol'),
            hasChanges: this.chargesChanged
          },
          template: this.templates.totalAmount
        };
        break;


    }
    return rc;
  }

  onSearch($event): void {
    this.searchChg.next($event);
  }

  protected onOverflowMenuActionSelected(id: string): any { }

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

  protected updateSortCriteria(colsSorted: Array<BucTableHeaderItem>): void { }

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

  onColSort(index): void { }

  protected setSort(sortColumn: BucTableHeaderItem): void { }

  onSelectRow(rows): void { }


}
