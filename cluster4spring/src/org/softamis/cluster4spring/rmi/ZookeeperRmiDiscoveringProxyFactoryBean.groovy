package org.softamis.cluster4spring.rmi

import org.I0Itec.zkclient.ZkClient
import org.softamis.cluster4spring.rmi.support.AbstractRmiShortFormProxyFactoryBean
import org.softamis.cluster4spring.rmi.support.RmiEndpoint
import org.softamis.cluster4spring.support.EndpointSelectionPolicy
import org.softamis.cluster4spring.support.ServiceMoniker
import org.softamis.cluster4spring.support.provider.ZookeeperDiscoveringEndPointProvider
import org.springframework.context.ApplicationEvent

/**
 * Created by Administrator on 2016/4/6.
 * 基于zookeeper的远程rmi对象代理bean工厂类
 * 使用zookeeperFactory的zookeeper单例，加载zookeeper上的指定node name的rmi url,
 * node name --> this.serviceInterface.getName();
 */
public class ZookeeperRmiDiscoveringProxyFactoryBean<SI extends ServiceMoniker> extends AbstractRmiShortFormProxyFactoryBean<SI> {

    //必须注入zookeeperFactory,是为了一个client 服务器，只需要维护一个zookeeper 实例
    ZookeeperFactoryBean zookeeperFactoryBean  = null;
    ZkClient zkClient
    //rmi服务名，客户端用此检索zookeeper上的rmi服务url列表
    String serviceName

    protected EndpointSelectionPolicy<RmiEndpoint<SI>, SI> fEndpointSelectionPolicy = null;

    public ZookeeperRmiDiscoveringProxyFactoryBean() {
        super();
    }

    protected ZookeeperDiscoveringEndPointProvider createEndpointProvider() {
        return new ZookeeperDiscoveringEndPointProvider();
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        if (!serviceName) {
            throw new IllegalArgumentException("serviceName is require!用于搜索rmi服务,请填入您要调用的rmi服务名");
        }
        if (!zookeeperFactoryBean && !zkClient) {
            throw new IllegalArgumentException("zookeeperFactoryBean is require!");
        }

        ZookeeperDiscoveringEndPointProvider provider = createEndpointProvider();
        fEndpointProvider = provider;
//        provider.watcherNodeName = this.serviceInterface.getName();  //改为使用rmi服务名
        provider.watcherNodeName = serviceName
        provider.setZookeeperFactoryBean(zookeeperFactoryBean);
        provider.setZkClient(zkClient);
        provider.setCacheEndpoints(fCacheEndpoints);
        provider.setEndpointSelectionPolicy(fEndpointSelectionPolicy);
        provider.afterPropertiesSet();
        super.afterPropertiesSet()
    }

    public void setEndpointSelectionPolicy(EndpointSelectionPolicy<RmiEndpoint<SI>, SI> aServiceSelectionPolicy) {
        fEndpointSelectionPolicy = aServiceSelectionPolicy;
    }

    /**
     * spring的applicationcontext被注入后，就会调用此方法：RemoteClientInterceptor#onApplicationEvent
     * 可以用于连接zookeeper服务
     * @param aEvent application event
     * @see #onApplicationEvent(ApplicationEvent)
     */
    @Override
    protected void doPreprocessApplicationEvent(ApplicationEvent aEvent) {
        super.doPreprocessApplicationEvent(aEvent)
    }
}
