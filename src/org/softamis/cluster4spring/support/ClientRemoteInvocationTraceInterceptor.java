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

import org.springframework.util.ClassUtils;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Utility interceptor that allows to intercept and log remote calls on client side.
 * Instance of this class is created by appropriate <code>ProxyFactoryBean</code>
 * which is used to create proxies to remote service.
 * <p/>
 * Invocation log is perfomed on DEBUG level, Exceptions that are Errors or Runtime are
 * logged on warn level, other exceptions are logged on info level.
 * <p/>
 * In addition, interceptors prints information about time spent for remote method invocation.
 *
 * @author Andrew Sazonov
 * @version 1.0
 * @see org.softamis.cluster4spring.rmi.RmiProxyFactoryBean#setRegisterTraceInterceptor(boolean)
 */

@SuppressWarnings({"ProhibitedExceptionDeclared", "ProhibitedExceptionThrown"})
public class ClientRemoteInvocationTraceInterceptor
        implements MethodInterceptor

{
    protected static final Log fLog = LogFactory.getLog(ClientRemoteInvocationTraceInterceptor.class);
    protected String fProtocolName = null;
    protected String fServiceKey = null;


    /**
     * Create a new RemoteInvocationTraceInterceptor.
     *
     * @param aProtocolName the name of the cluster4spring protocol
     * (to be used as context information in log messages)
     */
    public ClientRemoteInvocationTraceInterceptor(String aProtocolName, String aServiceKey) {
        fProtocolName = aProtocolName;
        fServiceKey = aServiceKey;
    }

    /**
     * Handles method invocation. Adds logging for invoked method as well as time of method executions.
     * Log category that is used is <b>org.softamis.cluster4spring.impl.ClientRemoteInvocationTraceInterceptor</b>.
     * Debug level for appropriate logger is DEBUG. If exception during invocation occured, it will be written to
     * log with ERROR (for <code>RuntimeException</code> or <code>Error</code>) or with WARN level (otherwise).
     * @param aInvocation remote method invocation
     * @return result of method invocation execution
     * @throws Throwable exception throwed by invocation
     */
    public Object invoke(MethodInvocation aInvocation)
            throws Throwable {
        Method method = aInvocation.getMethod();
        if (fLog.isDebugEnabled()) {
            String qualifiedMethodName = ClassUtils.getQualifiedMethodName(method);
            String message = format("Outgoing [{0}] remote call: [{1}]. Service Key: [{2}]", fProtocolName,
                    qualifiedMethodName, fServiceKey);
            fLog.debug(message);
        }
        try {
            long time = System.currentTimeMillis();

            Object retVal = aInvocation.proceed();

            long delta = System.currentTimeMillis() - time;
            if (fLog.isDebugEnabled()) {
                String qualifiedMethodName = ClassUtils.getQualifiedMethodName(method);
                String message = format(
                        "Finished processing of outgoing [{0}] remote call: [{1}]. Service Key: [{2}]. Execution time [{2}] ms.",
                        fProtocolName, qualifiedMethodName, fServiceKey, delta);
                fLog.debug(message);
            }
            return retVal;
        } catch (Throwable ex) {
            if (ex instanceof RuntimeException || ex instanceof Error) {
                if (fLog.isWarnEnabled()) {
                    String qualifiedMethodName = ClassUtils.getQualifiedMethodName(method);
                    String message = format(
                            "Processing of outgoing [{0}] remote call resulted in fatal exception: [{1}]. Service Key: [{2}]",
                            fProtocolName, qualifiedMethodName, fServiceKey);
                    fLog.warn(message, ex);
                }
            } else {
                if (fLog.isInfoEnabled()) {
                    String message = format(
                            "Processing of outgoing [{0}] remote call resulted in exception: [{1}]. Service Key: [{2}]",
                            fProtocolName, ClassUtils.getQualifiedMethodName(method), fServiceKey);
                    fLog.info(message, ex);
                }
            }
            throw ex;
        }
    }

}
