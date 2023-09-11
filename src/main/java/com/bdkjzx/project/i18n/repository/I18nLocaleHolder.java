package com.bdkjzx.project.i18n.repository;

import java.util.Locale;
import java.util.Optional;

public class I18nLocaleHolder {

    private final Locale globalLocale;
    private final ThreadLocal<Locale> localeThreadLocal = new InheritableThreadLocal<>();

    public I18nLocaleHolder(Locale globalLocale) {
        this.globalLocale = globalLocale;
    }

    /**
     * 获取目前的目标字符集
     *
     * @return 如果当前线程单独设置了，用线程的，否则使用全局设置{@link Locale}
     */
    public Locale getLocale() {
        return Optional.ofNullable(localeThreadLocal.get()).orElse(globalLocale);
    }

    /**
     * 直接获取默认的全局设置
     *
     * @return 直接获取默认的全局设置 {@link Locale}
     */
    public Locale getDefaultLocale() {
        return globalLocale;
    }

    /**
     * 为当前线程设置国际化
     *
     * @param locale 为当前线程设置国际化
     */
    public void setThreadLocale(Locale locale) {
        localeThreadLocal.set(locale);
    }

    /**
     * 请求结束时，清理语言设置
     */
    public void clear() {
        localeThreadLocal.remove();
    }
}