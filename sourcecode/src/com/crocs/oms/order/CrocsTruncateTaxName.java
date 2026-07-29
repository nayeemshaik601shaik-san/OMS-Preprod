package com.crocs.oms.order;

import org.w3c.dom.Document;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfs.japi.YFSEnvironment;

public class CrocsTruncateTaxName {
	
public Document crocsTruncateTaxName(YFSEnvironment env, Document inputDoc) {
        
        // Wrap the input org.w3c.dom.Document in YFCDocument for easier navigation
	
        YFCDocument vertexTaxInput = YFCDocument.getDocumentFor(inputDoc);
        YFCElement vertexTaxInputEle = vertexTaxInput.getDocumentElement();

        // Navigate to the <urn:Login> element inside the SOAP envelope
        YFCElement bodyElement = vertexTaxInputEle.getChildElement("soapenv:Body", true);
        YFCElement vertexEnvelope = bodyElement.getChildElement("VertexEnvelope", true);
        YFCElement quotationResponse = vertexEnvelope.getChildElement("QuotationResponse", true);
        
     // Loop through all LineItems
        for (YFCElement lineItem : quotationResponse.getChildren("LineItem")) {
            // Loop through all Taxes elements inside LineItem
            for (YFCElement taxesElement : lineItem.getChildren("Taxes")) {
                YFCElement imposition = taxesElement.getChildElement("Imposition");
                if (imposition != null) {
                    String value = imposition.getNodeValue();
                    if ("Retail Sales Tax (RST)".equals(value)) {
                        imposition.setNodeValue("ABC Sales Tax Testing (BST)");
                    }
                }
            }
        }

        // Return updated document
        return vertexTaxInput.getDocument();

    }

}
