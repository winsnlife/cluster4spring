/******************************************************************************
 * Copyright(c) 2005-2007 SoftAMIS (http://www.soft-amis.com)                 *
 * All Rights Reserved.                                                       *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * You may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *   http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package org.softamis.cluster4spring.support.invocation;

import java.text.MessageFormat;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.remoting.RemoteAccessException;

import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.EndpointSelectionPolicy;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Policy that performs selection of endpoint for invocation from the given list
 * of available endpoints. This implementation simply selects the endpoint with
 * minimal last access time.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param < SI >
 *            type of data used to invoke remote service (such as remote service
 *            URL)
 * @param < E >
 *            type of endpoints that could be created by this factory
 * @see Endpoint#getLastAccessTime
 */

public class InTurnEndpointSelectionPolicy<E extends Endpoint, SI extends ServiceMoniker>
        implements EndpointSelectionPolicy<E, SI> {

    protected static final Log fLog = LogFactory
            .getLog(InTurnEndpointSelectionPolicy.class);

    public InTurnEndpointSelectionPolicy() {
    }

    /**
     * Selects endpoint for invocation from the given list of endpoints based on
     * value of last access time for every endpoint. Endpoint with minimal value
     * is selected.
     *
     * @param aEndpoints
     *            list of endpoints available for invocation of remote service
     * @return selected endpoints
     * @throws org.springframework.remoting.RemoteAccessException
     *             throw if some error occured during seleting endpoint for
     *             invocation
     * @see org.softamis.cluster4spring.support.Endpoint#getLastAccessTime
     */
    public E selectServiceEndpoint(List<E> aEndpoints)
            throws RemoteAccessException {
        // check incoming params
        if (aEndpoints == null) {
            String message = "Unable to obtain service endpoint - list of endpoints is null";
            if (fLog.isErrorEnabled()) {
                fLog.error(message);
            }
            throw new RemoteAccessException(message);
        }

        //找出时间戳最小那个
        E lastAccessedEndpoint = aEndpoints.min { it.lastAccessTime }

        // check whether we actually found endpoint
        if (lastAccessedEndpoint == null) {
            String message = "Unable to obtain service endpoint";
            if (fLog.isErrorEnabled()) {
                fLog.error(message);
            }
            throw new RemoteAccessException(message);
        }

        //更新时间戳
        lastAccessedEndpoint.setLastAccessTime(new Date().getTime());
        E result = lastAccessedEndpoint;

        // debug logging
        if (fLog.isTraceEnabled()) {
            SI serviceInfo = result.getServiceInfo();
            String message = MessageFormat.format("EndPoint selected. Service URL: [{0}]", serviceInfo);
            fLog.trace(message);
        }
        return result;
    }
}
