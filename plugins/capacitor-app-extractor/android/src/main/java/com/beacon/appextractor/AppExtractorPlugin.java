package com.beacon.appextractor;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@CapacitorPlugin(name = "AppExtractor")
public class AppExtractorPlugin extends Plugin {

    // Returns every installed app that has a launcher icon (i.e. a "real" app
    // the person opens), plus its APK path and size, similar to what SHAREit shows.
    @PluginMethod
    public void listApps(PluginCall call) {
        PackageManager pm = getContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        JSArray result = new JSArray();

        for (ApplicationInfo app : apps) {
            boolean isSystem = (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean hasLauncherIcon = pm.getLaunchIntentForPackage(app.packageName) != null;

            // Skip system components with no launcher entry (keyboards, services, etc.)
            if (isSystem && !hasLauncherIcon) continue;

            try {
                File apkFile = new File(app.sourceDir);
                JSObject o = new JSObject();
                o.put("packageName", app.packageName);
                o.put("appName", pm.getApplicationLabel(app).toString());
                o.put("apkPath", app.sourceDir);
                o.put("size", apkFile.length());
                result.put(o);
            } catch (Exception e) {
                // Skip anything unreadable rather than failing the whole list
            }
        }

        JSObject ret = new JSObject();
        ret.put("apps", result);
        call.resolve(ret);
    }

    // Reads the APK file for a chosen package and returns it as base64
    // so the web layer can hand it to the existing Beacon file-send code.
    @PluginMethod
    public void getApkBase64(PluginCall call) {
        String apkPath = call.getString("apkPath");
        if (apkPath == null) {
            call.reject("apkPath is required");
            return;
        }
        try (FileInputStream fis = new FileInputStream(apkPath);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }

            String base64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            JSObject ret = new JSObject();
            ret.put("base64", base64);
            call.resolve(ret);
        } catch (IOException e) {
            call.reject("Could not read APK: " + e.getMessage());
        }
    }
}
