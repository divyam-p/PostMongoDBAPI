package ca.utoronto.utm.mcs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import javax.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Post implements HttpHandler {

  private MongoClient mongoClient;
  private MongoDatabase database;
  private MongoCollection<Document> collection;
  
  public Post(MongoClient mongoC) {
    mongoClient = mongoC;
    database = mongoClient.getDatabase("csc301a2");
    collection = database.getCollection("posts");
  }

  public void handle(HttpExchange r) throws IOException {
    try {
      
      if(r.getRequestMethod().equals("PUT")) {
        handlePut(r);
      }
      else if(r.getRequestMethod().equals("GET")) {
        handleGet(r);
      }
      else if(r.getRequestMethod().equals("DELETE")) {
        handleDelete(r);
      }
      else {
        r.sendResponseHeaders(405, -1);
      }
    } catch (Exception e) {
      r.sendResponseHeaders(500, -1);
      e.printStackTrace();
    }
  }
  
  
  public boolean checkDup(String title, String author, String content, ArrayList<String> tags){
    
    FindIterable<Document> documents = collection.find(new Document().append("title", title).append("author", author).append("content", content).append("tags", tags));
    if(documents.first() == null) {
      return false;
    }
    return true;
  
  }
  public void handlePut(HttpExchange r) throws IOException, JSONException{
    
    String body = Utils.convert(r.getRequestBody());
    JSONObject deserialized = new JSONObject(body);
    String title ="";
    String author ="";
    String content ="";
    JSONArray temp;
    ArrayList<String> tags = new ArrayList<String>();
    if(deserialized.has("title")) {
      title = deserialized.getString(("title"));
    }
    
    if(deserialized.has("author")) {
      author = deserialized.getString(("author"));
    }
    if(deserialized.has("content")) {
      content = deserialized.getString(("content"));
    }
    if(deserialized.has("tags")) {
      temp = deserialized.getJSONArray("tags");
      for(int i = 0; i < temp.length(); i++) {
        tags.add( temp.get(i).toString() );
      }
    } 
    if(!deserialized.has("tags") || !deserialized.has("title") || !deserialized.has("author") || !deserialized.has("content")) {
      r.sendResponseHeaders(400, -1);
    }
    else if(checkDup(title, author, content, tags)) {
      r.sendResponseHeaders(400, -1);
    }
    else {
      Document doc = new Document()
          .append("title", title).append("author", author).append("content", content).append("tags", tags);
      
      collection.insertOne(doc);
      
      ObjectId id = (ObjectId) doc.get("_id");
      
      JSONObject response = new JSONObject();
      response.put("_id", id);
      
      r.sendResponseHeaders(200, response.toString().length());
      OutputStream os = r.getResponseBody();
      os.write((response.toString()).getBytes());
      os.close();
    }

  }
  
  public void handleGet(HttpExchange r) throws IOException, JSONException{
    
    String body = Utils.convert(r.getRequestBody());
    JSONObject deserialized = new JSONObject(body);
    ArrayList<String> tracker = new ArrayList<>();
    String id = "";
    String title = "";
    ObjectId temp = null; 
    
    if(deserialized.has("title")) {
      title = deserialized.getString(("title"));
    }
    if(deserialized.has("_id")) {
      id = deserialized.getString("_id");
      try { 
        //What if an Id is given that cannot be found in the database, but the title exists then what do we do? 
        //Right now it would give a 404 not found error
        
        temp = new ObjectId(id);

        
      } 
      catch(Exception e) { 
        r.sendResponseHeaders(400, -1);
      }
    }
    
    if(!deserialized.has("_id") && !deserialized.has("title")) {
      r.sendResponseHeaders(400, -1);
    }
    if(deserialized.has("_id")) {
      FindIterable<Document> documents = collection.find(new Document().append("_id", temp));
      
      if(documents.first() == null) {
        r.sendResponseHeaders(404, -1);
      }

      Document newDoc = documents.first();
      tracker.add(newDoc.toJson());
   
      r.sendResponseHeaders(200, tracker.toString().length());
      OutputStream os = r.getResponseBody();
      os.write((tracker.toString()).getBytes());
      os.close();  
    }
    if(deserialized.has("title")) {
      FindIterable<Document> documents = collection.find().sort(new Document().append("title", 1));
      
      if(documents.first() == null) {
        r.sendResponseHeaders(404, -1);
      }
      
      for(Document doc : documents) {
        if(doc.get("title").toString().contains(title) && doc.get("_id") != (temp)){

          tracker.add(doc.toJson()); 
        }
      }
      if(tracker.isEmpty()) {
        r.sendResponseHeaders(404, -1);
      }
      r.sendResponseHeaders(200, tracker.toString().length());
      OutputStream os = r.getResponseBody();
      os.write((tracker.toString()).getBytes());
      os.close();    
    }  
  }
 
  public void handleDelete(HttpExchange r) throws IOException, JSONException{
    String body = Utils.convert(r.getRequestBody());
    JSONObject deserialized = new JSONObject(body);
    
    String id = "";
    ObjectId temp = null; 
    
    if(deserialized.has("_id")) {
      id = deserialized.getString("_id");
    }
    
    if(!deserialized.has("_id")) {
      r.sendResponseHeaders(400, -1);
    }
    else {
      try { 
        temp = new ObjectId(id);
      }
      catch(Exception e) { 
        r.sendResponseHeaders(400, -1);
      }
      Document random = collection.findOneAndDelete(new Document().append("_id", temp));
      if(random == null) {
        r.sendResponseHeaders(404, -1);
      }
      r.sendResponseHeaders(200, -1);
    }
  }
}
