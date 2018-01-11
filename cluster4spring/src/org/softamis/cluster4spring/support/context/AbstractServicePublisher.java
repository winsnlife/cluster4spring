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

import static java.text.MessageFormat.format;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

import org.softamis.cluster4spring.AutoDiscoveredServiceExporter;
import org.softamis.cluster4spring.ServicePublisher;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * <p>Abstract base implementation of service publisher. During refreshing application context,
 * publisher collects all service exporters with needed class and if they are applicable for
 * service publishing, it publishes information about these services in distributed registry.
 * Therefore, clients that requires autodiscoverable services may obtain information about
 * service urls and so use them.</p>
 * <p>To determine whether particular service exporter should be published by this publisher,
 * delegates actual logic to associated <code>ExporterAcceptor</code>.</p>
 * <p>
 * On closing service context, publisher notifies distributed registry for unregistration of
 * all service urls previosly published by publisher.
 * </p>
 * <p>While there is ready to use implementation of this class wich uses distributed registry
 * is included into part of the library, it's possible to use other implementation of distributed
 * services storage. If it's desired to use another storage (say, JBoss Cache), this class should
 * be considered as base one for implementing such integration. </p>
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <EC> type of service exporter which is supported by publisher
 * @see ExporterAcceptor
 */

@SuppressWarnings({"ProhibitedExceptionDeclared"})
public abstract class AbstractServicePublisher<EC extends AutoDiscoveredServiceExporter>
        implements ServicePublisher,
        InitializingBean,
        ApplicationListener,
        ApplicationContextAware,
        BeanNameAware {
    private static final Log fLog = LogFactory.getLog(AbstractServicePublisher.class);

    /**
     * Application context
     */
    protected ApplicationContext fContext = null;

    /**
     * Services group
     */
    protected String fServiceGroup = null;

    /**
     * Indicates whether services discovered for publishing during context refreshing should be
     * cached until context closing
     */
    protected boolean fCacheAutoDiscoveredServicesInfo = false;

    /**
     * Cache of discovered services
     */
    protected List<ServiceInfo> fAutodiscoveredServicesInfo = null;

    /**
     * ExporterAcceptor used to determine whether service exported by particular exporter could be published
     */
    protected ExporterAcceptor<EC, ServicePublisher> fExporterAcceptor = null;

    /**
     * Name of bean - used in logging
     */
    protected String fBeanName = null;

    /**
     * Type of server. If cluster represents group of servers that includes servers with
     * different types, this field allows to define to which kind of servers particular
     * one represents (just as example - "pdf generation server", "web server", "image processor"
     * etc).
     */
    protected String fServerType = "DEF";

    /**
     * If there are several servers of the same type exists withing the same cluster, it is necessary to
     * distinguish them. Server ID represents unques ID of server within a cluster.
     */
    protected long fServerID = Integer.MIN_VALUE;

    /**
     * Class of exporter should be published
     */
    protected Class<EC> fAutodiscoveredExporterClass = null;

    /**
     * Name of protocol
     */
    protected String fProtocolName = null;


    /**
     * Creates publisher
     */
    protected AbstractServicePublisher() {
        super();
    }

    /**
     * Set the aName of the bean in the bean factory that created this bean.
     *
     * @param aName aName of the bean
     */
    public void setBeanName(String aName) {
        fBeanName = aName;
    }

    /**
     * Return name of the bean in bean factory
     *
     * @return bean name
     */
    protected String getPublisherName() {
        return fBeanName;
    }

    /**
     * Handles Spring application event. During refreshing application context,
     * publisher collects all service exporters with needed class and if they are applicable for
     * service publishing, it publishes information about these services in distributed registry.
     * Therefore, clients that requires autodiscoverable services may obtain information about
     * service urls and so use them.
     * <p/>
     * On closing service context, publisher notifies distributed registry for unresistration of
     * all service urls previosly published by publisher.
     *
     * @param aEvent event from Spring
     * @see #contextRefreshed()
     * @see #contextClosed()
     */
    public void onApplicationEvent(ApplicationEvent aEvent) {
        try {
            if (aEvent instanceof ContextRefreshedEvent) {
                contextRefreshed();
            } else if (aEvent instanceof ContextClosedEvent) {
                contextClosed();
            }
        } catch (Exception e) {
            if (fLog.isErrorEnabled()) {
                String publisherName = getPublisherName();
                fLog.error(format("Exception during processing application event. Publisher: [{0}]", publisherName), e);
            }
        }
    }


    /**
     * Collects information about services that could be published. If there is no
     * services information in the cache, method tries to discover it.
     * <p/>
     * First, method selects service exporters with applicable type.
     * Then, for every publisher found it performs check whether it could be published
     * by given publisher (using ExporterAcceptor) and if service could be published,
     * appropriate service information is created based on exporter data.
     *
     * @return list of service items
     * @see #createServiceInfos(String, AutoDiscoveredServiceExporter)
     * @see ExporterAcceptor
     * @see #getAutodiscoveredExporterClass()
     */
    protected List<ServiceInfo> collectAutoDiscoveredServices() {
        List<ServiceInfo> result = null;
        if (fAutodiscoveredServicesInfo == null) // no services are cached for now
        {
            Class exporterClass = getAutodiscoveredExporterClass();
            try {
                // collect services
                Map<String, EC> exporters = doGetAutoDiscoveredServices(exporterClass);

                // here we collect only acceptable service (via ExporterAcceptor) and
                // create ServiceInfo item for every accepted service
                int capacity = exporters.size();
                result = new ArrayList<ServiceInfo>(capacity);
                for (Map.Entry<String, EC> entry : exporters.entrySet()) {
                    EC exporter = entry.getValue();
                    // let's acceptor decide whether we accept exporter
                    if (fExporterAcceptor.canAcceptExporterForPublishing(exporter, this)) {
                        String beanName = entry.getKey();
                        List<ServiceInfo> serviceInfos = createServiceInfos(beanName, exporter);
                        result.addAll(serviceInfos);
                    }
                }
            } catch (BeansException e) {
                result = Collections.emptyList();
                if (fLog.isErrorEnabled()) {
                    String publisherName = getPublisherName();
                    String message =
                            format("Exception occured during obtaining list of exporters. Publisher: [{0}]", publisherName);
                    fLog.error(message, e);
                }
            }
        } else {
            result = fAutodiscoveredServicesInfo;
        }
        return result;
    }

    /**
     * Collects exporters that potentially published from application context
     * Current implementation simply selects all beans from Spring Context with
     * provided class
     *
     * @param aExporterClass class of exporter
     * @return list of exporters
     * @throws BeansException thrown by Spring ApplicationContext during obtainig list of beans
     */
    protected Map<String, EC> doGetAutoDiscoveredServices(Class aExporterClass)
            throws BeansException {
        Map<String, EC> result = fContext.getBeansOfType(aExporterClass, false, true);
        return result;
    }


    /**
     * Handles closing application context. Performs unpublishing of services.
     *
     * @throws Exception throw if exception occured during closing context
     * @see #onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    protected void contextClosed()
            throws Exception {
        unPublishServices();
    }

    /**
     * Handles refreshing of application context. Performs publishing of services.
     *
     * @throws Exception
     * @see #onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    protected void contextRefreshed()
            throws Exception {
        publishServices();
    }

    /**
     * Creates key for published service. Using this key, information about server will be stored in
     * distributed registry and later will be obtained by <code>DiscoveringServiceProvider</code>.
     * Current implementation created key in form <pre>SERVICE_GROUP/SERVICE_NAME</pre>.
     *
     * @param aServiceInfo information about exported service.
     * @return service key
     */
    protected String getServiceKey(ServiceInfo aServiceInfo) {
        String serviceGroup = aServiceInfo.getServiceGroup();
        String serviceName = aServiceInfo.getServiceName();

        StringBuilder tmp = new StringBuilder(100);
        tmp.append(serviceGroup);
        tmp.append("/");
        tmp.append(serviceName);

        String result = tmp.toString();
        return result;
    }


    /**
     * Removes information about own published services from auto-discovering context. Simply
     * walks on all collected services and notifies distributed registry that service url should
     * be unregistered.
     *
     * @see #collectAutoDiscoveredServices()
     * @see #getServiceKey(ServiceInfo)
     */
    public void unPublishServices() {
        // collects services or obtains them from the cache
        List<ServiceInfo> autodiscoveredServices = collectAutoDiscoveredServices();

        if (fLog.isInfoEnabled()) {
            String publisherName = getPublisherName();
            int servicesCount = autodiscoveredServices.size();
            String message = format("Starting closing autodiscovered services. Publisher: [{0}]. Services count: [{1}]",
                    publisherName, servicesCount);
            fLog.info(message);
        }

        // unpublished services
        for (ServiceInfo serviceInfo : autodiscoveredServices) {
            unPublishService(serviceInfo);
            // tmp - potentially we could make some delay between unregistering
        }

        if (fLog.isInfoEnabled()) {
            String publisherName = getPublisherName();
            String message = format("Finished removing context services. Publisher: [{0}]", publisherName);
            fLog.info(message);
        }
    }

    /**
     * Collects and publishes all applicable services for auto-discovering. Simply
     * walks on all collected services and notifies distributed registry that service url should
     * be registered.
     * <p/>
     * <b>NOTE:</b> If service group is specified for found exporter, it will be used as for
     * creation of ServiceInfo. If service group is not specified for exporter, during
     * creation of ServiceInfo group from publisher will be used.
     *
     * @see #collectAutoDiscoveredServices()
     * @see #getServiceKey(ServiceInfo)
     */
    public void publishServices() {
        // fist we collect list of services to publish
        List<ServiceInfo> autodiscoveredServices = collectAutoDiscoveredServices();
        if (fLog.isInfoEnabled()) {
            String publisherName = getPublisherName();
            int servicesCount = autodiscoveredServices.size();
            String message = format("Starting publishing autodiscovered services. Publisher: [{0}]. Services count: [{1}]",
                    publisherName, servicesCount);
            fLog.info(message);
        }

        // and now we publish each found service individually
        for (ServiceInfo serviceInfo : autodiscoveredServices) {
            publishService(serviceInfo);
        }

        if (fCacheAutoDiscoveredServicesInfo) {
            fAutodiscoveredServicesInfo = autodiscoveredServices;
        }
        if (fLog.isInfoEnabled()) {
            String message = format("Finished publishing autodiscovered services.Publisher: [{0}]", getPublisherName());
            fLog.info(message);
        }
    }

    /**
     * Performs un-publishing of service denoted by given parameter by notifying underlying services registry.
     *
     * @param serviceInfo key used to denote service which should be unpublished
     */
    protected void unPublishService(ServiceInfo serviceInfo) {
        String serviceName = serviceInfo.getServiceName();
        String serviceUrl = serviceInfo.getServiceURL();
        String serviceGroup = serviceInfo.getServiceGroup();
        if (serviceGroup == null) {
            serviceInfo.setServiceGroup(fServiceGroup);
            serviceGroup = fServiceGroup;
        }
        try {
            String serviceKey = getServiceKey(serviceInfo);
            doUnregisterServiceInServicesRegistry(serviceKey);

            if (fLog.isInfoEnabled()) {
                String publisherName = getPublisherName();
                String message = format(
                        "Autodiscovered service removed.  Publisher: [{0}]. Service Group: [{1}] Service Name: [{2}] Service URL: [{3}]",
                        publisherName, serviceGroup, serviceName, serviceUrl);
                fLog.info(message);
            }
        } catch (Exception ex) {
            String publisherName = getPublisherName();
            String message = format(
                    "Exception while removing autodiscovered service.  Publisher: [{0}]. Service Group: [{1}] Service Name: [{2}] Service URL: [{3}]",
                    publisherName, serviceGroup, serviceName, serviceUrl);
            fLog.error(message, ex);
        }
    }


    /**
     * Performs publishing of service denoted by given parameter by notifying underlying services registry.
     *
     * @param aServiceInfo key used to denote service which should be published
     */
    protected void publishService(ServiceInfo aServiceInfo) {
        String serviceName = aServiceInfo.getServiceName();
        String serviceUrl = aServiceInfo.getServiceURL();
        String serviceGroup = aServiceInfo.getServiceGroup();
        if (serviceGroup == null) {
            aServiceInfo.setServiceGroup(fServiceGroup);
            serviceGroup = fServiceGroup;
        }
        try {
            // based on ServiceInfo, we create service key which will be used by
            // fServicesRegistry to store information about published service entry
            String serviceKey = getServiceKey(aServiceInfo);

            // here we create moniker for particular service which contains necessary information
            // about service and which is stored in registry under serviceKey key
            ServiceMoniker moniker = createServiceMoniker(aServiceInfo);

            // and here we simply register key/moniker pair in registry
            doRegisterServiceInServicesRegistry(serviceKey, moniker);
            if (fLog.isInfoEnabled()) {
                String publisherName = getPublisherName();
                String message = format(
                        "Autodiscovered service added. Publisher: [{0}]. Service Group: [{1}] Service Name: [{2}] Service URL: [{3}]",
                        publisherName, serviceGroup, serviceName, serviceUrl);
                fLog.info(message);
            }
        } catch (Exception ex) {
            String publisherName = getPublisherName();
            String message = format(
                    "Exception while adding autodiscovered service. Publisher: [{0}] .Service Group: [{1}] Service Name: [{2}] Service URL: [{3}]",
                    publisherName, serviceGroup, serviceName, serviceUrl);
            fLog.error(message, ex);
        }
    }


    /**
     * Creates <code>ServiceMoniker</code> by given discovered <code>ServiceInfo</code>
     *
     * @param aServiceInfo collected information about service
     * @return created <code>ServiceMoniker</code>
     */
    protected ServiceMoniker createServiceMoniker(ServiceInfo aServiceInfo) {
        String serviceUrl = aServiceInfo.getServiceURL();
        ServiceMoniker result = new ServiceMoniker(fServerID, fServerType, serviceUrl);
        return result;
    }

    /**
     * Called by Spring as part of bean lifecycle. Performs checking that service group is specified
     * externally (or creates default group name). Also checks whether ExporterAcceptor is specified and
     * if necessary creates default one.
     *
     * @throws Exception
     * @see #createServicesGroup()
     * @see #createDefaultExporterAcceptor()
     */
    public void afterPropertiesSet()
            throws Exception {
        // here we specify default values if ones were not set explicitely
        if (fServiceGroup == null) {
            fServiceGroup = createServicesGroup();
        }
        if (fExporterAcceptor == null) {
            fExporterAcceptor = createDefaultExporterAcceptor();
        }

        if (fServerID == Long.MIN_VALUE) {
            if (fLog.isWarnEnabled()) {
                fLog.warn("Server ID is not explicitely set - this potentially could affect selection of endpoints on client.");
            }
        }
    }

    /**
     * Creates ExporterAcceptor that will be used if ones is not specified explicitely.
     *
     * @return created ExporterAcceptor
     * @see DefaultExporterAcceptor
     */
    protected ExporterAcceptor<EC, ServicePublisher> createDefaultExporterAcceptor() {
        ExporterAcceptor<EC, ServicePublisher> result = new DefaultExporterAcceptor<EC, ServicePublisher>();
        return result;
    }

    /**
     * Creates default name for services group if one is not specified explicitely. By default,
     * format of service group is
     * <code>
     * AutoDiscoveredServiceExporter.DEFAULT_SERVICES_GROUP_PREFIX + getProtocolName()
     * </code>
     *
     * @return service group
     */
    protected String createServicesGroup() {
        String result = AutoDiscoveredServiceExporter.DEFAULT_SERVICES_GROUP_PREFIX + getProtocolName();
        return result;
    }


    public void setApplicationContext(ApplicationContext aContext)
            throws BeansException {
        fContext = aContext;
    }

    /**
     * Returns services group
     *
     * @return services group
     */
    public String getServiceGroup() {
        return fServiceGroup;
    }

    /**
     * Sets services group. If services group is not specified, default services group will be created.
     *
     * @param aServiceGroup services group
     */
    public void setServiceGroup(String aServiceGroup) {
        fServiceGroup = aServiceGroup;
    }

    /**
     * Specifies whether information about published services should be  cached until context closing
     *
     * @return <code>true</code> if caching is required
     */
    public boolean isCacheAutoDiscoveredServicesInfo() {
        return fCacheAutoDiscoveredServicesInfo;
    }

    /**
     * Specifies whether information about published services should be  cached until context closing.
     * Default value: <code>false</code>
     *
     * @param aCache true if autodiscovered services should be cached until unpublishing
     */
    public void setCacheAutoDiscoveredServicesInfo(boolean aCache) {
        fCacheAutoDiscoveredServicesInfo = aCache;
    }


    /**
     * Sets exporterAcceptor used to determine whether service exported by particular exporter could be published
     *
     * @param aExporterAcceptor exporterAcceptor
     */
    public void setExporterAcceptor(ExporterAcceptor<EC, ServicePublisher> aExporterAcceptor) {
        fExporterAcceptor = aExporterAcceptor;
    }

    /**
     * Returns type of server. If cluster contains servers of different types, it's necessary to define
     * which type of server particular service belongs to. In general, all services within the same server
     * will have the same server type.
     *
     * @return type of server where service recide
     */
    public String getServerType() {
        return fServerType;
    }

    /**
     * Setter for server types.  If cluster contains servers of different types, it's necessary to define
     * which type of server particular service belongs to. In general, all services within the same server
     * will have the same server type.
     *
     * @param aServerType type of server
     */
    public void setServerType(String aServerType) {
        fServerType = aServerType;
    }

    /**
     * If there are several servers of the same type exists withing the same cluster, it is necessary to
     * distinguish them. Server ID represents unques ID of server within a cluster. Typically all services
     * published from the same server will have the same server ID.
     *
     * @return server ID
     */
    public long getServerID() {
        return fServerID;
    }

    /**
     * Setter for ServerID property
     *
     * @param aServerID
     */
    public void setServerID(long aServerID) {
        fServerID = aServerID;
    }

    /**
     * Creates information about service based on given service exporter. This information will
     * later be used to publish service url.
     *
     * @param aBeanName name of bean
     * @param aExporter exporter
     * @return service information
     */
    protected List<ServiceInfo> createServiceInfos(String aBeanName, EC aExporter) {
        String serviceGroup = aExporter.getServiceGroup();

        String serviceName = aExporter.getServiceName();

        List<String> exporterServiceURLs = aExporter.provideExportedServiceURLs();
        List<ServiceInfo> result = new ArrayList<ServiceInfo>(exporterServiceURLs.size());
        for (String exportedServiceURL : exporterServiceURLs) {
            String serviceURL = prepareServiceURL(exportedServiceURL);
            ServiceInfo serviceInfo = new ServiceInfo(serviceGroup, serviceName, serviceURL);
            if (serviceInfo != null) {
                result.add(serviceInfo);
            }
        }
        return result;
    }

    /**
     * Prepared URL for service. Potentially may be used in inherited classess
     * for additional processing of service URL.
     *
     * @param aServiceUrl url of service
     * @return resulting URL that will be stored in <code>ServiceMoniker</code>
     */
    protected String prepareServiceURL(String aServiceUrl) {
        return aServiceUrl;
    }

    /**
     * Returns name of protocol used exporters. Used for creation of default name of servicesGroup and
     * for logging.
     *
     * @return name of protocol
     */
    protected String getProtocolName() {
        return fProtocolName;
    }

    /**
     * Return class of servicie exporter that is used to select information about services which
     * should be published by this publisher
     *
     * @return class of exporter
     */
    protected Class<EC> getAutodiscoveredExporterClass() {
        return fAutodiscoveredExporterClass;
    }

    /**
     * Notifies that service under given key should be unregistered in underlying
     * services registry
     * @param aServiceKey  key for service that should be unregistered
     */
    protected abstract void doUnregisterServiceInServicesRegistry(String aServiceKey);

    /**
     * Notifies underlying services registry that information about service should be
     * published
     * @param aServiceKey key for service
     * @param aMoniker service information
     */
    protected abstract void doRegisterServiceInServicesRegistry(String aServiceKey, ServiceMoniker aMoniker);

}
