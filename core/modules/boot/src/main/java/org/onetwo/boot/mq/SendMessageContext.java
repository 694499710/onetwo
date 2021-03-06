package org.onetwo.boot.mq;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

/**
 * @author wayshall
 * <br/>
 */
@Data
public class SendMessageContext<M extends Serializable> {
	
	final private M message;
//	final private ProducerService<M, R> source;
	final private SendMessageInterceptorChain chain;
	private SendMessageEntity messageEntity;
	final private long threadId;
	private boolean debug;
	private String key;
	

	@Builder(builderMethodName="newBuilder")
	public SendMessageContext(M message, SendMessageInterceptorChain chain,
			SendMessageEntity messageEntity, long threadId, boolean debug) {
		super();
		this.message = message;
		this.chain = chain;
		this.messageEntity = messageEntity;
		this.threadId = threadId;
		this.debug = debug;
	}
	
	
	public M getMessage(){
		return message;
	}

	public boolean isTransactional(){
		return false;
	}

}
