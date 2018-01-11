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

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationFactory;

/**
 * Provides endpoints for remote service. Since the same service could exists in
 * several locations, it is possible that there will be several endpoints for the
 * same service available. This class provides endpoint via which remote call will
 * be executed (based on list of available service locations).
 * <p/>
 * Since in the distributed environment some servers that provide service could become
 * unavailable, endpoints may become invalid. If cluster4spring-related exception occured
 * during endpoint invocation, the calling code may be aware about this fact and
 * mark particular endpoint invalid (so it will not  be selected during subsequent
 * invocations of service). As side effect of marking service invalid (if service is
 * autodiscovered), appropriate network notification could be issued to other
 * clients in cluster which use the same remote services.
 * <p/>
 * If there are several endpoints for the same service exist, the problem of selecting
 * particular one of them for invocation occurs. In general, <code>EndpointProvider</code>
 * delegates selection of endpoint to invocation to appropriate <code>EndpointSelectionPolicy</code>
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E> type of endpoints that could be created by this factory
 * @see EndpointSelectionPolicy
 */

public interface EndpointProvider<E extends Endpoint<SI>, SI extends ServiceMoniker> {
    /**
     * Returns endpoint that should be used for remote service invocation
     *
     * @param aRemoteInvocationFactory factory used to create remote invocations
     * @param aEndpointFactory factory used to create endpoints
     * @param aBeanName name of bean that is used as proxy for remote service
     * @return endpoint that should be used to remote invocation
     * @throws RemoteAccessException thrown if endpoint could not be selected of if
     * there are not currently available endpoints for remote service
     */
    public E getEndpoint(RemoteInvocationFactory aRemoteInvocationFactory,
                         EndpointFactory<E, SI> aEndpointFactory,
                         String aBeanName)
            throws RemoteAccessException;

    /**
     * Marks given endpoint invalid. This endpoint will not be later used for methods invocation.
     *
     * @param aBeanName name of bean that is used as proxy for remote service
     * @param aEndpoint endpoint to be marked invalid
     */
    public void markInvalid(String aBeanName, E aEndpoint);

    /**
     * Refreshes internal state of provider. As result of this method call, provider may
     * lookup and create underlying endpoints for remote service, update own internal
     * caches etc.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocation
     * @param aEndpointFactory factory used to create endpoints
     * @param aBeanName name of bean that is used as proxy for remote service
     * @throws RemoteAccessException throw if exception occured during refreshing
     */
    public void refresh(RemoteInvocationFactory aRemoteInvocationFactory,
                        EndpointFactory<E, SI> aEndpointFactory,
                        String aBeanName)
            throws RemoteAccessException;
}
