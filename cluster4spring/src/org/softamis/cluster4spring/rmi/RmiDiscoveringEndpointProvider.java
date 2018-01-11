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

package org.softamis.cluster4spring.rmi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.softamis.cluster4spring.rmi.support.RmiEndpoint;
import org.softamis.cluster4spring.support.ServiceMoniker;
import org.softamis.cluster4spring.support.provider.DiscoveringEndpointProvider;

/**
 * Implementation of <code>EndpointProvider</code> that automatically discovers RMI
 * services based on service name and service group. This implementation simply always
 * returns property protocol name {@link RmiServicePublisher#PROTOCOL_NAME} and is created
 * just for convenience - instead of it, usual <code>DiscoveringEndpointPorivder</code>
 * with explicitely specified name of procol should be used. 
 *
 * @author Andrew Sazonov
 * @version 1.0
 */

public class RmiDiscoveringEndpointProvider<SI extends ServiceMoniker>
        extends DiscoveringEndpointProvider<RmiEndpoint<SI>, SI> {
    private static final Log fLog = LogFactory.getLog(RmiDiscoveringEndpointProvider.class);

    public RmiDiscoveringEndpointProvider() {
        super();
    }

    /**
     * Returns name of protocol
     *
     * @return name of protocol
     */
    @Override
    public String getProtocolName() {
        return RmiServicePublisher.PROTOCOL_NAME;
    }
}
