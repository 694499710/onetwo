package org.onetwo.common.db.sqlext;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onetwo.common.db.Magazine;
import org.onetwo.common.db.sqlext.ExtQuery.K;
import org.onetwo.common.db.sqlext.ExtQueryUtils.F;
import org.onetwo.common.utils.CUtils;
import org.onetwo.dbm.query.JFishSQLSymbolManagerImpl;

public class JFishExtQueryImplTest {
	
	public static class LogRouteEntity {
	}

	Map<Object, Object> properties;
	
	private SQLSymbolManagerFactory sqlSymbolManagerFactory;
	

	@Before
	public void setup(){
		this.properties = new LinkedHashMap<Object, Object>();
		sqlSymbolManagerFactory = SQLSymbolManagerFactory.getInstance();
	}
	
	@Test
	public void testSqlQueryJoin(){
		properties.put(K.SQL_JOIN, F.sqlJoin("left join syn_log_supplier sup on sup.id = ent.log_supplier_id"));
		properties.put(K.IF_NULL, K.IfNull.Ignore);
		properties.put("routeName", " ");
		properties.put("log_supplier_id", 22l);
		properties.put(".sup.supplierCode", "supplierCodeValue");
		properties.put(F.sqlFunc("ceil(@syn_end_time-@syn_start_time):>="), 1l);
		ExtQuery q = sqlSymbolManagerFactory.getJdbc().createSelectQuery(LogRouteEntity.class, "ent",  properties);
		q.build();
		
		/*String sql2 = "select ent.CREATE_TIME as createTime, ent.DELETE_TOUR as deleteTour, ent.FAIL_REASON as failReason, ent.FAIL_TOUR as failTour, ent.ID as id, ent.LAST_UPDATE_TIME as lastUpdateTime, ent.NEW_TOUR as newTour, ent.REPET_LOG_SUPPLIER_ID as repetLogSupplierId, ent.ROUTE_NAME as routeName, ent.STATE as state, ent.SUPPLIER_ROUTE_CODE as supplierRouteCode, ent.SYN_END_TIME as synEndTime, ent.SYN_START_TIME as synStartTime, ent.TYPE as type, ent.UPDATE_TOUR as updateTour, ent.YOOYO_ROUTE_ID as yooyoRouteId from SYN_LOG_ROUTE ent " +
				"left join syn_log_supplier sup on sup.id = ent.log_supplier_id where ent.log_supplier_id = :ent_log_supplier_id0 and sup.supplierCode = :sup_supplierCode1 and ceil(t.syn_end_time-t.syn_start_time) >= :ceil_t_syn_end_time_t_syn_start_time_2 order by ent.ID desc";
		*/
		String sql = "select ent.CREATE_TIME, ent.DELETE_TOUR, ent.FAIL_REASON, ent.FAIL_TOUR, ent.ID, ent.LAST_UPDATE_TIME, ent.NEW_TOUR, ent.REPET_LOG_SUPPLIER_ID, ent.ROUTE_NAME, ent.STATE, ent.SUPPLIER_ROUTE_CODE, ent.SYN_END_TIME, ent.SYN_START_TIME, ent.TYPE, ent.UPDATE_TOUR, ent.YOOYO_ROUTE_ID from SYN_LOG_ROUTE ent left join syn_log_supplier sup on sup.id = ent.log_supplier_id "
				+ "where ent.log_supplier_id = ?1 and sup.supplierCode = ?2 and ceil(ent.syn_end_time-ent.syn_start_time) >= ?3";
		String paramsting = "[22, supplierCodeValue, 1]";
//		System.out.println("testSqlQueryJoin: " + q.getSql().trim());
//		System.out.println("testSqlQueryJoin: " + q.getParamsValue().getValues().toString());
		Assert.assertEquals(sql.trim(), q.getSql().trim());
		Assert.assertEquals(paramsting.trim(), q.getParamsValue().getValues().toString().trim());
	}
	

	@Test
	public void testJFishExtQuery(){
		JFishSQLSymbolManagerImpl jqm = JFishSQLSymbolManagerImpl.create();
		this.properties.put("name:", null);
		this.properties.put("nickname:", "way");
		this.properties.put(K.IF_NULL, K.IfNull.Ignore);
		//left join table:alias on maintable.id=tur.magazin_id
		this.properties.put(K.LEFT_JOIN, CUtils.newArray("t_user_role:tur", new Object[]{"tur.magazin_id", "id"}));
		ExtQuery query = jqm.createSelectQuery(Magazine.class, properties);
		query.build();
		String tsql = query.getSql();
		System.out.println("testJFishExtQuery:"+tsql);
		String expected = "select magazine.* from magazine magazine left join t_user_role tur on ( tur.magazin_id=magazine.id ) where magazine.nickname = :magazine_nickname0";
		Assert.assertEquals(expected, tsql.trim());
	}
	
	
}
