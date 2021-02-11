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
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.yasp.settings.preferences.CustomSeekBarPreference;
import com.yasp.settings.preferences.SystemSettingSwitchPreference;

@SearchIndexable
public class GestureSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_VOL_MUSIC_CONTROL = "volume_button_music_control";
    private static final String KEY_VOL_MUSIC_CONTROL_DELAY = "volume_button_music_control_delay";

    private SystemSettingSwitchPreference mVolMusicControl;
    private CustomSeekBarPreference mVolMusicControlDelay;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.yaap_settings_gestures);

        final ContentResolver resolver = getContentResolver();

        mVolMusicControlDelay = (CustomSeekBarPreference) findPreference(KEY_VOL_MUSIC_CONTROL_DELAY);
        int value = Settings.System.getIntForUser(resolver,
                KEY_VOL_MUSIC_CONTROL_DELAY, 500, UserHandle.USER_CURRENT);
        mVolMusicControlDelay.setValue(value);
        mVolMusicControlDelay.setOnPreferenceChangeListener(this);

        mVolMusicControl = (SystemSettingSwitchPreference) findPreference(KEY_VOL_MUSIC_CONTROL);
        boolean enabled = Settings.System.getIntForUser(resolver,
                KEY_VOL_MUSIC_CONTROL, 0, UserHandle.USER_CURRENT) == 1;
        mVolMusicControl.setChecked(enabled);
        mVolMusicControl.setOnPreferenceChangeListener(this);
        mVolMusicControlDelay.setVisible(enabled);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mVolMusicControl) {
            boolean enabled = (Boolean) newValue;
            Settings.System.putIntForUser(resolver,
                    KEY_VOL_MUSIC_CONTROL, enabled ? 1 : 0, UserHandle.USER_CURRENT);
            mVolMusicControlDelay.setVisible(enabled);
            return true;
        } else if (preference == mVolMusicControlDelay) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(resolver,
                    KEY_VOL_MUSIC_CONTROL_DELAY, value, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.YASP;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yaap_settings_gestures);
}
