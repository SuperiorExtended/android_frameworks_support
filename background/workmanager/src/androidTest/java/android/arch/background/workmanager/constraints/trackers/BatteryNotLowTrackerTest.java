/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.arch.background.workmanager.constraints.trackers;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.background.workmanager.constraints.listeners.BatteryNotLowListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BatteryNotLowTrackerTest {

    private static final int PLUGGED_IN = BatteryManager.BATTERY_PLUGGED_AC;
    private static final int NOT_PLUGGED_IN = BatteryNotLowTracker.BATTERY_PLUGGED_NONE;
    private static final int KNOWN_STATUS = BatteryManager.BATTERY_STATUS_CHARGING;
    private static final int UNKNOWN_STATUS = BatteryManager.BATTERY_STATUS_UNKNOWN;
    private static final float AT_LOW_PERCENTAGE = BatteryNotLowTracker.BATTERY_LOW_PERCENTAGE;
    private static final float ABOVE_LOW_PERCENTAGE  =
            BatteryNotLowTracker.BATTERY_LOW_PERCENTAGE + 0.01f;

    private Context mMockContext;
    private BatteryNotLowTracker mTracker;
    private BatteryNotLowListener mListener;

    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        when(mMockContext.getApplicationContext()).thenReturn(mMockContext);

        mTracker = new BatteryNotLowTracker(mMockContext);
        mListener = mock(BatteryNotLowListener.class);
        mTracker.mListeners.add(mListener);  // Add it silently so no broadcasts trigger.
    }

    @After
    public void tearDown() {
        mTracker.mListeners.remove(mListener);
    }

    @Test
    public void testGetIntentFilter() {
        IntentFilter intentFilter = mTracker.getIntentFilter();
        assertThat(intentFilter.hasAction(Intent.ACTION_BATTERY_OKAY), is(true));
        assertThat(intentFilter.hasAction(Intent.ACTION_BATTERY_LOW), is(true));
        assertThat(intentFilter.countActions(), is(2));
    }

    @Test
    public void testOnBroadcastReceive_invalidIntentAction_doesNotNotifyListeners() {
        mTracker.onBroadcastReceive(mMockContext, new Intent("INVALID"));
        verify(mListener, never()).setBatteryNotLow(anyBoolean());
    }

    @Test
    public void testOnBroadcastReceive_notifiesListeners() {
        mTracker.onBroadcastReceive(mMockContext, new Intent(Intent.ACTION_BATTERY_OKAY));
        verify(mListener).setBatteryNotLow(true);
        mTracker.onBroadcastReceive(mMockContext, new Intent(Intent.ACTION_BATTERY_LOW));
        verify(mListener).setBatteryNotLow(false);
    }

    private Intent createBatteryChangedIntent(int plugged, int status, float percent) {
        int scale = 100;
        int level = (int) (scale * percent);

        Intent intent = new Intent();
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, plugged);
        intent.putExtra(BatteryManager.EXTRA_STATUS, status);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, level);
        intent.putExtra(BatteryManager.EXTRA_SCALE, scale);
        return intent;
    }

    private void testSetUpInitialStateHelper(
            int plugged, int status, float percentage, boolean expectedBatteryNotLow) {
        Intent batteryChangedIntent = createBatteryChangedIntent(plugged, status, percentage);

        when(mMockContext.registerReceiver((BroadcastReceiver) isNull(), any(IntentFilter.class)))
                .thenReturn(batteryChangedIntent);

        mTracker.setUpInitialState(mListener);
        assertThat(mTracker.mIsBatteryNotLow, is(expectedBatteryNotLow));
    }

    @Test
    public void testSetUpInitialState_notPlugged_knownStatus_atBatteryLowPercentage() {
        testSetUpInitialStateHelper(NOT_PLUGGED_IN, KNOWN_STATUS, AT_LOW_PERCENTAGE, false);
    }

    @Test
    public void testSetUpInitialState_plugged_knownStatus_aboveBatteryLowPercentage() {
        testSetUpInitialStateHelper(PLUGGED_IN, KNOWN_STATUS, ABOVE_LOW_PERCENTAGE, true);
    }

    @Test
    public void testSetUpInitialState_plugged_knownStatus_atBatteryLowPercentage() {
        testSetUpInitialStateHelper(PLUGGED_IN, KNOWN_STATUS, AT_LOW_PERCENTAGE, true);
    }

    @Test
    public void testSetUpInitialState_plugged_unknownStatus_aboveBatteryLowPercentage() {
        testSetUpInitialStateHelper(PLUGGED_IN, UNKNOWN_STATUS, ABOVE_LOW_PERCENTAGE, true);
    }

    @Test
    public void testSetUpInitialState_plugged_unknownStatus_atBatteryLowPercentage() {
        testSetUpInitialStateHelper(PLUGGED_IN, UNKNOWN_STATUS, AT_LOW_PERCENTAGE, true);
    }

    @Test
    public void testSetUpInitialState_notPlugged_knownStatus_aboveBatteryLowPercentage() {
        testSetUpInitialStateHelper(NOT_PLUGGED_IN, KNOWN_STATUS, ABOVE_LOW_PERCENTAGE, true);
    }

    @Test
    public void testSetUpInitialState_notPlugged_unknownStatus_aboveBatteryLowPercentage() {
        testSetUpInitialStateHelper(NOT_PLUGGED_IN, UNKNOWN_STATUS, ABOVE_LOW_PERCENTAGE, true);
    }

    @Test
    public void testSetUpInitialState_notPlugged_unknownStatus_atBatteryLowPercentage() {
        testSetUpInitialStateHelper(NOT_PLUGGED_IN, UNKNOWN_STATUS, AT_LOW_PERCENTAGE, true);
    }
}
