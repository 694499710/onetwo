package org.onetwo.common.web.server.tomcat;

import java.util.Properties;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.onetwo.common.exception.BaseException;
import org.onetwo.common.log.JFishLoggerFactory;
import org.onetwo.common.spring.plugin.PluginInfo;
import org.onetwo.common.spring.plugin.SpringContextPluginManager;
import org.onetwo.common.spring.utils.ResourceUtils;
import org.onetwo.common.utils.ReflectUtils;
import org.onetwo.common.utils.StringUtils;
import org.onetwo.common.utils.list.JFishList;
import org.onetwo.common.utils.list.NoIndexIt;
import org.onetwo.common.utils.propconf.JFishProperties;
import org.onetwo.common.utils.propconf.PropUtils;
import org.onetwo.common.web.server.ServerConfig;
import org.onetwo.common.web.server.WebappConfig;
import org.onetwo.common.web.server.events.WebappAddEvent;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;

import com.google.common.eventbus.EventBus;

public class TomcatServer {

	public static TomcatServer create(){
		ServerConfig config = null;
		try {
			config = TomcatConfig.getInstance().asServerConfig();
		} catch (Throwable e) {
			System.out.println(e.getMessage());
			config = new ServerConfig();
		}
		return create(config);
	}

	public static TomcatServer create(ServerConfig webConfig){
		TomcatServer tomcat = new TomcatServer(webConfig);
		tomcat.initialize();
		return tomcat;
	}

	public static final String PLUGIN_WEBAPP_BASE_PATH = "classpath:META-INF";
	
	private ServerConfig webConfig;
	private Tomcat tomcat;
	private Logger logger = JFishLoggerFactory.logger(this.getClass());
	private EventBus eventBus = new EventBus();

	private TomcatServer(ServerConfig webConfig) {
		this.webConfig = webConfig;
	}
	
	public void scanPluginWebapps(final Tomcat tomcat){
		JFishList<Resource> pluginFiles =ResourceUtils.scanResources(SpringContextPluginManager.PLUGIN_PATH);
		

		pluginFiles.each(new NoIndexIt<Resource>(){

			@Override
			public void doIt(Resource pluginFile) throws Exception {
				Properties vconfig = new Properties();
				PropUtils.loadProperties(pluginFile.getInputStream(), vconfig);
				JFishProperties prop = new JFishProperties(true, vconfig);
				PluginInfo plugin = buildPluginInfo(prop);
				if(!plugin.isWebappPlugin()){
					return;
				}
				String webappPath = PLUGIN_WEBAPP_BASE_PATH + plugin.getContextPath();
				Resource res = ResourceUtils.getResource(webappPath);
				logger.info("found web plugin["+plugin+"] : {}", res.getURI().getPath() );
				tomcat.addWebapp(plugin.getContextPath(), res.getURI().getPath());
				logger.info("load web plugin : {} ", res.getURI().getPath());
				
				String listener = plugin.getWebappPluginServerListener();
				if(StringUtils.isNotBlank(listener)){
					Object lisnter = ReflectUtils.newInstance(listener);
					eventBus.register(lisnter);
				}
			}
			
		});

	}
	protected PluginInfo buildPluginInfo(JFishProperties prop){
		PluginInfo info = new PluginInfo();
		info.init(prop);
		return info;
	}

	public void initialize() {
		try {
			
			this.tomcat = new JFishTomcat();
			int port = webConfig.getPort();
			tomcat.setPort(port);
//			tomcat.setBaseDir(webConfig.getServerBaseDir());
			tomcat.setBaseDir(webConfig.getServerBaseDir());
			tomcat.getHost().setAppBase(webConfig.getWebappDir());
			Connector connector = tomcat.getConnector();
			connector.setURIEncoding("UTF-8");
			connector.setRedirectPort(webConfig.getRedirectPort());
			
			ProtocolHandler protocol = connector.getProtocolHandler();
			if(protocol instanceof AbstractHttp11Protocol){
				/*****
				 * <Connector port="8080" protocol="HTTP/1.1" 
					   connectionTimeout="20000" 
   						redirectPort="8181" compression="500" 
  						compressableMimeType="text/html,text/xml,text/plain,application/octet-stream" />
				 */
				AbstractHttp11Protocol hp = (AbstractHttp11Protocol) protocol;
				hp.setCompression("on");
				hp.setCompressableMimeTypes("text/html,text/xml,text/plain,application/octet-stream");
			}
			
			
			StandardServer server = (StandardServer) tomcat.getServer();
			AprLifecycleListener listener = new AprLifecycleListener();
			server.addLifecycleListener(listener);

			/*tomcat.addUser("adminuser", "adminuser");
			tomcat.addRole("adminuser", "admin");
			tomcat.addRole("adminuser", "admin");*/
		} catch (Exception e) {
			throw new BaseException("web server initialize error , check it. " + e.getMessage(), e);
		}
	}

	public void start() {
		try {
			/*this.tomcat = new Tomcat();
			int port = webConfig.getPort();
			tomcat.setPort(port);
//			tomcat.setBaseDir(webConfig.getServerBaseDir());
			tomcat.setBaseDir(webConfig.getServerBaseDir());
			tomcat.getHost().setAppBase(webConfig.getWebappDir());
			Connector connector = tomcat.getConnector();
			connector.setURIEncoding("UTF-8");
			connector.setRedirectPort(webConfig.getRedirectPort());
			
			ProtocolHandler protocol = connector.getProtocolHandler();
			if(protocol instanceof AbstractHttp11Protocol){
				*//*****
				 * <Connector port="8080" protocol="HTTP/1.1" 
					   connectionTimeout="20000" 
   						redirectPort="8181" compression="500" 
  						compressableMimeType="text/html,text/xml,text/plain,application/octet-stream" />
				 *//*
				AbstractHttp11Protocol hp = (AbstractHttp11Protocol) protocol;
				hp.setCompression("on");
				hp.setCompressableMimeTypes("text/html,text/xml,text/plain,application/octet-stream");
			}
			
			
			StandardServer server = (StandardServer) tomcat.getServer();
			AprLifecycleListener listener = new AprLifecycleListener();
			server.addLifecycleListener(listener);

			addWebapps();
			tomcat.addUser("adminuser", "adminuser");
			tomcat.addRole("adminuser", "admin");
			tomcat.addRole("adminuser", "admin");*/

			addWebapps();
			tomcat.start();
			printConnectors();
			tomcat.getServer().await();
		} catch (Exception e) {
			throw new BaseException("web server start error , check it. " + e.getMessage(), e);
		}
	}
	
	protected void addWebapps() throws ServletException{
		tomcat.addWebapp(webConfig.getContextPath(), webConfig.getWebappDir());
		for(WebappConfig webapp : webConfig.getWebapps()){
			logger.info("add webapp : {} ", webapp);
			tomcat.addWebapp(webapp.getContextPath(), webapp.getWebappDir());
		}

		this.scanPluginWebapps(tomcat);
		eventBus.post(new WebappAddEvent(tomcat));
	}
	
	protected void printConnectors(){
		Connector[] cons = tomcat.getService().findConnectors();
		for(Connector con : cons){
			System.out.println("Connector: " + con);
		}
	}
	
	protected void addSSLConnector(){
		Connector connector = new Connector("HTTP/1.1");
        // connector = new Connector("org.apache.coyote.http11.Http11Protocol"); 
        connector.setPort(8443);
	}
}
