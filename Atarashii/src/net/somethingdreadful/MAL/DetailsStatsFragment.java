package net.somethingdreadful.MAL;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.somethingdreadful.MAL.R;

public class DetailsStatsFragment extends Fragment {
    public DetailsStatsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        TextView textView = new TextView(getActivity());
        textView.setGravity(Gravity.CENTER);
        Bundle args = getArguments();
        textView.setText("STATS HERE");
        return textView;
    }
}