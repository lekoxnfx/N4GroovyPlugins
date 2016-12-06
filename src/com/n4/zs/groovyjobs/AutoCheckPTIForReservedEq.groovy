package com.n4.zs.groovyjobs

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.EquipIsoGroupEnum
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.reference.Equipment
import com.navis.argo.business.reference.LineOperator
import com.navis.framework.business.Roastery
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.inventory.InventoryEntity
import com.navis.inventory.InventoryField
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.atoms.UnitVisitStateEnum
import com.navis.inventory.business.units.EqBaseOrder
import com.navis.inventory.business.units.EqBaseOrderItem
import com.navis.inventory.business.units.Unit
import com.navis.orders.business.eqorders.EquipmentDeliveryOrder
import com.navis.orders.business.eqorders.EquipmentOrder
import com.navis.orders.business.eqorders.EquipmentOrderItem
import com.navis.road.RoadApptsEntity
import com.navis.road.RoadApptsField
import com.navis.road.RoadEntity
import com.navis.road.business.appointment.model.GateAppointment
import com.navis.road.business.atoms.AppointmentStateEnum
import com.navis.road.business.atoms.TranSubTypeEnum
import com.navis.road.business.atoms.TruckerFriendlyTranSubTypeEnum

/**
 * Created by liuminhang on 16/3/2.
 */
class AutoCheckPTIForReservedEq {
    GroovyApi api = new GroovyApi();

    public void execute(Map args){
        api.log("开始为提空箱预约绑定优先提的箱")
        api.log("查询活动的提空箱预约")
        DomainQuery dqAppt = QueryUtils.createDomainQuery(RoadApptsEntity.GATE_APPOINTMENT);
        dqAppt.addDqPredicate(PredicateFactory.in(RoadApptsField.GAPPT_STATE,AppointmentStateEnum.CREATED))
        dqAppt.addDqPredicate(PredicateFactory.in(RoadApptsField.GAPPT_TRAN_TYPE,TruckerFriendlyTranSubTypeEnum.PUM))
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
            dqAppt.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_VISIT_STATE,UnitVisitStateEnum.ACTIVE))//活动的记录
            dqAppt.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_FREIGHT_KIND,FreightKindEnum.MTY))//空箱
            dqAppt.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_LINE_OPERATOR,lineOperator))//LineOp

            def units = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqUnit);

            units.each {
                Unit u = it
                if(isoGroupEnum!=null){//填了箱类型
                    if(u.getUnitPrimaryUe().getUeEquipment().getEqIsoGroup().equals(isoGroupEnum)){//同箱型
                        if(u.getUnitActiveUfvNowActive().getUfvTransitState().equals(UfvTransitStateEnum.S40_YARD)){
                            //在场
                            equipmentOrderItem.reserveEq(u.getUnitPrimaryUe().getUeEquipment())
                        }
                    }
                }
                else{//箱型没填
                    api.log("没有找到设备订单项目的箱型信息")
                }


            }


        }



    }
}
