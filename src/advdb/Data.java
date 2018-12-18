package advdb;

import java.util.*;
public class Data {
	boolean isReplicated;
	TreeMap<Integer,DataItem> dataMap;
	
	// Variable Data (isReplicated) in TreeMap sorted by time
	public Data() {		
		dataMap = new TreeMap<Integer,DataItem>();
	}
	//get the data at given timestamp
	public DataItem getDataItem(int time) {
		return dataMap.floorEntry(time).getValue();
	}
	// get the latest data
	public DataItem getLatestData() {
		return dataMap.lastEntry().getValue();
	}
	public int getLatestModifyTime() {
		return dataMap.lastEntry().getKey();
	}
	public void setReplicated(boolean replicated) {
		this.isReplicated = replicated;
	}
	
	public boolean isReplicated() {
		return isReplicated;
	}
	// store the data with given timestamp
	public void setData(String varName, int val, int time) {
		dataMap.put(time, new DataItem(varName, val, false, -1));
	}
}
