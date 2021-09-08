package com.n4.zs.gate

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.inventory.business.moves.WorkInstruction
import com.navis.road.business.api.GroovyRoadApi
import com.navis.road.business.model.TruckTransaction
import com.navis.road.business.workflow.IUnitBean
import com.navis.road.business.workflow.TransactionAndVisitHolder

/**
 * Created by lekoxnfx on 15/7/2.
 */
class BiznessTaskTransferWI extends BizTaskInterceptor{
    @Override
    void intercept(TransactionAndVisitHolder inDao, IUnitBean inUnitBean, GroovyRoadApi api) {
        super.intercept(inDao, inUnitBean, api)
        com.navis.road.business.model.TruckTransaction trans = inDao.getTran()
        api.log("检查交换2,tranUnitId:" + trans.getTranUnit().getUnitId()+",tranGkey:" + trans.getTranGkey()
                    + ",tranNbr:" + trans.getTranNbr()
                    + ",transTvdtlsBatNbr:" + trans.getTranTruckVisit().getTvdtlsBatNbr())
        try{
            List wiList =
                    com.navis.inventory.business.moves.WorkInstruction.findGateMovesForTruckTransaction(trans.getTranGkey())
            wiList.each{com.navis.inventory.business.moves.WorkInstruction wi->
                api.log("获取到WI")
                api.log("刷新前")
                api.log("WI描述:" + wi.describeWi())
                String propsStr = "WI 属性：\n"
                wi.getProperties().each {k,v->
                    propsStr = propsStr.concat(k.toString()+":"+v.toString()+"\n")
                }
                api.log(propsStr)
                com.navis.framework.business.Roastery.getHibernateApi().refresh(wi)
                api.log("刷新后")
                api.log("WI描述:" + wi.describeWi())
                String propsStr2 = "WI 属性：\n"
                wi.getProperties().each {k,v->
                    propsStr2 = propsStr2.concat(k.toString()+":"+v.toString()+"\n")
                }
                api.log(propsStr2)
            }
        }
        catch (Exception e){
            api.logWarn(e.toString())
        }

    }
}