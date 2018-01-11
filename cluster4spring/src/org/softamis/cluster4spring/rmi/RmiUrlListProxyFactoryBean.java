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

import java.util.*;

import org.softamis.cluster4spring.rmi.support.AbstractRmiShortFormProxyFactoryBean;
import org.softamis.cluster4spring.rmi.support.RmiEndpoint;
import org.softamis.cluster4spring.support.EndpointSelectionPolicy;
import org.softamis.cluster4spring.support.ServiceMoniker;
import org.softamis.cluster4spring.support.provider.UrlListEndpointProvider;

/**
 * Implementation of <code>RmiProxyFactoryBean</code> that allows more compact form
 * of describing bean properteis in configuration xml. This implementation does not
 * accept endpoint provider, but creates it internally. Due to this, class has several
 * additional properties that are used to setup <code>EndpointProvider</code>
 * <p/>
 * Current implementation is used to create <code>UrlListEndpointProvider</code>
 * internally.
 * <p/>
 * While this factory may be considered less flexible comparing to usual
 * combination (RmiProxyFactoryBean/UrlListEndpointProvider), it can be defined in
 * configuration files in more compact form.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @see UrlListEndpointProvider
 */


public class RmiUrlListProxyFactoryBean<SI extends ServiceMoniker>
        extends AbstractRmiShortFormProxyFactoryBean<SI> {

    protected List<String> fServiceURLs = null;

    protected EndpointSelectionPolicy<RmiEndpoint<SI>, SI> fEndpointSelectionPolicy = null;

    public RmiUrlListProxyFactoryBean() {
        super();
    }

    protected UrlListEndpointProvider<RmiEndpoint<SI>, SI> createEndpointProvider() {
        UrlListEndpointProvider<RmiEndpoint<SI>, SI> result = new UrlListEndpointProvider<RmiEndpoint<SI>, SI>();
        return result;
    }


    @SuppressWarnings({"ProhibitedExceptionDeclared"})
    @Override
    public void afterPropertiesSet()
            throws Exception {
        UrlListEndpointProvider<RmiEndpoint<SI>, SI> provider = createEndpointProvider();
        fEndpointProvider = provider;
        provider.setCacheEndpoints(fCacheEndpoints);
        provider.setEndpointSelectionPolicy(fEndpointSelectionPolicy);
        //provider.setServiceMonikers(fServiceURLs);
        provider.setServiceURLs(fServiceURLs);
        provider.afterPropertiesSet();
        super.afterPropertiesSet();
    }

    public void setServiceURLs(List<String> aServiceInfos) {
        fServiceURLs = aServiceInfos;
    }

    public void setEndpointSelectionPolicy(EndpointSelectionPolicy<RmiEndpoint<SI>, SI> aServiceSelectionPolicy) {
        fEndpointSelectionPolicy = aServiceSelectionPolicy;
    }
}
