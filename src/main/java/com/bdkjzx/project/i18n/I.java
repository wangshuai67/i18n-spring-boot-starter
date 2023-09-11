package com.bdkjzx.project.i18n;

/**
 * 提供语法糖 在代码里看起来更简洁
 */
public class I {
    public static String n(String code, Object... params) {
        return I18nHolder.getI18nValue(code, params);
    }
}
