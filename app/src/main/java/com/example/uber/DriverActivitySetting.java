package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverActivitySetting extends AppCompatActivity {

    private EditText mNameField, mPhoneField, mCarField;
    private ImageView mProfileImage;
    private Button mConfirm, mBack;
    private FirebaseAuth mAuth;
    private Uri resultUri;
    private  String   user_id;
    private String mServices;

    private RadioGroup radioGroup;
    private DatabaseReference mDriverDataReference;
    // private String user_id;

    private String mName,mPhone,profileImageUri, mCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_setting);
        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.userphone);
        mConfirm = findViewById(R.id.confirm);
        mBack = findViewById(R.id.back);
        mProfileImage = findViewById(R.id.profileimage);
        mCarField = findViewById(R.id.car);
        radioGroup = findViewById(R.id.radioGroup);

        mAuth = FirebaseAuth.getInstance();
        user_id  = mAuth.getCurrentUser().getUid().toString();
        mDriverDataReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id);
        getInformation();
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savedataInformation();
            }
        });


        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent  = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });
    }

    private void getInformation(){
        mDriverDataReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String,Object>) snapshot.getValue();
                    if(map.get("name") != null){
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if(map.get("phone") != null){
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    } if(map.get("car") != null){
                        mCar = map.get("car").toString();
                        mCarField.setText(mCar);
                    }
                    if(map.get("service") != null){
                        mServices = map.get("service").toString();
                        switch (mServices){
                            case "UberX":
                                radioGroup.check(R.id.uberX);
                                break;
                            case "UberBlack":
                            radioGroup.check(R.id.uberBlack);
                            break;
                           case  "UberXl":
                            radioGroup.check(R.id.uberXL);
                            break;
                        }
                    }
                    if(map.get("profileImageUri") != null){
                        profileImageUri  = map.get("profileImageUri").toString();
                        Glide.with(getApplication()).load(profileImageUri).into(mProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void savedataInformation() {
        mName = mNameField.getText().toString();
        mPhone = mPhoneField.getText().toString();
        mCar   = mCarField.getText().toString();

        int selected_id = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(selected_id);

        if(radioButton.getText() == null){
            return;
        }

        mServices = radioButton.getText().toString();
        HashMap<String,Object> map = new HashMap<>();
        map.put("name",mName);
        map.put("phone",mPhone);
        map.put("car",mCar);
        map.put("service", mServices);
        mDriverDataReference.updateChildren(map);

        if(resultUri != null){
            final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("ProfileImages").child(user_id);

            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream boas =  new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,boas);
            byte[] data = boas.toByteArray();
            UploadTask uploadTask = storageReference.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Uri downloadUri  = uri;
                            Log.i("Dwnload Uri", downloadUri.toString());
                            System.out.println(downloadUri.toString());
                            Map map = new HashMap();
                            map.put("profileImageUri",downloadUri.toString());
                            mDriverDataReference.updateChildren(map);

                            finish();
                            return;

                        }
                    });
                }
            });

        }else {
            finish();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final  Uri uri  = data.getData();
            resultUri   = uri;
            mProfileImage.setImageURI(resultUri);
        }
    }
}