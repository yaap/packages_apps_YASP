/*
 * Copyright (C) 2022 Yet Another AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yasp.settings.preferences;

import android.content.Context;
import android.content.om.OverlayManager;
import android.content.om.OverlayManagerTransaction;
import android.content.om.OverlayIdentifier;
import android.content.om.OverlayInfo;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.SwitchPreferenceCompat;

import java.lang.SecurityException;
import java.util.List;

public class OverlaySwitchPreference extends SwitchPreferenceCompat {
    private final static String TAG = "OverlaySwitchPreference";
    private final static String SETTINGSNS = "http://schemas.android.com/apk/res-auto";
    private static final String DKEY = "dkey";
    private static final String DKEY_NIGHT_ONLY = "dkeyNightOnly";

    private final String mDisableKey;
    private final boolean mDKeyNightOnly;
    private final int mUserId;
    private final UserHandle mUserHandle;
    private final OverlayManager mOverlayManager;

    public OverlaySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDisableKey = attrs.getAttributeValue(SETTINGSNS, DKEY);
        mDKeyNightOnly = attrs.getAttributeBooleanValue(SETTINGSNS, DKEY_NIGHT_ONLY, false);
        mUserId = UserHandle.myUserId();
        mUserHandle = UserHandle.of(UserHandle.myUserId());
        mOverlayManager = context.getSystemService(OverlayManager.class);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (mOverlayManager == null) {
            setEnabled(false);
            return;
        }
        try {
            final OverlayIdentifier overlayID = getOverlayID(getKey());
            final OverlayInfo info = overlayID == null
                    ? null : mOverlayManager.getOverlayInfo(overlayID, mUserHandle);
            if (info == null) {
                Log.w(TAG, "Overlay " + getKey() + " not found, disabling preference");
                setEnabled(false);
                return;
            }
            setChecked(info.isEnabled());
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "Failed reading overlay " + getKey() + ", disabling preference", e);
            setEnabled(false);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        if (mOverlayManager == null) {
            setEnabled(false);
            return;
        }
        final OverlayIdentifier overlayID = getOverlayID(getKey());
        if (overlayID == null) {
            Log.w(TAG, "Overlay " + getKey() + " not found, disabling preference");
            setEnabled(false);
            return;
        }
        OverlayManagerTransaction.Builder transaction = new OverlayManagerTransaction.Builder();
        transaction.setEnabled(overlayID, checked, mUserId);
        if (mDisableKey != null && !mDisableKey.isEmpty()) {
            final OverlayIdentifier disableID = getOverlayID(mDisableKey);
            if (disableID == null) {
                Log.w(TAG, "Overlay " + mDisableKey + " not found, not toggling it");
            } else if (mDKeyNightOnly) {
                final boolean isNight = (getContext().getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                if (isNight)
                    transaction.setEnabled(disableID, !checked, mUserId);
                else // always enabled in day
                    transaction.setEnabled(disableID, true, mUserId);
            } else {
                transaction.setEnabled(disableID, !checked, mUserId);
            }
        }
        try {
            mOverlayManager.commit(transaction.build());
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "Failed setting overlay(s), future logs will point the reason");
            e.printStackTrace();
            return;
        }
        super.setChecked(checked);
    }

    /**
     * Resolves an overlay key to its identifier.
     *
     * @return the matching identifier, or {@code null} if the overlay does not exist
     */
    private OverlayIdentifier getOverlayID(String name) {
        if (mOverlayManager == null || name == null || name.isEmpty()) return null;
        if (name.contains(":")) {
            // specific overlay name in a package
            final String[] value = name.split(":");
            if (value.length < 2) return null;
            final String pkgName = value[0];
            final String overlayName = value[1];
            final List<OverlayInfo> infos =
                    mOverlayManager.getOverlayInfosForTarget(pkgName, mUserHandle);
            for (OverlayInfo info : infos) {
                if (overlayName.equals(info.getOverlayName()))
                    return info.getOverlayIdentifier();
            }
            return null;
        }
        // package with only one overlay
        final OverlayInfo info = mOverlayManager.getOverlayInfo(name, mUserHandle);
        return info != null ? info.getOverlayIdentifier() : null;
    }
}
