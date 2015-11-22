package com.z299studio.wordchain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	private static String DB_PATH;
	private static final String DB_NAME = "dict.db";
	private static final int DB_VERSION = 1;
	
	public static final String TABLE_EN = "English";
	private SQLiteDatabase mDB;
	private Context ctx;

	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.ctx = context;
		DB_PATH = ctx.getApplicationInfo().dataDir+"/databases/";
	}

	@Override
	public void onCreate(SQLiteDatabase arg0) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		Log.i("DatabaseHelper", "onUpgrade Called");
	}
	
	public void createDatabase() throws IOException {
		boolean exist = checkDatabase();
		if(!exist) {
			getReadableDatabase();
			try {
				copyDatabase();
			} catch (IOException e) {
				Log.e("WordChain", "Failed to create Dictionary database.");
				throw e; 
			}
		}
		openReadableDatabase();
	}
	
	public boolean checkDatabase() {
		String path = DB_PATH + DB_NAME;
		File dbFile;
		dbFile = new File(path); 
		return dbFile.exists();
	}
	
	public void copyDatabase() throws IOException {
		int length;
		byte [] buffer = new byte [4096];
		String outFileName = DB_PATH + DB_NAME;
		InputStream is = ctx.getAssets().open(DB_NAME);
		OutputStream os = new FileOutputStream(outFileName);
		while((length = is.read(buffer))>0) {
			os.write(buffer, 0, length);
		}
		os.flush();
		os.close();
		is.close();
	}
	
	public SQLiteDatabase openReadableDatabase()  {
		String path = DB_PATH + DB_NAME;
		mDB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		return mDB;
	}
	
	@Override
	public synchronized void close() {
		if(mDB != null ) {
			mDB.close();
		}
	}
	
	public String getText(int n) {
		int index;
		String ret = "";
		String columns[] = {"word"};
		String where = "ROWID = ?";
		String arg[]= {String.valueOf(n)};
		Cursor cursor = mDB.query(TABLE_EN, columns, where, arg, null, null, null);
		index = cursor.getColumnIndex(columns[0]);
		if(cursor.moveToFirst()) {
			ret = cursor.getString(index);			
		}
		cursor.close();
		return ret;
	}
	
	public boolean checkText(String text) {
		String columns[] = {"ROWID"};
		String where = "word = ?";
		String arg[] = {text};
		Cursor cursor = mDB.query(TABLE_EN, columns, where, arg, null, null, null);
		boolean hasData =  cursor.moveToFirst();
		cursor.close();
		return hasData;
	}
}
