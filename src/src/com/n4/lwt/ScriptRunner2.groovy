package src.com.n4.lwt

import com.navis.argo.ArgoBizMetafield
import com.navis.argo.business.model.CarrierVisit
import com.navis.argo.business.model.Complex
import com.navis.argo.business.model.Facility
import com.navis.argo.business.model.Operator
import com.navis.argo.business.model.Yard
import com.navis.argo.business.reference.RoutingPoint
import com.navis.argo.business.reference.ScopedBizUnit
import com.navis.inventory.InventoryField
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.vessel.business.atoms.StowageSchemeEnum
import com.navis.vessel.business.schedule.VesselVisitDetails

/**
 * Created by lekoxnfx on 16/1/13.
 */
class ScriptRunner2 {
    public Operator operator = Operator.findOperator("ZSCT")
    public Complex complex = Complex.findComplex("ZST", operator)
    public Facility facility = Facility.findFacility("DLT", complex)
    public Yard yard = Yard.findYard("DLT",facility)
    public String execute() {


        String vvid = "LH526001"

        CarrierVisit cv = CarrierVisit.findVesselVisit(facility,vvid)
        VesselVisitDetails vvd = VesselVisitDetails.resolveVvdFromCv(cv)

        if(vvd.getVvdVessel().getVesStowageScheme().equals(StowageSchemeEnum.ISO)){
            return "OK"

        }





    }
}
