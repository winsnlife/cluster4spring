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

/**
 * Interface that should be implemented by appropriate remote service publisher.
 * Service publisher is used to simplify working with "autodiscoverable" services.
 * Typically, <code>ServicePublisher</code> collectes information about
 * all <code>AutoDiscoverdServiceExporter</code>s that are registered in
 * Spring context, gathers information about services exported by exporters
 * and publishes information about them using appropriate services publishing
 * mechanism.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @see ServicePublisher
 * @see org.softamis.cluster4spring.support.context.ExporterAcceptor
 */

public interface ServicePublisher
{
  /**
   * Name of services group which could be published via particular publisher if
   * appropriate <code>Exporter</code> acceptor is used.
   *
   * @return group name
   * @see org.softamis.cluster4spring.support.context.GroupBasedExporterAcceptor
   */
  public String getServiceGroup();

  /**
   * Collects and publishes all applicable services for auto-discovering
   */
  public void publishServices();

  /**
   * Removes information about own published services from auto-discovering context.
   */
  public void unPublishServices();

  public static final String SERVER_TYPE_DELIMITER = "$";
}
