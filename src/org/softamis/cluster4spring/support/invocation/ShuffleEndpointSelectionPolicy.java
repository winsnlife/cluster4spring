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

package org.softamis.cluster4spring.support.invocation;

import static java.text.MessageFormat.format;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.remoting.RemoteAccessException;

import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.EndpointSelectionPolicy;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Policy that performs selection of endpoint for invocation from the given list of available
 * endpoints. This implementation creates copy of given list, shuffle it and selects first
 * endpoint from resulting list.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E> type of endpoints that could be created by this factory
 * @see Collections#shuffle(java.util.List)
 */

public class ShuffleEndpointSelectionPolicy<E extends Endpoint<SI>, SI extends ServiceMoniker>
        implements EndpointSelectionPolicy<E, SI> {
    protected static final Log fLog = LogFactory.getLog(ShuffleEndpointSelectionPolicy.class);

    public ShuffleEndpointSelectionPolicy() {
    }

    /**
     * Selects endpoint for invocation from the given list of endpoints. This implementation
     * creates copy of given list, shuffle it and selects firstendpoint from resulting list.
     *
     * @param aEndpoints list of endpoints available for invocation of remote service
     * @return selected endpoints
     * @throws org.springframework.remoting.RemoteAccessException
     *          throw if some error occured during seleting endpoint for invocation
     * @see Collections#shuffle(java.util.List)
     */
    public E selectServiceEndpoint(List<E> aEndpoints)
            throws RemoteAccessException {
        if (aEndpoints == null) {
            String message = "Unable to obtain service endpoint - list of endpoints is null";
            if (fLog.isErrorEnabled())
                throw new RemoteAccessException(message);
        }

        E result = null;

        if(aEndpoints.size() == 1)
            result = aEndpoints.get(0);
        else {
            Random random = new Random();
            Integer rs = random.nextInt(aEndpoints.size());
            result = aEndpoints.get(rs);
            if (fLog.isTraceEnabled()) {
                fLog.trace(format("endPoint size:{0},now get endPoint index is:{1}",aEndpoints.size(),rs));
            }
        }

        if (fLog.isTraceEnabled()) {
            SI serviceInfo = result.getServiceInfo();
            String message = format("EndPoint selected. Service URL: [{0}]", serviceInfo);
            fLog.trace(message);
        }
        return result;
    }

/* backup
    public E selectServiceEndpoint(List<E> aEndpoints)
            throws RemoteAccessException {
        if (aEndpoints == null) {
            String message = "Unable to obtain service endpoint - list of endpoints is null";
            if (fLog.isErrorEnabled())
            throw new RemoteAccessException(message);
        }

        // we create copy of originated list, shuffle it and select first item from that list
        List<E> copy = new ArrayList<E>(aEndpoints);
        Collections.shuffle(copy);
        E result = copy.get(0);
        if (fLog.isTraceEnabled()) {
            SI serviceInfo = result.getServiceInfo();
            String message = format("EndPoint selected. Service URL: [{0}]", serviceInfo);
            fLog.trace(message);
        }
        return result;
    }
*/
}
