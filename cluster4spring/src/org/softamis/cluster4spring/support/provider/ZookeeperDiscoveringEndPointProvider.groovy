package org.softamis.cluster4spring.support.provider

import org.I0Itec.zkclient.IZkChildListener
import org.I0Itec.zkclient.IZkDataListener
import org.I0Itec.zkclient.ZkClient
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.ZooDefs
import org.softamis.cluster4spring.rmi.ZookeeperFactoryBean
import org.softamis.cluster4spring.support.Endpoint
import org.softamis.cluster4spring.support.EndpointFactory
import org.softamis.cluster4spring.support.PathChcker
import org.softamis.cluster4spring.support.ServiceMoniker
import org.softamis.cluster4spring.support.invocation.ShuffleEndpointSelectionPolicy
import org.springframework.remoting.RemoteAccessException
import org.springframework.remoting.support.RemoteInvocationFactory

import static java.text.MessageFormat.format

/**
 * Created by Administrator on 2016/4/5.
 *
 */
public class ZookeeperDiscoveringEndPointProvider<E extends Endpoint<ServiceMoniker>, SI extends ServiceMoniker>
        extends AbstractUrlListEndpointProvider<E,SI> {

    protected static final Log fLog = LogFactory.getLog(ZookeeperDiscoveringEndPointProvider.class)

    /**
     * rmi服务在zookeeper上的的注册节点名称，实际上是ZookeeperFactory中zkProviderPath的一个子节点
     * 而ZookeeperSpringRmiPublisher中，默认将rmi Url 放在 以服务接口全路径为名字的节点中，
     * 实际上，watcherNodeName = 服务接口全路径名字，即与proxyFactory的serviceInterface一样
     * */
    String watcherNodeName

    ZookeeperFactoryBean zookeeperFactoryBean
    ZkClient zkClient

    PathChcker chcker

    /**
     * List of urls used to discover remote service and create service endpoints
     */
    protected List<SI> fServiceMonikers = null

    public ZookeeperDiscoveringEndPointProvider(){
        super()
    }

    /**
     * Invoked by Spring as part of bean' lifecycle. In addition to inherited checks, adds checking
     * that list of service urls is specified.
     *
     * @throws Exception
     */
    @Override
    void afterPropertiesSet() throws Exception {
        if(!chcker)
            chcker = new PathChcker()
        initZk()
        super.afterPropertiesSet()
    }

    void initZk(){

        if(!watcherNodeName){
            throw new IllegalArgumentException("watcherNodeName is require!")
        }

        if(!zookeeperFactoryBean && !zkClient){
            throw new IllegalArgumentException("zookeeperFactory or zkClient is require!")
        }
        if(!zkClient){
            zkClient = zookeeperFactoryBean.getObject()
            if (fLog.isTraceEnabled()) {
                String message = format("get zkClient instance from zookeeperFacotry.")
                fLog.trace(message)
            }
        }

        if (fLog.isTraceEnabled()) {
            String message = format("zkClient instance Info:[{0}]",zkClient)
            fLog.trace(message)
        }

        //跟换为节点路径
        //服务父节点： /rmiRegistryRoot/provider/com.hfzy.ihk.cloud.facade.callCenter.service.websocket.WebSocketService
        watcherNodeName = chcker.zkProviderPath+"/"+watcherNodeName
        if(!zkClient.exists(watcherNodeName,false)){
            zkClient.create(watcherNodeName,"springRmi 父节点".getBytes("utf-8"),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT)
        }

        zkClient.subscribeChildChanges(watcherNodeName, new IZkChildListener() {
            /**
             * 当该节点下孩子节点发生变化时调用
             * (event.getType() == EventType.NodeChildrenChanged || event.getType() == EventType.NodeCreated || event.getType() == EventType.NodeDeleted)
             */
            @Override
            void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                //clildren of path /rmiRegistryRoot/provider/test:[test_0000000663, test_0000000664, test_0000000662, test_0000000661, test_0000000660]
                fLog.info("handleChildChange->clildren of path " + parentPath + ":" + currentChilds)
                watcherUrlList("handleChildChange")
            }
        })

        /**
         * (event.getType() == EventType.NodeDataChanged || event.getType() == EventType.NodeDeleted || event.getType() == EventType.NodeCreated)
         * */
        zkClient.subscribeDataChanges(watcherNodeName, new IZkDataListener() {

            @Override
            void handleDataChange(String dataPath, Object data) throws Exception {
                //Data of /rmiRegistryRoot/provider/test has changed:data:18
                fLog.info("handleDataChange->Data of " + dataPath + " has changed: data:"+data)
                watcherUrlList("handleDataChange")
            }

            /**
             * 有异常时才触发
             * */
            @Override
            void handleDataDeleted(String dataPath) throws Exception {
                fLog.info("handleDataDeleted->"+dataPath + " has deleted")
                watcherUrlList("handleDataDeleted")
            }
        })

        chcker.initOrCheckPath(zkClient)

        String message = format("starting get rmi Url from watcherNodeName, watcherNodeName name:[{0}]",watcherNodeName)
        fLog.trace(message)

        watcherUrlList("init")

    }

    void watcherUrlList(String eventType){

        /*清空缓存，由于设置了缓存存根，*/
        synchronized (fCacheLock){
            fEndpointsCache = null
            fLog.info("监控节点发生变化，清空存根缓存。")
        }

        try {

            String message = format("invoking watcherUrlList().zkClient node eventType Name: [{0}], node Path: [{1}]", eventType, watcherNodeName)
            fLog.info(message)

            List<String> nodeList = zkClient.getChildren(watcherNodeName)

            message = format("get rmi nodeList from path:[{0}],size: [{1}].", watcherNodeName, nodeList?nodeList.size():0)
            fLog.info(message)

            List<String> dataList = new ArrayList<String>() // 用于存放所有子节点中的数据

            for (String node : nodeList) {
                byte[] data = zkClient.readData(watcherNodeName + "/" + node) // 获取 /registry 的子节点中的数据
                dataList.add(new String(data))
            }

             message = format("get node data from path:[{0}], data info: [{1}].", watcherNodeName, dataList)
            fLog.info(message)

            if(dataList)
                setServiceURLs(dataList)

        } catch (KeeperException e) {
                fLog.error("watcherUrlList KeeperException :[{0}].", e)
        } catch (InterruptedException e) {
            fLog.error("watcherUrlList InterruptedException :[{0}].", e)
        }

    }

    /**
     * Creates list of endpoints available for service. Inherited classes will override it to
     * provide specific implementation.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocation
     * @param aEndpointFactory factory used to create endpoints
     * @param aBeanName
     * @return list of created endpoints
     * @throws RemoteAccessException throws if list of endpoints could not be created
     */
    @Override
    protected List<E> doRefreshServiceEndpointsList(RemoteInvocationFactory aRemoteInvocationFactory, EndpointFactory<E, SI> aEndpointFactory, String aBeanName) throws RemoteAccessException {
        int servicesSize = fServiceMonikers.size()
        List<E> result = new ArrayList<E>(servicesSize)

        // simply walk over list of remote service locations and try to create endpoint for
        // every location
        for (SI serviceInfo : fServiceMonikers) {
            if (fLog.isTraceEnabled()) {
                String message =
                        format("Starting endpoint creation. Bean Name: [{0}]. Service Info: [{1}]", aBeanName, serviceInfo)
                fLog.trace(message)
            }

            E endpoint = null
            try {
                endpoint = doCreateServiceEndpoint(aRemoteInvocationFactory, aEndpointFactory, aBeanName, serviceInfo)
            } catch (RemoteAccessException e) {
                if (fLog.isErrorEnabled()) {
                    String message = format("Unable to create service endpoint. Bean Name: [{0}]. Service Info: [{1}]", aBeanName,
                            serviceInfo)
                    fLog.error(message, e)
                }
            }
            if (endpoint != null) {
                result.add(endpoint)
            }
        }
        if (result.isEmpty()) {
            String message =
                    format("Unable to determine at least one service endpoint for server. Bean Name: [{0}]", aBeanName)
            throw new RemoteAccessException(message)
        }

        return result
    }

    /**
     * Marks given endpoint invalid. This endpoint will not be later used for methods invocation.
     *
     * @param aBeanName name of bean that is used as proxy for remote service
     * @param aEndpoint endpoint to be marked invalid
     */
    @Override
    void markInvalid(String aBeanName, E aEndpoint) {
        if (fCacheEndpoints) // we are in cashe mode, so we need to remove endpoint from cache
        {
            synchronized (fCacheLock) {
                if (fEndpointsCache != null) {
                    fEndpointsCache.remove(aEndpoint)
                }
            }
        } else {
            // endpoints list will be selected during next invocation of remote service
        }
    }

    /**
     * Returns list of <code>ServiceMonikers</code> used to discover remote service and create service endpoints
     *
     * @return list of urls
     * @see ServiceMoniker
     */
    public List<SI> getServiceMonikers() {
        return fServiceMonikers
    }

    /**
     * Utility method which allows to specify list of URL when URL of service locations is specified
     * in simple string form.
     * @param aServiceURL
     */
    public void setServiceURLs(List<String> aServiceURL) {
        if (aServiceURL == null || aServiceURL.isEmpty()) {
            String message = "Null or empty list of service URLs passed"
            if (fLog.isErrorEnabled()) {
                fLog.error(message)
            }
            throw new IllegalArgumentException(message)
        }
        fServiceMonikers = new ArrayList<SI>(aServiceURL.size())

        for (String url : aServiceURL) {
            SI moniker = (SI) new ServiceMoniker(url)
            fServiceMonikers.add(moniker)
        }
    }

}
