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

package org.softamis.cluster4spring.support.provider;

import static java.text.MessageFormat.format;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationFactory;

import org.softamis.cluster4spring.support.Endpoint;
import org.softamis.cluster4spring.support.EndpointFactory;
import org.softamis.cluster4spring.support.ServiceMoniker;

/**
 * Implementation of <code>EndpointProvider</code> which uses the list of
 * explicitely specified "url" of the service to create service endpoint. Basically,
 * any object may  represent service "url" - it is responsibility of concrete implementation
 * of <code>EndpointFactory</code> to interpret given object and create appropriate
 * endpoint for remote service that could be located based on given "url".
 *
 * @param <SI> type of data used to invoke remote service (such as remote service URL)
 * @param <E>  type of endpoints that could be created by this factory
 * @author Andrew Sazonov
 * @version 1.0
 */
public class UrlListEndpointProvider<E extends Endpoint<SI>, SI extends ServiceMoniker>
        extends AbstractUrlListEndpointProvider<E, SI> {
    protected static final Log fLog = LogFactory.getLog(UrlListEndpointProvider.class);

    //存在部分服务端点调用失败开始时间
    private Date failStartTime;
    //失败次数
    private Long failTimes = 0L;

    /**
     * List of urls used to discover remote service and create service endpoints
     */
    protected List<SI> fServiceMonikers = null;

    public UrlListEndpointProvider() {
        super();
    }

    /**
     * Invoked by Spring as part of bean' lifecycle. In addition to inherited checks, adds checking
     * that list of service urls is specified.
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet()
            throws Exception {
        super.afterPropertiesSet();

        // simply check that we have information abount remote service locations
        if (fServiceMonikers == null || fServiceMonikers.isEmpty()) {
            String message = "List of service URLs should be provided during EndPointProvider initialization";
            if (fLog.isErrorEnabled()) {
                fLog.error(message);
            }
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Creates list of endpoints available for service. Method walks on list of specified urls for
     * service and tries to create service endpoint for every url. If creation of service endpoint
     * is failed for some url, method silently handles this and endpoint that corresponds to failed
     * url is not included into resulting list.
     *
     * @param aRemoteInvocationFactory factory used to create remote invocation
     * @param aEndpointFactory         factory used to create endpoints
     * @param aBeanName
     * @return list of created endpoints
     * @throws org.springframework.remoting.RemoteAccessException thrown if after processing of all urls the resulting list of endpoints is empty
     */
    @Override
    protected List<E> doRefreshServiceEndpointsList(RemoteInvocationFactory aRemoteInvocationFactory,
                                                    EndpointFactory<E, SI> aEndpointFactory,
                                                    String aBeanName)
            throws RemoteAccessException {
        int servicesSize = fServiceMonikers.size();
        List<E> result = new ArrayList<E>(servicesSize);

        // simply walk over list of remote service locations and try to create endpoint for
        // every location
        for (SI serviceInfo : fServiceMonikers) {
            if (fLog.isTraceEnabled()) {
                String message =
                        format("Starting endpoint creation. Bean Name: [{0}]. Service Info: [{1}]", aBeanName, serviceInfo);
                fLog.trace(message);
            }

            E endpoint = null;
            try {
                endpoint = doCreateServiceEndpoint(aRemoteInvocationFactory, aEndpointFactory, aBeanName, serviceInfo);
            } catch (RemoteAccessException e) {
                if (fLog.isErrorEnabled()) {
                    String message = format("无法创建RMI服务端点对象（一般是服务器挂了）,Unable to create service endpoint. Bean Name: [{0}]. Service Info: [{1}]", aBeanName,
                            serviceInfo.getServiceURL());
                    fLog.error(message, e);
                }
            }
            if (endpoint != null) {
                result.add(endpoint);
            }
        }
        if (result.isEmpty()) {
            String message =
                    format("Unable to determine at least one service endpoint for server. Bean Name: [{0}]", aBeanName);
            throw new RemoteAccessException(message);
        }
        return result;
    }

    @Override
    protected List<E> getCachedEndpoints(RemoteInvocationFactory aRemoteInvocationFactory, EndpointFactory<E, SI> aEndpointFactory, String aBeanName) {

        if (fEndpointsCache == null) {
            String message = format("缓存中没有RMI对象，现在刷新并更新缓存：refresh(aRemoteInvocationFactory, aEndpointFactory, Bean Name: [{0}]);", aBeanName);
            fLog.trace(message);
            refresh(aRemoteInvocationFactory, aEndpointFactory, aBeanName);
        }else {

            if(fServiceMonikers!= null && fEndpointsCache.size() < fServiceMonikers.size()) {

                String message = format("缓存中的对象数量少于配置的服务数,进行熔断保护检测，Bean Name: [{0}]);", aBeanName);
                fLog.trace(message);

                if(failTimes == 0){
                    failStartTime = new Date();
                    failTimes = 1L;
                }else {
                    failTimes += 1L;
                }

                if(failTimes>10){//10次失败后每过1分钟，刷新一次
                    //距离存在失败端点时的时间（秒）
                    Long timeOffset = (System.currentTimeMillis()-failStartTime.getTime())/1000;
                    Long veryMin = timeOffset%60;
                     message = format("10失败以上，现在检测一次，Bean Name: [{0}],timeOffset:[{1}],veryMin:[{2}]);", aBeanName,timeOffset,veryMin);
                    fLog.trace(message);
                    if( veryMin == 0){
                        message = format("检测条件满足，现在刷新并更新缓存：failStartTime:[{0}],failTimes:[{1}] Bean Name: [{2}]);",
                                failStartTime.toString(),failTimes, aBeanName);
                        fLog.trace(message);
                        refresh(aRemoteInvocationFactory, aEndpointFactory, aBeanName);
                    }

                }else {//失败后，允许10次连续刷新服务端点
                    message = format("10次失败内，缓存中的对象数量少于配置的服务数，现在刷新并更新缓存：failStartTime:[{0}],failTimes:[{1}] Bean Name: [{2}]);",
                            failStartTime.toString(),failTimes, aBeanName);
                    fLog.trace(message);
                    refresh(aRemoteInvocationFactory, aEndpointFactory, aBeanName);
                }
            }else {
                if(failTimes>0) {
                    if (fEndpointsCache.size() == fServiceMonikers.size()) {
                        String message = format("缓存中的对象数量已经不少于配置的服务数,确认所有端点对象都正常后，退出熔断保护检测，Bean Name: [{0}]);", aBeanName);
                        fLog.trace(message);
                        failStartTime = null;
                        failTimes = 0L;
                    }
                }
            }
        }
        List<E> result = doGetCachedEndpoints();
        fLog.trace("从缓存中返回RMI服务对象列表，beanName:"+aBeanName +" ,size:"+((result==null)?0:result.size()));
        return result;
    }

    /**
     * Marks given endpoint invalid. This endpoint will not be later used for methods invocation.
     * If configured to cache endpoints, removes endpoint from the list of cached endpoints.
     * Otherwise, does nothing.
     *
     * @param aBeanName name of bean that is used as proxy for remote service
     * @param aEndpoint endpoint to be marked invalid
     * @see #setCacheEndpoints(boolean)
     */
    public void markInvalid(String aBeanName, E aEndpoint) {
        if (fCacheEndpoints) // we are in cashe mode, so we need to remove endpoint from cache
        {
            synchronized (fCacheLock) {
                if (fEndpointsCache != null) {
                    fEndpointsCache.remove(aEndpoint);
                }
            }
        } else {
            // endpoints list will be selected during next invocation of remote service
        }
    }

    /**
     * Sets list of urls (in form of <code>ServiceMoniker</code>) used to discover remote service and create service endpoints
     *
     * @param aServiceMonikers list of urls
     * @see ServiceMoniker
     */
    public void setServiceMonikers(List<SI> aServiceMonikers) {
        if (aServiceMonikers == null || aServiceMonikers.isEmpty()) {
            String message = "Null or empty list of service monikers passed";
            if (fLog.isErrorEnabled()) {
                fLog.error(message);
            }
            throw new IllegalArgumentException(message);
        }
        fServiceMonikers = aServiceMonikers;
    }

    /**
     * Returns list of <code>ServiceMonikers</code> used to discover remote service and create service endpoints
     *
     * @return list of urls
     * @see ServiceMoniker
     */
    public List<SI> getServiceMonikers() {
        return fServiceMonikers;
    }

    /**
     * Utility method which allows to specify list of URL when URL of service locations is specified
     * in simple string form.
     *
     * @param aServiceURL
     */
    public void setServiceURLs(List<String> aServiceURL) {
        if (aServiceURL == null || aServiceURL.isEmpty()) {
            String message = "Null or empty list of service URLs passed";
            if (fLog.isErrorEnabled()) {
                fLog.error(message);
            }
            throw new IllegalArgumentException(message);
        }
        fServiceMonikers = new ArrayList<SI>(aServiceURL.size());
        for (String url : aServiceURL) {
            SI moniker = (SI) new ServiceMoniker(url);
            fServiceMonikers.add(moniker);
        }
    }
}
