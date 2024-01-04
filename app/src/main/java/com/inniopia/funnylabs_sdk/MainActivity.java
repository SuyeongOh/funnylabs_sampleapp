package com.inniopia.funnylabs_sdk;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class MainActivity extends FragmentActivity {

    private static final String[] LIST_NEW_SDK_PERMISSION = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(LIST_NEW_SDK_PERMISSION, 1);

        InitFragment fragment = new InitFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainerView, fragment)
                .commit();
    }

    public void replaceFragment(Fragment fragment){
        getSupportFragmentManager().popBackStack();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainerView, fragment)
                .commit();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
