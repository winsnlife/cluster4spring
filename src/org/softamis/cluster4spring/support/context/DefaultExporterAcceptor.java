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

import org.softamis.cluster4spring.AutoDiscoveredServiceExporter;
import org.softamis.cluster4spring.ServicePublisher;

/**
 * Simple policy that accepts all exporters which has <code>allowsAutoDiscovering</code> property
 * set to true.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <AE> type of service exporter
 * @param <SP> type of service publisher
 * @see AutoDiscoveredServiceExporter
 * @see ServicePublisher
 * @see org.softamis.cluster4spring.AutoDiscoveredServiceExporter#isAllowsAutoDiscovering()
 */

public class DefaultExporterAcceptor<AE extends AutoDiscoveredServiceExporter, SP extends ServicePublisher>
        implements ExporterAcceptor<AE, SP> {
    public DefaultExporterAcceptor() {
    }

    /**
     * Determines whether service exporter by given publisher should be published by
     * given publisher. Current implementation accepts any exporter that has
     * <code>allowsAutoDiscovering</code> property set to true.
     *
     * @param aExporter exporter
     * @param aPublisher publisher that publishes services
     * @return <code>true</code> if publisher should publish service exported by exporter
     */
    public boolean canAcceptExporterForPublishing(AE aExporter, SP aPublisher) {
        // we accept all exporter which allows autodiscovering
        boolean result = aExporter.isAllowsAutoDiscovering();
        return result;
    }
}
