package org.onetwo.common.excel;

import java.util.List;

import org.onetwo.common.utils.LangUtils;

public class WorkbookModel implements PoiModel {
	private String listener;
	
	private List<TemplateModel> sheets;
	
	@Override
	public void initModel(){
		for(TemplateModel template : sheets){
			 template.initModel();
		}
	}

	public List<TemplateModel> getSheets() {
		return sheets;
	}

	public void setSheets(List<TemplateModel> sheets) {
		this.sheets = sheets;
	}
	public void addSheet(TemplateModel sheet){
		if(sheets==null)
			sheets = LangUtils.newArrayList();
		this.sheets.add(sheet);
	}
	public TemplateModel getSheet(int index){
		return this.sheets.get(index);
	}

	public String getListener() {
		return listener;
	}

	public void setListener(String setter) {
		this.listener = setter;
	}
}
