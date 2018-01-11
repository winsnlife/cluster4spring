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

import java.lang.reflect.Method;

import static java.text.MessageFormat.format;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.springframework.util.ClassUtils.getQualifiedMethodName;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * AOP Alliance MethodInterceptor for tracing remote invocations.
 * Automatically applied by RemoteExporter and its subclasses.
 * <p/>
 * Logs an incoming remote call as well as the finished processing of a remote call
 * at DEBUG level. If the processing of a remote call results in a checked exception,
 * the exception will get logged at INFO level; if it results in an unchecked
 * exception (or error), the exception will get logged at WARN level.
 * <p/>
 * In addition, interceptors prints information about time spent for remote method invocation.
 * <p/>
 * The logging of exceptions is particularly useful to save the stacktrace
 * information on the server-side rather than just propagating the exception
 * to the client (who might or might not log it properly).
 *
 * @author Andrew Sazonov
 * @see org.springframework.remoting.support.RemoteExporter#setRegisterTraceInterceptor
 * @see org.springframework.remoting.support.RemoteExporter#getProxyForService
 * @since 1.2
 */

public class ServerRemoteInvocationTraceInterceptor implements MethodInterceptor {
    protected static final Log fLogger = LogFactory.getLog(ServerRemoteInvocationTraceInterceptor.class);

    protected String fExporterName = null;
    protected String fServiceKey = null;

    /**
     * Create a new RemoteInvocationTraceInterceptor.
     *
     * @param aProtocolName the name of the cluster4spring protocol
     * (to be used as context information in log messages)
     * @param aServiceKey key used to denote a service
     */
    public ServerRemoteInvocationTraceInterceptor(String aProtocolName, String aServiceKey) {
        fExporterName = aProtocolName;
        fServiceKey = aServiceKey;
    }

    @SuppressWarnings({"InstanceofCatchParameter", "ProhibitedExceptionThrown", "ProhibitedExceptionDeclared"})
    public Object invoke(MethodInvocation aInvocation)
            throws Throwable {
        Method method = aInvocation.getMethod();
        if (fLogger.isDebugEnabled()) {
            String qualifiedMethodName = getQualifiedMethodName(method);
            String message = format("Incoming {0} remote call [{1}]. Service Key: [{2}]", fExporterName, qualifiedMethodName,
                    fServiceKey);
            fLogger.debug(message);
        }
        try {
            long time = System.currentTimeMillis();

            Object retVal = aInvocation.proceed();

            long delta = System.currentTimeMillis() - time;

            if (fLogger.isDebugEnabled()) {
                String qualifiedMethodName = getQualifiedMethodName(method);
                fLogger.debug(format(
                        "Finished processing of {0} remote call: [{1}]. Service Key: [{2}]. Execution time: {3} ms",
                        fExporterName, qualifiedMethodName, fServiceKey, delta));
            }
            return retVal;
        } catch (Throwable ex) {
            if (ex instanceof RuntimeException || ex instanceof Error) {
                if (fLogger.isWarnEnabled()) {
                    String qualifiedMethodName = getQualifiedMethodName(method);
                    fLogger.warn(format("Processing of {0} remote call resulted in fatal exception: [{1}]. Service Key: [{2}]",
                            fExporterName, qualifiedMethodName, fServiceKey), ex);
                }
            } else {
                if (fLogger.isInfoEnabled()) {
                    String qualifiedMethodName = getQualifiedMethodName(method);
                    fLogger.info(format("Processing of {0} remote call resulted in exception: [{1}] Service Key: {2}",
                            fExporterName, qualifiedMethodName, fServiceKey), ex);
                }
            }
            throw ex;
        }
    }

}

