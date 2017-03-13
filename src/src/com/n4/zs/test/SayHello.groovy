package src.com.n4.zs.test

import com.navis.external.framework.ui.AbstractTableViewCommand
import com.navis.external.framework.ui.EUIExtensionHelper
import com.navis.framework.metafields.entity.EntityId
import com.navis.framework.util.message.MessageLevel
import com.ulcjava.base.application.ClientContext


class SayHello extends AbstractTableViewCommand {

	public void execute(EntityId inEntityId, List<Serializable> inGkeys,
		Map<String, Object> inParams) {
		log("Entity :" + inEntityId.getEntityName());
		EUIExtensionHelper extHelper = getExtensionHelper();

		long inGkey = inGkeys.get(0);


		String dialogTitle = "请在新打开的网页中查看";



		extHelper.showMessageDialog(MessageLevel.INFO, dialogTitle, "主键值" + ":" +  inGkey );



		ClientContext.showDocument("http://www.baidu.com")

	}
}
