package com.crocs.oms.item;
import java.rmi.RemoteException;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class CrocsManageItemData  {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsManageItemData.class);

	/**
		 * EOMS - 5234 Enhancement Product Feed : Attribute Extension for YFS_ITEM_LOCALE & Hangoff Table
		 * @param env
		 * @param inDoc
		 * @return
		 * @throws Exception
		 */
	
	/**
		 * Input XML:-
		 * <ItemList>
		<Item ItemID="10002-001-M23"
		      OrganizationCode="CROCS_NA"
		      UnitOfMeasure="EACH">
			<PrimaryInformation ColorCode="White" 
			                    Description="original, classic clog, classic, crocs, jjjjound, baya"
			                    EffectiveEndDate=""
			                    EffectiveStartDate=""
			                    ExtendedDescription="Original. Versatile. Comfortable.It’s the iconic clog that started a comfort revolution around the world! The irreverent go-to comfort shoe that you're sure to fall deeper in love with day after day. Crocs Classic Clogs offer lightweight Iconic Crocs Comfort™, a color for every personality, and an ongoing invitation to be comfortable in your own shoes.Classic Clog Details:Incredibly light and fun to wear Made with our innovative Croslite™ compound, which now contains 25% bio-circular materials like cooking oil from other industries Learn more about what we're doing with Crocs Croslite™ material.Lightweight, water-friendly and buoyant Designed to enhance breathability Easy to clean and quick to dry Pivoting heel straps for a more secure fit Customizable with Jibbitz™ charms Iconic Crocs Comfort™: Lightweight. Flexible. 360-degree comfort."
			                    ExtendedDisplayDescription="Classic Clog"
			                    ImageID="t_standard/products/10002_001_ALT170/crocs"
			                    ImageLocation="https://media.crocs.com/images"
			                    IsModelItem=""
			                    ItemType="Footwear"
			                    MaxOrderQuantity=""
			                    MinOrderQuantity="1"
			                    ModelItemUnitOfMeasure="EACH"
			                    ReturnWindow="45"
			                    ShortDescription="original, classic clog, classic, crocs, jjjjound, baya"
			                    SizeCode="M17"
			                    Status="3000"/>
			<ClassificationCodes Model="10002"
			                     TaxProductCode=""/>
			<ItemAliasList Reset="Y">
				<ItemAlias AliasName="UPC"
				           AliasValue="811358002504"/>
			</ItemAliasList>
			<Extn ExtnSAPSilhoette="Clog"
			      ExtnSAPGender="Unisex Adult"
			      ExtnSAPMaterialGroup="Footwear"
	                      ExtnCollabSKU="N" 
			      ExtnColor="001" ExtnImageUrl="https://media.crocs.com/images/t_thumbnail/f_auto%2Cq_auto/products/10001_4NS_ALT100/crocs-classic-clog-blue-calcite-charm-view" ExtnProductUrl="https://www.crocs.com/p/classic-clog/10001.html?cid=Blue%20Calcite">
	                      <CrocsItemOrgDataList>
	                              <CrocsItemOrgData OrganizationCode="CROCS_CA" SizeCode="M18" MaxOrderQuantity="14" ReturnWindow="20"/>
	                      </CrocsItemOrgDataList>
	</Extn>
			<AdditionalAttributeList Reset="Y">
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="HSTCode"
				                     Name="crocs_us"
				                     Value="6402993165"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="HSTCode"
				                     Name="crocs_ca"
				                     Value="6402993165"/>
				<AdditionalAttribute AttributeDomainID="isOnline"
				                     AttributeGroupID="siteid"
				                     Name="crocs_us"
				                     Value="Y"/>
				<AdditionalAttribute AttributeDomainID="isOnline"
				                     AttributeGroupID="siteid"
				                     Name="crocs_ca"
				                     Value="Y"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="ICFulfillmentCost"
				                     Name="crocs_us"
				                     Value="0.64"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="ICFulfillmentCost"
				                     Name="crocs_ca"
				                     Value="0.64"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="ICFulfillmentCurrency"
				                     Name="crocs_us"
				                     Value="USD"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="ICFulfillmentCurrency"
				                     Name="crocs_ca"
				                     Value="CAD"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="MSRP"
				                     Name="crocs_ca"
				                     Value="49.99"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="MSRP"
				                     Name="crocs_us"
				                     Value="49.99"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="StandardCost"
				                     Name="crocs_us"
				                     Value="4.59"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="StandardCurrency"
				                     Name="crocs_us"
				                     Value="USD"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="StandardCost"
				                     Name="crocs_ca"
				                     Value="4.59"/>
				<AdditionalAttribute AttributeDomainID="isCustom"
				                     AttributeGroupID="StandardCurrency"
				                     Name="crocs_ca"
				                     Value="CAD"/>
			</AdditionalAttributeList>
			<LanguageDescriptionList Reset="Y">
				<LanguageDescription Description="été 2009, original, clog, clogs, classic"
				                     ExtendedDescription="Originaux. Polyvalents. Confortables.Ce sont les sabots emblématiques qui ont amorcé une révolution du confort dans le monde! Les chaussures irrévérencieuses et confortables que vous adorerez de plus en plus jour après jour. Les sabots classiques de Crocs offrent le léger Confort™ emblématique de Crocs, une couleur pour chaque personnalité, et une invitation continue à être à l’aise dans vos propres chaussures.Caractéristiques des sabots classiques :Incroyablement légers et amusants à porter Fabriqués à l’aide de notre composé innovant, la Croslite™, comportant désormais 25 % de matériaux bio-circulaires comme de l’huile de cuisson provenant d’autres industries.Légers, résistants à l’eau et flottants Conçus pour améliorer la respirabilité Se lavent facilement et sèchent rapidement Les courroies pivotantes aux talons offrent un meilleur ajustement Personnalisables avec des breloques Jibbitz™ Iconic Crocs Comfort™ : Légèreté. Souples. Confort total."
				                     ExtendedDisplayDescription="Sabots classiques"
				                     LocaleCode="fr_CA"
				                     ShortDescription="été 2009, original, clog, clogs, classic" ImageID="" ImageLocation="" > 
	                           <Extn ExtnColorCode="M17" ExtnProductURL="https://www.crocs.com/p/classic-clog/10001.html?cid=Blue%20Calcite" />
	                          </LanguageDescription>
			</LanguageDescriptionList>
		</Item>
	</ItemList>
	 ***/
	
	public Document manageCrocsItemOrgData(YFSEnvironment env, Document inDoc)  {

		logger.beginTimer("CrocsManageItemData.CrocsItemOrgData");
		logger.verbose("CrocsManageItemData.CrocsItemOrgData Input XML: " + inDoc);
		Element eleItemList = inDoc.getDocumentElement();
		Element eleItem = SCXmlUtil.getChildElement(eleItemList, CrocsXmlConstants.E_ITEM);
		String itemID = eleItem.getAttribute(CrocsXmlConstants.A_ITEM_ID);
		Document getItemListOutput = null ;
		logger.info("CrocsManageItemData.CrocsItemOrgData ItemFeed is being prepare for itemID: " + itemID);

		// Build input for getItemList API
		if (!YFCCommon.isVoid(itemID)) {
			try {
				Document getItemListInpDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ITEM);
				Element eleGetItemList = getItemListInpDoc.getDocumentElement();
				eleGetItemList.setAttribute(CrocsXmlConstants.A_ITEM_ID,itemID);
				eleGetItemList.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE,
						eleItem.getAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE));
				eleGetItemList.setAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE,
						eleItem.getAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE));

				// Invoke getItemList API 
				 getItemListOutput = CommonUtil.invokeAPI(env,
						CrocsTemplateConstants.TEMPLATE_GET_ITEM_ORG_DATA_LIST, CrocsAPIConstants.API_GET_ITEM_LIST,
						getItemListInpDoc);
				 
				 logger.verbose("CrocsManageItemData.CrocsItemOrgData: get Item List Output " + SCXmlUtil.getString(getItemListOutput));

				String getItemListOItemKey = SCXmlUtil.getXpathAttribute(getItemListOutput.getDocumentElement(),
						CrocsXmlConstants.STR_XPATH_ITEM_KEY);			
				/*
				 * if getItemListOItemKey is blank, It means this item is new to be created.
				 * else we update the existing one
				 **/
				if (!YFCCommon.isVoid(getItemListOItemKey)) {
					
				  logger.info("CrocsManageItemData.CrocsItemOrgData:The item already exists in OMS. " + SCXmlUtil.getString(getItemListOutput));
					List<Node> crocsItemOrgDataNodeListInDoc = XMLUtil.getElementListByXpath(inDoc,
							CrocsXmlConstants.STR_XPATH_CROCS_ITEM_ORG_DATA);
					
					for (Node node : crocsItemOrgDataNodeListInDoc) {
						Element crocsItemOrgDataInDocEle = (Element) node;
						updateCrocsItemOrgDataForAnItem(env, getItemListOItemKey, crocsItemOrgDataInDocEle, getItemListOutput);
					}
				}else {
					/*
					 * Convert the Organization code to upper case for Hang off Table */
					inDoc=convertOrgCodeToUpperCase(inDoc);
					
				}
			} catch (Exception e) {
				logger.info("CrocsManageItemData.CrocsItemOrgData: Input the Received " + SCXmlUtil.getString(inDoc));
				logger.info("CrocsManageItemData.CrocsItemOrgData: getItemList output" + SCXmlUtil.getString(getItemListOutput));
				logger.verbose("Error in the method CrocsManageItemData.updateItemData: " + e.getLocalizedMessage());
			} 
		}
		logger.verbose("CrocsManageItemData.CrocsItemOrgData:  output Document for manageItem API"  + SCXmlUtil.getString(inDoc));
		return inDoc;
	}

	/**
	 * @param inDoc
	 * Convert the OrganizationCode to Upper Case
	 */
	private Document  convertOrgCodeToUpperCase(Document inDoc) {
		NodeList  crocsItemOrgDataNl = inDoc.getElementsByTagName(CrocsXmlConstants.E_CROCS_ITEM_ORG_DATA);
		
		 for (int i = 0; i < crocsItemOrgDataNl.getLength(); i++) {
             Element element = (Element) crocsItemOrgDataNl.item(i);
             String orgCode = element.getAttribute("OrganizationCode");

             // Convert to uppercase and update the attribute
             element.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, orgCode.toUpperCase());
         }
		 return inDoc;	
	}

	/**
	 * update or create the Record in the hangoff table
	 * @param env
	 * @param getItemListOItemKey
	 * @param crocsItemOrgDataInDocEle
	 * @param docGetItemListOutput
	 * @throws RemoteException
	 */
	private void updateCrocsItemOrgDataForAnItem(YFSEnvironment env, String getItemListOItemKey, Element crocsItemOrgDataInDocEle,
			Document docGetItemListOutput) throws RemoteException {
		
		/*Till this point . item exist in OMS */
		String itemOrgCode = crocsItemOrgDataInDocEle.getAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE);
		String locale = crocsItemOrgDataInDocEle.getAttribute(CrocsXmlConstants.A_LOCALE);

		String xpathQuery = String.format(
				"/ItemList/Item/Extn/CrocsItemOrgDataList/CrocsItemOrgData[@OrganizationCode='%s' and @Locale='%s']", itemOrgCode.toUpperCase(),locale);
		
		logger.verbose("CrocsManageItemData.updateCrocsItemOrgDataForAnItem: xpathQuery" +xpathQuery);
		
		Element existingCrocsItemOrgDataEle = SCXmlUtil.getXpathElement(docGetItemListOutput.getDocumentElement(),
				xpathQuery);
		
		logger.verbose("CrocsManageItemData.updateCrocsItemOrgDataForAnItem:existingCrocsItemOrgDataEle" +SCXmlUtil.getString(existingCrocsItemOrgDataEle));
		
		Element  getItemListEle =SCXmlUtil.getChildElement(docGetItemListOutput.getDocumentElement(), CrocsXmlConstants.E_ITEM);
		String getItemListItemID = getItemListEle.getAttribute(CrocsXmlConstants.A_ITEM_ID);
		
		if(!YFCObject.isVoid(existingCrocsItemOrgDataEle)) {
			
		/* Update existing item from master record to hang off table (CrocsItemOrgData)
		 **/ 
			String itemOrgDataKey = existingCrocsItemOrgDataEle.getAttribute(CrocsXmlConstants.A_ITEM_ORG_DATA_KEY);		
			Document updateDoc = buildItemOrgDataDoc(getItemListItemID,getItemListOItemKey, crocsItemOrgDataInDocEle);
			updateDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ITEM_ORG_DATA_KEY, itemOrgDataKey);
			logger.info("CrocsManageItemData.updateCrocsItemOrgDataForAnItem:Record already exists in CrocsItemOrgData HangOff. Updating the existing entry."  +itemOrgDataKey +" "+ getItemListItemID);
			logger.verbose("CrocsManageItemData.updateCrocsItemOrgDataForAnItem:updateCrocsItemOrgData" +SCXmlUtil.getString(updateDoc));
			CommonUtil.invokeService(env, CrocsAPIConstants.SERVICE_UPDATE_ITEM_ORG_DATA_LIST, updateDoc);
		} else {
			// Create a new record for CrocsItemOrgData hang off Table
			Document createDoc = buildItemOrgDataDoc(getItemListItemID,getItemListOItemKey, crocsItemOrgDataInDocEle);
			logger.info("CrocsManageItemData.updateCrocsItemOrgDataForAnItem:A new entry is being added to CrocsItemOrgData HangOff.." +getItemListItemID );
			logger.verbose("CrocsManageItemData.updateCrocsItemOrgDataForAnItem: createCrocsItemOrgData" +SCXmlUtil.getString(createDoc));
			CommonUtil.invokeService(env, CrocsAPIConstants.SERVICE_CREATE_ITEM_ORG_DATA_LIST, createDoc);
		}
		
		/**  since create and Update has already been managed to Hang off table, we need to remove it from the manageItem input doc 
		 * */		
		
		crocsItemOrgDataInDocEle.getParentNode().removeChild(crocsItemOrgDataInDocEle);
		logger.verbose("CrocsManageItemData.updateCrocsItemOrgDataForAnItem: remove the record from the ItemList" );
	}

	/**
	 * @param strItemKey
	 * @param eleCrocsItemOrgData
	 * @return
	 */
	private Document buildItemOrgDataDoc(String itemID,String strItemKey, Element eleCrocsItemOrgData) {
		/** Sample doc 	
			 * <CrocsItemOrgData ItemOrgDataKey="20250826094730143186"
					                  ItemKey="20250826093636143066"
					                  OrganizationCode="CROCS_CA"
					                  SizeCode="M18"
					                  MaxOrderQuantity="14"
					                  ReturnWindow="20"/>
		 * */		
		Document crocsItemOrgDataInputdoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_CROCS_ITEM_ORG_DATA);
		Element crocsItemOrgDataInputdocEle = crocsItemOrgDataInputdoc.getDocumentElement();
		crocsItemOrgDataInputdocEle.setAttribute(CrocsXmlConstants.A_ITEM_KEY, strItemKey);
		crocsItemOrgDataInputdocEle.setAttribute(CrocsXmlConstants.A_SIZE_CODE,
				eleCrocsItemOrgData.getAttribute(CrocsXmlConstants.A_SIZE_CODE));
		crocsItemOrgDataInputdocEle.setAttribute(CrocsXmlConstants.A_MAX_ORDER_QUANTITY,
				eleCrocsItemOrgData.getAttribute(CrocsXmlConstants.A_MAX_ORDER_QUANTITY));
		crocsItemOrgDataInputdocEle.setAttribute(CrocsXmlConstants.A_RETURN_WINDOW,
				eleCrocsItemOrgData.getAttribute(CrocsXmlConstants.A_RETURN_WINDOW));
		crocsItemOrgDataInputdocEle.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE,
				(eleCrocsItemOrgData.getAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE)).toUpperCase());
		crocsItemOrgDataInputdocEle.setAttribute(CrocsXmlConstants.A_ITEM_ID,itemID);
		crocsItemOrgDataInputdocEle.setAttribute(CrocsXmlConstants.A_LOCALE,
				(eleCrocsItemOrgData.getAttribute(CrocsXmlConstants.A_LOCALE)));
        crocsItemOrgDataInputdocEle.setAttribute(CrocsXmlConstants.A_PRODUCT_URL,
                (eleCrocsItemOrgData.getAttribute(CrocsXmlConstants.A_PRODUCT_URL)));
        crocsItemOrgDataInputdocEle.setAttribute(CrocsXmlConstants.A_EXTENDED_DISPLAY_DESCRIPTION,
                (eleCrocsItemOrgData.getAttribute(CrocsXmlConstants.A_EXTENDED_DISPLAY_DESCRIPTION)));
		return crocsItemOrgDataInputdoc;
	}
}
