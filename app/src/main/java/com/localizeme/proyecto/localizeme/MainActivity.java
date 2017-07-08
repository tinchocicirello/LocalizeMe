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
import android.support.design.widget.FloatingActionButton;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import http.HttpClient;
import http.OnHttpRequestComplete;
import http.Response;

//Import para httpUrlConection



public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, View.OnClickListener {
    private static final String TAG = MainActivity.class.getName();

    private TextView tvLocalidad;
    private TextView tvLat;
    private TextView tvLong;


    private TextView tvTemperatura;


    private String provincia;
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

        //Conectar el UI con la Actividad
        tvLocalidad= (TextView) findViewById(R.id.tvLocalidad);
        tvLat = (TextView) findViewById(R.id.tvLat);
        tvLong = (TextView) findViewById(R.id.tvLong);

        //Solicitar permisos si es necesario (Android 6.0+)
        requestPermissionIfNeedIt();

        //Inicializar el GoogleAPIClient y armar la Petición de Ubicación
        initGoogleAPIClient();

        //conectar con btns de la activity
        FloatingActionButton news = (FloatingActionButton)findViewById(R.id.news);
        news.setOnClickListener(this);

        FloatingActionButton maps = (FloatingActionButton)findViewById(R.id.maps);
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
        refreshUI();
        tvLocalidad.setText(localidad);
        tvLat.setText("Lat:"+latitud);
        tvLong.setText("Long:"+longitud);


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
                    provincia = address.getAdminArea();
                    tvLocalidad.setText(localidad+", "+provincia);

                    //Obtenemos temperatura
                    obtenerTemperatura();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //Metodo que vamos a usar para consumir la API de clima de yahoo y para parsear el json que nos devuelve
    public void obtenerTemperatura(){
        tvTemperatura = (TextView) findViewById(R.id.tvTemperatura);
        HttpClient client = new HttpClient(new OnHttpRequestComplete() {



            @Override

            public void onComplete(Response status) {
                //Verificamos que haya respondido la peticion
                if (status.isSuccess()){
                    Gson gson = new GsonBuilder().create();

                    try{
                        JSONObject jsono = new JSONObject(status.getResult());
                        Tiempo t = gson.fromJson(status.getResult(), Tiempo.class);


                        //Esto se podria borrar porque esta en desuso y se utilizaria para un JsonArray

                        /*
                        JSONArray jsonarray = jsono.getJSONArray("records");
                        ArrayList<Person> ListaPersonas = new ArrayList<Person>();
                            for (int i = 0; i < jsonarray.length();i++)
                                {
                                    String person = jsonarray.getString(i);
                                    Person p = gson.fromJson (person, Person.class);

                                    textoTest.setText(p.getName());

                                }
                                */

                        Double centigrados;

                    centigrados = Double.parseDouble(t.getQuery().getResults().getChannel().getItem().getCondition().getTemp());
                    centigrados = (centigrados - 32) * 5/9;

                        DecimalFormat formato = new DecimalFormat("0.0");


                        tvTemperatura.setText(formato.format(centigrados) + " °C");

//                        tvTemperatura.setText(t.getQuery().getResults().getChannel().getItem().getCondition().getTemp());

                    }
                    catch(Exception e)
                    {

                    }

                    //Si queremos mostrar el json completo en un toast
                    //Toast.makeText(MainActivity.this, status.getResult(), Toast.LENGTH_SHORT).show();
                }
            }
        });


        //URL de prueba
        //client.excecute("https://query.yahooapis.com/v1/public/yql?q=select%20item.condition.temp%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22Castelar%2C%20Buenos%20Aires%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys");
        String provApiYahoo;
        String locApiYahoo;

        //Guardamos las variables de localidad y provincia, y remplazamos los espacios para evitar fallas en la url generada
        provApiYahoo = provincia.replaceAll(" ", "%20");
        locApiYahoo = localidad.replaceAll(" ", "%20");
        client.excecute("https://query.yahooapis.com/v1/public/yql?q=select%20item.condition.temp%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22"+ locApiYahoo +"%2C%20" + provApiYahoo + "%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys");
    }

}