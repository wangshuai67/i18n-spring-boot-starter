package com.bdkjzx.project.i18n.repository;

import com.bdkjzx.project.i18n.config.I18nProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;

@Slf4j
public class I18nMessageResource extends AbstractMessageSource implements ResourceLoaderAware {
    private final Locale defaultLocale;

    @Autowired
    private List<I18nConfigDbLoader> i18NConfigDbLoaders;
    @Autowired
    private I18nProperties i18NProperties;
    @Lazy
    @Autowired(required = false)
    private I18nConfigDbLoader i18nConfigDbLoader;

    private final List<BiFunction<String, Locale, String>> getTextFunctionList = new ArrayList<>();

    public I18nMessageResource(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    @PostConstruct
    public void init() {
        if (this.i18NProperties.getEnable()) {
            getTextFunctionList.add(this::normalFinder);
            getTextFunctionList.add(this::languageFinder);
            getTextFunctionList.add(this::defaultLocaleFinder);

            if (i18NProperties.getUseLocale() && getParentMessageSource() != null) {
                getTextFunctionList.add(this::localFinder);
                getTextFunctionList.add(this::localDefaultFinder);
            }
        }
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
    }

    @Override
    protected MessageFormat resolveCode(@NonNull String code, @NonNull Locale locale) {
        String msg = getText(code, locale);
        return createMessageFormat(msg, locale);
    }

    @Override
    protected String resolveCodeWithoutArguments(@NonNull String code, @NonNull Locale locale) {
        return getText(code, locale);
    }

    /**
     * 这是加载国际化变量的核心方法，先从自己控制的内存中取，取不到了再到资源文件中取
     *
     * @param code   编码
     * @param locale 本地化语言
     * @return 查询对应语言的信息
     */
    private String getText(String code, Locale locale) {

        String result = getTextWithOutMark(code, locale);
        if (StringUtils.isEmpty(result)) {
            return result;
        }

        // 确实没有这项配置，确定是否要记录
        logger.warn("未找到国际化配置：" + code);
        if (i18NProperties.getMark()) {
            i18nConfigDbLoader.markInvalidCode(code);
        }
        //如果最终还是取不到，返回了NULL，则外面会用默认值，如果没有默认值，最终会返回给页面变量名称,所以变量名称尽量有含义，以作为遗漏配置的最后保障
        return code;
    }

    public String getTextWithOutMark(String code, Locale locale) {

        String result = "";
        // 从 function list中依次使用各种策略查询
        for (BiFunction<String, Locale, String> func : getTextFunctionList) {
            result = func.apply(code, locale);
            if (!StringUtils.isEmpty(result)) {
                return result;
            }
        }
        return result;
    }

    /**
     * 从指定locale获取值
     *
     * @param code   i18n code
     * @param locale 语言
     * @return 查询对应语言的信息
     */
    private String findValueFromLocale(String code, Locale locale) {
        String resultValue;
        for (I18nConfigDbLoader i18NConfigDbLoader : i18NConfigDbLoaders) {
            // 在loadE6I18nDictByLocaleEntity中做过缓存了
            resultValue = Optional.ofNullable(i18NConfigDbLoader.loadI18nDictByLocaleEntity())
                    .flatMap(localeMap -> Optional.ofNullable(localeMap.get(locale))
                            .map(codeMap -> codeMap.get(code)))
                    .orElse(null);
            if (!org.springframework.util.StringUtils.isEmpty(resultValue)) {
                return resultValue;
            }
        }
        return null;
    }

    // ======================================   查询字符的五种策略，加入function list   ======================================

    /**
     * 第一种情况：通过期望的语言类型查找
     *
     * @param code   国际化代码
     * @param locale 语言
     * @return 没找到时返回null
     */
    private String normalFinder(String code, Locale locale) {
        return findValueFromLocale(code, locale);
    }

    /**
     * 第二种情况，如果期望是 语言-国家 没有找到，那么尝试只找一下语言,比如zh-tw没找到，那就尝试找一下zh
     *
     * @param code   国际化代码
     * @param locale 语言
     * @return 没找到时返回null
     */
    private String languageFinder(String code, Locale locale) {
        if (locale.getLanguage() != null) {
            return findValueFromLocale(code, Locale.forLanguageTag(locale.getLanguage()));
        }
        return null;
    }

    /**
     * 第三种情况，如果没有找到 且不是默认语言包，则取默认语言包
     *
     * @param code   国际化代码
     * @param locale 语言
     * @return 没找到时返回null
     */
    private String defaultLocaleFinder(String code, Locale locale) {
        if (!Objects.equals(locale, defaultLocale)) {
            return findValueFromLocale(code, defaultLocale);
        }
        return null;
    }

    /**
     * 第四种情况，通过以上三种方式都没找到，那么尝试从本地配置文件加载期望的语言类型是否有
     *
     * @param code   国际化代码
     * @param locale 语言
     * @return 没找到时返回null
     */
    private String localFinder(String code, Locale locale) {
        String value = Objects.requireNonNull(getParentMessageSource()).getMessage(code, null, null, locale);
        if (logger.isDebugEnabled() && !StringUtils.isEmpty(value)) {
            logger.debug("从配置文件" + locale.toString() + "找到变量" + code + "=" + value);
        }
        return value;
    }

    /**
     * 第五种情况，如果没有找到，则从本地配置文件加载默认的语言类型是否有
     *
     * @param code   国际化代码
     * @param locale 语言
     * @return 没找到时返回null
     */
    private String localDefaultFinder(String code, Locale locale) {
        if (!Objects.equals(locale, defaultLocale)) {
            return this.localFinder(code, defaultLocale);
        }
        return null;
    }

}
