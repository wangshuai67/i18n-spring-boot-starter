package com.bdkjzx.project.i18n.config;


import com.bdkjzx.project.i18n.I18nHolder;
import com.bdkjzx.project.i18n.filter.I18nFilter;


import com.bdkjzx.project.i18n.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.bdkjzx.project.i18n.interceptor.I18nInterceptor;

import javax.sql.DataSource;
import java.util.Locale;
import java.util.concurrent.Executor;


@Configuration
@EnableConfigurationProperties({I18nProperties.class})
@Slf4j
public class I18nAutoConfig {
    @Bean
    public I18nHolder getI18nUtil(@Autowired(required = false) I18nMessageResource messageSource,
                                  @Autowired(required = false) I18nLocaleHolder i18nLocaleHolder,
                                  @Autowired I18nProperties i18NProperties) {
        // 不论是否启用都会配置，保证这个工具类不会报错
        return i18NProperties.getEnable() ? new I18nHolder(messageSource, i18nLocaleHolder) : new I18nHolder();
    }

    @ConditionalOnProperty(prefix = "spring.ex.i18n", name = "enable", havingValue = "true")
    @Configuration
    static class I18nFilterConfig {

        @Autowired
        private I18nLocaleHolder i18nLocaleHolder;

        @Bean
        public I18nFilter i18nFilter() {
            I18nFilter i18nFilter = new I18nFilter();

            I18nInterceptor interceptor = new I18nInterceptor();
            interceptor.setI18nLocaleHolder(i18nLocaleHolder);
            i18nFilter.setI18nInterceptor(interceptor);
            return i18nFilter;
        }
    }

    @ConditionalOnProperty(prefix = "spring.ex.i18n", name = "enable", havingValue = "true")
    @Configuration
    @EnableCaching
    @ComponentScan("com.bdkjzx.project.i18n")
    static class I18nResourceConfig {

        /**
         * 采用默认的配置文件配置 messages开头的文件，编码为utf8<br>
         * 如 messages_zh_CN.properties ,  messages_en_US.properties
         *
         * @return {@link MessageSourceProperties}
         */
        @Bean
        public MessageSourceProperties messageSourceProperties() {
            return new MessageSourceProperties();
        }

        @Bean
        public ResourceBundleMessageSource initResourceBundleMessageSource(MessageSourceProperties messageSourceProperties) {
            ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
            resourceBundleMessageSource.setBasename(messageSourceProperties.getBasename());
            resourceBundleMessageSource.setDefaultEncoding(messageSourceProperties.getEncoding().name());
            return resourceBundleMessageSource;
        }

        @Bean
        @Autowired
        public I18nMessageResource initMessageResource(ResourceBundleMessageSource resourceBundleMessageSource,
                                                       I18nLocaleHolder i18NLocaleSettings) {
            I18nMessageResource i18nMessageResource = new I18nMessageResource(i18NLocaleSettings.getDefaultLocale());
            i18nMessageResource.setParentMessageSource(resourceBundleMessageSource);
            return i18nMessageResource;
        }

        @Bean
        @Autowired
        public I18nLocaleHolder getI18nLocaleSetting(I18nProperties i18nProperties) {
            Locale locale;
            try {
                locale = new Locale.Builder()
                        .setLanguageTag(i18nProperties.getDefaultLocale().replace("_", "-").toLowerCase())
                        .build();
            } catch (Exception e) {
                log.error(String.format("解析默认语言时出现错误, setting = %s", i18nProperties.getDefaultLocale()), e);
                throw new IllegalArgumentException("解析默认语言时出现错误，请查看日志");
            }
            return new I18nLocaleHolder(locale);
        }

        @Bean(name = "i18nJdbcTemplate")
        @ConditionalOnMissingBean(name = "i18nJdbcTemplate")
        public JdbcTemplate getJdbcTemplate(@Autowired(required = false) @Qualifier("i18nDataSource") DataSource i18nDataSource) {
            try {
                if (i18nDataSource == null) {
                    log.error("未配置国家化数据源,请使用@Bean构造一个名为i18nDataSource的DataSource或者直接重新此方法");
                }
                return new JdbcTemplate(i18nDataSource);
            } catch (BeansException e) {
                log.error("无效的数据源{}", i18nDataSource, e);
                throw new IllegalArgumentException("创建数据源时出现错误，请查看日志");
            }
        }

        @Autowired
        @Bean(name = "defaultI18nDataLoadService")
        public I18nConfigDbLoader getI18nDataLoadService(I18nProperties i18nProperties,
                                                         @Qualifier("i18nJdbcTemplate") JdbcTemplate jdbcTemplate) {
            return new SimpleI18NConfigDbLoaderImpl(i18nProperties.getConfigCodeColumn(),
                    i18nProperties.getConfigTable(), jdbcTemplate);
        }

        @Autowired
        @Bean(name = "i18nCacheManager")
        public CacheManager getCacheManager(I18nProperties i18nProperties) {
            CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
            if (i18nProperties.getCacheHours() > 0) {
                // 缓存创建后，经过固定时间（小时），更新
                caffeineCacheManager.setCacheSpecification(String.format("refreshAfterWrite=%sH", i18nProperties.getCacheHours()));
            }
            return caffeineCacheManager;
        }

        /**
         * 线程池配置
         */
        @ConditionalOnProperty(prefix = "spring.ex.i18n", name = "mark", havingValue = "true")
        @Configuration
        @EnableAsync
        static class I18nInvalidMarkerConfig {

            @Bean("i18nExecutor")
            @Autowired
            public Executor getAsyncExecutor(I18nProperties i18NProperties) {
                ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
                executor.setCorePoolSize(0);
                executor.setMaxPoolSize(2);
                executor.setQueueCapacity(i18NProperties.getMarkPoolSize());
                executor.setThreadNamePrefix("i18n-executor-");
                executor.initialize();
                return executor;
            }

            @Bean
            public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return new SimpleAsyncUncaughtExceptionHandler();
            }

        }

    }

}
