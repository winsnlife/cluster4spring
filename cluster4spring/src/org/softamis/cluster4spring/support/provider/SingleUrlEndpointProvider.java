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

package org.softamis.cluster4spring.support.provider;

import static java.text.MessageFormat.format;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.EndpointFactory;
import org.softamis.cluster4spring.support.ServiceMoniker;


/**
 * Implementation of <code>EndpointProvider</code> that utilises single and
 * explicitely specified "url" of the service to create service endpoint. Basically,
 * any object may  represent service "url" - it is responsibility of concrete implementation
 * of <code>EndpointFactory</code> to interpret given object and create appropriate
 * endpoint for remote service that could be located based on given "url".
 * <p/>
 * In general, this is similar to traditional Spring cluster4spring.
 *
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E>  type of endpoints that could be created by this factory
 * @author Andrew Sazonov
 * @version 1.0
 */
@SuppressWarnings({"ProhibitedExceptionDeclared"})
public class SingleUrlEndpointProvider<E extends Endpoint<SI>, SI extends ServiceMoniker>
        extends BaseEndpointProvider<E, SI>
        implements InitializingBean {
    protected static final Log fLog = LogFactory.getLog(SingleUrlEndpointProvider.class);

    /**
     * Cached endpoint, if any.
     */
    protected E fEndpoint = null;

    /**
     * Information needed to discover remote service
     */
    protected SI fServiceMoniker = null;

    public SingleUrlEndpointProvider() {
        super();
    }

    /**
     * Called by Spring as part of bean lifecycle. Checks whether <code>serviceUrl</code>
     * property is specified.
     *
     * @throws Exception
     * @see #setServiceMoniker(ServiceMoniker)
     * @see #setServiceUrl(String)
     */
    public void afterPropertiesSet()
            throws Exception {
        if (fServiceMoniker == null) {
            String message = "Service URL should be set";
            if (fLog.isErrorEnabled()) {
                fLog.error(message);
            }
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Returns endpoint that will be used for remote service invocation.
     * If configured to cache discovered endpoints, simply returns cached endpoint.
     * <p/>
     * Otherwise, tries to create endpoint using provided <code>EndpointFactory</code.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocations
     * @param aEndpointFactory         factory used to create endpoints
     * @param aBeanName                name of bean that is used as proxy for remote service
     * @return endpoint that should be used to remote invocation
     * @throws org.springframework.remoting.RemoteAccessException thrown if endpoint could not be selected of if
     *                                                            there are not currently available endpoints for remote service
     * @see #setCacheEndpoints(boolean)
     */
    public E getEndpoint(RemoteInvocationFactory aRemoteInvocationFactory,
                         EndpointFactory<E, SI> aEndpointFactory,
                         String aBeanName)
            throws RemoteAccessException {
        E result = null;
        if (fCacheEndpoints) // perform cached policy
        {
            result = fEndpoint;
            if (result == null) // ok, no endpoint cached - try to create it
            {
                String serviceURL = fServiceMoniker.getServiceURL();
                String message = format(
                        "Unable locate cached endpoint - probably service is invalid. Bean Name: [{0}] Service URL: {1}",
                        aBeanName, serviceURL);
                if (fLog.isErrorEnabled()) {
                    fLog.error(message);
                }
                throw new RemoteAccessException(message);
            }
        } else // always create endpoint for the service
        {
            result = doCreateServiceEndpoint(aRemoteInvocationFactory, aEndpointFactory, aBeanName, fServiceMoniker);
        }
        return result;
    }

    /**
     * Marks given endpoint invalid. This endpoint will not be later used for methods invocation.
     * This implementation simply assigns <code>null</code> to cached endpoint.
     *
     * @param aBeanName name of bean that is used as proxy for remote service
     * @param aEndpoint endpoint to be marked invalid
     */
    public void markInvalid(String aBeanName, E aEndpoint) {
        if (fLog.isTraceEnabled()) {
            SI serviceInfo = fEndpoint.getServiceInfo();
            String serviceURL = serviceInfo.getServiceURL();
            String message =
                    format("Service is marked as invalid. Bean Name: [{0}] Service URL: [{1}]", aBeanName, serviceURL);
            fLog.trace(message);
        }

        // only one endpoint is supported by this provider, so we simply assign null to cached endpoint
        fEndpoint = null;
    }

    /**
     * Refreshes internal state of provider. If convifugred to cache endpoints,
     * tries to create endpoint using provided <code>EndpointFactory</code> and
     * stored created endpoint for later use.
     * <p/>
     * Otherwise, silently does nothing since endpoint will be obtained by
     * request.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocation
     * @param aEndpointFactory         factory used to create endpoints
     * @param aBeanName                name of bean that is used as proxy for remote service
     * @throws org.springframework.remoting.RemoteAccessException throw if exception occured during refreshing
     * @see #setCacheEndpoints(boolean)
     */
    public void refresh(RemoteInvocationFactory aRemoteInvocationFactory,
                        EndpointFactory<E, SI> aEndpointFactory,
                        String aBeanName)
            throws RemoteAccessException {
        if (fLog.isTraceEnabled()) {
            String message = format("Refreshing endpoints. Bean Name: [{0}]", aBeanName);
            fLog.trace(message);
        }

        if (fCacheEndpoints)  // if we are in cache mode, we need to re-create endpoint
        {
            fEndpoint = doCreateServiceEndpoint(aRemoteInvocationFactory, aEndpointFactory, aBeanName, fServiceMoniker);
        } else {
            // since endpoint will be re-created during next invocation, we should not do something
        }
    }

    /**
     * Sets information needed to discover remote service
     *
     * @param aServiceMoniker information needed to discover remote service
     */
    public void setServiceMoniker(SI aServiceMoniker) {
        fServiceMoniker = aServiceMoniker;
    }

    public SI getServiceMoniker() {
        return fServiceMoniker;
    }

    /**
     * Returns information needed to discover remote service
     *
     * @return service info
     */
    public String getServiceUrl() {
        String result = fServiceMoniker.getServiceURL();
        return result;
    }

    public void setServiceUrl(String aServiceURL) {
        fServiceMoniker = (SI) new ServiceMoniker(aServiceURL);
    }

}
