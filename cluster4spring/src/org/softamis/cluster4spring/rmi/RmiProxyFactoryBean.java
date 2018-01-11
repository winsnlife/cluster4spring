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

import org.softamis.cluster4spring.support.ServiceMoniker;
import org.softamis.cluster4spring.support.EndpointProvider;
import org.softamis.cluster4spring.rmi.support.RmiEndpoint;

/**
 * <p> Proxy factory that creates proxies for remote services exported via RMI.
 * This form of describing proxy factory for rmi assumes that all corresponding
 * reference to related objects requried for property work of factory will be
 * specified explicitely in Spring XML mapping.</p>
 * <p>While such approach of defining factory is very flexible and allows tune
 * most of acpects of factory functioning very precisely, resulting XML definitition
 * may be considered slightly verbose. To overcome this, there are several
 * pre-configure variants of factory which offers predefined endpoint providers (for
 * single url, url list and discovering mode of connecting to the server) as well as
 * the set of some additional parameters. The classes of such factories are extended from
 * <code>AbstractRmiShortFormProxyFactoryBean</code>.</p> *
 *
 * @version 1.0
 * @see RmiUrlListProxyFactoryBean
 * @see RmiSingleUrlProxyFactoryBean
 * @see RmiDiscoveringProxyFactoryBean
 */

public class RmiProxyFactoryBean<SI extends ServiceMoniker>
        extends AbstractRmiProxyFactoryBean<SI> {
    public RmiProxyFactoryBean() {
        super();
    }

    @Override
    public void setEndpointProvider(EndpointProvider<RmiEndpoint<SI>, SI> aEndpointProvider) {
        super.setEndpointProvider(aEndpointProvider);
    }
}
