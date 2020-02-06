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
            }// else if (r.getRequestMethod().equals("POST")) {
            //    handlePost(r);
            //} 
            else if (r.getRequestMethod().equals("PUT")) {		// /api/v1/AddActor
            	handlePut(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

 public void handlePut(HttpExchange r) throws IOException, JSONException {
	 	String body = Utils.convert(r.getRequestBody());
	 	JSONObject deserialized = new JSONObject(body);
	 	//driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "1234"));
	 	//driver = GraphDatabase.driver("http://localhost:7474", AuthTokens.basic("neo4j", "1234"));
	 	
	 	String name = memory.getValue();
        String Id = memory.getValue();
        
        if (deserialized.has("name"))
            name = deserialized.getString("name");
        else {
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        if (deserialized.has("actorId"))
            Id = deserialized.getString("actorId");
        else {
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        //NEED TO RETURN 400 if there is garbage after the actorid
        
	 	//create node 
        // Sessions are lightweight and disposable connection wrappers.
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{
        		StatementResult result = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a", parameters("actorId", Id));
        		//System.out.println(result.single().get( 0 ).asString());
        		System.out.println(result.hasNext());
        		if(!result.hasNext()) {
        			// Wrapping Cypher in an explicit transaction provides atomicity
                    // and makes handling errors much easier
                    tx.run("CREATE (a:actor {Name: {x}, id: {y}})", parameters("x", name,"y", Id));
                    tx.success();  // Mark this write as successful.
        		} else {
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
        //r.sendResponseHeaders(500, -1);
	}

public void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String Id = memory.getValue();
        StatementResult actor_name;
        StatementResult actor_movies;
        
        if (deserialized.has("actorId"))
            Id = deserialized.getString("actorId");
        else {
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        //NEED TO RETURN 400 if there is garbage after the actorid
        //NEED TO RETURN 400 FOR BAD FORMAT I.E DUPLICATE KEYS
        
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{	
        		actor_name = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a.Name", parameters("actorId", Id));
        		//System.out.println(result.hasNext()); 
        		if(actor_name.hasNext()) { //actor_id exists
        			//retrieve movies since we know actorID is in the database
        			//actor_movies = tx.run("MATCH (actor { actorId: {x} })<-[:ACTED_BY]-(movie) RETURN movie.Name", parameters("x", Id));
        			actor_movies = tx.run("MATCH (:actor { id: {x} })--(movie) RETURN movie.Name", parameters("x", Id));
        			System.out.println(actor_movies.list());
        			//if () --if movies list isnt empty fill movies_list
        			tx.success();  // Mark this write as successful.
        		} else {
        			r.sendResponseHeaders(404, -1); //SEND 404 NOT FOUND IF NAME ISNT FOUND I.E NO ACTORID IN DB
        			return;
        		}
        	}
        }catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	System.out.println(e.toString());
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
/*
    public void handlePost(HttpExchange r) throws IOException, JSONException{
        /* TODO: Implement this.
           Hint: This is very very similar to the get just make sure to save
                 your result in memory instead of returning a value.
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        long first = memory.getValue();
        long second = memory.getValue();

        if (deserialized.has("firstNumber"))
            first = deserialized.getLong("firstNumber");

        if (deserialized.has("secondNumber"))
            second = deserialized.getLong("secondNumber");

        /* TODO: Implement the math logic 
        long answer = first + second;
        memory.setValue(answer);

        r.sendResponseHeaders(200, -1);
    }
    */

	
}