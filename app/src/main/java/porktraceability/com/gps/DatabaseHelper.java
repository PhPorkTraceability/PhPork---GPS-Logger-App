package porktraceability.com.gps;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by user on 6/27/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "GPSPork.db";

    public static final String GPS_DETAIL = "GPS_DETAILS";
    public static final String FILE_COL1_ID = "FILE_ID";
    public static final String FILE_COL2_DISTANCE = "FILE_DISTANCE";
    public static final String FILE_COL3_SPEED = "FILE_SPEED";

    public static final String GPX_LOG_INFO = "GPSLOG_INFO";
    public static final String LOG_COL1_ID = "GPX_ID";
    public static final String LOG_COL2_POINT = "POINT";
    public static final String LOG_COL3_TIME = "TIME_REC";
    public static final String LOG_COL4_LAT = "LATITUDE";
    public static final String LOG_COL5_LNG = "LONGITUDE";
    public static final String LOG_COL6_ALT = "ALTITUDE";
    public static final String LOG_COL7_ACCURACY = "ACCURACY";
    public static final String LOG_COL8_DESC = "DESCRIPTION";

    public static final String SYNC_STAT = "sync_status";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " +
                GPS_DETAIL + " (" +
                FILE_COL1_ID + " TEXT PRIMARY KEY, " + //start date as id
                FILE_COL2_DISTANCE + " LONG, " + //distance traveled during the trip
                FILE_COL3_SPEED + " LONG," +
                SYNC_STAT + " TEXT" + ")" //average speed
        );

        sqLiteDatabase.execSQL("create table " +
                GPX_LOG_INFO + " (" +
                LOG_COL1_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                LOG_COL2_POINT + " INTEGER, " + //reference to check how many points in a given trip
                LOG_COL3_TIME + " TIME, " + //current time
                LOG_COL4_LAT + " DOUBLE, " + //latitude
                LOG_COL5_LNG + " DOUBLE, " + //longitude
                LOG_COL6_ALT + " INTEGER, " + //altitude
                LOG_COL7_ACCURACY + " INTEGER, " + //accuracy of the location
                LOG_COL8_DESC + " STRING," +
                SYNC_STAT + " TEXT" + ") " //start date
        );
        Log.v("GPSPork","Database Created");
    }

    public void clearLogs(){//deletes all the data
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        sqLiteDatabase.delete(GPX_LOG_INFO, null, null);
        sqLiteDatabase.delete(GPS_DETAIL, null, null);
    }

    public void deleteRow(String startDate){//deletes specific data according to what the user selected
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        sqLiteDatabase.delete(GPX_LOG_INFO, LOG_COL8_DESC + "=?", new String [] {startDate});
        sqLiteDatabase.delete(GPS_DETAIL, FILE_COL1_ID + "=?", new String [] {startDate});
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + GPS_DETAIL);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + GPX_LOG_INFO);
        onCreate(sqLiteDatabase);
    }

    public boolean insertDataFile(String name, long distance, long speed){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(FILE_COL1_ID, name);
        contentValues.put(FILE_COL2_DISTANCE, distance);
        contentValues.put(FILE_COL3_SPEED, speed);
        contentValues.put(SYNC_STAT, "new");
        long result = sqLiteDatabase.insert(GPS_DETAIL, null, contentValues);
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
        contentValues.put(SYNC_STAT, "new");
        long result = sqLiteDatabase.insert(GPX_LOG_INFO, null, contentValues);
        if (result == -1)
            return false;
        else
            return true;
    }

    public Cursor getAllData() {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor res = sqLiteDatabase.rawQuery("SELECT * FROM " + GPX_LOG_INFO, null);
        return res;
    }

    public Cursor getDetails() {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor res = sqLiteDatabase.rawQuery("SELECT * FROM " + GPS_DETAIL, null);
        return res;
    }

    public ArrayList<HashMap<String, String>> getGPSDetails(){
        ArrayList<HashMap<String, String>> list = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + GPS_DETAIL +
                " WHERE sync_status = 'new'";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Move to first row
        cursor.moveToFirst();

        for(int i = 0; i < cursor.getCount();i++)
        {
            HashMap<String, String> result = new HashMap<>();

            result.put(FILE_COL1_ID, cursor.getString(0));
            result.put(FILE_COL2_DISTANCE, cursor.getString(1));
            result.put(FILE_COL3_SPEED, cursor.getString(2));

            cursor.moveToNext();
            list.add(result);
        }

        cursor.close();
        db.close();

        return list;
    }

    public ArrayList<HashMap<String, String>> getGPXLogInfo(){
        ArrayList<HashMap<String, String>> list = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + GPX_LOG_INFO +
                " WHERE sync_status = 'new'";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Move to first row
        cursor.moveToFirst();

        for(int i = 0; i < cursor.getCount();i++)
        {
            HashMap<String, String> result = new HashMap<>();

            result.put(LOG_COL1_ID, cursor.getString(0));
            result.put(LOG_COL2_POINT, cursor.getString(1));
            result.put(LOG_COL3_TIME, cursor.getString(2));
            result.put(LOG_COL4_LAT, cursor.getString(3));
            result.put(LOG_COL5_LNG, cursor.getString(4));
            result.put(LOG_COL6_ALT, cursor.getString(5));
            result.put(LOG_COL7_ACCURACY, cursor.getString(6));
            result.put(LOG_COL8_DESC, cursor.getString(7));

            cursor.moveToNext();
            list.add(result);
        }

        cursor.close();
        db.close();

        return list;
    }

    public void updateGPS(String gps) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + GPS_DETAIL +
                " SET sync_status = 'old' " +
                "WHERE " + FILE_COL1_ID + " = '" + gps + "'";
        db.execSQL(query);
        db.close();
    }

    public void updateGPX(String gpx){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + GPX_LOG_INFO +
                " SET sync_status = 'old' " +
                "WHERE " + LOG_COL1_ID + " = '" + gpx + "'";
        db.execSQL(query);
        db.close();
    }


}
