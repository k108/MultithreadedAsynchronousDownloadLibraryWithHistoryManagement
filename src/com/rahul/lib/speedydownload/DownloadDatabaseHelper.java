package com.rahul.lib.speedydownload;

import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;









public class DownloadDatabaseHelper extends SQLiteOpenHelper {
	 private static DownloadDatabaseHelper mInstance=null;
	 public static DownloadDatabaseHelper getInstance(Context context){
		 if(mInstance==null)
			 mInstance=new DownloadDatabaseHelper(context.getApplicationContext());
	     return mInstance;
	 }
    // All Static variables
    // Database Version
    public static final int DATABASE_VERSION = 1;
 
    // Database Name
    public static final String DATABASE_NAME = "SppedyDownloadDatabase";
 
    // Contacts table name
    public static final String TABLE_DOWNLOAD_SECTION_HISTORY = "DownloadSectionHistory";
 
    // Contacts Table Columns names
    public static final String KEY_STATIC_URL = "upl";
    public static final String KEY_SECTION_LENGTH = "sectionLength";
    public static final String KEY_DATA = "data";
    public static final String KEY_ID="_id";
    private DownloadDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
 
    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_DOWNLOAD_SECTION_HISTORY + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_STATIC_URL + " TEXT,"
                + KEY_SECTION_LENGTH + " TEXT," + KEY_DATA + " TEXT"+")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOAD_SECTION_HISTORY);
 
        // Create tables again
        onCreate(db);
    }
    
    // Adding new contact
public synchronized void checkAndAddItem(DownloadUpdate contact,int Max) {
    SQLiteDatabase db = this.getWritableDatabase();
 
    db.delete(TABLE_DOWNLOAD_SECTION_HISTORY, KEY_STATIC_URL + " = ?",
            new String[] { contact.initialDetail.staticLink });
    
    
    ContentValues values = convertToContentValues(contact);
    
    
    String selectQuery = "SELECT  * FROM " + TABLE_DOWNLOAD_SECTION_HISTORY;
	 String dummy="error";
  
   Cursor cursor = db.rawQuery(selectQuery, null);
   int k=cursor.getCount();
   if(k<Max)
   {
	   db.insert(TABLE_DOWNLOAD_SECTION_HISTORY, null, values);
   }
   else
   {
	   
	   if (cursor.moveToLast()) {
		    
	    	dummy=cursor.getString(1) ;
	    	   
	           
	           }
	   
	    
	    if(!dummy.equals("error"))
	    {
	    	db.delete(TABLE_DOWNLOAD_SECTION_HISTORY, KEY_STATIC_URL + " = ?",
	                new String[] { dummy });
	    	 db.insert(TABLE_DOWNLOAD_SECTION_HISTORY, null, values);
		    	
	     
	    }  
   }
   
 
    // Inserting Row
   
    cursor.close();
    db.close(); // Closing database connection
}

public synchronized static ContentValues convertToContentValues(DownloadUpdate contact) {
		// TODO Auto-generated method stub
	ContentValues values=new ContentValues();
	values.put(KEY_STATIC_URL, contact.initialDetail.staticLink); // Contact Name
    values.put(KEY_SECTION_LENGTH, ""+String.valueOf(contact.sectionsDetails.size()));
    String temp="";
    for(int i=0;i<contact.sectionsDetails.size();i++)
    temp=temp+contact.sectionsDetails.get(i).currentlyDownloadedBytes+"/";
    values.put(KEY_DATA, temp);
    return values;
	}

public synchronized DownloadUpdate getItem(String staticUrl) {
    SQLiteDatabase db = this.getReadableDatabase();
	Cursor cursor = db.query(TABLE_DOWNLOAD_SECTION_HISTORY, new String[] { KEY_ID,
            KEY_STATIC_URL, KEY_SECTION_LENGTH,KEY_DATA}, KEY_STATIC_URL + "=?",
            new String[] { staticUrl }, null, null, null, null);
	
	if (cursor.getCount()>0)
    {
		
		cursor.moveToFirst();
    	DownloadUpdate update=new DownloadUpdate();
        InitialDownloadDetail detail=new InitialDownloadDetail();
        detail.staticLink=cursor.getString(1);
        update.initialDetail=detail;
        update.sectionsDetails=new ArrayList<DownloadSectionDetail>();
        int a,b=0;
        String data=cursor.getString(3);
        int sectionSize=Integer.parseInt(cursor.getString(2));
        for(int i=0;i<sectionSize;i++)
        {
        	DownloadSectionDetail sectionDetail=new DownloadSectionDetail();
        	a=data.indexOf("/",b);
        	if(a==-1)
        		return null;
        	sectionDetail.currentlyDownloadedBytes=Long.parseLong(data.substring(b,a));
        	update.sectionsDetails.add(sectionDetail);
        	b=a+1;
        }   	
        // return contact
    cursor.close(); 
    db.close();
    return update;
    }
    else
    {
    	cursor.close();
    	db.close();
    	return null;
    }
    }
public synchronized ArrayList<DownloadUpdate> getAllHistory() {
    ArrayList<DownloadUpdate> contactList = new ArrayList<DownloadUpdate>();
    // Select All Query
    String selectQuery = "SELECT  * FROM " + TABLE_DOWNLOAD_SECTION_HISTORY;
 
    SQLiteDatabase db = this.getWritableDatabase();
    Cursor cursor = db.rawQuery(selectQuery, null);
 
    // looping through all rows and adding to list
    if (cursor.moveToLast()) {
        do {
        	DownloadUpdate update=new DownloadUpdate();
            InitialDownloadDetail detail=new InitialDownloadDetail();
            detail.staticLink=cursor.getString(1);
            update.initialDetail=detail;
            update.sectionsDetails=new ArrayList<DownloadSectionDetail>();
            int a,b=0;
            String data=cursor.getString(3);
            int sectionSize=Integer.parseInt(cursor.getString(2));
            for(int i=0;i<sectionSize;i++)
            {
            	DownloadSectionDetail sectionDetail=new DownloadSectionDetail();
            	a=data.indexOf("/",b);
            	if(a==-1)
            		return null;
            	sectionDetail.currentlyDownloadedBytes=Long.parseLong(data.substring(b,a));
            	update.sectionsDetails.add(sectionDetail);
            	b=a+1;
            }
            contactList.add(update);
         
        } while (cursor.moveToPrevious());
    }
    cursor.close();
    db.close();
 
    // return contact list
    return contactList;
}

public synchronized int getItemsCount() {
    String countQuery = "SELECT  * FROM " + TABLE_DOWNLOAD_SECTION_HISTORY;
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(countQuery, null);
    cursor.close();

    int count =cursor.getCount();
    cursor.close();
    db.close();
    return count;
    
}
public synchronized int updateContact(DownloadUpdate contact) {
    SQLiteDatabase db = this.getWritableDatabase();
 
    ContentValues values=convertToContentValues(contact);// updating row
    int a= db.update(TABLE_DOWNLOAD_SECTION_HISTORY, values, KEY_STATIC_URL + " = ?",
            new String[] { String.valueOf(contact.initialDetail.staticLink) });
     db.close();
     return a;
}
public synchronized void deleteContact(String staticUrl) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.delete(TABLE_DOWNLOAD_SECTION_HISTORY, KEY_STATIC_URL + " = ?",
            new String[] { staticUrl });
    db.close();
}
public synchronized void deleteAll()
{
	SQLiteDatabase db = this.getWritableDatabase();
    db.delete(TABLE_DOWNLOAD_SECTION_HISTORY, null,
           null);
    db.close();
}




}


