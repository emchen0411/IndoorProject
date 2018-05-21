package com.example.user.indoortest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;

import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import android.location.LocationManager;

import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.resources.IAFloorPlan;

import com.indooratlas.android.sdk.resources.IAResourceManager;
import com.indooratlas.android.sdk.resources.IAResult;
import com.indooratlas.android.sdk.resources.IAResultCallback;
import com.indooratlas.android.sdk.resources.IATask;

import com.squareup.picasso.Target;

import com.indooratlas.android.wayfinding.IAWayfinder;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import com.squareup.picasso.Picasso;


public class MainActivity extends AppCompatActivity implements  LocationListener {

    private IALocationManager mIALocationManager;
    private IALocationListener mIALocationListener;
    private IAResourceManager mResourceManager;
    private IARegion.Listener mRegionListener;

    private Circle mCircle;


    private IATask mPendingAsyncResult;
    private Integer mFloor;
    private LatLng mLocation;
    private LatLng center;
    private Target mLoadTarget;
    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 2048;
    private boolean mCameraPositionNeedsUpdating = true;
    private boolean mShowIndoorLocation = false;
    private ImageView mFloorPlanImage;
    private GoogleMap mMap;
    private IALocationManager mLocationManager;




    private void startListeningPlatformLocations() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this );
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this );
        }
    }



    protected void onCreate(Bundle savedInstanceState) {
        //定期登錄使用者的位置
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);
        mIALocationManager = IALocationManager.create(this);
        mResourceManager = IAResourceManager.create(this);


        startListeningPlatformLocations();


        mFloorPlanImage = (ImageView) findViewById(R.id.image);


        setupListener();

        mIALocationManager.registerRegionListener(mRegionListener);

        final int CODE_PERMISSIONS = 1;

        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions( this, neededPermissions, CODE_PERMISSIONS);

    }


//    private IALocationListener mIALocationListener = new IALocationListener() {
//
//        // Called when the location has changed.
//        @Override
//        public void onLocationChanged(IALocation location) {
//
//            final String TAG="";
//            Log.d(TAG, "Latitude: " + location.getLatitude());
//            Log.d(TAG, "Longitude: " + location.getLongitude());
//            Log.d(TAG, "Floor number: " + location.getFloorLevel());
//        }
//    };
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Handle if any of the permissions are denied, in grantResults
    }

    private void setupListener() {
        mIALocationListener = new IALocationListener() {

            // Called when the location has changed.
            @Override
            public void onLocationChanged(IALocation location) {
                String TAG = "onLocationChanged";
                Log.d(TAG, "Latitude: " + location.getLatitude());
                Log.d(TAG, "Longitude: " + location.getLongitude());
                Log.d(TAG, "Floor number: " + location.getFloorLevel());
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                String TAG = "Status";
                Log.d(TAG, "onStatusChanged: 被呼叫了");
                Log.d(TAG, "onStatusChanged: ");
            }
        };

        //floor detection & indoor-outdoor
        mRegionListener = new IARegion.Listener() {
            IARegion mCurrentFloorPlan = null;


            @Override
            public void onEnterRegion(IARegion region) {
                String TAG ="EnterRegion";
                mIALocationManager.registerRegionListener(mRegionListener);
                if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                    fetchFloorPlan(region.getId());
                }
                if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                    Log.d(TAG, "Entered " + region.getName());
                    Log.d(TAG, "floor plan ID: " + region.getId());
                    mCurrentFloorPlan = region;
                }
                else if (region.getType() == IARegion.TYPE_VENUE) {
                    // triggered when near a new location
                    Log.d(TAG, "Location changed to " + region.getId());
                }
            }

            @Override
            public void onExitRegion(IARegion region) {
                // leaving a previously entered region
                if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                    mCurrentFloorPlan = null;
                    // notice that a change of floor plan (e.g., floor change)
                    // is signaled by an exit-enter pair so ending up here
                    // does not yet mean that the device is outside any mapped area
                }
            }
        };

    }



    private void fetchFloorPlan(String id) {
        // Cancel pending operation, if any
        if (mPendingAsyncResult != null && !mPendingAsyncResult.isCancelled()) {
            mPendingAsyncResult.cancel();
        }

        mPendingAsyncResult = mResourceManager.fetchFloorPlanWithId(id);
        if (mPendingAsyncResult != null) {
            mPendingAsyncResult.setCallback(new IAResultCallback<IAFloorPlan>() {
                @Override
                public void onResult(IAResult<IAFloorPlan> result) {
                    final String TAG="fetchFloorPlan";
                    Log.d(TAG, "onResult: %s"+result);

                    if (result.isSuccess()) {
                        handleFloorPlanChange(result.getResult());
                    } else {
                        // do something with error
//                        Toast.makeText( FloorPlanManagerActivity.this,
//                                "loading floor plan failed: " + result.getError(), Toast.LENGTH_LONG)
//                                .show();
                    }
                }
            }, Looper.getMainLooper()); // deliver callbacks in main thread
        }
    }


        IARegion mCurrentFloorPlan = null;


        public void onEnterRegion(IARegion region) {
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                final String TAG="";
                Log.d(TAG, "Entered " + region.getName());
                Log.d(TAG, "floor plan ID: " + region.getId());
                mCurrentFloorPlan = region;
            }
        }


        public void onExitRegion(IARegion region) {}

    @Override
    public void onLocationChanged(Location location) {
        if (!mShowIndoorLocation) {
            final String TAG="onLocationChanged";
            Log.d(TAG, "new LocationService location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());

            showLocationCircle(
                    new LatLng(location.getLatitude(), location.getLongitude()),
                    location.getAccuracy());
        }
    }




    //If we don’t have any errors, download the image.
    private void handleFloorPlanChange(IAFloorPlan newFloorPlan) {
        Picasso.get()
            .load(newFloorPlan.getUrl())
            .into(mFloorPlanImage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remember to clean up after ourselves
//        mIALocationManager.destroy();
//        if (mWayfinder != null) {
//            mWayfinder.close();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);

        mIALocationManager.registerRegionListener(mRegionListener);

//        mMap.setOnMapClickListener((GoogleMap.OnMapClickListener) this);
    }

    @Override
    protected void onPause()
    {
        mIALocationManager.removeLocationUpdates(mIALocationListener);
        super.onPause();
        // unregister location & region changes

    }



    private void showLocationCircle(LatLng center, double accuracyRadius) {
        if (mCircle == null) {
            // location can received before map is initialized, ignoring those updates
            if (mMap != null) {
                mCircle = mMap.addCircle(new CircleOptions()
                        .center(center)
                        .radius(accuracyRadius)
                        .fillColor(Color.argb(97, 93, 185, 139)) //定位點
                        .strokeColor(Color.argb(200, 190, 93, 90))//定位點圓周
                        .zIndex(1.0f)
                        .visible(true)
                        .strokeWidth(3.0f));
            }
        } else {
            // move existing markers position to received location
            mCircle.setCenter(center);
            mCircle.setRadius(accuracyRadius);
        }
    }
    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }


    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    private void setCircle(LatLng center, double accuracyRadius) {

        if (mCircle == null) {
            // location can received before map is initialized, ignoring those updates
            if (mFloorPlanImage != null) {
                mCircle = mMap.addCircle(new CircleOptions()
                        .center(center)
                        .radius(accuracyRadius)
                        .fillColor(0x801681FB)
                        .strokeColor(0x800A78DD)
                        .zIndex(1.0f)
                        .visible(true)
                        .strokeWidth(5.0f));
            }
        } else {
            // move existing markers position to received location
            mCircle.setCenter(center);
            mCircle.setRadius(accuracyRadius);
        }
    }


};

