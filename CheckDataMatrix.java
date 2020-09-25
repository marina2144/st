import java.io.*;

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


public class CheckDataMatrix extends HttpServlet{
	
	private static final Logger logger = Logger.getLogger("DataMatrix");
	
	private int HTTPstatus=200;
	private String message="";
	
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
			HTTPstatus=400;
			message="Bad request body";
		}
		if (param.length()==0){
			logMes("Bad request: Empty body");
			HTTPstatus=400;
			message="Empty body";			
		}
		
		if(HTTPstatus!=200){
			response.sendError(400,message);
			return;
		}
		
		//получение данных SQL и формирование ответа
		connectUT(param);
		
		if(HTTPstatus!=200){
			response.sendError(400,message);
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
		String query="select "
						+"	_Reference169._Fld4299 artikul "
						+"from "
						+"	_Reference25834 "
						+"left join _InfoRg25894 _InfoRg25894 on "
						+"	_Reference25834._IDRRef=_InfoRg25894._Fld25895RRef and _InfoRg25894._Fld26017RRef=0xA825AC1F6B01E73D11E9676FEDC17C8E --'INTRODUCED', введен в оборот "
						+"left join "
						+"	_Reference169 on "
						+"	_Reference169._IDRRef=_InfoRg25894._Fld25920RRef "
						+"where _Reference25834._Description='010290000021830721IGJ6QmlMUsq5v'";
		
		return query;
	}
}