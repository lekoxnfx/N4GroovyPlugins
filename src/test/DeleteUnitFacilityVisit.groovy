package test

import com.navis.argo.business.api.GroovyApi;
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit;
import com.navis.services.business.event.GroovyEvent;

class DeleteUnitFacilityVisit {
	
	GroovyApi api;
	public void execute(GroovyEvent inEvent,GroovyApi inApi){
		UnitFacilityVisit ufv = UnitFacilityVisit.hydrate(null)
		
		Unit u;
		
	}
}
