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

public class Movie implements HttpHandler, AutoCloseable
{
    private static Memory memory;
    Driver driver;
    
    public Movie(Memory mem, Driver drive) {
        memory = mem;
        driver = drive;
    }
    
    @Override
	public void close() throws Exception {
		driver.close();
	}

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r); 
            } else if (r.getRequestMethod().equals("PUT")) {		// /api/v1/AddMovie
            	handlePut(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

 public void handlePut(HttpExchange r) throws IOException, JSONException {
	 	String body = Utils.convert(r.getRequestBody());
	 	JSONObject deserialized;
	 	try {
	 		deserialized = new JSONObject(body);
	 	} catch (Exception e) {
	 		//Error parsing the JSON Message
	 		r.sendResponseHeaders(400, -1);
	 		return;
	 	}
	 	
	 	String name = memory.getValue();
        String Id = memory.getValue();
        
        if (deserialized.has("name"))
            name = deserialized.getString("name");
        else {
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        if (deserialized.has("movieId"))
            Id = deserialized.getString("movieId");
        else {
        	r.sendResponseHeaders(400, -1);
        	return;
        }        
	 	//create node 
        // Sessions are lightweight and disposable connection wrappers.
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{
        		StatementResult result = tx.run("MATCH (a:movie) WHERE a.id = $movieId RETURN a", parameters("movieId", Id));
        		//System.out.println(result.single().get( 0 ).asString());
        		//System.out.println(result.hasNext());
        		if(!result.hasNext()) {
        			// Wrapping Cypher in an explicit transaction provides atomicity
                    // and makes handling errors much easier
                    tx.run("CREATE (a:movie {Name: {x}, id: {y}})", parameters("x", name,"y", Id));
                    tx.success();  // Mark this write as successful.
        		} else {
        			//movie does exist
        			r.sendResponseHeaders(400, -1);
        			return;
        		}
        	}
        }catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	System.out.println(e.toString());
        	System.out.println("OOF");
        	return;
        }
        //check nodes with MATCH
        r.sendResponseHeaders(200, -1);
        return;
	}
public void handleGet(HttpExchange r) throws IOException, JSONException {
     String body = Utils.convert(r.getRequestBody());
	 	JSONObject deserialized;
	 	
	 	try {
	 		deserialized = new JSONObject(body);
	 	} catch (Exception e) {
	 		//Error parsing the JSON Message
	 		r.sendResponseHeaders(400, -1);
	 		return;
	 	}

     String Id = memory.getValue();
     StatementResult movie_name;
     StatementResult movie_actors;
     
     if (deserialized.has("movieId"))
         Id = deserialized.getString("movieId");
     else {
    	//no movieId in input
     	r.sendResponseHeaders(400, -1);
     	return;
     }
     
     try (Session session = driver.session())
     {	
     	try (Transaction tx = session.beginTransaction())
     	{	
     		movie_name = tx.run("MATCH (a:movie) WHERE a.id = $movieId RETURN a.Name", parameters("movieId", Id));
     		if(movie_name.hasNext()) { 
     			//movieId exists
     			//retrieve movies since we know actorID is in the database
     			movie_actors = tx.run("MATCH (:movie { id: {x} })--(actor) RETURN actor.id", parameters("x", Id));
     			tx.success();  // Mark this write as successful.
     		} else {
     			r.sendResponseHeaders(404, -1); //SEND 404 NOT FOUND IF NAME ISNT FOUND I.E NO movieId IN DB
     			return;
     		}
     	}
     }catch(Exception e) {
     	r.sendResponseHeaders(500, -1);
     	System.out.println(e.toString());
     	return;
     }
     
     String actors_list = "\n\t\t";
     //put list of movies into a long string
     List<Record> results = movie_actors.list(); // store .list() it makes it empty after using .list() once
     if (results.isEmpty()) 
    	 actors_list = "";
     else {
     	for (int i = 0; i < results.size(); i++) {
     		actors_list = actors_list + results.get( i ).get("actor.id");
     		if (i != results.size() -1)
     			actors_list += ",\n\t\t";
     	}
     	actors_list += "\n\t";
     }
     
     String response = "{\n\t" + 
     		"\"movieId\": " + "\"" + Id + "\",\n\t" +
     		"\"name\": " + "\"" + movie_name.single().get( 0 ).asString() + "\",\n\t" + 
     		"\"actors\": " + 
     			"[" + actors_list + "]"
     		+ "\n}";
     		
     r.sendResponseHeaders(200, response.length());
     OutputStream os = r.getResponseBody();
     os.write(response.getBytes());
     os.close();
     return;
}
}