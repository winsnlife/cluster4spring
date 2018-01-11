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

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.EndpointFactory;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Base abstract class to implement endpoint providers which provide endpoints based on
 * list of endpoints available for service and which maintains internal cache of
 * discovered endpoints.
 *
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E>  type of endpoints that could be created by this factory
 * @author Andrew Sazonov
 * @version 1.0
 */
public abstract class AbstractUrlListEndpointProvider<E extends Endpoint<SI>, SI extends ServiceMoniker>
        extends MultiURLEndpointProvider<E, SI> {
    protected static final Log fLog = LogFactory.getLog(AbstractUrlListEndpointProvider.class);

    /**
     * List which contains discovered endpoints
     */
    protected List<E> fEndpointsCache = new ArrayList<E>(8);

    /**
     * Object used as lock to access cache of endpoints
     */
    protected final Object fCacheLock = new Object();


    protected AbstractUrlListEndpointProvider() {
        super();
    }


    @Override
    public void afterPropertiesSet()
            throws Exception {
        super.afterPropertiesSet();
    }

    /**
     * Refreshes internal state of provider. If configured to cache endpoints,
     * tries to obtain list of endpoints available and saves it to cache.
     * Otherwise, silently does nothing since endpoint list will be obtained by
     * request.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocation
     * @param aEndpointFactory         factory used to create endpoints
     * @param aBeanName                name of bean that is used as proxy for remote service
     * @throws org.springframework.remoting.RemoteAccessException throw if exception occured during refreshing
     */
    public void refresh(RemoteInvocationFactory aRemoteInvocationFactory,
                        EndpointFactory<E, SI> aEndpointFactory,
                        String aBeanName)
            throws RemoteAccessException {
        if (fCacheEndpoints){ // if we are in caching mode, we'll try to resolve all endpoints and store them in cache
            if (fLog.isTraceEnabled()) {
                String message = format("Starting refreshing endpoints list. Bean Name: [{0}]", aBeanName);
                fLog.trace(message);
            }

            synchronized (fCacheLock) {
                fLog.trace("刷新RMI服务列表");
                List<E> endpoints = doRefreshServiceEndpointsList(aRemoteInvocationFactory, aEndpointFactory, aBeanName);
                fLog.trace("刷新RMI服务列表后，更新到缓存，size:"+((endpoints==null?0:endpoints.size())));
                saveFreshEndpointsToCache(endpoints);
            }

            if (fLog.isTraceEnabled()) {
                String message = format("Refreshing endpoints list finishted. Bean Name: [{0}]", aBeanName);
                fLog.trace(message);
            }
        }
    }

    /**
     * Performs actual obtaining of list of available endpoints. If configured to cache endpoints,
     * tries to provide endpoints from the endpoints cache. Otherwise, tries to create available
     * endpoints.
     *
     * @param aRemoteInvocationFactory factory used for remote invocation
     * @param aEndpointFactory         factory used for endpoints creation
     * @param aBeanName
     * @return list of endpoints available for service
     * @throws org.springframework.remoting.RemoteAccessException
     * @see #setCacheEndpoints(boolean)
     */
    @Override
    protected List<E> getServiceEndpointsList(RemoteInvocationFactory aRemoteInvocationFactory,
                                              EndpointFactory<E, SI> aEndpointFactory,
                                              String aBeanName)
            throws RemoteAccessException

    {
        List<E> result = null;
        // depending on mode, we either return cached list of endpoints or discover them for this call
        if (fCacheEndpoints) {
            fLog.trace("由于缓存了存根，从缓存中获取RMI服务列表");
            result = getCachedEndpoints(aRemoteInvocationFactory, aEndpointFactory, aBeanName);
        } else {
            fLog.trace("直接获取RMI服务列表");
            result = doRefreshServiceEndpointsList(aRemoteInvocationFactory, aEndpointFactory, aBeanName);
        }
        return result;
    }

    /**
     * Obtains endpoints from endpoints cache. If endpoints cache is null (i.e there were no
     * endpoints stored there), tries to perform refresh.
     *
     * @param aRemoteInvocationFactory factory to create remote invocations
     * @param aEndpointFactory         factory used to create endpoints
     * @param aBeanName
     * @return list of endpoints
     */
    protected List<E> getCachedEndpoints(RemoteInvocationFactory aRemoteInvocationFactory,
                                         EndpointFactory<E, SI> aEndpointFactory,
                                         String aBeanName) {
        // there are endpoints in cache, we try to obtain and create them first
        if (fEndpointsCache == null) {
            fLog.trace("缓存中没有RMI对象，现在刷新缓存：refresh(aRemoteInvocationFactory, aEndpointFactory, aBeanName);");
            refresh(aRemoteInvocationFactory, aEndpointFactory, aBeanName);
        }
        List<E> result = doGetCachedEndpoints();
        fLog.trace("从缓存中返回RMI服务对象列表，beanName:"+aBeanName +"size:"+((result==null)?0:result.size()));
        return result;
    }

    /**
     * Utility method to obtain list of enpoints from cache. Includes synchronization related code.
     *
     * @return list of cached endpoints
     */
    protected List<E> doGetCachedEndpoints() {
        List<E> result = null;
        // we always return copy of endpoints cache
        synchronized (fCacheLock) {
            result = new ArrayList<E>(fEndpointsCache);
        }
        return result;
    }

    /**
     * Save obtained endpoints to endpoints cache.
     *
     * @param aEndpoints list of endpoints to store in cache
     */
    protected void saveFreshEndpointsToCache(List<E> aEndpoints) {
        // we simply replace all content of endpoint cache by new content
        List<E> oldEndpoints = fEndpointsCache;
        synchronized (fCacheLock) {
            fEndpointsCache = aEndpoints;
        }
        if (oldEndpoints != null) {
            oldEndpoints.clear();
        }
    }

    /**
     * Creates list of endpoints available for service. Inherited classes will override it to
     * provide specific implementation.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocation
     * @param aEndpointFactory         factory used to create endpoints
     * @param aBeanName
     * @return list of created endpoints
     * @throws RemoteAccessException throws if list of endpoints could not be created
     */
    protected abstract List<E> doRefreshServiceEndpointsList(RemoteInvocationFactory aRemoteInvocationFactory,
                                                             EndpointFactory<E, SI> aEndpointFactory,
                                                             String aBeanName)
            throws RemoteAccessException;
}
