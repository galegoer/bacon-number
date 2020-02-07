package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

//Neo4j Imports

import org.neo4j.driver.v1.*;

import static org.neo4j.driver.v1.Values.parameters;

public class BaconPath implements HttpHandler, AutoCloseable
{
    private static Memory memory;
    Driver driver;
    
    public BaconPath(Memory mem, Driver drive) {
        memory = mem;
        driver = drive;
    }
    
    @Override
	public void close() throws Exception {
		driver.close();
	}

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("GET")) 
                handleGet(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


public void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String Id = memory.getValue();
        long number = 0;
        String pathList = "";
        if (deserialized.has("actorId"))
            Id = deserialized.getString("actorId");
        else {
        	//improper request
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{	
        		StatementResult actor_name = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a.Name", parameters("actorId", Id));
        		if(Id.equals("nm0000102")) { //IT's kevin bacon
        			StatementResult kevMovies = tx.run("MATCH (:actor { id: {x} })--(movie) RETURN movie.id", parameters("x", Id));
  				  	pathList = "\n\t\t{\n\t\t\t"
  				  			+ "\"actorId\": \"nm0000102\",\n\t\t\t"
  				  			+ "\"movieId\": " + kevMovies.list().get(0).get("movie.id") + "\n\t\t}"; //just get the first one
        			String response = "{\n\t" + 
    	        					  "\"baconNumber\": \"" + number +
    	        					  "\"\n\t\"baconPath\":[\t" + pathList + 
    	        					  "\n\t]\n}";
        			
    	        	r.sendResponseHeaders(200, response.length()); //No path respond with 200 and undefined
    	        	OutputStream os = r.getResponseBody();
    	        	os.write(response.getBytes());
    	        	os.close();
    	        	return;
        		}
        		if(actor_name.hasNext()) { //actor_id exists
        			//retrieve movies since we know actorID is in the database
        			StatementResult shortestPath = tx.run("MATCH (a:actor),(b:actor)"
                            +"WHERE a.id = $actorId AND b.id = $kevinId "
                            +"MATCH p = shortestPath((a)-[*]-(b))"
                            +"UNWIND nodes(p) as n "
                            +"MATCH (n)--(c:movie)"
                            +"RETURN n.id, c.id", parameters("actorId", Id, "kevinId", "nm0000102"));;
        			tx.success();  // Mark this write as successful.        			
        			if (!shortestPath.hasNext()) {
        				//No Path to Bacon
        				String response = "{\n\t" + 
            	        		"\"baconNumber\": \"undefined\"\n\t" +
        	        			"\"baconPath\":[]\n}";
            			r.sendResponseHeaders(200, response.length()); //No path respond with 200 and undefined
            			OutputStream os = r.getResponseBody();
            	        os.write(response.getBytes());
            	        os.close();
            			return;
        			} else {
        				//PATH EXISTS TRAVERSE / PARSE IT
        				List<Record> results = shortestPath.list();
        		        if (results.isEmpty())   //MIGHT BE OPTIONAL SINCE PATH_LIST IS == "" ALREADY INTIALLY
        		        	pathList = "";
        		        else {
        		        	for (int i = 0; i < results.size(); i++) {
        		        		pathList += "\n\t\t{\n\t\t\t"
        		        				  + "\"actorId\": " + results.get( i ).get("n.id") + ",\n\t\t\t"
        		        				  + "\"movieId\": " + results.get( i ).get("c.id") + "\n\t\t";
        		        		
        		        		if (i != results.size() -1)  //adding comma after } if not the last record
        		        			pathList += "},";
        		        		else
        		        			pathList += "}\n";
        		        	}
        		        	
        		        }
        		        number = results.size()/2; //computes bacon number
        	        	String response = "{\n\t" + 
        	        					  "\"baconNumber\": \"" + number +
        	        					  "\"\n\t\"baconPath\":[\t" + pathList + 
        	        					  "\n\t]\n}";
        	        	r.sendResponseHeaders(200, response.length()); //No path respond with 200 and undefined
        	        	OutputStream os = r.getResponseBody();
        	        	os.write(response.getBytes());
        	        	os.close();
        	        	return;
        	        }
        			
        		} else {
        			//no actor
        			r.sendResponseHeaders(400, -1);
                	return;
        		}
        	}
        } catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	return;
        }
    }
}
