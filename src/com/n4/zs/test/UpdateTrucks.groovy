package com.n4.zs.test

import com.navis.framework.business.Roastery
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.inventory.InventoryBizMetafield
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.road.RoadBizMetafield
import com.navis.road.business.appointment.model.GateAppointment
import com.navis.road.business.model.Truck

/**
 * Created by liuminhang on 2016/12/6.
 */
class UpdateTrucks {

    public String execute(){
        DomainQuery dq = QueryUtils.createDomainQuery("Truck")
        List<Truck> truckList = Roastery.getHibernateApi().findEntitiesByDomainQuery(dq)
        String result="trucks:\n";
        truckList.each {Truck t->
//            result = result.concat("id:"+t.getTruckId()
//                    +",lic:"+ t.getTruckLicenseNbr()
//                    +",bat:"+ t.getTruckBatNbr() )
            if(t.getTruckBatNbr()!=null||t.getTruckBatNbr()!=""){
                if(t.getTruckId().equals(t.getTruckBatNbr())){
                    t.setTruckId(t.getTruckLicenseNbr())
                    t.setTruckLicenseNbr(t.getTruckLicenseNbr()+"-"+t.getTruckBatNbr())
                }
            }
        }
        return  result


    }

}
