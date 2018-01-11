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

import org.softamis.cluster4spring.AutoDiscoveredServiceExporter;

/**
 * Default implementation of <code>ServicePublisher</code> which allows to
 * specify class of exporter and name of protocol via appropriate properties. 
 *
 * @author Andrew Sazonov
 * @version 1.0
 */


public class DefaultServicePublisher<EC extends AutoDiscoveredServiceExporter>
        extends GenericServicePublisher<EC> {
    private static final Log fLog = LogFactory.getLog(DefaultServicePublisher.class);

    public DefaultServicePublisher() {
        super();
    }

    public void setAutodiscoveredExporterClass(Class<EC> aAutodiscoveredExporterClass) {
        fAutodiscoveredExporterClass = aAutodiscoveredExporterClass;
    }


    public void setProtocolName(String aProtocolName) {
        fProtocolName = aProtocolName;
    }
}
