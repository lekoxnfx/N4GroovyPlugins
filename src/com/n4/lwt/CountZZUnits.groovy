package com.n4.lwt

import com.navis.argo.business.api.GroovyApi
import com.navis.framework.business.Roastery
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.inventory.InventoryEntity
import com.navis.inventory.InventoryField
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.event.GroovyEvent
import com.navis.vessel.business.schedule.VesselVisitDetails

/**
 * Created by liuminhang on 16/6/20.
 * 用于在船期结束后,判断该航次中转箱的数目。如果超过一定值,则记录一个事件。
 */
class CountZZUnits {

    String vvEventName = "";


    public void execute(GroovyEvent inEvent, GroovyApi inApi) {


        VesselVisitDetails vvd = (VesselVisitDetails)inEvent.getEntity()
        long cv_gkey = vvd.getCvdCv().getCvGkey();

        //查询Unit信息
        DomainQuery dqUfv = QueryUtils.createDomainQuery(InventoryEntity.UNIT_FACILITY_VISIT);
        dqUfv.addDqPredicate(PredicateFactory.in(InventoryField.UFV_TRANSIT_STATE,UfvTransitStateEnum.S70_DEPARTED))
        dqUfv.addDqPredicate(PredicateFactory.in(InventoryField.UFV_ACTUAL_OB_CV,cv_gkey))
        def res = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqUfv);

        //统计中转箱数据

        int zzUnitCount = 0
        res.each {
            UnitFacilityVisit ufv = it;
            //获取中转自定义字段
            String zzFlexString = ufv.getUfvUnit().getUnitFlexString11()
            if(zzFlexString.equals("中转")){
                zzUnitCount ++;
            }
        }
        //记录船期事件
        String note = "中转箱数目统计" //注释

        String postEventName = vvEventName //要触发的事件名
        BigDecimal quantity = zzUnitCount
        com.navis.argo.business.api.ServicesManager sm = (com.navis.argo.business.api.ServicesManager) com.navis.framework.business.Roastery.getBean(com.navis.argo.business.api.ServicesManager.BEAN_ID)
        com.navis.argo.business.model.VisitDetails v= (com.navis.argo.business.model.VisitDetails)inEvent.getEntity()
        com.navis.services.business.rules.EventType eventType = com.navis.services.business.rules.EventType.findEventType(postEventName)
        sm.recordEvent(eventType, note, quantity, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNITS, v)

    }


}
