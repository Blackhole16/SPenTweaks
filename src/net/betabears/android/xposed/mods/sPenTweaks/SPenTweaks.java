package net.betabears.android.xposed.mods.sPenTweaks;

import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class SPenTweaks implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	private boolean disableButtons = false;
    private long lastStylusTime;

	private int lastAction = -9999;
	private boolean pDebug;
    private boolean pEventLog;
    private boolean pDisableButtons;
    private int pHoverTimeout;


	private XSharedPreferences mPrefs ;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        // init Preferences
        mPrefs = new XSharedPreferences("net.betabears.android.xposed.mods.sPenTweaks");
        // check for World-Read Permissions (per default shared-pref files have 660 (Much shared, many wow))
        if (!mPrefs.getFile().canRead()) {
            if (!mPrefs.getFile().setReadable(true, false)) XposedBridge.log("Cannot make Preferences File readable," +
                    " Default settings will be loaded. To fix this set /data/data/net.betabears.android.xposed." +
                    "mods.sPenTweaks/net.betabears.android.xposed.mods.sPenTweaks_preferences to XX4-permissions.");
        }
    }

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // need to inject package `android` because there com.android.server is loaded
        if (lpparam.packageName.equals("android")) {
            updatePreferences(mPrefs);
            // update on Preference-Change
            new Thread() {
                @Override
                public void run() {
                    setPriority(Thread.MIN_PRIORITY);
                    while (true) {
                        if (!mPrefs.getFile().canRead()) {
                            XposedBridge.log("adapt pref: " + mPrefs.getFile().setReadable(true, false));
                        }
                        if (mPrefs.hasFileChanged()) {
                            mPrefs.reload();
                            updatePreferences(mPrefs);
                        }

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            if (pDebug) XposedBridge.log(e);
                            break;
                        }
                    }
                }
            }.start();


            if (pDebug) XposedBridge.log("Hook before com.android.server.wm.PointerEventDispatcher::onInputEvent.");
            findAndHookMethod("com.android.server.wm.PointerEventDispatcher", lpparam.classLoader, "onInputEvent",
                    InputEvent.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0] instanceof MotionEvent) {
                                MotionEvent evt = (MotionEvent) param.args[0];
                                if (pEventLog && evt.getAction() != lastAction) {
                                    lastAction = evt.getAction();
                                    XposedBridge.log(evt.toString());
                                }
                                int actionIndex = evt.getActionIndex();
                                int pointerId = evt.getPointerId(actionIndex);
                                int tool = evt.getToolType(pointerId);
                                int action = evt.getActionMasked();
                                lastStylusTime = System.currentTimeMillis();

                                // disable buttons if any stylus-action except exiting is done
                                disableButtons = tool == MotionEvent.TOOL_TYPE_STYLUS
                                        && action != MotionEvent.ACTION_HOVER_EXIT;
                            }
                        }
                    });

            if (pDebug) XposedBridge.log("Hook before android.hardware.input.InputManager::injectInputEvent.");
            // TODO / FIXME: Callback is only executed when XPosedAdditions is enabled -> find out why and fix it
            // FIXME: With System default back-button mappings long-press still goes back even if it should be rejected
            findAndHookMethod("android.hardware.input.InputManager", lpparam.classLoader, "injectInputEvent",
                    InputEvent.class, "int", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0] instanceof KeyEvent) {
                                KeyEvent evt = (KeyEvent) param.args[0];
                                if (pEventLog && evt.getAction() != lastAction) {
                                    lastAction = evt.getAction();
                                    XposedBridge.log(evt.toString());
                                }
                                if (pDisableButtons && disableButtons &&
                                        System.currentTimeMillis() - lastStylusTime < pHoverTimeout &&
                                        (evt.getKeyCode() == KeyEvent.KEYCODE_MENU
                                                || evt.getKeyCode() == KeyEvent.KEYCODE_BACK)) {
                                    param.setResult(true);
                                    if (pDebug) XposedBridge.log("  blocked " + evt.getAction());
                                }
                            }
                        }
                    });

            // called after InputManager::injectInputEvent
//		if (lpparam.packageName.equals("android")) {
//			XposedBridge.log("--------------------------- start");
//			XposedBridge.log("InputManagerService - Methods:");
//			for (Method m : findClass("com.android.server.input.InputManagerService", lpparam.classLoader).getMethods()) {
//				XposedBridge.log("    " + m);
//			}
//			XposedBridge.log("---------------------------- end");
//			findAndHookMethod("com.android.server.input.InputManagerService", lpparam.classLoader, "injectInputEvent", InputEvent.class, "int", new XC_MethodHook() {
//				@Override
//				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//					XposedBridge.log(param.args[0].getClass().toString());
//				}
//			});
//		}
        }
    }

    private void updatePreferences(XSharedPreferences prefs) {
        pDisableButtons = prefs.getBoolean("disableButtons", true);
        pDebug = prefs.getBoolean("debug", false);
        pEventLog = prefs.getBoolean("eventLogging", false);
        pHoverTimeout = prefs.getInt("hoverTimeout", 200);
        if (pDebug) XposedBridge.log("preferences updated: " + prefs.getAll());
    }




		// never called???
//		if(lpparam.packageName.equals("android")) {
//			XposedBridge.log("init ");
//			findAndHookMethod("com.android.server.accessibility.AccessibilityInputFilter", lpparam.classLoader, "onInputEvent", InputEvent.class, "int", new XC_MethodHook() {
//				@Override
//				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//					XposedBridge.log("AccIF: " + param.args[0].getClass().toString());
//				}
//			});
//		}

		// ClassNotFoundException
//		if (lpparam.packageName.equals("android")) {
//			XposedBridge.log("com.android.server.am.ActivityStackSupervisor$ActivityContainer");
//			findAndHookMethod("com.android.server.am.ActivityStackSupervisor$ActivityContainer", lpparam.classLoader, "injectEvent", InputEvent.class, new XC_MethodHook() {
//				@Override
//				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//					XposedBridge.log("ActivityContainer: " + param.args[0].getClass().toString());
//				}
//			});
//		}
}
