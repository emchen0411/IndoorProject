package com.example.user.indoortest;


import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


/**
 * A simple {@link Fragment} subclass.
 */
public class Page1Fragment extends Fragment implements OnMapReadyCallback{

    GoogleMap mGoogleMap;
    MapView mMapView;
    View mView;


    public Page1Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView= inflater.inflate( R.layout.fragment_page1, container, false );
        return mView;
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated( view,savedInstanceState );

        mMapView=(MapView)mView.findViewById( R.id.map );
        if(mMapView!=null){
            mMapView.onCreate( null );
            mMapView.onResume();
            mMapView.getMapAsync( this );
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize( getContext() );
        mGoogleMap=googleMap;
        googleMap.setMapType( GoogleMap.MAP_TYPE_NORMAL );
        googleMap.addMarker( new MarkerOptions().position( new LatLng( 25.0500,121.5595 ) ).title( "office" ).snippet( "work here" ) );
        CameraPosition Liberty = CameraPosition.builder().target( new LatLng( 25.0500,121.5595 ) ).zoom(20).bearing(0).tilt(45).build();
        mGoogleMap.animateCamera( CameraUpdateFactory.newCameraPosition( Liberty ) );
    }
}
