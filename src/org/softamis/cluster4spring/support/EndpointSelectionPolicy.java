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

import java.util.*;

import org.springframework.remoting.RemoteAccessException;

/**
 * Policy that performs selection of endpoint for invocation from the given list of available
 * endpoints. Various policies could be implemented to satisfy needs of particular application -
 * selecting first valid endpoint, selecting endpoints based on the last access time,
 * selecting endpoints randomly and so on.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E> type of endpoints that could be created by this factory
 */

public interface EndpointSelectionPolicy<E extends Endpoint<SI>, SI extends ServiceMoniker> {
    /**
     * Selects endpoint for invocation from the given list of endpoints. The logic which is used to
     * select particular endpoint if incapsulated in particular implementation of this method.
     *
     * @param aEndpoints list of endpoints available for invocation of remote service
     * @return selected endpoints
     * @throws RemoteAccessException throw if some error occured during seleting endpoint for invocation
     */
    public E selectServiceEndpoint(List<E> aEndpoints)
            throws RemoteAccessException;
}
