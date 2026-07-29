/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-D18, 5725-D10
 *
 * (C) Copyright International Business Machines Corp. 2019, 2025
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonService } from '@buc/common-components';
import { getArray } from '@buc/common-components';
import { BucAddressDisplayHelperService } from '../address-display/address-display-helper.service';
import { Constants } from '@buc/common-components';
import { BucSvcAngularStaticAppInfoFacadeUtil } from '@buc/svc-angular';
import { get } from 'lodash';
import { TranslateService } from '@ngx-translate/core';
import { isEmailValid } from '../common/functions';

@Component({
  selector: 'buc-address-edit',
  templateUrl: './address-edit.component.html',
  styleUrls: ['./address-edit.component.scss'],
})
export class AddressEditComponent implements OnChanges {
  @Input() addressFields: any;
  @Input() EnterpriseCode: any;
  @Input() personInfo;
  @Input() showContactInfo = false
  @Input() hideAddressFields = false;
  @Output() addressFieldValidation = new EventEmitter();
  @Output() enableSaveButton = new EventEmitter();
  @Output() getFocusKey = new EventEmitter();

  addressJson: any;
  countries: any[] = [];
  mandatoryFields: any = [];
  numberFields: any = [];
  stateList: any[] = [];
  contactInfo: any = [];
  regionSchemaKey: string;
  titles: any = [];
  numberPattern: any = /^[0-9]*$/;
  componentId = 'address-edit-component';
  emailAddressField: any = {};

  constructor(
    private helperService: BucAddressDisplayHelperService,
    private commonService: CommonService,
    private translate: TranslateService
  ) { }


  async ngOnChanges(simpleChang: SimpleChanges) {
    if (simpleChang.personInfo && simpleChang.personInfo.currentValue !== simpleChang.personInfo.previousValue) {
      await this.getAddressFieldDetails();
    }
  }

  setDropdownSelectedField(field, id) {
    if(field === 'EMailID') {
      this.validateEmail(false);
    }
    this.onFieldChange(field, id);
  }

  onFieldChange(field, id) {
    if ((this.personInfo[field] && this.personInfo[field] != id) || !this.personInfo[field]) {
      this.emitEnableButton({});
    }
    this.addressFields.forEach(obj => {
      if (obj.key === field) {
        obj.value = id;
      }
      if (this.numberFields.includes(obj.key)) {
        this.emitEnableButton(obj);
      }
    });
    this.personInfo[field] = id;
    this.getFocusKey.emit(field); // kick in validation once value changes
  }

  validateEmail(showError: boolean) {
    if (this.emailAddressField) {
      if (isEmailValid(this.emailAddressField.value)) {
        this.emailAddressField.errorMessage = '';
      } else {
        this.emailAddressField.errorMessage = showError ? this.translate.instant('EDIT_ADDRESS.ERROR_MESSAGE.LABEL_INVALID_FORMAT') : this.emailAddressField.errorMessage;
      }
    }
  }



  emitEnableButton(obj) {
    setTimeout(() => {
      this.enableSaveButton.emit(obj);
    }, 0);
  }

  async getAddressFieldDetails() {
    const locale = BucSvcAngularStaticAppInfoFacadeUtil.getOmsUserLocale().split("_")[1] || '';

    this.addressFields = [];
    this.addressJson = await this.helperService.getAddressJson(this.personInfo);
    const addressFieldsObj = { ...this.addressJson };
    this.mandatoryFields = addressFieldsObj.mandatoryFields.split(',');
    this.numberFields = addressFieldsObj.numberFields.split(',');
    this.contactInfo = addressFieldsObj.contactInfo;
    let fieldItems = [];
    Object.keys(addressFieldsObj.displayOrder).forEach(key => {
      const currentFieldItems = addressFieldsObj.displayOrder[key].
        replace(/[^a-z\d\s]+/gi, "")
        .split(' ');
      fieldItems = [...fieldItems, ...currentFieldItems];
    })
    fieldItems = this.showContactInfo ? fieldItems.concat(this.contactInfo) : fieldItems;
    fieldItems = Array.from(new Set(fieldItems));
    fieldItems.forEach(field => {
      this.addressFields.push({
        key: field,
        value: this.personInfo[field] ? this.personInfo[field] : field === Constants.FIELD_COUNTRY ? locale : '',
        isCountry: field === Constants.FIELD_COUNTRY,
        isState: field === Constants.FIELD_STATE,
        isTitle: field === Constants.FIELD_TITLE,
        isName: field === Constants.FIELD_NAME,
        isSurName: field === Constants.FIELD_SUR_NAME,
        isZipCode: field === Constants.FIELD_ZIPCODE,
        isContactInfo: this.showContactInfo && this.contactInfo.includes(field),
        errorMessage: '',
        maxLength: field === Constants.FIELD_ZIPCODE ? '35' : '280'
      });
    });

    if (fieldItems.some(field => field === Constants.FIELD_COUNTRY)) {
      // Check if PersonInfo has a false value for Country before setting to avoid overwriting
      if (!this.personInfo.Country && locale) {
        this.personInfo.Country = locale;
      }
    }

    this.emailAddressField = this.addressFields.find(obj => obj.key === 'EMailID');

    this.addressFieldValidation.emit({
      addressFields: this.addressFields,
      personInfo: this.personInfo,
      mandatoryFields: this.mandatoryFields,
      numberFields: this.numberFields
    });
    this.getCountryTitleList();
    this.getStateList(this.personInfo.Country);
  }

  // EOMS-2553 : Shipping Address : Call Center UI : Country Drop Down
  async getCountryTitleList() {
    try {
      await this.commonService
        .getCountryTitleCodeList(this.EnterpriseCode)
        .then((mashupOutput) => {
          const countryList = mashupOutput.countryList.CommonCodeList;
          const titleList = mashupOutput.titleList.CommonCodeList;

          // Filter countries based on the EnterpriseCode
          let filteredCountries = [];
          if (this.EnterpriseCode === 'CROCS_US') {
            filteredCountries = countryList.CommonCode.filter(
              (country) => country.CodeValue === 'US'
            );
          } else if (this.EnterpriseCode === 'CROCS_CA') {
            filteredCountries = countryList.CommonCode.filter(
              (country) => country.CodeValue === 'CA'
            );
          }

          if (filteredCountries.length > 0) {
            this.countries = filteredCountries.map(
              ({ CodeLongDescription: content, CodeValue: id }) => ({
                content,id,selected: this.personInfo.Country === id ? true : false,
              })
            );
          } else {
            this.countries = getArray(countryList.CommonCode)
            .map(({ CodeLongDescription: content, CodeValue: id }) =>
              ({ content, id, selected: this.personInfo.Country === id ? true : false })
            );
          }

          // Titles mapping remains the same
          if (titleList.CommonCode) {
            this.titles = getArray(titleList.CommonCode).map(
              ({ CodeShortDescription: content, CodeValue: id }) => ({
                content,id,selected: this.personInfo.Title === id ? true : false,
              })
            );
          }
        });
    } catch (err) {
      console.error(err);
    }
  }

  async getStateList(country) {
    if (country && this.addressFields.find(field => field.key === Constants.FIELD_STATE)) {
      try {
        this.setDropdownSelectedField('Country', country);
        const regionList = await this.commonService.getStateRegionList(country, this.EnterpriseCode);
        this.regionSchemaKey = regionList.Regions?.RegionSchema?.RegionSchemaKey;
        this.stateList = getArray(get(regionList, 'Regions.RegionSchema.Region'))
          .map(({ RegionName: content, RegionName: id }) =>
            ({ content, id, selected: this.personInfo.State === id ? true : false })
          );
      } catch (err) {
        console.error(err);
      }
    } else {
      this.stateList = [];
    }
  }

  onComboBoxSearch(evt, key) {
    if (evt) {
      let searched;
      if(key === 'Country') {
        searched = this.countries.find((item) => item.content.toLowerCase() === evt.toLowerCase());
      } else if(key === 'State') {
        searched = this.stateList.find((item) => item.content.toLowerCase() === evt.toLowerCase());
      }
      if (searched) {
        this.setDropdownSelectedField(key, searched.id);
      } else {
        this.setDropdownSelectedField(key, '');
      }
    } else {
      this.setDropdownSelectedField(key, '');
      if(key === 'Country') {
        this.countries.forEach(el => el.selected = false);
      } else if(key === 'State') {
        this.stateList.forEach(el => el.selected = false);
      }
    }
  }

  getKey(input) {
    if(input === 'EMailID') {
      this.validateEmail(true);
    }
    this.getFocusKey.emit(input);
  }

  async getCityStateForZip(zipcode, key) {
    this.getKey(key);
    if (this.personInfo.Country && this.regionSchemaKey) {
      try {
        await this.commonService.getCityStateForZip(zipcode, this.personInfo.Country, this.regionSchemaKey).then(mashupOutput => {
          const regions = mashupOutput.Regions?.RegionSchema?.Region;
          if (regions && regions.length) {
            regions.forEach(obj => {
              this.addressFields.forEach(obj2 => {
                if (obj.RegionLevelName == Constants.FIELD_CITY && obj2.key === Constants.FIELD_CITY) {
                  obj2.value = obj.RegionName;
                }
                if (obj.RegionLevelName == Constants.FIELD_STATE && obj2.key === Constants.FIELD_STATE) {
                  this.stateList = this.stateList.map(item => ({ ...item, selected: item.content === obj.RegionName }));
                  this.setDropdownSelectedField('State', obj.RegionName);
                }
              });
            });
          }
        });

      } catch (err) {
        console.error(err);
      }
    }
  }
}
