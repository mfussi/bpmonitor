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
package com.tangentlines.bpmonitor;

import com.tangentlines.bpmonitor.model.BloodPressureReading;
import com.tangentlines.bpmonitor.model.UserInformation;
import com.tangentlines.bpmonitor.utils.BinaryUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class BPMonitorDataConverter {

    public static BloodPressureReading bytesToBloodPressureData(byte[] paramArrayOfByte) {

        try {

            int battery = 0;
            Date date = null;
            boolean isIrregularPulseDetectionFlag = false;
            float pulseRate = 0.0f;
            int userId = 0;

            int statusByte = paramArrayOfByte[0];
            int index = 7;

            float systolic = BinaryUtils.doubleByteToFloat(Arrays.copyOfRange(paramArrayOfByte, 1, 3));
            float diastolic = BinaryUtils.doubleByteToFloat(Arrays.copyOfRange(paramArrayOfByte, 3, 5));
            float meanArterialPressure = BinaryUtils.doubleByteToFloat(Arrays.copyOfRange(paramArrayOfByte, 5, 7));

            if ((byte) (statusByte & 0x2) == 2) {
                index = 11;
                date = (BinaryUtils.toDate(Arrays.copyOfRange(paramArrayOfByte, 7, 11)));
            }

            if ((byte) (statusByte & 0x4) == 4) {
                pulseRate = (BinaryUtils.doubleByteToFloat(Arrays.copyOfRange(paramArrayOfByte, index, index + 2)));
                index += 2;
            }

            if ((byte) (statusByte & 0x8) == 8) {
                userId = (paramArrayOfByte[index]);
                index += 1;
            }

            if ((byte) (statusByte & 0x10) == 16) {
                int options = BinaryUtils.getUnsignedShort(new byte[]{paramArrayOfByte[index], paramArrayOfByte[(index + 1)]});

                if ((options & 0x4) == 0x4) {
                    isIrregularPulseDetectionFlag = true;
                } else {
                    isIrregularPulseDetectionFlag = false;
                }

            }

            index += 2;
            if ((byte) (statusByte & 0x20) == 1) {
                battery = (paramArrayOfByte[index]);
            }

            return new BloodPressureReading(userId, date, systolic, diastolic, meanArterialPressure, pulseRate, isIrregularPulseDetectionFlag, battery);

        } catch (Exception e) {
            return null;
        }

    }

    public static UserInformation bytesAsUserInformation(byte[] input) {

        if (input == null || input.length < 2) {
            return null;
        }

        return new UserInformation(input[0], new String(Arrays.copyOfRange(input, 1, input.length)).trim());

    }

    public static byte[] userInformationAsBytes(UserInformation userInformation) {

        byte[] output = new byte[17];
        byte[] bytesName = String.format("%1$-16s", userInformation.getName().substring(0, Math.min(16, userInformation.getName().length()))).getBytes();

        output[0] = (byte) userInformation.getId();
        System.arraycopy(bytesName, 0, output, 1, bytesName.length);

        return output;

    }

    public static byte[] currentDateTimeAsBytes() {

        Calendar startCalendar = new GregorianCalendar();
        startCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        startCalendar.set(2010, 1, 1);

        Calendar nowCalendar = new GregorianCalendar();
        nowCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        long secondsSince = ((nowCalendar.getTime().getTime() - startCalendar.getTime().getTime()) / 1000L);
        return BinaryUtils.longToBytes(secondsSince);

    }

    public static byte[] selectUserAsBytes(int userId) {
        return new byte[]{0, (byte) userId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    }

    public static byte[] randomBroadcastId() {
        return BinaryUtils.fillRandom(new byte[4]);
    }

    public static byte[] calculateChallengeResponse(byte[] challenge, byte[] salt) {

        byte[] output = new byte[challenge.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = (byte) (challenge[i] ^ salt[i]);
        }

        return output;

    }

}
