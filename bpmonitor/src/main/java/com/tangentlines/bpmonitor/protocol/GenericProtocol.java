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

package com.tangentlines.bpmonitor.protocol;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.tangentlines.bpmonitor.BPMonitor;
import com.tangentlines.bpmonitor.BPMonitorError;
import com.tangentlines.bpmonitor.Constants;

public abstract class GenericProtocol implements Protocol {

    private static final String TAG = GenericProtocol.class.getSimpleName();

    @NonNull
    protected final BPMonitor mDevice;

    protected final Handler mNotifyHandler = new Handler();

    private ProtocolListener mListener;

    GenericProtocol(BPMonitor device) {
        this.mDevice = device;
    }

    void onStart(){

        mNotifyHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onProtocolStarted();
                }
            }
        });

    }

    void onError(){

        mNotifyHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onProtocolError();
                }
            }
        });

    }

    void onFinished(){

        mNotifyHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onProtocolFinished();
                }
            }
        });

    }

    @Override
    public boolean handleCharacteristicRead(BluetoothGattCharacteristic characteristics) {

        final String uuid = characteristics.getUuid().toString();

        switch (uuid) {

            case Constants.UUID_CHARACTERISTIC_MANUFACTURER_STRING:
                this.mDevice.getDeviceInformation().setManufacturer(characteristics.getStringValue(0));
                return true;

            case Constants.UUID_CHARACTERISTIC_MODEL_NUMBER_STRING:
                this.mDevice.getDeviceInformation().setModelNumber(characteristics.getStringValue(0));
                return true;

            case Constants.UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING:
                this.mDevice.getDeviceInformation().setSoftwareRevision(characteristics.getStringValue(0));
                return true;

            case Constants.UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING:
                this.mDevice.getDeviceInformation().setHardwareRevision(characteristics.getStringValue(0));
                return true;

            case Constants.UUID_CHARACTERISTIC_SYSTEM_FIRMWARE_NUMBER_STRING:
                this.mDevice.getDeviceInformation().setFirmwareRevision(characteristics.getStringValue(0));
                return true;

            case Constants.UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING:
                this.mDevice.getDeviceInformation().setSerialNumber(characteristics.getStringValue(0));
                this.onSetupFinished();
                return true;

        }

        return false;

    }

    @Override
    public boolean handleCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    public boolean handleCharacteristicWrite(BluetoothGattCharacteristic characteristics, int status) {
        return false;
    }

    @Override
    public void setProtocolListener(ProtocolListener listener) {
        this.mListener = listener;
    }

    protected abstract void onSetupFinished();

    protected abstract void fireError(BPMonitorError error);

    /*
     * tries to receive generic device information
     */
    protected void requestDeviceInformation() {
        Log.d(TAG, "requestDeviceInformation()");

        this.mDevice.queueReadCharacteristic(Constants.UUID_SERVICE_DEVICE_INFORMATION, Constants.UUID_CHARACTERISTIC_MANUFACTURER_STRING);
        this.mDevice.queueReadCharacteristic(Constants.UUID_SERVICE_DEVICE_INFORMATION, Constants.UUID_CHARACTERISTIC_MODEL_NUMBER_STRING);
        this.mDevice.queueReadCharacteristic(Constants.UUID_SERVICE_DEVICE_INFORMATION, Constants.UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING);
        this.mDevice.queueReadCharacteristic(Constants.UUID_SERVICE_DEVICE_INFORMATION, Constants.UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING);
        this.mDevice.queueReadCharacteristic(Constants.UUID_SERVICE_DEVICE_INFORMATION, Constants.UUID_CHARACTERISTIC_SYSTEM_FIRMWARE_NUMBER_STRING);
        this.mDevice.queueReadCharacteristic(Constants.UUID_SERVICE_DEVICE_INFORMATION, Constants.UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING);

    }

    /*
     * Notify the device that we want to be informed on updates on the blood pressure data und control channel
     */
    protected void registerForNotifications() {
        Log.d(TAG, "registerForNotifications()");

        this.mDevice.queueRegisterForIndication(Constants.UUID_SERVICE_BLOOD_PRESSURE, Constants.UUID_CHARACTERISTIC_BLOOD_PRESSURE_DATA, Constants.UUID_DESCRIPTOR_CLIENT_CHARACTERISTICS_CONFIGURATION);
        this.mDevice.queueRegisterForIndication(Constants.UUID_SERVICE_BLOOD_PRESSURE, Constants.UUID_CHARACTERISTIC_INDICATE_CHALLENGE, Constants.UUID_DESCRIPTOR_CLIENT_CHARACTERISTICS_CONFIGURATION);

    }

}
