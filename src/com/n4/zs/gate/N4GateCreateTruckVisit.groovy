package com.n4.zs.gate

import com.navis.argo.impl.TruckVisitDocumentImpl
import groovy.xml.MarkupBuilder
import java.util.Map;

import org.apache.axis.client.Stub

import com.navis.argo.ContextHelper;
import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.model.Facility;
import com.navis.argo.webservice.types.v1_0.GenericInvokeResponseWsType
import com.navis.argo.webservice.types.v1_0.ScopeCoordinateIdsWsType
import com.navis.road.business.appointment.model.GateAppointment
import com.navis.road.business.model.RoadSequenceProvider
import com.navis.road.business.model.TruckVisitDetails

import com.navis.www.services.argoservice.ArgoServiceLocator;
import com.navis.www.services.argoservice.ArgoServicePort;

/*
 <groovy class-location="database" class-name="CreateTruckVisit">
	 <parameters>
		<parameter id="truck-lic" value="A3PU28"/>
		<parameter id="ingate-stage-id" value="ingate"/>
		<parameter id="unit-ids" value="CNTR1111111,CNTR2222222"/>
		<parameter id="appt-nbrs" value="101,102"/>
	 </parameters>
 </groovy>
  */
 
 /*
 resultXML:
 <create-truck-visit-response>
 <results result="true" />
 <info-message message="">
 <transactions>
	 <transaction nbr="" type="" unit-id="" freight-kind="" type-iso="" line="" gross-weight="" planned-slot="">
 </transactions>
 </create-truck-visit-response>
 
  */
class N4GateCreateTruckVisit {
	GroovyApi api = new GroovyApi();
	static String ARGO_SERVICE_URL = "http://localhost:9080/apex/services/argoservice"
	static String ROAD_SERVICE_URL = "http://localhost:9080/apex/services/roadservice"
	
	//parameters
	String tranUnitIds;
	String tranApptNbrs;
	String truckLic;
	String stageIdIngate ;
	
	TruckVisitDetails tvdtls;
	Long gosTvGkey;
	Long tvGkey;
	String gateId = "DLT GATE"
	
	ArgoServiceLocator argoServiceLocator;
	ArgoServicePort  argoServicePort;

	
	
	ScopeCoordinateIdsWsType scope;
	Facility fcy;
	
	public String execute(Map inParameters){
		
		truckLic = inParameters.get("truck-lic");
		stageIdIngate = inParameters.get("ingate-stage-id");
		tranApptNbrs = inParameters.get("appt-nbrs");
		
		
		argoServiceLocator = new ArgoServiceLocator();
		argoServicePort = argoServiceLocator.getArgoServicePort(new URL(this.ARGO_SERVICE_URL))
		
		scope = new ScopeCoordinateIdsWsType()
		scope.setOperatorId(ContextHelper.getThreadOperator().getOprId())
		scope.setComplexId(ContextHelper.getThreadComplex().getCpxId())
		scope.setFacilityId(ContextHelper.getThreadFacility().getFcyId())
		scope.setYardId(ContextHelper.getThreadYard().getYrdId())
		
		Stub stub = (Stub) argoServicePort
		stub._setProperty(Stub.USERNAME_PROPERTY, "admin")
		stub._setProperty(Stub.PASSWORD_PROPERTY, "admin")
		
		long apptLong = tranApptNbrs.toLong()
		GateAppointment appt = GateAppointment.findGateAppointment(apptLong)
		api.log("啊啊啊啊啊appointment container nbr before:" + appt.getGapptCtrId())
		appt.getGapptOrder().getEqboOrderItems()
		RoadSequenceProvider rsp = new RoadSequenceProvider();
		this.gosTvGkey = rsp.getTruckVisitNextSeqValue();
		def gateXMLWriter = new StringWriter()
		def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)
		gateXMLBuilder.'gate'() {
					'process-truck'() {
						'gate-id'(gateId)
						'stage-id'(this.stageIdIngate)
						'truck'('license-nbr':this.truckLic)
						'appointment-nbrs'{
							'appointment-nbr'(this.tranApptNbrs)
						}
					}
		}
		String xml = gateXMLWriter.toString()
		api.log("create Truck Visit XML\r\n" + xml)
		GenericInvokeResponseWsType response =  argoServicePort.genericInvoke(scope, xml)
		
		String responseXML = response.getCommonResponse().getQueryResults(0).getResult()
		api.log("Response XML:\n\r" + responseXML)
		
		def res = new XmlParser().parseText(responseXML)
		res.'create-truck-visit-response'.'truck-visit'.each {tv->
			this.tvGkey = tv.'@tv-key'.toLong()
		}
		api.log("TruckVisit Gkey:" + this.tvGkey)
		api.log("啊啊啊啊啊啊appointment container nbr after:" + appt.getGapptCtrId())


		
		
//		CreateTruckVisitResponseWSType ctWSResp = roadServicePort.createTruckVisit(scope, gateId, "", truckLic, "", "", "")
//		this.tvGkey = ctWSResp.getTruckVisitId()
//		tvdtls = TruckVisitDetails.findTruckVisitByGkey(tvGkey)
//		tvdtls.setTvdtlsGosTvKey(gosTvGkey)
		
		
	}

}
