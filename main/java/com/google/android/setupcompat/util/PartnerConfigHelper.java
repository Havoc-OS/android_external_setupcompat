/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.setupcompat.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.util.Log;
import com.google.android.setupcompat.util.PartnerConfig.ResourceType;
import java.util.EnumMap;

/** The helper reads and caches the partner configurations from SUW. */
public class PartnerConfigHelper {

  private static final String TAG = PartnerConfigHelper.class.getSimpleName();

  @VisibleForTesting
  public static final String SUW_AUTHORITY = "com.google.android.setupwizard.partner";

  @VisibleForTesting public static final String SUW_GET_PARTNER_CONFIG_METHOD = "getOverlayConfig";
  private static PartnerConfigHelper instance = null;

  @VisibleForTesting Bundle resultBundle = null;

  @VisibleForTesting
  final EnumMap<PartnerConfig, Object> partnerResourceCache = new EnumMap<>(PartnerConfig.class);

  public static synchronized PartnerConfigHelper get(@NonNull Context context) {
    if (instance == null) {
      instance = new PartnerConfigHelper(context);
    }
    return instance;
  }

  private PartnerConfigHelper(Context context) {
    getPartnerConfigBundle(context);
  }

  /**
   * Returns the color of given {@code resourceConfig}, or 0 if the given {@code resourceConfig} is
   * not found. If the {@code ResourceType} of the given {@code resourceConfig} is not color,
   * IllegalArgumentException will be thrown.
   *
   * @param context The context of client activity
   * @param resourceConfig The {@code PartnerConfig} of target resource
   */
  @ColorInt
  public int getColor(@NonNull Context context, PartnerConfig resourceConfig) {
    if (resourceConfig.getResourceType() != ResourceType.COLOR) {
      throw new IllegalArgumentException("Not a color resource");
    }

    if (partnerResourceCache.containsKey(resourceConfig)) {
      return (int) partnerResourceCache.get(resourceConfig);
    }

    int result = 0;
    try {
      String resourceName = resourceConfig.getResourceName();
      ResourceEntry resourceEntry = getResourceEntryFromKey(resourceName);
      Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());
      if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
        result = resource.getColor(resourceEntry.getResourceId(), null);
      } else {
        result = resource.getColor(resourceEntry.getResourceId());
      }
      partnerResourceCache.put(resourceConfig, result);
    } catch (PackageManager.NameNotFoundException | NullPointerException exception) {
      // fall through
    }
    return result;
  }

  /**
   * Returns the {@code Drawable} of given {@code resourceConfig}, or {@code null} if the given
   * {@code resourceConfig} is not found. If the {@code ResourceType} of the given {@code
   * resourceConfig} is not drawable, IllegalArgumentException will be thrown.
   *
   * @param context The context of client activity
   * @param resourceConfig The {@code PartnerConfig} of target resource
   */
  @Nullable
  public Drawable getDrawable(@NonNull Context context, PartnerConfig resourceConfig) {
    if (resourceConfig.getResourceType() != ResourceType.DRAWABLE) {
      throw new IllegalArgumentException("Not a drawable resource");
    }

    if (partnerResourceCache.containsKey(resourceConfig)) {
      return (Drawable) partnerResourceCache.get(resourceConfig);
    }

    Drawable result = null;
    try {
      ResourceEntry resourceEntry = getResourceEntryFromKey(resourceConfig.getResourceName());
      Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());
      if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
        result = resource.getDrawable(resourceEntry.getResourceId(), null);
      } else {
        result = resource.getDrawable(resourceEntry.getResourceId());
      }
      partnerResourceCache.put(resourceConfig, result);
    } catch (PackageManager.NameNotFoundException
        | NullPointerException
        | NotFoundException exception) {
      // fall through
    }
    return result;
  }

  /**
   * Returns the string of the given {@code resourceConfig}, or {@code null} if the given {@code
   * resourceConfig} is not found. If the {@code ResourceType} of the given {@code resourceConfig}
   * is not string, IllegalArgumentException will be thrown.
   *
   * @param context The context of client activity
   * @param resourceConfig The {@code PartnerConfig} of target resource
   */
  @Nullable
  public String getString(@NonNull Context context, PartnerConfig resourceConfig) {
    if (resourceConfig.getResourceType() != ResourceType.STRING) {
      throw new IllegalArgumentException("Not a string resource");
    }

    if (partnerResourceCache.containsKey(resourceConfig)) {
      return (String) partnerResourceCache.get(resourceConfig);
    }

    String result = null;
    try {
      ResourceEntry resourceEntry = getResourceEntryFromKey(resourceConfig.getResourceName());
      Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());
      result = resource.getString(resourceEntry.getResourceId());
      partnerResourceCache.put(resourceConfig, result);
    } catch (PackageManager.NameNotFoundException | NullPointerException exception) {
      // fall through
    }
    return result;
  }

  /**
   * Returns the boolean of given {@code resourceConfig}, or {@code defaultValue} if the given
   * {@code resourceName} is not found. If the {@code ResourceType} of the given {@code
   * resourceConfig} is not boolean, IllegalArgumentException will be thrown.
   *
   * @param context The context of client activity
   * @param resourceConfig The {@code PartnerConfig} of target resource
   * @param defaultValue The default value
   */
  public boolean getBoolean(
      @NonNull Context context, PartnerConfig resourceConfig, boolean defaultValue) {
    if (resourceConfig.getResourceType() != ResourceType.BOOL) {
      throw new IllegalArgumentException("Not a bool resource");
    }

    if (partnerResourceCache.containsKey(resourceConfig)) {
      return (boolean) partnerResourceCache.get(resourceConfig);
    }

    boolean result = defaultValue;
    try {
      ResourceEntry resourceEntry = getResourceEntryFromKey(resourceConfig.getResourceName());
      Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());
      result = resource.getBoolean(resourceEntry.getResourceId());
      partnerResourceCache.put(resourceConfig, result);
    } catch (PackageManager.NameNotFoundException | NullPointerException exception) {
      // fall through
    }
    return result;
  }

  /**
   * Returns the dimension of given {@code resourceConfig}. The default return value is 0.
   *
   * @param context The context of client activity
   * @param resourceConfig The {@code PartnerConfig} of target resource
   */
  public float getDimension(@NonNull Context context, PartnerConfig resourceConfig) {
    return getDimension(context, resourceConfig, 0);
  }

  /**
   * Returns the dimension of given {@code resourceConfig}. If the given {@code resourceConfig} not
   * found, will return {@code defaultValue}. If the {@code ResourceType} of given {@code
   * resourceConfig} is not dimension, will throw IllegalArgumentException.
   *
   * @param context The context of client activity
   * @param resourceConfig The {@code PartnerConfig} of target resource
   * @param defaultValue The default value
   */
  public float getDimension(
      @NonNull Context context, PartnerConfig resourceConfig, float defaultValue) {
    if (resourceConfig.getResourceType() != ResourceType.DIMENSION) {
      throw new IllegalArgumentException("Not a dimension resource");
    }

    if (partnerResourceCache.containsKey(resourceConfig)) {
      return (float) partnerResourceCache.get(resourceConfig);
    }

    float result = defaultValue;
    try {
      ResourceEntry resourceEntry = getResourceEntryFromKey(resourceConfig.getResourceName());
      Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());
      result = resource.getDimension(resourceEntry.getResourceId());
      partnerResourceCache.put(resourceConfig, result);
    } catch (PackageManager.NameNotFoundException | NullPointerException exception) {
      // fall through
    }
    return result;
  }

  private void getPartnerConfigBundle(Context context) {
    if (resultBundle == null || resultBundle.isEmpty()) {
      try {
        Uri contentUri =
            new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SUW_AUTHORITY)
                .appendPath(SUW_GET_PARTNER_CONFIG_METHOD)
                .build();
        resultBundle =
            context
                .getContentResolver()
                .call(
                    contentUri, SUW_GET_PARTNER_CONFIG_METHOD, /* arg= */ null, /* extras= */ null);
        partnerResourceCache.clear();
      } catch (IllegalArgumentException exception) {
        Log.w(TAG, "Fail to get config from suw provider");
      }
    }
  }

  private Resources getResourcesByPackageName(Context context, String packageName)
      throws PackageManager.NameNotFoundException {
    PackageManager manager = context.getPackageManager();
    return manager.getResourcesForApplication(packageName);
  }

  private ResourceEntry getResourceEntryFromKey(String resourceName) {
    if (resultBundle == null) {
      return null;
    }
    return ResourceEntry.fromBundle(resultBundle.getBundle(resourceName));
  }

  @VisibleForTesting
  public static synchronized void resetForTesting() {
    instance = null;
  }
}
