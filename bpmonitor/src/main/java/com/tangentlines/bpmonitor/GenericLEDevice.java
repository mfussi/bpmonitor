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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public abstract class GenericLEDevice {

    private static String TAG = GenericLEDevice.class.getSimpleName();

    private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;
    private Map<UUID, BluetoothGattService> mServices = new HashMap<>();

    private boolean mIsConnected = false;
    private int mConnectionSpeed = 400;

    protected GenericLEDevice(BluetoothDevice device) {
        mDevice = device;
    }

    protected boolean connectToGatt(Context context) {

        if (!mIsConnected && mBluetoothGatt == null) {

            mBluetoothGatt = mDevice.connectGatt(context, false, mGattCallback);
            mBluetoothGatt.connect();
            return true;

        } else {
            Log.d(TAG, "device already connected!");
        }

        return false;

    }

    protected boolean disconnectFromGatt() {

        if (mIsConnected && mBluetoothGatt != null) {

            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;

            mServices.clear();
            mBleRequest.clear();
            mIsConnected = false;

            onDisconnected();

            return true;
        } else {
            Log.d(TAG, "device already disconnected!");
        }

        return false;

    }

    protected void setConnectionSpeed(int speed) {
        this.mConnectionSpeed = speed;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    private void registerForIndication(@Nullable String serviceUUID, @NonNull String characteristicUUID, @NonNull String descriptorUUID) {
        writeDescriptor(mBluetoothGatt, serviceUUID, characteristicUUID, descriptorUUID, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
    }

    private void registerForNotification(@Nullable String serviceUUID, @NonNull String characteristicUUID, @NonNull String descriptorUUID) {
        writeDescriptor(mBluetoothGatt, serviceUUID, characteristicUUID, descriptorUUID, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }

    private void writeDescriptor(@NonNull BluetoothGatt gatt, @Nullable String serviceUUID, @NonNull String characteristicUUID, @NonNull String descriptorUUID, @NonNull byte[] data) {

        try {

            final BluetoothGattService service = mServices.get(UUID.fromString(serviceUUID));
            if (service == null) {
                Log.w(TAG, String.format("Service %s not found", characteristicUUID));
                return;
            }

            final BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
            if (characteristic == null) {
                Log.w(TAG, String.format("Characteristic %s not found", characteristicUUID));
                return;
            }

            gatt.setCharacteristicNotification(characteristic, true);

            final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUUID));
            if (descriptor == null) {
                Log.w(TAG, String.format("Descriptor %s not found", characteristicUUID));
                return;
            }

            descriptor.setValue(data);
            gatt.writeDescriptor(descriptor);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }


    private void writeCharacteristic(@Nullable String serviceUUID, @NonNull String characteristicUUID, byte[] data) {

        final BluetoothGattService service = mServices.get(UUID.fromString(serviceUUID));
        if (service == null) {
            Log.w(TAG, String.format("Service %s not found", characteristicUUID));
            return;
        }

        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
        if (characteristic == null) {
            Log.w(TAG, String.format("Characteristic %s not found", characteristicUUID));
            return;
        }

        try {
            characteristic.setValue(data);
            mBluetoothGatt.writeCharacteristic(characteristic);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void readCharacteristic(@Nullable String serviceUUID, @NonNull String characteristicUUID) {

        final BluetoothGattService service = mServices.get(UUID.fromString(serviceUUID));
        if (service == null) {
            Log.w(TAG, String.format("Service %s not found", characteristicUUID));
            return;
        }

        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
        if (characteristic == null) {
            Log.w(TAG, String.format("Characteristic %s not found", characteristicUUID));
            return;
        }

        try {
            mBluetoothGatt.readCharacteristic(characteristic);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (!mIsConnected && newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
                mIsConnected = true;
            } else if (mIsConnected && newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectFromGatt();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                mServices.clear();
                for (BluetoothGattService s : mBluetoothGatt.getServices()) {
                    mServices.put(s.getUuid(), s);
                }

                onConnected();

                mQueueHandler.postDelayed(mQueueRunnable, mConnectionSpeed);

            } else {

                onError("onServicesDiscovered received: " + status);

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("onCharacteristicRead(%s)", characteristic.getStringValue(0)));
                GenericLEDevice.this.onCharacteristicRead(characteristic);
            } else {
                Log.e(TAG, "onCharacteristicReadError()");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged()");
            GenericLEDevice.this.onCharacteristicChanged(characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead()");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite()");
            GenericLEDevice.this.onCharacteristicWrite(characteristic, status);
        }
    };

    @Override
    public String toString() {
        return mDevice != null ? mDevice.toString() : "unknown";
    }

    private static class BLERequest {

    }

    private static class ReadRequest extends BLERequest {
        String serviceUUID;
        String characteristicUUID;
    }

    private static class WriteRequest extends BLERequest {
        String serviceUUID;
        String characteristicUUID;
        byte[] data;
    }

    private static class IndicationRequest extends BLERequest {
        String serviceUUID;
        String characteristicUUID;
        String descriptorUUID;
    }

    private static class NotificationRequest extends BLERequest {
        String serviceUUID;
        String characteristicUUID;
        String descriptorUUID;
    }

    private Queue<BLERequest> mBleRequest = new LinkedList<>();

    public void queueReadCharacteristic(@Nullable String serviceUUID, @NonNull String characteristicUUID) {

        ReadRequest rr = new ReadRequest();
        rr.serviceUUID = serviceUUID;
        rr.characteristicUUID = characteristicUUID;
        mBleRequest.add(rr);

    }

    protected void queueWriteCharacteristic(@Nullable String serviceUUID, @NonNull String characteristicUUID, @NonNull byte[] data) {

        WriteRequest rr = new WriteRequest();
        rr.serviceUUID = serviceUUID;
        rr.characteristicUUID = characteristicUUID;
        rr.data = data;
        mBleRequest.add(rr);

    }

    public void queueRegisterForIndication(@Nullable String serviceUUID, @NonNull String characteristicUUID, @NonNull String descriptorUUID) {

        IndicationRequest r = new IndicationRequest();
        r.serviceUUID = serviceUUID;
        r.characteristicUUID = characteristicUUID;
        r.descriptorUUID = descriptorUUID;
        mBleRequest.add(r);

    }

    public void queueRegisterForNotification(@Nullable String serviceUUID, @NonNull String characteristicUUID, @NonNull String descriptorUUID) {

        NotificationRequest r = new NotificationRequest();
        r.serviceUUID = serviceUUID;
        r.characteristicUUID = characteristicUUID;
        r.descriptorUUID = descriptorUUID;
        mBleRequest.add(r);

    }


    private Handler mQueueHandler = new Handler();
    private Runnable mQueueRunnable = new Runnable() {

        @Override
        public void run() {

            /* it was disconnected */
            if (mDevice == null || !isConnected()) {
                return;
            }

            if (mBleRequest.size() > 0) {
                BLERequest rr = mBleRequest.poll();

                if (rr instanceof ReadRequest) {
                    readCharacteristic(((ReadRequest) rr).serviceUUID, ((ReadRequest) rr).characteristicUUID);
                } else if (rr instanceof WriteRequest) {
                    writeCharacteristic(((WriteRequest) rr).serviceUUID, ((WriteRequest) rr).characteristicUUID, ((WriteRequest) rr).data);
                } else if (rr instanceof IndicationRequest) {
                    registerForIndication(((IndicationRequest) rr).serviceUUID, ((IndicationRequest) rr).characteristicUUID, ((IndicationRequest) rr).descriptorUUID);
                } else if (rr instanceof NotificationRequest) {
                    registerForNotification(((NotificationRequest) rr).serviceUUID, ((NotificationRequest) rr).characteristicUUID, ((NotificationRequest) rr).descriptorUUID);
                }

            }

            mQueueHandler.postDelayed(mQueueRunnable, mConnectionSpeed);

        }

    };

    protected abstract void onConnected();

    protected abstract void onDisconnected();

    protected abstract void onCharacteristicRead(BluetoothGattCharacteristic characteristics);

    protected abstract void onCharacteristicChanged(BluetoothGattCharacteristic characteristic);

    protected abstract void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status);

    protected abstract void onError(String msg);

}
