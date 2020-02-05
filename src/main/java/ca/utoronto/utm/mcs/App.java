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
        //added
        Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "1234"));
        Session session = driver.session();
        Memory mem = new Memory();
        server.createContext("/api/v1/addActor", new AddActor(mem, driver));
        //server.createContext("/api/v1/addMovie", new AddMovie(mem));
        //server.createContext("/api/v1/addRelationship", new AddRelationship(mem));
        //server.createContext("/api/v1/getActor", new GetActor(mem));
        //server.createContext("/api/v1/getMovie", new GetMovie(mem));
        //server.createContext("/api/v1/hasRelationship", new HasRelationship(mem));
        //server.createContext("/api/v1/computeBaconNumber", new ComputeBaconNumber(mem));
        //server.createContext("/api/v1/computeBaconPath", new ComputeBaconPath(mem));
        //added
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
