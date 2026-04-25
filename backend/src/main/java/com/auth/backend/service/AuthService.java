package com.auth.backend.service;

import org.springframework.stereotype.Service;
import org.web3j.crypto.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;


import java.math.BigInteger;

@Service
public class AuthService {
    private final Map<String, String> nonces = new ConcurrentHashMap<>();
    public String generateNonce(String address) {

        String nonce = "Login to my app: " + UUID.randomUUID();

        nonces.put(address.toLowerCase(), nonce);

        return nonce;
    }

    public String getNonce(String address) {
        return nonces.get(address.toLowerCase());
    }

    public void removeNonce(String address) {
        nonces.remove(address.toLowerCase());
    }


    public String recoverAddress(String message, String signature) throws Exception {

        byte[] signatureBytes = org.web3j.utils.Numeric.hexStringToByteArray(signature);

        if (signatureBytes.length != 65) {
            throw new RuntimeException("Invalid signature length");
        }

        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;
        }

        byte[] r = new byte[32];
        byte[] s = new byte[32];

        System.arraycopy(signatureBytes, 0, r, 0, 32);
        System.arraycopy(signatureBytes, 32, s, 0, 32);

        org.web3j.crypto.Sign.SignatureData sigData =
                new org.web3j.crypto.Sign.SignatureData(v, r, s);

        BigInteger publicKey = org.web3j.crypto.Sign.signedPrefixedMessageToKey(
                message.getBytes(),
                sigData
        );

        return "0x" + org.web3j.crypto.Keys.getAddress(publicKey);
    }
}