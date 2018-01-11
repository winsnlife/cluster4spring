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

import java.net.*;
import java.net.UnknownHostException;
import java.rmi.*;
import java.util.*;

import org.apache.commons.logging.*;

import org.softamis.net.localinfo.*;
import org.softamis.net.localinfo.acceptor.*;

/**
 * Experimental version of RMI service Exporter used to support multihome systems
 * @version 1.0
 */

public class MultihomeRmiServiceExporter
        extends AbstractRmiServiceExporter {
    private static final Log fLog = LogFactory.getLog(MultihomeRmiServiceExporter.class);

    /**
     * Local address of machine
     */
    protected List<String> fLocalAddresses = null;

    protected LocalNetworkInfoProvider fLocalNetworkInfoProvider = null;
    protected InetAddressAcceptor fInetAddressAcceptor = null;


//  public static final String ADDRESSES_DELIMITER = "; ,";

    public MultihomeRmiServiceExporter() {
        super();
    }


    @Override
    public void afterPropertiesSet()
            throws RemoteException {
        super.afterPropertiesSet();
        prepareLocalAddresses();
    }

    protected InetAddressAcceptor obtainInetAddressAcceptor() {
        InetAddressAcceptor result = getInetAddressAcceptor();
        if (result == null) {
            result = Ip4AddressesAcceptor.INSTANCE_NO_LOCAL;
        }
        return result;
    }

    protected void prepareLocalAddresses() {
        if (fLocalAddresses == null || fLocalAddresses.isEmpty()) {
            try {
                InetAddressAcceptor inetAddressAcceptor = obtainInetAddressAcceptor();
                fLocalAddresses = fLocalNetworkInfoProvider.getLocalHostAddresses(inetAddressAcceptor);
            } catch (SocketException e) {
                if (fLog.isErrorEnabled()) {
                    fLog.error("Unable to determine list of local host addresses for multihome machine", e);
                }
                try {
                    String localAddress = LocalNetworkInfoUtils.getLocalHostAddress();
                    fLocalAddresses = new ArrayList<String>(1);
                    fLocalAddresses.add(localAddress);
                } catch (UnknownHostException e1) {
                    if (fLog.isErrorEnabled()) {
                        fLog.error("Unable to determine own local address", e1);
                    }
                }
            }
        }
    }


    public List<String> provideExportedServiceURLs() {
        List<String> result = new ArrayList<String>(fLocalAddresses.size());
        int servicePort = fServicePort;
        if (servicePort == 0) {
            servicePort = fRegistryPort;
        }
        String serviceAddress = fRegistryHost;
        if (fRegistryHost == null) {
            for (String localAddress : fLocalAddresses) {
                String url = createServiceUrl(localAddress, servicePort);
                result.add(url);
            }
        } else {
            String url = createServiceUrl(serviceAddress, servicePort);
            result.add(url);
        }
        return result;
    }

    // TMP: 1.0.1 Change this and make implementation with proper type of provider - one that returns list of string

//  /**
//   * Sets local address of machine
//   *
//   * @param aLocalAddresses local address of machine
//   */
//  public void setLocalAddressesList(List<String> aLocalAddresses)
//  {
//    fLocalAddresses = aLocalAddresses;
//  }
//
//  /**
//   * Returns local address of machine
//   *
//   * @return local address of machine
//   */
//  public List<String> getLocalAddressesList()
//  {
//    return fLocalAddresses;
//  }
//
//  public void setLocalAddresses(String aAddresses)
//  {
//    if (aAddresses == null)
//    {
//      throw new IllegalArgumentException("String of local addresses may not be null");
//    }
//    fLocalAddresses = new ArrayList<String>();
//    StringTokenizer tokens = new StringTokenizer(aAddresses, ADDRESSES_DELIMITER, false);
//    while (tokens.hasMoreTokens())
//    {
//      String rawAddress = tokens.nextToken();
//      String address = rawAddress.trim();
//      if (!StringUtils.isBlankOrEmpty(address))
//      {
//        fLocalAddresses.add(address);
//      }
//    }
//  }

    public LocalNetworkInfoProvider getLocalNetworkInfoProvider() {
        return fLocalNetworkInfoProvider;
    }

    public void setLocalNetworkInfoProvider(LocalNetworkInfoProvider aLocalNetworkInfoProvider) {
        fLocalNetworkInfoProvider = aLocalNetworkInfoProvider;
    }

    public InetAddressAcceptor getInetAddressAcceptor() {
        return fInetAddressAcceptor;
    }

    public void setInetAddressAcceptor(InetAddressAcceptor aInetAddressAcceptor) {
        fInetAddressAcceptor = aInetAddressAcceptor;
    }
}
