package org.onetwo.boot.core.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.onetwo.common.date.Dates;
import org.onetwo.common.exception.BaseException;
import org.onetwo.common.exception.ServiceException;
import org.onetwo.common.reflect.BeanToMapConvertor;
import org.onetwo.common.reflect.BeanToMapConvertor.BeanToMapBuilder;
import org.onetwo.common.utils.StringUtils;
import org.onetwo.common.web.userdetails.UserDetail;
import org.onetwo.ext.security.jwt.JwtSecurityUtils;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author wayshall
 * <br/>
 */
public class SimpleJwtTokenService implements JwtTokenService, InitializingBean {

	private final BeanToMapConvertor beanToMap = BeanToMapBuilder.newBuilder()
																			.enableFieldNameAnnotation()
																			.build();
	private String propertyKey = JwtUtils.PROPERTY_KEY;
	private JwtConfig jwtConfig;
	/*private BeanToMapConvertor beanToMap = BeanToMapBuilder.newBuilder()
													.excludeProperties("userName", "userId")
													.build();*/
	
	protected Long getExpirationInSeconds(){
		return jwtConfig.getExpirationInSeconds();
	}
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(StringUtils.isBlank(jwtConfig.getSigningKey())){
			throw new BaseException("jwt signingKey not found!");
		}
	}
	

	@Override
	public JwtTokenInfo generateToken(UserDetail userDetail){
		Map<String, Object> props = beanToMap.toFlatMap(userDetail);
		Long userId = (Long)props.remove(JwtUtils.CLAIM_USER_ID);
		String userName = (String)props.remove(JwtUtils.CLAIM_USER_NAME);
		JwtUserDetail jwtDetail = new JwtUserDetail(userId, userName);
		jwtDetail.setProperties(props);
		return generateToken(jwtDetail);
	}


	@Override
	public JwtTokenInfo generateToken(JwtUserDetail userDetail){
		if(userDetail==null){
			return null;
		}
		LocalDateTime issuteAt = LocalDateTime.now();
		Date expirationDate = Dates.toDate(issuteAt.plusSeconds(getExpirationInSeconds().intValue()));
		JwtBuilder builder = Jwts.builder()
							.setSubject(userDetail.getUserName())
							.setIssuer(jwtConfig.getIssuer())
							.setAudience(jwtConfig.getAudience())
//							.setId(jti)
							.claim(JwtSecurityUtils.CLAIM_USER_ID, userDetail.getUserId())
//							.claim(JwtUtils.CLAIM_AUTHORITIES, getAuthorities(userDetail))
							.setIssuedAt(Dates.toDate(issuteAt))
							.setExpiration(expirationDate)
							.signWith(SignatureAlgorithm.HS512, jwtConfig.getSigningKey());
		
		if(userDetail.getProperties()!=null){
			userDetail.getProperties().forEach((k,v)->builder.claim(getPropertyKey(k), v));
		}
		
		String token = builder.compact();
		
		return JwtTokenInfo.builder()
							.token(token)
							.build();
	}

	private String getPropertyKey(String prop){
		return propertyKey+prop;
	}
	private boolean isPropertyKey(Object key){
		return key.toString().startsWith(propertyKey);
	}
	private String getProperty(String key){
		return key.substring(propertyKey.length());
	}
	
	/*protected String getAuthorities(UserDetail userDetail){
		return null;
	}*/
	
	@Override
	public <T extends UserDetail> T createUserDetail(String token, Class<T> parameterType) {
		JwtUserDetail jwtUserDetail = this.createUserDetail(token);
		T userDetail = JwtUtils.createUserDetail(jwtUserDetail, parameterType);
		return userDetail;
	}
	
	@Override
	public JwtUserDetail createUserDetail(String token) {
		Claims claims = createClaimsFromToken(token);
		LocalDateTime expireation = Dates.toLocalDateTime(claims.getExpiration());
		if(expireation.isBefore(LocalDateTime.now())){
			throw new ServiceException(JwtErrors.CM_SESSION_EXPIREATION);
		}
		
		Map<String, Object> properties = claims.entrySet()
												.stream()
												.filter(entry->isPropertyKey(entry.getKey()))
												.collect(Collectors.toMap(entry->getProperty(entry.getKey()), entry->entry.getValue()));
		Long userId = Long.parseLong(claims.get(JwtSecurityUtils.CLAIM_USER_ID).toString());
		JwtUserDetail userDetail = buildJwtUserDetail(userId, claims.getSubject(), properties);
		return userDetail;
	}

	protected JwtUserDetail buildJwtUserDetail(Long userId, String userName, Map<String, Object> properties){
		JwtUserDetail userDetail = new JwtUserDetail(userId, userName);
		userDetail.setProperties(properties);
		return userDetail;
	}
	
	final protected Claims createClaimsFromToken(String token) {
		try {
			DefaultClaims claims = (DefaultClaims)Jwts.parser()
										.setSigningKey(jwtConfig.getSigningKey())
										.parse(token)
										.getBody();
			return claims;
		} catch (Exception e) {
			throw new ServiceException(JwtErrors.CM_ERROR_TOKEN, e);
		}
	}

	public void setJwtConfig(JwtConfig jwtConfig) {
		this.jwtConfig = jwtConfig;
	}
	
}
