
����cluster4spring ��ܵ���չ��
Դ��Ŀ��ַ��http://www.theserverside.com/discussions/thread/45766.html

���������Ļ����ϣ��޸���һЩbug��

��Ҫ�޸��˿ͻ���ʹ�ö��URL���ö��rmi�����ʱ������������������ô������񽫲����ٱ����õ����⡣

���⣬���˻���zookeeperʵ���˷����ע���뷢��(����com.101tec:zkclient:0.8,zookeeper:3.4.6+)��

�����ǰ�����
һ���ͻ��˸��أ���̬����

��1�����ͻ���

	<bean id="rmiClientSetting" abstract="true">
		<!-- �Ƿ�����ʱ����stub -->
		<property name="refreshEndpointsOnStartup" value="false" />
		<!-- �����쳣ʱ�������¼���stub -->
		<property name="refreshEndpointsOnConnectFailure" value="true" />
		<!-- ����ʧ��ʱ�Ƿ��л������������ -->
		<property name="switchEndpointOnFailure" value="true" />
		<!-- ׷������������ -->
		<property name="registerTraceInterceptor" value="true" />
		<!-- ����stub �̳�AbstractRmiShortFormProxyFactoryBean����-->
		<property name="cacheEndpoints" value="true" />
		
	</bean>
	
	<bean id="serviceName"
		  class="org.softamis.cluster4spring.rmi.RmiUrlListProxyFactoryBean" parent="rmiClientSetting">
		<!-- Զ�̷������ӿ��� -->
		<property name="serviceInterface" value="com.xxxx.xxxx.xxx"/>
		<!-- Զ�̶����ַlist �������һ���޷�ʹ�ã������쳣��Ȼ��ʹ�õڶ���ȥ����Զ�̶��󷽷����ؽ��-->
		<property name="serviceURLs">
			<list>
				<value>rmi://ip1:60000/serviceName</value>
				<value>rmi://ip2:60000/serviceName</value>
			</list>
		</property>
		<!-- ����ʱѡ���Ǹ�url�Ĳ��� -->
		<property name="endpointSelectionPolicy">
			<bean class="org.softamis.cluster4spring.support.invocation.ShuffleEndpointSelectionPolicy"/>
		</property>
	</bean>

	��2���������
		<!-- RMI���������� ���Ƶ���Ȩ�� -->
		<bean id="rmiSecurityInterceptor" class="org.softamis.cluster4spring.support.RmiSecurityInterceptor">
			<!-- ���������������rmi�Ŀͻ���IP��ַ ,��ʱ��������IP����-->
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
			<property name="registryPort" value="60000"/><!-- Ĭ��1099 -->
			<property name="servicePort" value="60000"/><!--tcpͨ�Ŷ˿ڣ���ָ������� -->
		</bean>
		
		<bean id="obj" class="xxx.xxx.x"/>
		
		<bean class="org.springframework.remoting.rmi.RmiServiceExporter" parent="rmiServerPropertySetting">
			<property name="serviceName" value="serviceName"/>
			<property name="service" ref="obj" />
			<property name="serviceInterface"  value="com.xxxx.xxxx.xxx" />
		</bean>
		
����ʹ��zookeeper��̬ע���뷢��rmi����

	ע��rmi����ע�ᵽzookeeper��·����/rmiRegistryRoot/provider/rmi������
		������ض���rmi������ʱ��֤Ψһ����Ҫ��Ϊ�˿ͻ��������˵Ľӿڴ�Ű������Բ���ͬ������ԣ�
	
	��1�����ͻ���
	
	<bean id="rmiClientSetting" abstract="true">
		<!-- �Ƿ�����ʱ����stub -->
		<property name="refreshEndpointsOnStartup" value="false" />
		<!-- �����쳣ʱ�������¼���stub -->
		<property name="refreshEndpointsOnConnectFailure" value="true" />
		<!-- ����ʧ��ʱ�Ƿ��л������������ -->
		<property name="switchEndpointOnFailure" value="true" />
		<!-- ׷������������ -->
		<property name="registerTraceInterceptor" value="true" />
		<!-- ����stub �̳�AbstractRmiShortFormProxyFactoryBean����-->
		<property name="cacheEndpoints" value="true" />
	</bean>

	<!--�ƺ�WebSocket����-->
	<bean id="zkClient" class="org.softamis.cluster4spring.rmi.ZookeeperFactoryBean">
		<!--zookeeper ����-->
		<property name="zkHosts" value="x.x.x.x:2181,x.x.x.x2:2181,x.x.x.x3:2181"/>
		<property name="zkSessionTimeOut" value="15000"/>
		<property name="zkConnectionTimeout" value="5000"/>
	</bean>
	
	<bean id="rmiClientSetting4Zoo" abstract="true" parent="rmiClientSetting">
		<property name="zkClient" ref="zkClient"/>
	</bean>
	
	<!--���°���Ĭ��������ԣ����Բ����õ��ò���-->
	<bean id="webSocketService" class="org.softamis.cluster4spring.rmi.ZookeeperRmiDiscoveringProxyFactoryBean" parent="rmiClientSetting4Zoo">
		<!--rmi�������Ǳ��룬��������rmi�����б�-->
		<property name="serviceName" value="rmiWebSocketService"/>
		<property name="serviceInterface" value="xxxx.xxx.xx.WebSocketService"/>
	</bean>

	��2���������
	<!-- RMI���������� ���Ƶ���Ȩ�� -->
		<bean id="rmiSecurityInterceptor" class="org.softamis.cluster4spring.support.RmiSecurityInterceptor">
			<!-- ���������������rmi�Ŀͻ���IP��ַ ,��ʱ��������IP����-->
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
			<property name="registryPort" value="60000"/><!-- Ĭ��1099 -->
			<property name="servicePort" value="60000"/><!--tcpͨ�Ŷ˿ڣ���ָ������� -->
		</bean>
		
		<bean id="obj" class="xxx.xxx.x"/>
		
		<bean id="xxxExporter" class="org.springframework.remoting.rmi.RmiServiceExporter" parent="rmiServerPropertySetting">
			<!--rmi�������Ǳ��룬��������rmi�����б�-->
			<property name="serviceName" value="rmiWebSocketService"/>
			<property name="service" ref="obj" />
			<property name="serviceInterface"  value="com.xxxx.xxxx.WebSocketService" />
		</bean>
		
		<!--ע�ᵽzookeeper-->
		<bean class="org.softamis.cluster4spring.rmi.ZookeeperSpringRmiPublisher">
			<!--zookeeper ����-->
			<property name="zkHosts" value="x.x.x.x:2181,x.x.x.x2:2181,x.x.x.x3:2181"/>
			<property name="zkSessionTimeOut" value="15000"/>
			<property name="zkConnectionTimeout" value="5000"/>
			<property name="rmiServiceExporters">
				<set>
					<ref bean="xxxExporter"/>
				</set>
			</property>
		</bean>