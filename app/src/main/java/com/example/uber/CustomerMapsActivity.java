package com.example.uber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int AUTOCOMPLETE_REQUEST_CODE = 1 ;
    private GoogleMap mMap;
    private Button mSignOut, mrequest, mSetting, mDestination_btn,mHistory_btn ;
    private LatLng pickupLocation;
    private Boolean isAvailable = false;
    private Marker pickUpMarker;
    private EditText mDestinationField;

    private RadioGroup radioGroup;

    final int Location_Request_code = 1;
    private LatLng DestinationLocation;
    private RatingBar ratingbar;

    GoogleApiClient googleApiClient;
    LocationRequest mlocationrequest;
    Location mLastlocation;
    private String destination, mRequestServices;
    private SupportMapFragment mapFragment;
    private LinearLayout mDriverInfo;
    private ImageView mDriverImage;
    private TextView mDriverName , mDriverPhone, mDriverCar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);
        mSignOut  = findViewById(R.id.logout);
        mrequest = findViewById(R.id.request);
        mSetting  = findViewById(R.id.setting);

        mDriverInfo= findViewById(R.id.DriverInfo);
        mDriverImage = findViewById(R.id.DriverProfileImage);
        mDriverName = findViewById(R.id.DriverName);
        mDriverPhone  = findViewById(R.id.DriverPhone);
        mDriverCar = findViewById(R.id.DriverCar);
        mDestinationField  =findViewById(R.id.destinationFelid);
        mDestination_btn = findViewById(R.id.confirm);
        ratingbar  = findViewById(R.id.ratingBar);
        mHistory_btn = findViewById(R.id.history);
        radioGroup = findViewById(R.id.radioGroup);
        radioGroup.check(R.id.uberX);

        DestinationLocation = new LatLng(0.0,0.0);
       
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment    = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapsActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},Location_Request_code);
        }else{
            mapFragment.getMapAsync(this);
        }
        Places.initialize(getApplicationContext(),"AIzaSyCRRrVnyySZ77z9gvp5n2Sof3J2Q-glfLs");
        PlacesClient placesClient = Places.createClient(this);

        mSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapsActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                return;

            }
        });


        mSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent (CustomerMapsActivity.this, CustomerSettingActivity.class);
                startActivity(intent);
            }
        });

        mrequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                if(isAvailable){

                    endride();
                }else {
                    int selected_id = radioGroup.getCheckedRadioButtonId();
                    RadioButton radioButton = findViewById(selected_id);

                    if(radioButton.getText() == null){
                        return;
                    }

                    mRequestServices = radioButton.getText().toString();

                    isAvailable = true;
                    String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("requestcustomer");
                    GeoFire geofire = new GeoFire(ref);
                    geofire.setLocation(user_id, new GeoLocation(mLastlocation.getLatitude(), mLastlocation.getLongitude()));
                    pickupLocation = new LatLng(mLastlocation.getLatitude(), mLastlocation.getLongitude());
                pickUpMarker =     mMap.addMarker(new MarkerOptions().position(pickupLocation).title("pick Here"));
                      mrequest.setText("Cancel Uber ride");
                    getCloserpick();
                }

            }
        });

        mHistory_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapsActivity.this,HistoryActivity.class);
                intent.putExtra("CustomerOrDriver","Customers");
                startActivity(intent);
            }
        });

/*

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.

        List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS,Place.Field.LAT_LNG,Place.Field.NAME);

        autocompleteFragment.setTypeFilter(TypeFilter.ESTABLISHMENT);

        autocompleteFragment.setLocationBias(RectangularBounds.newInstance(new LatLng(-33.880490,151.184363),
                new LatLng(-33.858754,151.229596)));
        autocompleteFragment.setCountries("IN");
       autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
      // autocompleteFragment.setPlaceFields(fieldList);




        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NotNull Place place) {
                // TODO: Get info about the selected place.


                    destination = place.getName().toString();


            }


            @Override
            public void onError(@NotNull Status status) {
                // TODO: Handle the error.

            }
        });


 */
        mDestination_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(mDestinationField.getText().toString())){
                    destination = mDestinationField.getText().toString();

                    getLocationFromAddress(destination);
                }



            }
        });





    }

    private void startAutoCompleteActivtiy() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .setTypeFilter(TypeFilter.CITIES)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(CustomerMapsActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},Location_Request_code);
        }
        buildGoogleApi();
        mMap.setMyLocationEnabled(true);

        // Add a marker in Sydney and move the camera

    }

    protected synchronized  void buildGoogleApi(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mlocationrequest = new LocationRequest();
        mlocationrequest.setInterval(1000);
        mlocationrequest.setFastestInterval(1000);
        mlocationrequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mlocationrequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastlocation = location;
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    private double radius = 1;
    private boolean driverfound  = false;
    private String driverFoundId;
    GeoQuery geoQuery;


    public void getCloserpick(){
        DatabaseReference ref  = FirebaseDatabase.getInstance().getReference("driveravailable");

        GeoFire geoFire = new GeoFire(ref);
         geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude,pickupLocation.longitude),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverfound ){
                    ratingbar.setVisibility(View.VISIBLE);
                    final DatabaseReference databaseReference  =FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists() && snapshot.getChildrenCount() > 0){
                                Map<String,Object> drivermap  = (Map<String,Object>) snapshot.getValue();

                                if(driverfound){
                                    return;
                                }



                                    if(drivermap.get("service").equals(mRequestServices)) {
                                        driverfound = true;
                                        driverFoundId = snapshot.getKey();

                                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");

                                        String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                        HashMap map = new HashMap<>();
                                        map.put("CustomerRideId", user_id);
                                        map.put("destination", destination);
                                        map.put("destinationLat",DestinationLocation.latitude);
                                        map.put("destinationLng",DestinationLocation.longitude);

                                        ref.updateChildren(map);
                                        mrequest.setText("Finding your Ride");
                                        getDriverLocation();
                                        getRidehasEnded();
                                        getDriverInfo();

                                    }

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });





                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverfound && isAvailable){
                    radius++;
                    getCloserpick();


                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }



    private Marker  marker;
    DatabaseReference driverLocation;
    private ValueEventListener driverValueEventListener;
    private void getDriverLocation() {

          driverLocation  = FirebaseDatabase.getInstance().getReference().child("driverworking").child(driverFoundId).child("l");
        driverValueEventListener =   driverLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange( DataSnapshot snapshot) {
                if(snapshot.exists() && isAvailable){
                    List<Object> map  = (List<Object>) snapshot.getValue();

                    double Locationlat = 0;
                    double Locationlong = 0;
                    mrequest.setText("Uber Find Succeesfully");

                    if(map.get(0) != null){
                    Locationlat =Double.parseDouble( map.get(0).toString());

                    }
                    if(map.get(1) != null){
                        Locationlong =Double.parseDouble( map.get(1).toString());

                    }
                    LatLng driverLocation = new LatLng(Locationlat,Locationlong);
                        if(marker != null){marker.remove();}



                        Location loc1  = new Location("");
                        loc1.setLatitude(pickupLocation.latitude);
                        loc1.setLongitude(pickupLocation.longitude);

                        Location loc2  = new Location("");
                        loc2.setLatitude(driverLocation.latitude);
                         loc2.setLongitude(driverLocation.longitude);

                         float distance = loc1.distanceTo(loc2);
                         if(distance <100){
                             mrequest.setText("Driver is Here");
                         }
                         mrequest.setText("Your Cab: "  +  distance);

                    marker =   mMap.addMarker(new MarkerOptions().position(driverLocation).title("Your Cab"));




                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == AUTOCOMPLETE_REQUEST_CODE){
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                destination = place.getName().toString();

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
            return;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case Location_Request_code:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                }else{
                    Toast.makeText(this, "Allow The Permission", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDataReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mCustomerDataReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("name") != null) {

                        mDriverName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {

                        mDriverPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("car") != null) {

                        mDriverCar.setText(map.get("car").toString());
                    }

                    if (map.get("profileImageUri") != null) {

                        Glide.with(getApplication()).load(map.get("profileImageUri").toString()).into(mDriverImage);
                    }
                    int ratingsum = 0;
                    int ratingTotal  = 0;
                    float ratingAvg = 0;


                    for(DataSnapshot child : snapshot.child("rating").getChildren()){
                        ratingsum = ratingsum + Integer.valueOf(child.getValue().toString());
                        ratingTotal++;

                    }
                    if(ratingTotal != 0){
                        ratingAvg = ratingsum/ratingTotal;
                        ratingbar.setRating(ratingAvg);
                    }


                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


  public LatLng getLocationFromAddress(String strAddress){
      Geocoder coder = new Geocoder(this);
      List<Address> address;
      LatLng p1 = null;

      try {
          // May throw an IOException
          address = coder.getFromLocationName(strAddress, 5);
          if (address == null) {
              return null;
          }

          Address location = address.get(0);
          p1 = new LatLng(location.getLatitude(), location.getLongitude() );

      } catch (Exception ex) {

          ex.printStackTrace();
      }
      DestinationLocation  =p1;
      Toast.makeText(this,"Latitiude " + p1.latitude + "Longitude"+ p1.longitude, Toast.LENGTH_SHORT).show();

      return p1;
  }
  private  DatabaseReference driverEndedRef;
    private ValueEventListener driverEndedRefListener;
    public void getRidehasEnded() {

        driverEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest").child("CustomerRideId");
        driverEndedRefListener = driverEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {


                } else {
                    endride();
                }



            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void endride() {
        ratingbar.setVisibility(View.GONE);
        ratingbar.setOnRatingBarChangeListener(null);
        isAvailable = false;
        geoQuery.removeAllListeners();
        driverLocation.removeEventListener(driverValueEventListener);
        driverEndedRef.removeEventListener(driverEndedRefListener);
        String user_id  = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("requestcustomer");
        GeoFire geofire = new GeoFire(ref);
        geofire.removeLocation(user_id);
        if(driverFoundId != null){
            DatabaseReference driverref  = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
            driverref.removeValue();
            driverFoundId = null;
        }
        if(pickUpMarker != null){
            pickUpMarker.remove();
        }
        driverfound = false;
        radius = 1;

        mrequest.setText("Call Uber");
        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("Destination : --" );
        mDriverImage.setImageResource(R.drawable.ic_launcher_background);
    }

}
