package org.onetwo.ext.ons;

import java.util.Map;
import java.util.Properties;

import lombok.Data;

import org.onetwo.common.utils.LangOps;
import org.onetwo.ext.alimq.JsonMessageDeserializer;
import org.onetwo.ext.alimq.JsonMessageSerializer;
import org.onetwo.ext.alimq.MessageDeserializer;
import org.onetwo.ext.alimq.MessageSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.google.common.collect.Maps;

/**
 * @author wayshall
 * <br/>
 */
@Data
@ConfigurationProperties("jfish.ons")
public class ONSProperties {

	public static final String TRANSACTIONAL_ENABLED_KEY = "jfish.ons.transactional.enabled";
//	public static final String TRANSACTIONAL_TASK_CRON_KEY = "jfish.ons.transactional.task.cron";
	public static final String TRANSACTIONAL_SEND_TASK_ENABLED_KEY = "jfish.ons.transactional.sendTask.enabled";
	public static final String TRANSACTIONAL_SEND_TASK_FIXED_RATE_STRING_KEY = "jfish.ons.transactional.sendTask.fixedRateString";

	//TODO
	public static final String TRANSACTIONAL_DELETE_TASK_ENABLED_KEY = "jfish.ons.transactional.deleteTask.enabled";
	public static final String TRANSACTIONAL_DELETE_TASK_CRON_KEY = "jfish.ons.transactional.deleteTask.cron";

	MqServerTypes serverType = MqServerTypes.ONS;
	
	String accessKey;
	String secretKey;
	String onsAddr;
	String namesrvAddr;
	MessageSerializerType serializer =  MessageSerializerType.JSON;
	Properties commons = new Properties();
	Map<String, Properties> producers = Maps.newHashMap();
	Map<String, Properties> consumers = Maps.newHashMap();
	
	TransactionalProps transactional = new TransactionalProps();

	public Map<String, Properties> getProducers() {
		return producers;
	}

	public Map<String, Properties> getConsumers() {
		return consumers;
	}

	public Properties baseProperties(){
		Properties baseConfig = new Properties();
		baseConfig.putAll(commons);
		if(MqServerTypes.ROCKETMQ==serverType){
			//自动填写默认值，避免ons客户端抛错
			this.accessKey = "<rocketmq don't need accessKey>";
			this.secretKey = "<rocketmq don't need secretKey>";
			Assert.hasText(namesrvAddr, "namesrvAddr must have text");
			baseConfig.setProperty(PropertyKeyConst.NAMESRV_ADDR, namesrvAddr);
		}
		Assert.hasText(onsAddr, "onsAddr must have text");
		baseConfig.setProperty(PropertyKeyConst.AccessKey, accessKey);
		baseConfig.setProperty(PropertyKeyConst.SecretKey, secretKey);
		baseConfig.setProperty(PropertyKeyConst.ONSAddr, onsAddr);
		return baseConfig;
	}
	
	@Data
	static public class TransactionalProps {
		SendMode sendMode = SendMode.SYNC;
		SendTaskProps sendTask = new SendTaskProps();
	}
	@Data
	public static class SendTaskProps {
		TaskLocks lock = TaskLocks.DB;
		String deleteBeforeAt;
		int deleteCountPerTask = 300;
		
		public long getDeleteBeforeAtInSeconds(){
			return LangOps.timeToSeconds(deleteBeforeAt, 60);
		}
	}
	public static enum SendMode {
		SYNC,
		ASYNC
	}
	public static enum TaskLocks {
		DB,
		REDIS
	}
	public static enum MessageSerializerType {
		JDK(MessageSerializer.DEFAULT, MessageDeserializer.DEFAULT),
		JSON(JsonMessageSerializer.INSTANCE, JsonMessageDeserializer.INSTANCE),
		CHECKED_JSON(JsonMessageSerializer.CHECKED_INSTANCE, JsonMessageDeserializer.INSTANCE);

		final private MessageSerializer serializer;
		final private MessageDeserializer deserializer;
		private MessageSerializerType(MessageSerializer serializer,
				MessageDeserializer deserializer) {
			this.serializer = serializer;
			this.deserializer = deserializer;
		}
		public MessageSerializer getSerializer() {
			return serializer;
		}
		public MessageDeserializer getDeserializer() {
			return deserializer;
		}
		
	}

	public static enum MqServerTypes {
		ONS,
		ROCKETMQ
	}
}
