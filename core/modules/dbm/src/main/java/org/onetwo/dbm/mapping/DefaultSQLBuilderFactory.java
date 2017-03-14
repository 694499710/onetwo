package org.onetwo.dbm.mapping;

public class DefaultSQLBuilderFactory implements SQLBuilderFactory {


	@Override
	public EntrySQLBuilder createNamed(DbmMappedEntryMeta entry, String alias, SqlBuilderType type){
		return new EntrySQLBuilderImpl(entry, alias, true, type);
	}
	
	/****
	 * TODO: 可根据type创建不同的sqlBuilder
	 */
	@Override
	public EntrySQLBuilderImpl createQMark(DbmMappedEntryMeta entry, String alias, SqlBuilderType type){
		return new EntrySQLBuilderImpl(entry, alias, false, type);
	}
	/*@Override
	public TableSQLBuilder createNamed(String tableName, String alias, SqlBuilderType type){
		return new TableSQLBuilder(tableName, alias, true, type);
	}
	
	@Override
	public TableSQLBuilder createQMark(String tableName, String alias, SqlBuilderType type){
		return new TableSQLBuilder(tableName, alias, false, type);
	}*/

}
