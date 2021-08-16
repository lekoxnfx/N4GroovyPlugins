package com.n4.zs.gate;

import com.navis.argo.business.api.GroovyApi
import com.navis.framework.portal.FieldChanges
import com.navis.inventory.InventoryField
import com.navis.road.business.api.GroovyRoadApi
import com.navis.road.business.workflow.GroovyRoadHandler
import com.navis.road.business.workflow.IUnitBean
import com.navis.road.business.workflow.TransactionAndVisitHolder

public class BizTaskGateApptCreate extends BizTaskInterceptor {

    @Override
    void intercept(TransactionAndVisitHolder inDao, IUnitBean inUnitBean, GroovyRoadApi api) {
        //当预约创建时，为指定提箱的空箱记录事件，并通过事件加专用锁
        com.navis.road.business.appointment.model.GateAppointment appt = inDao.getAppt()
        if(appt.getGapptTranType().equals(com.navis.road.business.atoms.TruckerFriendlyTranSubTypeEnum.PUI)){
            com.navis.inventory.business.units.Unit unit = appt.getGapptUnit()
            if(unit.getUnitFreightKind().equals(com.navis.argo.business.atoms.FreightKindEnum.MTY)){
                //通过记录事件加锁
                String postEventName = "UNIT_GATE_APPT_DI_MTY_LOCK" //要触发的事件名
                String note = "系统触发，该箱号被预约指定提空箱，预约号: ${appt.getGapptNbr()}" //注释
                com.navis.argo.business.api.ServicesManager sm =
                        (com.navis.argo.business.api.ServicesManager) com.navis.framework.business.Roastery.getBean(com.navis.argo.business.api.ServicesManager.BEAN_ID)
                com.navis.services.business.rules.EventType eventType = com.navis.services.business.rules.EventType.findEventType(postEventName)
                sm.recordEvent(eventType, note, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, unit)
            }
        }
    }


    public void testClass(){
        def trans = inDao.getTran();
        throw new Exception("${this.getClass().getCanonicalName()}")
    }

}
