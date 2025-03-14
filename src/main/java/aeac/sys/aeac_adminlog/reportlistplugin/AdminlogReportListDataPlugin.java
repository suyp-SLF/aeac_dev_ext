package aeac.sys.aeac_adminlog.reportlistplugin;

import org.apache.commons.lang3.StringUtils;

import kd.bos.algo.DataSet;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.OrmLocaleValue;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

/*
 * 修改数据库默认值   cosmic-log t_log_app  fk_aeac_adustatus = 'A'
 * 修改数据库默认值  cosmic-sys t_bas_attachment_oplog fk_aeac_adustatus = 'A'
 * 
 * 扩展bos_log_operation 添加下拉框 aeac_adustatus 未审计 A，已审计B
 * 扩展bos_attachment_oplog 添加下拉框 aeac_adustatus 未审计 A，已审计B
 * 
 * 报表数据源插件 aeac_adminlog
 * kd.cus.aeac.adminlog.plugin.AdminlogReportListDataPlugin
 * 用于将操作日志以及附件操作日志进行合并，并根据当前操作人显示不同的操作日志，以及审计功能
 * 
 * 安全管理员 
 * （弃用）
 * */

public class AdminlogReportListDataPlugin extends AbstractReportListDataPlugin {
	
	private static final String OPER_ENTITYNAME = "bos_log_operation";
	private static final String FILE_ENTITYNAME = "bos_attachment_oplog";
	
	private static final String[] OPER_FIELDS = {
			"id aeac_operid",//id
			"0 aeac_fileid",
			"user aeac_user",//操作用户名
			"clientip aeac_ipaddr",//IP地址
			"bizobj.name aeac_operobj",//操作对象
			"bizapp.name aeac_appname",//应用名
			"optime aeac_optime",//操作时间
			"opnamee aeac_opername",//操作种类
			"opdescriptione aeac_opercontent",//操作内容
			"opdescriptione aeac_operresult",//操作结果
			"aeac_adustatus aeac_adustatus"
	};
	private static final String[] FILE_FIELDS = {
			"'0' aeac_operid",
			"id aeac_fileid",//id
			"user aeac_user",//操作用户名
			"'附件目前没有IP地址' aeac_ipaddr",//IP地址
			"bizobj.name aeac_operobj",//操作对象
			"'附件目前没有应用' aeac_appname",//应用名
			"optime aeac_optime",//操作时间
			"optype aeac_opername",//操作种类
			"description aeac_opercontent",//操作内容
			"description aeac_operresult",//操作结果
			"aeac_adustatus aeac_adustatus"
	};

	@Override
	public DataSet query(ReportQueryParam arg0, Object arg1) throws Throwable {
		RequestContext requestContent = RequestContext.get();
		String userId = requestContent.getAccountId();
		QFilter[] qFilters = new QFilter[1];
		
//		if("2".equalsIgnoreCase(userId)) {
//			qFilters[0] = new QFilter("user.id", QCP.in, new String[] {"1","2"}) ;
//		}else if("3".equalsIgnoreCase(userId)) {
//			qFilters[0] = new QFilter("user.id", QCP.not_in, new String[] {"3", "2"})  ;
//		}else {
//			qFilters[0] = new QFilter("user.id", QCP.equals, "-1");
//		}
		
		String algoKey = getClass().getName() + ".query";
		
	 	DataSet Oper_AdminLog_DataSet = QueryServiceHelper.queryDataSet(algoKey,OPER_ENTITYNAME, StringUtils.join(OPER_FIELDS, ","), qFilters, "");
	 	DataSet File_AdminLog_dataSet = QueryServiceHelper.queryDataSet(algoKey,FILE_ENTITYNAME, StringUtils.join(FILE_FIELDS, ","), qFilters, "");
	 	//合并日志
	 	DataSet AdminLog_dataSet = Oper_AdminLog_DataSet.union(File_AdminLog_dataSet);
	 	
//	 	AdminLog_dataSet.print(true);
		return AdminLog_dataSet;
	}
	
	
}
