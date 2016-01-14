package com.n4.lwt

import com.navis.argo.ArgoBizMetafield
import com.navis.argo.business.reference.LineOperator
import com.navis.argo.business.reference.ScopedBizUnit
import com.navis.inventory.business.units.UnitFacilityVisit

/**
 * Created by lekoxnfx on 16/1/13.
 */
class ScriptRunner1 {
    public String execute() {
        long ufvGkey = 9083838
//        long ufvGkey = 9125449
        UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufvGkey)
//        LineOperator lineOperator = (LineOperator)ufv.getUfvUnit().getUnitLineOperator();
        ScopedBizUnit sbu = ufv.getUfvUnit().getUnitLineOperator();

        if(sbu.getField(ArgoBizMetafield.LINEOP_FLEX_STRING01) == "外贸"){
                    return "外贸"
        }
        else return "内贸"

    }
}
