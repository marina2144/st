
import  java.io.*; //вывод на страницу HTML и в файл todo убрать
import javax.servlet.*;
import javax.servlet.http.*;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import java.util.stream.Collectors;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.util.Iterator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.naming.NamingException;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


public class LabelData extends HttpServlet{
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		
		
		logMes("HI!!!");
		
		
		LocalDate date;
		try{
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			date=LocalDate.parse(request.getParameter("date"),dtf);
		}
		catch(Exception e){
			//todo добавить логирование ошибки
			response.sendError(400,"Bad date parameter");
			return;			
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
			//todo добавить логирование ошибки
			//param=e.toString();
			response.sendError(400,"Bad request body");
			return;
		}
		if (param.length()==0){
			//todo добавить логирование ошибки
			response.sendError(400,"Bad request body");
			return;
		}
		
		//получение данных SQL и формирование ответа
		String result=connectUT(param,date);//todo если параметр - ошибка, завершать
		
		//String result=connectUT();
		response.setContentType("application/json");
		PrintWriter pw=response.getWriter();
		pw.println(result);
		pw.close();
		
		//logMes(result);
		
		
	}
	
	//создает соединение с 1С УТ
	// 0-успешно
	// 1-ошибка
	private String connectUT(String param, LocalDate date){
		  String query=getQueryText(param, date);
		  
		  //if(query.length()>1)
		  //return query;	
		
		String result="";
		
		/*
		 // Create datasource.
      SQLServerDataSource ds = new SQLServerDataSource();
      ds.setUser("ut_read"); //todo перенести параметры подключения в файл настроек
      ds.setPassword("UT_Reader_123");
      ds.setServerName("hv03004");
      ds.setPortNumber(1433);
      ds.setDatabaseName("trade_2017_stockmann");
		
		*/
		  
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
				obj.put("price",rs.getString("price"));
				resJSON.add(obj);
			}
			StringWriter out = new StringWriter();
			JSONValue.writeJSONString(resJSON, out);
			result=out.toString();
	  }
	  // Handle any errors that may have occurred.//todo перенести в лог
	  catch (NamingException e) {
			result=e.toString();
			e.printStackTrace();			
      }
	  catch (SQLException e) {
			result=e.toString();
			e.printStackTrace();
	  }
	  catch (IOException e) {
			result=e.toString();
			e.printStackTrace();
	  }		  
	  
	  return result;
	}
	
	//записать сообщение в файл
	private void logMes(String mes){ //todo переделать на промышленное логирование //есть метод log в GeneralServlet
	
		// try(FileWriter writer = new FileWriter("C:\\JavaProjects\\otes3.txt", false))
        // {
        ////   запись всей строки
            // String text = mes;
            // writer.write(text);
            // writer.flush();
        // }
        // catch(IOException ex){
             
            // System.out.println(ex.getMessage());
        // } 
		
		log = LogFactory.getLog(getClass());
        log.info("Starting test case [   "+mes);
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