package net.betabears.android.xposed.mods.sPenTweaks;

import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class SPenTweaks implements IXposedHookLoadPackage {
	private boolean disableButtons = false;
	private int last = -9999;
	private boolean debug = false;

	// TODO: add settings

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

		// TODO: find correct package
		if (lpparam.packageName.equals("android")) {
			if (debug) XposedBridge.log("Hook before PointerEventDispatcher::onInputEvent.");
			findAndHookMethod("com.android.server.wm.PointerEventDispatcher", lpparam.classLoader, "onInputEvent",
					InputEvent.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (param.args[0] instanceof MotionEvent) {
						MotionEvent evt = (MotionEvent) param.args[0];
						if (debug && evt.getAction() != last) {
							last = evt.getAction();
							XposedBridge.log(evt.toString());
						}
						int actionIndex = evt.getActionIndex();
						int pointerId = evt.getPointerId(actionIndex);
						int tool = evt.getToolType(pointerId);
						int action = evt.getActionMasked();

						// disable buttons if any stylus-action except exiting is done
						disableButtons = tool == MotionEvent.TOOL_TYPE_STYLUS
								&& action != MotionEvent.ACTION_HOVER_EXIT;
					}
				}
			});
		}

		// TODO / FIXME: Callback is only executed when XPosedAdditions is enabled -> find out why and fix it
		// FIXME: With System default back-button mappings long-press still goes back even if it should be rejected
		// TODO: find correct package
		if (lpparam.packageName.equals("android")) {
			if (debug) XposedBridge.log("Hook before InputManager::injectInputEvent.");
			findAndHookMethod("android.hardware.input.InputManager", lpparam.classLoader, "injectInputEvent",
					InputEvent.class, "int", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (param.args[0] instanceof KeyEvent) {
						KeyEvent evt = (KeyEvent) param.args[0];
						if (debug && evt.getAction() != last) {
							last = evt.getAction();
							XposedBridge.log(evt.toString());
						}
						if (disableButtons && (evt.getKeyCode() == KeyEvent.KEYCODE_MENU
								|| evt.getKeyCode() == KeyEvent.KEYCODE_BACK)) {
							param.setResult(true);
							if (debug) XposedBridge.log("  blocked");
						}
					}
				}
			});
		}

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
}
