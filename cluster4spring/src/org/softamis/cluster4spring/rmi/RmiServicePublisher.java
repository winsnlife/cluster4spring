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

import org.softamis.cluster4spring.support.context.GenericServicePublisher;


/**
 * Service publisher used to publish services that are exported by <code>RmiServicePublisher</code>
 * Actually, this class was create for convenience of defining RMI related things in Spring XML
 * mapping. It Simply specifies correct values for requried exporter class and protocol name.
 * Therefore, the  <code>GenericServicePublisher</code> class could be used instead of this
 * class (of course, with property configuration in Spring config).
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <EC> type of service exporter which is supported by publisher
 * @see GenericServicePublisher
 */
public class RmiServicePublisher<EC extends AbstractRmiServiceExporter>
        extends GenericServicePublisher<EC> {
    protected static final String PROTOCOL_NAME = "RmiInvoker";

    public RmiServicePublisher()
            throws Exception {
        super();
        fBeanName = PROTOCOL_NAME;
        fProtocolName = PROTOCOL_NAME;
        fAutodiscoveredExporterClass = (Class<EC>) AbstractRmiServiceExporter.class;
    }
}
