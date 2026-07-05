package org.service.b.crypto.service;

/** Signals a user-facing problem talking to Bitpanda (bad key, unreachable, etc.). */
public class BitpandaException extends RuntimeException {
    public BitpandaException(String message) {
        super(message);
    }
}
