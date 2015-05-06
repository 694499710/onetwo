package org.onetwo.test.jorm.model.service;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.onetwo.common.jfishdb.spring.JFishDao;
import org.onetwo.common.utils.CUtils;
import org.onetwo.common.utils.LangUtils;
import org.onetwo.test.jorm.model.entity.UserAutoidEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserAutoidServiceImpl {
	
	/*@Resource
	private UserAutoidDao userAutoidDao;*/

	@Resource
	private DataSource dataSource;
	@Resource
	private JFishDao jfishDao;
	
	public int deleteAll(){
		return this.jfishDao.deleteAll(UserAutoidEntity.class);
	}
	
	public int saveList(String userNamePrefix, Date birthday, int count){ 
		List<UserAutoidEntity> list = LangUtils.newArrayList();
		for (int i = 0; i < count; i++) {
			UserAutoidEntity user = new UserAutoidEntity();
			user.setUserName(userNamePrefix+"-batch-"+i);
			user.setPassword("password-batch-"+i);
//			user.setCreateTime(new Date());
			user.setGender(i%2);
			user.setNickName("nickName-batch-"+i);
			user.setEmail("test@qq.com");
			user.setMobile("137"+i);
			user.setBirthday(birthday);
			
			list.add(user);
		}
		int insertCount = jfishDao.save(list);
		return insertCount;
	}
	
	public List<UserAutoidEntity> findUserAutoIdEntity(String userName, Date birthday){
		return jfishDao.findByProperties(UserAutoidEntity.class, CUtils.asMap(
																"userName:like", userName,
																"birthday", birthday
																));
	}
	
	public int update(List<UserAutoidEntity> users){
		return jfishDao.update(users);
	}
	
	public int delete(List<UserAutoidEntity> users){
		return jfishDao.delete(users);
	}

}
