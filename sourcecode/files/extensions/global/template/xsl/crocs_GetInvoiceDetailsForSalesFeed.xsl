<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	<xsl:template match="/">
		<InvoiceDetail>
			<InvoiceHeader>
				<xsl:copy-of select="//InvoiceHeader/@*"/>
				<xsl:variable name="vInvoiceKey">
					<xsl:value-of select="//InvoiceHeader/@OrderInvoiceKey"></xsl:value-of>
				</xsl:variable>
				<xsl:variable name="vEnterpriseCode" select="//InvoiceHeader/@EnterpriseCode" />
				<xsl:variable name="vRoundingRate">
					<xsl:choose>
						<xsl:when test="(//InvoiceDetail/InvoiceHeader/Order/Extn/@ExtnRoundingRate !='0.00') and (//InvoiceDetail/InvoiceHeader/Order/Extn/@ExtnRoundingRate &gt; 0)">
							<xsl:value-of select="//InvoiceDetail/InvoiceHeader/Order/Extn/@ExtnRoundingRate"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="'1'"></xsl:value-of>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				
				<xsl:variable name="vNodePostion">
					<xsl:for-each select="//OrderInvoiceList/OrderInvoice">
						<xsl:if test="@OrderInvoiceKey=$vInvoiceKey">
							<xsl:value-of select="position()"></xsl:value-of>
						</xsl:if>
					</xsl:for-each>
				</xsl:variable>
				<xsl:attribute name="NodePostion">
					<xsl:value-of select="$vNodePostion"/>
				</xsl:attribute>
				<xsl:attribute name="SAPInvoiceType">
					<xsl:choose>
						<xsl:when test="(//InvoiceHeader/@InvoiceType='SHIPMENT' and //InvoiceHeader/LineDetails/LineDetail/OrderLine[@ReshipParentLineKey!=''])">
							<xsl:value-of select="'FreeOfCharge'"></xsl:value-of>
						</xsl:when>
						<xsl:when test="//InvoiceHeader/Order/@EntryType='Call Center' and //InvoiceHeader/@DocumentType='0001' and not(//InvoiceHeader/Order/@OrderPurpose='REFUND') and not(//InvoiceHeader/Order/@OrderPurpose='EXCHANGE')">
             				 		<xsl:value-of select="'PromotionalOrder'"/>
           				 	</xsl:when>
						<!--  <xsl:when test="/InvoiceDetail/InvoiceHeader/Order/@OrderType='EXCHANGE' ">
							<xsl:value-of select="FreeOfCharge"></xsl:value-of>
						</xsl:when>
						<xsl:when test="/InvoiceDetail/InvoiceHeader/@InvoiceType='RETURN' and not(/InvoiceDetail/InvoiceHeader/Order/ExchangeOrders/ExchangeOrder[@OrderNo])">
							<xsl:value-of select="FreeOfCharge"></xsl:value-of>
						</xsl:when>-->
						<xsl:otherwise>
							<xsl:value-of select="''"></xsl:value-of>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<xsl:if test="//InvoiceHeader/LineDetails/LineDetail/OrderLine[@ReshipParentLineKey!='']">
					<xsl:attribute name="InvoiceCreationReason">
						<xsl:value-of select="'RESHIPMENT'"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="//InvoiceHeader/@InvoiceType='RETURN' and //InvoiceHeader/Order/@EntryType='STORE'">
					<xsl:attribute name="InvoiceType">
						<xsl:value-of select="concat(//InvoiceHeader/@InvoiceType,'_',//InvoiceHeader/Order/@EntryType)"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="//InvoiceHeader/@InvoiceType='RETURN' and //InvoiceHeader/Order/@OrderType='MP'">
					<xsl:attribute name="InvoiceType">
						<xsl:value-of select="concat(//InvoiceHeader/@InvoiceType,'_','RECEIPT')"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="//InvoiceHeader/@InvoiceType='RETURN' and //InvoiceHeader/Order/@OrderType='MP'">
					<xsl:attribute name="InvoiceNo">
						<xsl:value-of select="//InvoiceHeader/@OrderNo"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:attribute name="OrderNo">
					<xsl:value-of select="//InvoiceHeader/@OrderNo"/>
					<!-- <xsl:choose>
						<xsl:when test="$vNodePostion='1' or $vNodePostion=''">
							<xsl:value-of select="//InvoiceHeader/@OrderNo"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(//InvoiceHeader/@OrderNo,'_',$vNodePostion)"/>
						</xsl:otherwise>
					</xsl:choose> -->
				</xsl:attribute>
				<xsl:attribute name="Currency">
					<xsl:choose>
						<xsl:when test="$vEnterpriseCode='HEYDUDE_AU'">
							<xsl:value-of select="'USD'"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="//InvoiceHeader/@Currency"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<xsl:attribute name="SAPCarrierCode">
					<xsl:choose>
						<xsl:when test="//InvoiceHeader/@InvoiceType='SHIPMENT'">
							<xsl:value-of select="substring-before(//InvoiceHeader/@CarrierServiceCode,'-')"/>
						</xsl:when>
					</xsl:choose>
				</xsl:attribute>
				<xsl:attribute name="SAPShippingCondition">
					<xsl:choose>
						<xsl:when test="//InvoiceHeader/@InvoiceType='SHIPMENT'">
							<xsl:value-of select="substring-after(//InvoiceHeader/@CarrierServiceCode,'-')"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="'01'"/>
					</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<Order>
					<xsl:copy-of select="/InvoiceDetail/InvoiceHeader/Order/@*"/>
					<xsl:if test="//InvoiceHeader/Order/@OrderType=''">
						<xsl:attribute name="OrderType">
							<xsl:value-of select="'03'"/>
						</xsl:attribute>
					</xsl:if>
					<DerivedFromOrder>
						<xsl:attribute name="OrderHeaderKey">
							<xsl:value-of select = "/InvoiceDetail/InvoiceHeader/@DerivedFromOrderHeaderKey"/>
						</xsl:attribute>
					</DerivedFromOrder>
					<xsl:if test="//InvoiceHeader/@DocumentType='0001'">
						<ReturnOrdersForExchange>
							<xsl:copy-of select="//InvoiceDetail/InvoiceHeader/Order/ReturnOrdersForExchange/@*"/>
							<ReturnOrderForExchange>
								<OrderLines>
									<xsl:for-each select="/InvoiceDetail/InvoiceHeader/Order/ReturnOrdersForExchange/ReturnOrderForExchange/OrderLines/OrderLine">
									<OrderLine>
										<xsl:copy-of select="./@*"/>
										<DerivedFromOrder>
											<xsl:copy-of select="./InvoiceHeader/Order/ReturnOrdersForExchange/ReturnOrderForExchange/OrderLines/OrderLine/DerivedFromOrder/@*"/>
											<xsl:attribute name="OrderNo">
												<xsl:choose>
													<xsl:when test="(//InvoiceHeader/Order[@OrderPurpose!='']) and(//InvoiceHeader/Order/@OrderPurpose='EXCHANGE')">
														<xsl:value-of select="concat(//InvoiceHeader/Order/ReturnOrdersForExchange/ReturnOrderForExchange/OrderLines/OrderLine/DerivedFromOrder/@OrderNo,'E')"/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="./DerivedFromOrder/@OrderNo"/>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:attribute>
										</DerivedFromOrder>
									</OrderLine>
									</xsl:for-each>
								</OrderLines>
							</ReturnOrderForExchange>
						</ReturnOrdersForExchange>
					</xsl:if>

					<xsl:copy-of select="/InvoiceDetail/InvoiceHeader/Order/*[not(self::DerivedFromOrder)][not(self::ReturnOrdersForExchange)]"/>
				</Order>
				<xsl:choose>
					<xsl:when test="//InvoiceHeader/@InvoiceType='CREDIT_MEMO' or //InvoiceHeader/@InvoiceType='DEBIT_MEMO'">
						<xsl:copy-of select="/InvoiceDetail/InvoiceHeader/*[not(self:: LineDetails)][not(self:: Order)][not(self:: HeaderCharges)]"/>
						<LineDetails TotalLines="1">
							<LineDetail ItemID="T0000001" PrimeLineNo="1" Quantity="1.00" Tax="0.00" UnitOfMeasure="EACH" >
								<xsl:variable name="vTotalAmount">
									<xsl:choose>
										<xsl:when test="($vEnterpriseCode = 'HEYDUDE_CA' or $vEnterpriseCode = 'HEYDUDE_AU')">
											<xsl:value-of select = "format-number(//InvoiceDetail/InvoiceHeader/@TotalAmount * $vRoundingRate, '#.##')"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select = "/InvoiceDetail/InvoiceHeader/@TotalAmount"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:variable>
								<xsl:attribute name="LineTotal">
									<xsl:value-of select = "$vTotalAmount"/>
								</xsl:attribute>
								<xsl:attribute name="ExtendedPrice">
									<xsl:value-of select = "$vTotalAmount"/>
								</xsl:attribute>
								<xsl:attribute name="UnitPrice">
									<xsl:value-of select = "$vTotalAmount"/>
								</xsl:attribute>
								<OrderLine DeliveryMethod="SHP" OrderedQty="1.00" PrimeLineNo="1" Status="Shipped">
									<Item ItemID="T0000001"/>
									<LinePriceInfo DiscountPercentage="0.00" DiscountReference="" DiscountType="" InvoicedPricingQty="1.00" OrderedPricingQty="1.00">
										<xsl:attribute name="UnitPrice">
											<xsl:value-of select = "$vTotalAmount"/>
										</xsl:attribute>
										<xsl:attribute name="ListPrice">
											<xsl:value-of select = "$vTotalAmount"/>
										</xsl:attribute>
									</LinePriceInfo>
									<LineOverallTotals/>
								</OrderLine>
								<LineCharges/>
							</LineDetail>
						</LineDetails>
					</xsl:when>
					<xsl:otherwise>
						<xsl:copy-of select="/InvoiceDetail/InvoiceHeader/*[not(self:: LineDetails)][not(self:: Order)][not(self:: HeaderCharges)]"/>
						<HeaderCharges>
							<xsl:for-each select="/InvoiceDetail/InvoiceHeader/HeaderCharges/HeaderCharge">
								<HeaderCharge>
									<xsl:copy-of select="./@*"/>
									<xsl:variable name="vChargeAmount">
										<xsl:choose>
											<xsl:when test="($vEnterpriseCode = 'HEYDUDE_CA' or $vEnterpriseCode = 'HEYDUDE_AU')">
												<xsl:value-of select = "format-number(./@ChargeAmount * $vRoundingRate, '#.##')"/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:value-of select = "./@ChargeAmount"/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:variable>
									<xsl:attribute name="ChargeAmount">
										<xsl:value-of select = "$vChargeAmount"/>
									</xsl:attribute>
									<xsl:copy-of select="./*"/>
								</HeaderCharge>
							</xsl:for-each>
						</HeaderCharges>
						<LineDetails>
							<xsl:copy-of select="/InvoiceDetail/InvoiceHeader/LineDetails/@*"/>	
							<xsl:for-each select="/InvoiceDetail/InvoiceHeader/LineDetails/LineDetail">
								<LineDetail>
									<xsl:copy-of select="./@*"/>
									<xsl:variable name="vExtendedPrice">
										<xsl:choose>
											<xsl:when test="($vEnterpriseCode = 'HEYDUDE_CA' or $vEnterpriseCode = 'HEYDUDE_AU')">
												<xsl:value-of select = "format-number(./@ExtendedPrice * $vRoundingRate, '#.##')"/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:value-of select = "./@ExtendedPrice"/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:variable>
									<xsl:attribute name="ExtendedPrice">
										<xsl:value-of select = "$vExtendedPrice"/>
									</xsl:attribute>
									<xsl:choose>
										<xsl:when test="contains(//InvoiceHeader/@EnterpriseCode, '_US') or contains(//InvoiceHeader/@EnterpriseCode, '_AU')">
											<xsl:attribute name="Tax">
												<xsl:choose>
													<xsl:when test="./LineTaxes/LineTax[@TaxName='SalesTax']/@TaxPercentage &gt; 0">
														<xsl:value-of select="format-number((@ExtendedPrice * ./LineTaxes/LineTax[@TaxName='SalesTax']/@TaxPercentage),'#.##')"/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="'0.00'"></xsl:value-of>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:attribute>
										</xsl:when>
										<xsl:when test="contains(//InvoiceHeader/@EnterpriseCode, '_CA')">
											<xsl:attribute name="CAGstTax">
												<xsl:choose>
													<xsl:when test="./LineTaxes/LineTax[@TaxName='CAGstTax']/@TaxPercentage &gt; 0">
														<xsl:value-of select="format-number((@ExtendedPrice * ./LineTaxes/LineTax[@TaxName='CAGstTax']/@TaxPercentage),'#.##')"/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="'0.00'"></xsl:value-of>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:attribute>
											<xsl:attribute name="CAPstTax">
												<xsl:choose>
													<xsl:when test="./LineTaxes/LineTax[@TaxName='CAPstTax']/@TaxPercentage &gt; 0">
														<xsl:value-of select="format-number((@ExtendedPrice * ./LineTaxes/LineTax[@TaxName='CAPstTax']/@TaxPercentage),'#.##')"/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="'0.00'"></xsl:value-of>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:attribute>
											<xsl:attribute name="CAQstTax">
												<xsl:choose>
													<xsl:when test="./LineTaxes/LineTax[@TaxName='CAQstTax']/@TaxPercentage &gt; 0">
														<xsl:value-of select="format-number((@ExtendedPrice * ./LineTaxes/LineTax[@TaxName='CAQstTax']/@TaxPercentage),'#.##')"/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="'0.00'"></xsl:value-of>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:attribute>
											<xsl:attribute name="CAHstTax">
												<xsl:choose>
													<xsl:when test="./LineTaxes/LineTax[@TaxName='CAHstTax']/@TaxPercentage &gt; 0">
														<xsl:value-of select="format-number((@ExtendedPrice * ./LineTaxes/LineTax[@TaxName='CAHstTax']/@TaxPercentage),'#.##')"/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="'0.00'"></xsl:value-of>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:attribute>
										</xsl:when>
									</xsl:choose>
									
									<xsl:copy-of select="./*[not(self:: OrderLine) and not(self:: LineCharges)]"/>
									<OrderLine>
										<xsl:copy-of select="./OrderLine/@*"/>									
										<Item>
											<xsl:copy-of select="./OrderLine/Item/@*"/>	
										</Item>	
										<ItemDetails>
											<PrimaryInformation>
												<xsl:copy-of select="./OrderLine/ItemDetails/PrimaryInformation/@*[name()!='SizeCode']"/>
												<xsl:variable name="vItemId">
													<xsl:choose>
														<xsl:when test="contains(./OrderLine/Item/@ItemID,'-')">
															<xsl:value-of select="substring-after(./OrderLine/Item/@ItemID,'-')"></xsl:value-of>
														</xsl:when>
													</xsl:choose>
												</xsl:variable>	
												<xsl:attribute name="SizeCode">
													<xsl:choose>
														<xsl:when test="contains($vItemId,'-')">
															<xsl:value-of select="substring-after($vItemId,'-')"></xsl:value-of>
														</xsl:when>
														<xsl:otherwise>
															<xsl:value-of select="''"></xsl:value-of>
														</xsl:otherwise>
													</xsl:choose>
												</xsl:attribute>
											</PrimaryInformation>
										</ItemDetails>
										<xsl:if test="//InvoiceHeader/@DocumentType='0003'">
											<DerivedFromOrderLine>
												<xsl:copy-of select="./OrderLine/DerivedFromOrderLine/@*"/>
												<Order>
													<xsl:attribute name="OrderNo">
														<xsl:choose>
															<xsl:when test="//InvoiceHeader/Order/@EntryType='STORE'">
																<xsl:value-of select="./OrderLine/DerivedFromOrderLine/Order/@OrderNo"/>
															</xsl:when>
															<xsl:when test="./OrderLine/@ConditionVariable1='exchange'">
																<xsl:value-of select="concat(./OrderLine/DerivedFromOrderLine/Order/@OrderNo,'E')"/>
															</xsl:when>
															<xsl:otherwise>
																<xsl:value-of select="concat(./OrderLine/DerivedFromOrderLine/Order/@OrderNo,'R')"/>
															</xsl:otherwise>
														</xsl:choose>
													</xsl:attribute>
												<!-- EOMS-8778 Return Order for Exchange -->
												<ReturnOrdersForExchange>
													<xsl:copy-of select="./OrderLine/DerivedFromOrderLine/Order/ReturnOrdersForExchange/@*"/>
													<ReturnOrderForExchange>
														<xsl:copy-of select="./OrderLine/DerivedFromOrderLine/Order/ReturnOrdersForExchange/ReturnOrderForExchange/@*"/>

														<OrderLines>
															<xsl:for-each select="./OrderLine/DerivedFromOrderLine/Order/ReturnOrdersForExchange/ReturnOrderForExchange/OrderLines/OrderLine">
																<OrderLine>
																	<xsl:copy-of select="./@*"/>

																	<DerivedFromOrder>
																		<xsl:copy-of select="./DerivedFromOrder/@*"/>
																		<xsl:attribute name="OrderNo">
																			<xsl:value-of select="concat(./DerivedFromOrder/@OrderNo,'R')"/>
																		</xsl:attribute>
																	</DerivedFromOrder>
																</OrderLine>
															</xsl:for-each>
														</OrderLines>
													</ReturnOrderForExchange>
												</ReturnOrdersForExchange>
												</Order>
											</DerivedFromOrderLine>
										</xsl:if>
										<LinePriceInfo>
											<xsl:copy-of select="./OrderLine/LinePriceInfo/@*"/>
										</LinePriceInfo>
										<LineOverallTotals>
											<xsl:copy-of select="./OrderLine/LineOverallTotals/@*"/>
										</LineOverallTotals>
										<xsl:copy-of select="./OrderLine/*[not(self:: Item) and not(self:: ItemDetails) and not(self:: LinePriceInfo) and not(self:: LineOverallTotals) and not(self:: DerivedFromOrderLine)]"/>
									</OrderLine>
									<LineCharges>
										<xsl:for-each select="./LineCharges/LineCharge">									
											<xsl:choose>
												<xsl:when test="@ChargeCategory[.='PromotionDiscount'] or @ChargeCategory[.='PromotionHdrDiscount'] or @IsDiscount[.='Y']">
													<LineCharge>
														<xsl:copy-of select="./@*"/>
														<xsl:variable name="vChargePerLine">
															<xsl:choose>
																<xsl:when test="($vEnterpriseCode = 'HEYDUDE_CA' or $vEnterpriseCode = 'HEYDUDE_AU')">
																	<xsl:value-of select = "format-number(./@ChargePerLine * $vRoundingRate, '#.##')"/>
																</xsl:when>
																<xsl:otherwise>
																	<xsl:value-of select = "./@ChargePerLine"/>
																</xsl:otherwise>
															</xsl:choose>
														</xsl:variable>
														<xsl:attribute name="ChargePerLine">
															<xsl:value-of select = "$vChargePerLine"/>
														</xsl:attribute>
														<xsl:choose>
															<xsl:when test="contains(//InvoiceHeader/@EnterpriseCode, '_US') or contains(//InvoiceHeader/@EnterpriseCode, '_AU')" >
																<xsl:attribute name="Tax">
																	<xsl:choose>
																		<xsl:when test="../../LineTaxes/LineTax[@TaxName='SalesTax']/@TaxPercentage &gt; 0">
																			<xsl:value-of select="format-number((@ChargeAmount * ../../LineTaxes/LineTax[@TaxName='SalesTax']/@TaxPercentage),'#.##')"/>
																		</xsl:when>
																		<xsl:otherwise>
																			<xsl:value-of select="'0.00'"></xsl:value-of>
																		</xsl:otherwise>
																	</xsl:choose>
																</xsl:attribute>
															</xsl:when>
															<xsl:when test="contains(//InvoiceHeader/@EnterpriseCode, '_CA')" >
																<xsl:attribute name="CAGstTax">
																	<xsl:choose>
																		<xsl:when test="../../LineTaxes/LineTax[@TaxName='CAGstTax']/@TaxPercentage &gt; 0">
																			<xsl:value-of select="format-number((@ChargeAmount * ../../LineTaxes/LineTax[@TaxName='CAGstTax']/@TaxPercentage),'#.##')"/>
																		</xsl:when>
																		<xsl:otherwise>
																			<xsl:value-of select="'0.00'"></xsl:value-of>
																		</xsl:otherwise>
																	</xsl:choose>
																</xsl:attribute>
																<xsl:attribute name="CAPstTax">
																	<xsl:choose>
																		<xsl:when test="../../LineTaxes/LineTax[@TaxName='CAPstTax']/@TaxPercentage &gt; 0">
																			<xsl:value-of select="format-number((@ChargeAmount * ../../LineTaxes/LineTax[@TaxName='CAPstTax']/@TaxPercentage),'#.##')"/>
																		</xsl:when>
																		<xsl:otherwise>
																			<xsl:value-of select="'0.00'"></xsl:value-of>
																		</xsl:otherwise>
																	</xsl:choose>
																</xsl:attribute>
																<xsl:attribute name="CAQstTax">
																	<xsl:choose>
																		<xsl:when test="../../LineTaxes/LineTax[@TaxName='CAQstTax']/@TaxPercentage &gt; 0">
																			<xsl:value-of select="format-number((@ChargeAmount * ../../LineTaxes/LineTax[@TaxName='CAQstTax']/@TaxPercentage),'#.##')"/>
																		</xsl:when>
																		<xsl:otherwise>
																			<xsl:value-of select="'0.00'"></xsl:value-of>
																		</xsl:otherwise>
																	</xsl:choose>
																</xsl:attribute>
																<xsl:attribute name="CAHstTax">
																	<xsl:choose>
																		<xsl:when test="../../LineTaxes/LineTax[@TaxName='CAHstTax']/@TaxPercentage &gt; 0">
																			<xsl:value-of select="format-number((@ChargeAmount * ../../LineTaxes/LineTax[@TaxName='CAHstTax']/@TaxPercentage),'#.##')"/>
																		</xsl:when>
																		<xsl:otherwise>
																			<xsl:value-of select="'0.00'"></xsl:value-of>
																		</xsl:otherwise>
																	</xsl:choose>
																</xsl:attribute>
															</xsl:when>
														</xsl:choose>
														<xsl:copy-of select="./*"/>
													</LineCharge>
												</xsl:when>
												<xsl:otherwise>
													<LineCharge>
														<xsl:copy-of select="./@*"/>
														<xsl:copy-of select="./*"/>
													</LineCharge>
												</xsl:otherwise>
											</xsl:choose>									
										</xsl:for-each>
									</LineCharges>
								</LineDetail>
							</xsl:for-each>
						</LineDetails>	
					</xsl:otherwise>
				</xsl:choose>			
			</InvoiceHeader>		
		</InvoiceDetail>
	</xsl:template>
</xsl:stylesheet>
