package org.onetwo;

import test.entity.UserEntity;



public class Test {

	public static void main(String[] args) throws Exception {
	}
	
	public static UserEntity createUser(){
		UserEntity user = new UserEntity();
		user.setUserName("est");
		System.out.println("class: " + user.getClass().getClassLoader());
		return user;
	}
	
	
}
