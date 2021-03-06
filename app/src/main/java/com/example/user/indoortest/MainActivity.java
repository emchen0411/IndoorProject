package com.example.user.indoortest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.wearable.LargeAssetApi;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import android.location.LocationManager;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.indooratlas.android.sdk.resources.IAResourceManager;
import com.indooratlas.android.sdk.resources.IAResult;
import com.indooratlas.android.sdk.resources.IAResultCallback;
import com.indooratlas.android.sdk.resources.IATask;
import com.indooratlas.android.wayfinding.IARoutingLeg;
import com.indooratlas.android.wayfinding.IARoutingPoint;
import com.indooratlas.android.wayfinding.IAWayfinder;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
public class MainActivity extends AppCompatActivity implements  LocationListener, OnMapReadyCallback {

    private IALocationManager mIALocationManager;
    private IALocationListener mIALocationListener;
    private IAResourceManager mResourceManager;
    //    private IARegion.Listener mRegionListener;
    private Circle mCircle;
    private Marker mMarker;
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
    private IAWayfinder wayfinder;
    private Polyline mPath;
    private Polyline mPathCurrent;
    private Polyline mPolyline;
    private Button startnavigating;
    private TextView showLatLng;
    private TextView showDestination;

    private android.app.FragmentManager fmgr;
    private android.app.FragmentTransaction fragmentTransaction;
    private ViewGroup container;
    private Page1Fragment Page1Fragment;
    private Target mLoadTarget;
    private int MAX_DIMENSION = 2048;
    private Bundle extras;


    //顯示座標、樓層
    private void startListeningPlatformLocations() {
        LocationManager locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if (locationManager != null && (ActivityCompat.checkSelfPermission( this, ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED)) {

            locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, this );
            locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, 0, 0, this );
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        //定期登錄使用者的位置
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        // prevent the screen going to sleep while app is on foreground避免手機進入待機模式
        findViewById( android.R.id.content ).setKeepScreenOn( true );
        mIALocationManager = IALocationManager.create( MainActivity.this );
        mResourceManager = IAResourceManager.create( MainActivity.this );
        startListeningPlatformLocations();
        mFloorPlanImage = (ImageView) findViewById( R.id.image );
        setupListener();
        mLocationManager = IALocationManager.create( this, extras );
        String graphJson = loadJSONFromAsset( this, "wayfinding-graph.json" );
        wayfinder = IAWayfinder.create( this, graphJson );

//        mIALocationManager.registerRegionListener(mRegionListener);
        final int CODE_PERMISSIONS = 1;
        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                //讓 API 使用 WiFi 或手機基地台訊號資料（或兩者）來判斷裝置的位置。
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions( MainActivity.this, neededPermissions, CODE_PERMISSIONS );
        //當前位置
        showLatLng = (TextView) findViewById( R.id.showLatLng );
        //終點位置
        showDestination = (TextView) findViewById( R.id.showDestination );
        //按鈕為開始導航
        startnavigating = (Button) findViewById( R.id.startnavigating );
        //取得container，作為容器使用
        container = (ViewGroup) findViewById( R.id.container );
        //取得FragmentManager物件實體
        fmgr = getFragmentManager();
        //建立1個Fragment物件實體
        Page1Fragment = new Page1Fragment();
        //取得交易物件
        fragmentTransaction = fmgr.beginTransaction();
        //初始加入第一頁，並與container結合
        fragmentTransaction.add( R.id.container, Page1Fragment );
        //實現動作程序
        fragmentTransaction.commit();

    }

    public String loadJSONFromAsset(Context context, String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open( fileName );
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read( buffer );
            is.close();
            json = new String( buffer, "UTF-8" );
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void changeToPage1(View view) {
        fragmentTransaction = fmgr.beginTransaction();
        fragmentTransaction.replace( R.id.container, Page1Fragment );
        fragmentTransaction.commit();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        //Handle if any of the permissions are denied, in grantResults
    }

    private void setupListener() {
        mIALocationListener = new IALocationListenerSupport() {
            // Called when the location has changed.
            @Override
            public void onLocationChanged(IALocation location) {
                String TAG = "onLocationChanged";
                Log.d( TAG, "Latitude: " + location.getLatitude() );
                Log.d( TAG, "Longitude: " + location.getLongitude() );
                Log.d( TAG, "Floor number: " + location.getFloorLevel() );
                showLatLng.setText( "Your current venue:" + location.getLatitude() + "," + location.getLongitude() );
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

        mPendingAsyncResult = mResourceManager.fetchFloorPlanWithId( id );
        if (mPendingAsyncResult != null) {
            mPendingAsyncResult.setCallback( new IAResultCallback <IAFloorPlan>() {
                @Override
                public void onResult(IAResult <IAFloorPlan> result) {
                    final String TAG = "fetchFloorPlan";
                    Log.d( TAG, " 下載地圖" + result );
                    if (result.isSuccess()) {
                        fetchFloorPlanBitmap( result.getResult() );
                    } else {
                        // do something with error
                        Log.d( TAG, "onResult: " + "loading floor plan failed: " + result.getError() );
                        Toast.makeText( MainActivity.this,
                                "loading floor plan failed: " + result.getError(), Toast.LENGTH_LONG )
                                .show();
                    }
                }
            }, Looper.getMainLooper() ); // deliver callbacks in main thread
        }
    }

    /**
     * Download floor plan using Picasso library.
     */
    private void fetchFloorPlanBitmap(final IAFloorPlan floorPlan) {

        final String url = floorPlan.getUrl();

        if (mLoadTarget == null) {
            mLoadTarget = new Target() {

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    String TAG = "BitmapLoaded";
                    Log.d( TAG, "地圖下載中 " + bitmap.getWidth() + "x"
                            + bitmap.getHeight() );
                    setupGroundOverlay( floorPlan, bitmap );
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    Toast.makeText( MainActivity.this,
                            "Failed to load bitmap ", Toast.LENGTH_LONG )
                            .show();
                    mOverlayFloorPlan = null;
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    // N/A
                }

            };
        }

        RequestCreator request = Picasso.get().load( url );

        final int bitmapWidth = floorPlan.getBitmapWidth();
        final int bitmapHeight = floorPlan.getBitmapHeight();

        if (bitmapHeight > MAX_DIMENSION) {
            request.resize( 0, MAX_DIMENSION );
        } else if (bitmapWidth > MAX_DIMENSION) {
            request.resize( MAX_DIMENSION, 0 );
        }

        request.into( mLoadTarget );
    }

    private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap) {
//        LatLng MapPosition = new LatLng( 25.0499593, 121.5593166357556 );
        IALatLng iaLatLng = floorPlan.getTopRight();
        String TAG = "中心座標";
        Log.d( TAG, "中心座標是:" + iaLatLng.latitude + "," + iaLatLng.longitude );
        LatLng MapPosition = new LatLng( iaLatLng.latitude, iaLatLng.longitude );
        if (mGroundOverlay != null) {
            mGroundOverlay.remove();
        }

        GroundOverlayOptions mapposition = new GroundOverlayOptions()
                .image( BitmapDescriptorFactory.fromBitmap( bitmap ) )
                .zIndex( 0.0f )
                .position( MapPosition, floorPlan.getWidthMeters(), floorPlan.getHeightMeters() );

        mGroundOverlay = mMap.addGroundOverlay( mapposition );
    }

    //If we don’t have any errors, download the image.
    private void handleFloorPlanChange(IAFloorPlan newFloorPlan) {
        Picasso.get()
                .load( newFloorPlan.getUrl() )
                .into( mFloorPlanImage );
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!mShowIndoorLocation) {
            final String TAG = "onLocationChanged";
            Log.d( TAG, "new LocationService location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude() );

            showLocationMarker( location );
            wayfinder.setLocation( location.getLatitude(), location.getLongitude(), 2 );
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

    //    TODO::Resume
    @Override
    protected void onResume() {
        super.onResume();

        mIALocationManager.requestLocationUpdates( IALocationRequest.create(), mIALocationListener );
//        mIALocationManager.registerRegionListener(mRegionListener);
//                fetchFloorPlan("e4c4db63-5ef1-4ae6-ae6b-22e0507a3973");
    }

    @Override
    protected void onPause() {
        mIALocationManager.removeLocationUpdates( mIALocationListener );
        mIALocationManager.removeLocationUpdates( mListener );
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

    private void showLocationMarker(Location location) {
        String TAG = "Location Marker";
        Log.d( TAG, String.valueOf( "判斷Marker是不是null" + mMarker == null ) );
        if (mMarker == null) {
            mMarker = mMap.addMarker( new MarkerOptions()
                    .position( new LatLng( location.getLatitude(), location.getLongitude() ) )
                    .title( "here" ) );
            // location can received before map is initialized, ignoring those updates
        } else {
            // move existing markers position to received location
            mMarker.remove();
            mMarker = mMap.addMarker( new MarkerOptions()
                    .position( new LatLng( location.getLatitude(), location.getLongitude() ) )
                    .title( "here" ) );
            Log.d( TAG, "你的位置:" );
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
            final String TAG = "onLocationChanged";
            Log.d( TAG, "new location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude() );

            if (mMap == null) {
                // location received before map is initialized, ignoring update here
                return;
            }

            final LatLng center = new LatLng( location.getLatitude(), location.getLongitude() );

            mFloor = location.getFloorLevel();
            mLocation = new LatLng( location.getLatitude(), location.getLongitude() );
            if (mWayfinder != null) {
                mWayfinder.setLocation(mLocation.latitude, mLocation.longitude, mFloor);
            }
            updateRoute();

            if (mShowIndoorLocation) {
//                showLocationCircle(center, location.getAccuracy());

            }
            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                mMap.animateCamera( CameraUpdateFactory.newLatLngZoom( center, 17.5f ) );
                mCameraPositionNeedsUpdating = false;
            }
        }

        ;

        /**
         * Load "wayfinding_graph.json" from raw resources folder of the app module
         * @return
         */
        private void updateRoute() {
            if (mLocation == null || mDestination == null || mWayfinder == null) {
                return;
            }
            final String TAG = "路徑改變";
            Log.d( TAG, "Updating the wayfinding route" );

            mCurrentRoute = mWayfinder.getRoute();
            if (mCurrentRoute == null || mCurrentRoute.length == 0) {
                // Wrong credentials or invalid wayfinding graph
                return;
            }
            if (mPath != null) {
                // Remove old path if any
                clearOldPath();
            }
            visualizeRoute( mCurrentRoute );
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
     *
     * @param legs Array of IARoutingLeg objects returned from IAWayfinder.getRoute()
     */
    private void visualizeRoute(IARoutingLeg[] legs) {
        // optCurrent will contain the wayfinding path in the current floor and opt will contain the
        // whole path, including parts in other floors.
        PolylineOptions opt = new PolylineOptions();
        PolylineOptions optCurrent = new PolylineOptions();

        for (IARoutingLeg leg : legs) {
            opt.add( new LatLng( leg.getBegin().getLatitude(), leg.getBegin().getLongitude() ) );
            if (leg.getBegin().getFloor() == mFloor && leg.getEnd().getFloor() == mFloor) {
                optCurrent.add(
                        new LatLng( leg.getBegin().getLatitude(), leg.getBegin().getLongitude() ) );
                optCurrent.add(
                        new LatLng( leg.getEnd().getLatitude(), leg.getEnd().getLongitude() ) );
            }
        }
        optCurrent.color( Color.RED );
        if (legs.length > 0) {
            IARoutingLeg leg = legs[legs.length - 1];
            opt.add( new LatLng( leg.getEnd().getLatitude(), leg.getEnd().getLongitude() ) );
        }
        // Here wayfinding path in different floor than current location is visualized in blue and
        // path in current floor is visualized in red
        mPath = mMap.addPolyline( opt );
        mPathCurrent = mMap.addPolyline( optCurrent );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        fetchFloorPlan( "e4c4db63-5ef1-4ae6-ae6b-22e0507a3973" );
        // 設置MapClick事件

        googleMap.setOnMapClickListener( new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                android.util.Log.i( "onMapClick", "destination has been chosen!" );
                String TAG = "destination";
                Log.d( TAG, String.valueOf( "判斷DestinationMarker是不是null" + mDestinationMarker == null ) );
                if (mDestinationMarker == null) {
                    mDestinationMarker = mMap.addMarker( new MarkerOptions()
                            .position( new LatLng( latLng.latitude, latLng.longitude ) )
                            .icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_BLUE ) )
                            .title( "destination" ) );
                    // location can received before map is initialized, ignoring those updates
                } else {
                    // move existing markers position to received location
                    mDestinationMarker.remove();
                    mDestinationMarker = mMap.addMarker( new MarkerOptions()
                            .position( new LatLng( latLng.latitude, latLng.longitude ) )
                            .icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_BLUE ) )
                            .title( "destination" ) );
                    Log.d( TAG, "你的位置:" );
                }
                showDestination.setText( "Here's yor destination: " + latLng.latitude + "," + latLng.longitude );
                //wayfinder
                wayfinder.setDestination( latLng.latitude, latLng.longitude, 2 );
                IARoutingLeg[] route = wayfinder.getRoute();
                String uploadgrahJson = loadGraphJSON();

                for (int i = 0; i < route.length; i++) {
                    IARoutingPoint begin = route[i].getBegin();
                    IARoutingPoint end = route[i].getEnd();
                    if(mPolyline == null)
                    {
                        mPolyline = mMap.addPolyline( new PolylineOptions()
                                .add( new LatLng( begin.getLatitude(),begin.getLongitude()),new LatLng(end.getLatitude(),end.getLongitude()))
                                .width( 25 )
                                .color( Color.YELLOW )
                                .geodesic( true )
                        );
                    }
                    else
                    {
                        mPolyline.remove();
                        mPolyline = mMap.addPolyline( new PolylineOptions()
                                .add( new LatLng( begin.getLatitude(),begin.getLongitude()),new LatLng(end.getLatitude(),end.getLongitude()))
                                .width( 25 )
                                .color( Color.YELLOW )
                                .geodesic( true )
                        );
                    }
        }

    }
        });
    }

    private String loadGraphJSON() {
        try {
            Resources res = getResources();
            int resourceIdentifier = res.getIdentifier( "wayfinding-graph", "raw", getPackageName() );
            InputStream in_s = res.openRawResource( resourceIdentifier );

            byte[] b = new byte[in_s.available()];
            in_s.read( b );
            return new String( b );
        } catch (Exception e) {
            final String TAG = "下載路徑";
            Log.e( TAG, "Could not find wayfinding_graph.json from raw resources folder" );
            return null;
        }
    }



}


