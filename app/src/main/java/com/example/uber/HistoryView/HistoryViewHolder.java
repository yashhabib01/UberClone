package com.example.uber.HistoryView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber.HistorySingleActivity;
import com.example.uber.R;

public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


     TextView rideId;
     TextView time;
    public HistoryViewHolder(@NonNull View itemView) {
        super(itemView);

        itemView.setOnClickListener(this);
       rideId = itemView.findViewById(R.id.rideId);
       time = itemView.findViewById(R.id.time);

    }

    @Override
    public void onClick(View v) {

        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        intent.putExtra("rideId",rideId.getText().toString());
        v.getContext().startActivity(intent);

    }
}
