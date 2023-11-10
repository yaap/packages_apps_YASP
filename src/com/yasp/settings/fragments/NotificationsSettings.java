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
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.yaap.YaapUtils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.yasp.settings.preferences.CustomSeekBarPreference;
import com.yasp.settings.preferences.SystemSettingListPreference;
import com.yasp.settings.preferences.SystemSettingMasterSwitchPreference;
import com.yasp.settings.preferences.SystemSettingSwitchPreference;
import com.yasp.settings.Utils;

@SearchIndexable
public class NotificationsSettings extends DashboardFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "NotificationsSettings";
    private static final String INCALL_VIB_OPTIONS = "incall_vib_options";
    private static final String FLASH_ON_CALL_OPTIONS = "on_call_flashlight_category";
    private static final String FLASH_ON_NOTIFICATION_OPTIONS = "notification_flashlight_category";
    private static final String PREF_FLASH_ON_CALL = "flashlight_on_call";
    private static final String PREF_FLASH_ON_CALL_DND = "flashlight_on_call_ignore_dnd";
    private static final String PREF_FLASH_ON_CALL_RATE = "flashlight_on_call_rate";
    private static final String PREF_FLASH_ON_NOTIFY = "default_notification_torch";
    private static final String PREF_FLASH_ON_NOTIFY_TIMES = "default_notification_torch1";
    private static final String PREF_FLASH_ON_NOTIFY_RATE = "default_notification_torch2";
    private static final String KEY_BATT_LIGHT = "battery_light_enabled";
    private static final String KEY_EDGE_LIGHTNING = "pulse_ambient_light";

    private SystemSettingListPreference mFlashOnCall;
    private SystemSettingSwitchPreference mFlashOnCallIgnoreDND;
    private CustomSeekBarPreference mFlashOnCallRate;
    private SwitchPreference mFlashOnNotify;
    private CustomSeekBarPreference mFlashOnNotifyTimes;
    private CustomSeekBarPreference mFlashOnNotifyRate;
    private SystemSettingMasterSwitchPreference mEdgeLightning;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.yaap_settings_notifications;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        PreferenceScreen prefScreen = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

        PreferenceCategory incallVibCategory = (PreferenceCategory) findPreference(INCALL_VIB_OPTIONS);
        if (!Utils.isVoiceCapable(getActivity())) {
            prefScreen.removePreference(incallVibCategory);
        }

        if (!YaapUtils.deviceHasFlashlight(getActivity())) {
            PreferenceCategory flashOnCallCategory = (PreferenceCategory)
                    findPreference(FLASH_ON_CALL_OPTIONS);
            PreferenceCategory flashOnNotifCategory = (PreferenceCategory)
                    findPreference(FLASH_ON_NOTIFICATION_OPTIONS);
            prefScreen.removePreference(flashOnCallCategory);
            prefScreen.removePreference(flashOnNotifCategory);
        } else {
            mFlashOnCallRate = (CustomSeekBarPreference)
                    findPreference(PREF_FLASH_ON_CALL_RATE);
            int value = Settings.System.getInt(resolver,
                    Settings.System.FLASHLIGHT_ON_CALL_RATE, 1);
            mFlashOnCallRate.setValue(value);
            mFlashOnCallRate.setOnPreferenceChangeListener(this);

            mFlashOnCallIgnoreDND = (SystemSettingSwitchPreference)
                    findPreference(PREF_FLASH_ON_CALL_DND);
            value = Settings.System.getInt(resolver,
                    Settings.System.FLASHLIGHT_ON_CALL, 0);
            mFlashOnCallIgnoreDND.setVisible(value > 1);
            mFlashOnCallRate.setVisible(value != 0);

            mFlashOnCall = (SystemSettingListPreference)
                    findPreference(PREF_FLASH_ON_CALL);
            mFlashOnCall.setSummary(mFlashOnCall.getEntries()[value]);
            mFlashOnCall.setOnPreferenceChangeListener(this);

            mFlashOnNotifyTimes = (CustomSeekBarPreference)
                    findPreference(PREF_FLASH_ON_NOTIFY_TIMES);
            mFlashOnNotifyRate = (CustomSeekBarPreference)
                    findPreference(PREF_FLASH_ON_NOTIFY_RATE);
            mFlashOnNotify = (SwitchPreference)
                    findPreference(PREF_FLASH_ON_NOTIFY);
            String strVal = Settings.System.getStringForUser(resolver,
                    PREF_FLASH_ON_NOTIFY, UserHandle.USER_CURRENT);
            final boolean enabled = strVal != null && !strVal.isEmpty();
            mFlashOnNotify.setChecked(enabled);
            updateFlashOnNotifyValues(enabled, strVal);
            mFlashOnNotify.setOnPreferenceChangeListener(this);
            mFlashOnNotifyTimes.setOnPreferenceChangeListener(this);
            mFlashOnNotifyRate.setOnPreferenceChangeListener(this);
        }

        mEdgeLightning = (SystemSettingMasterSwitchPreference)
                findPreference(KEY_EDGE_LIGHTNING);
        boolean enabled = Settings.System.getIntForUser(resolver,
                KEY_EDGE_LIGHTNING, 0, UserHandle.USER_CURRENT) == 1;
        mEdgeLightning.setChecked(enabled);
        mEdgeLightning.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mFlashOnCall) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.FLASHLIGHT_ON_CALL, value);
            mFlashOnCall.setSummary(mFlashOnCall.getEntries()[value]);
            mFlashOnCallIgnoreDND.setVisible(value > 1);
            mFlashOnCallRate.setVisible(value != 0);
            return true;
        } else if (preference == mFlashOnCallRate) {
            int value = (Integer) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.FLASHLIGHT_ON_CALL_RATE, value);
            return true;
        } else if (preference == mEdgeLightning) {
            boolean value = (Boolean) newValue;
            Settings.System.putIntForUser(resolver, KEY_EDGE_LIGHTNING,
                    value ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mFlashOnNotify) {
            boolean value = (Boolean) newValue;
            if (!value) setFlashOnNotifyValues(0, 0);
            else setFlashOnNotifyValues(2, 2);
            return true;
        } else if (preference == mFlashOnNotifyTimes) {
            int value = (Integer) newValue;
            setFlashOnNotifyValues(value, mFlashOnNotifyRate.getValue());
            return true;
        } else if (preference == mFlashOnNotifyRate) {
            int value = (Integer) newValue;
            setFlashOnNotifyValues(mFlashOnNotifyTimes.getValue(), value);
            return true;
        }
        return false;
    }

    private void updateFlashOnNotifyValues(boolean enabled) {
        final String val = Settings.System.getStringForUser(
                getActivity().getContentResolver(),
                PREF_FLASH_ON_NOTIFY, UserHandle.USER_CURRENT);
        updateFlashOnNotifyValues(enabled, val);
    }

    private void updateFlashOnNotifyValues(boolean enabled, String val) {
        if (enabled) {
            if (val.equals("1")) {
                mFlashOnNotifyTimes.setValue(2);
                mFlashOnNotifyRate.setValue(2);
            } else {
                String[] vals = val.split(",");
                mFlashOnNotifyTimes.setValue(Integer.valueOf(vals[0]));
                mFlashOnNotifyRate.setValue(Integer.valueOf(vals[1]));
            }
        }
        mFlashOnNotifyTimes.setVisible(enabled);
        mFlashOnNotifyRate.setVisible(enabled);
    }

    private void setFlashOnNotifyValues(int times, int rate) {
        final boolean enabled = times != 0 && rate != 0;
        String val = String.valueOf(times) + "," + String.valueOf(rate);
        if (times == 2 && rate == 2) val = "1";
        Settings.System.putStringForUser(getActivity().getContentResolver(),
                PREF_FLASH_ON_NOTIFY, enabled ? val : null, UserHandle.USER_CURRENT);
        updateFlashOnNotifyValues(enabled, val);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.YASP;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yaap_settings_notifications);
}
