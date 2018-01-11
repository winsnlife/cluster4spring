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

import static java.text.MessageFormat.format;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.BeansException;

import org.aopalliance.aop.Advice;

/**
 * Utility class used to collect advices with given names from bean factory.
 * *
 *
 * @author Andrew Sazonov
 * @version 1.0
 */
public class AdvicesListProvidingUtils {
    private static final Log fLog = LogFactory.getLog(AdvicesListProvidingUtils.class);

    /**
     * Collects advices from given bean factory based on their name. If bean denoted by name
     * is not advisor or advice, it silently ignored and is not added to resulting list.
     *
     * @param aBeanFactory factory where advice beans are collected
     * @param aInterceptorNames array of advices names
     * @return list of advices that corresponds to given names
     * @throws org.springframework.beans.BeansException thrown if obtaining Advice bean from
     * bean factory by one of given interceptor names was not successfull
     */
    public static List<Advice> getAdvices(BeanFactory aBeanFactory, String[] aInterceptorNames)
            throws BeansException {
        List<Advice> result = new ArrayList<Advice>();

        if (aInterceptorNames != null && aInterceptorNames.length != 0) {
            // materialize interceptor chain from bean names
            for (String beanName : aInterceptorNames) {
                if (fLog.isDebugEnabled()) {
                    fLog.debug(format("Configuring advisor or advice ''{0}''", beanName));
                }

                if (isNamedBeanAnAdvisorOrAdvice(aBeanFactory, beanName)) {
                    Advice advice = (Advice) aBeanFactory.getBean(beanName);
                    result.add(advice);
                } else {
                    if (fLog.isErrorEnabled()) {
                        fLog.error(format("Bean marked as advice does not represent advice. Bean name: {0}", beanName));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Determines whether bean with given name represents Advisor or Advice
     * @param aBeanFactory factory used to check bean
     * @param aBeanName bean name
     * @return true if bean with given name is Advisor or Advice, false otherwise
     */
    protected static boolean isNamedBeanAnAdvisorOrAdvice(BeanFactory aBeanFactory, String aBeanName) {
        boolean result = true;
        Class namedBeanClass = aBeanFactory.getType(aBeanName);
        if (namedBeanClass != null) {
            result = Advisor.class.isAssignableFrom(namedBeanClass) || Advice.class.isAssignableFrom(namedBeanClass);
        }
        return result;
    }

}
