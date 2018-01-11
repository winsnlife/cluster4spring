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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import static java.text.MessageFormat.format;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.rmi.RmiClientInterceptorUtils;

import org.softamis.cluster4spring.support.EndpointFactory;
import org.softamis.cluster4spring.support.RemoteClientInterceptor;
import org.softamis.cluster4spring.support.ServiceMoniker;

import org.aopalliance.aop.AspectException;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Interceptor for accessing remote services via RMI.  Adds processing or RMI specific
 * exceptions in <code>doInvoke</code> method. 
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 */


@SuppressWarnings({"ProhibitedExceptionDeclared", "ProhibitedExceptionThrown"})
public class RmiClientInterceptor<SI extends ServiceMoniker>
        extends RemoteClientInterceptor<RmiEndpoint<SI>, SI> {
    protected static final Log fLog = LogFactory.getLog(RmiClientInterceptor.class);

    public static final String PROTOCOL_RMI = "RMI";

    public RmiClientInterceptor() {
        super();
    }

    /**
     * Returns protocol which identifies interceptor. Used for logging.
     *
     * @return protocol name
     */
    @Override
    protected String getProtocol() {
        return PROTOCOL_RMI;
    }

    /**
     * Creates default endpoint factory used to create RMI endpoints.
     *
     * @return EndpointFactory that should be used if one is is not specified explicitely
     * @see RmiEndpointFactory
     */
    @Override
    protected EndpointFactory<RmiEndpoint<SI>, SI> createDefaultEndpointFactory() {
        EndpointFactory<RmiEndpoint<SI>, SI> result = new RmiEndpointFactory<SI>();
        return result;
    }

    /**
     * Determine whether the given RMI exception indicates a connect failure.
     *
     * @param aException the RMI exception to check
     * @return whether the exception should be treated as connect failure
     * @see org.springframework.remoting.rmi.RmiClientInterceptorUtils#isConnectFailure
     */
    @Override
    protected boolean isConnectFailure(RemoteAccessException aException) {
        boolean result = false;
        Throwable ex = aException.getCause();
        if (ex instanceof RemoteException) {
            RemoteException remoteException = (RemoteException) ex;
            result = isConnectFailureOnRemoteException(remoteException);
        }
        return result;
    }

    /**
     * Utility method to definie whether given remote excption indicates a connect failure.
     * Default implementation delegates to RmiClientInterceptorUtils.
     *
     * @param aRemoteException exception to inspect
     * @return <code>true</code> if given exception indicates a connect failure
     */
    protected boolean isConnectFailureOnRemoteException(RemoteException aRemoteException) {
        boolean result = RmiClientInterceptorUtils.isConnectFailure(aRemoteException);
        return result;
    }


    /**
     * Perform the given aInvocation on the given RMI aStub.
     *
     * @param aInvocation the AOP method aInvocation
     * @param aServiceEndpoint the RMI aStub to invoke
     * @return the aInvocation result, if any
     * @throws Throwable in case of aInvocation failure
     */
    @Override
    protected Object doInvoke(MethodInvocation aInvocation, RmiEndpoint<SI> aServiceEndpoint)
            throws Throwable {
        if (fLog.isTraceEnabled()) {
            fLog.trace("Starting invoking remote invocation via RMI");
        }

        // here we delegate invocation of method to endpoint
        // and adds RMI-specific processing of exceptions which
        // may occur during that call

        Object result = null;
        try {
            result = aServiceEndpoint.invoke(aInvocation);
        } catch (RemoteException ex) {
            if (fLog.isTraceEnabled()) {
                fLog.trace("Remote Exception: ", ex);
            }
            throwRmiAccessException(aInvocation, ex, aServiceEndpoint);
        } catch (InvocationTargetException ex) {
            if (fLog.isTraceEnabled()) {
                fLog.trace("Invocation Target Exception:", ex);
            }

            Throwable targetEx = ex.getTargetException();
            if (targetEx instanceof RemoteException) {
                RemoteException rex = (RemoteException) targetEx;
                throwRmiAccessException(aInvocation, rex, aServiceEndpoint);
            } else {
                throw targetEx;
            }
        } catch (Throwable ex) {
            if (fLog.isTraceEnabled()) {
                fLog.trace("Throwable on invocation", ex);
            }

            SI serviceInfo = aServiceEndpoint.getServiceInfo();
            String protocol = getProtocol();
            String message = format("Failed to invoke {0} remote service [{0}] on [1]", protocol, fBeanName, serviceInfo);
            throw new AspectException(message, ex);
        }
        return result;
    }

    /**
     * Utility method to throw RmiAccessException
     * Utility method to throw RmiAccessException
     *
     * @param aInvocation
     * @param aRemoteException
     * @param aServiceEndpoint
     * @throws Exception
     */
    protected void throwRmiAccessException(MethodInvocation aInvocation,
                                           RemoteException aRemoteException,
                                           RmiEndpoint<SI> aServiceEndpoint)
            throws Exception {
        Method method = aInvocation.getMethod();
        boolean connectFailure = isConnectFailureOnRemoteException(aRemoteException);
        SI serviceInfo = aServiceEndpoint.getServiceInfo();
        Exception e = convertRmiAccessException(method, aRemoteException, connectFailure, serviceInfo);
        throw e;
    }

    /**
     * Utility method used to convert remote exception to corresponding
     * <code>RemoteAccessException</code>
     *
     * @param aMethod methods that threw exception
     * @param aRemoteException remote exception
     * @param aConnectFailure true if exception is connect failure
     * @param aServiceInfo information about service url where remote exception occured
     * @return appropriate conferted exception
     */
    protected Exception convertRmiAccessException(Method aMethod,
                                                  RemoteException aRemoteException,
                                                  boolean aConnectFailure,
                                                  SI aServiceInfo) {
        if (fLog.isDebugEnabled()) {
            String message = format("Remote service [{0}] threw exception", aServiceInfo);
            fLog.debug(message, aRemoteException);
        }
        Exception result = null;
        Class<?>[] exceptionTypes = aMethod.getExceptionTypes();
        List<? extends Class<?>> exceptionClassesList = Arrays.asList(exceptionTypes);
        if (exceptionClassesList.contains(RemoteException.class)) {
            result = new RemoteAccessException("Cannot access remote service [" + aServiceInfo + "]", aRemoteException);
        } else {
            if (aConnectFailure) {
                result = new RemoteConnectFailureException("Cannot connect to remote service [" + aServiceInfo + "]",
                        aRemoteException);
            } else {
                String message = format("Cannot access remote service [{0}]", aServiceInfo);
                result = new RemoteAccessException(message, aRemoteException);
            }
        }
        return result;
    }
}
