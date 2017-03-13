package src.com.n4.lwt

import com.navis.argo.business.reference.RoutingPoint
import com.navis.inventory.business.units.UnitFacilityVisit

/**
 * Created by lekoxnfx on 16/1/21.
 */
class CopyNotesToPOD {
    boolean result = true;
    String info;

    public void copyNotesToPod(List<Serializable> inUfvGkeys){

        inUfvGkeys.each{ ufvGkey->
            UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufvGkey)
            String notes = ufv.getUfvUnit().getUnitGoods().getGdsDestination()

            RoutingPoint routingPoint = RoutingPoint.findRoutingPoint(notes)
            if(routingPoint == null){
                result = false;
                logInfo("箱号" + ufv.getUfvUnit().getUnitId() + "执行失败,找不到该港口:" + notes)
            }
            else {
                try{
                    ufv.getUfvUnit().getUnitRouting().setRtgPOD1(routingPoint)
                    logInfo("箱号" + ufv.getUfvUnit().getUnitId() + "执行成功，卸货港：" + ufv.getUfvUnit().getUnitRouting().getRtgPOD1().getPointId())
                }catch(Exception e){
                    result = false;
                    logInfo("箱号" + ufv.getUfvUnit().getUnitId() + "执行失败，" + e.toString())
                }
            }
        }

    }
    public String returnTest(){
        return "TEST"
    }
    public void logInfo(String inInfo){
        info = info + info + "\n"
    }


}
