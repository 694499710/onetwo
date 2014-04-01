package org.onetwo.common.excel;

import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.onetwo.common.excel.data.RowContextData;
import org.onetwo.common.exception.BaseException;
import org.onetwo.common.profiling.JFishLogger;
import org.onetwo.common.utils.LangUtils;
import org.onetwo.common.utils.StringUtils;

@SuppressWarnings("unchecked")
public class IteratorRowProcessor extends DefaultRowProcessor {

	private RowProcessor titleRowProcessor;
	
	public IteratorRowProcessor(PoiExcelGenerator excelGenerator, RowProcessor titleRowProcessor) {
		super(excelGenerator);
		this.titleRowProcessor = titleRowProcessor;
	}

	public void processRow(RowContextData rowContext) {
		
		if(rowContext.getRowModel().isRenderHeader()){
//			RowModel header = rowModel.copy();
//			header.setType(RowModel.Type.HEADER_KEY);
			this.titleRowProcessor.processRow(rowContext);
		}
		processIterator(rowContext);
	}
	
	@SuppressWarnings("rawtypes")
	public void processIterator(RowContextData rowContext) {
//		Sheet sheet = rowContext.getSheet();
		RowModel iterator = rowContext.getRowModel();
		
		Object dataSourceValue = rowContext.parseValue(iterator.getDatasource());
//		Object dataSourceValue = rowContext.getSheetDatas();
		Iterator it = LangUtils.convertIterator(dataSourceValue);
		if(it==null)
			return ;

		int index = 0;
		Map context = rowContext.getSelfContext();
		String indexName = StringUtils.isBlank(iterator.getIndex())?"rowIndex":iterator.getIndex();

//		FieldProcessor fieldProcessor = getFieldProcessor(iterator, context);
		for (Object ele = null; it.hasNext(); index++) {
//			UtilTimerStack.push(iterator.getName());
			if(index%1000==0)
				JFishLogger.INSTANCE.log("create row " + index);
			
			ele = it.next();
			rowContext.setCurrentRowObject(ele);
			Row row = createRow(rowContext);
			rowContext.setCurrentRow(row);
			
			if(context!=null){
				context.put(iterator.getName(), ele);
				context.put(indexName, index);
			}

			FieldModel field = null;
			try {
				for (int i = 0; i < iterator.size(); i++) {
					field = iterator.getField(i);
					this.processField(getFieldRootValue(rowContext, field), rowContext, field);
				}
			} catch (Exception e) {
				throw new BaseException("generate field["+iterator.getTemplate().getLabel()+","+iterator.getName()+","+field.getName()+"] error: "+e.getMessage() , e);
			}finally{
				rowContext.setCurrentRow(null);
				rowContext.setCurrentRowObject(null);
				if(context!=null){
					context.remove(iterator.getName());
					context.remove(indexName);
				}
			}

//			UtilTimerStack.pop(iterator.getName());
		}
	}
}
