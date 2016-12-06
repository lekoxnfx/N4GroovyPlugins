package com.n4.zs.message

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.UnitCategoryEnum
import com.navis.cargo.CargoBizMetafield
import com.navis.cargo.business.model.BillOfLading
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import groovy.sql.Sql
import groovy.xml.MarkupBuilder
import org.apache.commons.lang.StringEscapeUtils

import java.text.SimpleDateFormat

/**
 * Created by lekoxnfx on 15/11/11.
 * 用于接收处理海关MT0102报文的指令
 *
 */
/*
 <groovy class-location="database" class-name="ProcessMT0102OLD">
	 <parameters>
		<parameter id="xmlcontent" value=""/>
	 </parameters>
 </groovy>
*/

/*
resultXML:
<message-response>
    <responses response="OK" />
    <messages message="">
</message-response>
 */
/*
CUST INS = info[yyyyMMddHHmmss]
记录UNIT_UPDATE_CUST事件，追踪对应字段，记录
 */
class ProcessMT0102OLD {
    static String OK = "OK";
    static String ERROR = "ERROR";
    Map ins_map =  new HashMap<String,String>();
    Sql sql
    boolean noError = true;
    String notes = "未知错误,请检查报文格式以及内容！";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss")
    SimpleDateFormat sdfN4 = new SimpleDateFormat("yyyyMMdd HHmmss")


    GroovyApi api = new GroovyApi();
    public String execute(Map inParameters) {



        api.log("开始处理MT0102报文")
        try{
            init();

            String xmlContent = inParameters.get("xmlcontent")
            String xmlUnescaped = StringEscapeUtils.unescapeXml(xmlContent)
            api.log("xmlContent:\n\r"+xmlUnescaped)

            def RES = new XmlParser().parseText(xmlUnescaped)
            String sendTimeStr = RES.'Head'.'SendTime'.text();
            String messageId = RES.'Head'.'MessageId'.text();

            api.log("海关指令：发送时间："+sendTimeStr)


            RES.'Body'.'List'.each {u->
                String unitNbr = u.'CargoNo'.text()
                String insCode = u.'CustomInstruction'.text()
                String jouneyId = u.'JouneyId'.text()
                String blNbr = u.'BillNo'.text()
                String ieBizCode = u.'IEBizCode'.text()
                String shipUNCode = u.'ShipUnCode'.text()
                //N4数据
                String voyage,vesselCode;


                if(unitNbr==null){
                    notes = "报文中CargoNo不能为空！"
                    api.logWarn(notes)
                    noError = false
                }else{
                    if(unitNbr.equals(blNbr)){
                        //这是件杂货
                        api.log("判断是件杂货指令")
                        api.log("海关指令件杂货"+unitNbr)
                        List<BillOfLading> blList = BillOfLading.findAllBillsOfLading(blNbr)
                        BillOfLading matchedBl

                        blList.each {bl->
                            api.log("对比提单:" + bl.getBlNbr())
                            boolean isImportOrExport = true;
                            if(bl.getBlCategory().equals(UnitCategoryEnum.IMPORT)&&ieBizCode.equals("I")){//都为进口
                                //进口提单，取进口航次
                                voyage = bl.getBlCarrierVisit().getCarrierIbVoyNbrOrTrainId()
                                vesselCode = bl.getBlCarrierVisit().getCarrierDocumentationNbr()==null?"":bl.getBlCarrierVisit().getCarrierDocumentationNbr()
                                api.log("进口航次:"+voyage+" 船舶UN代码:" + vesselCode)

                                if(jouneyId.equalsIgnoreCase(voyage)){//比对航次等
                                    api.log("进口航次匹配")
                                    if(vesselCode==null||vesselCode==""){
                                        if(shipUNCode==null||shipUNCode==""){
                                            api.log("船舶UN代码均为空")
                                            String insStr = insCode + ins_map.get(insCode) + "["+sendTimeStr+"]"
                                            api.log("海关指令："+"提单号"+blNbr+"找到对应提单")
                                            matchedBl = bl
                                        }
                                    }
                                    else {
                                        if(shipUNCode!=null&&shipUNCode!=""){
                                            shipUNCode = shipUNCode.length()>2?shipUNCode[2..-1]:shipUNCode; //去除头两个字母
                                        }
                                        if(vesselCode.equalsIgnoreCase(shipUNCode)){
                                            api.log("船舶UN代码匹配")
                                            String insStr = insCode + ins_map.get(insCode) + "["+sendTimeStr+"]"
                                            api.log("海关指令："+"提单号"+blNbr+"找到对应提单")
                                            matchedBl = bl
                                        }
                                    }

                                }

                            }
                        }
                        if(matchedBl == null){

                            notes = "提单号"+blNbr+"找不到匹配的提单（船舶代码:航次）:" + shipUNCode+":"+jouneyId
                            api.logWarn(notes)
                            noError = false
                        }
                        else {
                            //判断当前指令时间
                            boolean  timeCorrect = true;
                            Date curInsDate
                            api.log("解析当前指令发送时间")
                            try{
                                curInsDate = sdf.parse(sendTimeStr)
                            }catch (Exception e){
                                notes = "解析当前指令时间失败,报文解析出错,时间值:"+ sendTimeStr
                                api.log(notes)
                                timeCorrect = false
                                noError = false
                            }
                            if(noError){
                                Date oriInsDate
                                try{
                                    String oriInsCodeStr = matchedBl.getBlNotes()
                                    String oriSendTimeStr = oriInsCodeStr.substring(oriInsCodeStr.lastIndexOf("[")+1, oriInsCodeStr.lastIndexOf("]"))
                                    api.log("海关指令：箱号"+unitNbr+"旧指令的发送时间:"+oriSendTimeStr)
                                    //原指令时间与现指令时间
                                    oriInsDate = sdfN4.parse(oriSendTimeStr)
                                    if(oriInsDate<=curInsDate){
                                        api.log("海关指令：箱号"+unitNbr+"当前指令比旧指令晚，将会更新")
                                    }
                                    else{
                                        notes = "海关指令：箱号"+unitNbr+"当前指令比旧指令早，将不会更新"
                                        api.log(notes)
                                        timeCorrect = false
                                    }

                                }catch(Exception e){
                                    api.log("海关指令：箱号"+unitNbr+"没有旧指令的时间标志，或解析失败")
                                }
                                if(timeCorrect){//
                                    String insStr = insCode + ins_map.get(insCode) + "["+sdfN4.format(curInsDate)+"]" + "(" + messageId + ")"
                                    matchedBl.blNotes = insStr
                                    api.log("海关指令："+"箱号"+unitNbr+"收到海关指令" + insStr + "["+sendTimeStr+"]" + "(" + messageId + ")")

                                }
                            }
                        }




                    }
                    else {
                        //这是集装箱
                        api.log("判断是集装箱指令")
                        api.log("海关指令集装箱"+unitNbr)
                        String sqlStr = """
							select gkey,active_ufv from inv_unit where   VISIT_STATE = '1ACTIVE' and id = '${unitNbr}'
						"""
                        int count = 0
                        long gkey,ufv_gkey;
                        sql.eachRow(sqlStr) {row->
                            count++
                            gkey = row.'gkey'
                            ufv_gkey = row.'active_ufv'
                            api.log("找到记录:Gkey:" + gkey + ",active_ufv:" + ufv_gkey)
                        }
                        if(count==1){
                            Unit unit = Unit.hydrate(gkey)
                            UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufv_gkey)

                            boolean isIETUnit = true;
                            if((unit.getUnitCategory().equals(UnitCategoryEnum.EXPORT)||unit.getUnitCategory().equals(UnitCategoryEnum.TRANSSHIP)&&ieBizCode.equals("E"))){
                                //中转或出口箱，取出口航次
                                api.log("中转或出口箱，取出口航次")
                                voyage = ufv.getUfvActualObCv().getCarrierObVoyNbrOrTrainId()
                                vesselCode = ufv.getUfvActualObCv().getCarrierDocumentationNbr()
                            }else if(unit.getUnitCategory().equals(UnitCategoryEnum.IMPORT)&&ieBizCode.equals("I")){
                                //进口箱，取进口航次
                                api.log("进口箱，取进口航次")
                                voyage = ufv.getUfvActualIbCv().getCarrierIbVoyNbrOrTrainId()
                                vesselCode = ufv.getUfvActualIbCv().getCarrierDocumentationNbr()
                            }else{
                                notes = "箱号"+unitNbr+"不是进口或者出口/中转"
                                noError = false
                                isIETUnit = false
                                api.log(notes)
                            }
                            if(isIETUnit){
                                //判断当前指令时间
                                boolean timeCorrect = true
                                Date curInsDate
                                api.log("解析当前指令发送时间")
                                try{
                                    curInsDate = sdf.parse(sendTimeStr)
                                }catch (Exception e){
                                    notes = "解析当前指令时间失败,报文解析出错,时间值:"+ sendTimeStr
                                    api.log(notes)
                                    timeCorrect = false
                                    noError = false
                                }
                                if(noError){
                                    try{
                                        String oriInsCodeStr = unit.getUnitFlexString01()
                                        api.log("旧指令：" + oriInsCodeStr)
                                        String oriSendTimeStr = oriInsCodeStr.substring(oriInsCodeStr.lastIndexOf("[")+1, oriInsCodeStr.lastIndexOf("]"))
                                        api.log("海关指令：箱号"+unitNbr+"旧指令的发送时间:"+oriSendTimeStr)
                                        //原指令时间与现指令时间
                                        Date oriInsDate = sdfN4.parse(oriSendTimeStr)
                                        if(oriInsDate<=curInsDate){
                                            api.log("海关指令：箱号"+unitNbr+"当前指令比旧指令晚，将会更新")
                                        }
                                        else{
                                            notes = "海关指令：箱号"+unitNbr+"当前指令比旧指令早，将不会更新"
                                            api.log(notes)
                                            timeCorrect = false
                                        }
                                    }catch(Exception e){
                                        api.log("海关指令：箱号"+unitNbr+"没有旧指令的时间标志，或解析失败")
                                    }
                                    if(timeCorrect){
                                        boolean  vesCodeMatch = false
                                        if(vesselCode==null||vesselCode==""){
                                            if(shipUNCode==null||shipUNCode==""){
                                                api.log("船舶UN代码均为空")
                                                vesCodeMatch = true
                                            }
                                        }
                                        else{
                                            if(shipUNCode!=null&&shipUNCode!=""){
                                                shipUNCode = shipUNCode.length()>2?shipUNCode[2..-1]:shipUNCode; //去除头两个字母
                                            }
                                            if(vesselCode.equalsIgnoreCase(shipUNCode)){
                                                vesCodeMatch = true
                                                api.log("船舶UN代码比对:" + vesCodeMatch.toString())
                                            }
                                        }

                                        if(jouneyId.equalsIgnoreCase(voyage)&&vesCodeMatch){//比对航次
                                            String insStr = insCode + ins_map.get(insCode) + "["+sdfN4.format(curInsDate)+"]" + "(" + messageId + ")"
                                            unit.setUnitFlexString01(insStr)
                                            api.log("海关指令："+"箱号"+unitNbr+"收到海关指令" + insStr + "["+sendTimeStr+"]" + "(" + messageId + ")")

                                        }
                                        else{
                                            notes = "海关指令："+"箱号"+unitNbr+"对应船名航次不匹配，收到：" + shipUNCode+":"+jouneyId+",系统："+vesselCode+":"+voyage
                                            api.log(notes)
                                            noError = false
                                        }

                                    }
                                }

                            }




                        }
                        else{
                            notes = "海关指令："+"箱号"+unitNbr+"有"+count+"个活动记录！"
                            api.logWarn(notes)
                            noError = false
                        }
                    }
                }

            }
        }
        catch (Exception e){
            noError = false
            notes = e.toString()
        }
        finally {
            if (noError){
                return  generateResult(this.OK,"指令接收成功")
            }
            else {
                return generateResult(this.ERROR,notes)
            }
        }


    }
    public String generateResult(String response,String message){
        def xmlStringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(xmlStringWriter)
        xmlBuilder.'message-ressponse'{
            'responses'('response':response)
            'messages'('message':message)
        }
        api.log("生成回复内容:\n\r" + xmlStringWriter.toString())
        return  xmlStringWriter.toString()
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

        try{
            String DB = "jdbc:oracle:thin:@" + "192.168.50.32" + ":" + "1521" + ":" + "n4"
            String USER = "n4user"
            String PASSWORD = "n4dlt"
            String DRIVER = 'oracle.jdbc.driver.OracleDriver'
            sql = Sql.newInstance(DB, USER, PASSWORD, DRIVER)
        }catch(Exception e){
            noError = false
            notes = e.toString()
            api.logWarn(notes)
        }
    }
}
