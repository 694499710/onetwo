package org.onetwo.boot.module.alioss;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.onetwo.apache.io.IOUtils;
import org.onetwo.common.exception.BaseException;
import org.onetwo.common.file.FileStorer;
import org.onetwo.common.file.FileUtils;
import org.onetwo.common.file.SimpleFileStoredMeta;
import org.onetwo.common.file.StoringFileContext;
import org.onetwo.common.utils.StringUtils;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author wayshall
 * <br/>
 */
public class OssFileStore implements FileStorer<SimpleFileStoredMeta>, InitializingBean {
	
	private OssClientWrapper wrapper;
	private OssProperties ossProperties;
	private String bucketName;

	public OssFileStore(OssClientWrapper wrapper, OssProperties ossProperties) {
		super();
		this.wrapper = wrapper;
		this.ossProperties = ossProperties;
		this.bucketName = ossProperties.getBucketName();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.wrapper.createBucketIfNotExists(ossProperties.getBucketName());
	}

	@Override
	public SimpleFileStoredMeta write(StoringFileContext context) {
		String key = context.getKey();
		if(StringUtils.isBlank(key)){
			String prefix = FileUtils.replaceBackSlashToSlash(StringUtils.emptyIfNull(context.getModule())).replace("/", "-");
			key = prefix+"-"+UUID.randomUUID().toString()+FileUtils.getExtendName(context.getFileName(), true);
//			key = StringUtils.emptyIfNull(context.getModule())+"-"+UUID.randomUUID().toString()+FileUtils.getExtendName(context.getFileName(), true);
		}
		wrapper.objectOperation(bucketName, key)
				.store(context.getInputStream());
		
		String accessablePath = "/"+key;
		SimpleFileStoredMeta meta = new SimpleFileStoredMeta(context.getFileName(), key);
		meta.setSotredFileName(key);
		meta.setAccessablePath(accessablePath);
		meta.setFullAccessablePath(ossProperties.getUrl(key));
		meta.setStoredServerLocalPath(key);
		meta.setBizModule(context.getModule());
		meta.setSotredFileName(key);
		return meta;
	}
	
	@Override
	public void readFileTo(String accessablePath, OutputStream output) {
		InputStream in = readFileStream(accessablePath);
		try {
			IOUtils.copy(in, output);
		} catch (IOException e) {
			throw new BaseException("copy inputstream error!", e);
		}
	}

	@Override
	public InputStream readFileStream(String accessablePath) {
		String key = accessablePath;
		InputStream in = wrapper.objectOperation(bucketName, key)
								.getOSSObject()
								.orElseThrow(()->new BaseException("no file found for key: " + key))
								.getObjectContent();
		return in;
	}

	@Override
	public long getLastModified(String accessablePath) {
		return 0;
	}
	
	

}
