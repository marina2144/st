import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.stream.Collectors;
import org.json.simple.*;
import org.json.simple.parser.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.naming.NamingException;

import java.util.logging.Logger;
import java.util.logging.Level;


public class CheckDataMatrix extends HttpServlet{
	
	private static final Logger logger = Logger.getLogger("CheckDataMatrix");
	
	private int HTTPstatus=200;
	private String message="";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
						
		// //извлекаем список артикулов из тела запроса
		// String param="";
		
		// String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		// try{
			// JSONObject JSONobj = (JSONObject)  new JSONParser().parse(body);  
			// JSONArray items = (JSONArray) JSONobj.get("items");
			
			// int count=items.size();
			// for (int i=0;i<count;i++){
				// param=param+"'"+items.get(i).toString()+"',";
			// }
			// param=param.substring(0,param.length()-1);
		// }
		// catch (ParseException e){
			// logMes("Bad request body: "+param+". Exception: "+e.toString());
			// HTTPstatus=400;
			// message="Bad request body";
		// }
		// if (param.length()==0){
			// logMes("Bad request: Empty body");
			// HTTPstatus=400;
			// message="Empty body";			
		// }
		
		// if(HTTPstatus!=200){
			// response.sendError(HTTPstatus,message);
			// return;
		// }
		
		String param=request.getParameter("DM");
		if(param==null){
			response.sendError(400,"DM parameter expected!");
			return;			
		}
		
		log(param);
		
		//получение данных SQL и формирование ответа
		connectUT(param);
		
		if(HTTPstatus!=200){
			response.sendError(HTTPstatus,message);
			return;
		}

		response.setContentType("application/json");
		PrintWriter pw=response.getWriter();
		pw.println(message);
		pw.close();		
		
	}
	
	private void connectUT(String param){
		String query=getQueryText(param);

		DataSource ds;
		JSONArray resJSON = new JSONArray();
		
		try 
		{
			Context initContext = new InitialContext();
			Context envContext  = (Context)initContext.lookup("java:/comp/env");
			ds = (DataSource)envContext.lookup("jdbc/UT");

			Connection con = ds.getConnection();
			CallableStatement cstmt = con.prepareCall(query);

			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				JSONObject obj=new JSONObject();
				obj.put("artikul",rs.getString("artikul"));
				//obj.put("DM",rs.getString("DM"));
				resJSON.add(obj);
			}
			StringWriter out = new StringWriter();
			JSONValue.writeJSONString(resJSON, out);
			message=out.toString();
		}
		// Handle any errors that may have occurred.
		catch (NamingException e) {
			logMes("NamingException: "+e.toString());
			HTTPstatus=500;
			message="NamingException";				
		}
		catch (SQLException e) {
			logMes("SQLException: "+e.toString());
			HTTPstatus=500;
			message="SQLException";			
		}
		catch (IOException e) {
			logMes("IOException: "+e.toString());
			HTTPstatus=500;
			message="IOException";	
		}		  

	}
	
	//записать сообщение в файл
	private void logMes(String mes){ //todo переделать на промышленное логирование //есть метод log в GeneralServlet
		logger.warning(mes);
	}
	private String getQueryText(String param){ //текст запроса к ценам товаров		
		// String query="select"
						  // +" 	nomenkl._Fld4299 artikul"
						  // +" 	,DM._Description DM"
						  // +" 	,statusDM._Fld26017RRef st"
						  // +" from "
						  // +" 	_Reference25834 DM "
						  // +" join _InfoRg25894 statusDM on "
						  // +" 	DM._IDRRef=statusDM._Fld25895RRef and statusDM._Fld26017RRef=0xA825AC1F6B01E73D11E9676FEDC17C8E --'INTRODUCED', введен в оборот "
						  // +" join "
						  // +" 	_Reference169 nomenkl on "
						  // +" 	nomenkl._IDRRef=statusDM._Fld25920RRef"
						  // +" where DM._Description='010290000021830721IGJ6QmlMUsq5v'";
		
		
		// String query="select "
						 // +" 	DM._IDRRef DMref,"
						 // +" 	DM._Description DM31 "
						 // +" into #DM"
						 // +" from "
						 // +" 	_Reference25834 DM"
						 // +" where"
						 // +" 	DM._Description='010290000021830721IGJ6QmlMUsq5v'"
						 // +" select"
						 // +" 	DM.DMref,"
						 // +" 	statusDM._Fld25920RRef nomenklref,"
						 // +" 	DM.DM31 DM31"
						 // +" from "
						 // +" 	#DM DM"
						 // +" 	join _InfoRg25894 statusDM on"
						 // +" 	DM.DMref=statusDM._Fld25895RRef"
						 // +" where	"
						 // +" 	statusDM._Fld26017RRef=0xA825AC1F6B01E73D11E9676FEDC17C8E --'INTRODUCED', введен в оборот";
		
		// String query=" select "
						// +" 	DM._IDRRef DMref,"
						// +" 	DM._Description DM31 "
						// +" into #DM"
						// +" from "
						// +" _Reference25834 DM"
						// +" where"
						// +" 	DM._Description='010290000021830721IGJ6QmlMUsq5v'"
						// +" select"
						// +" 	DM.DMref,"
						// +" 	statusDM._Fld25920RRef nomenklref,"
						// +" 	DM.DM31 DM31"
						// +" into #DMstatus"
						// +" from "
						// +" 	#DM DM"
						// +" 	join _InfoRg25894 statusDM on"
						// +" 	DM.DMref=statusDM._Fld25895RRef"
						// +" where	"
						// +" 	statusDM._Fld26017RRef=0xA825AC1F6B01E73D11E9676FEDC17C8E --'INTRODUCED', введен в оборот"
						// +" select "
						// +" 	DMstatus.nomenklref nomenklref,"
						// +" 	nomenkl._Fld4299 artikul,"
						// +" 	DMstatus.DM31 DM31"
						// +" from "
						// +" 	#DMstatus DMstatus"
						// +" 	join _Reference169 nomenkl on"
						// +" 	DMstatus.nomenklref=nomenkl._IDRRef"
						// +" drop table #DM"
						// +" drop table #DMstatus";
		
			 //query="select top 100 _Fld4299 artikul from _Reference169 where _Fld4299='310836643'";
			 
			 
			 
			String query="use trade_2017_stockmann; "
						 +" select "
						 +" nomenkl._Fld4299 artikul "
						 +" from "
						 +" _Reference169 nomenkl "
						 +" join "
						 +" _InfoRg25894 statusDM "
						 +" on nomenkl._IDRRef=statusDM._Fld25920RRef "
						 +" join "
						 +" _Reference25834 DM on "
						 +" DM._IDRRef=statusDM._Fld25895RRef and statusDM._Fld26017RRef=0xA825AC1F6B01E73D11E9676FEDC17C8E --'INTRODUCED', введен в оборот "
						 +" and DM._Description='"+param+"'";

		return query;
	}
}