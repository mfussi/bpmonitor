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

import java.util.Date;

public class BloodPressureReading implements Parcelable {

    private int userId;
    private Date date;
    private float systolic;
    private float diastolic;
    private float meanArterialPressure;
    private float pulseRate;
    private boolean isIrregularPulseDetectionFlag;
    private int battery;

    public BloodPressureReading(int userId, Date date, float systolic, float diastolic, float meanArterialPressure, float pulseRate, boolean isIrregularPulseDetectionFlag, int battery) {
        this.userId = userId;
        this.date = date;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.meanArterialPressure = meanArterialPressure;
        this.pulseRate = pulseRate;
        this.isIrregularPulseDetectionFlag = isIrregularPulseDetectionFlag;
        this.battery = battery;
    }

    public int getUserId() {
        return userId;
    }

    public Date getDate() {
        return date;
    }

    public float getSystolic() {
        return systolic;
    }

    public float getDiastolic() {
        return diastolic;
    }

    public float getMeanArterialPressure() {
        return meanArterialPressure;
    }

    public float getPulseRate() {
        return pulseRate;
    }

    public boolean isIrregularPulseDetectionFlag() {
        return isIrregularPulseDetectionFlag;
    }

    public int getBattery() {
        return battery;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.userId);
        dest.writeLong(this.date != null ? this.date.getTime() : -1);
        dest.writeFloat(this.systolic);
        dest.writeFloat(this.diastolic);
        dest.writeFloat(this.meanArterialPressure);
        dest.writeFloat(this.pulseRate);
        dest.writeByte(this.isIrregularPulseDetectionFlag ? (byte) 1 : (byte) 0);
        dest.writeInt(this.battery);
    }

    protected BloodPressureReading(Parcel in) {
        this.userId = in.readInt();
        long tmpDate = in.readLong();
        this.date = tmpDate == -1 ? null : new Date(tmpDate);
        this.systolic = in.readFloat();
        this.diastolic = in.readFloat();
        this.meanArterialPressure = in.readFloat();
        this.pulseRate = in.readFloat();
        this.isIrregularPulseDetectionFlag = in.readByte() != 0;
        this.battery = in.readInt();
    }

    public static final Parcelable.Creator<BloodPressureReading> CREATOR = new Parcelable.Creator<BloodPressureReading>() {
        @Override
        public BloodPressureReading createFromParcel(Parcel source) {
            return new BloodPressureReading(source);
        }

        @Override
        public BloodPressureReading[] newArray(int size) {
            return new BloodPressureReading[size];
        }
    };

    @Override
    public String toString() {
        return "BloodPressureReading{" +
                "userId=" + userId +
                ", date=" + date +
                ", systolic=" + systolic +
                ", diastolic=" + diastolic +
                ", meanArterialPressure=" + meanArterialPressure +
                ", pulseRate=" + pulseRate +
                ", isIrregularPulseDetectionFlag=" + isIrregularPulseDetectionFlag +
                ", battery=" + battery +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BloodPressureReading that = (BloodPressureReading) o;

        if (userId != that.userId) return false;
        if (Float.compare(that.systolic, systolic) != 0) return false;
        if (Float.compare(that.diastolic, diastolic) != 0) return false;
        if (Float.compare(that.meanArterialPressure, meanArterialPressure) != 0) return false;
        if (Float.compare(that.pulseRate, pulseRate) != 0) return false;
        if (isIrregularPulseDetectionFlag != that.isIrregularPulseDetectionFlag) return false;
        if (battery != that.battery) return false;
        return date != null ? date.equals(that.date) : that.date == null;
    }

    @Override
    public int hashCode() {
        int result = userId;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (systolic != +0.0f ? Float.floatToIntBits(systolic) : 0);
        result = 31 * result + (diastolic != +0.0f ? Float.floatToIntBits(diastolic) : 0);
        result = 31 * result + (meanArterialPressure != +0.0f ? Float.floatToIntBits(meanArterialPressure) : 0);
        result = 31 * result + (pulseRate != +0.0f ? Float.floatToIntBits(pulseRate) : 0);
        result = 31 * result + (isIrregularPulseDetectionFlag ? 1 : 0);
        result = 31 * result + battery;
        return result;
    }

}