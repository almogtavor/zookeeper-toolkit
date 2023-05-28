package io.github.almogtavor.service;

import lombok.RequiredArgsConstructor;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ZipService {
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
}
