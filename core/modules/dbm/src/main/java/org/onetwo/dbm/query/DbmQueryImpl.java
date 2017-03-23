package org.onetwo.dbm.query;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.onetwo.common.db.DbmQueryValue;
import org.onetwo.common.log.JFishLoggerFactory;
import org.onetwo.common.profiling.UtilTimerStack;
import org.onetwo.common.utils.Assert;
import org.onetwo.dbm.core.spi.DbmSessionImplementor;
import org.onetwo.dbm.dialet.DBDialect;
import org.slf4j.Logger;
import org.springframework.jdbc.core.RowMapper;

@SuppressWarnings("unchecked")
public class DbmQueryImpl implements DbmQuery {
	private static final int INVALID_VALUE = -1;
	private static final int INVALID_VALUE_MAX_RESULTS = 0;

	private static final String FIRST_RESULT_NAME = "DbmQueryFirstResult";
	private static final String MAX_RESULT_NAME = "DbmQueryMaxResult";
	
	
	protected final Logger logger = JFishLoggerFactory.getLogger(this.getClass());
	
	private DbmSessionImplementor dbmSessionImplementor;
	private DBDialect dbDialect;
	private String sqlString;
//	private Map<String, Object> parameters = new LinkedHashMap<String, Object>();
	private DbmQueryValue parameters = null;
	
	private Class<?> resultClass;
	
	private int firstResult = 0;
	private int maxResults = INVALID_VALUE_MAX_RESULTS;
	
	private RowMapper<?> rowMapper;
//	private QType qtype;

	public DbmQueryImpl(DbmSessionImplementor session, String sqlString, Class<?> resultClass) {
		super();
		this.dbmSessionImplementor = session;
		this.dbDialect = this.dbmSessionImplementor.getDialect();
		this.sqlString = sqlString;
		this.resultClass = resultClass;
		this.parameters = DbmQueryValue.create(null);
	}
	
	/**********
	 * field = :1 and field = :2
	 */
	@Override
	public DbmQuery setParameter(Integer index, Object value){
//		this.checkQueryType(PlaceHolder.POSITION);
//		createParameterIfNull();
		this.parameters.setValue(index, value);
		return this;
	}
	
	/*********
	 * field = :name1 and field = :name2
	 */
	@Override
	public DbmQuery setParameter(String name, Object value){
//		createParameterIfNull();
		this.parameters.setValue(name, value);
		return this;
	}
	
	public DbmQuery setParameters(Map<String, Object> params){
//		createParameterIfNull();
		this.parameters.setValue(params);
		return this;
	}
	
	public DbmQuery setParameters(List<?> params){
		Assert.notNull(params);
		for(int index=0; index<params.size(); index++){
			setParameter(index, params.get(index));
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see org.onetwo.common.fish.JFishQuery#getSingleResult()
	 */
	@Override
	public <T> T getSingleResult(){
		/*String fname = this.getClass().getSimpleName()+".getSingleResult";
		UtilTimerStack.push(fname);*/
		
		String sql = getSqlString();
		DbmQueryValue params = this.getActualParameters(sql);
		T result = null;
		
		if(rowMapper!=null){
			result = (T)this.dbmSessionImplementor.findUnique(params, rowMapper);
		}else{
			result = this.dbmSessionImplementor.findUnique(params);
		}
		
//		UtilTimerStack.pop(fname);
		return result;
	}

	/****
	 * 返回结果集，返回值不为null
	 */
	@Override
	public <T> List<T> getResultList(){
		return (List<T>)getResultList(rowMapper);
	}
	
	public <T> List<T> getResultList(RowMapper<T> rowMapper){
		String fname = this.getClass().getSimpleName()+".getResultList";
		UtilTimerStack.push(fname);
		
		List<T> result = null;
		String sql = getSqlString();
		DbmQueryValue params = this.getActualParameters(sql);
		
		if(rowMapper!=null){
			result = (List<T>)this.dbmSessionImplementor.findList(params, rowMapper);
		}else{
			result = this.dbmSessionImplementor.findList(params);
		}
		
		UtilTimerStack.pop(fname);
		return result;
	}
	

	public String getSqlString() {
		String sql = sqlString;
		if(isLimitedQuery()){
			sql = dbDialect.getLimitStringWithNamed(sqlString, FIRST_RESULT_NAME, MAX_RESULT_NAME);
		}
		if(UtilTimerStack.isActive()){
			this.logger.info("sql:"+sql);
		}
		return sql;
	}

	public DbmSessionImplementor getJFishDaoImplementor() {
		return dbmSessionImplementor;
	}

	public DbmQueryValue getActualParameters(String sql) {
		this.parameters.setResultClass(resultClass);
		this.parameters.setSql(sql);
		if(!isLimitedQuery()){
			return this.parameters;
		}
		/*if(!params.containsKey(FIRST_RESULT_NAME))
			params.put(FIRST_RESULT_NAME, this.firstResult);
		if(!params.containsKey(MAX_RESULT_NAME))
			params.put(MAX_RESULT_NAME, dbDialect.getMaxResults(this.firstResult, maxResults));*/
		
		this.dbDialect.addLimitedValue(parameters, FIRST_RESULT_NAME, firstResult, MAX_RESULT_NAME, maxResults);
		
		if(UtilTimerStack.isActive()){
			this.logger.info("params"+parameters.getValues());
		}
		
		return this.parameters;
	}

	public boolean isLimitedQuery(){
		return this.getFirstResult()>INVALID_VALUE && this.getMaxResults()>INVALID_VALUE_MAX_RESULTS;
	}

	/* (non-Javadoc)
	 * @see org.onetwo.common.fish.JFishQuery#getFirstResult()
	 */
	public int getFirstResult() {
		return firstResult;
	}

	/* (non-Javadoc)
	 * @see org.onetwo.common.fish.JFishQuery#setFirstResult(int)
	 */
	@Override
	public DbmQuery setFirstResult(int firstResult) {
		this.firstResult = firstResult;
		return this;
	}

	/* (non-Javadoc)
	 * @see org.onetwo.common.fish.JFishQuery#getMaxResult()
	 */
	public int getMaxResults() {
		return maxResults;
	}

	/* (non-Javadoc)
	 * @see org.onetwo.common.fish.JFishQuery#setMaxResult(int)
	 */
	@Override
	public DbmQuery setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	public DbmQuery setResultClass(Class<?> resultClass) {
		this.resultClass = resultClass;
		return this;
	}

	public int executeUpdate(){
		int result = 0;
		String sql = getSqlString();
		DbmQueryValue params = getActualParameters(sql);
		result = dbmSessionImplementor.executeUpdate(params);
		return result;
	}

	protected void setSqlString(String sqlString) {
		this.sqlString = sqlString;
	}

	public void setRowMapper(RowMapper<?> rowMapper) {
		this.rowMapper = rowMapper;
	}

	@Override
	public void setQueryAttributes(Map<Object, Object> params) {
		Object key;
		for(Entry<Object, Object> entry : params.entrySet()){
			key = entry.getKey();
			if(String.class.isInstance(key)){
				setParameter(key.toString(), entry.getValue());
			}else if(Integer.class.isInstance(key)){
				setParameter((Integer)key, entry.getValue());
			}
		}
	}
}
