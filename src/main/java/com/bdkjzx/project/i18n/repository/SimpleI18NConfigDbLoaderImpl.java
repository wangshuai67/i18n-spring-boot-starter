package com.bdkjzx.project.i18n.repository;


import com.bdkjzx.project.i18n.I18nHolder;
import com.bdkjzx.project.i18n.config.I18nProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import sun.util.locale.LanguageTag;

import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 国际化加载，从数据库，外部可以指定 数据源，查询SQL及主键列名称
 **/
@CacheConfig(cacheNames = "I18n", cacheManager = "i18nCacheManager")
public class SimpleI18NConfigDbLoaderImpl implements I18nConfigDbLoader {


    private static final Logger log = LoggerFactory.getLogger(SimpleI18NConfigDbLoaderImpl.class);

    private final String configTable;
    private final String columnNameCode;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 插入配置表的SQL模板
     */
    private String insertSqlTemplate;
    /**
     * 配置表中的字段个数
     */
    private Integer countOfColumns;
    /**
     * 配置表的表名
     */
    private String schemaAndTableName;
    /**
     * 查询字段的SQL
     */
    private static final String SCHEMA_SQL_TEMPLATE = "SELECT column_name FROM information_schema.COLUMNS " +
            "WHERE UPPER(TRIM(table_name)) = '%s' AND UPPER(TRIM(table_schema)) = '%s' ORDER BY ORDINAL_POSITION ASC";


    @Autowired
    private I18nProperties i18NProperties;

    @Autowired
    private List<I18nConfigDbLoader> i18NConfigDbLoaderList;
    public SimpleI18NConfigDbLoaderImpl(String configCodeColumn,
                                        String configTable,
                                        JdbcTemplate jdbcTemplate) {
        this.columnNameCode = configCodeColumn;
        this.configTable = configTable;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @CacheEvict
    public void cacheEvict() {
        // 使用缓存抽象更新缓存
    }

    @Cacheable
    @Override
    public Map<Locale,Map<String,String>> loadI18nDictByLocaleEntity() {

        Map<Locale,Map<String,String>> localeKvMap = new HashMap<>(8);

        if (log.isDebugEnabled()) {
            log.debug("==== 更新i18n设置 ====");
        }

        jdbcTemplate.query(String.format("select * from %s", configTable), (ResultSet resultSet) -> {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            // 过滤掉时间字段
            List<String> columnNameList = new LinkedList<>();
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                if (!I18nHolder.COLUMN_NAME_MODIFIED_TIME.equalsIgnoreCase(resultSetMetaData.getColumnName(i))) {
                    columnNameList.add(resultSetMetaData.getColumnName(i));
                }
            }
            int colCount = columnNameList.size();
            @SuppressWarnings({"rowtypes", "unchecked"})
            HashMap<String, String>[] kvMapArray = new HashMap[colCount-1];
            if(!columnNameCode.equals(resultSetMetaData.getColumnName(1).toLowerCase())){
                log.error("i18n_message 表格中第一列的列名称必须是: {}", columnNameCode);
                throw new SQLException(String.format("i18n_message 表格中第一列的列名称必须是: %s", columnNameCode));
            }

            //数据库表中的字段不区分大小写，如果只有语言，应该是两个字符，如果是语言加国家，应该是语言-国家，有个中划线
            for(int i=2; i<=colCount; i++){
                String columnNameOfLang = columnNameList.get(i-1).toLowerCase();
                @SuppressWarnings({"rowtypes"})
                HashMap<String, String> kvMap = new HashMap<>(8);
                kvMapArray[i-2] = kvMap;
                //切分字符串，拼装为Locale
                LanguageTag languageTag = LanguageTag.parse(columnNameOfLang, null);
                Locale locale = new Locale(languageTag.getLanguage(),languageTag.getRegion());
                if (log.isDebugEnabled()) {
                    log.debug("i18n_message中列名{}解析为语言类型为:{},({})", i, locale.toString(), locale.getDisplayName());
                }
                localeKvMap.put(locale, kvMap);
            }
            do{
                String code = resultSet.getString(1);
                for(int i=2;i<=colCount;i++){
                    kvMapArray[i-2].put(code,resultSet.getString(i));
                }
            } while (resultSet.next());
        });

        return localeKvMap;
    }

    /**
     * 这个配置的目的是获取i18n配置表的插入SQL模板，以便将未找到配置的code保存到表里
     */
    @PostConstruct
    public void init() {
        try {
            String tableSchema = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection().getCatalog();
            String tableName = i18NProperties.getConfigTable();
            String sql = String.format(SCHEMA_SQL_TEMPLATE, tableName.trim().toUpperCase(), tableSchema.trim().toUpperCase());
            List<String> columnList = jdbcTemplate.query(sql, (ResultSet resultSet, int i) -> resultSet.getString(1))
                    .stream()
                    .filter(column -> !I18nHolder.COLUMN_NAME_MODIFIED_TIME.equalsIgnoreCase(column))
                    .collect(Collectors.toList());
            if (log.isDebugEnabled()) {
                log.debug("i18n 配置表中的字段：{}", columnList);
            }
            // 用配置表中的字段生成插入脚本
            List<String> saveColumnList = columnList.stream()
                    .map(column -> "`" + column + "`")
                    .collect(Collectors.toList());
            List<String> valueList = columnList.stream().map(column -> "?").collect(Collectors.toList());
            this.insertSqlTemplate = "insert into `" + tableName + "` " +
                    "(" + String.join(", ", saveColumnList) + ") " +
                    "values (" + String.join(", ", valueList) + ")";
            this.countOfColumns = columnList.size();
            this.schemaAndTableName = tableSchema + "." + tableName;
            if (log.isDebugEnabled()) {
                log.debug("i18n 配置表插入SQL模板：{}", insertSqlTemplate);
            }
        } catch (Exception e) {
            log.error("查询i18n配置表时出现错误：", e);
        }

    }

    /**
     * 使用国际化模块自己的线程池，异步保存
     * @param code 编码
     */
    @Async("i18nExecutor")
    @Override
    public void markInvalidCode(String code) {
        try {
            log.info("记录新增国际化代码：{}，稍后请在 {} 表中翻译，并刷新缓存。", code, this.schemaAndTableName);
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertSqlTemplate);
                IntStream.range(0, this.countOfColumns).forEach(i -> {
                    try {
                        ps.setString(i + 1, code);
                    } catch (SQLException e) {
                        log.debug("插入国际化代码时遇到错误", e);
                    }
                });
                return ps;
            });
            // 插入完成后，刷新一下缓存
            for (I18nConfigDbLoader i18NConfigDbLoader : i18NConfigDbLoaderList) {
                i18NConfigDbLoader.cacheEvict();
                i18NConfigDbLoader.loadI18nDictByLocaleEntity();
            }
        } catch (Exception e) {
            log.debug(String.format("无法插入国际化代码：%s", code), e);
        }
    }
}
