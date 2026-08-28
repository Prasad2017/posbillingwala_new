package com.posbillingwala.owner.Extra;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.posbillingwala.owner.BuildConfig;

/**
 * Applies screenshot policy from {@link BuildConfig#ALLOW_SCREENSHOT}
 * to every activity, fragment, and dialog window.
 */
public final class ScreenshotConfig {

    private ScreenshotConfig() {
    }

    public static boolean isAllowed() {
        return BuildConfig.ALLOW_SCREENSHOT;
    }

    public static void install(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                apply(activity);
                registerFragments(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                apply(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                apply(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    public static void apply(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        applyWindow(activity.getWindow());
        applyFragmentTree(activity);
    }

    public static void applyWindow(@Nullable Window window) {
        if (window == null) {
            return;
        }
        if (isAllowed()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            WindowManager.LayoutParams lp = window.getAttributes();
            if (lp != null) {
                lp.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
                window.setAttributes(lp);
            }
        } else {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public static void applyDialog(@Nullable Dialog dialog) {
        if (dialog != null) {
            applyWindow(dialog.getWindow());
        }
    }

    private static void registerFragments(Activity activity) {
        if (!(activity instanceof FragmentActivity)) {
            return;
        }
        ((FragmentActivity) activity).getSupportFragmentManager()
                .registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
                        applyFragment(fragment);
                    }

                    @Override
                    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
                        applyFragment(fragment);
                    }

                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment fragment,
                                                      @NonNull View v, @Nullable Bundle savedInstanceState) {
                        applyFragment(fragment);
                    }
                }, true);
    }

    private static void applyFragmentTree(Activity activity) {
        if (!(activity instanceof FragmentActivity)) {
            return;
        }
        applyFragmentManager(((FragmentActivity) activity).getSupportFragmentManager());
    }

    private static void applyFragmentManager(FragmentManager fm) {
        for (Fragment fragment : fm.getFragments()) {
            applyFragment(fragment);
            if (fragment.isAdded()) {
                applyFragmentManager(fragment.getChildFragmentManager());
            }
        }
    }

    private static void applyFragment(@Nullable Fragment fragment) {
        if (fragment == null) {
            return;
        }
        Activity host = fragment.getActivity();
        if (host != null) {
            applyWindow(host.getWindow());
        }
        if (fragment instanceof DialogFragment) {
            applyDialog(((DialogFragment) fragment).getDialog());
        }
    }
}
