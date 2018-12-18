package advdb;

import java.util.*;

public class TransactionManager {
	
	int time;
	final static int siteNum = 10;
	SiteManager[] sites;
	LockManager lockMgr = null;
	HashMap<String, Transaction> transactions = null;
	HashMap<String, Integer> unique = null;

	public TransactionManager() {
		this.time = 0;
		this.sites = new SiteManager[siteNum+1];
		
		unique = new HashMap<String, Integer>();	//<variable, List<site>>
		for (int i = 1; i <= 20; i++) {
			if (i % 2 != 0) {
				unique.put(("X"+i), (1+i%10));
			}
		}	
		this.lockMgr = new LockManager(this, sites,unique);
		
		transactions = new HashMap<String, Transaction>();
		for (int i=1;i<=siteNum;i++) {
			sites[i] = new SiteManager(i);
		}
	}
	// process input command
	public void process(String type, String op) {
		time++;
		type = type.toLowerCase();
		op = op.toUpperCase();
		if(op.isEmpty() && !type.equals("dump")) {
			System.out.println("Parsing error");
			return;
		}
		System.out.println("*** Command : "+type +" . " + op+" ***");
		
		switch (type) {
		case "begin":
			begin(op, false);
			break;
		case "beginro":
			begin(op, true);
			break;
		case "r":
			readwrite(op, type);
			break;
		case "w":
			readwrite(op,type);
			break;
		case "fail":
			fail(op);
			break;
		case "recover":
			recovery(op);
			break;
		case "end":
			end(op);
			break;
		case "dump":
			dumpAll();
			break;
		case "dumpvar":
			dumpVariable(op);
		default:
			System.out.println("Cannot parse command");
		}
		for(Transaction t:transactions.values())
			t.printCache();
	}
	
	public void begin(String op, boolean readOnly) {
		
		String transactionName = op.trim();
		Transaction t = new Transaction(transactionName, time, readOnly);	
		transactions.put(transactionName, t);
	}
	// update committed variable to site
	public void updateVariable(String varName, int value, int writeTime) {
		
		if (!unique.containsKey(varName)) {
			for (int i=1;i<=TransactionManager.siteNum;i++) {
				// check writeTime that the site is up when transaction write
				if(writeTime < sites[i].lastStartupTime)	{
					System.out.println("site "+i+" will not update "+varName+" since when transaction write, the site is not up");
					continue;
				}
				sites[i].updateData(varName, value, time);
			}			
		} else {
			sites[unique.get(varName)].updateData(varName, value, time);
		}
	}
	
	public void end(String op) {
		String transactionName = op.trim();	
		
		Transaction t = transactions.get(transactionName);
		
		if (transactionName.isEmpty() || transactions.get(transactionName).getStatus() != Status.RUNNING) {
			System.out.println(transactionName+" status is "+transactions.get(transactionName).getStatus());
			return;
		}
		// commit, update value of writelock in cache
		for(Map.Entry<String,DataItem> entry:t.getCache().entrySet()) {
			DataItem dt = (DataItem) entry.getValue();
			if(!dt.isRead()) {
				updateVariable(dt.getVarName(), dt.getValue(), dt.getWriteTime());
			}
		}		
		if(!t.isReadOnly())	lockMgr.releaseUpdateLock(transactionName);
		t.getCache().clear();
		t.setStatus(Status.STOP);
		System.out.println("Transaction " + transactionName + " commits");
	}
	// when site fail, call abort transaction to clean up cache and lock
	// clean up siteMap
	public void fail(String ops) {
		int site = Integer.parseInt(ops.trim());
		
		sites[site].fail();
		for(String t:lockMgr.getRelatedTransaction(site)) {
			abortTransaction(transactions.get(t), "site "+site+" fail");
		}
		lockMgr.clearRelatedTransaction(site);
	}
	
	// when site recover, setup site and check waiting transactions
	public void recovery(String ops) {
		try {
			int siteNum = Integer.parseInt(ops);
			sites[siteNum].recovery(time);
			// if data is unique, make readonly can read
			
			// active lock checking for each variable in this site
			for(String variable:sites[siteNum].getVariableList())
				lockMgr.updateLock(variable);
		} catch (NumberFormatException ne) {
			ne.printStackTrace();
			System.out.println("site name is not in right format :" + ops);
		}
	}		
	// for read only transaction, if there is available site, get Data
	// for read/write transaction, get lock (LockManager will check available site before giving lock) and get Data
	//							   if we can't get lock, check whether there is deadlock
	public void readwrite(String op, String type) {
		
		String[] tk = op.split(",");
		if (tk.length < 2) {
			System.out.println("read/write parameter parsing error");
			return;
		}
		
		String transactionName = tk[0].trim();
		String varName = tk[1].trim();
		String value = null;
		if(tk.length == 3) {
			value = tk[2].trim();
		}
		if (transactionName.isEmpty() || transactions.get(transactionName).getStatus() != Status.RUNNING) {
			System.out.println(transactionName+" status is "+transactions.get(transactionName).getStatus());
			return;
		}
		if (varName.isEmpty()) {
			System.out.println("varName is empty");
			return;
		}

		Transaction t = transactions.get(transactionName);
			
		if (type.equals("r") && t.isReadOnly()) {  // readonly transaction
			// find available site to read
			int readSite = -1;
			if(unique.containsKey(varName)) {
				readSite = unique.get(varName);
				if(!sites[readSite].isReadable(varName))
					readSite = -1;
			} else {
				for(int idx=1; idx<=TransactionManager.siteNum; idx++) {
					if(sites[idx].isReadable(varName)) {
						readSite = idx;
						break;
					}
				}
			}
			if(readSite>0) {
				// store the retrieved data in cache
				//lockMgr.updateSiteMap(readSite,transactionName);	// no need to record site since if site fail just wait
				DataItem item = getData(varName, readSite, t.getStartTime());
				System.out.println(item.getVarName()+":"+item.getValue());
				t.addData(item);
			} else {
				//all read site fail, put in variable waitList
				lockMgr.addWaitingList(varName, transactionName, "r", true, null);
			} 
		} else { // read/write transaction
			// use data in cache without checking lock
			// if (1) we need to read and we already read it before (2) we write it before
			if(t.hasData(varName) && (type.equals("r") || !t.getCache().get(varName).isRead())) {
				System.out.println("Already has write data in cache");
				DataItem item = t.getData(varName);
				if(type.equals("r"))	{
					System.out.println(item.getVarName()+":"+item.getValue());
				} else {
					t.addData(new DataItem(varName, Integer.valueOf(value), false, time));
				}
				return;
			}
			
			int site = lockMgr.getLock(varName, transactionName, type, value);
			if(site > 0) { 
				// if it got the lock,
				// for read, read Data from the site and store the retrieved data in cache
				// for write, store the write data in cache
				if(type.equals("r"))	{
					DataItem item = getData(varName, site, time);
					System.out.println(item.getVarName()+":"+item.getValue());
					t.addData(item);
				}else {
					t.addData(new DataItem(varName, Integer.valueOf(value), false, time));
				}
			} else {
				// couldn't get lock, so need to wait 
				// check deadlock. if exists, abort youngest one
				t.setStatus(Status.WAITING);
				Set<String> set = getDeadLockSet(t);
				if(!set.isEmpty()) {
					int maxTime = Integer.MIN_VALUE;
					String youngestTransaction = new String();
					for(String name:set) {
						int startTime = transactions.get(name).getStartTime();
						if(maxTime < startTime) {
							maxTime = startTime;
							youngestTransaction = name;
						}
					}
					abortTransaction(transactions.get(youngestTransaction), "deadlock, so abort the youngest");
				}
			}
		}
	}
	
	public void abortTransaction(Transaction transaction, String msg) {
		String transactionName = transaction.getTransactionName();
		
		System.out.println("Trasaction " + transactionName + " aborted because " + msg);
		lockMgr.releaseUpdateLock(transactionName);
		transaction.getCache().clear();
		transaction.setStatus(Status.STOP);
	}
	// read transaction get data based on readTime
	// if it's readonly, readTime will be transactionStartTime
	public DataItem getData(String varName, int site, int readTime) { 
		return sites[site].getData(varName, readTime);
	}
	
	public Transaction getTransaction(String transName) {
		return transactions.get(transName);
	}
	
	// when transaction needs to wait, check whether there is deadlock
	public Set<String> getDeadLockSet(Transaction t) {
		//System.out.println("getDeadLockSet "+t.getTransactionName());
		Set<String> cycle = new HashSet<String>();
		for(String w:t.getWaiting()) {
			//System.out.print("waiting "+w);
			if(w.equals(t.getTransactionName()))	continue;
			if(transactions.get(w).getStatus() == Status.WAITING && 
					isCycle(w,t.getTransactionName(),new HashSet<String>(), new HashSet<String>(),cycle)) {
				break;
			}
		}
		return cycle;
	}
	public boolean isCycle(String cur, String target, Set<String> route, Set<String> visited, Set<String> result) {
		visited.add(cur);
		route.add(cur);
		if(cur.equals(target)) {
			result.addAll(route);
			return true;
		}		
		for(String w:transactions.get(cur).getWaiting()) {
			if(!visited.contains(w) && (transactions.get(w).getStatus() == Status.WAITING) 
					&& (isCycle(w, target, route, visited, result)))
				return true;
		}
		route.remove(cur);
		return false;
	}
	
	public void dumpAll() {
		for (SiteManager sm : sites) {
			if (sm != null)
				sm.dump();
		}
	}
	public void dumpVariable(String op) {
		String varName = op.trim();
		Transaction t = transactions.get(varName);
		System.out.print(t.getStatus());
		if(t.getStatus()==Status.WAITING) {
			System.out.print(" waitVar:"+t.getWaitingVar()+" (");
			for(String s:t.getWaiting())
				System.out.print(s+",");
			System.out.print(")");
		}
		System.out.print(" LastStartTime:"+t.getStartTime()+" ");
		for(DataItem item:t.getCache().values()) {
			System.out.print(item.varName+item.value+(item.isRead()?" (R) ":" (W) "));
		}
	}
}
