package com.n4.zs.message

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.event.GroovyEvent
import com.navis.vessel.business.schedule.VesselVisitDetails
import groovy.sql.Sql

import java.text.SimpleDateFormat

class GenerateCOARRI_TCLC extends GroovyApi {
	public String fileDir = "D:\\MESSAGE\\COARRI_TCLC"
	public String fileDirBak = "D:\\MESSAGE\\Backup\\COARRI_TCLC"
	public String messageType = "COARRI"
	public String sender_id = "ZSDND"
	
	String line_sep = "\n"
	
	public GroovyEvent event
	public GroovyApi api
	public Map<String,String> paras	//额外参数
	/*
	 * key = "ZX",value = "Z"/"X" 装船/卸船
	 * key = "RECEIVER_CODE" 接收方代码
	 */
	public Map<String,String> ISO_TO_GP
	public boolean noError = true;
	public String note = "";
	Sql sql;
	
	public void execute(GroovyEvent inEvent,GroovyApi inApi,Map<String,String> inParas){
		this.event = inEvent
		this.api = inApi
		this.paras = inParas
		init();
		VesselVisitDetails vvd = (VesselVisitDetails)event.getEntity();
		
		File f = generateCOARRIFile(vvd)
		if(sendFileToFtp(f)){
			File bakDir = new File(this.fileDirBak)
			if(!bakDir.exists()){
				bakDir.mkdirs()
			}
			File bakFile = new File(bakDir.getAbsolutePath()+"/"+f.getName())
			f.renameTo(bakFile)
		}
	}
	public String generateCOARRIText(VesselVisitDetails vvd){
		try{
			//获取对应参数
			long cvgkey = vvd.getCvdCv().getCvGkey()
			api.log("CarrierVisitGKey:"+cvgkey)
			String zx = paras.get("ZX")
			String file_description;
			String unitQueryStr;
			switch(zx){
				case "X"://进口卸船
				file_description = "DISCHARGE REPORT"
				unitQueryStr = """
				select ufv.gkey from inv_unit_fcy_visit ufv, inv_unit u where ufv.actual_ib_cv = '${cvgkey}' and ufv.unit_gkey = u.gkey
				""" 
				break
				case "Z"://出口装船
				file_description = "LOAD REPORT"
				unitQueryStr = """
				select ufv.gkey from inv_unit_fcy_visit ufv, inv_unit u where ufv.actual_ob_cv = '${cvgkey}' and ufv.unit_gkey = u.gkey
				""" 
				break
				default:
				throw new Exception("装卸标志错误")
			}
			
			String sender_code = "ZSDND"
			String receiver_code
			if(paras.get("RECEIVER_CODE")!=null){
				receiver_code = paras.get("RECEIVER_CODE")
			}else{
				throw new Exception("接收方代码为空")
			}
			String file_create_time = new SimpleDateFormat("yyyyMMddHHmm").format(new Date())
			
			List<UnitFacilityVisit> list_ufv = new ArrayList<UnitFacilityVisit>();//装卸箱列表
			//数据库查询
			sql.eachRow(unitQueryStr) {r->
				long ufv_gkey = r.'gkey'
				UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufv_gkey)
				//只统计TCLC的箱子
				if(ufv.getUfvUnit().getUnitLineOperator().getBzuId().equals("TCLC")){
					list_ufv.add(ufv)
				}
			}
			
			
			String ediString = ""
			int record_count = 0
			//00记录
			String edi00 = """
			00:${messageType}:${file_description}:9:${sender_id}:${receiver_code}:${file_create_time}::'
			""" 
			edi00 = edi00.trim() + line_sep
			ediString = ediString + edi00
			record_count++
			
			//01记录 其他接收方 无
			//10记录 船舶信息
			String vessel_code = vvd.getCarrierDocumentationNbr()
			String vessel = vvd.getCarrierVehicleName()
			if(vessel_code==null||vessel_code=="null"){
					vessel_code = ""
			}
			String voyage
			switch(zx){
				case "Z"://出口装船
				voyage = vvd.getVvdObVygNbr()
				break
				case "X"://进口卸船
				voyage = vvd.getVvdIbVygNbr()
				break
				default:
				throw new Exception("进出标志错误")
			}
			String nationality_code = vvd.getVvdVessel().getVesCountry().getCntryCode()
			if(nationality_code==null||nationality_code=="null"){
				nationality_code = ""
			}
			String ctn_numbers = list_ufv.size()
			
			String arrival_time,sailing_time,commenced_time_of_disch,completed_time_of_disch,commence_time_of_load,complete_time_of_load
			try{//到达时间
				arrival_time = new SimpleDateFormat("yyyyMMddHHmm").format(vvd.getCvdCv().getCvATA())
			}catch(Exception e){
				arrival_time = ""
			}
			try{//离开时间
				sailing_time = new SimpleDateFormat("yyyyMMddHHmm").format(vvd.getCvdCv().getCvATD())
			}catch(Exception e){
				sailing_time = ""
			}
			try{//开始卸船时间
				commenced_time_of_disch = new SimpleDateFormat("yyyyMMddHHmm").format(vvd.getVvdTimeStartWork())
			}catch(Exception e){
				commenced_time_of_disch = ""
			}
			try{//结束卸船时间
				completed_time_of_disch = new SimpleDateFormat("yyyyMMddHHmm").format(vvd.getVvdTimeEndWork())
			}catch(Exception e){
				completed_time_of_disch = ""
			}
			try{//开始装船时间
				commence_time_of_load = new SimpleDateFormat("yyyyMMddHHmm").format(vvd.getVvdTimeStartWork())
			}catch(Exception e){
				commence_time_of_load = ""
			}
			try{//结束装船时间
				complete_time_of_load = new SimpleDateFormat("yyyyMMddHHmm").format(vvd.getVvdTimeEndWork())
			}catch(Exception e){
				complete_time_of_load = ""
			}
			String edi10 = """
			10:${vessel_code}:${vessel}:${voyage}:${nationality_code}::${arrival_time}:${sailing_time}:${completed_time_of_disch}:${completed_time_of_disch}:${commence_time_of_load}:${completed_time_of_disch}:${ctn_numbers}'
			""" 
			edi10 = edi10.trim() + line_sep
			ediString = ediString + edi10
			record_count++
			
			//50,52,53循环
			list_ufv.each {u->
				UnitFacilityVisit ufv = u
				Unit unit = ufv.getUfvUnit()
				//50记录 箱信息
				String ctn_no = unit.getUnitId()
				api.log("获取箱信息:" + ctn_no)
				String unit_iso_code = unit.getUnitPrimaryUe().getUeEquipment().eqEquipType.eqtypId
				String ctn_size_type = ISO_TO_GP.get(unit_iso_code)==null?unit_iso_code:ISO_TO_GP.get(unit_iso_code)
				String ctn_operator_code = unit.getUnitLineOperator().getBzuId()
				String ctn_status;
				switch(unit.unitFreightKind){
					case FreightKindEnum.FCL:
					ctn_status = "F";
					break;
					case FreightKindEnum.MTY:
					ctn_status = "E";
					break;
					case FreightKindEnum.LCL:
					ctn_status = "L";
					break;
					default:
					ctn_status = "F";
				}
				String bl_no;
				if(unit.unitFreightKind.equals(FreightKindEnum.MTY)){	//空箱提单
					bl_no = ufv.getUfvFlexString08()
				}
				else{
					bl_no = unit.getUnitGoods().getGdsBlNbr()
				}
				if(bl_no!=null&&bl_no!=""){
					bl_no = bl_no.replace("+", "")
				}
				if(bl_no==null||bl_no=="null"){
					bl_no = ""
				}
				String seal_no = unit.getUnitSealNbr1()
				if(seal_no==null||seal_no=="null"){
					seal_no = ""
				}
				String stowage_location;
				switch(zx){
					case "Z"://出口装船
					stowage_location = ufv.getUfvLastKnownPosition().getPosSlot()
					break
					case "X"://进口卸船
					stowage_location = unit.getUnitArrivePositionSlot()
					break
					default:
					throw new Exception("进出标志错误")
				}
				if(stowage_location==null||stowage_location=="null"){
					stowage_location = ""
				}			
				else{
					if(stowage_location.length()<7){
						stowage_location = "0" + stowage_location
					}
				}
				
				String edi50 = """
				50:${ctn_no}:${ctn_size_type}:${ctn_operator_code}::${ctn_status}:${seal_no}:${stowage_location}'
				""" 
				edi50 = edi50.trim() + line_sep
				
				//52记录运输信息
				String discharge_port_code="",load_port_code = ""
				try{
					load_port_code = unit.getUnitRouting().getRtgPOL().getPointId()
				}
				catch(Exception e1){
					api.logWarn("获取装货港异常！")
					api.logWarn(e1.toString())
					if(zx.equals("Z")){
						api.log("装船，装货港以CNZOS代替")
						load_port_code = "CNZOS"
					}
				}
				try{
					discharge_port_code = unit.getUnitRouting().getRtgPOD1().getPointId()
				}
				catch(Exception e1){
					api.logWarn("获取卸货港异常！")
					api.logWarn(e1.toString())
					if(zx.equals("X")){
						api.log("卸船，卸货港以CNZOS代替")
						discharge_port_code = "CNZOS"
					}
				}

				String gross_weight = unit.getUnitGoodsAndCtrWtKg()
				
				String edi52 = """
				52:${discharge_port_code}::${load_port_code}::::${gross_weight}'
				""" 
				edi52 = edi52.trim() + line_sep
				
				//53残损循环(暂无)
				
				ediString = ediString + edi50 + edi52
				record_count++
				record_count++
				
			}
			
			//99字段 尾记录
			record_count++;
			String edi99 = """
			99:${record_count}'
			""" 
			edi99 = edi99.trim() + line_sep
			ediString = ediString + edi99
			
			return ediString.trim()
		}catch(Exception e){
			noError = false;
			note = e.toString()
			api.logWarn(note)
			return null
		}
				
	}
	
	public File generateCOARRIFile(VesselVisitDetails vvd){
		File dir = new File(this.fileDir)
		if(!dir.exists()){
			dir.mkdirs()
		}
		String edi = this.generateCOARRIText(vvd)
		if(edi==null){
			api.registerError("生成失败")
			return null
		}
		else{
			File f = new File(fileDir + "\\" + generateFileName() + ".txt")
			
			def printWriter = f.newPrintWriter('UTF-8')
			printWriter.append(edi)
			printWriter.flush()
			printWriter.close()
			return f
		}
	}
	public String generateFileName(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
		String ranstr = ""
		for(int i=0;i<5;i++){
			ranstr += new Random().nextInt(10).toString()
		}
		return messageType + "_" + sdf.format(new Date()) + "_" + ranstr
	}
	public void init(){
		try{
			ISO_TO_GP = api.getGroovyClassInstance("EDIMessageConventer").get_ISO_TO_GP_MAP()
			
			
			String DB = "jdbc:oracle:thin:@" + "192.168.50.32" + ":" + "1521" + ":" + "n4"
			String USER = "n4user"
			String PASSWORD = "n4dlt"
			String DRIVER = 'oracle.jdbc.driver.OracleDriver'
			sql = Sql.newInstance(DB, USER, PASSWORD, DRIVER)
		}catch(Exception e){
			noError = false;
			note = e.toString()
			api.logWarn(note)
		}
		
	}
	public boolean sendFileToFtp(File f){
		try{
			return api.getGroovyClassInstance("ZSEDIFtpHandler").uploadFile(f)
			
		}catch(Exception e){
			api.logWarn(e.toString())
			return false
		}
	}
}
