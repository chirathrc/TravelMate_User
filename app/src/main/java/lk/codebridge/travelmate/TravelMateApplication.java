package lk.codebridge.travelmate;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class TravelMateApplication extends Application {


//    private ShakeDetector shakeDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        // Force the app to always use light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

//        shakeDetector = new ShakeDetector(this);
//        shakeDetector.start();
    }

//    public ShakeDetector getShakeDetector() {
//        return shakeDetector;
//    }
}
