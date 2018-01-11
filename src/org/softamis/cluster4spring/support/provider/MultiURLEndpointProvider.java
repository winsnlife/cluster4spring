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

import org.softamis.cluster4spring.support.invocation.ShuffleEndpointSelectionPolicy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.EndpointFactory;
import org.softamis.cluster4spring.support.EndpointSelectionPolicy;
import org.softamis.cluster4spring.support.ServiceMoniker;
import org.softamis.cluster4spring.support.invocation.LastAccessTimeEndpointSelectionPolicy;

/**
 * Base abstract class to implement endpoint providers that hangle situation with
 * several endpoints available for the same service.  Since the same service could exists in
 * several locations, it is possible that there will be several endpoints for the
 * same service available.
 *
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E>  type of endpoints that could be created by this factory
 * @author Andrew Sazonov
 * @version 1.0
 */

public abstract class MultiURLEndpointProvider<E extends Endpoint<SI>, SI extends ServiceMoniker>
        extends BaseEndpointProvider<E, SI>
        implements InitializingBean {
    private static final Log fLog = LogFactory.getLog(MultiURLEndpointProvider.class);

    /**
     * Policy used to select endpoints for invocation
     */
    protected EndpointSelectionPolicy<E, SI> fEndpointSelectionPolicy = null;

    protected MultiURLEndpointProvider() {
        super();
    }

    /**
     * Invoked by Spring as part of bean lifecycle. Used to check whether
     * endpoints selection policy is specified during bean configuration. If one is not specified,
     * default endpoints selection policy is created.
     *
     * @throws Exception
     * @see #createDefaultEndpointSelectionPolicy()
     */
    public void afterPropertiesSet()
            throws Exception {
        // if policy is not explicitely specified, we'll create default one
        if (fEndpointSelectionPolicy == null) {
            createDefaultEndpointSelectionPolicy();
        }
    }

    /**
     * Returns endpoint that will be used for remote service invocation. First, method
     * obtains list of available endpoints. If list is obtained, it delegates selection
     * of endpoint for invocation to <code>EndpointSelectionPolicy</code> and returns accepted
     * endpoint.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocations
     * @param aEndpointFactory         factory used to create endpoints
     * @param aBeanName                name of bean that is used as proxy for remote service
     * @return endpoint that should be used to remote invocation
     * @throws org.springframework.remoting.RemoteAccessException thrown if endpoint could not be selected of if
     *                                                            there are not currently available endpoints for remote service
     * @see #createDefaultEndpointSelectionPolicy()
     * @see #setEndpointSelectionPolicy(org.softamis.cluster4spring.support.EndpointSelectionPolicy)
     * @see #getServiceEndpointsList(org.springframework.remoting.support.RemoteInvocationFactory, org.softamis.cluster4spring.support.EndpointFactory, String)
     */
    public E getEndpoint(RemoteInvocationFactory aRemoteInvocationFactory,
                         EndpointFactory<E, SI> aEndpointFactory,
                         String aBeanName)
            throws RemoteAccessException {
        if (fLog.isTraceEnabled()) {
            String message = format("Starting obtaining service endpoint. Bean Name: [{0}]", aBeanName);
            fLog.trace(message);
        }

        // first, we try to obtain list of available endpoints
        List<E> serviceEndpoints = getServiceEndpointsList(aRemoteInvocationFactory, aEndpointFactory, aBeanName);

        if (serviceEndpoints == null || serviceEndpoints.isEmpty()) {
            String message = format("找不到服务点点There are no service endpoints in the list available. Bean Name: [{0}]", aBeanName);
            if (fLog.isErrorEnabled()) {
                fLog.error(message);
            }
            throw new RemoteAccessException(message);
        }

        // if we are there, we have endpoints to invoke. So, now we had to select exact one that
        // will be actually invoked. We delegate this to EndpointSelectionPolicy
        E result = null;
        synchronized (this) {
            result = fEndpointSelectionPolicy.selectServiceEndpoint(serviceEndpoints);
        }

        if (fLog.isTraceEnabled()) {
            if (result == null) {
                String message = format("No endpoint selected. Bean Name: [{0}]", aBeanName);
                fLog.trace(message);
            } else {
                String message = format("Endpoint selected. Bean Name: [{0}] Endpoint Info: [{1}]", aBeanName, result.getServiceInfo());
                fLog.trace(message);
            }
        }
        return result;
    }

    /**
     * Performs actual obtaining of list of available endpoints. Inherited classes will
     * implement it to provide implementation-specific functionality.
     *
     * @param aRemoteInvocationFactory factory used for remote invocation
     * @param aEndpointFactory         factory used for endpoints creation
     * @param aBeanName
     * @return list of endpoints available for service
     * @throws RemoteAccessException
     */
    protected abstract List<E> getServiceEndpointsList(RemoteInvocationFactory aRemoteInvocationFactory,
                                                       EndpointFactory<E, SI> aEndpointFactory,
                                                       String aBeanName)
            throws RemoteAccessException;

    /**
     * Creates default endpoints selection policy if one is not specified explicitely.
     *
     * @see LastAccessTimeEndpointSelectionPolicy
     */
    protected void createDefaultEndpointSelectionPolicy() {
        // by default, we'll select endpoint that was not used for longest time.
        // potentially, this should balance workload between all endpoints
        fEndpointSelectionPolicy = new ShuffleEndpointSelectionPolicy<E, SI>();
    }

    /**
     * Sets endpoint selection policy used to select endpoints from the list of endpoints available
     *
     * @param aEndpointSelectionPolicy
     */
    public void setEndpointSelectionPolicy(EndpointSelectionPolicy<E, SI> aEndpointSelectionPolicy) {
        fEndpointSelectionPolicy = aEndpointSelectionPolicy;
    }
}
