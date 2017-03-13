package src.com.n4.zs.cargo

import com.navis.argo.business.api.GroovyApi;
import com.navis.cargo.business.model.CargoLot;
import com.navis.inventory.business.units.Unit;
import com.navis.services.business.event.GroovyEvent;

class UpdateProductToUnit {
	public void execute(GroovyEvent event,GroovyApi api){
		
		Unit unit = (Unit) event.getEntity()
		
		CargoLot cl = CargoLot.findCargoLotById(unit.getUnitId())
		
		

		
	}

}
