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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.springframework.remoting.rmi.RmiClientInterceptorUtils;
import org.springframework.remoting.rmi.RmiInvocationHandler;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.softamis.cluster4spring.support.Endpoint;

import org.aopalliance.intercept.MethodInvocation;


/**
 * Implementation of Endpoint that is used in RMI based cluster4spring.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> - information about service
 */
@SuppressWarnings({"ProhibitedExceptionDeclared"})
public class RmiEndpoint<SI extends Serializable>
        extends Endpoint<SI> {
    /**
     * Remote object used by endpoint fpr remote service invocation
     */
    protected Remote fRemote = null;

    /**
     * Creates RMI endpoint.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocation
     * @param aEndpointKey key used to identity endpoint
     * @param aServiceInfo information need to locate service
     * @param aRemote remote object
     */
    public RmiEndpoint(RemoteInvocationFactory aRemoteInvocationFactory,
                       String aEndpointKey,
                       SI aServiceInfo,
                       Remote aRemote) {
        super(aRemoteInvocationFactory, aEndpointKey, aServiceInfo);
        fRemote = aRemote;
    }


    /**
     * Performs invocation of given MehodInvocation. Method analyzes whether passed invocation
     * is <code>RmiInvocationHandler</code> or regular RMI stub and invokes them accordingly.
     *
     * @param aMethodInvocation method invocation to invoke
     * @return result of invocation, if any
     * @throws Throwable exception that could occur during invocation
     * @see #invokeUsingInvocationHandler(org.aopalliance.intercept.MethodInvocation)
     * @see #invokeUsingRmiStub(org.aopalliance.intercept.MethodInvocation)
     */
    @Override
    protected Object doInvoke(MethodInvocation aMethodInvocation)
            throws Throwable {
        Object result;

        // Remote contained by endpoint may be either traditional RMI stub or
        // instance of RmiInvocation handler and we should treat each case
        // separately

        if (fRemote instanceof RmiInvocationHandler)   // RMI invoker
        {
            result = invokeUsingInvocationHandler(aMethodInvocation);

        } else // traditional RMI aStub
        {
            result = invokeUsingRmiStub(aMethodInvocation);

        }
        return result;
    }

    /**
     * Invokes given method invocation using traditional RMI stub.
     *
     * @param aInvocation method invocation to invoke
     * @return result of invocation, if any
     * @throws Throwable exception that could occur during invocation
     */
    protected Object invokeUsingRmiStub(MethodInvocation aInvocation)
            throws Throwable {
        // traditional RMI aStub
        Object result = RmiClientInterceptorUtils.doInvoke(aInvocation, fRemote);
        return result;
    }

    /**
     * Invokes given method invocation using RMI invoker
     *
     * @param aInvocation method invocation to invoke
     * @return result of invocation, if any
     * @throws Throwable exception that could occur during invocation
     */
    protected Object invokeUsingInvocationHandler(MethodInvocation aInvocation)
            throws Throwable {
        // RMI invoker
        Object result = doInvokeInternal(aInvocation, (RmiInvocationHandler) fRemote);
        return result;
    }

    /**
     * Apply the given AOP method invocation to the given RmiInvocationHandler.
     * The default implementation calls invoke with a plain RemoteInvocation.
     * <p>Can be overridden in subclasses to provide custom RemoteInvocation
     * subclasses, containing additional invocation parameters like user
     * credentials. Can also process the returned result object.
     *
     * @param aMethodInvocation the current AOP method invocation
     * @param aInvocationHandler the RmiInvocationHandler to apply the invocation to
     * @return the invocation result
     * @throws NoSuchMethodException     if the method name could not be resolved
     * @throws IllegalAccessException    if the method could not be accessed
     * @throws InvocationTargetException if the method invocation resulted in an exception
     * @see RemoteInvocation
     */
    protected Object doInvokeInternal(MethodInvocation aMethodInvocation, RmiInvocationHandler aInvocationHandler)
            throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        RemoteInvocation remoteInvocation = createRemoteInvocation(aMethodInvocation);
        Object result = aInvocationHandler.invoke(remoteInvocation);
        return result;
    }


    /**
     * Returns remote object used by endpoint
     *
     * @return remote object used by endpoint
     */
    public Remote getRemote() {
        return fRemote;
    }

    /**
     * Sets remote object used by endpoint
     *
     * @param aRemote remote object used by endpoint
     */
    public void setRemote(Remote aRemote) {
        fRemote = aRemote;
    }
}
