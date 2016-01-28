package com.rahul.lib.speedydownload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.ClientProtocolException;

import com.rahul.lib.speedydownload.DownloadSectionDetail.DownloadStatus;
import com.rahul.lib.speedydownload.DownloadUpdate.SectionProtocol;
import com.rahul.lib.speedydownload.MultiConnectionErrorDiscription.MultiConnectionError;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StatFs;
import android.util.Log;

public class MultiConnectionDownloader {

	public interface OnDownloadUpdateListener {
		public void onStartDownload(DownloadUpdate update);

		public void onUpdate(DownloadUpdate update);

		public void onError(MultiConnectionErrorDiscription error);

		public void onDownloadComplete(DownloadUpdate currentUpdate);

		public void onDownloadInterrupted(DownloadUpdate update,
				int interruptedId);

		public void onDownloadStopped(DownloadUpdate update);
	}

	public interface OnPreparedListener {
		public void onPreparedFail(MultiConnectionErrorDiscription error,
				DownloadUpdate update);

		public void onPreparedSuccess(DownloadUpdate update);
	}

	public DownloadUpdate getCurrentUpdate() {
		return this.currentUpdate;
	}

	private Random rand;
	private AutoSaveRunnable asr = null;
	private boolean resumable, fileExistedBefore = false;
	private OnDownloadUpdateListener ls;
	private ExecutorService executorService;
	private String url;
	private String staticUrl;
	private String finalSaveLocation;
	private long fileSize;
	private SectionProtocol protocol;
	private int sectionCount;
	private ArrayList<Long> sectionsSizes;
	private String fileName;
	private DownloadUpdate currentUpdate;
	private boolean cancel = false;
	private Handler handler;
	private String lastSavedString="";
	private String metaData;
	private Context context;
	public Intent startIntent;
	private UpdateRunnable ur;

	public MultiConnectionDownloader(Context context, Intent startIntent) {
		this.context = context;
		this.startIntent = startIntent;
		sectionsSizes = new ArrayList<Long>();

		handler = new Handler(Looper.getMainLooper());
		rand = new Random();
		this.disableConnectionReuseIfNecessary();
	}

	@SuppressLint("NewApi")
	private void disableConnectionReuseIfNecessary() {
		// Work around pre-Froyo bugs in HTTP connection reuse.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");

		}
	}

	public void prepareNewDownloadAsync(long id, String fileName,
			String metaData, final String url, String staticUrl,
			final SectionProtocol protoco, String saveLocation,
			final OnPreparedListener listener,
			final OnDownloadUpdateListener ls,
			final boolean shouldAutomaticallyStartOnPreperation) {
		File file = new File(saveLocation);
		file.mkdirs();
		sectionsSizes = new ArrayList<Long>();

		this.url = url;
		this.ls = ls;
		this.finalSaveLocation = saveLocation;
		this.fileName = fileName;
		this.staticUrl = staticUrl;
		this.metaData = metaData;

		DownloadUpdate update = new DownloadUpdate();
		update.currentTimeMillis = System.currentTimeMillis();
		update.downloadInitiationTimeMillis = id;
		update.updateId = update.downloadInitiationTimeMillis;

		InitialDownloadDetail initialDetail = new InitialDownloadDetail();
		initialDetail.fileLink = url;
		initialDetail.fileName = fileName;
		initialDetail.fileSize = 1;
		initialDetail.metaData = metaData;
		initialDetail.resumable = true;
		initialDetail.staticLink = staticUrl;
		initialDetail.saveLocation = saveLocation;

		update.initialDetail = initialDetail;
		update.protocol = SectionProtocol.TOTAL;

		update.sectionsDetails = new ArrayList<DownloadSectionDetail>();
		DownloadSectionDetail detail = new DownloadSectionDetail();
		detail.byteMarkOnTotalFileSize = 1;
		detail.currentlyDownloadedBytes = 0;
		detail.initialByte = 0;
		detail.onePastEndByte = 2;
		detail.parentFileName = fileName;
		detail.Range = 1;
		detail.status = DownloadStatus.ONGOING;
		update.sectionsDetails.add(detail);
		this.currentUpdate = update;
		/*
		 * handler.post(new Runnable(){
		 * 
		 * @Override public void run() { // TODO Auto-generated method stub
		 * ls.onUpdate(currentUpdate); }});
		 */
		(new Thread() {
			@Override
			public void run() {

				boolean sdCardState = isSDCardThere();
				if (!sdCardState) {
					handler.post(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							MultiConnectionErrorDiscription dis = new MultiConnectionErrorDiscription();
							dis.error = MultiConnectionError.errorNoWritableSdCard;
							listener.onPreparedFail(dis, currentUpdate);
						}

					});
					return;
				}

				fileSize = getFileSize(url);
				if (fileSize == -13) {
					Log.v("MultiConnection", "no network");
					handler.post(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							MultiConnectionErrorDiscription dis = new MultiConnectionErrorDiscription();
							dis.error = MultiConnectionError.errorNoInternetConnection;

							listener.onPreparedFail(dis, currentUpdate);
						}
					});
					return;

				}
				if (fileSize == -12) {
					shoutStopped();
					return;
				}
				if (fileSize < 1) {

					handler.post(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							MultiConnectionErrorDiscription dis = new MultiConnectionErrorDiscription();
							dis.error = MultiConnectionError.notAValidLink;

							listener.onPreparedFail(dis, currentUpdate);
						}
					});
					return;
				}
				protocol = setUpExecutor(protoco,
						currentUpdate.initialDetail.staticLink);
				setUpAdvancedPreperation(listener,
						shouldAutomaticallyStartOnPreperation);
			}
		}).start();

	}

	public void stopForcibly() {
		this.cancel = true;
	}

	protected void setUpAdvancedPreperation(final OnPreparedListener listener,
			boolean b) {
		// TODO Auto-generated method stub
		if (cancel) {
			shoutStopped();
			return;
		}
		long sectionWidth = this.fileSize / sectionCount;
		for (int i = 0; i < (this.sectionCount - 1); i++)
			sectionsSizes.add(sectionWidth);
		long lastSectionSize = fileSize - ((sectionCount - 1) * sectionWidth);
		sectionsSizes.add(lastSectionSize);

		ArrayList<DownloadSectionDetail> sectionsDetails = new ArrayList<DownloadSectionDetail>();

		for (int i = 0; i < sectionCount; i++) {
			if (cancel) {
				shoutStopped();
				return;
			}
			DownloadSectionDetail detail = new DownloadSectionDetail();
			if (i == 0)
				detail.initialByte = 1;
			else {
				long temp = 0;
				for (int k = 0; k < i; k++)
					temp = temp + sectionsSizes.get(k);
				detail.initialByte = temp + 1;
			}
			if (i == (sectionCount - 1))
				detail.onePastEndByte = fileSize + 1;
			else {
				long temp = 0;
				for (int k = 0; k < i + 1; k++)
					temp = temp + sectionsSizes.get(k);
				detail.onePastEndByte = temp + 1;
			}
			detail.currentlyDownloadedBytes = 0;
			detail.status = DownloadStatus.ONGOING;
			detail.parentFileName = this.fileName;
			detail.refreshDependentData();
			sectionsDetails.add(detail);
		}

		DownloadUpdate update = new DownloadUpdate();
		update.currentTimeMillis = currentUpdate.currentTimeMillis;
		update.downloadInitiationTimeMillis = currentUpdate.downloadInitiationTimeMillis;
		update.updateId = update.downloadInitiationTimeMillis;
		InitialDownloadDetail initialDetail = new InitialDownloadDetail();
		initialDetail.fileLink = this.url;
		initialDetail.fileName = this.fileName;
		initialDetail.fileSize = this.fileSize;
		initialDetail.metaData = this.metaData;
		initialDetail.resumable = this.resumable;
		initialDetail.staticLink = this.staticUrl;
		initialDetail.saveLocation = this.finalSaveLocation;

		update.initialDetail = initialDetail;
		update.protocol = this.protocol;
		update.sectionsDetails = sectionsDetails;
		this.currentUpdate = update;

		File f = new File(currentUpdate.initialDetail.saveLocation
				+ File.separator + currentUpdate.initialDetail.staticLink.hashCode()+currentUpdate.initialDetail.fileName);
		fileExistedBefore = f.isFile();
		if (fileExistedBefore && currentUpdate.initialDetail.resumable) {
			for (int i = 0; i < currentUpdate.sectionsDetails.size(); i++) {
				currentUpdate.sectionsDetails.get(i).currentlyDownloadedBytes = getCurrentlyDownloadedBytesFromHistory(
						currentUpdate, i);
			}
			if (currentUpdate.sectionsDetails.size() == 1) {
				if (currentUpdate.sectionsDetails.get(0).currentlyDownloadedBytes == 0) {
					currentUpdate.sectionsDetails.get(0).currentlyDownloadedBytes = f
							.length();
				}
			}

		}

		long totalSizeNeededToDownload = 0;

		for (int i = 0; i < currentUpdate.sectionsDetails.size(); i++) {
			totalSizeNeededToDownload = totalSizeNeededToDownload
					+ (currentUpdate.sectionsDetails.get(i).Range - currentUpdate.sectionsDetails
							.get(i).currentlyDownloadedBytes);
		}
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
				.getPath());
		double sdAvailSize = (double) stat.getAvailableBlocks()
				* (double) stat.getBlockSize();
		Log.v("size Available", "" + sdAvailSize);
		if (fileExistedBefore) {
			for (int i = 0; i < currentUpdate.sectionsDetails.size() - 2; i++) {
				sdAvailSize = sdAvailSize
						- currentUpdate.sectionsDetails.get(i).currentlyDownloadedBytes;
			}
		}
		final double needed = totalSizeNeededToDownload, avail = sdAvailSize;
		if (totalSizeNeededToDownload > sdAvailSize) {
			handler.post(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					MultiConnectionErrorDiscription dis = new MultiConnectionErrorDiscription();
					dis.error = MultiConnectionError.errorInsufficientStorage;
					dis.extraLong1 = needed;
					dis.extraLong2 = avail;
					listener.onPreparedFail(dis, currentUpdate);

				}
			});
			return;
		}

		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				listener.onPreparedSuccess(currentUpdate);
			}
		});

		if (b)
			startDownload();

	}

	private void shoutStopped() {
		// TODO Auto-generated method stub
		if(ur!=null)
			ur.cancell=true;
		if(asr!=null)
			asr.ccancel=true;
		for (int i = 0; i < currentUpdate.sectionsDetails.size(); i++)
			currentUpdate.sectionsDetails.get(i).status = DownloadStatus.INTERRUPTED;
		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				ls.onDownloadStopped(currentUpdate);
			}
		});
	}

	public void startDownload() {
		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				ls.onStartDownload(currentUpdate);
			}
		});
		if (asr != null)
			asr.ccancel = true;
		asr = new AutoSaveRunnable();
		handler.postDelayed(asr, 5000);
		if(ur!=null)
			ur.cancell=true;
		ur=new UpdateRunnable();
		handler.postDelayed(ur, 3000);
		for (int i = 0; i < this.currentUpdate.sectionsDetails.size(); i++)
			executorService.submit(new SectionDownloader(i));
	}

	private class SectionDownloader implements Runnable {

		int id;

		SectionDownloader(int id) {
			this.id = id;
		}

		@Override
		public void run() {

			boolean success = downloadSection(id);
			if (success) {

			}
			if (!success && !cancel) {
				handler.post(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						ls.onDownloadInterrupted(currentUpdate, id);
					}
				});
			}
			if (checkDownloadComplete() && !cancel) {
				packUp();
			}
			if (cancel) {
				handler.post(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						ls.onDownloadStopped(currentUpdate);
					}
				});
			}
			addUpdateToDownloadHistory(currentUpdate, 1000);

		}

	}

	private void packUp() {
		for (int i = 0; i < currentUpdate.sectionsDetails.size(); i++) {
			currentUpdate.sectionsDetails.get(i).status = DownloadStatus.COMPLETE;
		}
		// TODO Auto-generated method stub
		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (asr != null)
					asr.ccancel = true;
				if(ur!=null)
					ur.cancell=true;
				ls.onDownloadComplete(currentUpdate);

			}
		});
		executorService.shutdownNow();

	}

	public boolean checkDownloadComplete() {
		// TODO Auto-generated method stub
		for (int i = 0; i < currentUpdate.sectionsDetails.size(); i++) {
			DownloadSectionDetail detail = currentUpdate.sectionsDetails.get(i);
			if (detail.currentlyDownloadedBytes < detail.Range)
				return false;
		}
		return true;
	}

	private boolean downloadSection(final int id) {

		HttpURLConnection conn = null;
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(
					currentUpdate.initialDetail.saveLocation + File.separator
							+ currentUpdate.initialDetail.staticLink.hashCode()+currentUpdate.initialDetail.fileName, "rw");
			URL imageUrl = new URL(currentUpdate.initialDetail.fileLink);

			if (cancel) {
				currentUpdate.sectionsDetails.get(id).status = DownloadStatus.INTERRUPTED;
				return false;
			}
			conn = (HttpURLConnection) imageUrl.openConnection();
			if (cancel) {
				currentUpdate.sectionsDetails.get(id).status = DownloadStatus.INTERRUPTED;
				return false;
			}
			if (id == currentUpdate.sectionsDetails.size() - 1) {
				conn.setRequestProperty(
						"Range",
						"bytes="
								+ (currentUpdate.sectionsDetails.get(id).initialByte - 1 + currentUpdate.sectionsDetails
										.get(id).currentlyDownloadedBytes)
								+ "-");
			} else {
				conn.setRequestProperty(
						"Range",
						"bytes="
								+ (currentUpdate.sectionsDetails.get(id).initialByte - 1 + currentUpdate.sectionsDetails
										.get(id).currentlyDownloadedBytes)
								+ "-"
								+ (currentUpdate.sectionsDetails.get(id).onePastEndByte - 1));
			}

			conn.setConnectTimeout(100000);
			conn.setReadTimeout(100000);
			if (cancel) {
				currentUpdate.sectionsDetails.get(id).status = DownloadStatus.INTERRUPTED;
				return false;
			}
			file.seek(currentUpdate.sectionsDetails.get(id).initialByte
					- 1
					+ currentUpdate.sectionsDetails.get(id).currentlyDownloadedBytes);
			if (cancel) {
				currentUpdate.sectionsDetails.get(id).status = DownloadStatus.INTERRUPTED;
				return false;
			}
			InputStream is = conn.getInputStream();
			if (cancel) {
				currentUpdate.sectionsDetails.get(id).status = DownloadStatus.INTERRUPTED;
				return false;
			}
			byte buffer[];
			buffer = new byte[1024];
			while (true) {
				int read = is.read(buffer);
				if (cancel) {
					currentUpdate.sectionsDetails.get(id).status = DownloadStatus.INTERRUPTED;
					return false;
				}
				if (read == -1) {
					break;
				}
				file.write(buffer, 0, read);
				currentUpdate.sectionsDetails.get(id).currentlyDownloadedBytes = currentUpdate.sectionsDetails
						.get(id).currentlyDownloadedBytes + read;
				currentUpdate.sectionsDetails.get(id).refreshDependentData();
				currentUpdate.currentTimeMillis = System.currentTimeMillis();
				currentUpdate.sectionsDetails.get(id).status = DownloadStatus.ONGOING;
				if (cancel) {
					currentUpdate.sectionsDetails.get(id).status = DownloadStatus.INTERRUPTED;
					return false;
				}
		
				if (cancel) {
					currentUpdate.sectionsDetails.get(id).status = DownloadStatus.INTERRUPTED;
					return false;
				}
			}

			currentUpdate.sectionsDetails.get(id).status = DownloadStatus.COMPLETE;
			return true;

		} catch (Exception ex) {
			ex.printStackTrace();

			try {

				Thread.sleep(rand.nextInt(2000));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			handler.post(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					currentUpdate.sectionsDetails.get(id).status = DownloadStatus.INTERRUPTED;
					ls.onUpdate(currentUpdate);
				}
			});

			return false;
		} finally {
			if (conn != null)
				conn.disconnect();
			try {
				if (file != null)
					file.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private long getCurrentlyDownloadedBytesFromHistory(
			DownloadUpdate currentUpdate2, int id) {
		// TODO Auto-generated method stub
		int i = 0;
		while (true) {
			try {
				DownloadUpdate update = DownloadDatabaseHelper.getInstance(
						context).getItem(
						currentUpdate2.initialDetail.staticLink);
				if (update == null)
					return 0;
				return update.sectionsDetails.get(id).currentlyDownloadedBytes;

			} catch (Exception e) {
				try {
					i++;
					if (i > 4)
						return 0;
					Thread.sleep(rand.nextInt(400));
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

	}

	private int getAnyPreviousSectionProtocolFromHistory(String staticLink) {
		// TODO Auto-generated method stub
		int i = 0;
		while (true) {
			try {
				DownloadUpdate update = DownloadDatabaseHelper.getInstance(
						context).getItem(staticLink);
				if (update == null)
					return -1;
				return update.sectionsDetails.size();

			} catch (Exception e) {
				try {
					i++;
					if (i > 4)
						return 0;
					Thread.sleep(rand.nextInt(400));
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

	}

	private SectionProtocol setUpExecutor(SectionProtocol protocol,
			String staticLink) {
		// TODO Auto-generated method stub

		if (!resumable) {
			executorService = Executors.newFixedThreadPool(1);
			sectionCount = 1;
			return SectionProtocol.TOTAL;
		}
		int anyPreviousProtocol = this
				.getAnyPreviousSectionProtocolFromHistory(staticLink);
		if (anyPreviousProtocol > 0) {
			executorService = Executors.newFixedThreadPool(anyPreviousProtocol);
			Log.v("MulticonnectionDownloader",
					"sectionLength=anyPreviousProtocol");
			switch (anyPreviousProtocol) {
			case 1:
				sectionCount = 1;
				return SectionProtocol.TOTAL;
			case 2:
				sectionCount = 2;
				return SectionProtocol.HALF;
			case 3:
				sectionCount = 3;
				return SectionProtocol.ONE_THIRD;
			case 4:
				sectionCount = 4;
				return SectionProtocol.ONE_FOURTH;
			}
		}

		if (protocol.equals(SectionProtocol.TOTAL)) {
			executorService = Executors.newFixedThreadPool(1);
			sectionCount = 1;
		}
		if (protocol.equals(SectionProtocol.HALF)) {
			executorService = Executors.newFixedThreadPool(2);
			sectionCount = 2;
		}

		if (protocol.equals(SectionProtocol.ONE_THIRD)) {
			executorService = Executors.newFixedThreadPool(3);
			sectionCount = 3;
		}

		if (protocol.equals(SectionProtocol.ONE_FOURTH)) {
			executorService = Executors.newFixedThreadPool(4);
			sectionCount = 4;
		}

		return protocol;

	}

	private long getFileSize(String url) {
		// TODO Auto-generated method stub
		if (context != null)
			if (noConnection(context))
				return -13;

		HttpURLConnection conn = null;
		try {
			if (cancel)
				return -12;
			URL Url = new URL(url);
			if (cancel)
				return -12;
			conn = (HttpURLConnection) Url.openConnection();
			if (cancel)
				return -12;

			conn.setRequestMethod("HEAD");

			if (cancel)
				return -12;

			String acceptRange = conn.getHeaderField("Accept-Ranges");
			if (cancel)
				return -12;
			String fileSize = conn.getHeaderField("Content-Length");
			if (cancel)
				return -12;
			if (acceptRange == null) {
				this.resumable = false;
			} else {
				if (acceptRange.toLowerCase().equals("bytes"))
					this.resumable = true;
				else
					this.resumable = false;
			}
			if (fileSize == null)
				return 0;
			else {
				return Long.parseLong(fileSize);
			}

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	private void addUpdateToDownloadHistory(DownloadUpdate update, int max) {

		int i = 0;
		while (true) {
			try {
				DownloadDatabaseHelper.getInstance(context).checkAndAddItem(
						update, 1000);
				break;
			} catch (Exception e) {
				try {
					i++;
					if (i > 4)
						break;
					Thread.sleep(rand.nextInt(400));
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

	private boolean isSDCardThere() {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
	}

	private boolean noConnection(Context context) {
		// TODO Auto-generated method stub
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()
				&& networkInfo.isAvailable()) {
			return false;

		}
		return true;
	}

	private class AutoSaveRunnable implements Runnable {
		public boolean ccancel = false;

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				if (cancel)
					return;
				if (ccancel) {
					ccancel = false;
					return;
				}
				Log.v("MultiConnectionDownloader", "auto saving");
				addUpdateToDownloadHistory(currentUpdate, 1000);
				if (cancel)
					return;
				if (ccancel) {
					ccancel = false;
					return;
				}
				handler.postDelayed(this, 15000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
private class UpdateRunnable implements Runnable{
    boolean cancell=false;
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(cancell||cancel){
			this.cancell=false;
			return;}
		if(currentUpdate.getTotalDownloaded()<1024*1024){
		    sendUpdate();
		    handler.postDelayed(this, 4000);
			
		    return;
		}
		Log.v("multifucjer",getSizeStringFromBytes(currentUpdate.getTotalDownloaded()).equals(lastSavedString)+","+lastSavedString);
		if(!getSizeStringFromBytes(currentUpdate.getTotalDownloaded()).equals(lastSavedString)){
			sendUpdate();
			handler.postDelayed(this, 4000);
			
			lastSavedString=getSizeStringFromBytes(currentUpdate.getTotalDownloaded());
			Log.v("LastSaved","got a new value="+lastSavedString);
			return;
			
		}
		handler.postDelayed(this, 4000);
		
	}
	
}	



private String getSizeStringFromBytes(double size) {
	// TODO Auto-generated method stub
	  if(size<=1)
	    	return "0 B";
		if(size > 1024 * 1024 * 1024)//giga
		{
			return String.format("%.1f GB", size / (1024 * 1024 * 1024));
		}
		else if(size > 1024 * 1024)  //mega
	    {
	      return String.format("%.1f MB", size /(1024 * 1024));
	    }
	    else if(size > 1024)  //kilo
	    {
	      return String.format("%.1f KB", size / 1024.0f);
	    }
	    else
	    {
	      return String.format("%d B",size);
	    }

}

public void sendUpdate() {
	// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		ls.onUpdate(currentUpdate);
		
}

}
