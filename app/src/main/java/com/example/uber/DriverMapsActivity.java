package com.example.uber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.internal.$Gson$Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, RoutingListener {

private GoogleMap mMap;
private Button mSignOut, mStatusRide;
private String CustomerId = "",destination;
private Boolean isDisconnected = false;
private LinearLayout mCustomerInfo;
private int  status  = 0;
private LatLng destinationLatLng;
private ImageView mCustomerImage;
private    SupportMapFragment mapFragment;
private Button mSetting;
private  LatLng pickUpLocation;
private Marker customerDestinationmarker;
private float rideDistance;
private Switch workingSwitch;
private TextView mCustomerName , mCustomerPhone, mCustomerDestination;


        GoogleApiClient googleApiClient;
        LocationRequest mlocationrequest;
        Location mLastlocation;


@Override
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);
        mSignOut  = findViewById(R.id.logout);
        mCustomerInfo= findViewById(R.id.CustomerInfo);
        mCustomerImage = findViewById(R.id.customerProfileImage);
        mCustomerName = findViewById(R.id.customerName);
        mCustomerPhone  = findViewById(R.id.customerPhone);
        workingSwitch = findViewById(R.id.workingSwitch);
        mCustomerDestination = findViewById(R.id.customerDestination);
        mSetting = findViewById(R.id.seting);
        mStatusRide  =findViewById(R.id.rideStatus);
        mStatusRide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        switch(status){
                                case 1:
                                        status = 2;
                                        erasePolyLiens();
                                        
                                        if(destinationLatLng.latitude!=0.0 && destinationLatLng.longitude!= 0.0){
                                               getLocationPickUp(destinationLatLng);
                                        }
                                        mStatusRide.setText("Driver Completed");
                                        break;
                                case 2:
                                        recordRide();
                                        endride();
                                        break;
                        }
                }
        });

        polylines = new ArrayList<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment   = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(DriverMapsActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},Location_Request_code);

        }else{
                mapFragment.getMapAsync(this);
        }

        mSignOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        isDisconnected = true;
                        Destroy();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapsActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                return;
                }
        });

        mSetting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        Intent intent = new Intent(DriverMapsActivity.this,DriverActivitySetting.class);
                        startActivity(intent);

                        return;
                }
        });

        getAssignedCustomer();

        workingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked){
                                connectedHere();
                        }
                }
        });
}

public void getAssignedCustomer(){
        String driver_id  = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverref  = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driver_id).child("customerRequest").child("CustomerRideId");
        driverref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                                status = 1;

                                        CustomerId = snapshot.getValue().toString();
                                        getAssigenedCustomerPickupLocation();
                                        getAssignedCusomerDestination();
                                        getCustomerInfo();

                                }else{
                               endride();

                        }
                        }


                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
        });


}
        Marker pickMarker;
     private    DatabaseReference driverPickUPLocation;
      private   ValueEventListener driverPickUPLocationListener;

        private void getAssigenedCustomerPickupLocation() {

                  driverPickUPLocation  = FirebaseDatabase.getInstance().getReference().child("requestcustomer").child(CustomerId).child("l");
                driverPickUPLocationListener =    driverPickUPLocation.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(snapshot.exists() && !CustomerId.equals("")){

                                        List<Object> map  = (List<Object>)snapshot.getValue();
                                        double Locationlat = 0;
                                        double Locationlong = 0;


                                        if(map.get(0) != null){
                                                Locationlat =Double.parseDouble( map.get(0).toString());

                                        }
                                        if(map.get(1) != null){
                                                Locationlong =Double.parseDouble( map.get(1).toString());

                                        }
                                        pickUpLocation  = new LatLng(Locationlat,Locationlong);
                                   pickMarker =  mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Your PickUP"));
                                   getLocationPickUp(pickUpLocation);

                                }
                        }



                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                });

        }

        private void getLocationPickUp(LatLng pickUpLocation) {

                Routing routing = new Routing.Builder()
                        .travelMode(AbstractRouting.TravelMode.DRIVING)
                        .withListener(this)
                        .alternativeRoutes(true)
                        .waypoints(new LatLng(mLastlocation.getLatitude(), mLastlocation.getLongitude()), pickUpLocation)
                        .build();
                routing.execute();
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

        return;
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

        }

        private void connectedHere(){

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(DriverMapsActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},Location_Request_code);
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

        if(getApplicationContext() != null) {

                if(!CustomerId.equals("")){
                        rideDistance+= mLastlocation.distanceTo(location)/1000;
                }


                String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference DriverAvailableref = FirebaseDatabase.getInstance().getReference("driveravailable");
                DatabaseReference DriverWorking = FirebaseDatabase.getInstance().getReference("driverworking");


                GeoFire geoFireAvailable = new GeoFire(DriverAvailableref);
                GeoFire geoFireWorking = new GeoFire(DriverWorking);

                mLastlocation = location;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                switch (CustomerId) {
                        case "":
                                geoFireWorking.removeLocation(user_id);
                                geoFireAvailable.setLocation(user_id, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                break;
                        default:
                                geoFireAvailable.removeLocation(user_id);
                                geoFireWorking.setLocation(user_id, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                break;
                }
        }

        }
        public void Destroy(){
                String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driveravailable");

                GeoFire geoFire = new GeoFire(ref);
                geoFire.removeLocation(user_id);
        }


        private void getCustomerInfo(){
        mCustomerInfo.setVisibility(View.VISIBLE);
            DatabaseReference  mCustomerDataReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(CustomerId);
                mCustomerDataReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(snapshot.exists() ) {
                                        Map<String, Object> map = (Map<String,Object>) snapshot.getValue();
                                        if(map.get("name") != null){

                                                mCustomerName.setText(map.get("name").toString());
                                        }
                                        if(map.get("phone") != null){

                                                mCustomerPhone.setText(map.get("phone").toString());
                                        } if(map.get("profileImageUri") != null){

                                                Glide.with(getApplication()).load(map.get("profileImageUri").toString()).into(mCustomerImage);
                                        }

                                }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                });
        }

        public void getAssignedCusomerDestination() {
                String driver_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference driverref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driver_id).child("customerRequest");
                driverref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {

                                        Map<String,Object> map = (Map<String,Object>) snapshot.getValue();
                                        if(map.get("destination")!= null){
                                                destination = map.get("destination").toString();
                                                mCustomerDestination.setText("Destinaion --  " + destination);
                                        }else {
                                                mCustomerDestination.setText("Destination : --");

                                        }

                                        double destinationLat = 0;
                                        double destinationLng = 0;
                                        if(map.get("destinationLat")!= null){
                                                destinationLat  = Double.valueOf(map.get("destinationLat").toString());
                                        }
                                        if(map.get("destinationLng")!= null){
                                                destinationLng  = Double.valueOf(map.get("destinationLng").toString());
                                        }
                                        destinationLatLng = new LatLng(destinationLat,destinationLng);



                                }
                        }


                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                });


        }

        final int Location_Request_code = 1;
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);

                switch(requestCode){
                        case Location_Request_code :
                                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                                        mapFragment.getMapAsync(this);
                                }else{
                                        Toast.makeText(this, "Allow The Permission", Toast.LENGTH_SHORT).show();
                                }
                                break;
                }
        }



        private List<Polyline> polylines;
        private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

        @Override
        public void onRoutingFailure(RouteException e) {
                if(e != null) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }else {
                        Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
                }
        }

        @Override
        public void onRoutingStart() {

        }

        @Override
        public void onRoutingSuccess(ArrayList<Route> route, int shortestpickupLocation) {

                if(polylines.size()>0) {
                        for (Polyline poly : polylines) {
                                poly.remove();
                        }
                }

                polylines = new ArrayList<>();
                //add route(s) to the map.
                for (int i = 0; i <route.size(); i++) {

                        //In case of more than 5 alternative routes
                        int colorIndex = i % COLORS.length;

                        PolylineOptions polyOptions = new PolylineOptions();
                        polyOptions.color(getResources().getColor(COLORS[colorIndex]));
                        polyOptions.width(10 + i * 3);
                        polyOptions.addAll(route.get(i).getPoints());
                        Polyline polyline = mMap.addPolyline(polyOptions);
                        polylines.add(polyline);

                        Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
                }
        }

        @Override
        public void onRoutingCancelled() {

        }

        private void erasePolyLiens(){
                for(Polyline line : polylines){
                        line.remove();
                }
                polylines.clear();
        }

        private void endride() {
               mStatusRide.setText("Pick up");
               erasePolyLiens();
                String user_id  = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("requestcustomer");
                GeoFire geofire = new GeoFire(ref);
                geofire.removeLocation(CustomerId);
                CustomerId = "";

                        DatabaseReference driverref  = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id).child("customerRequest");
                        driverref.removeValue();

                if(pickMarker != null){
                        pickMarker.remove();
                }
                if(driverPickUPLocation != null) {
                        driverPickUPLocation.removeEventListener(driverPickUPLocationListener);
                }
                rideDistance = 0;
                mCustomerInfo.setVisibility(View.GONE);
                mCustomerName.setText("");
                mCustomerPhone.setText("");
                mCustomerDestination.setText("Destination : --" );
                mCustomerImage.setImageResource(R.drawable.ic_launcher_background);



        }

        private void recordRide(){
                String user_id  = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference driverref  = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id).child("history");
                DatabaseReference customerref  = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(CustomerId).child("history");
                DatabaseReference historef = FirebaseDatabase.getInstance().getReference().child("history");
                String history  = historef.push().getKey();
                driverref.child(history).setValue(true);
                customerref.child(history).setValue(true);
                HashMap hashMap = new HashMap();
                hashMap.put("driver",user_id);
                hashMap.put("customer",CustomerId);
                hashMap.put("rating",0);
                hashMap.put("Timestamp",getCurrentTimeStamp());
                hashMap.put("destination",destination);
                hashMap.put("location/from/lat",pickUpLocation.latitude);
                hashMap.put("location/from/lng",pickUpLocation.longitude);
                hashMap.put("location/to/lat",destinationLatLng.latitude);
                hashMap.put("location/to/lng",destinationLatLng.longitude);
                hashMap.put("distance",rideDistance);
                historef.child(history).updateChildren(hashMap);
        }

        private Long getCurrentTimeStamp() {
                Long time = System.currentTimeMillis()/1000;
                return time;
        }
}