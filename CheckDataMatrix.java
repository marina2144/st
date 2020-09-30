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
			response.sendError(HTTPstatus,message);
			return;
		}
		
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

		Context initContext;
		Context envContext;
		DataSource ds;
		Connection con;
		CallableStatement cstmt;
		ResultSet rs;
		
		JSONArray resJSON = new JSONArray();
		
		try 
		{
			initContext = new InitialContext();
			envContext  = (Context)initContext.lookup("java:/comp/env");
			ds = (DataSource)envContext.lookup("jdbc/UT");

			con = ds.getConnection();
			cstmt = con.prepareCall(query);

			rs = cstmt.executeQuery();
			
			JSONObject obj=new JSONObject();
			if (rs.next()) {
				obj.put("artikul",rs.getString("artikul"));
				resJSON.add(obj);
			}
			else{
				obj.put("artikul",null);
				resJSON.add(obj);
			}
			StringWriter out = new StringWriter();
			JSONValue.writeJSONString(resJSON, out);
			message=out.toString();
			
			rs.close();
			cstmt.close();
			
		}
		// Handle any errors that may have occurred.
		catch (NamingException e) {
			logMes("NamingException param = "+param+" : "+e.toString());
			HTTPstatus=500;
			message="NamingException";				
		}
		catch (SQLException e) {
			logMes("SQLException param = "+param+" : "+e.toString());
			HTTPstatus=500;
			message="SQLException";			
		}
		catch (IOException e) {
			logMes("IOException param = "+param+" : "+e.toString());
			HTTPstatus=500;
			message="IOException";	
		}		  

	}
	
	//записать сообщение в файл
	private void logMes(String mes){ //todo переделать на промышленное логирование //есть метод log в GeneralServlet
		logger.warning(mes);
	}
	private String getQueryText(String param){ //текст запроса к ценам товаров		
		 String query="declare @encoded varchar(max), @decoded varchar(max) "
							+"set @encoded = '"+param+"' "
							+"set @decoded = convert(varchar(max), cast('' as xml).value('xs:base64Binary(sql:variable(\"@encoded\"))', 'varbinary(max)')) "
						 +" select "
						 +" 	nomenkl._Fld4299 artikul "
						 +" from "
						 +" 	_Reference25834 DM "
						 +" join _InfoRg25894 statusDM on "
						 +" 	DM._IDRRef=statusDM._Fld25895RRef "
						 +" join "
						 +" 	_Reference169 nomenkl on "
						 +" 	statusDM._Fld25920RRef=nomenkl._IDRRef "
						 +" where DM._Description=@decoded and statusDM._Fld26017RRef=0xA825AC1F6B01E73D11E9676FEDC17C8E --'INTRODUCED', введен в оборот";
						 
		logMes(query);
		
		return query;
	}
}