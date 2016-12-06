package test

import java.sql.ResultSet
import java.text.SimpleDateFormat

class Test {

	static main(args) {
		
//		Date date = new Date();
//		SimpleDateFormat sdf =  new SimpleDateFormat("yyyyMMddHHmmSS")
//		Date startDate = sdf.parse("20150101000000")
//		println date.getTime()-startDate.getTime();


		String xml = """
c		"""
            xml = xml.trim()

            def RES = new XmlParser().parseText(xml)
            String sendTimeStr = RES.'Manifest'.'Head'.'SendTime'.text();
            RES.'Manifest'.'Response'.each {u->
                String unitNbr = u.'TransportEquipment'.'EquipmentIdentification'.'ID'.text()
                String insCode = u.'TransportEquipment'.'ResponseType'.'Code'.text()
                String insStr = u.'TransportEquipment'.'ResponseType'.'Text'.text()
                String jouneyId = u.'BorderTransportMeans'.'JourneyID'.text()
                String vesselUnCode = u.'BorderTransportMeans'.'ID'.text()
                println unitNbr
                println insCode
                println insStr
                println jouneyId
		}

	}
    public class VesselUnitBase {
        public String vesselVisitId;
        public String unitId;
        public String unitCategory;
        public String unitTypeIso;
        public String unitBasicLength;


        public VesselUnitBase(ResultSet inResult){
            try {
                this.vesselVisitId = inResult.getString("CV_ID");//船期编号
                this.unitId = inResult.getString("UNIT_ID");//集装箱号
                this.unitCategory = inResult.getString("UNIT_CATEGORY");//集装箱号
                this.unitTypeIso = inResult.getString("TYPE_ISO");//集装箱号
                this.unitBasicLength = inResult.getString("BASIC_LENGTH");//集装箱号

            }
            catch (Exception e){
            }
        }

    }

}
