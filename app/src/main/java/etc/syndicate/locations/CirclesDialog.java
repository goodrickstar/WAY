package etc.syndicate.locations;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CirclesDialog extends DialogFragment implements View.OnClickListener {
    private RequestOptions profileOptions = new RequestOptions().circleCrop();
    private MI MI;
    private Context context;
    private UsersAdapter usersAdapter;
    private RecyclerView circlesRecyclerView;
    private RecyclerView usersRecyclerView;
    private ArrayList<Circle> circles = new ArrayList<>();
    private ArrayList<UsersArray> users = new ArrayList<>();
    private TextView title, backButton, invite;
    private ImageView addbutton;
    private int mode = 0;
    private User user;
    private Gson gson = new Gson();

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                switch (mode) {
                    case 0:
                        dismiss();
                        break;
                    case 1:
                        mode = 0;
                        updateUi();
                        break;
                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        if (MI != null) MI.vibrate();
        switch (view.getId()) {
            case R.id.invite:
                if (MI != null) MI.invite(usersAdapter.getCircle());
                dismiss();
                break;
            case R.id.back:
                switch (mode) {
                    case 0: //New Circle
                        dismiss();
                        break;
                    case 1: //Delete Circle
                        mode = 0;
                        updateUi();
                        break;
                }
                break;
            case R.id.logo:
                updateUi();
                break;
            case R.id.add_circle:
                switch (mode) {
                    case 0: //New Circle
                        if (MI != null) MI.showNewCircleDialog();
                        dismiss();
                        break;
                    case 1: //Delete Circle
                        Circle circle = usersAdapter.getCircle();
                        if (MI != null && circle != null) {
                            if (circle.getOwner().getUserId().equals(user.getUserId()))
                                MI.deleteCircle(circle);
                            else MI.leaveCircle(circle);
                        }
                        dismiss();
                        break;
                }
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        MI = (MI) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.circles_dialog_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        circles = gson.fromJson(getArguments().getString("data", null), new TypeToken<List<Circle>>() {
        }.getType());
        user = gson.fromJson(getArguments().getString("user", null), User.class);
        mode = getArguments().getInt("mode");
        title = view.findViewById(R.id.title);
        invite = view.findViewById(R.id.invite);
        ImageView logoButton = view.findViewById(R.id.logo);
        addbutton = view.findViewById(R.id.add_circle);
        backButton = view.findViewById(R.id.back);
        DatabaseReference databaseReference = MI.database();
        backButton.setOnClickListener(this);
        logoButton.setOnClickListener(this);
        addbutton.setOnClickListener(this);
        invite.setOnClickListener(this);
        CirclesAdapter circlesAdapter = new CirclesAdapter(); //channels setup
        circlesRecyclerView = view.findViewById(R.id.circles_recycler);
        circlesRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        circlesRecyclerView.setHasFixedSize(true);
        circlesRecyclerView.setAdapter(circlesAdapter);
        usersRecyclerView = view.findViewById(R.id.users_recycler);//users setup
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        usersRecyclerView.setHasFixedSize(true);
        if (mode == 1) {
            Circle circle = gson.fromJson(getArguments().getString("circle", gson.toJson(new Circle())), Circle.class);
            usersAdapter = new UsersAdapter(new ArrayList<>(), circle);
            showCircle(circle);
        }
        updateUi();
        for (Circle circle : circles) {
            databaseReference.child("rollcall").child(circle.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ArrayList<User> circleUsers = new ArrayList<>();
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        User userClass = child.getValue(User.class);
                        if (!userClass.getUserId().equals(user.getUserId()))
                            circleUsers.add(userClass);
                    }
                    users.add(new UsersArray(circle.getId(), circleUsers));
                    circlesAdapter.notifyItemChanged(indexOfCircles(circle.getId()));
                    if (mode == 1) usersAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private int indexOfCircles(String id) {
        for (int i = 0; i < circles.size(); i++) {
            if (circles.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    private void showCircle(Circle circle) {
        mode = 1;
        title.setText(circle.getName());
        UsersArray usersArray = returnUsersArray(circle);
        if (usersArray != null) usersAdapter = new UsersAdapter(usersArray.getUsers(), circle);
        usersRecyclerView.setAdapter(usersAdapter);
        updateUi();
    }

    private void updateUi() {
        switch (mode) {
            case 0: //channels
                circlesRecyclerView.setVisibility(View.VISIBLE);
                usersRecyclerView.setVisibility(View.INVISIBLE);
                title.setText("My Circles");
                backButton.setText("CLOSE");
                addbutton.setImageResource(R.drawable.add_circle_w);
                addbutton.setVisibility(View.VISIBLE);
                invite.setVisibility(View.INVISIBLE);
                break;
            case 1: //users
                circlesRecyclerView.setVisibility(View.INVISIBLE);
                usersRecyclerView.setVisibility(View.VISIBLE);
                backButton.setText("BACK");
                addbutton.setVisibility(View.VISIBLE);
                addbutton.setImageResource(R.drawable.delete_w);
                if (usersAdapter.getCircle().getOwner().getUserId().equals(user.getUserId())) {//owner
                    invite.setVisibility(View.VISIBLE);
                } else {//member
                    invite.setVisibility(View.INVISIBLE);
                }
                break;
        }
    }

    private UsersArray returnUsersArray(Circle circle) {
        for (UsersArray array : users) {
            if (array.getId().equals(circle.getId())) return array;
        }
        return null;
    }

    class CirclesAdapter extends RecyclerView.Adapter<CirclesAdapter.CircleViewHolder> implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (MI != null) MI.vibrate();
            showCircle((Circle) view.getTag());
        }

        @NonNull
        @Override
        public CircleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CircleViewHolder(getLayoutInflater().inflate(R.layout.circles_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CircleViewHolder holder, int i) {
            Circle circle = circles.get(i);
            LocalDateTime datetime = LocalDateTime.ofInstant(Instant.ofEpochSecond(circle.getStamp()), ZoneOffset.UTC);
            String formatted = DateTimeFormatter.ofPattern("MM-dd-yyyy").format(datetime);
            holder.name.setText(circle.getName());
            holder.ownerName.setText("Created by " + circle.getOwner().getName());
            holder.date.setText("on " + formatted);
            UsersArray usersArray = returnUsersArray(circle);
            if (usersArray != null)
                holder.count.setText(String.valueOf(usersArray.getUsers().size()));
            holder.itemView.setTag(circle);
            holder.itemView.setOnClickListener(this);
        }

        @Override
        public int getItemCount() {
            return circles.size();
        }

        class CircleViewHolder extends RecyclerView.ViewHolder {
            TextView name, ownerName, date, count;

            CircleViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.name);
                ownerName = itemView.findViewById(R.id.owner_name);
                date = itemView.findViewById(R.id.date);
                count = itemView.findViewById(R.id.users_count);
            }
        }
    }

    class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
        private ArrayList<User> circleUsers;
        private Circle circle;

        UsersAdapter(ArrayList<User> circleUsers, Circle circle) {
            this.circleUsers = circleUsers;
            this.circle = circle;
        }

        Circle getCircle() {
            return circle;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserViewHolder(getLayoutInflater().inflate(R.layout.user_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int i) {
            User user = circleUsers.get(i);
            new GlideImageLoader(context, holder.profilePhoto, holder.progressBar).load(user.getPhotoUrl(), profileOptions);
            holder.userName.setText(user.getName());
            holder.itemView.setTag(user);
        }

        @Override
        public int getItemCount() {
            return circleUsers.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView profilePhoto;
            ProgressBar progressBar;
            TextView userName;

            UserViewHolder(View itemView) {
                super(itemView);
                profilePhoto = itemView.findViewById(R.id.profilePhoto);
                progressBar = itemView.findViewById(R.id.progressBar);
                userName = itemView.findViewById(R.id.userName);
            }
        }
    }
}
