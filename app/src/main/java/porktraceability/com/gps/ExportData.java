package porktraceability.com.gps;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ExportData extends Activity {

    private static final String TAG = ExportData.class.getSimpleName();
    private boolean tag_error = false;

    // Table Pen
    private static final String KEY_PENID = "pen_id";

    // Table Pig Breeds
    private static final String KEY_BREEDID = "breed_id";

    // Table Pig
    private static final String KEY_PIGID = "pig_id";
    private static final String KEY_BOARID = "boar_id";
    private static final String KEY_SOWID = "sow_id";
    private static final String KEY_FOSTER = "foster_sow";
    private static final String KEY_WEEKF = "week_farrowed";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_FARROWING = "farrowing_date";
    private static final String KEY_PIGSTAT = "pig_status";
    private static final String KEY_USER = "user";
    private static final String KEY_GNAME = "pig_batch";

    // Table Weight Record
    private static final String KEY_WRID = "record_id";
    private static final String KEY_RECDATE = "record_date";
    private static final String KEY_RECTIME = "record_time";
    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_REMARKS = "remarks";

    // Table Feeds
    private static final String KEY_FEEDID = "feed_id";

    // Table Feed Transaction
    private static final String KEY_FTID = "ft_id";
    private static final String KEY_QUANTITY = "quantity";
    private static final String KEY_UNIT = "unit";
    private static final String KEY_DATEGIVEN = "date_given";
    private static final String KEY_TIMEGIVEN = "time_given";
    private static final String KEY_PRODDATE = "prod_date";

    // Table Med Record
    private static final String KEY_MRID = "mr_id";
    private static final String KEY_MEDID = "med_id";

    // Table RFID Tags
    private static final String KEY_TAGID = "tag_id";
    private static final String KEY_TAGRFID = "tag_rfid";
    private static final String KEY_TAGSTAT = "status";
    private static final String KEY_LABEL = "label";

    // Table User Transaction
    private static final String KEY_TRANSID = "trans_id";
    private static final String KEY_USERID = "user_id";
    private static final String KEY_DATEEDITED = "date_edited";
    private static final String KEY_IDEDITED = "id_edited";
    private static final String KEY_TYPEEDITED = "type_edited";
    private static final String KEY_PREVVAL = "prev_value";
    private static final String KEY_CURVAL = "curr_value";
    private static final String KEY_FLAG = "flag";

    String[] pigs = {};
    String[] weights = {};
    String[] fed_lists = {};
    String[] med_lists = {};
    String[] rfid_tags = {};
    String[] user_trans = {};

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
        progressDialog.setTitle("Exporting Data to Server.");
        //Set the dialog message to 'Loading application View, please wait...'.
        progressDialog.setMessage("Sending, please wait...");
        //This dialog can't be canceled by pressing the back key.
        progressDialog.setCancelable(false);
        //This dialog isn't indeterminate.
        progressDialog.setIndeterminate(true);
        //Display the progress dialog.
        progressDialog.show();

    }
/*
    @Override
    public void onStart(){
        super.onStart();

        int status = NetworkUtil.getConnectivityStatus(getApplicationContext());
        if(status == 0){
            displayAlert("Cannot establish connection to server.");
        } else {
            sendDataToServer();
        }
    }*/

    public void updateDB(){
        /*for (String pig : pigs) {
            db.updatePig(pig);
        }

        for (String weight : weights) {
            db.updateWeightRec(weight);
        }

        for (String fed_list : fed_lists) {
            db.updateFeedRec(fed_list);
        }

        for (String med_list : med_lists) {
            db.updateMedRec(med_list);
        }

        for(String rfid_tag : rfid_tags) {
            db.updateTagStat(rfid_tag);
        }

        for(String trans_id : user_trans) {
            db.updateUserTrans(trans_id);
        }*/

        nextPage();
    }

    /*private void sendDataToServer() {
//        final String tag_string_send = "send_alldata";

        ArrayList<HashMap<String, String>> weight_records = db.getWeightRecs();
        ArrayList<HashMap<String, String>> feed_records = db.getFeedRecs();
        ArrayList<HashMap<String, String>> med_records = db.getMedRecs();
        ArrayList<HashMap<String, String>> newPigs = db.getNewPigs();
        ArrayList<HashMap<String, String>> updatedTags = db.getUpdatedTags();
        ArrayList<HashMap<String, String>> userTrans = db.getUserTransactions();

        final JSONObject allData = new JSONObject();
        final JSONObject jsonObject = new JSONObject();
        final JSONArray allDataArray = new JSONArray();

        try{

            JSONArray wr_jsonArray = new JSONArray();
            JSONArray ft_jsonArray = new JSONArray();
            JSONArray mr_jsonArray = new JSONArray();
            JSONArray pig_jsonArray = new JSONArray();
            JSONArray rfid_jsonArray = new JSONArray();
            JSONArray userTrans_jsonArray = new JSONArray();

            pigs = new String[newPigs.size()];
            fed_lists = new String[feed_records.size()];
            weights = new String[weight_records.size()];
            med_lists = new String[med_records.size()];
            rfid_tags = new String[updatedTags.size()];
            user_trans = new String[userTrans.size()];

            for(int i = 0;i < newPigs.size();i++){
                HashMap<String, String> data = newPigs.get(i);
                JSONObject jsonObjectData = new JSONObject();
                jsonObjectData.put(KEY_PIGID, data.get(KEY_PIGID));
                jsonObjectData.put(KEY_BOARID, data.get(KEY_BOARID));
                jsonObjectData.put(KEY_SOWID, data.get(KEY_SOWID));
                jsonObjectData.put(KEY_FOSTER, data.get(KEY_FOSTER));
                jsonObjectData.put(KEY_WEEKF, data.get(KEY_WEEKF));
                jsonObjectData.put(KEY_GENDER, data.get(KEY_GENDER));
                jsonObjectData.put(KEY_FARROWING, data.get(KEY_FARROWING));
                jsonObjectData.put(KEY_PIGSTAT, data.get(KEY_PIGSTAT));
                jsonObjectData.put(KEY_PENID, data.get(KEY_PENID));
                jsonObjectData.put(KEY_BREEDID, data.get(KEY_BREEDID));
                jsonObjectData.put(KEY_USER, data.get(KEY_USER));
                jsonObjectData.put(KEY_GNAME, data.get(KEY_GNAME));
                pig_jsonArray.put(jsonObjectData);

                pigs[i] = data.get(KEY_PIGID);
            }

            for(int i = 0;i < weight_records.size();i++){
                HashMap<String, String> data = weight_records.get(i);
                JSONObject jsonObjectData = new JSONObject();
                jsonObjectData.put(KEY_RECDATE, data.get(KEY_RECDATE));
                jsonObjectData.put(KEY_RECTIME, data.get(KEY_RECTIME));
                jsonObjectData.put(KEY_WEIGHT, data.get(KEY_WEIGHT));
                jsonObjectData.put(KEY_PIGID, data.get(KEY_PIGID));
                jsonObjectData.put(KEY_REMARKS, data.get(KEY_REMARKS));
                jsonObjectData.put(KEY_USER, data.get(KEY_USER));
                wr_jsonArray.put(jsonObjectData);

                weights[i] = data.get(KEY_WRID);
            }

            for(int i = 0;i < feed_records.size();i++){
                HashMap<String, String> data = feed_records.get(i);
                JSONObject jsonObjectData = new JSONObject();
                jsonObjectData.put(KEY_QUANTITY, data.get(KEY_QUANTITY));
                jsonObjectData.put(KEY_UNIT, data.get(KEY_UNIT));
                jsonObjectData.put(KEY_DATEGIVEN, data.get(KEY_DATEGIVEN));
                jsonObjectData.put(KEY_TIMEGIVEN, data.get(KEY_TIMEGIVEN));
                jsonObjectData.put(KEY_PIGID, data.get(KEY_PIGID));
                jsonObjectData.put(KEY_FEEDID, data.get(KEY_FEEDID));
                jsonObjectData.put(KEY_PRODDATE, data.get(KEY_PRODDATE));
                ft_jsonArray.put(jsonObjectData);

                fed_lists[i] = data.get(KEY_FTID);
            }

            for(int i = 0;i < med_records.size();i++){
                HashMap<String, String> data = med_records.get(i);
                JSONObject jsonObjectData = new JSONObject();
                jsonObjectData.put(KEY_DATEGIVEN, data.get(KEY_DATEGIVEN));
                jsonObjectData.put(KEY_TIMEGIVEN, data.get(KEY_TIMEGIVEN));
                jsonObjectData.put(KEY_QUANTITY, data.get(KEY_QUANTITY));
                jsonObjectData.put(KEY_UNIT, data.get(KEY_UNIT));
                jsonObjectData.put(KEY_PIGID, data.get(KEY_PIGID));
                jsonObjectData.put(KEY_MEDID, data.get(KEY_MEDID));
                mr_jsonArray.put(jsonObjectData);

                med_lists[i] = data.get(KEY_MRID);
            }

            for(int i = 0;i < updatedTags.size();i++){
                HashMap<String, String> data = updatedTags.get(i);
                JSONObject jsonObjectData = new JSONObject();
                jsonObjectData.put(KEY_TAGID, data.get(KEY_TAGID));
                jsonObjectData.put(KEY_TAGRFID, data.get(KEY_TAGRFID));
                jsonObjectData.put(KEY_PIGID, data.get(KEY_PIGID));
                jsonObjectData.put(KEY_LABEL, data.get(KEY_LABEL));
                jsonObjectData.put(KEY_TAGSTAT, data.get(KEY_TAGSTAT));
                rfid_jsonArray.put(jsonObjectData);

                rfid_tags[i] = data.get(KEY_TAGID);
            }

            for(int i = 0;i < userTrans.size();i++){
                HashMap<String, String> data = userTrans.get(i);
                JSONObject jsonObjectData = new JSONObject();
                jsonObjectData.put(KEY_USERID, data.get(KEY_USERID));
                jsonObjectData.put(KEY_DATEEDITED, data.get(KEY_DATEEDITED));
                jsonObjectData.put(KEY_IDEDITED, data.get(KEY_IDEDITED));
                jsonObjectData.put(KEY_TYPEEDITED, data.get(KEY_TYPEEDITED));
                jsonObjectData.put(KEY_PREVVAL, data.get(KEY_PREVVAL));
                jsonObjectData.put(KEY_CURVAL, data.get(KEY_CURVAL));
                jsonObjectData.put(KEY_PIGID, data.get(KEY_PIGID));
                jsonObjectData.put(KEY_FLAG, data.get(KEY_FLAG));
                userTrans_jsonArray.put(jsonObjectData);

                user_trans[i] = data.get(KEY_TRANSID);
            }


         *//*   jsonObject.put("weight_record", wr_jsonArray);
            jsonObject.put("feed_transaction", ft_jsonArray);
            jsonObject.put("med_record", mr_jsonArray);
            jsonObject.put("pig", pig_jsonArray);
            jsonObject.put("rfid_tags", rfid_jsonArray);
            jsonObject.put("user_transaction", userTrans);

            allDataArray.put(jsonObject);

            allData.put("response", allDataArray);*//*

            allData.put("pig", pig_jsonArray);
            allData.put("weight_record", wr_jsonArray);
            allData.put("feed_transaction", ft_jsonArray);
            allData.put("med_record", mr_jsonArray);
            allData.put("rfid_tags", rfid_jsonArray);
            allData.put("user_transaction", userTrans_jsonArray);

            //Toast.makeText(this, String.valueOf(allData), Toast.LENGTH_LONG).show();
            Log.d(TAG,  String.valueOf(allData));
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

    }*/

    public void nextPage(){
        Intent i = new Intent();
        i.setClass(ExportData.this, MapsActivity.class);
        startActivity(i);
        finish();
    }

   /* public void displayAlert(String message){

        alertDialog = new AlertDialog.Builder(this)
                .setTitle(message)
                .setCancelable(false)
                .setMessage("Do you want to import offline?")
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
    }*/

    /*public void exportOffline() {
        *//* code from: http://stackoverflow.com/questions/23527767/ *//*
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
            db.exportTables(this);
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
                    db.exportTables(this);
                }
            }
        }
    }*/

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
