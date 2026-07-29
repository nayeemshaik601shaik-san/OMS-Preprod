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

import { Component, Inject, OnDestroy, OnInit, TemplateRef, ViewChild, ViewContainerRef } from '@angular/core';
import { CCNotificationService, COMMON, getArray, getCurrentLocale, TableModelExtension } from '@buc/common-components';
import { TranslateService } from '@ngx-translate/core';
import { BaseModal, TableHeaderItem } from 'carbon-components-angular';
import { isEmpty } from 'lodash';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { OrderSharedService } from '@call-center/order-shared/lib/order-shared.service';
import { SharedExtensionConstants } from '@call-center/order-shared/lib/shared-extension.constants';
import { Constants } from '@call-center/order-shared/lib/common/order.constants';
import { OrderCommonService } from '@call-center/order-shared/lib/data-service/order-common.service';

@Component({
    selector: 'call-center-adjust-pricing-modal',
    templateUrl: './adjust-pricing-modal.component.html',
    styleUrls: ['./adjust-pricing-modal.component.scss'],
    providers:[ OrderSharedService ]
})
export class ExtnAdjustPricingModalComponent extends BaseModal implements OnInit, OnDestroy {
    EXTENSION = {
        TOP: SharedExtensionConstants.ADJUST_PRICING_MODAL_RS_TOP,
        BOTTOM: SharedExtensionConstants.ADJUST_PRICING_MODAL_RS_BOTTOM
    };

    @ViewChild('chargeName', { static: true }) private chargeName: TemplateRef<any>;
    @ViewChild('chargeType', { static: true }) private chargeType: TemplateRef<any>;
    @ViewChild('amount', { static: true }) private amount: TemplateRef<any>;
    @ViewChild('chargeApplyTo', { static: true }) private chargeApplyTo: TemplateRef<any>;
    @ViewChild('amountReadOnly', { static: true }) private amountReadOnly: TemplateRef<any>;
    @ViewChild('chargeNameReadOnly', { static: true }) private chargeNameReadOnly: TemplateRef<any>;
    @ViewChild('chargeTypeReadOnly', { static: true }) private chargeTypeReadOnly: TemplateRef<any>;
    @ViewChild('chargeApplyToReadOnly', { static: true }) private chargeApplyToReadOnly: TemplateRef<any>;

    public readonly defaultPageLen = Constants.TABLE_PAGE_LENGTH_10;
    componentId = 'AdjustPricingModalComponent';
    model = new TableModelExtension();
    lineModel = new TableModelExtension();
    paginationTranslations: any;
    pageSize: number;
    pageNo: number;
    chargeHeaders: any;
    summaryDetails: any;
    headerChargeDetailsData: any;
    chargeNameList = [];
    chargeInfoList = [];
    chargeApplyToList = [];
    couponList = [];
    mapChargeCategory;
    mapCouponList;
    emptyChargeName: any;
    applyEnabled = true;
    appliedCouponPromo = [];
    newChargeDetails = {
        HeaderCharge: [],
        LineCharge: []
    };
    headerCharges: any;
    lineCharges: any;
    orderHeaderKey: string;
    orderLineKey: string;
    isLineLevel: boolean;
    isDraftOrder:any;
    lineChargeDetailsData: any;
    currentApplyTo: any;
    isApplyToChanged: boolean;
    selectedOrderLines = [];
    saveChargesEnabled = false;
    isChargesAdded = false;
    isAnyChangeAppliedOnModal = false;
    note = '';
    allowedModifications;
    allowModificationPrice: boolean;
    modificationInfoList= [];
    permissionsArray = [];
    chargeTypes: any;
    readonly resourceIdsForOrderActions = {
      ADD_MODIFY_CHARGES_ORDER : 'ICC000003',
      ADD_MODIFY_CHARGES_ORDERLINE : 'ICC000004'
    }
    isResourceAllowedToAddChangeOrderCharges: boolean;
    notificationShown = false;
    notificationObj: any;
    selectedChargePairs = [];
    remainChargeNamesForType = {};
    totalChargeNamesForType = {};
    // flag for toggle the add new row button
    ranOutOfChargeOptions = false;
    isInitialized = false;
    showManagerIDField = false;
    managerID = '';
    invalidManagerID = false;
    hasOverrideError = false;
    cTypes;
    modifiedIndex;
    modifiedId;
    modifiedAmount;
    violationOutput;
    curLocale;
    currencySymbolBefore;
    

    protected readonly nlsMap: any = {
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CATEGORY': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_NAME': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_TYPE': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_AMOUNT': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_TAXES': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_APPLY_TO': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_LINE': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_UNIT': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_ADJUSTMENT_SAVED_MSG': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGES_ERROR_MSG': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.MSG_TOOLTIP_ORDER_LINE': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.MSG_TOOLTIP_ORDER': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGENAMENONREFUNDABLEDES': '',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_BILLABLE':'',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_DISCOUNT':'',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.MSG_TOOLTIP_LINE_DISCOUNT':'',
        'ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.MSG_TOOLTIP_LINE_CHARGE':''
    };

    private amountChangedSub: Subject<any> = new Subject();
    private amountSubscription = new Subscription();

    constructor(
        @Inject('modalData') public modalData,
        public translate: TranslateService,
        private orderCommonService: OrderCommonService,
        private notificationService: CCNotificationService,
        public orderSharedService:OrderSharedService
    ) {
        super();
    }

    async ngOnInit() {
        
        await this.initialize();
        this.amountSubscription = this.amountChangedSub.pipe(
            debounceTime(750),
            distinctUntilChanged())
            .subscribe(({ index, value, chargeAmountId }) => {
            isEmpty(value) ? null : this.saveCharges(index, value, chargeAmountId)
            this.modifiedId = chargeAmountId;
            this.modifiedAmount = value;
            this.modifiedIndex = index;
        });
        this.isInitialized = true;
    }


    async initialize() {
        this.pageSize = Constants.TABLE_PAGE_LENGTH_10;
        this.pageNo = this.pageNo ? this.pageNo : 1;
        this.curLocale = getCurrentLocale();
        if (this.curLocale.startsWith('zh')) {
            this.curLocale = 'zh';
        }
        this.orderHeaderKey = this.modalData.summaryDetails.OrderHeaderKey;
        this.isLineLevel = this.modalData.isLineLevel
        this.isDraftOrder = this.modalData.summaryDetails.DraftOrderFlag === 'Y';
        await this._initTranslations();
        this.hasResourcePremissions();
        this.isLineLevel ? this.getOrderLineDetails(this.modalData, this.isChargesAdded) :
        this.getOrderDetails(this.modalData.summaryDetails);
        this.prepareHeaders();
        await this.callInitApis();
        this.prepareChargeTypes();
        await this.initializeChargeTables();
        this.isChargeModificationPermissionAllowed();
    }


    prepareChargeTypes(){
        this.cTypes = [
            {
                content: this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_BILLABLE'],
                code:Constants.BILLABLE_CODE
            },
            {
                content: this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_DISCOUNT'],
                code:Constants.DISCOUNT_CODE
            }
        ];

        // keep track of the total number of chargeNames for each type, used to recover the number for tracking purposes
        // using the code of each charge type for identification
        this.totalChargeNamesForType[Constants.DISCOUNT_CODE] = this.getChargeNameList('Y', 'Y').length;
        this.totalChargeNamesForType[Constants.BILLABLE_CODE] = this.getChargeNameList('N', 'Y').length;

        this.remainChargeNamesForType[Constants.DISCOUNT_CODE] = this.totalChargeNamesForType[Constants.DISCOUNT_CODE];
        this.remainChargeNamesForType[Constants.BILLABLE_CODE] = this.totalChargeNamesForType[Constants.BILLABLE_CODE];
    }

    _repopulateCharges(data, promotions, removeOperation){
        this.selectedChargePairs = [];
        // recover the remaing charge names numbers back to the original
        this.cTypes.forEach(ctype => {
            this.remainChargeNamesForType[ctype.code] = this.totalChargeNamesForType[ctype.code];
        })
        data.forEach(charges => {
            // make sure the row actually exists
            const obj = {
                chargeType: charges[0].data.code,
                chargeName: charges[1].data.value
            };
            this.selectedChargePairs.push(obj);
            if(charges[3].value || charges[3].data.value){
                this.remainChargeNamesForType[obj.chargeType] -= 1;
            }
        });
        if(promotions == 0 && removeOperation){
            // promotions are decrement to 0 and at the same time
            this.remainChargeNamesForType[Constants.DISCOUNT_CODE] += 1
        }
    }

    async getOrderDetails(modalData) {
        this.summaryDetails = (modalData.isPromotionApplied) ? (modalData.responseData) : modalData;
        if (!isEmpty(this.summaryDetails?.Promotions)) {
            this.appliedCouponPromo = (this.summaryDetails.Promotions.Promotion);
        }
        const charges = this.groupByUtil(this.summaryDetails?.HeaderCharges?.HeaderCharge, Constants.KEY_ISMANUAL);
        this.headerChargeDetailsData = {
            HeaderCharge: charges
        };
        this.allowedModifications = this.summaryDetails?.Modifications;
        if(this.modificationInfoList.length == 0){
          this.modificationInfoList = this.allowedModifications?.Modification;
        }
        if (modalData.isPromotionApplied) {
            await this.tableData();
        }
        this.addChargeDescriptions(charges);
    }

    getOrderLineDetails(modalData, isChargesAdded) {
        let orderLineData;
        if (isChargesAdded) {
            this.selectedOrderLines = modalData?.OrderLine.filter((orderLine) => orderLine.OrderLineKey === this.orderLineKey);
            orderLineData = this.selectedOrderLines[0];
        } else {
            this.summaryDetails = modalData?.summaryDetails;
            orderLineData = modalData?.lineDetails.line;
            if(!this.modalData.skipModificationPermissionCheck){
            if(!orderLineData.Modifications && this.summaryDetails.OrderLines[0]){
              orderLineData.Modifications= this.summaryDetails.OrderLines[0].Modifications;
            }
            }
            this.selectedOrderLines = [orderLineData];
        }
        this.currencySymbolBefore = COMMON.isCurrencySymbolBefore(this.curLocale, this.summaryDetails?.PriceInfo?.Currency);
        this.selectedOrderLines = this.selectedOrderLines.map(obj => ({ ...obj, PriceInfo: { Currency: this.summaryDetails?.PriceInfo?.Currency } }));
        if(this.modificationInfoList.length == 0 && !this.modalData.skipModificationPermissionCheck){
        this.modificationInfoList = orderLineData.Modifications?.Modification;
        }
        this.lineChargeDetailsData = orderLineData.LineCharges;
        this.orderLineKey = this.modalData.lineDetails.line.OrderLineKey;
        if (!this.lineChargeDetailsData?.LineCharge !== undefined) {
            const charges = this.lineChargeDetailsData?.LineCharge
                ?.map((element) => ({
                    ...element,
                    ChargeApplyTo: this.getChargesApplyTo(element)
                }));
            this.lineChargeDetailsData.LineCharge = this.groupByUtil(charges, Constants.KEY_ISMANUAL);
            this.addChargeDescriptions(this.lineChargeDetailsData.LineCharge);
        }
    }

    addChargeDescriptions(charges) {
        charges?.forEach( charge => {
            if (charge.ChargeAmount > 0){
                if (charge.IsShippingCharge === 'N' ){
                    if (this.summaryDetails.Awards?.Award ){
                    const promo = this.summaryDetails.Awards.Award.filter(award =>
                        ( charge.ChargeCategory === award.ChargeCategory && charge.ChargeName === award.ChargeName
                        && award.AwardApplied === 'Y' ));

                        promo.forEach( item => {
                            charge.Description = charge.Description ? charge.Description + ", " + item.Description : item.Description;
                        });
                    }
                }
            }
        });
    }

    protected async _initTranslations() {
        const keys = Object.keys(this.nlsMap);
        const json = await this.translate.get(keys).toPromise();
        keys.forEach(k => this.nlsMap[k] = json[k]);
    }

    async callInitApis() {
        const couponInput = {
            OrderHeaderKey: this.orderHeaderKey,
            GetCoupons: 'Y',
            GetOrderRules: 'Y'
        };
        const categoryListInput = {
            CallingOrganizationCode: this.summaryDetails.EnterpriseCode,
            DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
            DocumentType: this.summaryDetails.DocumentType
        };

        const chargeKeys = {
            CallingOrganizationCode: this.summaryDetails.EnterpriseCode,
            DisplayLocalizedFieldInLocale: BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale(),
            DocumentType: this.summaryDetails.DocumentType,
            ChargeCategory: ''
        };

        await this.orderCommonService.getChargeNameAndCategoryList(categoryListInput, chargeKeys).then(
            mashupOutput => {
                this.mapChargeCategory = mashupOutput.getChargeCategoryList;
                this.emptyChargeName = mashupOutput.getChargeNameList;


            if(this.mapChargeCategory.ChargeCategoryList.ChargeCategory && this.emptyChargeName.ChargeNameList.ChargeName){
                this.chargeInfoList = this.emptyChargeName.ChargeNameList.ChargeName.map(chargeName => {
                    const chargeCategory = this.mapChargeCategory.ChargeCategoryList.ChargeCategory.find(catgory =>
                    catgory.ChargeCategory === chargeName.ChargeCategory);
                    const cName= chargeName.ChargeName;
                    const chargeNameDesc = chargeCategory.IsRefundable ==='Y' ? chargeName.Description : this.translate.instant('ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGENAMENONREFUNDABLEDES', {cName: chargeName.Description});
                    return {category: chargeCategory.ChargeCategory, isRefundable:chargeCategory.IsRefundable, isBillable: chargeCategory.IsBillable,
                        desc:chargeNameDesc,isDiscount:chargeCategory.IsDiscount, cName:cName}
                  });
                }
        });




    }

    prepareHeaders() {
        this.chargeHeaders = 
            [

              new TableHeaderItem({ data: this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_TYPE'],
                    sortable: false, style: { width: '19rem', height: '3rem' } }),

                new TableHeaderItem({ data: this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_NAME'],
                    sortable: false, style: { width: '19rem', height: '3rem' }, className: this.isLineLevel ? 'headerChargeName' : '' }),

                this.isLineLevel &&
                new TableHeaderItem({ data: this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_APPLY_TO'],
                    sortable: false, style: { width: '17rem', height: '3rem' }, className: 'headerApplyTo' }),
                new TableHeaderItem({ data: this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_AMOUNT'],
                    sortable: false, style: { width: '15rem', height: '3rem' } })
            ]
        ;
    }

    async initializeChargeTables() {
        this.model.header = this.chargeHeaders;
        this.model.isLoading = false;
        this.model.pageLength = this.pageSize;
        this.model.currentPage = this.pageNo;
        this.model.data = [];
        await this.tableData();
    }
    isChargeModificationPermissionAllowed(){
      this.allowModificationPrice = false;
      if(this.modificationInfoList?.length > 0){
         if(this.modificationInfoList.find(mod => mod.ModificationType === Constants.STR_PRICE
          && mod.ModificationAllowed === 'Y')){
          this.allowModificationPrice = true;
         }
      }else if(this.modalData.skipModificationPermissionCheck){
        this.allowModificationPrice = true;
      }
      return this.allowModificationPrice;
    }
    async addNewChargeRow() {
      if(this.isResourceAllowedToAddChangeOrderCharges){
        if(this.isChargeModificationPermissionAllowed){
            if (this.isLineLevel) {
                const summaryLineCharge = {
                    ChargeAmount: '',
                    ChargeCategory: '',
                    ChargeName: '',
                    ChargeType: '',
                    ChargeApplyTo: '',
                    newRow: true,
                };
                if (this.lineChargeDetailsData?.LineCharge === undefined) {
                    const lineChargeArr = [];
                    lineChargeArr.push(summaryLineCharge);
                    const key = Constants.KEY_LINE_CHARGE;
                    this.lineChargeDetailsData[key] = lineChargeArr;
                } else {
                    this.lineChargeDetailsData.LineCharge.push(summaryLineCharge);
                }
            } else {
                const summaryCharge = {
                    ChargeAmount: '',
                    ChargeCategory: '',
                    ChargeType: '',
                    ChargeName: '',
                    newRow: true,
                };
                if (this.headerChargeDetailsData?.HeaderCharge === undefined) {
                    const headerChargeArr = [];
                    headerChargeArr.push(summaryCharge);
                    const key = Constants.KEY_HEADER_CHARGE;
                    this.headerChargeDetailsData[key] = headerChargeArr;
                } else {
                    this.headerChargeDetailsData.HeaderCharge.push(summaryCharge);
                }
            }
        }
        this.tableData();
      }
    }

    async tableData() {
        const chargeSummaryTable = (model: TableModelExtension, data: Array<any>) => {

            model.asMap = {};
            model.fullTable = this.prepareChargeSummaryTableResponse(data);
            model.fullTableLen = model.fullTable.length;
            model.totalDataLength = model.fullTableLen;

            if (model.fullTableLen > 0) {
                model.pages = COMMON.calcPagination(model.fullTable, model.pageLength);
                model.calcPgLen = model.pageLength;
                model.data = model.pages[0];
                model.currentPage = 1;
                if(!this.isInitialized){
                    this._repopulateCharges(this.model.data, 0, false);
                }
            } else {
                model.data = [];
            }
        };

        try {
            let data;
            if (this.isLineLevel) {
                if (this.lineChargeDetailsData) {
                    data = getArray(this.lineChargeDetailsData?.LineCharge);
                    const filteredData = data.filter((item) => !(item.IsManual === 'N' && !parseFloat(item.ChargeAmount)));
                    if(filteredData.length > 0){
                    [{ model: this.model }]
                        .forEach(o => chargeSummaryTable(o.model, filteredData));
                    }else{
                      this.addNewChargeRow();
                    }
                } else {
                    [this.model]
                        .forEach(m => {
                            m.totalDataLength = 0;
                            m.data = [];
                        });
                }
            } else {
                if (this.headerChargeDetailsData) {
                    data = getArray(this.headerChargeDetailsData?.HeaderCharge);
                    const filteredData = data.filter((item) => !(item.IsManual === 'N' && !parseFloat(item.ChargeAmount)));
                    if(filteredData.length > 0){
                        [{ model: this.model }]
                            .forEach(o => chargeSummaryTable(o.model, filteredData));
                    }else{
                      this.addNewChargeRow();
                    }
                } else {
                    [this.model]
                        .forEach(m => {
                            m.totalDataLength = 0;
                            m.data = [];
                        });
                }
            }
        } catch (err) {
            [this.model]
                .forEach(m => {
                    m.totalDataLength = 0;
                    m.data = [];
                });
            console.log('error occured', err);
        }
    }

    mapChargeTypeToDisplay(isBillable, isDiscount){
        return isBillable === 'Y' && isDiscount === 'N'? this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_BILLABLE'] : this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_DISCOUNT'];
    }

    prepareChargeSummaryTableResponse(response) {

        return response.map((i, index) => {
            const currentCharge = this.getChargeDetails(i);
            const row = [
                {
                    data: {
                        value: this.mapChargeTypeToDisplay(i.IsBillable, i.IsDiscount),
                        index,
                        code: i.IsBillable == 'Y' && i.IsDiscount == 'N' ?  Constants.BILLABLE_CODE : Constants.DISCOUNT_CODE,
                        newRow: i.newRow,
                        list: this.getChargeTypeList(),
                        isDisable: false,
                        isManual: i.IsManual
                    },
                    template: !this.isResourceAllowedToAddChangeOrderCharges || this.cannotAllowModification(i.IsManual,i.newRow) ? this.chargeTypeReadOnly : this.chargeType,
                    title: this.mapChargeTypeToDisplay(i.IsBillable, i.IsDiscount)
                },
                {
                    data: {
                        value: this.getChargeName(i.ChargeCategory, i.ChargeName),
                        discountLabel:  i.Description ? (i.IsDiscount === 'Y' ? this.translate.instant('ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.MSG_TOOLTIP_LINE_DISCOUNT', {desc: i.Description}) : 
                        this.translate.instant('ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.MSG_TOOLTIP_LINE_CHARGE', {desc: i.Description })) : "",
                        index,
                        newRow: i.newRow,
                        isDisable: true,
                        isManual: i.IsManual
                    },
                    template:!this.isResourceAllowedToAddChangeOrderCharges || this.cannotAllowModification(i.IsManual,i.newRow)   ? this.chargeNameReadOnly : this.chargeName,
                    title: this.getChargeName(i.ChargeCategory,i.ChargeName),
                },
                this.isLineLevel && {
                    data: {
                        value: i.ChargeApplyTo,
                        index,
                        list: this.getApplyToList(i.ChargeApplyTo),
                        newRow: i.newRow,
                        isDisable: i.ChargeName ? false : true,
                        isManual: i.IsManual,
                        uniqueId: (i.ChargeCategory) ? index + '~' + (i.ChargeCategory) : index,
                    },
                    template:!this.isResourceAllowedToAddChangeOrderCharges ||  this.cannotAllowModification(i.IsManual,i.newRow)    ? this.chargeApplyToReadOnly : this.chargeApplyTo,
                    title: i.ChargeApplyTo
                },
                {
                    data: {
                        value: currentCharge, // formatNumber(this.curLocale, currentCharge),
                        index,
                        isDiscount: i.ChargeCategory,
                        isDisable: this.isChargeAmountDisabled(i),
                        isManual: i.IsManual,
                        uniqueId: (i.ChargeCategory) ? index + '~' + (i.ChargeCategory) : index,
                        chargeAmountId: (i.ChargeCategory) ? index + '~' + (i.ChargeCategory) : index,
                    },
                    template:!this.isResourceAllowedToAddChangeOrderCharges ||  this.cannotAllowModification(i.IsManual,i.newRow)     ? this.amountReadOnly : this.amount,
                    title: i.ChargeAmount
                },
            ];
            return row;
        });
    }
   cannotAllowModification(manual, newRow){
    return !this.allowModificationPrice || !this.isManual(manual, newRow) ;
   }
   hasResourcePremissions(){
      const permissionToCheck= !this.isLineLevel ? this.resourceIdsForOrderActions.ADD_MODIFY_CHARGES_ORDER : this.resourceIdsForOrderActions.ADD_MODIFY_CHARGES_ORDERLINE;
      this.isResourceAllowedToAddChangeOrderCharges = BucSvcAngularStaticAppInfoFacadeUtil.canUserAccessResource(permissionToCheck);
   }
    getChargeDetails(charge){
      if(this.isLineLevel){
        if(charge.ChargeApplyTo === this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_UNIT']){
          return  charge.ChargePerUnit;
        }else{
          return  charge.ChargePerLine;
        }
      }else{
        return charge.ChargeAmount
      }
    }

    getChargeName(categoryName,chargeName){
      let name: any;
        this.chargeInfoList.filter((item) => {
            if ( item && item.category === categoryName && item.cName === chargeName) {
                name = item.desc;
            }
        });
        if(!name){
            name = chargeName;
        }
        return name;
    }


  getChargeTypeList(){
    this.chargeTypes = [];
    this.cTypes.forEach(cType=> {
        // the charge type option exists only when it has at least one charge name options
        if(this.remainChargeNamesForType[cType.code] > 0){
            this.chargeTypes.push(
                {
                    content:cType.content,
                    value :cType.code
                }
            )
        }
    });

    return this.chargeTypes;
  }

   /**  getChargeNameList(discount, billable) {
        this.chargeNameList = [];
            if(this.chargeInfoList){
              this.chargeNameList =  this.chargeInfoList.map(info=> {
                if(info && info.isDiscount === discount && info.isBillable === billable){
                  return {
                    content: info.desc !== '' ? info.desc : info.cName,
                    value: info.cName ,
                    data: info
                  }
                }
            });
            return this.chargeNameList = this.chargeNameList.filter(x=>x);
          }
    }**/
        //Crocs related changes EOMS-3805 start
          getChargeNameList(discount, billable) {
            this.chargeNameList = [];
        
            if (!this.chargeInfoList) return [];
        
            const matchingList = this.chargeInfoList.filter(info =>
                info &&
                info.isDiscount === discount &&
                info.isBillable === billable
            );
        
            const categoryMap: Record<string, any[]> = {}; 
            for (const info of matchingList) {
                if (!categoryMap[info.category]) {
                    categoryMap[info.category] = [];
                }
                categoryMap[info.category].push(info);
            }
        
            if (this.isLineLevel && categoryMap["ShippingCharge"]) {
                const nonShippingCharges = categoryMap["ShippingCharge"].filter(i => i.desc !== "ShippingCharge");
                if (nonShippingCharges.length === 0) {
                    delete categoryMap["ShippingCharge"]; 
                }
            }
        
            Object.values(categoryMap).forEach((group: any[]) => {
                group.forEach(info => {
                    this.chargeNameList.push({
                        content: info.desc !== '' ? info.desc : info.cName,
                        value: info.cName,
                        data: info
                    });
                });
            });
        
            return this.chargeNameList = this.chargeNameList.filter(x=>x);
        }
        //Crocs related changes EOMS-3805 end
        

    getApplyToList(value) {
        this.chargeApplyToList = [];
        /**const applyType = [
            this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_LINE'],
            this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_UNIT']
        ];**/
        const applyType = [
            this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_LINE']
        ];
        applyType.forEach((element) => {
            this.chargeApplyToList.push({
                content: element,
                value: element,
                selected: value === element ? true : false
            });
        });
        return this.chargeApplyToList;
    }

    getChargesApplyTo(element) {
        if (parseFloat(element.ChargePerUnit)) {
            return this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_UNIT'];
        } else {
            return this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_LINE'];
        }
    }

    isManual(isManual, newRow) {
      return newRow ? true : isManual != 'Y' ? false : true;
    }

    changeInChargeType(event, index){
        const changeKey = Constants.KEY_CHARGE_NAME;
        const changeValue = event.item.value;

        // bookmarking the selected pairs
        if(index < this.selectedChargePairs.length){
            this.selectedChargePairs[index].chargeType = changeValue;
            this.selectedChargePairs[index].chargeName = null;
        }else{
            this.selectedChargePairs.push({
                chargeType: changeValue,
                chargeName: null
            });
        }

        let list=[];
        this.model.data[index][1].data.isDisable = false;

        list = [];
        if(changeValue === Constants.BILLABLE_CODE){
            list = this.getChargeNameList('N','Y');
        }else if(changeValue === Constants.DISCOUNT_CODE){
            list = this.getChargeNameList('Y','Y');
        }

        list = this._filterSelectedChargeNames(list, changeValue);
        this.model.data[index][0].data.value = changeValue;
        this.model.data[index][1].data.list = list;
        // turn the disable
    }

   /**  _filterSelectedChargeNames(list, chargeType){
        let results = []
        for (let i = 0; i < list.length; i++) {
            const rowData = list[i];
            let exists = false;
            for (let j = 0; j < this.selectedChargePairs.length; j++) {
                const chargePair = this.selectedChargePairs[j]
                if(chargePair.chargeType == chargeType && chargePair.chargeName == rowData.data.desc){
                    exists = true;
                    break;
                }
            }
            if(!exists){
                results.push(rowData);
            }
        }
        return results;
    }**/

        //Crocs related changes EOMS-3805 start
        _filterSelectedChargeNames(list, chargeType) {
            let results = [];
            for (let i = 0; i < list.length; i++) {
                const rowData = list[i];
                
                // Exclude ShippingCharge if it's line-level adjustment
                if (this.isLineLevel && rowData.data.desc === "ShippingCharge") {
                    continue;
                }
        
                let exists = false;
                for (let j = 0; j < this.selectedChargePairs.length; j++) {
                    const chargePair = this.selectedChargePairs[j];
                    if (chargePair.chargeType == chargeType && chargePair.chargeName == rowData.data.desc) {
                        exists = true;
                        break;
                    }
                }
        
                if (!exists) {
                    results.push(rowData);
                }
            }
            return results;
        }
        //Crocs related changes EOMS-3805 end
    changeInChargeNameList(event, index) {
        const keyChargeName = Constants.KEY_CHARGE_NAME;
        const keyChargeCategory = Constants.KEY_CHARGE_CATEGORY;
        const changeValue = event.item.value;
        const category = event.item.data.category;

        this.selectedChargePairs[index].chargeName = event.item.data.desc; // use desc attribute since the value of the object is desc
        // keep track of the number of remaining charge name option for a particular charge type
        this.remainChargeNamesForType[this.model.data[index][0].data.value] -= 1;

        // if no more charge name for the current options, double check if any charge type left
        if(this.remainChargeNamesForType[this.model.data[index][0].data.value] == 0){
            this.ranOutOfChargeOptions = this.getChargeTypeList().length == 0;
        }

        if (this.isLineLevel) {
            this.lineChargeDetailsData.LineCharge[index].ChargeName = changeValue;
            this.lineChargeDetailsData.LineCharge[index].ChargeCategory = category;
            this.utilityLineCharge(keyChargeName, changeValue, index);
            this.utilityLineCharge(keyChargeCategory, category, index);
            this.model.data[index][2].data.isDisable = false;
        } else {
            this.headerChargeDetailsData.HeaderCharge[index].ChargeName = changeValue;
            this.headerChargeDetailsData.HeaderCharge[index].ChargeCategory = category;
            this.utilityHeaderCharge(keyChargeName, changeValue, index,category);
            this.model.data[index][3].data.isDisable = false;
        }
    }

    ChangeInApplyToField(event, data) {
        const changeKey = Constants.KEY_CHARGE_APPLYTO;
        const changeValue = event.item.value;
        const index= data.index;

        if(!this.lineChargeDetailsData.LineCharge[index].ChargeApplyTo || this.lineChargeDetailsData.LineCharge[index].ChargeApplyTo !== changeValue.toString()){
        const chargeAmount = this.getChargeDetails(this.lineChargeDetailsData.LineCharge[index]);
        this.lineChargeDetailsData.LineCharge[index].ChargeApplyTo = changeValue;
        this.utilityLineCharge(changeKey, changeValue, index);
        this.model.data[index][3].data.isDisable = false;
        if(chargeAmount && chargeAmount > 0){
          this.isApplyToChanged = true;
          this.amountChangedSub.next({ index: data.index, value:chargeAmount, chargeAmountId: data.uniqueId });
        }
        }
    }

    utilityHeaderCharge(changeKey, changeValue, index, category?) {
        const chargeCategoryKey= Constants.KEY_CHARGE_CATEGORY;
        const mappedKey = { id: index };
        if (this.newChargeDetails.HeaderCharge.length === 0) {
            this.newChargeDetails.HeaderCharge.push({ ...this.headerChargeDetailsData.HeaderCharge[index], ...mappedKey });
        } else if (this.newChargeDetails.HeaderCharge.length > 0) {
            const newChargeIndex = this.newChargeDetails.HeaderCharge.findIndex(obj => obj.id === index);
            if (newChargeIndex === -1) {
                this.newChargeDetails.HeaderCharge.push({ ...this.headerChargeDetailsData.HeaderCharge[index], ...mappedKey });
            } else {
              if(category && category!==this.newChargeDetails.HeaderCharge[newChargeIndex][chargeCategoryKey]){
                this.newChargeDetails.HeaderCharge[newChargeIndex][chargeCategoryKey] = category;
              }

                this.newChargeDetails.HeaderCharge[newChargeIndex][changeKey] = changeValue;
            }
        }
    }

    utilityLineCharge(changeKey, changeValue, index) {
        const mappedKey = { id: index };
        if (this.newChargeDetails.LineCharge.length === 0) {
            this.newChargeDetails.LineCharge.push({ ...this.lineChargeDetailsData.LineCharge[index], ...mappedKey });
        } else if (this.newChargeDetails.LineCharge.length > 0) {
            const newChargeIndex = this.newChargeDetails.LineCharge.findIndex(obj => obj.id === index);
            if (newChargeIndex === -1) {
                this.newChargeDetails.LineCharge.push({ ...this.lineChargeDetailsData.LineCharge[index], ...mappedKey });
            } else {
                this.newChargeDetails.LineCharge[newChargeIndex][changeKey] = changeValue;
            }
        }
    }

    isChargeAmountDisabled(item) {
        let disabled = true;
        if (this.isLineLevel) {
             item.ChargeName && item.ChargeApplyTo ? disabled = false : disabled = true;
        } else {
             item.ChargeName ? disabled = false : disabled = true;
        }
        return disabled;
    }

    changeInAmountField(data, value) {
      if (!value || !isNaN(value?.toString())) {
        data.chargeAmountId = value?.toString();
        this.amountChangedSub.next({ index: data.index, value: value || !isNaN(value) ? value?.toString() : undefined,
            chargeAmountId: data.chargeAmountId });
      }
    }

    saveCharges(index, changeValue, chargeAmountId) {
      this.modifiedId = chargeAmountId;
      this.modifiedAmount = changeValue;
      this.modifiedIndex = index;
        const changeKey = Constants.KEY_CHARGE_AMOUNT;
        if (this.isLineLevel) {
            const chargeLine = this.lineChargeDetailsData.LineCharge[index];
            if (chargeLine.ChargeAmount !== changeValue || this.isApplyToChanged || this.hasOverrideError) {
                this.isApplyToChanged = false;
                this.lineChargeDetailsData.LineCharge[index].ChargeAmount = changeValue;
                this.utilityLineCharge(changeKey, changeValue, index);
                this.addLineCharges({
                    lineCharge: this.lineChargeDetailsData, index,
                    orderLineKey: this.orderLineKey, updateLine: true, newChargeDetails: this.newChargeDetails
                });
            }
        } else {
            const chargeAmountString = chargeAmountId.toString();
            let updatedIndex;
            if (chargeAmountString.includes('~')) {
                const chargeAmountArray = chargeAmountString.split('~');
                updatedIndex = (this.headerChargeDetailsData.HeaderCharge).findIndex(obj => obj.ChargeCategory === chargeAmountArray[1]);
            } else {
                updatedIndex = index;
            }
            const chargeLine = this.headerChargeDetailsData.HeaderCharge[updatedIndex];
            if (chargeLine?.ChargeAmount !== changeValue || this.hasOverrideError) {
                this.headerChargeDetailsData.HeaderCharge[updatedIndex].ChargeAmount = changeValue;
                this.utilityHeaderCharge(changeKey, changeValue, updatedIndex);
                this.addHeaderCharges({
                    headerCharge: this.headerChargeDetailsData, index: updatedIndex,
                    updateHeaderCharge: true, newChargeDetails: this.newChargeDetails
                });
            }
        }
    }

    addHeaderCharges(item) {
        this.headerCharges = item.headerCharge;
        this.newChargeDetails = item.newChargeDetails;
        if (item.updateHeaderCharge) {
            this.addCharges(item.index);
        }
    }

    addLineCharges(item) {
        this.lineCharges = item.lineCharge;
        this.orderLineKey = item.orderLineKey;
        this.newChargeDetails = item.newChargeDetails;
        if (item.updateLine) {
            this.addCharges(item.index);
        }
    }

    async addCharges(index) {
        this.hasOverrideError = false;
        let input = {
            OrderHeaderKey: this.orderHeaderKey,
        };
        let summaryChargeArray = [];
        if (this.isLineLevel) {
            if (this.newChargeDetails !== undefined) {
                summaryChargeArray = this.newChargeDetails.LineCharge.filter(charge => charge.id === index).map(lineCharge => ({
                    ChargeCategory: lineCharge.ChargeCategory,
                    ChargeName: lineCharge.ChargeName,
                    ChargePerLine: lineCharge.ChargeApplyTo ===
                        this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_LINE'] ? lineCharge.ChargeAmount : '',
                    ChargePerUnit: lineCharge.ChargeApplyTo ===
                        this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_CHARGE_PER_UNIT'] ? lineCharge.ChargeAmount : ''
                }));
                const lineCharges = {
                    OrderLines: {
                        OrderLine: [
                            {
                                OrderLineKey: this.orderLineKey,
                                LineCharges: {
                                    LineCharge: summaryChargeArray
                                }
                            }
                        ]
                    }
                };
                if(!this.isDraftOrder){
                  const pendingChanges = { PendingChanges: { RecordPendingChanges: 'Y' } };
                  input = (this.violationOutput && Object.keys(this.violationOutput).length !== 0
                    && this.violationOutput.Violation.ApproverUserID) ? Object.assign(input, lineCharges, pendingChanges, this.violationOutput)
                    : input = Object.assign(input, lineCharges, pendingChanges);
                } else {
                  input = (this.violationOutput && Object.keys(this.violationOutput).length !== 0
                    && this.violationOutput.Violation.ApproverUserID) ? Object.assign(input, lineCharges, this.violationOutput)
                    : Object.assign(input, lineCharges);
                }

            }
        } else {
            if (this.newChargeDetails.HeaderCharge !== undefined) {
                summaryChargeArray = this.newChargeDetails.HeaderCharge.filter(charge => charge.id === index).map(headerCharge => ({
                    ChargeAmount: headerCharge.ChargeAmount,
                    ChargeCategory: headerCharge.ChargeCategory,
                    ChargeName: headerCharge.ChargeName
                }));
                const headercharges = {
                    HeaderCharges: {
                        HeaderCharge: summaryChargeArray
                    }
                };
                const pendingChanges = { PendingChanges: { RecordPendingChanges: 'Y' } };
                input = (this.violationOutput && Object.keys(this.violationOutput).length !== 0
                  && this.violationOutput.Violation.ApproverUserID) ? Object.assign(input, headercharges, pendingChanges, this.violationOutput)
                  : Object.assign(input, headercharges, pendingChanges);
            }
        }
            await this.orderCommonService.addModifyCharges(input, this.isLineLevel, this.summaryDetails).then(mashupOutput => {
                this.isLineLevel ? this.getOrderLineDetails(mashupOutput.Order.OrderLines, true) :
                    this.getOrderDetails(mashupOutput.Order);
                    this.tableData();
                    this.saveChargesEnabled = true;
                    this.isAnyChangeAppliedOnModal = true;
                  }, mashupError => {
                    this.showManagerIDField = mashupError.showManagerIDField;
                    this.violationOutput = mashupError.violation;
                    this.showNotification(mashupError.errorMsg);
                    this.hasOverrideError = true;
                    this.managerID = mashupError.showManagerIDField ? this.managerID : '';
                  });
    }

    groupByUtil(objectArray, property) {
        return objectArray?.sort((a, b) => {
            const x = a[property];
            const y = b[property];
            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        });
    }

    onSaveClick() {
        if (this.note) {
            this.addNote();
        } else {
            this.saveAction();
        }
    }
    async savePendingChanges(){
        const input= {
            Order: {
                OrderHeaderKey: this.orderHeaderKey
            }
        }
        await this.orderCommonService.savePendingChanges(input);
     }

    async discardPendingChanges(){
       const  input= {
         Order: {
          OrderHeaderKey: this.orderHeaderKey
         }
       }
              await this.orderCommonService.resetPendingChanges(input) ;
     }

     async saveAction() {
        await this.savePendingChanges();
        this.modalData.successCallback();
        this.closeModal();
        const msg = this.nlsMap['ORDER_PRICING_SUMMARY.ADJUST_PRICING_MODAL.LABEL_ADJUSTMENT_SAVED_MSG'];
        this.notificationService.notify({
            type: 'success',
            title: msg    });
    }

    private async _getNls(key, params?): Promise<any> {
        return this.translate.get(key, params).toPromise();
    }

    async addNote() {
        let noteDetail;
        const loginUserId = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLoginId();
        if (this.isLineLevel) {
            noteDetail = {
                Order: {
                    OrderHeaderKey: this.orderHeaderKey,
                    OrderLines: {
                        OrderLine: [
                            {
                                OrderLineKey: this.orderLineKey,
                                Notes: {
                                    Note: {
                                        NoteText: this.note,
                                        Createuserid: loginUserId,
                                        Modifyuserid: loginUserId
                                    }
                                }
                            }
                        ]
                    }
                }
            };
        } else {
            noteDetail = {
                Order: {
                    OrderHeaderKey: this.orderHeaderKey,
                    Notes: {
                        Note: {
                            NoteText: this.note,
                            Createuserid: loginUserId,
                            Modifyuserid: loginUserId
                        }
                    }
                }
            };
        }

            this.isLineLevel ?
                await this.orderCommonService.addOrderlineNote(noteDetail) : await this.orderCommonService.addNote(noteDetail);
            this.saveAction();
    }

    onNoteChange() {
        if (!this.isAnyChangeAppliedOnModal) {
            this.note ? this.saveChargesEnabled = true : this.saveChargesEnabled = false;
        }
    }

    onManagerIDChange() {
      setTimeout(() => {
        if (this.managerID) {
          this.violationOutput.Violation.ApproverUserID = this.managerID;
          this.amountChangedSub.next({ index: this.modifiedIndex, value: this.modifiedAmount, chargeAmountId: this.modifiedId });
          }
        }, 1000);
    }

    async onCloseModalClick() {
      if(this.isAnyChangeAppliedOnModal && !this.isDraftOrder){
        await this.discardPendingChanges();
      }
        this.closeModal();
    }

    ngOnDestroy() {
        this.amountSubscription.unsubscribe();
    }

    setFlags(flagData) {
        this.saveChargesEnabled = flagData.saveChargesEnabled;
        this.isAnyChangeAppliedOnModal = flagData.isAnyChangeAppliedOnModal;
        this._repopulateCharges(this.model.data, flagData.numPromotionApplied, flagData.removePromotionOperation);
    }

    onPromoError(errorMsg){
      this.showNotification(errorMsg);
    }

    showNotification(mashupError) {
      this.notificationObj = {
        type: 'error',
        title: mashupError,
        showClose: true,
        lowContrast: true
      };
      this.notificationShown = true;
    }

    closeNotification() {
      this.notificationShown = false;
    }

    async onDisplayChange(data, $event) {
      if ($event) {
        data.displayValue = $event;
      }
    }
}
