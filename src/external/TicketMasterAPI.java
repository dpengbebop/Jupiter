package external;
//external api related
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String API_KEY = "3QWKRFaNzyhXtqi0jUdlmdPn5EmA2sbr";
	
	public List<Item> search(double lat, double lon, String keyword) {
		//Encode keyword in url since it may contain special characters
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		try {
			keyword = java.net.URLEncoder.encode(keyword, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//Convert lat/lon to geoHash
		String geoHash = GeoHash.encodeGeohash(lat, lon, 8); 
		
		//Make the url query part into right format with parameters
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, keyword, 50);
		try {
			//Open a HTTP connection between my java application and TicketMaster based on url
			HttpURLConnection connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
			//Set request method to GET
			connection.setRequestMethod("GET");
			//Send request to TicketMaster and get response, response code could be returned directly
			//Response body is saved in InputStream of connection
			
			int responseCode = connection.getResponseCode();
			System.out.println("\nSendling 'Get' request to URL: " + URL + "?" + query);
			System.out.println("Response Code: " + responseCode);
			
			//Now read response body to get events data
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			JSONObject obj = new JSONObject(response.toString());
			if (obj.isNull("_embedded")) {
				return new ArrayList<>();
			}
			JSONObject embedded = obj.getJSONObject("_embedded");
			JSONArray events = embedded.getJSONArray("events");
			return getItemList(events);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new ArrayList<>();
	}
	
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(41.836081, -87.639100);
	}


	
	private void queryAPI(double lat, double lon) {
		List<Item> ItemList = search(lat, lon, null);
		try {
			for (Item item : ItemList) {
				JSONObject jsonObject = item.toJSONObject();
				System.out.println(jsonObject);
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Helper methods
	 */

	//  {
	//    "name": "laioffer",
              //    "id": "12345",
              //    "url": "www.laioffer.com",
	//    ...
	//    "_embedded": { //object
	//	    "venues": [//json array, each JSONObject with 2 keys
	//	        {
	//		        "address": {
	//		           "line1": "101 First St,",
	//		           "line2": "Suite 101",
	//		           "line3": "...",
	//		        },
	//		        "city": {
	//		        	"name": "San Francisco"
	//		        }
	//		        ...
	//	        },
	//	        ...
	//	    ]
	//    }
	//    ...
	//  }
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
	//needs all kinds of categories this time
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
				JSONObject event = events.getJSONObject(i); //everytime get a new index get it from the JSONobject
				//tickerMaster get activity in the "event"
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
				Item item = builder.build();
				itemList.add(item);
			}
			
			return itemList;
		}

		
	

}
