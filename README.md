# zkt - Zookeeper Toolkit 🫎

An API with Swagger to let you download and upload files & dirs to Zookeeper.

## Routes

- **downloadConfDirs**: This endpoint should accept a list of directories and create a ZIP file containing each directory's contents.
- **uploadConfDirs**: This endpoint should accept a ZIP file and extract its contents into the specified directories in ZooKeeper.
- **transferConfDirs**: This endpoint should copy directories from a source ZooKeeper to a target ZooKeeper.

