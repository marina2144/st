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
						 +" 	nomenkl._Fld4299 artikul "
						 +" from "
						 +" 	_Reference25834 DM "
						 +" join _InfoRg25894 statusDM on "
						 +" 	DM._IDRRef=statusDM._Fld25895RRef "
						 +" join "
						 +" 	_Reference169 nomenkl on "
						 +" 	statusDM._Fld25920RRef=nomenkl._IDRRef "
						 +" where DM._Description='010290000021830721IGJ6QmlMUsq5v' and statusDM._Fld26017RRef=0xA825AC1F6B01E73D11E9676FEDC17C8E";
		
		return query;
	}
}