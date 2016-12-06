package com.n4.zs.groovyjobs

import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.atoms.UnitCategoryEnum
import com.navis.argo.portal.job.GroovyJob
import com.navis.framework.business.Roastery
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.JoinType
import com.navis.framework.portal.query.PredicateFactory
import com.navis.inventory.InventoryEntity
import com.navis.inventory.InventoryField
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.atoms.UnitVisitStateEnum
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit

/**
 * Created by liuminhang on 16/3/1.
 */
class AutoCheckPTIDueDate {
    public void execute(Map args){
        Date currentDate = new Date();


        DomainQuery dqUfv = QueryUtils.createDomainQuery(InventoryEntity.UNIT_FACILITY_VISIT);
        dqUfv.addDqPredicate(PredicateFactory.in(InventoryField.UFV_TRANSIT_STATE,UfvTransitStateEnum.S40_YARD))
        dqUfv.addDqPredicate(PredicateFactory.isNotNull(InventoryField.UFV_FLEX_DATE01))//PTI过期日
        dqUfv.addDqPredicate(PredicateFactory.le(InventoryField.UFV_FLEX_DATE01,currentDate))//PTI过期日

        def res = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqUfv);

        res.each {
            UnitFacilityVisit ufv = it
            Unit unit = ufv.getUfvUnit()


            String postEventName = "UNIT_PTI_ADD_HOLD" //要触发的事件名
            String note = "系统检测到过期" //注释
            com.navis.argo.business.api.ServicesManager sm = (com.navis.argo.business.api.ServicesManager) com.navis.framework.business.Roastery.getBean(com.navis.argo.business.api.ServicesManager.BEAN_ID)
            com.navis.services.business.rules.EventType eventType = com.navis.services.business.rules.EventType.findEventType(postEventName)
            sm.recordEvent(eventType, note, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, unit)
        }
    }
}
