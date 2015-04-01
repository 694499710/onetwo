package org.onetwo.common.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.onetwo.common.log.MyLoggerFactory;
import org.onetwo.common.utils.Page;
import org.onetwo.common.utils.ReflectUtils;
import org.slf4j.Logger;

@SuppressWarnings({"unchecked", "rawtypes"})
abstract public class BaseCrudServiceImpl<T, PK extends Serializable> implements CrudEntityManager<T, PK> {
 
	protected final Logger logger = MyLoggerFactory.getLogger(this.getClass());
	
	protected Class entityClass;
	
	public BaseCrudServiceImpl(){
		this.entityClass = ReflectUtils.getSuperClassGenricType(this.getClass(), BaseCrudServiceImpl.class);
	}
	
	abstract public BaseEntityManager getBaseEntityManager();

	@Override
	public T findById(PK id) {
		return (T)getBaseEntityManager().findById(entityClass, id);
	}


	@Override
	public Number countRecord(Map<Object, Object> properties) {
		return getBaseEntityManager().countRecord(entityClass, properties);
	}

	@Override
	public Number countRecord(Object... params) {
		return getBaseEntityManager().countRecord(entityClass, params);
	}

	@Override
	public List<T> findAll() {
		return getBaseEntityManager().findAll(entityClass);
	}

	@Override
	public List<T> findByProperties(Map<Object, Object> properties) {
		return getBaseEntityManager().findByProperties(entityClass, properties);
	}

	@Override
	public List<T> findByProperties(Object... properties) {
		return getBaseEntityManager().findByProperties(entityClass, properties);
	}

	@Override
	public Page<T> findPage(Page<T> page, Map<Object, Object> properties) {
		getBaseEntityManager().findPage(entityClass, page, properties);
		return page;
	}

	@Override
	public Page<T> findPage(Page<T> page, Object... properties) {
		getBaseEntityManager().findPage(entityClass, page, properties);
		return page;
	}

	@Override
	public T load(PK id) {
		return (T)getBaseEntityManager().load(entityClass, id);
	}

	@Override
	public T remove(T entity) {
		getBaseEntityManager().remove(entity);
		return entity;
	}

	@Override
	public void removes(Collection<T> entities) {
		getBaseEntityManager().removes(entities);
	}

	@Override
	public T removeById(PK id) {
		return (T)getBaseEntityManager().removeById(entityClass, id);
	}

	/*@Override
	public T createNew(T entity) {
		getBaseEntityManager().persist(entity);
		return entity;
	}

	@Override
	public T updateAttributes(T entity) {
		getBaseEntityManager().update(entity);
		return entity;
	}*/

	public T save(T entity) {
		return getBaseEntityManager().save(entity);
	}

	@Override
	public void delete(ILogicDeleteEntity entity) {
		getBaseEntityManager().delete(entity);
	}

	@Override
	public ILogicDeleteEntity deleteById(Serializable id) {
		return (ILogicDeleteEntity)getBaseEntityManager().deleteById(entityClass, id);
	}

	@Override
	public T findUnique(Map<Object, Object> properties) {
		return (T)getBaseEntityManager().findUnique(entityClass, properties);
	}

	@Override
	public T findUnique(Object... properties) {
		return (T)getBaseEntityManager().findUnique(entityClass, properties);
	}
	@Override
	public T findOne(Object... properties) {
		return (T)getBaseEntityManager().findOne(entityClass, properties);
	}

	@Override
	public List<T> findByProperties(QueryBuilder query) {
		return getBaseEntityManager().findByProperties(query);
	}
	@Override
	public void findPage(final Page page, QueryBuilder query) {
		getBaseEntityManager().findPage(page, query);
	}

	@Override
	public Collection<T> removeByIds(Class<T> entityClass, PK[] ids) {
		return getBaseEntityManager().removeByIds(entityClass, ids);
	}
	
}
