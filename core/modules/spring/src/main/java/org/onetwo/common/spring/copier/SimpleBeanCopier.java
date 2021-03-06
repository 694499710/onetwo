package org.onetwo.common.spring.copier;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;

import org.onetwo.common.reflect.ReflectUtils;
import org.onetwo.common.spring.SpringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.util.ReflectionUtils;

public class SimpleBeanCopier {
	
	public static SimpleBeanCopier unmodifyCopier(SimpleBeanCopier copier){
		return new SimpleBeanCopier(copier.propertyNameConvertor, copier.propertyFilter){

			@Override
			public void setPropertyFilter(PropertyFilter propertyFilter) {
				throw new UnsupportedOperationException("can not modify!");
			}
			@Override
			public void setPropertyNameConvertor(
					PropertyNameConvertor propertyNameConvertor) {
				throw new UnsupportedOperationException("can not modify!");
			}
	    	
	    };
	}
	
    public static final PropertyNameConvertor NOTHING_CONVERTOR = (name)-> name;
    public static final CommonPropertyValueSetter COMMON_PROPERTY_VALUE_COPIER = new CommonPropertyValueSetter();

	
//	private BeanWrapper targetBeanWrapper;
	private PropertyNameConvertor propertyNameConvertor = NOTHING_CONVERTOR;
//	private final T target;
//	private boolean ignoreNull;
	private PropertyFilter propertyFilter = SimplePropertyFilters.COPY_ALL;
	private PropertyValueCopier propertyValueCopier = COMMON_PROPERTY_VALUE_COPIER;

	public SimpleBeanCopier() {
	}

	public SimpleBeanCopier(PropertyNameConvertor convertor) {
		this.propertyNameConvertor = convertor;
	}

	public SimpleBeanCopier(PropertyNameConvertor propertyNameConvertor, PropertyFilter propertyFilter) {
		super();
		this.propertyNameConvertor = propertyNameConvertor;
		this.propertyFilter = propertyFilter;
	}

	public <T> T fromObject(Object src, Class<T> targetClass){
		return fromObject(src, ReflectUtils.newInstance(targetClass));
	}
	
	/****
	 * 
	 * @param src src object can be a map or java bean
	 * @param target
	 * @return
	 */
	public <T> T fromObject(Object src, T target){
		if(Class.class.isInstance(target)){
			Class<T> targetClass = ReflectUtils.getObjectClass(target);
			target = ReflectUtils.newInstance(targetClass);
		}
		BeanWrapper targetBeanWrapper = SpringUtils.newBeanWrapper(target);
		
    	BeanWrapper srcBean = CopyUtils.newBeanWrapper(src);
    	PropertyDescriptor[] properties = targetBeanWrapper.getPropertyDescriptors();
		for(PropertyDescriptor property : properties){
			/*if(property.getWriteMethod()==null)
				continue ;*/
			String targetPropertyName = property.getName();
			//if target no writable
			if(!targetBeanWrapper.isWritableProperty(targetPropertyName)){
				continue;
			}
			
			//if src no property
			if(!isSrcHasProperty(srcBean, targetPropertyName)){
				if(propertyNameConvertor!=null){
					targetPropertyName = propertyNameConvertor.convert(targetPropertyName);
				}
				if(!isSrcHasProperty(srcBean, targetPropertyName)){
					continue;
				}
			}
			
			Object srcValue = getPropertyValue(srcBean, targetPropertyName);
			if(propertyFilter!=null && !propertyFilter.isCopiable(property, srcValue)){
				continue;
			}
//			setPropertyValue(targetBeanWrapper, property, srcValue);
			this.propertyValueCopier.copyPropertyValue(this, targetBeanWrapper, property, srcValue);
    	}
		return target;
	}
	
	private boolean isSrcHasProperty(BeanWrapper srcBean, String targetPropertyName){
		if(srcBean.getWrappedInstance() instanceof Map){
			Map<?, ?> map = (Map<?, ?>)srcBean.getWrappedInstance();
			return map.containsKey(targetPropertyName);
		}else{
			return srcBean.isReadableProperty(targetPropertyName);
		}
	}

	protected void setPropertyValue0(BeanWrapper targetBeanWrapper, String propertyName, Object value) {
		targetBeanWrapper.setPropertyValue(propertyName, value);
	}
	
	protected void setPropertyFilter(PropertyFilter propertyFilter) {
		this.propertyFilter = propertyFilter;
	}
	
	
	protected Cloneable getCloneableAnnotation(Object target, PropertyDescriptor property){
		Cloneable cloneable = property.getReadMethod().getAnnotation(Cloneable.class);
		if(cloneable==null){
			Field field = ReflectionUtils.findField(target.getClass(), property.getName());
			if(field!=null){
				cloneable = field.getAnnotation(Cloneable.class);
			}
		}
		return cloneable;
	}

	

	/***
	 * 如果原对象没有目标对象的属性，则返回null
	 * @param srcBean
	 * @param targetPropertyName
	 * @return
	 */
	private Object getPropertyValue(BeanWrapper srcBean, String targetPropertyName){
		Object srcValue = null;
		if(srcBean.getWrappedInstance() instanceof Map){
			Map<?, ?> map = (Map<?, ?>)srcBean.getWrappedInstance();
			srcValue = map.get(targetPropertyName);
		}/*else if(srcBean.isReadableProperty(targetPropertyName)){
			srcValue = srcBean.getPropertyValue(targetPropertyName);
		}*/else{
			srcValue = srcBean.getPropertyValue(targetPropertyName);
		}
		return srcValue;
	}

	protected void setPropertyNameConvertor(PropertyNameConvertor propertyNameConvertor) {
		this.propertyNameConvertor = propertyNameConvertor;
	}

	protected void setPropertyValueCopier(PropertyValueCopier propertyValueCopier) {
		this.propertyValueCopier = propertyValueCopier;
	}

	/*public BeanCopier<T> ignoreNull() {
		this.ignoreNull = true;
		return this;
	}*/
	
}
