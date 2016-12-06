package com.n4.zs.message

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
import com.navis.vessel.VesselBizMetafield
import com.navis.vessel.business.schedule.VesselVisitDetails
import groovy.sql.Sql
import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat

/**
 * Created by liuminhang on 16/6/16.
 */
/*
<?xml version="1.0" encoding="UTF-8" ?>
<Message  version="1.0.0">
	<Head>
		<MessageId>MT9801_111111111_2904_20150811100951</MessageId>
		<MessageType>MT9801</MessageType>
		<SendId>111111111</SendId>
		<ReceiveId>2904</ReceiveId>
		<SendTime>20150711100951</SendTime>
		<Obligate></Obligate>
	</Head>
	<Body>
		<List>
			<VesselCode>FC0005899</VesselCode>
			<VesselEName>LIANHE 16</VesselEName>
			<VesselCName>联合16</VesselCName>
			<InVoyage>1106</InVoyage>
			<OutVoyage>1107</OutVoyage>
			<TradeType>W</TradeType>
			<OperatorCode>UCM</OperatorCode>
			<Operator>胡小</Operator>
			<ArrivedPortTime>20150710100951</ArrivedPortTime>
			<ETAArrivedTime>20150710100951</ETAArrivedTime>
			<ActualArrivedTime>20150710100951</ActualArrivedTime>
			<ETASailingTime>20150710100951</ETASailingTime>
			<ActualSailingTime>20150710100951</ActualSailingTime>
			<Obligate></Obligate>
		</List>
		<List>
			<VesselCode>FC0005819</VesselCode>
			<VesselEName>LIANHE 26</VesselEName>
			<VesselCName>联合26</VesselCName>
			<InVoyage>1206</InVoyage>
			<OutVoyage></OutVoyage>
			<TradeType>W</TradeType>
			<OperatorCode>UCM</OperatorCode>
			<Operator>胡小</Operator>
			<ArrivedPortTime>20150710100951</ArrivedPortTime>
			<ETAArrivedTime>20150710100951</ETAArrivedTime>
			<ActualArrivedTime></ActualArrivedTime>
			<ETASailingTime></ETASailingTime>
			<ActualSailingTime></ActualSailingTime>
			<Obligate></Obligate>
		</List>
	</Body>
</Message>
 */
/*
MessageId	报文唯一编号	字符类型	64	非空	参见：3.1.4 报文唯一编号规范
MessageType	业务申报类型	字符类型	6	非空	参见：4.1.3 业务申报类别代码表
SendId	企业组织机构代码	字符类型	9	非空	企业的组织机构代码(9位)
ReceiveId	接收海关代码	字符类型	4	非空	参考：4.1.1 海关代码表
SendTime	创建报文时间	字符类型	14	非空	时间格式：yyyyMMddHHmmss（精确到秒）
VesselCode	船舶代码(UN代码)	字符类型	64	非空	船舶代码(UN代码)
VesselEName	英文船名	字符类型	64	非空	英文船名
VesselCName	中文船名	字符类型	64		中文船名
InVoyage	进口航次	字符类型	32		进口航次
OutVoyage	出口航次	字符类型	32		出口航次
TradeType	贸易类型	字符类型	1	非空	参考：4.1.2 贸易类型代码表
OperatorCode	船舶经营人代码	字符类型	32		船舶经营人代码
Operator	船舶经营人	字符类型	64		船舶经营人
ArrivedPortTime	抵港时间	字符类型	14		时间格式：yyyyMMddHHmmss（精确到秒）
ETAArrivedTime	预计靠泊时间	字符类型	14		时间格式：yyyyMMddHHmmss（精确到秒）
ETASailingTime	预计离泊时间	字符类型	14		时间格式：yyyyMMddHHmmss（精确到秒）
ActualArrivedTime	实际靠泊时间	字符类型	14		时间格式：yyyyMMddHHmmss（精确到秒）
ActualSailingTime	实际离泊时间	字符类型	14		时间格式：yyyyMMddHHmmss（精确到秒）
Obligate	预留字段	字符类型	256		预留字段
 */
class GenerateMT9801 {

    public Operator operator = Operator.findOperator("ZSCT")
    public Complex complex = Complex.findComplex("ZST", operator)
    public Facility facility = Facility.findFacility("DLT", complex)
    public Yard yard = Yard.findYard("DLT",facility)

    public GroovyEvent event
    public GroovyApi api
    Sql sql;

    String messageType = "MT9801"


    boolean isSuccess = true;
    String notes;

    public void execute(GroovyEvent inEvent,GroovyApi inApi) {
        this.event = inEvent
        this.api = inApi

        VesselVisitDetails vvd = (VesselVisitDetails)event.getEntity();

        api.log("开始生成MT9801报文(船期)")
        boolean isSuccess = true;

        try{


            //获取字符串
            String xmlString = createMT9901RequestXml(vvd)
            String paras = "className=SendCQMT9801&servletXmlStr="+xmlString
            api.log("url参数:"+paras)
            //发送请求
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
        catch(Exception e){
            isSuccess = false
            notes = e.toString()
            api.logWarn(notes)
        }
        finally {
            if(isSuccess){
//                vvd.setFieldValue(VesselBizMetafield.VV_FLEX_STRING01,("发送MT9801(船期)成功！" + notes))
                api.log(notes)
            }
            else {
//                vvd.setFieldValue(VesselBizMetafield.VV_FLEX_STRING01,("发送MT9801(船期)失败！" + notes))
                api.log(notes)
            }
        }





    }
    public String createMT9901RequestXml(VesselVisitDetails vvd){

        api.log("开始创建xml请求字符串")
        //CV参数
        String msgVesselCode = ""
        String msgVesselEName = ""
		String msgVesselCName = ""
        String msgInVoyage = ""
		String msgOutVoyage = ""
        String msgTradeType = ""
        String msgOperatorCode = ""
        String msgOperator = ""
        String msgArrivedPortTime = ""
        String msgETAArrivedTime = ""
        String msgActualArrivedTime = ""
        String msgETASailingTime = ""
        String msgActualSailingTime = ""
        String msgObligate = ""


       api.log("获取船期信息")
        String vesselCountry = vvd.getCarrierCountryName();
        String vesselCode = vvd.getCarrierDocumentationNbr()
        api.log("船舶国籍:" + vesselCountry)
        if(vesselCountry!=null){
            if(vesselCountry.toUpperCase()!="CHINA"){
                vesselCode = "UN" + vesselCode
            }
            else{
                vesselCode = "CN" + vesselCode
            }
        }
        msgVesselCode = vesselCode
        msgVesselEName = vvd.getCarrierVehicleName()
        msgVesselCName = ""
        msgInVoyage = vvd.getVvdIbVygNbr()
        msgOutVoyage = vvd.getVvdObVygNbr()
        msgTradeType = "W"
        msgOperatorCode = vvd.getCvdCv().getCvOperator().getBzuId()
//        msgOperator = vvd.getCvdCv().getCvOperator().getBzuName()
        msgArrivedPortTime = formatToDayTime(vvd.getCvdCv().getCvATA())
        msgETAArrivedTime = formatToDayTime(vvd.getCvdETA())
        msgActualArrivedTime = formatToDayTime(vvd.getCvdCv().getCvATA())
        msgETASailingTime = formatToDayTime(vvd.getCvdETD())
        msgActualSailingTime = formatToDayTime(vvd.getCvdCv().getCvATD())

        def xmlStringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(xmlStringWriter)
        xmlBuilder.'Body'{
            'List'{
                'VesselCode'(msgVesselCode)
                'VesselEName'(msgVesselEName)
                'VesselCName'(msgVesselCName)
                'InVoyage'(msgInVoyage)
                'OutVoyage'(msgOutVoyage)
                'TradeType'(msgTradeType)
                'OperatorCode'(msgOperatorCode)
                'Operator'(msgOperator)
                'ArrivedPortTime'(msgArrivedPortTime)
                'ETAArrivedTime'(msgETAArrivedTime)
                'ActualArrivedTime'(msgActualArrivedTime)
                'ETASailingTime'(msgETASailingTime)
                'ActualSailingTime'(msgActualSailingTime)
                'Obligate'('')

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
}
