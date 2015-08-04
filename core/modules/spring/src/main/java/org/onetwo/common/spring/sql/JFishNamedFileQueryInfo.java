package org.onetwo.common.spring.sql;

import java.util.Map;

import org.onetwo.common.db.ExtQueryUtils;
import org.onetwo.common.jdbc.DataBase;
import org.onetwo.common.utils.LangUtils;
import org.onetwo.common.utils.ReflectUtils;
import org.onetwo.common.utils.StringUtils;
import org.onetwo.common.utils.propconf.NamespaceProperty;

public class JFishNamedFileQueryInfo extends NamespaceProperty {
	public static final String COUNT_POSTFIX = "-count";
	public static final String TEMPLATE_KEY = "template";
	public static final String TEMPLATE_DOT_KEY = TEMPLATE_KEY + DOT_KEY;

	public static boolean isCountName(String name){
		return name.endsWith(COUNT_POSTFIX);
	}
	public static String trimCountPostfix(String name){
		if(!isCountName(name))
			return name;
		return name.substring(0, name.length() - COUNT_POSTFIX.length());
	}

	private DataBase dataBaseType;
	private String mappedEntity;
	private String countSql;
	private FileSqlParserType parser = FileSqlParserType.NONE;
	
	
	private Class<?> mappedEntityClass;
	private boolean autoGeneratedCountSql = true;
	
	final private Map<String, String> attrs = LangUtils.newHashMap();

	private boolean hql;

	public String getSql() {
		return getValue();
	}
	
	public String getCountName(){
		return getFullName() + COUNT_POSTFIX;
	}

	public void setSql(String sql) {
		this.setValue(sql);
	}

	public String getMappedEntity() {
		return mappedEntity;
	}

	public Class<?> getMappedEntityClass() {
		return mappedEntityClass;
	}

	public void setMappedEntity(String mappedEntity) {
		this.mappedEntity = mappedEntity;
		if(StringUtils.isNotBlank(mappedEntity)){
			this.mappedEntityClass = ReflectUtils.loadClass(mappedEntity);
		}
	}

	public String getCountSql() {
		if(!autoGeneratedCountSql){
			return countSql;
		}else{
//			throw new BaseException("countSql is null, and you shoud generated it by sql.");
			return ExtQueryUtils.buildCountSql(getSql(), null);
		}
	}

	protected String getCountSqlString() {
		if(!autoGeneratedCountSql){
			return countSql;
		}else{
			return "";
		}
	}

	public boolean isAutoGeneratedCountSql() {
		return autoGeneratedCountSql;
	}
	/*public String getCountSql2() {
		if(StringUtils.isBlank(countSql)){
			this.countSql = ExtQueryUtils.buildCountSql(this.getSql(), "");
		}
		return countSql;
	}*/

	public void setCountSql(String countSql) {
		autoGeneratedCountSql = false;
		this.countSql = countSql;
	}

	public boolean isIgnoreNull() {
		return parser==FileSqlParserType.IGNORENULL;
	}

	
	public FileSqlParserType getFileSqlParserType() {
		return parser;
	}

	public void setParser(String parser) {
		this.parser = FileSqlParserType.valueOf(parser.trim().toUpperCase());
	}
	
/*
	public boolean isNeedParseSql(){
		return isIgnoreNull();
	}*/

	public boolean isHql() {
		return hql;
	}
	public void setHql(boolean hql) {
		this.hql = hql;
	}
	public Map<String, String> getAttrs() {
		return attrs;
	}
	public String getTemplateName(String attr){
		return getFullName() + "."+TEMPLATE_DOT_KEY + attr;
	}
	public DataBase getDataBaseType() {
		return dataBaseType;
	}
	public void setDataBaseType(DataBase dataBaseType) {
		this.dataBaseType = dataBaseType;
	}
	public String toString() {
		return LangUtils.append("{namespace:, ", getNamespace(), ", name:", getName(), ", config:", getConfig(), ", mappedEntity:", mappedEntity, ", sql:", getSql(), ", countSql:", getCountSqlString(), "}");
	}
}
