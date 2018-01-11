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

package org.softamis.cluster4spring.support;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Class contains information about location of single service. Such information
 * includes - type of server where service resides, server ID and url used to access
 * service.
 * <code>EndpointFactory</code> will create <code>Endpoint</code> using information stored in <code>ServiceMoniker</code>
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @see org.softamis.cluster4spring.support.Endpoint
 * @see org.softamis.cluster4spring.support.EndpointFactory
 * @see org.softamis.cluster4spring.support.context.ExporterAcceptor
 */

public class ServiceMoniker implements Externalizable {
    private static final long serialVersionUID = 1;

    /**
     * Type of server.
     * @see org.softamis.cluster4spring.support.context.AbstractServicePublisher#fServerType
     */
    protected String fServerType = null;

    /**
     * Internal ID of server within cluster
     */
    protected long fServerID = Long.MIN_VALUE;

    /**
     * URL of the service.
     */
    protected String fServiceURL = null;


    public ServiceMoniker() {
    }

    public ServiceMoniker(String aServiceURL) {
        fServiceURL = aServiceURL;
    }


    public ServiceMoniker(long aServerID, String aServiceGroup, String aServiceURL) {
        fServerID = aServerID;
        fServerType = aServiceGroup;
        fServiceURL = aServiceURL;
    }

    public ServiceMoniker(long aServerID, String aServiceURL) {
        fServerID = aServerID;
        fServiceURL = aServiceURL;
    }

    public long getServerID() {
        return fServerID;
    }

    public void setServerID(long aServerID) {
        fServerID = aServerID;
    }

    public String getServiceURL() {
        return fServiceURL;
    }

    public void setServiceURL(String aServiceURL) {
        fServiceURL = aServiceURL;
    }

    public String getServerType() {
        return fServerType;
    }

    public void setServerType(String aServerType) {
        fServerType = aServerType;
    }


    public void writeExternal(ObjectOutput out)
            throws IOException {
        out.writeLong(fServerID);
        out.writeUTF(fServerType);
        out.writeUTF(fServiceURL);
    }

    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException {
        fServerID = in.readLong();
        fServerType = in.readUTF();
        fServiceURL = in.readUTF();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceMoniker that = (ServiceMoniker) o;

        if (fServerID != that.fServerID) {
            return false;
        }
        if (fServerType != null ? !fServerType.equals(that.fServerType) : that.fServerType != null) {
            return false;
        }
        if (!fServiceURL.equals(that.fServiceURL)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (fServerType != null ? fServerType.hashCode() : 0);
        result = 31 * result + (int) (fServerID ^ (fServerID >>> 32));
        result = 31 * result + fServiceURL.hashCode();
        return result;
    }

    @SuppressWarnings({"ChainedMethodCall"})
    @Override
    public String toString() {
        String result = new StringBuilder().append("Service Moniker:[")
                .append(fServerID)
                .append("][")
                .append(fServerType)
                .append("][")
                .append(fServiceURL)
                .append(']')
                .toString();
        return result;
    }
}
