package etc.syndicate.locations;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;

public class NotificationMessage extends DialogFragment {
    private MI MI;
    private Context context;
    private UserNotification notification;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (MI) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notification_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notification = new Gson().fromJson(getArguments().getString("data", null), UserNotification.class);
        TextView title = view.findViewById(R.id.title);
        ImageView profile = view.findViewById(R.id.profilePhoto);
        TextView content = view.findViewById(R.id.content);
        TextView accept = view.findViewById(R.id.search_button);
        title.setText(notification.getTitle());
        new GlideImageLoader(context, profile, view.findViewById(R.id.progressBar)).load(notification.getUser().getPhotoUrl());
        content.setText(notification.getExtra());
        accept.setOnClickListener(view1 -> {
            if (MI != null) {
                MI.vibrate();
                MI.deleteNotification(notification);
            }
            dismiss();
        });
    }
}
