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
public class ZookeeperFileService {

    public List<String> getZookeeperFiles(String path, ZooKeeper zkClient) throws KeeperException, InterruptedException {
        return zkClient.getChildren(path, null);
    }

    public void addFilesToZip(String basePath, List<String> files, ZipOutputStream zipOutputStream, ZooKeeper zkClient) throws IOException, KeeperException, InterruptedException {
        for (String file : files) {
            String filePath = basePath.equals("/") ? basePath + file : basePath + "/" + file;
            Stat stat = zkClient.exists(filePath, false);
            if (stat != null) {
                if (stat.getNumChildren() == 0) {
                    // Node is a file
                    byte[] fileData = zkClient.getData(filePath, false, null);
                    if (fileData != null) {
                        String zipEntryPath = basePath.equals("/") ? file : filePath.substring(1);
                        ZipEntry zipEntry = new ZipEntry(zipEntryPath);
                        zipOutputStream.putNextEntry(zipEntry);
                        zipOutputStream.write(fileData);
                        zipOutputStream.closeEntry();
                    }
                } else {
                    // Node is a directory
                    List<String> subFiles = getZookeeperFiles(filePath, zkClient);
                    addFilesToZip(filePath, subFiles, zipOutputStream, zkClient);
                }
            }
        }
    }

    public void copyFilesToTargetZk(String sourcePath, String targetPath, List<String> files, ZooKeeper sourceZkClient, ZooKeeper targetZkClient) throws IOException, KeeperException, InterruptedException {
        for (String file : files) {
            String sourceFilePath = sourcePath.equals("/") ? sourcePath + file : sourcePath + "/" + file;
            String targetFilePath = targetPath.equals("/") ? targetPath + file : targetPath + "/" + file;
            Stat stat = sourceZkClient.exists(sourceFilePath, false);
            if (stat != null) {
                if (stat.getNumChildren() == 0) {
                    byte[] fileData = sourceZkClient.getData(sourceFilePath, false, null);
                    if (fileData != null) {
                        // Create the parent node if it does not exist
                        String parentDir = targetFilePath.substring(0, targetFilePath.lastIndexOf('/'));
                        if (targetZkClient.exists(parentDir, false) == null) {
                            targetZkClient.create(parentDir, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                        }
                        // Create the child node
                        targetZkClient.create(targetFilePath, fileData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    }
                } else {
                    List<String> subFiles = sourceZkClient.getChildren(sourceFilePath, null);
                    copyFilesToTargetZk(sourceFilePath, targetFilePath, subFiles, sourceZkClient, targetZkClient);
                }
            }
        }
    }
}
