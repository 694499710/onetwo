package org.onetwo.ext.ons.producer;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.onetwo.boot.mq.InterceptableMessageSender;
import org.onetwo.boot.mq.MQUtils;
import org.onetwo.boot.mq.SendMessageFlags;
import org.onetwo.boot.mq.interceptor.SendMessageInterceptor;
import org.onetwo.boot.mq.interceptor.SendMessageInterceptor.InterceptorPredicate;
import org.onetwo.boot.mq.interceptor.SendMessageInterceptorChain;
import org.onetwo.common.exception.BaseException;
import org.onetwo.common.spring.SpringUtils;
import org.onetwo.ext.alimq.MessageSerializer;
import org.onetwo.ext.alimq.MessageSerializer.MessageDelegate;
import org.onetwo.ext.alimq.OnsMessage;
import org.onetwo.ext.alimq.OnsMessage.TracableMessage;
import org.onetwo.ext.alimq.SimpleMessage;
import org.onetwo.ext.ons.ONSProperties;
import org.onetwo.ext.ons.ONSUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.bean.ProducerBean;
import com.aliyun.openservices.ons.api.exception.ONSClientException;

/**
 * @author wayshall
 * <br/>
 */
public class ONSProducerServiceImpl extends ProducerBean implements InitializingBean, DisposableBean, ProducerService {

//	private final Logger logger = JFishLoggerFactory.getLogger(this.getClass());
	
	private MessageSerializer messageSerializer;

	private ONSProperties onsProperties;
	private String producerId;
	
//	private ONSProducerListenerComposite producerListenerComposite;
	@Autowired
	private List<SendMessageInterceptor> sendMessageInterceptors;
	private InterceptableMessageSender<SendResult> interceptableMessageSender;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	public ONSProducerServiceImpl() {
	}

	@Autowired
	public void setOnsProperties(ONSProperties onsProperties) {
		this.onsProperties = onsProperties;
	}
	
	public void setProducerId(String producerId) {
		this.producerId = producerId;
	}

	@Autowired
	public void setMessageSerializer(MessageSerializer messageSerializer) {
		this.messageSerializer = messageSerializer;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(producerId, "produerId must has text");
		Assert.notNull(onsProperties, "onsProperties can not be null");
		Assert.notNull(messageSerializer, "messageSerializer can not be null");
		

		this.interceptableMessageSender = new InterceptableMessageSender<SendResult>(sendMessageInterceptors);
		
		Properties producerProperties = onsProperties.baseProperties();
		Properties customProps = onsProperties.getProducers().get(producerId);
		if(customProps!=null){
			producerProperties.putAll(customProps);
		}
		producerProperties.setProperty(PropertyKeyConst.ProducerId, producerId);
		
		this.setProperties(producerProperties);
		this.start();
	}
	
	@Override
	public void destroy() throws Exception {
		this.shutdown();
	}
	
	@Override
	public void sendMessage(String topic, String tags, Object body){
		SimpleMessage message = SimpleMessage.builder()
											.topic(topic)
											.tags(tags)
											.body(body)
											.build();
		this.sendMessage(message);
	}
	
	/*public SendResult sendBytesMessage(String topic, String tags, byte[] body){
		SendResult result =  sendBytesMessage(topic, tags, body, errorHandler);
		return result;
	}*/
	
	
	@Override
	public SendResult sendMessage(OnsMessage onsMessage){
		return sendMessage(onsMessage, SendMessageFlags.Default);
	}
	

	@Override
	public SendResult send(Serializable onsMessage, InterceptorPredicate interceptorPredicate) {
		if(onsMessage instanceof Message){
			return sendRawMessage((Message)onsMessage, interceptorPredicate);
		}else if(onsMessage instanceof OnsMessage){
			return sendMessage((OnsMessage)onsMessage, interceptorPredicate);
		}else{
			throw new IllegalArgumentException("error message type: " + onsMessage.getClass());
		}
	}

	@Override
	public SendResult sendMessage(OnsMessage onsMessage, InterceptorPredicate interceptorPredicate){
		Object body = onsMessage.getBody();
		if(body instanceof Message){
			return sendRawMessage((Message)body, interceptorPredicate);
		}
		
		Message message = onsMessage.toMessage();
		
		String topic = resolvePlaceholders(message.getTopic());
		message.setTopic(topic);
		String tag = resolvePlaceholders(message.getTag());
		message.setTag(tag);
		
		if(needSerialize(body)){
			message.setBody(this.messageSerializer.serialize(onsMessage.getBody(), new MessageDelegate(message)));
			if(StringUtils.isBlank(message.getKey()) && onsMessage instanceof TracableMessage) {
				//自动生成key
				//如果是延迟消息，用实际延迟发送的时间替换已存在的发生时间
				TracableMessage tracableMessage = (TracableMessage) onsMessage;
				if(message.getStartDeliverTime()>0) {
					tracableMessage.setOccurOn(new Date(message.getStartDeliverTime()));
				}
				String key = ONSUtils.toKey(topic, tag, tracableMessage);
				message.setKey(key);
			}
		}else{
			message.setBody((byte[])body);
		}
		
		return sendRawMessage(message, interceptorPredicate);
	}
	
	protected String resolvePlaceholders(String value){
		return SpringUtils.resolvePlaceholders(applicationContext, value);
	}
	
	protected boolean needSerialize(Object body){
		if(body==null){
			return false;
		}
		return !byte[].class.isInstance(body);
	}
	
	
	protected SendResult sendRawMessage(Message message, final InterceptorPredicate interPredicate){
		final InterceptorPredicate interceptorPredicate = interPredicate==null?SendMessageFlags.Default:interPredicate;
		
		/*List<SendMessageInterceptor> messageInterceptors = Lists.newArrayList(sendMessageInterceptors);
		List<SendMessageInterceptor> increasingInters = interceptorPredicate.getIncreasingInterceptors();
		if(!increasingInters.isEmpty()){
			messageInterceptors.addAll(interceptorPredicate.getIncreasingInterceptors());
			AnnotationAwareOrderComparator.sort(messageInterceptors);
		}
		SendMessageInterceptorChain chain = new SendMessageInterceptorChain(messageInterceptors, 
																			()->this.send(message), 
																			interceptorPredicate);
		
		ONSSendMessageContext ctx = ONSSendMessageContext.builder()
													.message(message)
													.source(this)
													.producer(this)
													.chain(chain)
													.debug(true)
													.threadId(Thread.currentThread().getId())
													.build();
		chain.setSendMessageContext(ctx);
		chain.setDebug(ctx.isDebug());
		
		return (SendResult)chain.invoke();*/
		return interceptableMessageSender.sendIntercetableMessage(interPredicate, messageInterceptors->{
			SendMessageInterceptorChain chain = new SendMessageInterceptorChain(messageInterceptors, 
					interceptorPredicate,
					()->this.doSendRawMessage(message));
			
			ONSSendMessageContext ctx = ONSSendMessageContext.builder()
															.message(message)
															.source(this)
															.producer(this)
															.chain(chain)
															.debug(true)
															.threadId(Thread.currentThread().getId())
															.build();
			chain.setSendMessageContext(ctx);
			chain.setDebug(ctx.isDebug());
			
			Object res = chain.invoke();
			if(MQUtils.isSuspendResult(res)){
				return ONSUtils.ONS_SUSPEND;
			}
			return (SendResult)res;
		});
	}
	

	protected SendResult doSendRawMessage(Message message){
		try {
			return send(message);
		} catch (ONSClientException e) {
			handleException(e, message);
		}catch (Throwable e) {
			handleException(e, message);
		}
		return null;
	}

	protected void handleException(Throwable e, Message message){
		final Logger logger = ONSUtils.getONSLogger();
		if(logger.isErrorEnabled()){
			logger.error("send message topic: {}, tags: {}, key: {}, msgId: {}", message.getTopic(), message.getTag(), message.getKey(), message.getMsgID());
		}
		
		if(e instanceof ONSClientException){
			throw (ONSClientException)e;
		}else{
			throw new BaseException("发送消息失败", e);
		}
	}

	
	@Override
	public <T> T getRawProducer(Class<T> targetClass) {
		return targetClass.cast(this);
	}

	@Override
	public boolean isTransactional() {
		return false;
	}
	
	
}
