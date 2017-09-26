package porktraceability.com.gps;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by user on 6/27/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "GPSPork.db";

    public static final String FILE_TABLE_NAME = "GPS_DETAILS";
    public static final String FILE_COL1_ID = "FILE_ID";
    public static final String FILE_COL2_DISTANCE = "FILE_DISTANCE";
    public static final String FILE_COL3_SPEED = "FILE_SPEED";

    public static final String LOG_TABLE_NAME = "GPSLOG_INFO";
    public static final String LOG_COL1_ID = "GPX_ID";
    public static final String LOG_COL2_POINT = "POINT";
    public static final String LOG_COL3_TIME = "TIME_REC";
    public static final String LOG_COL4_LAT = "LATITUDE";
    public static final String LOG_COL5_LNG = "LONGITUDE";
    public static final String LOG_COL6_ALT = "ALTITUDE";
    public static final String LOG_COL7_ACCURACY = "ACCURACY";
    public static final String LOG_COL8_DESC = "DESCRIPTION";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " +
                FILE_TABLE_NAME + " (" +
                FILE_COL1_ID + " TEXT PRIMARY KEY, " + //start date as id
                FILE_COL2_DISTANCE + " LONG, " + //distance traveled during the trip
                FILE_COL3_SPEED + " LONG)" //average speed
        );

        sqLiteDatabase.execSQL("create table " +
                LOG_TABLE_NAME + " (" +
                LOG_COL1_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                LOG_COL2_POINT + " INTEGER, " + //reference to check how many points in a given trip
                LOG_COL3_TIME + " TIME, " + //current time
                LOG_COL4_LAT + " DOUBLE, " + //latitude
                LOG_COL5_LNG + " DOUBLE, " + //longitude
                LOG_COL6_ALT + " INTEGER, " + //altitude
                LOG_COL7_ACCURACY + " INTEGER, " + //accuracy of the location
                LOG_COL8_DESC + " STRING) " //start date
        );
        Log.v("GPSork","Database Created");
    }

    public void clearLogs(){//deletes all the data
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        sqLiteDatabase.delete(LOG_TABLE_NAME, null, null);
        sqLiteDatabase.delete(FILE_TABLE_NAME, null, null);
    }

    public void deleteRow(String startDate){//deletes specific data according to what the user selected
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        sqLiteDatabase.delete(LOG_TABLE_NAME, LOG_COL8_DESC + "=?", new String [] {startDate});
        sqLiteDatabase.delete(FILE_TABLE_NAME, FILE_COL1_ID + "=?", new String [] {startDate});
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + FILE_TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public boolean insertDataFile(String name, long distance, long speed){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(FILE_COL1_ID, name);
        contentValues.put(FILE_COL2_DISTANCE, distance);
        contentValues.put(FILE_COL3_SPEED, speed);
        long result = sqLiteDatabase.insert(FILE_TABLE_NAME, null, contentValues);
        if (result == -1)
            return false;
        else
            return true;
    }

    public boolean insertLogFile(int ID, String time, double lat, double lng, double alt, float acc, String desc){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOG_COL2_POINT, ID);
        contentValues.put(LOG_COL3_TIME, time);
        contentValues.put(LOG_COL4_LAT, lat);
        contentValues.put(LOG_COL5_LNG, lng);
        contentValues.put(LOG_COL6_ALT, alt);
        contentValues.put(LOG_COL7_ACCURACY, acc);
        contentValues.put(LOG_COL8_DESC, desc);
        long result = sqLiteDatabase.insert(LOG_TABLE_NAME, null, contentValues);
        if (result == -1)
            return false;
        else
            return true;
    }

    public Cursor getAllData() {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor res = sqLiteDatabase.rawQuery("select * from GPSLOG_INFO", null);
        return res;
    }

    public Cursor getDetails() {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor res = sqLiteDatabase.rawQuery("select * from GPS_DETAILS", null);
        return res;
    }

}
