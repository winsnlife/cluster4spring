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

package org.softamis.cluster4spring.rmi.support;

import org.softamis.cluster4spring.rmi.AbstractRmiProxyFactoryBean;
import org.softamis.cluster4spring.support.EndpointProvider;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Base abstract class to implement RMI proxy factory beans that allows compact form of
 * bean definition in application context. These providers does not accept EndpointProviders
 * set externally during configuration. Instread, they create appropriate <code>EndpointProviders</code>
 * internally. To configure create endpoint providers, such factory beans have additional set of
 * properties needed by endpoint provider.
 * <p/>
 * While factories extended from this class may be considered less flexible comparing to usual
 * combination (<code>RmiProxyFactoryBean</code>/<code>EndpointsProvider</code>), they can be
 * defined in configuration files in more compact form.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL) 
 */

public abstract class AbstractRmiShortFormProxyFactoryBean<SI extends ServiceMoniker>
        extends AbstractRmiProxyFactoryBean<SI> {
    /**
     * Indicates whether endpoints provider should cache obtained endpoints
     */
    protected boolean fCacheEndpoints = false;

    protected AbstractRmiShortFormProxyFactoryBean() {
        super();
    }

    /**
     * Sets option which specifies that discovered endpoints should cached for later use
     *
     * @param aCacheEndpoints <code>true</code> if endpoints should be cached
     * @see org.softamis.cluster4spring.support.provider.BaseEndpointProvider#setCacheEndpoints(boolean)
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

    /**
     * Sets provider for endpoints used for remote services invocation. Current implementation throws
     * <code>IllegalArgumetException</code> on attempt to specify endpoint provider externally, since
     * it's assumed that factories for more compact declaration will create all providers internally.
     *
     * @param aEndpointProvider provider
     */
    @Override
    protected void setEndpointProvider(EndpointProvider<RmiEndpoint<SI>, SI> aEndpointProvider) {
        throw new IllegalArgumentException("External assignment of endpoint provider is not supported!. " +
                "To use different provider, override method createEndpointProvider()!");


    }
}
