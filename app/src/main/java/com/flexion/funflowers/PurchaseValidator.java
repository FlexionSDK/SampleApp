package com.flexion.funflowers;

import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * Purchase validator utility. For a secure implementation, all of this code
 * should be implemented on a server that communicates with the
 * application on the device. For the sake of simplicity and clarity of this
 * example, this code is included here and is executed on the device. If you
 * must verify the purchases on the phone, you should obfuscate this code to
 * make it harder for an attacker to replace the code with stubs that treat all
 * purchases as verified.
 */
public class PurchaseValidator {

    private static final String TAG = "PurchaseValidator";

    private static final String FLEXION_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCyRJrfK4rDPgQ2fQzZaABgcA4OuOY/3wO+Q3V2/g9GwGhPQbfJ7UKVsi"
            + "TUIyThrAwPnn9FsSjUgTfqSUKm8oEVvJBV8cYWp2meKbpSpM/5Y5snj8B6nuCzLCFhbtMD6YIk0PXvJHWWFWXcP8BIUjSX7EtPi2E+8GaqDWtMYVPgfwIDAQAB";

    public static boolean verifyPurchaseData(String signedJsonData, String b64SignatureToVerifyWith) throws Exception {
        Log.d(TAG, "Validating signature: '" + b64SignatureToVerifyWith + "' of purchase JSON: '" + signedJsonData + "'");
        byte[] decodedKey = Base64.decode(FLEXION_PUBLIC_KEY, Base64.DEFAULT);
        KeyFactory keyFactory = KeyFactory.getInstance( "RSA");
        final X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(decodedKey);
        PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
        Signature contentSignature = java.security.Signature.getInstance("SHA1withRSA");
        contentSignature.initVerify(publicKey);
        contentSignature.update(signedJsonData.getBytes());
        byte[] signatureToVerifyWith = Base64.decode(b64SignatureToVerifyWith, Base64.DEFAULT);
        boolean signatureIsVerified = contentSignature.verify(signatureToVerifyWith);
        return signatureIsVerified;
    }
}
