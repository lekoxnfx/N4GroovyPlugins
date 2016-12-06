package test

import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder

/*
 <groovy class-location="database" class-name="ProcessMT0102OLD">
	 <parameters>
		<parameter id="xmlcontent" value=""/>
	 </parameters>
 </groovy>
*/
class TestMain {

	static main(args) {

		String value = """
			<message-response>
				<responses response="OK" />
				<messages message="aaa">
			</message-response>
		"""

//		def xmlStringWriter = new StringWriter()
//		def xmlBuilder = new MarkupBuilder(xmlStringWriter)
//		xmlBuilder.'groovy'('class-location':"database",'class-name':"ProcessMT0102OLD"){
//			'parameters'{
//				'parameter'('id':'xmlcontent',value:value.trim())
//			}
//		}
//		println  xmlStringWriter.toString()

		def builder = new StreamingMarkupBuilder()
		def request = {
			'groovy'('class-location':'database'){

			}

		}
		println(builder.bind(request).toString())
	}

}
