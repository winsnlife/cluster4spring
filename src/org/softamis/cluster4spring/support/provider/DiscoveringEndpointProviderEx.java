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

import java.util.*;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.softamis.net.exchange.spi.ItemEntry;
import org.softamis.net.exchange.spi.RegistryEventProcessor;
import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.EndpointFactory;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Implementation of <code>EndpointProvider</code> that automatically discovers
 * services based on service name and service group. Such auto-discoverable
 * services are published via appropriate <code>ServicePublisher</code>.
 * To discover urls for service, class relies on <code>ConsumingRegistry</code>
 * that handles details of networking communications.
 * <p/>
 * While <code>DiscoveringEndpointProvider</code> determines when refresh of
 * endpoints should be performed (if configured to cache endpoints) based on
 * state of networking registry, this class (if configured to cache endpoints)
 * acts as listener of <code>ConsumingRegistry</code> and is able to create/remove
 * service endpoints as soon as appropriate service url is registered/unregistered
 * in distributed registry.
 * <p/>
 * If configured not to cache endpoints, it behaves exatly as <code>DiscoveringEndpointProvider</code>
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E> type of endpoints that could be created by this factory
 * @see org.softamis.net.registry.ConsumingRegistry
 * @see #setCacheEndpoints(boolean)
 */

public class DiscoveringEndpointProviderEx<E extends Endpoint<SI>, SI extends ServiceMoniker>
        extends DiscoveringEndpointProvider<E, SI>
        implements RegistryEventProcessor<String, SI> {
    /**
     * Option that specifies whether provider should register itself as listener for <code>ConsumingRegistry</code>
     */
    protected boolean fListenRegistry = true;

    /**
     * Factory used to create remote invocations
     */
    protected RemoteInvocationFactory fRemoteInvocationFactory = null;

    /**
     * Factory used to create endpoints
     */
    protected EndpointFactory<E, SI> fEndpointFactory = null;

    // utility flag that defines whether it's still necessary to check ConsumingRegistry state
    protected boolean fCheckRegistryDirty = true;

    /**
     * Callback for processing {@link org.softamis.net.exchange.CommunicationHelper#COMMAND_ITEM_UNREGISTERED} notification.
     * If url is registered for service that is processed by given provider, removes endpoint from endpoints cache.
     *
     * @param aEntry recevied item entry
     */
    public void processItemUnregistered(ItemEntry<String, SI> aEntry) {
        if (isApplicableEntry(aEntry)) {
            SI serviceInfo = aEntry.getValue();
            removeEndpointFromCache(serviceInfo);
        }
    }

    /**
     * Callback for processing {@link org.softamis.net.exchange.CommunicationHelper#COMMAND_ITEM_REGISTERED} notification.
     * If url is registered for service that is processed by given provider, creates endpoint for obtained url and
     * adds it to endpoints cache.
     *
     * @param aEntry recevied item entry
     */
    public void processItemRegistered(ItemEntry<String, SI> aEntry) {
        if (isApplicableEntry(aEntry)) {
            createAndAddEndpointToCache(aEntry);
        }
    }

    /**
     * Creates endpoint and adds it to endpoints cache based on service key and service url. Endpoint is added to
     * cache only if there is no endpoint for given url.
     * If endpoint cannot be created for obtained service, error is silently ignored.
     *
     * @param aEntry entry that contains information about service key and service url.
     */
    protected void createAndAddEndpointToCache(ItemEntry<String, SI> aEntry) {
        SI serviceInfo = aEntry.getValue();
        E newEndpoint = null;
        try {
            newEndpoint = doCreateServiceEndpoint(fRemoteInvocationFactory, fEndpointFactory, fServiceKey, serviceInfo);
        } catch (RemoteAccessException e) {
            if (fLog.isErrorEnabled()) {
                fLog.error("Unable to create service endpoint during item registration", e);
            }
        }
        if (newEndpoint != null) {
            synchronized (fCacheLock) {
                List<E> endpoints = doGetCachedEndpoints();
                boolean alreadyContains = false;
                for (E endpoint : endpoints) {
                    SI endpointServiceInfo = endpoint.getServiceInfo();
                    if (endpointServiceInfo.equals(serviceInfo)) {
                        alreadyContains = true;
                        break;
                    }
                }
                if (!alreadyContains) {
                    endpoints.add(newEndpoint);
                    saveFreshEndpointsToCache(endpoints);
                }
            }
        }
    }

    /**
     * Callback for processing {@link org.softamis.net.exchange.CommunicationHelper#COMMAND_ITEM_REQUEST} notification - typically will be processed by
     * {@link org.softamis.net.registry.ProvidingRegistry}. In this implementation intentionally does nothing.
     *
     * @param aEntry recevied item entry
     */
    public void processItemsRequest(ItemEntry<String, SI> aEntry) {
        // intentionally does nothing
    }

    /**
     * Callback for processing {@link org.softamis.net.exchange.CommunicationHelper#COMMAND_ITEM_INVALID} notification
     * If url is registered for service that is processed by given provider, removes endpoint from endpoints cache.
     *
     * @param aEntry recevied item entry
     */
    public void processItemInvalid(ItemEntry<String, SI> aEntry) {
        if (isApplicableEntry(aEntry)) {
            SI serviceInfo = aEntry.getValue();
            removeEndpointFromCache(serviceInfo);
        }
    }

    /**
     * Removes endpoint that corresponds to given service url from endpoints cache.
     *
     * @param aServiceInfo service url
     */
    protected void removeEndpointFromCache(SI aServiceInfo) {
        synchronized (fCacheLock) {
            List<E> endpoints = doGetCachedEndpoints();
            for (E endpoint : endpoints) {
                SI endpointServiceInfo = endpoint.getServiceInfo();
                if (endpointServiceInfo.equals(aServiceInfo)) {
                    endpoints.remove(endpoint);
                    saveFreshEndpointsToCache(endpoints);
                    break;
                }
            }
        }
    }

    /**
     * Determines whether refresh for cached endpoints is required. If configured to be listener of <code>ConsumingRegistry</code>,
     * determines whether endpoints case is empty and if so, returns true.
     * If is not configured to listen registry, simple checks registry state.
     *
     * @param aServiceKey key used to check whether cahe should be refreshed
     * @return true if endpoints cache should be refreshed.
     * @see org.softamis.net.registry.ConsumingRegistry#isDirty(java.io.Serializable)
     * @see #setListenRegistry(boolean)
     */
    @Override
    protected boolean isRefreshRequiredForCachedEndpoints(String aServiceKey) {
        boolean result = false;
        if (fCheckRegistryDirty) {
            result = super.isRefreshRequiredForCachedEndpoints(aServiceKey);
        } else {
            synchronized (fCacheLock) {
                List<E> cachedEndpoints = doGetCachedEndpoints();
                result = cachedEndpoints.isEmpty();
            }
        }
        return result;
    }

    /**
     * Utility method used to determine whether url obtained as part of <code>ConsumingRegistry</code> notification
     * is related to service which is processed by current provider.
     *
     * @param aEntry entry that contains information about service key and service url
     * @return <code>true</code> if obtained entry corresponds service processed by provider
     */
    protected boolean isApplicableEntry(ItemEntry<String, SI> aEntry) {
        String itemKey = aEntry.getKey();
        boolean result = equals(itemKey, fServiceKey);
        return result;
    }

    protected boolean equals(String aFirst, String aSecond) {
        boolean result = true;
        if (aFirst != aSecond) {
            if (aFirst != null || aSecond != null) {
                result = (aFirst != null) ? aFirst.equals(aSecond) : aSecond.equals(aFirst);
            }
        }
        return result;
    }


    @Override
    public void afterPropertiesSet()
            throws Exception {
        super.afterPropertiesSet();
        if (fListenRegistry) {
            checkRemoteInvocationFactory();
            checkEndpointsFactory();
        }
    }

    protected void checkRemoteInvocationFactory() {
        if (fRemoteInvocationFactory == null) {
            throw new IllegalArgumentException("Remote invocation factory is not set!");
        }
    }

    private void checkEndpointsFactory() {
        if (fEndpointFactory == null) {
            throw new IllegalArgumentException("Endpoints factory is not set");
        }
    }

    @Override
    protected void checkServiceName() {
        if (fServiceName == null) {
            String message = "Service name is not specified!";
            if (fLog.isErrorEnabled()) {
                fLog.error(message);
            }
            throw new IllegalArgumentException(message);
        } else {
            fServiceKey = obtainServiceKey(null);
        }
    }

    @Override
    protected void onContextRefreshed() {
        super.onContextRefreshed();
        init();
    }

    @Override
    protected void onContextClosed() {
        close();
        super.onContextClosed();
    }

    /**
     * Method used for initialization of provider. If provider is configured to listen registry and
     * to cache endpoints, it add itself as listener to registry. Otherwise, does nothing.
     * Due to this, this method <b>SHOULD BE DECLARED</b> as <code>init-method</code> of Spring bean.
     */
    public void init() {
        if (fListenRegistry) {
            if (fCacheEndpoints) {
                fClientServicesRegistry.addRegistryEventProcessor(this);
                fCheckRegistryDirty = false;
            }
        }
    }

    /**
     * If provider added itself as listener to the registry, removes listener. This method
     * <b>SHOULD BE DELARED</b> as <code>destroy-method</code> in Spring definition of bean.
     */
    public void close() {
        fClientServicesRegistry.removeRegistryEventProcessor(this);
    }

    /**
     * Returns option that specifies whether provider should register itself as listener for <code>ConsumingRegistry</code>
     *
     * @return <code>true</code> if provider listens registry
     */
    public boolean isListenRegistry() {
        return fListenRegistry;
    }


    /**
     * Sets option that specifies whether provider should register itself as listener for <code>ConsumingRegistry</code>
     *
     * @param aListenRegistry option that specifies that provider should listen registry
     */
    public void setListenRegistry(boolean aListenRegistry) {
        fListenRegistry = aListenRegistry;
    }

    /**
     * Returns factory used to create remote invocations
     *
     * @return factory used to create remote invocations
     */
    public RemoteInvocationFactory getRemoteInvocationFactory() {
        return fRemoteInvocationFactory;
    }

    /**
     * Sets factory used to create remote invocations - should be specified only provider is configured to be
     * listener for registry.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocations
     * @see #setListenRegistry(boolean)
     */
    public void setRemoteInvocationFactory(RemoteInvocationFactory aRemoteInvocationFactory) {
        fRemoteInvocationFactory = aRemoteInvocationFactory;
    }

    /**
     * Returns factory used to create endpoints.
     *
     * @return factory used to create endpoints
     */
    public EndpointFactory<E, SI> getEndpointFactory() {
        return fEndpointFactory;
    }

    /**
     * Sets factory used to create endpoints.- should be specified only provider is configured to be
     * listener for registry.
     *
     * @param aEndpointFactory factory used to create endpoints
     * @see #setListenRegistry(boolean)
     */
    public void setEndpointFactory(EndpointFactory<E, SI> aEndpointFactory) {
        fEndpointFactory = aEndpointFactory;
    }
}
