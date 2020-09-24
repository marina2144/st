import javax.servlet.*;
import javax.servlet.http.*;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
//import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import java.util.stream.Collectors;
import org.json.simple.*;
import org.json.simple.parser.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.naming.NamingException;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.Collection.HashMap;


public class DataMatrix extends HttpServlet{
	
	private static final Logger logger = Logger.getLogger("DataMatrix");
	
	private HashMap<Integer,String> HTTPresult=new HashMap<>();
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
						
		//извлекаем список артикулов из тела запроса
		String param="";
		
		String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		try{
			JSONObject JSONobj = (JSONObject)  new JSONParser().parse(body);  
			JSONArray items = (JSONArray) JSONobj.get("items");
			
			int count=items.size();
			for (int i=0;i<count;i++){
				param=param+"'"+items.get(i).toString()+"',";
			}
			param=param.substring(0,param.length()-1);
		}
		catch (ParseException e){
			logMes("Bad request body: "+param+". Exception: "+e.toString());
			HTTPresult.put(400,"Bad request body");
		}
		if (param.length()==0){
			logMes("Bad request: Empty body");
			HTTPresult.put(400,"Empty body");
		}
		
		String mes=HTTPresult.get(400);
		if(mes!=null){
			response.sendError(400,mes);
			return;
		}
		
		//получение данных SQL и формирование ответа
		connectUT(param);
		
		String mes=HTTPresult.get(500);
		if(mes!=null){
			response.sendError(500,mes);
			return;
		}		
		
		String mes=HTTPresult.get(200);
		
		response.setContentType("application/json");
		PrintWriter pw=response.getWriter();
		pw.println(mes);
		pw.close();		
		
	}
	
	private void connectUT(String param){
		String query=getQueryText(param);
		String response="";

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
				obj.put("DM",rs.getString("DM"));
				resJSON.add(obj);
			}
			StringWriter out = new StringWriter();
			JSONValue.writeJSONString(resJSON, out);
			response=out.toString();
		}
		// Handle any errors that may have occurred.//todo перенести в лог
		catch (NamingException e) {
			logMes("NamingException: "+e.toString());
			HTTPresult.put(500,"NamingException");
		}
		catch (SQLException e) {
			logMes("SQLException: "+e.toString());
			HTTPresult.put(500,"SQLException");
		}
		catch (IOException e) {
			logMes("IOException: "+e.toString());
			HTTPresult.put(500,"IOException");
		}		  

		HTTPresult.put(200,response);
		return response;
	}
	
	//записать сообщение в файл
	private void logMes(String mes){ //todo переделать на промышленное логирование //есть метод log в GeneralServlet
		logger.warning(mes);
	}
	private String getQueryText(String param){ //текст запроса к ценам товаров		
		String query="select "
						+"			_IDRRef nomenklref"
						+"			,_Fld4299 artikul"
						+"		into #nomenkl"
						+"		from _Reference169 nomenkl"
						+"		where _Fld4299 in ("+param+")"
						+"		select"
						+"			--nomenkl.nomenklref nomenklref"
						+"			--,statusDM._Fld25895RRef DMref"
						+"			nomenkl.artikul artikul"
						+"			,DM._Description DM"
						+"		from"
						+"			#nomenkl nomenkl"
						+"			join _InfoRg25894 statusDM on"
						+"				nomenkl.nomenklref=statusDM._Fld25920RRef"
						+"				and statusDM._Fld26017RRef=0xA825AC1F6B01E73D11E9676FEDC17C8E --'INTRODUCED', введен в оборот"
						+"			join _Reference25834 DM on"
						+"				statusDM._Fld25895RRef=DM._IDRRef"
						+"		drop table #nomenkl";
			
		return query;
	}
}