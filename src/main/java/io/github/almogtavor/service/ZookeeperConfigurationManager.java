package io.github.almogtavor.service;

import lombok.RequiredArgsConstructor;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ZookeeperConfigurationManager {
    public static final String ZOOKEEPER_FILES_ZIP_FILENAME = "zookeeper_files.zip";
    public static final int ZK_SESSION_TIMEOUT = 30000;
    public static final String CONFIGS_DIR_PATH = "/configs/";
    private final ZookeeperFilesManager zookeeperFileService;

    private ByteArrayOutputStream createZNodesPathsZip(String zkHost, List<String> paths) throws IOException, KeeperException, InterruptedException {
        try (ZooKeeper zkClient = new ZooKeeper(zkHost, ZK_SESSION_TIMEOUT, null)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                for (String path : paths) {
                    List<String> zookeeperFiles = zookeeperFileService.getZookeeperFiles(path, zkClient);
                    zookeeperFileService.addFilesToZip(path, zookeeperFiles, zipOutputStream, zkClient);
                }
            }
            return byteArrayOutputStream;
        }
    }

    private ResponseEntity<byte[]> wrapZipWithResponse(ByteArrayOutputStream byteArrayOutputStream, List<String> paths) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", paths.size() == 1 && !paths.get(0).equals("/") ? paths.get(0).substring(CONFIGS_DIR_PATH.length()) + ".zip" : ZOOKEEPER_FILES_ZIP_FILENAME);
        return ResponseEntity.ok().headers(headers).body(byteArrayOutputStream.toByteArray());
    }

    public ResponseEntity<byte[]> downloadAllZkData(String zkHost) throws IOException, KeeperException, InterruptedException {
        List<String> paths = Collections.singletonList("/");
        return wrapZipWithResponse(createZNodesPathsZip(zkHost, paths), paths);
    }

    /**
     * @param dirs This param specifies which ZNode are wanted by the user to get downloaded.
     *             The {@link ZookeeperConfigurationManager#createZNodesPathsZip} downloads all ZNodes that are children to these paths.
     */
    public ResponseEntity<byte[]> downloadConfDirs(String zkHost, List<String> dirs) throws IOException, KeeperException, InterruptedException {
        List<String> paths = dirs.stream().map(dir -> CONFIGS_DIR_PATH + dir).toList();
        return wrapZipWithResponse(createZNodesPathsZip(zkHost, paths), paths);
    }

    public ResponseEntity<String> uploadConfDir(String host, String dirName, byte[] fileData) throws IOException, InterruptedException, KeeperException {
        try (ZooKeeper zkClient = new ZooKeeper(host, ZK_SESSION_TIMEOUT, null)) {
            if (fileData.length > 0) {
                // Unzip the uploaded file and get its contents
                extractAndUploadZip(fileData, dirName, zkClient);
                return ResponseEntity.ok("Files successfully uploaded to ZooKeeper");
            } else {
                return ResponseEntity.badRequest().body("No file data provided");
            }
        }
    }

    public void copyConfDirs(String sourceZkHost, String targetZkHost, List<String> dirs, Map<String, String> dirMap) throws IOException, KeeperException, InterruptedException {
        try (ZooKeeper sourceZkClient = new ZooKeeper(sourceZkHost, ZK_SESSION_TIMEOUT, null);
             ZooKeeper targetZkClient = new ZooKeeper(targetZkHost, ZK_SESSION_TIMEOUT, null)) {
            for (String sourceDir : dirs) {
                String targetDir = (dirMap != null && dirMap.containsKey(sourceDir)) ? dirMap.get(sourceDir) : sourceDir;
                List<String> sourceFiles = sourceZkClient.getChildren(CONFIGS_DIR_PATH + sourceDir, null);
                zookeeperFileService.copyFilesToTargetZk(CONFIGS_DIR_PATH + sourceDir, CONFIGS_DIR_PATH + targetDir, sourceFiles, sourceZkClient, targetZkClient);
            }
        }
    }

    public Mono<List<String>> viewDirs(String zkHost, String basePath) throws InterruptedException, KeeperException {
        if (basePath == null) basePath = "/";
        SolrZkClient zkClient = new SolrZkClient(zkHost, ZK_SESSION_TIMEOUT, ZK_SESSION_TIMEOUT);
        ZkStateReader zkStateReader = new ZkStateReader(zkClient);
        List<String> dirs;
        try {
            zkStateReader.createClusterStateWatchersAndUpdate();
            dirs = zkStateReader.getZkClient().getChildren(basePath, null, true);
        } finally {
            zkStateReader.close();
            zkClient.close();
        }
        return Mono.just(dirs);
    }

    public void extractAndUploadZip(byte[] fileData, String dirName, ZooKeeper zkClient) throws IOException, KeeperException, InterruptedException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(fileData))) {
            String rootDir = "/configs" + (Optional.ofNullable(dirName).isPresent() ? "/" + dirName : "");
            // Ensure the root directory node exists
            if (Optional.ofNullable(dirName).isPresent() && zkClient.exists(rootDir, false) == null) {
                zkClient.create(rootDir, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            for (ZipEntry zipEntry = zipInputStream.getNextEntry(); zipEntry != null; zipEntry = zipInputStream.getNextEntry()) {
                if (zipEntry.isDirectory()) continue;

                byte[] entryBytes = zipInputStream.readAllBytes();
                String[] parts = zipEntry.getName().split("/");
                String currentPart = rootDir;

                // Create each part of the node hierarchy if it doesn't exist
                for (int i = 0; i < parts.length - 1; i++) {
                    currentPart += "/" + parts[i];
                    if (zkClient.exists(currentPart, false) == null) {
                        zkClient.create(currentPart, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    }
                }

                // Upload to ZooKeeper
                zkClient.create(currentPart + "/" + parts[parts.length - 1], entryBytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }
    }

    public ResponseEntity<String> uploadZkConfFile(String host, String confDir, String fileName, byte[] fileData) throws IOException, InterruptedException, KeeperException {
        try (ZooKeeper zkClient = new ZooKeeper(host, ZK_SESSION_TIMEOUT, null)) {
            String znodePath = CONFIGS_DIR_PATH + confDir + "/" + fileName;

            // Check if the znode exists and create it if not
            if (zkClient.exists(znodePath, false) == null) {
                zkClient.create(znodePath, fileData, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                zkClient.setData(znodePath, fileData, zkClient.exists(znodePath, false).getVersion());
            }

            return ResponseEntity.ok("File successfully uploaded to ZooKeeper");
        }
    }
}
