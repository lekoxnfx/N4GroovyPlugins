package com.n4.zs.gate

import com.navis.argo.business.api.ArgoServicesUtils
import com.navis.argo.webservice.ArgoWebServices
import com.navis.framework.business.Roastery
import com.navis.framework.portal.FieldChanges
import com.navis.road.business.api.RoadManager
import com.navis.road.business.apihandler.ProcessTruckHandler
import com.navis.road.business.apihandler.SubmitTransactionHandler
import com.navis.road.business.apihandler.TruckGatePosition
import com.navis.road.business.atoms.GateClientTypeEnum
import com.navis.road.business.atoms.TranSubTypeEnum
import com.navis.road.business.model.Gate
import com.navis.road.business.model.RoadManagerPea
import com.navis.road.business.model.Truck
import com.navis.road.business.model.TruckTransaction
import com.navis.road.business.model.TruckVisitDetails
import com.navis.road.business.workflow.GateConfigFactory
import com.navis.road.business.workflow.TransactionAndVisitHolder
import com.navis.road.business.workflow.TruckActionsHolder
import com.navis.road.portal.configuration.CachedGateConfiguration
import com.navis.road.portal.configuration.CachedGateStage
import com.navis.road.webservice.GateWebservice
import com.navis.www.services.argoservice.ArgoServiceLocator
import com.navis.www.services.argoservice.ArgoServicePort

/**
 * Created by lekoxnfx on 2017/3/27.
 */
class ScriptRunnerGate1 {

    public String execute(){

        return doTest();

    }
    public String doTest(){

        Gate gate = Gate.findGateById("DLT GATE", true);
        CachedGateConfiguration config = GateConfigFactory.getGateConfiguration(gate, false, true);
        CachedGateStage stage = config.getStage("ingate");

        Truck truck = Truck.findTruckById("A3PU28")
        TruckVisitDetails truckVisitDetails = TruckVisitDetails.create(gate,truck.getTruckTrkCo(),truck.getTruckLicenseNbr());
        FieldChanges tranFieldChanges = new FieldChanges();
        FieldChanges nonTranFieldChanges = new FieldChanges();
        String pin = null;
        Long apptNbr = 1841L;
        TruckActionsHolder truckActionsHolder = new TruckActionsHolder();
        RoadManager roadManager = (RoadManager)Roastery.getBean("roadManager");


        TruckTransaction tran1 = ((RoadManager)Roastery.getBean("roadManager")).submitTransactionByAppointment(gate,stage,truckVisitDetails,tranFieldChanges,nonTranFieldChanges,apptNbr, (Map)null, pin,null,truckActionsHolder,GateClientTypeEnum.AUTOGATE);
//        TruckTransaction tran2 = ((RoadManager)Roastery.getBean("roadManager")).submitTransactionByAppointment(gate,stage,truckVisitDetails,tranFieldChanges,nonTranFieldChanges,apptNbr, (Map)null, pin,null,truckActionsHolder,GateClientTypeEnum.AUTOGATE);


//        TruckTransaction tran1 = TruckTransaction.create(truckVisitDetails,TranSubTypeEnum.DM)
//        tran1.setTranAppointmentNbr("1841")
//
//        TransactionAndVisitHolder tranRes1 = roadManager.submitTransaction(tran1,tranFieldChanges,nonTranFieldChanges,gate,GateClientTypeEnum.AUTOGATE,null)


//        RoadManagerPea


        return  tran1.getPrimaryKey() + " "// + tranRes1.getTran().getPrimaryKey() + " " //+ tran2.getPrimaryKey()
        SubmitTransactionHandler

    }



}
