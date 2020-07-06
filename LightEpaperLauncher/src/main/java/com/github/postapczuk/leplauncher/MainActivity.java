package com.github.postapczuk.leplauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MainActivity extends Activity {

    private static final int LIST_PADDING_DP = 60;
    private static final int SEARCH_BOX_PADDING_TOP_DP = 30;
    private static final int SEARCH_BOX_PADDING_BOTTOM_DP = 20;
    private static final int SEARCH_BOX_PADDING_DP = 30;
    private static final int TEXT_SIZE_DP = 25;

    private PackageManager packageManager;
    private ArrayList<String> packageNames;
    private ArrayAdapter<String> adapter;

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private ListView appList;
    private EditText searchBox;
    private List<Pair<String, String>> appsPosition = new ArrayList<>();

    @Override
    protected void onResume() {
        super.onResume();
        if (getActivities().size() - 1 != appsPosition.size()) {
            searchBox.getText().clear();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        setContentView(layout);

        appList = createListView();
        searchBox = createSearchBox(appList);

        layout.addView(searchBox);
        layout.addView(appList);

        fillAppNames();
        assignClickListeners(appList);
        fetchAppList(appList);
    }

    private EditText createSearchBox(final ListView listView) {
        EditText searchBox = new EditText(getApplicationContext());
        searchBox.setBackgroundColor(Color.WHITE);
        searchBox.setTextSize(pxToDp(TEXT_SIZE_DP));
        searchBox.setHint("search");
        searchBox.setPadding(pxToDp(SEARCH_BOX_PADDING_DP), pxToDp(SEARCH_BOX_PADDING_TOP_DP), pxToDp(SEARCH_BOX_PADDING_DP), pxToDp(SEARCH_BOX_PADDING_BOTTOM_DP));
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                fetchAppList(listView);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String lowered = charSequence.toString().toLowerCase();
                lock.readLock().lock();
                List<Pair<String, String>> list = new ArrayList<>();
                for (Pair<String, String> entry : appsPosition) {
                    if (lowered.length() == 0) {
                        list.add(entry);
                    } else {
                        for (String word : entry.first.toLowerCase().split("\\s+")) {
                            if (word.startsWith(lowered)) {
                                list.add(entry);
                                break;
                            }
                        }
                    }
                }
                appsPosition = list;
                (MainActivity.this).adapter.getFilter().filter(charSequence);
                lock.readLock().unlock();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        return searchBox;
    }

    private ListView createListView() {
        ListView listView = new ListView(this);
        listView.setId(android.R.id.list);
        listView.setBackgroundColor(Color.WHITE);
        return listView;
    }

    private int pxToDp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getApplicationContext().getResources().getDisplayMetrics());
    }

    private void fillAppNames() {
        packageManager = getPackageManager();
        adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1,
                new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK);
                textView.setPadding(pxToDp(LIST_PADDING_DP), 0, pxToDp(LIST_PADDING_DP), 0);
                return view;
            }
        };
        packageNames = new ArrayList<>();
    }

    private void assignClickListeners(final ListView listView) {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String packageName = appsPosition.get(position).second;
                try {
                    startActivity(packageManager.getLaunchIntentForPackage(packageName));
                } catch (Exception e) {
                    Toast.makeText(
                            MainActivity.this,
                            String.format("Couldn't launch %s", packageName),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    // Attempt to launch the app with the package name
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + appsPosition.get(position).second));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    fetchAppList(listView);
                }
                return false;
            }
        });
    }

    private void fetchAppList(ListView listView) {
        adapter.clear();
        packageNames.clear();
        appsPosition.clear();

        List<ResolveInfo> activities = getActivities();

        Collections.sort(activities, new ResolveInfo.DisplayNameComparator(packageManager));
        for (ResolveInfo resolver : activities) {
            String appName = (String) resolver.loadLabel(packageManager);
            if (appName.equals("Settings") || appName.equals("Light ePaper Launcher"))
                continue;
            adapter.add(appName);
            packageNames.add(resolver.activityInfo.packageName);
            appsPosition.add(Pair.create(appName, resolver.activityInfo.packageName));
        }
        listView.setAdapter(adapter);
    }

    private List<ResolveInfo> getActivities() {
        return packageManager.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER), 0);
    }
}
