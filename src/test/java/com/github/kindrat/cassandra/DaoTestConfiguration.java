package com.github.kindrat.cassandra;

import com.datastax.oss.protocol.internal.request.query.QueryOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SessionBuilderConfigurer;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.cassandra.repository.support.SimpleCassandraRepository;

import javax.annotation.Nonnull;

import java.net.SocketOptions;

import static org.springframework.beans.BeanUtils.instantiateClass;

@EntityScan("com.github.kindrat.cassandra")
@EnableConfigurationProperties(CassandraProperties.class)
@Configuration
@RequiredArgsConstructor
@EnableCassandraRepositories(repositoryBaseClass = SimpleCassandraRepository.class)
@AutoConfigureDataJpa
public class DaoTestConfiguration extends AbstractCassandraConfiguration {

    @Override
    protected String getKeyspaceName() {
        return "keyspace";
    }
}
