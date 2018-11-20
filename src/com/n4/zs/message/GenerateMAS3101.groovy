package com.n4.zs.message

import com.navis.argo.business.api.GroovyApi

/*
 * 用于向海关发送出口重箱抵港报文
 * 类型：Manifest_Arrival_Ship_3101_2
 */
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.model.CarrierVisit
import com.navis.argo.business.model.Complex
import com.navis.argo.business.model.Facility
import com.navis.argo.business.model.Operator
import com.navis.cargo.business.model.GoodsBl
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.event.GroovyEvent
import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat

class GenerateMAS3101 extends GroovyApi {
    public static String SENDER_ID = "ZSDND"
    public static String RECEIVER_ID = "0000"
    public static String OFFICE_ID = "2904" //属地海关

    public Map<String,String> ISO_TO_HGCODE

    public Operator operator = Operator.findOperator("ZSCT")
    public Complex complex = Complex.findComplex("ZST", operator)
    public Facility facility = Facility.findFacility("DLT", complex)

    public currentMessageId;
    public String fileDir = "D:\\MESSAGE\\出口抵港"
    public String fileDirBak = "D:\\MESSAGE\\Backup\\出口抵港"
    public GroovyEvent event
    public GroovyApi api
    public String sendTimeStr;
    public String messageType;
    public String version = "1.0"
    public String versionInFileName = "1p0"

    public void execute(GroovyEvent inEvent,GroovyApi inApi){
        init();
        this.event = inEvent
        this.api = inApi
        Unit unit = (Unit)event.getEntity()
        api.log("开始生成出口抵港报文,Unit ID:" + unit.getUnitId())
        File f = generateYDBGXMLFile(unit)
		if(sendFileToFtp(f)){
			File bakDir = new File(this.fileDirBak)
			if(!bakDir.exists()){
				bakDir.mkdirs()
			}
            File bakFile = new File(bakDir.getAbsolutePath()+"/"+f.getName())

            f.renameTo(bakFile)
            api.log("已发送文件:"+f.getName())
            String str = "已发送，文件名为" + f.getName()
            unit.setUnitFlexString01(str)
			}
    }
    public String createYDBGXML(Unit unit){
        messageType = "MT3101";
        sendTimeStr = getXMLTime()
        //获取相关信息
        CarrierVisit cv;
        UnitFacilityVisit ufv = unit.getUnitActiveUfvNowActive()
        if(ufv == null){
            api.log("ufv is NULL!")
        }
        String vesselCode,vesselName,voyage,inGateTime,characteristicCode,fullLessCode;

        cv = ufv.getUfvActualObCv()
        if(cv == null){
            api.log("CV is NULL!")
        }


        if(ufv.getUfvTimeIn()!=null){
            inGateTime = this.formatToDayTime(ufv.getUfvTimeIn())
        }
        else{
            inGateTime = this.formatToDayTime(new Date())
        }

        voyage = ufv.getUfvActualObCv().getCarrierObVoyNbrOrTrainId()

        GoodsBl goodsBl = GoodsBl.findOrCreateGoodsBl(unit)
        int blCount = 0
        String blNbr = ufv.getUfvUnit().getUnitGoods().getGdsBlNbr()
        if(blNbr!=null&&blNbr!=""){
            blNbr = blNbr.replace("+", "")
            blCount = goodsBl.getGdsblBlGoodsBls().size()
        }

        String messageId = this.generateMessageId(messageType)
        this.currentMessageId = messageId
        vesselCode = cv.carrierDocumentationNbr
        String vesselCountry = cv.getCvCvd().getCarrierCountryName();
        api.log("船舶国籍:" + vesselCountry)
//        if(vesselCountry!=null){
//            if(vesselCountry.toUpperCase()!="CHINA"){
//                vesselCode = "UN" + vesselCode
//            }
//            else{
//                vesselCode = "CN" + vesselCode
//            }
//        }
        vesselCode = "UN" + vesselCode
        vesselName = cv.getCarrierVehicleName()
        switch(ufv.getUfvUnit().unitFreightKind){
            case FreightKindEnum.FCL:
                fullLessCode = "8";
                break;
            case FreightKindEnum.MTY:
                fullLessCode = "4";
                break;
            case FreightKindEnum.LCL:
                fullLessCode = "7";
                break;
            default:
                fullLessCode = "";
        }
        String unit_iso_code = unit.getUnitPrimaryUe().getUeEquipment().eqEquipType.eqtypId

        characteristicCode = this.ISO_TO_HGCODE.get(unit_iso_code,unit_iso_code)

        def xmlStringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(xmlStringWriter)
        xmlBuilder.'Manifest'(
                'xmlns':'urn:Declaration:datamodel:standard:CN:MT3101:1',
                'xmlns:Declaration':'Declaration',
                'xmlns:Head':'Head',
                'xmlns:Message':'Message',
                'xmlns:xdb':'http://xmlns.oracle.com/xdb',
                'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
                'xsi:schemaLocation':'urn:Declaration:datamodel:standard:CN:MT3101:1 Manifest_Arrival_Ship_3101_2.xsd'
        )
                {
                    'Head'{
                        'MessageID'(messageId)
                        'FunctionCode'("2")
                        'MessageType'(messageType)
                        'SenderID'(this.SENDER_ID) //"_SP05490001"
                        'ReceiverID'(this.RECEIVER_ID)
                        'SendTime'(this.sendTimeStr)
                        'Version'("1.0")
                    }
                    'Declaration'{
                        'DeclarationOfficeID'(this.OFFICE_ID)
                        'BorderTransportMeans'{
                            'JourneyID'(voyage)
                            'TypeCode'("1")
                            'ID'(vesselCode)
                            'Name'(vesselName)
                        }
                        'UnloadingLocation'{
                            'ID'("CNZOS/"+ OFFICE_ID)
                            'ArrivalDate'(inGateTime)
                        }
                        'TransportEquipment'{
                            'EquipmentIdentification'{
                                'ID'(unit.getUnitId())
                            }
                            'CharacteristicCode'(characteristicCode)
                            'FullnessCode'(fullLessCode)
                        }
                    }
                }
        return xmlStringWriter.toString()
    }

    public File generateYDBGXMLFile(Unit unit){
        File dir = new File(this.fileDir)
        if(!dir.exists()){
            dir.mkdirs()
        }
        api.log("创建XML...")

        String xml = this.createYDBGXML(unit)
        api.log(xml)
        File f = new File(fileDir + "\\" + this.currentMessageId + ".xml")
//        while (f.exists()){
//            f = new File(fileDir + "\\" + this.currentMessageId + ".xml")
//        }

        def printWriter = f.newPrintWriter('UTF-8')
        String head = """<?xml version="1.0" encoding="UTF-8" ?>"""

        printWriter.append(head + '\n')
        printWriter.append(xml)
        printWriter.flush()
        printWriter.close()
        return f
    }


    public String generateMessageId(String inMessageType){
        return "CN_"+inMessageType + "_" + this.versionInFileName + "_" + this.SENDER_ID + "_" + this.sendTimeStr
    }
    public static synchronized String getXMLTime(){
        try{
            Thread.sleep(500)
        }catch(Exception e){
        }
        finally {
            Date d = new Date()
            return formatToXMLTime(d)
        }

    }
    public static String formatToXMLTime(Date date){
        if(date==null){
            return ""
        }
        else{
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS")//设置日期格式
            return df.format(date)// new Date()为获取当前系统时间
        }

    }
    public String formatToDayTime(Date date){
        if(date==null){
            return ""
        }
        else{
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd")//设置日期格式
            return df.format(date)// new Date()为获取当前系统时间
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
    public void init(){
        ISO_TO_HGCODE = new HashMap<String,String>()
        ISO_TO_HGCODE.put("2200", "22G1")
        ISO_TO_HGCODE.put("2530", "25R1")
        ISO_TO_HGCODE.put("4200", "42G1")
        ISO_TO_HGCODE.put("4500", "45G1")
        ISO_TO_HGCODE.put("4530", "45R1")

    }
}
