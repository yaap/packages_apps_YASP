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

import com.android.internal.logging.nano.MetricsProto;

import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.yasp.settings.preferences.SecureSettingSwitchPreference;

@SearchIndexable
public class LockScreenSettings extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.yaap_settings_lockscreen);

        final boolean hasFOD = getResources().getBoolean(
                com.android.internal.R.bool.config_needCustomFODView);
        if (!hasFOD) {
            SecureSettingSwitchPreference fodOnDozePref =
                    (SecureSettingSwitchPreference) findPreference("fod_on_doze");
            getPreferenceScreen().removePreference(fodOnDozePref);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.YASP;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yaap_settings_lockscreen);
}
