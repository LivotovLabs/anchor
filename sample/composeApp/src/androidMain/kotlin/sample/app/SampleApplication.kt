package sample.app

import android.app.Application

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocationManager.monitor()
    }
}
