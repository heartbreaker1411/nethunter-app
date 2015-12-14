package com.offsec.nethunter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.widget.Toast;

import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static android.database.DatabaseUtils.sqlEscapeString;

class SearchSploitSQL extends SQLiteOpenHelper {
    NhPaths nh;
    Context context;
    ShellExecuter exe = new ShellExecuter();
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "SearchSploit";
    private static final String TAG = "SearchSploitSQL";
    private static final String CSVfileName = "/sdcard/nh_files/files.csv"; // tmp location
    private static final String CSVfileName_chroot = "/data/local/nhsystem/kali-armhf/usr/share/exploitdb/files.csv"; // origin
    public SearchSploitSQL(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        nh = new NhPaths();

    }

    private String CREATE_SEARCHSPLOIT_TABLE =
            "CREATE TABLE  IF NOT EXISTS " + SearchSploit.TABLE +
                    " (" + SearchSploit.ID + " INTEGER PRIMARY KEY, " +
                    SearchSploit.FILE + " TEXT," +
                    SearchSploit.DESCRIPTION + " TEXT," +
                    SearchSploit.DATE + " TEXT," +
                    SearchSploit.AUTHOR + " TEXT," +
                    SearchSploit.PLATFORM + " TEXT," +
                    SearchSploit.TYPE + " TEXT," +
                    SearchSploit.PORT + " INTEGER)";

    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_SEARCHSPLOIT_TABLE);
    }

    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + SearchSploit.TABLE);
        onCreate(database);
    }

    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        onUpgrade(database, oldVersion, newVersion);
    }

    public void doDrop(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + SearchSploit.TABLE);

    }

    public Boolean doDbFeed() {
 ;
         // copy csv to /sdcard as temp (so we can read it)
        String cmd = "csv2sqlite.py /usr/share/exploitdb/files.csv /sdcard/nh_files/SearchSploit "+ SearchSploit.TABLE + "&& exit";
        // move to app db folder
        try {
            Intent intent =
                    new Intent("com.offsec.nhterm.RUN_SCRIPT_NH");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("com.offsec.nhterm.iInitialCommand", cmd);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.d(TAG, "ERROR CRATING DB WTF?");
            return false;
        }
        return true;
    }
    public long getCount() {
        String sql = "SELECT COUNT(*) FROM " + SearchSploit.TABLE;
        SQLiteDatabase db = this.getWritableDatabase();
        SQLiteStatement statement = db.compileStatement(sql);
        long count = statement.simpleQueryForLong();
        return count;
    }
    public List<SearchSploit> getAllExploits() {
        String query = "SELECT  * FROM " + SearchSploit.TABLE + " LIMIT 100";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        List<SearchSploit> _List = createExploitList(cursor);
        db.close();
        return _List;
    }
    public List<SearchSploit> getAllExploitsFiltered(String filter, String platform, String type, String port) {
        String wildcard = "%" + filter + "%";
        String query = "SELECT * FROM " + SearchSploit.TABLE
                + " WHERE "+ SearchSploit.DESCRIPTION +" like ?" +
                " and " + SearchSploit.PLATFORM + "='" + platform +"'"+
                " and " + SearchSploit.TYPE +"='" + type +"'"+
                " and " + SearchSploit.PORT +"='" + port +"'";
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("QUERYYY", query);
        Cursor cursor = db.rawQuery(query, new String[]{wildcard});
        List<SearchSploit> _List = createExploitList(cursor);
        db.close();
        return _List;
    }
    private List<SearchSploit> createExploitList(Cursor cursor){
        List<SearchSploit> commandList = new LinkedList<>();
        if (cursor.moveToFirst()) {
            do {
                SearchSploit _exploit = new SearchSploit();
                _exploit.setId(cursor.getInt(0));                  // id
                _exploit.setFile(cursor.getString(1));             // file
                _exploit.setDescription(cursor.getString(2));      // desc
                _exploit.setDate(cursor.getString(3));             // date
                _exploit.setAuthor(cursor.getString(4));           // author
                _exploit.setPlatform(cursor.getString(5));         // platform
                _exploit.setType(cursor.getString(6));             // type
                _exploit.setPort(cursor.getInt(7));                // port
                commandList.add(_exploit);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return commandList;
    }


    public List<String> getTypes() {
        String query = "SELECT DISTINCT "+ SearchSploit.TYPE +
                " FROM " + SearchSploit.TABLE +
                " ORDER BY "+ SearchSploit.TYPE +" ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        List<String> _List = createStringList(cursor);
        db.close();
        return _List;
    }
    public List<String> getPorts() {
        String query = "SELECT DISTINCT "+ SearchSploit.PORT +
                " FROM " + SearchSploit.TABLE +
                " ORDER BY "+ SearchSploit.PORT +" ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        List<String> intList = new LinkedList<>();
        if (cursor.moveToFirst()) {
            do {
                    intList.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();

        db.close();
        return intList;
    }
    public List<String> getPlatforms() {
        String query = "SELECT DISTINCT "+ SearchSploit.PLATFORM +" FROM " + SearchSploit.TABLE  + " ORDER BY "+ SearchSploit.PLATFORM +" ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        List<String> _List = createStringList(cursor);
        db.close();
        return _List;
    }
    private List<String> createStringList(Cursor cursor){
        List<String> strList = new LinkedList<>();
        if (cursor.moveToFirst()) {
            do {
                   strList.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return strList;
    }

}