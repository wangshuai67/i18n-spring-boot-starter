package com.bdkjzx.project.i18n;


import com.bdkjzx.project.i18n.repository.I18nLocaleHolder;
import com.bdkjzx.project.i18n.repository.I18nMessageResource;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;


public class I18nHolder {

    /**
     * 记录表中时间戳字段的名称
     */
    public static final String COLUMN_NAME_MODIFIED_TIME = "modified_time";

    @SuppressWarnings("unused")
    private static final Logger log = getLogger(I18nHolder.class);

    private static I18nMessageResource messageResource;
    private static I18nLocaleHolder i18nLocaleHolder;
    private static boolean enable;
    /**
     * 每个消息对应一个 MessageFormat，把MessageFormat保存起来用以格式化
     */
    private static HashMap<String, MessageFormat> messageFormatMap;

    public I18nHolder(I18nMessageResource messageSource, I18nLocaleHolder i18nLocaleHolder) {
        I18nHolder.messageResource = messageSource;
        I18nHolder.i18nLocaleHolder = i18nLocaleHolder;
        I18nHolder.enable = true;
    }

    public I18nHolder() {
        I18nHolder.enable = false;
    }

    /**
     * 获取国际化消息
     *
     * @param code   消息代码
     * @param params 消息参数
     * @return 国际化后的消息
     */
    public static String getI18nValue(String code, Object... params) {
        if (!enable) {
            return getDummyValue(code, params);
        }
        return getI18nValue(i18nLocaleHolder.getLocale(), code, params);
    }

    /**
     * 指定语言，获取国际化消息
     *
     * @param locale 指定区域
     * @param code   消息代码
     * @param params 消息参数
     * @return 国际化后的消息
     */
    public static String getI18nValue(Locale locale, String code, Object... params) {
        if (!enable) {
            return getDummyValue(code, params);
        }
        if (StringUtils.isEmpty(code)) {
            return code;
        }
        return messageResource.getMessage(code, params, code, locale);
    }

    /**
     * 未启用I18N功能时进行本地处理
     *
     * @param code   消息代码
     * @param params 消息参数
     * @return 本地处理后的信息
     */
    private static String getDummyValue(String code, Object... params) {
        if (Objects.isNull(params)) {
            return code;
        }
        initMessageFormatMap();
        return getMessageFormat(code).format(params);
    }

    private static MessageFormat getMessageFormat(String code) {
        if (messageFormatMap.containsKey(code)) {
            return messageFormatMap.get(code);
        }
        MessageFormat mf = new MessageFormat(code);
        synchronized (messageFormatMap) {
            messageFormatMap.put(code, mf);
        }
        return mf;
    }

    /**
     * 初始化 messageFormatMap
     */
    private static void initMessageFormatMap() {
        if (Objects.nonNull(messageFormatMap)) {
            return;
        }
        synchronized (I18nHolder.class) {
            if (Objects.nonNull(messageFormatMap)) {
                return;
            }
            messageFormatMap = new HashMap<String, MessageFormat>(1000);
        }
    }

    public static I18nMessageResource getMessageResource() {
        return messageResource;
    }

    public static I18nLocaleHolder getI18nLocaleHolder() {
        return i18nLocaleHolder;
    }
}
