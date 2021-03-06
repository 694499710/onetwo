package org.onetwo.boot.module.oauth2.clientdetails;

import java.util.Optional;

import org.onetwo.boot.core.web.controller.AbstractBaseController;
import org.onetwo.boot.module.oauth2.util.OAuth2Errors;
import org.onetwo.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author wayshall
 * <br/>
 */
public class AbstractClientDetailController extends AbstractBaseController {
	@Autowired(required=false)
	protected Oauth2ClientDetailManager oauth2ClientDetailManager;
	
	protected Optional<ClientDetails> getCurrentClientDetail(){
		return oauth2ClientDetailManager.getCurrentClientDetail();
	}
	protected ClientDetails requiredCurrentClientDetail(){
		Optional<ClientDetails> opt = oauth2ClientDetailManager.getCurrentClientDetail();
		return opt.orElseThrow(()->new ServiceException(OAuth2Errors.CLIENT_ACCESS_TOKEN_NOT_FOUND));
	}
	
	protected <T extends ClientDetails> Optional<T> getCurrentClientDetail(Class<T> clazz){
		return oauth2ClientDetailManager.getCurrentClientDetail();
	}
	
	protected <T extends ClientDetails> T requiredCurrentClientDetail(Class<T> clazz){
		Optional<T> opt = oauth2ClientDetailManager.getCurrentClientDetail();
		return opt.orElseThrow(()->new ServiceException(OAuth2Errors.CLIENT_ACCESS_TOKEN_NOT_FOUND));
	}
}
