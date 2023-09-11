package com.bdkjzx.project.i18n.repository;

import java.util.Locale;
import java.util.Map;

/**
 * 定义接口，引用方可以实现这个接口来传入自己的国际化常量内容<br>
 * 如果自己实现了其他的Service，要求缓存
 **/
public interface I18nConfigDbLoader {
    /**
     * 载入国际化的常量配置
     * @return
     */
    Map<Locale, Map<String,String>> loadI18nDictByLocaleEntity();

    /**
     * 刷新缓存（如果有缓存）
     */
    void cacheEvict();


    /**
     * 将无效的编码记录下来，以便维护
     * @param code 编码
     */
    void markInvalidCode(String code);
}
