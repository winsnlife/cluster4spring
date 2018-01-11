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

import java.lang.reflect.InvocationTargetException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.text.MessageFormat;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.rmi.RmiInvocationHandler;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;

import org.softamis.cluster4spring.AutoDiscoveredServiceExporter;
import org.softamis.cluster4spring.support.AdvicesListProvidingUtils;
import org.softamis.cluster4spring.support.ServerRemoteInvocationTraceInterceptor;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

/**
 * Abstract implementation of Service Exporter to export remote services via RMI
 *
 * @version 1.0
 */

public abstract class AbstractRmiServiceExporter extends
        RemoteInvocationBasedExporter implements InitializingBean,
        DisposableBean, AutoDiscoveredServiceExporter, BeanFactoryAware {
    private static final Log fLog = LogFactory
            .getLog(AbstractRmiServiceExporter.class);

    protected String fServiceName = null;
    protected RMIClientSocketFactory fClientSocketFactory = null;
    protected RMIServerSocketFactory fServerSocketFactory = null;
    protected boolean fAllowsAutoDiscovering = true;
    protected String fServiceGroup = null;
    protected BeanFactory fBeanFactory = null;
    protected String[] fInterceptorNames = null;
    protected RMIClientSocketFactory fRegistryClientSocketFactory = null;
    protected MethodInterceptor fRemoteInvocationTraceInterceptor = null;
    protected RMIServerSocketFactory fRegistryServerSocketFactory = null;
    protected int fServicePort = 0; // anonymous port
    protected int fRegistryPort = Registry.REGISTRY_PORT;
    protected Registry fRegistry = null;
    protected String fRegistryHost = null;
    protected Remote fExportedObject = null;

    protected boolean registerTraceInterceptor = false;

    public void setBeanFactory(BeanFactory aBeanFactory) throws BeansException {
        fBeanFactory = aBeanFactory;
    }

    public void setRegisterTraceInterceptor(boolean val) {

        this.registerTraceInterceptor = val;
    }

    /**
     * Get a proxy for the given service object, implementing the specified
     * service interface.
     * <p>
     * Used to export a proxy that does not expose any internals but just a
     * specific interface intended for remote access. Furthermore, a
     * RemoteInvocationTraceInterceptor gets registered (by default).
     *
     * @return the proxy
     * @see #setServiceInterface
     * @see #setRegisterTraceInterceptor
     * @see org.springframework.remoting.support.RemoteInvocationTraceInterceptor
     */
    @Override
    protected Object getProxyForService() {
        checkService();
        checkServiceInterface();
        ProxyFactory proxyFactory = new ProxyFactory();
        Class serviceInterface = getServiceInterface();
        proxyFactory.addInterface(serviceInterface);

        List<Advice> advices = AdvicesListProvidingUtils.getAdvices(
                fBeanFactory, fInterceptorNames);
        if (advices != null) {
            for (Advice advice : advices) {
                proxyFactory.addAdvice(advice);
            }
        }

        if (isRegisterTraceInterceptor()) {
            MethodInterceptor traceInterceptor = obtainRemoteInvocationTraceInterceptor();
            proxyFactory.addAdvice(traceInterceptor);
        }
        Object service = getService();
        proxyFactory.setTarget(service);
        Object result = proxyFactory.getProxy();
        return result;
    }

    private boolean isRegisterTraceInterceptor() {
        return registerTraceInterceptor;
    }

    protected MethodInterceptor obtainRemoteInvocationTraceInterceptor() {
        MethodInterceptor result = null;
        if (fRemoteInvocationTraceInterceptor == null) {
            result = createDefaultRemoteInvocationTraceInterceptor();
        } else {
            result = fRemoteInvocationTraceInterceptor;
        }
        return result;
    }

    protected MethodInterceptor createDefaultRemoteInvocationTraceInterceptor() {
        String exporterName = getExporterName();
        MethodInterceptor result = new ServerRemoteInvocationTraceInterceptor(
                exporterName, fServiceName);
        return result;
    }

    /**
     * Test the given RMI fRegistry, calling some operation on it to check
     * whether it is still active.
     * <p>
     * Default implementation calls <code>Registry.list()</code>.
     *
     * @param aRegistry
     *            the RMI fRegistry to test
     * @throws java.rmi.RemoteException
     *             if thrown by fRegistry methods
     * @see java.rmi.registry.Registry#list()
     */
    protected void testRegistry(Registry aRegistry) throws RemoteException {
        aRegistry.list();
    }

    protected void unexportServiceObject(Registry aRegistry,
                                         String aServiceName, int aRegistryPort, Remote aExportedObject) {
        if (fLog.isInfoEnabled()) {
            String message = MessageFormat.format(
                    "Unbinding RMI service [{0}] from registry at port [{1}]",
                    aServiceName, aRegistryPort);
            fLog.info(message);
        }
        try {
            aRegistry.unbind(aServiceName);
        } catch (NotBoundException ex) {
            if (fLog.isWarnEnabled()) {
                fLog.warn(
                        MessageFormat
                                .format("RMI service [{0}] is not bound to registry at port [{1}] anymore",
                                        aServiceName, aRegistryPort), ex);
            }
        } catch (Exception ex) {
            if (fLog.isWarnEnabled()) {
                fLog.warn(
                        MessageFormat
                                .format("RMI service [{0}] is not bound to registry at port [{1}] anymore",
                                        aServiceName, aRegistryPort), ex);
            }
        } finally {
            unexportObjectSilently(aExportedObject);
        }
    }

    /**
     * Unexport the registered RMI object, logging any exception that arises.
     *
     * @param aRemote
     *            remote object to unexport
     */
    protected void unexportObjectSilently(Remote aRemote) {
        try {
            UnicastRemoteObject.unexportObject(aRemote, true);
        } catch (NoSuchObjectException ex) {
            if (fLog.isWarnEnabled()) {
                fLog.warn(MessageFormat.format(
                        "RMI object for service [{0}] isn''t exported anymore",
                        fServiceName), ex);
            }
        }
    }

    public void setAllowsAutoDiscovering(boolean aAllowsAutoDiscovering) {
        fAllowsAutoDiscovering = aAllowsAutoDiscovering;
    }

    public String getServiceGroup() {
        return fServiceGroup;
    }

    public void setServiceGroup(String aServiceGroup) {
        fServiceGroup = aServiceGroup;
    }

    /**
     * Set the name of the exported RMI service, i.e.
     * <code>rmi://host:port/NAME</code>
     */
    public void setServiceName(String aServiceName) {
        fServiceName = aServiceName;
    }

    public String getServiceName() {
        return fServiceName;
    }

    /**
     * Set a custom RMI client socket factory to use for exporting the service.
     * <p>
     * If the given object also implements
     * <code>java.rmi.server.RMIServerSocketFactory</code>, it will
     * automatically be registered as server socket factory too.
     *
     * @see #setServerSocketFactory
     * @see java.rmi.server.RMIClientSocketFactory
     * @see java.rmi.server.RMIServerSocketFactory
     * @see java.rmi.server.UnicastRemoteObject#exportObject(java.rmi.Remote, int, java.rmi.server.RMIClientSocketFactory, java.rmi.server.RMIServerSocketFactory)
     */
    public void setClientSocketFactory(
            RMIClientSocketFactory aClientSocketFactory) {
        fClientSocketFactory = aClientSocketFactory;
    }

    /**
     * Set a custom RMI server socket factory to use for exporting the service.
     * <p>
     * Only needs to be specified when the client socket factory does not
     * implement <code>java.rmi.server.RMIServerSocketFactory</code> already.
     *
     * @see #setClientSocketFactory
     * @see java.rmi.server.RMIClientSocketFactory
     * @see java.rmi.server.RMIServerSocketFactory
     * @see java.rmi.server.UnicastRemoteObject#exportObject(java.rmi.Remote, int, java.rmi.server.RMIClientSocketFactory, java.rmi.server.RMIServerSocketFactory)
     */
    public void setServerSocketFactory(
            RMIServerSocketFactory aServerSocketFactory) {
        fServerSocketFactory = aServerSocketFactory;
    }

    public String[] getInterceptorNames() {
        return fInterceptorNames;
    }

    public void setInterceptorNames(String[] aInterceptorNames) {
        fInterceptorNames = aInterceptorNames;
    }

    public void setRemoteInvocationTraceInterceptor(
            MethodInterceptor aRemoteInvocationTraceInterceptor) {
        fRemoteInvocationTraceInterceptor = aRemoteInvocationTraceInterceptor;
    }

    /**
     * Set a custom RMI client socket factory to use for the RMI fRegistry.
     * <p>
     * If the given object also implements
     * <code>java.rmi.server.RMIServerSocketFactory</code>, it will
     * automatically be registered as server socket factory too.
     *
     * @see #setRegistryServerSocketFactory
     * @see java.rmi.server.RMIClientSocketFactory
     * @see java.rmi.server.RMIServerSocketFactory
     * @see java.rmi.registry.LocateRegistry#getRegistry(String, int, java.rmi.server.RMIClientSocketFactory)
     */
    public void setRegistryClientSocketFactory(
            RMIClientSocketFactory aRegistryClientSocketFactory) {
        fRegistryClientSocketFactory = aRegistryClientSocketFactory;
    }

    /**
     * Set a custom RMI server socket factory to use for the RMI fRegistry.
     * <p>
     * Only needs to be specified when the client socket factory does not
     * implement <code>java.rmi.server.RMIServerSocketFactory</code> already.
     *
     * @see #setRegistryClientSocketFactory
     * @see java.rmi.server.RMIClientSocketFactory
     * @see java.rmi.server.RMIServerSocketFactory
     * @see java.rmi.registry.LocateRegistry#createRegistry(int, java.rmi.server.RMIClientSocketFactory, java.rmi.server.RMIServerSocketFactory)
     */
    public void setRegistryServerSocketFactory(
            RMIServerSocketFactory aRegistryServerSocketFactory) {
        fRegistryServerSocketFactory = aRegistryServerSocketFactory;
    }

    /**
     * Locate or create the RMI fRegistry for this exporter.
     *
     * @param aRegistryHost
     *            the fRegistry host to use (if this is specified, no implicit
     *            creation of a RMI fRegistry will happen)
     * @param aRegistryPort
     *            the fRegistry port to use
     * @param aClientSocketFactory
     *            the RMI client socket factory for the fRegistry (if any)
     * @param aServerSocketFactory
     *            the RMI server socket factory for the fRegistry (if any)
     * @return the RMI fRegistry
     * @throws java.rmi.RemoteException
     *             if the fRegistry couldn't be located or created
     */
    protected Registry getRegistry(String aRegistryHost, int aRegistryPort,
                                   RMIClientSocketFactory aClientSocketFactory,
                                   RMIServerSocketFactory aServerSocketFactory) throws RemoteException {
        Registry result = null;
        if (aRegistryHost != null) {
            // Host explictly specified: only lookup possible.
            if (fLog.isInfoEnabled()) {
                String message = MessageFormat
                        .format("Looking for RMI fRegistry at port [{0}] of host [{1}]",
                                aRegistryPort, aRegistryHost);
                fLog.info(message);
            }
            result = LocateRegistry.getRegistry(aRegistryHost, aRegistryPort,
                    aClientSocketFactory);
            testRegistry(result);
        } else {
            result = getRegistry(aRegistryPort, aClientSocketFactory,
                    aServerSocketFactory);
        }
        return result;
    }

    /**
     * Locate or create the RMI fRegistry for this exporter.
     *
     * @param aRegistryPort
     *            the fRegistry port to use
     * @param aClientSocketFactory
     *            the RMI client socket factory for the fRegistry (if any)
     * @param aServerSocketFactory
     *            the RMI server socket factory for the fRegistry (if any)
     * @return the RMI fRegistry
     * @throws java.rmi.RemoteException
     *             if the fRegistry couldn't be located or created
     */
    protected Registry getRegistry(int aRegistryPort,
                                   RMIClientSocketFactory aClientSocketFactory,
                                   RMIServerSocketFactory aServerSocketFactory) throws RemoteException {
        Registry result = null;
        if (aClientSocketFactory != null) {
            if (fLog.isInfoEnabled()) {
                String message = MessageFormat
                        .format("Looking for RMI fRegistry at port [{0}], using custom socket factory",
                                aRegistryPort);
                fLog.info(message);
            }
            try {
                // Retrieve existing fRegistry.
                Registry reg = LocateRegistry.getRegistry(null, aRegistryPort,
                        aClientSocketFactory);
                testRegistry(reg);
                result = reg;
            } catch (RemoteException ex) {
                fLog.debug("RMI fRegistry access threw exception", ex);
                fLog.warn("Could not detect RMI fRegistry - creating new one");
                // Assume no fRegistry found -> create new one.
                result = LocateRegistry.createRegistry(aRegistryPort,
                        aClientSocketFactory, aServerSocketFactory);
            }
        } else {
            result = getRegistry(aRegistryPort);
        }
        return result;
    }

    /**
     * Locate or create the RMI fRegistry for this exporter.
     *
     * @param aRegistryPort
     *            the fRegistry port to use
     * @return the RMI fRegistry
     * @throws java.rmi.RemoteException
     *             if the fRegistry couldn't be located or created
     */
    protected Registry getRegistry(int aRegistryPort) throws RemoteException {
        if (fLog.isInfoEnabled()) {
            String message = MessageFormat.format(
                    "Looking for RMI fRegistry at port [{0}]", aRegistryPort);
            fLog.info(message);
        }
        Registry result = null;
        try {
            // Retrieve existing fRegistry.
            Registry reg = LocateRegistry.getRegistry(aRegistryPort);
            testRegistry(reg);
            result = reg;
        } catch (RemoteException ex) {
            fLog.debug("RMI fRegistry access threw exception", ex);
            fLog.warn("Could not detect RMI fRegistry - creating new one");
            // Assume no fRegistry found -> create new one.
            result = LocateRegistry.createRegistry(aRegistryPort);
        }
        return result;
    }

    public void afterPropertiesSet() throws RemoteException {
        prepare();
    }

    protected void prepare() throws RemoteException {
        checkService();

        if (fServiceName == null) {
            throw new IllegalArgumentException("serviceName is required");
        }

        // Check socket factories for exported object.
        if (fClientSocketFactory instanceof RMIServerSocketFactory) {
            fServerSocketFactory = (RMIServerSocketFactory) fClientSocketFactory;
        }
        if ((fClientSocketFactory != null && fServerSocketFactory == null)
                || (fClientSocketFactory == null && fServerSocketFactory != null)) {
            throw new IllegalArgumentException(
                    "Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
        }

        // Check socket factories for RMI fRegistry.
        if (fRegistryClientSocketFactory instanceof RMIServerSocketFactory) {
            fRegistryServerSocketFactory = (RMIServerSocketFactory) fRegistryClientSocketFactory;
        }
        if (fRegistryClientSocketFactory == null
                && fRegistryServerSocketFactory != null) {
            throw new IllegalArgumentException(
                    "RMIServerSocketFactory without RMIClientSocketFactory for registry not supported");
        }
        exportObject();
    }

    protected String createServiceUrl(String aServiceAddress, int aServicePort) {
        StringBuilder tmp = new StringBuilder(80);
        tmp.append(aServiceAddress);
        tmp.append(':');
        tmp.append(aServicePort);
        tmp.append("/");
        tmp.append(fServiceName);
        String result = tmp.toString();
        return result;
    }

    public int getServicePort() {
        return fServicePort;
    }

    /**
     * Set the port that the exported RMI service will use.
     * <p>
     * Default is 0 (anonymous port).
     */
    public void setServicePort(int aServicePort) {
        fServicePort = aServicePort;
    }

    /**
     * Set the port of the fRegistry for the exported RMI service, i.e.
     * <code>rmi://host:PORT/name</code>
     * <p>
     * Default is <code>Registry.REGISTRY_PORT</code> (1099).
     *
     * @see java.rmi.registry.Registry#REGISTRY_PORT
     */
    public void setRegistryPort(int aRegistryPort) {
        fRegistryPort = aRegistryPort;
    }

    /**
     * Returns port for RMI Registry
     *
     * @return registry port
     */
    public int getRegistryPort() {
        return fRegistryPort;
    }

    public boolean isAllowsAutoDiscovering() {
        return fAllowsAutoDiscovering;
    }

    protected void prepareRegistry() throws RemoteException {
        // Determine RMI fRegistry to use.
        if (fRegistry == null) {
            fRegistry = getRegistry(fRegistryHost, fRegistryPort,
                    fRegistryClientSocketFactory, fRegistryServerSocketFactory);
        }
    }

    /**
     * Specify the RMI fRegistry to register the exported service with.
     * Typically used in combination with RmiRegistryFactoryBean.
     * <p>
     * Alternatively, you can specify all fRegistry properties locally. This
     * exporter will then try to locate the specified fRegistry, automatically
     * creating a new local one if appropriate.
     * <p>
     * Default is a local fRegistry at the default port (1099), created on the
     * fly if necessary.
     *
     * @see org.springframework.remoting.rmi.RmiRegistryFactoryBean
     * @see #setRegistryHost
     * @see #setRegistryPort
     * @see #setRegistryClientSocketFactory
     * @see #setRegistryServerSocketFactory
     */
    public void setRegistry(Registry aRegistry) {
        fRegistry = aRegistry;
    }

    /**
     * Set the port of the fRegistry for the exported RMI service, i.e.
     * <code>rmi://HOST:port/name</code>
     * <p>
     * Default is localhost.
     *
     * @param aRegistryHost
     */
    public void setRegistryHost(String aRegistryHost) {
        fRegistryHost = aRegistryHost;
    }

    /**
     * Returns RMI registry used for exporting remote objects
     *
     * @return registry
     */
    public Registry getRegistry() {
        return fRegistry;
    }

    /**
     * Returns host where RMI Registry is located
     *
     * @return host address
     */
    public String getRegistryHost() {
        return fRegistryHost;
    }

    /**
     * Unbind the RMI service from the fRegistry on bean factory shutdown.
     */
    public void destroy() throws RemoteException {
        String serviceName = fServiceName;
        Registry registry = fRegistry;
        int registryPort = fRegistryPort;
        Remote exportedObject = fExportedObject;
        unexportServiceObject(registry, serviceName, registryPort,
                exportedObject);
    }

    protected void exportObject() throws RemoteException {
        prepareRegistry();

        // Initialize and registry exported object.
        fExportedObject = getObjectToExport();

        if (RmiServiceExporter.fLog.isInfoEnabled()) {
            String message = MessageFormat.format(
                    "Binding RMI service [{0}] to registry at port [{1}]",
                    fServiceName, fRegistryPort);
            RmiServiceExporter.fLog.info(message);
        }

        // Export RMI object.
        if (fClientSocketFactory != null) {
            UnicastRemoteObject.exportObject(fExportedObject, fServicePort,
                    fClientSocketFactory, fServerSocketFactory);
        } else {
            UnicastRemoteObject.exportObject(fExportedObject, fServicePort);
        }

        // Bind RMI object to fRegistry.
        try {
            fRegistry.rebind(fServiceName, fExportedObject);
        } catch (RemoteException ex) {
            // Registry binding failed: let's unexport the RMI object as well.
            unexportObjectSilently(fExportedObject);
            throw ex;
        }
    }

    protected Remote getObjectToExport() {
        Remote result = null;
        // determine remote object
        Object service = getService();
        Class serviceInterface = getServiceInterface();
        if (service instanceof Remote
                && ((serviceInterface == null) || Remote.class
                .isAssignableFrom(serviceInterface))) {
            // conventional RMI service
            result = (Remote) service;
        } else {
            // RMI invoker
            if (fLog.isDebugEnabled()) {
                fLog.debug(MessageFormat.format(
                        "RMI service [{0}] is an RMI invoker", service));
            }
            Object proxyForService = getProxyForService();
            result = new RmiInvocationWrapper(proxyForService, this);
        }
        return result;
    }

    class RmiInvocationWrapper implements RmiInvocationHandler {

        private final Object fWrappedObject;

        private final AbstractRmiServiceExporter fExporter;

        protected RmiInvocationWrapper(Object wrappedObject,
                                       AbstractRmiServiceExporter aRmiServiceExporter) {
            fWrappedObject = wrappedObject;
            fExporter = aRmiServiceExporter;
        }

        /**
         * Exposes the exporter's service interface, if any, as target
         * interface.
         *
         * @see org.springframework.remoting.rmi.RmiBasedExporter#getServiceInterface()
         */
        public String getTargetInterfaceName() {
            Class serviceInterface = fExporter.getServiceInterface();
            String result = null;
            if (serviceInterface != null) {
                result = serviceInterface.getName();
            } else {
                result = null;
            }
            return result;

        }

        public Object invoke(RemoteInvocation aInvocation)
                throws RemoteException, NoSuchMethodException,
                IllegalAccessException, InvocationTargetException {
            aInvocation.addAttribute(ATTRIBUTE_OWN_PROXY_SERVICE_NAME,
                    fExporter.getServiceName());
            Object result = fExporter.invoke(aInvocation, fWrappedObject);
            return result;
        }
    }

    public static final String ATTRIBUTE_OWN_PROXY_SERVICE_NAME = "serviceName";
}
