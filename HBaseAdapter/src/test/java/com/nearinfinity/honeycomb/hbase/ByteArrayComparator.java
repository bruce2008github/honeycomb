package com.nearinfinity.honeycomb.hbase;

import com.google.common.primitives.UnsignedBytes;

import java.util.Comparator;

/**
 * Comparator to sort byte arrays.  Short byte arrays sort first.  Arrays of
 * equal length sort according to byte value comparison from left to right.
 */
public class ByteArrayComparator implements Comparator<byte[]> {
    @Override
    public int compare(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            return b1.length - b2.length;
        } else {
            for (int i = 0; i < b1.length; i++) {
                if (b1[i] == b2[i]) {
                    continue;
                } else {
                    return UnsignedBytes.compare(b1[i], b2[i]);
                }
            }
        }
        return 0;
    }
}