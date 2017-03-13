package src.com.n4.zs.message

import com.navis.argo.business.api.GroovyApi;
import com.navis.argo.business.model.CarrierVisit
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.event.GroovyEvent;
import com.navis.argo.business.atoms.FreightKindEnum;
import com.navis.argo.business.atoms.UnitCategoryEnum;

import groovy.sql.Sql
import java.io.File;
import java.text.SimpleDateFormat

class GenerateCODECO_COS extends GroovyApi{
	public String fileDir = "D:\\MESSAGE\\CODECO_COS"
	public String fileDirBak = "D:\\MESSAGE\\Backup\\CODECO_COS"
	public String messageType = "CODECO"
	public String sender_id = "ZSDND"
	public String line_sep = "\n"
	
	public GroovyEvent event
	public GroovyApi api
	public Map<String,String> paras	//额外参数
	/*
	 * key = "IO",value = "I"/"E" 进门/出门
	 * key = "RECEIVER_CODE" 接收方代码
	 */
	public Map<String,String> ISO_TO_GP
	public boolean noError = true;
	public String note = "";
	Sql sql
	
	public void execute(GroovyEvent inEvent,GroovyApi inApi,Map<String,String> inParas){
		this.event = inEvent
		this.api = inApi
		this.paras = inParas
		init()
		Unit u = (Unit)event.getEntity()

		if(noError){
			File f = this.generateCodecoFile(u)
			if(sendFileToFtp(f)){
				File bakDir = new File(this.fileDirBak)
				if(!bakDir.exists()){
					bakDir.mkdirs()
				}
				File bakFile = new File(bakDir.getAbsolutePath()+"/"+f.getName())

				f.renameTo(bakFile)
			}
		}
	}
	//依据这个Unit执行
	public String generateCodecoText(Unit unit){
		try{
			UnitFacilityVisit ufv;
			CarrierVisit vslcv,trkcv;			
			//获取参数
			String io = paras.get("IO")
			String file_description;
			switch(io){
				case "I": 
				file_description = "GATE-IN REPORT"
				vslcv = unit.getOutboundCv()
				trkcv = unit.getInboundCv()
				break
				case "O":
				file_description = "GATE-OUT REPORT"
				vslcv = unit.getInboundCv()
				trkcv = unit.getOutboundCv()
				break
				default:
				throw new Exception("进出标志错误")
			}
			String sender_code = "ZSDND"
			String receiver_code
			if(paras.get("RECEIVER_CODE")!=null){
				receiver_code = paras.get("RECEIVER_CODE")
			}else{
				throw new Exception("接收方代码为空")
			}
			String file_create_time = new SimpleDateFormat("yyyyMMddHHmm").format(new Date())
			
			long unit_gkey = unit.getUnitGkey()
			String ufvQuery = """select gkey from inv_unit_fcy_visit ufv where unit_gkey = '${unit_gkey}'"""
			sql.eachRow(ufvQuery) {r->
				long ufv_gkey = r.'gkey'
				ufv = UnitFacilityVisit.hydrate(ufv_gkey)
			}
			
			
			String ediString = ""
			int record_count = 0
			 
			//00字段 头记录
			String edi00 = """
			00:${messageType}:${file_description}:9:${sender_id}:${receiver_code}:${file_create_time}::'
			"""
			edi00 = edi00.trim() + line_sep
			ediString = ediString + edi00
			record_count++;
			
			//01字段 其他接收方 无
			//10字段 描述船舶及箱经营人的数据项
			if(vslcv==null||trkcv == null){
				throw new Exception("没有获取到CarrierVisit")
			}
			String vessel_code = vslcv.getCarrierDocumentationNbr()
			String vessel = vslcv.getCarrierVehicleName()
			if(vessel_code==null||vessel_code=="null"){
				vessel_code = ""
			}
			String voyage;
			switch(io){
				case "I": 				
				voyage = vslcv.getCarrierObVoyNbrOrTrainId() + "E"
				break
				case "O":
				voyage = vslcv.getCarrierIbVoyNbrOrTrainId() + "I"
				break
				default:
				throw new Exception("进出标志错误")
			}
			
			String edi10="""
			10:${vessel_code}:${vessel}:${voyage}:COS:'
			"""
			
			edi10 = edi10.trim() + line_sep
			ediString = ediString + edi10
			record_count++;
			
			//50字段  描述箱信息的有关项目
			
			String ctn_no = unit.getUnitId()
			String unit_iso_code = unit.getUnitPrimaryUe().getUeEquipment().eqEquipType.eqtypId
			String ctn_size_type = ISO_TO_GP.get(unit_iso_code)==null?unit_iso_code:ISO_TO_GP.get(unit_iso_code)
			
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
			String pps
			if(io.equalsIgnoreCase("I")&&unit.unitFreightKind.equals(FreightKindEnum.MTY)){//进门空箱
				pps = "I"
			}
			if(io.equalsIgnoreCase("O")&&unit.unitFreightKind.equals(FreightKindEnum.MTY)){//出门空箱
				pps = "E"
			}
			if(io.equalsIgnoreCase("I")&&(unit.unitFreightKind.equals(FreightKindEnum.FCL)||unit.unitFreightKind.equals(FreightKindEnum.LCL))){//进门重箱
				pps = "E"
			}
			if(io.equalsIgnoreCase("O")&&(unit.unitFreightKind.equals(FreightKindEnum.FCL)||unit.unitFreightKind.equals(FreightKindEnum.LCL))){//出门重箱
				pps = "I"
			}
			String gate_in_time="",gate_out_time=""
			try{
				switch(io){
					case "I"://进门写入进门时间
					gate_in_time = new SimpleDateFormat("yyyyMMddHHmm").format(ufv.getUfvTimeIn())
					break
					case "O"://出门写入出门时间
					gate_out_time = new SimpleDateFormat("yyyyMMddHHmm").format(ufv.getUfvTimeOut())
					break
					default:
					throw new Exception("进出标志错误")
				}
			}catch(Exception e){
				noError = false;
				note = e.toString()
				api.logWarn(note)
			}
			
			String bl_no;
			if(unit.unitFreightKind.equals(FreightKindEnum.MTY)){	//空箱提单
				bl_no = unit.getUnitFlexString08()
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
			String eir_no = unit.getUnitFlexString07()
			if(eir_no==null||eir_no=="null"){
				eir_no = ""
			}
			String gross_weight = unit.getUnitGoodsAndCtrWtKg()
			String edi50 = """
			50:${ctn_no}:${ctn_size_type}:${ctn_status}:${pps}:${eir_no}:${bl_no}:${gross_weight}:${seal_no}:${gate_in_time}:${gate_out_time}:'
			"""
			
			edi50 = edi50.trim() + line_sep
			ediString = ediString + edi50
			record_count++;
			
			//51字段 待议
			//52字段 联运人
			String trailer_trademark = trkcv.getCvId()
			String carrier_code = unit.getUnitLineOperator().getBzuId()
			String edi52 = """
			52:3:${trailer_trademark}::${carrier_code}:'
			"""
			
			edi52 = edi52.trim() + line_sep
			ediString = ediString + edi52
			record_count++;
			
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
	
	public File generateCodecoFile(Unit unit){
		File dir = new File(this.fileDir)
		if(!dir.exists()){
			dir.mkdirs()
		}
		String edi = this.generateCodecoText(unit)
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
