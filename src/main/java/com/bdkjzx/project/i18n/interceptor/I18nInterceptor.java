package com.bdkjzx.project.i18n.interceptor;

import com.bdkjzx.project.i18n.repository.I18nLocaleHolder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Slf4j
public class I18nInterceptor implements HandlerInterceptor {

    private I18nLocaleHolder i18nLocaleHolder;

    private final Map<String, Locale> localeMap = new HashMap<>(8);

    private static final String NAME_OF_LANGUAGE_SETTING = "lang";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String lang = getLangFromCookies(request);
        if (StringUtils.isEmpty(lang)) {
            lang = getLangFromHeader(request);
        }

        if (StringUtils.isEmpty(lang)) {
            return true;
        }
        try {
            i18nLocaleHolder.setThreadLocale(getLocaleByLang(lang));
        } catch (Exception e) {
            log.error("无效的语言设置：{}", lang, e);
        }

        return true;
    }


    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        try {
            i18nLocaleHolder.clear();
        } catch (Exception e) {
            log.error("清理语言设置时遇到错误：", e);
        }
    }

    public I18nLocaleHolder getI18nLocaleHolder() {
        return i18nLocaleHolder;
    }

    public void setI18nLocaleHolder(I18nLocaleHolder i18nLocaleHolder) {
        this.i18nLocaleHolder = i18nLocaleHolder;
    }

    /**
     * 获得语言设置
     *
     * @param lang 语言设置
     * @return {@link Locale}
     */
    private Locale getLocaleByLang(String lang) {
        return Optional.ofNullable(localeMap.get(lang))
                .orElseGet(() -> {
                    Locale locale = new Locale.Builder().setLanguageTag(lang).build();
                    localeMap.put(lang, locale);
                    return locale;
                });
    }

    /**
     * 从 cookie 中获取 国际化语言
     *
     * @param request
     * @return
     */
    private static String getLangFromCookies(HttpServletRequest request) {
        String lang = Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(cookie -> NAME_OF_LANGUAGE_SETTING.equals(cookie.getName()))
                        .findFirst())
                .map(Cookie::getValue)
                .orElse("");
        return lang;
    }

    /**
     * 从 header 中获取 国际化语言
     *
     * @param request
     * @return
     */
    private String getLangFromHeader(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        return Optional.ofNullable(acceptLanguage)
                .map(lang -> lang.split(","))
                .filter(array -> array.length > 0)
                .map(array -> array[0])
                .orElse("");
    }

}
