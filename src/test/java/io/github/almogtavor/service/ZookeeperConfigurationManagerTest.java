package io.github.almogtavor.service;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZookeeperConfigurationManagerTest {
    @Mock
    private ZooKeeper zkClient;
    @Mock
    private ZookeeperFilesManager zookeeperFilesManager;
    @InjectMocks
    private ZookeeperConfigurationManager zookeeperConfigurationManager;

    @Test
    void shouldDownloadAllZkData() throws KeeperException, InterruptedException, IOException {
        // Given
        String zkHost = "testHost";
        List<String> paths = Collections.singletonList("/");
        byte[] mockData = "mockData".getBytes();

        when(zookeeperFilesManager.getZookeeperFiles(anyString(), any(ZooKeeper.class))).thenReturn(Collections.singletonList("mockFile"));

        try (MockedConstruction<ZooKeeper> mocked = Mockito.mockConstruction(ZooKeeper.class,
                (mock, context) -> when(mock.getChildren(anyString(), anyBoolean())).thenReturn(Collections.singletonList("mockFile")))) {

            // When
            ResponseEntity<byte[]> responseEntity = zookeeperConfigurationManager.downloadAllZkData(zkHost);

            // Then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        }
    }

    @Test
    void shouldUploadConfDirsSuccessfully() throws KeeperException, InterruptedException, IOException {
        // Given
        String host = "testHost";
        byte[] fileData = "mockData".getBytes();

        try (MockedConstruction<ZooKeeper> mocked = Mockito.mockConstruction(ZooKeeper.class)) {
            // When
            ResponseEntity<String> responseEntity = zookeeperConfigurationManager.uploadConfDirs(host, fileData);

            // Then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isEqualTo("Files successfully uploaded to ZooKeeper");
        }
    }

    @Test
    void shouldHandleNoDataInUploadConfDirs() throws KeeperException, InterruptedException, IOException {
        // Given
        String host = "testHost";
        byte[] fileData = new byte[0];

        try (MockedConstruction<ZooKeeper> mocked = Mockito.mockConstruction(ZooKeeper.class)) {
            // When
            ResponseEntity<String> responseEntity = zookeeperConfigurationManager.uploadConfDirs(host, fileData);

            // Then
            assertThat(responseEntity).isNotNull();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(responseEntity.getBody()).isEqualTo("No file data provided");
        }
    }

}