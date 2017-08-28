package org.onetwo.boot.core.embedded;

import org.onetwo.common.file.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.MultipartProperties;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

/**
 * 
设置server最大上传
根据spring.http.multipart自动设置内嵌tomcat的最后postsize
也可以自行配置server.maxHttpPostSize
server: 
	maxHttpPostSize: 10*1024*1024
serverProperties#maxHttpPostSize
ServerProperties#Tomcat#customizeMaxHttpPostSize

not work:
spring.http.multipart -> MultipartProperties

 * @author wayshall
 * <br/>
 */
public class BootServletContainerCustomizer implements EmbeddedServletContainerCustomizer {
	
	@Autowired
	private MultipartProperties multipartProperties;

	@Override
	public void customize(ConfigurableEmbeddedServletContainer container) {
		if (container instanceof TomcatEmbeddedServletContainerFactory) {
            TomcatEmbeddedServletContainerFactory tomcat = (TomcatEmbeddedServletContainerFactory) container;
            tomcat.addConnectorCustomizers(
                (connector) -> {
                	//default is 2 mb
                    connector.setMaxPostSize(10*1024*1024); // 10 MB
                    connector.setMaxPostSize(FileUtils.parseSize(multipartProperties.getMaxFileSize()));
                }
            );
        }
	}

	
	

}
