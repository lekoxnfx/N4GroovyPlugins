package com.n4.lwt

import com.navis.argo.business.api.GroovyApi
import com.navis.inventory.InventoryField
import com.navis.inventory.business.units.Unit
import com.navis.services.business.event.GroovyEvent

/**
 * Created by lekoxnfx on 16/1/19.
 */
class AutoUpdateZZ {
    public void execute(GroovyEvent inEvent,GroovyApi inApi) {
        inApi.log("判断是否是中转箱")
        com.navis.inventory.business.units.Unit u = (com.navis.inventory.business.units.Unit) inEvent.getEntity();
        u.setFieldValue(com.navis.inventory.InventoryField.UNIT_FLEX_STRING11,"中转")
    }
}
