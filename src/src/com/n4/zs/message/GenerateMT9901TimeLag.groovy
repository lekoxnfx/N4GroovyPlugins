package src.com.n4.zs.message

import com.navis.argo.business.api.GroovyApi

import java.text.SimpleDateFormat

/**
 * MT9901报文生成与时间有关,需要间隔1秒以上
 * Created by liuminhang on 16/7/14.
 */
class GenerateMT9901TimeLag {
    public GroovyApi api


    public static synchronized String makeTimeLag(){
        String timeLag = ""

        try{
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS")//设置日期格式
            timeLag = df.format(new Date())
            Thread.sleep(1200)
            timeLag = timeLag + "  " + df.format(new Date())

        }catch(Exception e){

        }
        finally {
            return timeLag
        }
    }
}
