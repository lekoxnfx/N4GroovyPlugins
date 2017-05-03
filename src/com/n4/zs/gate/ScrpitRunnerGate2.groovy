package com.n4.zs.gate

import com.navis.framework.util.unit.LengthUnit
import com.navis.orders.business.eqorders.EquipmentOrderItem
import com.navis.road.business.appointment.model.GateAppointment
import com.navis.road.business.atoms.TruckerFriendlyTranSubTypeEnum

/**
 * Created by lekoxnfx on 2017/3/29.
 */
class ScrpitRunnerGate2 {
    public String execute() {
        GateAppointment appointment = GateAppointment.findGateAppointment(1841L)


        EquipmentOrderItem equipmentOrderItem = EquipmentOrderItem.hydrate(appointment.getGapptOrderItem().getPrimaryKey())
//        return equipmentOrderItem.getEqoiEqSize().getValueInUnits(LengthUnit.FEET)
//        return equipmentOrderItem.getEqoiEqHeight().getValueInUnits(LengthUnit.FEET)
        return equipmentOrderItem.getEqoiSampleEquipType().getEqtypId()

    }
}
