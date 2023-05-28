package io.github.almogtavor.service;

import io.github.almogtavor.service.ZookeeperDownloadUploadService;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ZookeeperDownloadUploadServiceTest {
    private ZookeeperDownloadUploadService controller;

    @Mock
    private SolrZkClient mockZooKeeper;

    @Mock
    private ZkStateReader mockZkStateReader;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new ZookeeperDownloadUploadService(null, null, null);
    }

    @Test
    void testViewDirs() throws KeeperException, InterruptedException {
        String zkHost = "localhost:2181";
        List<String> expectedDirs = Arrays.asList("configs", "zookeeper", "overseer", "aliases.json", "live_nodes", "collections", "overseer_elect", "security.json", "clusterstate.json", "autoscaling", "autoscaling.json");

        // Mocking ZooKeeper and ZkStateReader
        when(mockZkStateReader.getZkClient()).thenReturn(mockZooKeeper);
        when(mockZkStateReader.getZkClient().getChildren("/", null, true)).thenReturn(expectedDirs);

        try {
            // Calling the viewDirs method
            Mono<List<String>> result = controller.viewDirs(zkHost, "/");

            // Verifying the interactions and asserting the result
//            verify(mockZkStateReader).createClusterStateWatchersAndUpdate();
            verify(mockZkStateReader).getZkClient();
            assertEquals(expectedDirs, result.block());
        } finally {
            // Ensuring the resources are closed
            mockZkStateReader.close();
            mockZooKeeper.close();
        }
    }
}
