package org.onetwo.common.spring.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;

import org.onetwo.common.reflect.ReflectUtils;
import org.onetwo.common.spring.SpringUtils;
import org.onetwo.common.utils.Assert;
import org.onetwo.common.utils.FieldName;
import org.springframework.beans.BeanWrapper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * @author wayshall
 * <br/>
 */
public class MapToBeanConvertor {


	final protected static LoadingCache<PropertyContext, String> API_METHOD_CACHES = CacheBuilder.newBuilder()
																.build(new CacheLoader<PropertyContext, String>() {
																	@Override
																	public String load(PropertyContext pc) throws Exception {
																		Method method = pc.propertyDescriptor.getReadMethod();
																		FieldName fn = method.getAnnotation(FieldName.class);
																		if(fn==null){
																			fn = ReflectUtils.getIntro(method);
																		}
																		return apiMethod;
																	}
																});
	
	private static Function<PropertyContext, String> DEFAULT_KEY_CONVERTOR = pd->{
		return API_METHOD_CACHES.get(pd);
	};

	private Function<PropertyContext, String> keyConvertor = DEFAULT_KEY_CONVERTOR;
	
	public <T> T toBean(Map<String, Object> propValues, Class<T> beanClass){
		Assert.notNull(beanClass);
		Assert.notNull(propValues);
		T bean = ReflectUtils.newInstance(beanClass);
		BeanWrapper bw = SpringUtils.newBeanWrapper(bean);
		for(PropertyDescriptor pd : bw.getPropertyDescriptors()){
			PropertyContext pc = new PropertyContext(beanClass, pd);
			String key = getMapKey(pc);
			Object value = propValues.get(key);
			if(value==null){
				continue;
			}
			value = SpringUtils.getFormattingConversionService().convert(value, pd.getPropertyType());
			bw.setPropertyValue(pd.getName(), value);
		}
		return bean;
	}
	
	protected String getMapKey(PropertyContext pc){
		return keyConvertor.apply(pc);
	}
	
	protected static class PropertyContext {
		final private Class<?> beanClass;
		final private PropertyDescriptor propertyDescriptor;
		public PropertyContext(Class<?> beanClass, PropertyDescriptor propertyDescriptor) {
			super();
			this.beanClass = beanClass;
			this.propertyDescriptor = propertyDescriptor;
		}
		
		public Class<?> getBeanClass() {
			return beanClass;
		}

		public PropertyDescriptor getPropertyDescriptor() {
			return propertyDescriptor;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((beanClass == null) ? 0 : beanClass.hashCode());
			result = prime
					* result
					+ ((propertyDescriptor == null) ? 0 : propertyDescriptor
							.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PropertyContext other = (PropertyContext) obj;
			if (beanClass == null) {
				if (other.beanClass != null)
					return false;
			} else if (!beanClass.equals(other.beanClass))
				return false;
			if (propertyDescriptor == null) {
				if (other.propertyDescriptor != null)
					return false;
			} else if (!propertyDescriptor.equals(other.propertyDescriptor))
				return false;
			return true;
		}
		
		
	}
}
