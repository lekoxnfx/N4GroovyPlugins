package src.com.n4.lwt


import com.navis.argo.business.api.GroovyApi
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.argo.business.extract.ChargeableUnitEvent
import com.navis.framework.business.Roastery
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.argo.ArgoExtractEntity
import com.navis.argo.ArgoExtractField
import groovy.sql.Sql

/**
 * 根据列表的事件类型,写入相应数据
 * Created by Badger on 16/1/27.
 */
class LWT_CUEProcess extends GroovyApi {
    private List<String> eventTypeList
    private List<Unit> unitList
    private Sql sqlN4

    public void execute(Map args) {
        this.unitList = new ArrayList<Unit>()
        eventTypeList = ['UNIT_LOAD', 'UNIT_DISCH']
        process()
    }

    private void process() {
        logInfo("更新ChargeableUnitEvent");


        updateUnitIdList()
        this.unitList.each { unit ->
            unit.getUnitUfvSet().each { ufvItem ->
                UnitFacilityVisit ufv = (UnitFacilityVisit) ufvItem

                def changeValue = unit.getUnitFlexString11()

                //当Unit的中转字段不为空,则写入相应的事件
                if (changeValue.size() > 0) {
                    // 获取ChargeableUnitEvent
                    eventTypeList.each { eventTypeId ->

                        List cueList = findByEventTypeIdAndUfv(ufv, eventTypeId);

                        if (!cueList.isEmpty()) {
                            cueList.each { cueItem ->
                                ChargeableUnitEvent cue = (ChargeableUnitEvent) cueItem;
                                if (cue != null)
                                    cue.setBexuFlexString07(changeValue)
                            }
                        }
                    }
                }
            }

        }
        logInfo("结束更新");
    }

    private void updateUnitIdList() {

        try {
            //初始化N4数据库连接
            String DB = "jdbc:oracle:thin:@" + "192.168.37.110" + ":" + "1521" + ":" + "n4"
            String USER = "n4user"
            String PASSWORD = "n4lwt"
            String DRIVER = 'oracle.jdbc.driver.OracleDriver'
            sqlN4 = Sql.newInstance(DB, USER, PASSWORD, DRIVER)


            String unitQueryStr = """
SELECT GKEY
FROM INV_UNIT
WHERE FLEX_STRING11 LIKE '中转'
"""
            sqlN4.eachRow(unitQueryStr) { row ->
                long unit_gkey = (long) row.'gkey'
                Unit unit = Unit.hydrate(unit_gkey)
                unitList.add(unit)
            }
        } catch (Exception e) {
        }
    }

    /**
     * 根据事件名,找到相应UFV的ChargeableUnitEvent
     */
    private List findByEventTypeIdAndUfv(UnitFacilityVisit inUfv, String inEventTypeId) {
        DomainQuery dq = QueryUtils.createDomainQuery(ArgoExtractEntity.CHARGEABLE_UNIT_EVENT)
                .addDqPredicate(PredicateFactory.eq(ArgoExtractField.BEXU_EVENT_TYPE, inEventTypeId))
                .addDqPredicate(PredicateFactory.eq(ArgoExtractField.BEXU_UFV_GKEY, inUfv.getPrimaryKey()));
        return Roastery.getHibernateApi().findEntitiesByDomainQuery(dq);
    }

}
