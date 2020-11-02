package com.github.kindrat.cassandra.client.configuration;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.data.cassandra.config.CassandraEntityClassScanner;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;

import java.util.Collections;

//@SuppressWarnings("SpringJavaAutowiringInspection")
@Configuration
@EnableConfigurationProperties(CassandraProperties.class)
public class CassandraConfiguration implements BeanClassLoaderAware {

    private ClassLoader classLoader;

    @Bean
    @Lazy
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public CqlSessionFactoryBean session(String contactPoints, String keyspace) {
        CqlSessionFactoryBean session = new CqlSessionFactoryBean();
        session.setContactPoints(contactPoints);
        session.setKeyspaceName(keyspace);
        return session;
    }

    @Bean
    @Lazy
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public CassandraConverter cassandraConverter(CqlSession session, String keyspace) throws ClassNotFoundException {
        return new MappingCassandraConverter(cassandraMapping(session, keyspace));
    }

    @Bean
    @Lazy
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public CassandraMappingContext cassandraMapping(CqlSession cluster, String keyspace) throws ClassNotFoundException {
        CassandraMappingContext mappingContext = new CassandraMappingContext();
        mappingContext.setBeanClassLoader(classLoader);
        mappingContext.setInitialEntitySet(CassandraEntityClassScanner.scan(getEntityBasePackages()));
//        MappingCassandraConverter converter=new MappingCassandraConverter();
        CassandraCustomConversions customConversions = customConversions();
//        mappingContext.setCustomConversions(customConversions);
        mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
//        mappingContext.setUserTypeResolver(new SimpleUserTypeResolver(cluster, keyspace));
        return mappingContext;
    }

    @Bean
    public CassandraCustomConversions customConversions() {
        return new CassandraCustomConversions(Collections.emptyList());
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private String[] getEntityBasePackages() {
        return new String[]{getClass().getPackage().getName()};
    }
}
