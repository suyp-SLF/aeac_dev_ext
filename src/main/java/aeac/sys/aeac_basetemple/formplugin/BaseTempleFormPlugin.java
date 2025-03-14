package aeac.sys.aeac_basetemple.formplugin;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.MulBasedataDynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.db.DB;
import kd.bos.entity.datamodel.events.LoadDataEventArgs;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.MulBasedataProp;
import kd.bos.fileservice.FileServiceFactory;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.list.ListShowParameter;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.AttachmentServiceHelper;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import org.apache.commons.lang3.StringUtils;

public class BaseTempleFormPlugin extends AbstractBillPlugIn implements BeforeF7SelectListener {
	private static final String USER_ENTITYNAME = "bos_user";
	private static final String ALGORITHM = "aeac_algorithm";
	private static Log logger =  LogFactory.getLog(BaseTempleFormPlugin.class);
	@Override
	public void propertyChanged(PropertyChangedArgs e) {
		super.propertyChanged(e);

		if("aeac_attachment".equalsIgnoreCase(e.getProperty().getName())) {
			DynamicObject this_model = this.getModel().getDataEntity(true);
			DynamicObjectCollection this_model_col = this_model.getDynamicObjectCollection("attachmententity");
			this_model_col.stream().forEach(item -> {
				MulBasedataDynamicObjectCollection attachment = (MulBasedataDynamicObjectCollection) item.get("aeac_attachment");
				String fileNames = attachment.stream().map(i -> ((DynamicObject) i.get("fbasedataid")).get("name").toString()).collect(Collectors.joining("；"));
				System.out.println(fileNames);
				item.set("aeac_attname", fileNames);
			});
			this.getView().updateView("attachmententity");
		}
	}

	@Override
	public void loadData(LoadDataEventArgs e) {
		super.loadData(e);

		System.out.println(1);
	}

	@Override
	public void registerListener(EventObject e) {
		// TODO Auto-generated method stub
		super.registerListener(e);
		BasedataEdit attsecret = this.getControl("aeac_attsecret");
		if (attsecret != null){
			attsecret.addBeforeF7SelectListener(this);
		}
		BasedataEdit secret = this.getControl("aeac_secret");
		if (secret != null){
			secret.addBeforeF7SelectListener(this);
		}
	}
	
	
	@Override
	public void afterCreateNewData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterCreateNewData(e);
		updateSectrity();
		updateKeyPair();
		DynamicObject algorithm = BusinessDataServiceHelper.loadSingle(ALGORITHM, "", new QFilter[] {new QFilter("enable", QCP.equals, "1")});
		if(algorithm != null) {
			this.getModel().setValue("aeac_algorithm", algorithm);
		}else {
			this.getView().showErrorNotification("没有查询到可用的加密算法，请联系安全管理员！");
		}
	}
	
	/**
	 * 获得公钥私钥
	 */
	private void updateKeyPair() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void afterLoadData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterLoadData(e);
		updateSectrity();
	}
	
	private void updateSectrity() {
		
	}
	
	private void sectrity() {
		String message_bill = "";
		String message_file = "";
		//获得当前人员密级
		RequestContext content = RequestContext.get();
		long userid = UserServiceHelper.getCurrentUserId();
		
		DynamicObject user_dy = QueryServiceHelper.queryOne(USER_ENTITYNAME, "aeac_usersecrettype.aeac_highestsecret.number", new QFilter[] {new QFilter("id", QCP.equals, userid)});
		DynamicObject bill_secret = (DynamicObject)this.getModel().getValue("aeac_secret");
		DynamicObjectCollection bill_files = this.getModel().getEntryEntity("attachmententity");
		
		List<DynamicObject> file_secret = bill_files.stream().map(bill_file->bill_file.getDynamicObject("aeac_attsecret")).collect(Collectors.toList());
		
		Long user_secret_number = 0L;
		Long bill_secret_number = 0L;
		List<Long> file_secret_numbers = new ArrayList<>();
		Long file_secret_number = 0L;
		if(user_dy != null) {
			user_secret_number = Long.parseLong(user_dy.getString("aeac_usersecrettype.aeac_highestsecret.number"));
		}
		if(bill_secret != null) {
			bill_secret_number = Long.parseLong(bill_secret.getString("number"));
		}
		for(DynamicObject item : file_secret) {
			DynamicObject attSecret = item.getDynamicObject("aeac_attsecret");
			if(attSecret != null) {
				String number = attSecret.getString("number");
				if(Long.parseLong(number) > file_secret_number) {
					file_secret_number = Long.parseLong(number);
				}
			}
		}
		
		if(bill_secret_number > user_secret_number) {
			message_bill =" 当前人员无法设置此单据权限";
		}
		if(file_secret_number > bill_secret_number) {
			message_file = " 当前附件无法设置高于单据的密级";
		}
		
		
		System.out.println(1);
	}

	@Override
	public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
		// TODO Auto-generated method stub
		RequestContext content = RequestContext.get();
		long userid = UserServiceHelper.getCurrentUserId();
		
		DynamicObject user_dy = QueryServiceHelper.queryOne(USER_ENTITYNAME, "aeac_usersecrettype.aeac_highestsecret.number", new QFilter[] {new QFilter("id", QCP.equals, userid)});
		DynamicObject bill_secret = (DynamicObject)this.getModel().getValue("aeac_secret");
		
		
		String user_secret_number = "0";
		String bill_secret_numer = "0";
				
		if(null != user_dy) {
			user_secret_number = user_dy.getString("aeac_usersecrettype.aeac_highestsecret.number");
		}
		if(null != bill_secret) {
			bill_secret_numer = bill_secret.getString("number");
		}
				
		BasedataEdit basedataEdit = (BasedataEdit) beforeF7SelectEvent.getSource();
		
		String fieldKey = basedataEdit.getFieldKey();
		if("aeac_secret".equalsIgnoreCase(fieldKey)) {
			ListShowParameter listShowParameter = (ListShowParameter) beforeF7SelectEvent.getFormShowParameter();// 获取f7页面打开参数
	        listShowParameter.getListFilterParameter().getQFilters().add(new QFilter("number", QCP.less_equals, user_secret_number));// 将过滤条件加入
		}else if("aeac_attsecret".equalsIgnoreCase(fieldKey)){
			ListShowParameter listShowParameter = (ListShowParameter) beforeF7SelectEvent.getFormShowParameter();// 获取f7页面打开参数
	        listShowParameter.getListFilterParameter().getQFilters().add(new QFilter("number", QCP.less_equals, bill_secret_numer));// 将过滤条件加入
		}
	}
	@Override
	public void afterDoOperation(AfterDoOperationEventArgs args) {
		// TODO Auto-generated method stub
		super.afterDoOperation(args);
		try {

			if (args.getOperationResult().isSuccess() && StringUtils.equals("submit", args.getOperateKey())) {
				// 创建一个list存储待更新的数据
				List<DynamicObject> dataList = new ArrayList<DynamicObject>();
				// 1.获取当前单据数据包
				
				// 2.获取附件字段所在单据体（如附件字段在单据头，直接传递）

				DynamicObjectCollection cols = this.getModel().getEntryEntity("attachmententity");
				for (DynamicObject row : cols) {
					// 3.获取附件实体集合
					DynamicObjectCollection attachmentfield = row.getDynamicObjectCollection("aeac_attachment");
					for (DynamicObject dynamicObject : attachmentfield) {
						// 4.获取附件字段对象
						DynamicObject attObj = dynamicObject.getDynamicObject(MulBasedataProp.KEY_BASEDATAID);
						// 5.获取url属性
						String path = attObj.getString("url");
						if (StringUtils.isNotEmpty(path)) {
							if (path.contains("download.do")) {
								path = URLDecoder.decode(path, "UTF-8");
								path = path.substring(path.lastIndexOf("path=//") + 7);
							}
						}
						// 6.获取url真实路径
						path = FileServiceFactory.getAttachmentFileService().getFileServiceExt().getRealPath(path);
						// 查询当前单据行（附件）的密级
						DynamicObject secret = row.getDynamicObject("aeac_attsecret");
						// 根据附件路径查询关系表
						QFilter filter = new QFilter("aeac_path", QCP.equals, path);
						DynamicObject relationObject = BusinessDataServiceHelper.loadSingle("aeac_relation",
								"id,aeac_secret,aeac_path", new QFilter[] { filter });
						// 如果不为空，修改关系表中附件的密级
						if (relationObject != null) {
							relationObject.set("aeac_secret", secret);
							dataList.add(relationObject);
						}
					}
				}
				if (dataList.size() > 0) {
					SaveServiceHelper.save(dataList.toArray(new DynamicObject[dataList.size()]));
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			logger.error("保存单据后修改关系表附件密级失败:",e);
		}
	}
}
