package com.rahul.lib.speedydownload;

import android.os.Parcel;
import android.os.Parcelable;


public class MultiConnectionErrorDiscription implements Parcelable{
	public enum MultiConnectionError{
		errorNoInternetConnection,errorInsufficientStorage,errorConectionTimeout,errorNoWritableSdCard,notAValidLink,unknownError
	}
	public MultiConnectionError error=MultiConnectionError.unknownError;
	public String discription=" ";
	public double extraLong1=0.0,extraLong2=0.0;
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
	   	dest.writeInt(error.ordinal());
	   	dest.writeString(discription);
	   	dest.writeDouble(extraLong1);
	   	dest.writeDouble(extraLong2);
	}
	public static final Parcelable.Creator<MultiConnectionErrorDiscription> CREATOR = new Parcelable.Creator<MultiConnectionErrorDiscription>() {
        public MultiConnectionErrorDiscription createFromParcel(Parcel in) {
            return new MultiConnectionErrorDiscription(in);
        }

        public MultiConnectionErrorDiscription[] newArray(int size) {
            return new MultiConnectionErrorDiscription[size];
        }
    };
    public MultiConnectionErrorDiscription(Parcel parcel){
    	switch(parcel.readInt()){
    	case 0:
    		this.error=MultiConnectionError.errorNoInternetConnection;
    		break;
    	case 1:
    		error=MultiConnectionError.errorInsufficientStorage;
    		break;
    	case 2:
    		error=MultiConnectionError.errorConectionTimeout;
    		break;
    	case 3:
    		error=MultiConnectionError.errorNoWritableSdCard;
    		break;
    	case 4:
    		error=MultiConnectionError.notAValidLink;
    		break;
    	}
    	discription=parcel.readString();
    	extraLong1=parcel.readDouble();
    	extraLong2=parcel.readDouble();
    }
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	public MultiConnectionErrorDiscription(){}
	
	
} 
	