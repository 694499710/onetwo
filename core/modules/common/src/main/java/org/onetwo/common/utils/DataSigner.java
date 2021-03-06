package org.onetwo.common.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.onetwo.common.exception.ErrorType;
import org.onetwo.common.exception.ServiceException;
import org.onetwo.common.log.JFishLoggerFactory;
import org.onetwo.common.md.CodeType;
import org.onetwo.common.md.Hashs;
import org.onetwo.common.md.MessageDigestHasher;
import org.onetwo.common.reflect.BeanToMapConvertor;
import org.onetwo.common.reflect.BeanToMapConvertor.BeanToMapBuilder;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

/**
 * @author wayshall
 * <br/>
 */
public interface DataSigner {
	
	String sign(String signKey, long timestamp, Object request, String... excludeProperties);
	void checkSign(SigningConfig config, SignableRequest signRequest, Object actualRequest, String... excludeProperties);
	
	public class SignableRequest {
		public static final String FIELD_SIGNKEY = "signkey";
		public static final String FIELD_TIMESTAMP = "timestamp";
		
		private String signkey;
		private Long timestamp;
		
		public String getSignkey() {
			return signkey;
		}
		public void setSignkey(String signkey) {
			this.signkey = signkey;
		}
		public Long getTimestamp() {
			return timestamp;
		}
		public void setTimestamp(Long timestamp) {
			this.timestamp = timestamp;
		}
		
		
	}
	
	public class SigningConfig {
		private int maxDelayTimeInSeconds = 60*10;
		private String signingKey;
		public int getMaxDelayTimeInSeconds() {
			return maxDelayTimeInSeconds;
		}
		public void setMaxDelayTimeInSeconds(int maxDelayTimeInSeconds) {
			this.maxDelayTimeInSeconds = maxDelayTimeInSeconds;
		}
		public String getSigningKey() {
			return signingKey;
		}
		public void setSigningKey(String signingKey) {
			this.signingKey = signingKey;
		}

	}

	
	public class DefaultDataSigner implements DataSigner {
		protected Logger logger = JFishLoggerFactory.getLogger(this.getClass());
	
	
		private BeanToMapConvertor getBeanToMapConvertor(String... excludeProperties){
			List<String> excludes = Lists.newArrayList();
			excludes.add(SignableRequest.FIELD_SIGNKEY);
			excludes.add(SignableRequest.FIELD_TIMESTAMP);
			excludes.addAll(Arrays.asList(excludeProperties));
			BeanToMapConvertor convertor = BeanToMapBuilder.newBuilder()
															.excludeProperties(excludes.toArray(new String[0]))
															.enableFieldNameAnnotation()
															.build();
			return convertor;
		}
		protected String convertToSourceString(String signKey, long timestamp, Object request, String... excludeProperties){
			Assert.hasText(signKey, "signKey can not blank");
			Map<String, Object> requestMap = getBeanToMapConvertor(excludeProperties).toFlatMap(request);
			final String paramString = ParamUtils.comparableKeyMapToParamString(requestMap);
			String sourceString = signKey+paramString+timestamp;
			if(logger.isDebugEnabled()){
				logger.debug("param string: {}", paramString);
				logger.debug("source string: {}", sourceString);
			}
			return sourceString;
		}
		
		public String sign(String signKey, long timestamp, Object request, String... excludeProperties){
			String sourceString = convertToSourceString(signKey, timestamp, request, excludeProperties);
			MessageDigestHasher hasher = Hashs.sha1(false, CodeType.HEX);
			String hashString = hasher.hash(sourceString);
			if(logger.isDebugEnabled()){
				logger.debug("source string: {}", sourceString);
				logger.debug("hash string: {}", hashString);
			}
			return hashString;
		}
		
		public void checkSign(SigningConfig config, SignableRequest signRequest, Object actualRequest, String... excludeProperties){
			if(signRequest.getTimestamp()==null){
				throw new ServiceException(SignErrors.TIMESTAMP_ERR);
			}
			if(StringUtils.isBlank(signRequest.getSignkey())){
				throw new ServiceException(SignErrors.SIGNKEY_ERR);
			}
			if(config==null){
				throw new ServiceException(SignErrors.CONFIG_ERR);
			}
			DateTime requestTime = new DateTime(signRequest.getTimestamp());
			//如果请求时间+延迟时间后少于当前时间，则可能是攻击
			if(requestTime.plusSeconds(config.getMaxDelayTimeInSeconds()).isBefore(DateTime.now())){
				throw new ServiceException(SignErrors.INVALID_INVOKE);
			}
			
			Object request = actualRequest==null?signRequest:actualRequest;
			String sourceString = convertToSourceString(config.getSigningKey(), signRequest.getTimestamp(), request, excludeProperties);
			MessageDigestHasher hasher = Hashs.sha1(false, CodeType.HEX);
			String hashString = hasher.hash(sourceString);
			if(logger.isDebugEnabled()){
				logger.debug("hash string: {}", hashString);
			}
			
			if(!hasher.checkHash(sourceString, signRequest.getSignkey())){
				throw new ServiceException(SignErrors.INVALID_SIGNKEY);
			}
		}
	}
	

	public enum SignErrors implements ErrorType {

		TIMESTAMP_ERR("时间戳不能为空"),
		SIGNKEY_ERR("签名不能为空"),
		CONFIG_ERR("配置错误"),
		INVALID_INVOKE("非法调用"),
		INVALID_SIGNKEY("非法签名");

		final private String message;

		private SignErrors(String message) {
			this.message = message;
		}

		@Override
		public String getErrorCode() {
			return name();
		}

		public String getErrorMessage() {
			return message;
		}

		@Override
		public Integer getStatusCode() {
			return 200;
		}

	}
	
}
