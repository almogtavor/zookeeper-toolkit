package io.github.almogtavor.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.zookeeper.KeeperException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import io.github.almogtavor.configuration.PreConfiguredZkHosts;
import io.github.almogtavor.service.ZookeeperDownloadUploadService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ZookeeperToolkitController {
    private final ZookeeperDownloadUploadService zookeeperDownloadUploadService;

    @PostMapping(value = "/downloadAllZkData")
    public ResponseEntity<byte[]> downloadAllZkData(@RequestParam("zkHost") PreConfiguredZkHosts preConfiguredZkHosts) throws IOException, KeeperException, InterruptedException {
        return zookeeperDownloadUploadService.downloadAllZkData(preConfiguredZkHosts.getHost());
    }

    @PostMapping(value = "/downloadConfDirs")
    public ResponseEntity<byte[]> downloadConfDirs(@RequestParam("zkHost") PreConfiguredZkHosts preConfiguredZkHosts, @RequestBody List<String> dirs) throws IOException, KeeperException, InterruptedException {
        return zookeeperDownloadUploadService.downloadConfDirs(preConfiguredZkHosts.getHost(), dirs);
    }

    /**
     * Currently uploadConfDirs doesn't work from Swagger / Postman
     */
//    @Deprecated
    @PostMapping(value = "/uploadConfDirs/zip", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> uploadConfDirs(@RequestParam("zkHost") PreConfiguredZkHosts zkHost, @RequestPart("file") MultipartFile file) throws IOException, KeeperException, InterruptedException {
        return zookeeperDownloadUploadService.uploadConfDirs(zkHost.getHost(), file);
    }

    @PostMapping(value = "/uploadConfDirs/binary", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> uploadConfDirs(@RequestParam("zkHost") PreConfiguredZkHosts zkHost, @RequestBody byte[] fileData) throws IOException, KeeperException, InterruptedException {
        return zookeeperDownloadUploadService.uploadConfDirs(zkHost.getHost(), fileData);
    }

    @PostMapping(value = "/uploadConfDirs")
    public ResponseEntity<String> uploadConfDirs(@RequestParam("zkHost") PreConfiguredZkHosts zkHost, @RequestParam("dirName") String dirName, @RequestPart("files") MultipartFile[] files) throws IOException, KeeperException, InterruptedException {
        return zookeeperDownloadUploadService.uploadConfDirs(zkHost.getHost(), dirName, files);
    }

        @PostMapping(value = "/transferConfDirs")
    public ResponseEntity<String> transferConfDirs(@RequestParam("sourceZkHost") PreConfiguredZkHosts sourceZkHost,
                                                   @RequestParam("targetZkHost") PreConfiguredZkHosts targetZkHost,
                                                   @Schema(type = "array", defaultValue = "[\"_default\"]")
                                                   @RequestParam List<String> dirs,
                                                   @Schema(type = "object", defaultValue = "{\"_default\": \"targetDirName\"}")
                                                   @RequestParam Map<String, String> dirMap) throws IOException, KeeperException, InterruptedException {
        zookeeperDownloadUploadService.transferConfDirs(sourceZkHost.getHost(), targetZkHost.getHost(), dirs, dirMap);
        return ResponseEntity.ok().body("Files successfully transferred from source to target ZooKeeper");
    }

    @GetMapping("/viewConfigDirs")
    public Mono<List<String>> viewConfigDirs(@RequestParam("zkHost") PreConfiguredZkHosts preConfiguredZkHosts) throws InterruptedException, KeeperException {
        return zookeeperDownloadUploadService.viewDirs(preConfiguredZkHosts.getHost(), "/configs");
    }


    @PostMapping(value = "/custom/downloadAllZkData")
    @Tag(name = "custom")
    public ResponseEntity<byte[]> customDownloadAllZkData(@RequestParam("zkHost") String zkHost) throws IOException, KeeperException, InterruptedException {
        return zookeeperDownloadUploadService.downloadAllZkData(zkHost);
    }

    @PostMapping(value = "/custom/transferConfDirs")
    @Tag(name = "custom")
    public ResponseEntity<String> customTransferConfDirs(@RequestParam("sourceZkHost") String sourceZkHost,
                                                   @RequestParam("targetZkHost") String targetZkHost,
                                                   @RequestParam List<String> dirs,
                                                   @RequestParam Map<String, String> dirMap) throws IOException, KeeperException, InterruptedException {
        zookeeperDownloadUploadService.transferConfDirs(sourceZkHost, targetZkHost, dirs, dirMap);
        return ResponseEntity.ok().body("Files successfully transferred from source to target ZooKeeper");
    }

    @PostMapping(value = "/custom/downloadConfDirs")
    @Tag(name = "custom")
    public ResponseEntity<byte[]> customDownloadConfDirs(@RequestParam("zkHost") String zkHost, @RequestBody List<String> dirs) throws IOException, KeeperException, InterruptedException {
        return zookeeperDownloadUploadService.downloadConfDirs(zkHost, dirs);
    }

    @GetMapping("/custom/viewDirs")
    @Tag(name = "custom")
    public Mono<List<String>> customViewDirs(@RequestParam("zkHost") String zkHost, @RequestParam("basePath") String basePath) throws InterruptedException, KeeperException {
        return zookeeperDownloadUploadService.viewDirs(zkHost, basePath);
    }
}
