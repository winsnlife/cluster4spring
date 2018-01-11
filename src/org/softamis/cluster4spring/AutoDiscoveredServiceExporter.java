/******************************************************************************
 * Copyright(c) 2005-2007 SoftAMIS (http://www.soft-amis.com)                 *
 * All Rights Reserved.                                                       *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * You may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *   http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package org.softamis.cluster4spring;

import java.util.*;

/**
 * Interface that should be implemented by appropriate remote service exporter to be
 * included into list of services that may be published for autodiscovering (via
 * corresponding <code>ServicePublisher)</code>.
 * <p/>
 * Each service exported by exporter may belong to some "group".
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @see ServicePublisher
 * @see org.softamis.cluster4spring.support.context.ExporterAcceptor
 */

public interface AutoDiscoveredServiceExporter
{
  /**
   * Indicates whether particular exporter is available for publishing in
   * auto-discoverable services. In general, the final solution about including
   * particular exporter into list of services available for autodiscovered is
   * delegated to appropriate implementation of
   * {@link org.softamis.cluster4spring.support.context.ExporterAcceptor},
   * however, this methods (if implementation of <code>Exporter</code> is aware of
   * it) allows to disable automatic service publishing.
   *
   * @return <code>true</code> if exporter could be published for auto-discovering
   * @see org.softamis.cluster4spring.support.context.ExporterAcceptor
   */
  public boolean isAllowsAutoDiscovering();

  /**
   * Services exporter by exporters may belong to serveral groups. In general,
   * the groups of services are intended to support auto-discovering services and
   * are less important if client uses one or more predefined services' URL's.
   * <p/>
   * Groups could be used by <code>ServicePublishers</code> to select which
   * services should be published and also allows to precisly control discovered
   * services visibility on the client side.
   *
   * @return name of group which exported service belong to.
   */
  public String getServiceGroup();

  /**
   * Returns list of URLs that could be used to locate service
   * @return list of service URL's
   */
  public List<String> provideExportedServiceURLs();

  /**
   * Returns name of the service
   * @return service name
   */
  public String getServiceName();

  /**
   * Default services group name (used if group name is not specified explicitely)
   */
  public static final String DEFAULT_SERVICES_GROUP_PREFIX = "/auto/discovered/";
}
