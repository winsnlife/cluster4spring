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

import org.springframework.context.ApplicationEvent;

import org.softamis.net.registry.ConsumingRegistry;
import org.softamis.cluster4spring.rmi.support.AbstractRmiShortFormProxyFactoryBean;
import org.softamis.cluster4spring.rmi.support.RmiEndpoint;
import org.softamis.cluster4spring.support.EndpointSelectionPolicy;
import org.softamis.cluster4spring.support.ServiceMoniker;
import org.softamis.cluster4spring.support.provider.DiscoveringEndpointProvider;
import org.softamis.cluster4spring.support.provider.DiscoveringEndpointProviderEx;

/**
 * <p>Implementation of <code>RmiProxyFactoryBean</code> that allows more compact form
 * of describing bean properteis in configuration XML. This implementation does not
 * accept endpoint provider, but creates it internally.
 * Due to this, class has several additional properties that are used to
 * setup <code>EndpointProvider</code></p>
 * <p>As it follows from the class name, that class is intended to simply RmiProxyFactoryBeans which
 * utilizes discovering endpoints provider. </p>
 * <p>While this factory may be considered less flexible comparing to usual
 * combination (RmiProxyFactoryBean/DiscoveringEndpointProvider), it can be defined in
 * configuration files in more compact form.</p>
 * <p> The following sample illustrates definition of RMI proxy factory bean in
 * Spring XML:</p>
 * <code>
 * <pre>
 *
 *   &lt;bean id="remote.service" class="org.softamis.cluster4spring.rmi.RmiDiscoveringProxyFactoryBean"&gt;
 *     &lt;property name=" " value=" "/&gt;
 *   &lt;/bean&gt;
 * </pre>
 * </code>
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @see DiscoveringEndpointProviderEx
 */

public class RmiDiscoveringProxyFactoryBean<SI extends ServiceMoniker>
        extends AbstractRmiShortFormProxyFactoryBean<SI> {
    protected String fServiceGroup = null;
    protected String fServiceName = null;
    protected EndpointSelectionPolicy<RmiEndpoint<SI>, SI> fServiceSelectionPolicy = null;
    protected ConsumingRegistry<String, SI> fClientServicesRegistry = null;
    protected boolean fListenRegistry = false;

    protected DiscoveringEndpointProviderEx<RmiEndpoint<SI>, SI> createEndpointProvider() {
        return new DiscoveringEndpointProviderEx<RmiEndpoint<SI>, SI>();
    }

    @SuppressWarnings({"ProhibitedExceptionDeclared"})
    @Override
    public void afterPropertiesSet()
            throws Exception {
        // fists, we need to create endpoint provider which will be used by this proxy factory
        fEndpointProvider = createEndpointProvider();
        DiscoveringEndpointProviderEx<RmiEndpoint<SI>, SI> provider =
                (DiscoveringEndpointProviderEx<RmiEndpoint<SI>, SI>) fEndpointProvider;

        provider.setProtocolName(RmiServicePublisher.PROTOCOL_NAME);
        provider.setCacheEndpoints(fCacheEndpoints);
        provider.setClientServicesRegistry(fClientServicesRegistry);
        provider.setServiceGroup(fServiceGroup);
        provider.setListenRegistry(fListenRegistry);
        if (fServiceName != null) {
            provider.setServiceName(fServiceName);
        } else {
            provider.setServiceName(fBeanName);
        }
        provider.setEndpointSelectionPolicy(fServiceSelectionPolicy);
        super.afterPropertiesSet();
        provider.setEndpointFactory(fEndpointFactory);
        provider.setRemoteInvocationFactory(fRemoteInvocationFactory);
        // we call this explicitely to make sure that all things necessary for
        // endpoints provider is properly set
        provider.afterPropertiesSet();
    }

    @Override
    protected void doPreprocessApplicationEvent(ApplicationEvent aEvent) {
        // we simply should notify endpoints provider about this event
        DiscoveringEndpointProvider<RmiEndpoint<SI>, SI> provider =
                (DiscoveringEndpointProvider<RmiEndpoint<SI>, SI>) fEndpointProvider;
        provider.onApplicationEvent(aEvent);
    }

    /**
     * Allows to specify service group which will be used by endpoint provider
     *
     * @param aServiceGroup name of service group
     * @see org.softamis.cluster4spring.support.provider.DiscoveringEndpointProvider#setServiceGroup(String)
     */
    public void setServiceGroup(String aServiceGroup) {
        fServiceGroup = aServiceGroup;
    }

    /**
     * Allows to specify service name which will be used by endpoint provider
     *
     * @param aServiceName name of service group
     * @see org.softamis.cluster4spring.support.provider.DiscoveringEndpointProvider#setServiceName(String)
     */
    public void setServiceName(String aServiceName) {
        fServiceName = aServiceName;
    }

    public boolean isListenRegistry() {
        return fListenRegistry;
    }

    public void setListenRegistry(boolean aListenRegistry) {
        fListenRegistry = aListenRegistry;
    }

    public ConsumingRegistry<String, SI> getClientServicesRegistry() {
        return fClientServicesRegistry;
    }

    /**
     * Additional setup for discovering endpoints provider which allows to specify client services
     * registry wich is used to discover remote services.
     *
     * @param aClientServicesRegistry registry should be used by provider
     * @see org.softamis.cluster4spring.support.provider.DiscoveringEndpointProvider#setClientServicesRegistry(org.softamis.net.registry.ConsumingRegistry)
     */
    public void setClientServicesRegistry(ConsumingRegistry<String, SI> aClientServicesRegistry) {
        fClientServicesRegistry = aClientServicesRegistry;
    }

    public EndpointSelectionPolicy<RmiEndpoint<SI>, SI> getServiceProvidingPolicy() {
        return fServiceSelectionPolicy;
    }

    /**
     * Specifies policy will be used by endpoints provider for selection of particular endpoint from
     * the list of endpoints available
     *
     * @param aServiceSelectionPolicy endpoints selection policy will be used by endpoints provider
     * @see org.softamis.cluster4spring.support.provider.DiscoveringEndpointProvider#setEndpointSelectionPolicy(org.softamis.cluster4spring.support.EndpointSelectionPolicy)
     */
    public void setServiceProvidingPolicy(EndpointSelectionPolicy<RmiEndpoint<SI>, SI> aServiceSelectionPolicy) {
        fServiceSelectionPolicy = aServiceSelectionPolicy;
    }
}
