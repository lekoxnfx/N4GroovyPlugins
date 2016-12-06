package com.n4.zs.message

import com.navis.argo.business.api.GroovyApi;
import com.navis.inventory.business.units.Unit;
import com.navis.inventory.business.units.UnitFacilityVisit;
import com.navis.services.business.event.GroovyEvent;
import com.navis.argo.business.atoms.UnitCategoryEnum;

import groovy.sql.Sql
import java.text.SimpleDateFormat

class GetHGZL {
	GroovyApi api = new GroovyApi();
	public String fileDir = "D:\\MESSAGE\\HGZL"
	public String fileDirBak = "D:\\MESSAGE\\Backup\\HGZL"
	String note;
	Sql sql;
	Map ins_map =  new HashMap<String,String>();
	public void execute(Map args){
		init();
		api.log("海关指令：开始收取海关指令")
		try{
			List<File> list_f;
			list_f = api.getGroovyClassInstance("ZSEDIFtpHandler").downloadFiles("/MT0102_out","/MT0102_out_temp",fileDir)
			api.log("海关指令：收取"+list_f.size()+"个文件")
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
			SimpleDateFormat sdfN4 = new SimpleDateFormat("yyyyMMdd HHmmss")
			list_f.each{file->
				File f = file
				boolean noError = true
				try{
					api.log("海关指令：解析文件："+ f.getName())
					def RES = new XmlParser().parse(f)
					String sendTimeStr = RES.'Head'.'SendTime'.text();
					api.log("海关指令：发送时间："+sendTimeStr)
					RES.'Declaration'.'ApplyHead'.each {u->
						String unitNbr = u.'ContainerNum'.text()
						String insCode = u.'CustomInstruction'.text()
						String jouneyId = u.'JouneyId'.text()
						String sqlStr = """
							select gkey,active_ufv from inv_unit where   VISIT_STATE = '1ACTIVE' and id = '${unitNbr}'
						"""
						int count = 0
						long gkey,ufv_gkey;
						sql.eachRow(sqlStr) {row->
							count++
							gkey = row.'gkey'
							ufv_gkey = row.'active_ufv'
						}
						if(count==1){
							Unit unit = Unit.hydrate(gkey)
							UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufv_gkey)
							String voyage;
							boolean isImportOrExport = true;
							if(unit.unitCategory.equals(UnitCategoryEnum.EXPORT)){
								//出口箱,取出口航次
								voyage = ufv.getUfvActualObCv().getCarrierObVoyNbrOrTrainId()
							} else if(unit.unitCategory.equals(UnitCategoryEnum.IMPORT)){
								//进口箱,取进口航次
								voyage = ufv.getUfvActualIbCv().getCarrierIbVoyNbrOrTrainId()
							}
							else{
								api.log("海关指令：箱号"+unitNbr+"不是进口或者出口")
								isImportOrExport = false
								noError = false
							}
							//判断当前指令时间
							boolean timeCorrect
							Date oriInsDate
							Date curInsDate
							try{
								String oriInsCodeStr = unit.getUnitFlexString01()
								String oriSendTimeStr = oriInsCodeStr.substring(oriInsCodeStr.lastIndexOf("[")+1, oriInsCodeStr.lastIndexOf("]"))
								api.log("海关指令：箱号"+unitNbr+"旧指令的发送时间:"+oriSendTimeStr)
								//原指令时间与现指令时间
								oriInsDate = sdfN4.parse(oriSendTimeStr)
								curInsDate = sdf.parse(sendTimeStr.replace("T", " "))
								if(oriInsDate<curInsDate){
									timeCorrect = true
									api.log("海关指令：箱号"+unitNbr+"当前指令比旧指令晚，将会更新")
								}
								else{
									timeCorrect = false
									api.log("海关指令：箱号"+unitNbr+"当前指令比旧指令早，将不会更新")
								}
								
							}catch(Exception e){
								api.log("海关指令：箱号"+unitNbr+"没有旧指令的时间标志，或解析失败")
								timeCorrect = true
							}
							if(isImportOrExport&&jouneyId.equals(voyage)&&timeCorrect){//比对航次
								String insStr = insCode + ins_map.get(insCode) + "["+sdfN4.format(curInsDate)+"]"
								unit.setUnitFlexString01(insStr)
								api.log("海关指令："+"箱号"+unitNbr+"收到海关指令" + insStr + "["+sendTimeStr+"]")
								
		
							}
							else{
								api.log("海关指令："+"箱号"+unitNbr+"对应航次不匹配，收到：" + jouneyId+",系统："+voyage)
								noError = false	
							}
							
						}
						else{
							api.logWarn("海关指令："+"箱号"+unitNbr+"有"+count+"个活动记录！")
							noError = false
						}
					}
					if(noError){
						File bakDir = new File(this.fileDirBak)
						File bakFile = new File(bakDir.getAbsolutePath()+"/"+f.getName())
						if(!bakDir.exists()){
							bakDir.mkdirs()
						}
						f.renameTo(bakFile)
					}
					
				}catch(Exception e){
					api.logWarn("海关指令："+e.toString())
				}
			}
			
		}catch(Exception e){
			api.logWarn("海关指令："+e.toString())
		}finally{
			api.log("海关指令：收取结束")
		}
	}
	public void init(){
		ins_map.put("1","可装载")
		ins_map.put("2","可放行")
		ins_map.put("7","施封出场")
		ins_map.put("8","验封进场")
		ins_map.put("9","退运可出场")
		ins_map.put("I","区港联动－放行[可入区]")
		ins_map.put("A","查验指令[人工检查]")
		ins_map.put("B","查验指令[机检检查]")

		File dir = new File(fileDir)
		if (!dir.exists()){
			dir.mkdir();
		}
		File dirBack = new File(fileDirBak)
		if(!dirBack.exists()){
			dirBack.mkdir()
		}
		
		try{
			String DB = "jdbc:oracle:thin:@" + "192.168.50.32" + ":" + "1521" + ":" + "n4"
			String USER = "n4user"
			String PASSWORD = "n4dlt"
			String DRIVER = 'oracle.jdbc.driver.OracleDriver'
			sql = Sql.newInstance(DB, USER, PASSWORD, DRIVER)
		}catch(Exception e){
			note = e.toString()
			api.logWarn(note)
		}
		
	}
}
