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

package org.softamis.cluster4spring.support.provider;

import static java.text.MessageFormat.format;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.EndpointFactory;
import org.softamis.cluster4spring.support.ServiceMoniker;
import org.softamis.cluster4spring.AutoDiscoveredServiceExporter;

/**
 * Base abstract class for implementation of <code>EndpointProvider</code> that
 * automatically discovers services based on service name and service group.
 * Such auto-discoverable services are published via appropriate
 * <code>ServicePublisher</code>. Class contains all basic logic related to
 * discovering, but does not include any references to underlying services
 * storage.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI>
 *            type of data used to invoke remote service (such as remote service
 *            URL)
 * @param <E>
 *            type of endpoints that could be created by this factory
 */

public abstract class AbstractDiscoveringEndpointProvider<E extends Endpoint<SI>, SI extends ServiceMoniker>
        extends AbstractUrlListEndpointProvider<E, SI> implements
        ApplicationListener {
    private static final Log fLog = LogFactory
            .getLog(AbstractDiscoveringEndpointProvider.class);
    /**
     * Name of service
     */
    protected String fServiceName = null;

    // cached service key - we don't need to calculate it since this
    // implementation of provider
    // always works with one proxy factory
    protected String fServiceKey = null;
    /**
     * Name of protocol
     */
    protected String fProtocolName = null;
    /**
     * Group to which service belong
     */
    protected String fServiceGroup = null;

    protected AbstractDiscoveringEndpointProvider() {
        super();
    }

    /**
     * <p>
     * Determines whether refresh for cached endpoints is required.
     * Implementation will rely on underlying services registry.
     * </p>
     *
     *
     * @param aServiceKey
     *            key used to check whether cahe should be refreshed
     * @return true if endpoints cache should be refreshed.
     * @see org.softamis.net.registry.ConsumingRegistry#isDirty(java.io.Serializable)
     */
    protected abstract boolean isRefreshRequiredForCachedEndpoints(
            String aServiceKey);

    /**
     * Returns set of service url's from service locations registry
     *
     * @param aServiceKey
     *            key used to obtain service urls
     * @return found service urls
     * @throws Exception
     */
    protected abstract Set<SI> obtainServiceUrlsFromRegistry(String aServiceKey)
            throws Exception;

    /**
     * Methods used to notify underlying services registry that particular
     * service location should be invalidated.
     *
     * @param aServiceKey
     *            key used to obtain service urls
     * @param aServiceInfo
     *            information about particular service location
     */
    protected abstract void invalidateServiceInRegistry(String aServiceKey,
                                                        SI aServiceInfo);

    /**
     * Invoked by Spring as part of bean lifecycle. Adds checking for protocol
     * name, consuming registry, etc.
     *
     * @throws Exception
     * @see #createDefaultEndpointSelectionPolicy()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (getProtocolName() == null) {
            throw new IllegalArgumentException(
                    "Protocol name is not specified for endpoint provider");
        }
        if (fServiceGroup == null) {
            fServiceGroup = AutoDiscoveredServiceExporter.DEFAULT_SERVICES_GROUP_PREFIX
                    + getProtocolName();
        }
        checkServiceName();
    }

    /**
     * Obtains endpoints from endpoints cache. First, tries to determine whether
     * cache should be refreshed. If one should, refreshes cache. After this
     * returns cached endpoints.
     *
     * @param aRemoteInvocationFactory
     *            factory to create remote invocations
     * @param aEndpointFactory
     *            factory used to create endpoints
     * @param aBeanName
     * @return list of endpoints
     * @see #isRefreshRequiredForCachedEndpoints(String)
     */
    @Override
    protected List<E> getCachedEndpoints(
            RemoteInvocationFactory aRemoteInvocationFactory,
            EndpointFactory<E, SI> aEndpointFactory, String aBeanName) {
        String serviceKey = obtainServiceKey(aBeanName);
        List<E> result = null;
        synchronized (fCacheLock) {
            if (isRefreshRequiredForCachedEndpoints(serviceKey)) {
                try {
                    refresh(aRemoteInvocationFactory, aEndpointFactory,
                            aBeanName);
                } catch (RemoteAccessException e) {
                    if (fLog.isErrorEnabled()) {
                        fLog.error(
                                "Exception occured during refreshing service endpoints registry",
                                e);
                    }
                }
            }
            result = super.getCachedEndpoints(aRemoteInvocationFactory,
                    aEndpointFactory, aBeanName);
        }
        return result;
    }

    /**
     * Creates list of endpoints available for service. Method obtains list of
     * urls that corresponds to required service key from the
     * <code>ConsumingRegistry</code> and based on this list tries to create
     * list of endpoints.
     * <p/>
     * If endpoint creation is failed for some service url, method notes such
     * url and after endpoints creation notifies <code>ConsumingRegistry</code>
     * that particular service url is invalid.
     *
     * @param aRemoteInvocationFactory
     *            factory used to create remote invocation
     * @param aEndpointFactory
     *            factory used to create endpoints
     * @param aBeanName
     * @return list of created endpoints
     * @throws org.springframework.remoting.RemoteAccessException
     *             throws if list of endpoints could not be created
     */
    @Override
    protected List<E> doRefreshServiceEndpointsList(
            RemoteInvocationFactory aRemoteInvocationFactory,
            EndpointFactory<E, SI> aEndpointFactory, String aBeanName)
            throws RemoteAccessException {
        if (fLog.isTraceEnabled()) {
            String message = format(
                    "Starting refreshing of service endpoints list. Bean Name: [{0}]",
                    aBeanName);
            fLog.trace(message);
        }

        // first, list of service "URL's" is obtained
        Set<SI> serviceURLs = null;
        String serviceKey = obtainServiceKey(aBeanName);
        try {
            serviceURLs = obtainServiceUrlsFromRegistry(serviceKey);
        } catch (Exception e) {
            String message = format(
                    "Unable to determine list of service URL from registry. Bean Name: [{0}]",
                    aBeanName);
            throw new RemoteAccessException(message, e);
        }

        // For which found URL we trying to create an appropriate Endpoint
        int size = serviceURLs.size();
        List<E> result = new ArrayList<E>(size);
        List<SI> invalidURLs = new ArrayList<SI>(size); // here we collect URL's
        // for which creation of
        // endpoint is failed
        for (SI serviceURL : serviceURLs) {
            E endpoint = doCreateServiceEndpoint(aRemoteInvocationFactory,
                    aEndpointFactory, aBeanName, serviceURL);
            if (endpoint != null) {
                result.add(endpoint);
            } else {
                invalidURLs.add(serviceURL);
            }
        }

        if (!invalidURLs.isEmpty()) // we have invalid urls - we should notify
        // registry about them
        {
            if (fLog.isTraceEnabled()) {
                fLog.trace(format(
                        "Invalidating not valid endpoints on refresh. Bean Name: [{0}]",
                        aBeanName));
            }

            for (SI invalidURL : invalidURLs) {
                markServiceInvalidInternal(serviceKey, serviceURLs, invalidURL);
            }
        }

        if (result.isEmpty()) {
            String message = format(
                    "Unable to determine at least one server endpoint for service. Bean Name: [{0}]",
                    aBeanName);
            throw new RemoteAccessException(message);
        }
        return result;
    }

    /**
     * Marks given endpoint invalid. This endpoint will not be later used for
     * methods invocation. Method removes endpoint from cache (if configured to
     * cache endpoints) and later marks particular service url invalid via
     * ConsumingRegistry.
     *
     * @param aBeanName
     *            name of bean that is used as proxy for remote service
     * @param aEndpoint
     *            endpoint to be marked invalid
     * @see #setCacheEndpoints(boolean)
     */
    public void markInvalid(String aBeanName, E aEndpoint) {
        if (fLog.isTraceEnabled()) {
            String message = format(
                    "Starting endpoint invalidation. Bean Name: [{0}] Endpoint Info: [{1}]",
                    aBeanName, aEndpoint.getServiceInfo());
            Throwable e = new Exception();
            e.fillInStackTrace();
            fLog.trace(message, e);
        }

        synchronized (this) {
            if (fCacheEndpoints) {
                synchronized (fCacheLock) {
                    fEndpointsCache.remove(aEndpoint);
                }
            }
            Set<SI> cachedServiceInfos = null;
            String serviceKey = obtainServiceKey(aBeanName);
            try {
                // TMP revisit this - this call may lead to not necessary
                // discovering of the
                // TMP service. Probably it's better to add some method like
                // "hasLocalItems()"
                // TMP into ConsumingRegistry
                cachedServiceInfos = obtainServiceUrlsFromRegistry(serviceKey);
            } catch (Exception e) {
                if (fLog.isErrorEnabled()) {
                    String message = format(
                            "Unable to obtain list of service urls from registry. Service Name is [{0}] bean name is [{1}]",
                            fServiceName, aBeanName);
                    fLog.error(message, e);
                }
            }
            if (cachedServiceInfos != null) {
                SI serviceUrl = aEndpoint.getServiceInfo();
                markServiceInvalidInternal(serviceKey, cachedServiceInfos,
                        serviceUrl);
            }
        }
    }

    /**
     * Handles application event
     *
     * @param aEvent
     *            event to handle
     * @see #onContextRefreshed()
     * @see #onContextClosed()
     */
    public void onApplicationEvent(ApplicationEvent aEvent) {
        try {
            if (aEvent instanceof ContextRefreshedEvent) {
                onContextRefreshed();
            } else if (aEvent instanceof ContextClosedEvent) {
                onContextClosed();
            }
        } catch (Exception e) {
            if (fLog.isErrorEnabled()) {
                String publisherName = getProtocolName();
                String message = format(
                        "Exception during processing application event. Protocol: [{0}]",
                        publisherName);
                fLog.error(message, e);
            }
        }
    }

    /**
     * Handles <code>ContextRefreshed</code> application event.
     *
     * @see #onApplicationEvent(ApplicationEvent)
     */
    protected void onContextRefreshed() {
    }

    /**
     * Handles <code>ContextClosed</code> application event. On closing context
     * clears endpoints cache
     *
     * @see #onApplicationEvent(ApplicationEvent)
     */
    protected void onContextClosed() {
        if (fLog.isTraceEnabled()) {
            fLog.trace("Clearing cached endpoints on closing context");
        }

        synchronized (fCacheLock) {
            if (fEndpointsCache != null) {
                fEndpointsCache.clear();
                fEndpointsCache = null;
            }
        }
    }

    /**
     * Returns key of service in consuming registry. This key is used to obtain
     * information about urls for that service.
     * <p/>
     * Key is composed in form <code>serviceGroup/serviceName</code>. If service
     * name is not specified during provider configuration, given name of proxy
     * bean is used instead of service name.
     *
     * @param aBeanName
     *            name of proxy bean
     * @return service key used to obtain services from
     *         <code>ConsumingRegistry</code>
     */
    protected String obtainServiceKey(String aBeanName) {
        String result = null;
        if (fServiceKey == null) // calculate service key only once - and later
        // use cached value
        {
            result = createServiceKey(aBeanName);
        } else {
            result = fServiceKey;
        }
        return result;
    }

    /**
     * Represensts implementation of actual algorythm of service key
     * calculation. Currntly, it simply creates key in the following form:
     * <code>serviceGroup/serviceName</code> This method should be overriden if
     * some custom implementation is necesary.
     *
     * @param aBeanName
     * @return key for service
     */
    protected String createServiceKey(String aBeanName) {
        String result;
        String serviceName = null;
        if (fServiceName == null) {
            serviceName = aBeanName;
        } else {
            serviceName = fServiceName;
        }
        StringBuilder tmp = new StringBuilder(100);
        tmp.append(fServiceGroup);
        tmp.append("/");
        tmp.append(serviceName);
        result = tmp.toString();
        return result;
    }

    /**
     * Notifies underlying registry that given service url under given service
     * key is invalid if service url is contained in provided set of service
     * urls
     *
     * @param aServiceKey
     *            key used to identify services url in ConsumingRegistry
     * @param aServiceInfos
     *            list of services urls
     * @param aServiceInfo
     *            service url that should be marked invalid
     */
    protected void markServiceInvalidInternal(String aServiceKey,
                                              Set<SI> aServiceInfos, SI aServiceInfo) {
        if (fLog.isTraceEnabled()) {
            String message = format(
                    "Starting processing service invalidation request. Service Key: [{0}] Service Info: [{1}]",
                    aServiceKey, aServiceInfo);
            Throwable ex = new Exception(); // just to have more informative
            // stacktrace in log
            ex.fillInStackTrace();
            fLog.trace(message, ex);
        }

        if (aServiceInfos.remove(aServiceInfo)) {
            if (fLog.isTraceEnabled()) {
                fLog.trace("Item is forwareded for registry to invalidation");
            }
            // if we were able to remove service, we need to notify registry
            invalidateServiceInRegistry(aServiceKey, aServiceInfo);
        } else {
            if (fLog.isTraceEnabled()) {
                fLog.trace(format(
                        "Service invalidation was silently skipped. Service Info: [{0}]",
                        aServiceInfo));
            }
        }
    }

    protected void checkServiceName() {
        if (fServiceName == null) {
            if (DiscoveringEndpointProvider.fLog.isWarnEnabled()) {
                DiscoveringEndpointProvider.fLog
                        .warn("Service name is not specified. Bean name will be used instead");
            }
        } else {
            fServiceKey = obtainServiceKey(null);
        }
    }

    /**
     * Returns group to which service belong
     *
     * @return group to which service belong
     */
    public String getServiceGroup() {
        return fServiceGroup;
    }

    /**
     * Sets group to which service belong. If group name is not specified
     * explicitely, it will be created by default. Name of group on client
     * should correspond to name of group on server.
     *
     * @param aServiceGroup
     *            group to which service belong
     */
    public void setServiceGroup(String aServiceGroup) {
        fServiceGroup = aServiceGroup;
    }

    /**
     * Returns name of service
     *
     * @return name of service
     */
    public String getServiceName() {
        return fServiceName;
    }

    /**
     * Sets name of service
     *
     * @param aServiceName
     *            name of service
     */
    public void setServiceName(String aServiceName) {
        fServiceName = aServiceName;
    }

    /**
     * Returns name of protocol
     *
     * @return name of protocol
     */
    public String getProtocolName() {
        return fProtocolName;
    }

    /**
     * Sets name of protocol. Used for logging and to form default name of group
     * if service group is not specified explicitely
     *
     * @param aProtocolName
     *            name of protocol
     */
    public void setProtocolName(String aProtocolName) {
        fProtocolName = aProtocolName;
    }
}
