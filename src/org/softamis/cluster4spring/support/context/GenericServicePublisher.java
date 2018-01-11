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

package org.softamis.cluster4spring.support.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.softamis.net.registry.ProvidingRegistry;
import org.softamis.cluster4spring.AutoDiscoveredServiceExporter;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Generic implementation of <code>ServicePublisher</code> which uses <code>ProvidingRegistry</code>
 * as distributed storage of services locations.
 *
 * @author Andrew Sazonov
 * @version 1.0
 */

public class GenericServicePublisher<EC extends AutoDiscoveredServiceExporter>
        extends AbstractServicePublisher<EC> {
    private static final Log fLog = LogFactory.getLog(GenericServicePublisher.class);

    /**
     * Registry used to publish information about auto-discoverable services
     */
    protected ProvidingRegistry<String, ServiceMoniker> fServicesRegistry = null;

    public GenericServicePublisher() {
        super();
    }

    @Override
    public void afterPropertiesSet()
            throws Exception {
        super.afterPropertiesSet();
        if (fServicesRegistry == null) {
            throw new IllegalArgumentException("ServicesRegistry should be specified for services publisher!");
        }
    }

    @Override
    protected void doUnregisterServiceInServicesRegistry(String aServiceKey) {
        fServicesRegistry.unRegisterItem(aServiceKey);
    }

    @Override
    protected void doRegisterServiceInServicesRegistry(String aServiceKey, ServiceMoniker aMoniker) {
        fServicesRegistry.registerItem(aServiceKey, aMoniker);
    }

    /**
     * Returns registry used to publish information about auto-discoverable services
     *
     * @return registry
     */
    public ProvidingRegistry<String, ServiceMoniker> getServicesRegistry() {
        return fServicesRegistry;
    }

    /**
     * Sets registry used to publish information about auto-discoverable services
     *
     * @param aServicesRegistry registry
     */
    public void setServicesRegistry(ProvidingRegistry<String, ServiceMoniker> aServicesRegistry) {
        fServicesRegistry = aServicesRegistry;
    }
}
