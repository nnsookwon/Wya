package bigdee2k.wy.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FacebookAuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import bigdee2k.wy.R;
import bigdee2k.wy.models.FacebookFriend;
import bigdee2k.wy.models.MyRecyclerAdapter;

public class MainActivity extends AppCompatActivity implements MyRecyclerAdapter.RecyclerViewClickListener {

    private static final int REQUEST_IMAGE_CAPTURE = 111;
    private static final String FIREBASE_IMAGES = "FIREBASE_IMAGES";

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    boolean facebookSDKInitilized;


    private ArrayList<FacebookFriend> friends;

    private RecyclerView recyclerView;
    private MyRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* UNCOMMENT IF PLACING CUSTOM IMAGE IN TOP TOOLBAR
        final android.support.v7.app.ActionBar actionBar= getSupportActionBar();
        actionBar.setCustomView(R.layout.actionbar_custom_view_home);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        */

        friends = new ArrayList<>();

        adapter = new MyRecyclerAdapter(friends);
        adapter.setItemListener(this);
        recyclerView = (RecyclerView)findViewById(R.id.friend_list_recycler_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter.notifyDataSetChanged();

        checkAuth();
    }

    private void checkAuth() {
        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        else {
            FacebookSdk.sdkInitialize(getApplicationContext(), new FacebookSdk.InitializeCallback() {
                @Override
                public void onInitialized() {
                    if (AccessToken.getCurrentAccessToken() != null) {
                        facebookSDKInitilized = true;
                        initFriendsList();
                    }
                }
            });

        }
    }

    private void initFriendsList() {
        //Grab User's friends also using this app
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/friends",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                    /* handle the result */
                        Log.d("facebook friends:" ,response.toString());
                        try {
                            friends.clear();
                            JSONObject json = response.getJSONObject();
                            JSONArray jArray = json.getJSONArray("data");
                            Log.d("Facebook Json:", jArray.toString());
                            for (int i = jArray.length()-1; i >= 0; i--){
                                JSONObject jsonFriend = jArray.getJSONObject(i);
                                FacebookFriend friend =
                                        new FacebookFriend(jsonFriend.getString("id"),
                                                jsonFriend.getString("name"),
                                                null);
                                friend.setPhotoUrl("https://graph.facebook.com/" +
                                        friend.getId() + "/picture?type=normal");
                                friends.add(friend);
                            }
                            Log.d("facebook friends", friends.toString());
                            adapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }
                }
        ).executeAsync();

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.button_sign_out:
                logOut(null);
                break;
        }
        return true;
    }

    public void logOut(View view) {
        mFirebaseAuth.signOut(); //sign out of firebase
        LoginManager.getInstance().logOut(); //sign out of facebook
        startActivity(new Intent(this, SignInActivity.class));
        finish();
    }

    // JACK TODO: send wya request upon selecting friend
    @Override
    public void recyclerViewListClicked(View v, int position) {
        FacebookFriend friend = friends.get(position);
        System.out.println("User name: " + friend.getUserName() + "\nID: " + friend.getId() + "\n");
        //requestConfirmation(friend.getUserName());
        onLaunchCamera();
    }

    public void requestConfirmation(String name) {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage("Wya request sent to " + name + "." )
                .setTitle("Wya Request Sent!")
                .show();
    }

    // Check permissions before calling this function
    public void onLaunchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // mImageLabel.setImageBitmap(imageBitmap);
            encodeBitmapAndSaveToFirebase(imageBitmap);
        }
    }

    public void encodeBitmapAndSaveToFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, baos);
        String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(FIREBASE_IMAGES)
                .child(Profile.getCurrentProfile().getId())
                .child("imageUrl");

        ref.setValue(imageEncoded);
    }

    public static Bitmap decodeFromFirebaseBase64(String image) throws IOException {
        byte[] decodedByteArray = android.util.Base64.decode(image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.length);
    }
}
