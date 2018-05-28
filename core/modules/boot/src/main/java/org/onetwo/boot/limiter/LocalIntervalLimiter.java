package org.onetwo.boot.limiter;

import java.util.Optional;

import org.onetwo.boot.limiter.InvokeContext.InvokeType;
import org.onetwo.boot.limiter.InvokeLimiter.BaseInvokeLimiter;
import org.onetwo.common.exception.BaseException;
import org.onetwo.common.utils.LangOps;

/**
 * 即自定义时间的tps
 * @author wayshall
 * <br/>
 */
public class LocalIntervalLimiter extends BaseInvokeLimiter {
	private String interval;
	private int intervalInMillis = 1000*60;
	private LimiterState limiterState;
	
	@Override
	public void init() {
		super.init();
		this.setInvokeType(InvokeType.BEFORE);
		this.intervalInMillis = (int)LangOps.timeToMills(interval, intervalInMillis);
		
		this.limiterState = new DefaultLimiterState();
	}

	@Override
	public void consume(InvokeContext invokeContext) {
		if(invokeContext.getInvokeType()==InvokeType.BEFORE){
			invokeContext.setAttribute(START_KEY, System.currentTimeMillis());
		}else{
			Optional<Long> startMillisOpt = invokeContext.getAttributeOpt(START_KEY);
			if(!startMillisOpt.isPresent()){
				throw new BaseException("start time not found!");
			}
			Long costTime = System.currentTimeMillis() - startMillisOpt.get();
		}
	}
	
	protected DefaultLimiterState getCurrentValidLimiterState(){
		
	}
	
}