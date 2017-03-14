package com.n4.lwt

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.reference.ScopedBizUnit
import com.navis.inventory.business.units.Unit
import com.navis.services.business.event.Event
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.argo.business.extract.ChargeableUnitEvent
import com.navis.framework.business.Roastery
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.argo.ArgoExtractEntity
import com.navis.argo.ArgoExtractField
import com.navis.services.business.event.GroovyEvent

/**
 * 根据Unit中跟新的GuaranteeParty信息,更新ChargeableUnitEvent中的GuaranteeParty字段
 * Created by Badger on 16/1/23.
 */
class LWT_GenChargeableGuarantee extends GroovyApi {
    private GroovyEvent event
    private String eventTypeId
    private String guaranteePartyId
    private Date guaranteeThruDate

    public void execute(GroovyEvent inEvent, String inEventTypeId) {
        this.event = inEvent
        this.eventTypeId = inEventTypeId
        guaranteePartyId = null
        guaranteeThruDate = null
        process()
    }

    private void process() {
        logInfo("更新ChargeableUnitEvent");

        Unit unit = (Unit) event.getEntity()
        Event evnt = (Event) event.getEvent();
        Date evntAppliedDate = evnt.getEvntAppliedDate();


        UnitFacilityVisit ufv = unit.getUfvForEventTime(evntAppliedDate);
        // 获取GuaranteeParty
        if (ufv) {
            if (ufv.getUfvGuaranteeParty()) {
                guaranteePartyId = ufv.getUfvGuaranteeParty().getBzuId()
            }
            guaranteeThruDate = ufv.getUfvGuaranteeThruDay()
        }

        // 获取ChargeableUnitEvent
        List cueList = findByEventTypeIdAndUfv(ufv, eventTypeId);

        if (!cueList.isEmpty()) {
            if (cueList.size() > 1) {

                logWarn("多于1个的事件 : " + eventTypeId + " 箱号 : " + ufv.getPrimaryEqId());
            } else {
                ChargeableUnitEvent cue = cueList.get(0);
                if (cue != null) {
                    cue.setBexuGuaranteeParty(guaranteePartyId)
                    cue.setBexuGuaranteeThruDay(guaranteeThruDate)
                }
            }
        }
        logInfo("结束更新");
    }

    /**
     * 根据事件名,找到相应UFV的ChargeableUnitEvent
     */
    private List findByEventTypeIdAndUfv(UnitFacilityVisit inUfv, String inEventTypeId) {
        DomainQuery dq = QueryUtils.createDomainQuery(ArgoExtractEntity.CHARGEABLE_UNIT_EVENT)
                .addDqPredicate(PredicateFactory.eq(ArgoExtractField.BEXU_EVENT_TYPE, inEventTypeId))
                .addDqPredicate(PredicateFactory.eq(ArgoExtractField.BEXU_UFV_GKEY, inUfv.getPrimaryKey()));
        return Roastery.getHibernateApi().findEntitiesByDomainQuery(dq);
    }
}
