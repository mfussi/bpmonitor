/*
 * Copyright (C) 2017 Markus Fußenegger.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tangentlines.bpmonitor.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by markus on 23.08.17.
 */

public class BinaryUtils {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static byte[] hexStringToByteArray(String s) {

        if (s == null) {
            return null;
        }

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }



    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] longToBytes(long paramLong) {
        long unsigned = 0xFFFFFFFFFFFFFFFFL & paramLong;
        return new byte[]{(byte) (int) (0xFF & unsigned), (byte) (int) ((0xFF00 & unsigned) >>> 8), (byte) (int) ((0xFF0000 & unsigned) >>> 16), (byte) (int) ((unsigned & 0xFFFFFFFFFF000000L) >>> 24)};
    }

    public static byte[] fillRandom(byte[] array) {

        if (array == null) {
            return null;
        }

        for (int i = 0; i < array.length; i++) {
            array[i] = (byte) (Math.random() * 256);
        }

        return array;

    }

    /* TODO: support for negative numbers is missing */
    public static float doubleByteToFloat(byte[] input) {

        int intValue = (0xFF00 & input[1] << 8) + (input[0] & 0xFF);            // input as int
        int sign = (0x8000 & intValue) >>> 15;                                  // sign
        int exponent = (0xF000 & intValue) >>> 12;                              // exponent

        return (float) ((intValue & 0x1FFF) * Math.pow(10.0D, exponent));

    }

    public static Date toDate(byte[] paramArrayOfByte) {

        long secondsSince2010 = getUnsignedInt(paramArrayOfByte);

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2010, 1, 1);

        return new Date(cal.getTimeInMillis() + (secondsSince2010 * 1000L));

    }

    public static long getUnsignedInt(byte[] bytes) {

        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return ((long) buffer.getInt(0) & 0xffffffffL);

    }

    public static int getUnsignedShort(byte[] paramArrayOfByte) {
        return 0xFF00 & paramArrayOfByte[1] << 8 | paramArrayOfByte[0] & 0xFF;
    }

}
