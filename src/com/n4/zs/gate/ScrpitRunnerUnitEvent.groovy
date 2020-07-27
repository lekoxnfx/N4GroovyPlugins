package com.n4.zs.gate

import com.navis.argo.business.api.ServicesManager
import com.navis.argo.business.atoms.LogicalEntityEnum
import com.navis.framework.business.Roastery
import com.navis.inventory.business.moves.WorkInstruction
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.api.EventManager
import com.navis.services.business.event.Event
import com.navis.services.business.rules.EventType

/**
 * Created by lekoxnfx on 2017/3/29.
 */
class ScrpitRunnerUnitEvent {
    public String execute() {
        String hold_id = "PTI_H"
        long unit_gkey = 13802690
        com.navis.inventory.business.units.Unit unit = com.navis.inventory.business.units.Unit.hydrate(unit_gkey);
        com.navis.argo.business.api.ServicesManager sm = (com.navis.argo.business.api.ServicesManager) Roastery.getBean(ServicesManager.BEAN_ID)
        com.navis.services.business.api.EventManager sem = (com.navis.services.business.api.EventManager)Roastery.getBean("eventManager");
        EventType et_unit_create = EventType.findEventType("UNIT_CREATE")
        List event_list = sem.getEventHistory(et_unit_create,unit)
        if(event_list.size()==1){
            Event event = event_list.get(0)
            String event_note =  event.getEventNote().toString()
            if(event_note.contains('strip')){
                String hold_note = "applied by system"
                sm.applyHold(hold_id,unit,null,null,hold_note)
            }
        }
    }
}
