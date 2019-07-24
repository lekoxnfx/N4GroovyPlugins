package com.n4.zs.test

import com.navis.external.framework.ui.AbstractTableViewCommand
import com.navis.external.framework.ui.EUIExtensionHelper
import com.navis.framework.metafields.entity.EntityId
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.util.message.MessageLevel
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.ulcjava.base.application.ClientContext
import groovy.sql.Sql


class SayHello extends AbstractTableViewCommand {

	public String execute(){

		long ufv_gkey = 2258597
		UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(ufv_gkey)







	}
}
