package org.onetwo.boot.core.web.controller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.ValidationException;

import org.onetwo.apache.io.IOUtils;
import org.onetwo.boot.core.config.BootSiteConfig;
import org.onetwo.boot.core.web.utils.BootWebUtils;
import org.onetwo.boot.core.web.view.BootJsonView;
import org.onetwo.common.log.JFishLoggerFactory;
import org.onetwo.common.spring.SpringApplication;
import org.onetwo.common.spring.validator.ValidationBindingResult;
import org.onetwo.common.spring.validator.ValidatorWrapper;
import org.onetwo.common.utils.FileUtils;
import org.onetwo.common.utils.SimpleBlock;
import org.onetwo.common.utils.StringUtils;
import org.onetwo.common.utils.UserDetail;
import org.onetwo.common.web.utils.WebContextUtils;
import org.slf4j.Logger;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

abstract public class AbstractBaseController {
//	public static final String SINGLE_MODEL_FLAG_KEY = "__SINGLE_MODEL_FLAG_KEY__";
	
	public static final String DEFAULT_CONTENT_TYPE = "application/download; charset=GBK";
	
	public static final String REDIRECT = "redirect:";
	public static final String MESSAGE = "message";
	public static final String MESSAGE_TYPE = "messageType";
	public static final String MESSAGE_TYPE_ERROR = "error";
	public static final String MESSAGE_TYPE_SUCCESS = "success";
	

	public static final String FILTER_KEYS = BootJsonView.FILTER_KEYS;
	public static final String JSON_DATAS = BootJsonView.JSON_DATAS;
	
	protected final Logger logger = JFishLoggerFactory.getLogger(this.getClass());
	
	private SimpleBlock<Object, String> TO_STRING = new SimpleBlock<Object, String>() {
		@Override
		public String execute(Object object) {
			return object.toString();
		}
	};

	/*@Resource
	private CodeMessager codeMessager;*/
	
	/*@Resource
	private XmlTemplateExcelViewResolver xmlTemplateExcelViewResolver;*/
	

	@Resource
	private BootSiteConfig bootSiteConfig;
	
	protected AbstractBaseController(){
	}
	
	/*public String getMessage(String code, Object...args){
		return codeMessager.getMessage(code, args);
	}*/

	protected String redirect(String path){
		return REDIRECT + path;
	}
	
	protected void addFlashMessage(RedirectAttributes redirectAttributes, String msg){
		redirectAttributes.addFlashAttribute(MESSAGE, StringUtils.trimToEmpty(msg));
		redirectAttributes.addFlashAttribute(MESSAGE_TYPE, MESSAGE_TYPE_SUCCESS);
	}

	/*protected WebHelper webHelper(){
		return JFishWebUtils.webHelper();
	}*/

	
	/*****
	 * 根据model返回一个ModelAndView
	 * @param models
	 * @return
	 */
	protected ModelAndView model(Object... models){
		return mv(null, models);
	}
	
	protected ModelAndView redirectTo(String path){
		return mv(redirect(path));
	}
	
	protected ModelAndView redirectTo(String path, String message){
		return mv(redirect(path), MESSAGE, message, MESSAGE_TYPE, MESSAGE_TYPE_SUCCESS);
	}
	
	protected ModelAndView redirectToWithError(String path, String error){
		return mv(redirect(path), MESSAGE, error, MESSAGE_TYPE, MESSAGE_TYPE_ERROR);
	}
	
	protected ModelAndView putSuccessMessage(ModelAndView mv, String message){
		Assert.notNull(mv);
		mv.addObject(MESSAGE, message);
		mv.addObject(MESSAGE_TYPE, MESSAGE_TYPE_SUCCESS);
		return mv;
	}
	
	protected ModelAndView putErrorMessage(ModelAndView mv, String message){
		Assert.notNull(mv);
		mv.addObject(MESSAGE, message);
		mv.addObject(MESSAGE_TYPE, MESSAGE_TYPE_ERROR);
		return mv;
	}
	
	/**********
	 * 根据view名称和model返回一个ModelAndView
	 * @param viewName
	 * @param models "key1", value1, "key2", value2 ...
	 * @return
	 */
	protected ModelAndView mv(String viewName, Object... models){
		return BootWebUtils.mv(viewName, models);
	}
	
	protected ModelAndView messageMv(String message){
		return mv(MESSAGE, MESSAGE, message, MESSAGE_TYPE, MESSAGE_TYPE_SUCCESS);
	}
	
	public ModelAndView doInModelAndView(HttpServletRequest request, ModelAndView mv){
		return mv;
	}
	
	/*********
	 * 
	 * @param template 模板名称
	 * @param fileName 下载的文件名称
	 * @param models 生成excel的context
	 * @return
	 *protected ModelAndView exportExcel(String template, String fileName, Object... models){
		ModelAndView mv = mv(template, models);
		JFishExcelView view = new JFishExcelView();
		String path = this.xmlTemplateExcelViewResolver.getPrefix()+template;
		view.setUrl(path);
		view.setFileName(fileName);
		view.setSuffix(this.xmlTemplateExcelViewResolver.getSuffix());
		mv.setView(view);
		return mv;
	}*/
	
	@SuppressWarnings("unchecked")
	protected <T extends UserDetail> T getCurrentUserLogin(HttpSession session){
		return (T)WebContextUtils.getUserDetail(session);
	}
	
	protected <T> void validate(T object, BindingResult bindResult, Class<?>... groups){
		this.getValidator().validate(object, bindResult, groups);
	}
	
	protected ValidatorWrapper getValidator(){
		 return SpringApplication.getInstance().getValidator();
	}
	
	protected <T> ValidationBindingResult validate(T object, Class<?>... groups){
		return getValidator().validate(object, groups);
	}
	

	protected void validateAndThrow(Object obj, Class<?>... groups){
		ValidationBindingResult validations = validate(obj, groups);
		if(validations.hasErrors()){
			throw new ValidationException(validations.getFieldErrorMessagesAsString());
		}
	}
	
	protected void download(HttpServletResponse response, String filePath){
		String filename = FileUtils.getFileName(filePath);
		try {
			download(response, new FileInputStream(filePath), filename);
		} catch (FileNotFoundException e) {
			String msg = "下载文件出错：";
			logger.error(msg + e.getMessage(), e);
		}
	}
	
	protected void download(HttpServletResponse response, InputStream input, String filename){
		try {
			response.setContentType(DEFAULT_CONTENT_TYPE); 
			String name = new String(filename.getBytes("GBK"), "ISO8859-1");
			response.setHeader("Content-Disposition", "attachment;filename=" + name);
			IOUtils.copy(input, response.getOutputStream());
		} catch (Exception e) {
			String msg = "下载文件出错：";
			logger.error(msg + e.getMessage(), e);
		} finally{
			IOUtils.closeQuietly(input);
		}
	}
	
	protected void exportText(HttpServletResponse response, List<?> datas, String filename){
		exportText(response, datas, filename, TO_STRING);
	}
	
	protected void exportText(HttpServletResponse response, List<?> datas, String filename, SimpleBlock<Object, String> block){
		PrintWriter out = null;
		try {
			out = response.getWriter();
			response.setContentType(DEFAULT_CONTENT_TYPE); 
			String name = new String(filename.getBytes("GBK"), "ISO8859-1");
			response.setHeader("Content-Disposition", "attachment;filename=" + name);
			for(Object data : datas){
				out.println(block.execute(data));
			}
			out.flush();
		} catch (Exception e) {
			String msg = "下载文件出错：";
			logger.error(msg + e.getMessage(), e);
		} finally{
			IOUtils.closeQuietly(out);
		}
	}
	

	protected UserDetail getCurrentLoginUser(){
		return BootWebUtils.getUserDetail();
	}

	public BootSiteConfig getBootSiteConfig() {
		return bootSiteConfig;
	}
	
	
}
