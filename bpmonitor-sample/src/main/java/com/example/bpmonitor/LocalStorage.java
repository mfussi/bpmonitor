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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LocalStorage {

    private static final String PREF_MAC_ADDRESS = "pref.mac.address";
    private static final String PREF_PASSWORD = "pref.password";
    private static final String PREF_BROADCAST_ID = "pref.broadcast.id";


    public static void storeDevice(Context context, String macAddress, String password, String broadcastId){

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(PREF_MAC_ADDRESS, macAddress);
        editor.putString(PREF_PASSWORD, password);
        editor.putString(PREF_BROADCAST_ID, broadcastId);
        editor.apply();

    }

    public static String getMacAddress(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_MAC_ADDRESS, null);
    }

    public static String getPassword(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PASSWORD, null);
    }

    public static String getBroadcastId(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_BROADCAST_ID, null);
    }

}
