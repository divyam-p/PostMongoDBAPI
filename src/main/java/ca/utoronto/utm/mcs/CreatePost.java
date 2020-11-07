package ca.utoronto.utm.mcs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import javax.inject.Inject;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class CreatePost implements HttpHandler {

  private MongoClient mongoClient;
  private MongoDatabase database;
  private MongoCollection<Document> collection;
  
  public CreatePost(MongoClient mongoC) {
    mongoClient = mongoC;
    database = mongoClient.getDatabase("csc301a2");
    collection = database.getCollection("posts");
  }


  // Not sure if this is right [Does not match TA's]
  
 
  public void handle(HttpExchange r) {
    try {
      
      if(r.getRequestMethod().equals("PUT")) {
        handlePut(r);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  
  
  public void handlePut(HttpExchange r) throws IOException, JSONException{
    
    String body = Utils.convert(r.getRequestBody());
    JSONObject deserialized = new JSONObject(body);
    
    String title ="";
    String author ="";
    String content ="";
    String[] tags = {};
    
    
    if(deserialized.has("title")) {
      title = deserialized.getString(("titel"));
    }
    
    if(deserialized.has("author")) {
      author = deserialized.getString(("author"));
    }
    if(deserialized.has("content")) {
      content = deserialized.getString(("content"));
    }
    if(deserialized.has("tags")) {
      tags = JSONObject.getNames("tags");
    }
    
    if(!deserialized.has("title") || !deserialized.has("author") || !deserialized.has("content") || !deserialized.has("content") || !deserialized.has("tags")) {
      r.sendResponseHeaders(400, -1);
    }
    else {
      Document doc = new Document()
          .append("title", title).append("author", author).append("content", content).append("tags", tags);
          
      collection.insertOne(doc);
      r.sendResponseHeaders(200, 7);
      
    }
    
   ;
    
    
    
    
  }
  
  
  
  
  
  
  
}
