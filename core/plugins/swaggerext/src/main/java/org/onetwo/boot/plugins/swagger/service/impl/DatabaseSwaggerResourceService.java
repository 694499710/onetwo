package org.onetwo.boot.plugins.swagger.service.impl;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.onetwo.boot.plugins.swagger.entity.SwaggerEntity;
import org.onetwo.boot.plugins.swagger.entity.SwaggerFileEntity;
import org.onetwo.boot.plugins.swagger.entity.SwaggerFileEntity.Status;
import org.onetwo.boot.plugins.swagger.entity.SwaggerFileEntity.StoreTypes;
import org.onetwo.common.db.builder.Querys;
import org.onetwo.common.db.spi.BaseEntityManager;
import org.onetwo.common.spring.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import springfox.documentation.spring.web.json.Json;
import springfox.documentation.spring.web.json.JsonSerializer;

/**
 * @author wayshall
 * <br/>
 */
@Transactional
@Service
public class DatabaseSwaggerResourceService {
	
	@Autowired
	private BaseEntityManager baseEntityManager;
	@Autowired
	private SwaggerServiceImpl swaggerService;
	@Autowired
	private SwaggerModelServiceImpl swaggerModelService;
	@Autowired
	private SwaggerParameterServiceImpl swaggerParameterService;
	@Autowired
	private JsonSerializer jsonSerializer;
	
	public Optional<Json> findJsonByGroupName(String groupName){
		/*SwaggerFileEntity fileEntity = findByGroupName(groupName);
		if(fileEntity==null){
			return Optional.empty();
		}
		Optional<Json> json = Optional.of(new Json(fileEntity.getContent()));*/
		Optional<Json> json = convertByGroupName(groupName).map(s->jsonSerializer.toJson(s));
		return json;
	}
	

	public Optional<Swagger> convertByGroupName(String groupName){
		SwaggerFileEntity file = findByGroupName(groupName);
		if(file==null){
//			throw new BaseException("swagger module not found for: " + groupName);
			return Optional.empty();
		}
		SwaggerEntity swaggerEntity = swaggerService.findBySwaggerFileId(file.getId());
		if(swaggerEntity==null){
//			throw new BaseException("swagger not found for swaggerFileId: " + file.getId());
			return Optional.empty();
		}
		Swagger swagger = this.swaggerService.convertBySwagger(swaggerEntity);
		return Optional.ofNullable(swagger);
	}
	
	
	/***
	 * 通过分组名称查找
	 * @author wayshall
	 * @param groupName
	 * @return
	 */
	public SwaggerFileEntity findByGroupName(String groupName){
		SwaggerFileEntity file = baseEntityManager.findOne(SwaggerFileEntity.class, "groupName", groupName);
		return file;
	}

	public List<SwaggerFileEntity> findAllEnabled(){
		return findListByStatus(SwaggerFileEntity.Status.ENABLED);
	}
	
	public List<SwaggerFileEntity> findListByStatus(Status status){
		List<SwaggerFileEntity> files = Querys.from(baseEntityManager, SwaggerFileEntity.class)
											  .where()
											  	.field("status").equalTo(status)
											  	.ignoreIfNull()
											  .end()
											  .toQuery()
											  .list();
		return files;
	}
	
	public SwaggerFileEntity saveSwaggerFile(StoreTypes storeType, Swagger swagger, String groupName, String content){
		SwaggerFileEntity swaggerFileEntity = baseEntityManager.findOne(SwaggerFileEntity.class, "groupName", groupName);
		if(swaggerFileEntity==null){
			swaggerFileEntity = new SwaggerFileEntity();
		}
		swaggerFileEntity.setGroupName(groupName);
		swaggerFileEntity.setApplicationName(swagger.getInfo().getTitle());
		swaggerFileEntity.setStatus(Status.ENABLED);
		swaggerFileEntity.setStoreType(storeType);
		swaggerFileEntity.setContent(content);
		baseEntityManager.save(swaggerFileEntity);
		return swaggerFileEntity;
	}
	
	public SwaggerFileEntity importSwagger(String groupName, MultipartFile swaggerFile){
		String content = SpringUtils.readMultipartFile(swaggerFile);
		if(StringUtils.isBlank(groupName)){
			groupName = swaggerFile.getOriginalFilename();
		}
		return importSwagger(groupName, content);
	}
	
	public SwaggerFileEntity importSwagger(String groupName, String content){
		StoreTypes storeType;
		Swagger swagger;
		SwaggerParser parser = new SwaggerParser();
		if(content.startsWith("http")){
			storeType = StoreTypes.URL;
			swagger = parser.read(content);
		}else if(content.startsWith("file:")){
			storeType = StoreTypes.DATA;
			swagger = parser.read(content);
		}else{
			storeType = StoreTypes.DATA;
			swagger = parser.parse(content);
		}
		SwaggerFileEntity file = saveSwaggerFile(storeType, swagger, groupName, content);
		
		this.swaggerService.save(file, swagger);
		return file;
	}

	public SwaggerFileEntity removeWithCascadeData(String groupName){
		SwaggerFileEntity swaggerFileEntity = findByGroupName(groupName);
		return removeWithCascadeData(swaggerFileEntity);
	}
	public SwaggerFileEntity removeWithCascadeData(Long swaggerFileId){
		SwaggerFileEntity swaggerFileEntity = baseEntityManager.load(SwaggerFileEntity.class, swaggerFileId);
		return removeWithCascadeData(swaggerFileEntity);
	}
    public SwaggerFileEntity removeWithCascadeData(SwaggerFileEntity swaggerFileEntity){
    	this.swaggerService.removeWithCascadeData(swaggerFileEntity.getId());
    	baseEntityManager.remove(swaggerFileEntity);
    	return swaggerFileEntity;
    }
}
