package com.n4.zs.generalnotices

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.api.ServicesManager
import com.navis.framework.business.Roastery
import com.navis.services.business.event.Event
import com.navis.services.business.event.GroovyEvent
import com.navis.services.business.rules.EventType

class UnitCreate {
    public void execute(GroovyEvent event, GroovyApi api) {
        //拆箱后，自动添加HOLD PTI_H
        String hold_id = "PTI_H"
        String hold_note = "applied by system"
        com.navis.inventory.business.units.Unit unit = (com.navis.inventory.business.units.Unit)event.getEntity()
        com.navis.argo.business.api.ServicesManager sm = (com.navis.argo.business.api.ServicesManager) Roastery.getBean(ServicesManager.BEAN_ID)
        com.navis.services.business.api.EventManager sem = (com.navis.services.business.api.EventManager)Roastery.getBean("eventManager");
        com.navis.services.business.rules.EventType et_unit_create = com.navis.services.business.rules.EventType.findEventType("UNIT_CREATE")
        List event_list = sem.getEventHistory(et_unit_create,unit)
        if(event_list.size()==1){
            com.navis.services.business.event.Event event_uc = event_list.get(0)
            String event_note =  event_uc.getEventNote().toString()
            if(event_note.contains('strip')){
                sm.applyHold(hold_id,unit,null,null,hold_note)
            }
        }

    }
}
