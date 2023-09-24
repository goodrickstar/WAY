package etc.syndicate.locations;

import com.google.firebase.database.DatabaseReference;

interface MI {
    void moveCamera(double lattitude, double longitude, float zoom);

    void deleteNotification(UserNotification notification);

    DatabaseReference database();

    void showCirclesDialog(int mode, Circle circle);

    void showNewCircleDialog();

    void createNewCircle(String name);

    void deleteCircle(Circle circle);

    void leaveCircle(Circle circle);

    void invite(Circle circle);
}