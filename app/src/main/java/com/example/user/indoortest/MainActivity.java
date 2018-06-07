package com.example.user.indoortest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import android.location.LocationManager;

import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.resources.IAFloorPlan;

import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.indooratlas.android.sdk.resources.IAResourceManager;
import com.indooratlas.android.sdk.resources.IAResult;
import com.indooratlas.android.sdk.resources.IAResultCallback;
import com.indooratlas.android.sdk.resources.IATask;

import com.indooratlas.android.wayfinding.IARoutingLeg;


import com.indooratlas.android.wayfinding.IAWayfinder;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.squareup.picasso.Picasso;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.widget.TextView;



import java.io.InputStream;



public class MainActivity extends AppCompatActivity implements  LocationListener {

    private IALocationManager mIALocationManager;
    private IALocationListener mIALocationListener;
    private IAResourceManager mResourceManager;
//    private IARegion.Listener mRegionListener;

    private Circle mCircle;


    private IATask mPendingAsyncResult;
    private Integer mFloor;
    private LatLng mLocation;
    private IAWayfinder mWayfinder;
    private LatLng center;
//    private Target mLoadTarget;
    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 2048;
    private boolean mCameraPositionNeedsUpdating = true;
    private boolean mShowIndoorLocation = false;
    private ImageView mFloorPlanImage;
    private LatLng mDestination;
    private Marker mDestinationMarker;
    private IARoutingLeg[] mCurrentRoute;
    private GoogleMap mMap;
    private IALocationManager mLocationManager;
    private Polyline mPath;
    private Polyline mPathCurrent;
    private Button startnavigating;
    private TextView showLatLng;
    private TextView showDestination;


    //顯示座標、樓層
    private void startListeningPlatformLocations() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this );
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this );
        }
    }




    protected void onCreate(Bundle savedInstanceState) {
        //定期登錄使用者的位置
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // prevent the screen going to sleep while app is on foreground避免手機進入待機模式
        findViewById(android.R.id.content).setKeepScreenOn(true);
        mIALocationManager = IALocationManager.create(MainActivity.this);
        mResourceManager = IAResourceManager.create(MainActivity.this);

        startListeningPlatformLocations();

        mFloorPlanImage = (ImageView) findViewById(R.id.image);


        setupListener();

        mIALocationManager.registerRegionListener(mRegionListener);

        final int CODE_PERMISSIONS = 1;

        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                //讓 API 使用 WiFi 或手機基地台訊號資料（或兩者）來判斷裝置的位置。
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions( MainActivity.this, neededPermissions, CODE_PERMISSIONS);

        //當前位置
        showLatLng=(TextView)findViewById( R.id.showLatLng );
        //終點位置
        showDestination=(TextView)findViewById( R.id.showDestination );
        //取自indooratlas的 floor plam id
        fetchFloorPlan("e4c4db63-5ef1-4ae6-ae6b-22e0507a3973");
        //按鈕為開始導航
        startnavigating=(Button)findViewById( R.id.startnavigating );


    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Handle if any of the permissions are denied, in grantResults
    }

    private void setupListener() {
        mIALocationListener = new IALocationListener()
 {

            // Called when the location has changed.
            @Override
            public void onLocationChanged(IALocation location) {
                String TAG = "onLocationChanged";
                Log.d( TAG, "Latitude: " + location.getLatitude() );
                Log.d( TAG, "Longitude: " + location.getLongitude() );
                Log.d( TAG, "Floor number: " + location.getFloorLevel() );
                showLatLng.setText( "Your current venue:"+location.getLatitude()+","+location.getLongitude() );
                showLocationCircle( new LatLng( location.getLatitude(),location.getLongitude() ),15.0f );
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                String TAG = "Status";
                Log.d( TAG, "onStatusChanged: 被呼叫了" );
                Log.d( TAG, "onStatusChanged: " );

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
                    Log.d(TAG, " 下載地圖"+result);

                    if (result.isSuccess()) {
                        handleFloorPlanChange(result.getResult());
                    } else {
                        // do something with error
                        Toast.makeText( MainActivity.this,
                                "loading floor plan failed: " + result.getError(), Toast.LENGTH_LONG)
                                .show();
                    }
                }
            }, Looper.getMainLooper()); // deliver callbacks in main thread
        }
    }


    //If we don’t have any errors, download the image.
    private void handleFloorPlanChange(IAFloorPlan newFloorPlan) {
        Picasso.get()
                .load(newFloorPlan.getUrl())
                .into(mFloorPlanImage);
    }

//floor detection確認樓層
    private IARegion.Listener mRegionListener = new IARegion.Listener() {
        IARegion mCurrentFloorPlan = null;
        @Override
        public void onEnterRegion(IARegion region) {
            if(region.getType()==IARegion.TYPE_FLOOR_PLAN)
            {
                String TAG="";
                Log.d(TAG, "Entered " + region.getName());
                Log.d(TAG, "floor plan ID: " + region.getId());
                mCurrentFloorPlan = region;
            }
            else if (region.getType() == IARegion.TYPE_VENUE) {
                // triggered when near a new location
                String TAG="";
                Log.d(TAG, "Location changed to " + region.getId());
            }

        }

        @Override
        public void onExitRegion(IARegion iaRegion) {
            // leaving a previously entered region
            if (iaRegion.getType() == IARegion.TYPE_FLOOR_PLAN) {
                mCurrentFloorPlan = null;
                // notice that a change of floor plan (e.g., floor change)
                // is signaled by an exit-enter pair so ending up here
                // does not yet mean that the device is outside any mapped area
            }
        }
    };



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




    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remember to clean up after ourselves
        //結束導航清除資料
        mIALocationManager.destroy();
        if (mWayfinder != null) {
            mWayfinder.close();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);

        mIALocationManager.registerRegionListener(mRegionListener);

    }

    @Override
    protected void onPause()
    {
        mIALocationManager.removeLocationUpdates(mIALocationListener);
        mIALocationManager.removeLocationUpdates(mListener);
        super.onPause();
        // unregister location & region changes

    }



    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }




    private   void  showLocationCircle(LatLng center, double accuracyRadius) {
       String TAG="LocationCircle";
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
            Log.d( TAG,"你的位置:" );
        }

    }


    /**
     * Listener that handles location change events.
     */
    private IALocationListener mListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */
        @Override
        public void onLocationChanged(IALocation location) {
            final String TAG="onLocationChanged";
            Log.d(TAG, "new location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());

            if (mMap == null) {
                // location received before map is initialized, ignoring update here
                return;
            }

            final LatLng center = new LatLng(location.getLatitude(), location.getLongitude());

            mFloor = location.getFloorLevel();
            mLocation = new LatLng(location.getLatitude(), location.getLongitude());
            if (mWayfinder != null) {
                mWayfinder.setLocation(mLocation.latitude, mLocation.longitude, mFloor);
            }
            updateRoute();

            if (mShowIndoorLocation) {
                showLocationCircle(center, location.getAccuracy());
            }

            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                mMap.animateCamera( CameraUpdateFactory.newLatLngZoom(center, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        };


        /**
         * Load "wayfinding_graph.json" from raw resources folder of the app module
         * @return
         */


        private String loadGraphJSON() {
            try {
                Resources res = getResources();
                int resourceIdentifier = res.getIdentifier("wayfinding_graph", "raw", getPackageName());
                InputStream in_s = res.openRawResource(resourceIdentifier);

                byte[] b = new byte[in_s.available()];
                in_s.read(b);
                return new String(b);
            } catch (Exception e) {
                final String TAG="下載路徑";
                Log.e(TAG, "Could not find wayfinding_graph.json from raw resources folder");
                return null;
            }

        }

        public void onMapClick(LatLng point) {
            if (mMap != null) {

                mDestination = point;
                if (mDestinationMarker == null) {
                    mDestinationMarker = mMap.addMarker(new MarkerOptions()
                            .position(point)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                } else {
                    mDestinationMarker.setPosition(point);
                }
                if (mWayfinder != null) {
                    mWayfinder.setDestination(point.latitude, point.longitude, mFloor);
                }
                final String TAG="設立終點";
                Log.d(TAG, "Set destination: (" + mDestination.latitude + ", " +
                        mDestination.longitude + "), floor=" + mFloor);

                updateRoute();
            }
        }

        private void updateRoute() {
            if (mLocation == null || mDestination == null || mWayfinder == null) {
                return;
            }
            final String TAG="路徑改變";
            Log.d(TAG, "Updating the wayfinding route");

            mCurrentRoute = mWayfinder.getRoute();
            if (mCurrentRoute == null || mCurrentRoute.length == 0) {
                // Wrong credentials or invalid wayfinding graph
                return;
            }
            if (mPath != null) {
                // Remove old path if any
                clearOldPath();
            }
            visualizeRoute(mCurrentRoute);
        }
        /**
         * Clear the visualizations for the wayfinding paths
         */
        private void clearOldPath() {
            mPath.remove();
            mPathCurrent.remove();
        }

    };
    /**
     * Visualize the IndoorAtlas Wayfinding path on top of the Google Maps.
     * @param legs Array of IARoutingLeg objects returned from IAWayfinder.getRoute()
     */
    private void visualizeRoute(IARoutingLeg[] legs) {
        // optCurrent will contain the wayfinding path in the current floor and opt will contain the
        // whole path, including parts in other floors.
        PolylineOptions opt = new PolylineOptions();
        PolylineOptions optCurrent = new PolylineOptions();

        for (IARoutingLeg leg : legs) {
            opt.add(new LatLng(leg.getBegin().getLatitude(), leg.getBegin().getLongitude()));
            if (leg.getBegin().getFloor() == mFloor && leg.getEnd().getFloor() == mFloor) {
                optCurrent.add(
                        new LatLng(leg.getBegin().getLatitude(), leg.getBegin().getLongitude()));
                optCurrent.add(
                        new LatLng(leg.getEnd().getLatitude(), leg.getEnd().getLongitude()));
            }
        }
        optCurrent.color(Color.RED);
        if (legs.length > 0) {
            IARoutingLeg leg = legs[legs.length-1];
            opt.add(new LatLng(leg.getEnd().getLatitude(), leg.getEnd().getLongitude()));
        }
        // Here wayfinding path in different floor than current location is visualized in blue and
        // path in current floor is visualized in red
        mPath = mMap.addPolyline(opt);
        mPathCurrent = mMap.addPolyline(optCurrent);
    }


    public IARegion.Listener getRegionListener() {
        return mRegionListener;
    }

    public void setRegionListener(IARegion.Listener regionListener) {
        mRegionListener = regionListener;
    }
}




