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
 * Factory that is used to create service endpoints. An appropriate type of endpoint and
 * therefore factory should be implemented for every corresponging cluster4spring protocol.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E> type of endpoints that could be created by this factory
 */


public interface EndpointFactory<E extends Endpoint<SI>, SI extends ServiceMoniker> {
    /**
     * Creates endpoint for cluster4spring protocol supported by particular factory.
     *
     * @param aFactory factory used to create remote invocations
     * @param aBeanName name of bean/service
     * @param aServiceInfo information about remote service
     * @return created endpoint
     * @throws RemoteAccessException thrown if endpoint could not be created.
     */
    public E createServiceEndpoint(RemoteInvocationFactory aFactory, String aBeanName, SI aServiceInfo)
            throws RemoteAccessException;
}
