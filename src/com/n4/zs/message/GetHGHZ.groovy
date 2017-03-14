package com.n4.zs.message

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.UnitCategoryEnum
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import groovy.sql.Sql

import java.text.SimpleDateFormat

/**
 * Created by lekoxnfx on 15/5/5.
 * 获取新海关抵港报文回执
 */
class GetHGHZ {
    GroovyApi api = new GroovyApi();
    public String fileDir = "D:\\MESSAGE\\海关回执"
    public String fileDirBak = "D:\\MESSAGE\\Backup\\海关回执"
    String note;
    Sql sql;
    public void execute(Map args){
        init();
        api.log("海关回执：开始收取海关回执")
        try{
            List<File> list_f;
            list_f = api.getGroovyClassInstance("ZSEDIFtpHandler").downloadFiles("/MT3101_out","/MT3101_out_temp",fileDir)
            api.log("海关回执：收取"+list_f.size()+"个文件")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS")
            SimpleDateFormat sdfN4 = new SimpleDateFormat("yyyyMMdd HHmmss")
            list_f.each{file->
                File f = file
                boolean noError = true
                try{
                    boolean timeCorrect = true
                    Date curInsDate
                    api.log("海关回执：解析文件："+ f.getName())
                    def RES = new XmlParser().parse(f)
                    String sendTimeStr = RES.'Head'.'SendTime'.text();
                    api.log("海关回执：发送时间："+sendTimeStr)
                    try{
                        curInsDate = sdf.parse(sendTimeStr)
                    }catch (Exception e){
                        note = "解析当前指令时间失败,报文解析出错,时间值:"+ sendTimeStr
                        api.log(note)
                        timeCorrect = false
                        noError = false
                    }
                    RES.'Response'.each {u->
                        String unitNbr = u.'TransportEquipment'.'EquipmentIdentification'.'ID'.text()
                        String insCode = u.'TransportEquipment'.'ResponseType'.'Code'.text()
                        String insStr = u.'TransportEquipment'.'ResponseType'.'Text'.text()
                        String jouneyId = u.'BorderTransportMeans'.'JourneyID'.text()
                        String vesselUnCode = u.'BorderTransportMeans'.'ID'.text()
                        if(vesselUnCode.length()>2){
                            vesselUnCode = vesselUnCode[2..-1]//海关登记的UN代码前面会加上CN/UN
                        }
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
                            String vesselCode;
                            boolean isImportOrExport = true;
                            switch (unit.unitCategory){
                                case UnitCategoryEnum.EXPORT:                                //出口箱,取出口航次
                                case UnitCategoryEnum.TRANSSHIP:                               //中转箱，取出口航次
                                    voyage = ufv.getUfvActualObCv().getCarrierObVoyNbrOrTrainId()
                                    vesselCode = ufv.getUfvActualObCv().getCarrierDocumentationNbr()
                                    break;
                                case UnitCategoryEnum.IMPORT:                                //进口箱,取进口航次
                                    voyage = ufv.getUfvActualIbCv().getCarrierIbVoyNbrOrTrainId()
                                    vesselCode = ufv.getUfvActualIbCv().getCarrierDocumentationNbr()
                                    break;
                                default:
                                    api.log("海关回执：箱号"+unitNbr+"不是进口或者出口或者中转")
                                    isImportOrExport = false
                                    noError = false
                            }

                            //判断当前指令时间
                            Date oriInsDate
                            try{
                                String oriInsCodeStr = unit.getUnitFlexString01()
                                String oriSendTimeStr = oriInsCodeStr.substring(oriInsCodeStr.lastIndexOf("[")+1, oriInsCodeStr.lastIndexOf("]"))
                                api.log("海关回执：箱号"+unitNbr+"旧指令的发送时间:"+oriSendTimeStr)
                                //原指令时间与现指令时间
                                oriInsDate = sdfN4.parse(oriSendTimeStr)
                                if(oriInsDate<curInsDate){
                                    timeCorrect = true
                                    api.log("海关回执：箱号"+unitNbr+"当前指令比旧指令晚，将会更新")
                                }
                                else{
                                    timeCorrect = false
                                    api.log("海关回执：箱号"+unitNbr+"当前指令比旧指令早，将不会更新")
                                }

                            }catch(Exception e){
                                api.log("海关回执：箱号"+unitNbr+"没有旧指令的时间标志，或解析失败")
                                timeCorrect = true
                            }
                            if(isImportOrExport&&jouneyId.equals(voyage)&&vesselUnCode.equalsIgnoreCase(vesselCode)&&timeCorrect){//比对航次
                                insStr = insStr + "["+sdfN4.format(curInsDate)+"]"
                                unit.setUnitFlexString01(insStr)
                                api.log("海关回执："+"箱号"+unitNbr+"收到海关回执" + insStr + "["+sendTimeStr+"]")


                            }
                            else{
                                api.log("海关回执："+"箱号"+unitNbr+"对应航次不匹配，收到：" + jouneyId +","+vesselUnCode+",系统："+voyage + ","+vesselCode)
                                noError = false
                            }

                        }
                        else{
                            api.logWarn("海关回执："+"箱号"+unitNbr+"有"+count+"个活动记录！")
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
                    api.logWarn("海关回执："+e.toString())
                }
            }

        }catch(Exception e){
            api.logWarn("海关回执："+e.toString())
        }finally{
            api.log("海关回执：收取结束")
        }
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
