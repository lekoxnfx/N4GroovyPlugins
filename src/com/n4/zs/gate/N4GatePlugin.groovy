package com.n4.zs.gate

import com.navis.argo.ContextHelper
import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.atoms.UnitCategoryEnum
import com.navis.argo.webservice.types.v1_0.*
import com.navis.framework.business.Roastery
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.framework.util.unit.LengthUnit
import com.navis.inventory.InventoryEntity
import com.navis.inventory.InventoryField
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.atoms.UnitVisitStateEnum
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.orders.business.eqorders.EquipmentOrderItem
import com.navis.road.business.appointment.model.GateAppointment
import com.navis.road.business.atoms.AppointmentStateEnum
import com.navis.road.business.atoms.TranSubTypeEnum
import com.navis.road.business.atoms.TruckStatusEnum
import com.navis.road.business.atoms.TruckerFriendlyTranSubTypeEnum
import com.navis.road.business.model.RoadSequenceProvider
import com.navis.road.business.model.Truck
import com.navis.road.business.model.TruckTransaction
import com.navis.road.business.model.TruckVisitDetails
import com.navis.www.services.argoservice.ArgoServiceLocator
import com.navis.www.services.argoservice.ArgoServicePort
import groovy.sql.Sql
import groovy.xml.MarkupBuilder

import javax.xml.rpc.Stub
import java.text.SimpleDateFormat

/**
 /*
 <groovy class-location="database" class-name="N4GatePlugin">
 <parameters>
 <parameter id="truck-lic" value="苏A3PU28"/>
 <parameter id="truck-scale-weight" value="2800"/>
 <parameter id="stage-id" value="ingate"/>
 <parameter id="unit-ids" value="CNTR1111111,CNTR2222222"/>
 <parameter id="appt-nbrs" value="101,102"/>
 <parameter id="request-id" value=""/>
 </parameters>
 </groovy>
 */

/*
resultXML:
<create-truck-visit-response>
<request id="1234567" truck-lic="苏A3PU28"/>
<results result="true" />
<info-message message="">
<transactions>
	<transaction nbr="" type="" unit-id="" freight-kind="" type-iso="" line="" gross-weight="" planned-slot="">
</transactions>
</create-truck-visit-response>

Created by liuminhang on 16/2/19.

*/


class N4GatePlugin {

    //全局标志
    boolean isNext = true; //这个标志变为false,则表示流程结束,返回结果

    //返回结果
    boolean isAllowed = false  //表示禁行or放行
    String resHint = ""  //返回原因

    //传入参数
    String inTruckLicense;
    String inTruckScaleWeight;
    String inStageId;
    String inUnitIds;
    String inApptNbrs ;
    String inRequestId;

    //内用变量
    Truck truck;
    Sql sqlBS;

    TruckVisitDetails tvdtls;
    Long tvGkey;

    GroovyApi api = new GroovyApi()
    N4Operator n4Operator = new N4Operator()


    //入口
    public String execute(Map inParameters){

        api.log("收到闸口请求")
        //解析参数
        try {
            inTruckLicense = inParameters.get("truck-lic");
            inTruckScaleWeight = inParameters.get("truck-scale-weight")
            inStageId = inParameters.get("stage-id")
            inUnitIds = inParameters.get("unit-ids");
            inApptNbrs = inParameters.get("appt-nbrs");
            inRequestId = inParameters.get("request-id")
        } catch (Exception e) {
            api.log("解析参数出现异常:" + e.toString())
            isNext = false;
            isAllowed = false
            resHint = "解析参数失败,请联系技术人员"
        }
        //解析结束,没问题继续
        api.log("解析参数结束,参数:" + inTruckLicense + "," + inTruckScaleWeight + ","
                + inStageId + "," + inUnitIds + "," + inApptNbrs + "," + inRequestId)
        if (isNext) {
            //获取车号,并获得对应Truck对象
            api.log("获取车号")
            if(inTruckLicense==null||inTruckLicense==""){
                isNext = false;
                isAllowed = false;
                resHint="车号为空！";
                api.log(resHint)
            }
            else{
                try{
                    String truckLicState = inTruckLicense[0]
                    String truckLicNoState = inTruckLicense[1..-1]
                    api.log("车号："+truckLicNoState+"   省份："+truckLicState)
                    truck = Truck.findTruckByLicNbr(truckLicNoState)
                    if(truck!=null&&truck.truckLicenseState.equals(truckLicState)){
                        //获取到卡车对象
                        api.log("获取卡车对象,Gkey:" + truck.getPrimaryKey())
                    }
                    else{
                        isNext = false;
                        isAllowed = false;
                        resHint = "车号未登记！"
                        api.log(resHint)

                    }
                }catch(Exception e){
                    isNext = false;
                    isAllowed =false;
                    resHint = "车号解析错误！"
                    api.log(resHint)
                    api.log(e.toString())

                }
            }

        }
        //获取卡车状态
        if(isNext){
            if(truck.isTruckBanned()){
                resHint = "卡车被禁止！"
                isAllowed = false
                isNext = false
                api.log(resHint)
            }
            else{
                if(truck.getTruckTrkCo()==null){
                    resHint = "卡车公司被禁止！"
                    isAllowed = false
                    isNext = false
                    api.log(resHint)
                }
                else if(truck.getTruckTrkCo().getTrkcStatus().equals(TruckStatusEnum.BANNED)){
                    resHint = "卡车公司被禁止！"
                    isAllowed = false
                    isNext = false
                    api.log(resHint)
                }
            }
        }




        //获取车辆信息完毕,继续继续,开始判断车辆类型
        String truckType = "";
        if (isNext){
            api.log("获取卡车类型, id " + truck.getTruckId())
            try{
                truckType = truck.getCustomFlexFields().get("truckCustomDFF_TruckType")
            }catch(Exception e){//获取卡车类型失败
                api.log("获取车辆类型出现异常:"+e.toString())
                truckType = ""
            }
            if(truckType==null||truckType==""){
                try{
                    api.log("获取卡车公司类型")
                    truckType = truck.getTruckTrkCo().getCustomFlexFields().get("bzuCustomDFF_BizType")
                }catch(Exception e2){
                    api.log("获取卡车公司类型出现异常:" + e2.toString())
                    truckType = ""
                }
            }
            api.log("卡车类型：" + truckType)
        }
        //获取车辆类型成功,开始分类型处理
        if(truckType==null||truckType==""){
            resHint = "获取卡车、卡车公司类型出现异常,请确保录入卡车或卡车公司类型"
            api.log("行政车辆,直接放行")
            isAllowed = false
        }
        else if(truckType.equals("行政车辆")||truckType.equals("行政公司")){
            //行政车辆,直接放行
            api.log("行政车辆,直接放行")
            isAllowed = true
            resHint = "行政车辆"

        }
        else if(truckType.equals("散货车辆")||truckType.equals("仅散货")){
            if(inStageId.equalsIgnoreCase("ingate")){//散货车辆进门
                api.log("散杂货卡车进门")

                //检查是否有活动记录
                List<TruckVisitDetails> truckVisitDetailsList = TruckVisitDetails.findTVActiveByTruckLicenseNbr(truck.getTruckLicenseNbr())
                if(truckVisitDetailsList!=null&&truckVisitDetailsList.size()>0){
                    resHint = "有活动的记录未处理"
                    isAllowed = false
                    api.log(resHint)
                }
                else {
                    boolean isBooked = false
                    long truckGkey = truck.getPrimaryKey()
                    String _sql = " select * from bookingdetails b, bookingtrucks t ,N4ZS_VIEW_TRK v " +
                            "where t.bookingnbr = b.bookingnbr and t.trucksgkey = v.truck_gkey and b.status = 'ACTIVE' and t.status = 'ACTIVE' and v.truck_status = 'OK' "+
                            "and v.truck_gkey = '${truckGkey}'" + "and b.startdate < sysdate and (b.enddate > sysdate or b.enddate is null) "
                    try{
                        String DB2 = "jdbc:oracle:thin:@" + "192.168.50.32" + ":" + "1521" + ":" + "n4"
                        String USER2 = "zsserver"
                        String PASSWORD2 = "zsserver"
                        String DRIVER2 = 'oracle.jdbc.driver.OracleDriver'
                        sqlBS = Sql.newInstance(DB2, USER2, PASSWORD2, DRIVER2)
                        sqlBS.eachRow(_sql){
                            isBooked = true
                        }
                        if(isBooked==false){
                            isAllowed = false
                            resHint = "该卡车未预约或已过时间"
                            api.log(resHint)
                        }
                        else{
                            isAllowed = true
                            resHint = "散货车进闸"
                            api.log(resHint)
                        }
                    }catch(Exception e){
                        e.printStackTrace()
                        isAllowed = false
                        api.log("查询散货预约数据异常:" + e.toString())
                        resHint = "查询散货预约数据异常,请联系技术人员"

                    }
                }


            }
            else if(inStageId.equalsIgnoreCase("outgate")){//出门
                api.log("散杂货卡车出门")
                List<TruckVisitDetails> truckVisitDetailsList = TruckVisitDetails.findTVActiveByTruckLicenseNbr(truck.getTruckLicenseNbr())
                if(truckVisitDetailsList!=null&&truckVisitDetailsList.size()==1){
                    //生成XML,提交N4闸口
                    api.log("生成出闸xml")
                    def stringWriter = new StringWriter()
                    def markupBuilder = new MarkupBuilder(stringWriter)
                    markupBuilder.'gate'() {
                        'process-truck'('scan-status':0) {
                            'gate-id'("DLT GC GATE")
                            'stage-id'("outgate")
                            'truck'('license-nbr':truck.getTruckLicenseNbr())
                        }
                    }
                    String bbkTrkOutXml = stringWriter.toString()
                    api.log("生成xml:\n" + bbkTrkOutXml)
                    //发送给N4,获取结果

                    try {
                        n4Operator.sendRequestWithXml(bbkTrkOutXml)
                        api.log("返回:\n" + n4Operator.STATUS + "\n" + n4Operator.PAYLOAD + "\n" + n4Operator.RESULTS )
                        //解析结果
                        //XXXX
                        if(n4Operator.STATUS.equals(N4Operator.ERRORS)){
                            //
                            isAllowed = false
                            resHint = "向N4发送信息异常,请手动处理" + n4Operator.MESSAGE_COLLECTOR==null?"":n4Operator.MESSAGE_COLLECTOR.getMessages().collect {return it.getMessage()}
                            api.log(resHint)

                        }else{
                            String gateResponse = n4Operator.RESULTS[0]
                            //解析N4返回的XML
                            def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))
                            String gateStatus = root.'process-truck-response'[0].'truck-visit'[0]["@status"]
                            if(gateStatus.equals("TROUBLE")){
                                def gateMessage2 = root.'process-truck-response'[0].'truck-transactions'.'truck-transaction'.'messages'.'message'["@message-text"]
                                def gateMessage1 = root.'process-truck-response'[0].'truck-visit'.'messages'.'message'["@message-text"]
                                isAllowed = false
                                resHint = gateMessage1 + gateMessage2
                                api.log(resHint)
                            }
                            else if (gateStatus.equals("COMPLETE")){
                                isAllowed = true
                                resHint = "散货车出闸"
                                api.log(resHint)
                            }else {
                                isAllowed = false
                                resHint = "车辆访问状态异常,请手动处理[" + gateStatus +"]"
                                api.log(resHint)
                            }

                        }


                    } catch (Exception e1) {
                        isAllowed = false
                        resHint = "向N4请求异常,请手动处理" + e1.toString()
                        api.log(resHint)
                        api.log(e1.toString())
                    }


                }
                else {
                    isAllowed = false
                    resHint = "没有找到或有多条进场记录，请手动处理"
                    api.log(resHint)
                }

            }else {
                isAllowed = false
                resHint = "不明的stage:" + inStageId
                api.log(resHint)
            }

        }
        else if(truckType.equals("集装箱车辆")||truckType.equals("仅集装箱")){
            if(inStageId.equalsIgnoreCase("ingate")) {//进门
                api.log("集装箱进门")
                //检查是否有活动记录
                List<TruckVisitDetails> truckVisitDetailsList = TruckVisitDetails.findTVActiveByTruckLicenseNbr(truck.getTruckLicenseNbr())
                if(truckVisitDetailsList!=null&&truckVisitDetailsList.size()>1){
                    resHint = "有活动的记录未处理"
                    isAllowed = false
                    api.log(resHint)
                }
                else if(truckVisitDetailsList.size()==1
                        &&!truckVisitDetailsList[0].hasTrouble()){
                    resHint = "有活动的记录未处理"
                    isAllowed = false
                    api.log(resHint)
                }
                else {
                    String[] appts,cntrs
                    List<GateAppointment> apptList = new ArrayList<GateAppointment>();
                    List<Unit> cntrList = new ArrayList<Unit>();
                    if((inApptNbrs==null||inApptNbrs=="")&&(inUnitIds==null||inUnitIds=="")){
                        resHint = "没有预约号和箱号。"
                        api.log(resHint)
                        isAllowed = false;
                        isNext = false;
                    }
                    if(isNext){
                        if(inApptNbrs!=null&&inApptNbrs!=""){
                            try{
                                //解析预约号
                                String[] apptStrs = inApptNbrs.contains(",")?inApptNbrs.split(",",-1):[inApptNbrs]
                                /*
                                修改于2017年03月13日
                                */
                                List<String> apptStrList = new ArrayList<>()
                                apptStrs.each {apptStr->
                                    if (apptStr.endsWith("A")){
                                        apptStrList.add(apptStr[0..-2])
                                    }
                                    else if(apptStr.endsWith("B")){
                                        apptStrList.add(apptStr[0..-2])
                                        apptStrList.add(apptStr[0..-2])
                                    }
                                    else {
                                        //兼容旧版预约号
                                        apptStrList.add(apptStr[0..-2])
                                    }
                                }
                                appts = apptStrList.toArray()

                            }catch(Exception e){
                                resHint = "预约号解析出错。"
                                api.log(resHint)
                                isAllowed = false;
                                isNext = false;
                            }
                            appts.each{appt->
                                if(appt == null || appt == ""){
                                    resHint = "预约解析出错,存在空值"
                                    api.log(resHint)
                                    isAllowed = false;
                                    isNext = false;
                                }
                            }
                        }
                        if(inUnitIds!=null&&inUnitIds!=""){
                            try{
                                //解析箱号
                                cntrs = this.inUnitIds.contains(",")?inUnitIds.split(",",-1):[inUnitIds]
                            }catch(Exception e){
                                resHint = "箱号解析出错。"
                                api.log(resHint)
                                isAllowed = false;
                                isNext = false;
                            }
                            //判断是否有空值
                            cntrs.each{cntr->
                                if(cntr == null || cntr == ""){
                                    resHint = "箱号解析出错,存在空值"
                                    api.log(resHint)
                                    isAllowed = false;
                                    isNext = false;
                                }
                            }
                        }
                    }
                    //预约号箱号均成功解析
                    if(isNext){

                        try{
                            //检测预约号是否存在
                            appts.each {apptNbr->
                                if(isNext){
                                    api.log("处理预约号：" + apptNbr)
                                    GateAppointment appt;
                                    try{
                                        long apptLong = apptNbr.toLong()
                                        appt = GateAppointment.findGateAppointment(apptLong)

                                        if(appt==null||!appt.getApptState().equals(AppointmentStateEnum.CREATED)){
                                            resHint = "预约号不存在或已过期:"+apptNbr
                                            api.log(resHint)
                                            isAllowed = false;
                                            isNext = false;
                                        }
                                        else {
                                            apptList.add(appt)
                                        }
                                    }catch(Exception ee) {
                                        resHint = "预约号不存在:" + apptNbr
                                        api.log(resHint)
                                        isAllowed = false;
                                        isNext = false;
                                    }
                                }

                            }
                            //检测箱号是否存在
                            if(isNext){
                                cntrs.each { cntrId->
                                    if(isNext){
                                        api.log("处理箱号：" + cntrId)
                                        //检查是否预录入
                                        boolean isInbound=false;
                                        DomainQuery dqUnit = QueryUtils.createDomainQuery(InventoryEntity.UNIT);
                                        dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_ID,cntrId))
                                        dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_VISIT_STATE,UnitVisitStateEnum.ACTIVE))
                                        dqUnit.addDqPredicate(PredicateFactory.in(InventoryField.UNIT_CATEGORY,[UnitCategoryEnum.EXPORT,UnitCategoryEnum.STORAGE]))
                                        def res = Roastery.getHibernateApi().findEntitiesByDomainQuery(dqUnit);
                                        res.each {
                                            Unit u = it
                                            if(u.getUnitActiveUfvNowActive().getUfvTransitState().equals(UfvTransitStateEnum.S20_INBOUND)){
                                                isInbound = true;
                                                cntrList.add(u);
                                            }
                                        }

                                        if (!isInbound){
                                            resHint = "箱号未预录:" + cntrId
                                            api.log(resHint)
                                            isAllowed = false;
                                            isNext = false;
                                        }
                                    }
                                }
                            }

                            //预约号/箱号都没有问题
                            if(isNext){
                                String tvGosKey = getGosTvKey();
                                //创建truckVisit
                                if(isNext){
                                    def gateXMLWriter = new StringWriter()
                                    def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)
                                    api.log("创建TruckVisit")
                                    /*
                                    <gate>
                                        <create-truck-visit>
                                            <gate-id>DLT GATE</gate-id>
                                            <stage-id>ingate</stage-id>
                                            <truck license-nbr="A3PU28"/>
                                            <truck-visit gos-tv-key="20100901001"/>
                                        </create-truck-visit>
                                    </gate>
                                     */
                                    gateXMLBuilder.'gate'(){
                                        'create-truck-visit'(){
                                            'gate-id'("DLT GATE")
                                            'stage-id'("ingate")
                                            'truck'('license-nbr':this.truck.getTruckLicenseNbr())
                                            'truck-visit'(
                                                    'gos-tv-key':tvGosKey,
                                                    'bat-nbr':this.truck.getTruckBatNbr()
                                            )
                                        }
                                    }
                                    String xmlTv = gateXMLWriter.toString()
                                    api.log("xml:\n" + xmlTv)
                                    try {
                                        n4Operator.sendRequestWithXml(xmlTv)
                                        api.log("返回:\n" + n4Operator.STATUS + "\n" + n4Operator.PAYLOAD + "\n" + n4Operator.RESULTS )
                                        //解析结果
                                        //XXXX
                                        if(n4Operator.STATUS.equals(N4Operator.ERRORS)){
                                            //
                                            isAllowed = false
                                            isNext = false
                                            resHint = "向N4发送信息异常,请手动处理" + n4Operator.MESSAGE_COLLECTOR==null?"":n4Operator.MESSAGE_COLLECTOR.getMessages().collect {return it.getMessage()}
                                            api.log(resHint)

                                        }else{
                                            String gateResponse = n4Operator.RESULTS[0]
                                            api.log(gateResponse)

                                            //解析N4返回的XML
                                            def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))
                                            String gateStatus = root.'create-truck-visit-response'[0].'truck-visit'[0]["@status"]
                                            if(gateStatus.equals("TROUBLE")){
                                                def gateMessage1 = root.'create-truck-visit-response'[0].'truck-visit'.'messages'.'message'["@message-text"]
                                                isAllowed = false
                                                isNext = false;
                                                resHint = gateMessage1
                                                api.log(resHint)
                                            }
                                            else if (gateStatus.equals("OK")){
                                                isAllowed = true
                                                resHint = "集装箱车进闸"
                                                root.'create-truck-visit-response'.'truck-visit'.each { tv ->
                                                    this.tvGkey = tv.'@tv-key'.toLong()
                                                }
                                                this.tvdtls = TruckVisitDetails.findTruckVisitByGkey(this.tvGkey)
                                                //获取返回
                                                api.log(resHint)
                                            }else {
                                                isAllowed = false
                                                isNext = false
                                                resHint = "创建车辆访问异常,请手动处理[" + gateStatus +"]"
                                                api.log(resHint)
                                            }

                                        }


                                    } catch (Exception e1) {
                                        isAllowed = false
                                        isNext = false
                                        resHint = "向N4请求异常,请联系技术人员" + e1.toString()
                                        api.log(resHint)
                                        api.log(e1.toString())
                                    }
                                }
                                //处理预约号
                                if(isNext){
                                    api.log("开始处理预约号：")
                                    /*
                                    有具体箱号的预约号
                                    <gate>
                                      <submit-transaction>
                                        <gate-id>DLT GATE</gate-id>
                                        <stage-id>ingate</stage-id>
                                        <truck-visit gos-tv-key="20100901001"/>
                                        <truck-transaction appointment-nbr="0001841"/>
                                      </submit-transaction>
                                    </gate>
                                    无具体箱号的预约号
                                    <gate>
                                      <submit-transaction>
                                        <gate-id>DLT GATE</gate-id>
                                        <stage-id>ingate</stage-id>
                                        <truck-visit gos-tv-key='20170327163359893' />
                                        <truck-transaction  order-nbr="ABCE" tran-type="PUM" >
                                                <eq-order order-nbr="ABCE" order-type="EDO" line-id="COS" freight-kind="MTY">
                                                  <eq-order-items>
                                                    <eq-order-item eq-length="40.0" eq-iso-group="RE" eq-height="9.501312335958005" type="4530"/>
                                                  </eq-order-items>
                                                </eq-order>
                                        </truck-transaction>
                                      </submit-transaction>
                                    </gate>
                                     */
                                    apptList.eachWithIndex {it,n->
                                        GateAppointment gateAppointment = it
                                        api.log("处理预约：" + gateAppointment.getApptNbr() + "，类型：" + gateAppointment.getGapptTranType().getName())
                                        String xmlTv
                                        //EDO 提空
                                        if(gateAppointment.getGapptTranType().equals(TruckerFriendlyTranSubTypeEnum.PUM)){
                                            EquipmentOrderItem equipmentOrderItem = EquipmentOrderItem.hydrate(gateAppointment.getGapptOrderItem().getPrimaryKey())
                                            def gateXMLWriter = new StringWriter()
                                            def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)
                                            gateXMLBuilder.'gate'(){
                                                'submit-transaction'(){
                                                    'gate-id'("DLT GATE")
                                                    'stage-id'("ingate")
                                                    'truck-visit'('gos-tv-key':tvGosKey)
                                                    'truck-transaction'(
                                                            'order-nbr':gateAppointment.getGapptOrder().getEqboNbr(),
                                                            'tran-type':"PUM"
                                                    ){
                                                        'eq-order'(
                                                                'order-nbr':gateAppointment.getGapptOrder().getEqboNbr(),
                                                                'order-type':'EDO',
                                                                'freight-kind':'MTY',
                                                                'line-id':gateAppointment.getGapptLineOperator().getBzuId()
                                                        ){
                                                            'eq-order-items'(){
                                                                'eq-order-item'(
                                                                        'eq-length':equipmentOrderItem.getEqoiEqSize().getValueInUnits(LengthUnit.FEET),
                                                                        'eq-iso-group':equipmentOrderItem.getEqoiEqIsoGroup().getName(),
                                                                        'eq-height':equipmentOrderItem.getEqoiEqHeight().getValueInUnits(LengthUnit.FEET),
                                                                        'type':equipmentOrderItem.getEqoiSampleEquipType().getEqtypId()
                                                                )
                                                            }
                                                        }

                                                    }

                                                }
                                            }

                                            xmlTv = gateXMLWriter.toString()
                                        }
                                        else {//指定箱号
                                            def gateXMLWriter = new StringWriter()
                                            def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)
                                            gateXMLBuilder.'gate'(){
                                                'submit-transaction'(){
                                                    'gate-id'("DLT GATE")
                                                    'stage-id'("ingate")
                                                    'truck-visit'('gos-tv-key':tvGosKey)
                                                    'truck-transaction'(
                                                            'appointment-nbr':gateAppointment.getApptNbr(),
                                                    )
                                                }
                                            }
                                            xmlTv = gateXMLWriter.toString()
                                        }
                                        api.log("xml:\n" + xmlTv)
                                        try {
                                            n4Operator.sendRequestWithXml(xmlTv)
                                            api.log("返回:\n" + n4Operator.STATUS + "\n" + n4Operator.PAYLOAD + "\n" + n4Operator.RESULTS )
                                            //解析结果
                                            //XXXX
                                            if(n4Operator.STATUS.equals(N4Operator.ERRORS)){
                                                //
                                                isAllowed = false
                                                isNext = false
                                                resHint = "向N4发送信息异常,请手动处理" + n4Operator.MESSAGE_COLLECTOR==null?"":n4Operator.MESSAGE_COLLECTOR.getMessages().collect {return it.getMessage()}
                                                api.log(resHint)

                                            }else{
                                                String gateResponse = n4Operator.RESULTS[0]
                                                api.log(gateResponse)

                                                //解析N4返回的XML
                                                def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))
                                                String gateStatus = root.'submit-transaction-response'[0].'truck-visit'[0]["@status"]
                                                if(gateStatus.equals("TROUBLE")){
                                                    def gateMessage1 = root.'submit-transaction-response'[0].'truck-transactions'.'truck-transaction'.'messages'.'message'["@message-text"]
                                                    isAllowed = false
                                                    isNext = false;
                                                    resHint = gateMessage1 + ("(预约" + gateAppointment.getApptNbr() + ")")
                                                    api.log(resHint)
                                                }
                                                else if (gateStatus.equals("OK")){
                                                    Thread.sleep(3000)

                                                }else {
                                                    isAllowed = false
                                                    isNext = false
                                                    resHint = "创建车辆事务异常,请手动处理[" + gateStatus +"]"
                                                    api.log(resHint)
                                                }
                                            }
                                        } catch (Exception e1) {
                                            isAllowed = false
                                            isNext = false
                                            resHint = "向N4请求异常,请联系技术人员" + e1.toString()
                                            api.log(resHint)
                                            api.log(e1.toString())
                                        }

                                    }

                                }
                                //处理箱号
                                if(isNext){
                                    api.log("开始处理箱号：")
                                    /*
                                    <gate>
                                      <submit-transaction>
                                        <gate-id>DLT GATE</gate-id>
                                        <stage-id>ingate</stage-id>
                                        <truck-visit gos-tv-key="20100901001"/>
                                        <truck-transaction tran-type="RE">
                                           <container eqid="TRHU2045071" />
                                        </truck-transaction>
                                      </submit-transaction>
                                    </gate>
                                     */
                                    cntrList.each {
                                        Unit unit = it
                                        api.log("处理箱号：" + unit.getUnitId())
                                        String tranType;
                                        switch (unit.getUnitCategory()){
                                            case UnitCategoryEnum.STORAGE:
                                                api.log("堆存箱，事务类型为RM")
                                                tranType = "RM"
                                                break;
                                            case UnitCategoryEnum.EXPORT:
                                                api.log("出口箱，事务类型为RE")
                                                tranType = "RE"
                                                break;
                                        }
                                        def gateXMLWriter = new StringWriter()
                                        def gateXMLBuilder = new MarkupBuilder(gateXMLWriter)
                                        gateXMLBuilder.'gate'(){
                                            'submit-transaction'(){
                                                'gate-id'("DLT GATE")
                                                'stage-id'("ingate")
                                                'truck-visit'('gos-tv-key':tvGosKey)
                                                'truck-transaction'('tran-type':tranType){
                                                    'container'('eqid':unit.getUnitId())
                                                }
                                            }
                                        }
                                        String xmlTv = gateXMLWriter.toString()
                                        api.log("xml:\n" + xmlTv)
                                        try {
                                            n4Operator.sendRequestWithXml(xmlTv)
                                            api.log("返回:\n" + n4Operator.STATUS + "\n" + n4Operator.PAYLOAD + "\n" + n4Operator.RESULTS )
                                            //解析结果
                                            //XXXX
                                            if(n4Operator.STATUS.equals(N4Operator.ERRORS)){
                                                //
                                                isAllowed = false
                                                isNext = false
                                                resHint = "向N4发送信息异常,请手动处理" + n4Operator.MESSAGE_COLLECTOR==null?"":n4Operator.MESSAGE_COLLECTOR.getMessages().collect {return it.getMessage()}
                                                api.log(resHint)

                                            }else{
                                                String gateResponse = n4Operator.RESULTS[0]
                                                api.log(gateResponse)

                                                //解析N4返回的XML
                                                def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))
                                                String gateStatus = root.'submit-transaction-response'[0].'truck-visit'[0]["@status"]
                                                if(gateStatus.equals("TROUBLE")){
                                                    def gateMessage1 = root.'submit-transaction-response'[0].'truck-transactions'.'truck-transaction'.'messages'.'message'["@message-text"]
                                                    isAllowed = false
                                                    isNext = false;
                                                    resHint = gateMessage1 + ("(箱号" + unit.getUnitId() + ")")
                                                    api.log(resHint)
                                                }
                                                else if (gateStatus.equals("OK")){

                                                }else {
                                                    isAllowed = false
                                                    isNext = false
                                                    resHint = "创建车辆事务异常,请手动处理[" + gateStatus +"]"
                                                    api.log(resHint)
                                                }
                                            }
                                        } catch (Exception e1) {
                                            isAllowed = false
                                            isNext = false
                                            resHint = "向N4请求异常,请联系技术人员" + e1.toString()
                                            api.log(resHint)
                                            api.log(e1.toString())
                                        }
                                    }
                                }

                                //推进stage
                                if (isNext){
                                    api.log("推进stage")
                                    /*
                                    <gate>
                                      <stage-done>
                                           <gate-id>DLT GATE</gate-id>
                                           <stage-id>ingate</stage-id>
                                           <truck-visit gos-tv-key="20100901001"/>
                                      </stage-done>
                                    </gate>
                                     */
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
                                    api.log("xml:\n" + xmlTv)
                                    try {
                                        n4Operator.sendRequestWithXml(xmlTv)
                                        api.log("返回:\n" + n4Operator.STATUS + "\n" + n4Operator.PAYLOAD + "\n" + n4Operator.RESULTS )
                                        //解析结果
                                        //XXXX
                                        if(n4Operator.STATUS.equals(N4Operator.ERRORS)){
                                            //
                                            isAllowed = false
                                            isNext = false
                                            resHint = "向N4发送信息异常,请手动处理" + n4Operator.MESSAGE_COLLECTOR==null?"":n4Operator.MESSAGE_COLLECTOR.getMessages().collect {return it.getMessage()}
                                            api.log(resHint)

                                        }else{
                                            String gateResponse = n4Operator.RESULTS[0]
                                            api.log(gateResponse)

                                            //解析N4返回的XML
                                            def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))
                                            String gateStatus = root.'stage-done-response'[0].'truck-visit'[0]["@status"]
                                            if(gateStatus.equals("TROUBLE")){
                                                def gateMessage1 = root.'stage-done-response'[0].'truck-transactions'.'truck-transaction'.'messages'.'message'["@message-text"]
                                                isAllowed = false
                                                isNext = false;
                                                resHint = gateMessage1
                                                api.log(resHint)
                                            }
                                            else if (gateStatus.equals("OK")){

                                            }else {
                                                isAllowed = false
                                                isNext = false
                                                resHint = "创建车辆事务异常,请手动处理[" + gateStatus +"]"
                                                api.log(resHint)
                                            }
                                        }
                                    } catch (Exception e1) {
                                        isAllowed = false
                                        isNext = false
                                        resHint = "向N4请求异常,请联系技术人员" + e1.toString()
                                        api.log(resHint)
                                        api.log(e1.toString())
                                    }
                                }
                            }


                        }
                        catch(Exception e){
                            resHint = "发生异常,请手动处理" + e.toString()
                            api.log(resHint)
                            isAllowed = false;
                            isNext = false;
                        }
                    }
                }



            }
            else if(inStageId.equalsIgnoreCase("outgate")){//出门
                api.log("集装箱卡车出门")
                List<TruckVisitDetails> truckVisitDetailsList = TruckVisitDetails.findTVActiveByTruckLicenseNbr(truck.getTruckLicenseNbr())
                if(truckVisitDetailsList!=null&&truckVisitDetailsList.size()==1){
                    api.log("生成出闸xml")
                    def stringWriter = new StringWriter()
                    def markupBuilder = new MarkupBuilder(stringWriter)
                    markupBuilder.'gate'() {
                        'process-truck'('scan-status':0) {
                            'gate-id'("DLT GATE")
                            'stage-id'("outgate")
                            'truck'('license-nbr':truck.getTruckLicenseNbr())
                        }
                    }
                    String outgateXml = stringWriter.toString()
                    api.log("xml:\n" + outgateXml)
                    try {
                        n4Operator.sendRequestWithXml(outgateXml)
                        api.log("返回:\n" + n4Operator.STATUS + "\n" + n4Operator.PAYLOAD + "\n" + n4Operator.RESULTS )
                        //解析结果
                        //XXXX
                        if(n4Operator.STATUS.equals(N4Operator.ERRORS)){
                            //
                            isAllowed = false
                            resHint = "向N4发送信息异常,请手动处理" + n4Operator.MESSAGE_COLLECTOR==null?"":n4Operator.MESSAGE_COLLECTOR.getMessages().collect {return it.getMessage()}
                            api.log(resHint)

                        }else{
                            String gateResponse = n4Operator.RESULTS[0]
                            //解析N4返回的XML
                            def root = new XmlParser().parse(new ByteArrayInputStream(gateResponse.getBytes("utf-8")))

                            String gateStatus = root.'process-truck-response'[0].'truck-visit'[0]["@status"]
                            if(gateStatus.equals("TROUBLE")){
                                def gateMessage2 = root.'process-truck-response'[0].'truck-transactions'.'truck-transaction'.'messages'.'message'["@message-text"]
                                def gateMessage1 = root.'process-truck-response'[0].'truck-visit'.'messages'.'message'["@message-text"]
                                isAllowed = false
                                resHint = gateMessage1 + gateMessage2
                                api.log(resHint)
                            }
                            else if (gateStatus.equals("COMPLETE")){
                                isAllowed = true
                                resHint = "集装箱车出闸"
                                api.log(resHint)
                            }else {
                                isAllowed = false
                                resHint = "车辆访问状态异常,请手动处理[" + gateStatus +"]"
                                api.log(resHint)
                            }

                        }


                    } catch (Exception e1) {
                        isAllowed = false
                        resHint = "向N4请求异常,请手动处理" + e1.toString()
                        api.log(resHint)
                        api.log(e1.toString())
                    }
                }
                else{
                    isAllowed = false
                    resHint = "没有找到进场记录或有多条进场记录，请人工处理"
                    api.log(resHint)
                }


            }
            else {
                isAllowed = false
                resHint = "不明的stage:" + inStageId
                api.log(resHint)
            }
        }


        //处理完毕,依据结果生成XML
        def resultXML = new StringWriter()
        def result = new MarkupBuilder(resultXML)

        result."ProcessTruckVisitResponse"{
            "request"("id":inRequestId,"truck-lic":inTruckLicense==null?"":inTruckLicense)
            "results"("result":isAllowed)
            "info-message"("message":resHint)
            if(isAllowed&&tvdtls!=null){
                "transactions"{
                    tvdtls.getActiveTransactions().each {
                        TruckTransaction tran = it
                        "transaction"()
                                {
                                    "parameter"("id":"nbr","value":tran.getTranNbr())
                                    "parameter"("id":"type","value":tran.getTranSubType().getName())
                                    "parameter"("id":"appt-nbr","value":tran.getTranAppointmentNbr()==null?"":tran.getTranAppointmentNbr())
                                    "parameter"("id":"unit-id","value":(tran.getTranSubType().equals(TranSubTypeEnum.DM))?"箱号待定":tran.getTranCtrNbr())
                                    "parameter"("id":"freight-kind","value":tran.getTranCtrFreightKind().getName())
                                    "parameter"("id":"type-iso","value":tran.getTranCtrTypeId())
                                    "parameter"("id":"gross-weight","value":tran.getTranCtrGrossWeight())
                                    "parameter"("id":"planned-position","value":tran.getTranCtrPosition().getPosSlot()==null?"":tran.getTranCtrPosition().getPosSlot()[0..3])
                                }
                    }
                }
            }
        }
        api.log("返回XML:\n" + resultXML.toString())
        return resultXML.toString()



    }

    class N4Operator{


        String ARGO_SERVICE_URL = "http://localhost:9080/apex/services/argoservice"
        String n4UserName = "gos"
        String n4UserPassword = "gosgos"

        public static String OK = "0"
        public static String INFO = "1"
        public static String WARNINGS = "2"
        public static String ERRORS = "3"


        ScopeCoordinateIdsWsType scope
        ArgoServiceLocator service
        ArgoServicePort port

        public String STATUS=""
        public String PAYLOAD=""
        public String[] RESULTS=null
        public MessageCollectorType MESSAGE_COLLECTOR = null;

        N4Operator(){
            scope = new ScopeCoordinateIdsWsType()
            scope.setOperatorId(ContextHelper.getThreadOperator().getOprId())
            scope.setComplexId(ContextHelper.getThreadComplex().getCpxId())
            scope.setFacilityId(ContextHelper.getThreadFacility().getFcyId())
            scope.setYardId(ContextHelper.getThreadYard().getYrdId())

            // 确定Web服务主机

            this.service = new ArgoServiceLocator()
            this.port = this.service.getArgoServicePort(new URL(this.ARGO_SERVICE_URL))
            Stub stub = (Stub) this.port

            // 指定用户名和密码
            stub._setProperty(Stub.USERNAME_PROPERTY, this.n4UserName)
            stub._setProperty(Stub.PASSWORD_PROPERTY, this.n4UserPassword)


        }
        // 发送带XML的请求，返回执行结果状态
        def sendRequestWithXml(String _xmlString) {
            // 发送请求
            GenericInvokeResponseWsType response = this.port.genericInvoke(this.scope, _xmlString)
            this.PAYLOAD = response.getResponsePayLoad()
            // 解析API响应
            ResponseType commonResponse = response.getCommonResponse()
            // 获取执行状态
            this.STATUS = commonResponse.getStatus()

            this.MESSAGE_COLLECTOR = commonResponse.getMessageCollector()


            if(commonResponse.getQueryResults()==null){
                this.RESULTS = new String[0]
            }
            else{
                this.RESULTS = new String[commonResponse.getQueryResults().length]
                commonResponse.getQueryResults().eachWithIndex { QueryResultType resultType, int i ->
                    this.RESULTS[i] = resultType.getResult()
                }
            }


        }
        String getResultsString(){
            if (this.RESULTS!=null){
                String res = ""
                this.RESULTS.each {r->
                    res = res + '\n' +r
                }
                return res
            }
            else {
                return ""
            }
        }
    }
    static synchronized String getGosTvKey(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS")
        return simpleDateFormat.format(new Date())
    }

}
