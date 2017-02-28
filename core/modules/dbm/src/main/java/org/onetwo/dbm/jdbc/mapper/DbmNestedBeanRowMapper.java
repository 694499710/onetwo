package org.onetwo.dbm.jdbc.mapper;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.onetwo.common.convert.Types;
import org.onetwo.common.log.JFishLoggerFactory;
import org.onetwo.common.reflect.ClassIntroManager;
import org.onetwo.common.reflect.Intro;
import org.onetwo.common.reflect.ReflectUtils;
import org.onetwo.common.utils.JFishProperty;
import org.onetwo.dbm.annotation.DbmNestedResult;
import org.onetwo.dbm.annotation.DbmNestedResult.NestedType;
import org.onetwo.dbm.annotation.DbmResultMapping;
import org.onetwo.dbm.exception.DbmException;
import org.onetwo.dbm.jdbc.JdbcResultSetGetter;
import org.onetwo.dbm.jdbc.JdbcUtils;
import org.onetwo.dbm.utils.DbmUtils;
import org.slf4j.Logger;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.util.Assert;

import com.google.common.collect.Maps;

public class DbmNestedBeanRowMapper<T> implements RowMapper<T> {
	final protected Logger logger = JFishLoggerFactory.getLogger(this.getClass());
	
	private static ConversionService conversionService = new DefaultConversionService();

	protected Class<T> mappedClass;
	protected JdbcResultSetGetter jdbcResultSetGetter;
	protected ResultClassMapper resultClassMapper;

	public DbmNestedBeanRowMapper(JdbcResultSetGetter jdbcResultSetGetter, Class<T> mappedClass, DbmResultMapping dbmResultMapping) {
		this.mappedClass = mappedClass;
		this.jdbcResultSetGetter = jdbcResultSetGetter;
		ClassMapperContext context = new ClassMapperContext(jdbcResultSetGetter, dbmResultMapping);
		ResultClassMapper resultClassMapper = new RootResultClassMapper(context, dbmResultMapping.idField(), dbmResultMapping.columnPrefix(), mappedClass);
//		resultClassMapperMap.put(mappedClass, resultClassMapper);
		resultClassMapper.initialize();
		this.resultClassMapper = resultClassMapper;
	}

	public static ConversionService getConversionService() {
		return conversionService;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		Assert.state(this.mappedClass != null, "Mapped class was not specified");
		ResultSetWrappingSqlRowSet resutSetWrapper = new ResultSetWrappingSqlRowSet(rs);
		SqlRowSetMetaData rsmd = resutSetWrapper.getMetaData();
		Map<String, Integer> names = DbmUtils.lookupColumnNames(rsmd);
		
		T mappedObject = (T)this.resultClassMapper.mapResult(names, resutSetWrapper);
		return mappedObject;
	}

	protected static BeanWrapper createBeanWrapper(Object mappedObject) {
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);
		return bw;
	}
	protected static void initBeanWrapper(BeanWrapper bw) {
		ConversionService cs = getConversionService();
		if (cs != null) {
			bw.setConversionService(cs);
		}
	}
	
	static class DbmNestedResultData {
		final private String property;
		final private String id;
		final private String columnPrefix;
		final private NestedType nestedType;
		public DbmNestedResultData(DbmNestedResult nested) {
			this(nested.property(), nested.id(), nested.columnPrefix(), nested.nestedType());
		}
		public DbmNestedResultData(String property, String id, String columnPrefix, NestedType nestedType) {
			super();
			this.property = property;
			this.id = id;
			this.columnPrefix = columnPrefix;
			this.nestedType = nestedType;
		}
		public String getProperty() {
			return property;
		}
		public String getId() {
			return id;
		}
		public String getColumnPrefix() {
			return columnPrefix;
		}
		public String getColumnPrefix(String defValue) {
			return StringUtils.isBlank(columnPrefix)?defValue:columnPrefix;
		}
		public NestedType getNestedType() {
			return nestedType;
		}
		
	}

	static class ClassMapperContext {
		protected JdbcResultSetGetter jdbcResultSetGetter;
		protected Map<String, DbmNestedResultData> accessPathResultClassMapperMap = Maps.newHashMap();
		public ClassMapperContext(JdbcResultSetGetter jdbcResultSetGetter, DbmResultMapping dbmResultMapping) {
			super();
			this.jdbcResultSetGetter = jdbcResultSetGetter;
			for(DbmNestedResult nested : dbmResultMapping.value()){
				this.accessPathResultClassMapperMap.put(nested.property(), new DbmNestedResultData(nested));
			}
		}
		
		public DbmNestedResultData getDbmNestedResult(String accessPath){
			return this.accessPathResultClassMapperMap.get(accessPath);
		}
		
	}


	static class RootResultClassMapper extends ResultClassMapper {

		public RootResultClassMapper(ClassMapperContext context,
				String idField, String columnPrefix, Class<?> mappedClass) {
			super(context, idField, columnPrefix, mappedClass);
		}

		@Override
		protected Object afterMapResult(Object entity, Integer hash, boolean isNew){
			if(!isNew){
				return null;
			}
			return entity;
		}
	}
	static class PropertyResultClassMapper extends ResultClassMapper {
		final private ResultClassMapper parentMapper;
		final private JFishProperty belongToProperty;
		public PropertyResultClassMapper(ResultClassMapper parentMapper, String idField, String columnPrefix, JFishProperty belongToProperty) {
			this(parentMapper, idField, columnPrefix, belongToProperty, belongToProperty.getType());
		}
		public PropertyResultClassMapper(ResultClassMapper parentMapper, String idField, String columnPrefix, JFishProperty belongToProperty, Class<?> propertyType) {
			super(parentMapper.context, idField, columnPrefix, propertyType);
			this.belongToProperty = belongToProperty;
			this.parentMapper = parentMapper;
			this.accessPathPrefix = getAcessPath(parentMapper.accessPathPrefix, belongToProperty.getName());
		}
		public JFishProperty getBelongToProperty() {
			return belongToProperty;
		}
		public void linkToParent(BeanWrapper parent, Object propertyValue){
			if(propertyValue!=null){
				parent.setPropertyValue(belongToProperty.getName(), propertyValue);
			}
		}
		public ResultClassMapper getParentMapper() {
			return parentMapper;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static class CollectionPropertyResultClassMapper extends PropertyResultClassMapper {
		private Intro<? extends Collection> collectionClassIntro;
		public CollectionPropertyResultClassMapper(
				ResultClassMapper parentMapper, 
				String idField, String columnPrefix,
				JFishProperty belongToProperty) {
			super(parentMapper, idField, columnPrefix, belongToProperty, (Class<?>)belongToProperty.getFirstParameterType());
			if(belongToProperty.getFirstParameterType()==null){
				throw new DbmException("the collection property must be a parameterType: " + belongToProperty.getName());
			}
			collectionClassIntro = (Intro<? extends Collection>)belongToProperty.getTypeClassWrapper();
		}
		
		public void linkToParent(BeanWrapper parent, Object propertyValue){
			if(propertyValue==null){
				return ;
			}
			String propName = getBelongToProperty().getName();
			Collection values = (Collection)parent.getPropertyValue(propName);
			if(values==null){
				values = collectionClassIntro.newInstance();
				parent.setPropertyValue(propName, values);
			}
			if(!values.contains(propertyValue)){
				values.add(propertyValue);
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static class MapPropertyResultClassMapper extends PropertyResultClassMapper {
		private Intro<? extends Map> collectionClassIntro;
		private Class<?> keyClass;
		public MapPropertyResultClassMapper(
				ResultClassMapper parentMapper, 
				String idField, String columnPrefix,
				JFishProperty belongToProperty) {
			super(parentMapper, idField, columnPrefix, belongToProperty, (Class<?>)belongToProperty.getParameterType(1));
			this.keyClass = (Class<?>)belongToProperty.getParameterType(0);
			if(keyClass==null || getResultClass()==null){
				throw new DbmException("the Map property must be a parameterType: " + belongToProperty.getName());
			}
			collectionClassIntro = (Intro<? extends Map>)belongToProperty.getBeanClassWrapper();
		}
		
		public void linkToParent(BeanWrapper parent, Object propertyValue){
			if(propertyValue==null){
				return ;
			}
			if(!hasIdField()){
				throw new DbmException("no id configured for map : " + this.getBelongToProperty().getName());
			}
			Object id = ReflectUtils.getPropertyValue(propertyValue, getIdProperty().getName());
			if(id==null){
				throw new DbmException("id value can not be null for map : " + this.getBelongToProperty().getName());
			}
			String propName = getBelongToProperty().getName();
			id = Types.convertValue(id, keyClass);
			Map values = (Map)parent.getPropertyValue(getBelongToProperty().getName());
			if(values==null){
				values = collectionClassIntro.newInstance();
				parent.setPropertyValue(propName, values);
			}
			values.put(id, propertyValue);
		}
	}
	
	static class ResultClassMapper {
		private String idPropertyName = "";
		private String columnPrefix = "";
		private Intro<?> classIntro;
		private JFishProperty idProperty;
		private Map<String, JFishProperty> simpleFields = Maps.newHashMap();
		private Map<String, PropertyResultClassMapper> complexFields = Maps.newHashMap();
		protected Map<Integer, BeanWrapper> datas = Maps.newHashMap();
		private Class<?> resultClass;
		protected String accessPathPrefix;
		protected final ClassMapperContext context;
		
		public ResultClassMapper(ClassMapperContext context, String idField, String columnPrefix, Class<?> mappedClass) {
			super();
			this.idPropertyName = idField;
			this.columnPrefix = columnPrefix;
//			this.classIntro = ClassIntroManager.getInstance().getIntro(mappedClass);
//			this.initialize(mappedClass);
			this.resultClass = mappedClass;
			this.context = context;
		}

		public Class<?> getResultClass() {
			return resultClass;
		}

		public JFishProperty getIdProperty() {
			return idProperty;
		}

		public boolean hasIdField(){
			return idProperty!=null;
		}
		
		public static String getAcessPath(String accessPathPrefix, String propName){
			String acessPath = StringUtils.isBlank(accessPathPrefix)?propName:accessPathPrefix + "." + propName;
			return acessPath;
		}
		
		public void initialize() {
			this.classIntro = ClassIntroManager.getInstance().getIntro(resultClass);
			this.idProperty = this.classIntro.getJFishProperty(idPropertyName, false);
			for (PropertyDescriptor pd : classIntro.getProperties()) {
				if(pd.getWriteMethod()==null){
					continue;
				}
				JFishProperty jproperty = getActualJFishProperty(pd.getName());
				String accessPath = getAcessPath(accessPathPrefix, jproperty.getName());
				DbmNestedResultData result = context.getDbmNestedResult(accessPath);
				if(result!=null){
					PropertyResultClassMapper propertyMapper = null;
					NestedType nestedType = result.getNestedType();
					if(nestedType==NestedType.COLLECTION){
						propertyMapper = new CollectionPropertyResultClassMapper(this, result.getId(), appendPrefix(result.getColumnPrefix(jproperty.getName())), jproperty);
					}else if(nestedType==NestedType.MAP){
						propertyMapper = new MapPropertyResultClassMapper(this, result.getId(), appendPrefix(result.getColumnPrefix(jproperty.getName())), jproperty);
					}else{
						propertyMapper = new PropertyResultClassMapper(this, result.getId(), appendPrefix(result.getColumnPrefix(jproperty.getName())), jproperty);
					}
					propertyMapper.initialize();
					complexFields.put(jproperty.getName(), propertyMapper);
				}else{
					this.simpleFields.put(JdbcUtils.lowerCaseName(pd.getName()), jproperty);
				}
			}
		}
		
		private JFishProperty getActualJFishProperty(String propName){
			JFishProperty jproperty = classIntro.getJFishProperty(propName, false);
			JFishProperty actualProperty = jproperty;
			if(!jproperty.hasAnnotation(DbmNestedResult.class) 
					&& jproperty.getCorrespondingJFishProperty().isPresent()
					&& jproperty.getCorrespondingJFishProperty().get().hasAnnotation(DbmNestedResult.class)){
				actualProperty = jproperty.getCorrespondingJFishProperty().get();
			}
			return actualProperty;
		}
		
		protected String appendPrefix(String name){
			return columnPrefix + name;
		}
		
		protected Object afterMapResult(Object entity, Integer hash, boolean isNew){
			return entity;
		}
		
		public Object mapResult(Map<String, Integer> names, ResultSetWrappingSqlRowSet resutSetWrapper){
			Integer hash = null;
			Object entity = null;
			BeanWrapper bw = null;
			boolean isNew = true;
			if(hasIdField()){
				String actualColumnName = getActualColumnName(names, idProperty);
				if(actualColumnName==null){
					throw new DbmException("no id column found on resultSet for specified id field: " + idPropertyName+", columnPrefix:"+columnPrefix);
				}
				int index = names.get(actualColumnName);
				Object idValue = context.jdbcResultSetGetter.getColumnValue(resutSetWrapper, index, idProperty.getPropertyDescriptor());
				if(idValue==null){
					throw new DbmException("id column can not be null for specified id field: " + idPropertyName+", columnPrefix:"+columnPrefix);
				}
				hash = idValue.hashCode();
				if(datas.containsKey(hash)){
					bw = datas.get(hash);
					isNew = false;
				}else{
					bw = mapResultClassObject(names, resutSetWrapper);
					datas.put(hash, bw);
				}
			}else{
				bw = mapResultClassObject(names, resutSetWrapper);
				if(bw==null){
					return null;
				}
				hash = HashCodeBuilder.reflectionHashCode(bw.getWrappedInstance());
				if(datas.containsKey(hash)){
					bw = datas.get(hash);
					isNew = false;
				}else{
					datas.put(hash, bw);
				}
			}
			entity = bw.getWrappedInstance();
			for(PropertyResultClassMapper pm : this.complexFields.values()){
				Object propertyValue = pm.mapResult(names, resutSetWrapper);
				pm.linkToParent(bw, propertyValue);
			}
			return afterMapResult(entity, hash, isNew);
		}
		protected BeanWrapper mapResultClassObject(Map<String, Integer> names, ResultSetWrappingSqlRowSet resutSetWrapper){
			BeanWrapper bw = null;
			boolean hasColumn = false;
			for(Entry<String, JFishProperty> entry : simpleFields.entrySet()){
				JFishProperty jproperty = entry.getValue();
				String actualColumnName = getActualColumnName(names, jproperty);
				if(actualColumnName==null){
					continue;
				}else if(!hasColumn){
					hasColumn = true;
				}
				int index = names.get(actualColumnName);
				Object value = context.jdbcResultSetGetter.getColumnValue(resutSetWrapper, index, jproperty.getPropertyDescriptor());
				if(value!=null){
					if(bw==null){
						Object mappedObject = classIntro.newInstance();
						bw = createBeanWrapper(mappedObject);
					}
					bw.setPropertyValue(jproperty.getName(), value);
				}
			}
			return bw;
		}
		
		private String getActualColumnName(Map<String, Integer> names, JFishProperty jproperty){
			String fullName = appendPrefix(jproperty.getName());
			String columName = JdbcUtils.lowerCaseName(fullName);
			if(names.containsKey(columName)){
				return columName;
			}
			columName = JdbcUtils.underscoreName(fullName);
			if(names.containsKey(columName)){
				return columName;
			}
			return null;
		}
		/*public Object getPropertyValue(Map<String, Integer> names, ResultSetWrappingSqlRowSet resutSetWrapper, JFishProperty jproperty, String colName){
			Object value = null;
			Integer index = names.get(colName);
			value = getColumnValue(resutSetWrapper, index, jproperty.getPropertyDescriptor());
			return value;
		}*/

	}

}
