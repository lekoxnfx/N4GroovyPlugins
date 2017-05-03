package com.n4

import com.navis.argo.business.api.GroovyApi
import com.navis.services.business.event.GroovyEvent

import java.text.SimpleDateFormat

/**
 * Created by lekoxnfx on 2017/4/28.
 */
class TestGroovy {

    public String execute(Map args){

        String para1 = args.get("para1")

        return "para1 is " + para1

//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss")
//        File file = new File("C://Test.txt")
//        file.append(simpleDateFormat.format(new Date()) + "\r\n")
    }
}
