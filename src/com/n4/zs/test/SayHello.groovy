package com.n4.zs.test

import com.navis.external.framework.ui.AbstractTableViewCommand
import com.navis.external.framework.ui.EUIExtensionHelper
import com.navis.framework.metafields.entity.EntityId
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.util.message.MessageLevel
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.ulcjava.base.application.ClientContext
import groovy.sql.Sql


class SayHello extends AbstractTableViewCommand {

	public String execute(){

		long ufv_gkey = 10833598
		UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufv_gkey)

		String stowage_location=""
		try{
			//已经上船的获取最后位置
			if(ufv.getUfvTransitState().equals(UfvTransitStateEnum.S60_LOADED)||
					ufv.getUfvTransitState().equals(UfvTransitStateEnum.S70_DEPARTED)){
				stowage_location = ufv.getUfvLastKnownPosition().getPosSlot();
			}
			else{
				//未上船的获取计划位置
				stowage_location = ufv.getFinalPlannedPosition().getPosSlot();
			}
		}catch(Exception e){
			stowage_location = ""
		}
		if(stowage_location!=null&&stowage_location!=""){
			return  ufv.getUfvUnit().getUnitId() +"    " + stowage_location + "  true"
		}
		else{
			return  ufv.getUfvUnit().getUnitId() +"    " + stowage_location + "  false"
		}






	}
}
