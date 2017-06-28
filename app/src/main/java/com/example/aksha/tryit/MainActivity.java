package com.example.aksha.tryit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aksha.tryit.Model.Feed;
import com.example.aksha.tryit.Model.Thumbnail;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private String mUsername;
    private static final String TAG = "MainActivity";
    public static final String ANONYMOUS = "anonymous";
    private static final int RC_SIGN_IN = 1;
    private int PICK_IMAGE_REQUEST = 2;
    FirebaseDatabase mDatabase;
    FirebaseStorage mStorage;
    RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<Feed, ImageHolder> mAdapter;
    private FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
    private int label_length;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseCrash.log("MainActivity");
        remoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings
                .Builder().setDeveloperModeEnabled(true)
                .build());
        HashMap<String, Object> defaults=new HashMap<>();
        defaults.put("max_labels",1);
        remoteConfig.setDefaults(defaults);
        final Task<Void> fetch=remoteConfig.fetch(0);
        fetch.addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                remoteConfig.activateFetched();
                label_length= (int) remoteConfig.getLong("max_labels");
            }
        });
        initView();


    }

    private void initView() {
        mUsername = ANONYMOUS;
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    onSignedInInitialise(user.getDisplayName());

                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                } else {
                    // User is signed out
                    onSignedOutCleanUp();
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };


        DatabaseReference ref = mDatabase.getReference().child("feed");
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new FirebaseRecyclerAdapter<Feed, ImageHolder>(
                Feed.class,
                R.layout.feed_image_item,
                ImageHolder.class,
                ref) {
            @Override
            public void populateViewHolder(ImageHolder holder, Feed feed, final int position) {
                holder.setmImage(feed.getDownloadUrl(), getApplicationContext());
                String[] x=feed.getTitle().split(" ");
                String str="";
                for (int i = 0; i <label_length; i++) {
                    str+="#"+x[i]+" ";
                }
                holder.mTitle.setText(str);
                holder.parent.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mDatabase.getReference().child("images")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        int x = 0;
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                            if (x == position) {
                                                Thumbnail thumbnail = snapshot.getValue(Thumbnail.class);
                                                Log.i(TAG, "onDataChange: " + thumbnail.getThumbnail());
                                            }
                                            x++;
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
                    }
                });
            }
        };
        recyclerView.setAdapter(mAdapter);

    }

    public void uploadImage(byte[] photoBytes, String lastPathSegment) {

        final DatabaseReference ref = mDatabase.getReference().child("feed").push();
        mStorage.getReference().child(mAuth.getCurrentUser().getUid()).child(lastPathSegment).putBytes(photoBytes)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                        ref.setValue(new Feed(taskSnapshot.getDownloadUrl().toString(),
                                mAuth.getCurrentUser().getUid(), taskSnapshot.getMetadata().getPath(), ""));
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void onSignedOutCleanUp() {
        mUsername = ANONYMOUS;
    }

    private void onSignedInInitialise(String displayName) {
        mUsername = displayName;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }


        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] photoBytes = baos.toByteArray();
                uploadImage(photoBytes, uri.getLastPathSegment());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void openChooser(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    public static class ImageHolder extends RecyclerView.ViewHolder {
        private final ImageView mImage;
        private final TextView mTitle;
        private final CardView parent;

        public ImageHolder(View itemView) {
            super(itemView);
            mImage = (ImageView) itemView.findViewById(R.id.image_item);
            mTitle = (TextView) itemView.findViewById(R.id.title_item);
            parent = (CardView) itemView.findViewById(R.id.parent);
        }

        public void setName(String name) {
            mTitle.setText(name);
        }

        public void setmImage(String downloadUrl, Context applicationContext) {
            Picasso.with(applicationContext).load(downloadUrl).into(mImage);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.cleanup();
    }
}
