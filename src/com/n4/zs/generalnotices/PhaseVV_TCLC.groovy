package com.n4.zs.generalnotices

import com.navis.argo.business.api.GroovyApi
import com.navis.services.business.event.GroovyEvent

/**
 * Created by liuminhang on 16/7/25.
 */
class PhaseVV_TCLC {
    public void execute(GroovyEvent event, GroovyApi api) {
        //船舶离开时,检查船舶所允许的船公司,并执行对应事件发送报文

        //获取Service manager
        com.navis.argo.business.api.ServicesManager sm = (com.navis.argo.business.api.ServicesManager) com.navis.framework.business.Roastery.getBean(com.navis.argo.business.api.ServicesManager.BEAN_ID)
        //获取VesselVisitDetails实体
        com.navis.vessel.business.schedule.VesselVisitDetails vvd = (com.navis.argo.business.model.VisitDetails) event.getEntity()
        //获取所允许的Lines
        vvd.getVvdVvlineSet().each { l ->
            com.navis.vessel.business.schedule.VesselVisitLine vvdl = l;
            if (vvdl.getVvlineBizu().getBzuId().equals("TCLC")) {
                //触发TCLC装卸报文生成事件
                //卸船
                String postEventName1 = "VV_SEND_TCLC_COARRI_DISCHARGE" //要触发的事件名
                String note1 = "系统触发" //注释
                com.navis.services.business.rules.EventType eventType1 = com.navis.services.business.rules.EventType.findEventType(postEventName1)
                sm.recordEvent(eventType1, note1, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, vvd)
                //装船
                String postEventName2 = "VV_SEND_TCLC_COARRI_LOAD" //要触发的事件名
                String note2 = "系统触发" //注释
                com.navis.services.business.rules.EventType eventType2 = com.navis.services.business.rules.EventType.findEventType(postEventName2)
                sm.recordEvent(eventType2, note2, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, vvd)
            }
        }
    }
}
