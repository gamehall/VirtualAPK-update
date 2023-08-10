/*
 * Copyright (C) 2017 Beijing Didi Infinity Technology and Development Co.,Ltd. All rights reserved.
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

package com.didi.virtualapk.internal;

import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.internal.utils.DexUtil;
import com.didi.virtualapk.internal.utils.PackageParserCompat;
import com.didi.virtualapk.internal.utils.PluginUtil;
import com.didi.virtualapk.utils.Reflector;
import com.didi.virtualapk.utils.RunUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Created by renyugang on 16/8/9.
 */
public class LoadedPlugin {

    public static final String TAG = Constants.TAG_PREFIX + "LoadedPlugin";

    protected File getDir(Context context, String name) {
        return context.getDir(name, Context.MODE_PRIVATE);
    }

    protected ClassLoader createClassLoader(Context context, File apk, File libsDir, ClassLoader parent) throws Exception {
        File dexOutputDir = getDir(context, Constants.OPTIMIZE_DIR);
        String dexOutputPath = dexOutputDir.getAbsolutePath();
        DexClassLoader loader = new DexClassLoader(apk.getAbsolutePath(), dexOutputPath, libsDir.getAbsolutePath(), parent);
        
        if (Constants.COMBINE_CLASSLOADER) {
            DexUtil.insertDex(loader, parent, libsDir);
        }

        return loader;
    }

    protected AssetManager createAssetManager(Context context, File apk) throws Exception {
        AssetManager am = AssetManager.class.newInstance();
        Reflector.with(am).method("addAssetPath", String.class).call(apk.getAbsolutePath());
        return am;
    }

    protected Resources createResources(Context context, String packageName, File apk) throws Exception {
        if (Constants.COMBINE_RESOURCES) {
            return ResourcesManager.createResources(context, packageName, apk);
        } else {
            Resources hostResources = context.getResources();
            AssetManager assetManager = createAssetManager(context, apk);
            return new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
        }
    }

    public PluginContext createPluginContext(Context context) {
        if (context == null) {
            return new PluginContext(this);
        }

        return new PluginContext(this, context);
    }

    protected ResolveInfo chooseBestActivity(Intent intent, String s, int flags, List<ResolveInfo> query) {
        return query.get(0);
    }

    protected final String mLocation;
    protected PluginManager mPluginManager;
    protected Context mHostContext;
    protected Context mPluginContext;
    protected final File mNativeLibDir;
    protected final PackageParser.Package mPackage;
    protected final PackageInfo mPackageInfo;
    protected Resources mResources;
    protected ClassLoader mClassLoader;

    protected Map<ComponentName, ActivityInfo> mActivityInfos;
    protected Map<ComponentName, ServiceInfo> mServiceInfos;
    protected Map<ComponentName, ActivityInfo> mReceiverInfos;
    protected Map<ComponentName, ProviderInfo> mProviderInfos;
    protected Map<String, ProviderInfo> mProviders; // key is authorities of provider
    protected Map<ComponentName, InstrumentationInfo> mInstrumentationInfos;

    protected Application mApplication;

    public LoadedPlugin(PluginManager pluginManager, Context context, File apk) throws Exception {
        this.mPluginManager = pluginManager;
        this.mHostContext = context;
        this.mLocation = apk.getAbsolutePath();
        this.mPackage = PackageParserCompat.parsePackage(context, apk, PackageParser.PARSE_MUST_BE_APK);
        this.mPackage.applicationInfo.metaData = this.mPackage.mAppMetaData;
        this.mPackageInfo = new PackageInfo();
        this.mPackageInfo.applicationInfo = this.mPackage.applicationInfo;
        this.mPackageInfo.applicationInfo.sourceDir = apk.getAbsolutePath();

        if (Build.VERSION.SDK_INT >= 28
                || (Build.VERSION.SDK_INT == 27 && Build.VERSION.PREVIEW_SDK_INT != 0)) { // Android P Preview
            try {
                this.mPackageInfo.signatures = this.mPackage.mSigningDetails.signatures;
            } catch (Throwable e) {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
                this.mPackageInfo.signatures = info.signatures;
            }
        } else {
            this.mPackageInfo.signatures = this.mPackage.mSignatures;
        }

        this.mPackageInfo.packageName = this.mPackage.packageName;
        if (pluginManager.getLoadedPlugin(mPackageInfo.packageName) != null) {
            throw new RuntimeException("plugin has already been loaded : " + mPackageInfo.packageName);
        }
        this.mPackageInfo.versionCode = this.mPackage.mVersionCode;
        this.mPackageInfo.versionName = this.mPackage.mVersionName;
        this.mPackageInfo.permissions = new PermissionInfo[0];
        this.mPluginContext = createPluginContext(null);
        this.mNativeLibDir = getDir(context, Constants.NATIVE_DIR);
        this.mPackage.applicationInfo.nativeLibraryDir = this.mNativeLibDir.getAbsolutePath();
        this.mResources = createResources(context, getPackageName(), apk);
        this.mClassLoader = createClassLoader(context, apk, this.mNativeLibDir, context.getClassLoader());

        tryToCopyNativeLib(apk);

        // Cache instrumentations
        Map<ComponentName, InstrumentationInfo> instrumentations = new HashMap<ComponentName, InstrumentationInfo>();
        for (PackageParser.Instrumentation instrumentation : this.mPackage.instrumentation) {
            instrumentations.put(instrumentation.getComponentName(), instrumentation.info);
        }
        this.mInstrumentationInfos = Collections.unmodifiableMap(instrumentations);
        this.mPackageInfo.instrumentation = instrumentations.values().toArray(new InstrumentationInfo[instrumentations.size()]);

        // Cache activities
        Map<ComponentName, ActivityInfo> activityInfos = new HashMap<ComponentName, ActivityInfo>();
        for (PackageParser.Activity activity : this.mPackage.activities) {
            activity.info.metaData = activity.metaData;
            activityInfos.put(activity.getComponentName(), activity.info);
        }
        this.mActivityInfos = Collections.unmodifiableMap(activityInfos);
        this.mPackageInfo.activities = activityInfos.values().toArray(new ActivityInfo[activityInfos.size()]);

        // Cache services
        Map<ComponentName, ServiceInfo> serviceInfos = new HashMap<ComponentName, ServiceInfo>();
        for (PackageParser.Service service : this.mPackage.services) {
            serviceInfos.put(service.getComponentName(), service.info);
        }
        this.mServiceInfos = Collections.unmodifiableMap(serviceInfos);
        this.mPackageInfo.services = serviceInfos.values().toArray(new ServiceInfo[serviceInfos.size()]);

        // Cache providers
        Map<String, ProviderInfo> providers = new HashMap<String, ProviderInfo>();
        Map<ComponentName, ProviderInfo> providerInfos = new HashMap<ComponentName, ProviderInfo>();
        for (PackageParser.Provider provider : this.mPackage.providers) {
            providers.put(provider.info.authority, provider.info);
            providerInfos.put(provider.getComponentName(), provider.info);
        }
        this.mProviders = Collections.unmodifiableMap(providers);
        this.mProviderInfos = Collections.unmodifiableMap(providerInfos);
        this.mPackageInfo.providers = providerInfos.values().toArray(new ProviderInfo[providerInfos.size()]);

        // Register broadcast receivers dynamically
        Map<ComponentName, ActivityInfo> receivers = new HashMap<ComponentName, ActivityInfo>();
        for (PackageParser.Activity receiver : this.mPackage.receivers) {
            receivers.put(receiver.getComponentName(), receiver.info);

            BroadcastReceiver br = BroadcastReceiver.class.cast(getClassLoader().loadClass(receiver.getComponentName().getClassName()).newInstance());
            for (PackageParser.ActivityIntentInfo aii : receiver.intents) {
                this.mHostContext.registerReceiver(br, aii);
            }
        }
        this.mReceiverInfos = Collections.unmodifiableMap(receivers);
        this.mPackageInfo.receivers = receivers.values().toArray(new ActivityInfo[receivers.size()]);
        // try to invoke plugin's application
        invokeApplication();
    }

    protected void tryToCopyNativeLib(File apk) throws Exception {
        PluginUtil.copyNativeLib(apk, mHostContext, mPackageInfo, mNativeLibDir);
    }

    public String getLocation() {
        return this.mLocation;
    }

    public String getPackageName() {
        return this.mPackage.packageName;
    }

    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    public Resources getResources() {
        return this.mResources;
    }

    public void updateResources(Resources newResources) {
        this.mResources = newResources;
    }

    public ClassLoader getClassLoader() {
        return this.mClassLoader;
    }

    public PluginManager getPluginManager() {
        return this.mPluginManager;
    }

    public Context getHostContext() {
        return this.mHostContext;
    }

    public Context getPluginContext() {
        return this.mPluginContext;
    }

    public Application getApplication() {
        return mApplication;
    }

    public void invokeApplication() throws Exception {
        Log.d(TAG, "invokeApplication() called");
        final Exception[] temp = new Exception[1];
        // make sure application's callback is run on ui thread.
        RunUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mApplication != null) {
                    return;
                }
                try {
                    mApplication = makeApplication(false, mPluginManager.getInstrumentation());
                } catch (Exception e) {
                    temp[0] = e;
                }
            }
        }, true);

        if (temp[0] != null) {
            throw temp[0];
        }
    }

    public String getPackageResourcePath() {
        int myUid = Process.myUid();
        ApplicationInfo appInfo = this.mPackage.applicationInfo;
        return appInfo.uid == myUid ? appInfo.sourceDir : appInfo.publicSourceDir;
    }

    public String getCodePath() {
        return this.mPackage.applicationInfo.sourceDir;
    }

    public Intent getLaunchIntent() {
        ContentResolver resolver = this.mPluginContext.getContentResolver();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        for (PackageParser.Activity activity : this.mPackage.activities) {
            for (PackageParser.ActivityIntentInfo intentInfo : activity.intents) {
                if (intentInfo.match(resolver, launcher, false, TAG) > 0) {
                    return Intent.makeMainActivity(activity.getComponentName());
                }
            }
        }

        return null;
    }

    public Intent getLeanbackLaunchIntent() {
        ContentResolver resolver = this.mPluginContext.getContentResolver();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

        for (PackageParser.Activity activity : this.mPackage.activities) {
            for (PackageParser.ActivityIntentInfo intentInfo : activity.intents) {
                if (intentInfo.match(resolver, launcher, false, TAG) > 0) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(activity.getComponentName());
                    intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                    return intent;
                }
            }
        }

        return null;
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mPackage.applicationInfo;
    }

    public PackageInfo getPackageInfo() {
        return this.mPackageInfo;
    }

    public ActivityInfo getActivityInfo(ComponentName componentName) {
        return this.mActivityInfos.get(componentName);
    }

    public ServiceInfo getServiceInfo(ComponentName componentName) {
        return this.mServiceInfos.get(componentName);
    }

    public ActivityInfo getReceiverInfo(ComponentName componentName) {
        return this.mReceiverInfos.get(componentName);
    }

    public ProviderInfo getProviderInfo(ComponentName componentName) {
        return this.mProviderInfos.get(componentName);
    }

    public Resources.Theme getTheme() {
        Resources.Theme theme = this.mResources.newTheme();
        theme.applyStyle(PluginUtil.selectDefaultTheme(this.mPackage.applicationInfo.theme, Build.VERSION.SDK_INT), false);
        return theme;
    }

    public void setTheme(int resid) {
        Reflector.QuietReflector.with(this.mResources).field("mThemeResId").set(resid);
    }

    protected Application makeApplication(boolean forceDefaultAppClass, Instrumentation instrumentation) throws Exception {
        if (null != this.mApplication) {
            return this.mApplication;
        }

        String appClass = this.mPackage.applicationInfo.className;
        if (forceDefaultAppClass || null == appClass) {
            appClass = "android.app.Application";
        }

        this.mApplication = instrumentation.newApplication(this.mClassLoader, appClass, this.getPluginContext());
        // inject activityLifecycleCallbacks of the host application
        mApplication.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksProxy());
        instrumentation.callApplicationOnCreate(this.mApplication);
        return this.mApplication;
    }

    public ResolveInfo resolveActivity(Intent intent, int flags) {
        List<ResolveInfo> query = this.queryIntentActivities(intent, flags);
        if (null == query || query.isEmpty()) {
            return null;
        }

        ContentResolver resolver = this.mPluginContext.getContentResolver();
        return chooseBestActivity(intent, intent.resolveTypeIfNeeded(resolver), flags, query);
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        ContentResolver resolver = this.mPluginContext.getContentResolver();

        for (PackageParser.Activity activity : this.mPackage.activities) {
            if (match(activity, component)) {
                ResolveInfo resolveInfo = new ResolveInfo();
                resolveInfo.activityInfo = activity.info;
                resolveInfos.add(resolveInfo);
            } else if (component == null) {
                // only match implicit intent
                for (PackageParser.ActivityIntentInfo intentInfo : activity.intents) {
                    if (intentInfo.match(resolver, intent, true, TAG) >= 0) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.activityInfo = activity.info;
                        resolveInfos.add(resolveInfo);
                        break;
                    }
                }
            }
        }

        return resolveInfos;
    }

    public ResolveInfo resolveService(Intent intent, int flags) {
        List<ResolveInfo> query = this.queryIntentServices(intent, flags);
        if (null == query || query.isEmpty()) {
            return null;
        }

        ContentResolver resolver = this.mPluginContext.getContentResolver();
        return chooseBestActivity(intent, intent.resolveTypeIfNeeded(resolver), flags, query);
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        ContentResolver resolver = this.mPluginContext.getContentResolver();

        for (PackageParser.Service service : this.mPackage.services) {
            if (match(service, component)) {
                ResolveInfo resolveInfo = new ResolveInfo();
                resolveInfo.serviceInfo = service.info;
                resolveInfos.add(resolveInfo);
            } else if (component == null) {
                // only match implicit intent
                for (PackageParser.ServiceIntentInfo intentInfo : service.intents) {
                    if (intentInfo.match(resolver, intent, true, TAG) >= 0) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.serviceInfo = service.info;
                        resolveInfos.add(resolveInfo);
                        break;
                    }
                }
            }
        }

        return resolveInfos;
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        ContentResolver resolver = this.mPluginContext.getContentResolver();

        for (PackageParser.Activity receiver : this.mPackage.receivers) {
            if (receiver.getComponentName().equals(component)) {
                ResolveInfo resolveInfo = new ResolveInfo();
                resolveInfo.activityInfo = receiver.info;
                resolveInfos.add(resolveInfo);
            } else if (component == null) {
                // only match implicit intent
                for (PackageParser.ActivityIntentInfo intentInfo : receiver.intents) {
                    if (intentInfo.match(resolver, intent, true, TAG) >= 0) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.activityInfo = receiver.info;
                        resolveInfos.add(resolveInfo);
                        break;
                    }
                }
            }
        }

        return resolveInfos;
    }

    public ProviderInfo resolveContentProvider(String name, int flags) {
        return this.mProviders.get(name);
    }

    protected boolean match(PackageParser.Component component, ComponentName target) {
        ComponentName source = component.getComponentName();
        if (source == target) return true;
        if (source != null && target != null
                && source.getClassName().equals(target.getClassName())
                && (source.getPackageName().equals(target.getPackageName())
                || mHostContext.getPackageName().equals(target.getPackageName()))) {
            return true;
        }
        return false;
    }


}
