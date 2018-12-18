package advdb;

import java.util.*;

import advdb.Lock.commandSet;
public class LockManager {

	HashMap<String, Lock> locks;
	HashMap<Integer, Set<String>> siteLockMap;
	HashMap<String, Integer> unique;
	HashMap<String, Transaction> transactions;
	TransactionManager transactionManager;
	SiteManager[] sites;
	
	public LockManager(TransactionManager tm, SiteManager[] sites, HashMap<String, Integer> unique) {
		locks = new HashMap<String, Lock>();
		siteLockMap = new HashMap<Integer,Set<String>>();
		this.unique = unique;	
		this.transactionManager = tm;
		this.sites = sites;
	}
	
	// get all transactions related to this site (means has get lock or access this site before)
	public Set<String> getRelatedTransaction(int site) {
		return siteLockMap.getOrDefault(site, new HashSet<String>());
	}
	
	public void clearRelatedTransaction(int site) {
		if(siteLockMap.get(site) != null)
			siteLockMap.get(site).clear();
	}
	public void updateSiteMap(int site, String transactionName) {
		Set<String> list = siteLockMap.getOrDefault(site, new HashSet<String>());
		list.add(transactionName);
		siteLockMap.put(site, list);
	}
	
	// check transaction can get lock or not
	//if yes, return site
	//if no, add it into waiting list
	public int getLock(String varName, String transactionName, String rw, String value) {
		Lock lock = locks.getOrDefault(varName, new Lock());
		int site = -1;
		
		if (lock.getWriteLock().isEmpty()) {
			if (rw.equals("w")) { 
				// to get a write lock, if readlock is empty or 
				// promotion readlock to writelock : transaction own readlock and want to write, and no other waiting
				if (lock.getReadLock().isEmpty()||
						((lock.getReadLock().size()==1) && (lock.getReadLock().contains(transactionName) &&
								(lock.getWaitingTransaction().isEmpty())))) {  
					// (1) no one in read lock 
					// (2) if the transaction is the only one which has read lock, it can get write lock
					if(unique.containsKey(varName)) {
						site = unique.get(varName);
						if(sites[site].getStatus() == Status.STOP)
							site = -1;
					} else {
						for(int idx=1; idx<=TransactionManager.siteNum; idx++) {
							if(sites[idx].getStatus() == Status.RUNNING) {
								site = idx;
								break;
							}
						}
					}
				}				
			} else {
				// get a read lock, since there is no waiting write lock, it can combine with cur read lock
				if(unique.containsKey(varName)) {
					site = unique.get(varName);
					if(!sites[site].isReadable(varName))
						site = -1;
				} else {
					for(int idx=1; idx<=TransactionManager.siteNum; idx++) {
						if(sites[idx].isReadable(varName)) {
							site = idx;
							break;
						}
					}
				}
			}
			if(site > 0) {
				if(rw.equals("w")) {
					// update siteLockMap, if replicated, write affect all site.
					lock.setWriteLock(transactionName);
					if(unique.containsKey(varName)) {
						updateSiteMap(site, transactionName);
					} else {
						for(int idx=1; idx<=TransactionManager.siteNum; idx++) {
							if(sites[idx].getStatus() == Status.RUNNING) {
								updateSiteMap(idx, transactionName);
							}
						}
					}
				} else {
					// read only reference one site
					lock.setReadLock(transactionName);
					updateSiteMap(site, transactionName);
				}
				locks.put(varName, lock);
				return site;
			}
		}
		System.out.println("doesn't get lock, put waiting");
		Set<String> waitingT = lock.addWaitingTransaction(new String[] {transactionName, rw, value});	
		transactionManager.getTransaction(transactionName).setWaiting(waitingT, varName);
		return site;
	}
	
	// when transaction abort or commit, release its lock and check the waiting transactions
	public void releaseUpdateLock(String transactionName) {
		//System.out.println("release all locks for "+transactionName);
		Transaction t = transactionManager.getTransaction(transactionName);	
		for(String varName:t.getCache().keySet()) {
			Lock lock = locks.get(varName);
			lock.removeReadLock(transactionName);
			lock.removeWriteLock();
			updateLock(varName);
		}
		if(t.getStatus() == Status.WAITING) {
			locks.get(t.getWaitingVar()).removeWaitingTransaction(transactionName);
			updateLock(t.getWaitingVar());
		}
		// clear transaction in siteLockMap
		for(Set<String> set:siteLockMap.values()) {
			if(set.contains(transactionName))
				set.remove(transactionName);
		}
	}
	
	// check waiting transaction, process if there is processable waiting transaction
	public void updateLock(String varName) {
		//System.out.println("updateLock "+varName);
		if(!locks.containsKey(varName))	return;
		Lock lock = locks.get(varName);
		
		// clear and retry all readonly
		List<String> readonlyTransaction = new ArrayList<String>(lock.getWaitingReadOnly());
		lock.clearWaitingReadOnly();
		for(String transactionName:readonlyTransaction) {
			transactionManager.getTransaction(transactionName).setStatus(Status.RUNNING);
			transactionManager.readwrite(transactionName+","+varName, "r");
		}

		// check locking status 
		//if readlock can promote to writelock, continue to get the lock
		//if it block by others, return 
		if(!lock.getWriteLock().isEmpty() || (!lock.getReadLock().isEmpty())) {
				if((lock.getReadLock().size() ==1) && (!lock.getWaitingTransaction().isEmpty()) && (!lock.getWaitingTransaction().get(0).isRead()) && 
								(lock.getWaitingTransaction().get(0).getTransactions().equals(lock.getReadLock()))) {
					System.out.println("promote readlock to writelock");
				} else {
					System.out.println("Lock still occupied by others");
					return;
				}
		}
		
		// check waiting transaction set and find canProcess one
		// write can go ahead, read --> wait write or can read
		commandSet canProcess = null;
		int readSite = -1;
		for(commandSet set:locks.get(varName).getWaitingTransaction()) {
			if(set.isRead()) {
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
				if(readSite<0) {
					continue;
				} else {
					canProcess = set;
					break;
				}
			} else {
				canProcess = set;
				break;
			}
		}
		if(canProcess != null) {
			//System.out.println("has canProcess isRead:"+canProcess.isRead());
			if(canProcess.isRead()) {
				// set waiting reads to get readlock
				Set<String> read = canProcess.getTransactions();
				for(String r:read) {
					Transaction t = transactionManager.getTransaction(r);
					t.setStatus(Status.RUNNING);
					t.addData(transactionManager.getData(varName, readSite, transactionManager.time));
					locks.get(varName).removeWaitingTransaction(r);
					locks.get(varName).setReadLock(r);
					updateSiteMap(readSite, r);	
				}
			} else {
				// set waiting write to get writelock, also update all siteLockMap
				Set<String> write = canProcess.getTransactions();
				for(String w:write) {
					Transaction t = transactionManager.getTransaction(w);
					t.setStatus(Status.RUNNING);
					t.addData(new DataItem(varName, canProcess.value, false, transactionManager.time));
					locks.get(varName).removeWaitingTransaction(w);
					locks.get(varName).setWriteLock(w);
					for(int idx=1; idx<=TransactionManager.siteNum; idx++) {
						if(sites[idx].getStatus() == Status.RUNNING) {
							updateSiteMap(idx, w);
						}
					}					
				}	
			}
		}
	}
	
	// if transaction can't be process, add to waiting list
	public void addWaitingList(String varName, String transactionName, String rw, boolean readonly, String value) {
		Lock lock = locks.get(varName);	
		if(lock == null)	{ //update 1208
			locks.put(varName, new Lock());
			lock = locks.get(varName);	
		}
		if(readonly) {
			lock.addWaitingReadOnly(transactionName);
		} else {
			Set<String> waitingT = lock.addWaitingTransaction(new String[] {transactionName, rw, value});	
			//System.out.print("addWaiting: waitingTSet");
			//System.out.println(waitingT);
			transactionManager.getTransaction(transactionName).setWaiting(waitingT, varName);
		}
	}
		
	public void printLock() {
		System.out.println("==========================================");
		Set s = locks.keySet();
		Iterator it = s.iterator();
		while (it.hasNext()) {
			String k = (String)it.next();
			System.out.println("READ for "+k+ " : "+locks.get(k).getReadLock());
			System.out.println("WRITE for "+k+ " : "+locks.get(k).getWriteLock());
			//System.out.println("WAIT for "+k + " : "+ locks.get(k).getWaitingTransaction().get(0));
			System.out.print("WAIT for "+k + " :");
			List list = locks.get(k).getWaitingTransaction();
			if (list != null) {
				for (int i=0;i<list.size();i++) {
					String[] wait = (String[])list.get(i);
					System.out.print(wait[0]+" ");
				}
			}
			System.out.println("");
		}		
		System.out.println("==========================================");
	}
	
}
