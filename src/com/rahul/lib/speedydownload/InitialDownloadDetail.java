package com.rahul.lib.speedydownload;

import android.os.Parcel;
import android.os.Parcelable;

public class InitialDownloadDetail implements Parcelable{

	public String fileName;
	public String metaData;
	public String fileLink;
	public String staticLink;
	public long fileSize;
	public String saveLocation;
	public boolean resumable;
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(fileName);
		dest.writeString(metaData);
		dest.writeString(fileLink);
		dest.writeString(staticLink);
		dest.writeLong(fileSize);
		dest.writeString(saveLocation);
		dest.writeBooleanArray(new boolean[]{resumable});
	}
	public static final Parcelable.Creator<InitialDownloadDetail> CREATOR = new Parcelable.Creator<InitialDownloadDetail>() {
        public InitialDownloadDetail createFromParcel(Parcel in) {
            return new InitialDownloadDetail(in);
        }

        public InitialDownloadDetail[] newArray(int size) {
            return new InitialDownloadDetail[size];
        }
    };
    public InitialDownloadDetail(Parcel parcel){
    	fileName=parcel.readString();
    	metaData=parcel.readString();
    	fileLink=parcel.readString();
    	staticLink=parcel.readString();
    	fileSize=parcel.readLong();
    	saveLocation=parcel.readString();
    	boolean[] resum=new boolean[1];
    	parcel.readBooleanArray(resum);
    	this.resumable=resum[0];
    }
    public InitialDownloadDetail(){
    	
    }
}
