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

package org.softamis.cluster4spring.support;

import java.lang.reflect.Method;

import static java.text.MessageFormat.format;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Interceptor for accessing remote services. It contains only general logic
 * related to invoking remote services.
 *
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E>  type of endpoints that could be created by this factory
 * @author Andrew Sazonov
 * @version 1.0
 */

@SuppressWarnings({"ProhibitedExceptionDeclared", "CatchGenericClass", "ProhibitedExceptionThrown"})
public abstract class RemoteClientInterceptor<E extends Endpoint<SI>, SI extends ServiceMoniker>
        extends RemoteInvocationBasedAccessor
        implements MethodInterceptor,
        InitializingBean,
        BeanNameAware,
        ApplicationListener {
    protected static final Log fLog = LogFactory.getLog(RemoteClientInterceptor.class);

    /**
     * Name of proxy bean registered in spring context
     */
    protected String fBeanName = null;

    /**
     * Provider for endpoints used for remote services invocation
     */
    protected EndpointProvider<E, SI> fEndpointProvider = null;

    /**
     * Factory used to create endpoints
     */
    protected EndpointFactory<E, SI> fEndpointFactory = null;

    /**
     * Specifies whether endpoints provider should be refreshed if connection failure occurs
     */
    protected boolean fRefreshEndpointsOnConnectFailure = false;

    /**
     * Specifies whether different endpoint shoudl be selected if connection failure occurs
     */
    protected boolean fSwitchEndpointOnFailure = true;

    /**
     * Specifies whether endpoints should be refreshed on startup
     */
    protected boolean fRefreshEndpointsOnStartup = true;

    protected RemoteClientInterceptor() {
        super();
    }

    /**
     * Invoked by Spring as part of bean lifecycle and is used to check
     * whether <code>EndpointProvider</code> is specified and perform other
     * preparations
     *
     * @throws Exception
     */
    public void afterPropertiesSet()
            throws Exception {
        if (fEndpointProvider == null) {
            throw new IllegalArgumentException("Endpoint provider should be speicified. Bean Name: " + fBeanName);
        }
        prepare();
    }

    /**
     * Called as part of <code>afterPropertiesSet()<code> procedure and
     * checks whether endpoint factory is specified. If one is not specified,
     * creates default one.
     */
    public void prepare() {
        // if endpoint factory is not specified explicitely, we'll create default one
        if (fEndpointFactory == null) {
            fEndpointFactory = createDefaultEndpointFactory();
        }
    }

    /**
     * Called by Spring and handles application events
     *
     * @param aEvent application event
     */
    public void onApplicationEvent(ApplicationEvent aEvent) {
        try {
            // invoke hook for event preprocessing
            doPreprocessApplicationEvent(aEvent);

            // process event
            if (aEvent instanceof ContextRefreshedEvent) {
                onContextRefreshed();
            } else if (aEvent instanceof ContextClosedEvent) {
                onContextClosed();
            }
        } catch (Exception e) {
            if (fLog.isErrorEnabled()) {
                fLog.error("Exception during processing application event. ", e);
            }
        }
    }

    /**
     * Utility method that is called during processing of application events.
     * May be overriden to perform some specific processing
     *
     * @param aEvent application event
     * @see #onApplicationEvent(ApplicationEvent)
     */
    protected void doPreprocessApplicationEvent(ApplicationEvent aEvent) {
    }

    /**
     * Handles <code>ContextRefreshedEvent</code> event. If specified by appropriate
     * property, performs refreshing of endpoints.
     *
     * @see #setRefreshEndpointsOnStartup(boolean)
     * @see #onApplicationEvent(ApplicationEvent)
     */
    protected void onContextRefreshed() {
        // if we need preinialize endpoints, ask provider for refresh
        if (fRefreshEndpointsOnStartup) {
            refreshEndpointProvider();
        }
    }

    /**
     * Handles <code>ContextClosedEvent</code> event. May be overriden by inherited
     * classes to add additional processing of this event.
     *
     * @see #onApplicationEvent(ApplicationEvent)
     */
    protected void onContextClosed() {
    }

    /**
     * Forces refresh of endpoint provider.
     *
     * @throws RemoteAccessException thrown if occured during refreshing provider
     */
    protected void refreshEndpointProvider()
            throws RemoteAccessException {
        try {
            if (fLog.isTraceEnabled()) {
                fLog.trace(format("Starting refreshing endpoint provider. Bean Name: [{0}]", fBeanName));
            }

            fEndpointProvider.refresh(fRemoteInvocationFactory, fEndpointFactory, fBeanName);
        } catch (RemoteAccessException e) {
            Class serviceInterface = getServiceInterface();
            String name = serviceInterface.getName();
            String protocol = getProtocol();
            String message = format("No {0} remote endpoints found for service. Bean Name: [{1}] with interface [{2}]",
                    protocol, fBeanName, name);
            if (fLog.isErrorEnabled()) {
                fLog.error(message);
            }
            throw new RemoteAccessException(message, e);
        }
    }


    /**
     * Performs invocation of remote service method. Specially handles <code>toString()</code>
     * method and delegates further processing to <code>doInvoke()</code method.
     *
     * @param aInvocation method invocation to invoke remote method
     * @return result of method invocation
     * @see #doInvoke(MethodInvocation)
     */
    public Object invoke(MethodInvocation aInvocation)
            throws Throwable {
        Object result = null;
        Method method = aInvocation.getMethod();
        if (AopUtils.isToStringMethod(method)) // specially handle toString() method to avoid remote invocation
        {
            String protocol = getProtocol();
            result = format("{0} invoker proxy for bean [{1}]", protocol, fBeanName);
        } else {
            result = doInvoke(aInvocation);
        }
        return result;
    }

    /**
     * First obtains endpoint for invocation. If exception occured during selecting
     * endpoint and is configured to refresh endpoints on connect failure,
     * tries to refresh endpoint provider and obtain endpoint again.
     * <p/>
     * If endpoint if selected, thies to invoke it. If during invocation some
     * exception occured, analyses exception and if this is connect failure,
     * tries to handle it according to specified policies.
     *
     * @param aInvocation method invocation to invoke
     * @return result of method invocation
     * @throws Throwable application logic specific exception or <code>RemoteAccessException</code>
     * @see RemoteAccessException
     * @see #setRefreshEndpointsOnConnectFailure(boolean)
     * @see #setSwitchEndpointOnFailure(boolean)
     * @see #invoke(MethodInvocation)
     * @see EndpointProvider
     */
    protected Object doInvoke(MethodInvocation aInvocation)
            throws Throwable {
        E serviceEndpoint = null;
        if (fLog.isTraceEnabled()) {
            String message = format("Starting remote method invocation. Invocation Method: [{0}]", aInvocation.getMethod());
            fLog.trace(message);
        }

        //here we try to obtain endpoint that should be used for invocation
        //第一次调用
        try {
            if (fLog.isTraceEnabled()) {
                fLog.trace("Obtaining endpoint to execute...");
            }
            //在列表中随机返回一个端点对象
            fLog.trace("开始调用方法,随机返回端点");
            serviceEndpoint = obtainEndpointToExecute();
            if (fLog.isTraceEnabled()) {
                fLog.trace("Endpoint obtained.");
            }
        } catch (RemoteLookupFailureException e) // no luck, let's try to handle this
        {
            if (fLog.isTraceEnabled()) {
                fLog.trace("Remote Lookup failure on obtaining endpoint. Starting refresh of endpoint provider");
            }

            //加载端点列表失败，刷新
            // here we try to refresh provider - probably we'll discover another endpoint available
            fLog.trace("调用发生了RemoteLookupFailureException异常，刷新RMI服务列表");
            refreshEndpointProvider();

            if (fLog.isTraceEnabled()) {
                fLog.trace("Provider refreshed");
            }

            // we assume that endpoint provider is refreshed, so here we try
            // to obtain endpoint used for invocation again
            fLog.trace("发生了RemoteLookupFailureException异常后，再次调用,随机返回端点");
            serviceEndpoint = obtainEndpointToExecute();
            if (fLog.isTraceEnabled()) {
                fLog.trace("Endpoint obtained - 2.");
            }
        }

        // here we should have endpoint, so we try perform invocation using it

        Object result = null;
        try {
            if (fLog.isTraceEnabled()) {
                fLog.trace(format("Starting invocation. Method Invocation: [{0}] Endpoint: [{1}]", aInvocation,
                        serviceEndpoint.getServiceInfo()));
            }

            result = doInvoke(aInvocation, serviceEndpoint);
        } catch (RemoteConnectFailureException ex) // here we try to handle connect failure
        {
            if (fLog.isTraceEnabled()) {
                fLog.trace("Remote connect failure on invocation", ex);
            }
            fLog.trace("发生了RemoteLookupFailureException异常后，再次调用,随机返回端点后发生RemoteConnectFailureException。进入handleRemoteConnectFailure流程");
            result = handleRemoteConnectFailure(aInvocation, ex, serviceEndpoint);
        } catch (RemoteAccessException ex) // probably this is failure, probably not
        {
            if (fLog.isTraceEnabled()) {
                fLog.trace("Remote access exception", ex);
            }
            fLog.trace("进入handleRemoteConnectFailure流程后，又有异常RemoteAccessException");
            if (isConnectFailure(ex)) // sheck whether this exception is related to connect failure
            {
                if (fLog.isTraceEnabled()) {
                    fLog.trace("Handling remote connect failure");
                }
                result = handleRemoteConnectFailure(aInvocation, ex, serviceEndpoint); // ok, try to handle it
            } else // this is not cluster4spring related exception, but exception from remote service
            {
                if (fLog.isTraceEnabled()) {
                    fLog.trace("Throwing service exception...");
                }
                throwServiceException(ex);
            }
        }
        return result;
    }

    /**
     * Utility method that is used to throw reason of RemoteAccessException (if any) or
     * given exception itself.
     *
     * @param aException exception to analyze
     * @throws Throwable reason of remote exception or given exception
     * @see #doInvoke(MethodInvocation)
     */
    protected void throwServiceException(RemoteAccessException aException)
            throws Throwable {
        Throwable reason = aException.getCause();
        if (reason == null) {
            throw aException;
        } else {
            throw reason;
        }
    }

    /**
     * Handles remote connection failure according to specified policies. First, it
     * marks failed endpoint as invalid via <code>EndpointProvider</code>.
     * Actual processing of failure depends on current configuration:
     * <ul>
     * <li>If is configured to refresh on connect failure, tries to refresh <code>EndpointProvider</code>
     * and perform invocation again using fresh endpoint.
     * <li>If is configured to switch endpoint of connect failure, performs invocation again
     * using different endpoint
     * <li>If no one options mentioned above is not set, simply rethrows the original exception.
     * </ul>
     *
     * @param aInvocation      the aInvocation that failed
     * @param aException       the exception raised on remote aInvocation
     * @param aServiceEndpoint service endpoint with failed invocation
     * @return the result value of the new aInvocation, if succeeded
     * @throws Throwable an exception raised by the new aInvocation, if failed too.
     * @see #markServiceInvalid(Endpoint)
     * @see #setRefreshEndpointsOnConnectFailure(boolean)
     * @see #setSwitchEndpointOnFailure(boolean)
     * @see #doInvoke(MethodInvocation)
     */
    protected Object handleRemoteConnectFailure(MethodInvocation aInvocation, Exception aException, E aServiceEndpoint)
            throws Throwable {
        // first, we mark failed endpoint invalid
        SI serviceInfo = aServiceEndpoint.getServiceInfo();
        markServiceInvalid(aServiceEndpoint);

        Object result = null;
        if (fRefreshEndpointsOnConnectFailure) // we need to refresh on failure
        {
            if (fLog.isDebugEnabled()) {
                String protocol = getProtocol();
                String message = format("Could not connect to {0} service [{1}] on [{2}] - refreshing and retrying", protocol,
                        fBeanName, serviceInfo);
                fLog.debug(message, aException);
            } else if (fLog.isWarnEnabled()) {
                String protocol = getProtocol();
                String message = format("Could not connect to {0} service [{1}] on [{2}]- refreshing and retrying", protocol,
                        fBeanName, serviceInfo);
                fLog.warn(message);
            }

            // here we refresh provider and retry
            result = refreshAndRetry(aInvocation);
        } else if (fSwitchEndpointOnFailure) // we need to switch to another enpoint on failure
        {
            if (fLog.isDebugEnabled()) {
                String protocol = getProtocol();
                String message = format("Could not connect to {0} service [{1}] on [{2}] - switching and retrying", protocol,
                        fBeanName, serviceInfo);
                fLog.debug(message, aException);
            }
            // since original endpoint already marked as invalid, we need to simply retry
            result = retry(aInvocation);
        } else // failed, simply rethrow original exception
        {
            throw aException;
        }
        return result;
    }

    /**
     * Refreshes <code>EndpointProvider</code> and tries to invoke invocation again.
     * Called by invoke on connect failure.
     *
     * @param aInvocation the AOP method aInvocation
     * @return the aInvocation result, if any
     * @throws Throwable in case of aInvocation failure
     * @see #handleRemoteConnectFailure(org.aopalliance.intercept.MethodInvocation, Exception, Endpoint)
     * @see #doInvoke(org.aopalliance.intercept.MethodInvocation)
     */
    protected Object refreshAndRetry(MethodInvocation aInvocation)
            throws Throwable {
        if (fLog.isTraceEnabled()) {
            String message = format("Refreshing and retrying. Method Invocation: [{0}]", aInvocation.getMethod());
            fLog.trace(message);
        }

        // first we inform endpoint provider that it should refresh itself
        refreshEndpointProvider();

        // and now we simply invoke remote service again
        Object result = doInvoke(aInvocation);
        return result;
    }

    /**
     * Invokes given method invocation.  Called by invoke on connect failure.
     *
     * @param aInvocation the AOP method aInvocation
     * @return the aInvocation result, if any
     * @throws Throwable in case of aInvocation failure
     * @see #handleRemoteConnectFailure(org.aopalliance.intercept.MethodInvocation, Exception, Endpoint)
     * @see #doInvoke(org.aopalliance.intercept.MethodInvocation)
     */
    protected Object retry(MethodInvocation aInvocation)
            throws Throwable {
        if (fLog.isTraceEnabled()) {
            String message = format("Retrying. Method Invocation: [{0}]", aInvocation.getMethod());
            fLog.trace(message);
        }

        // here we simply try to invoke service again. Here we assume that
        // endpoint failed before is already marked as invalid, so EndpointProvider
        // should return different one
        Object result = doInvoke(aInvocation);
        return result;
    }

    /**
     * Marks service denoted by given endpoint as invalid by calling by notifying <code>EndpointProvider</code>
     *
     * @param aServiceEndpoint service endpoint that should be invalidated
     * @see #doInvoke(MethodInvocation)
     */
    protected void markServiceInvalid(E aServiceEndpoint) {
        if (fLog.isInfoEnabled()) {
            SI serviceInfo = aServiceEndpoint.getServiceInfo();
            String protocol = getProtocol();
            String message = format("Unable to invoke {0} service [{1}] on [{2}] - marking URL invalid", protocol, fBeanName,
                    serviceInfo);
            fLog.info(message);
        }

        fLog.trace("将rmi服务端点标识为无效："+aServiceEndpoint.fServiceInfo.fServiceURL);
        fEndpointProvider.markInvalid(fBeanName, aServiceEndpoint);
    }

    /**
     * Returns protocol which identifies interceptor. Used for logging.
     *
     * @return protocol name
     */
    protected abstract String getProtocol();

    /**
     * Creates default endpoint factory used to create endpoints. Concrete implementation
     * of interceptor will implement this method to impl particular cluster4spring protocol.
     *
     * @return EndpointFactory that should be used if one is is not specified explicitely
     */
    protected abstract EndpointFactory<E, SI> createDefaultEndpointFactory();

    /**
     * Utility method that is used to determine whether exception represents connect
     * failure or is "normal" application logic specific exception.
     *
     * @param aException exception to analyze
     * @return <code>true</code> if given exception represents connect failure
     */
    protected abstract boolean isConnectFailure(RemoteAccessException aException);

    /**
     * Performs invocation of given method invocation using given service endpoint
     *
     * @param aInvocation      the AOP method aInvocation
     * @param aServiceEndpoint endpoint used for remote service invocation
     * @return the aInvocation result, if any
     * @throws Throwable in case of aInvocation failure
     */
    protected abstract Object doInvoke(MethodInvocation aInvocation, E aServiceEndpoint)
            throws Throwable;

    /**
     * Provides endpoint that should be used for remote service invocation.
     * Simply delegates providing endpoints to used <code>EndpointProvider</code>.
     *
     * @return endpoint to invoke
     * @throws RemoteLookupFailureException thrown if EndpointProvider is unable to
     *                                      provide endpoint
     */
    protected E obtainEndpointToExecute()
            throws RemoteLookupFailureException {
        E result = null;
        try {
            result = fEndpointProvider.getEndpoint(fRemoteInvocationFactory, fEndpointFactory, fBeanName);

            if (fLog.isInfoEnabled()) {
                Class serviceInterface = getServiceInterface();
                String name = serviceInterface.getName();
                SI serviceInfo = result.getServiceInfo();
                String protocol = getProtocol();
                String message = format("{0} endpoint selected to invocation -  bean [{1}] on URL [{2}] with interface [{3}]",
                        protocol, fBeanName, serviceInfo, name);
                fLog.info(message);
            }
        } catch (RemoteAccessException e) {
            throwRemoteLookupFailureException(e);
        } catch (Throwable ex) {
            throwRemoteLookupFailureException(ex);
        }
        return result;
    }

    /**
     * Utility name used to throw <code>RemoteLookupFailureException</code>
     *
     * @param aException original exception
     * @throws RemoteLookupFailureException thrown exception
     */
    protected void throwRemoteLookupFailureException(Throwable aException)
            throws RemoteLookupFailureException {
        Class serviceInterface = getServiceInterface();
        String name = serviceInterface.getName();
        String protocol = getProtocol();
        String message = format("{0} lookup for bean [{1}] with interface [{2}] failed ", protocol, fBeanName, name);
        throw new RemoteLookupFailureException(message, aException);
    }

    /**
     * Set whether to refresh the endpoints on connect failure.
     * <p/>
     * Default is <code>false</code>.
     * <p/>
     * Can be turned on to allow for hot restart of the RMI server.
     * If a cached RMI stub throws an RMI exception that indicates a
     * remote connect failure, a fresh proxy will be fetched and the
     * invocation will be retried.
     *
     * @param aRefreshOnFailure value of policy, if true - interceptor will
     *                          refresh service endpoints if remote failure occured.
     */
    public void setRefreshEndpointsOnConnectFailure(boolean aRefreshOnFailure) {
        fRefreshEndpointsOnConnectFailure = aRefreshOnFailure;
    }

    /**
     * Returns specifies whether different endpoint should be selected if connection failure occurs
     *
     * @return true if endpoint switch should be performed
     */
    public boolean isSwitchEndpointOnFailure() {
        return fSwitchEndpointOnFailure;
    }

    /**
     * Set whether to switch endponts on connect failure (ignore connect failure and simply
     * select different endpoint, if any).
     * <p/>
     * Default is <code>true</code>
     *
     * @param aSwitchEndpointOnFailure specifies whether different endpoint shoudl be selected if connection failure occurs
     */
    public void setSwitchEndpointOnFailure(boolean aSwitchEndpointOnFailure) {
        fSwitchEndpointOnFailure = aSwitchEndpointOnFailure;
    }

    /**
     * Sets whether endpoints should be refreshed on startup
     * <p/>
     * Default is <code>true</code>
     *
     * @param aRefreshEndpointsOnStartup specifies whether endpoints should be refreshed on startup
     */
    public void setRefreshEndpointsOnStartup(boolean aRefreshEndpointsOnStartup) {
        fRefreshEndpointsOnStartup = aRefreshEndpointsOnStartup;
    }

    /**
     * Sets name of proxy bean registered in spring context
     *
     * @param aName name of proxy bean
     */
    public void setBeanName(String aName) {
        fBeanName = aName;
    }

    /**
     * Sets provider for endpoints used for remote services invocation
     *
     * @param aEndpointProvider provider for endpoints
     */
    protected void setEndpointProvider(EndpointProvider<E, SI> aEndpointProvider) {
        fEndpointProvider = aEndpointProvider;
    }

    /**
     * Returns factory used to create endpoints
     *
     * @return factory
     */
    protected EndpointFactory<E, SI> getEndpointFactory() {
        return fEndpointFactory;
    }

    /**
     * Sets factory used to create endpoints. If factory is not set explicitely,
     * default one will be created.
     *
     * @param aEndpointFactory factory used to create endpoints
     * @see #createDefaultEndpointFactory()
     */
    protected void setEndpointFactory(EndpointFactory<E, SI> aEndpointFactory) {
        fEndpointFactory = aEndpointFactory;
    }

    /**
     * Returns name of proxy bean registered in spring context
     *
     * @return name of proxy bean
     */
    public String getBeanName() {
        return fBeanName;
    }

    /**
     * Returns trues if endpoints provider should be refreshed if connection failure occurs
     *
     * @return specifies whether endpoints provider should be refreshed if connection failure occurs
     */
    public boolean isRefreshEndpointsOnConnectFailure() {
        return fRefreshEndpointsOnConnectFailure;
    }

    /**
     * Returns true if endpoints should be refreshed on startup
     *
     * @return true if endpoints should be refreshed on startup
     */
    public boolean isRefreshEndpointsOnStartup() {
        return fRefreshEndpointsOnStartup;
    }

}
