package etc.syndicate.locations;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.firebase.database.FirebaseDatabase;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

class Utils {
    private static FirebaseDatabase mDatabase;

    static FirebaseDatabase getDatabase() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(false);
        }
        return mDatabase;
    }

    static String since(long stamp) {
        Instant then = Instant.ofEpochSecond(stamp);
        Instant now = Instant.now();
        return parseSeconds(Duration.between(then, now).getSeconds());
    }

    private static String parseSeconds(long seconds) {
        int days = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        if (days == 1) return days + " day, " + hours + " hours ago";
        if (days > 0) return days + " days, " + hours + " hours ago";
        if (hours == 1) return hours + " hour, " + minutes + " mins ago";
        if (hours > 0) return hours + " hours, " + minutes + " mins ago";
        if (minutes == 1) return "1 min ago";
        if (minutes < 1) return "just now";
        return minutes + " mins ago";
    }

    static void showKeyboard(Context context, View view) {
        InputMethodManager methodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        view.requestFocus();
        view.postDelayed(() -> methodManager.showSoftInput(view, 0), 200);
    }

    static void hideKeyboard(Context context, View view) {
        InputMethodManager methodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        view.requestFocus();
        view.postDelayed(() -> methodManager.hideSoftInputFromWindow(view.getWindowToken(), 0), 200);
    }
}
