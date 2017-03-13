package src.com.n4.zs.gate

import com.navis.road.business.RoadFacade
import com.navis.road.business.api.RoadManager
import com.navis.road.business.atoms.GateClientTypeEnum;
import com.navis.road.business.atoms.TranSubTypeEnum;
import com.navis.road.business.atoms.TruckStatusEnum;
import com.navis.road.business.model.Gate
import com.navis.road.business.model.Truck
import com.navis.road.business.model.TruckTransaction;
import com.navis.road.business.model.TruckVisitDetails
import com.navis.road.portal.configuration.CachedGateStage
import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.UnitCategoryEnum;
import com.navis.argo.business.model.Facility
import com.navis.framework.business.Roastery;
import com.navis.framework.portal.BizRequest
import com.navis.framework.portal.BizResponse
import com.navis.framework.portal.CrudOperation
import com.navis.framework.portal.FieldChanges
import com.navis.framework.util.message.MessageLevel;
import com.navis.road.RoadField;
import com.navis.road.RoadEntity;
import com.navis.road.business.appointment.model.GateAppointment;
import com.navis.argo.ContextHelper;
import com.navis.inventory.business.units.EqBaseOrderItem
import com.navis.inventory.business.units.Unit;
import com.navis.inventory.business.units.UnitFacilityVisit;

import groovy.sql.Sql
/*
<groovy class-location="database" class-name="CreateTruckVisit">
    <parameters>
       <parameter id="truck-lic" value="A3PU28"/>
       <parameter id="gate-id" value="DLT GATE"/>
       <parameter id="ingate-stage-id" value="ingate"/>
       <parameter id="unit-ids" value="CNTR1111111,CNTR2222222"/>
       <parameter id="appt-nbrs" value="101,102"/>
       <parameter id="cancel-when-trouble" "value=true"/>
    </parameters>
</groovy>
 */
class CreateTruckVisit {
	
	//settings
	
	GroovyApi api = new GroovyApi();
	//response
	String infoMessage=""
	BizResponse bizResp;
	//parameters
	String tranUnitIds;
	String tranApptNbrs;
	String truckLic;
	String stageIdIngate;
	
	
	//internal use
	Truck truck;
	String truckType;
	Facility fcy;
	Date date;
	Sql sql;
	
	public String execute(Map inParameters){
		//init
		date = new Date();
		fcy = ContextHelper.getThreadFacility();
		
		tranUnitIds = inParameters.get("unit-ids");
		tranApptNbrs = inParameters.get("appt-nbrs");
		truckLic = inParameters.get("truck-lic");
		stageIdIngate = inParameters.get("ingate-stage-id");
		
		init();

		//process
		api.log("接收到CreateTruckVisit请求")
		if(processTruckLic()){
			if(processTruckType()){
				processTruckVisit();
			}
		}
		return infoMessage
		
	}
	public boolean processTruckLic(){//检查TruckLic 是否存在并获取
		//判断是否为空
		if(truckLic==null||truckLic==""){
			infoMessage="车号不能为空！";
			return false;
		}
		else{
			try{
				String truckLicState = truckLic[0]
				truckLic = truckLic[1..-1]
				api.log("车号："+truckLic+"   省份："+truckLicState)
				truck = Truck.findTruckByLicNbr(truckLic)
				if(truck!=null&&truck.truckLicenseState.equals(truckLicState)){
					return true;
				}
				else{
					infoMessage = "车号未登记！"
					return false;
				}
			}catch(Exception e){
			    infoMessage = "车号解析错误！"
				api.log(e.toString())
				return false;
			}
		}
		
	}
	
public boolean processTruckType(){//处理卡车类型
		//判断车辆类型
		api.log("process truck, id " + truck.getTruckId())
		try{
			truckType = truck.getCustomFlexFields().get("truckCustomDFF_TruckType")
		}catch(Exception e){//获取卡车类型失败
			try{
			truckType = truck.getTruckTrkCo().getBzuBizu().getCustomFlexFields().get("bzuCustomDFF_BizType")
			}catch(Exception e2){
			infoMessage = "卡车类型未定义，且卡车公司业务类型未定义"
			return false;
			}
		}
	}
	
public boolean processTruckVisit(){//处理卡车业务
		switch(truckType){
			case "行政车辆":
				return processAdminTruck();
			break;
			case "散货车辆":
				return processBBKTruck();
			break;
			case "集装箱车辆":
				return processCNTRTruck();
			break;
			default:
				infoMessage = "不支持的车辆类型，请人工处理。"
				return false;
			break;
		}
	}
	
	private boolean processAdminTruck(){//处理行政车辆
		if(truck.getTruckTrkCo().getTrkcStatus().equals(TruckStatusEnum.BANNED)){
		    infoMessage = "卡车公司被禁止！"
			return false
		}
		else{
			if(truck.isTruckBanned()){
				infoMessage = "卡车被禁止！"
				return false
			}
			else{
				infoMessage = "行政车辆"
				return true
			}
		}
	}

	private boolean processBBKTruck(){//处理散货车辆
		return true;
	}
	
	private boolean processCNTRTruck(){//处理集装箱车辆
		String gateId = "DLT GATE"
		Gate gate = Gate.findGateById(gateId);
		String truckingCo = truck.getTruckTrkCo().getBzuId()
		
		RoadManager rm = (RoadManager) Roastery.getBean(RoadManager.BEAN_ID);
		RoadFacade roadFacade = (RoadFacade) Roastery.getBean(RoadFacade.BEAN_ID);
		BizRequest req = new BizRequest(ContextHelper.getThreadUserContext()); ;
		BizResponse response = new BizResponse();

		
		if(stageIdIngate.toLowerCase().equals("ingate")){
			boolean noError = true;
			String[] appts,cntrs
			if((tranApptNbrs==null||tranApptNbrs=="")&&(tranUnitIds==null||tranUnitIds=="")){
				infoMessage = "没有预约号和箱号。"
				api.log(infoMessage)
				noError = false;
			}
			if(tranApptNbrs!=null&&tranApptNbrs!=""){
				try{
					//解析预约号
					appts = this.tranApptNbrs.contains(",")?tranApptNbrs.split(","):[tranApptNbrs]
				}catch(Exception e){
					infoMessage = "预约号解析出错。"
					api.log(infoMessage)
					noError = false;
				}
			}
			if(tranUnitIds!=null&&tranUnitIds!=""){
				try{
					//解析箱号
					cntrs = this.tranUnitIds.contains(",")?tranUnitIds.split(","):[tranUnitIds]
				}catch(Exception e){
					infoMessage = "箱号解析出错。"
					api.log(infoMessage)
					noError = false;
				}
			}
			
			
			if(noError){
				try{
					//create truck visit
					TruckVisitDetails tvdtls = rm.createTruckVisit(gateId, stageIdIngate, null, null, null, null, truckLic, truckingCo, null,
						null, null, null, null, null);
					
					//create truck transcation
					//deal with appointments
					appts.each {apptNbr->
						api.log("处理预约号：" + apptNbr)
						GateAppointment appt;
						try{
							long apptLong = apptNbr.toLong()
							appt = GateAppointment.findGateAppointment(apptLong)
							if(appt==null){
								infoMessage = "预约号不存在:"+apptNbr
								api.log(infoMessage)
								noError = false;
							}
						}catch(Exception ee){
							infoMessage = "预约号不存在:"+apptNbr
							api.log(infoMessage)
							noError = false;
						}
						if(noError){
							switch(appt.getTranSubTypeEnum()){
								case TranSubTypeEnum.DM://提空箱预约
									api.log("事务类型：提空(DM)")
									def eqoNbr = appt.getGapptOrder().getEqboNbr() //获取EDO
									api.log("订单编号："+eqoNbr)
									FieldChanges fields = new FieldChanges();
									fields.setFieldChange(RoadField.TRAN_EQO_NBR,eqoNbr);
									fields.setFieldChange(RoadField.TRAN_STAGE_ID, stageIdIngate);
									fields.setFieldChange(RoadField.TRAN_SUB_TYPE, TranSubTypeEnum.DM);
									fields.setFieldChange(RoadField.TRAN_TRUCK_VISIT, tvdtls.getPrimaryKey());
									fields.setFieldChange(RoadField.TRAN_CREATOR, ContextHelper.getThreadUserContext().getUserId());
									
									//添加操作
									api.log("添加操作")
									CrudOperation crud = new CrudOperation(null, CrudOperation.TASK_INSERT, RoadEntity.TRUCK_TRANSACTION_BUNDLE, fields, null);
									req = new BizRequest(ContextHelper.getThreadUserContext());
									response = new BizResponse();
									req.addCrudOperation(crud);	
									api.log("提交信息")
									req.setParameter(RoadField.TRAN_STAGE_ID.getFieldId(), stageIdIngate);
									req.setParameter(RoadField.GATE_ID.getFieldId(), gateId);
									api.log("Request:" + req);
									roadFacade = (RoadFacade) Roastery.getBean(RoadFacade.BEAN_ID);
									roadFacade.submitTransaction(req, response);
								break;
								
								case TranSubTypeEnum.DI://提重箱预约
									api.log("事务类型：提重(DI)")
									def tranUnitId = appt.getGapptCtrId()
									api.log("箱号："+tranUnitId)
									FieldChanges fields = new FieldChanges();
									fields.setFieldChange(RoadField.TRAN_UNIT_ID, tranUnitId);
									fields.setFieldChange(RoadField.TRAN_STAGE_ID, stageIdIngate);
									fields.setFieldChange(RoadField.TRAN_SUB_TYPE, TranSubTypeEnum.DI);
									fields.setFieldChange(RoadField.TRAN_TRUCK_VISIT, tvdtls.getPrimaryKey());
									fields.setFieldChange(RoadField.TRAN_CREATOR, ContextHelper.getThreadUserContext().getUserId());
									//添加操作
									CrudOperation crud = new CrudOperation(null, CrudOperation.TASK_INSERT, RoadEntity.TRUCK_TRANSACTION, fields, null);
									req.addCrudOperation(crud);	
								break;
								
								default:
									infoMessage = "不支持的预约类型，请人工处理。"
									api.log(infoMessage)
									noError = false;
								break;
							}
						}
					}
					
					//deal with unitIds
					if(noError){
						cntrs.each { cntrId->
							api.log("处理箱号：" + cntrId)
							//检查是否预录入
							boolean isinbound=false;
							Unit unit;
							String sqlstr = """
							select U.gkey from INV_UNIT_FCY_VISIT UFV, INV_UNIT U
							where UFV.VISIT_STATE='1ACTIVE' and UFV.TRANSIT_STATE = 'S20_INBOUND'
							and UFV.UNIT_GKEY = U.GKEY and U.CATEGORY IN ('EXPRT','STRGE') 
							and U.ID = '${cntrId}'
							"""
							sql.eachRow(sqlstr) {r->
								isinbound = true;
								long unitGkey = r.'gkey'
								unit = Unit.hydrate(unitGkey)
							}
							if(isinbound){//预录箱存在
								switch(unit.getUnitCategory()){
									case UnitCategoryEnum.EXPORT://出口重箱
										api.log("业务类型：收出口重箱(RE)")
										FieldChanges fields = new FieldChanges();
										fields.setFieldChange(RoadField.TRAN_UNIT_ID, cntrId);
										fields.setFieldChange(RoadField.TRAN_STAGE_ID, stageIdIngate);
										fields.setFieldChange(RoadField.TRAN_SUB_TYPE, TranSubTypeEnum.RE);
										fields.setFieldChange(RoadField.TRAN_TRUCK_VISIT, tvdtls.getPrimaryKey());
										fields.setFieldChange(RoadField.TRAN_CREATOR, ContextHelper.getThreadUserContext().getUserId());
										//添加操作
										CrudOperation crud = new CrudOperation(null, CrudOperation.TASK_INSERT, RoadEntity.TRUCK_TRANSACTION, fields, null);
										req.addCrudOperation(crud);
										api.log("提交信息")
										req.setParameter(RoadField.TRAN_STAGE_ID.getFieldId(), stageIdIngate);
										req.setParameter(RoadField.GATE_ID.getFieldId(), gateId);
										api.log("Request:" + req);
										roadFacade = (RoadFacade) Roastery.getBean(RoadFacade.BEAN_ID);
										roadFacade.submitTransaction(req, response);
									break;
									
									case UnitCategoryEnum.STORAGE://堆存空箱
										api.log("业务类型：收空箱(RM)")
										FieldChanges fields = new FieldChanges();
										fields.setFieldChange(RoadField.TRAN_UNIT_ID, cntrId);
										fields.setFieldChange(RoadField.TRAN_STAGE_ID, stageIdIngate);
										fields.setFieldChange(RoadField.TRAN_SUB_TYPE, TranSubTypeEnum.RM);
										fields.setFieldChange(RoadField.TRAN_TRUCK_VISIT, tvdtls.getPrimaryKey());
										fields.setFieldChange(RoadField.TRAN_CREATOR, ContextHelper.getThreadUserContext().getUserId());
										//添加操作
										CrudOperation crud = new CrudOperation(null, CrudOperation.TASK_INSERT, RoadEntity.TRUCK_TRANSACTION, fields, null);
										req.addCrudOperation(crud);									
										break;
									default:
										noError = false;
										infoMessage = "不支持的进箱类型，请人工处理"
										api.log(infoMessage)
									break;
								}
							}
							else{//预录箱不存在
								infoMessage = "箱号"+ cntrId +"未预录"
								api.log(infoMessage)
								noError=false;
							}
						}
					}
					
					//submit
					if(noError){
//						//提交信息
//						api.log("提交信息")
//						req.setParameter(RoadField.TRAN_STAGE_ID.getFieldId(), stageId);
//						req.setParameter(RoadField.GATE_ID.getFieldId(), gateId);
//						api.log("Request:" + req);
//						roadFacade = (RoadFacade) Roastery.getBean(RoadFacade.BEAN_ID);
//						roadFacade.submitTransaction(req, response);
//						
//						if(response.status.equals(BizResponse.INDENT_3)){//Error
//							response.getMessages(MessageLevel.SEVERE).each {msg->
//								api.log("Severe Message:" + msg)
//								infoMessage =infoMessage + msg;
//							}
//							noError = false;
//						}
						
						if(noError){
//							rm.submitStageDone(tvdtls, gate, stageId, null, false, null, null)
						}
						else{
							tvdtls.cancelTruckVisitAndTransactions()
						}
					}
					return noError
				}
				catch(Exception e){
					infoMessage = e.toString()
					e.printStackTrace()
					return false;
				}
			}
		}
	}
	
	private void init(){
		try{
			String DB = "jdbc:oracle:thin:@" + "192.168.50.14" + ":" + "1521" + ":" + "n4gc"
			String USER = "n4user"
			String PASSWORD = "n4dlt"
			String DRIVER = 'oracle.jdbc.driver.OracleDriver'
			sql = Sql.newInstance(DB, USER, PASSWORD, DRIVER)
		}catch(Exception e){
			api.logWarn(e.toString())
		}
	}
}
