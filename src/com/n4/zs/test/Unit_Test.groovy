package com.n4.zs.test

import com.navis.argo.business.api.GroovyApi
import com.navis.framework.business.Roastery
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.inventory.InventoryBizMetafield
import com.navis.inventory.InventoryEntity
import com.navis.inventory.InventoryField
import com.navis.inventory.business.atoms.UnitVisitStateEnum
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.road.business.appointment.model.GateAppointment
import com.navis.services.business.event.GroovyEvent

/**
 * Created by liuminhang on 2016/12/6.
 */
class Unit_Test {

    public String execute(){
//        String ctnNbr = "SEGU9606507"
//        DomainQuery dqUnit = QueryUtils.createDomainQuery(InventoryEntity.UNIT);
//        dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_VISIT_STATE, UnitVisitStateEnum.ACTIVE))
//        dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_ID,ctnNbr))
//        def res = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqUnit);
//        Unit unit = (Unit)res[0]
//        return "unit id:${unit.getUnitId()},unit category:${unit.getUnitCategory()}"
        GateAppointment gateAppointment = GateAppointment.findGateAppointment(18722)
//        gateAppointment.
    }

}
