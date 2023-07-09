package etc.syndicate.locations;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.jetbrains.annotations.NotNull;

public class CreateCircleDialog extends DialogFragment implements View.OnClickListener {
    private MI MI;
    private Context context;
    private EditText channelNameEdittext;
    private boolean empty = false;

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                if (MI != null) MI.showCirclesDialog(0, null);
                Utils.hideKeyboard(context, channelNameEdittext);
                dismiss();
            }
        };
    }

    @Override
    public void onClick(View view) {
        if (MI != null) MI.vibrate();
        Utils.hideKeyboard(context, channelNameEdittext);
        switch (view.getId()) {
            case R.id.search_button:
                dismiss();
                if (MI != null) MI.createNewCircle(channelNameEdittext.getText().toString().trim());
                break;
            case R.id.back:
                dismiss();
                if (MI != null && !empty) MI.showCirclesDialog(0, null);
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
        return inflater.inflate(R.layout.create_circle_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        empty = getArguments().getBoolean("empty", true);
        TextView accept = view.findViewById(R.id.search_button);
        TextView back = view.findViewById(R.id.back);
        channelNameEdittext = view.findViewById(R.id.circle_name);
        accept.setOnClickListener(this);
        back.setOnClickListener(this);
        Utils.showKeyboard(context, channelNameEdittext);
    }
}
