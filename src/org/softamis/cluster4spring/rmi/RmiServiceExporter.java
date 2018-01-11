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

import java.net.UnknownHostException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.softamis.net.localinfo.LocalNetworkInfoUtils;

/**
 * RMI exporter that exposes the specified service as RMI object with the specified
 * name. Such services can be accessed via plain RMI or via RmiProxyFactoryBean.
 * Also supports exposing any non-RMI service via RMI invokers, to be accessed via
 * RmiClientInterceptor/RmiProxyFactoryBean's automatic detection of such invokers.
 * <p/>
 * <p>With an RMI invoker, RMI communication works on the RmiInvocationHandler
 * level, needing only one stub for any service. Service interfaces do not have to
 * extend java.rmi.Remote or throw RemoteException on all methods, but in and out
 * parameters have to be serializable.
 *
 * @author Andrew Sazonov
 */
public class RmiServiceExporter extends AbstractRmiServiceExporter {
    protected static final Log fLog = LogFactory.getLog(RmiServiceExporter.class);

    public RmiServiceExporter() {
        super();
    }

    public List<String> provideExportedServiceURLs() {
        List<String> result = new ArrayList<String>(1);
        int servicePort = fServicePort;
        if (servicePort == 0) {
            servicePort = fRegistryPort;
        }
        String serviceAddress = fRegistryHost;
        if (fRegistryHost == null) {
            try {
                serviceAddress = LocalNetworkInfoUtils.getLocalHostAddress();
            } catch (UnknownHostException e) {
                String message = "Unable to determine local host address";
                if (fLog.isErrorEnabled()) {
                    fLog.error(message, e);
                }
                throw new IllegalStateException(message);
            }
        }
        String url = createServiceUrl(serviceAddress, servicePort);
        result.add(url);
        return result;
    }
}
