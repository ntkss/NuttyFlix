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
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.ImageView;
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
    private ScrollView mainScroll;
    private GridLayout gridFavorites;
    private GridLayout gridApplications;
    private TextView btnToggleApplications;
    private ImageView btnSettings;
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
        mainScroll = findViewById(R.id.main_scroll);
        gridFavorites = findViewById(R.id.grid_favorites);
        gridApplications = findViewById(R.id.grid_applications);
        btnToggleApplications = findViewById(R.id.btn_toggle_applications);
        btnSettings = findViewById(R.id.btn_settings);

        final int radiusPx = (int) (10 * getResources().getDisplayMetrics().density);
        cardOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
            }
        };

        setupSettingsButton();
        setupToggleApplicationsButton();
        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isRearranging) {
            loadApps();
        }
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

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Set<String> addedPackages = new HashSet<>();
        List<AppInfo> allDiscovered = new ArrayList<>();

        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN, null);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> leanbackApps = pm.queryIntentActivities(leanbackIntent, 0);

        Intent standardIntent = new Intent(Intent.ACTION_MAIN, null);
        standardIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> standardApps = pm.queryIntentActivities(standardIntent, 0);

        List<ResolveInfo> allResolves = new ArrayList<>();
        if (leanbackApps != null) allResolves.addAll(leanbackApps);
        if (standardApps != null) allResolves.addAll(standardApps);

        String myPackage = getPackageName();

        for (ResolveInfo info : allResolves) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(myPackage) || addedPackages.contains(pkg)) {
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

        dialog.show();
        btnMove.requestFocus();
    }

    private void startRearrange(AppInfo app, boolean isFav) {
        isRearranging = true;
        rearrangingApp = app;
        isRearrangingFavorites = isFav;

        // Prevent settings button from stealing focus during rearrange
        btnSettings.setFocusable(false);
        btnToggleApplications.setFocusable(false);

        renderGrids();
        focusRearrangingCard();
    }

    private void finishRearrange() {
        if (!isRearranging) return;
        isRearranging = false;
        rearrangingApp = null;

        // Re-enable settings focus
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
                    
                    // Direct view swap without recreating all views
                    View movingView = targetGrid.getChildAt(currentIndex);
                    targetGrid.removeView(movingView);
                    targetGrid.addView(movingView, currentIndex - 1);
                    movingView.requestFocus();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (currentIndex >= 0 && currentIndex < targetList.size() - 1) {
                    Collections.swap(targetList, currentIndex, currentIndex + 1);
                    
                    // Direct view swap without recreating all views
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
