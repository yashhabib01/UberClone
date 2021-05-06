package com.example.uber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity  implements OnMapReadyCallback, RoutingListener {

    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private String currentuser_id, rideId, customerId, driverId, DriverorCustomer;
    private TextView mrideLocation;
    private TextView mrideDistance;
    private LatLng destinationLatLng,pickUpLocationLatLng;
    private TextView mrideDate;
    private TextView userName;
    private TextView userPhone;
    private ImageView userImage;
    private RatingBar ratingBar;
    private String distance;
    private double ridePrice;
    private Button mpayHere;
    private Boolean customerPaid = false;
    private DatabaseReference historyRideInfoDb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);
        rideId = getIntent().getExtras().getString("rideId");
        currentuser_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        polylines = new ArrayList<>();
        Places.initialize(getApplicationContext(),"AIzaSyCRRrVnyySZ77z9gvp5n2Sof3J2Q-glfLs");
        PlacesClient placesClient = Places.createClient(this);

        Intent intent  = new Intent(this,PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,config);
        startService(intent);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mrideLocation = findViewById(R.id.ridedestination);
        mrideDate = findViewById(R.id.rideDate);
        mrideDistance = findViewById(R.id.rideDistance);
        userPhone = findViewById(R.id.userphone);
        userName = findViewById(R.id.username);
        mpayHere= findViewById(R.id.pay);
        userImage = findViewById(R.id.userImage);
        ratingBar = findViewById(R.id.ratingBar);
        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getRideInformation();

    }

    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot child : snapshot.getChildren()){
                        if(child.getKey().equals("customer")){
                            customerId  = child.getValue().toString();
                            if(!customerId.equals(currentuser_id)){
                                DriverorCustomer = "Drivers";
                                getUserINformation("Customers",customerId);

                            }

                        }if(child.getKey().equals("driver")){
                            driverId  = child.getValue().toString();
                            if(!driverId.equals(currentuser_id)){
                                DriverorCustomer = "Customers";
                                getDriverINfoObjecst();
                                getUserINformation("Drivers",driverId);
                            }
                        } if(child.getKey().equals("Timestamp")){
                            mrideDate.setText(getDate(Long.valueOf( child.getValue().toString())));
                        } if(child.getKey().equals("rating")){
                          ratingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }

                        if(child.getKey().equals("destination")){
                            mrideDistance.setText( child.getValue().toString());
                        } if(child.getKey().equals("distance")){
                            distance = child.getValue().toString();
                            mrideLocation.setText( distance.substring(0,Math.min(distance.length(),5)) + "km" );
                            ridePrice = Double.valueOf(distance)*0.5;
                        }if(child.getKey().equals("CustomerPaid")){
                            customerPaid = true;
                        }
                        if(child.getKey().equals("location")){
                            pickUpLocationLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("from").child("lng").getValue().toString()));
                        if(destinationLatLng != new LatLng(0,0)){
                           getLocationPickUp();
                        }

                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getDriverINfoObjecst() {

        ratingBar.setVisibility(View.VISIBLE);
        mpayHere.setVisibility(View.VISIBLE);
        mpayHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPaypalPayment();
            }
        });

        if(customerPaid){
            mpayHere.setEnabled(false);
        }else{
            mpayHere.setEnabled(true);
        }
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideInfoDb.child("rating").setValue(rating);
                DatabaseReference ref  = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("rating");
                ref.child(rideId).setValue(rating);
            }
        });
    }

    private int PAL_PAYMENT  = 1;
    public static PayPalConfiguration config  =  new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PayPalConfig.Client_Pay_Id);

    private void getPaypalPayment() {
        PayPalPayment payPalPayment = new PayPalPayment(new BigDecimal(ridePrice) ,"USD","Uber Ride",PayPalPayment.PAYMENT_INTENT_SALE);

        Intent intent = new Intent(HistorySingleActivity.this, PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT,payPalPayment);
        startActivityForResult(intent,PAL_PAYMENT);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode ==  PAL_PAYMENT){
            if(resultCode  == RESULT_OK){
                PaymentConfirmation confirmation = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);

                if(confirmation!= null){

                    try {
                        JSONObject jsonObject = new JSONObject(confirmation.toJSONObject().toString());
                        String payment  = jsonObject.getJSONObject("response").getString("state");

                        if(payment.equals("approved")){
                            Toast.makeText(this, "Succesful", Toast.LENGTH_SHORT).show();
                            historyRideInfoDb.child("CustomerPaid").setValue(true);
                            mpayHere.setEnabled(false);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }else{
                Toast.makeText(this, "Transaction Unscessfull", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(getApplicationContext(),PayPalService.class));
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
    private String getDate(Long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm", cal).toString();
        return date;
    }

    private void getUserINformation(String OtherUserDriverOrCustomer , String otherId){
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(OtherUserDriverOrCustomer).child(otherId);
    databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (snapshot.exists()) {
                Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                if (map.get("name") != null) {
                    userName.setText(map.get("name").toString());
                }
                if (map.get("phone") != null) {
                    userPhone.setText(map.get("phone").toString());
                }
                if (map.get("profileImageUri") != null) {
                    Glide.with(getApplication()).load(map.get("profileImageUri").toString()).into(userImage);
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    });
    }

    private void getLocationPickUp() {

        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(true)
                .waypoints( pickUpLocationLatLng,destinationLatLng)
                .build();
        routing.execute();
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

            LatLngBounds.Builder builder  = new LatLngBounds.Builder();
            builder.include(pickUpLocationLatLng);
            builder.include(destinationLatLng);

            int width = getResources().getDisplayMetrics().widthPixels;
            int padding = (int) (width*0.2);

            LatLngBounds bounds  = builder.build();
            CameraUpdate cameraUpdate =  CameraUpdateFactory.newLatLngBounds(bounds,padding);
            map.animateCamera(cameraUpdate);

            map.addMarker(new MarkerOptions().position(pickUpLocationLatLng).title("pick Location"));
            map.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));


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
                Polyline polyline = map.addPolyline(polyOptions);
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
}