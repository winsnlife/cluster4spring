package org.softamis.cluster4spring.rmi

import org.I0Itec.zkclient.ZkClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean

/**
 * Created by Administrator on 2016/4/1.
 * zookeeper默认的工厂bean,注意与工厂类的区别
 * sping工厂bean，返回的是getObject中创建的对象
 * 而工厂类，是返回类本身的实例
 */
class ZookeeperFactoryBean implements FactoryBean<ZkClient>,InitializingBean,DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperFactoryBean.class)

    //zkClient(集群)地址:172.16.8.107:2181,172.16.8.100:2181,172.16.8.101:2181
    String zkHosts
    //zookeeper连接超时时间(ms)
    int zkSessionTimeOut

    int zkConnectionTimeout = Long.MAX_VALUE //zookeeper客户端与服务端连接超时时间,ZkClient默认长连接

    ZkClient zkClient

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     * @throws Exception in the event of misconfiguration (such
     * as failure to set an essential property) or if initialization fails.
     */
    @Override
    void afterPropertiesSet() throws Exception {
        initZk()
    }

    private ZkClient initZk(){

        try {
            zkClient = new ZkClient(zkHosts, zkSessionTimeOut,zkConnectionTimeout)
            LOGGER.info("zkClient connection is connected")
        } catch (IOException e) {
            LOGGER.error("IOException : init zkClient failure", e)
        } catch (InterruptedException e){
            LOGGER.error("InterruptedException : init zkClient failure", e)
        }

        return zkClient
    }

    /**
     * Return an instance (possibly shared or independent) of the object
     * managed by this factory.
     * <p>As with a {@link BeanFactory}, this allows support for both the
     * Singleton and Prototype design pattern.
     * <p>If this FactoryBean is not fully initialized yet at the time of
     * the call (for example because it is involved in a circular reference),
     * throw a corresponding {@link FactoryBeanNotInitializedException}.
     * <p>As of Spring 2.0, FactoryBeans are allowed to return {@code null}
     * objects. The factory will consider this as normal value to be used; it
     * will not throw a FactoryBeanNotInitializedException in this case anymore.
     * FactoryBean implementations are encouraged to throw
     * FactoryBeanNotInitializedException themselves now, as appropriate.
     * @return an instance of the bean (can be {@code null})
     * @throws Exception in case of creation errors
     * @see FactoryBeanNotInitializedException
     */
    @Override
    public ZkClient getObject() throws Exception {
        return zkClient
    }

    /**
     * Return the type of object that this FactoryBean creates,
     * or {@code null} if not known in advance.
     * <p>This allows one to check for specific types of beans without
     * instantiating objects, for example on autowiring.
     * <p>In the case of implementations that are creating a singleton object,
     * this method should try to avoid singleton creation as far as possible;
     * it should rather estimate the type in advance.
     * For prototypes, returning a meaningful type here is advisable too.
     * <p>This method can be called <i>before</i> this FactoryBean has
     * been fully initialized. It must not rely on state created during
     * initialization; of course, it can still use such state if available.
     * <p><b>NOTE:</b> Autowiring will simply ignore FactoryBeans that return
     * {@code null} here. Therefore it is highly recommended to implement
     * this method properly, using the current state of the FactoryBean.
     * @return the type of object that this FactoryBean creates,
     * or {@code null} if not known at the time of the call
     * @see ListableBeanFactory#getBeansOfType
     */
    @Override
    public Class<?> getObjectType() {
        return ZkClient.class
    }

    /**
     * Is the object managed by this factory a singleton? That is,
     * will {@link #getObject()} always return the same object
     * (a reference that can be cached)?
     * <p><b>NOTE:</b> If a FactoryBean indicates to hold a singleton object,
     * the object returned from {@code getObject ( )} might get cached
     * by the owning BeanFactory. Hence, do not return {@code true}
     * unless the FactoryBean always exposes the same reference.
     * <p>The singleton status of the FactoryBean itself will generally
     * be provided by the owning BeanFactory; usually, it has to be
     * defined as singleton there.
     * <p><b>NOTE:</b> This method returning {@code false} does not
     * necessarily indicate that returned objects are independent instances.
     * An implementation of the extended {@link SmartFactoryBean} interface
     * may explicitly indicate independent instances through its
     * {@link SmartFactoryBean#isPrototype()} method. Plain {@link FactoryBean}
     * implementations which do not implement this extended interface are
     * simply assumed to always return independent instances if the
     * {@code isSingleton ( )} implementation returns {@code false}.
     * @return whether the exposed object is a singleton
     * @see #getObject()
     * @see SmartFactoryBean#isPrototype()
     */
    @Override
    public boolean isSingleton() {
        return true
    }

    /**
     * Invoked by a BeanFactory on destruction of a singleton.
     * @throws Exception in case of shutdown errors.
     * Exceptions will get logged but not rethrown to allow
     * other beans to release their resources too.
     */
    @Override
    public void destroy() throws Exception {
        zkClient.close()
    }
}
