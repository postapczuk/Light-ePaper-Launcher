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
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private PackageManager packageManager;
    private ArrayList<String> packageNames;
    private ArrayAdapter<String> adapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createListView();
        setContentView(listView);

        fillAppNames();
        assignClickListeners();
        fetchAppList();
    }

    private void createListView() {
        listView = new ListView(this);
        listView.setId(android.R.id.list);
        listView.setBackgroundColor(Color.WHITE);
        int pxToDp = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                60,
                getApplicationContext().getResources().getDisplayMetrics());
        listView.setPadding(pxToDp, pxToDp, pxToDp, pxToDp);
    }

    private void fillAppNames() {
        packageManager = getPackageManager();
        adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1,
                new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK);
                return view;
            }
        };
        packageNames = new ArrayList<>();
    }

    private void assignClickListeners() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    startActivity(packageManager.getLaunchIntentForPackage(packageNames.get(position)));
                } catch (Exception e) {
                    fetchAppList();
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    // Attempt to launch the app with the package name
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + packageNames.get(position)));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    fetchAppList();
                }
                return false;
            }
        });
    }

    private void fetchAppList() {
        adapter.clear();
        packageNames.clear();

        List<ResolveInfo> activities = packageManager.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER), 0);

        Collections.sort(activities, new ResolveInfo.DisplayNameComparator(packageManager));
        for (ResolveInfo resolver : activities) {
            String appName = (String) resolver.loadLabel(packageManager);
            if (appName.equals("Settings") || appName.equals("Light Android Launcher"))
                continue;
            adapter.add(appName);
            packageNames.add(resolver.activityInfo.packageName);
        }
        listView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        fetchAppList();
    }
}
