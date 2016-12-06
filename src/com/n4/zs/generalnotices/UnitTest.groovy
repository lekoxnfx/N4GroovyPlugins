package com.n4.zs.generalnotices

import com.navis.argo.business.api.GroovyApi
import com.navis.services.business.event.GroovyEvent

/**
 * Created by liuminhang on 2016/12/6.
 */
class UnitTest {

    public void execute(GroovyEvent event, GroovyApi api) {

        String postEventName = "UNIT_TEST"
        String note = "系统触发"
        com.navis.argo.business.api.ServicesManager sm = (com.navis.argo.business.api.ServicesManager) com.navis.framework.business.Roastery.getBean(com.navis.argo.business.api.ServicesManager.BEAN_ID)
        com.navis.inventory.business.units.Unit u = (com.navis.inventory.business.units.Unit)event.getEntity()
        com.navis.services.business.rules.EventType eventType = com.navis.services.business.rules.EventType.findEventType(postEventName)

        sm.recordEvent(eventType, note, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, u)


        def myGroovyCode = api.getGroovyClassInstance("Unit_Test")


        myGroovyCode.execute(event, api)
    }

}
