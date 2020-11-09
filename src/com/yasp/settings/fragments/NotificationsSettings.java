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
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.R;
import com.android.settingslib.search.SearchIndexable;

import com.yasp.settings.preferences.CustomSeekBarPreference;
import com.yasp.settings.preferences.SystemSettingListPreference;
import com.yasp.settings.preferences.SystemSettingMasterSwitchPreference;
import com.yasp.settings.preferences.SystemSettingSwitchPreference;
import com.yasp.settings.Utils;

@SearchIndexable
public class NotificationsSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String INCALL_VIB_OPTIONS = "incall_vib_options";
    private static final String PREF_FLASH_ON_CALL = "flashlight_on_call";
    private static final String PREF_FLASH_ON_CALL_DND = "flashlight_on_call_ignore_dnd";
    private static final String PREF_FLASH_ON_CALL_RATE = "flashlight_on_call_rate";
    private static final String KEY_BATT_LIGHT = "battery_light_enabled";
    private static final String KEY_EDGE_LIGHTNING = "pulse_ambient_light";

    private SystemSettingListPreference mFlashOnCall;
    private SystemSettingSwitchPreference mFlashOnCallIgnoreDND;
    private CustomSeekBarPreference mFlashOnCallRate;
    private SystemSettingMasterSwitchPreference mBatteryLight;
    private SystemSettingMasterSwitchPreference mEdgeLightning;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.yaap_settings_notifications);
        PreferenceScreen prefScreen = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

        PreferenceCategory incallVibCategory = (PreferenceCategory) findPreference(INCALL_VIB_OPTIONS);
        if (!Utils.isVoiceCapable(getActivity())) {
            prefScreen.removePreference(incallVibCategory);
        }

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
        mFlashOnCall.setOnPreferenceChangeListener(this);

        mBatteryLight = (SystemSettingMasterSwitchPreference)
                findPreference(KEY_BATT_LIGHT);
        boolean hasLed = getResources().getBoolean(
                com.android.internal.R.bool.config_hasNotificationLed);
        if (hasLed) {
            boolean enabled = Settings.System.getInt(resolver,
                    KEY_BATT_LIGHT, 1) == 1;
            mBatteryLight.setChecked(enabled);
            mBatteryLight.setOnPreferenceChangeListener(this);
        } else {
            mBatteryLight.setVisible(false);
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
            mFlashOnCallIgnoreDND.setVisible(value > 1);
            mFlashOnCallRate.setVisible(value != 0);
            return true;
        } else if (preference == mFlashOnCallRate) {
            int value = (Integer) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.FLASHLIGHT_ON_CALL_RATE, value);
            return true;
        } else if (preference == mBatteryLight) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver, KEY_BATT_LIGHT, value ? 1 : 0);
            return true;
        } else if (preference == mEdgeLightning) {
            boolean value = (Boolean) newValue;
            Settings.System.putIntForUser(resolver, KEY_EDGE_LIGHTNING,
                    value ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.YASP;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yaap_settings_notifications);
}
