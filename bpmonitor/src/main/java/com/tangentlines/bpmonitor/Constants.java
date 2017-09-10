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

public class Constants {

    /* GENERIC BLE CONSTANTS */
    public static final String UUID_SERVICE_DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";

    public static final String UUID_CHARACTERISTIC_MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHARACTERISTIC_SYSTEM_FIRMWARE_NUMBER_STRING = "00002a26-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING = "00002a27-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING = "00002a28-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHARACTERISTIC_MANUFACTURER_STRING = "00002a29-0000-1000-8000-00805f9b34fb";

    public static final String UUID_DESCRIPTOR_CLIENT_CHARACTERISTICS_CONFIGURATION = "00002902-0000-1000-8000-00805f9b34fb";


    /* PHILIPS SPECIFIC BLE CONSTANTS */
    public static final String UUID_SERVICE_BLOOD_PRESSURE = "00007899-0000-1000-8000-00805f9b34fb";

    public static final String UUID_CHARACTERISTIC_BLOOD_PRESSURE_DATA = "00008a91-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHARACTERISTIC_CONTROL = "00008a81-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHARACTERISTIC_INDICATE_CHALLENGE = "00008a82-0000-1000-8000-00805f9b34fb";

    public static final byte CMD_FINISH = (byte) 0x22;
    public static final byte CMD_SEND_XOR = (byte) 0x20;

    public static final byte CMD_SEND_TIME = (byte) 0x02;
    public static final byte CMD_SEND_BROADCAST_ID = (byte) 0x21;

    public static final byte CMD_SET_USER = (byte) 0x03;
    public static final byte CMD_SELECT_USER = (byte) 0x51;

    public static final byte RESPONSE_PASSWORD = (byte) 0xA0;
    public static final byte RESPONSE_CHALLENGE = (byte) 0xA1;
    public static final byte RESPONSE_GET_USER = (byte) 0x83;

}
