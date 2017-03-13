package src.com.n4.lwt.ui

import com.navis.argo.business.api.GroovyApi
import com.navis.external.framework.ui.AbstractTableViewCommand
import com.navis.external.framework.ui.EUIExtensionHelper
import com.navis.framework.metafields.entity.EntityId
import com.navis.framework.util.message.MessageLevel
import com.ulcjava.base.application.ClientContext

/**
 * Created by lekoxnfx on 15/10/14.
 */
class CopyNotesToPODUI extends AbstractTableViewCommand{

    GroovyApi api = new GroovyApi()

    public void execute(EntityId inEntityId, List<Serializable> inGkeys,
                        Map<String, Object> inParams){

        EUIExtensionHelper extHelper = getExtensionHelper();


        String result = ""
        inGkeys.each { gkey->
            result = result + gkey + ","
        }
        result = result.trim()[0..-2]

        extHelper.showMessageDialog(MessageLevel.INFO, "结果",result );



    }

}
