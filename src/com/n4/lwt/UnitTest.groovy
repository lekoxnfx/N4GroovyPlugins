package com.n4.lwt

import com.navis.argo.business.api.GroovyApi
import com.navis.inventory.business.units.Unit
import com.navis.services.business.event.GroovyEvent


class UnitTest {


    public void execute(GroovyEvent inEvent,GroovyApi inApi){

        Unit unit = (Unit)inEvent.getEntity()

        File file = new File("C:\\UnitTest.txt")
        file.write("hahahahaha" + unit.getUnitId())

    }

//    public void execute(Map args){
//
//        File file = new File("C:\\UnitTest.txt")
//        file.write("hahahahaha")
//
//    }

}
