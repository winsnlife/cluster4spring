/******************************************************************************
 * Copyright(c) 2005-2007 SoftAMIS (http://www.soft-amis.com)                 *
 * All Rights Reserved.                                                       *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * You may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 * *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package org.softamis.cluster4spring.rmi;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.BeansException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.util.ClassUtils;

import org.softamis.cluster4spring.rmi.support.RmiClientInterceptor;
import org.softamis.cluster4spring.rmi.support.RmiEndpoint;
import org.softamis.cluster4spring.support.ServiceMoniker;
import org.softamis.cluster4spring.support.AdvicesListProvidingUtils;
import org.softamis.cluster4spring.support.ClientRemoteInvocationTraceInterceptor;
import org.softamis.cluster4spring.support.EndpointFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.aop.Advice;

/**
 * Abstract class used to implement RMI based proxy factory beans
 * @author Andrew Sazonov
 */


public abstract class AbstractRmiProxyFactoryBean<SI extends ServiceMoniker>
        extends RmiClientInterceptor<SI>
        implements FactoryBean,
        BeanFactoryAware {
    private static final Log fLog = LogFactory.getLog(AbstractRmiProxyFactoryBean.class);
    /**
     * Proxy object
     */
    protected Object fServiceProxy = null;
    /**
     * Indicates whether trace interceptor should be registered
     */
    protected boolean fRegisterTraceInterceptor = false;
    /**
     * Bean factory where bean is included
     */
    protected BeanFactory fBeanFactory = null;
    /**
     * List of interceptor names that should be registered
     */
    protected String[] fInterceptorNames = null;
    /**
     * Interceptor that should be registered to trace remote calls
     */
    protected MethodInterceptor fRemoteInvocationTraceInterceptor = null;

    public AbstractRmiProxyFactoryBean() {
    }

    /**
     * Callback that supplies the owning factory to a bean instance.
     * <p>Invoked after population of normal bean properties but before an init
     * callback like InitializingBean's afterPropertiesSet or a custom init-method.
     *
     * @param aBeanFactory owning BeanFactory (may not be null).
     * The bean can immediately call methods on the factory.
     * @throws org.springframework.beans.BeansException
     *          in case of initialization errors
     * @see org.springframework.beans.factory.BeanInitializationException
     */
    public void setBeanFactory(BeanFactory aBeanFactory)
            throws BeansException {
        fBeanFactory = aBeanFactory;
    }

    @SuppressWarnings({"ProhibitedExceptionDeclared"})
    @Override
    public void afterPropertiesSet()
            throws Exception {
        super.afterPropertiesSet();

        // check that class of service interface is specified
        if (getServiceInterface() == null) {
            throw new IllegalArgumentException("serviceInterface is required");
        }

        Class serviceInterface = getServiceInterface();

        // here we create proxy
        ProxyFactory proxyFactory = new ProxyFactory();

        // check and add advices specified for proxy
        addSpecifiedAdvices(proxyFactory);

        // if necessary, we register trace interceptor
        if (fRegisterTraceInterceptor) {
            MethodInterceptor traceInterceptor = obtainRemoteInvocationTraceInterceptor();
            proxyFactory.addAdvice(traceInterceptor);
        }


        proxyFactory.addInterface(serviceInterface);
        proxyFactory.addAdvice(this);
        fServiceProxy = proxyFactory.getProxy();
    }

    /**
     * Discovers advices specified in bean definition and adds them to proxy factory
     * used to obtain proxy.
     *
     * @param aProxyFactory proxy factory used to create proxy for remote invocation
     * @see #getInterceptorNames()
     */
    protected void addSpecifiedAdvices(ProxyFactory aProxyFactory) {
        //first, we select advices applicable
        List<Advice> advices = AdvicesListProvidingUtils.getAdvices(fBeanFactory, fInterceptorNames);

        // and add them to proxy which will be used
        if (advices != null) {
            for (Advice advice : advices) {
                aProxyFactory.addAdvice(advice);
            }
        }
    }

    /**
     * Utility method that returns interceptor used to trace remote invocation.
     * Method called if <code>registerTraceInterceptor</code> property is set to
     * <code>true</code>.
     *
     * @return interceptor that should be used to trace remote invocations. If
     * interceptor specified explicitely, returns it. Otherwise, creates
     * default one.
     * @see #createDefaultRemoteInvocationTraceInterceptor()
     * @see #isRegisterTraceInterceptor()
     */
    protected MethodInterceptor obtainRemoteInvocationTraceInterceptor() {
        MethodInterceptor result = null;

        // RemoteInvocationTraceInterceptor may be either created by default
        // or be specified explicitely
        if (fRemoteInvocationTraceInterceptor == null) {
            result = createDefaultRemoteInvocationTraceInterceptor();
        } else {
            result = fRemoteInvocationTraceInterceptor;
        }
        return result;
    }

    /**
     * Creates default remote invocation trace method interceptor
     *
     * @return created interceptor
     * @see org.softamis.cluster4spring.support.ClientRemoteInvocationTraceInterceptor
     */
    protected MethodInterceptor createDefaultRemoteInvocationTraceInterceptor() {
        String proxyName = getProxyName();
        String key = fBeanName;
        MethodInterceptor result = new ClientRemoteInvocationTraceInterceptor(proxyName, key);
        return result;
    }

    /**
     * Returns name of the proxy
     *
     * @return proxy name
     */
    protected String getProxyName() {
        Class<? extends AbstractRmiProxyFactoryBean> clazz = getClass();
        String result = ClassUtils.getShortName(clazz);
        return result;
    }

    public Object getObject() {
        return fServiceProxy;
    }

    public Class getObjectType() {
        return getServiceInterface();
    }

    /**
     * Is the bean managed by this factory a singleton or a prototype?
     * That is, will getObject() always return the same object?
     * <p>The singleton status of the FactoryBean itself will generally
     * be provided by the owning BeanFactory; usually, it has to be
     * defined as singleton there.
     *
     * @return alwas returns true
     */
    public boolean isSingleton() {
        return true;
    }

    /**
     * Specifies whether trace interceptor should be created for this factory.
     * If this option is set, either interceptor specified as trace one will be
     * registered or default trace interceptor will be created.
     *
     * @param aRegisterTraceInterceptor <code>true</code> if interceptor should be created
     * @see #createDefaultRemoteInvocationTraceInterceptor()
     */
    public void setRegisterTraceInterceptor(boolean aRegisterTraceInterceptor) {
        fRegisterTraceInterceptor = aRegisterTraceInterceptor;
    }

    /**
     * Returns indicates whether trace interceptor should be registered.
     *
     * @return indicates whether trace interceptor should be registered
     */
    public boolean isRegisterTraceInterceptor() {
        return fRegisterTraceInterceptor;
    }

    /**
     * Returns list of interceptor names that should be registered in proxy factory.
     * These interceptors could be used, for example, to add custom processing or
     * profiling functionality.
     *
     * @return list of interceptor names that should be registered
     */
    public String[] getInterceptorNames() {
        return fInterceptorNames;
    }

    /**
     * Sets list of interceptor names that should be registered
     *
     * @param aInterceptorNames list of interceptor names that should be registered
     */
    public void setInterceptorNames(String[] aInterceptorNames) {
        fInterceptorNames = aInterceptorNames;
    }

    /**
     * Returns interceptor that should be registered to trace remote calls
     *
     * @return interceptor that should be registered to trace remote calls
     * @see #isRegisterTraceInterceptor()
     * @see #setRegisterTraceInterceptor(boolean)
     */
    public MethodInterceptor getRemoteInvocationTraceInterceptor() {
        return fRemoteInvocationTraceInterceptor;
    }

    /**
     * Sets interceptor that should be registered to trace remote calls
     *
     * @param aRemoteInvocationTraceInterceptor interceptor that should be registered to trace remote calls
     */
    public void setRemoteInvocationTraceInterceptor(MethodInterceptor aRemoteInvocationTraceInterceptor) {
        fRemoteInvocationTraceInterceptor = aRemoteInvocationTraceInterceptor;
    }

    @Override
    public EndpointFactory<RmiEndpoint<SI>, SI> getEndpointFactory() {
        return super.getEndpointFactory();
    }

    @Override
    public void setEndpointFactory(EndpointFactory<RmiEndpoint<SI>, SI> aEndpointFactory) {
        super.setEndpointFactory(aEndpointFactory);
    }
}
