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
import com.tangentlines.bpmonitor.callbacks.PairingCallbacks;
import com.tangentlines.bpmonitor.model.UserInformation;
import com.tangentlines.bpmonitor.utils.ListUtils;

import java.util.Arrays;

public class PairingProtocol extends GenericProtocol {

    private static final String TAG = PairingProtocol.class.getSimpleName();

    @NonNull
    private final PairingCallbacks mCallbacks;

    public PairingProtocol(BPMonitor device, @NonNull PairingCallbacks callbacks) {
        super(device);
        this.mCallbacks = callbacks;
    }

    @Override
    public void start() {

        mDevice.setDeviceState(BPMonitor.DeviceState.PAIRING);

        super.onStart();
        super.requestDeviceInformation();

    }

    public void selectUser(int userId, String name) {

        if (userId < 1 || userId > 2) {
            fireError(new BPMonitorError(BPMonitorError.TYPE_ILLEGAL_ARGUMENT, "userId needs to be 1 or 2"));
            return;
        }

        super.onStart();

        UserInformation userInformation = new UserInformation(userId, name);
        this.mDevice.sendControlSequence((byte) Constants.CMD_SET_USER, BPMonitorDataConverter.userInformationAsBytes(userInformation));
        this.mDevice.sendControlSequence((byte) Constants.CMD_SELECT_USER, BPMonitorDataConverter.selectUserAsBytes(userId));
        this.mDevice.sendControlSequence((byte) Constants.CMD_SEND_TIME, BPMonitorDataConverter.currentDateTimeAsBytes());
        this.mDevice.sendControlSequence((byte) Constants.CMD_FINISH, new byte[0]);

    }

    @Override
    public boolean handleCharacteristicRead(BluetoothGattCharacteristic characteristics) {
        super.handleCharacteristicRead(characteristics);
        return false;
    }

    @Override
    public boolean handleCharacteristicChanged(BluetoothGattCharacteristic characteristic) {

        final String uuid = characteristic.getUuid().toString();

        switch (uuid) {

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

            super.onFinished();

            if (mDevice.isConnected()) {
                mNotifyHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallbacks.onFinished(mDevice, mDevice.getDeviceInformation().getPassword(), mDevice.getDeviceInformation().getBroadcastId());
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
        super.onError();
        mNotifyHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallbacks.onPairingFailed(mDevice, error);
            }
        });
    }

    private void startPairingProcess(byte[] challenge) {

        if (this.mDevice.getDeviceInformation().getPassword() == null) {
            fireError(new BPMonitorError(BPMonitorError.TYPE_ILLEGAL_ARGUMENT, "no password available, cannot start pairing"));
            return;
        }

        this.mDevice.sendControlSequence((byte) Constants.CMD_SEND_XOR, BPMonitorDataConverter.calculateChallengeResponse(challenge, this.mDevice.getDeviceInformation().getPassword()));

    }

    private void sendBroadcastId() {

        byte[] broadcastId = BPMonitorDataConverter.randomBroadcastId();

        this.mDevice.getDeviceInformation().setBroadcastId(broadcastId);
        this.mDevice.sendControlSequence((byte) Constants.CMD_SEND_BROADCAST_ID, broadcastId);

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

                case Constants.RESPONSE_PASSWORD:
                    Log.i(TAG, "Password received");
                    this.mDevice.getDeviceInformation().setPassword(data);
                    sendBroadcastId();
                    return;

                case Constants.RESPONSE_GET_USER:
                    Log.i(TAG, "User received");
                    if (mDevice.getDeviceInformation().addUser(BPMonitorDataConverter.bytesAsUserInformation(data))) {

                        super.onFinished();

                        if (mDevice.isConnected()) {
                            mNotifyHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mCallbacks.onUsersReceived(mDevice, ListUtils.asList(mDevice.getDeviceInformation().getUsers()));
                                }
                            });
                        }


                    }
                    return;

            }

        }

    }

    private void onChallengeReceived(byte[] challenge) {
        Log.d(TAG, "onChallengeReceived()");
        startPairingProcess(challenge);
    }

}
