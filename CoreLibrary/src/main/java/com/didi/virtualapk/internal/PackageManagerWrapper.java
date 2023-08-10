package com.didi.virtualapk.internal;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.utils.Reflector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PackageManagerWrapper {

    public static PackageInfo getPackageInfo(String packageName, int flags) throws PackageManager.NameNotFoundException {

        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.mPackageInfo;
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getPackageInfo(packageName, flags);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static PackageInfo getPackageInfo(VersionedPackage versionedPackage, int i) throws PackageManager.NameNotFoundException {

        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(versionedPackage.getPackageName());
        if (null != plugin) {
            return plugin.mPackageInfo;
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getPackageInfo(versionedPackage, i);
    }


    public static String[] currentToCanonicalPackageNames(String[] names) {
        return PluginManager.getInstance().getHostContext().getPackageManager().currentToCanonicalPackageNames(names);
    }


    public static String[] canonicalToCurrentPackageNames(String[] names) {
        return PluginManager.getInstance().getHostContext().getPackageManager().canonicalToCurrentPackageNames(names);
    }


    public static Intent getLaunchIntentForPackage(@NonNull String packageName) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.getLaunchIntent();
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getLaunchIntentForPackage(packageName);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)

    public static Intent getLeanbackLaunchIntentForPackage(@NonNull String packageName) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.getLeanbackLaunchIntent();
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getLeanbackLaunchIntentForPackage(packageName);
    }


    public static int[] getPackageGids(@NonNull String packageName) throws PackageManager.NameNotFoundException {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPackageGids(packageName);
    }

    @TargetApi(Build.VERSION_CODES.N)

    public static int[] getPackageGids(String packageName, int flags) throws PackageManager.NameNotFoundException {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPackageGids(packageName, flags);
    }

    @TargetApi(Build.VERSION_CODES.N)

    public static int getPackageUid(String packageName, int flags) throws PackageManager.NameNotFoundException {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPackageUid(packageName, flags);
    }


    public static PermissionInfo getPermissionInfo(String name, int flags) throws PackageManager.NameNotFoundException {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPermissionInfo(name, flags);
    }


    public static List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws PackageManager.NameNotFoundException {
        return PluginManager.getInstance().getHostContext().getPackageManager().queryPermissionsByGroup(group, flags);
    }


    public static PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws PackageManager.NameNotFoundException {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPermissionGroupInfo(name, flags);
    }


    public static List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getAllPermissionGroups(flags);
    }


    public static ApplicationInfo getApplicationInfo(String packageName, int flags) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.getApplicationInfo();
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getApplicationInfo(packageName, flags);
    }


    public static ActivityInfo getActivityInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
        if (null != plugin) {
            return plugin.mActivityInfos.get(component);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getActivityInfo(component, flags);
    }


    public static ActivityInfo getReceiverInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
        if (null != plugin) {
            return plugin.mReceiverInfos.get(component);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getReceiverInfo(component, flags);
    }


    public static ServiceInfo getServiceInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
        if (null != plugin) {
            return plugin.mServiceInfos.get(component);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getServiceInfo(component, flags);
    }


    public static ProviderInfo getProviderInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
        if (null != plugin) {
            return plugin.mProviderInfos.get(component);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getProviderInfo(component, flags);
    }


    public static List<PackageInfo> getInstalledPackages(int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getInstalledPackages(flags);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)

    public static List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPackagesHoldingPermissions(permissions, flags);
    }


    public static int checkPermission(String permName, String pkgName) {
        return PluginManager.getInstance().getHostContext().getPackageManager().checkPermission(permName, pkgName);
    }

    @TargetApi(Build.VERSION_CODES.M)

    public static boolean isPermissionRevokedByPolicy(@NonNull String permName, @NonNull String pkgName) {
        return PluginManager.getInstance().getHostContext().getPackageManager().isPermissionRevokedByPolicy(permName, pkgName);
    }


    public static boolean addPermission(PermissionInfo info) {
        return PluginManager.getInstance().getHostContext().getPackageManager().addPermission(info);
    }


    public static boolean addPermissionAsync(PermissionInfo info) {
        return PluginManager.getInstance().getHostContext().getPackageManager().addPermissionAsync(info);
    }


    public static void removePermission(String name) {
        PluginManager.getInstance().getHostContext().getPackageManager().removePermission(name);
    }


    public static int checkSignatures(String pkg1, String pkg2) {
        return PluginManager.getInstance().getHostContext().getPackageManager().checkSignatures(pkg1, pkg2);
    }


    public static int checkSignatures(int uid1, int uid2) {
        return PluginManager.getInstance().getHostContext().getPackageManager().checkSignatures(uid1, uid2);
    }


    public static String[] getPackagesForUid(int uid) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPackagesForUid(uid);
    }


    public static String getNameForUid(int uid) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getNameForUid(uid);
    }


    public static List<ApplicationInfo> getInstalledApplications(int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getInstalledApplications(flags);
    }

    @TargetApi(Build.VERSION_CODES.O)

    public static boolean isInstantApp() {
        return PluginManager.getInstance().getHostContext().getPackageManager().isInstantApp();
    }

    @TargetApi(Build.VERSION_CODES.O)

    public static boolean isInstantApp(String packageName) {
        return PluginManager.getInstance().getHostContext().getPackageManager().isInstantApp(packageName);
    }

    @TargetApi(Build.VERSION_CODES.O)

    public static int getInstantAppCookieMaxBytes() {
        return PluginManager.getInstance().getHostContext().getPackageManager().getInstantAppCookieMaxBytes();
    }

    @TargetApi(Build.VERSION_CODES.O)
    @NonNull

    public static byte[] getInstantAppCookie() {
        return PluginManager.getInstance().getHostContext().getPackageManager().getInstantAppCookie();
    }

    @TargetApi(Build.VERSION_CODES.O)

    public static void clearInstantAppCookie() {
        PluginManager.getInstance().getHostContext().getPackageManager().clearInstantAppCookie();
    }

    @TargetApi(Build.VERSION_CODES.O)

    public static void updateInstantAppCookie(@Nullable byte[] cookie) {
        PluginManager.getInstance().getHostContext().getPackageManager().updateInstantAppCookie(cookie);
    }


    public static String[] getSystemSharedLibraryNames() {
        return PluginManager.getInstance().getHostContext().getPackageManager().getSystemSharedLibraryNames();
    }

    @TargetApi(Build.VERSION_CODES.O)
    @NonNull

    public static List<SharedLibraryInfo> getSharedLibraries(int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getSharedLibraries(flags);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Nullable

    public static ChangedPackages getChangedPackages(int sequenceNumber) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getChangedPackages(sequenceNumber);
    }


    public static FeatureInfo[] getSystemAvailableFeatures() {
        return PluginManager.getInstance().getHostContext().getPackageManager().getSystemAvailableFeatures();
    }


    public static boolean hasSystemFeature(String name) {
        return PluginManager.getInstance().getHostContext().getPackageManager().hasSystemFeature(name);
    }

    @TargetApi(Build.VERSION_CODES.N)

    public static boolean hasSystemFeature(String name, int version) {
        return PluginManager.getInstance().getHostContext().getPackageManager().hasSystemFeature(name, version);
    }


    public static ResolveInfo resolveActivity(Intent intent, int flags) {
        ResolveInfo resolveInfo = PluginManager.getInstance().resolveActivity(intent, flags);
        if (null != resolveInfo) {
            return resolveInfo;
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().resolveActivity(intent, flags);
    }


    public static List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        if (null == component) {
            if (intent.getSelector() != null) {
                intent = intent.getSelector();
                component = intent.getComponent();
            }
        }

        if (null != component) {
            LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
            if (null != plugin) {
                ActivityInfo activityInfo = plugin.getActivityInfo(component);
                if (activityInfo != null) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = activityInfo;
                    return Arrays.asList(resolveInfo);
                }
            }
        }

        List<ResolveInfo> all = new ArrayList<ResolveInfo>();

        List<ResolveInfo> pluginResolveInfos = PluginManager.getInstance().queryIntentActivities(intent, flags);
        if (null != pluginResolveInfos && pluginResolveInfos.size() > 0) {
            all.addAll(pluginResolveInfos);
        }

        List<ResolveInfo> hostResolveInfos = PluginManager.getInstance().getHostContext().getPackageManager().queryIntentActivities(intent, flags);
        if (null != hostResolveInfos && hostResolveInfos.size() > 0) {
            all.addAll(hostResolveInfos);
        }

        return all;
    }


    public static List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().queryIntentActivityOptions(caller, specifics, intent, flags);
    }


    public static List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        if (null == component) {
            if (intent.getSelector() != null) {
                intent = intent.getSelector();
                component = intent.getComponent();
            }
        }

        if (null != component) {
            LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
            if (null != plugin) {
                ActivityInfo activityInfo = plugin.getReceiverInfo(component);
                if (activityInfo != null) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = activityInfo;
                    return Arrays.asList(resolveInfo);
                }
            }
        }

        List<ResolveInfo> all = new ArrayList<>();

        List<ResolveInfo> pluginResolveInfos = PluginManager.getInstance().queryBroadcastReceivers(intent, flags);
        if (null != pluginResolveInfos && pluginResolveInfos.size() > 0) {
            all.addAll(pluginResolveInfos);
        }

        List<ResolveInfo> hostResolveInfos = PluginManager.getInstance().getHostContext().getPackageManager().queryBroadcastReceivers(intent, flags);
        if (null != hostResolveInfos && hostResolveInfos.size() > 0) {
            all.addAll(hostResolveInfos);
        }

        return all;
    }


    public static ResolveInfo resolveService(Intent intent, int flags) {
        ResolveInfo resolveInfo = PluginManager.getInstance().resolveService(intent, flags);
        if (null != resolveInfo) {
            return resolveInfo;
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().resolveService(intent, flags);
    }


    public static List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        if (null == component) {
            if (intent.getSelector() != null) {
                intent = intent.getSelector();
                component = intent.getComponent();
            }
        }

        if (null != component) {
            LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
            if (null != plugin) {
                ServiceInfo serviceInfo = plugin.getServiceInfo(component);
                if (serviceInfo != null) {
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.serviceInfo = serviceInfo;
                    return Arrays.asList(resolveInfo);
                }
            }
        }

        List<ResolveInfo> all = new ArrayList<ResolveInfo>();

        List<ResolveInfo> pluginResolveInfos = PluginManager.getInstance().queryIntentServices(intent, flags);
        if (null != pluginResolveInfos && pluginResolveInfos.size() > 0) {
            all.addAll(pluginResolveInfos);
        }

        List<ResolveInfo> hostResolveInfos = PluginManager.getInstance().getHostContext().getPackageManager().queryIntentServices(intent, flags);
        if (null != hostResolveInfos && hostResolveInfos.size() > 0) {
            all.addAll(hostResolveInfos);
        }

        return all;
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().queryIntentContentProviders(intent, flags);
    }


    public static ProviderInfo resolveContentProvider(String name, int flags) {
        ProviderInfo providerInfo = PluginManager.getInstance().resolveContentProvider(name, flags);
        if (null != providerInfo) {
            return providerInfo;
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().resolveContentProvider(name, flags);
    }


    public static List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().queryContentProviders(processName, uid, flags);
    }


    public static InstrumentationInfo getInstrumentationInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
        if (null != plugin) {
            return plugin.mInstrumentationInfos.get(component);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getInstrumentationInfo(component, flags);
    }


    public static List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().queryInstrumentation(targetPackage, flags);
    }


    public static Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.mResources.getDrawable(resid);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getDrawable(packageName, resid, appInfo);
    }


    public static Drawable getActivityIcon(ComponentName component) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
        if (null != plugin) {
            return plugin.mResources.getDrawable(plugin.mActivityInfos.get(component).icon);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getActivityIcon(component);
    }


    public static Drawable getActivityIcon(Intent intent) throws PackageManager.NameNotFoundException {
        ResolveInfo ri = PluginManager.getInstance().resolveActivity(intent);
        if (null != ri) {
            LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(ri.resolvePackageName);
            return plugin.mResources.getDrawable(ri.activityInfo.icon);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getActivityIcon(intent);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)

    public static Drawable getActivityBanner(ComponentName component) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
        if (null != plugin) {
            return plugin.mResources.getDrawable(plugin.mActivityInfos.get(component).banner);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getActivityBanner(component);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)

    public static Drawable getActivityBanner(Intent intent) throws PackageManager.NameNotFoundException {
        ResolveInfo ri = PluginManager.getInstance().resolveActivity(intent);
        if (null != ri) {
            LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(ri.resolvePackageName);
            return plugin.mResources.getDrawable(ri.activityInfo.banner);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getActivityBanner(intent);
    }


    public static Drawable getDefaultActivityIcon() {
        return PluginManager.getInstance().getHostContext().getPackageManager().getDefaultActivityIcon();
    }


    public static Drawable getApplicationIcon(ApplicationInfo info) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(info.packageName);
        if (null != plugin) {
            return plugin.mResources.getDrawable(info.icon);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getApplicationIcon(info);
    }


    public static Drawable getApplicationIcon(String packageName) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.mResources.getDrawable(plugin.mPackage.applicationInfo.icon);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getApplicationIcon(packageName);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)

    public static Drawable getApplicationBanner(ApplicationInfo info) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(info.packageName);
        if (null != plugin) {
            return plugin.mResources.getDrawable(info.banner);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getApplicationBanner(info);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)

    public static Drawable getApplicationBanner(String packageName) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.mResources.getDrawable(plugin.mPackage.applicationInfo.banner);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getApplicationBanner(packageName);
    }


    public static Drawable getActivityLogo(ComponentName component) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
        if (null != plugin) {
            return plugin.mResources.getDrawable(plugin.mActivityInfos.get(component).logo);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getActivityLogo(component);
    }


    public static Drawable getActivityLogo(Intent intent) throws PackageManager.NameNotFoundException {
        ResolveInfo ri = PluginManager.getInstance().resolveActivity(intent);
        if (null != ri) {
            LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(ri.resolvePackageName);
            return plugin.mResources.getDrawable(ri.activityInfo.logo);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getActivityLogo(intent);
    }


    public static Drawable getApplicationLogo(ApplicationInfo info) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(info.packageName);
        if (null != plugin) {
            return plugin.mResources.getDrawable(0 != info.logo ? info.logo : 0);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getApplicationLogo(info);
    }


    public static Drawable getApplicationLogo(String packageName) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.mResources.getDrawable(0 != plugin.mPackage.applicationInfo.logo ? plugin.mPackage.applicationInfo.logo : 0);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getApplicationLogo(packageName);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)

    public static Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getUserBadgedIcon(icon, user);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Drawable getUserBadgeForDensity(UserHandle user, int density) {
        try {
            return Reflector.with(PluginManager.getInstance().getHostContext().getPackageManager())
                    .method("getUserBadgeForDensity", UserHandle.class, int.class)
                    .call(user, density);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)

    public static Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)

    public static CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getUserBadgedLabel(label, user);
    }


    public static CharSequence getText(String packageName, int resid, ApplicationInfo appInfo) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.mResources.getText(resid);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getText(packageName, resid, appInfo);
    }


    public static XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return plugin.mResources.getXml(resid);
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getXml(packageName, resid, appInfo);
    }


    public static CharSequence getApplicationLabel(ApplicationInfo info) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(info.packageName);
        if (null != plugin) {
            try {
                return plugin.mResources.getText(info.labelRes);
            } catch (Resources.NotFoundException e) {
                // ignored.
            }
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getApplicationLabel(info);
    }


    public static Resources getResourcesForActivity(ComponentName component) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(component);
        if (null != plugin) {
            return plugin.mResources;
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getResourcesForActivity(component);
    }


    public static Resources getResourcesForApplication(ApplicationInfo app) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(app.packageName);
        if (null != plugin) {
            return plugin.mResources;
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getResourcesForApplication(app);
    }


    public static Resources getResourcesForApplication(String appPackageName) throws PackageManager.NameNotFoundException {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(appPackageName);
        if (null != plugin) {
            return plugin.mResources;
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getResourcesForApplication(appPackageName);
    }


    public static void verifyPendingInstall(int id, int verificationCode) {
        PluginManager.getInstance().getHostContext().getPackageManager().verifyPendingInstall(id, verificationCode);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)

    public static void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
        PluginManager.getInstance().getHostContext().getPackageManager().extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
    }


    public static void setInstallerPackageName(String targetPackage, String installerPackageName) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(targetPackage);
        if (null != plugin) {
            return;
        }

        PluginManager.getInstance().getHostContext().getPackageManager().setInstallerPackageName(targetPackage, installerPackageName);
    }


    public static String getInstallerPackageName(String packageName) {
        LoadedPlugin plugin = PluginManager.getInstance().getLoadedPlugin(packageName);
        if (null != plugin) {
            return PluginManager.getInstance().getHostContext().getPackageName();
        }

        return PluginManager.getInstance().getHostContext().getPackageManager().getInstallerPackageName(packageName);
    }


    public static void addPackageToPreferred(String packageName) {
        PluginManager.getInstance().getHostContext().getPackageManager().addPackageToPreferred(packageName);
    }


    public static void removePackageFromPreferred(String packageName) {
        PluginManager.getInstance().getHostContext().getPackageManager().removePackageFromPreferred(packageName);
    }


    public static List<PackageInfo> getPreferredPackages(int flags) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPreferredPackages(flags);
    }


    public static void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {
        PluginManager.getInstance().getHostContext().getPackageManager().addPreferredActivity(filter, match, set, activity);
    }


    public static void clearPackagePreferredActivities(String packageName) {
        PluginManager.getInstance().getHostContext().getPackageManager().clearPackagePreferredActivities(packageName);
    }


    public static int getPreferredActivities(@NonNull List<IntentFilter> outFilters, @NonNull List<ComponentName> outActivities, String packageName) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPreferredActivities(outFilters, outActivities, packageName);
    }


    public static void setComponentEnabledSetting(ComponentName component, int newState, int flags) {
        PluginManager.getInstance().getHostContext().getPackageManager().setComponentEnabledSetting(component, newState, flags);
    }


    public static int getComponentEnabledSetting(ComponentName component) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getComponentEnabledSetting(component);
    }


    public static void setApplicationEnabledSetting(String packageName, int newState, int flags) {
        PluginManager.getInstance().getHostContext().getPackageManager().setApplicationEnabledSetting(packageName, newState, flags);
    }


    public static int getApplicationEnabledSetting(String packageName) {
        return PluginManager.getInstance().getHostContext().getPackageManager().getApplicationEnabledSetting(packageName);
    }


    public static boolean isSafeMode() {
        return PluginManager.getInstance().getHostContext().getPackageManager().isSafeMode();
    }

    @TargetApi(Build.VERSION_CODES.O)

    public static void setApplicationCategoryHint(@NonNull String packageName, int categoryHint) {
        PluginManager.getInstance().getHostContext().getPackageManager().setApplicationCategoryHint(packageName, categoryHint);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)

    public static @NonNull
    PackageInstaller getPackageInstaller() {
        return PluginManager.getInstance().getHostContext().getPackageManager().getPackageInstaller();
    }

    @TargetApi(Build.VERSION_CODES.O)

    public static boolean canRequestPackageInstalls() {
        return PluginManager.getInstance().getHostContext().getPackageManager().canRequestPackageInstalls();
    }

    public static Drawable loadItemIcon(PackageItemInfo itemInfo, ApplicationInfo appInfo) {
        if (itemInfo == null) {
            return null;
        }
        return itemInfo.loadIcon(PluginManager.getInstance().getHostContext().getPackageManager());
    }
}
