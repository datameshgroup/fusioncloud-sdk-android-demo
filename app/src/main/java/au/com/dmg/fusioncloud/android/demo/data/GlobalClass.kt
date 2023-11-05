package au.com.dmg.fusioncloud.android.demo.data

import android.app.Application

// TODO: Will utilise this for Preauthorisation and Completion
class GlobalClass : Application() {
    override fun onCreate() {
        context = this
        super.onCreate()
    }

    companion object {
        // or return instance.getApplicationContext();
        var context: GlobalClass? = null
            private set
            get() = field
        // or return instance.getApplicationContext();
    }
}