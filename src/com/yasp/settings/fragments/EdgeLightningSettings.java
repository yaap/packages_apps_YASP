/*
 * Copyright (C) 2020 Yet Another AOSP Project
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
import android.util.TypedValue;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.yasp.settings.preferences.colorpicker.ColorPickerPreference;
import com.yasp.settings.preferences.CustomSeekBarPreference;
import com.yasp.settings.preferences.SystemSettingListPreference;
import com.yasp.settings.preferences.SystemSettingSwitchPreference;

@SearchIndexable
public class EdgeLightningSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_AMBIENT = "ambient_notification_light_enabled";
    private static final String KEY_DURATION = "ambient_notification_light_duration";
    private static final String KEY_REPEATS = "ambient_notification_light_repeats";
    private static final String KEY_TIMEOUT = "ambient_notification_light_timeout";
    private static final String KEY_COLOR_MODE = "ambient_notification_color_mode";
    private static final String KEY_COLOR = "ambient_notification_light_color";

    private CustomSeekBarPreference mDurationPref;
    private CustomSeekBarPreference mRepeatsPref;
    private SystemSettingListPreference mTimeoutPref;
    private SystemSettingListPreference mColorModePref;
    private ColorPickerPreference mColorPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.edge_lightning_settings);
        final ContentResolver resolver = getContentResolver();
        final int accentColor = getAccentColor();

        SystemSettingSwitchPreference mAmbientPref = findPreference(KEY_AMBIENT);
        boolean aodEnabled = Settings.Secure.getIntForUser(resolver,
                    Settings.Secure.DOZE_ALWAYS_ON, 0, UserHandle.USER_CURRENT) == 1;
        if (!aodEnabled) {
            mAmbientPref.setChecked(false);
            mAmbientPref.setEnabled(false);
            mAmbientPref.setSummary(R.string.aod_disabled);
        }

        mDurationPref = findPreference(KEY_DURATION);
        int value = Settings.System.getIntForUser(resolver,
                KEY_DURATION, 2, UserHandle.USER_CURRENT);
        mDurationPref.setValue(value);
        mDurationPref.setOnPreferenceChangeListener(this);

        mRepeatsPref = findPreference(KEY_REPEATS);
        int repeats = Settings.System.getIntForUser(resolver,
                KEY_REPEATS, 0, UserHandle.USER_CURRENT);
        mRepeatsPref.setValue(repeats);
        mRepeatsPref.setOnPreferenceChangeListener(this);

        mTimeoutPref = findPreference(KEY_TIMEOUT);
        value = Settings.System.getIntForUser(resolver,
                KEY_TIMEOUT, accentColor, UserHandle.USER_CURRENT);
        mTimeoutPref.setValue(Integer.toString(value));
        mTimeoutPref.setSummary(mTimeoutPref.getEntry());
        mTimeoutPref.setOnPreferenceChangeListener(this);
        updateTimeoutEnablement(repeats);

        mColorPref = findPreference(KEY_COLOR);
        value = Settings.System.getIntForUser(resolver,
                KEY_COLOR, accentColor, UserHandle.USER_CURRENT);
        mColorPref.setDefaultColor(accentColor);
        String colorHex = String.format("#%08x", value);
        if (value == accentColor) {
            mColorPref.setSummary(R.string.default_string);
        } else {
            mColorPref.setSummary(colorHex);
        }
        mColorPref.setNewPreviewColor(value);
        mColorPref.setOnPreferenceChangeListener(this);

        mColorModePref = findPreference(KEY_COLOR_MODE);
        value = Settings.System.getIntForUser(resolver,
                KEY_COLOR_MODE, 0, UserHandle.USER_CURRENT);
        mColorModePref.setValue(Integer.toString(value));
        mColorModePref.setSummary(mColorModePref.getEntry());
        mColorModePref.setOnPreferenceChangeListener(this);
        mColorPref.setEnabled(value == 3);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.YASP;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getContentResolver();
        if (preference == mDurationPref) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(resolver,
                    KEY_DURATION, value, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRepeatsPref) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(resolver,
                    KEY_REPEATS, value, UserHandle.USER_CURRENT);
            updateTimeoutEnablement(value);
            return true;
        } else if (preference == mTimeoutPref) {
            int value = Integer.parseInt((String) newValue);
            int index = mTimeoutPref.findIndexOfValue((String) newValue);
            mTimeoutPref.setSummary(mTimeoutPref.getEntries()[index]);
            Settings.System.putIntForUser(resolver,
                    KEY_TIMEOUT, value, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mColorModePref) {
            int value = Integer.parseInt((String) newValue);
            int index = mColorModePref.findIndexOfValue((String) newValue);
            mColorModePref.setSummary(mColorModePref.getEntries()[index]);
            Settings.System.putIntForUser(resolver,
                    KEY_COLOR_MODE, value, UserHandle.USER_CURRENT);
            mColorPref.setEnabled(value == 3);
            return true;
        } else if (preference == mColorPref) {
            int accentColor = getAccentColor();
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.parseInt(String.valueOf(newValue)));
            if (hex.equals(String.format("#%08x", accentColor))) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int color = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putIntForUser(resolver,
                    KEY_COLOR, color, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    private int getAccentColor() {
        final TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, value, true);
        return value.data;
    }

    private void updateTimeoutEnablement(int repeats) {
        if (repeats == 0) {
            int value = Settings.System.getIntForUser(getContentResolver(),
                    KEY_TIMEOUT, 0, UserHandle.USER_CURRENT);
            mTimeoutPref.setValue(Integer.toString(value));
            mTimeoutPref.setSummary(mTimeoutPref.getEntry());
            mTimeoutPref.setEnabled(true);
        } else {
            mTimeoutPref.setSummary(R.string.set_to_zero);
            mTimeoutPref.setEnabled(false);
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.edge_lightning_settings);
}
