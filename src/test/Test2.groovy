package test

import groovy.xml.MarkupBuilder

import java.sql.ResultSet
import java.text.SimpleDateFormat

class Test2 {

	static main(args) {

        String tvGosKey = getGosTvKey();
        if(true){
            def gateXMLWriter = new StringWriter()
            def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)
            //创建truckVisit
            println("创建TruckVisit")
            gateXMLBuilder.'gate'(){
                'create-truck-visit'(){
                    'gate-id'("DLT GATE")
                    'stage-id'("ingate")
                    'truck'('license-nbr':"A3PU28")
                    'truck-visit'('gos-tv-key':tvGosKey)
                }
            }
            String xmlTv = gateXMLWriter.toString()
            println(xmlTv)

            def gateResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <argo:gate-response xmlns:argo="http://www.navis.com/argo" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.navis.com/argo GateWebserviceResponse.xsd">
              <create-truck-visit-response action="TROUBLE_LANE">
                <truck-visit tv-key="3220646" gos-tv-key="20170323111412454" next-stage-id="ingate" status="TROUBLE" action="TROUBLE_LANE" gate-id="DLT GATE" entered="2017-03-23T11:16:01">
                  <messages locale="en">
                    <message message-id="gate.truck_license_unknown" message-text="Truck with license nbr A3PU29 is unknown" message-severity="SEVERE" />
                  </messages>
                  <actions overall-id="TROUBLE_LANE">
                    <action id="TROUBLE_LANE" />
                  </actions>
                </truck-visit>
              </create-truck-visit-response>
            </argo:gate-response> 
            """.trim()

            def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))
            String gateStatus = root.'create-truck-visit-response'[0].'truck-visit'[0]["@status"]
            def gateMessage1 = root.'create-truck-visit-response'[0].'truck-visit'.'messages'.'message'["@message-text"]
            String tvGkey;
            root.'create-truck-visit-response'.'truck-visit'.each { tv ->
                tvGkey = tv.'@tv-key'.toLong()
            }
            println(tvGkey)
            println(gateStatus)
            println(gateMessage1)
        }

        if(true){
            def gateXMLWriter = new StringWriter()
            def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)

            gateXMLBuilder.'gate'(){
                'submit-transaction'(){
                    'gate-id'("DLT GATE")
                    'stage-id'("ingate")
                    'truck-visit'('gos-tv-key':tvGosKey)
                    'truck-transaction'('appointment-nbr':"1183",notes:"notes")
                }
            }
            String xmlTv = gateXMLWriter.toString()
            println(xmlTv)

            def gateResponse = """
<?xml version="1.0" encoding="UTF-8"?>
<argo:gate-response xmlns:argo="http://www.navis.com/argo" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.navis.com/argo GateWebserviceResponse.xsd">
  <submit-transaction-response>
    <truck-visit tv-key="3220568" gos-tv-key="20100901001" is-internal="false" next-stage-id="ingate" status="TROUBLE" action="TROUBLE_LANE" gate-id="DLT GATE" entered="2017-03-23T10:09:55">
      <truck id="A3PU28" license-nbr="A3PU28" />
      <chassis-profile id="40" />
    </truck-visit>
    <truck-transactions>
      <truck-transaction tran-key="3220591" tran-nbr="25199" tv-key="3220568" tran-type="PUM" category="STRGE" freight-kind="MTY" next-stage-id="ingate" gate-id="DLT GATE" appointment-nbr="1841" order-nbr="40RH(2)" status="TROUBLE" action="TROUBLE_LANE" is-hazard="false">
        <container eqid="CBHU2823289" type="4530" is-sealed="false" gross-weight="3800.0" line-id="COS" carrier-visit-id="GEN_TRUCK" departure-order-nbr="40RH(2)" has-documents="false" is-placarded="false">
          <routing pod="CNZOS" pod2="CNZOS" />
        </container>
        <eq-order order-nbr="40RH(2)" line-id="COS" freight-kind="MTY">
          <eq-order-items>
            <eq-order-item eq-length="40.0" eq-length-name="40'" eq-iso-group="RE" eq-iso-group-name="Refrigerated container" eq-height="9.501312335958005" eq-height-name="9'6&quot;" />
          </eq-order-items>
        </eq-order>
        <documents>
          <document doc-key="3220599" type="TROUBLE">
            <content><![CDATA[<argo:Document xmlns:argo="http://www.navis.com/argo">
              <argo:docDescription>
                <docName>TROUBLE</docName>
                <docType>2454857</docType>
              </argo:docDescription>
              <argo:docBody>
                <argo:truckVisit>
                  <tvdtlsLicNbr>A3PU28</tvdtlsLicNbr>
                  <tvdtlsTruckId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsDriverCardId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsDriverName xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsDriverLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsBatNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsTvKey>3220568</tvdtlsTvKey>
                  <tvdtlsGosTvKey>20100901001</tvdtlsGosTvKey>
                  <tvdtlsTrkStartTime>2017-03-23 10:09:55</tvdtlsTrkStartTime>
                  <tvdtlsFlexString01 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsFlexString02 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsFlexString03 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <argo:tvdtlsRequiredStages>
                    <stageId>ingate</stageId>
                    <stageOrder>1</stageOrder>
                    <stageDescription>ingate</stageDescription>
                  </argo:tvdtlsRequiredStages>
                  <tvdtlsAppointmentNbr>null</tvdtlsAppointmentNbr>
                  <argo:tvdtlsAnnouncements/>
                </argo:truckVisit>
                <argo:trkTransaction>
                  <argo:tranCtrOperator>
                    <bizuId>COS</bizuId>
                  </argo:tranCtrOperator>
                  <argo:tranEqo>
                    <eqboNbr>40RH(2)</eqboNbr>
                    <eqoLineId>COS</eqoLineId>
                    <eqoLineName>中国远洋</eqoLineName>
                  </argo:tranEqo>
                  <argo:tranEqoItem>
                    <eqoiEqHeight>NOM96</eqoiEqHeight>
                    <eqoiEqSize>NOM40</eqoiEqSize>
                    <eqoiEqIsoGroup>RE</eqoiEqIsoGroup>
                    <eqoiCscExpiration xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <eqoiSerialRanges xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <eqoiRemarks xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  </argo:tranEqoItem>
                  <argo:tranCarrierVisit>
                    <cvId>GEN_TRUCK</cvId>
                  </argo:tranCarrierVisit>
                  <argo:tranDischargePoint1>
                    <pointId>CNZOS</pointId>
                  </argo:tranDischargePoint1>
                  <argo:tranDischargePoint2>
                    <pointId>CNZOS</pointId>
                  </argo:tranDischargePoint2>
                  <argo:tranFacility>
                    <fcyId>DLT</fcyId>
                  </argo:tranFacility>
                  <argo:tranComplex>
                    <cpxId>ZST</cpxId>
                  </argo:tranComplex>
                  <argo:tranOperator>
                    <oprId>ZSCT</oprId>
                  </argo:tranOperator>
                  <tranNbr>25199</tranNbr>
                  <tranExchangeAreaId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranSubType>DM</tranSubType>
                  <tranTruckerFriendlySubType>PUM</tranTruckerFriendlySubType>
                  <tranStatus>TROUBLE</tranStatus>
                  <tranCreated>2017-3-23 上午10:13</tranCreated>
                  <tranCreator>admin</tranCreator>
                  <tranStageId>ingate</tranStageId>
                  <tranNextStageId>ingate</tranNextStageId>
                  <tranTrouble xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTroubleStatus xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranHadTrouble>true</tranHadTrouble>
                  <tranCancelCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranNotes xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTerminalId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranExportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranImportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranAppointmentNbr>1841</tranAppointmentNbr>
                  <tranPinNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTrkcId>FYJY</tranTrkcId>
                  <tranCtrTruckPosition xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrDoorDirection xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrFreightKind>MTY</tranCtrFreightKind>
                  <tranCtrTypeId>4530</tranCtrTypeId>
                  <tranCtrIsDamaged>false</tranCtrIsDamaged>
                  <tranCtrGrossWeight>3800.0</tranCtrGrossWeight>
                  <tranCtrNbrAssigned>CBHU2823289</tranCtrNbrAssigned>
                  <tranCtrTicketPosId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsIsOwners>false</tranChsIsOwners>
                  <tranChsTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsIsDamaged>false</tranChsIsDamaged>
                  <tranChsLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsNbrAssigned xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTradeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranOrigin xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranDestination xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranLineId>COS</tranLineId>
                  <tranShipper xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranConsignee xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranScAgent xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranEqoEqIsoGroup>RE</tranEqoEqIsoGroup>
                  <tranEqoEqLength>NOM40</tranEqoEqLength>
                  <tranEqoEqHeight>NOM96</tranEqoEqHeight>
                  <tranEqoNbr>40RH(2)</tranEqoNbr>
                  <tranMilitaryVNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranMilitaryTcn xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShandId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShandId2 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShandId3 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranGroupId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranIdoId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCommodityCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCommodityDescription xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranVentRequired>null</tranVentRequired>
                  <tranVentSetting>null</tranVentSetting>
                  <tranIsHazard>false</tranIsHazard>
                  <tranIsPlacarded>false</tranIsPlacarded>
                  <tranIsPlacardedOk>false</tranIsPlacardedOk>
                  <tranIsOog>false</tranIsOog>
                  <tranCtrIsSealed>false</tranCtrIsSealed>
                  <tranIsXrayRequired>false</tranIsXrayRequired>
                  <tranCscDate xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranUnitCategory>UnitCategoryEnum[STRGE]</tranUnitCategory>
                  <argo:tranRequiredStages>
                    <stageId>ingate</stageId>
                    <stageSequence>1</stageSequence>
                    <stageDescription>ingate</stageDescription>
                  </argo:tranRequiredStages>
                  <tranStartTime>2017-03-23 10:13:39</tranStartTime>
                  <tranAppointmentState>CREATED</tranAppointmentState>
                </argo:trkTransaction>
              </argo:docBody>
              <argo:Messages locale="en">
                <argo:Message>
                  <errKey>XPS_TPC_ERROR</errKey>
                  <message>Internal error connecting to XPS</message>
                  <severity>SEVERE</severity>
                </argo:Message>
                <argo:Message>
                  <errKey>gate.matching_ctr_not_found</errKey>
                  <message>No suitable equipment found.</message>
                  <severity>SEVERE</severity>
                </argo:Message>
              </argo:Messages>
              </argo:Document>]]></content>
            </document>
          </documents>
          <messages locale="en">
            <message message-id="gate.matching_ctr_not_found" message-text="No suitable equipment found." message-severity="SEVERE" />
            <message message-id="XPS_TPC_ERROR" message-text="Internal error connecting to XPS" message-severity="SEVERE" />
          </messages>
          <actions overall-id="TROUBLE_LANE">
            <action id="TROUBLE_LANE" />
          </actions>
        </truck-transaction>
      </truck-transactions>
    </submit-transaction-response>
  </argo:gate-response>

            """.trim()

            def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))
            String gateStatus = root.'submit-transaction-response'[0].'truck-visit'[0]["@status"]
            def gateMessage1 = root.'submit-transaction-response'[0].'truck-transactions'.'truck-transaction'.'messages'.'message'["@message-text"]
            println(gateStatus)
            println(gateMessage1)
        }
        if(true){
            def gateXMLWriter = new StringWriter()
            def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)
            gateXMLBuilder.'gate'(){
                'submit-transaction'(){
                    'gate-id'("DLT GATE")
                    'stage-id'("ingate")
                    'truck-visit'('gos-tv-key':tvGosKey)
                    'truck-transaction'('tran-type':'RM'){
                        'container'('eqid':'AABB2233445')
                    }
                }
            }
            String xmlTv = gateXMLWriter.toString()
            println(xmlTv)

            def gateResponse = """
<?xml version="1.0" encoding="UTF-8"?>
<argo:gate-response xmlns:argo="http://www.navis.com/argo" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.navis.com/argo GateWebserviceResponse.xsd">
  <submit-transaction-response>
    <truck-visit tv-key="3220568" gos-tv-key="20100901001" is-internal="false" next-stage-id="ingate" status="TROUBLE" action="TROUBLE_LANE" gate-id="DLT GATE" entered="2017-03-23T10:09:55">
      <truck id="A3PU28" license-nbr="A3PU28" />
      <chassis-profile id="40" />
    </truck-visit>
    <truck-transactions>
      <truck-transaction tran-key="3220667" tran-nbr="25201" tv-key="3220568" tran-type="DOM" category="STRGE" next-stage-id="ingate" gate-id="DLT GATE" status="TROUBLE" action="TROUBLE_LANE" is-hazard="false">
        <container eqid="AABB2233445" is-sealed="false" carrier-visit-id="GEN_CARRIER" has-documents="false" is-placarded="false">
          <routing />
        </container>
        <documents>
          <document doc-key="3220669" type="TROUBLE">
            <content><![CDATA[<argo:Document xmlns:argo="http://www.navis.com/argo">
              <argo:docDescription>
                <docName>TROUBLE</docName>
                <docType>2454857</docType>
              </argo:docDescription>
              <argo:docBody>
                <argo:truckVisit>
                  <tvdtlsLicNbr>A3PU28</tvdtlsLicNbr>
                  <tvdtlsTruckId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsDriverCardId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsDriverName xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsDriverLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsBatNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsTvKey>3220568</tvdtlsTvKey>
                  <tvdtlsGosTvKey>20100901001</tvdtlsGosTvKey>
                  <tvdtlsTrkStartTime>2017-03-23 10:09:55</tvdtlsTrkStartTime>
                  <tvdtlsFlexString01 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsFlexString02 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsFlexString03 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <argo:tvdtlsRequiredStages>
                    <stageId>ingate</stageId>
                    <stageOrder>1</stageOrder>
                    <stageDescription>ingate</stageDescription>
                  </argo:tvdtlsRequiredStages>
                  <tvdtlsAppointmentNbr>null</tvdtlsAppointmentNbr>
                  <argo:tvdtlsAnnouncements/>
                </argo:truckVisit>
                <argo:trkTransaction>
                  <argo:tranCtrOperator/>
                  <argo:tranCarrierVisit>
                    <cvId>GEN_CARRIER</cvId>
                  </argo:tranCarrierVisit>
                  <argo:tranFacility>
                    <fcyId>DLT</fcyId>
                  </argo:tranFacility>
                  <argo:tranComplex>
                    <cpxId>ZST</cpxId>
                  </argo:tranComplex>
                  <argo:tranOperator>
                    <oprId>ZSCT</oprId>
                  </argo:tranOperator>
                  <tranNbr>25201</tranNbr>
                  <tranExchangeAreaId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranSubType>RM</tranSubType>
                  <tranTruckerFriendlySubType>DOM</tranTruckerFriendlySubType>
                  <tranStatus>TROUBLE</tranStatus>
                  <tranCreated>2017-3-23 下午12:04</tranCreated>
                  <tranCreator>admin</tranCreator>
                  <tranStageId>ingate</tranStageId>
                  <tranNextStageId>ingate</tranNextStageId>
                  <tranTrouble xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTroubleStatus xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranHadTrouble>true</tranHadTrouble>
                  <tranCancelCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranNotes xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTerminalId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranExportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranImportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranAppointmentNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranPinNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTrkcId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrTruckPosition xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrDoorDirection xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrNbr>AABB2233445</tranCtrNbr>
                  <tranCtrOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrIsDamaged>false</tranCtrIsDamaged>
                  <tranCtrNbrAssigned xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrTicketPosId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsIsOwners>false</tranChsIsOwners>
                  <tranChsTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsIsDamaged>false</tranChsIsDamaged>
                  <tranChsLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsNbrAssigned xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTradeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranOrigin xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranDestination xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranLineId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShipper xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranConsignee xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranScAgent xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranEqoEqIsoGroup>GP</tranEqoEqIsoGroup>
                  <tranEqoEqLength>NOM20</tranEqoEqLength>
                  <tranEqoEqHeight>NOM86</tranEqoEqHeight>
                  <tranEqoNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranMilitaryVNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranMilitaryTcn xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShandId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShandId2 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShandId3 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranGroupId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranIdoId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCommodityCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCommodityDescription xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranVentRequired>null</tranVentRequired>
                  <tranVentSetting>null</tranVentSetting>
                  <tranIsHazard>false</tranIsHazard>
                  <tranIsPlacarded>false</tranIsPlacarded>
                  <tranIsPlacardedOk>false</tranIsPlacardedOk>
                  <tranIsOog>false</tranIsOog>
                  <tranCtrIsSealed>false</tranCtrIsSealed>
                  <tranIsXrayRequired>false</tranIsXrayRequired>
                  <tranCscDate xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranUnitCategory>UnitCategoryEnum[STRGE]</tranUnitCategory>
                  <argo:tranRequiredStages>
                    <stageId>ingate</stageId>
                    <stageSequence>1</stageSequence>
                    <stageDescription>ingate</stageDescription>
                  </argo:tranRequiredStages>
                  <tranStartTime>2017-03-23 12:04:27</tranStartTime>
                </argo:trkTransaction>
              </argo:docBody>
              <argo:Messages locale="en">
                <argo:Message>
                  <errKey>XPS_TPC_ERROR_POSITION</errKey>
                  <message>Requesting position for AABB2233445 - NO RESPONSE</message>
                  <severity>SEVERE</severity>
                </argo:Message>
              </argo:Messages>
              </argo:Document>]]></content>
            </document>
          </documents>
          <messages locale="en">
            <message message-id="XPS_TPC_ERROR_POSITION" message-text="Requesting position for AABB2233445 - NO RESPONSE" message-severity="SEVERE" />
          </messages>
          <actions overall-id="TROUBLE_LANE">
            <action id="TROUBLE_LANE" />
          </actions>
        </truck-transaction>
      </truck-transactions>
    </submit-transaction-response>
  </argo:gate-response>


            """.trim()

            def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))
            String gateStatus = root.'submit-transaction-response'[0].'truck-visit'[0]["@status"]
            def gateMessage1 = root.'submit-transaction-response'[0].'truck-transactions'.'truck-transaction'.'messages'.'message'["@message-text"]

            println(gateStatus)
            println(gateMessage1)

        }
        if(true){
            def gateXMLWriter = new StringWriter()
            def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)
            gateXMLBuilder.'gate'(){
                'stage-done'(){
                    'gate-id'("DLT GATE")
                    'stage-id'("ingate")
                    'truck-visit'('gos-tv-key':tvGosKey)
                }
            }
            String xmlTv = gateXMLWriter.toString()
            println(xmlTv)

            def gateResponse = """
<?xml version="1.0" encoding="UTF-8"?>
<argo:gate-response xmlns:argo="http://www.navis.com/argo" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.navis.com/argo GateWebserviceResponse.xsd">
  <stage-done-response action="TROUBLE_LANE">
    <truck-visit tv-key="3220568" gos-tv-key="20100901001" is-internal="false" next-stage-id="ingate" status="TROUBLE" action="TROUBLE_LANE" gate-id="DLT GATE" entered="2017-03-23T10:09:55">
      <truck id="A3PU28" license-nbr="A3PU28" />
      <chassis-profile id="40" />
    </truck-visit>
    <truck-transactions>
      <truck-transaction tran-key="3220591" tran-nbr="25199" tv-key="3220568" tran-type="PUM" category="STRGE" freight-kind="MTY" next-stage-id="ingate" gate-id="DLT GATE" appointment-nbr="1841" order-nbr="40RH(2)" status="TROUBLE" action="TROUBLE_LANE" is-hazard="false">
        <container eqid="CBHU2823289" type="4530" is-sealed="false" gross-weight="3800.0" line-id="COS" carrier-visit-id="GEN_TRUCK" departure-order-nbr="40RH(2)" has-documents="false" is-placarded="false">
          <routing pod="CNZOS" pod2="CNZOS" />
        </container>
        <eq-order order-nbr="40RH(2)" line-id="COS" freight-kind="MTY">
          <eq-order-items>
            <eq-order-item eq-length="40.0" eq-length-name="40'" eq-iso-group="RE" eq-iso-group-name="Refrigerated container" eq-height="9.501312335958005" eq-height-name="9'6&quot;" />
          </eq-order-items>
        </eq-order>
        <documents>
          <document doc-key="3220599" type="TROUBLE">
            <content><![CDATA[<argo:Document xmlns:argo="http://www.navis.com/argo">
              <argo:docDescription>
                <docName>TROUBLE</docName>
                <docType>2454857</docType>
              </argo:docDescription>
              <argo:docBody>
                <argo:truckVisit>
                  <tvdtlsLicNbr>A3PU28</tvdtlsLicNbr>
                  <tvdtlsTruckId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsDriverCardId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsDriverName xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsDriverLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsBatNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsTvKey>3220568</tvdtlsTvKey>
                  <tvdtlsGosTvKey>20100901001</tvdtlsGosTvKey>
                  <tvdtlsTrkStartTime>2017-03-23 10:09:55</tvdtlsTrkStartTime>
                  <tvdtlsFlexString01 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsFlexString02 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tvdtlsFlexString03 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <argo:tvdtlsRequiredStages>
                    <stageId>ingate</stageId>
                    <stageOrder>1</stageOrder>
                    <stageDescription>ingate</stageDescription>
                  </argo:tvdtlsRequiredStages>
                  <tvdtlsAppointmentNbr>null</tvdtlsAppointmentNbr>
                  <argo:tvdtlsAnnouncements/>
                </argo:truckVisit>
                <argo:trkTransaction>
                  <argo:tranCtrOperator>
                    <bizuId>COS</bizuId>
                  </argo:tranCtrOperator>
                  <argo:tranEqo>
                    <eqboNbr>40RH(2)</eqboNbr>
                    <eqoLineId>COS</eqoLineId>
                    <eqoLineName>中国远洋</eqoLineName>
                  </argo:tranEqo>
                  <argo:tranEqoItem>
                    <eqoiEqHeight>NOM96</eqoiEqHeight>
                    <eqoiEqSize>NOM40</eqoiEqSize>
                    <eqoiEqIsoGroup>RE</eqoiEqIsoGroup>
                    <eqoiCscExpiration xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <eqoiSerialRanges xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <eqoiRemarks xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  </argo:tranEqoItem>
                  <argo:tranCarrierVisit>
                    <cvId>GEN_TRUCK</cvId>
                  </argo:tranCarrierVisit>
                  <argo:tranDischargePoint1>
                    <pointId>CNZOS</pointId>
                  </argo:tranDischargePoint1>
                  <argo:tranDischargePoint2>
                    <pointId>CNZOS</pointId>
                  </argo:tranDischargePoint2>
                  <argo:tranFacility>
                    <fcyId>DLT</fcyId>
                  </argo:tranFacility>
                  <argo:tranComplex>
                    <cpxId>ZST</cpxId>
                  </argo:tranComplex>
                  <argo:tranOperator>
                    <oprId>ZSCT</oprId>
                  </argo:tranOperator>
                  <tranNbr>25199</tranNbr>
                  <tranExchangeAreaId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranSubType>DM</tranSubType>
                  <tranTruckerFriendlySubType>PUM</tranTruckerFriendlySubType>
                  <tranStatus>TROUBLE</tranStatus>
                  <tranCreated>2017-3-23 上午10:13</tranCreated>
                  <tranCreator>admin</tranCreator>
                  <tranStageId>ingate</tranStageId>
                  <tranNextStageId>ingate</tranNextStageId>
                  <tranTrouble xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTroubleStatus xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranHadTrouble>true</tranHadTrouble>
                  <tranCancelCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranNotes xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTerminalId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranExportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranImportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranAppointmentNbr>1841</tranAppointmentNbr>
                  <tranPinNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTrkcId>FYJY</tranTrkcId>
                  <tranCtrTruckPosition xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrDoorDirection xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrFreightKind>MTY</tranCtrFreightKind>
                  <tranCtrTypeId>4530</tranCtrTypeId>
                  <tranCtrIsDamaged>false</tranCtrIsDamaged>
                  <tranCtrGrossWeight>3800.0</tranCtrGrossWeight>
                  <tranCtrNbrAssigned>CBHU2823289</tranCtrNbrAssigned>
                  <tranCtrTicketPosId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCtrAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsIsOwners>false</tranChsIsOwners>
                  <tranChsTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsIsDamaged>false</tranChsIsDamaged>
                  <tranChsLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsNbrAssigned xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranChsAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranTradeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranOrigin xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranDestination xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranLineId>COS</tranLineId>
                  <tranShipper xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranConsignee xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranScAgent xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranEqoEqIsoGroup>RE</tranEqoEqIsoGroup>
                  <tranEqoEqLength>NOM40</tranEqoEqLength>
                  <tranEqoEqHeight>NOM96</tranEqoEqHeight>
                  <tranEqoNbr>40RH(2)</tranEqoNbr>
                  <tranMilitaryVNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranMilitaryTcn xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShandId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShandId2 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranShandId3 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranGroupId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranIdoId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCommodityCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranCommodityDescription xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranVentRequired>null</tranVentRequired>
                  <tranVentSetting>null</tranVentSetting>
                  <tranIsHazard>false</tranIsHazard>
                  <tranIsPlacarded>false</tranIsPlacarded>
                  <tranIsPlacardedOk>false</tranIsPlacardedOk>
                  <tranIsOog>false</tranIsOog>
                  <tranCtrIsSealed>false</tranCtrIsSealed>
                  <tranIsXrayRequired>false</tranIsXrayRequired>
                  <tranCscDate xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                  <tranUnitCategory>UnitCategoryEnum[STRGE]</tranUnitCategory>
                  <argo:tranRequiredStages>
                    <stageId>ingate</stageId>
                    <stageSequence>1</stageSequence>
                    <stageDescription>ingate</stageDescription>
                  </argo:tranRequiredStages>
                  <tranStartTime>2017-03-23 10:13:39</tranStartTime>
                  <tranAppointmentState>CREATED</tranAppointmentState>
                </argo:trkTransaction>
              </argo:docBody>
              <argo:Messages locale="en">
                <argo:Message>
                  <errKey>XPS_TPC_ERROR</errKey>
                  <message>Internal error connecting to XPS</message>
                  <severity>SEVERE</severity>
                </argo:Message>
                <argo:Message>
                  <errKey>gate.matching_ctr_not_found</errKey>
                  <message>No suitable equipment found.</message>
                  <severity>SEVERE</severity>
                </argo:Message>
              </argo:Messages>
              </argo:Document>]]></content>
            </document>
          </documents>
          <messages locale="en">
            <message message-id="gate.matching_ctr_not_found" message-text="No suitable equipment found." message-severity="SEVERE" />
            <message message-id="XPS_TPC_ERROR" message-text="Internal error connecting to XPS" message-severity="SEVERE" />
          </messages>
        </truck-transaction>
        <truck-transaction tran-key="3220620" tran-nbr="25200" tv-key="3220568" tran-type="DOE" category="EXPRT" next-stage-id="ingate" gate-id="DLT GATE" status="TROUBLE" action="TROUBLE_LANE" is-hazard="false">
          <container eqid="TRHU2045071" is-sealed="false" carrier-visit-id="JL1621" has-documents="false" is-placarded="false">
            <routing />
          </container>
          <documents>
            <document doc-key="3220622" type="TROUBLE">
              <content><![CDATA[<argo:Document xmlns:argo="http://www.navis.com/argo">
                <argo:docDescription>
                  <docName>TROUBLE</docName>
                  <docType>2454857</docType>
                </argo:docDescription>
                <argo:docBody>
                  <argo:truckVisit>
                    <tvdtlsLicNbr>A3PU28</tvdtlsLicNbr>
                    <tvdtlsTruckId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tvdtlsDriverCardId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tvdtlsDriverName xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tvdtlsDriverLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tvdtlsBatNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tvdtlsTvKey>3220568</tvdtlsTvKey>
                    <tvdtlsGosTvKey>20100901001</tvdtlsGosTvKey>
                    <tvdtlsTrkStartTime>2017-03-23 10:09:55</tvdtlsTrkStartTime>
                    <tvdtlsFlexString01 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tvdtlsFlexString02 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tvdtlsFlexString03 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <argo:tvdtlsRequiredStages>
                      <stageId>ingate</stageId>
                      <stageOrder>1</stageOrder>
                      <stageDescription>ingate</stageDescription>
                    </argo:tvdtlsRequiredStages>
                    <tvdtlsAppointmentNbr>null</tvdtlsAppointmentNbr>
                    <argo:tvdtlsAnnouncements/>
                  </argo:truckVisit>
                  <argo:trkTransaction>
                    <argo:tranCtrOperator/>
                    <argo:tranCarrierVisit>
                      <cvId>JL1621</cvId>
                      <cvCvdCarrierVehicleName>JOSCO LILY</cvCvdCarrierVehicleName>
                      <cvCvdCarrierIbVygNbr>1620W</cvCvdCarrierIbVygNbr>
                      <cvCvdCarrierObVygNbr>1621E</cvCvdCarrierObVygNbr>
                      <cvATA>2016-8-13 上午8:00</cvATA>
                      <cvCvdETA>2016-8-12 上午8:00</cvCvdETA>
                      <cvCvdETD>2016-8-12 下午11:00</cvCvdETD>
                    </argo:tranCarrierVisit>
                    <argo:tranFacility>
                      <fcyId>DLT</fcyId>
                    </argo:tranFacility>
                    <argo:tranComplex>
                      <cpxId>ZST</cpxId>
                    </argo:tranComplex>
                    <argo:tranOperator>
                      <oprId>ZSCT</oprId>
                    </argo:tranOperator>
                    <tranNbr>25200</tranNbr>
                    <tranExchangeAreaId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranSubType>RE</tranSubType>
                    <tranTruckerFriendlySubType>DOE</tranTruckerFriendlySubType>
                    <tranStatus>TROUBLE</tranStatus>
                    <tranCreated>2017-3-23 上午10:32</tranCreated>
                    <tranCreator>admin</tranCreator>
                    <tranStageId>ingate</tranStageId>
                    <tranNextStageId>ingate</tranNextStageId>
                    <tranTrouble xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranTroubleStatus xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranHadTrouble>true</tranHadTrouble>
                    <tranCancelCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranNotes xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranTerminalId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranExportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranImportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranAppointmentNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranPinNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranTrkcId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCtrTruckPosition xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCtrDoorDirection xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCtrNbr>TRHU2045071</tranCtrNbr>
                    <tranCtrOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCtrTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCtrIsDamaged>false</tranCtrIsDamaged>
                    <tranCtrNbrAssigned xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCtrTicketPosId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCtrAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCtrAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCtrAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranChsNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranChsOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranChsIsOwners>false</tranChsIsOwners>
                    <tranChsTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranChsIsDamaged>false</tranChsIsDamaged>
                    <tranChsLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranChsNbrAssigned xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranChsAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranChsAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranChsAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranTradeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranOrigin xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranDestination xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranLineId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranShipper xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranConsignee xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranScAgent xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranEqoEqIsoGroup>GP</tranEqoEqIsoGroup>
                    <tranEqoEqLength>NOM20</tranEqoEqLength>
                    <tranEqoEqHeight>NOM86</tranEqoEqHeight>
                    <tranEqoNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranMilitaryVNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranMilitaryTcn xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranShandId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranShandId2 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranShandId3 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranGroupId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranIdoId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCommodityCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranCommodityDescription xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranVentRequired>null</tranVentRequired>
                    <tranVentSetting>null</tranVentSetting>
                    <tranIsHazard>false</tranIsHazard>
                    <tranIsPlacarded>false</tranIsPlacarded>
                    <tranIsPlacardedOk>false</tranIsPlacardedOk>
                    <tranIsOog>false</tranIsOog>
                    <tranCtrIsSealed>false</tranCtrIsSealed>
                    <tranIsXrayRequired>false</tranIsXrayRequired>
                    <tranCscDate xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <tranUnitCategory>UnitCategoryEnum[EXPRT]</tranUnitCategory>
                    <argo:tranRequiredStages>
                      <stageId>ingate</stageId>
                      <stageSequence>1</stageSequence>
                      <stageDescription>ingate</stageDescription>
                    </argo:tranRequiredStages>
                    <tranStartTime>2017-03-23 10:32:43</tranStartTime>
                  </argo:trkTransaction>
                </argo:docBody>
                <argo:Messages locale="en">
                  <argo:Message>
                    <errKey>XPS_TPC_ERROR_POSITION</errKey>
                    <message>Requesting position for TRHU2045071 - NO RESPONSE</message>
                    <severity>SEVERE</severity>
                  </argo:Message>
                </argo:Messages>
                </argo:Document>]]></content>
              </document>
            </documents>
            <messages locale="en">
              <message message-id="XPS_TPC_ERROR_POSITION" message-text="Requesting position for TRHU2045071 - NO RESPONSE" message-severity="SEVERE" />
            </messages>
          </truck-transaction>
          <truck-transaction tran-key="3220667" tran-nbr="25201" tv-key="3220568" tran-type="DOM" category="STRGE" next-stage-id="ingate" gate-id="DLT GATE" status="TROUBLE" action="TROUBLE_LANE" is-hazard="false">
            <container eqid="AABB2233445" is-sealed="false" carrier-visit-id="GEN_CARRIER" has-documents="false" is-placarded="false">
              <routing />
            </container>
            <documents>
              <document doc-key="3220669" type="TROUBLE">
                <content><![CDATA[<argo:Document xmlns:argo="http://www.navis.com/argo">
                  <argo:docDescription>
                    <docName>TROUBLE</docName>
                    <docType>2454857</docType>
                  </argo:docDescription>
                  <argo:docBody>
                    <argo:truckVisit>
                      <tvdtlsLicNbr>A3PU28</tvdtlsLicNbr>
                      <tvdtlsTruckId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tvdtlsDriverCardId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tvdtlsDriverName xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tvdtlsDriverLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tvdtlsBatNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tvdtlsTvKey>3220568</tvdtlsTvKey>
                      <tvdtlsGosTvKey>20100901001</tvdtlsGosTvKey>
                      <tvdtlsTrkStartTime>2017-03-23 10:09:55</tvdtlsTrkStartTime>
                      <tvdtlsFlexString01 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tvdtlsFlexString02 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tvdtlsFlexString03 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <argo:tvdtlsRequiredStages>
                        <stageId>ingate</stageId>
                        <stageOrder>1</stageOrder>
                        <stageDescription>ingate</stageDescription>
                      </argo:tvdtlsRequiredStages>
                      <tvdtlsAppointmentNbr>null</tvdtlsAppointmentNbr>
                      <argo:tvdtlsAnnouncements/>
                    </argo:truckVisit>
                    <argo:trkTransaction>
                      <argo:tranCtrOperator/>
                      <argo:tranCarrierVisit>
                        <cvId>GEN_CARRIER</cvId>
                      </argo:tranCarrierVisit>
                      <argo:tranFacility>
                        <fcyId>DLT</fcyId>
                      </argo:tranFacility>
                      <argo:tranComplex>
                        <cpxId>ZST</cpxId>
                      </argo:tranComplex>
                      <argo:tranOperator>
                        <oprId>ZSCT</oprId>
                      </argo:tranOperator>
                      <tranNbr>25201</tranNbr>
                      <tranExchangeAreaId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranSubType>RM</tranSubType>
                      <tranTruckerFriendlySubType>DOM</tranTruckerFriendlySubType>
                      <tranStatus>TROUBLE</tranStatus>
                      <tranCreated>2017-3-23 下午12:04</tranCreated>
                      <tranCreator>admin</tranCreator>
                      <tranStageId>ingate</tranStageId>
                      <tranNextStageId>ingate</tranNextStageId>
                      <tranTrouble xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranTroubleStatus xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranHadTrouble>true</tranHadTrouble>
                      <tranCancelCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranNotes xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranTerminalId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranExportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranImportReleaseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranAppointmentNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranPinNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranTrkcId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCtrTruckPosition xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCtrDoorDirection xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCtrNbr>AABB2233445</tranCtrNbr>
                      <tranCtrOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCtrTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCtrIsDamaged>false</tranCtrIsDamaged>
                      <tranCtrNbrAssigned xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCtrTicketPosId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCtrAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCtrAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCtrAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranChsNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranChsOwnerId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranChsIsOwners>false</tranChsIsOwners>
                      <tranChsTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranChsIsDamaged>false</tranChsIsDamaged>
                      <tranChsLicenseNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranChsNbrAssigned xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranChsAccNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranChsAccTypeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranChsAccFuelLevel xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranTradeId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranOrigin xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranDestination xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranLineId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranShipper xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranConsignee xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranScAgent xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranEqoEqIsoGroup>GP</tranEqoEqIsoGroup>
                      <tranEqoEqLength>NOM20</tranEqoEqLength>
                      <tranEqoEqHeight>NOM86</tranEqoEqHeight>
                      <tranEqoNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranMilitaryVNbr xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranMilitaryTcn xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranShandId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranShandId2 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranShandId3 xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranGroupId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranIdoId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCommodityCode xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranCommodityDescription xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranVentRequired>null</tranVentRequired>
                      <tranVentSetting>null</tranVentSetting>
                      <tranIsHazard>false</tranIsHazard>
                      <tranIsPlacarded>false</tranIsPlacarded>
                      <tranIsPlacardedOk>false</tranIsPlacardedOk>
                      <tranIsOog>false</tranIsOog>
                      <tranCtrIsSealed>false</tranCtrIsSealed>
                      <tranIsXrayRequired>false</tranIsXrayRequired>
                      <tranCscDate xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                      <tranUnitCategory>UnitCategoryEnum[STRGE]</tranUnitCategory>
                      <argo:tranRequiredStages>
                        <stageId>ingate</stageId>
                        <stageSequence>1</stageSequence>
                        <stageDescription>ingate</stageDescription>
                      </argo:tranRequiredStages>
                      <tranStartTime>2017-03-23 12:04:27</tranStartTime>
                    </argo:trkTransaction>
                  </argo:docBody>
                  <argo:Messages locale="en">
                    <argo:Message>
                      <errKey>XPS_TPC_ERROR_POSITION</errKey>
                      <message>Requesting position for AABB2233445 - NO RESPONSE</message>
                      <severity>SEVERE</severity>
                    </argo:Message>
                  </argo:Messages>
                  </argo:Document>]]></content>
                </document>
              </documents>
              <messages locale="en">
                <message message-id="XPS_TPC_ERROR_POSITION" message-text="Requesting position for AABB2233445 - NO RESPONSE" message-severity="SEVERE" />
              </messages>
            </truck-transaction>
          </truck-transactions>
        </stage-done-response>
        <notify-wait />
      </argo:gate-response>

            """.trim()

            def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))
            String gateStatus = root.'stage-done-response'[0].'truck-visit'[0]["@status"]
            def gateMessage1 = root.'stage-done-response'[0].'truck-transactions'.'truck-transaction'.'messages'.'message'["@message-text"]


            println(gateStatus)
            println(gateMessage1)

        }


//
	}


    static synchronized getGosTvKey(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS")
        return (simpleDateFormat.format(new Date()))
    }


}
