package com.example.payroll;

import android.app.Activity;
import android.os.Bundle;
import com.startapp.sdk.adsbase.StartAppSDK;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init Start.io Ads
        StartAppSDK.init(this, "203832106", true);
    }
}
