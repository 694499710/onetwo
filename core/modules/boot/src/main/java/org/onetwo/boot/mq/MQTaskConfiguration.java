package org.onetwo.boot.mq;

import org.onetwo.boot.mq.MQProperties.DeleteTaskProps;
import org.onetwo.boot.mq.MQProperties.TaskLocks;
import org.onetwo.boot.mq.task.CompensationSendMessageTask;
import org.onetwo.boot.mq.task.DeleteSentMessageTask;

/**
 * @author wayshall
 * <br/>
 */
public class MQTaskConfiguration {

	private MQProperties mqProperties;
	
	public MQTaskConfiguration(MQProperties mqProperties) {
		super();
		this.mqProperties = mqProperties;
	}

	public CompensationSendMessageTask createCompensationSendMessageTask(){
		CompensationSendMessageTask task = new CompensationSendMessageTask();
		TaskLocks taskLock = mqProperties.getTransactional().getSendTask().getLock();
		if(taskLock==TaskLocks.REDIS){
			task.setUseReidsLock(true);
		}
		return task;
	}
	
	public DeleteSentMessageTask createDeleteSentMessageTask(){
		DeleteSentMessageTask task = new DeleteSentMessageTask();
		DeleteTaskProps deleteProps = mqProperties.getTransactional().getDeleteTask();
		if(deleteProps.getLock()==TaskLocks.REDIS){
			task.setUseReidsLock(true);
		}
		task.setDeleteBeforeAt(deleteProps.getDeleteBeforeAt());
		return task;
	}
}