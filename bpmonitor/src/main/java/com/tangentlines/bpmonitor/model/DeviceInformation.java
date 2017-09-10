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
package com.tangentlines.bpmonitor.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import java.util.Arrays;

public class DeviceInformation implements Parcelable {

    private String macAddress;
    private String modelNumber;
    private String serialNumber;
    private String firmwareRevision;
    private String hardwareRevision;
    private String softwareRevision;
    private String manufacturer;
    private byte[] password;
    private byte[] broadcastId;
    private SparseArray<UserInformation> users = new SparseArray<>();

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getFirmwareRevision() {
        return firmwareRevision;
    }

    public void setFirmwareRevision(String firmwareRevision) {
        this.firmwareRevision = firmwareRevision;
    }

    public String getHardwareRevision() {
        return hardwareRevision;
    }

    public void setHardwareRevision(String hardwareRevision) {
        this.hardwareRevision = hardwareRevision;
    }

    public String getSoftwareRevision() {
        return softwareRevision;
    }

    public void setSoftwareRevision(String softwareRevision) {
        this.softwareRevision = softwareRevision;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public byte[] getBroadcastId() {
        return broadcastId;
    }

    public void setBroadcastId(byte[] broadcastId) {
        this.broadcastId = broadcastId;
    }

    public SparseArray<UserInformation> getUsers() {
        return users;
    }

    public boolean addUser(UserInformation user) {

        if (user != null) {

            if (users.get(user.getId()) != null) {
                return true;
            }

            users.put(user.getId(), user);
        }

        return false;

    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.macAddress);
        dest.writeString(this.modelNumber);
        dest.writeString(this.serialNumber);
        dest.writeString(this.firmwareRevision);
        dest.writeString(this.hardwareRevision);
        dest.writeString(this.softwareRevision);
        dest.writeString(this.manufacturer);
        dest.writeByteArray(this.password);
        dest.writeByteArray(this.broadcastId);
        dest.writeSparseArray((SparseArray) this.users);
    }

    public DeviceInformation() {
    }

    protected DeviceInformation(Parcel in) {
        this.macAddress = in.readString();
        this.modelNumber = in.readString();
        this.serialNumber = in.readString();
        this.firmwareRevision = in.readString();
        this.hardwareRevision = in.readString();
        this.softwareRevision = in.readString();
        this.manufacturer = in.readString();
        this.password = in.createByteArray();
        this.broadcastId = in.createByteArray();
        this.users = in.readSparseArray(UserInformation.class.getClassLoader());
    }

    public static final Creator<DeviceInformation> CREATOR = new Creator<DeviceInformation>() {
        @Override
        public DeviceInformation createFromParcel(Parcel source) {
            return new DeviceInformation(source);
        }

        @Override
        public DeviceInformation[] newArray(int size) {
            return new DeviceInformation[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceInformation that = (DeviceInformation) o;

        if (macAddress != null ? !macAddress.equals(that.macAddress) : that.macAddress != null)
            return false;
        if (modelNumber != null ? !modelNumber.equals(that.modelNumber) : that.modelNumber != null)
            return false;
        if (serialNumber != null ? !serialNumber.equals(that.serialNumber) : that.serialNumber != null)
            return false;
        if (firmwareRevision != null ? !firmwareRevision.equals(that.firmwareRevision) : that.firmwareRevision != null)
            return false;
        if (hardwareRevision != null ? !hardwareRevision.equals(that.hardwareRevision) : that.hardwareRevision != null)
            return false;
        if (softwareRevision != null ? !softwareRevision.equals(that.softwareRevision) : that.softwareRevision != null)
            return false;
        if (manufacturer != null ? !manufacturer.equals(that.manufacturer) : that.manufacturer != null)
            return false;
        if (!Arrays.equals(password, that.password)) return false;
        if (!Arrays.equals(broadcastId, that.broadcastId)) return false;
        return users != null ? users.equals(that.users) : that.users == null;
    }

    @Override
    public int hashCode() {
        int result = macAddress != null ? macAddress.hashCode() : 0;
        result = 31 * result + (modelNumber != null ? modelNumber.hashCode() : 0);
        result = 31 * result + (serialNumber != null ? serialNumber.hashCode() : 0);
        result = 31 * result + (firmwareRevision != null ? firmwareRevision.hashCode() : 0);
        result = 31 * result + (hardwareRevision != null ? hardwareRevision.hashCode() : 0);
        result = 31 * result + (softwareRevision != null ? softwareRevision.hashCode() : 0);
        result = 31 * result + (manufacturer != null ? manufacturer.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(password);
        result = 31 * result + Arrays.hashCode(broadcastId);
        result = 31 * result + (users != null ? users.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceInformation{" +
                "macAddress='" + macAddress + '\'' +
                ", modelNumber='" + modelNumber + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", firmwareRevision='" + firmwareRevision + '\'' +
                ", hardwareRevision='" + hardwareRevision + '\'' +
                ", softwareRevision='" + softwareRevision + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", password=" + Arrays.toString(password) +
                ", broadcastId=" + Arrays.toString(broadcastId) +
                ", users=" + users +
                '}';
    }

}
