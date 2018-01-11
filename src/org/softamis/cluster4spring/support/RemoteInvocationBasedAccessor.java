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

import org.springframework.remoting.support.DefaultRemoteInvocationFactory;
import org.springframework.remoting.support.RemoteAccessor;
import org.springframework.remoting.support.RemoteInvocationFactory;


/**
 * Base class for remote service accessors that are based on
 * serialization of RemoteInvocation objects. Provides a "remoteInvocationFactory"
 * property, with a DefaultRemoteInvocationFactory as default.
 *
 * @author Andrew Sazonov
 * @since 1.1
 */
@SuppressWarnings({"ProhibitedExceptionDeclared", "SuppressionAnnotation"})
public class RemoteInvocationBasedAccessor extends RemoteAccessor {
    protected RemoteInvocationFactory fRemoteInvocationFactory = new DefaultRemoteInvocationFactory();

    protected RemoteInvocationBasedAccessor() {
        super();
    }

    /**
     * Set the RemoteInvocationFactory to use for this accessor.
     * A custom invocation factory can add further context information
     * to the invocation, for example user credentials.
     * @param aRemoteInvocationFactory remote invocation factory
     */
    public void setRemoteInvocationFactory(RemoteInvocationFactory aRemoteInvocationFactory) {
        fRemoteInvocationFactory = aRemoteInvocationFactory;
    }

    /**
     * Return the RemoteInvocationFactory used by this accessor.
     */
    public RemoteInvocationFactory getRemoteInvocationFactory() {
        return fRemoteInvocationFactory;
    }

}
