package com.n4.zs.gate

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.atoms.LocTypeEnum
import com.navis.argo.business.atoms.UnitCategoryEnum
import com.navis.argo.business.model.CarrierVisit
import com.navis.inventory.business.units.Unit
import com.navis.road.business.model.TruckTransaction
import com.navis.vessel.business.schedule.VesselVisitDetails

/**
 * Created by lekoxnfx on 15/7/2.
 */
class BiznessTask {
    GroovyApi api
    public void execute(){

        def tran = (TruckTransaction)inDao.getTran();

        com.navis.inventory.business.units.Unit unit = tran.getTranUnit()
        if(unit.getUnitFreightKind().equals(FreightKindEnum.MTY)&&unit.getUnitLineOperator().getBzuId().equals("COS")){
            api.log("COS的空箱")
            api.log("unit id:" + unit.getUnitId())
            api.log("Empty bl : " + unit.getUnitFlexString08())
            if(unit.getUnitFlexString08()==null){
                api.log("Empty BL is null!!")
                tran.tranFlexString01 = "01"
            }
        }



    }
    public void checkTime(){
        //用于检测出口箱是否在允许的时间范围内 add by lekoxnfx
        def tran = (TruckTransaction)inDao.getTran();
        api.log("出口箱" + "unit id:" + tran.getTranContainer().getEqIdFull())
        com.navis.argo.business.model.CarrierVisit obcv = tran.getTranCarrierVisit()
        api.log("出口航次" + obcv.getCvId())
        if(obcv!=null){
            if (obcv.getCvCarrierMode().equals(com.navis.argo.business.atoms.LocTypeEnum.VESSEL)){
                com.navis.vessel.business.schedule.VesselVisitDetails vvd = com.navis.vessel.business.schedule.VesselVisitDetails.resolveVvdFromCv(obcv)
                if (vvd!=null){
                    Date currentDate = new Date()
                    Date startDate = vvd.getVvFlexDate01()
                    api.log("出口航次允许提箱时间:" + startDate.toString())
                    if(startDate!=null){
                        if(currentDate<startDate){
                            tran.tranFlexString02 = "X"
                            api.log("早于允许提箱时间,标记tranFlexString02")
                        }

                    }
                    Date endDate = vvd.getVvFlexDate02()
                    api.log("出口航次允许提箱时间:" + startDate.toString())
                    if(endDate!=null){
                        if(currentDate>endDate){
                            tran.tranFlexString02 = "X"
                            api.log("晚于允许提箱时间,标记tranFlexString02")
                        }

                    }
                }
            }
        }

    }
    public void clearAppt(){

        def tran = (TruckTransaction)inDao.getTran();

        def tranApptNbr = tran.getTranAppointmentNbr();

        api.log("关联的预约号：ApptNbr:" + tranApptNbr);
        if(tranApptNbr!=null){
            def cntrAssigned = tran. getTranCtrNbrAssigned();

            api.log("关联的箱号：CntrAssigned-before:" + cntrAssigned);



            tran. setTranCtrNbrAssigned(null);
            api.log("CntrAssigned-after:" + cntrAssigned);
        }

    }

}
