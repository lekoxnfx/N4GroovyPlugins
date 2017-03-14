package com.n4.lwt

import com.navis.argo.ArgoBizMetafield
import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.EquipIsoGroupEnum
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.extract.ChargeableUnitEvent
import com.navis.argo.business.reference.LineOperator
import com.navis.argo.business.reference.ScopedBizUnit
import com.navis.framework.business.Roastery
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.Join
import com.navis.framework.portal.query.JoinType
import com.navis.framework.portal.query.PredicateFactory
import com.navis.framework.query.common.SavedQueryFactory
import com.navis.inventory.InventoryField
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.atoms.UnitVisitStateEnum
import com.navis.inventory.business.units.EqBaseOrder
import com.navis.inventory.business.units.EqBaseOrderItem
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.orders.business.eqorders.EquipmentDeliveryOrder
import com.navis.orders.business.eqorders.EquipmentOrder
import com.navis.orders.business.eqorders.EquipmentOrderItem
import com.navis.road.RoadApptsEntity
import com.navis.road.RoadApptsField
import com.navis.inventory.InventoryEntity
import com.navis.road.business.appointment.model.GateAppointment
import com.navis.road.business.atoms.AppointmentStateEnum
import com.navis.road.business.atoms.TranSubTypeEnum
import com.navis.road.business.atoms.TruckerFriendlyTranSubTypeEnum
import com.navis.road.business.model.TruckVisitDetails
import com.navis.services.business.event.GroovyEvent

/**
 * Created by lekoxnfx on 16/1/13.
 */
class ScriptRunner1 {
    GroovyApi api = new GroovyApi()
    String OUTPUT = ""
    public String execute() {

//        queryContainerInbound()
        queryTV()
    }
    public String queryContainerInbound(){

        DomainQuery dqAppt = QueryUtils.createDomainQuery(RoadApptsEntity.GATE_APPOINTMENT);
        dqAppt.addDqPredicate(PredicateFactory.in(RoadApptsField.GAPPT_STATE,AppointmentStateEnum.CREATED))
        dqAppt.addDqPredicate(PredicateFactory.in(RoadApptsField.GAPPT_TRAN_TYPE,TruckerFriendlyTranSubTypeEnum.PUM))
        dqAppt.addDqPredicate(PredicateFactory.in(RoadApptsField.GAPPT_NBR,1216))
        def res = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqAppt);
        String result =""
        res.each {
            GateAppointment gateAppointment = it;
            api.log("预约号:" + gateAppointment.getApptNbr())

            //获取对应信息
            EqBaseOrder eqBaseOrder = gateAppointment.getGapptOrder()
            EqBaseOrderItem eqBaseOrderItem= gateAppointment.getGapptOrderItem()
            EquipmentOrder equipmentOrder = EquipmentDeliveryOrder.resolveEqoFromEqbo(eqBaseOrder)
            EquipmentOrderItem equipmentOrderItem = EquipmentOrderItem.resolveEqoiFromEqboi(eqBaseOrderItem)


            LineOperator lineOperator = LineOperator.resolveLineOprFromScopedBizUnit(equipmentOrder.getEqoLine())
            EquipIsoGroupEnum isoGroupEnum = equipmentOrderItem.getEqoiEqIsoGroup()

            api.log("箱公司:" + lineOperator.getBzuId())
            api.log("箱ISO:" + isoGroupEnum==null?"":isoGroupEnum.getName())
            //查找对应的箱号
            DomainQuery dqUnit = QueryUtils.createDomainQuery(InventoryEntity.UNIT);
            dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_VISIT_STATE,UnitVisitStateEnum.ACTIVE))//活动的记录
            dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_FREIGHT_KIND,FreightKindEnum.MTY))//空箱
            dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_LINE_OPERATOR,lineOperator.getPrimaryKey()))//LineOp

            def units = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqUnit);

            units.each {
                Unit u = it
                u.getUnitActiveUfvNowActive().getUfvTime
                if(isoGroupEnum!=null){//填了箱类型
                    if(u.getUnitPrimaryUe().getUeEquipment().getEqIsoGroup().equals(isoGroupEnum)){//同箱型
                        if(u.getUnitActiveUfvNowActive().getUfvTransitState().equals(UfvTransitStateEnum.S40_YARD)){
                            //在场
                            equipmentOrderItem.reserveEq(u.getUnitPrimaryUe().getUeEquipment())
                            OUTPUT = OUTPUT + u.getUnitId() + ","
                        }
                    }
                }
                else{//箱型没填
                    api.log("没有找到设备订单项目的箱型信息")
                }
            }


        }

        return OUTPUT

    }
    public String queryTV(){
        List<TruckVisitDetails> truckVisitDetailsList = TruckVisitDetails.findTVActiveByTruckLicenseNbr("")
        if(truckVisitDetailsList!=null&&truckVisitDetailsList.size()==1){
            return "found"
        }
    }


}
