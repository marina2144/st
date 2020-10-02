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

import java.util.HashMap;


public class CheckDataMatrix extends HttpServlet{
	
	private static final Logger logger = Logger.getLogger("CheckDataMatrix");
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
			
		//извлекаем DataMatrix из тела запроса, параметр DM, ожидается 31 символ кода, закодированные в Base64
		String param="";
	
		HashMap<String,String> HTTPresult=new HashMap<>();
		HTTPresult.put("code","200");
		HTTPresult.put("message","");
			
		String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		try{
			JSONObject JSONobj = (JSONObject)  new JSONParser().parse(body);  
			param = JSONobj.get("DM").toString();
		}
		catch (ParseException e){
			logMes("ParseException. Bad request body. Exception: "+e.toString());
			HTTPresult.put("code","400");
			HTTPresult.put("message","Bad request body");
		}
		catch (NullPointerException e){
			logMes("NullPointerException. Bad request body. Exception: "+e.toString());
			HTTPresult.put("code","400");
			HTTPresult.put("message","Bad request body");			
		}

		if (HTTPresult.get("code")=="200" && param.length()==0){
			logMes("Bad request: Empty body");
			HTTPresult.put("code","400");
			HTTPresult.put("message","Empty body");
		}
		
		//получение данных SQL и формирование ответа
		if (HTTPresult.get("code")=="200"){
			connectUT(param,HTTPresult);
		}
		
		if(HTTPresult.get("code")!="200"){
			response.sendError(Integer.valueOf(HTTPresult.get("code")),HTTPresult.get("message"));
			return;
						
		}
		else{
			response.setContentType("application/json");
			PrintWriter pw=response.getWriter();
			pw.println(HTTPresult.get("message"));
			pw.close();		
		}
	}
	
	private void connectUT(String param, HashMap<String,String> HTTPresult){
		String query=getQueryText(param);

		Context initContext;
		Context envContext;
		DataSource ds;
		Connection con;
		CallableStatement cstmt;
		ResultSet rs;
		
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
			}
			else{
				obj.put("artikul",null);
			}
			StringWriter out = new StringWriter();
			JSONValue.writeJSONString(obj, out);
			HTTPresult.put("message",out.toString());
			
			rs.close();
			cstmt.close();
			
		}
		// Handle any errors that may have occurred.
		catch (NamingException e) {
			logMes("NamingException param = "+param+" : "+e.toString());
			HTTPresult.put("code","500");
			HTTPresult.put("message","NamingException");				
		}
		catch (SQLException e) {
			logMes("SQLException param = "+param+" : "+e.toString());
			HTTPresult.put("code","500");
			HTTPresult.put("message","SQLException");			
		}
		catch (IOException e) {
			logMes("IOException param = "+param+" : "+e.toString());
			HTTPresult.put("code","500");
			HTTPresult.put("message","IOException");	
		}		  

	}
	
	//записать сообщение в лог
	private void logMes(String mes){ 
		logger.warning(mes);
	}
	private String getQueryText(String param){ 		
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
						 
		//logMes(query);
		
		return query;
	}
}