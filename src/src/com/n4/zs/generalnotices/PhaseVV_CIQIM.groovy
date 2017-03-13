package src.com.n4.zs.generalnotices

import com.navis.argo.business.api.GroovyApi
import com.navis.services.business.event.GroovyEvent
import com.navis.vessel.business.atoms.StowageSchemeEnum

/**
 * Created by liuminhang on 16/8/3.
 */
class PhaseVV_CIQIM {
    public void execute(GroovyEvent event, GroovyApi api) {
        String postEventName = "VV_SEND_CIQIM_DISCHARGE" //要触发的事件名
        String note = "系统触发" //注释
        com.navis.vessel.business.schedule.VesselVisitDetails v= (com.navis.vessel.business.schedule.VesselVisitDetails)event.getEntity()

        if(v.getVvdVessel().getVesStowageScheme().equals(com.navis.vessel.business.atoms.StowageSchemeEnum.ISO)){
            event.postNewEvent(postEventName,note)
        }

    }
}