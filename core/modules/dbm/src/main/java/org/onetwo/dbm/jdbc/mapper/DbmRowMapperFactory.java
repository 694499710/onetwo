package org.onetwo.dbm.jdbc.mapper;

import java.util.concurrent.ExecutionException;

import org.onetwo.common.db.dquery.DynamicMethod;
import org.onetwo.common.db.dquery.NamedQueryInvokeContext;
import org.onetwo.common.reflect.ReflectUtils;
import org.onetwo.common.utils.Assert;
import org.onetwo.dbm.annotation.DbmResultMapping;
import org.onetwo.dbm.annotation.DbmRowMapper;
import org.onetwo.dbm.exception.DbmException;
import org.onetwo.dbm.jdbc.JdbcResultSetGetter;
import org.onetwo.dbm.mapping.DbmMappedEntry;
import org.onetwo.dbm.mapping.MappedEntryManager;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class DbmRowMapperFactory extends JdbcDaoRowMapperFactory {

	private LoadingCache<Class<?>, RowMapper<?>> beanPropertyRowMapperCache = CacheBuilder.newBuilder()
																						.build(new CacheLoader<Class<?>, RowMapper<?>>(){

																							@Override
																							public RowMapper<?> load(Class<?> type)
																									throws Exception {
																								return getBeanPropertyRowMapper0(type);
																							}
																							
																						});
	private MappedEntryManager mappedEntryManager;
	private JdbcResultSetGetter jdbcResultSetGetter;
	
	public DbmRowMapperFactory(MappedEntryManager mappedEntryManager, JdbcResultSetGetter jdbcResultSetGetter) {
		super();
		this.mappedEntryManager = mappedEntryManager;
		this.jdbcResultSetGetter = jdbcResultSetGetter;
	}

	public MappedEntryManager getMappedEntryManager() {
		return mappedEntryManager;
	}

	public void setMappedEntryManager(MappedEntryManager mappedEntryManager) {
		this.mappedEntryManager = mappedEntryManager;
	}

	protected RowMapper<?> getBeanPropertyRowMapper(Class<?> type) {
		try {
			return beanPropertyRowMapperCache.get(type);
		} catch (ExecutionException e) {
			throw new DbmException("no BeanPropertyRowMapper found for type:"+type);
		}
	}
	protected RowMapper<?> getBeanPropertyRowMapper0(Class<?> type) {
		RowMapper<?> rowMapper = null;
		if(getMappedEntryManager().isSupportedMappedEntry(type)){
			DbmMappedEntry entry = this.getMappedEntryManager().getEntry(type);
			rowMapper = new EntryRowMapper<>(entry, this.jdbcResultSetGetter);
			return rowMapper;
		}else if(type.getAnnotation(DbmRowMapper.class)!=null){
			DbmRowMapper dbmRowMapper = type.getAnnotation(DbmRowMapper.class);
			if(dbmRowMapper.value()==Void.class){
				return new DbmBeanPropertyRowMapper<>(this.jdbcResultSetGetter,  type);
			}else{
				Assert.isAssignable(RowMapper.class, dbmRowMapper.value());
				Class<? extends RowMapper<?>> rowMapperClass = (Class<? extends RowMapper<?>>)dbmRowMapper.value();
				return ReflectUtils.newInstance(rowMapperClass, type);
			}
		}else{
//			rowMapper = super.getBeanPropertyRowMapper(type);
			rowMapper = new DbmBeanPropertyRowMapper<>(this.jdbcResultSetGetter,  type);
		}
		return rowMapper;
	}
	
	@Override
	public RowMapper<?> createRowMapper(NamedQueryInvokeContext invokeContext) {
		DynamicMethod dmethod = invokeContext.getDynamicMethod();
		DbmResultMapping dbmCascadeResult = dmethod.getMethod().getAnnotation(DbmResultMapping.class);
		if(dbmCascadeResult==null){
			return super.createRowMapper(invokeContext);
		}
		DbmNestedBeanRowMapper<?> rowMapper = new DbmNestedBeanRowMapper<>(jdbcResultSetGetter, dmethod.getComponentClass(), dbmCascadeResult);
		return rowMapper;
	}
	
}
