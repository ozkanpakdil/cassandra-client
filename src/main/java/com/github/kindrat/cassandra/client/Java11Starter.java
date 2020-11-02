package com.github.kindrat.cassandra.client;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Java11Starter {
    public static String driver;

    public static void main(String[] args) throws IOException {
        String[] classpath = System.getProperty("java.class.path").split(":");

        driver = Stream.of(classpath).filter(it -> it.contains("java-driver-core")).findFirst().get();
        driver = driver.substring(driver.lastIndexOf("-") + 1);
        driver = driver.replace(".jar", "");
        System.out.println(driver);

        CqlSession s = CqlSession.builder().build();
        s.execute("DESC KEYSPACES;").all().forEach(i -> {
            System.out.println(i.getFormattedContents());
        });

        CassandraClientGUI.main(args);
    }
}
