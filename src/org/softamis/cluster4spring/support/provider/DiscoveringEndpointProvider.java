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

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.softamis.net.registry.ConsumingRegistry;
import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Implementation of <code>EndpointProvider</code> that automatically discovers
 * services based on service name and service group. Such auto-discoverable
 * services are published via appropriate <code>ServicePublisher</code>.
 * To discover urls for service, class relies on <code>ConsumingRegistry</code>
 * that handles details of networking communications.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E> type of endpoints that could be created by this factory
 * @see ConsumingRegistry
 */

@SuppressWarnings({"ProhibitedExceptionDeclared"})
public class DiscoveringEndpointProvider<E extends Endpoint<SI>, SI extends ServiceMoniker>
        extends AbstractDiscoveringEndpointProvider<E, SI> {
    protected static final Log fLog = LogFactory.getLog(DiscoveringEndpointProvider.class);

    /**
     * Registry of "urls" for service
     */
    protected ConsumingRegistry<String, SI> fClientServicesRegistry = null;

    protected DiscoveringEndpointProvider() {
        super();
    }

    /**
     * Invoked by Spring as part of bean lifecycle. Adds checking for protocol name, consuming registry, etc.
     *
     * @throws Exception
     * @see #createDefaultEndpointSelectionPolicy()
     */
    @Override
    public void afterPropertiesSet()
            throws Exception {
        super.afterPropertiesSet();
        if (fClientServicesRegistry == null) {
            throw new IllegalArgumentException("Service URLs storage is not set");
        }
    }

    /**
     * Determines whether refresh for cached endpoints is required. Current implementation
     * simply checks whether <code>ConsumingRegistry</code> is dirty.
     *
     * @param aServiceKey key used to check whether cahe should be refreshed
     * @return true if endpoints cache should be refreshed.
     * @see ConsumingRegistry#isDirty(Serializable)
     */
    protected boolean isRefreshRequiredForCachedEndpoints(String aServiceKey) {
        boolean result = fClientServicesRegistry.isDirty(aServiceKey);
        return result;
    }

    @Override
    protected void saveFreshEndpointsToCache(List<E> aEndpoints) {
        super.saveFreshEndpointsToCache(aEndpoints);
    }

    protected void invalidateServiceInRegistry(String aServiceKey, SI aServiceInfo) {
        fClientServicesRegistry.markItemInvalid(aServiceKey, aServiceInfo);
    }

    /**
     * Returns set of service url's from consuming registry
     *
     * @param aServiceKey key used to obtain service urls
     * @return found service urls
     * @throws Exception
     */
    protected Set<SI> obtainServiceUrlsFromRegistry(String aServiceKey)
            throws Exception {
        Set<SI> tmp = fClientServicesRegistry.getItems(aServiceKey);
        Set<SI> result = new HashSet<SI>(tmp);
        return result;
    }

    /**
     * Returns registry of "urls" for service
     *
     * @return registry of "urls" for service
     */
    public ConsumingRegistry<String, SI> getClientServicesRegistry() {
        return fClientServicesRegistry;
    }

    /**
     * Sets registry used to obtain information about of "urls" for service
     *
     * @param aClientServicesRegistry registry of "urls" for service
     */
    public void setClientServicesRegistry(ConsumingRegistry<String, SI> aClientServicesRegistry) {
        fClientServicesRegistry = aClientServicesRegistry;
    }

}