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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.tangentlines.bpmonitor.callbacks.ConnectionCallbacks;
import com.tangentlines.bpmonitor.callbacks.PairingCallbacks;
import com.tangentlines.bpmonitor.callbacks.SynchronizationCallbacks;
import com.tangentlines.bpmonitor.model.DeviceInformation;
import com.tangentlines.bpmonitor.protocol.PairingProtocol;
import com.tangentlines.bpmonitor.protocol.Protocol;
import com.tangentlines.bpmonitor.protocol.ProtocolListener;
import com.tangentlines.bpmonitor.protocol.SynchronizeProtocol;
import com.tangentlines.bpmonitor.utils.BinaryUtils;

public class BPMonitor extends GenericLEDevice implements ProtocolListener {

    private static final String TAG = BPMonitor.class.getSimpleName();

    private static final int TIMEOUT = 1000 * 15; // 10 seconds

    private DeviceInformation mDeviceInformation;
    private DeviceState mCurrentDeviceState = DeviceState.DISCONNECTED;

    private ConnectionCallbacks mConnectionCallbacks;

    private final Handler mNotifyHandler = new Handler();
    private final Handler mTimeoutHandler = new Handler();

    private Protocol mProtocol;

    protected BPMonitor(BluetoothDevice device) {
        super(device);

        mDeviceInformation = new DeviceInformation();
        mDeviceInformation.setMacAddress(device.getAddress());

    }

    public void sendControlSequence(byte cmd) {
        sendControlSequence(cmd, new byte[0]);
    }

    public void sendControlSequence(byte cmd, @NonNull byte[] data) {

        byte[] output = new byte[data.length + 1];
        output[0] = cmd;
        for (int i = 0; i < data.length; i++) {
            output[i + 1] = data[i];
        }

        Log.d(TAG, String.format("sendControlSequence(%s)", BinaryUtils.bytesToHex(output)));
        super.queueWriteCharacteristic(Constants.UUID_SERVICE_BLOOD_PRESSURE, Constants.UUID_CHARACTERISTIC_CONTROL, output);

    }

    /*
     * Initially connect to the bluetooth device, and
     * discover its GATT services
     */
    public void connect(Context context) {

        if (super.connectToGatt(context)) {
            mCurrentDeviceState = DeviceState.CONNECTING;
            mTimeoutHandler.postDelayed(mTimeoutRunnable, TIMEOUT);
        }

    }

    public void disconnect() {
        super.disconnectFromGatt();
    }

    /*
     * Starts the pairing mode
     * After pairing the broadcastId and password are returned to the user
     */
    public void startPairing(@NonNull PairingCallbacks callbacks) {

        mProtocol = new PairingProtocol(this, callbacks);
        mProtocol.setProtocolListener(this);
        mProtocol.start();

    }

    /*
     * Starts the synchronization process
     * password and broadcastId were obtained while pairing
     * If the synchronization process is successfully, the device will return its blood pressure readings
     */
    public void startSynchronization(byte[] password, byte[] broadcastId, @NonNull SynchronizationCallbacks callbacks) {

        mProtocol = new SynchronizeProtocol(this, password, broadcastId, callbacks);
        mProtocol.setProtocolListener(this);
        mProtocol.start();

    }

    /*
     * Selects the user if the device is in pairing mode
     */
    public void selectUser(int id, String name) {

        if (mProtocol != null && mProtocol instanceof PairingProtocol) {
            ((PairingProtocol) mProtocol).selectUser(id, name);
        } else {
            Log.e(TAG, "not in pairing mode");
        }

    }

    @Override
    protected void onConnected() {

        mCurrentDeviceState = DeviceState.CONNECTED;
        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);

        if (mConnectionCallbacks != null) {
            mNotifyHandler.post(new Runnable() {
                @Override
                public void run() {
                    mConnectionCallbacks.onConnected(BPMonitor.this);
                }
            });
        }

    }

    @Override
    protected void onDisconnected() {

        mProtocol = null;
        mCurrentDeviceState = DeviceState.DISCONNECTED;
        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);

        if (mCurrentDeviceState == DeviceState.PAIRING || mCurrentDeviceState == DeviceState.SYNCHRONIZING) {
            fireError(new BPMonitorError(BPMonitorError.TYPE_UNEXPECTED_DISCONNECT, "device unexpected disconnected"));
        }

        if (mConnectionCallbacks != null) {
            mNotifyHandler.post(new Runnable() {
                @Override
                public void run() {
                    mConnectionCallbacks.onDisconnected(BPMonitor.this);
                }
            });
        }

    }

    @Override
    protected void onCharacteristicRead(BluetoothGattCharacteristic characteristics) {

        if (mProtocol != null) {
            mProtocol.handleCharacteristicRead(characteristics);
        }

    }

    @Override
    protected void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {

        if (mProtocol != null) {
            mProtocol.handleCharacteristicChanged(characteristic);
        }

    }

    @Override
    protected void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {

        if (mProtocol != null) {
            mProtocol.handleCharacteristicWrite(characteristic, status);
        }

    }

    @Override
    protected void onError(final String msg) {
        fireError(new BPMonitorError(BPMonitorError.TYPE_UNKNOWN, !TextUtils.isEmpty(msg) ? msg : "unknown error"));
    }

    private void fireError(@NonNull final BPMonitorError error) {

        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
        if (mConnectionCallbacks != null) {
            mNotifyHandler.post(new Runnable() {
                @Override
                public void run() {
                    mConnectionCallbacks.onConnectionFailed(BPMonitor.this, error);
                }
            });
        }

    }

    public DeviceInformation getDeviceInformation() {
        return mDeviceInformation;
    }

    public DeviceState getDeviceState() {
        return mCurrentDeviceState;
    }

    void setConnectionCallbacks(ConnectionCallbacks connectionCallbacks) {
        this.mConnectionCallbacks = connectionCallbacks;
    }

    public void setDeviceState(DeviceState deviceState) {
        this.mCurrentDeviceState = deviceState;
    }

    @Override
    public void onProtocolStarted() {
        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
        mTimeoutHandler.postDelayed(mTimeoutRunnable, TIMEOUT);
    }

    @Override
    public void onProtocolFinished() {
        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
    }

    @Override
    public void onProtocolError() {
        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
    }

    public enum DeviceState {
        PAIRING, SYNCHRONIZING, DISCONNECTED, CONNECTING, CONNECTED;
    }

    private final Runnable mTimeoutRunnable = new Runnable() {

        @Override
        public void run() {
            fireError(new BPMonitorError(BPMonitorError.TYPE_TIMEOUT, "timeout"));
        }

    };

    public static class Builder {

        private BluetoothDevice bleDevice;
        private ConnectionCallbacks connectionCallbacks;
        private int connectionSpeed;

        public Builder() {

        }

        public Builder with(BluetoothDevice bleDevice) {
            this.bleDevice = bleDevice;
            return this;
        }

        public Builder setConnectionCallbacks(ConnectionCallbacks connectionCallbacks) {
            this.connectionCallbacks = connectionCallbacks;
            return this;
        }

        public Builder setConnectionSpeed(int connectionSpeed) {
            this.connectionSpeed = connectionSpeed;
            return this;
        }

        public BPMonitor create() {

            if (bleDevice == null) {
                throw new IllegalArgumentException("with() must be called with a valid bluetooth le device");
            }

            if (connectionCallbacks == null) {
                throw new IllegalArgumentException("connection callbacks has to be set");
            }

            BPMonitor device = new BPMonitor(bleDevice);
            device.setConnectionCallbacks(connectionCallbacks);

            if (connectionSpeed != 0) {
                device.setConnectionSpeed(connectionSpeed);
            }

            return device;

        }

    }

}
