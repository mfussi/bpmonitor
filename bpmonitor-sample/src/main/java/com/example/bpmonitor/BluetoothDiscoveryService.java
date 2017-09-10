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
package com.example.bpmonitor;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class BluetoothDiscoveryService extends Service {

    private static final String TAG = BluetoothDiscoveryService.class.getSimpleName();

    private static final int DISCOVERY_DEVICE_TIMEOUT = 1000 * 5;

    private BluetoothAdapter mBluetoothAdapter;
    private Map<String, MyBluetoothDevice> mDiscoveredDevices = new HashMap<>();
    private boolean isStopped = false;
    private boolean isScanning = false;

    private Set<String> mNameFilter = new HashSet<>();

    private List<BluetoothDiscoveryListener> mListener = new CopyOnWriteArrayList<>();

    public class LocalBinder extends Binder {
        BluetoothDiscoveryService getService() {
            return BluetoothDiscoveryService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopSearching();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    public void startSearching(String... nameFilter) {

        mNameFilter.clear();

        if(nameFilter != null) {
            mNameFilter.addAll(Arrays.asList(nameFilter));
        }

        if (!isScanning && mBluetoothAdapter != null) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            mDeviceTimeoutHandler.post(mDeviceTimeoutRunnable);
            isScanning = true;
        } else {
            /* error handling */
        }

    }

    public void stopSearching() {

        if (isScanning && mBluetoothAdapter != null) {
            isStopped = true;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mDeviceTimeoutHandler.removeCallbacks(mDeviceTimeoutRunnable);
            isScanning = false;
        }

    }

    public List<BluetoothDevice> getDiscoveredDevices() {

        List<BluetoothDevice> devices = new ArrayList<>(mDiscoveredDevices.size());
        for (MyBluetoothDevice d : mDiscoveredDevices.values()) {
            devices.add(d.device);
        }
        return devices;

    }

    public void addListener(BluetoothDiscoveryListener listener) {
        mListener.add(listener);
    }

    public void removeListener(BluetoothDiscoveryListener listener) {
        mListener.remove(listener);
    }

    private void fireDeviceDiscovered(MyBluetoothDevice device) {

        for (BluetoothDiscoveryListener l : mListener) {
            l.onDeviceDiscovered(device.device);
        }

    }

    private void fireDeviceLost(MyBluetoothDevice device) {

        for (BluetoothDiscoveryListener l : mListener) {
            l.onDeviceLost(device.device);
        }

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =

            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                    String name = device.getName();
                    if (mDiscoveredDevices.containsKey(device.getAddress())) {
                        mDiscoveredDevices.get(device.getAddress()).discoveryTime = System.currentTimeMillis();
                    } else if(mNameFilter.size() == 0 || mNameFilter.contains(device.getName())){

                        MyBluetoothDevice myBluetoothDevice = new MyBluetoothDevice();
                        myBluetoothDevice.device = device;
                        myBluetoothDevice.discoveryTime = System.currentTimeMillis();
                        mDiscoveredDevices.put(device.getAddress(), myBluetoothDevice);

                        fireDeviceDiscovered(myBluetoothDevice);

                    }

                }

            };


    private final Handler mDeviceTimeoutHandler = new Handler();
    private final Runnable mDeviceTimeoutRunnable = new Runnable() {

        @Override
        public void run() {

            List<String> lostDevices = new ArrayList<>();
            long currentTime = System.currentTimeMillis();

            for (String key : mDiscoveredDevices.keySet()) {
                if (currentTime > mDiscoveredDevices.get(key).discoveryTime + DISCOVERY_DEVICE_TIMEOUT) {
                    lostDevices.add(key);
                }
            }

            for (String macAddress : lostDevices) {

                MyBluetoothDevice lostDevice = mDiscoveredDevices.get(macAddress);
                mDiscoveredDevices.remove(macAddress);

                fireDeviceLost(lostDevice);

            }

            if (!isStopped) {
                mDeviceTimeoutHandler.postDelayed(mDeviceTimeoutRunnable, 1000);
            }

        }

    };

    private class MyBluetoothDevice {

        private BluetoothDevice device;
        private long discoveryTime;

    }

    public interface BluetoothDiscoveryListener {
        void onDeviceDiscovered(BluetoothDevice device);
        void onDeviceLost(BluetoothDevice device);
    }

}
