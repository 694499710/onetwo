package org.onetwo.boot.module.oauth2;

import org.onetwo.boot.core.json.ObjectMapperProvider;
import org.onetwo.boot.module.oauth2.OAuth2ExceptionDataResultJsonSerializer.OAuth2ExceptionMixin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 配置定制的OAuth2ExceptionRenderer，返回框架约定的的格式
 * @author wayshall
 * <br/>
 */
@Configuration
public class OAuth2ErrorConfiguration {
	
	@Bean
	public ObjectMapperProvider objectMapperProvider(){
		return ()->{
			ObjectMapper jsonMapper = ObjectMapperProvider.DEFAULT.createObjectMapper();
			jsonMapper.addMixIn(OAuth2Exception.class, OAuth2ExceptionMixin.class);
			return jsonMapper;
		};
	}
	@Bean
	public DataResultOAuth2ExceptionRenderer oauth2ExceptionRenderer(){
		return new DataResultOAuth2ExceptionRenderer(objectMapperProvider());
	}
	
	/*@Bean
	public OAuth2AuthenticationEntryPoint oauth2AuthenticationEntryPoint(){
		OAuth2AuthenticationEntryPoint ep = new OAuth2AuthenticationEntryPoint();
		ep.setExceptionRenderer(oauth2ExceptionRenderer());
		return ep;
	}
	
	@Bean
	public OAuth2AccessDeniedHandler oauth2AccessDeniedHandler(){
		OAuth2AccessDeniedHandler dh = new OAuth2AccessDeniedHandler();
		dh.setExceptionRenderer(oauth2ExceptionRenderer());
		return dh;
	}
	
	@Bean
	public DataResultOAuth2ExceptionRenderer oauth2ExceptionRenderer(){
		return new DataResultOAuth2ExceptionRenderer();
	}*/

}
