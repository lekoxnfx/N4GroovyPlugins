package test

import java.sql.ResultSet

class Test2 {

	static main(args) {
        println("test start")
        String[] appts
		String inApptNbrs = ""
        if(inApptNbrs!=null&&inApptNbrs!=""){
            try{
                //解析预约号
//                                appts = this.inApptNbrs.contains(",")?inApptNbrs.split(",",-1):[inApptNbrs]
                //解析预约号
                String[] apptStrs = inApptNbrs.contains(",")?inApptNbrs.split(",",-1):[inApptNbrs]
                println("appStrs=" + apptStrs)
                /*
                修改于2017年03月13日
                */

                List<String> apptList = new ArrayList<>()
                apptStrs.each {apptStr->
                    println("appStr=" + apptStr)
                    if (apptStr.endsWith("A")){
                        apptList.add(apptStr[0..-2])
                    }
                    else if(apptStr.endsWith("B")){
                        apptList.add(apptStr[0..-2])
                        apptList.add(apptStr[0..-2])
                    }
                    else {
                        //兼容旧版预约号
                        apptList.add(apptStr[0..-2])
                    }
                }
                appts = apptList.toArray()
            }catch(Exception e){
               e.printStackTrace()
            }

            println("appts=" + appts)
        }
//
	}


}
