package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import org.neo4j.driver.v1.*;

import static org.neo4j.driver.v1.Values.parameters;

public class App 
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "1234"));
        Session session = driver.session();
        Memory mem = new Memory();
        server.createContext("/api/v1/addActor", new Actor(mem, driver));
        server.createContext("/api/v1/addMovie", new Movie(mem, driver));
        server.createContext("/api/v1/addRelationship", new Relationship(mem, driver));
        server.createContext("/api/v1/getActor", new Actor(mem, driver));
        server.createContext("/api/v1/getMovie", new Movie(mem, driver));
        server.createContext("/api/v1/hasRelationship", new Relationship(mem, driver));
        server.createContext("/api/v1/computeBaconNumber", new BaconNumber(mem, driver));
        server.createContext("/api/v1/computeBaconPath", new BaconPath(mem, driver));
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
