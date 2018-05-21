package com.example.user.indoortest;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.resources.IAResourceManager;

public class ResourceManagerActivity extends AppCompatActivity {
    private IALocationManager mLocationManager;
    private IAResourceManager mResourceManager;
    private ImageView mFloorPlanImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_manager);

        mFloorPlanImage = (ImageView) findViewById(R.id.image);
        // ...
        // Create instance of IAResourceManager class
        mResourceManager = IAResourceManager.create(this);
    }


}
