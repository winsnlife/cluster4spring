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

import java.io.Serializable;

/**
 * Utility class used by service publisher to collect information about exported service.
 *
 * @author Andrew Sazonov
 * @version 1.0
 */
public class ServiceInfo implements Serializable {
    private static final long serialVersionUID = 1;

    protected String fServiceGroup = null;
    protected String fServiceName = null;
    protected String fServiceURL = null;

    public ServiceInfo(String aServiceName, String aServiceURL) {
        fServiceName = aServiceName;
        fServiceURL = aServiceURL;
    }

    public ServiceInfo(String aServiceGroup, String aServiceName, String aServiceURL) {
        fServiceGroup = aServiceGroup;
        fServiceName = aServiceName;
        fServiceURL = aServiceURL;
    }

    public String getServiceGroup() {
        return fServiceGroup;
    }

    public void setServiceGroup(String aServiceGroup) {
        fServiceGroup = aServiceGroup;
    }

    public String getServiceName() {
        return fServiceName;
    }

    public void setServiceName(String aServiceName) {
        fServiceName = aServiceName;
    }

    public String getServiceURL() {
        return fServiceURL;
    }

    public void setServiceURL(String aServiceURL) {
        fServiceURL = aServiceURL;
    }
}
