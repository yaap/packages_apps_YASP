/*
 * Copyright (C) 2017-2019 The PixelDust Project
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
package com.yasp.settings.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.yasp.settings.preferences.SecureSettingSwitchPreference;
import com.yasp.settings.preferences.SystemSettingListPreference;
import com.yasp.settings.preferences.SystemSettingMasterSwitchPreference;
import com.yasp.settings.preferences.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SearchIndexable
public class StatusBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";

    private static final String NETWORK_TRAFFIC_STATE = "network_traffic_state";
    private static final String CLOCK_POSITION = "statusbar_clock_position";
    private static final String BATTERY_STYLE = "status_bar_battery_style";
    private static final String SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String SHOW_BATTERY_PERCENT_INSIDE = "status_bar_show_battery_percent_inside";

    private SystemSettingMasterSwitchPreference mNetTrafficState;
    private SystemSettingListPreference mClockPosition;
    private SystemSettingListPreference mBatteryStyle;
    private SystemSettingSwitchPreference mBatteryPercent;
    private SystemSettingSwitchPreference mBatteryPercentInside;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.yaap_settings_statusbar);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

        mNetTrafficState = findPreference(NETWORK_TRAFFIC_STATE);
        mNetTrafficState.setOnPreferenceChangeListener(this);
        boolean enabled = Settings.System.getInt(resolver,
                NETWORK_TRAFFIC_STATE, 0) == 1;
        mNetTrafficState.setChecked(enabled);
        updateNetTrafficSummary(enabled);

        mClockPosition = findPreference(CLOCK_POSITION);
        int value = Settings.System.getIntForUser(resolver,
                CLOCK_POSITION, 0, UserHandle.USER_CURRENT);
        mClockPosition.setValue(Integer.toString(value));
        mClockPosition.setSummary(mClockPosition.getEntry());
        mClockPosition.setOnPreferenceChangeListener(this);

        mBatteryPercentInside = findPreference(SHOW_BATTERY_PERCENT_INSIDE);
        mBatteryPercent = findPreference(SHOW_BATTERY_PERCENT);
        enabled = Settings.System.getIntForUser(resolver,
                SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT) == 1;
        mBatteryPercent.setChecked(enabled);
        mBatteryPercent.setOnPreferenceChangeListener(this);
        mBatteryPercentInside.setEnabled(enabled);

        mBatteryStyle = findPreference(BATTERY_STYLE);
        value = Settings.System.getIntForUser(resolver,
                BATTERY_STYLE, 0, UserHandle.USER_CURRENT);
        mBatteryStyle.setValue(Integer.toString(value));
        mBatteryStyle.setSummary(mBatteryStyle.getEntry());
        mBatteryStyle.setOnPreferenceChangeListener(this);
        updatePercentEnablement(value != 2);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNetTrafficSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mNetTrafficState) {
            boolean enabled = (boolean) objValue;
            Settings.System.putInt(resolver, NETWORK_TRAFFIC_STATE, enabled ? 1 : 0);
            updateNetTrafficSummary(enabled);
            return true;
        } else if (preference == mClockPosition) {
            int value = Integer.parseInt((String) objValue);
            int index = mClockPosition.findIndexOfValue((String) objValue);
            mClockPosition.setSummary(mClockPosition.getEntries()[index]);
            Settings.System.putIntForUser(resolver,
                    CLOCK_POSITION, value, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mBatteryStyle) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStyle.findIndexOfValue((String) objValue);
            mBatteryStyle.setSummary(mBatteryStyle.getEntries()[index]);
            Settings.System.putIntForUser(resolver,
                    BATTERY_STYLE, value, UserHandle.USER_CURRENT);
            updatePercentEnablement(value != 2);
            return true;
        } else if (preference == mBatteryPercent) {
            boolean enabled = (boolean) objValue;
            Settings.System.putInt(resolver,
                    SHOW_BATTERY_PERCENT, enabled ? 1 : 0);
            mBatteryPercentInside.setEnabled(enabled);
            return true;
        }
        return false;
    }

    private void updateNetTrafficSummary() {
        final boolean enabled = Settings.System.getInt(
                getActivity().getContentResolver(),
                NETWORK_TRAFFIC_STATE, 0) == 1;
        updateNetTrafficSummary(enabled);
    }

    private void updateNetTrafficSummary(boolean enabled) {
        if (mNetTrafficState == null) return;
        String summary = getActivity().getString(R.string.switch_off_text);
        if (enabled) {
            final boolean onStatus = Settings.System.getInt(
                    getActivity().getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, 0) == 0;
            summary = getActivity().getString(R.string.network_traffic_state_summary);
            summary += " " + (onStatus
                    ? getActivity().getString(R.string.traffic_statusbar)
                    : getActivity().getString(R.string.traffic_expanded_statusbar));
        }
        mNetTrafficState.setSummary(summary);
    }

    private void updatePercentEnablement(boolean enabled) {
        mBatteryPercent.setEnabled(enabled);
        mBatteryPercentInside.setEnabled(enabled && mBatteryPercent.isChecked());
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.YASP;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yaap_settings_statusbar);
}
