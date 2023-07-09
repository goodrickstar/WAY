package etc.syndicate.locations;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationService extends Service {
    private final int NOTIFICATION_ID = 12645;
    private final String CHANNEL_ID = "Locations";
    private final String CHANNEL_NAME = "Sharing with friends";
    private final String CHANNEL_GROUP_NAME = "Sharing Status";
    private final String CHANNEL_GROUP_ID = "sharing_status";
    public FusedLocationProviderClient mFusedLocationClient;
    private locationCallback locationCallback = new locationCallback();
    private DatabaseReference databaseReference;
    private User user;
    private ArrayList<Circle> circles = new ArrayList<>();
    private Gson gson = new Gson();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        user = gson.fromJson(intent.getStringExtra("user"), User.class);
        circles = gson.fromJson(intent.getStringExtra("circles"), new TypeToken<List<Circle>>() {}.getType());
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentText("Sharing your location")
                .setSmallIcon(R.drawable.square_pin_w)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //create notification group
        notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(CHANNEL_GROUP_ID, CHANNEL_GROUP_NAME));
        //create notification channel
        NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        serviceChannel.setGroup(CHANNEL_GROUP_ID);
        notificationManager.createNotificationChannel(serviceChannel);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(90000);
        mLocationRequest.setFastestInterval(30000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());
    }

    class locationCallback extends LocationCallback {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null && user != null) {
                for (Location location : locationResult.getLocations()) {
                    Coordinates coordinates = convertLocationToCoordinates(location);
                    coordinates.setUser(user);
                    for (Circle circle : circles) {
                        databaseReference.child("locations").child(circle.getId()).child(user.getUserId()).setValue(coordinates);
                    }
                }
            }
        }

    }

    private Coordinates convertLocationToCoordinates(Location location) {
        return new Coordinates(location.getLatitude(), location.getLongitude(), location.getBearing(), location.getSpeed(), location.getAltitude());
    }

    public LocationService() {
    }
}
