package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

//Neo4j Imports

import org.neo4j.driver.v1.*;

import static org.neo4j.driver.v1.Values.parameters;

public class Actor implements HttpHandler, AutoCloseable
{
    private static Memory memory;
    Driver driver;
    
    public Actor(Memory mem, Driver drive) {
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
            } else if (r.getRequestMethod().equals("PUT")) {
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
        //Can have more names so only check for name and actorId
        if(deserialized.has("actorId") && deserialized.has("name")) {
        	name = deserialized.getString("name");
        	Id = deserialized.getString("actorId");
        } else {
        	//missing actorId and or name
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        // Sessions are lightweight and disposable connection wrappers.
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{
        		StatementResult result = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a", parameters("actorId", Id));
        		if(!result.hasNext()) {
        			//add actor because it doesn't exist
                    tx.run("CREATE (a:actor {Name: {x}, id: {y}})", parameters("x", name,"y", Id));
                    tx.success();  // Mark this write as successful.
                    r.sendResponseHeaders(200, -1);
                    return;
        		} else {
        			//actor does exist
        			r.sendResponseHeaders(400, -1);
        			return;
        		}
        	}
        } catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	return;
        }
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
        StatementResult actor_name;
        StatementResult actor_movies;
        
        if (deserialized.has("actorId"))
            Id = deserialized.getString("actorId");
        else {
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        //NEED TO RETURN 400 FOR BAD FORMAT I.E DUPLICATE KEYS
        
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{	
        		actor_name = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a.Name", parameters("actorId", Id)); 
        		if(actor_name.hasNext()) { //actor_id exists
        			//retrieve movies since we know actorID is in the database
        			//actor_movies = tx.run("MATCH (actor { actorId: {x} })<-[:ACTED_BY]-(movie) RETURN movie.Name", parameters("x", Id));
        			actor_movies = tx.run("MATCH (:actor { id: {x} })--(movie) RETURN movie.Name", parameters("x", Id));
        			//if () --if movies list isnt empty fill movies_list
        			tx.success();  // Mark this write as successful.
        		} else {
        			r.sendResponseHeaders(404, -1); //SEND 404 NOT FOUND IF NAME ISNT FOUND I.E NO ACTORID IN DB
        			return;
        		}
        	}
        }catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	return;
        }
        
        String movies_list = "\n\t\t";
        //put list of movies into a long string
        if (!actor_movies.hasNext())
        	movies_list = "";
        else {
        	for (int i = 0; i < actor_movies.list().size(); i++) {
        		movies_list = movies_list + "\"" + actor_movies.list().get( i ).toString() + "\",\n";
        	}
        	movies_list += "\n\t";
        }
        
        String response = "{\n\t" + 
        		"\"actorId\": " + "\"" + Id + "\"\n\t" +
        		"\"name\": " + "\"" + actor_name.single().get( 0 ).asString() + "\"\n\t" + 
        		"\"movies\": " + 
        			"[" + movies_list + "]"
        		+ "\n}";  //change [] to actual list, only empty if list is actually empty 
        		
        //String response_Id = Id + "\n";
        //String response_name = actor_name + "\n";
        //String response_movies = actor_movies + "\n";
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;

    }
}