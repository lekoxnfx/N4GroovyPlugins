package src.com.n4.zs.message

import groovy.sql.Sql
import java.io.File;
import java.text.SimpleDateFormat
import java.util.Map;

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.FreightKindEnum;
import com.navis.argo.business.model.VisitDetails
import com.navis.framework.portal.query.DefaultQueryFilter
import com.navis.inventory.business.units.Unit;
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.event.GroovyEvent;
import com.navis.vessel.business.schedule.VesselVisitDetails

class GenerateCIQIM {
	public String fileDir = "D:\\MESSAGE\\CIQIM"
	public String fileDirBak = "D:\\MESSAGE\\Backup\\CIQIM"
	public String messageType = "CIQIM"
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
		api.log("国检CIQIM:开始生成CIQIM报文...")
		init();
		api.log("国检CIQIM:"+"初始化完毕...")
		
		VesselVisitDetails vvd = (VesselVisitDetails)event.getEntity();
		
		File f = generateCOARRIFile(vvd)
		if(sendFileToFtp(f)){
			api.log("国检CIQIM:"+"上传完毕...")
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
			api.log("国检CIQIM:"+"获取对应参数...")
			
			long cvgkey = vvd.getCvdCv().getCvGkey()
			api.log("CarrierVisitGKey:"+cvgkey)
			String zx = paras.get("ZX")
			String file_description = "CUSTI4";
			String unitQueryStr;
			switch(zx){
				case "X"://进口卸船
				unitQueryStr = """
				select ufv.gkey from inv_unit_fcy_visit ufv, inv_unit u where ufv.actual_ib_cv = '${cvgkey}' and ufv.unit_gkey = u.gkey
				""" 
				break
				case "Z"://出口装船
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
				list_ufv.add(ufv)
			}
			
			
			String ediString = ""
			int record_count = 0
			//00记录
			String edi00 = """
			00:${messageType}:${file_description}:9:${sender_id}:${receiver_code}:${file_create_time}:::'
			""" 
			edi00 = edi00.trim() + line_sep
			ediString = ediString + edi00
			record_count++
			
			api.log("国检CIQIM:"+ edi00)
			api.log("国检CIQIM:"+"00记录生成完毕...")
			
			//01记录 其他接收方 无
			
			
			//50循环
			api.log("国检CIQIM:"+"开始生成50记录...")
			String vessel_code_un = vvd.getVvdVessel().getVesDocumentationNbr()
			if(vessel_code_un==null||vessel_code_un=="null"){
				vessel_code_un = ""
			}
			String vessel_name_en = ""
			String voyage
			String vessel_code_local = vessel_code_un
			String direction = ""
			switch(zx){
				case "Z"://出口装船
				voyage = vvd.getVvdObVygNbr()
				direction = "E"
				break
				case "X"://进口卸船
				voyage = vvd.getVvdIbVygNbr()
				direction = "I"
				break
				default:
				throw new Exception("进出标志错误")
			}
			list_ufv.each {u->
				UnitFacilityVisit ufv = u
				Unit unit = ufv.getUfvUnit()
				//50记录 箱信息
				String ctn_no = unit.getUnitId()
				String unit_iso_code = unit.getUnitPrimaryUe().getUeEquipment().eqEquipType.eqtypId
				String ctn_size_type = ISO_TO_GP.get(unit_iso_code)==null?unit_iso_code:ISO_TO_GP.get(unit_iso_code)
				String ctn_size,ctn_type
				try{
					ctn_size = ctn_size_type[0..1]
					ctn_type = ctn_size_type[2..3]
				}catch(Exception e){}
				String in_gate_date = ""
				String cargo_type_code = ""
				
				try{
					in_gate_date = new SimpleDateFormat("yyyyMMddHHmmss").format(u.getUfvTimeIn())
				}catch(Exception e){}
				try{
					cargo_type_code = unit.getUnitGoods().getGdsCommodity().getCmdyId()
						if(cargo_type_code==null||cargo_type_code=="null"){
							cargo_type_code = ""
						}				
				}catch(Exception e){}
				String ctn_owner = unit.getUnitLineOperator().getBzuId()
				String ctn_status;
				String bill_count="";
				
				switch(unit.unitFreightKind){
					case FreightKindEnum.FCL:
					ctn_status = "F";
					bill_count = "001"
					break;
					case FreightKindEnum.MTY:
					ctn_status = "E";
					break;
					case FreightKindEnum.LCL:
					ctn_status = "L";
					bill_count = "003"
					break;
					default:
					ctn_status = "F";
					bill_count = "001"
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
				String location = ""
				String ctn_gross_weight = unit.getUnitGoodsAndCtrWtKg()
				String disch_port_code = "CNZOS"
				String package_num = ""
				String cargo_measure = ""
				String machine_no = ""
				
				String edi50 = """
				50:${vessel_code_local}:${voyage}:${direction}:${ctn_no}:${ctn_size}:${in_gate_date}:${cargo_type_code}:${ctn_status}:${bl_no}:${bill_count}:${vessel_name_en}:${vessel_code_un}:${location}:${ctn_gross_weight}:${ctn_owner}:${disch_port_code}:${seal_no}:${ctn_type}:${package_num}:${cargo_measure}:${machine_no}'
				""" 
				
				edi50 = edi50.trim() + line_sep
				api.log("国检CIQIM:"+ edi50)
				ediString = ediString + edi50
				record_count++;
			}
			api.log("国检CIQIM:"+"50记录生成完毕...")
			
			//99字段 尾记录
			record_count++;
			String edi99 = """
			99:${record_count}'
			""" 
			edi99 = edi99.trim() + line_sep
			ediString = ediString + edi99

			api.log("国检CIQIM:"+ edi99)
			api.log("国检CIQIM:"+"99记录生成完毕...")
			api.log("国检CIQIM:"+"报文生成完毕...")
			return ediString.trim()
		}catch(Exception e){
			noError = false;
			note = e.toString()
			e.printStackTrace()
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
			api.log("国检CIQIM:"+"生成文件完毕，文件名:" + f.getName())
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
			//test
//			def uploader = api.getGroovyClassInstance("ZSEDIFtpHandler")
//			uploader.uploadpath = "/in_test"
//			return uploader.uploadFile(f)
			return api.getGroovyClassInstance("ZSEDIFtpHandler").uploadFile(f)
			
		}catch(Exception e){
			api.logWarn(e.toString())
			return false
		}
	}
}
