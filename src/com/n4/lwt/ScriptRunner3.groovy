package com.n4.lwt

import com.navis.argo.business.atoms.LocTypeEnum
import com.navis.argo.business.model.CarrierVisit
import com.navis.argo.business.model.Complex
import com.navis.argo.business.model.Facility
import com.navis.argo.business.model.Operator
import com.navis.argo.business.model.VisitDetails
import com.navis.argo.business.model.Yard
import com.navis.argo.business.reference.UnLocCode
import com.navis.framework.business.Roastery
import com.navis.framework.portal.FieldChange
import com.navis.framework.portal.FieldChanges
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.inventory.InventoryBizMetafield
import com.navis.inventory.InventoryEntity
import com.navis.inventory.InventoryField
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.atoms.UnitVisitStateEnum
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.reference.business.locale.RefCountry
import com.navis.services.business.event.Event
import com.navis.services.business.event.GroovyEvent
import com.navis.vessel.business.schedule.VesselVisitDetails
import com.navis.vessel.business.schedule.VesselVisitLine

/**
 * Created by liuminhang on 16/6/20.
 */
class ScriptRunner3 {
    public Operator operator = Operator.findOperator("ZSCT")
    public Complex complex = Complex.findComplex("ZST", operator)
    public Facility facility = Facility.findFacility("DLT", complex)
    public Yard yard = Yard.findYard("DLT",facility)

    String resultStr = "";

    public String execute() {
        long cv_gkey;
        String cv_id = "JS1616";
        String ctnNbr = "MSCU6141110"



//        CarrierVisit cv = CarrierVisit.findVesselVisit(facility,cv_id)
//        cv_gkey = cv.getCvGkey()
//        VisitDetails vd = cv.getCvCvd();
//        VesselVisitDetails vvd = VesselVisitDetails.resolveVvdFromCv(cv)
//        if(vvd != null){
//            vvd.getVvdVvlineSet().each{l->
//                VesselVisitLine vvdl = l;
//                resultStr = resultStr + vvdl.getVvlineBizu().getBzuId() + "*"
//            }
//
//        }
//
//        resultStr = resultStr + cv_gkey + "*"
//
//        //查询Unit信息
//        DomainQuery dqUfv = QueryUtils.createDomainQuery(InventoryEntity.UNIT_FACILITY_VISIT);
//        dqUfv.addDqPredicate(PredicateFactory.in(InventoryField.UFV_TRANSIT_STATE,UfvTransitStateEnum.S70_DEPARTED))
//        dqUfv.addDqPredicate(PredicateFactory.in(InventoryField.UFV_ACTUAL_OB_CV,cv_gkey))
//        def res = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqUfv);
//
//
//        res.each {
//            UnitFacilityVisit ufv = it;
//            resultStr = resultStr + ufv.getUfvUnit().getUnitId() + "#"
//
//        }
        DomainQuery dqUnit = QueryUtils.createDomainQuery(InventoryEntity.UNIT);
        dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_VISIT_STATE,UnitVisitStateEnum.ACTIVE))

        dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_ID,ctnNbr))
        def res = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqUnit);

        resultStr = resultStr + res.toString()

        if(res.size()==0){
            resultStr = resultStr + "find none active ufv"
        }
        res.each {
            Unit unit = it;
            unit.setFieldValue(InventoryBizMetafield.UNIT_FLEX_STRING01,"")
//            UnitFacilityVisit ufv = unit.getUnitActiveUfvNowActive()
//            resultStr = resultStr + ufv.getUfvUnit().getUnitId() + "#"

//            UnLocCode unLocCode = null;
//            unLocCode = ufv.getUfvUnit().getUnitRouting().getRtgPOD1().getPointUnLoc()
//            resultStr = resultStr + unLocCode.getUnlocCntry().getId() + "#"
//            if(!unLocCode.getUnlocCntry().equals(RefCountry.findCountry("CN"))){
//                resultStr = resultStr + "Port CN false!" + "#";
//            }

//            if(!unit.getUnitActiveImpediments().contains("VGM_V")){
//                resultStr = resultStr + "VGM_V"
//            }
//
//            double vgmWeight = 3900;
//            FieldChanges fieldChanges = new FieldChanges()
//            fieldChanges.setFieldChange(InventoryField.UNIT_GOODS_AND_CTR_WT_KG,unit.getUnitGoodsAndCtrWtKg(),vgmWeight)
//            unit.updateGoodsAndCtrWtKg(vgmWeight)
//            String postEventName = "UNIT_VGM_UPDATE" //要触发的事件名
//            String note = "VGM重量更新" //注释
//            com.navis.argo.business.api.ServicesManager sm = (com.navis.argo.business.api.ServicesManager) com.navis.framework.business.Roastery.getBean(com.navis.argo.business.api.ServicesManager.BEAN_ID)
//            com.navis.services.business.rules.EventType eventType = com.navis.services.business.rules.EventType.findEventType(postEventName)
//            sm.recordEvent(eventType, note, 1.0, com.navis.argo.business.atoms.ServiceQuantityUnitEnum.UNKNOWN, unit,fieldChanges)
//
//            String applyFlagId = "VGM_V"   //要操作的标志ID
//            String note2 = "依据VGM报文放行,文件名:"
//            sm.applyPermission(applyFlagId,unit,null,null,note2)
          }
        return resultStr



    }
}