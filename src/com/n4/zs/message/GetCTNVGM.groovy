package com.n4.zs.message

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.api.ServicesManager
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.atoms.LocTypeEnum
import com.navis.argo.business.atoms.UnitCategoryEnum
import com.navis.argo.business.model.CarrierVisit
import com.navis.argo.business.model.Complex
import com.navis.argo.business.model.Facility
import com.navis.argo.business.model.Operator
import com.navis.argo.business.model.Yard
import com.navis.framework.business.Roastery
import com.navis.framework.portal.FieldChanges
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.inventory.InventoryEntity
import com.navis.inventory.InventoryField
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.atoms.UnitVisitStateEnum
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import groovy.sql.Sql

import java.text.SimpleDateFormat

/**
 * Created by liuminhang on 16/6/20.
 */
class GetCTNVGM {
    public Operator operator = Operator.findOperator("ZSCT")
    public Complex complex = Complex.findComplex("ZST", operator)
    public Facility facility = Facility.findFacility("DLT", complex)
    public Yard yard = Yard.findYard("DLT",facility)

    GroovyApi api = new GroovyApi();
    public String fileDir = "D:\\MESSAGE\\CTNVGM"
    public String fileDirBak = "D:\\MESSAGE\\Backup\\CTNVGM"
    String note;

    public void execute(Map args){
        init();
        api.log("CTNVGM：开始收取CTNVGM")
        try{
            List<File> list_f;
            list_f = api.getGroovyClassInstance("ZSEDIFtpHandler").downloadFiles("/CTNVGM_out","/CTNVGM_out_temp",fileDir)
            api.log("CTNVGM：收取"+list_f.size()+"个文件")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm")
            SimpleDateFormat sdfN4 = new SimpleDateFormat("yyyyMMdd HHmmss")
            list_f.each{file->
                File f = file
                boolean noError = true
                try{
                    String fileName = file.getName();
                    api.log("文件名:" + fileName)
                    //读取文件内容
                    api.log("读取文件内容")
                    String fileContent="";
                    file.eachLine {l->
                        fileContent = fileContent +l;
                    }
                    api.log(fileContent)
                    api.log("解析报文...")
                    CTNVGM ctnvgm = new CTNVGM();
                    if(!ctnvgm.parseEDIString(fileContent.trim())){
                        api.log("报文解析出错")
                    }
                    else {
                        api.log("报文解析成功,开始处理报文")
                        api.log("获取报文时间信息")
                        String fileSendTime = ctnvgm.ctnvgm_r00.contents[CTNVGM_R00.FILE_CREATE_TIME]
                        api.log("获取报文中船期信息")
                        String vesselCode = ctnvgm.ctnvgm_r10.contents[CTNVGM_R10.VESSEL_CODE]
                        //去除UN前缀
                        if(vesselCode.length()>2&&vesselCode.startsWith("UN")){
                            vesselCode = vesselCode[2..-1]
                        }
                        String voyage = ctnvgm.ctnvgm_r10.contents[CTNVGM_R10.VOYAGE]
                        String direct = ctnvgm.ctnvgm_r10.contents[CTNVGM_R10.DIRECT]
                        if(direct!=null||direct!=''){
                            voyage = voyage + direct
                        }
                        api.log("船舶代码:" + vesselCode + ";航次:" + voyage)
                        api.log("开始比对箱信息")
                        ctnvgm.ctnvgm_loop_50s.each {loop->
                            CTNVGM_LOOP_50 ctnvgm_loop_50 = loop;
                            String ctnNbr = ctnvgm_loop_50.ctnvgm_r50.contents[CTNVGM_R50.CONTAINER_NUMBER]
                            String vgmWeightStr = ctnvgm_loop_50.ctnvgm_r50.contents[CTNVGM_R50.VGM_GROSS_WEIGHT]
                            api.log("箱号:"+ctnNbr +";VGM毛重:" + vgmWeightStr)
                            //查询UnitFacilityVisit信息
                            api.log("查询箱号为"+ctnNbr+"的活动记录")
                            DomainQuery dqUnit = QueryUtils.createDomainQuery(InventoryEntity.UNIT);
                            dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_VISIT_STATE,UnitVisitStateEnum.ACTIVE))
                            dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_ID,ctnNbr))
                            def res = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqUnit);

                            if(res.size()!=1){
                                api.log("活动记录为" + res.size() + "条,出错")
                            }
                            else{
                                api.log("找到唯一的活动记录,继续校验出口航次的航次号和船舶代码")
                                String hint = ""
                                Unit unit = res[0];
                                UnitFacilityVisit ufv = unit.getUnitActiveUfvNowActive();
                                if(!ufv.getUfvActualObCv().getCvCarrierMode().equals(LocTypeEnum.VESSEL)){
                                    hint = "错误:出口航次类型不是船舶"
                                    api.log(hint)
                                    recordFailureEvent(unit,hint,fileName)
                                }
                                else{
                                    CarrierVisit cv = ufv.getUfvActualObCv()
                                    String unitVoyage = cv.getCarrierObVoyNbrOrTrainId();
                                    String unitVesselCode = cv.getCarrierDocumentationNbr();
                                    api.log(("系统中船舶代码:" + unitVesselCode + ";航次:" + unitVoyage))
                                    //比对船舶代码
                                    boolean  vesCodeMatch = false
                                    if(vesselCode==null||vesselCode==""){
                                        if(unitVesselCode==null||unitVesselCode==""){
                                            api.log("船舶代码均为空")
                                            vesCodeMatch = true
                                        }
                                    }
                                    else{
                                        if(vesselCode.equalsIgnoreCase(unitVesselCode)){
                                            vesCodeMatch = true
                                            api.log("船舶代码比对:" + vesCodeMatch.toString())
                                        }
                                    }
                                    //比对航次
                                    boolean voyageMatch = false
                                    if(unitVoyage.equalsIgnoreCase(voyage)){
                                        voyageMatch = true
                                        api.log("航次比对:" + voyageMatch.toString())
                                    }
                                    //处理箱
                                    if(!(vesCodeMatch&&voyageMatch)){
                                        hint = "船舶代码、航次匹配失败,报文:"+ vesselCode + " "+ voyage +",系统:" + unitVesselCode + " " + unitVoyage
                                        api.log(hint)
                                        recordFailureEvent(unit,hint,fileName)
                                    }
                                    else{
                                        //解析箱类型
                                        if(!(unit.getUnitFreightKind().equals(FreightKindEnum.FCL)||unit.getUnitFreightKind().equals(FreightKindEnum.LCL))){
                                            //非重箱
                                            hint = "该箱不是是重箱,无法更新VGM"
                                            recordFailureEvent(unit,hint,fileName)
                                        }
                                        else{
                                            String unitActiveImpediments = unit.getUnitActiveImpediments();
                                            if(unitActiveImpediments==null||unitActiveImpediments==""||!unitActiveImpediments.contains("VGM_V")){
                                                hint = "该箱没有VGM限制或已经VGM放行"
                                                recordFailureEvent(unit,hint,fileName)
                                            }
                                            else {
                                                //解析时间
                                                boolean timeCorrect = true
                                                Date curInsDate
                                                api.log("解析当前报文发送时间")
                                                try{
                                                    curInsDate = sdf.parse(fileSendTime)
                                                }catch (Exception e){
                                                    api.log("解析当前指令时间失败,报文解析出错,时间值:"+ fileSendTime)
                                                    timeCorrect = false
                                                }
                                                try{
                                                    String oriInsCodeStr = unit.getUnitFlexString09()
                                                    api.log("旧指令：" + oriInsCodeStr)
                                                    String oriSendTimeStr = oriInsCodeStr.substring(oriInsCodeStr.lastIndexOf("[")+1, oriInsCodeStr.lastIndexOf("]"))
                                                    api.log("VGM：箱号"+ctnNbr+"旧指令的发送时间:"+oriSendTimeStr)
                                                    //原指令时间与现指令时间
                                                    Date oriInsDate = sdfN4.parse(oriSendTimeStr)
                                                    if(oriInsDate<=curInsDate){
                                                        api.log("VGM：箱号"+ctnNbr+"当前指令比旧指令晚，将会更新")
                                                    }
                                                    else{
                                                        hint = "VGM：箱号"+ctnNbr+"当前指令比旧指令早，将不会更新"
                                                        api.log(hint)
                                                        recordFailureEvent(unit,hint,fileName)
                                                        timeCorrect = false
                                                    }
                                                }catch(Exception e){
                                                    api.log("VGM：箱号"+ctnNbr+"没有旧指令的时间标志，或解析失败")
                                                }
                                                if(timeCorrect){

                                                    //解析重量
                                                    double vgmWeight = 0.0f;
                                                    try {
                                                        vgmWeight = Double.parseDouble(vgmWeightStr)
                                                        //判断重量是否合理
                                                        double tareWeightKg = unit.getUnitPrimaryUe().getUeEquipment().getEqTareWeightKg()
                                                        double safeWeightKg = unit.getUnitPrimaryUe().getUeEquipment().getEqSafeWeightKg()
                                                        if (vgmWeight < tareWeightKg) {
                                                            hint = "VGM重量:" + vgmWeight + "不能小于箱的净重"
                                                            recordFailureEvent(unit,hint,fileName)
                                                        } else if (safeWeightKg>0 && vgmWeight > safeWeightKg) {
                                                            hint = "VGM重量:" + vgmWeight + "不能大于箱的安全重量"
                                                            recordFailureEvent(unit,hint,fileName)
                                                        } else {
                                                            api.log("更新VGM重量与状态")
                                                            Double oriUnitWeight = unit.getUnitGoodsAndCtrWtKg();
                                                            String oriVGMInfo = unit.getUnitFlexString09()
//                                                            unit.updateGoodsAndCtrWtKg(vgmWeight)
                                                            hint = "已收到重量" + vgmWeight + "[" +sdfN4.format(curInsDate)+ "]"
                                                            unit.setFieldValue(InventoryField.UNIT_FLEX_STRING09, hint)

                                                            FieldChanges fieldChanges = new FieldChanges()
//                                                            fieldChanges.setFieldChange(InventoryField.UNIT_GOODS_AND_CTR_WT_KG, oriUnitWeight, vgmWeight)
                                                            fieldChanges.setFieldChange(InventoryField.UNIT_FLEX_STRING09, oriVGMInfo, hint)

                                                            String postEventName = "UNIT_VGM_UPDATE" //要触发的事件名
                                                            String note = "VGM重量更新,依据文件名:" + fileName //注释

                                                            api.log("记录事件并更新许可")
                                                            ServicesManager sm = (ServicesManager) Roastery.getBean(ServicesManager.BEAN_ID)
                                                            com.navis.services.business.rules.EventType eventType = com.navis.services.business.rules.EventType.findEventType(postEventName)
                                                            sm.recordEvent(eventType, note, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, unit, fieldChanges)

                                                            String applyFlagId = "VGM_V"   //要操作的标志ID
                                                            String note2 = "依据VGM报文放行,文件名:" + fileName
                                                            sm.applyPermission(applyFlagId, unit, null, null, note2)

                                                            api.log("更新完毕")
                                                        }
                                                    }
                                                    catch (Exception e){
                                                        hint = "重量解析失败:" + vgmWeightStr
                                                    }

                                                }
                                            }

                                        }
                                    }

                                }
                            }
                        }
                    }
                    File bakDir = new File(this.fileDirBak)
                    File bakFile = new File(bakDir.getAbsolutePath()+"/"+f.getName())
                    if(!bakDir.exists()){
                        bakDir.mkdirs()
                    }
                    f.renameTo(bakFile)
                }catch(Exception e){
                    api.logWarn("CTNVGM："+e.toString())
                    e.getStackTrace().each {ele->
                        api.log(ele.toString())
                    }
                }
            }

        }catch(Exception e){
            api.logWarn("CTNVGM："+e.toString())
        }finally{
            api.log("CTNVGM：收取结束")
        }
    }


    public void recordFailureEvent(Unit inUnit,String inVGMInfo,String fileName){
        ServicesManager sm = (ServicesManager) Roastery.getBean(ServicesManager.BEAN_ID)
        String postEventName = "UNIT_VGM_FAILURE" //要触发的事件名
        String note = "VGM重量更新出错,原因:"+ inVGMInfo +",依据文件名:" + fileName //注释
        com.navis.services.business.rules.EventType eventType = com.navis.services.business.rules.EventType.findEventType(postEventName)
        sm.recordEvent(eventType, note, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, inUnit)

    }



    public void init(){
        File dir = new File(fileDir)
        if (!dir.exists()){
            dir.mkdir();
        }
        File dirBack = new File(fileDirBak)
        if(!dirBack.exists()){
            dirBack.mkdir()
        }
    }

    public class CTNVGM{
        CTNVGM_R00 ctnvgm_r00;
        CTNVGM_R10 ctnvgm_r10;
        List<CTNVGM_LOOP_50> ctnvgm_loop_50s;
        CTNVGM_R99 ctnvgm_r99;

        CTNVGM(){
            ctnvgm_r00 = new CTNVGM_R00()
            ctnvgm_r10 = new CTNVGM_R10()
            ctnvgm_loop_50s = new ArrayList<>()
            ctnvgm_r99 = new CTNVGM_R99()
        }
        boolean parseEDIString(String inEDIString) {
            boolean result = true;
            String[] res = inEDIString.split("'");
            println res
            int i = 0;
            while(result&&i<res.length){
                res[i] = res[i].trim();
                println ("i:" + i + "  " + res[i])
                if(res[i].startsWith("00")){
                    println("解析00")
                    result = ctnvgm_r00.parseEDIString(res[i]);
                    i++;
                }
                else if(res[i].startsWith("10")){
                    println("解析10")
                    result = ctnvgm_r10.parseEDIString(res[i]);
                    i++;
                }
                //start loop_50
                else if(result&&res[i].startsWith("50")){
                    println("解析50")
                    CTNVGM_LOOP_50 l_50 = new CTNVGM_LOOP_50();
                    if(result&&res[i].startsWith("50")){
                        result = l_50.ctnvgm_r50.parseEDIString(res[i])
                        i++;
                    }
                    ctnvgm_loop_50s.add(l_50)
                }
                //end loop_50
                else if(res[i].startsWith("99")){
                    println("解析99")
                    result = ctnvgm_r99.parseEDIString(res[i]);
                    i++;
                }
                else {
                    println("不支持的片段:" + res[i])
                    i++;
                }
            }
            return  result
        }
    }
    public class CTNVGM_R {
        static String seprator_symbol = ":";
        int fields_count = 1;
        public String[] contents;

        boolean parseEDIString(String inEDIString){
            boolean result = true;
            try{
                String[] contents2 = inEDIString.trim().split(seprator_symbol, -1);
                for(int i = 0;i<contents.length;i++){
                    if(i<contents2.length){
                        contents[i] = contents2[i];
                    }
                    else{
                        contents[i] = "";
                    }
                }
            }catch(Exception e){
                result = false;
                e.printStackTrace();
            }
            return result;
        }
        String toEDIString(){
            String result = "";
            for(String str:contents){
                result = result + str + seprator_symbol;
            }
            if(!result.isEmpty()){
                result = result.substring(0, result.length()-1);
            }
            result = result.replace("null", "");
            return result;
        }
    }
    public class CTNVGM_R00 extends CTNVGM_R{
        /*
         * HEAD RECOED
         */
        static int RECOED_ID = 0;               //记录类型标识
        static int MESSAGE_TYPE = 1;            //报文类型
        static int FILE_DESCRIPTION =2;         //文件说明
        static int FILE_FUNCTION = 3;           //文件功能
        static int SENDER_CODE = 4;             //发送方代码
        static int RECEIVER_CODE = 5;           //接收方代码
        static int FILE_CREATE_TIME = 6;        //文件建立时间
        static int SENDER_PORT_CODE= 7;         //发送港代码
        static int RECEIVER_PORT_CODE = 8;      //接收港代码
        static int REMARKS = 9;                 //备注

        CTNVGM_R00(){
            fields_count = 7;
            contents = new String[fields_count];
        }
    }
    public class CTNVGM_R10 extends CTNVGM_R{
        static int RECOED_ID = 0;                   //记录类型标识
        static int VESSEL_CODE = 1;                 //船名代码
        static int VESSEL = 2;                      //船名
        static int VOYAGE = 3;                      //航次
        static int DIRECT = 4;                      //航向
        static int BOOKING_DEPT_CODE = 5;           //订舱部门代码
        static int BOOKING_DEPT_NAME = 6;           //订舱部门名称
        static int REMARKS = 7;                     //备注

        CTNVGM_R10(){
            fields_count = 8;
            contents = new String[fields_count]
        }
    }
    public class CTNVGM_R50 extends CTNVGM_R{
        static int RECOED_ID = 0;                   //记录类型标识
        static int CONTAINER_NUMBER = 1;            //箱号
        static int CTN_SIZE_TYPE = 2;               //集装箱尺寸类型
        static int CTN_STATUS = 3;                  //集装箱状态 E=空 F=整 L=拼 S=自拼
        static int CTN_OPERATOR_CODE = 4;           //箱经营人代码
        static int CTN_OPERATOR = 5;                //箱经营人
        static int CONTAINER_TYPE = 6;              //集装箱类型
        static int VGM_GROSS_WEIGHT = 7;            //VGM箱总重
        static int VGM_DATE = 8;                    //VGM称重时间
        static int VGM_METHOD = 9;                  //VGM称重方法
        static int LOCATION = 10;                   //VGM称重地点
        static int SHIPPER_NAME = 11;               //托运人名称
        static int VGM_SIGNATURE = 12;              //VGM申报人签名
        static int VGM_TELEPHONE = 13;              //VGM联系方式
        static int VGM_EMAIL = 14;                  //VGM联系邮箱
        static int VGM_ADDRESS = 15;                //VGM联系地址
        static int OTHER1 = 16;                     //预留
        static int OTHER2 = 17;                     //预留
        static int REMARKS = 18;                    //备注

        CTNVGM_R50(){
            fields_count = 19;
            contents = new String[fields_count]
        }
    }
    public class CTNVGM_R99 extends CTNVGM_R{
        static int RECOED_ID = 0;                   //记录类型标识
        static int RECORD_TOTAL_OF_FILE = 1;        //记录总数

        CTNVGM_R99(){
            fields_count = 2;
            contents = new String[fields_count]
        }
    }
    public class CTNVGM_LOOP_50{
        CTNVGM_R50 ctnvgm_r50
        CTNVGM_LOOP_50(){
            ctnvgm_r50 = new CTNVGM_R50()
        }
    }
}
