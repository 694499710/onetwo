package org.onetwo.dbm.support;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;

import javax.sql.DataSource;
import javax.validation.Validator;

import org.onetwo.common.db.BaseCrudEntityManager;
import org.onetwo.common.db.BaseEntityManager;
import org.onetwo.common.db.CrudEntityManager;
import org.onetwo.common.exception.BaseException;
import org.onetwo.common.spring.Springs;
import org.onetwo.dbm.exception.DbmException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

final public class Dbms {
	
	final private static LoadingCache<Class<?>, CrudEntityManager<?, ?>> MANAGER_MAPPER = CacheBuilder.newBuilder()
																						.weakKeys()
																						.weakValues()
																						.build(new CacheLoader<Class<?>, CrudEntityManager<?, ?>>() {

																							@Override
																							public CrudEntityManager<?, ?> load(Class<?> entityClass) throws Exception {
																								return Dbms.newCrudManager(entityClass);
																							}
																							
																						});
	
	private static class BaseEntityManagerHoder {
		private static BaseEntityManager instance = Springs.getInstance().getBean(BaseEntityManager.class);
	}
	
	public static BaseEntityManager obtainBaseEntityManager(){
		return BaseEntityManagerHoder.instance;
	}
	
	@SuppressWarnings("unchecked")
	public static <E, ID  extends Serializable> CrudEntityManager<E, ID> obtainCrudManager(Class<E> entityClass){
		try {
			return (CrudEntityManager<E, ID>)MANAGER_MAPPER.get(entityClass);
		} catch (ExecutionException e) {
			throw new BaseException("obtain entityManager error: " + e.getMessage(), e);
		}
	}
	
	/****
	 * 使用spring配置的数据源创建CrudEntityManager
	 * @param entityClass
	 * @return
	 */
	public static <E, ID  extends Serializable> CrudEntityManager<E, ID> newCrudManager(Class<E> entityClass){
		return new BaseCrudEntityManager<>(entityClass);
	}
	/*****
	 * 使用指定的数据源创建CrudEntityManager
	 * @param entityClass
	 * @param dataSource
	 * @return
	 */
	public static <E, ID  extends Serializable> CrudEntityManager<E, ID> newCrudManager(Class<E> entityClass, DataSource dataSource){
		return new BaseCrudEntityManager<>(entityClass, newEntityManager(dataSource));
	}
	public static <E, ID  extends Serializable> CrudEntityManager<E, ID> newCrudManager(Class<E> entityClass, BaseEntityManager baseEntityManager){
		return new BaseCrudEntityManager<>(entityClass, baseEntityManager);
	}
	
	public static DbmEntityManager newEntityManager(DataSource dataSource) {
		DbmDaoImplementor dbmDao = (DbmDaoImplementor)newDao(dataSource);
		DbmEntityManagerImpl entityManager = new DbmEntityManagerImpl(dbmDao);
		try {
			entityManager.afterPropertiesSet();
		} catch (Exception e) {
			throw new DbmException("init EntityManager error: " +e.getMessage());
		}
		return entityManager;
	}
	
	public static DbmDao newDao(DataSource dataSource){
		return newDao(dataSource, null);
	}
	
	public static DbmDao newDao(DataSource dataSource, Validator validator){
		DbmDaoImpl dao = new DbmDaoImpl(dataSource);
		dao.setServiceRegistry(SimpleDbmInnserServiceRegistry.createServiceRegistry(dataSource, validator));
		dao.initialize();
		return dao;
	}

	private Dbms(){
	}
}
