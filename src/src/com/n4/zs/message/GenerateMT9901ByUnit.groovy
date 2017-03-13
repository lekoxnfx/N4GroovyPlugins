package src.com.n4.zs.message

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.model.CarrierVisit
import com.navis.argo.business.model.Complex
import com.navis.argo.business.model.Facility
import com.navis.argo.business.model.Operator
import com.navis.argo.business.model.Yard

import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.event.GroovyEvent
import groovy.sql.Sql
import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat

/**
 * Created by lekoxnfx on 15/11/4.
 */
/*
<Declaration>
		<ApplyHead>
			<TransportTypeCode>2</TransportTypeCode>
			<IEPortTypeCode>1</IEPortTypeCode>
			<TransportName>ABCD</TransportName>
			<JouneyId>123A</JouneyId>
			<ShipUnCode>UN070401</ShipUnCode>
			<UnloadingStartDate>20110611035046</UnloadingStartDate>
			<UnloadingEndDate>20110611064528</UnloadingEndDate>
			<Obligate></Obligate>
			<ApplyList>
				<DocumentNum>861977728</DocumentNum>
				<GoodsType>0</GoodsType>
				<ContainerNum>XXXU0816031</ContainerNum>
				<FullnessCode>2</FullnessCode>
				<LockNum></LockNum>
				<Quantity>0</Quantity>
				<GrossWeight>0</GrossWeight>
				<Obligate1></Obligate1>
				<Obligate2></Obligate2>
			</ApplyList>
			<ApplyList>
				<DocumentNum>861977729</DocumentNum>
				<GoodsType>0</GoodsType>
				<ContainerNum>XXXU0816032</ContainerNum>
				<FullnessCode>2</FullnessCode>
				<LockNum></LockNum>
				<Quantity>0</Quantity>
				<GrossWeight>0</GrossWeight>
				<Obligate1></Obligate1>
				<Obligate2></Obligate2>
			</ApplyList>
		</ApplyHead>
 */

class GenerateMT9901ByUnit {


    public Operator operator = Operator.findOperator("ZSCT")
    public Complex complex = Complex.findComplex("ZST", operator)
    public Facility facility = Facility.findFacility("DLT", complex)
    public Yard yard = Yard.findYard("DLT",facility)

    public GroovyEvent event
    public GroovyApi api
    Sql sql;

    String messageType = "MT9901"


    boolean isSuccess = true;
    String notes;

    public void execute(GroovyEvent inEvent,GroovyApi inApi,Map<String,String> inParas ){
        this.event = inEvent
        this.api = inApi
        Unit unit = (Unit)event.getEntity()
        unit.setUnitFlexString01("准备发送MT9901报文（单箱）")

        api.log("开始生成MT9901报文")

        try{
            String ioType = inParas.get("ioType");

            if(ioType!=null){
                switch(ioType.toUpperCase()){

                    case "TI":
                        api.log("类型：TI（卡车进门）")
                        break;
                    case "TO":
                        api.log("类型：TO（卡车出门）")
                        break
                    default:
                        isSuccess = false
                        notes = "ioType参数不正确"
                }
                //获取字符串
                String xmlString = createMT9901RequestXmlByUnit(unit,ioType)
                String paras = "className=SendJCJGMT9901&servletXmlStr="+xmlString
                api.log("url参数:"+paras)
                //发送请求
                //获取时间间隔
                api.log("制造时间间隔")

                api.log("间隔后时间:" + formatToDayTime(new Date()))
                api.log("发送POST请求")
                String response = api.getGroovyClassInstance("SEDIHttpClient").sendPost(paras)
                api.log("response:" + response)
                if(response.contains(":")){
                    String[] result = response.split(":")
                    if(result[0].equals("MS_SENT")){
                        isSuccess = true
                        notes = response
                    }
                    else{
                        isSuccess = false
                        notes = response
                    }
                }
                else {
                    isSuccess = false
                    notes = response
                }
            }
            else {
                isSuccess = false
                notes = "type参数为空"
            }

        }
        catch(Exception e){
            isSuccess = false
            notes = e.toString()
            api.logWarn(notes)
        }
        finally {
            if(isSuccess){
                unit.setUnitFlexString01("发送MT9901(进出门)成功！" + notes)
                api.log(notes)
            }
            else {
                unit.setUnitFlexString01("发送MT9901(进出门)失败！" + notes)
                api.logWarn(notes)
            }
        }




    }
    public String createMT9901RequestXmlByUnit(Unit unit,String ioType){

        api.log("开始创建xml请求字符串")
        //CV参数
        String transportTypeCodeStr = '4' //运输工具代码
        String iEPortTypeCodeStr = ''
        String transportNameStr = ''
        String jouneyIdStr = ''
        String shipUnCodeStr = ''
        String unloadingStartDateStr = ''
        String unloadingEndDateStr = ''

        //Unit参数
        String documentNumStr = ''
        String goodsTypeStr = '0'
        String containerNumStr = ''
        String fullnessCodeStr = ''
        String lockNumStr = ''
        String quantityStr = ''
        String grossWeightStr = ''


        //获取卡车相关信息
        api.log("获取卡车信息")
        CarrierVisit cv;
        long unitActiveUfvKey = unit.getFieldLong("unitActiveUfv")
        UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(unitActiveUfvKey)


        if(ufv == null){
            api.log("ufv is NULL!")
        }

        cv = ufv.getUfvActualObCv()
        if(cv == null){
            api.log("CV is NULL!")
        }

        switch(ioType.toUpperCase()){

            case "TI"://进门
                iEPortTypeCodeStr = '1'
                transportNameStr = ufv.getUfvActualIbCv().getCarrierIbVoyNbrOrTrainId()
                break;
            case "TO"://出门
                iEPortTypeCodeStr = '2'
                transportNameStr = ufv.getUfvActualObCv().getCarrierObVoyNbrOrTrainId()
                break
        }


        containerNumStr = unit.getUnitId()

        unloadingStartDateStr = formatToDayTime(ufv.getUfvTimeIn())
        unloadingEndDateStr = formatToDayTime(ufv.getUfvTimeIn())

        String blNbr = ufv.getUfvUnit().getUnitGoods().getGdsBlNbr()
        if(blNbr!=null&&blNbr!=""){
            documentNumStr = blNbr.replace("+", "")
        }


        switch(ufv.getUfvUnit().unitFreightKind){
            case FreightKindEnum.FCL:
                fullnessCodeStr = "2";
                break;
            case FreightKindEnum.MTY:
                fullnessCodeStr = "1";
                break;
            case FreightKindEnum.LCL:
                fullnessCodeStr = "2";
                break;
            default:
                fullnessCodeStr = "";
        }

        grossWeightStr = unit.getUnitGoodsAndCtrWtKg()


        def xmlStringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(xmlStringWriter)
        xmlBuilder.'Declaration'{
            'ApplyHead'{
                'TransportTypeCode'(transportTypeCodeStr)
                'IEPortTypeCode'(iEPortTypeCodeStr)
                'TransportName'(transportNameStr)
                'JouneyId'(jouneyIdStr)
                'ShipUnCode'(shipUnCodeStr)
                'UnloadingStartDate'(unloadingStartDateStr)
                'UnloadingEndDate'(unloadingEndDateStr)
                'Obligate'('')
                'ApplyList'{
                    'DocumentNum'(documentNumStr)
                    'GoodsType'(goodsTypeStr)
                    'ContainerNum'(containerNumStr)
                    'FullnessCode'(fullnessCodeStr)
                    'LockNum'(lockNumStr)
                    'Quantity'(quantityStr)
                    'GrossWeight'(grossWeightStr)
                    'Obligate1'('')
                    'Obligate2'('')
                }

            }
        }

        return  xmlStringWriter.toString()
    }



    public String formatToDayTime(Date date){
        if(date==null){
            return ""
        }
        else{
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss")//设置日期格式
            return df.format(date)// new Date()为获取当前系统时间
        }
    }

    public void init(){
        try{

            String DB = "jdbc:oracle:thin:@" + "192.168.50.32" + ":" + "1521" + ":" + "n4"
            String USER = "n4user"
            String PASSWORD = "n4dlt"
            String DRIVER = 'oracle.jdbc.driver.OracleDriver'
            sql = Sql.newInstance(DB, USER, PASSWORD, DRIVER)
        }catch(Exception e){
            isSuccess = false;
            notes = e.toString()
            api.logWarn(notes)
        }
    }

}
