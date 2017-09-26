package porktraceability.com.gps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class StartLog2 extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, OnMapReadyCallback {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    DatabaseHelper databaseHelper;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String dateTimeStarted;
    private int i=0;

    private static final String LOGCAT = StartLog2.class.getSimpleName();
    TextView textViewTime;
    TextView textViewLat;
    TextView textViewLng;
    TextView textViewDistance;
    TextView textViewSpd;
    TextView alertTime;
    Boolean update = false;

    public FloatingActionButton stop;
    public FloatingActionButton details;
    public FloatingActionButton back;
    public double latitude;
    public double longitude;
    public long timeElapsed;
    public List<LatLng> points = new ArrayList<LatLng>();
    public LatLng point, prevPoint;
    public boolean isRunning = false;
    public Runnable UI_UPDATE_RUNNABLE;
    Handler handler = new Handler();
    public Chronometer stopWatch;
    public long distanceTraveled = 0;
    public long speed = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapslog);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        databaseHelper = new DatabaseHelper(this); //instantiates the database

        stopWatch = (Chronometer) findViewById(R.id.chrono);

        dateTimeStarted = DateFormat.getDateTimeInstance().format(new Date());

        stop = (FloatingActionButton) findViewById(R.id.stop_log);
        details = (FloatingActionButton)findViewById(R.id.details);
        back = (FloatingActionButton) findViewById(R.id.action_back);
        textViewTime = (TextView) findViewById(R.id.chronometer);

        //instantiates the map fragment
        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        } catch (Exception e) {
            Log.d("MAP", e.toString());
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(AppIndex.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)        // requests for the location every second
                .setFastestInterval(500); // if a location is available at half a second, takes that instead

        //thread to update the map every 5 seconds
        UI_UPDATE_RUNNABLE = new Runnable() {

            @Override
            public void run() {
                updateMap();//Method that will get employee location and draw it on map
                handler.postDelayed(this, 5000);
            }
        };

        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(StartLog2.this, MainActivity.class);
                databaseHelper.deleteRow(dateTimeStarted); //deletes the logs of the session if back is chosen
                startActivity(i);
                finish();

            }

        });


        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                databaseHelper.insertDataFile(dateTimeStarted, distanceTraveled, speed); //inserts other details about the trip
                Intent i = new Intent(StartLog2.this, MapsActivity.class);
                startActivity(i);
                finish();
            }

        });

        details.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog viewdetails = new AlertDialog.Builder(StartLog2.this).create();
                viewdetails.show();

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

                lp.copyFrom(viewdetails.getWindow().getAttributes());
                lp.width = 1000;
                lp.height = 1500;
                lp.x=0;
                lp.y=0;

                viewdetails.getWindow().setContentView(R.layout.viewdetails);

                //references the textviews from the view
                textViewLat = (TextView) viewdetails.findViewById(R.id.latitude_data);
                textViewLng = (TextView) viewdetails.findViewById(R.id.longitude_data);
                textViewSpd = (TextView) viewdetails.findViewById(R.id.speed_data);;
                textViewDistance = (TextView) viewdetails.findViewById(R.id.distance_traveled_data);
                alertTime = (TextView) viewdetails.findViewById(R.id.time_elapsed_data);

                //boolean to see if the textviews from the layout are already referenced
                update = true;
            }

        });
    }



    /***************************************START OF BACKEND*************************************************/

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();

    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOGCAT, "GoogleApiClient connection has been suspended. Please reconnect");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(LOGCAT, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        //checks if the location accuracy is less than 30m, if more than 30m, it does not record the location.
        if(location.getAccuracy()<30){
            //inserts the log points to the database
            databaseHelper.insertLogFile(
                    i+=1,
                    DateFormat.getTimeInstance().format(new Date()).toString(),
                    location.getLatitude(), location.getLongitude(),
                    location.getAltitude(),
                    location.getAccuracy(),
                    dateTimeStarted
            );

            //gets the location's coordinates
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            //instantiates a new LatLng
            point = new LatLng(location.getLatitude(), location.getLongitude());

            //checks if the update map and the stopwatch is already running
            if(isRunning == false) {
                handler.postDelayed(UI_UPDATE_RUNNABLE, 5000);

                //formats the chronometer to HH:MM:SS
                stopWatch.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener(){
                    @Override
                    public void onChronometerTick(Chronometer cArg) {
                        long time = SystemClock.elapsedRealtime() - cArg.getBase();
                        timeElapsed = time;
                        int h   = (int)(time /3600000);
                        int m = (int)(time - h*3600000)/60000;
                        int s= (int)(time - h*3600000- m*60000)/1000 ;
                        String hh = h < 10 ? "0"+h: h+"";
                        String mm = m < 10 ? "0"+m: m+"";
                        String ss = s < 10 ? "0"+s: s+"";
                        cArg.setText(hh+":"+mm+":"+ss);
                        if(update) //checks if the textview is already referenced, if it is updates the textview every second.
                            alertTime.setText(cArg.getText());
                    }
                });
                stopWatch.setBase(SystemClock.elapsedRealtime());
                stopWatch.start();

                isRunning = true;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    public void updateMap(){
        mMap.clear(); //clears the map first

        points.add(point);//adds the current point to the array of points
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17));//zooms the camera to the new point

        mMap.addMarker(new MarkerOptions().position(point));//adds a marker to the new point

        PolylineOptions options = new PolylineOptions().width(5).color(Color.rgb(157, 206, 149));//formats the polyline
        for (int i = 0; i < points.size(); i++) {//adds all the recorded points since the start of the log to the polyline
            LatLng point1 = points.get(i);
            options.add(point1);
        }

        mMap.addPolyline(options);//adds the polyline to the map

        if(prevPoint != null)//checks if there are previous points, if there are, calculates the distance between the previous point to the current point
            distanceBetween(prevPoint, point);

        long time = (timeElapsed/1000);
        if(time!=0)
            speed = (long) distanceTraveled/time;

        prevPoint = point;//updates the value of the previous point

        if(update){//checks if the textviews are referenced, if yes updates the textviews' data
            textViewLat.setText(String.valueOf(latitude));
            textViewLng.setText(String.valueOf(longitude));
            textViewDistance.setText(String.valueOf(distanceTraveled)+" m");
            textViewSpd.setText(String.valueOf(speed)+" m/s");
        }

    }

    private void distanceBetween(LatLng latLng1, LatLng latLng2) {
        //computes the distance traveled of the device
        Location loc1 = new Location(LocationManager.GPS_PROVIDER);
        Location loc2 = new Location(LocationManager.GPS_PROVIDER);

        loc1.setLatitude(latLng1.latitude);
        loc1.setLongitude(latLng1.longitude);

        loc2.setLatitude(latLng2.latitude);
        loc2.setLongitude(latLng2.longitude);

        distanceTraveled += loc1.distanceTo(loc2);
    }
}