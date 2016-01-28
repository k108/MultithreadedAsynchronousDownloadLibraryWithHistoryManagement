package com.rahul.lib.speedydownload;
import java.util.ArrayList;

public class DownloadUpdate {
      public static enum SectionProtocol{
    	  TOTAL,
    	  HALF,
    	  ONE_THIRD,
    	  ONE_FOURTH
      };
      public String extraString="";
      public int extraInt=0;
   
      public InitialDownloadDetail initialDetail;
	  public long downloadInitiationTimeMillis;
	  public long currentTimeMillis;
	  public SectionProtocol protocol;
	  public ArrayList<DownloadSectionDetail> sectionsDetails;
      public Long updateId=(long) -1;
public long getTotalDownloaded(){
	long temp=0;
	for(int i=0;i<sectionsDetails.size();i++)
		temp=temp+sectionsDetails.get(i).currentlyDownloadedBytes;
	return temp;
}
}
