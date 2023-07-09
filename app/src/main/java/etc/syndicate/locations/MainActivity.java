package etc.syndicate.locations;
import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.cb3g.channel19.Logger;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class MainActivity extends FragmentActivity implements MI, ValueEventListener, ChildEventListener, View.OnClickListener, OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraMoveListener {
    private DatabaseReference databaseReference;
    private final recycler_adapter shortcutsAdapter = new recycler_adapter();
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private RequestOptions profileOptions = new RequestOptions().circleCrop();
    private GoogleSignInClient mGoogleSignInClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest = new LocationRequest();
    private final Map<String, String> STATE_MAP = new HashMap<>();
    private Locale locale;
    private int GOOGLESIGNINTAG = 669;
    private Snackbar snackbar;
    private GoogleMap map;
    private FirebaseAuth mAuth;
    private ImageView signInButton, profilePhoto, sharingIcon, followButton;
    private TextView userName, userLocation, mapLocation;
    private ConstraintLayout shelf, navBar;
    private SupportMapFragment mapFragment;
    private ConstraintLayout intro;
    private SharedPreferences sharedPreferences;
    private locationCallback locationCallback = new locationCallback();
    private Position position;
    private boolean sharing = false;
    private User user;
    private ArrayList<Coordinates> users = new ArrayList<>();
    private Gson gson = new Gson();
    private List<Marker> markers = new ArrayList<>();
    private String following;
    private boolean follow = true;
    private ArrayList<Circle> circles = new ArrayList<>();
    private int mapType = 0;
    private ArrayList<UserNotification> notifications = new ArrayList<>();
    private CountDownTimer timerTask;

    @SuppressLint({"NonConstantResourceId", "NotifyDataSetChanged"})
    @Override
    public void onClick(View view) {
        vibrate();
        switch (view.getId()) {
            case R.id.circles:
                showCirclesDialog(0, null);
                break;
            case R.id.sharingIcon:
                if (!sharing) {
                    if (permissionGranted()) {
                        showSnack(new Snack("Location sharing ON", Snackbar.LENGTH_SHORT));
                        sharing = true;
                        startSharing();
                    } else requestionLocationPermisions();
                } else {
                    showSnack(new Snack("Location sharing OFF", Snackbar.LENGTH_SHORT));
                    removeLocations();
                    sharing = false;
                    stopSharing();
                }
                break;
            case R.id.follow:
                if (follow) {
                    showSnack(new Snack("Follow mode DISABLED", Snackbar.LENGTH_SHORT));
                    follow = false;
                } else {
                    showSnack(new Snack("Follow mode ENABLED", Snackbar.LENGTH_SHORT));
                    follow = true;
                    if (following != null) {
                        int markerIndex = indexOfMarkers(following);
                        if (markerIndex != -1) {
                            Marker marker = markers.get(markerIndex);
                            moveCamera(marker.getPosition().latitude, marker.getPosition().longitude, position.getZoom());
                        }
                    }
                }
                updateUi();
                break;
            case R.id.sign_in_button:
                logInWithGoogle();
                break;
            case R.id.profilePhoto:
                logOutWithGoogle();
                break;
            case R.id.myLocation:
                following = user.getUserId();
                shortcutsAdapter.notifyDataSetChanged();
                if (permissionGranted()) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location ->
                            moveCamera(location.getLatitude(), location.getLongitude(), position.getZoom()));
                }
                break;
            case R.id.layers:
                switch (mapType) {
                    case 0:
                        mapType = 1;
                        break;
                    case 1:
                        mapType = 0;
                        break;
                }
                setMapType(mapType);
                sharedPreferences.edit().putInt("maptype", mapType).apply();
                switch (mapType) {
                    case 0:
                        showSnack(new Snack("Map set to NORMAL", Snackbar.LENGTH_SHORT));
                        break;
                    case 1:
                        showSnack(new Snack("Map set to HYBRID", Snackbar.LENGTH_SHORT));
                        break;
                }
                break;
        }
    }

    @Override
    public void showCirclesDialog(int mode, Circle circle) {
        if (circles.isEmpty()) showNewCircleDialog();
        else {
            CirclesDialog circlesDialog = new CirclesDialog();
            Bundle bundle = new Bundle();
            bundle.putString("data", gson.toJson(circles));
            bundle.putString("user", gson.toJson(user));
            bundle.putString("circle", gson.toJson(circle));
            bundle.putInt("mode", mode);
            circlesDialog.setArguments(bundle);
            circlesDialog.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.mydialog);
            circlesDialog.show(getSupportFragmentManager(), "circlesDialog");
        }
    }

    @Override
    public void showNewCircleDialog() {
        CreateCircleDialog createCircleDialog = new CreateCircleDialog();
        Bundle bundle = new Bundle();
        bundle.putBoolean("empty", circles.isEmpty());
        createCircleDialog.setArguments(bundle);
        createCircleDialog.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.mydialog);
        createCircleDialog.show(getSupportFragmentManager(), "createCircleDialog");
    }

    @Override
    public void createNewCircle(String name) {
        Circle circle = new Circle(databaseReference.child("circles").push().getKey(), name, user);
        databaseReference.child("circles").child(circle.getId()).setValue(circle).addOnSuccessListener(aVoid -> {
            databaseReference.child("subscriptions").child(user.getUserId()).child(circle.getId()).setValue(circle).addOnSuccessListener(aVoid1 -> showCirclesDialog(1, circle));
            databaseReference.child("rollcall").child(circle.getId()).child(user.getUserId()).setValue(user);
        });
    }

    @Override
    public void deleteCircle(Circle circle) {
        databaseReference.child("circles").child(circle.getId()).removeValue().addOnSuccessListener(aVoid -> {
            databaseReference.child("subscriptions").child(user.getUserId()).child(circle.getId()).removeValue().addOnSuccessListener(aVoid1 -> showCirclesDialog(0, null));
            databaseReference.child("rollcall").child(circle.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        User user = child.getValue(User.class);
                        databaseReference.child("subscriptions").child(user.getUserId()).child(circle.getId()).removeValue();
                        if (!user.getUserId().equals(MainActivity.this.user.getUserId()))
                            sendMessage(user.getUserId(), new UserNotification(databaseReference.push().getKey(), "Circle was deleted", user.getName() + " deleted " + circle.getName(), user));
                        else
                            postNotification("Circle was deleted", circle.getName() + " was successfully deleted");
                    }
                    databaseReference.child("rollcall").child(circle.getId()).removeValue();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        });
    }

    @Override
    public void leaveCircle(Circle circle) {
        databaseReference.child("subscriptions").child(user.getUserId()).child(circle.getId()).removeValue().addOnSuccessListener(aVoid1 -> {
            showCirclesDialog(0, null);
            databaseReference.child("rollcall").child(circle.getId()).child(user.getUserId()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    postNotification("Left Circle", "You successfully left " + circle.getName());
                }
            });
        });
    }

    @Override
    public void invite(Circle circle) {
        InviteUser inviteUser = new InviteUser();
        Bundle bundle = new Bundle();
        bundle.putString("user", gson.toJson(user));
        bundle.putString("circle", gson.toJson(circle));
        inviteUser.setArguments(bundle);
        inviteUser.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.mydialog);
        inviteUser.show(getSupportFragmentManager(), "inviteUser");
    }

    void postNotification(String title, String content) {
        notifications.add(new UserNotification("", title, content, new User()));
        checkForNotifications();
    }

    private void sendMessage(String receivingId, UserNotification notification) {
        databaseReference.child("notifications").child(receivingId).child("open").child(notification.getNotificationId()).setValue(notification);
    }

    private void checkForNotifications() {
        if (!notifications.isEmpty()) {
            NotificationMessage notificationMessage = new NotificationMessage();
            Bundle bundle = new Bundle();
            bundle.putString("data", gson.toJson(notifications.get(0)));
            notificationMessage.setArguments(bundle);
            notificationMessage.setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.mydialog);
            notificationMessage.show(getSupportFragmentManager(), "notification");
        }
    }

    @Override
    public void deleteNotification(UserNotification notification) {
        if (notification.getNotificationId().isEmpty()) {
            notifications.remove(notification);
            return;
        }
        databaseReference.child("notifications").child(user.getUserId()).child("open").child(notification.getNotificationId()).removeValue();
        databaseReference.child("notifications").child(user.getUserId()).child("closed").child(notification.getNotificationId()).setValue(notification);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) { //notifications
        notifications.clear();
        for (DataSnapshot child : dataSnapshot.getChildren()) {
            notifications.add(child.getValue(UserNotification.class));
        }
        checkForNotifications();
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {//Circles
        String userId = dataSnapshot.getKey();
        if (userId.equals(user.getUserId())) return;
        Coordinates coordinates = dataSnapshot.getValue(Coordinates.class);
        if (indexOfMarkers(userId) == -1) {
            Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(coordinates.getLatitude(), coordinates.getLongitude())));
            marker.setTag(coordinates);
            marker.setFlat(true);
            marker.setTitle(coordinates.getUser().getName());
            marker.setSnippet(Utils.since(coordinates.getStamp()));
            markers.add(marker);
        }
        if (indexOfUsers(userId) == -1) {
            users.add(coordinates);
            shortcutsAdapter.notifyItemInserted(indexOfUsers(coordinates.getUser().getUserId()));
        }
        if (following != null) {
            if (userId.equals(following)) {
                moveCamera(coordinates.getLatitude(), coordinates.getLongitude(), position.getZoom());
                position.setLatitude(coordinates.getLatitude());
                position.setLongitude(coordinates.getLongitude());
            }
        }
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {//Circles
        String userId = dataSnapshot.getKey();
        if (userId.equals(user.getUserId())) return;
        Coordinates coordinates = dataSnapshot.getValue(Coordinates.class);
        int markerIndex = indexOfMarkers(userId);
        int userIndex = indexOfUsers(userId);
        if (markerIndex != -1) {
            //animate marker change
            boolean wasShown = markers.get(markerIndex).isInfoWindowShown();
            markers.get(markerIndex).setTag(coordinates);
            markers.get(markerIndex).setSnippet(Utils.since(coordinates.getStamp()));
            Marker marker = markers.get(markerIndex);
            double[] startValues = new double[]{marker.getPosition().latitude, marker.getPosition().longitude};
            double[] endValues = new double[]{coordinates.getLatitude(), coordinates.getLongitude()};
            ValueAnimator latLngAnimator = ValueAnimator.ofObject(new DoubleArrayEvaluator(), startValues, endValues);
            latLngAnimator.setDuration(600);
            latLngAnimator.setInterpolator(new DecelerateInterpolator());
            latLngAnimator.addUpdateListener(animation -> {
                double[] animatedValue = (double[]) animation.getAnimatedValue();
                markers.get(markerIndex).setPosition(new LatLng(animatedValue[0], animatedValue[1]));
            });
            latLngAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    if (wasShown) markers.get(markerIndex).hideInfoWindow();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (wasShown) markers.get(markerIndex).showInfoWindow();
                }
            });
            latLngAnimator.start();
            ///////////////////////
        }
        if (userIndex != -1) users.set(userIndex, coordinates);
        if (following != null && follow) {
            if (userId.equals(following)) {
                moveCamera(coordinates.getLatitude(), coordinates.getLongitude(), position.getZoom());
                position.setLatitude(coordinates.getLatitude());
                position.setLongitude(coordinates.getLongitude());
            }
        }
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {//Circles
        String userId = dataSnapshot.getKey();
        if (userId.equals(user.getUserId())) return;
        int markerIndex = indexOfMarkers(userId);
        int userIndex = indexOfUsers(userId);
        if (markerIndex != -1) markers.remove(markerIndex);
        if (userIndex != -1) {
            users.remove(userIndex);
            shortcutsAdapter.notifyItemRemoved(userIndex);
        }
        if (following.equals(userId)) following = null;
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {//Circles
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
    }

    private void removeLocations() {
        for (Circle circle : circles) {
            databaseReference.child("locations").child(circle.getId()).child(user.getUserId()).removeValue();
        }
    }

    private void startSharing() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        updateUi();
    }

    private void stopSharing() {
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        updateUi();
    }

    private void updateUi() {
        if (sharing) sharingIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.square_pin_w));
        else sharingIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.square_pin_g));
        if (follow) followButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.follow_yes));
        else followButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.follow_no));
    }

    private void requestionLocationPermisions() {
        if (!permissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Sharing")
                        .setMessage("This app revolves around the Location permission, please accept to allow functionality")
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION))
                        .create()
                        .show();
            } else
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLESIGNINTAG) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                showSnack(new Snack("Google sign in failed", Snackbar.LENGTH_LONG));
            }
        }
    }

    private boolean permissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            showSnack(new Snack("Location sharing ON", Snackbar.LENGTH_SHORT));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            map.setMyLocationEnabled(true);
            sharing = true;
            startSharing();
        }
    }

    private void logInWithGoogle() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), GOOGLESIGNINTAG);
    }

    private void logOutWithGoogle() {
        removeLocations();
        authState(null);
        stopSharing();
        stopListening();
        mAuth.signOut();
        mGoogleSignInClient.signOut();
        showSnack(new Snack("Logged Out", Snackbar.LENGTH_SHORT));
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        showSnack(new Snack("Login succesful", Snackbar.LENGTH_SHORT));
                        authState(mAuth.getCurrentUser());
                        startListening();
                    } else
                        showSnack(new Snack("Firebase Authentification failed", Snackbar.LENGTH_LONG));
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = Utils.getDatabase().getReference();
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        locale = Locale.getDefault();
        signInButton = findViewById(R.id.sign_in_button);
        profilePhoto = findViewById(R.id.profilePhoto);
        shelf = findViewById(R.id.shelf);
        navBar = findViewById(R.id.nav_bar);
        intro = findViewById(R.id.intro);
        userName = findViewById(R.id.userName);
        userLocation = findViewById(R.id.userLocation);
        mapLocation = findViewById(R.id.map_location);
        sharingIcon = findViewById(R.id.sharingIcon);
        ImageView layersButton = findViewById(R.id.layers);
        ImageView locationButton = findViewById(R.id.myLocation);
        ImageView circlesButton = findViewById(R.id.circles);
        followButton = findViewById(R.id.follow);
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        sharing = sharedPreferences.getBoolean("sharing", false) && permissionGranted();
        follow = sharedPreferences.getBoolean("follow", false);
        position = gson.fromJson(sharedPreferences.getString("position", gson.toJson(new Position(40.7128, 74.0060, 17.787579f))), Position.class);
        following = sharedPreferences.getString("following", null);
        mapType = sharedPreferences.getInt("maptype", 0);
        STATE_MAP.putAll(createStateShorts());
        locationRequest.setInterval(30000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        sharingIcon.setOnClickListener(this);
        profilePhoto.setOnClickListener(this);
        layersButton.setOnClickListener(this);
        locationButton.setOnClickListener(this);
        followButton.setOnClickListener(this);
        circlesButton.setOnClickListener(this);
        RecyclerView shortcuts = findViewById(R.id.shortcuts);
        shortcuts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        shortcuts.setAdapter(shortcutsAdapter);
        authState(mAuth.getCurrentUser());
        updateUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isChangingConfigurations()) {
            if (permissionGranted() && sharing && user != null) {
                this.stopService(new Intent(this, LocationService.class));
                startSharing();
            }
            if (user != null) startListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //if (user != null) sendMessage(user.getUserId(), new UserNotification(databaseReference.push().getKey(), "Joined Your Circle", "A random user has accepted your invite to join this circle", user));
        checkForNotifications();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            if (permissionGranted() && user != null && sharing && !circles.isEmpty())
                this.startForegroundService(new Intent(this, LocationService.class).putExtra("user", gson.toJson(user)).putExtra("circles", gson.toJson(circles)));
            stopListening();
            stopSharing();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.edit().putString("position", gson.toJson(position)).apply();
        sharedPreferences.edit().putBoolean("sharing", sharing).apply();
        sharedPreferences.edit().putBoolean("follow", follow).apply();
        sharedPreferences.edit().putString("following", following).apply();
    }

    @Override
    public DatabaseReference database() {
        return databaseReference;
    }

    private void authState(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            user = null;
            signInButton.setOnClickListener(this);
            mapFragment.getView().setVisibility(View.GONE);
            intro.setVisibility(View.VISIBLE);
            shelf.setVisibility(View.GONE);
            navBar.setVisibility(View.GONE);
            userName.setText("");
            userLocation.setText("");
            users.clear();
            circles.clear();
            shortcutsAdapter.notifyDataSetChanged();
        } else {
            user = new User(firebaseUser.getUid(), firebaseUser.getEmail(), firebaseUser.getDisplayName(), firebaseUser.getPhotoUrl().toString().replace("96", "400"), Instant.now().getEpochSecond());
            new GlideImageLoader(this, profilePhoto, findViewById(R.id.progressBar)).load(user.getPhotoUrl(), profileOptions);
            userName.setText(user.getName());
            userLocation.setText("");
            intro.setVisibility(View.GONE);
            mapFragment.getView().setVisibility(View.VISIBLE);
            shelf.setVisibility(View.VISIBLE);
            navBar.setVisibility(View.VISIBLE);
            databaseReference.child("users").child(user.getUserId()).setValue(user);
        }
    }

    private void startListening() {
        databaseReference.child("notifications").child(user.getUserId()).child("open").addValueEventListener(this);
        databaseReference.child("subscriptions").child(user.getUserId()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Circle circle = dataSnapshot.getValue(Circle.class);
                if (!circles.contains(circle)) {
                    circles.add(circle);
                    databaseReference.child("locations").child(circle.getId()).addChildEventListener(MainActivity.this);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Circle circle = dataSnapshot.getValue(Circle.class);
                int circleIndex = indexOfCircles(circle.getId());
                if (circleIndex != -1)
                    circles.remove(circleIndex);
                databaseReference.child("locations").child(circle.getId()).removeEventListener((ChildEventListener) MainActivity.this);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void stopListening() {
        users.clear();
        shortcutsAdapter.notifyDataSetChanged();
        for (Circle circle : circles) {
            databaseReference.child("locations").child(circle.getId()).removeEventListener((ChildEventListener) this);
        }
        circles.clear();
        databaseReference.child("notifications").child(user.getUserId()).child("open").removeEventListener((ValueEventListener) this);
    }

    @Override
    public void moveCamera(double lattitude, double longitude, float zoom) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lattitude, longitude), zoom));
    }

    @Override
    public void onCameraIdle() {
        position.setLatitude(map.getCameraPosition().target.latitude);
        position.setLongitude(map.getCameraPosition().target.longitude);
        position.setZoom(map.getCameraPosition().zoom);
        if (snackbar != null) {
            if (!snackbar.isShownOrQueued())
                locationLabel(mapLocation, position.getLatitude(), position.getLongitude());
        } else locationLabel(mapLocation, position.getLatitude(), position.getLongitude());
    }

    @Override
    public void onCameraMove() {
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Coordinates coordinates = (Coordinates) marker.getTag();
        marker.setSnippet(Utils.since(coordinates.getStamp()));
        /*
        if (marker.isInfoWindowShown()) marker.hideInfoWindow();
        else marker.showInfoWindow();
         */
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        setMapType(mapType);
        map.setIndoorEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setOnCameraIdleListener(this);
        map.setOnCameraMoveListener(this);
        map.setOnMarkerClickListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        if (position == null) return;
        if (following != null && !users.isEmpty()) {
            int index = indexOfUsers(following);
            if (index != -1) {
                Coordinates user = users.get(index);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(user.getLatitude(), user.getLongitude()), position.getZoom()));
            } else
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(position.getLatitude(), position.getLongitude()), position.getZoom()));
        } else
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(position.getLatitude(), position.getLongitude()), position.getZoom()));
    }

    private void setMapType(int type) {
        switch (type) {
            case 0:
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case 1:
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
        }
    }

    private int indexOfUsers(String id) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUser().getUserId().equals(id)) return i;
        }
        return -1;
    }

    private int indexOfMarkers(String userId) {
        for (int i = 0; i < markers.size(); i++) {
            Coordinates coordinates = (Coordinates) markers.get(i).getTag();
            if (userId.equals(coordinates.getUser().getUserId())) return i;
        }
        return -1;
    }

    private int indexOfCircles(String id) {
        for (int i = 0; i < circles.size(); i++) {
            if (circles.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    public void showSnack(Snack snack) {
        snackbar = Snackbar.make(findViewById(R.id.coordinator), snack.getMessage(), snack.getLength());
        View view = snackbar.getView();
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        if (snack.getLength() == Snackbar.LENGTH_INDEFINITE) {
            snackbar.setActionTextColor(Color.WHITE);
            snackbar.setAction("10 4", view1 -> {
                sendBroadcast(new Intent("nineteenVibrate"));
                snackbar.dismiss();
            });
        } else tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        if (mapLocation.getVisibility() == View.VISIBLE) mapLocation.setVisibility(View.INVISIBLE);
        snackbar.show();
    }

    @Override
    public void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    class locationCallback extends LocationCallback {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null) {
                for (Location location : locationResult.getLocations()) {
                    locationLabel(userLocation, location.getLatitude(), location.getLongitude());
                    Coordinates myCoordinates = convertLocationToCoordinates(location);
                    myCoordinates.setUser(user);
                    for (Circle circle : circles) {
                        databaseReference.child("locations").child(circle.getId()).child(user.getUserId()).setValue(myCoordinates);
                    }
                    if (following != null && follow) {
                        if (following.equals(user.getUserId())) {
                            if (position == null)
                                moveCamera(myCoordinates.getLatitude(), myCoordinates.getLongitude(), .16f);
                            else
                                moveCamera(myCoordinates.getLatitude(), myCoordinates.getLongitude(), position.getZoom());
                        }
                    }
                }
            }
        }

    }

    private void locationLabel(TextView textView, double latitude, double longitude) {
        new MyTask().execute(textView, latitude, longitude);
    }

    private Coordinates convertLocationToCoordinates(Location location) {
        return new Coordinates(location.getLatitude(), location.getLongitude(), location.getBearing(), location.getSpeed(), location.getAltitude());
    }

    private String getAbbreviationFromUSState(String state) {
        if (STATE_MAP.containsKey(state)) return STATE_MAP.get(state);
        else return state;
    }

    private Map<String, String> createStateShorts() {
        Map<String, String> map = new HashMap<>();
        map.put("Alabama", "AL");
        map.put("Alaska", "AK");
        map.put("Alberta", "AB");
        map.put("Arizona", "AZ");
        map.put("Arkansas", "AR");
        map.put("British Columbia", "BC");
        map.put("California", "CA");
        map.put("Colorado", "CO");
        map.put("Connecticut", "CT");
        map.put("Delaware", "DE");
        map.put("District of Columbia", "DC");
        map.put("Florida", "FL");
        map.put("Georgia", "GA");
        map.put("Guam", "GU");
        map.put("Hawaii", "HI");
        map.put("Idaho", "ID");
        map.put("Illinois", "IL");
        map.put("Indiana", "IN");
        map.put("Iowa", "IA");
        map.put("Kansas", "KS");
        map.put("Kentucky", "KY");
        map.put("Louisiana", "LA");
        map.put("Maine", "ME");
        map.put("Manitoba", "MB");
        map.put("Maryland", "MD");
        map.put("Massachusetts", "MA");
        map.put("Michigan", "MI");
        map.put("Minnesota", "MN");
        map.put("Mississippi", "MS");
        map.put("Missouri", "MO");
        map.put("Montana", "MT");
        map.put("Nebraska", "NE");
        map.put("Nevada", "NV");
        map.put("New Brunswick", "NB");
        map.put("New Hampshire", "NH");
        map.put("New Jersey", "NJ");
        map.put("New Mexico", "NM");
        map.put("New York", "NY");
        map.put("Newfoundland", "NF");
        map.put("North Carolina", "NC");
        map.put("North Dakota", "ND");
        map.put("Northwest Territories", "NT");
        map.put("Nova Scotia", "NS");
        map.put("Nunavut", "NU");
        map.put("Ohio", "OH");
        map.put("Oklahoma", "OK");
        map.put("Ontario", "ON");
        map.put("Oregon", "OR");
        map.put("Pennsylvania", "PA");
        map.put("Prince Edward Island", "PE");
        map.put("Puerto Rico", "PR");
        map.put("Quebec", "QC");
        map.put("Rhode Island", "RI");
        map.put("Saskatchewan", "SK");
        map.put("South Carolina", "SC");
        map.put("South Dakota", "SD");
        map.put("Tennessee", "TN");
        map.put("Texas", "TX");
        map.put("Utah", "UT");
        map.put("Vermont", "VT");
        map.put("Virgin Islands", "VI");
        map.put("Virginia", "VA");
        map.put("Washington", "WA");
        map.put("West Virginia", "WV");
        map.put("Wisconsin", "WI");
        map.put("Wyoming", "WY");
        map.put("Yukon Territory", "YT");
        return map;
    }

    class recycler_adapter extends RecyclerView.Adapter<recycler_adapter.MyViewHolder> implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            vibrate();
            Coordinates coordinates = (Coordinates) view.getTag();
            following = coordinates.getUser().getUserId();
            int markerIndex = indexOfMarkers(coordinates.getUser().getUserId());
            if (markerIndex != -1) {
                if (markers.get(markerIndex).isInfoWindowShown())
                    markers.get(markerIndex).hideInfoWindow();
            }
            moveCamera(coordinates.getLatitude(), coordinates.getLongitude(), position.getZoom());
            position.setLatitude(coordinates.getLatitude());
            position.setLongitude(coordinates.getLongitude());
            if (timerTask != null) timerTask.cancel();
            for (int x = 0; x < users.size(); x++) {
                if (!users.get(x).getUser().getUserId().equals(following))
                    users.get(x).setChecked(false);
                else users.get(x).setChecked(true);
            }
            shortcutsAdapter.notifyDataSetChanged();
            timerTask = new CountDownTimer(1250, 1250) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    for (int x = 0; x < users.size(); x++) {
                        users.get(x).setChecked(false);
                    }
                    shortcutsAdapter.notifyDataSetChanged();
                }
            };
            timerTask.start();
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(getLayoutInflater().inflate(R.layout.shortcuts_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int i) {
            Coordinates coordinates = users.get(i);
            new GlideImageLoader(MainActivity.this, holder.profile, holder.progressBar).load(coordinates.getUser().getPhotoUrl(), profileOptions);
            holder.profile.setTag(coordinates);
            holder.profile.setOnClickListener(this);
            if (coordinates.getChecked()) {
                holder.label.setVisibility(View.VISIBLE);
                holder.label.setText(coordinates.getUser().getName());
            } else {
                holder.label.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView profile;
            TextView label;
            ProgressBar progressBar;

            MyViewHolder(View itemView) {
                super(itemView);
                profile = itemView.findViewById(R.id.profile);
                label = itemView.findViewById(R.id.name);
                progressBar = itemView.findViewById(R.id.progressBar);
            }
        }
    }

    public class MyTask extends AsyncTask<Object, Void, LabelObject> {
        @Override
        protected LabelObject doInBackground(Object... params) {
            try {
                final Geocoder geoCoder = new Geocoder(MainActivity.this, locale);
                final List<Address> addresses = geoCoder.getFromLocation((double) params[1], (double) params[2], 1);
                if (!addresses.isEmpty()) {
                    final Address address = addresses.get(0);
                    final String city = address.getLocality();
                    final String state = address.getAdminArea();
                    if (address.getCountryCode() != null) {
                        if (city != null && state != null) {
                            if (address.getCountryCode().equals("US"))
                                return new LabelObject((TextView) params[0], city.trim() + ", " + getAbbreviationFromUSState(state));
                            else
                                return new LabelObject((TextView) params[0], address.getLocality() + ", " + address.getCountryCode());
                        }
                    }
                }
            } catch (IOException e) {
                Logger.INSTANCE.e("geoCoder EXCEPTION " + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(LabelObject object) {
            super.onPostExecute(object);
            if (object != null) {
                object.textView.setVisibility(View.VISIBLE);
                object.textView.setText(object.locationString);
            }
        }
    }

    class LabelObject {
        TextView textView;
        String locationString;

        LabelObject(TextView textView, String locationString) {
            this.textView = textView;
            this.locationString = locationString;
        }
    }
}
