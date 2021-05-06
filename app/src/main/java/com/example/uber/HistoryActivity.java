package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.format.DateFormat;

import com.example.uber.HistoryView.HistoryAdapter;
import com.example.uber.HistoryView.HistoryObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {


    private RecyclerView mHistoryRecyclerView;
    private String CustomerOrDriver, userId;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mHistoryRecyclerView = findViewById(R.id.historyRecyclerView);
        mHistoryRecyclerView.setHasFixedSize(true);
        mHistoryRecyclerView.setNestedScrollingEnabled(true);
        mHistoryLayoutManager = new LinearLayoutManager( HistoryActivity.this);
        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(),HistoryActivity.this);
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);

        CustomerOrDriver= getIntent().getExtras().getString("CustomerOrDriver");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUsersIdHIstory();



    }

    private void getUsersIdHIstory() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(CustomerOrDriver).child(userId).child("history");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    for (DataSnapshot ref : snapshot.getChildren()) {
                        FetchHistoryId(ref.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void FetchHistoryId(String ridekey) {
        DatabaseReference HistoryReference = FirebaseDatabase.getInstance().getReference().child("history").child(ridekey);
        HistoryReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String rideId =  snapshot.getKey();
                        Long timestamp = 0L;
                   for(DataSnapshot timeres : snapshot.getChildren()){
                       if(timeres.getKey().equals(timestamp)){
                           timestamp = Long.valueOf(timeres.getValue().toString());
                       }

                   }
                    HistoryObject historyObject = new HistoryObject(rideId,getDate(timestamp));
                    recyclerHistory.add(historyObject);
                mHistoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String getDate(Long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm", cal).toString();
        return date;
    }

    private ArrayList recyclerHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDataSetHistory() {
        return recyclerHistory;

    }
}