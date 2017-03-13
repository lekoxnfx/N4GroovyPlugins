package src.com.n4.zs.message
/*
 * 用于提供EDI报文所需的转换表
 */
class EDIMessageConventer {
	Map ISO_TO_GP; 
	public Map get_ISO_TO_GP_MAP(){
		ISO_TO_GP = new HashMap<String,String>();
		ISO_TO_GP.put("4530", "45R1")
		ISO_TO_GP.put("4230", "42R1")
		ISO_TO_GP.put("2230", "22R1")
		ISO_TO_GP.put("2530", "25R1")
		ISO_TO_GP.put("4200", "40GP")
		ISO_TO_GP.put("2200", "20GP")	
		ISO_TO_GP.put("4500", "42GP")
		
		return ISO_TO_GP
	}
}
