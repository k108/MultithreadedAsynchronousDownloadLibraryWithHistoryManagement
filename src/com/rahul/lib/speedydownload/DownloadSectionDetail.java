package com.rahul.lib.speedydownload;

import android.os.Parcel;
import android.os.Parcelable;

public class DownloadSectionDetail implements Parcelable{
   
	public static enum DownloadStatus{
	    	  ONGOING,
	    	  COMPLETE,
	    	  INTERRUPTED
	          }
	   
	public String parentFileName;
	public long initialByte;
	public long onePastEndByte;
	public long Range;
	public long currentlyDownloadedBytes;
	public long byteMarkOnTotalFileSize;
	public DownloadStatus status;
	public void refreshDependentData(){
		this.Range=this.onePastEndByte-this.initialByte;
		this.byteMarkOnTotalFileSize=initialByte+currentlyDownloadedBytes;
		}
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(parentFileName);
		dest.writeLong(initialByte);
		dest.writeLong(onePastEndByte);
		dest.writeLong(Range);
		dest.writeLong(currentlyDownloadedBytes);
		dest.writeLong(byteMarkOnTotalFileSize);
		dest.writeInt(status==DownloadStatus.COMPLETE?0:(status==DownloadStatus.ONGOING?1:2));
		
	}
	
	public static final Parcelable.Creator<DownloadSectionDetail> CREATOR = new Parcelable.Creator<DownloadSectionDetail>() {
        public DownloadSectionDetail createFromParcel(Parcel in) {
            return new DownloadSectionDetail(in);
        }

        public DownloadSectionDetail[] newArray(int size) {
            return new DownloadSectionDetail[size];
        }
    };
    
    public DownloadSectionDetail(Parcel parcel){
    	this.parentFileName=parcel.readString();
    	this.initialByte=parcel.readLong();
    	this.onePastEndByte=parcel.readLong();
    	this.Range=parcel.readLong();
    	this.currentlyDownloadedBytes=parcel.readLong();
    	this.byteMarkOnTotalFileSize=parcel.readLong();
    	int a=parcel.readInt();
    	status=a==0?DownloadStatus.COMPLETE:(a==1?DownloadStatus.ONGOING:DownloadStatus.INTERRUPTED);
    }
    public DownloadSectionDetail(){}
	
}
