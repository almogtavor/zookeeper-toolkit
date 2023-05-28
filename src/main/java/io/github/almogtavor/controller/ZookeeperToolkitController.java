package io.github.almogtavor.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.zookeeper.KeeperException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import io.github.almogtavor.configuration.PreConfiguredZkHosts;
import io.github.almogtavor.service.ZookeeperConfigurationManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ZookeeperToolkitController {
    private final ZookeeperConfigurationManager zookeeperDownloadUploadService;

    @PostMapping(value = "/downloadAllZkData")
    public ResponseEntity<byte[]> downloadAllZkData(@RequestParam("zkHost") PreConfiguredZkHosts preConfiguredZkHosts) throws IOException, KeeperException, InterruptedException {
        return zookeeperDownloadUploadService.downloadAllZkData(preConfiguredZkHosts.getHost());
    }

    @PostMapping(value = "/downloadConfDirs")
    public ResponseEntity<byte[]> downloadConfDirs(@RequestParam("zkHost") PreConfiguredZkHosts preConfiguredZkHosts, @RequestBody List<String> dirs) throws IOException, KeeperException, InterruptedException {
        return zookeeperDownloadUploadService.downloadConfDirs(preConfiguredZkHosts.getHost(), dirs);
    }

    @PostMapping(value = "/uploadConfDirs", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> uploadConfDirs(@RequestParam("zkHost") PreConfiguredZkHosts zkHost, @RequestBody byte[] fileData) throws IOException, InterruptedException, KeeperException {
        return zookeeperDownloadUploadService.uploadConfDirs(zkHost.getHost(), fileData);
    }
    @PostMapping(value = "/copyConfDirsBetweenZks")
    public ResponseEntity<String> copyConfDirs(@RequestParam("sourceZkHost") PreConfiguredZkHosts sourceZkHost,
                                                   @RequestParam("targetZkHost") PreConfiguredZkHosts targetZkHost,
                                                   @Schema(type = "array", defaultValue = "[\"_default\"]")
                                                   @RequestParam List<String> dirsToCopyFromSource,
                                                   @Schema(type = "object", defaultValue = "{\"_default\": \"targetDirName\"}")
                                                   @RequestParam Map<String, String> sourceToTargetDirNamesMapping) throws IOException, KeeperException, InterruptedException {
        zookeeperDownloadUploadService.copyConfDirs(sourceZkHost.getHost(), targetZkHost.getHost(), dirsToCopyFromSource, sourceToTargetDirNamesMapping);
        return ResponseEntity.ok().body("Files successfully copied from source to target ZooKeeper");
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

    @PostMapping(value = "/custom/uploadConfDirs", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Tag(name = "custom")
    public ResponseEntity<String> customUploadConfDirs(@RequestParam("zkHost") String zkHost, @RequestBody byte[] fileData) throws IOException, InterruptedException, KeeperException {
        return zookeeperDownloadUploadService.uploadConfDirs(zkHost, fileData);
    }

    @PostMapping(value = "/custom/copyConfDirsBetweenZks")
    @Tag(name = "custom")
    public ResponseEntity<String> customCopyConfDirs(@RequestParam("sourceZkHost") String sourceZkHost,
                                                   @RequestParam("targetZkHost") String targetZkHost,
                                                   @RequestParam List<String> dirsToCopyFromSource,
                                                   @RequestParam Map<String, String> sourceToTargetDirNamesMapping) throws IOException, KeeperException, InterruptedException {
        zookeeperDownloadUploadService.copyConfDirs(sourceZkHost, targetZkHost, dirsToCopyFromSource, sourceToTargetDirNamesMapping);
        return ResponseEntity.ok().body("Files successfully copied from source to target ZooKeeper");
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
