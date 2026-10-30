package com.quanlynhakhoa.billing.service;

import java.security.SecureRandom;
import java.sql.Connection;

/** Ten base32 characters (50 random bits). PK is final arbiter; collision is surfaced, never retried. */
public final class RandomInvoiceIdGenerator implements InvoiceIdGenerator {
    private static final char[] ALPHABET="0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private final SecureRandom random=new SecureRandom();
    @Override public String next(Connection connection) {
        char[] id=new char[10];for(int i=0;i<id.length;i++)id[i]=ALPHABET[random.nextInt(ALPHABET.length)];
        return new String(id);
    }
}
