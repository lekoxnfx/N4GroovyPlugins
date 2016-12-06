package com.n4.zs.gate

import com.navis.road.business.api.RoadManager
import com.navis.road.business.atoms.AppointmentStateEnum
import com.navis.road.business.atoms.TranSubTypeEnum;
import com.navis.road.business.atoms.TruckStatusEnum
import com.navis.road.business.model.Gate
import com.navis.road.business.model.RoadSequenceProvider;
import com.navis.road.business.model.Truck
import com.navis.road.business.model.TruckTransaction;
import com.navis.road.business.model.TruckVisitDetails
import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.UnitCategoryEnum
import com.navis.argo.business.model.Facility
import com.navis.framework.business.Roastery
import com.navis.road.business.appointment.model.GateAppointment;
import com.navis.argo.ContextHelper
import com.navis.inventory.business.units.Unit;
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.argo.webservice.types.v1_0.GenericInvokeResponseWsType
import com.navis.argo.webservice.types.v1_0.ScopeCoordinateIdsWsType
import com.navis.www.services.roadservice.RoadServiceLocator
import com.navis.www.services.roadservice.RoadServicePort



import groovy.sql.Sql
import groovy.xml.MarkupBuilder
import org.apache.axis.client.Stub
/*
<groovy class-location="database" class-name="GateProcess3">
	<parameters>
	   <parameter id="truck-lic" value="苏A3PU28"/>
	   <parameter id="stage-id" value="ingate"/>
	   <parameter id="unit-ids" value="CNTR1111111,CNTR2222222"/>
	   <parameter id="appt-nbrs" value="101,102"/>
	   <parameter id="request-id" value=""/>
	</parameters>
</groovy>
 */

/*
resultXML:
<create-truck-visit-response>
<request id="1234567" truck-lic="苏A3PU28"/>
<results result="true" />
<info-message message="">
<transactions>
	<transaction nbr="" type="" unit-id="" freight-kind="" type-iso="" line="" gross-weight="" planned-slot="">
</transactions>
</create-truck-visit-response>

 */
class N4GateProcess3 extends GroovyApi {
	static String ROAD_SERVICE_URL = "http://localhost:9080/apex/services/roadservice"

	public static String OK = "0"
	public static String INFO = "1"
	public static String WARNINGS = "2"
	public static String ERRORS = "3"
	
	//settings
	GroovyApi api = new GroovyApi()
	//response
	String infoMessage=""
	String resultMessage=""
	//parameters
	String tranUnitIds;
	String tranApptNbrs;
	String truckLic;
	String stageId;
	String requestId;
	
	//internal use
	boolean noError = true;
	Truck truck;
	TruckVisitDetails tvdtls;
	Long tvGkey;
	String truckType;
	RoadServiceLocator roadServiceLocator;
	RoadServicePort  roadServicePort;
	ScopeCoordinateIdsWsType scope;
	Facility fcy;
	Date date;
	Sql sql,sqlBS;
	
	public String execute(Map inParameters){
		//init
		date = new Date();
		fcy = ContextHelper.getThreadFacility();
		
		tranUnitIds = inParameters.get("unit-ids");
		tranApptNbrs = inParameters.get("appt-nbrs");
		truckLic = inParameters.get("truck-lic");
		stageId = inParameters.get("stage-id");
		requestId = inParameters.get("request-id")

		api.log("收到闸口处理请求N4GateProcess")
		api.log("收到的参数：")
		inParameters.each {
			api.log(it.key + ":" + it.value)
		}

		
		initSql();
		initSqlBS();
		initRoadServicePort();

		//process
		if(noError){
			processTruckLic()
		}
		if(noError){
			processTruckType()
		}
		if(noError){
			processTruckVisit()
		}
		this.createReturnMessage()
		api.log("返回结果：" + resultMessage)
		return this.resultMessage
		
	}
	public void processTruckLic(){//检查TruckLic 是否存在并获取
		//判断是否为空
		if(truckLic==null||truckLic==""){
			infoMessage="车号不能为空！";
			noError = false;
		}
		else{
			try{
				String truckLicState = truckLic[0]
				String truckLicNoState = truckLic[1..-1]
				api.log("车号："+truckLicNoState+"   省份："+truckLicState)
				truck = Truck.findTruckByLicNbr(truckLicNoState)
				if(truck!=null&&truck.truckLicenseState.equals(truckLicState)){
				}
				else{
					infoMessage = "车号未登记！"
					api.log(infoMessage)
					noError = false;
				}
			}catch(Exception e){
				infoMessage = "车号解析错误！"
				api.log(infoMessage)
				api.log(e.toString())
				noError = false;
			}
		}
		
	}
	
public void processTruckType(){//处理卡车类型
		//判断车辆类型
		api.log("process truck, id " + truck.getTruckId())
		try{
			truckType = truck.getCustomFlexFields().get("truckCustomDFF_TruckType")
		}catch(Exception e){//获取卡车类型失败
			try{
			api.log(e.toString())
				truckType = truck.getTruckTrkCo().getCustomFlexFields().get("bzuCustomDFF_BizType")
			}catch(Exception e2){
				api.log(e2.toString())
				infoMessage = "卡车类型未定义，且卡车公司业务类型未定义"
				noError = false;
			}
		}
		api.log("TruckType：" + truckType)
	}
	
public void processTruckVisit(){//处理卡车业务
		switch(truckType){
			case "行政车辆":	//卡车类型
			case "行政公司": //公司业务
				processAdminTruck();
			break;
			case "散货车辆":
			case "仅散货":
				processBBKTruck();
			break;
			case "集装箱车辆":
			case "仅集装箱":
				processCNTRTruck();
			break;
			default:
				infoMessage = "不支持的车辆类型，请人工处理。"
				noError = false;
			break;
		}
	}
	
	private void processAdminTruck(){//处理行政车辆
		if(truck.getTruckTrkCo().getTrkcStatus().equals(TruckStatusEnum.BANNED)){
			infoMessage = "卡车公司被禁止！"
			noError = false
		}
		else{
			if(truck.isTruckBanned()){
				infoMessage = "卡车被禁止！"
				noError = false
			}
			else{
				infoMessage = "行政车辆"
			}
		}
	}

	private void processBBKTruck() {//处理散货车辆
		String gateId = "DLT GC GATE"
		if (stageId.toLowerCase().equals("ingate")) {
			api.log("散杂货卡车进门")
			boolean isBooked = false
			String truckLic = truck.getTruckLicenseNbr()
			String truckLiiState = truck.getTruckLicenseState()
			long truckGkey = truck.getPrimaryKey()
			String _sql = " select * from bookingdetails b, bookingtrucks t ,N4ZS_VIEW_TRK v " +
					"where t.bookingnbr = b.bookingnbr and t.trucksgkey = v.truck_gkey and b.status = 'ACTIVE' and t.status = 'ACTIVE' and v.truck_status = 'OK' "+
					"and v.truck_gkey = '${truckGkey}'" + "and b.startdate < sysdate and (b.enddate > sysdate or b.enddate is null) "
			try{
				sqlBS.eachRow(_sql){
					isBooked = true
				}
				if(isBooked==false){
					noError = false
					infoMessage = "该卡车未预约或已过时间"
				}
			}catch(Exception e){
				e.printStackTrace()
				noError = false
				infoMessage = "查询预约数据失败，转入手动处理。"
			}

		} else if (stageId.toLowerCase().equals("outgate")) {
			api.log("散杂货卡车出门")
			List<TruckVisitDetails> truckVisitDetailsList = TruckVisitDetails.findTVActiveByTruckLicenseNbr(truck.getTruckLicenseNbr())
			if(truckVisitDetailsList!=null&&truckVisitDetailsList.size()>0){
				truckVisitDetailsList.each {TruckVisitDetails tvd ->
					if(noError){


						tvGkey = tvd.getPrimaryKey()
						api.log("发现活动的TruckVisit,Gkey:"+tvGkey)

						ResponseType response = roadServicePort.submitStageDone(scope,tvGkey,gateId,stageId)

						String ctvStatus = response.getStatus()
						api.log("Submit outgate stage done,结果：STATUS："+ctvStatus + response.getStatusDescription())
						api.log("RESPONSE RESULT:")
						response.getMessageCollector().getMessages().eachWithIndex { MessageType entry, int i ->
							api.log("["+i+"]:" +entry.getSeverityLevel()+ ":" + entry.getMessage())
							if(entry.getSeverityLevel().equals(this.ERRORS)||ctvStatus.equals(this.WARNINGS)){
								infoMessage = infoMessage + entry.getMessage()
							}
						}
						if(ctvStatus.equals(this.ERRORS)||ctvStatus.equals(this.WARNINGS)){
							noError = false;
						}
					}

				}
			}
			else{
				noError = false
				infoMessage = "没有找到进场记录，请人工处理"
				api.log(infoMessage)
			}

		}
		else{
			noError = false
			infoMessage = "不支持的阶段：" + stageId
			api.log(infoMessage)
		}
	}
	
	private void processCNTRTruck(){//处理集装箱车辆
		String gateId = "DLT GATE"
		Gate gate = Gate.findGateById(gateId);
	
		String truckingCo
		try{
			truckingCo = truck.getTruckTrkCo().getBzuId()
		}catch(Exception et){
			truckingCo = null;
		}
		
		RoadManager rm = (RoadManager) Roastery.getBean(RoadManager.BEAN_ID);
		
		
		if(stageId.toLowerCase().equals("ingate")){
			String[] appts,cntrs
			List<GateAppointment> apptsNoDM = new ArrayList<GateAppointment>();
			List<Unit> cntrList = new ArrayList<Unit>();
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
				//create truck visit
				try{
					
					try{
						
						CreateTruckVisitResponseWSType response = roadServicePort.createTruckVisit(scope, gateId, "", truck.getTruckLicenseNbr(), "", "", "")

						String ctvStatus = response.getCommonResponse().getStatus()
						api.log("创建TruckVisit结果：STATUS："+ctvStatus + response.getCommonResponse().getStatusDescription())
						api.log("RESPONSE RESULT:")
						response.getCommonResponse().getMessageCollector().getMessages().eachWithIndex { MessageType entry, int i ->
							api.log("["+i+"]:" +entry.getSeverityLevel()+ ":" + entry.getMessage())
							if(entry.getSeverityLevel().equalsIgnoreCase("Severe")){
								infoMessage = infoMessage + "+" + entry.getMessage()
							}
						}
						if(ctvStatus.equals(this.ERRORS)){
							noError = false;

						}
						else{
							tvGkey = response.getTruckVisitId()
							tvdtls = TruckVisitDetails.findTruckVisitByGkey(tvGkey)
						}
					}catch(Exception et){
						noError = false;
						infoMessage = "创建TruckVisit失败："+ et.toString()
					}
					
					//create truck transcation
					//deal with appointments
					
					if(noError){
						appts.each {apptNbr->
							api.log("处理预约号：" + apptNbr)
							GateAppointment appt;
							try{
								long apptLong = apptNbr.toLong()
								appt = GateAppointment.findGateAppointment(apptLong)
								if(appt==null||!appt.getApptState().equals(AppointmentStateEnum.CREATED)){
									infoMessage = "预约号不存在或已过期:"+apptNbr
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
									TruckTransactionRequestWSType ttr = new TruckTransactionRequestWSType()
									String eqboNbr = appt.getGapptOrder().getEqboNbr();
									api.log("EqboNbr:" + eqboNbr);
									ttr.setEqoNbr(eqboNbr);

									SubmitTransactionRequestWSType str = new SubmitTransactionRequestWSType()
									str.setGateId(gateId)
									str.setStageId(stageId)
									str.setTranWSType("DM")
									str.setTruckTransaction(ttr)
									str.setTruckVisitId(tvGkey)
									SubmitTransactionResponseWSType response = roadServicePort.submitTransaction(scope, str)
									this.processSubmitTransactionResponseWSType(response)

									break;

//									case TranSubTypeEnum.DI://提重箱预约
//
//										api.log("事务类型：提空(DI)")
//										TruckTransactionRequestWSType ttr = new TruckTransactionRequestWSType()
//										ttr.setCtrNbr(appt.getGapptCtrId())
//										SubmitTransactionRequestWSType str = new SubmitTransactionRequestWSType()
//										str.setGateId(gateId)
//										str.setStageId(stageId)
//										str.setTranWSType("DI")
//										str.setTruckTransaction(ttr)
//										str.setTruckVisitId(tvGkey)
//										SubmitTransactionResponseWSType response = roadServicePort.submitTransaction(scope, str)
//										this.processSubmitTransactionResponseWSType(response)
//
//										break;
									
									default:
										noError = false;
										infoMessage = "不支持的预约业务类型("+appt.getTranSubTypeEnum().getName()+")，请人工处理"
										api.log(infoMessage)
									break;
								}
							}
						}
					}
//					
//					
					//deal with unitIds
					if(noError){
						cntrs.each { cntrId->
							api.log("处理箱号：" + cntrId)
							//检查是否预录入
							boolean isinbound=false;
							Unit unit;
							UnitFacilityVisit ufv;
							String sqlstr = """
							select UFV.gkey  from INV_UNIT_FCY_VISIT UFV, INV_UNIT U
							where UFV.VISIT_STATE='1ACTIVE' and UFV.TRANSIT_STATE = 'S20_INBOUND'
							and UFV.UNIT_GKEY = U.GKEY and U.CATEGORY IN ('EXPRT','STRGE') 
							and U.ID = '${cntrId}'
							"""
							try {
								sql.eachRow(sqlstr) {r->
                                    isinbound = true;
                                    long ufvGkey = r.'gkey'
                                    ufv = UnitFacilityVisit.hydrate(ufvGkey)
                                    unit = ufv.getUfvUnit()
                                }
							} catch (Exception e) {
								api.log(e.toString())
								noError = false;
								infoMessage = "查询预录数据失败，转入手动处理。"
							}
							if(noError) {
								if (isinbound) {//预录箱存在
									api.log("预录箱存在：")
									switch (unit.getUnitCategory()) {
										case UnitCategoryEnum.EXPORT://出口重箱
											api.log("业务类型：收出口重箱(RE)")
											TruckTransactionRequestWSType ttr = new TruckTransactionRequestWSType()
											ttr.setCtrNbr(unit.getUnitId())
											SubmitTransactionRequestWSType str = new SubmitTransactionRequestWSType()
											str.setGateId(gateId)
											str.setStageId(stageId)
											str.setTranWSType("RE")
											str.setTruckTransaction(ttr)
											str.setTruckVisitId(tvGkey)
											SubmitTransactionResponseWSType response = roadServicePort.submitTransaction(scope, str)
											this.processSubmitTransactionResponseWSType(response)
											break;

										case UnitCategoryEnum.STORAGE://堆存空箱
											api.log("业务类型：收堆存空箱(RM)")
											TruckTransactionRequestWSType ttr = new TruckTransactionRequestWSType()
											ttr.setCtrNbr(unit.getUnitId())
											SubmitTransactionRequestWSType str = new SubmitTransactionRequestWSType()
											str.setGateId(gateId)
											str.setStageId(stageId)
											str.setTranWSType("RM")
											str.setTruckTransaction(ttr)
											str.setTruckVisitId(tvGkey)
											SubmitTransactionResponseWSType response = roadServicePort.submitTransaction(scope, str)
											this.processSubmitTransactionResponseWSType(response)
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
					}
					
					//submit
					if(noError){
							api.log("下一步：Processed to Next Stage")
							rm.submitStageDone(tvdtls, gate, stageId, null, false, null, null)
					}
					else{
						//发生错误，取消
						api.log("发生错误，取消TruckVisit")
						if(tvdtls!=null){
							tvdtls.cancelTruckVisitAndTransactions()
						}
					}
										
				}
				catch(Exception e){
					infoMessage = e.toString()
					e.printStackTrace()
					noError = false
				}
			}
		}
		else if(stageId.toLowerCase().equals("outgate")){
			 //查找TruckVisit
			api.log("集装箱卡车出门")
			List<TruckVisitDetails> truckVisitDetailsList = TruckVisitDetails.findTVActiveByTruckLicenseNbr(truck.getTruckLicenseNbr())
			if(truckVisitDetailsList!=null&&truckVisitDetailsList.size()>0){
				truckVisitDetailsList.each {TruckVisitDetails tvd ->
					if(noError){


						tvGkey = tvd.getPrimaryKey()
						api.log("发现活动的TruckVisit,Gkey:"+tvGkey)

						ResponseType response = roadServicePort.submitStageDone(scope,tvGkey,gateId,stageId)

						String ctvStatus = response.getStatus()
						api.log("Submit outgate stage done,结果：STATUS："+ctvStatus + response.getStatusDescription())
						api.log("RESPONSE RESULT:")
						response.getMessageCollector().getMessages().eachWithIndex { MessageType entry, int i ->
							api.log("["+i+"]:" +entry.getSeverityLevel()+ ":" + entry.getMessage())
							if(entry.getSeverityLevel().equals(this.ERRORS)||entry.getSeverityLevel().equals(this.WARNINGS)){
								infoMessage = infoMessage + entry.getMessage()
							}
						}
						if(ctvStatus.equals(this.ERRORS)||ctvStatus.equals(this.WARNINGS)){
							noError = false;
						}
					}

				}
			}
			else{
				noError = false
				infoMessage = "没有找到进场记录，请人工处理"
				api.log(infoMessage)
			}
		}
		else{
			noError = false
			infoMessage = "不支持的阶段：" + stageId
			api.log(infoMessage)
		}
	}

	private void initSql(){
		try {
			String DB = "jdbc:oracle:thin:@" + "192.168.50.32" + ":" + "1521" + ":" + "n4"
			String USER = "n4user"
			String PASSWORD = "n4dlt"
			String DRIVER = 'oracle.jdbc.driver.OracleDriver'
			sql = Sql.newInstance(DB, USER, PASSWORD, DRIVER)
		} catch (Exception e) {
			api.log(e.toString())
			api.log("初始化N4 SQL链接失败")
		}
	}
	private  void initSqlBS(){
		try {
			String DB2 = "jdbc:oracle:thin:@" + "192.168.50.32" + ":" + "1521" + ":" + "n4"
			String USER2 = "zsserver"
			String PASSWORD2 = "zsserver"
			String DRIVER2 = 'oracle.jdbc.driver.OracleDriver'
			sqlBS = Sql.newInstance(DB2, USER2, PASSWORD2, DRIVER2)
		} catch (Exception e) {
			api.log(e.toString())
			api.log("初始化BS SQL链接失败")
		}
	}
	private void initRoadServicePort(){

		roadServiceLocator = new RoadServiceLocator();
		roadServicePort = roadServiceLocator.getRoadServicePort(new URL(this.ROAD_SERVICE_URL))

		scope = new ScopeCoordinateIdsWsType()
		scope.setOperatorId(ContextHelper.getThreadOperator().getOprId())
		scope.setComplexId(ContextHelper.getThreadComplex().getCpxId())
		scope.setFacilityId(ContextHelper.getThreadFacility().getFcyId())
		scope.setYardId(ContextHelper.getThreadYard().getYrdId())

		Stub stub = (Stub) roadServicePort
		stub._setProperty(Stub.USERNAME_PROPERTY, "admin")
		stub._setProperty(Stub.PASSWORD_PROPERTY, "admin")

	}


	private void createReturnMessage(){
		def resultXML = new StringWriter()
		def result = new MarkupBuilder(resultXML)
		
		result."ProcessTruckVisitResponse"{
			"request"("id":requestId,"truck-lic":truckLic==null?"":truckLic)
			"results"("result":noError)
			"info-message"("message":infoMessage)
			if(noError&&tvdtls!=null){
				"transactions"{
					tvdtls.getActiveTransactions().each {
						TruckTransaction tran = it
						"transaction"(
//							"nbr":tran.getTranNbr(),
//							"type":tran.getTranSubType().getName(),
//							"appt-nbr":tran.getTranAppointmentNbr()==null?"":tran.getTranAppointmentNbr(),
//							"unit-id":tran.getTranCtrNbr(),
//							"freight-kind":tran.getTranCtrFreightKind().getName(),
//							"type-iso":tran.getTranCtrTypeId(),
//							"gross-weight":tran.getTranCtrGrossWeight(),
//							"planned-position":tran.getTranCtrPosition().getPosSlot()
							)
						{
							"parameter"("id":"nbr","value":tran.getTranNbr())
							"parameter"("id":"type","value":tran.getTranSubType().getName())
							"parameter"("id":"appt-nbr","value":tran.getTranAppointmentNbr()==null?"":tran.getTranAppointmentNbr())
							"parameter"("id":"unit-id","value":(tran.getTranSubType().equals(TranSubTypeEnum.DM))?"箱号待定":tran.getTranCtrNbr())
							"parameter"("id":"freight-kind","value":tran.getTranCtrFreightKind().getName())
							"parameter"("id":"type-iso","value":tran.getTranCtrTypeId())
							"parameter"("id":"gross-weight","value":tran.getTranCtrGrossWeight())
							"parameter"("id":"planned-position","value":tran.getTranCtrPosition().getPosSlot()==null?"":tran.getTranCtrPosition().getPosSlot()[0..3])
						}
					}
				}
			}
		}
		resultMessage = resultXML.toString()
	}
	private void processSubmitTransactionResponseWSType(SubmitTransactionResponseWSType response){
		String ctvStatus = response.getCommonResponse().getStatus()
		api.log("创建TruckTranscation结果：STATUS："+ctvStatus + response.getCommonResponse().getStatusDescription())
		api.log("RESPONSE RESULT:")
		response.getCommonResponse().getMessageCollector().getMessages().eachWithIndex { MessageType entry, int i ->
			api.log("["+i+"]:" +entry.getSeverityLevel()+ ":" + entry.getMessage())
			if(entry.getSeverityLevel().equals(this.ERRORS)||entry.getSeverityLevel().equals(this.WARNINGS)){
				infoMessage =  infoMessage + "+" + entry.getMessage()
			}
		}
		if(ctvStatus.equals(this.ERRORS)||ctvStatus.equals(this.WARNINGS)){
			noError = false;
		}
	}

}
