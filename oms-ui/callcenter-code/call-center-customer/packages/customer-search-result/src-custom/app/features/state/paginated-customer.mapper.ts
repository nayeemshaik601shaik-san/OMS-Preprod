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
  Constants,
  Entity,
  MashupMapperResponseChain,
  PaginatedMashupResponseMapper,
  CustomerMapper,
} from '@call-center/customer-shared';
import { groupBy } from 'lodash';

type mapResponseEntitiesType = {
  entities: Entity[];
  additionalEntities: Entity[];
};

export class PaginatedCustomerMapper extends PaginatedMashupResponseMapper {

  constructor(private returnOrderMapper: CustomerMapper) {
    super();
  }

  getOutputListType(): string {
    return 'CustomerList';
  }

  getEntityPath(): string {
    return 'Output.CustomerList.Customer';
  }

  mapResponseEntities(responseEntities: any[], mapperChain?: MashupMapperResponseChain): mapResponseEntitiesType {
    const mappedEntities: Entity[] = [];
    responseEntities.forEach((re) => this.returnOrderMapper.mapCustomer(re, mappedEntities));

    const toReturn = groupBy(mappedEntities, (e) => {
      return e.entity_type === Constants.ENTITY_TYPE_CUSTOMER ? 'entities' : 'additionalEntities'
    });

    return toReturn as mapResponseEntitiesType;
  }
}
