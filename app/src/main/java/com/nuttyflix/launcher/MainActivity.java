package com.nuttyflix.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private ImageView imgWallpaper;
    private View viewOverlay;
    private ScrollView mainScroll;
    private GridLayout gridFavorites;
    private GridLayout gridApplications;
    private TextView btnToggleApplications;
    private ImageView btnSettings;
    private ImageView ivNetworkStatus;
    private LayoutInflater inflater;
    private ViewOutlineProvider cardOutlineProvider;
    private SharedPreferences prefs;

    private final List<AppInfo> favoriteApps = new ArrayList<>();
    private final List<AppInfo> otherApps = new ArrayList<>();

    // Rearrange mode state
    private boolean isRearranging = false;
    private AppInfo rearrangingApp = null;
    private boolean isRearrangingFavorites = true;
    private boolean isApplicationsExpanded = false;

    private static final String PREFS_NAME = "nutty_launcher_prefs";
    private static final String KEY_FAV_ORDER = "favorites_order";
    private static final String KEY_OLED_MODE = "oled_pure_black_mode";
    private static final String KEY_HIDDEN_APPS = "hidden_apps_set";

    private static final String[] DEFAULT_PINNED_PACKAGES = {
        "com.netflix.ninja",
        "com.google.android.youtube.tv",
        "in.startv.hotstar.dplus.tv",
        "com.disney.disneyplus",
        "com.viu.tv",
        "com.truedmp.idtv",
        "com.wbd.stream",
        "com.hbo.asia.androidtv",
        "com.amazon.amazonvideo.livingroom",
        "com.iqiyi.i18n.tv",
        "com.ais.mimo.aisplay.tv"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        inflater = LayoutInflater.from(this);

        imgWallpaper = findViewById(R.id.img_wallpaper);
        viewOverlay = findViewById(R.id.view_overlay);
        mainScroll = findViewById(R.id.main_scroll);
        gridFavorites = findViewById(R.id.grid_favorites);
        gridApplications = findViewById(R.id.grid_applications);
        btnToggleApplications = findViewById(R.id.btn_toggle_applications);
        btnSettings = findViewById(R.id.btn_settings);
        ivNetworkStatus = findViewById(R.id.iv_network_status);

        final int radiusPx = (int) (10 * getResources().getDisplayMetrics().density);
        cardOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
            }
        };

        applyOledMode(isOledModeEnabled());
        setupSettingsButton();
        setupToggleApplicationsButton();
        updateNetworkStatus();
        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatus();
        if (!isRearranging) {
            loadApps();
        }
    }

    private boolean isOledModeEnabled() {
        return prefs.getBoolean(KEY_OLED_MODE, false);
    }

    private void applyOledMode(boolean enabled) {
        if (enabled) {
            if (imgWallpaper != null) imgWallpaper.setVisibility(View.GONE);
            if (viewOverlay != null) viewOverlay.setVisibility(View.GONE);
            getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        } else {
            if (imgWallpaper != null) imgWallpaper.setVisibility(View.VISIBLE);
            if (viewOverlay != null) viewOverlay.setVisibility(View.VISIBLE);
            getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void updateNetworkStatus() {
        if (ivNetworkStatus == null) return;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (caps != null) {
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        ivNetworkStatus.setImageResource(R.drawable.ic_ethernet_connected);
                        ivNetworkStatus.setVisibility(View.VISIBLE);
                        return;
                    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        ivNetworkStatus.setImageResource(R.drawable.ic_wifi_connected);
                        ivNetworkStatus.setVisibility(View.VISIBLE);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}
        ivNetworkStatus.setVisibility(View.GONE);
    }

    private void setupSettingsButton() {
        btnSettings.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    btnSettings.setColorFilter(Color.parseColor("#0F172A"));
                    v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).start();
                } else {
                    btnSettings.setColorFilter(Color.WHITE);
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                }
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLauncherSettingsDialog();
            }
        });
    }

    private void setupToggleApplicationsButton() {
        btnToggleApplications.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    btnToggleApplications.setTextColor(Color.WHITE);
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).start();
                } else {
                    btnToggleApplications.setTextColor(Color.parseColor("#94A3B8"));
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                }
            }
        });

        btnToggleApplications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isApplicationsExpanded = !isApplicationsExpanded;
                updateApplicationsVisibility();
            }
        });
    }

    private void updateApplicationsVisibility() {
        if (isApplicationsExpanded) {
            gridApplications.setVisibility(View.VISIBLE);
            btnToggleApplications.setText("Hide Applications  ▲");
            mainScroll.post(new Runnable() {
                @Override
                public void run() {
                    mainScroll.smoothScrollTo(0, btnToggleApplications.getTop() - 20);
                }
            });
        } else {
            gridApplications.setVisibility(View.GONE);
            btnToggleApplications.setText("All Applications  ▼");
            mainScroll.post(new Runnable() {
                @Override
                public void run() {
                    mainScroll.smoothScrollTo(0, 0);
                }
            });
        }
    }

    private Set<String> getHiddenApps() {
        return prefs.getStringSet(KEY_HIDDEN_APPS, new HashSet<String>());
    }

    private void setAppHidden(String packageName, boolean hidden) {
        Set<String> current = new HashSet<>(getHiddenApps());
        if (hidden) {
            current.add(packageName);
        } else {
            current.remove(packageName);
        }
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, current).apply();
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Set<String> addedPackages = new HashSet<>();
        List<AppInfo> allDiscovered = new ArrayList<>();
        Set<String> hiddenSet = getHiddenApps();

        // 1. Android TV Leanback Apps
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN, null);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> leanbackApps = pm.queryIntentActivities(leanbackIntent, 0);

        // 2. Sideloaded / Mobile Standard Apps
        Intent standardIntent = new Intent(Intent.ACTION_MAIN, null);
        standardIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> standardApps = pm.queryIntentActivities(standardIntent, 0);

        List<ResolveInfo> allResolves = new ArrayList<>();
        if (leanbackApps != null) allResolves.addAll(leanbackApps);
        if (standardApps != null) allResolves.addAll(standardApps);

        String myPackage = getPackageName();

        for (ResolveInfo info : allResolves) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(myPackage) || addedPackages.contains(pkg) || hiddenSet.contains(pkg)) {
                continue;
            }
            addedPackages.add(pkg);

            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            Drawable banner = null;
            try {
                banner = info.activityInfo.loadBanner(pm);
                if (banner == null) {
                    banner = info.activityInfo.applicationInfo.loadBanner(pm);
                }
            } catch (Exception ignored) {}

            String className = info.activityInfo.name;
            allDiscovered.add(new AppInfo(label, pkg, className, icon, banner));
        }

        // 3. Hardware TV Inputs (HDMI 1, HDMI 2, Live TV)
        try {
            TvInputManager tvInputManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (tvInputManager != null) {
                List<TvInputInfo> inputs = tvInputManager.getTvInputList();
                if (inputs != null) {
                    Drawable hdmiIcon = getDrawable(R.drawable.ic_hdmi_banner);
                    for (TvInputInfo input : inputs) {
                        if (input.isPassthroughInput()) {
                            String inputId = input.getId();
                            String pkgKey = "input:" + inputId;
                            if (addedPackages.contains(pkgKey) || hiddenSet.contains(pkgKey)) {
                                continue;
                            }
                            addedPackages.add(pkgKey);

                            String label = "HDMI";
                            CharSequence cs = input.loadLabel(this);
                            if (cs != null && !cs.toString().trim().isEmpty()) {
                                label = cs.toString().trim();
                            } else if (inputId.contains("HW4") || inputId.contains("1")) {
                                label = "HDMI 1";
                            } else if (inputId.contains("HW5") || inputId.contains("2")) {
                                label = "HDMI 2";
                            } else if (inputId.contains("HW6") || inputId.contains("3")) {
                                label = "HDMI 3";
                            }

                            allDiscovered.add(new AppInfo(label, pkgKey, inputId, hdmiIcon, null, true, inputId));
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        List<String> favOrder = getSavedFavoriteOrder();
        Set<String> favSet = new HashSet<>(favOrder);

        favoriteApps.clear();
        otherApps.clear();

        for (AppInfo app : allDiscovered) {
            if (favSet.contains(app.getPackageName())) {
                favoriteApps.add(app);
            } else {
                otherApps.add(app);
            }
        }

        Collections.sort(favoriteApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                int indexA = favOrder.indexOf(a.getPackageName());
                int indexB = favOrder.indexOf(b.getPackageName());
                if (indexA == -1) indexA = Integer.MAX_VALUE;
                if (indexB == -1) indexB = Integer.MAX_VALUE;
                return Integer.compare(indexA, indexB);
            }
        });

        Collections.sort(otherApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return a.getLabel().compareToIgnoreCase(b.getLabel());
            }
        });

        renderGrids();
    }

    private List<String> getSavedFavoriteOrder() {
        String saved = prefs.getString(KEY_FAV_ORDER, null);
        if (saved != null && !saved.trim().isEmpty()) {
            return new ArrayList<>(Arrays.asList(saved.split(",")));
        }
        return new ArrayList<>(Arrays.asList(DEFAULT_PINNED_PACKAGES));
    }

    private void saveFavoriteOrder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < favoriteApps.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(favoriteApps.get(i).getPackageName());
        }
        prefs.edit().putString(KEY_FAV_ORDER, sb.toString()).apply();
    }

    private void renderGrids() {
        gridFavorites.removeAllViews();
        for (int i = 0; i < favoriteApps.size(); i++) {
            gridFavorites.addView(createCardView(favoriteApps.get(i), true, i));
        }

        gridApplications.removeAllViews();
        for (int i = 0; i < otherApps.size(); i++) {
            gridApplications.addView(createCardView(otherApps.get(i), false, i));
        }

        if (otherApps.isEmpty()) {
            btnToggleApplications.setVisibility(View.GONE);
        } else {
            btnToggleApplications.setVisibility(View.VISIBLE);
        }

        if (!isRearranging) {
            gridFavorites.post(new Runnable() {
                @Override
                public void run() {
                    if (gridFavorites.getChildCount() > 0) {
                        gridFavorites.getChildAt(0).requestFocus();
                    }
                }
            });
        }
    }

    private View createCardView(final AppInfo app, final boolean isFav, final int position) {
        View card = inflater.inflate(R.layout.item_app_card, null);

        float density = getResources().getDisplayMetrics().density;
        int widthPx = (int) (136 * density);
        int heightPx = (int) (76 * density);
        int marginPx = (int) (4 * density);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = widthPx;
        params.height = heightPx;
        params.setMargins(marginPx, marginPx, marginPx, marginPx);
        card.setLayoutParams(params);

        card.setOutlineProvider(cardOutlineProvider);
        card.setClipToOutline(true);

        ImageView imgBanner = card.findViewById(R.id.app_banner);
        View layoutFallback = card.findViewById(R.id.card_fallback);
        ImageView imgIcon = card.findViewById(R.id.app_icon);
        final TextView txtName = card.findViewById(R.id.app_name);
        final View cardBorder = card.findViewById(R.id.card_border);

        final boolean hasBanner = (app.getBanner() != null);
        if (hasBanner) {
            imgBanner.setImageDrawable(app.getBanner());
            imgBanner.setVisibility(View.VISIBLE);
            layoutFallback.setVisibility(View.GONE);
        } else {
            imgBanner.setVisibility(View.GONE);
            layoutFallback.setVisibility(View.VISIBLE);
            if (app.getIcon() != null) {
                imgIcon.setImageDrawable(app.getIcon());
            }
            txtName.setText(app.getLabel());
        }

        if (isRearranging && rearrangingApp != null && rearrangingApp.getPackageName().equals(app.getPackageName())) {
            cardBorder.setBackgroundResource(R.drawable.flauncher_border_rearrange);
            cardBorder.setVisibility(View.VISIBLE);
            card.setScaleX(1.10f);
            card.setScaleY(1.10f);
            card.setTranslationZ(16f);
        }

        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (isRearranging) return;

                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).translationZ(12f).setDuration(120).start();
                    cardBorder.setBackgroundResource(R.drawable.flauncher_border_focused);
                    cardBorder.setVisibility(View.VISIBLE);
                    if (!hasBanner) {
                        txtName.setTextColor(Color.parseColor("#0F172A"));
                    }
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(120).start();
                    cardBorder.setVisibility(View.GONE);
                    if (!hasBanner) {
                        txtName.setTextColor(Color.WHITE);
                    }
                }
            }
        });

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRearranging) {
                    finishRearrange();
                    return;
                }
                launchApp(app);
            }
        });

        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isRearranging) {
                    showAppManageDialog(app, isFav);
                    return true;
                }
                return false;
            }
        });

        return card;
    }

    private void launchApp(AppInfo app) {
        if (app.isTvInput()) {
            try {
                Uri uri = TvContract.buildChannelUriForPassthroughInput(app.getInputId());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Cannot switch to " + app.getLabel(), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        try {
            Intent launchIntent = new Intent(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launchIntent.setComponent(new ComponentName(app.getPackageName(), app.getClassName()));
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(launchIntent);
        } catch (Exception e) {
            try {
                Intent fallback = getPackageManager().getLaunchIntentForPackage(app.getPackageName());
                if (fallback != null) {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(fallback);
                } else {
                    Toast.makeText(MainActivity.this, "Cannot open " + app.getLabel(), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                Toast.makeText(MainActivity.this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showAppManageDialog(final AppInfo app, final boolean isFav) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app_manage);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView txtName = dialog.findViewById(R.id.dialog_app_name);
        TextView btnMove = dialog.findViewById(R.id.btn_action_move);
        TextView btnFav = dialog.findViewById(R.id.btn_action_fav);
        TextView btnHide = dialog.findViewById(R.id.btn_action_hide);
        TextView btnInfo = dialog.findViewById(R.id.btn_action_info);
        TextView btnUninstall = dialog.findViewById(R.id.btn_action_uninstall);

        txtName.setText(app.getLabel());
        btnFav.setText(isFav ? "Unfavorite" : "Favorite");

        btnMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startRearrange(app, isFav);
            }
        });

        btnFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (isFav) {
                    favoriteApps.remove(app);
                    otherApps.add(0, app);
                } else {
                    otherApps.remove(app);
                    favoriteApps.add(app);
                }
                saveFavoriteOrder();
                renderGrids();
            }
        });

        btnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                setAppHidden(app.getPackageName(), true);
                Toast.makeText(MainActivity.this, "Hidden " + app.getLabel(), Toast.LENGTH_SHORT).show();
                loadApps();
            }
        });

        if (app.isTvInput()) {
            btnInfo.setVisibility(View.GONE);
            btnUninstall.setVisibility(View.GONE);
        } else {
            btnInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + app.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Cannot open details", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            btnUninstall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    try {
                        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                        uninstallIntent.setData(Uri.parse("package:" + app.getPackageName()));
                        uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                        startActivity(uninstallIntent);
                    } catch (Exception e) {
                        try {
                            Intent delIntent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.getPackageName()));
                            startActivity(delIntent);
                        } catch (Exception ex) {
                            Toast.makeText(MainActivity.this, "Cannot uninstall system app", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        dialog.show();
        btnMove.requestFocus();
    }

    private void showLauncherSettingsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_launcher_settings);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final TextView btnOled = dialog.findViewById(R.id.btn_setting_oled);
        TextView btnScreenOff = dialog.findViewById(R.id.btn_setting_screen_off);
        TextView btnHidden = dialog.findViewById(R.id.btn_setting_hidden);
        TextView btnSystem = dialog.findViewById(R.id.btn_setting_system);

        final boolean currentOled = isOledModeEnabled();
        btnOled.setText("OLED Pure Black: " + (currentOled ? "ON" : "OFF"));

        btnOled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newOled = !isOledModeEnabled();
                prefs.edit().putBoolean(KEY_OLED_MODE, newOled).apply();
                applyOledMode(newOled);
                btnOled.setText("OLED Pure Black: " + (newOled ? "ON" : "OFF"));
            }
        });

        btnScreenOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showScreenOffMode();
            }
        });

        btnHidden.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showManageHiddenAppsDialog();
            }
        });

        btnSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent fallback = getPackageManager().getLaunchIntentForPackage("com.android.tv.settings");
                        if (fallback != null) startActivity(fallback);
                    } catch (Exception ex) {
                        Toast.makeText(MainActivity.this, "Cannot open Settings", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        dialog.show();
        btnOled.requestFocus();
    }

    private void showManageHiddenAppsDialog() {
        final Set<String> hiddenSet = getHiddenApps();
        if (hiddenSet.isEmpty()) {
            Toast.makeText(this, "No hidden apps", Toast.LENGTH_SHORT).show();
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.dialog_glass_bg);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);
        root.setLayoutParams(new LinearLayout.LayoutParams(
            (int) (260 * getResources().getDisplayMetrics().density),
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView header = new TextView(this);
        header.setText("Hidden Apps (Click to Unhide)");
        header.setTextColor(Color.WHITE);
        header.setTextSize(15);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 0, 0, pad / 2);
        root.addView(header);

        PackageManager pm = getPackageManager();
        for (final String pkg : hiddenSet) {
            String label = pkg;
            try {
                if (pkg.startsWith("input:")) {
                    label = "Input: " + pkg.substring(6);
                } else {
                    label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
                }
            } catch (Exception ignored) {}

            TextView btnApp = new TextView(this);
            btnApp.setText("Unhide: " + label);
            btnApp.setBackgroundResource(R.drawable.btn_dialog_selector);
            btnApp.setTextColor(Color.WHITE);
            btnApp.setPadding(pad / 2, pad / 2, pad / 2, pad / 2);
            btnApp.setFocusable(true);
            btnApp.setClickable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = pad / 3;
            btnApp.setLayoutParams(lp);

            btnApp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setAppHidden(pkg, false);
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "Unhid app", Toast.LENGTH_SHORT).show();
                    loadApps();
                }
            });
            root.addView(btnApp);
        }

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void showScreenOffMode() {
        final Dialog screenOffDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        Window w = screenOffDialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.screenBrightness = 0.001f;
            w.setAttributes(lp);
        }

        TextView txt = new TextView(this);
        txt.setText("Audio Mode Active\nPress any remote key to wake screen");
        txt.setTextColor(Color.parseColor("#475569"));
        txt.setTextSize(14);
        txt.setGravity(android.view.Gravity.CENTER);
        screenOffDialog.setContentView(txt);

        // Auto fade out hint text after 3 seconds
        txt.postDelayed(new Runnable() {
            @Override
            public void run() {
                txt.animate().alpha(0f).setDuration(1000).start();
            }
        }, 3000);

        screenOffDialog.setOnKeyListener(new android.content.DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    screenOffDialog.dismiss();
                    return true;
                }
                return false;
            }
        });

        screenOffDialog.show();
    }

    private void startRearrange(AppInfo app, boolean isFav) {
        isRearranging = true;
        rearrangingApp = app;
        isRearrangingFavorites = isFav;

        btnSettings.setFocusable(false);
        btnToggleApplications.setFocusable(false);

        renderGrids();
        focusRearrangingCard();
    }

    private void finishRearrange() {
        if (!isRearranging) return;
        isRearranging = false;
        rearrangingApp = null;

        btnSettings.setFocusable(true);
        btnToggleApplications.setFocusable(true);

        saveFavoriteOrder();
        renderGrids();
    }

    private void focusRearrangingCard() {
        GridLayout targetGrid = isRearrangingFavorites ? gridFavorites : gridApplications;
        List<AppInfo> targetList = isRearrangingFavorites ? favoriteApps : otherApps;

        int index = targetList.indexOf(rearrangingApp);
        if (index >= 0 && index < targetGrid.getChildCount()) {
            final View child = targetGrid.getChildAt(index);
            child.post(new Runnable() {
                @Override
                public void run() {
                    child.requestFocus();
                }
            });
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isRearranging && event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            List<AppInfo> targetList = isRearrangingFavorites ? favoriteApps : otherApps;
            GridLayout targetGrid = isRearrangingFavorites ? gridFavorites : gridApplications;
            int currentIndex = targetList.indexOf(rearrangingApp);

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (currentIndex > 0) {
                    Collections.swap(targetList, currentIndex, currentIndex - 1);
                    View movingView = targetGrid.getChildAt(currentIndex);
                    targetGrid.removeView(movingView);
                    targetGrid.addView(movingView, currentIndex - 1);
                    movingView.requestFocus();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (currentIndex >= 0 && currentIndex < targetList.size() - 1) {
                    Collections.swap(targetList, currentIndex, currentIndex + 1);
                    View movingView = targetGrid.getChildAt(currentIndex);
                    targetGrid.removeView(movingView);
                    targetGrid.addView(movingView, currentIndex + 1);
                    movingView.requestFocus();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BACK) {
                finishRearrange();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (isRearranging) {
            finishRearrange();
            return;
        }
        if (isApplicationsExpanded) {
            isApplicationsExpanded = false;
            updateApplicationsVisibility();
            return;
        }
        if (gridFavorites.getChildCount() > 0) {
            gridFavorites.getChildAt(0).requestFocus();
        }
    }
}
