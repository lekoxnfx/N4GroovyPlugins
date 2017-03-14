package com.n4.lwt

/**
 * Created by lekoxnfx on 15/11/20.
 */
import com.navis.external.framework.ui.AbstractTableViewCommand
import com.navis.external.framework.ui.EUIExtensionHelper
import com.navis.framework.metafields.MetafieldId
import com.navis.framework.metafields.MetafieldIdFactory
import com.navis.framework.metafields.entity.EntityId
import com.navis.framework.util.message.MessageLevel
import com.navis.inventory.business.moves.MoveEvent
import com.ulcjava.base.application.ClientContext


class ExcludeMoveHistory extends AbstractTableViewCommand {

    public void execute(EntityId inEntityId, List<Serializable> inGkeys,
                        Map<String, Object> inParams) {
        log("Entity :" + inEntityId.getEntityName());
        EUIExtensionHelper extHelper = getExtensionHelper();

        String paras = ""

        inGkeys.each{gkey->
            paras = paras + gkey + ","

            MoveEvent moveEvent = MoveEvent.resolveMoveEvent(MoveEvent.hydrate(gkey))
            MetafieldId mid = MetafieldIdFactory.valueOf("mveExclude")
            moveEvent.setFieldValue(mid,true)

        }
        paras = paras[0..-2]
        String dialogTitle = "Entity:Gkeys";



        extHelper.showMessageDialog(MessageLevel.INFO, dialogTitle, inEntityId.getEntityName() + ":" +  paras )




    }
}

