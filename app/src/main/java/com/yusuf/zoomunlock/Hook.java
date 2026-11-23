package com.yusuf.zoomunlock;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;
import android.util.Range;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {
    private static final float NEW_MAX = 30.0f;
    private static final String T = "ZoomUnlock";
    private static final Set<String> seen = new HashSet<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.android.camera".equals(lp.packageName)) return;
        Log.i(T, "loaded in " + lp.processName);
        try {
            XposedHelpers.findAndHookMethod(CameraCharacteristics.class, "get",
                CameraCharacteristics.Key.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam p) {
                        try {
                            Object res = p.getResult();
                            if (res == null) return;
                            String name = ((CameraCharacteristics.Key<?>) p.args[0]).getName();
                            if (!name.toLowerCase().contains("zoom")) return;
                            String val = (res instanceof float[]) ? Arrays.toString((float[]) res) : String.valueOf(res);
                            if (seen.add(name + val)) Log.i(T, "read " + name + " = " + val);
                            boolean rangeKey = name.equals("android.control.zoomRatioRange")
                                    || name.equals("com.xiaomi.algo.satZoomRatioRange");
                            if (rangeKey && res instanceof Range) {
                                Range<?> r = (Range<?>) res;
                                float lo = ((Number) r.getLower()).floatValue();
                                float hi = ((Number) r.getUpper()).floatValue();
                                if (hi == 10.0f) { p.setResult(new Range<Float>(lo, NEW_MAX)); Log.i(T, "patched " + name); }
                            } else if (rangeKey && res instanceof float[]) {
                                float[] a = ((float[]) res).clone();
                                if (a.length == 2 && a[1] == 10.0f) { a[1] = NEW_MAX; p.setResult(a); Log.i(T, "patched " + name); }
                            } else if ((name.equals("android.scaler.availableMaxDigitalZoom") || name.equals("xiaomi.smoothTransition.xiaomiSatMaxZoom"))
                                    && res instanceof Float && ((Float) res) == 10.0f) {
                                p.setResult(NEW_MAX); Log.i(T, "patched " + name);
                            }
                        } catch (Throwable t) { Log.e(T, "err " + t); }
                    }
                });
            Log.i(T, "hook installed");
        } catch (Throwable t) { Log.e(T, "hook failed " + t); }
    }
}
