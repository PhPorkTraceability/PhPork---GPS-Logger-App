package porktraceability.com.gps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener, View.OnDragListener{
    private static final String LOGCAT = MainActivity.class.getSimpleName();
    String function = "";

    public ImageView start_log;
    public ImageView view_map;
    public LinearLayout bot_cont;
    public FloatingActionButton back;
    public ImageView logo;
    public FloatingActionButton help;
    Toolbar toolbar;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


       // toolbar = (Toolbar) findViewById(R.id.toolbar);
      //  setSupportActionBar(toolbar);

        start_log = (ImageView) findViewById(R.id.start_log);
        view_map = (ImageView) findViewById(R.id.view_map);
        bot_cont = (LinearLayout) findViewById(R.id.bottom_container);
        back = (FloatingActionButton) findViewById(R.id.action_back);
        logo = (ImageView)  findViewById(R.id.logo);

        start_log.setOnTouchListener(this);
        view_map.setOnTouchListener(this);
        bot_cont.setOnDragListener(this);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Thank You for using GPS LOGGER, Bye!!!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, SplashOut.class));
                finish();
            }
        });

    }



    public boolean onDrag(View v, DragEvent e) {
        int action = e.getAction();
        switch(action){
            case DragEvent.ACTION_DRAG_STARTED:
                Log.d(LOGCAT, "Drag event started");
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                Log.d(LOGCAT, "Drag event entered into "+ v.toString());
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                Log.d(LOGCAT, "Drag event exited from " + v.toString());
                break;
            case DragEvent.ACTION_DROP:
                TextView tv_drag = (TextView) findViewById(R.id.tv_dragHere);
                TextView tv_subs = (TextView) findViewById(R.id.tv_subs);
                View view = (View) e.getLocalState();
                ViewGroup from = (ViewGroup) view.getParent();
                from.removeView(view);
                view.invalidate();
                LinearLayout to = (LinearLayout) v;
                to.addView(view);
                to.removeView(tv_drag);
                tv_subs.setVisibility(View.VISIBLE);
                view.setVisibility(View.VISIBLE);

                int id = view.getId();
                function = findViewById(id).getTag().toString();
                String  function2 = function.toUpperCase();

                String viewmap= "viewmap";
                String viewmapU= viewmap.toUpperCase();
                String startlog= "startlog";
                String startlogU= startlog.toUpperCase();
                int vid = to.getId();
                if(findViewById(vid) == findViewById(R.id.bottom_container)) {
                    Toast.makeText(MainActivity.this, "Chosen " + function.toUpperCase(),
                            Toast.LENGTH_LONG).show();
                    if (function2.equals(viewmapU)) {
                        Intent x = new Intent(MainActivity.this, MapsActivity.class);
                        x.putExtra("function", function);
                        startActivity(x);
                        finish();

                    }  if (function2.equals(startlogU)){
                        Intent i = new Intent(MainActivity.this, StartLog2.class);
                        i.putExtra("function", function);
                        startActivity(i);
                        finish();


                    }

                }

                Log.d(LOGCAT, "Dropped " + function);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                Log.d(LOGCAT, "Drag ended");
                break;
            default:
                break;
        }
        return true;

    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDrag(null, shadowBuilder, v, 0);
            return true;
        }
        else { return false; }
    }

    @Override
    public void onBackPressed(){super.onBackPressed(); finish(); }
}



