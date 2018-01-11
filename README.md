
这是cluster4spring 框架的扩展。
源项目地址：http://www.theserverside.com/discussions/thread/45766.html

本人在他的基础上，修复了一些bug。

主要修复了客户端使用多个URL调用多个rmi服务的时候，如果服务端重启，那么这个服务将不会再被调用的问题。

另外，个人基于zookeeper实现了服务的注册与发现(依赖com.101tec:zkclient:0.8,zookeeper:3.4.6+)。

下面是案例：
一、客户端负载，静态配置

（1）、客户端

	<bean id="rmiClientSetting" abstract="true">
		<!-- 是否启动时加载stub -->
		<property name="refreshEndpointsOnStartup" value="false" />
		<!-- 调用异常时，将重新加载stub -->
		<property name="refreshEndpointsOnConnectFailure" value="true" />
		<!-- 调用失败时是否切换到其他服务端 -->
		<property name="switchEndpointOnFailure" value="true" />
		<!-- 追踪拦截器启用 -->
		<property name="registerTraceInterceptor" value="true" />
		<!-- 缓存stub 继承AbstractRmiShortFormProxyFactoryBean才有-->
		<property name="cacheEndpoints" value="true" />
		
	</bean>
	
	<bean id="serviceName"
		  class="org.softamis.cluster4spring.rmi.RmiUrlListProxyFactoryBean" parent="rmiClientSetting">
		<!-- 远程服务对象接口类 -->
		<property name="serviceInterface" value="com.xxxx.xxxx.xxx"/>
		<!-- 远程对象地址list ，如果第一个无法使用，会抛异常，然后使用第二个去调用远程对象方法返回结果-->
		<property name="serviceURLs">
			<list>
				<value>rmi://ip1:60000/serviceName</value>
				<value>rmi://ip2:60000/serviceName</value>
			</list>
		</property>
		<!-- 调用时选择那个url的策略 -->
		<property name="endpointSelectionPolicy">
			<bean class="org.softamis.cluster4spring.support.invocation.ShuffleEndpointSelectionPolicy"/>
		</property>
	</bean>

	（2）、服务端
		<!-- RMI调用拦截器 控制调用权限 -->
		<bean id="rmiSecurityInterceptor" class="org.softamis.cluster4spring.support.RmiSecurityInterceptor">
			<!-- 这里配置允许访问rmi的客户端IP地址 ,空时允许所有IP访问-->
			<property name="allowed">
				<set>
					<!--
					<value>ip</value>
					-->
				</set>
			</property>
		</bean>
		
		<bean id="rmiServerPropertySetting" abstract="true">
			<property name="interceptors">
				<list>
					<ref bean="rmiSecurityInterceptor"/>
				</list>
			</property>
			<property name="registryPort" value="60000"/><!-- 默认1099 -->
			<property name="servicePort" value="60000"/><!--tcp通信端口，不指定就随机 -->
		</bean>
		
		<bean id="obj" class="xxx.xxx.x"/>
		
		<bean class="org.springframework.remoting.rmi.RmiServiceExporter" parent="rmiServerPropertySetting">
			<property name="serviceName" value="serviceName"/>
			<property name="service" ref="obj" />
			<property name="serviceInterface"  value="com.xxxx.xxxx.xxx" />
		</bean>
		
二，使用zookeeper动态注册与发现rmi服务

	注：rmi服务注册到zookeeper上路径：/rmiRegistryRoot/provider/rmi服务名
		所以务必定义rmi服务名时保证唯一（主要是为了客户端与服务端的接口存放包名可以不相同这个特性）
	
	（1）、客户端
	
	<bean id="rmiClientSetting" abstract="true">
		<!-- 是否启动时加载stub -->
		<property name="refreshEndpointsOnStartup" value="false" />
		<!-- 调用异常时，将重新加载stub -->
		<property name="refreshEndpointsOnConnectFailure" value="true" />
		<!-- 调用失败时是否切换到其他服务端 -->
		<property name="switchEndpointOnFailure" value="true" />
		<!-- 追踪拦截器启用 -->
		<property name="registerTraceInterceptor" value="true" />
		<!-- 缓存stub 继承AbstractRmiShortFormProxyFactoryBean才有-->
		<property name="cacheEndpoints" value="true" />
	</bean>

	<!--云呼WebSocket服务-->
	<bean id="zkClient" class="org.softamis.cluster4spring.rmi.ZookeeperFactoryBean">
		<!--zookeeper 主机-->
		<property name="zkHosts" value="x.x.x.x:2181,x.x.x.x2:2181,x.x.x.x3:2181"/>
		<property name="zkSessionTimeOut" value="15000"/>
		<property name="zkConnectionTimeout" value="5000"/>
	</bean>
	
	<bean id="rmiClientSetting4Zoo" abstract="true" parent="rmiClientSetting">
		<property name="zkClient" ref="zkClient"/>
	</bean>
	
	<!--最新版已默认随机策略，可以不配置调用策略-->
	<bean id="webSocketService" class="org.softamis.cluster4spring.rmi.ZookeeperRmiDiscoveringProxyFactoryBean" parent="rmiClientSetting4Zoo">
		<!--rmi服务名是必须，用来检索rmi服务列表-->
		<property name="serviceName" value="rmiWebSocketService"/>
		<property name="serviceInterface" value="xxxx.xxx.xx.WebSocketService"/>
	</bean>

	（2）、服务端
	<!-- RMI调用拦截器 控制调用权限 -->
		<bean id="rmiSecurityInterceptor" class="org.softamis.cluster4spring.support.RmiSecurityInterceptor">
			<!-- 这里配置允许访问rmi的客户端IP地址 ,空时允许所有IP访问-->
			<property name="allowed">
				<set>
					<!--
					<value>ip</value>
					-->
				</set>
			</property>
		</bean>
		
		<bean id="rmiServerPropertySetting" abstract="true">
			<property name="interceptors">
				<list>
					<ref bean="rmiSecurityInterceptor"/>
				</list>
			</property>
			<property name="registryPort" value="60000"/><!-- 默认1099 -->
			<property name="servicePort" value="60000"/><!--tcp通信端口，不指定就随机 -->
		</bean>
		
		<bean id="obj" class="xxx.xxx.x"/>
		
		<bean id="xxxExporter" class="org.springframework.remoting.rmi.RmiServiceExporter" parent="rmiServerPropertySetting">
			<!--rmi服务名是必须，用来检索rmi服务列表-->
			<property name="serviceName" value="rmiWebSocketService"/>
			<property name="service" ref="obj" />
			<property name="serviceInterface"  value="com.xxxx.xxxx.WebSocketService" />
		</bean>
		
		<!--注册到zookeeper-->
		<bean class="org.softamis.cluster4spring.rmi.ZookeeperSpringRmiPublisher">
			<!--zookeeper 主机-->
			<property name="zkHosts" value="x.x.x.x:2181,x.x.x.x2:2181,x.x.x.x3:2181"/>
			<property name="zkSessionTimeOut" value="15000"/>
			<property name="zkConnectionTimeout" value="5000"/>
			<property name="rmiServiceExporters">
				<set>
					<ref bean="xxxExporter"/>
				</set>
			</property>
		</bean>