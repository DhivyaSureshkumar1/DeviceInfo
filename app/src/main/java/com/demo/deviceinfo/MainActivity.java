package com.demo.deviceinfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Size;
import android.view.View;

import com.demo.deviceinfo.databinding.ActivityMainBinding;
import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    ActivityMainBinding activityMainBinding;
    private Context context;

    String manufacturer = "Manufacturer = " + Build.MANUFACTURER;
    String model = "Model Name = "+Build.MODEL;
    String modelNumber = "Model Number = "+Build.DEVICE;
    String androidVersion = "Android Version = "+Build.VERSION.RELEASE;

    //String hardware = Build.HARDWARE;
    String cpuAbi = "CPU Information = " + Arrays.toString(Build.SUPPORTED_ABIS);

    private SensorManager mSensorManager;
    private Sensor mGyroscopeSensor;
    private Sensor mBarometerSensor;
    private Sensor mAccelerometerSensor;
    private Sensor mRotationVectorSensor;
    private Sensor mProximitySensor;
    private Sensor mLightSensor;
    String ram_text, storage_text, battery_percent, megapixels, aperture, Gpu_info, Gyro_Info, Baro_info, Accl_info, Rot_vec, Proximity, Ambient_light, gps, imei;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = activityMainBinding.getRoot();
        setContentView(view);
        context = getApplicationContext();
        final ActivityManager activityManager =  (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        String glVersion = configurationInfo.getGlEsVersion();
        Gpu_info = "GL version = "+glVersion;


        activityMainBinding.progressBar.setVisibility(View.VISIBLE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long ram = memoryInfo.totalMem / (1024 * 1024); // RAM memory
        ram_text = "RAM = "+ram +" MB";


        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalStorage = stat.getBlockCountLong() * blockSize / (1024 * 1024 * 1024); // Storage in GB
        storage_text = "Storage = "+totalStorage+" GB";

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100 / (float) scale; // Battery level in percentage
        battery_percent = "Battery Charge = "+batteryPct+" %";

        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            String cameraId = cameraIds[0]; // Choose the first camera

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
            int mp = sizes[0].getWidth() * sizes[0].getHeight() / 1000000;
            megapixels = "Camera MegaPixel = " +mp+ " MP";

            float[] apertureRange = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
            aperture = "Camera Aperture = "+ apertureRange[0] + " - " + apertureRange[apertureRange.length - 1];
        } catch (CameraAccessException e) {
            Log.e("CAMERA", String.valueOf(e));
        }

        imei = "IMEI = "+findIMEI();
        gps = location();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mBarometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (mBarometerSensor!= null) {
            return;
        } else {
            activityMainBinding.barometer.setText("DEVICE DONT HAVE BAROMETER SENSOR");
        }
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        fillDetails();


    }

    private void fillDetails() {


        activityMainBinding.manufacturer.setText(manufacturer);
        activityMainBinding.modelName.setText(model);
        activityMainBinding.modelNumber.setText(modelNumber);
        activityMainBinding.ram.setText(ram_text);
        activityMainBinding.storage.setText(storage_text);
        activityMainBinding.batteryCharge.setText(battery_percent);
        activityMainBinding.androidVer.setText(androidVersion);
        activityMainBinding.cameraMegapixel.setText(megapixels);
        activityMainBinding.cameraAperture.setText(aperture);
        activityMainBinding.cpuInfo.setText(cpuAbi);
        activityMainBinding.gpuInfo.setText(Gpu_info);
        activityMainBinding.imei.setText(imei);
        activityMainBinding.gps.setText(gps);
        activityMainBinding.progressBar.setVisibility(View.INVISIBLE);

    }

    public String findIMEI() {
        String deviceId;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } else {
            final TelephonyManager mTelephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephony.getDeviceId() != null) {
                deviceId = mTelephony.getDeviceId();
            } else {
                deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        }

        return deviceId;

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register sensor and location listeners
        mSensorManager.registerListener(this, mGyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mBarometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister sensor and location listeners
        mSensorManager.unregisterListener(this);
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                float gyroX = event.values[0];
                float gyroY = event.values[1];
                float gyroZ = event.values[2];
                Gyro_Info = "Gyroscope value : x = "+ gyroX +", y = "+gyroY+", z = "+gyroZ;
                activityMainBinding.gyroscope.setText(Gyro_Info);
                break;
            case Sensor.TYPE_PRESSURE:
                float pressure = event.values[0];
                if(pressure == 0.0){
                    activityMainBinding.barometer.setText("DEVICE DONT HAVE BAROMETER SENSOR");
                }else{
                    Baro_info = "Barometer  "+ pressure;
                    activityMainBinding.barometer.setText(Baro_info);
                }
                break;
            case Sensor.TYPE_ACCELEROMETER:
                float accelX = event.values[0];
                float accelY = event.values[1];
                float accelZ = event.values[2];
                Accl_info = "Accelerometer: X=" + accelX + ", Y=" + accelY + ", Z=" + accelZ;
                activityMainBinding.accelerometer.setText(Accl_info);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                float rotX = event.values[0];
                float rotY = event.values[1];
                float rotZ = event.values[2];
                activityMainBinding.rotationVector.setText(Rot_vec);
                Rot_vec = "Rotation Vector: X=" + rotX + ", Y=" + rotY + ", Z=" + rotZ;
                break;
            case Sensor.TYPE_PROXIMITY:
                float proximity = event.values[0];
                Proximity = "Proximity: " + proximity;
                activityMainBinding.proximity.setText(Proximity);
                break;
            case Sensor.TYPE_LIGHT:
                float light = event.values[0];
                Ambient_light = "Light: " + light;
                activityMainBinding.ambientLightSensor.setText(Ambient_light);
                break;
        }
    }

    String location() {
        String location = null;
        GpsTracker gpsTracker;

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        gpsTracker = new GpsTracker(MainActivity.this);
        if(gpsTracker.canGetLocation()){
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            location = "Location : Latitude = "+ latitude +"\tLongitude = "+ longitude;

        }else{
            gpsTracker.showSettingsAlert();
        }
        return location;
    }



}