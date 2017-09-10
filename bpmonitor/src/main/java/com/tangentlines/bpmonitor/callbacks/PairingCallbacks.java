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
package com.tangentlines.bpmonitor.callbacks;

import com.tangentlines.bpmonitor.BPMonitorError;
import com.tangentlines.bpmonitor.BPMonitor;
import com.tangentlines.bpmonitor.model.UserInformation;

import java.util.List;

/**
 * Created by markus on 24.08.17.
 */

public interface PairingCallbacks {

    void onUsersReceived(BPMonitor device, List<UserInformation> users);
    void onFinished(BPMonitor device, byte[] password, byte[] broadcastId);
    void onPairingFailed(BPMonitor device, BPMonitorError exception);

}
