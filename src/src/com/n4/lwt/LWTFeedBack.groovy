package src.com.n4.lwt

import com.navis.argo.ArgoBizMetafield
import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.FreightKindEnum
import com.navis.argo.business.reference.LineOperator
import com.navis.argo.business.reference.ScopedBizUnit
import com.navis.inventory.InventoryField
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.event.GroovyEvent
import com.navis.vessel.VesselBizMetafield
import com.navis.vessel.business.schedule.VesselVisitDetails
import groovy.sql.Sql

import java.sql.SQLException
import java.text.SimpleDateFormat

/**
 * Created by lekoxnfx on 15/11/11.
 * 用于龙湾将船期作业回写集团反馈数据库
 *
 */
class LWTFeedBack extends  GroovyApi{

    public GroovyEvent event
    public GroovyApi api
    Sql sqlN4;
    Sql sqlGroup;
    boolean isSuccess = false;
    boolean noError = true;
    String notes = "";

    //
    String globalCBBH = "";

    public void execute(GroovyEvent inEvent,GroovyApi inApi){
        this.event = inEvent
        this.api = inApi

        VesselVisitDetails vvd = (VesselVisitDetails)event.getEntity();
        if (vvd == null){
            notes = "获取不到船期信息，vvd为空！";
            api.log(notes)
        }
        else if(vvd.getVvFlexString06()==null){
            notes = "获取不到预确报编号！";
            api.log(notes)
        }
        else {
            api.log("开始回写")
            globalCBBH = vvd.getVvFlexString06()
            init()
            if (noError){
                doFeedBack(vvd)
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss")
                if(isSuccess){
                    vvd.setFieldValue(VesselBizMetafield.VV_FLEX_STRING07,"码头反馈成功！[" + dateFormat.format(new Date()) + "]")
                }
                else {
                    vvd.setFieldValue(VesselBizMetafield.VV_FLEX_STRING07,"码头反馈失败，原因：" +notes + "[" + dateFormat.format(new Date()) + "]")
                }
            }
        }


    }
    public void doFeedBack(VesselVisitDetails vvd){
        try {
            //定义时间格式
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            //获取集装箱列表
            List<UnitFacilityVisit> ufvLoadList = new ArrayList<UnitFacilityVisit>()
            List<UnitFacilityVisit> ufvDschList = new ArrayList<UnitFacilityVisit>()
            long cvgkey = vvd.getCvdCv().getCvGkey()
            api.log("查询装卸船列表")
            //查询装卸的Unit列表
            String unitQueryDschStr = """
				select ufv.gkey from inv_unit_fcy_visit ufv, inv_unit u where ufv.actual_ib_cv = '${cvgkey}' and ufv.unit_gkey = u.gkey
                and ufv.TRANSIT_STATE in ('S40_YARD','S50_ECOUT','S60_LOADED','S70_DEPARTED')
				"""
            String unitQueryLoadStr = """
				select ufv.gkey from inv_unit_fcy_visit ufv, inv_unit u where ufv.actual_ob_cv = '${cvgkey}' and ufv.unit_gkey = u.gkey                 and ufv.TRANSIT_STATE in ('S30_ECIN','S40_YARD','S50_ECOUT','S60_LOADED','S70_DEPARTED')
                and ufv.TRANSIT_STATE in ('S60_LOADED','S70_DEPARTED')
				"""
            api.log("sql语句：\n\r" + unitQueryDschStr + "\n\r" + unitQueryLoadStr)

            sqlN4.eachRow(unitQueryDschStr) {r->
                long ufv_gkey = r.'gkey'
                UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufv_gkey)
                ufvDschList.add(ufv)
            }
            sqlN4.eachRow(unitQueryLoadStr) {r->
                long ufv_gkey = r.'gkey'
                UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufv_gkey)
                ufvLoadList.add(ufv)
            }
            api.log("装卸船箱列表查询完毕")
            //插入的SQL语句组
            List<String> sql_str_list_v = new ArrayList<String>()
            List<String> sql_str_list_u = new ArrayList<String>()
            api.log("获取回写的船信息")
            //获取船期信息
            FeedBackVessel fv = new FeedBackVessel()

            fv.JLBH = vvd.getPrimaryKey()
            fv.LX = "J"
            fv.CBBH = globalCBBH
            fv.JGSJ = simpleDateFormat.format(vvd.getCvdCv().getCvATA())
            fv.KBSJ = simpleDateFormat.format(vvd.getCvdCv().getCvATA())
            fv.KGSJ = simpleDateFormat.format(vvd.getVvdTimeStartWork())
            fv.WGSJ = simpleDateFormat.format(vvd.getVvdTimeEndWork())
            fv.LBSJ = simpleDateFormat.format(vvd.getCvdCv().getCvATD())
            fv.LGSJ = simpleDateFormat.format(vvd.getCvdCv().getCvATD())
            fv.CZLX = "I"
            fv.SFHQ = "N"
            //船期SQL Merge语句
            def sql_v = """
            MERGE INTO SC_CB_MTFK USING dual ON ( JLBH = '${fv.JLBH}' )
            WHEN MATCHED THEN UPDATE SET
                LX = 'J',
                CBBH = '${fv.CBBH}',
                JGSJ = TO_DATE('${fv.JGSJ}','YYYY-MM-DD hh24:mi:ss'),
                KBSJ = TO_DATE('${fv.KBSJ}','YYYY-MM-DD hh24:mi:ss'),
                KGSJ = TO_DATE('${fv.KGSJ}','YYYY-MM-DD hh24:mi:ss'),
                WGSJ = TO_DATE('${fv.WGSJ}','YYYY-MM-DD hh24:mi:ss'),
                LBSJ = TO_DATE('${fv.LBSJ}','YYYY-MM-DD hh24:mi:ss'),
                LGSJ = TO_DATE('${fv.LGSJ}','YYYY-MM-DD hh24:mi:ss'),
                CZLX = 'U',
                SFHQ = 'N',
                HQSJ = ''
            WHEN NOT MATCHED THEN INSERT (JLBH,LX,CBBH,JGSJ,KBSJ,KGSJ,WGSJ,LBSJ,LGSJ,CZLX,SFHQ)
                VALUES ('${fv.JLBH}',
                'J',
                '${fv.CBBH}',
                TO_DATE('${fv.JGSJ}','YYYY-MM-DD hh24:mi:ss'),
                TO_DATE('${fv.KBSJ}','YYYY-MM-DD hh24:mi:ss'),
                TO_DATE('${fv.KGSJ}','YYYY-MM-DD hh24:mi:ss'),
                TO_DATE('${fv.WGSJ}','YYYY-MM-DD hh24:mi:ss'),
                TO_DATE('${fv.LBSJ}','YYYY-MM-DD hh24:mi:ss'),
                TO_DATE('${fv.LGSJ}','YYYY-MM-DD hh24:mi:ss'),
                'I',
                'N')
            """

            api.log("船期SQL:" + sql_v)
            sql_str_list_v.add(sql_v)

            api.log("获取回写的箱信息")
            //获取箱信息
            List<FeedBackUnit> feedBackUnitList = new ArrayList<>();
            ufvDschList.each { ufv->
                FeedBackUnit fu = new FeedBackUnit()
                fu.JLBH = ufv.getPrimaryKey()
                fu.CBBH = fv.CBBH
                fu.PZBH = ufv.getPrimaryKey()
                ScopedBizUnit sbu = ufv.getUfvUnit().getUnitLineOperator();
                if(sbu.getField(ArgoBizMetafield.LINEOP_FLEX_STRING01) == "外贸"){
                    fu.MYXZ = "W"
                }
                else {
                    fu.MYXZ = "N"
                }
                fu.JCK = "I"
                fu.HWLX = "J"
                try {
                    fu.HWDM = ufv.getUfvUnit().getUnitGoods().getGdsCommodity().getCmdyId()
                } catch (Exception e) {
                    fu.HWDM = ""
                }
                try {
                    fu.HWMC = ufv.getUfvUnit().getUnitGoods().getGdsCommodity().getCmdyShortName()
                } catch (Exception e) {
                    fu.HWMC = ""
                }
                fu.HWZL = ufv.getUfvUnit().getUnitGoodsAndCtrWtKg()
                fu.XH = ufv.getUfvUnit().getUnitId()
                fu.YYR = sbu.getBzuName()
                if(ufv.getUfvUnit().getUnitFreightKind().equals(FreightKindEnum.MTY)){
                    fu.KZ = "E"
                    fu.HWDM = "空箱"
                    fu.HWMC = "空箱"
                }
                else {
                    fu.KZ = "F"
                }
                String unitType = convent(ufv.getUfvUnit().getPrimaryEq().getEqEquipType().getEqtypId())
                fu.CC = unitType[0..1]
                fu.XX = unitType[2..3]

                fu.CZLX = "I"
                fu.SFHQ = "N"
                try {
                    fu.ZHG = ufv.getUfvUnit().getUnitRouting().getRtgPOL().getPointUnLoc().getUnlocPlaceName()
                } catch (Exception e1) {
                    fu.ZHG = ""
                }
                try {
                    fu.XHG = ufv.getUfvUnit().getUnitRouting().getRtgPOD1().getPointUnLoc().getUnlocPlaceName()
                } catch (Exception e1) {
                    fu.XHG = ""
                }

                feedBackUnitList.add(fu)
            }

            ufvLoadList.each { ufv->
                FeedBackUnit fu = new FeedBackUnit()
                fu.JLBH = ufv.getPrimaryKey()
                fu.CBBH = fv.CBBH
                fu.PZBH = vvd.getPrimaryKey()
                ScopedBizUnit sbu = ufv.getUfvUnit().getUnitLineOperator();
                if(sbu.getField(ArgoBizMetafield.LINEOP_FLEX_STRING01) == "外贸"){
                    fu.MYXZ = "W"
                }
                else {
                    fu.MYXZ = "N"
                }
                fu.JCK = "O"
                fu.HWLX = "J"
                try {
                    fu.HWDM = ufv.getUfvUnit().getUnitGoods().getGdsCommodity().getCmdyId()
                } catch (Exception e) {
                    fu.HWDM = ""
                }
                try {
                    fu.HWMC = ufv.getUfvUnit().getUnitGoods().getGdsCommodity().getCmdyShortName()
                } catch (Exception e) {
                    fu.HWMC = ""
                }
                fu.HWZL = ufv.getUfvUnit().getUnitGoodsAndCtrWtKg()
                fu.XH = ufv.getUfvUnit().getUnitId()
                fu.YYR = sbu.getBzuName()
                if(ufv.getUfvUnit().getUnitFreightKind().equals(FreightKindEnum.MTY)){
                    fu.KZ = "E"
                    fu.HWDM = "空箱"
                    fu.HWMC = "空箱"
                }
                else {
                    fu.KZ = "F"
                }
                String unitType = convent(ufv.getUfvUnit().getPrimaryEq().getEqEquipType().getEqtypId())
                fu.CC = unitType[0..1]
                fu.XX = unitType[2..3]

                fu.CZLX = "I"
                fu.SFHQ = "N"
                try {
                    fu.ZHG = ufv.getUfvUnit().getUnitRouting().getRtgPOL().getPointUnLoc().getUnlocPlaceName()
                } catch (Exception e1) {
                    fu.ZHG = "温州"
                }
                try {
                    fu.XHG = ufv.getUfvUnit().getUnitRouting().getRtgPOD1().getPointUnLoc().getUnlocPlaceName()
                } catch (Exception e1) {
                    fu.XHG = ""
                }

                feedBackUnitList.add(fu)
            }

            //生成sql列表
            feedBackUnitList.each {u->

                String sql_u = """
                MERGE INTO SC_CB_HWPZ USING dual ON ( JLBH = '${u.JLBH}' )
                WHEN MATCHED THEN UPDATE SET
                    CBBH = '${u.CBBH}',
                    PZBH = '${u.PZBH}',
                    MYXZ = '${u.MYXZ}',
                    JCK = '${u.JCK}',
                    HWLX = '${u.HWLX}',
                    HWDM = '${u.HWDM}',
                    HWMC = '${u.HWMC}',
                    HZ = '',
                    HWZL = '${u.HWZL}',
                    XH = '${u.XH}',
                    YYR = '${u.YYR}',
                    KZ = '${u.KZ}',
                    CC = '${u.CC}',
                    XX = '${u.XX}',
                    XHL = '${u.XHL}',
                    ZHG = '${u.ZHG}',
                    XHG = '${u.XHG}',
                    CZLX = 'U',
                    SFHQ = 'N',
                    HQSJ = ''
                WHEN NOT MATCHED THEN INSERT (JLBH,CBBH,PZBH,MYXZ,JCK,HWLX,HWDM,HWMC,HZ,HWZL,XH,YYR,KZ,CC,XX,XHL,ZHG,XHG,CZLX,SFHQ)
                    VALUES ('${u.JLBH}',
                    '${u.CBBH}',
                    '${u.PZBH}',
                    '${u.MYXZ}',
                    '${u.JCK}',
                    '${u.HWLX}',
                    '${u.HWDM}',
                    '${u.HWMC}',
                    '',
                    '${u.HWZL}',
                    '${u.XH}',
                    '${u.YYR}',
                    '${u.KZ}',
                    '${u.CC}',
                    '${u.XX}',
                    '${u.XHL}',
                    '${u.ZHG}',
                    '${u.XHG}',
                    'I',
                    'N')
                """

                api.log("箱SQL:" + sql_u)
                sql_str_list_u.add(sql_u)
            }


            //数据库操作
            api.log("开始数据库操作")
            try {
                sqlGroup.withTransaction {
                    //处理船期
                    def res_v = sqlGroup.withBatch { stmt->
                        sql_str_list_v.each {s->
                            stmt.addBatch(s)
                        }
                    }
                    if(res_v == [1]){
                        api.log("插入船期信息成功！")
                        //处理箱信息
                        //先全部标记删除，然后标记为I或U

                        def sqlStr_deleteUnit = "update SC_CB_HWPZ set CZLX = 'D',HQSJ=''  where PZBH = '${vvd.getPrimaryKey()}'"
                        def res_d = sqlGroup.executeUpdate(sqlStr_deleteUnit)
                        if(res_d == [1]){
                            api.log("删除旧历史记录成功！")
                        }
                        def res_u = sqlGroup.withBatch { stmt->
                            sql_str_list_u.each {s->
                                stmt.addBatch(s)
                            }
                        }
                        def r_u = true
                        res_u.each {r->
                            if(r!=1) r_u = false
                        }
                        if(r_u == true){
                            api.log("插入Unit信息成功！")
                            isSuccess = true
                            notes = "码头反馈成功！"
                        }
                    }
                    else {
                        noError = false
                        notes = "插入船期信息失败！"
                    }

                    api.log("数据库操作结束")
                }
            } catch (SQLException se) {
                api.log("插入数据错误！回滚！")
                api.log(se.toString())
                noError = false;
                notes =  se.toString()
            }

            api.log("回写结束")

        } catch (Exception e) {
            api.log(e.toString())
            noError = false
            notes = e.toString()
            e.printStackTrace()
        }




    }


    public void init(){
        try{
            //初始化N4数据库连接
            String DB = "jdbc:oracle:thin:@" + "192.168.37.110" + ":" + "1521" + ":" + "n4"
            String USER = "n4user"
            String PASSWORD = "n4lwt"
            String DRIVER = 'oracle.jdbc.driver.OracleDriver'
            sqlN4 = Sql.newInstance(DB, USER, PASSWORD, DRIVER)
            //初始化集团回写数据库连接
            String DB2 = "jdbc:oracle:thin:@" + "192.168.37.103" + ":" + "1521" + ":" + "database"
            String USER2 = "lwjk"
            String PASSWORD2 = "wzlwjk"
            String DRIVER2 = 'oracle.jdbc.driver.OracleDriver'
            sqlGroup = Sql.newInstance(DB2, USER2, PASSWORD2, DRIVER2)
        }catch(Exception e){
            noError = false;
            notes = e.toString()
            api.logWarn(notes)
        }
    }
    public String createJLBH(String inId){//创建记录编号(依据VesselVisitId或者UnitId)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return sdf.format(new Date()) + "-" + inId

    }
    def convent(def _Value){
        switch(_Value){
            case '20GP':
            case '20DC':
                return '2200'
                break
            case '40GP':
                return '4200'
                break
            case '40HQ':
                return '4500'
                break
            case '45GP':
                return '9500'
                break
//-----------------------------------------------------------------
            case '4500':
                return '40HQ'
                break
            case '4200':
                return '40GP'
                break
            case '2200':
                return '20GP'
                break
            case '2500':
                return '20GP'
                break
            case '9500':
                return '45GP'
                break
            default:
                return _Value
        }

    }
    class FeedBackVessel {
        String JLBH = ""    //VARCHAR2(32)	记录编号	KEY	是
        String LX	= ""    //CHAR(1)	类型（散货S/集装箱J）		是
        String CBBH	= ""    //NUMBER(10)	预确报编号，即SHIPDICTID(包括散货船和集装箱船)		是
        String JGSJ = ""    //	DATE	进港时间		是
        String KBSJ = ""    //	DATE	靠泊时间		是
        String KGSJ = ""    //	DATE	开工时间		是
        String WGSJ = ""    //	DATE	完工时间		是
        String LBSJ = ""    //	DATE	离泊时间		是
        String LGSJ = ""    //	DATE	离港时间		是
        String ZRYSSS = ""    //	NUMBER(10，2)	自然因素艘时		否
        String ZGZTS = ""    //	NUMBER(10，2)	在港总停时		否
        String CSS = ""    //	NUMBER(10，2)	舱时数		否
        String SCXSS = ""    //	NUMBER(10，2)	生产性艘时		否
        String ZXSS = ""    //	NUMBER(10，2)	装卸艘时		否
        String FSCXSS = ""    //	NUMBER(10，2)	非生产性艘时		否
        String GFYYSS = ""    //	NUMBER(10，2)	港方原因艘时		否
        String DMTSS = ""    //	NUMBER(10，2)	等码头艘时		否
        String CFYYSS = ""    //	NUMBER(10，2)	船方原因艘时		否
        String WZBMYYSS = ""    //	NUMBER(10，2)	物资部门原因艘时		否
        String QTYYSS = ""    //	NUMBER(10，2)	其他原因艘时		否
        String CZLX = ""    //	CHAR(1)	操作类型（I:插入，U:更新，D:删除）		是
        String SFHQ = ""    //	CHAR(1)	是否获取（Y-是，N-否）		是
        String HQSJ = ""    //	DATE	获取时间		否

    }
    class FeedBackUnit {
        String JLBH = ""    //	VARCHAR2(32)	记录编号	KEY	是
        String CBBH = ""    //	NUMBER(10)	预确报编号，即SHIPDICTID		是
        String MYXZ = ""    //	CHAR(1)	贸易性质（内贸N/外贸W） (散货默认 N）		是
        String JCK = ""    //	CHAR(1)	进出口（进I/出O） （散货默认 I）		是
        String HWLX = ""    //	CHAR(1)	货物类型（散货S/集装箱J）		是
        String HWDM = ""    //	VARCHAR2(20)	货物代码		是
        String HWMC = ""    //	VARCHAR2(100)	货物名称		是
        String HZ = ""    //	VARCHAR2(40)	货主		否
        String HWJS = ""    //	NUMBER(10，3)	货物件数		否
        String HWZL = ""    //	NUMBER(10，3)	货物重量		否
        String HWTJ = ""    //	NUMBER(10，3)	货物体积		否
        String XH = ""    //	VARCHAR2(11)	箱号（集装箱填入）		否
        String YYR = ""    //	VARCHAR2(40)	营运人（集装箱填入）		否
        String KZ = ""    //	CHAR(1)	E/F(集装箱填入)		否
        String CC = ""    //	VARCHAR2(2)	20/40/45/48(集装箱填入)		否
        String XX = ""    //	VARCHAR2(2)	FR/GP/HQ/OT/RF/RH/TK/HT(集装箱填入)		否
        String XHL = ""    //	VARCHAR2(2)	普通货/危险品|冷藏箱(集装箱填入)		否
        String CZLX = ""    //	CHAR(1)	操作类型（I:插入，U:更新，D:删除）		是
        String SFHQ = ""    //	CHAR(1)	是否获取（Y-是，N-否）		是
        String HQSJ = ""    //	DATE	获取时间		否
        String ZHG = ""     //装货港
        String XHG = ""     //卸货港
        String PZBH = ""    //配载编号？
    }

}
