package com.yusuf.zoomunlock;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Range;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {
    private static final float NEW_MAX = 30.0f;   // غيّر الرقم هنا اذا تريد غير 30

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.android.camera".equals(lp.packageName)) return;
        try {
            XposedHelpers.findAndHookMethod(CameraCharacteristics.class, "get",
                CameraCharacteristics.Key.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam p) {
                        try {
                            Object res = p.getResult();
                            if (res == null) return;
                            String name = ((CameraCharacteristics.Key<?>) p.args[0]).getName();
                            boolean zoomKey = name.equals("android.control.zoomRatioRange")
                                    || name.equals("com.xiaomi.algo.satZoomRatioRange");
                            if (zoomKey) {
                                if (res instanceof Range) {
                                    Range<?> r = (Range<?>) res;
                                    float lo = ((Number) r.getLower()).floatValue();
                                    float hi = ((Number) r.getUpper()).floatValue();
                                    if (hi == 10.0f) p.setResult(new Range<Float>(lo, NEW_MAX));
                                } else if (res instanceof float[]) {
                                    float[] a = ((float[]) res).clone();
                                    if (a.length == 2 && a[1] == 10.0f) { a[1] = NEW_MAX; p.setResult(a); }
                                }
                            } else if (name.equals("android.scaler.availableMaxDigitalZoom")
                                    && res instanceof Float && ((Float) res) == 10.0f) {
                                p.setResult(NEW_MAX);
                            }
                        } catch (Throwable t) { XposedBridge.log("ZoomUnlock: " + t); }
                    }
                });
            XposedBridge.log("ZoomUnlock: hooked");
        } catch (Throwable t) { XposedBridge.log("ZoomUnlock hook failed: " + t); }
    }
}
