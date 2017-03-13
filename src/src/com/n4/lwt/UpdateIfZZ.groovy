package src.com.n4.lwt

import com.navis.argo.business.api.GroovyApi
import com.navis.inventory.business.units.Unit
import com.navis.services.business.event.GroovyEvent

/**
 * Created by lekoxnfx on 15/9/28.
 * 当Unit导入时用于判断是否是中转箱
 * 当pol!=WNZ,pod!=WNZ,判断为中转箱
 */
class UpdateIfZZ {
    public void execute(GroovyEvent inEvent,GroovyApi inApi){
        inApi.log("判断是否是中转箱")
        Unit u = (Unit)inEvent.getEntity();
        if(u==null){
            inApi.log("获取Unit失败")

        }
        else{
            inApi.log("Unit Id:" + u.unitId)

            String pol,pod;

            try{
                pol = u.getUnitRouting().getRtgPOL().getPointId()

            }catch (Exception e){
                inApi.log("pol获取失败，为空？")
            }
            try{
                pod = u.getUnitRouting().getRtgPOD1().getPointId()

            }catch (Exception e){
                inApi.log("pod获取失败，为空？")
            }

            inApi.log("POL:" + pol + ";POD:" + pod)

            if (!pol.equals("WNZ")&&!pod.equals("WNZ")){
                inApi.log("判断为中转箱，做标记")
                u.unitFlexString11 = "中转"
            }
            else {
                inApi.log("判断为非中转箱")
            }
        }





    }

}
