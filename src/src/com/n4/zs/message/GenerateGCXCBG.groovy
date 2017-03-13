package src.com.n4.zs.message

import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.model.CarrierVisit
import com.navis.argo.business.model.Complex;
import com.navis.argo.business.model.Facility;
import com.navis.argo.business.model.Operator;
import com.navis.argo.business.model.VisitDetails
import com.navis.cargo.business.model.BillOfLading;
import com.navis.cargo.business.model.BillOfLadingHbr;
import com.navis.cargo.business.model.BillOfLadingManagerPea;
import com.navis.services.business.event.GroovyEvent;

import java.text.SimpleDateFormat
import java.util.Date;

class GenerateGCXCBG extends GroovyApi{
	public static String SENDER_ID = "ZSDND"
	public static String RECEIVER_ID = "2904"
	public static String BARRIER_NUM = "290402"
	
	public Operator operator = Operator.findOperator("ZSCT")
	public Complex complex = Complex.findComplex("ZST", operator)
	public Facility facility = Facility.findFacility("DLT", complex)
	
	public currentMessageId;
	public String fileDir = "D:\\MESSAGE\\YDBG"
	public GroovyEvent event
	public GroovyApi api
	
	public void execute(GroovyEvent inEvent,GroovyApi inApi){
		this.event = inEvent
		this.api = inApi
		VisitDetails vd = (VisitDetails)event.getEntity();
//		BillOfLading bol = (BillOfLading)event.getEntity();
//		MetafieldId unitCategoryMid = MetafieldIdFactory.valueOf("")
	}
	
	public String createGCXCBGXML(VisitDetails vd){
		CarrierVisit cv = vd.getCvdCv()
		long cvGkey = cv.getCvGkey()
		
		//查询所有提单的Gkey
		String vesselCode,vesselName,voyage,inOutGateTime,truckNo,freightKind;
		
		
		
		
		
		
//		def ZXCUnitString = new StringWriter()
//		def ZXCUnits = new MarkupBuilder(ZXCUnitString)
//		ZXCUnits.'HarbourEDI'{
//			'Head'{
//				'MessageId'(Message_Id)
//				'MessageType'(Message_Type)
//				'SendId'(Send_Id)
//				'ReceiveId'(Receive_Id)
//				'BarrierNum'(Barrier_Num)
//				'SendTime'(TimeGenerator.GetXMLTime())
//			}
//			'Declaration'{
//				'ApplyHead'{
//					'VesselCode'(v._VesselCode)
//					'VesselName'(v._VesselName)
//					'Voyage'(v._Voyage)
//					'Direct'(v._Direct)
//					'IEFlag'(v._IEFlag)
//					'NationalityCode'(v._NationalityCode)
//					'ArrivePortTime'(v._ArrivePortTime)
//					'LeavePortTime'(v._LeavePortTime)
//					'FactLoadDischargeTime'(v._FactLoadDischargeTime)
//					'LoadDischargePortCode'(v._LoadDischargePortCode)
//					'LoadDischargePort'(v._LoadDischargePort)
//					'ApplyLists'{
//						l_zcu.each { u->
//							'ApplyList'{
//									'ContainerNo'(u._ContainerNo)
//									'ContainerSize'(u._ContainerSize)
//									'ContainerOperatorCode'(u._ContainerOperatorCode)
//									'ContainerOperator'(u._ContainerOperator)
//									'ContainerStatus'(u._ContainerStatus)
//									'ContainerType'(u._ContainerType)
//									'SealNo'(u._SealNo)
//									'StowageLocation'(u._StowageLocation)
//									'BillNo'(u._BillNo)
//									'GrossWeight'(u._GrossWeight)
//									'VirtualShipName'(u._VirtualShipName)
//									'VirtualVoyageNo'(u._VirtualVoyageNo)
//									'GoodsType'(u._GoodsType)
//							}
//						}
//					}
//				}
//			}
//		}
		 
	}
	
	
	public String generateMessageId(String inMessageType){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
		String ranstr = ""
		for(int i=0;i<5;i++){
			ranstr += new Random().nextInt(10).toString()
		}
		return inMessageType + "_" + sdf.format(new Date()) + "_" + ranstr
	}
	public String getXMLTime(){
		Date d = new Date()
		return formateToXMLTime(d)
	}
	public String formateToXMLTime(Date date){
		if(date==null){
			return ""
		}
		else{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")//设置日期格式
			SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss")
			return df.format(date) + 'T' + df2.format(date)// new Date()为获取当前系统时间
		}
		
	}
}
