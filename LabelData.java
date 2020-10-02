import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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


public class LabelData extends HttpServlet{
	
	private static final Logger logger = Logger.getLogger("LocalData");	
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{

		HashMap<String,String> HTTPresult=new HashMap<>();
		HTTPresult.put("code","200");
		HTTPresult.put("message","");		

		LocalDate date=LocalDate.now();
		try{
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			date=LocalDate.parse(request.getParameter("date"),dtf);
		}
		catch(Exception e){
			logMes("Bad date parameter. "+e.toString());
			HTTPresult.put("code","400");
			HTTPresult.put("message","Bad date parameter");			
		}
		
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
			connectUT(param,date,HTTPresult);
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
	
	private void connectUT(String param, LocalDate date,  HashMap<String,String> HTTPresult){
		String query=getQueryText(param, date);		
		
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
			envContext  = (Context)initContext.lookup("java:/comp/env"); //todo описать
			ds = (DataSource)envContext.lookup("jdbc/UT");

			con = ds.getConnection();
			cstmt = con.prepareCall(query);
			rs = cstmt.executeQuery();

			while (rs.next()) {
				JSONObject obj=new JSONObject();
				obj.put("artikul",rs.getString("artikul"));
				obj.put("price",rs.getString("price"));
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
	
	private void logMes(String mes){ 
		logger.warning(mes);
	}
	private String getQueryText(String param, LocalDate date) //текст запроса к ценам товаров
	{
		LocalDate dateAdd2000=date.plusYears(2000); //в 1С сдвиг на 2000
		dateAdd2000=dateAdd2000.plusDays(1);
		
		String query="select"
						+"			_IDRRef nomenklref"
						+"			,_Fld4299 artikul"
						+"		into #nomenkl"
						+"		from _Reference169 nomenkl"
						+"		where _Fld4299 in ("+param+")"
						+"		select"
						+"			nomenkl.nomenklref nomenklref"
						+"			,pricelist._Fld19836RRef vid"
						+"			,max(pricelist._Period) period"
						+"		into #pricelistmax"
						+"		from "
						+"			#nomenkl nomenkl"
						+"		join _InfoRg19834 pricelist on "
						+"			nomenkl.nomenklref=pricelist._Fld19835RRef"
						+"			and pricelist._Fld19836RRef=0x80E700155D02DA0711E72054F4EA65E3"
						+"		where pricelist._Period<'"+dateAdd2000+"'"
						+"		group by "
						+"			nomenkl.nomenklref"
						+"			,pricelist._Fld19836RRef"

						+"		select "
						+"			pricelist._Fld19835RRef nomenklref"
						+"			,pricelist._Fld19838 price"
						+"			,pricelist._Fld19836RRef pricetype"
						+"			,vidprice._Description pricetypename"
						+"			,pricelist._Period period"
						+"		into #pricelist"
						+"		from "
						+"			#pricelistmax pricelistmax"
						+"		join _InfoRg19834 pricelist on "
						+"			(pricelistmax.nomenklref=pricelist._Fld19835RRef"
						+"			and pricelistmax.vid=pricelist._Fld19836RRef"
						+"			and pricelistmax.period=pricelist._Period)"
						+"		join _Reference77 vidprice on"
						+"			pricelist._Fld19836RRef=vidprice._IDRRef"
						+"		where _IDRRef=0x80E700155D02DA0711E72054F4EA65E3"

						+"		select"
						+"			nomenkl.nomenklref"
						+"			,nomenkl.artikul"
						+"			,pricelist.price"
						+"			,pricelist.pricetype"
						+"			,pricelist.pricetypename"
						+"		 from #nomenkl nomenkl"
						+"		 left join #pricelist pricelist on "
						+"			nomenkl.nomenklref=pricelist.nomenklref"

						+"		drop table #nomenkl"
						+"		drop table #pricelistmax"
						+"		drop table #pricelist";
		return query;
	}
}