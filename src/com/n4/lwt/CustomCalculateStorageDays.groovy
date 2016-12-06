package com.n4.lwt

import com.navis.external.framework.util.EFieldChanges
import com.navis.framework.portal.FieldChanges
import com.navis.inventory.InventoryField
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.inventory.external.inventory.AbstractStorageRule

/**
 * Created by lekoxnfx on 15/12/3.
 */
class CustomCalculateStorageDays extends AbstractStorageRule {

    @Override
    Date calculateStorageStartDate(EFieldChanges eFieldChanges) {
        Unit unit = (Unit)((FieldChanges) eFieldChanges).getFieldChange(InventoryField.UFV_UNIT).getNewValue();
        UnitFacilityVisit ufv = (UnitFacilityVisit)unit.getField(InventoryField.UNIT_ACTIVE_UFV)
        Date result;

        Calendar calendar = Calendar.getInstance();
        if(ufv.getUfvTimeIn() == null){
            result = calendar.getTime()
        }
        else {
            calendar.clear()
            calendar.setTime(ufv.getUfvTimeIn())
            int hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (hour<=10){
            }else {
                calendar.add(Calendar.DAY_OF_MONTH ,1)
                calendar.set(Calendar.HOUR_OF_DAY,0)
                calendar.set(Calendar.MINUTE,0)
                calendar.set(Calendar.SECOND,0)
                calendar.set(Calendar.MILLISECOND,0)
            }
            result = calendar.getTime()
        }


        return  result
    }

    @Override
    Date calculateStorageEndDate(EFieldChanges eFieldChanges) {
        return null
    }
}
