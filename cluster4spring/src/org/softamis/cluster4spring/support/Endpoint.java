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

import java.io.Serializable;
import java.lang.reflect.Method;

import static java.text.MessageFormat.format;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.aopalliance.intercept.MethodInvocation;

/**
 * Abstract class that encapsulates low-level logic of calling underlying remote service.
 * Roughly speaking, it could be considered as client-side proxy for remote server-side service.
 * An appropriate type of endpoints should be implemented for every corresponging
 * cluster4spring protocol.
 * <p/>
 * Instance of this class is created by appropriate <code>ProxyFactoryBean</code>
 * which is used to create proxies to remote service.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 */

public abstract class Endpoint<SI extends Serializable> {
    protected static final Log fLog = LogFactory.getLog(Endpoint.class);

    /**
     * Key used to identify endpoint
     */
    protected String fEndpointKey = null;

    /**
     * information about remote service (for example, URL of remote service)
     */
    protected SI fServiceInfo = null;

    /**
     * Custom data which could be associated with endpoint. Such a custom data could be
     * used, for example, by <code>EndpointSelectionPolicy</code>
     */
    protected Map<String, Object> fAttributes = null;

    /**
     * Factory used to create remote invocations
     */
    protected RemoteInvocationFactory fRemoteInvocationFactory = null;

    /**
     * Time of last invocation of remote service via this endpoint
     */
    protected long fLastAccessTime = Long.MIN_VALUE;

    /**
     * Creates endpoint.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocations
     */
    protected Endpoint(RemoteInvocationFactory aRemoteInvocationFactory) {
        fRemoteInvocationFactory = aRemoteInvocationFactory;
    }

    /**
     * Creates endpoint.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocations
     * @param aEndpointKey key of endpoint
     * @param aServiceInfo information about service that could be used to perform service invocation
     */
    protected Endpoint(RemoteInvocationFactory aRemoteInvocationFactory, String aEndpointKey, SI aServiceInfo) {
        this(aRemoteInvocationFactory);
        fServiceInfo = aServiceInfo;
        fEndpointKey = aEndpointKey;
    }

    /**
     * Performs invocation of given <code>MethodInvocation</code>. After invocation,
     * updates <pre>lastAccessTime</code> of endpoint.
     *
     * @param aMethodInvocation method invocation to invoke
     * @return object which represents result of method invocation
     * @throws Throwable
     */
    @SuppressWarnings({"ProhibitedExceptionDeclared"})
    public Object invoke(MethodInvocation aMethodInvocation)
            throws Throwable {
        if (fLog.isTraceEnabled()) {
            Method method = aMethodInvocation.getMethod();
            String methodName = method.getName();
            String message =
                    format("Starting endpoint invocation. Endpoint Key: [{0}]. Method Name: [{1}]", fEndpointKey, methodName);
            fLog.trace(message);
        }
        long lastAccessTime = System.currentTimeMillis();
        setLastAccessTime(lastAccessTime);

        Object result = doInvoke(aMethodInvocation);

        if (fLog.isTraceEnabled()) {
            String message = format("Endpoint invocation finished. Endpoint Key: [{0}]", fEndpointKey);
            String message1 = format("Endpoint invocation finished.更新了最新访问时间：lastAccessTime. Endpoint Key: [{0}]", fEndpointKey);
            fLog.trace(message);
            fLog.trace(message1);
        }
        return result;
    }

    /**
     * Perform invocation of given MethoInvocation taking into consideration details of
     * concrete cluster4spring protocol. This method should be implemented for particular
     * remote protocol impl.
     *
     * @param aMethodInvocation method invocation to invoke
     * @return result of invocation, if any
     * @throws Throwable exception that could occur during invocation
     */
    @SuppressWarnings({"ProhibitedExceptionDeclared"})
    protected abstract Object doInvoke(MethodInvocation aMethodInvocation)
            throws Throwable;

    /**
     * Returns custom attributes which could be associated with endpoint. Such a custom data could be
     * used, for example, by <code>EndpointSelectionPolicy</code> to implement algorithm of
     * endpoints selection
     *
     * @return custom data
     */
    public Map<String, Object> getAttributes() {
        return fAttributes;
    }

    /**
     * Sets custom attributes which could be associated with endpoint.
     *
     * @param aAttributes custom data
     */
    public void setAttributes(Map<String, Object> aAttributes) {
        fAttributes = aAttributes;
    }

    /**
     * Create a new RemoteInvocation object for the given AOP method invocation.
     * The default implementation delegates to the RemoteInvocationFactory.
     * <p>Can be overridden in subclasses to provide custom RemoteInvocation
     * subclasses, containing additional invocation parameters like user credentials.
     * Note that it is preferable to use a custom RemoteInvocationFactory which
     * is a reusable strategy.
     *
     * @param aMethodInvocation the current AOP method invocation
     * @return the RemoteInvocation object
     * @see org.springframework.remoting.support.RemoteInvocationFactory#createRemoteInvocation
     */
    protected RemoteInvocation createRemoteInvocation(MethodInvocation aMethodInvocation) {
        RemoteInvocation result = fRemoteInvocationFactory.createRemoteInvocation(aMethodInvocation);
        return result;
    }

    /**
     * Returns information about remote service (for example, URL of remote service)
     *
     * @return information about remote service
     */
    public SI getServiceInfo() {
        return fServiceInfo;
    }

    /**
     * Returns time of last invocation of remote service via this endpoint. This time could be
     * used for statistical purposes and also for selection of particular endpoint if there are
     * serveral ones exists for the same remote service.
     *
     * @return time of last invocation of remote service via this endpoint
     * @see org.softamis.cluster4spring.support.invocation.LastAccessTimeEndpointSelectionPolicy
     */
    public long getLastAccessTime() {
        return fLastAccessTime;
    }

    /**
     * Sets time of last invocation of remote service via this endpoint
     *
     * @param aLastAccessTime time
     */
    public void setLastAccessTime(long aLastAccessTime) {
        fLastAccessTime = aLastAccessTime;
    }

    /**
     * Returns key for endpoint
     *
     * @return key for endpoint
     */
    public String getEndpointKey() {
        return fEndpointKey;
    }

    /**
     * Sets key for endpoint
     *
     * @param aEndpointKey key for endpoint
     */
    public void setEndpointKey(String aEndpointKey) {
        fEndpointKey = aEndpointKey;
    }
}
