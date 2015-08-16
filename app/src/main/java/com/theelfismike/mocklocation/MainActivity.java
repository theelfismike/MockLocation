package com.theelfismike.mocklocation;

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks {

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private View touchReceptor;
    private List<LatLng> mLatlngs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        touchReceptor = findViewById(R.id.touchReceptor);
        touchReceptor.setOnTouchListener(new TouchReceptorListener());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

    }

    private Location getLastKnownLocationSync() {
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_reset) {
            Toast.makeText(this, "RESET", Toast.LENGTH_LONG).show();
            if (map != null) {
                mLatlngs.clear();
                map.clear();
            }
            return true;
        } else if (id == R.id.action_draw) {
            Toast.makeText(this, "DRAW", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = getLastKnownLocationSync();
        LatLng lastKnown = new LatLng(location.getLatitude(), location.getLongitude());

        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnown, 13));

        map.addMarker(new MarkerOptions()
                .title("Sydney")
                .snippet("The most populous city in Australia.")
                .position(lastKnown));
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    private class TouchReceptorListener implements View.OnTouchListener {
        private final int slop;
        private Point downPoint;
        private boolean moved;

        public TouchReceptorListener() {
            slop = ViewConfiguration.get(MainActivity.this).getScaledTouchSlop();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!mGoogleApiClient.isConnected() || map == null) {
                return false;
            }
            Point point = new Point();
            point.x = (int) event.getX();
            point.y = (int) event.getY();
            LatLng geoPoint = map.getProjection().fromScreenLocation(point);

            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (!moved) {
                        map.addMarker(new MarkerOptions()
                                .title("Sydney")
                                .snippet("The most populous city in Australia.")
                                .position(geoPoint));
                    }
                case MotionEvent.ACTION_CANCEL: //FALL THRU
                    downPoint = null;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (downPoint != null) {
                        if (moveGreaterThanSlop(point)) {
                            moved = true;
                            mLatlngs.add(geoPoint);
                            PolylineOptions mPolylineOptions = new PolylineOptions();
                            mPolylineOptions.color(Color.RED);
                            mPolylineOptions.width(3);
                            mPolylineOptions.addAll(mLatlngs);
                            map.addPolyline(mPolylineOptions);
                        } else {
                            // ignoring small move event
                        }
                    } else {
                        downPoint = point;
                    }
                    break;

                case MotionEvent.ACTION_DOWN:
                    downPoint = point;
                    moved = false;
                    break;
            }


            return true;
        }

        private boolean moveGreaterThanSlop(Point point) {
            return Math.abs(point.x - downPoint.x) > slop
                    && Math.abs(point.y - downPoint.y) > slop;
        }
    }
}
