package com.n4.zs.gate

import com.navis.argo.business.api.GroovyApi

/**
 * Created by lekoxnfx on 15/6/17.
 */
import com.navis.road.business.atoms.AppointmentStateEnum;
import com.navis.framework.business.Roastery;
import com.navis.framework.metafields.MetafieldId;
import com.navis.framework.persistence.HibernateApi;
import com.navis.framework.portal.Ordering;
import com.navis.framework.portal.QueryUtils;
import com.navis.framework.portal.query.DomainQuery;
import com.navis.framework.portal.query.Junction;
import com.navis.framework.portal.query.PredicateFactory;
import com.navis.framework.query.common.api.QueryResult;
import com.navis.road.RoadApptsEntity;
import com.navis.road.RoadApptsField;
import com.navis.road.RoadApptsPropertyKeys;
import com.navis.road.business.api.RoadCompoundField;
import com.navis.road.business.appointment.model.GateAppointment;
class ClearApptUnit {
    GroovyApi api = new GroovyApi();
    public String execute(Long apptNbr){
        api.log("解除预约号绑定的箱……")
        api.log("预约号：" + apptNbr)
        List gappts = null;
        GateAppointment appt = null;

        DomainQuery dq = QueryUtils.createDomainQuery(RoadApptsEntity.GATE_APPOINTMENT)
                .addDqPredicate(PredicateFactory.in(RoadApptsField.GAPPT_NBR, 399));
        gappts = Roastery.getHibernateApi().findEntitiesByDomainQuery(dq);
        if (gappts != null && gappts.size() == 1) {
            appt = gappts.get(0);
            api.log("原箱号：" + appt.gapptUnit)
            appt.gapptUnit = null;
            api.log("现箱号：" + appt.gapptUnit)

        }

    }
}
