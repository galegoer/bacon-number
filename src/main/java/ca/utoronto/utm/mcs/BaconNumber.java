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

public class BaconNumber implements HttpHandler, AutoCloseable
{
    private static Memory memory;
    Driver driver;
    
    public BaconNumber(Memory mem, Driver drive) {
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
        
        if (deserialized.has("actorId"))
            Id = deserialized.getString("actorId");
        else {
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{	
        		StatementResult actor_name = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a.Name", parameters("actorId", Id));
        		//System.out.println(result.hasNext()); 
        		if(actor_name.hasNext()) { //actor_id exists
        			//retrieve movies since we know actorID is in the database
        			//StatementResult actor_movies = tx.run("MATCH (:actor { id: {x} })--(movie) RETURN movie.id", parameters("x", Id));
        			
        			StatementResult shortest_path = tx.run("MATCH (a:actor),(b:actor)"
        											+"WHERE a.id = $actorId AND b.id = $kevinId "
        											+"MATCH p = shortestPath((a)-[*]-(b))"
        											+"RETURN p", parameters("actorId", Id, "kevinId", "nm0000102"));
        			//System.out.println(shortest_path.list().size());
        			//Shortest path lists out every step can divide by 2 because each connection is a pair
        			number = shortest_path.list().toString().split(",").length / 2;
        			tx.success();  // Mark this write as successful.
        		} else {
        			String response = "{\n\t" + 
        	        		"\"baconNumber\": " + "\" undefined"
        	        		+ "\"\n}";
        			r.sendResponseHeaders(200, response.length()); //No path respond with 200 and undefined
        			OutputStream os = r.getResponseBody();
        	        os.write(response.getBytes());
        	        os.close();
        			return;
        		}
        	}
        }catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	System.out.println(e.toString());
        	return;
        }
        String response = "{\n\t" + 
        		"\"baconNumber\": " + "\"" + number
        		+ "\"\n}";
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;

    }
}
