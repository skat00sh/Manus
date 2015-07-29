/**
 * Copyright (C) 2015 Manus Machina
 *
 * This file is part of the Manus SDK.
 *
 * Manus SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Manus SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Manus SDK. If not, see <http://www.gnu.org/licenses/>.
 */

package com.manusmachina.labs.manusmobile;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;

import com.manusmachina.labs.manussdk.*;


public class ManusTestActivity extends ActionBarActivity implements ActionBar.OnNavigationListener, Manus.OnGloveChangedListener {
    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    private long timestamp = System.currentTimeMillis();
    private ManusTestActivity mScope = this;
    private Manus.GloveBinder mBinder = null;
    private ArrayAdapter<String> mArray = null;
    private int mSelectedGlove = 0;
    private Menu mMenu = null;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mBinder = (Manus.GloveBinder) service;
            mBinder.addOnGloveChangedListener(mScope);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manus_test);

        // Bind to Manus service
        Intent intent = new Intent(this, Manus.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // Create an array for navigation,
        mArray = new ArrayAdapter<String>(
                actionBar.getThemedContext(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                new String[] { "Glove 0", "Glove 1" });

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                mArray,
                this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinder = null;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getSupportActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getSupportActionBar().getSelectedNavigationIndex());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_manus_test, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.calibrate_imu) {
            mBinder.getGlove(mSelectedGlove).calibrate(true, true, false);
            return true;
        } else if (id == R.id.calibrate_fingers) {
            mBinder.getGlove(mSelectedGlove).calibrate(false, false, true);
            ProgressDialog.show(this, "Finger Calibration", "Open and close the hands to calibrate.", true, true, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    mBinder.getGlove(mSelectedGlove).calibrate(false, false, false);
                }
            });
            return true;
        } else if (id == R.id.calibrate_hand) {
            mBinder.getGlove(mSelectedGlove).setHandedness(!item.isChecked());
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        mSelectedGlove = position;
        return true;
    }

    @Override
    public void OnGloveChanged(int index, Glove glove) {
        if (index != mSelectedGlove)
            return;

        Glove.Quaternion quat = glove.getQuaternion();
        Glove.Vector euler = glove.getEuler(quat);
        Glove.Vector degrees = euler.ToDegrees();
        float[] fingers = glove.getFingers();

        SeekBar yaw = (SeekBar)findViewById(R.id.yaw);
        SeekBar pitch = (SeekBar)findViewById(R.id.pitch);
        SeekBar roll = (SeekBar)findViewById(R.id.roll);
        SeekBar[] fingerBars = {
                (SeekBar)findViewById(R.id.thumb),
                (SeekBar)findViewById(R.id.index),
                (SeekBar)findViewById(R.id.middle),
                (SeekBar)findViewById(R.id.ring),
                (SeekBar)findViewById(R.id.pinky)
        };
        SeekBar interval = (SeekBar)findViewById(R.id.interval);

        roll.setProgress((int)degrees.x + 180);
        pitch.setProgress((int)degrees.y + 90);
        yaw.setProgress((int) degrees.z + 180);
        for (int i = 0; i < 5; i++)
            fingerBars[i].setProgress((int)(fingers[i] * 255.0f));
        interval.setProgress((int)(System.currentTimeMillis() - timestamp));

        mMenu.findItem(R.id.calibrate_hand).setChecked(glove.getHandedness() == Glove.Handedness.RIGHT_HAND);

        timestamp = System.currentTimeMillis();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.activity_manus_test, container, false);
            return rootView;
        }
    }

}
