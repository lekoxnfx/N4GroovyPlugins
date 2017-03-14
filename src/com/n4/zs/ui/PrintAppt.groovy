package com.n4.zs.ui

import com.navis.external.framework.ui.AbstractTableViewCommand
import com.navis.external.framework.ui.EUIExtensionHelper
import com.navis.framework.metafields.entity.EntityId
import com.navis.framework.util.message.MessageLevel
import com.ulcjava.base.application.ClientContext

/**
 * Created by lekoxnfx on 15/10/14.
 */
class PrintAppt extends AbstractTableViewCommand{

    String url1 = """
    http://192.168.50.20:8380/jasperserver/flow.html?_flowId=viewReportFlow&standAlone=true&_flowId=viewReportFlow&ParentFolderUri=%2FReports&reportUnit=%2FReports%2FDLTAppt&decorate=no&j_username=n4&j_password=n4
    """

    public void execute(EntityId inEntityId, List<Serializable> inGkeys,
                        Map<String, Object> inParams){

        EUIExtensionHelper extHelper = getExtensionHelper();


        String url2 = "&ApptGkeys="
        inGkeys.each { gkey->
            url2 = url2 + gkey + ","
        }
        url2 = url2.trim()[0..-2]
        String url = url1.trim() + url2


        String dialogTitle = "请在新打开的网页中查看";



        extHelper.showMessageDialog(MessageLevel.INFO, dialogTitle, "参数值" + ":" +  url2 );



        ClientContext.showDocument(url)
    }

}
