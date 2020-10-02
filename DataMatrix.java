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


public class DataMatrix extends HttpServlet{
	
	private static final Logger logger = Logger.getLogger("DataMatrix");
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
						
		//извлекаем список артикулов из тела запроса
		String param="";
		
		HashMap<String,String> HTTPresult=new HashMap<>();
		HTTPresult.put("code","200");
		HTTPresult.put("message","");
		
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
		
		JSONArray resJSON = new JSONArray();
		
		boolean hasData=false;
		
		try 
		{

			initContext = new InitialContext();
			envContext  = (Context)initContext.lookup("java:/comp/env");
			ds = (DataSource)envContext.lookup("jdbc/UT");

			con = ds.getConnection();
			cstmt = con.prepareCall(query);

			rs = cstmt.executeQuery();
			
			while (rs.next()) {
				JSONObject obj=new JSONObject();
				obj.put("artikul",rs.getString("artikul"));
				obj.put("DMbase64",rs.getString("DMbase64"));
				resJSON.add(obj);
				hasData=true;
			}
			
			StringWriter out = new StringWriter();
			if (hasData==true){
				JSONValue.writeJSONString(resJSON, out);
			}
			else{
				JSONObject obj=new JSONObject();
				obj.put("result",null);
				JSONValue.writeJSONString(obj, out);
			}
			HTTPresult.put("message",out.toString());
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
	
	//записать сообщение в файл
	private void logMes(String mes){ 
		logger.warning(mes);
	}
	private String getQueryText(String param){ 	
		String query="select _IDRRef nomenklref ,_Fld4299 artikul "
						+"into #nomenkl "
						+"from _Reference169 nomenkl "
						+"where _Fld4299 in ("+param+") "
 
						+"select nomenkl.artikul artikul ,DM._Description DM "
						+",cast(DM._Description as varbinary(max)) bin "
						+"into #restable "
						+"from #nomenkl nomenkl "
						+"join _InfoRg25894 statusDM on nomenkl.nomenklref=statusDM._Fld25920RRef "
						+"join _Reference25834 DM on statusDM._Fld25895RRef=DM._IDRRef "
						+"where statusDM._Fld26017RRef=0xA825AC1F6B01E73D11E9676FEDC17C8E "
						+"select artikul, DM, "
						+"cast(N'' as xml).value('xs:base64Binary(xs:hexBinary(sql:column(\"bin\")))', 'varchar(max)') DMbase64 from #restable restable "
						+"drop table #nomenkl "
						+"drop table #restable";
						
		
		logMes(query);
			
		return query;
	}
}