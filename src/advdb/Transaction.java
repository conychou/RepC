package advdb;

import java.util.*;
enum Status{
	RUNNING,WAITING,STOP;
}
public class Transaction {
	
	Status status; 
	int startTime;			// to store the transaction start time
	String transactionName;	// transaction name such as T1
	HashMap<String, DataItem> cache;  // varName, DataItem
	Set<String> waitingT;	// the transition that i am waiting for
	String waitingVar;
	
	boolean readOnly;
	//String lastCommand;
	
	public Transaction(String transactionName, int time, boolean readOnly) {
		this.status = Status.RUNNING;
		this.startTime = time;
		this.transactionName = transactionName;
		this.readOnly = readOnly;
		cache = new HashMap<String, DataItem>();
		waitingT = new HashSet<String>();
		waitingVar = new String();
	}
	
	public void setWaiting(Set<String> waitingT, String waitingVar) {
		this.waitingT = waitingT;
		this.waitingVar = waitingVar;
		status = Status.WAITING;
	}
	
	public String getWaitingVar() {
		return this.waitingVar;
	}
	
	public Status getStatus() {
		return status;
	}
		
	public Set<String> getWaiting() {
		return this.waitingT;
	}
	
	 //set transaction status ( RUNNING, WAITING, STOP)
	public void setStatus(Status status) {
		this.status = status;
	}
	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public DataItem getData(String varName) {
		return cache.get(varName);
	}
	
	// add read or write data in cache
	public void addData(DataItem dataItem) {
		cache.put(dataItem.varName, dataItem);
	}

	public boolean hasData(String varName) {
		return cache.containsKey(varName);
	}	
	
	public String getTransactionName() {
		return transactionName;
	}

	public void destroy() {
		status = null;
		startTime = -1;
		transactionName = null;
		cache = null;
		waitingT = null;
	}
	
	// Print transaction current cache data
	public void printCache() {
		System.out.println("Data cache for transaction: "+this.transactionName);
		for(Map.Entry<String, DataItem> entry:cache.entrySet()) {
			System.out.print(entry.getKey()+": "+entry.getValue().getValue()+", ");
		}
		System.out.println("");
	}

	public HashMap<String, DataItem> getCache() {
		return cache;
	}

	public void setCache(HashMap<String, DataItem> cache) {
		this.cache = cache;
	}
}
