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

import { Component, OnInit, Input, ViewChildren, QueryList, TemplateRef, AfterViewInit } from '@angular/core';
import { BaseFieldDetailsComponent, BucFieldHelperService as BucFieldAttributeHelperService, TemplateIdDirective, getArray } from '@buc/common-components';
import {  Return, cloneFieldDetailAttributes } from '@call-center/return-shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalService } from 'carbon-components-angular';
import { Observable, of } from 'rxjs';
import { cloneDeep, get } from 'lodash';
import { BucBaseUtil, BucCommOmsMashupService } from '@buc/svc-angular';
import { TrackingNumberModalComponent } from '../tracking-number-modal/tracking-number-modal.component';

@Component({
  selector: 'call-center-return-fulfillment-summary',
  templateUrl: './return-fulfillment-summary.component.html',
  styleUrls: ['./return-fulfillment-summary.component.scss'],
})
export class ReturnFulfillmentSummaryComponent extends BaseFieldDetailsComponent implements AfterViewInit {

  public readonly componentId = 'return-fulfillment-summary';
  public readonly FIELDS = {
    shipping: {
      returnFrom: {
        binding: 'fulfillmentGroup.Shipment.FromAddress',
        templateId: 'addressDisplay',
      },
      returnTo: {
        binding: 'fulfillmentGroup.Shipment.ToAddress',
        templateId: 'addressDisplay',
      },
      refundTo: {
        binding: 'returnEntity.personInfoBillTo',
        templateId: 'addressDisplay',
      },
      tracking: {
        templateId: 'tracking',
        binding: 'fulfillmentGroup.Shipment.Containers.Container',
      },
    },
  };

  @Input() fulfillmentGroup: any;
  @Input() type: string;
  @Input() returnEntity: Return;
  // show/hide actions panel on the right
  public showActionPanel = false;
  d: Return;
  // the attribute list to show next to the progress panel
  public fulfillmentAttributes: Array<any> = [];

  public templates: Record<string, TemplateRef<any>> = {};
  @ViewChildren(TemplateIdDirective) private set _templates(ql: QueryList<TemplateIdDirective>) {
    if (ql) {
      ql.forEach(i => this.templates[i.id] = i.template)
    }
  }

  constructor(
    public translate: TranslateService,
    public modalService: ModalService,
    bucFieldHelperService: BucFieldAttributeHelperService,
    private bucCommOmsMashupService: BucCommOmsMashupService,

  ) {
    super(bucFieldHelperService, modalService);
  }

  trackingNo: any;
   trackingUrl: any;
   documentType:any;
   entryType:any;
  ngAfterViewInit(): void {
    this.initializeSummary()
    //EOMS-734 For a return order created by customer service via the OMS call center, OMS needs to connect with Narvar via an API to generate a return label.
    this.fetchFulfillmentSummary()
      .then(result => {
        this.entryType=result.Order.EntryType
        
         if(this.entryType=='WEB' || this.entryType=='Call Center'){
          this.trackingNo = result.Order.ShippingGroups.ShippingGroup[0].OrderLines.OrderLine[0].CustomAttributes.Text1
        this.trackingUrl = result.Order.ShippingGroups.ShippingGroup[0].OrderLines.OrderLine[0].CustomAttributes.Text2
        this.documentType = result.Order.DocumentType
        }
//EOMS-734 -End
      

        console.log('Fulfillment Summary Result:', result); // ✅ Output here
          
      })


  }

  initializeSummary() {

    this.initializeFieldDetailAttributes(`${this.type}-fulfillment-summary`)
      .then(() => this.loadPageAttributes().toPromise())
      .then(
        () =>
        (this.fulfillmentAttributes = cloneFieldDetailAttributes(
          this.getFieldDetailsConfiguration().getAttributeList()
        ))

      );

  }
  //EOMS-734 Start
  fetchFulfillmentSummary() {
    const a = { "Order":
       {  "OrderHeaderKey": this.returnEntity.id } }
    return this.bucCommOmsMashupService.callMashup("icc.return.return-details.getReturnFulfillmentSummaryDetails", a, {}).toPromise()
      .then(mashupOutput => this.bucCommOmsMashupService.getMashupOutput(mashupOutput, "icc.return.return-details.getReturnFulfillmentSummaryDetails"));
  }
  //EOMS-734 END
  openTrackingUrlInNewWindow(data) {
    const containers = getArray(data);
    const url = containers.length === 1 ? data[0].TrackingURL : '';
    if (!BucBaseUtil.isVoid(url)) {
      window.open(url, '_blank');
    } else if (containers.length > 1) {
      this.modalService.destroy();
      this.modalService.create({
        component: TrackingNumberModalComponent,
        inputs: {
          shipment: cloneDeep(this.fulfillmentGroup.Shipment),
        },
      });
    }
  }

  protected fetchPageAttributeData(): Observable<Array<any>> {
    let shipment = get(this.fulfillmentGroup, 'Shipment');
   
    const shipNode = this.fulfillmentGroup.ShipNode;
    if (BucBaseUtil.isVoid(shipment)) {
      return of(
        getArray({
          returnEntity: this.returnEntity,
          fulfillmentGroup: {
            Shipment: {
              FromAddress: this.returnEntity.personInfoShipTo,
              ToAddress: !BucBaseUtil.isVoid(shipNode)
                ? {
                  FirstName: shipNode.ShipNode,
                  ...shipNode.ShipNodePersonInfo,
                }
                : { FirstName: '', PersonInfoKey: '' },
            },
          },
        })
      );
    }
    shipment = cloneDeep(shipment);
    shipment.ToAddress = {
      ...shipment.ToAddress,
      FirstName: shipNode.Description || '',
      LastName: '',
    };
    return of(
      getArray({
        returnEntity: this.returnEntity,
        fulfillmentGroup: { ...this.fulfillmentGroup, Shipment: shipment },
      })
    );
  }

  protected getDataForAttribute(id: string, data: any) {
    const toReturn: any = { data: '' };
    const field = get(this.FIELDS, [this.type, id]);
    if (!BucBaseUtil.isVoid(field)) {
      toReturn.data = get(data, field.binding);
      toReturn.valueTemplate = this.templates[field.templateId];
    }
    return toReturn;
  }

  protected async onFieldDetailsAttributeLoadComplete() { }

  protected async onApplyFieldConfigurationComplete() { }

}