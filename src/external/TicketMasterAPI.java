package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import com.sun.tools.javac.util.List;

import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String API_KEY = "VhlYH024Vnve5GigAk8KMnHBtPGfhc6V";
	
	public List<Item> search(double lat, double lon, String keyword) {
	    // Encode keyword in url since it may contain special characters
		if(keyword == null) {
	    	keyword = DEFAULT_KEYWORD;
	    }
	    try {
	    	keyword = java.net.URLEncoder.encode(keyword, "UTF-8"); //Suya Yin => Suya20%Yin
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
	    
	    //Convert lat, lon to geoHash
	    String geoHash = GeoHash.encodeGeohash(lat, lon, 9);
	    
	 // Make your url query part like: "apikey=12345&geoPoint=abcd&keyword=music&radius=50"   %s表示字符串类型
	 	String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, keyword, "50");
	 	// Open a HTTP connection between your Java application and TicketMaster based on url
	    try {
	    	HttpURLConnection connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection(); //强制类型转换
	    	// Set requrest method to GET
	    	connection.setRequestMethod("GET");
	    	
	    	// Send request to TicketMaster and get response, response code could be returned directly 
	    	//一个函数完成两个操作：先发送请求，再得到response code
	    	// response body is saved in InputStream of connection.
	    	int responseCode = connection.getResponseCode();
	    	System.out.println("Response Code:"+ responseCode);  //判断返回结果是不是200(成功),若不是，则代表这行前面有错
	    	
	    	
	    	// Now read response body to get events data
	    	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream())); //客户端的输入对应服务器端的输出
	    	//通过connection.getInputStream()得到服务器端返回的结果(response)，再通过BufferReader一行行的读出来
	    	//使用BufferReader来减少系统负担，而不是直接一下子都读出来，提高效率
	    	String inputLine;
	    	StringBuilder response = new StringBuilder();
	    	while ((inputLine = in.readLine()) != null) {
	    		response.append(inputLine);
	    	}
	    	in.close();
	    	JSONObject obj = new JSONObject(response.toString());   //把resopnse string变成JSON object
	    	if (obj.isNull("_embedded")) {   //判断object"_embeded"的key是不是存在
	    		return new ArrayList<>();
	    	}
	    	JSONObject embedded = obj.getJSONObject("_embedded");
	    	JSONArray events = embedded.getJSONArray("events");
	    	return getItemList(events);    	
	    }catch(Exception e) {
	    	e.printStackTrace();
	    }
	    return new ArrayList<>();
	}

	/**
	 * Helper methods
	 */	
	private String getAddress(JSONObject event) throws JSONException {
		if (!event.isNull("_embedded")) {
			JSONObject embedded = event.getJSONObject("_embedded");
			
			if (!embedded.isNull("venues")) {
				JSONArray venues = embedded.getJSONArray("venues");
				for (int i = 0; i < venues.length(); ++i) {
					JSONObject venue = venues.getJSONObject(i);
					
					StringBuilder sb = new StringBuilder();
					
					if (!venue.isNull("address")) {
						JSONObject address = venue.getJSONObject("address");
						if (!address.isNull("line1")) {
							sb.append(address.getString("line1"));
						}
						if (!address.isNull("line2")) {
							sb.append(address.getString("line2"));
						}
						if (!address.isNull("line3")) {
							sb.append(address.getString("line3"));
						}
						sb.append(",");
					}
					
					if (!venue.isNull("city")) {
						JSONObject city = venue.getJSONObject("city");
						if (!city.isNull("name")) {
							sb.append(city.getString("name"));
						}
					}
					
					if (!sb.toString().equals("")) {
						return sb.toString();
					}
				}
			}
		}
		
		return "";
	}


	// {"images": [{"url": "www.example.com/my_image.jpg"}, ...]}
	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull("images")) {
			JSONArray array = event.getJSONArray("images");
			for (int i = 0; i < array.length(); i++) {
				JSONObject image = array.getJSONObject(i);
				if (!image.isNull("url")) {
					return image.getString("url");
				}
			}
		}
		return "";
	}

	// {"classifications" : [{"segment": {"name": "music"}}, ...]}
	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		if (!event.isNull("classifications")) {
			JSONArray classifications = event.getJSONArray("classifications");
			for (int i = 0; i < classifications.length(); i++) {
				JSONObject classification = classifications.getJSONObject(i);
				if (!classification.isNull("segment")) {
					JSONObject segment = classification.getJSONObject("segment");
					if (!segment.isNull("name")) {
						String name = segment.getString("name");
						categories.add(name);
					}
				}
			}
		}
		return categories;
	}

	// Convert JSONArray to a list of item objects.
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();
		
		for (int i = 0; i < events.length(); ++i) {
			JSONObject event = events.getJSONObject(i);
			
			ItemBuilder builder = new ItemBuilder();
			
			if (!event.isNull("name")) {
				builder.setName(event.getString("name"));
			}
			if (!event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			if (!event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			if (!event.isNull("rating")) {
				builder.setRating(event.getDouble("rating"));
			}
			if (!event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			
			builder.setCategories(getCategories(event));
			builder.setAddress(getAddress(event));
			builder.setImageUrl(getImageUrl(event));
			
			itemList.add(builder.build());
		}
		
		return itemList;
	}

	//add a print function to show JSON array returned from TicketMaster for debugging.
	//print the result to console
	private void queryAPI(double lat, double lon) {
		List<Item> itemList = search(lat, lon, null);
		try {
			for (Item item : itemList) {
				JSONObject jsonObject = item.toJSONObject();
				System.out.println(jsonObject);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//In addition, since TicketMaster asked to use GeoPoint instead of latitude and longitude directly in request, 
	//we need a helper class to do GeoHash encoding for us.  => GeoHash.java
	
	/**
	 * Main entry for sample TicketMaster API requests.
	 */
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(29.682684, -95.295410);
	}	
	
}
