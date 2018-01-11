package org.softamis.cluster4spring.support

import org.I0Itec.zkclient.ZkClient
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Administrator on 2016/4/6.
 *
 * 服务端：生成rmi Url的存储节点
 * 客户端：检查
 */
class PathChcker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathChcker.class)

    String zkSpringRmiRegistryRoot = "/rmiRegistryRoot"

    void initOrCheckPath(ZkClient zk){

        if(zkSpringRmiRegistryRoot.indexOf('/') == -1)
            throw new IllegalArgumentException("zkSpringRmiRegistryRoot error!")

        if(zkProviderPath.indexOf('/') == -1)
            throw new IllegalArgumentException("zkProviderPath error!")

        if(zkConsumerPath.indexOf('/') == -1)
            throw new IllegalArgumentException("zkConsumerPath error!")

        if(!zk.exists(this.zkSpringRmiRegistryRoot,false)){
            zk.create(this.zkSpringRmiRegistryRoot,"spring rmi 根节点".getBytes("utf-8"),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT)
        }

        if(!zk.exists(this.zkProviderPath,false)){
            zk.create(this.zkProviderPath,"spring rmi provider 根节点".getBytes("utf-8"),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT)
        }

        if(!zk.exists(this.zkConsumerPath,false)){
            zk.create(this.zkConsumerPath,"spring rmi consumer 根节点".getBytes("utf-8"),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT)
        }

        LOGGER.info("zkClient path  has checked !")
    }

    String getZkProviderPath(){
        return zkSpringRmiRegistryRoot + "/provider"
    }

    String getZkConsumerPath(){
        return zkSpringRmiRegistryRoot + "/consumer"
    }
}
