package com.n4.zs.gate

import com.navis.argo.business.api.GroovyApi
import com.navis.road.business.RoadFacade
import com.navis.road.business.api.RoadManager
import com.navis.road.business.atoms.GateClientTypeEnum;
import com.navis.road.business.atoms.TranSubTypeEnum;
import com.navis.road.business.atoms.TruckStatusEnum;
import com.navis.road.business.model.Document;
import com.navis.road.business.model.Gate
import com.navis.road.business.model.RoadSequenceProvider;
import com.navis.road.business.model.Truck
import com.navis.road.business.model.TruckTransaction;
import com.navis.road.business.model.TruckVisitDetails
import com.navis.road.portal.configuration.CachedGateConfiguration;
import com.navis.road.portal.configuration.CachedGateStage
import com.navis.road.webservice.types.v1_0.CreateTruckVisitResponseWSType
import com.navis.road.webservice.types.v1_0.SubmitTransactionRequestWSType;
import com.navis.road.webservice.types.v1_0.TruckTransactionRequestWSType
import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.UnitCategoryEnum;
import com.navis.argo.business.model.DocumentType;
import com.navis.argo.business.model.Facility
import com.navis.argo.business.reference.Container;
import com.navis.argo.business.security.ArgoUser;
import com.navis.framework.business.Roastery;
import com.navis.framework.portal.BizRequest
import com.navis.framework.portal.BizResponse
import com.navis.framework.portal.CrudOperation
import com.navis.framework.portal.FieldChanges
import com.navis.framework.presentation.internationalization.DefaultMessageTranslatorProvider;
import com.navis.framework.presentation.internationalization.MessageTranslator
import com.navis.framework.presentation.internationalization.MessageTranslatorUtils;
import com.navis.framework.printing.PrintRequest
import com.navis.framework.util.BizViolation
import com.navis.framework.util.internationalization.UserMessage;
import com.navis.framework.util.message.MessageLevel;
import com.navis.road.RoadField;
import com.navis.road.RoadEntity;
import com.navis.road.business.appointment.model.GateAppointment;
import com.navis.argo.ContextHelper;
import com.navis.inventory.business.units.EqBaseOrderItem
import com.navis.inventory.business.units.Unit;
import com.navis.inventory.business.units.UnitFacilityVisit;
import com.xenos.framework.messages.InfoMessage;


import com.navis.road.webservice.types.v1_0.ScopeCoordinateIdsWsType
import com.navis.www.services.argoservice.ArgoServiceLocator
import com.navis.www.services.argoservice.ArgoServicePort
import com.navis.www.services.roadservice.RoadServiceLocator
import com.navis.www.services.roadservice.RoadServicePort

import groovy.sql.Sql
import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat
import java.util.Date;
import java.util.Map;

import org.apache.axis.client.Stub
/*
<groovy class-location="database" class-name="CreateTruckVisit">
	<parameters>
	   <parameter id="truck-lic" value="A3PU28"/>
	   <parameter id="ingate-stage-id" value="ingate"/>
	   <parameter id="unit-ids" value="CNTR1111111,CNTR2222222"/>
	   <parameter id="appt-nbrs" value="101,102"/>
	</parameters>
</groovy>
 */

/*
resultXML:
<create-truck-visit-response>
<results result="true" />
<info-message message="">
<transactions>
	<transaction nbr="" type="" unit-id="" freight-kind="" type-iso="" line="" gross-weight="" planned-slot="">
</transactions>
</create-truck-visit-response>

 */
class N4GateProcess extends GroovyApi {
	static String ROAD_SERVICE_URL = "http://localhost:9080/apex/services/roadservice"
	
	//settings
	GroovyApi api = new GroovyApi()
	//response
	String infoMessage=""
	String resultMessage=""
	//parameters
	String tranUnitIds;
	String tranApptNbrs;
	String truckLic;
	String stageIdIngate ;
	
	
	//internal use
	boolean noError = true;
	Truck truck;
	TruckVisitDetails tvdtls;
	Long tvdtlsGkey;
	String truckType;
	RoadServiceLocator roadServiceLocator;
	RoadServicePort roadServicePort;
	ScopeCoordinateIdsWsType scope;
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
				truckLic = truckLic[1..-1]
				api.log("车号："+truckLic+"   省份："+truckLicState)
				truck = Truck.findTruckByLicNbr(truckLic)
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
			truckType = truck.getTruckTrkCo().getBzuBizu().getCustomFlexFields().get("bzuCustomDFF_BizType")
			}catch(Exception e2){
			infoMessage = "卡车类型未定义，且卡车公司业务类型未定义"
				noError = false;
			}
		}
	}
	
public void processTruckVisit(){//处理卡车业务
		switch(truckType){
			case "行政车辆":
				processAdminTruck();
			break;
			case "散货车辆":
				processBBKTruck();
			break;
			case "集装箱车辆":
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

	private void processBBKTruck(){//处理散货车辆

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
		
		
		if(stageIdIngate.toLowerCase().equals("ingate")){
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
//							tvdtls = rm.createTruckVisit(gateId, stageId, null, null, null, null, truckLic, truckingCo, null,
//						null, null, null, null, null);
//
//							RoadSequenceProvider rsp = new RoadSequenceProvider();
//							tvdtls.setTvdtlsGosTvKey(rsp.getTruckVisitNextSeqValue());
						CreateTruckVisitResponseWSType cTVRWST= roadServicePort.createTruckVisit(scope, "DLT GATE", "", truckLic, "", "", "")
						tvdtlsGkey = cTVRWST.getTruckVisitId()
						api.log("truckVisitId:" + cTVRWST.getTruckVisitId())
						
						tvdtls = TruckVisitDetails.findTruckVisitByGkey(tvdtlsGkey)
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
										FieldChanges nonFileds = new FieldChanges();
										fields.setFieldChange(RoadField.TRAN_EQO_NBR,eqoNbr);
										fields.setFieldChange(RoadField.TRAN_STAGE_ID, stageIdIngate);
										fields.setFieldChange(RoadField.TRAN_SUB_TYPE, TranSubTypeEnum.DM);
										fields.setFieldChange(RoadField.TRAN_TRUCK_VISIT, tvdtls.getPrimaryKey());
										fields.setFieldChange(RoadField.TRAN_CREATOR, ContextHelper.getThreadUserContext().getUserId());
										//提交操作
										TruckTransaction  trkTrans = TruckTransaction.create(tvdtls, TranSubTypeEnum.DM,this.getTranscationNbr())
										api.log("提交DM Transcation")
										try{
											noError = rm.submitTransaction(trkTrans, fields, fields, gate, null, null)
										}catch(BizViolation b){
											noError = false;
											infoMessage = b.getMessage()
										}
										api.log("提交结果(0为失败)："+noError)
									break;
									
									default:
										api.log("其他事务类型：转入GateXML处理")
										
										apptsNoDM.add(appt)
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
							sql.eachRow(sqlstr) {r->
								isinbound = true;
								long ufvGkey = r.'gkey'
								ufv = UnitFacilityVisit.hydrate(ufvGkey)
								unit = ufv.getUfvUnit()
							}
							if(isinbound){//预录箱存在
								api.log("预录箱存在：转入GateXML处理")
								switch(unit.getUnitCategory()){
									case UnitCategoryEnum.EXPORT://出口重箱
										api.log("业务类型：收出口重箱(RE)")
										TruckTransactionRequestWSType ttRequest = new TruckTransactionRequestWSType();
										ttRequest.setCtrNbr(cntrId)
										SubmitTransactionRequestWSType stRequest = new SubmitTransactionRequestWSType();
										stRequest.setGateId(gateId)
										stRequest.setStageId(stageIdIngate)
										stRequest.setTranWSType("RE")
										stRequest.setTruckVisitId(tvdtlsGkey)
										stRequest.setTruckTransaction(ttRequest)
										roadServicePort.submitTransaction(scope, stRequest)
									break;
									
									case UnitCategoryEnum.STORAGE://堆存空箱
//										roadServicePort.resolveContainer(scope, "RM", cntrId)
									break;
									default:
										noError = false;
										infoMessage = "不支持的进箱类型，请人工处理"
										api.log(infoMessage)
									break;
								}							}
							else{//预录箱不存在
								infoMessage = "箱号"+ cntrId +"未预录"
								api.log(infoMessage)
								noError=false;
							}
						}
					}
//					
//					//submit
//					if(noError){
//						
//						if(apptsNoDM.size()+cntrList.size()>0){//有DM之外的业务
//							api.log("使用GateXML处理余下业务")
//							def gateXMLWriter = new StringWriter()
//							def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)
//							gateXMLBuilder.'gate'() {
//										'submit-multiple-transactions'() {
//											'gate-id'(gateId)
//											'stage-id'(this.stageId)
//											'truck-visit'('gos-tv-key':tvdtls.getTvdtlsGosTvKey())
//											'truck-transactions'{
//												if(apptsNoDM.size()>0){
//													apptsNoDM.each{appt->
//														GateAppointment ga = appt;
//														'truck-transaction'('appointment-nbr':ga.getApptNbr(),'tran-type':appt.getTranSubTypeEnum().getName())
//													}
//													
//												}
//												if(cntrList.size()>0){
//													cntrList.each { unit ->
//														String tranType
//														switch(unit.getUnitCategory()){
//															case UnitCategoryEnum.EXPORT://出口重箱
//																tranType = "RE"
//															break;
//																
//															case UnitCategoryEnum.STORAGE://堆存空箱
//																tranType = "RM"
//															break;
//														}		
//														'truck-transaction'('tran-type':tranType){
//															'container'('eqid':unit.getUnitId())
//														}
//													}													
//												}
//											}
//										}
//							}
//							String gateXMLString = gateXMLWriter.toString()
//							api.log("GateXML:\r\n" + gateXMLString)
//							GenericInvokeResponseWsType gateResponse = this.sendGateXML(gateXMLString)
//						}
//						else{
//							api.log("无业务需要GateXML处理，直接下一步：Processed to Next Stage")
////							rm.submitStageDone(tvdtls, gate, stageId, null, false, null, null)
//						}
//						
//						
//					}
//					else{
//						//发生错误，取消
//						api.log("发生错误，取消TruckVisit")
//						tvdtls.cancelTruckVisitAndTransactions()
//					}
//										
				}
				catch(Exception e){
					infoMessage = e.toString()
					e.printStackTrace()
					noError = false
				}
			}
		}
	}
	
	private void init(){
//		try{
			String DB = "jdbc:oracle:thin:@" + "192.168.50.14" + ":" + "1521" + ":" + "n4gc"
			String USER = "n4user"
			String PASSWORD = "n4dlt"
			String DRIVER = 'oracle.jdbc.driver.OracleDriver'
			sql = Sql.newInstance(DB, USER, PASSWORD, DRIVER)
			
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
//		}catch(Exception e){
//			api.log(e.toString())
//		}
	}
	
	private long getTranscationNbr(){
		RoadSequenceProvider rsp = new RoadSequenceProvider();
		return rsp.getTransactionNextSeqValue();
	}

	private void createReturnMessage(){
		def resultXML = new StringWriter()
		def result = new MarkupBuilder(resultXML)
		
		result."CreateTruckVisitResponse"{
			"results"("result":noError)
			"info-message"("message":infoMessage)
//			if(noError){
//				"transactions"{
//					tvdtls.getActiveTransactions().each {
//						TruckTransaction tran = it
//						"transaction"(
//							"nbr":tran.getTranNbr(),
//							"type":tran.getTranSubType().getName(),
//							"appt-nbr":tran.getTranAppointmentNbr()==null?"":tran.getTranAppointmentNbr(),
//							"unit-id":tran.getTranCtrNbr(),
//							"freight-kind":tran.getTranCtrFreightKind().getName(),
//							"type-iso":tran.getTranCtrTypeId(),
//							"gross-weight":tran.getTranCtrGrossWeight(),
//							"planned-position":tran.getTranCtrPosition().getPosSlot()
//							)
//					}
//				}
//			}
		}
		resultMessage = resultXML.toString()
	}

}
