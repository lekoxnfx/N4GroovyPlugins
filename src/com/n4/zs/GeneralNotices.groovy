package com.n4.zs

import com.navis.argo.business.api.GroovyApi
import com.navis.services.business.event.GroovyEvent
import com.navis.vessel.business.atoms.StowageSchemeEnum

/**
 * Created by liuminhang on 16/3/2.
 */
class GeneralNotices {

    public void testMethod(){
        GroovyApi api;
        GroovyEvent event;

        String postEventName = "VV_SEND_CIQIM_DISCHARGE" //要触发的事件名
        String note = "系统触发" //注释
        com.navis.vessel.business.schedule.VesselVisitDetails v= (com.navis.vessel.business.schedule.VesselVisitDetails)event.getEntity()
        if(v.getVvdVessel().getVesStowageScheme().equals(com.navis.vessel.business.atoms.StowageSchemeEnum.ISO)){
            event.postNewEvent(postEventName,note)
        }


//        String postEventName = "VV_SEND_YXCD_COARRI_DISCHARGE" //要触发的事件名
//        String note = "系统触发" //注释
//        com.navis.argo.business.api.ServicesManager sm = (com.navis.argo.business.api.ServicesManager) com.navis.framework.business.Roastery.getBean(com.navis.argo.business.api.ServicesManager.BEAN_ID)
//        com.navis.argo.business.model.VisitDetails v= (com.navis.argo.business.model.VisitDetails)event.getEntity()
//        com.navis.services.business.rules.EventType eventType = com.navis.services.business.rules.EventType.findEventType(postEventName)
//        sm.recordEvent(eventType, note, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, v)



//        String postEventName = "UNIT_SEND_COS_CODECO_INGATE" //要触发的事件名
//        String note = "系统触发" //注释
//        com.navis.argo.business.api.ServicesManager sm = (com.navis.argo.business.api.ServicesManager) com.navis.framework.business.Roastery.getBean(com.navis.argo.business.api.ServicesManager.BEAN_ID)
//        com.navis.inventory.business.units.Unit u = (com.navis.inventory.business.units.Unit)event.getEntity()
//        com.navis.services.business.rules.EventType eventType = com.navis.services.business.rules.EventType.findEventType(postEventName)
//        sm.recordEvent(eventType, note, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, u)
    }

}
