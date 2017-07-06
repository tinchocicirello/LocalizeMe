package com.localizeme.proyecto.localizeme;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, View.OnClickListener {
    private static final String TAG = MainActivity.class.getName();

    private TextView tvLocalidad;
    private TextView tvLatLong;

    private String localidad;
    private double latitud;
    private double longitud;

    private static final int RC_LOCATION_PERMISION= 100;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private static int INTERVAL = 10000;
    private static int FAST_INTERVAL = 5000;

    private boolean mRequestingLocationUpdates = false;
    private Location mCurrentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setContentView(R.layout.content_main);

        //Conectar el UI con la Actividad
        tvLocalidad= (TextView) findViewById(R.id.tvLocalidad);
        tvLatLong = (TextView) findViewById(R.id.tvLatLong);

        //Solicitar permisos si es necesario (Android 6.0+)
        requestPermissionIfNeedIt();

        //Inicializar el GoogleAPIClient y armar la Petición de Ubicación
        initGoogleAPIClient();

        //conectar con btns de la activity
        Button news = (Button)findViewById(R.id.news);
        news.setOnClickListener(this);

        Button maps = (Button)findViewById(R.id.maps);
        maps.setOnClickListener(this);
    }


    public void onClick(View v) {
        switch (v.getId()){

            case R.id.news: {

                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://infobae.com/search/"+ localidad.replaceAll(" ", "+")));
                startActivity(i);

                break;
            }
            case R.id.maps: {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="+latitud+","+longitud+"(¡Usted está aquí!)"));
                startActivity(intent);

                break;
            }

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected())
                startLocationUpdates();
            else
                mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initGoogleAPIClient(){
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            //Creamos una peticion de ubicacion con el objeto LocationRequest
            createLocationRequest();
        }
    }

    protected void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FAST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates(){
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                mRequestingLocationUpdates = true;
            }
        }
    }

    private void stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && mRequestingLocationUpdates){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private void requestPermissionIfNeedIt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, RC_LOCATION_PERMISION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_LOCATION_PERMISION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                requestPermissionIfNeedIt();
            }
        }
    }

    /*
    * Implementación del GoogleApiClient.ConnectionCallbacks
    * */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
        if (!mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended");
        Toast.makeText(this, getString(R.string.app_name), Toast.LENGTH_LONG).show();
    }

    /*
    * Implementación del GoogleApiClient.OnConnectionFailedListener
    * */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed");
    }

    /*
    * Implementación del LocationListener
    * */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        tvLocalidad.setText(localidad);
        tvLatLong.setText(latitud+" ; "+longitud);
        refreshUI();
        this.setLocation();
    }

    private void refreshUI(){
        if (mCurrentLocation != null) {
            latitud = mCurrentLocation.getLatitude();
            longitud = mCurrentLocation.getLongitude();
        }
    }

    public void setLocation() {
        //Obtener la direccion de la calle a partir de la latitud y la longitud
        if (latitud != 0.0 && longitud != 0.0) {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> list = geocoder.getFromLocation(latitud, longitud, 1);
                if (!list.isEmpty()) {
                    Address address = list.get(0);
                    localidad = address.getLocality();
                    tvLocalidad.setText(localidad);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}