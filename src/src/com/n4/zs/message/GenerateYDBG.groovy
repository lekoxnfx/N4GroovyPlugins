package src.com.n4.zs.message

import com.navis.argo.business.api.GroovyApi;
import com.navis.argo.business.atoms.FreightKindEnum;
import com.navis.argo.business.atoms.UnitCategoryEnum;
import com.navis.argo.business.model.CarrierVisit
import com.navis.argo.business.model.Complex;
import com.navis.argo.business.model.Facility
import com.navis.argo.business.model.Operator
import com.navis.cargo.business.model.BillOfLading;
import com.navis.cargo.business.model.BillOfLadingManagerPea;
import com.navis.cargo.business.model.GoodsBl;
import com.navis.framework.metafields.MetafieldId;
import com.navis.framework.metafields.MetafieldIdFactory;
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit;
import com.navis.services.business.event.GroovyEvent

import groovy.xml.MarkupBuilder

import java.io.File;
import java.text.SimpleDateFormat

class GenerateYDBG extends GroovyApi{
	public static String SENDER_ID = "ZSDND"
	public static String RECEIVER_ID = "2904"
	public static String BARRIER_NUM = "290402"
	
	public Operator operator = Operator.findOperator("ZSCT")
	public Complex complex = Complex.findComplex("ZST", operator)
	public Facility facility = Facility.findFacility("DLT", complex)
	
	public currentMessageId;
	public String fileDir = "D:\\MESSAGE\\YDBG"
	public String fileDirBak = "D:\\MESSAGE\\Backup\\YDBG"
	public GroovyEvent event
	public GroovyApi api
	
	public void execute(GroovyEvent inEvent,GroovyApi inApi){
		this.event = inEvent
		this.api = inApi
		Unit unit = (Unit)event.getEntity()
		api.log("开始生成YDBG报文")
		File f = generateYDBGXMLFile(unit)
		if(sendFileToFtp(f)){
			File bakDir = new File(this.fileDirBak)
			if(!bakDir.exists()){
				bakDir.mkdirs()
			}
			f.renameTo(bakFile)
			File bakFile = new File(bakDir.getAbsolutePath()+"/"+f.getName())

			}
		}
	public String createYDBGXML(Unit unit){
		String messageType = "MT3101";
		String inOutFlag;	//进出场标志, I=进场，O=出场
		String iEFlag;	//进出口标志 I=进口 E=出口
		CarrierVisit cv;
		UnitFacilityVisit ufv = unit.getUnitActiveUfvNowActive()
		if(cv == null){
			api.log("ufv is NULL!")
		}
		String vesselCode,vesselName,voyage,inOutGateTime,truckNo,freightKind;
		boolean isCorrect = true;
		if(unit.unitCategory.equals(UnitCategoryEnum.EXPORT)){
			//出口箱进场
			iEFlag = "E"
			inOutFlag = "I"
			cv = ufv.getUfvActualObCv()
			if(cv == null){
				api.log("CV is NULL!")
			}
			inOutGateTime = this.formateToXMLTime(ufv.getUfvTimeIn())
			truckNo = ufv.getUfvActualIbCv().getCvId()
			voyage = ufv.getUfvActualObCv().getCarrierObVoyNbrOrTrainId()
		} else if(unit.unitCategory.equals(UnitCategoryEnum.IMPORT)){
			//进口箱出场
			iEFlag = "I"
			inOutFlag = "O"
			cv = ufv.getUfvActualIbCv()
			if(cv == null){
				api.log("CV is NULL!")
			}
			inOutGateTime = this.formateToXMLTime(ufv.getUfvTimeOut())
			truckNo = ufv.getUfvActualObCv().getCvId()
			voyage = ufv.getUfvActualIbCv().getCarrierIbVoyNbrOrTrainId()			
		} else {
			isCorrect = false
		}
		GoodsBl goodsBl = GoodsBl.findOrCreateGoodsBl(unit)
		int blCount = 0		
		String blNbr = ufv.getUfvUnit().getUnitGoods().getGdsBlNbr()
		if(blNbr!=null&&blNbr!=""){
			blNbr = blNbr.replace("+", "")
			blCount = goodsBl.getGdsblBlGoodsBls().size()
		}
		if(isCorrect){
			String messageId = this.generateMessageId(messageType)
			this.currentMessageId = messageId
			vesselCode = cv.carrierDocumentationNbr
			vesselName = cv.getCarrierVehicleName()
			switch(ufv.getUfvUnit().unitFreightKind){
				case FreightKindEnum.FCL:
				freightKind = "F";
				break;
				case FreightKindEnum.MTY:
				freightKind = "E";
				break;
				case FreightKindEnum.LCL:
				freightKind = "L";
				break;
				default:
				freightKind = "F";
			}
			def YDBGString = new StringWriter()
			def YDBG = new MarkupBuilder(YDBGString)
			YDBG.'HarbourEDI'{
				'Head'{
					'MessageId'(messageId)
					'MessageType'(messageType)
					'SendId'(this.SENDER_ID)
					'ReceiveId'(this.RECEIVER_ID)
					'BarrierNum'(this.BARRIER_NUM)
					'SendTime'(this.getXMLTime())
				}
				'Declaration'{
					'ApplayHead'{
						'VesselCode'(vesselCode)
						'VesselName'(vesselName)
						'Voyage'(voyage)
						'Direct'("")
						'ContainerNo'(unit.unitId)
						'ContainerSize'(unit.getUnitPrimaryUe().getUeEquipment().eqEquipType.eqtypId)
						'InOutGateTime'(inOutGateTime)
						'CargoCode'("")
						'ContainerStatus'(freightKind)
						'BillNo'(blNbr)
						'BillCount'(blCount)
						'PackageNumber'("0")
						'CargoMeasure'("0")
						'GrossWeight'(ufv.getUfvUnit().getUnitGoodsAndCtrWtKg())
						'ContainerOperatorCode'(ufv.getUfvUnit().getUnitLineOperator().getBzuId())
						'SealNo'(ufv.getUfvUnit().getUnitSealNbr1())
						'TruckNo'(truckNo)
						'IEFlag'(iEFlag)
						'InOutFlag'(inOutFlag)
						'ContainerType'("")
						'CustomsSealNo'("")
						'GoodsType'("0")
					}
				}
			}
			return YDBGString.toString()
		}		
	}
	
	public File generateYDBGXMLFile(Unit unit){
		File dir = new File(this.fileDir)
		if(!dir.exists()){
			dir.mkdirs()
		}
		String xml = this.createYDBGXML(unit)
		File f = new File(fileDir + "\\" + this.currentMessageId + ".xml")
		
		def printWriter = f.newPrintWriter('UTF-8')
		String head = """<?xml version="1.0" encoding="UTF-8" ?>"""
		printWriter.append(head + '\n')
		printWriter.append(xml)
		printWriter.flush()
		printWriter.close()
		return f
	}
	
	
	public String generateMessageId(String inMessageType){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
		String ranstr = ""
		for(int i=0;i<5;i++){
			ranstr += new Random().nextInt(10).toString()
		}
		return inMessageType + "_" + sdf.format(new Date()) + "_" + ranstr
	}
	public String getXMLTime(){
		Date d = new Date()
		return formateToXMLTime(d)
	}
	public String formateToXMLTime(Date date){
		if(date==null){
			return ""
		}
		else{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")//设置日期格式
			SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss")
			return df.format(date) + 'T' + df2.format(date)// new Date()为获取当前系统时间
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
