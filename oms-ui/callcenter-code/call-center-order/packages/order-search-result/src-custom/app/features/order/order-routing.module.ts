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

import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { OrderSearchResultComponent } from './order-search-result/order-search-result.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'home',
    component: OrderSearchResultComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class OrderRoutingModule { }
