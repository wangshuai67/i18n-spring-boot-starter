package com.bdkjzx.project.i18n.config;

import com.bdkjzx.project.i18n.I18nHolder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "spring.ex.i18n")
@Setter
@Getter
public class I18nProperties {

    /**
     * 是否启用国际化功能：<br>
     *     - 启用：会创建和国际化相关的数据源、缓存等等；<br>
     *     - 不启用：{@link I18nHolder} 可以正常使用，返回原值，不会创建国际化相关的各种Bean<br>
     *
     *  默认：不启用，需要手动开启
     */
    private Boolean enable = false;

    /**
     * 国际化数据表所在的数据源，入需指定，则写入数据源名称。<br>
     *     此配置的作用是允许多个服务通过共享一个i18n配置表，从而共用一套i18n翻译。<br>
     *     默认为空，表示使用primary数据源。
     */
    private String dataSource = "primary";

    /**
     * 默认地区（语言）
     */
    private String defaultLocale = "zh_CN";

    /**
     * 查询i18n配置表的名称，用于自定义修改表。<br>
     *     默认：config_i18n_message
     */
    private String configTable = "config_i18n_message";

    /**
     * i18n配置表的字段名。根据i18n配置表决定此配置<br>
     *  默认：code
     */
    private String configCodeColumn = "code";

    /**
     * i18n缓存更新时间（小时数），会提供手工刷新缓存的入口，所以不必频繁刷新<br>
     *     默认值为-1，表示长期有效。<br>
     */
    private Integer cacheHours = -1;

    /**
     * 当未找到i18n的code时，是否将其记录到表中，以便统一处理<br>
     *     默认：关闭
     */
    private Boolean mark = false;

    /**
     * 用于记录无效code的线程池缓冲区大小
     */
    private Integer markPoolSize = 2000;

    /**
     * 是否在 {@link com.bdkjzx.project.i18n.repository.I18nMessageResource} 未找到配置时，再使用Spring默认方案，
     *     从本地加载国际化资源。
     *  默认：关闭
     */
    private Boolean useLocale = false;


}
