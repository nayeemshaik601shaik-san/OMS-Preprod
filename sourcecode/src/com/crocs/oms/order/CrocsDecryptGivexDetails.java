package com.crocs.oms.order;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.json.JSONException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;

public class CrocsDecryptGivexDetails implements CrocsXmlConstants {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsDecryptGivexDetails.class);

	/**
	 * Takes an input XML containing an encrypted SvcNo and returns the decrypted Givex card number.
	 * <PaymentMethod SvcNo="xzj064/leaYdQjr/rEymABTlVXJ814N2ThaaP9o88Zk=" PaymentType="Givex" />
	 * @param yfsEnvironment
	 * @param inDoc
	 * @return
	 */
	public Document decryptGivexDetails(YFSEnvironment yfsEnvironment, Document inDoc) {
		logger.verbose("Start of method decryptGivexDetails with input: " + SCXmlUtil.getString(inDoc));

		Element paymentMethod = inDoc.getDocumentElement();

		String paymentType = paymentMethod.getAttribute(A_PAYMENT_TYPE);
		if (paymentType.equalsIgnoreCase(CrocsConstant.STR_GIVEX)) {

			String encodedSvcNo = paymentMethod.getAttribute(CrocsConstant.SvcNo);
			try {
				CrocsDecryption decryption = new CrocsDecryption();
				String decodedSvcNo = decryption.getDecryptedDataForGivex(encodedSvcNo);

				logger.verbose("Decrypted SvcNo: " + decodedSvcNo);
				paymentMethod.setAttribute(CrocsConstant.SvcNo, decodedSvcNo);

			} catch (InvalidKeyException | DOMException | NoSuchAlgorithmException | NoSuchPaddingException
					| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
					| JSONException e) {
	            logger.error("Error decrypting givex card details: " + e.getMessage());
				throw new YFCException(e.getMessage());
			}
		}
		logger.verbose("End of method decryptGivexDetails with output: " + SCXmlUtil.getString(inDoc));
		return inDoc;
	}
}
