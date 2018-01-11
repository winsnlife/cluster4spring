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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.EndpointFactory;
import org.softamis.cluster4spring.support.EndpointProvider;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Base abstract class to implement endpoint providers.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E> type of endpoints that could be created by this factory
 * @see java.util.Collections#shuffle(java.util.List)
 */

public abstract class BaseEndpointProvider<E extends Endpoint<SI>, SI extends ServiceMoniker>
        implements EndpointProvider<E, SI> {
    protected static final Log fLog = LogFactory.getLog(BaseEndpointProvider.class);

    /**
     * option which specifies that discovered endpoints should cached for later use
     */
    protected boolean fCacheEndpoints = false;

    protected BaseEndpointProvider() {
    }

    /**
     * Low level method for creation endpoints. Delegates details of <code>Endpoint</code> creation
     * to given EndpointFactory.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocation
     * @param aEndpointFactory factory used to create endpoints
     * @param aBeanName name of servcice/bean
     * @param aServiceInfo information about service
     * @return created endpoint
     * @throws RemoteAccessException thrown by endpoint factory if occured during creation endpoint
     */
    protected E doCreateServiceEndpoint(RemoteInvocationFactory aRemoteInvocationFactory,
                                        EndpointFactory<E, SI> aEndpointFactory,
                                        String aBeanName,
                                        SI aServiceInfo)
            throws RemoteAccessException {
        if (fLog.isTraceEnabled()) {
            String message = format("Starting creation of service endpoint. Bean Name: [{0}] . Service Info: [{1}]",
                    aBeanName, aServiceInfo);
            fLog.trace(message);
        }

        // simply deletegate creation of Endpoint to factory
        E result = aEndpointFactory.createServiceEndpoint(aRemoteInvocationFactory, aBeanName, aServiceInfo);

        if (fLog.isTraceEnabled()) {
            fLog.trace("Endpoint created.");
        }

        return result;
    }

    /**
     * Sets option which specifies that discovered endpoints should cached for later use
     *
     * @param aCacheEndpoints <code>true</code> if endpoints should be cached
     */
    public void setCacheEndpoints(boolean aCacheEndpoints) {
        fCacheEndpoints = aCacheEndpoints;
    }

    /**
     * Returns option which specifies that discovered endpoints should cached for later use
     *
     * @return <code>true</code> if endpoints should be cached
     */
    public boolean isCacheEndpoints() {
        return fCacheEndpoints;
    }
}
