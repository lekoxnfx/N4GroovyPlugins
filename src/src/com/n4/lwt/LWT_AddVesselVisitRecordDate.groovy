package src.com.n4.lwt

import com.navis.argo.business.api.GroovyApi
import com.navis.services.business.event.GroovyEvent
import com.navis.vessel.VesselBizMetafield
import com.navis.vessel.business.schedule.VesselVisitDetails
import com.navis.argo.business.model.CarrierVisit

/**
 * 船舶离港时候,记录登记时间为当前离港时间
 * Created by Badger on 16/1/20.
 */
class LWT_AddVesselVisitRecordDate extends GroovyApi {
    private GroovyEvent event
    private GroovyApi api
    private String notes

    public void execute(GroovyEvent inEvent, GroovyApi inApi) {
        this.event = inEvent
        this.api = inApi
        process()
    }

    private void process() {
        Date vesselATD

        VesselVisitDetails visitDetails = (VesselVisitDetails) this.event.getEntity()
        if (!visitDetails) {
            notes = "获取不到船期信息，vvd为空！";
            api.log(notes)
        } else {
            CarrierVisit carrierVisit = visitDetails.getCvdCv()
            if (carrierVisit) {
                vesselATD = carrierVisit.getCvATD()
                //如果当前登记时间不为空,则不去修改
                if (!visitDetails.getVvFlexDate01()) {
                    visitDetails.setFieldValue(VesselBizMetafield.VV_FLEX_DATE01, vesselATD)
                }
            }
        }
    }

}
