package io.github.almogtavor.service;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZookeeperFilesManager {

    public List<String> getZookeeperFiles(String path, ZooKeeper zkClient) throws KeeperException, InterruptedException {
        return zkClient.getChildren(path, null);
    }

    private String buildFilePath(String basePath, String file) {
        return basePath.equals("/") ? basePath + file : basePath + "/" + file;
    }

    public void addFilesToZip(String basePath, List<String> files, ZipOutputStream zipOutputStream, ZooKeeper zkClient) throws InterruptedException, KeeperException, IOException {
        for (String file : files) {
            String filePath = buildFilePath(basePath, file);
            Stat stat = zkClient.exists(filePath, false);
            if (stat == null || stat.getNumChildren() != 0) {
                addFilesToZip(filePath, zkClient.getChildren(filePath, null), zipOutputStream, zkClient);
            } else {
                byte[] fileData = zkClient.getData(filePath, false, null);
                if (fileData == null) continue;

                ZipEntry zipEntry = new ZipEntry(basePath.equals("/") ? file : filePath.substring(1));
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(fileData);
                zipOutputStream.closeEntry();
            }
        }
    }

    public void copyFilesToTargetZk(String sourcePath, String targetPath, List<String> files, ZooKeeper sourceZkClient, ZooKeeper targetZkClient) throws KeeperException, InterruptedException {
        for (String file : files) {
            String sourceFilePath = buildFilePath(sourcePath, file);
            String targetFilePath = buildFilePath(targetPath, file);
            Stat stat = sourceZkClient.exists(sourceFilePath, false);
            if (stat == null || stat.getNumChildren() != 0) {
                copyFilesToTargetZk(sourceFilePath, targetFilePath, sourceZkClient.getChildren(sourceFilePath, null), sourceZkClient, targetZkClient);
            } else {
                byte[] fileData = sourceZkClient.getData(sourceFilePath, false, null);
                if (fileData == null) continue;

                String parentDir = targetFilePath.substring(0, targetFilePath.lastIndexOf('/'));
                if (targetZkClient.exists(parentDir, false) == null) {
                    targetZkClient.create(parentDir, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                targetZkClient.create(targetFilePath, fileData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }
    }
}
