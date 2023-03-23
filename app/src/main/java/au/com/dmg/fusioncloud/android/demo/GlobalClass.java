package au.com.dmg.fusioncloud.android.demo;

import android.app.Application;
// TODO: Will utilise this for Preauthorisation and Completion
public class GlobalClass extends Application {

    private static GlobalClass instance;

    public static GlobalClass getInstance() {
        return instance;
    }

    public static GlobalClass getContext(){
        return instance;
        // or return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
    }

}
