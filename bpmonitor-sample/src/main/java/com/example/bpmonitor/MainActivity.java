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

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.tangentlines.bpmonitor.BPMonitorError;
import com.tangentlines.bpmonitor.BPMonitor;
import com.tangentlines.bpmonitor.model.BloodPressureReading;
import com.tangentlines.bpmonitor.callbacks.ConnectionCallbacks;
import com.tangentlines.bpmonitor.callbacks.PairingCallbacks;
import com.tangentlines.bpmonitor.callbacks.SynchronizationCallbacks;
import com.tangentlines.bpmonitor.model.UserInformation;
import com.tangentlines.bpmonitor.utils.BinaryUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ViewHolder mViews;
    private DeviceAdapter mDeviceAdapter;
    private BPMonitor mDevice;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.bpmonitor.R.layout.activity_main);

        mViews = new ViewHolder(this);

        mDeviceAdapter = new DeviceAdapter(this);
        mViews.deviceListView.setAdapter(mDeviceAdapter);

        mViews.deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                connect(mDeviceAdapter.getItem(i));
            }

        });

        mViews.btnPair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPairing();
            }
        });

        mViews.btnSynchronize.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startSynchronization();
            }

        });

        mViews.btnDisconnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                disconnect();
            }
        });

        bindToBluetoothDiscoveryService();
        hideProgress();
        updateUI();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private void addOutput(final String line) {
        mViews.tvOutput.setText(mViews.tvOutput.getText().length() > 0 ? mViews.tvOutput.getText() + "\n" + line : line);
    }

    private void connect(BluetoothDevice device) {
        addOutput("Connecting to " + device.getName());

        if (mDevice == null) {

            mDevice = new BPMonitor.Builder()
                    .with(device)
                    .setConnectionCallbacks(mConnectionCallbacks)
                    .create();

            mDevice.connect(this);

            showProgress();
            updateUI();

        } else {
            addOutput("Already connected to a device");
        }

    }

    private void disconnect() {

        if (mDevice == null) {
            addOutput("No device selected");
            return;
        }

        showProgress();
        mDevice.disconnect();
        updateUI();

    }

    private void startPairing() {

        if (mDevice == null) {
            addOutput("No device selected");
            return;
        }

        showProgress();
        mDevice.startPairing(mPairingCallbacks);
        updateUI();

    }

    private void startSynchronization() {

        if (mDevice == null) {
            addOutput("No device selected");
            return;
        }

        String boundMacAddress = LocalStorage.getMacAddress(this);
        String boundPassword = LocalStorage.getPassword(this);
        String boundBroadcastId = LocalStorage.getBroadcastId(this);

        if (TextUtils.isEmpty(boundMacAddress) || !mDevice.getDeviceInformation().getMacAddress().equals(boundMacAddress)) {
            addOutput("cannot synchronize - pairing required");
            return;
        }

        showProgress();
        mDevice.startSynchronization(BinaryUtils.hexStringToByteArray(boundPassword), BinaryUtils.hexStringToByteArray(boundBroadcastId), mSynchronizationCallbacks);
        updateUI();

    }

    private void selectUser(int id, String name) {

        if (mDevice == null) {
            addOutput("No device selected");
            return;
        }

        showProgress();
        mDevice.selectUser(id, name);
        updateUI();

    }

    private void showSetUserDialog() {

        final View view = LayoutInflater.from(this).inflate(com.example.bpmonitor.R.layout.dialog_set_user, null, false);

        final Spinner spUserId = view.findViewById(com.example.bpmonitor.R.id.spUserId);
        String[] items = new String[]{"UserId: 1", "UserId: 2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        spUserId.setAdapter(adapter);

        final EditText etUserName = view.findViewById(com.example.bpmonitor.R.id.etUserName);
        etUserName.setText("Your Name");

        new AlertDialog.Builder(this)
                .setTitle("User Selection")
                .setView(view)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectUser(spUserId.getSelectedItemPosition() + 1, etUserName.getText().toString());
                    }

                })
                .show();

    }

    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {

        @Override
        public void onConnected(BPMonitor device) {

            addOutput("Successfully connected!");
            hideProgress();
            updateUI();

        }

        @Override
        public void onConnectionFailed(BPMonitor device, BPMonitorError exception) {
            addOutput(String.format("Failed to connect: %s", exception.toString()));
        }

        @Override
        public void onDisconnected(BPMonitor device) {

            addOutput("Disconnected!");
            mDevice = null;
            hideProgress();
            updateUI();

        }

    };

    private PairingCallbacks mPairingCallbacks = new PairingCallbacks() {

        @Override
        public void onUsersReceived(BPMonitor device, List<UserInformation> users) {

            for (UserInformation info : users) {
                addOutput(info.toString());
            }

            showSetUserDialog();
            hideProgress();

        }

        @Override
        public void onFinished(BPMonitor device, byte[] password, byte[] broadcastId) {

            addOutput("Device: " + device.getDeviceInformation().getManufacturer() + " " + device.getDeviceInformation().getModelNumber());
            addOutput("Serial number: " + device.getDeviceInformation().getSerialNumber());
            addOutput("Firmware version: " + device.getDeviceInformation().getFirmwareRevision());

            addOutput(String.format("Pairing to %s completed", device.getDeviceInformation().getModelNumber()));
            addOutput("Password: 0x" + BinaryUtils.bytesToHex(password));
            addOutput("BroadcastId: 0x" + BinaryUtils.bytesToHex(broadcastId));

            /* storing pairing information for future use */
            LocalStorage.storeDevice(MainActivity.this,
                    mDevice.getDeviceInformation().getMacAddress(),
                    BinaryUtils.bytesToHex(password),
                    BinaryUtils.bytesToHex(broadcastId));

            hideProgress();
            updateUI();

        }

        @Override
        public void onPairingFailed(BPMonitor device, BPMonitorError exception) {
            addOutput(String.format("Failed to pair: %s", exception.getMessage()));
            hideProgress();
        }

    };

    private SynchronizationCallbacks mSynchronizationCallbacks = new SynchronizationCallbacks() {

        @Override
        public void onSynchronizationStarted(BPMonitor device) {

            addOutput("Device: " + device.getDeviceInformation().getManufacturer() + " " + device.getDeviceInformation().getModelNumber());
            addOutput("Serial number: " + device.getDeviceInformation().getSerialNumber());
            addOutput("Firmware version: " + device.getDeviceInformation().getFirmwareRevision());

            addOutput(String.format("Sync on %s started. Waiting for measurements", device.getDeviceInformation().getModelNumber()));
            hideProgress();

        }

        @Override
        public void onReadingReceived(BPMonitor device, BloodPressureReading reading) {
            addOutput(reading.toString());
        }

        @Override
        public void onSynchronizationFailed(BPMonitor device, BPMonitorError exception) {
            addOutput(String.format("Failed to set up synchronization: %s", exception.getMessage()));
            hideProgress();
        }

    };

    private void updateUI() {

        String name = "";

        if (mDevice != null) {
            name = mDevice.getDeviceInformation().getMacAddress();
            if (!TextUtils.isEmpty(mDevice.getDeviceInformation().getManufacturer()) && !TextUtils.isEmpty(mDevice.getDeviceInformation().getModelNumber())) {
                name = mDevice.getDeviceInformation().getManufacturer() + " " + mDevice.getDeviceInformation().getModelNumber();
            }
        }

        mViews.tvConnectedDevice.setText(mDevice != null ? name : "none");
        mViews.tvState.setText(String.format("State: %s", mDevice != null && mDevice.getDeviceState() != null ? mDevice.getDeviceState().name() : "Unknown"));

        if (mDevice == null || mDevice.getDeviceState() == BPMonitor.DeviceState.DISCONNECTED) {

            mViews.btnDisconnect.setEnabled(false);
            mViews.btnPair.setEnabled(false);
            mViews.btnSynchronize.setEnabled(false);

        } else {

            mViews.btnDisconnect.setEnabled(true);
            mViews.btnPair.setEnabled(true);
            mViews.btnSynchronize.setEnabled(true);

        }

    }

    private BluetoothDiscoveryService mBluetoothDiscoveryService;
    private boolean mIsBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {

            mBluetoothDiscoveryService = ((BluetoothDiscoveryService.LocalBinder) service).getService();
            mBluetoothDiscoveryService.addListener(mBluetoothDeviceDiscoveryListener);

            mDeviceAdapter.addAll(mBluetoothDiscoveryService.getDiscoveredDevices());
            mDeviceAdapter.notifyDataSetChanged();

            mBluetoothDiscoveryService.startSearching("0DL87651", "1DL87651");

        }

        public void onServiceDisconnected(ComponentName className) {

            mBluetoothDiscoveryService.removeListener(mBluetoothDeviceDiscoveryListener);
            mBluetoothDiscoveryService = null;

            mDeviceAdapter.clear();
            mDeviceAdapter.notifyDataSetChanged();

        }

    };

    void bindToBluetoothDiscoveryService() {
        bindService(new Intent(this, BluetoothDiscoveryService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

    }

    private final BluetoothDiscoveryService.BluetoothDiscoveryListener mBluetoothDeviceDiscoveryListener = new BluetoothDiscoveryService.BluetoothDiscoveryListener() {

        @Override
        public void onDeviceDiscovered(BluetoothDevice device) {
            mDeviceAdapter.add(device);
            mDeviceAdapter.notifyDataSetChanged();
        }

        @Override
        public void onDeviceLost(BluetoothDevice device) {
            mDeviceAdapter.remove(device);
            mDeviceAdapter.notifyDataSetChanged();
        }

    };

    private void showProgress() {
        mViews.progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mViews.progressBar.setVisibility(View.INVISIBLE);
    }

    private class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {

        public DeviceAdapter(@NonNull Context context) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = super.getView(position, convertView, parent);
            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
            TextView text2 = (TextView) view.findViewById(android.R.id.text2);

            final BluetoothDevice device = getItem(position);
            if (device != null) {
                text1.setText(device.getName() != null ? device.getName() : "unknown");
                text2.setText(device.getAddress());
            }

            return view;

        }

    }

    class ViewHolder {

        Button btnPair;
        Button btnSynchronize;
        Button btnDisconnect;

        TextView tvConnectedDevice;
        TextView tvState;
        TextView tvOutput;
        ListView deviceListView;

        ProgressBar progressBar;

        ViewHolder(Activity activity) {

            btnPair = activity.findViewById(com.example.bpmonitor.R.id.btnPair);
            btnSynchronize = activity.findViewById(com.example.bpmonitor.R.id.btnSynchronize);
            btnDisconnect = activity.findViewById(com.example.bpmonitor.R.id.btnDisconnect);

            tvConnectedDevice = findViewById(com.example.bpmonitor.R.id.tvConnectedDevice);
            tvState = findViewById(com.example.bpmonitor.R.id.tvState);
            tvOutput = findViewById(com.example.bpmonitor.R.id.tvOutput);
            deviceListView = findViewById(com.example.bpmonitor.R.id.list);

            progressBar = findViewById(com.example.bpmonitor.R.id.progressBar);

        }

    }

}