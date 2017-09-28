package porktraceability.com.gps;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import app.AppConfig;
import app.AppController;
import helper.NetworkUtil;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by marmagno on 3/21/2016.
 */

public class ExportData extends Activity {

    LatLng start;
    LatLng end;
    String selected;
    File file;

    public ExportData(){}

    public ExportData(LatLng start, LatLng end, String selected, File file) {
        this.start = start;
        this.end = end;
        this.selected = selected;
        this.file = file;
    }

    private static final String TAG = ExportData.class.getSimpleName();

    // Table GPS Detail
    public static final String KEY_FILEID = "FILE_ID";
    public static final String KEY_DISTANCE = "FILE_DISTANCE";
    public static final String KEY_SPEED = "FILE_SPEED";

    // Table GPX Log Info
    public static final String KEY_GPXID = "GPX_ID";
    public static final String KEY_LOGPOINT = "POINT";
    public static final String KEY_TIMEREC = "TIME_REC";
    public static final String KEY_LATITUDE = "LATITUDE";
    public static final String KEY_LONGTITUDE = "LONGITUDE";
    public static final String KEY_ALTITUDE = "ALTITUDE";
    public static final String KEY_ACCURACY = "ACCURACY";
    public static final String KEY_DESC = "DESCRIPTION";

    String[] gps_data = {};
    String[] gpx_logs = {};

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    DatabaseHelper db;
    private ProgressDialog progressDialog;
    private AlertDialog alertDialog;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.loading_syncall);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        db = new DatabaseHelper(this);

        //Create a new progress dialog.
        progressDialog = new ProgressDialog(ExportData.this);
        //Set the progress dialog to display a horizontal bar .
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //Set the dialog title to 'Loading...'.
        progressDialog.setTitle("Exporting Data. . .");
        //Set the dialog message to 'Loading application View, please wait...'.
        progressDialog.setMessage("Please wait...");
        //This dialog can't be canceled by pressing the back key.
        progressDialog.setCancelable(false);
        //This dialog isn't indeterminate.
        progressDialog.setIndeterminate(true);
        //Display the progress dialog.
        progressDialog.show();

    }

    @Override
    public void onStart(){
        super.onStart();

        int status = NetworkUtil.getConnectivityStatus(getApplicationContext());
        if(status == 0){
            displayAlert("Cannot establish connection to server.");
        } else {
            sendDataToServer();
        }
    }

    public void updateDB(){
        for (String gps : gps_data) {
            db.updateGPS(gps);
        }

        for (String gpx : gpx_logs) {
            db.updateGPX(gpx);
        }

        nextPage();
    }

    private void sendDataToServer() {
//        final String tag_string_send = "send_alldata";

        ArrayList<HashMap<String, String>> gps_details = db.getGPSDetails();
        ArrayList<HashMap<String, String>> gpx_log_info = db.getGPXLogInfo();

        final JSONObject allData = new JSONObject();

        try{
            JSONArray gps_jsonarray = new JSONArray();
            JSONArray gpx_jsonArray = new JSONArray();

            gps_data = new String[gps_details.size()];
            gpx_logs = new String[gpx_log_info.size()];

            for(int i = 0;i < gps_details.size();i++){
                HashMap<String, String> data = gps_details.get(i);
                JSONObject jsonObjectData = new JSONObject();
                jsonObjectData.put("date_logged", data.get(KEY_FILEID));
                jsonObjectData.put("total_distance", Double.valueOf(data.get(KEY_DISTANCE)));
                jsonObjectData.put("travel_speed", Float.valueOf(data.get(KEY_SPEED)));

                gps_jsonarray.put(jsonObjectData);

                gps_data[i] = data.get(KEY_FILEID);
            }

            for(int i = 0;i < gpx_log_info.size();i++){
                HashMap<String, String> data = gpx_log_info.get(i);
                JSONObject jsonObjectData = new JSONObject();

                jsonObjectData.put("log_point", Integer.valueOf(data.get(KEY_LOGPOINT)));
                jsonObjectData.put("time_record", data.get(KEY_TIMEREC));
                jsonObjectData.put("latitude", Double.parseDouble(data.get(KEY_LATITUDE)));
                jsonObjectData.put("longitude", Double.parseDouble(data.get(KEY_LONGTITUDE)));
                jsonObjectData.put("altitude", Double.parseDouble(data.get(KEY_ALTITUDE)));
                jsonObjectData.put("accuracy", Float.parseFloat(data.get(KEY_ACCURACY)));
                jsonObjectData.put("date_logged", data.get(KEY_DESC));
                gpx_jsonArray.put(jsonObjectData);

                gpx_logs[i] = data.get(KEY_GPXID);
            }

            allData.put("gps_detail", gps_jsonarray);
            allData.put("gpx_log_info", gpx_jsonArray);

            Log.d(TAG, allData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                AppConfig.URL_SENDNEWDATA, allData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Getting response : " + response.toString());

                        try {
                            //JSONObject resp = new JSONObject(response);
                            boolean error = response.getBoolean("error");

                            if(!error)
                                updateDB();
                            else {
                                String errorMsg = response.getString("error_message");
                                Log.e(TAG, "Error Response: " + errorMsg);
                                displayAlert("Error Response: " + errorMsg);
                            }

                        } catch (JSONException e) {
                            try {
                                e.printStackTrace();
                                Log.e(TAG, "JSON Error: " + response.toString(3));
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                                displayAlert("Error Response: " + e1.getMessage());
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "Volley error: " + error.getMessage());
                    displayAlert("Connection failed.");
            }
        }){

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json;charset=utf-8");
                return headers;
            }

        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                3000, // timeout in ms
                0, // no of retries
                1)); // backoff multiplier

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(request);//, tag_string_send);

    }

    public void nextPage(){
        finish();
    }

    public void displayAlert(String message){

        alertDialog = new AlertDialog.Builder(this)
                .setTitle(message)
                .setCancelable(false)
                .setMessage("Do you want to export data here?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //TODO: Do intense testing on this part
                        exportOffline();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        nextPage();
                    }
                }).show();
    }

    public void exportData() {
        try {
            FileOutputStream stream = new FileOutputStream(file, false);
            Cursor res = db.getAllData();
            //writes the kml file. version 2.2
            stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes());
            stream.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n".getBytes());
            stream.write("<Document>\n".getBytes());

            //specifies the style of the polyline when being rendered
            stream.write("<Style id=\"yellowLineGreenPoly\">\n".getBytes());
            stream.write("<LineStyle>\n".getBytes());
            stream.write("<color>7f00ffff</color>\n".getBytes());
            stream.write("<width>5</width>\n".getBytes());
            stream.write("</LineStyle>\n".getBytes());
            stream.write("</Style>\n".getBytes());

            //creates the polyline
            stream.write("<Placemark>\n".getBytes());
            stream.write("<styleUrl>#yellowLineGreenPoly</styleUrl>\n".getBytes());
            stream.write("<LineString>\n".getBytes());
            stream.write("<extrude>1</extrude>\n".getBytes());
            stream.write("<tesselate>1</tesselate>\n".getBytes());
            stream.write("<altitudeMode>absolute</altitudeMode>\n".getBytes());
            stream.write("<coordinates>\n".getBytes());
            while (res.moveToNext()) {
                if (res.getString(7).equals(selected)) {
                    String string = String.valueOf(res.getDouble(4)) + "," + String.valueOf(res.getDouble(3)) + ",0\n";
                    stream.write(string.getBytes());
                }
            }
            stream.write("</coordinates>\n".getBytes());
            stream.write("</LineString>\n".getBytes());
            stream.write("</Placemark>\n".getBytes());

            res.moveToFirst();//cursor is reset

            //adds a marker on the start of the trip
            stream.write("<Placemark>\n".getBytes());
            stream.write("<name>START</name>\n".getBytes());
            stream.write("<Point>\n".getBytes());
            stream.write("<coordinates>\n".getBytes());
            String temp = String.valueOf(start.longitude) + "," + String.valueOf(start.latitude) + ",0\n";
            stream.write(temp.getBytes());
            stream.write("</coordinates>\n".getBytes());
            stream.write("</Point>\n".getBytes());
            stream.write("</Placemark>\n".getBytes());

            while (res.moveToNext()){
                if(res.getString(7).equals(selected)) {
                    if(res.getInt(1) % 10 == 0) {
                        //adds markers every 10 log points
                        stream.write("<Placemark>\n".getBytes());
                        stream.write("<name>START</name>\n".getBytes());
                        stream.write("<Point>\n".getBytes());
                        stream.write("<coordinates>\n".getBytes());
                        String string = String.valueOf(res.getDouble(4)) + "," + String.valueOf(res.getDouble(3)) + ",0\n";
                        stream.write(string.getBytes());
                        stream.write("</coordinates>\n".getBytes());
                        stream.write("</Point>\n".getBytes());
                        stream.write("</Placemark>\n".getBytes());
                    }
                }
            }

            //adds a marker at the end of the trip
            stream.write("<Placemark>\n".getBytes());
            stream.write("<name>END</name>\n".getBytes());
            stream.write("<Point>\n".getBytes());
            stream.write("<coordinates>\n".getBytes());
            String temp1 = String.valueOf(end.longitude) + "," + String.valueOf(end.latitude) + ",0\n";
            stream.write(temp1.getBytes());
            stream.write("</coordinates>\n".getBytes());
            stream.write("</Point>\n".getBytes());
            stream.write("</Placemark>\n".getBytes());

            stream.write("</Document>\n".getBytes());
            stream.write("</kml>\n".getBytes());

            stream.flush();
            stream.close();

            Toast.makeText(ExportData.this, "Map Data Saved", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void exportOffline() {
        /* code from: http://stackoverflow.com/questions/23527767/ */
        boolean hasPermissionWrite =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
        boolean hasPermissionRead =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

        if (!(hasPermissionWrite || hasPermissionRead)) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        } else {
            exportData();
        }

        nextPage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    exportData();
                }
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        //Close the progress dialog
        if(progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        if(alertDialog != null && alertDialog.isShowing())
            alertDialog.dismiss();
    }
}
