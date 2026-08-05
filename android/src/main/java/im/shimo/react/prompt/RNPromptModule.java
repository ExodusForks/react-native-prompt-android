package im.shimo.react.prompt;


import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.dialog.DialogModule;

import java.lang.ref.WeakReference;

import java.util.Map;

import javax.annotation.Nullable;

@ReactModule(name = RNPromptModule.NAME)
public class RNPromptModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    /* package */ static final String FRAGMENT_TAG =
            "im.shimo.react.prompt.RNPromptModule";

    /* package */ static final String NAME = "PromptAndroid";

    /* package */ static final String ACTION_BUTTON_CLICKED = "buttonClicked";
    /* package */ static final String ACTION_DISMISSED = "dismissed";
    /* package */ static final String KEY_TITLE = "title";
    /* package */ static final String KEY_MESSAGE = "message";
    /* package */ static final String KEY_BUTTON_POSITIVE = "buttonPositive";
    /* package */ static final String KEY_BUTTON_NEGATIVE = "buttonNegative";
    /* package */ static final String KEY_BUTTON_NEUTRAL = "buttonNeutral";
    /* package */ static final String KEY_ITEMS = "items";
    /* package */ static final String KEY_CANCELABLE = "cancelable";
    /* package */ static final String KEY_TYPE = "type";
    /* package */ static final String KEY_STYLE = "style";
    /* package */ static final String KEY_DEFAULT_VALUE = "defaultValue";
    /* package */ static final String KEY_PLACEHOLDER = "placeholder";
    /* package */ static final String KEY_SHOW_INPUT = "showInput";
    /* package */ static final String KEY_SHOW_OVER_TOP_ACTIVITY = "showOverTopActivity";
    /* package */ static final String TICK_EVENT = "PromptAndroidTick";

    /* package */ static final Map<String, Object> CONSTANTS = MapBuilder.<String, Object>of(
            ACTION_BUTTON_CLICKED, ACTION_BUTTON_CLICKED,
            ACTION_DISMISSED, ACTION_DISMISSED,
            KEY_BUTTON_POSITIVE, DialogInterface.BUTTON_POSITIVE,
            KEY_BUTTON_NEGATIVE, DialogInterface.BUTTON_NEGATIVE,
            KEY_BUTTON_NEUTRAL, DialogInterface.BUTTON_NEUTRAL,
            KEY_SHOW_OVER_TOP_ACTIVITY, true);

    private boolean mIsInForeground;
    // getCurrentActivity() only reports ours; this sees third-party activities too.
    private WeakReference<Activity> mTopActivity = new WeakReference<>(null);
    private volatile Handler mTicker;

    public RNPromptModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initialize() {
        getReactApplicationContext().addLifecycleEventListener(this);
        Application app = (Application) getReactApplicationContext().getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) { mTopActivity = new WeakReference<>(a); }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    // Paired with the option above: React Native stops JS timers while our host is paused.
    @ReactMethod
    public void startTicker(double intervalMs) {
        stopTicker();
        final long interval = (long) intervalMs;
        mTicker = new Handler(Looper.getMainLooper());
        mTicker.postDelayed(new Runnable() {
            @Override
            public void run() {
                ReactApplicationContext ctx = getReactApplicationContext();
                if (ctx.hasActiveCatalystInstance()) {
                    ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(TICK_EVENT, null);
                }
                // Re-read, never capture: re-posting to a dropped handler ticks forever.
                Handler ticker = mTicker;
                if (ticker != null) ticker.postDelayed(this, interval);
            }
        }, interval);
    }

    @ReactMethod
    public void stopTicker() {
        final Handler ticker = mTicker;
        mTicker = null;
        if (ticker != null) ticker.removeCallbacksAndMessages(null);
    }

    // Not a DialogFragment: recreation restores it without its listener, so nothing settles.
    private void showAlertOverTopActivity(final Bundle args, final Callback callback) {
        final Activity activity = mTopActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            FLog.w(RNPromptModule.class, "Tried to show an alert with no activity on top");
            if (callback != null) callback.invoke(ACTION_DISMISSED);
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PromptFragmentListener listener =
                        callback == null ? null : new PromptFragmentListener(callback);
                RNPromptFragment fragment = new RNPromptFragment();
                fragment.setListener(listener);
                Dialog dialog = fragment.createDialog(activity, args);
                dialog.setCancelable(!args.containsKey(KEY_CANCELABLE) || args.getBoolean(KEY_CANCELABLE));
                if (listener != null) dialog.setOnDismissListener(listener);
                dialog.show();
            }
        });
    }

    @Override
    public void onHostPause() {
        mIsInForeground = false;
        FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
        if (fragmentManagerHelper != null) {
            fragmentManagerHelper.dismissExisting();
        } else {
            FLog.w(DialogModule.class, "onHostPause called but no FragmentManager found");
        }
    }

    @Override
    public void onHostDestroy() {
    }

    @Override
    public void onHostResume() {
        mIsInForeground = true;
        stopTicker();
        // Check if a dialog has been created while the host was paused, so that we can show it now.
        FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
        if (fragmentManagerHelper != null) {
            fragmentManagerHelper.showPendingAlert();
        } else {
            FLog.w(DialogModule.class, "onHostResume called but no FragmentManager found");
        }
    }

    /**
     * reference: https://github.com/facebook/react-native/commit/0cd2f77116cbac8c5a402a355c2d359163429962
     */
    @ReactMethod
    public void alertWithArgs(
         ReadableMap options, final Callback callback) {
        final Bundle args = new Bundle();
        if (options.hasKey(KEY_TITLE)) {
            args.putString(RNPromptFragment.ARG_TITLE, options.getString(KEY_TITLE));
        }
        if (options.hasKey(KEY_MESSAGE)) {
            args.putString(RNPromptFragment.ARG_MESSAGE, options.getString(KEY_MESSAGE));
        }
        if (options.hasKey(KEY_BUTTON_POSITIVE)) {
            args.putString(RNPromptFragment.ARG_BUTTON_POSITIVE, options.getString(KEY_BUTTON_POSITIVE));
        }
        if (options.hasKey(KEY_BUTTON_NEGATIVE)) {
            args.putString(RNPromptFragment.ARG_BUTTON_NEGATIVE, options.getString(KEY_BUTTON_NEGATIVE));
        }
        if (options.hasKey(KEY_BUTTON_NEUTRAL)) {
            args.putString(RNPromptFragment.ARG_BUTTON_NEUTRAL, options.getString(KEY_BUTTON_NEUTRAL));
        }
        if (options.hasKey(KEY_ITEMS)) {
            ReadableArray items = options.getArray(KEY_ITEMS);
            CharSequence[] itemsArray = new CharSequence[items.size()];
            for (int i = 0; i < items.size(); i++) {
                itemsArray[i] = items.getString(i);
            }
            args.putCharSequenceArray(RNPromptFragment.ARG_ITEMS, itemsArray);
        }
        if (options.hasKey(KEY_CANCELABLE)) {
            args.putBoolean(KEY_CANCELABLE, options.getBoolean(KEY_CANCELABLE));
        }
        if (options.hasKey(KEY_SHOW_OVER_TOP_ACTIVITY) && options.getBoolean(KEY_SHOW_OVER_TOP_ACTIVITY)) {
            showAlertOverTopActivity(args, callback);
            return;
        }

        final FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
        if (fragmentManagerHelper == null) {
            FLog.w(RNPromptModule.class, "Tried to show an alert while not attached to an Activity");
            return;
        }
        fragmentManagerHelper.showNewAlert(mIsInForeground, args, callback);
    }


    @ReactMethod
    public void promptWithArgs(ReadableMap options, final Callback callback) {
        final FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
        if (fragmentManagerHelper == null) {
            FLog.w(RNPromptModule.class, "Tried to show an alert while not attached to an Activity");
            return;
        }

        final Bundle args = new Bundle();
        if (options.hasKey(KEY_TITLE)) {
            args.putString(RNPromptFragment.ARG_TITLE, options.getString(KEY_TITLE));
        }
        if (options.hasKey(KEY_MESSAGE)) {
            String message = options.getString(KEY_MESSAGE);
            if (!message.isEmpty()) {
                args.putString(RNPromptFragment.ARG_MESSAGE, options.getString(KEY_MESSAGE));
            }
        }
        if (options.hasKey(KEY_BUTTON_POSITIVE)) {
            args.putString(RNPromptFragment.ARG_BUTTON_POSITIVE, options.getString(KEY_BUTTON_POSITIVE));
        }
        if (options.hasKey(KEY_BUTTON_NEGATIVE)) {
            args.putString(RNPromptFragment.ARG_BUTTON_NEGATIVE, options.getString(KEY_BUTTON_NEGATIVE));
        }
        if (options.hasKey(KEY_BUTTON_NEUTRAL)) {
            args.putString(RNPromptFragment.ARG_BUTTON_NEUTRAL, options.getString(KEY_BUTTON_NEUTRAL));
        }
        if (options.hasKey(KEY_ITEMS)) {
            ReadableArray items = options.getArray(KEY_ITEMS);
            CharSequence[] itemsArray = new CharSequence[items.size()];
            for (int i = 0; i < items.size(); i++) {
                itemsArray[i] = items.getString(i);
            }
            args.putCharSequenceArray(RNPromptFragment.ARG_ITEMS, itemsArray);
        }
        if (options.hasKey(KEY_CANCELABLE)) {
            args.putBoolean(KEY_CANCELABLE, options.getBoolean(KEY_CANCELABLE));
        }
        if (options.hasKey(KEY_TYPE)) {
            args.putString(KEY_TYPE, options.getString(KEY_TYPE));
        }
        if (options.hasKey(KEY_STYLE)) {
            args.putString(KEY_STYLE, options.getString(KEY_STYLE));
        }
        if (options.hasKey(KEY_DEFAULT_VALUE)) {
            args.putString(KEY_DEFAULT_VALUE, options.getString(KEY_DEFAULT_VALUE));
        }
        if (options.hasKey(KEY_PLACEHOLDER)) {
            args.putString(KEY_PLACEHOLDER, options.getString(KEY_PLACEHOLDER));
        }
        args.putBoolean(KEY_SHOW_INPUT, true);
        fragmentManagerHelper.showNewAlert(mIsInForeground, args, callback);
    }


    private class FragmentManagerHelper {

        // Exactly one of the two is null
        private final
        @Nullable
        android.app.FragmentManager mFragmentManager;

        private
        @Nullable
        RNPromptFragment mFragmentToShow;


        public FragmentManagerHelper(android.app.FragmentManager fragmentManager) {
            mFragmentManager = fragmentManager;
        }

        public void showPendingAlert() {
            if (mFragmentToShow == null) {
                return;
            }
            mFragmentToShow.show(mFragmentManager, FRAGMENT_TAG);
            mFragmentToShow = null;
        }

        private void dismissExisting() {
            if (mFragmentManager != null) {
                RNPromptFragment oldFragment =
                        (RNPromptFragment) mFragmentManager.findFragmentByTag(FRAGMENT_TAG);
                if (oldFragment != null) {
                    oldFragment.dismiss();
                }
            }
        }

        public void showNewAlert(boolean isInForeground, Bundle arguments, Callback actionCallback) {
            dismissExisting();

            PromptFragmentListener actionListener =
                    actionCallback != null ? new PromptFragmentListener(actionCallback) : null;

            final RNPromptFragment promptFragment = new RNPromptFragment();
            promptFragment.setListener(actionListener);
            promptFragment.setArguments(arguments);

            if (isInForeground) {
                if (arguments.containsKey(KEY_CANCELABLE)) {
                    promptFragment.setCancelable(arguments.getBoolean(KEY_CANCELABLE));
                }
                promptFragment.show(mFragmentManager, FRAGMENT_TAG);
            } else {
                mFragmentToShow = promptFragment;
            }
        }
    }

    class PromptFragmentListener implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

        private final Callback mCallback;
        private boolean mCallbackConsumed = false;

        public PromptFragmentListener(Callback callback) {
            mCallback = callback;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (!mCallbackConsumed) {
                if (getReactApplicationContext().hasActiveCatalystInstance()) {
                    mCallback.invoke(ACTION_BUTTON_CLICKED, which);
                    mCallbackConsumed = true;
                }
            }
        }

        public void onConfirm(int which, String input) {
            if (!mCallbackConsumed) {
                if (getReactApplicationContext().hasActiveCatalystInstance()) {
                    mCallback.invoke(ACTION_BUTTON_CLICKED, which, input);
                    mCallbackConsumed = true;
                }
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (!mCallbackConsumed) {
                if (getReactApplicationContext().hasActiveCatalystInstance()) {
                    mCallback.invoke(ACTION_DISMISSED);
                    mCallbackConsumed = true;
                }
            }
        }
    }

    @Override
    public Map<String, Object> getConstants() {
        return CONSTANTS;
    }

    private
    @Nullable
    FragmentManagerHelper getFragmentManagerHelper() {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            return null;
        }
        return new FragmentManagerHelper(activity.getFragmentManager());
    }
}
