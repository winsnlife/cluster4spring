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

package org.softamis.cluster4spring.rmi.support;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import static java.text.MessageFormat.format;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.softamis.cluster4spring.support.EndpointFactory;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Endpoint factory used to create RMI related endpoints.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> - information about service.
 */
public class RmiEndpointFactory<SI extends ServiceMoniker>
        implements EndpointFactory<RmiEndpoint<SI>, SI> {
    private static final Log fLog = LogFactory.getLog(RmiEndpointFactory.class);

    /**
     * Prefix used for constructing full RMI specific URL. This implementation
     * assumes that if URL for service does not start with standard RMI related
     * protocol (<code>rmi://</code>) it will append it first.
     */
    public static final String RMI_PROTOCOL_PREFIX = "rmi://";

    public RmiEndpointFactory() {
    }


    /**
     * Creates endpoint for cluster4spring protocol supported by particular factory. Method tries to obtain
     * remote object based on information provided and if remote object is located, creates <code>RMIEndpoint</code>.
     *
     * @param aFactory factory used to create remote invocations
     * @param aBeanName name of bean/service
     * @param aServiceInfo information about remote service
     * @return created endpoint
     * @throws org.springframework.remoting.RemoteAccessException
     *          thrown if endpoint could not be created.
     * @see #obtainRemoteStub(String, ServiceMoniker)
     * @see #createRmiEndpoint(org.springframework.remoting.support.RemoteInvocationFactory, String, ServiceMoniker, java.rmi.Remote)
     */
    public RmiEndpoint<SI> createServiceEndpoint(RemoteInvocationFactory aFactory, String aBeanName, SI aServiceInfo)
            throws RemoteAccessException {
        RmiEndpoint<SI> result = null;
        try {
            // first we try to locate RMI stub for remote service using given service info
            //首次尝试去加载rmi stub;lookUp(serviceUrl)
            Remote remote = obtainRemoteStub(aBeanName, aServiceInfo);

            // if remote is located, we create corresponding endpoint for it
            result = createRmiEndpoint(aFactory, aBeanName, aServiceInfo, remote);
        } catch (Exception e) {
            String message = format("Unable to obtain remote for bean [{0}] with URL [{1}]", aBeanName, aServiceInfo);
            if (fLog.isErrorEnabled()) {
                fLog.error(message, e);
            }
            throw new RemoteAccessException(message, e);
        }
        return result;
    }

    /**
     * Creates RMIEndpoint using given parameters
     *
     * @param aRemoteInvocationFactory factory used to create remote invocation
     * @param aEndpointKey endpoint key
     * @param aServiceInfo information about service
     * @param aRemote remote object used by RMIEndpoint
     * @return Create RMIEndpoint
     */
    protected RmiEndpoint<SI> createRmiEndpoint(RemoteInvocationFactory aRemoteInvocationFactory,
                                                String aEndpointKey,
                                                SI aServiceInfo,
                                                Remote aRemote) {
        RmiEndpoint<SI> result = new RmiEndpoint<SI>(aRemoteInvocationFactory, aEndpointKey, aServiceInfo, aRemote);
        return result;
    }

    /**
     * Obtains remote stub based on given information about service. First method tries to determine
     * url of remote object and based on them tries to perform usual lookup of RMI remote object.
     *
     * @param aBeanName name of proxy bean
     * @param aServiceInfo information about service (remote object)
     * @return remote object
     * @throws NotBoundException
     * @throws MalformedURLException
     * @throws RemoteException
     * @see #getServiceUrl(ServiceMoniker)
     */
    protected Remote obtainRemoteStub(String aBeanName, SI aServiceInfo)
            throws NotBoundException, MalformedURLException, RemoteException {
        // first we need to restore service url which may be either in short (without prefix)
        // or full form
        String serviceURL = getServiceUrl(aServiceInfo);

        // using full URL of RMI service, we try to locate it to obtain RMI stub
        Remote result = Naming.lookup(serviceURL);
        if (fLog.isDebugEnabled()) {
            String message = format("Located RMI stub for bean [{0}] with URL [{1}]", aBeanName, serviceURL);
            fLog.debug(message);
        }
        return result;
    }

    /**
     * Returns URL used to lookup remote object.
     * Current implementation assumes that given service info represents either string with traditional
     * RMI url in form "rmi://server_name:port/remote_object_name" or short form of url (one which starts
     * without <code>rmi://</code> protocol prefix
     *
     * @param aServiceInfo service info
     * @return url of remote object
     */
    public String getServiceUrl(SI aServiceInfo) {
        String serviceUrl = aServiceInfo.getServiceURL();
        String result = null;
        if (serviceUrl.startsWith(RMI_PROTOCOL_PREFIX)) // this is full form of RMI url - for compatibility with ordinary RMI
        {
            result = serviceUrl;
        } else // this is short form of RMI service url which does not include "rmi://" prefix
        {
            StringBuilder tmp = new StringBuilder(60);
            tmp.append(RMI_PROTOCOL_PREFIX);
            tmp.append(serviceUrl);
            result = tmp.toString();
        }
        return result;
    }
}
