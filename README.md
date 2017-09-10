bpmonitor
========

Android library to read blood pressure data from a Philips DL8765 bluetooth device

Description
--------
This library allows the user to connect, pair and synchronize wiht a Philips DL8765 blood pressure device. Connection is established over bluetooth.

Features:
* Read device information
* Pair with device (list users and select user)
* Read recorded blood pressure measurements

Supported Hardware
--------
Currently only the DL8765 device by Philips is supported.

Download
--------
gradle:
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
```
dependencies {
  compile 'com.github.mfussi:bpmonitor:v0.8.0'
}
```

How to use
--------
Connect to a device:
```
mDevice = new BPMonitor.Builder()
                    .with(device)
                    .setConnectionCallbacks(mConnectionCallbacks)
                    .create();

mDevice.connect(this);
```
```
private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {

        @Override
        public void onConnected(BPMonitor device) {
            // do something here
        }

        @Override
        public void onConnectionFailed(BPMonitor device, BPMonitorError exception) {
            // do something here
        }

        @Override
        public void onDisconnected(BPMonitor device) {
            // do something here
        }

    };
```        
After the device is connected you can either pair or synchronize data. Before synchronizing a pairing is required as you need to obtain the password and broadcastID of the device and user. Each user has its own broadcastID, which is used to distinguish between the users.

Start pairing:

```
mDevice.startPairing(mPairingCallbacks);
```
```
private PairingCallbacks mPairingCallbacks = new PairingCallbacks() {

        @Override
        public void onUsersReceived(BPMonitor device, List<UserInformation> users) {
            // list users, after one user is selected, call:
            // mDevice.selectUser(id, name)
        }

        @Override
        public void onFinished(BPMonitor device, byte[] password, byte[] broadcastId) {
            // store pasword and broadcastId for later use
        }

        @Override
        public void onPairingFailed(BPMonitor device, BPMonitorError exception) {
            // do something
        }

    };
```
Start synchronization: 
Use password and broadcastId obtained in the pairing process.
```
mDevice.startSynchronization(password, broadcastId, mSynchronizationCallbacks);
```
```
private SynchronizationCallbacks mSynchronizationCallbacks = new SynchronizationCallbacks() {

        @Override
        public void onSynchronizationStarted(BPMonitor device) {
            // device will now return blood pressure data if there are any
        }

        @Override
        public void onReadingReceived(BPMonitor device, BloodPressureReading reading) {
            // new blood pressure data received
        }

        @Override
        public void onSynchronizationFailed(BPMonitor device, BPMonitorError exception) {
            // do something
        }

    };
```

License
=======

    Copyright 2017 Markus Fußenegger

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
