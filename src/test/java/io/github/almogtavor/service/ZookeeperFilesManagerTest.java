package io.github.almogtavor.service;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class ZookeeperFilesManagerTest {

    @InjectMocks
    private ZookeeperFilesManager zookeeperFileService;

    @Mock
    private ZooKeeper sourceZkClient;

    @Mock
    private ZooKeeper targetZkClient;

    @Test
    void testCopyFilesToTargetZk() throws Exception {
        // Given
        String sourcePath = "/source";
        String targetPath = "/target";
        String fileName = "testFile";
        byte[] fileData = "testData".getBytes();
        Stat fileStat = new Stat();
        fileStat.setNumChildren(0);

        // When
        when(sourceZkClient.exists(sourcePath + "/" + fileName, false)).thenReturn(fileStat);
        when(sourceZkClient.getData(sourcePath + "/" + fileName, false, null)).thenReturn(fileData);
        when(targetZkClient.exists(targetPath, false)).thenReturn(null);

        // Call the method under test
        zookeeperFileService.copyFilesToTargetZk(sourcePath, targetPath, Arrays.asList(fileName), sourceZkClient, targetZkClient);

        // Then
        verify(targetZkClient, times(1)).create(targetPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        verify(targetZkClient, times(1)).create(targetPath + "/" + fileName, fileData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }
}