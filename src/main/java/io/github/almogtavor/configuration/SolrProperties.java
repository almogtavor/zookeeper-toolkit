package io.github.almogtavor.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "solr")
@Data
public class SolrProperties {
  private String zkHost;
  private String collection;
  private String defaultField;
  private int maxRows;
  private int start;
  private String hlFields;
  private String hlSimplePre;
  private String hlSimplePost;
  private int hlFragSize;
  private boolean hlRequireFieldMatch;
  private boolean hlUseFastVectorHighlighter;
  private boolean hlUsePhraseHighlighter;
  private String solrUrl;
  private String solrHome;
  private String solrXml;
  private String schemaXml;
  private int maxConnectionsPerHost;
  private int maxTotalConnections;
  private int connectionTimeout;
  private int maxRetries;
  private boolean followRedirects;
  private String basicAuthUsername;
  private String basicAuthPassword;
  private String sslKeyStore;
  private String sslKeyStorePassword;
  private String sslTrustStore;
  private String sslTrustStorePassword;
  private boolean allowCompression;
  private boolean useMultiPartPost;
  private boolean useAuth;
  private String updateHandler;
  private String updateLog;
  private int maxWarmingSearchers;
  private int searcherExecutorPoolSize;
  private int searcherExecutorMaxPoolSize;
  private int searcherExecutorQueueSize;
  private boolean useColdSearcher;
  private int minIdleThreadCount;
  private int maxIdleThreadCount;
  private int maxThreadCount;
  private int minThreadCount;
  private int threadIdleTime;
}
