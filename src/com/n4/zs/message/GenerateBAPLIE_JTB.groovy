package com.n4.zs.message

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.atoms.TempUnitEnum
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.event.GroovyEvent
import com.navis.vessel.business.schedule.VesselVisitDetails
import groovy.sql.Sql

/*
 * 用于出口船生成出口预配船图BAPLIE_F文件（交通部格式）
 */
import java.text.SimpleDateFormat

class GenerateBAPLIE_JTB {
    public String fileDir = "D:\\MESSAGE\\BAPLIE_JTB"
    public String fileDirBak = "D:\\MESSAGE\\Backup\\BAPLIE_JTB"
    public String messageType = "BAPLIE"
    public String sender_id = "ZSDND"

    String line_sep = "\n"

    public GroovyEvent event
    public GroovyApi api
    public Map<String,String> paras	//额外参数

    String vv_id;
    /*
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
        init();
        VesselVisitDetails vvd = (VesselVisitDetails)event.getEntity();
        api.log("开始生成BAPLIE报文")
        File f = generateBAPLIEFile(vvd)

        File bakDir = new File(this.fileDirBak)
        if(!bakDir.exists()){
            bakDir.mkdirs()
        }
        File bakFile = new File(bakDir.getAbsolutePath()+"/"+f.getName())
        f.renameTo(bakFile)

    }
    public String generateBAPLIEText(VesselVisitDetails vvd){
        try{
            //获取对应参数
            long cvgkey = vvd.getCvdCv().getCvGkey()
            api.log("CarrierVisitGKey:"+cvgkey)
            String file_description = "BAYPLAN";
            String unitQueryStr;
            unitQueryStr = """
			select ufv.gkey from inv_unit_fcy_visit ufv, inv_unit u where ufv.actual_ob_cv = '${cvgkey}' and ufv.unit_gkey = u.gkey
			"""

            String receiver_code = ""

            String file_create_time = new SimpleDateFormat("yyyyMMddHHmm").format(new Date())

            List<UnitFacilityVisit> list_ufv = new ArrayList<UnitFacilityVisit>();//预配箱列表
            //数据库查询
            sql.eachRow(unitQueryStr) {r->
                long ufv_gkey = r.'gkey'
                UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufv_gkey)
                list_ufv.add(ufv)
            }


            String ediString = ""
            int record_count = 0
            //00记录
            api.log("生成00记录")

            String edi00 = """
			00:${messageType}:${file_description}:9:${sender_id}:${receiver_code}:${file_create_time}'
			"""
            edi00 = edi00.trim() + line_sep
            ediString = ediString + edi00
            record_count++

            //10记录 船舶信息
            api.log("生成10记录")

            vv_id = vvd.getCvdCv().getCvId()

            String vessel_code = vvd.getCarrierDocumentationNbr()
            String vessel = vvd.getCarrierVehicleName()
            if(vessel_code==null||vessel_code=="null"){
                vessel_code = ""
            }
            String nationality_code = ""
            try{
                nationality_code = vvd.getVvdVessel().getVesCountry().getCntryCode()
            }catch(Exception e){
                nationality_code = ""
            }
            if(nationality_code==null||nationality_code=="null"){
                nationality_code = ""
            }
            String voyage = vvd.getVvdObVygNbr()
            String trade_code = ""
            String trade = ""
            String etd_arrived_date = ""
            String sailing_date = ""
            String depart_port_code = ""
            String depart_port = ""
            String next_calling_port_code = ""
            String next_calling_port = ""

            String edi10 = """
			10:${vessel_code}:${vessel}:${nationality_code}:${voyage}:${trade_code}:${trade}:${etd_arrived_date}:${sailing_date}:${depart_port_code}:${depart_port}:${next_calling_port_code}:${next_calling_port}'
			"""
            edi10 = edi10.trim() + line_sep
            ediString = ediString + edi10
            record_count++

            //11记录 船舶补充信息
            api.log("生成11记录")

            String shipping_line_code = vvd.getVvdBizu().getBzuId()
            String shipping_line = ""
            String edi11 = """
			11:${shipping_line_code}:${shipping_line}'
			"""
            edi11 = edi11.trim() + line_sep
            ediString = ediString + edi11
            record_count++

            //50,51,52,53,54循环
            list_ufv.each {u->
                UnitFacilityVisit ufv = u
                Unit unit = ufv.getUfvUnit()
                //50记录 箱信息
                api.log("生成50记录")

                String ctn_no = unit.getUnitId()
                String unit_iso_code = unit.getUnitPrimaryUe().getUeEquipment().eqEquipType.eqtypId
                String ctn_size_type = ISO_TO_GP.get(unit_iso_code)==null?unit_iso_code:ISO_TO_GP.get(unit_iso_code)
                String ctn_status;
                switch(unit.unitFreightKind){
                    case FreightKindEnum.FCL:
                        ctn_status = "F";
                        break;
                    case FreightKindEnum.MTY:
                        ctn_status = "E";
                        break;
                    case FreightKindEnum.LCL:
                        ctn_status = "L";
                        break;
                    default:
                        ctn_status = "F";
                }
                String stowage_location =""
                try{
                    stowage_location = ufv.getFinalPlannedPosition().getPosSlot();
                }catch(Exception e){
                    stowage_location = ""
                    api.logWarn(e.toString())
                }
                if(stowage_location==null||stowage_location=="null"){
                    try{
                        stowage_location = ufv.getUfvLastKnownPosition().getPosSlot();
                    }catch(Exception e){
                        stowage_location = ""
                        api.logWarn(e.toString())
                    }
                }
                if(stowage_location==null||stowage_location=="null"){
                    stowage_location = ""
                }else{
                    if(stowage_location.length()<7&&stowage_location.length()>0){
                        stowage_location = "0" + stowage_location
                    }
                }
                String temperature_id,temperature_setting,min_temperature="",max_temperature="";
                switch(vvd.getVvdVessel().getVesTemperatureUnits()){
                    case TempUnitEnum.CELSIUS:
                        temperature_id = "C"
                        temperature_setting = ufv.getRfreqTempRequiredInC()
                        break;
                    case TempUnitEnum.FAHRENHEIT:
                        temperature_id = "F"
                        temperature_setting = ufv.getRfreqTempRequiredInF()
                        break;
                }
                String over_length_front = unit.getUnitOogFrontCm()
                String over_length_back = unit.getUnitOogBackCm()
                String over_width_left = unit.getUnitOogLeftCm()
                String over_width_right = unit.getUnitOogRightCm()
                String over_height = unit.getUnitOogTopCm()
                String gross_weight = unit.getUnitGoodsAndCtrWtKg()
                String tare_weight = unit.getUnitPrimaryUe().getUeEquipment().getEqTareWeightKg()
                String ctn_operator_code = unit.getUnitLineOperator().getBzuId()

                if(temperature_setting==null||temperature_setting=="null"){temperature_setting = ""}
                if(min_temperature==null||min_temperature=="null"){min_temperature = ""}
                if(max_temperature==null||max_temperature=="null"){max_temperature = ""}
                if(over_length_front==null||over_length_front=="null"){over_length_front = ""}
                if(over_length_back==null||over_length_back=="null"){over_length_back = ""}
                if(over_width_left==null||over_width_left=="null"){over_width_left = ""}
                if(over_width_right==null||over_width_right=="null"){over_width_right = ""}
                if(over_height==null||over_height=="null"){over_height = ""}


                String ctn_operator = ""
                String edi50 = """
				50:${ctn_no}:${ctn_size_type}:${ctn_status}:${stowage_location}:${temperature_id}:${temperature_setting}:${min_temperature}:${max_temperature}:${over_length_front}:${over_length_back}:${over_width_left}:${over_width_right}:${over_height}:${gross_weight}:${tare_weight}:${ctn_operator_code}:${ctn_operator}'
				"""
                edi50 = edi50.trim() + line_sep

                //51提单信息,可选信息



                //52 地点信息
                api.log("生成52记录")

                String discharge_port_code= "",load_port_code=""
                try{
                    discharge_port_code = unit.getUnitRouting().getRtgPOD1().getPointId()
                    load_port_code = unit.getUnitRouting().getRtgPOL().getPointId()
                }catch(Exception e){
                    api.log(e.toString())
                }

                String edi52 = """
				52:${load_port_code}::${discharge_port_code}:::::'
				"""
                edi52 = edi52.trim() + line_sep

                //53 可选卸货港信息 暂无

                //54 危险品信息 暂无

                ediString = ediString + edi50 + edi52
                record_count++
                record_count++

            }

            //99字段 尾记录
            api.log("生成99记录")

            record_count++;
            String edi99 = """
			99:${record_count}'
			"""
            edi99 = edi99.trim() + line_sep
            ediString = ediString + edi99
            api.log("EdiString:" + ediString)
            return ediString
        }catch(Exception e){
            noError = false;
            note = e.toString()
            api.logWarn(note)
            return null
        }

    }

    public File generateBAPLIEFile(VesselVisitDetails vvd){
        File dir = new File(this.fileDir)
        if(!dir.exists()){
            dir.mkdirs()
        }
        String edi = this.generateBAPLIEText(vvd)
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
            return f
        }
    }
    public String generateFileName(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
        return messageType + "_" + this.vv_id + "_" + sdf.format(new Date())
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
}
