package com.example.myapplication2.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maciej Plewko on 04/17/14.
 */
public class MapActivity extends Activity {

    private static final float DEGREES_OF_TILT = 30.0f;
    private static final int ANIMATION_DURATION_MS = 1000;
    public static float currentZoom = 16;
    private GoogleMap googleMap;
    private LatLng centerPoint;
    private List<LatLng> route = new ArrayList<LatLng>();
    private List<Marker> movingMarkers = new ArrayList<Marker>();
    private boolean mapCreated = false;
    private Thread thread;
    private Handler handler = new Handler();
    private int loopCounter = 0;
    //single move time [ms]
    final private long duration = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        initializeRouteList();
        mapCreated = initializeMap();

        //infinite thread loop
        thread = new Thread() {
            public void run() {
                // do something here
                Log.d("TAG", "local Thread sleeping");
                handler.postDelayed(this, duration);
                setMarkersAnimation();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePlayServices();
        if(!mapCreated){
            if(!initializeMap()){
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        handler.removeCallbacks(thread);
        handler.postDelayed(thread, 0);
        Log.d("TAG", "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();

        handler.removeCallbacks(thread);
        Log.d("TAG", "onPause");
    }

    private void setMarkersAnimation() {
        if (loopCounter < route.size() - 1) {
            loopCounter++;
        } else {
            loopCounter = 0;
        }

        //going through all markers and animate them to the next point
        for (int i = 0; i < movingMarkers.size(); i++) {
            if ((i + loopCounter) <= route.size() - 1) {
                animateMarker(movingMarkers.get(i), route.get(i + loopCounter), false);
            } else {
                animateMarker(movingMarkers.get(i), route.get(Math.abs(route.size() - i - loopCounter)), false);
            }
        }
    }

    /**
     * function to initialize list of route points.
     * */
    private void initializeRouteList() {
        route.add(0, new LatLng(53.4275, 14.5518));
        route.add(1, new LatLng(53.4275, 14.5538));
        route.add(2, new LatLng(53.4295, 14.5538));
        route.add(3, new LatLng(53.4295, 14.5498));
        route.add(4, new LatLng(53.4275, 14.5498));
    }

    private void checkGooglePlayServices() {
        // Check status of Google Play Services
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        // Check Google Play Service Available
        try {
            if (status != ConnectionResult.SUCCESS) {
                int RQS_GooglePlayServices = 1;
                GooglePlayServicesUtil.getErrorDialog(status, this, RQS_GooglePlayServices).show();
            }
        } catch (Exception e) {
            Log.e("Error: GooglePlayServiceUtil: ", "", e);
        }
    }

    /**
     * function to load map. If map is not created it will create it for you
     * */
    private boolean initializeMap() {
        try{
            if (googleMap == null) {
                FragmentManager fragmentManager = getFragmentManager();
                Fragment fragment = fragmentManager.findFragmentById(R.id.map);
                googleMap = ((MapFragment) fragment).getMap();

                // check if map is created successfully or not
                if (googleMap == null) {
                    return false;
                }
            }

            googleMap.setMyLocationEnabled(true);
            googleMap.setBuildingsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(false);
            googleMap.getUiSettings().setZoomControlsEnabled(false);
            googleMap.getUiSettings().setZoomGesturesEnabled(true);
            googleMap.getUiSettings().setRotateGesturesEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            setCenterPointOfMap(new LatLng(53.4285, 14.5528));
            moveCamera(centerPoint);

            //adding as many markers as route points to the map
            for (LatLng aRoute : route) {
                addMarkerToMap(aRoute, R.drawable.flag_red);
            }

        }catch (Exception e){
            Log.d("cos", "Exception", e);
            return false;
        }
        return true;
    }

    private void setCenterPointOfMap(LatLng latLng) {
        this.centerPoint = latLng;
    }

    private void moveCamera(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(currentZoom)
                .tilt(DEGREES_OF_TILT)
                .bearing(googleMap.getCameraPosition().bearing)
                .build();

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), ANIMATION_DURATION_MS, null);
    }

    private void addMarkerToMap(LatLng position, int iconID) {
        MarkerOptions myMarkerOptions = new MarkerOptions()
                .position(position)
                .anchor(0.0f, 1.0f)
                .icon(BitmapDescriptorFactory.fromResource(iconID));
        movingMarkers.add(googleMap.addMarker(myMarkerOptions));
    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {

        final long start = SystemClock.uptimeMillis();
        final LatLng startLatLng = marker.getPosition();

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed
                            / duration);
                    double lng = t * toPosition.longitude + (1 - t)
                            * startLatLng.longitude;
                    double lat = t * toPosition.latitude + (1 - t)
                            * startLatLng.latitude;
                    marker.setPosition(new LatLng(lat, lng));

                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    } else {
                        if (hideMarker) {
                            marker.setVisible(false);
                        } else {
                            marker.setVisible(true);
                        }
                    }
                }
        });
    }
}
