package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

//Neo4j Imports

import org.neo4j.driver.v1.*;

import static org.neo4j.driver.v1.Values.parameters;

public class AddActor implements HttpHandler, AutoCloseable
{
    private static Memory memory;
    Driver driver;
    
    public AddActor(Memory mem, Driver drive) {
        memory = mem;
        driver = drive;
    }
    
    @Override
	public void close() throws Exception {
		driver.close();
		
	}

    public void handle(HttpExchange r) {
        try {
            //if (r.getRequestMethod().equals("GET")) {
            //    handleGet(r);
            //} else if (r.getRequestMethod().equals("POST")) {
            //    handlePost(r);
            //} 
        	if (r.getRequestMethod().equals("PUT")) {		// /api/v1/AddActor
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
        		StatementResult result = tx.run("MATCH ($x) RETURN x", parameters("x", Id));
        		System.out.println(result.single().get( 0 ).asString());
        	}
            // Wrapping Cypher in an explicit transaction provides atomicity
            // and makes handling errors much easier.
            try (Transaction tx = session.beginTransaction())
            {
                tx.run("CREATE (a:Actor {name: {x}, actorId: {y}})", parameters("x", name,"y", Id));
                tx.success();  // Mark this write as successful.
            }catch(Exception e) {
            	r.sendResponseHeaders(500, -1);
            	System.out.println("MEH");
            	return;
            }
        }catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	System.out.println("OOF");
        	return;
        }
        //check nodes with MATCH
        r.sendResponseHeaders(200, -1);
        return;
        //r.sendResponseHeaders(500, -1);
	}
/*
 public void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        long first = memory.getValue();
        long second = memory.getValue();

        if (deserialized.has("firstNumber"))
            first = deserialized.getLong("firstNumber");

        if (deserialized.has("secondNumber"))
            second = deserialized.getLong("secondNumber");

        /* TODO: Implement the math logic//
        long answer = first + second;
		System.out.println(first+","+second+","+answer);
        String response = Long.toString(answer) + "\n";
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

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