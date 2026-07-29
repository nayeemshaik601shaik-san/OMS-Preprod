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
  Constants,
  Entity,
  MashupMapperResponseChain,
  PaginatedMashupResponseMapper,
  OrderMapper,
} from '@call-center/order-shared';
import { groupBy } from 'lodash';

type mapResponseEntitiesType = {
  entities: Entity[];
  additionalEntities: Entity[];
};

export class PaginatedOrderMapper extends PaginatedMashupResponseMapper {

  constructor(private orderMapper: OrderMapper) {
    super();
  }

  getOutputListType(): string {
    return 'OrderList';
  }

  getEntityPath(): string {
    return 'Output.OrderList.Order';
  }

  mapResponseEntities(responseEntities: any[], mapperChain?: MashupMapperResponseChain): mapResponseEntitiesType {
    const mappedEntities: Entity[] = [];
    responseEntities.forEach((re) => this.orderMapper.mapOrder(re, mappedEntities));
    const toReturn = groupBy(mappedEntities, (e) => {
      return e.entity_type === Constants.ENTITY_TYPE_ORDER ? 'entities' : 'additionalEntities'
    });

    return toReturn as mapResponseEntitiesType;
  }
}
