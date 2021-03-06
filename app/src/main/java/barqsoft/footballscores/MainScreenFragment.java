package barqsoft.footballscores;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.service.MyFetchService;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainScreenFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {
    public ScoresAdapter mAdapter;
    public static final int SCORES_LOADER = 0;
    private String[] fragmentdate = new String[1];
    private int last_selected_item = -1;

    public MainScreenFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long date = getArguments().getLong("date");
        Date fragmentdate = new Date(date);
        SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
        setFragmentDate(mformat.format(fragmentdate));
    }


    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    private void update_scores() {
        Intent service_start = new Intent(getActivity(), MyFetchService.class);
        getActivity().startService(service_start);
    }

    public void setFragmentDate(String date) {
        fragmentdate[0] = date;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        update_scores();
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final ListView score_list = (ListView) rootView.findViewById(R.id.scores_list);

        //Empty view to display no matches
        View emptyView = rootView.findViewById(R.id.matches_empty);
        score_list.setEmptyView(emptyView);

        mAdapter = new ScoresAdapter(getActivity(), null, 0);
        score_list.setAdapter(mAdapter);
        getLoaderManager().initLoader(SCORES_LOADER, null, this);
        mAdapter.detail_match_id = MainActivity.selected_match_id;
        score_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewHolder selected = (ViewHolder) view.getTag();
                mAdapter.detail_match_id = selected.match_id;
                MainActivity.selected_match_id = (int) selected.match_id;
                mAdapter.notifyDataSetChanged();
            }
        });
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(), DatabaseContract.scores_table.buildScoreWithDate(),
                null, null, fragmentdate, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        //Log.v(FetchScoreTask.LOG_TAG,"loader finished");
        //cursor.moveToFirst();
        /*
        while (!cursor.isAfterLast())
        {
            Log.v(FetchScoreTask.LOG_TAG,cursor.getString(1));
            cursor.moveToNext();
        }
        */

        int i = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            i++;
            cursor.moveToNext();
        }
        //Log.v(FetchScoreTask.LOG_TAG,"Loader query: " + String.valueOf(i));
        mAdapter.swapCursor(cursor);
        //mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    /*
          Updates the empty list view with contextually relevant information that the user can
          use to determine why they aren't seeing weather.
       */
    private void updateEmptyView() {
        if (mAdapter.getCount() == 0) {
            TextView tv = (TextView) getView().findViewById(R.id.matches_empty);
            if (null != tv) {
                // if cursor is empty, why? do we have an invalid location
                int message = R.string.empty_matches_list;
                @MyFetchService.MatchesStatus int matchesStatus = Utilities.getMatchesStatus(getActivity());
                switch (matchesStatus) {
                    case MyFetchService.MATCHES_STATUS_SERVER_DOWN:
                        message = R.string.empty_matches_list_server_down;
                        break;
                    case MyFetchService.MATCHES_STATUS_SERVER_INVALID:
                        message = R.string.empty_matches_list_server_error;
                        break;
                    case MyFetchService.MATCHES_STATUS_INVALID:
                        message = R.string.empty_matches_list_invalid_location;
                        break;
                    default:
                        if (!Utilities.isNetworkAvailable(getActivity())) {
                            message = R.string.empty_matches_list_no_network;
                        }
                }
                tv.setText(message);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_matches_status_key))) {
            updateEmptyView();
        }
    }
}
