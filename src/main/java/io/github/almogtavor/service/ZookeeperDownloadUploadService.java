package io.github.almogtavor.service;

import lombok.RequiredArgsConstructor;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ZookeeperDownloadUploadService {

    private final ZookeeperFileService zookeeperFileService;
    private final ZookeeperClientService zookeeperClientService;
    private final ZipService zipService;

    public ResponseEntity<byte[]> downloadAllZkData(String zkHost) throws IOException, KeeperException, InterruptedException {
        ZooKeeper zkClient = zookeeperClientService.createZookeeperClient(zkHost);
        try {
            // Retrieve the list of ZooKeeper files
            List<String> zookeeperFiles = zookeeperFileService.getZookeeperFiles("/", zkClient);

            // Generate the zip file
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                // Add ZooKeeper files to the zip
                zookeeperFileService.addFilesToZip("/", zookeeperFiles, zipOutputStream, zkClient);
            }

            // Set the response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "zookeeper_files.zip");

            // Return the zip file as the response
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(byteArrayOutputStream.toByteArray());
        } finally {
            zookeeperClientService.closeZookeeperClient(zkClient);
        }
    }

    public ResponseEntity<byte[]> downloadConfDirs(String zkHost, List<String> dirs) throws IOException, KeeperException, InterruptedException {
        ZooKeeper zkClient = zookeeperClientService.createZookeeperClient(zkHost);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                for (String dir : dirs) {
                    List<String> zookeeperFiles = zookeeperFileService.getZookeeperFiles("/configs/" + dir, zkClient);
                    zookeeperFileService.addFilesToZip(dir, zookeeperFiles, zipOutputStream, zkClient);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "zookeeper_files.zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(byteArrayOutputStream.toByteArray());
        } finally {
            zookeeperClientService.closeZookeeperClient(zkClient);
        }
    }

    public ResponseEntity<String> uploadConfDirs(String zkHost, MultipartFile file) throws IOException, KeeperException, InterruptedException {
        if (!file.isEmpty()) {
            ZooKeeper zkClient = zookeeperClientService.createZookeeperClient(zkHost);
            try {
                // Unzip the uploaded file and get its contents
                extractAndUploadZip(file, zkClient);
                return ResponseEntity.ok("Files successfully uploaded to ZooKeeper");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
            } finally {
                zookeeperClientService.closeZookeeperClient(zkClient);
            }
        } else {
            return ResponseEntity.badRequest().body("No file uploaded");
        }
    }

    public void uploadFileToZk(MultipartFile file, ZooKeeper zkClient, String dirName) throws IOException, KeeperException, InterruptedException {
        String entryName = file.getOriginalFilename();

        if (entryName != null && !entryName.isEmpty()) {
            // Upload the file bytes to the corresponding path in ZooKeeper
            zkClient.create("/configs/" + dirName + "/" + entryName, file.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    public void extractAndUploadZip(MultipartFile file, ZooKeeper zkClient) throws IOException, KeeperException, InterruptedException {
        // Unzip the uploaded file and get its contents
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(file.getBytes()));
        ZipEntry zipEntry = zipInputStream.getNextEntry();

        while (zipEntry != null) {
            String entryName = zipEntry.getName();

            if (!zipEntry.isDirectory()) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;

                while ((len = zipInputStream.read(buffer)) > 0) {
                    byteArrayOutputStream.write(buffer, 0, len);
                }

                byte[] entryBytes = byteArrayOutputStream.toByteArray();
                // Upload the entry bytes to the corresponding path in ZooKeeper
                zkClient.create("/configs/" + entryName, entryBytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

                byteArrayOutputStream.close();
            }

            zipInputStream.closeEntry();
            zipEntry = zipInputStream.getNextEntry();
        }

        zipInputStream.close();
    }

    public void transferConfDirs(String sourceZkHost, String targetZkHost, List<String> dirs, Map<String, String> dirMap) throws IOException, KeeperException, InterruptedException {
        ZooKeeper sourceZkClient = new ZooKeeper(sourceZkHost, 30000, null);
        ZooKeeper targetZkClient = new ZooKeeper(targetZkHost, 30000, null);

        try {
            for (String sourceDir : dirs) {
                String targetDir = (dirMap != null && dirMap.containsKey(sourceDir)) ? dirMap.get(sourceDir) : sourceDir;
                List<String> sourceFiles = sourceZkClient.getChildren("/configs/" + sourceDir, null);
                zookeeperFileService.copyFilesToTargetZk("/configs/" + sourceDir, "/configs/" + targetDir, sourceFiles, sourceZkClient, targetZkClient);
            }
        } finally {
            sourceZkClient.close();
            targetZkClient.close();
        }
    }

    public Mono<List<String>> viewDirs(String zkHost, String basePath) throws InterruptedException, KeeperException {
        if (basePath == null) basePath = "/";
        SolrZkClient zkClient = new SolrZkClient(zkHost, 30000, 30000);
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

    public ResponseEntity<String> uploadConfDirs(String host, String dirName, MultipartFile[] files) {
        try {
            if (files != null && files.length > 0) {
                ZooKeeper zkClient = zookeeperClientService.createZookeeperClient(host);
                try {
                    // Process each file
                    for (MultipartFile file : files) {
                        if (!file.isEmpty()) {
                            uploadFileToZk(file, zkClient, dirName);
                        }
                    }
                    return ResponseEntity.ok("Files successfully uploaded to ZooKeeper");
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
                } finally {
                    zookeeperClientService.closeZookeeperClient(zkClient);
                }
            } else {
                return ResponseEntity.badRequest().body("No file uploaded");
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<String> uploadConfDirs(String host, byte[] fileData) {
        try {
            if (fileData.length > 0) {
                ZooKeeper zkClient = zookeeperClientService.createZookeeperClient(host);
                try {
                    // Unzip the uploaded file and get its contents
                    extractAndUploadZip(fileData, zkClient);
                    return ResponseEntity.ok("Files successfully uploaded to ZooKeeper");
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
                } finally {
                    zookeeperClientService.closeZookeeperClient(zkClient);
                }
            } else {
                return ResponseEntity.badRequest().body("No file data provided");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void extractAndUploadZip(byte[] fileData, ZooKeeper zkClient) throws IOException, KeeperException, InterruptedException {
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(fileData));
        ZipEntry zipEntry = zipInputStream.getNextEntry();

        while (zipEntry != null) {
            String entryName = zipEntry.getName();

            if (!zipEntry.isDirectory()) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;

                while ((len = zipInputStream.read(buffer)) > 0) {
                    byteArrayOutputStream.write(buffer, 0, len);
                }

                byte[] entryBytes = byteArrayOutputStream.toByteArray();

                // Break down the entryName into parts
                String[] parts = entryName.split("/");
                String currentPart = "/configs";

                // Create each part of the node hierarchy if it doesn't exist
                for (int i = 0; i < parts.length - 1; i++) {
                    currentPart += "/" + parts[i];
                    if (zkClient.exists(currentPart, false) == null) {
                        zkClient.create(currentPart, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    }
                }

                // Upload the entry bytes to the corresponding path in ZooKeeper
                zkClient.create("/configs/" + entryName, entryBytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

                byteArrayOutputStream.close();
            }

            zipInputStream.closeEntry();
            zipEntry = zipInputStream.getNextEntry();
        }

        zipInputStream.close();
    }

}
