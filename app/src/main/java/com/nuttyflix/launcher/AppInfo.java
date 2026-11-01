package com.nuttyflix.launcher;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private final String label;
    private final String packageName;
    private final String className;
    private final Drawable icon;
    private final Drawable banner;
    private final boolean isTvInput;
    private final String inputId;

    public AppInfo(String label, String packageName, String className, Drawable icon, Drawable banner) {
        this(label, packageName, className, icon, banner, false, null);
    }

    public AppInfo(String label, String packageName, String className, Drawable icon, Drawable banner, boolean isTvInput, String inputId) {
        this.label = label;
        this.packageName = packageName;
        this.className = className;
        this.icon = icon;
        this.banner = banner;
        this.isTvInput = isTvInput;
        this.inputId = inputId;
    }

    public String getLabel() {
        return label;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public Drawable getIcon() {
        return icon;
    }

    public Drawable getBanner() {
        return banner;
    }

    public boolean isTvInput() {
        return isTvInput;
    }

    public String getInputId() {
        return inputId;
    }
}
