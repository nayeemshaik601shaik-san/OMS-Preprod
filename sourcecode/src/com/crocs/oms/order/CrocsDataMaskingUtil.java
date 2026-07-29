package com.crocs.oms.order;

import org.w3c.dom.Document;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * Utility class for masking sensitive information in a Vertex tax SOAP request.
 * EOMS - 
 */
public class CrocsDataMaskingUtil {

    /**
     * Masks sensitive information such as UserName, Password, and TrustedId
     * from the input SOAP Document.
     *
     * @param env       YFSEnvironment (unused in current implementation)
     * @param inputDoc  Input XML Document representing the SOAP message
     * @return          Modified Document with sensitive values masked
     */
    public Document maskSensitiveInformation(YFSEnvironment env, Document inputDoc) {
        
        // Wrap the input org.w3c.dom.Document in YFCDocument for easier navigation
        YFCDocument vertexTaxInput = YFCDocument.getDocumentFor(inputDoc);
        YFCElement vertexTaxInputEle = vertexTaxInput.getDocumentElement();

        // Navigate to the <urn:Login> element inside the SOAP envelope
        YFCElement bodyElement = vertexTaxInputEle.getChildElement("SOAP-ENV:Body", true);
        YFCElement vertexEnvelope = bodyElement.getChildElement("urn:VertexEnvelope", true);
        YFCElement loginElement = vertexEnvelope.getChildElement("urn:Login", true);

        // Fetch individual sensitive fields
        YFCElement userNameElement = loginElement.getChildElement("urn:UserName", true);
        YFCElement passwordElement = loginElement.getChildElement("urn:Password", true);
        YFCElement trustedIdElement = loginElement.getChildElement("urn:TrustedId", true);

        // Masking string
        final String MASK = "*********";
        
        // Apply masking
        userNameElement.setNodeValue(MASK);
        passwordElement.setNodeValue(MASK);
        trustedIdElement.setNodeValue(MASK);

        // Return the masked org.w3c.dom.Document
        return vertexTaxInput.getDocument();
    }
    
}
