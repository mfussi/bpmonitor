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
import android.support.annotation.NonNull;
import android.util.Log;

import com.tangentlines.bpmonitor.BPMonitor;
import com.tangentlines.bpmonitor.BPMonitorDataConverter;
import com.tangentlines.bpmonitor.BPMonitorError;
import com.tangentlines.bpmonitor.Constants;
import com.tangentlines.bpmonitor.callbacks.SynchronizationCallbacks;
import com.tangentlines.bpmonitor.model.BloodPressureReading;
import com.tangentlines.bpmonitor.utils.ListUtils;

import java.util.Arrays;

/**
 * Created by markus on 02.09.17.
 */

public class SynchronizeProtocol extends GenericProtocol {

    private static final String TAG = SynchronizeProtocol.class.getSimpleName();

    @NonNull private final byte[] mBroadcastId;
    @NonNull private final byte[] mPassword;
    @NonNull private final SynchronizationCallbacks mCallbacks;

    public SynchronizeProtocol(BPMonitor device, @NonNull byte[] password, @NonNull byte[] broadcastId, @NonNull SynchronizationCallbacks callbacks) {
        super(device);
        this.mBroadcastId = broadcastId;
        this.mPassword = password;
        this.mCallbacks = callbacks;
    }

    @Override
    public void start() {

        mDevice.setDeviceState(BPMonitor.DeviceState.SYNCHRONIZING);
        mDevice.getDeviceInformation().setPassword(mPassword);
        mDevice.getDeviceInformation().setBroadcastId(mBroadcastId);

        super.requestDeviceInformation();
        super.onStart();

    }

    @Override
    public boolean handleCharacteristicRead(BluetoothGattCharacteristic characteristics) {
        return super.handleCharacteristicRead(characteristics);
    }

    @Override
    public boolean handleCharacteristicChanged(BluetoothGattCharacteristic characteristic) {

        final String uuid = characteristic.getUuid().toString();

        switch (uuid) {

            case Constants.UUID_CHARACTERISTIC_BLOOD_PRESSURE_DATA:
                handleDataChannelResponse(characteristic.getValue());
                break;

            case Constants.UUID_CHARACTERISTIC_INDICATE_CHALLENGE:
                handleControlChannelResponse(characteristic.getValue());
                break;

            default:
                return super.handleCharacteristicChanged(characteristic);

        }

        return false;

    }

    @Override
    public boolean handleCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {

        if (status == 0) {
            /* we have an error */
            /* notify and disconnect */
        }

        final String uuid = characteristic.getUuid().toString();
        final byte[] value = characteristic.getValue();

        if (uuid.equals(Constants.UUID_CHARACTERISTIC_CONTROL) && value[0] == (byte) Constants.CMD_FINISH) {

            onFinished();

            if(mDevice.isConnected()) {
                mNotifyHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallbacks.onSynchronizationStarted(mDevice);
                    }
                });
            }


            return true;

        }

        return super.handleCharacteristicWrite(characteristic, status);
    }

    @Override
    protected void onSetupFinished() {
        super.registerForNotifications();
    }

    @Override
    protected void fireError(final BPMonitorError error) {
        onError();
        mNotifyHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallbacks.onSynchronizationFailed(mDevice, error);
            }
        });
    }

    private void startSynchronizationProcess(byte[] challenge) {

        if (super.mDevice.getDeviceInformation().getPassword() == null) {
            fireError(new BPMonitorError(BPMonitorError.TYPE_ILLEGAL_ARGUMENT, "no password available, cannot upload data"));
            return;
        }

        if (this.mDevice.getDeviceInformation().getBroadcastId() == null) {
            fireError(new BPMonitorError(BPMonitorError.TYPE_ILLEGAL_ARGUMENT, "no broadcastId available, cannot upload data"));
            return;
        }

        this.mDevice.sendControlSequence((byte) Constants.CMD_SEND_XOR, BPMonitorDataConverter.calculateChallengeResponse(challenge, super.mDevice.getDeviceInformation().getPassword()));
        this.mDevice.sendControlSequence((byte) Constants.CMD_SEND_BROADCAST_ID, this.mDevice.getDeviceInformation().getBroadcastId());
        this.mDevice.sendControlSequence((byte) Constants.CMD_SEND_TIME, BPMonitorDataConverter.currentDateTimeAsBytes());
        this.mDevice.sendControlSequence((byte) Constants.CMD_FINISH);

    }

    private void handleControlChannelResponse(byte[] bytes) {

        if (bytes.length > 0) {

            byte cmd = bytes[0];
            byte[] data = Arrays.copyOfRange(bytes, 1, bytes.length);

            switch (cmd) {

                case Constants.RESPONSE_CHALLENGE:
                    Log.i(TAG, "Challenge received!");
                    onChallengeReceived(data);
                    return;


            }

        }

    }

    private void handleDataChannelResponse(byte[] bytes) {

        if (bytes.length > 0) {

            final BloodPressureReading measurement = BPMonitorDataConverter.bytesToBloodPressureData(bytes);

            if (measurement != null && mDevice.isConnected()) {
                mNotifyHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallbacks.onReadingReceived(mDevice, measurement);
                    }
                });

            }

        }

    }

    private void onChallengeReceived(byte[] challenge) {
        Log.d(TAG, "onChallengeReceived()");
        startSynchronizationProcess(challenge);
    }

}
