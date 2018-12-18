package advdb;

import java.util.*;
public class SiteManager {

	//Provide read/Write persistent(commited) Data
	int siteNum;
	Status status; // 1: up, -1 : down
	LinkedHashMap<String, Data> data;
	int lastStartupTime;
		
	public SiteManager(int siteNum) {
		this.siteNum = siteNum;
		this.status = Status.RUNNING;
		this.lastStartupTime = 0;
		data = new LinkedHashMap<String, Data>();
		for (int i=1;i<=20;i++) {			
			if (i%2==0) {
				Data var = new Data();
				var.setData("X"+i, i*10, 0);
				var.setReplicated(true);
				data.put("X"+i, var);
			} else {
				if (1+i%10 == siteNum) {
					Data var = new Data();
					var.setData("X"+i, i*10, 0);
					var.setReplicated(false);
					data.put("X"+i, var);
				}
			}
		}
	}
	
	// update data on the site (called from TransactionManager)
	public void updateData(String varName, int value, int time) {
		if(this.status != Status.RUNNING)	return;
		Data var = data.get(varName);
		var.setData(varName, value, time);
		data.put(varName, var);
	}
	
	public Set<String> getVariableList() {
		return data.keySet();
	}
	
	public DataItem getData(String varName, int time) {
		Data var = data.get(varName);
		DataItem item = var.getDataItem(time);
		item.setIsRead(true);
		return item;
	}
	//provide information that whether variable can be read or not. 
	//If the site fail and doesn’t get first write to updated value, the variable is not readable. 
	public boolean isReadable(String varName) {
		if(status == Status.STOP)	return false;	// if the site is down, not readable	
		if (!data.get(varName).isReplicated) return true; // if the data is not replicated, it should be accessible
		// if it's replicated, it should have write before read
		if(data.get(varName).getLatestModifyTime() < this.lastStartupTime) 
			return false;
		else
			return true;
	}
	
	public void fail(){
		this.status = Status.STOP;
	}
	
	public void recovery(int time) {
		this.lastStartupTime = time;
		this.status = Status.RUNNING;
	}

	public void dump() {
		System.out.print("site "+(siteNum) +" - ");
		StringBuilder msg = new StringBuilder();
		
		for(Map.Entry<String,Data> entry:data.entrySet()) {
			DataItem data = entry.getValue().getLatestData();
			if(msg.length() != 0)	msg.append(", ");
			msg.append(data.varName+": "+data.value);	
		}
		System.out.println(msg.toString());
	}
	public int getLastStartupTime() {
		return lastStartupTime;
	}

	public void setLastStartupTime(int lastStartupTime) {
		this.lastStartupTime = lastStartupTime;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}	

}
