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
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class InviteUser extends DialogFragment implements View.OnClickListener {
    private RequestOptions profileOptions = new RequestOptions().circleCrop();
    private MI MI;
    private Context context;
    private Circle circle;
    private User user;
    private Gson gson = new Gson();
    private SearchView searchBox;
    private ResultsAdapter resultsAdapter = new ResultsAdapter();
    private ArrayList<User> results = new ArrayList<>();
    private ArrayList<User> users = new ArrayList<>();
    private TextView close;

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                if (MI != null) MI.showCirclesDialog(1, circle);
                dismiss();
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.close:
                if (MI != null) {
                    MI.vibrate();
                    MI.showCirclesDialog(0, null);
                }
                dismiss();
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
        return inflater.inflate(R.layout.invite_user_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        user = gson.fromJson(getArguments().getString("user", gson.toJson(new User())), User.class);
        circle = gson.fromJson(getArguments().getString("circle", gson.toJson(new Circle())), Circle.class);
        searchBox = view.findViewById(R.id.search_box);
        close = view.findViewById(R.id.close);
        close.setOnClickListener(this);
        Utils.showKeyboard(context, searchBox);
        searchBox.setQueryHint("name or email");
        searchBox.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (MI != null) MI.vibrate();
                search(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    results.clear();
                    resultsAdapter.notifyDataSetChanged();
                } else search(newText);
                return false;
            }
        });
        //Utils.showKeyboard(context, searchBox);
        RecyclerView resultsRecycler = view.findViewById(R.id.resultsRecycler);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        resultsRecycler.setHasFixedSize(true);
        resultsRecycler.setAdapter(resultsAdapter);
        if (MI != null) {
            MI.database().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    users.clear();
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        users.add(child.getValue(User.class));
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void search(String query) {
        results.clear();
        resultsAdapter.notifyDataSetChanged();
        if (!query.isEmpty()) {
            for (User user : users) {
                if (user.getEmail().equalsIgnoreCase(query) || user.getName().equalsIgnoreCase(query)) {
                    if (!results.contains(user)) {
                        results.add(user);
                        resultsAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ResultsViewHolder> implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (MI != null) MI.vibrate();
        }

        @NonNull
        @Override
        public ResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ResultsViewHolder(getLayoutInflater().inflate(R.layout.user_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ResultsViewHolder holder, int i) {
            User user = results.get(i);
            new GlideImageLoader(context, holder.profilePhoto, holder.progressBar).load(user.getPhotoUrl(), profileOptions);
            holder.userName.setText(user.getName());
            holder.itemView.setTag(user);
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        class ResultsViewHolder extends RecyclerView.ViewHolder {
            ImageView profilePhoto;
            ProgressBar progressBar;
            TextView userName;

            ResultsViewHolder(View itemView) {
                super(itemView);
                profilePhoto = itemView.findViewById(R.id.profilePhoto);
                progressBar = itemView.findViewById(R.id.progressBar);
                userName = itemView.findViewById(R.id.userName);
            }
        }
    }
}
