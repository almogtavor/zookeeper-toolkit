package io.github.almogtavor.service;

import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ZookeeperClientService {

    public ZooKeeper createZookeeperClient(String zkHost) throws IOException {
        return new ZooKeeper(zkHost, 30000, null);
    }

    public void closeZookeeperClient(ZooKeeper zkClient) throws InterruptedException {
        if (zkClient != null) {
            zkClient.close();
        }
    }
}
