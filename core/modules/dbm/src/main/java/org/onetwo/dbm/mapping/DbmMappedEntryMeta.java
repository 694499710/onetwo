package org.onetwo.dbm.mapping;

import java.util.Collection;

import org.onetwo.common.annotation.AnnotationInfo;

public interface DbmMappedEntryMeta {

	public Collection<AbstractMappedField> getFields();
	public Collection<AbstractMappedField> getFields(DbmMappedFieldType... type);
	
	public DbmMappedField getField(String fieldName);
	
	public AnnotationInfo getAnnotationInfo();

	public boolean contains(String field);

	public boolean containsColumn(String col);


	public DbmMappedField getFieldByColumnName(String columnName);


	public DbmMappedEntryMeta addMappedField(AbstractMappedField field);

	public Class<?> getEntityClass();
	
	public String getEntityName();

	public TableInfo getTableInfo();

	public DbmMappedField getIdentifyField();
	
	public MappedType getMappedType();

	public boolean isJoined();
	public boolean isEntity();
	
	public boolean isInstance(Object entity);
	
	public DbmMappedField getVersionField();
	
	public boolean isVersionControll();
	
}