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
  Component, EventEmitter, Injector, Input, OnChanges, OnInit, Output, QueryList, SimpleChanges, TemplateRef,
  ViewChild, ViewChildren
} from '@angular/core';
import {
  BaseTableComponent, BucCommonCurrencyFormatPipe, BucIconTemplatesComponent, BucTableConfiguration, BucTableHeaderItem,
  removeHeaderStyling, BucTableHelperService, BucTableModel, BucTableTemplateMapping, BucTableToolbarModel,
  COMMON, TableComponent, TemplateIdDirective, BucTemplateDirective,
  CommonBinaryOptionModalComponent,
  getArray,
  localeBuc2Angular
} from '@buc/common-components';
import { Constants, OrderCommonService } from '@call-center/order-shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalService, TableHeaderItem } from 'carbon-components-angular';
import { from, Observable, of, ReplaySubject } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ExtensionConstants } from '../../../extension.constants';
import { OrderSummaryService } from '../../data-service/order-summary.service';
import { BucCommOmsMashupService, BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { get } from 'lodash';

@Component({
  selector: 'call-center-appease-customer-order-line-table',
  templateUrl: './appease-customer-order-line-table.component.html',
  styleUrls: ['./appease-customer-order-line-table.component.scss']
})
export class AppeaseCustomerOrderLineTableComponent extends BaseTableComponent implements OnInit {
  EXTENSION = {
    TOP: ExtensionConstants.APPEASE_CUSTOMER_ORDER_LINE_TABLE_OD_TOP,
    BOTTOM: ExtensionConstants.APPEASE_CUSTOMER_ORDER_LINE_TABLE_OD_BOTTOM
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

  @Input() orderLines = [];
  @Input() orderDetails;
  @Input() showSelectionColumn = false;
  @Output() orderLinesSelectionChange = new EventEmitter<string[]>();

  // Table headers
  public readonly TH_LINE = 'line';
  public readonly TH_ITEM_NAME = 'itemName';
  public readonly TH_LINE_QUANTITY = 'lineQuantity';
  public readonly TH_TOTAL = 'total';


  public componentId = 'appease-customer-line-table';
  private GET_COMPLETE_ORDER_LINES_LIST_MASHUP_ID = 'icc.order.summary.getCompleteOrderLineList';

  model: BucTableModel = new BucTableModel();
  toolbarModel: BucTableToolbarModel;
  private searchChg = new ReplaySubject<string>(1);
  cancelText: any = { CANCEL: '' };
  sortKey = 'PrimeLineNo';
  sortOrder: 'Desc' | 'Asc' = 'Desc';
  defaultSortColumnId = this.TH_LINE;
  pageNo = 1;
  direction = 1;
  pageSize;
  initialLoad: boolean;
  paginationTranslations: any;
  searchValue: any;
  selected: string[] = [];
  selectedOrderLines = [];
  loadedOrderLines = [];
  lcl = localeBuc2Angular(BucSvcAngularStaticAppInfoFacadeUtil.getUserLanguage());
  public currPipe: BucCommonCurrencyFormatPipe = new BucCommonCurrencyFormatPipe(this.lcl)

  constructor(
    private mdlService: ModalService,
    private inj: Injector,
    public translate: TranslateService,
    private orderSummaryService: OrderSummaryService,
    private bucCommOmsMashupService: BucCommOmsMashupService,
		private orderCommonService: OrderCommonService
  ) {
    super(inj.get(BucTableHelperService), mdlService);
  }

  ngOnInit(): void {
    this.initialize();
  }

  async initialize(): Promise<any> {
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
    await this.initializeTable('appease-customer-orderline-table',
      templateMapping, [], []);
    const tableConfiguration: BucTableConfiguration = this.getTableConfiguration();
    this.model = tableConfiguration.getBucTableModel();
    this.pageSize = this.model.defaultPageLength;
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
    if (this.orderDetails) {
      const payload = {
        orderNumber: this.orderDetails.OrderNo,
        orderHeaderKey: this.orderDetails.OrderHeaderKey,
        enterpriseCode: this.orderDetails.EnterpriseCode,
        sellerOrganizationCode: this.orderDetails.SellerOrganizationCode,
        sort: this.sortKey || 'PrimeLineNo',
        by: this.sortOrder,
        pageNumber: this.pageNo,
        pageSize: this.pageSize
      };
      let key = this.pageNo === 1 ? 'START' : 'NEXT';
      if (this.direction < 0 && this.pageNo > 1) {
        key = 'PREVIOUS';
      }
      const input = this.pageNo === 1 ? {} : this.model;

      return from(this.orderSummaryService.getOrderLineListMashup(key, input, payload, { StatusQryType: 'NE', Status: '9000', BundleFulfillmentModeQryType: 'NE', BundleFulfillmentMode: '01' }))
        .pipe(map((mashupOutput: any) => {
          const apiOutput = this.bucCommOmsMashupService.getPaginatedMashupOutput(
            mashupOutput, this.GET_COMPLETE_ORDER_LINES_LIST_MASHUP_ID
          );
          Object.assign(this.model, apiOutput);
          let respArray = apiOutput.Output.OrderLineList && apiOutput.Output.OrderLineList.OrderLine;

          // EOMS-9303 - Changes Start 
          // Remove lines with Status Return Received
          respArray = respArray?.filter(line => line?.Status !== 'Return Received');
          // EOMS-9303 - Changes End

          this.model.totalDataLength = apiOutput.Output.OrderLineList.TotalNumberOfRecords;
          if (apiOutput.Output.OrderLineList.TotalNumberOfRecords === '0') {
            this.model.totalDataLength = respArray.length;
          }
          if (this.model.totalDataLength > 0) {
            this.loadedOrderLines.push(...respArray);
            return respArray;
          } else {
            return [];
          }
        }),
          catchError((err) => {
            this.model.totalDataLength = 0;
            return [];
          }));
    } else {
      if (this.orderLines?.length) {
        // EOMS-9303 changes start
        // Filter out Return Received lines
        const filteredLines = this.orderLines.filter(
          line => line?.Status !== 'Return Received'
        );
        this.model.totalDataLength = filteredLines.length;
        const indexFrom = (this.model.currentPage - 1) * this.pageSize;
        const indexTo = indexFrom + this.pageSize;
        const lines = filteredLines.slice(indexFrom, indexTo);
        // EOMS-9303 changes start
        return of(lines);
      } else {
        return of([]);
      }
    }
  }

  protected getDataForColumn(id: string, item: any): Promise<any> {
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
      case this.TH_TOTAL:
        const currencyObj = item.Order && item.Order.length && item.Order[0];
        rc = {
          id: item.OrderLineKey,
          data: {
            value: this.currPipe.transform(item.LineOverallTotals.LineTotal, currencyObj && currencyObj.PriceInfo.Currency, 'symbol'),
            priceIncluded: item.LinePriceInfo.IsLinePriceForInformationOnly === 'Y'
          },
          template: this.templates.pricePaid
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

  openRelatedItemsModal(data) {
    const relatedItemData = {
      lineNo: data.item.PrimeLineNo,
      itemDesc: data.item.ItemDetails.PrimaryInformation?.ShortDescription,
      relatedLines: data.relatedLines
    };
    this.orderCommonService.openRelatedItemsModal(relatedItemData, this.templates.relatedLines, true);
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
        this.model.rowsSelected[i] = sel[(row[0] as any).id] ? true : false
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

  async selectPage(pageNumber) {
    this.direction = pageNumber - this.pageNo;
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

  onColSort(index): void {
    const header: BucTableHeaderItem = this.model.getHeader(index) as BucTableHeaderItem;
    this.initialLoad = false;
    this.setSort(header);
    this.loadTableAsync();
  }

  protected setSort(sortColumn: BucTableHeaderItem): void {
    this.sortKey = sortColumn?.['sortKey'];
    this.sortOrder = sortColumn.descending ? 'Desc' : 'Asc';
  }

  onSelectRow(rows): void {
    const lines = this.loadedOrderLines.filter((item) => rows.includes(item.OrderLineKey));
    this.orderLinesSelectionChange.emit(lines);
  }


}
