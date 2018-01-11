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

import org.softamis.cluster4spring.rmi.support.AbstractRmiShortFormProxyFactoryBean;
import org.softamis.cluster4spring.rmi.support.RmiEndpoint;
import org.softamis.cluster4spring.support.ServiceMoniker;
import org.softamis.cluster4spring.support.provider.SingleUrlEndpointProvider;

/**
 * Implementation of <code>RmiProxyFactoryBean</code> that allows more compact form
 * of describing bean properteis in configuration xml. This implementation does not
 * accept endpoint provider, but creates it internally. Due to this, class has several
 * additional properties that are used to setup <code>EndpointProvider</code>
 * <p/>
 * Current implementation is used to create <code>SingleUrlEndpointProvider</code>
 * internally.
 * <p/>
 * While this factory may be considered less flexible comparing to usual
 * combination (RmiProxyFactoryBean/SingleUrlEndpointProvider), it can be defined in
 * configuration files in more compact form.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @see SingleUrlEndpointProvider
 */

public class RmiSingleUrlProxyFactoryBean<SI extends ServiceMoniker>
        extends AbstractRmiShortFormProxyFactoryBean<SI> {
    protected SI fServiceUrl = null;

    protected SingleUrlEndpointProvider<RmiEndpoint<SI>, SI> createEndpointProvider() {
        SingleUrlEndpointProvider<RmiEndpoint<SI>, SI> result = new SingleUrlEndpointProvider<RmiEndpoint<SI>, SI>();
        return result;
    }


    public RmiSingleUrlProxyFactoryBean() {
        super();
    }

    @Override
    public void afterPropertiesSet()
            throws Exception {
        SingleUrlEndpointProvider<RmiEndpoint<SI>, SI> provider = createEndpointProvider();
        fEndpointProvider = provider;
        provider.setCacheEndpoints(fCacheEndpoints);
        provider.setServiceMoniker(fServiceUrl);
        provider.afterPropertiesSet();
        super.afterPropertiesSet();
    }

    /**
     * Returns information needed to discover remote service
     *
     * @return service info
     */
    public String getServiceUrl() {
        String result = fServiceUrl.getServiceURL();
        return result;
    }

    public void setServiceUrl(String aServiceURL) {
        fServiceUrl = (SI) new ServiceMoniker(aServiceURL);
    }
}
