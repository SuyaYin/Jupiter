package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;

// Recommendation based on geo distance and similar categories.
public class GeoRecommendation {

	public List<Item> recommendItems(String userId, double lat, double lon) {
		List<Item> recommendedItems = new ArrayList<>();

		DBConnection conn = DBConnectionFactory.getConnection();

		// Step 1 Get all favorited itemIds
		Set<String> favoritedItemIds = conn.getFavoriteItemIds(userId);

		// Step 2 Get all categories of favorited items
		//通过ItemId得到categories再存入hashmap中
		Map<String, Integer> allCategories = new HashMap<>();
		for (String itemId : favoritedItemIds) {
			Set<String> categories = conn.getCategories(itemId);  //一个item对应很多categories
			for (String category : categories) {
				if (allCategories.containsKey(category)) {
					allCategories.put(category, allCategories.get(category) + 1);
				} else {
					allCategories.put(category, 1);
				}
			}
		}
		
		//根据key-value中的value进行排序，出现的次数越多，越靠前，在下一步搜索相同类型活动时先搜索它
		List<Entry<String, Integer>> categoryList = new ArrayList<>(allCategories.entrySet());
		Collections.sort(categoryList, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return Integer.compare(o2.getValue(), o1.getValue()); //从大到小降序排列，如果用treeMap的话默认用key排序 
			}
		});

		// Step 3 Do search based on catgory, filter out favorited items, sort by
		// distance
		Set<String> visitedItemIds = new HashSet<>();
		for (Entry<String, Integer> entry : categoryList) {
			List<Item> items = conn.searchItems(lat, lon, entry.getKey()); //得到items的list 
			List<Item> filteredItems = new ArrayList<>();

			for (Item item : items) {
				if (!visitedItemIds.contains(item.getItemId()) && !favoritedItemIds.contains(item.getItemId())) {
					filteredItems.add(item);
					visitedItemIds.add(item.getItemId());
				}
			}

			Collections.sort(filteredItems, new Comparator<Item>() {
				@Override
				public int compare(Item o1, Item o2) {
					return Double.compare(o1.getDistance(), o2.getDistance());
				}
			});

			recommendedItems.addAll(filteredItems);
		}

		conn.close();
		return recommendedItems;
  }

	
}
