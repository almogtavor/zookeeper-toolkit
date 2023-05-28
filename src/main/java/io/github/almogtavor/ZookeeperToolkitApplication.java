package io.github.almogtavor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class, SolrAutoConfiguration.class})
public class ZookeeperToolkitApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZookeeperToolkitApplication.class, args);
    }
}
