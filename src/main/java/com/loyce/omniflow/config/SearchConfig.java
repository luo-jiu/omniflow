package com.loyce.omniflow.config;

import com.loyce.omniflow.service.TagService;
import com.loyce.omniflow.service.impl.tag.ElasticsearchTagServiceImpl;
import com.loyce.omniflow.service.impl.tag.MysqlTagServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 搜索条件配置选择
 */
@Configuration
public class SearchConfig {

    @Bean
    @ConditionalOnProperty(name = "search.engine", havingValue = "mysql", matchIfMissing = true)
    public TagService mysqlTagService() {
        return new MysqlTagServiceImpl();
    }

    @Bean
    @ConditionalOnProperty(name = "search.engine", havingValue = "elasticsearch")
    public TagService elasticsearchTagService() {
        return new ElasticsearchTagServiceImpl();
    }
}
