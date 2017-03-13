package src.com.n4.zs.test

import com.navis.argo.business.api.GroovyApi
import com.navis.inventory.InventoryBizMetafield
import com.navis.inventory.business.units.Unit
import com.navis.services.business.event.GroovyEvent

/**
 * Created by liuminhang on 2016/12/6.
 */
class Unit_Test {

    public void execute(GroovyEvent inEvent, GroovyApi inApi){
        Unit u = (Unit)inEvent.getEntity()
        u.setFieldValue(InventoryBizMetafield.UNIT_FLEX_STRING01,"TEST")
    }

}
