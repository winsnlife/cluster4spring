package com.hfzy.ihk.cloud.common.rmi;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.rmi.server.RemoteServer;
import java.util.Set;

/**
 * RMI 拦截器
 * 只允许 allowed 中 存在的ip 调用方法,空时允许所有ip访问
 * @author winsn 2015-8-3
 * @version 1.0
 * */
class RmiSecurityInterceptor implements MethodInterceptor {

    private Set allowed;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {

        String clientHost = RemoteServer.getClientHost();

        if (allowed.isEmpty() || allowed.contains(clientHost)) {
            return methodInvocation.proceed();
        } else {
            throw new SecurityException("非法访问。");
        }

    }

    public void setAllowed(Set allowed) {
        this.allowed = allowed;
    }

}
