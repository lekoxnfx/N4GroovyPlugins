package com.n4.zs.message

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.model.*
import com.navis.argo.business.reference.UnLocCode
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.reference.business.locale.RefCountry
import com.navis.services.business.event.GroovyEvent
import com.navis.vessel.business.schedule.VesselVisitDetails
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

class GenerateMT9901ByVV {


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
    List<UnitFacilityVisit> ufvList = new ArrayList<UnitFacilityVisit>();//装卸箱列表

    public void execute(GroovyEvent inEvent,GroovyApi inApi,Map<String,String> inParas ){
        this.event = inEvent
        this.api = inApi
        init();

        VesselVisitDetails vvd = (VesselVisitDetails)event.getEntity();




//        unit.setUnitFlexString01("准备发送MT9901报文")

        api.log("开始生成MT9901报文(装卸)")

        try{
            String ioType = inParas.get("ioType");

            if(ioType!=null){
                switch(ioType.toUpperCase()){

                    case "VD":
                        api.log("类型：VD（卸船）")
                        break;
                    case "VL":
                        api.log("类型：VL（装船）")
                        break
                    default:
                        isSuccess = false
                        notes = "ioType参数不正确"
                }
                //获取字符串
                String xmlString = createMT9901RequestXmlByVV(vvd,ioType)
                String paras = "className=SendJCJGMT9901&servletXmlStr="+xmlString
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
                ufvList.each {ufv->
                    ufv.getUfvUnit().setUnitFlexString01("发送MT9901(装卸船)成功！" + notes)
                }
                api.log(notes)
            }
            else {
                ufvList.each {ufv->
                    ufv.getUfvUnit().setUnitFlexString01("发送MT9901(装卸船)失败！" + notes)
                }
                api.log(notes)
            }
        }




    }
    public String createMT9901RequestXmlByVV(VesselVisitDetails vvd,String ioType){
        long cvgkey = vvd.getCvdCv().getCvGkey()

        api.log("开始创建xml请求字符串")
        //CV参数
        String transportTypeCodeStr = '1' //运输工具代码
        String iEPortTypeCodeStr = ''
        String transportNameStr = ''
        String jouneyIdStr = ''
        String shipUnCodeStr = ''
        String unloadingStartDateStr = ''
        String unloadingEndDateStr = ''



        //船信息
        transportNameStr = vvd.getCarrierVehicleName()
        String vesselCode = vvd.getCarrierDocumentationNbr()
        String vesselCountry = vvd.getCarrierCountryName();
        api.log("船舶国籍:" + vesselCountry)
        if(vesselCountry!=null){
            if(vesselCountry.toUpperCase()!="CHINA"){
                vesselCode = "UN" + vesselCode
            }
            else{
                vesselCode = "CN" + vesselCode
            }
        }
        shipUnCodeStr = vesselCode
        try{
            unloadingStartDateStr = formatToDayTime(vvd.getCvdCv().getCvATA())
        }catch (Exception e){
            unloadingStartDateStr = ''
        }
        try{
            unloadingEndDateStr = formatToDayTime(vvd.getCvdCv().getCvATD())
        }catch (Exception e){
            unloadingEndDateStr = ''
        }


        //Unit 信息
        String unitQueryStr;
        switch(ioType.toUpperCase()){

            case "VD"://卸船
                iEPortTypeCodeStr = '1' // 船信息
                jouneyIdStr = vvd.getVvdIbVygNbr() //进口航次
                unitQueryStr = """
				select ufv.gkey from inv_unit_fcy_visit ufv, inv_unit u where ufv.actual_ib_cv = '${cvgkey}' and  ufv.unit_gkey = u.gkey and u.freight_kind in ('FCL','LCL','MTY')
				"""
                break;
            case "VL"://装船
                iEPortTypeCodeStr = '2' // 船信息
                jouneyIdStr = vvd.getVvdObVygNbr() //出口航次
                unitQueryStr = """
				select ufv.gkey from inv_unit_fcy_visit ufv, inv_unit u where ufv.actual_ob_cv = '${cvgkey}' and ufv.unit_gkey = u.gkey and u.freight_kind in ('FCL','LCL','MTY')
				"""
                break
        }


        //数据库查询
        sql.eachRow(unitQueryStr) {r->
            long ufv_gkey = r.'gkey'
            UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufv_gkey)
            try{
                UnLocCode unLocCode = null;
                switch(ioType.toUpperCase()) {
                    case "VD"://卸船
                        unLocCode = ufv.getUfvUnit().getUnitRouting().getRtgPOL().getPointUnLoc()
                        break;
                    case "VL"://装船
                        unLocCode = ufv.getUfvUnit().getUnitRouting().getRtgPOD1().getPointUnLoc()
                        break;
                }
                if(ufv.getUfvUnit().getUnitFreightKind().equals(UnitFreight))
                if(!unLocCode.getUnlocCntry().equals(RefCountry.findCountry("CN"))){
                    ufvList.add(ufv)
                }
            }
            catch(Exception e){
                api.log("获取装/卸港信息失败，不计入出口箱")
//                ufvList.add(ufv)
            }
        }

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
                ufvList.each {ufv->
                    //Unit参数
                    String documentNumStr = ''
                    String goodsTypeStr = '0'
                    String containerNumStr = ''
                    String fullnessCodeStr = ''
                    String lockNumStr = ''
                    String quantityStr = ''
                    String grossWeightStr = ''

                    Unit unit = ufv.getUfvUnit()
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
