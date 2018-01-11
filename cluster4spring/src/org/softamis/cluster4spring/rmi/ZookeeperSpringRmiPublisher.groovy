package org.softamis.cluster4spring.rmi

import org.I0Itec.zkclient.IZkStateListener
import org.I0Itec.zkclient.ZkClient
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.ZooDefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.softamis.cluster4spring.support.PathChcker
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.remoting.rmi.RmiServiceExporter

/**
 * Created by Administrator on 2016/4/1.
 * @authr wws
 * 将spring rmi 注册到zookeeper上
 */
public class ZookeeperSpringRmiPublisher implements InitializingBean,DisposableBean{

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperSpringRmiPublisher.class)
    String zkHosts //zookeeper连接地址
    int zkSessionTimeOut = 5000 //zookeeper会话超时时间ms
    int zkConnectionTimeout = Long.MAX_VALUE //zookeeper客户端与服务端连接超时时间,ZkClient默认长连接
    ZkClient zkClient

    Set<RmiServiceExporter> rmiServiceExporters
    PathChcker chcker

    @Override
    void afterPropertiesSet() throws Exception {
        initZkClient()
        if(!chcker)
            chcker = new PathChcker()
        pushServiceToZk()
    }

    /**rmi://172.16.8.56:60000/serviceName*/
    String getRmiUrl(String localhost,int port,String serviceName){
        String url = String.format("rmi://%s:%d/%s", localhost, port, serviceName)
        LOGGER.info("rmiUrl is: "+url)
        return url
    }

    void initZkClient(){

        zkClient = new ZkClient(zkHosts,zkSessionTimeOut,zkConnectionTimeout);

        zkClient.subscribeStateChanges(new IZkStateListener() {

            /**
             *连接状态改变时调用
             */
            @Override
            void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
                System.out.println("------------handleStateChanged,stat----------------:" + state+"::::"+new Date().toLocaleString())
            }

            /**
             * zookeeper会话过时或者创建新的会话时调用，这里用于重新创建临时节点
             */
            @Override
            void handleNewSession() throws Exception {
                System.out.println("-------------handleNewSession()--------------"+new Date().toLocaleString())
                pushServiceToZk()
            }

            /**
             * 当zookeeper会话无法重新创建时被调用，这应该是用于实现连接故障处理等。重试连接或pass掉错误
             */
            @Override
            void handleSessionEstablishmentError(Throwable error) throws Exception {
                System.out.println("handleSessionEstablishmentError()-------------------:"+new Date().toLocaleString())
                zkClient.waitUntilConnected()
            }

        })
    }
    // 创建 rmiNode
    void createRmiNode(String path,String url) {
        try {
            String nodePath = zkClient.createEphemeralSequential(path, url, ZooDefs.Ids.OPEN_ACL_UNSAFE)// 创建一个临时性且有序的 node
            LOGGER.info("create rmi node: {} => {} ", nodePath, url)
        } catch (IOException e) {
            LOGGER.error("IOException : create rmi node failure", e)
        } catch (InterruptedException e){
            LOGGER.error("InterruptedException : create rmi node failure", e)
        }
    }

    void push(RmiServiceExporter rmiServiceExporter){

        //接口名作为同一rmi节点父节点
        //String serviceNode = rmiServiceExporter.serviceInterface.getName() //不再使用接口名作为节点名称
        //改为使用rmi服务名，要求全局唯一
        String serviceNode = rmiServiceExporter.serviceName

        serviceNode = chcker.zkProviderPath + "/"+serviceNode

        LOGGER.info("pushing rmiNode path is: {}",serviceNode)
        boolean  serviceNodeExists = zkClient.exists(serviceNode)
        if(!serviceNodeExists) {
            zkClient.createPersistent(serviceNode,null,ZooDefs.Ids.OPEN_ACL_UNSAFE)
            LOGGER.info("create interFace node: {}",serviceNode)
        }

        String localhost = java.net.InetAddress.getLocalHost().getHostAddress()
        int port = rmiServiceExporter.registryPort
        String serviceName = rmiServiceExporter.serviceName
        String rmiUrl = getRmiUrl(localhost,port,serviceName)

        createRmiNode(serviceNode+"/node_",rmiUrl)
    }

    void pushServiceToZk() {

        if(!rmiServiceExporters)
            throw new IllegalArgumentException("rmiServiceExporters is has no one!")

        chcker.initOrCheckPath(zkClient)

        for(RmiServiceExporter rmiServiceExporter : rmiServiceExporters){
            push(rmiServiceExporter)
        }

        LOGGER.info("push rmi service to zkClient over,size:{}",rmiServiceExporters.size())
    }

    @Override
    void destroy() throws Exception {
        rmiServiceExporters = null
        chcker = null
        zkClient.close()
    }
}
