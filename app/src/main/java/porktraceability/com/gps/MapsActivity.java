package porktraceability.com.gps;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    DatabaseHelper databaseHelper;

    private GoogleMap mMap;
    private ImageView export;
    public ImageView back;
    public ImageView delete;
    private FloatingActionButton viewdata;
    private FloatingActionButton camera;
    private FloatingActionButton log_files;

    LatLng start;
    LatLng end;
    String startDate;
    String endDate;
    TextView startLat;
    TextView startLng;
    TextView endLat;
    TextView endLng;
    TextView tstartDate;
    TextView tendDate;
    TextView distanceTraveled;
    TextView averageSpeed;
    String selected = "";
    Boolean data = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        databaseHelper = new DatabaseHelper(this);//instantiates the database

        //instantiates the map fragment
        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            Toast.makeText(MapsActivity.this, "Map Loaded Successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.d("MAP", e.toString());
        }

        //references the buttons from the layout
        camera = (FloatingActionButton) findViewById(R.id.camera);
        export = (ImageView) findViewById(R.id.export);
        back = (ImageView) findViewById(R.id.action_back);
        delete = (ImageView) findViewById(R.id.action_delete);
        viewdata = (FloatingActionButton) findViewById(R.id.viewdata);
        log_files = (FloatingActionButton) findViewById(R.id.action_open);

        log_files.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V){
                Toast.makeText(MapsActivity.this, "Select Trip", Toast.LENGTH_LONG).show();
                final AlertDialog viewlogs = new AlertDialog.Builder(MapsActivity.this).create();//creates a final variable to be referenced inside the OnClickListener
                viewlogs.show();
                Cursor res = databaseHelper.getAllData();

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

                lp.copyFrom(viewlogs.getWindow().getAttributes());
                lp.width = 1000;
                lp.height = 1500;
                lp.x=0;
                lp.y=0;

                viewlogs.getWindow().setContentView(R.layout.log_scroller);

                LinearLayout scroll_view = (LinearLayout) viewlogs.findViewById(R.id.scrollable);
                LinearLayout buttons = (LinearLayout) viewlogs.findViewById(R.id.buttons);
                Button clear_log = (Button) viewlogs.findViewById(R.id.clear_logs);
                buttons.removeView(clear_log);

                String temp, prev = "";
                res.moveToLast();//moves to last to put the latest log at the top
                while(res.moveToPrevious()) {
                    temp = res.getString(7);//gets the start date of the current row

                    if(!prev.equals(temp)){//if the previous date is equal to the current date, does not make a new button
                        Button addButton = new Button(MapsActivity.this);
                        addButton.setText(temp);

                        final String date = temp;//creates a final variable to be referenced inside the OnClickListener
                        addButton.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View v){
                                selected = date;//updates the variable
                                getData(mMap);//updates the map according to the selected date
                                viewlogs.dismiss();//closes the dialogbox
                            }
                        });
                        scroll_view.addView(addButton);//adds the newly created button to the view
                        prev = temp;//updates prev
                    }
                }
            }
        });

        viewdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog viewdata = new AlertDialog.Builder(MapsActivity.this).create();
                viewdata.show();

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

                lp.copyFrom(viewdata.getWindow().getAttributes());
                lp.width = 1000;
                lp.height = 1500;
                lp.x=0;
                lp.y=0;

                viewdata.getWindow().setContentView(R.layout.viewdata);

                startLat = (TextView) viewdata.findViewById(R.id.start_latitude_data);
                startLng = (TextView) viewdata.findViewById(R.id.start_longitude_data);
                endLat = (TextView) viewdata.findViewById(R.id.end_latitude_data);
                endLng = (TextView) viewdata.findViewById(R.id.end_longitude_data);
                tstartDate = (TextView) viewdata.findViewById(R.id.departure_time_data);
                tendDate = (TextView) viewdata.findViewById(R.id.arrival_time_data);
                distanceTraveled = (TextView) viewdata.findViewById(R.id.distance_traveled_data);
                averageSpeed = (TextView) viewdata.findViewById(R.id.average_speed_data);

                if(data) { //checks if data is present to prevent errors

                    startLat.setText(String.valueOf(start.latitude));
                    startLng.setText(String.valueOf(start.longitude));
                    endLat.setText(String.valueOf(end.latitude));
                    endLng.setText(String.valueOf(end.longitude));
                    tstartDate.setText(startDate);
                    tendDate.setText(endDate);

                    Cursor res = databaseHelper.getDetails();

                    while(res.moveToNext()){//gets the extra details from the database
                        if(res.getString(0).equals(selected)){
                            distanceTraveled.setText(String.valueOf(res.getLong(1))+" m");
                            averageSpeed.setText(String.valueOf(res.getLong(2))+" m/s");
                        }
                    }
                }
            }
        });

        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String mPath = Environment.getExternalStorageDirectory().toString() + "/" + selected + ".kml";
                File file = new File(mPath);//creates a kml file with the log name as the file name

                if(data) {
                    try {
                        FileOutputStream stream = new FileOutputStream(file, false);
                        Cursor res = databaseHelper.getAllData();
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

                        Toast.makeText(MapsActivity.this, "Map Data Saved", Toast.LENGTH_SHORT).show();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MapsActivity.this, "No Data Selected", Toast.LENGTH_LONG).show();
                }
            }
        });

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date now = new Date();
                android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

                captureScreen(now);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MapsActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        });


        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                final AlertDialog viewlogs = new AlertDialog.Builder(MapsActivity.this).create();
                viewlogs.show();
                Cursor res = databaseHelper.getAllData();

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

                lp.copyFrom(viewlogs.getWindow().getAttributes());
                lp.width = 1000;
                lp.height = 1500;
                lp.x=0;
                lp.y=0;

                viewlogs.getWindow().setContentView(R.layout.log_scroller);

                final LinearLayout scroll_view = (LinearLayout) viewlogs.findViewById(R.id.scrollable);
                LinearLayout buttons = (LinearLayout) viewlogs.findViewById(R.id.buttons);
                TextView text = (TextView) viewlogs.findViewById(R.id.textGoesHere);
                buttons.removeView(text);

                Button clear_logs = (Button) viewlogs.findViewById(R.id.clear_logs);
                clear_logs.setOnClickListener(new View.OnClickListener(){ //creates a prompt to make sure that the user wants to delete all the logs
                    @Override
                    public void onClick(View v){//removes all the logs
                        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                        builder.setMessage("Are you sure you want to delete all logs?")
                                .setCancelable(false)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        databaseHelper.clearLogs();//deletes all the logs
                                        mMap.clear();//clears the map
                                        scroll_view.removeAllViews();//removes all the buttons in the scroll view
                                        data = false;//resets the boolean data to prevent errors when opening view details
                                        viewlogs.dismiss();//closes the alert dialog box
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                });

                String temp, prev = "";
                res.moveToLast();
                while(res.moveToPrevious()) {
                    temp = res.getString(7);

                    if(!prev.equals(temp)){
                        final Button addButton = new Button(MapsActivity.this);
                        addButton.setText(temp);

                        final String date = temp;
                        addButton.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View v){//creates a prompt to make sure that the user wants to delete a specific log
                                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                                builder.setMessage("Are you sure you want to delete " + date + "?")
                                        .setCancelable(false)
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                // fire an intent go to your next activity
                                                Toast.makeText(MapsActivity.this, "CLEARED " + date, Toast.LENGTH_SHORT).show();

                                                databaseHelper.deleteRow(date);//deletes the selected log
                                                scroll_view.removeView(addButton);//removes it from the view
                                                mMap.clear();//clears the map
                                                data = false;//resets the data to prevent errors
                                            }
                                        })
                                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.show();
                            }
                        });
                        scroll_view.addView(addButton);
                        prev = temp;
                    }
                }
            }

        });

    }

    public void captureScreen(final Date date)
    {
        final Bitmap[] bitmap = new Bitmap[1];
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback()
        {

            @Override
            public void onSnapshotReady(Bitmap snapshot)
            {
                // TODO Auto-generated method stub
                bitmap[0] = snapshot;

                OutputStream fout;

                try {
                    // image naming and path  to include sd card  appending name you choose for file
                    String mPath = Environment.getExternalStorageDirectory().toString() + "/" + date + ".jpg";

                    // create bitmap screen capture
                    File imageFile = new File(mPath);//creates a jpg file

                    FileOutputStream outputStream = new FileOutputStream(imageFile);
                    int quality = 100;
                    bitmap[0].compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                    outputStream.flush();
                    outputStream.close();

                    openScreenshot(imageFile);
                } catch (Throwable e) {
                    // Several error may come out with file handling or OOM
                    e.printStackTrace();
                }
            }
        };

        mMap.snapshot(callback);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    public void getData(GoogleMap googlemap){
        Cursor res = databaseHelper.getAllData();
        PolylineOptions options = new PolylineOptions().width(5).color(Color.rgb(157, 206, 149));

        mMap.clear();//clears the map
        if (res.getCount() == 0) {
            Toast.makeText(MapsActivity.this, "ERROR: No Data Found", Toast.LENGTH_SHORT).show();
            Log.d("MapsActivity Data", "ERROR: No Data Found");
            return;
        }

        while(res.moveToNext()){
            if(res.getString(7).equals(selected)) {//adds the point where the start date is equal to what the user selected
                LatLng point = new LatLng(res.getDouble(3), res.getDouble(4));
                options.add(point);
                if(res.getInt(1) == 1 ){//adds a marker at the start of the trip
                    googlemap.addMarker(new MarkerOptions().position(point).title("START\n"));
                    start = point;//updates the information for later use in the view details
                    startDate = res.getString(2);
                    googlemap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15));//zooms in on the starting marker
                }
                else if(res.getInt(1) % 10 == 0)//adds a marker every 10 points it gets
                    googlemap.addMarker(new MarkerOptions().position(point));

                end = point;//updates the information for later use
                endDate = res.getString(2);
            }
        }

        googlemap.addPolyline(options);//creates the polyline connecting all the points that were scanned
        googlemap.addMarker(new MarkerOptions().position(end).title("END\n"));//adds a marker at the end of the trip

        data = true;//updates the boolean to prompt that data is ready for the view details button
    }

}
