package advdb;

import java.util.*;
public class Lock {
	Set<String> read;
	Set<String> write;
	List<commandSet> waiting;	
	Set<String> waitingReadOnly;
	
	class commandSet {
		boolean isRead;
		Set<String> transactions;	
		int value;
		Set<String> waitingSet;	// the set it's waiting
		
		public commandSet(String[] command, Set<String> w) {
			this.isRead = (command[1].equals("r")?true:false);
			transactions = new HashSet<String>();
			transactions.add(command[0]);
			if(command[1].equals("w"))
				value = Integer.valueOf(command[2]);
			this.waitingSet = w;
		}	
		public void addCommand(String[] command) {	//{transactionName, rw, value}
			if(command[1].equals("r"))
				transactions.add(command[0]);
			else
				System.out.println("write command couldn't combine");
		}
		public boolean isRead() {
			return isRead;
		}
		public Set<String> getTransactions() {
			return this.transactions;
		}

		public Set<String> getWaitingSet() {
			return this.waitingSet;
		}
	}
	public Lock() {
		read = new HashSet<String>();	
		write = new HashSet<String>();
		waiting = new ArrayList<commandSet>();
		waitingReadOnly = new HashSet<String>();
	}
	public Set<String> getReadLock() {
		return read;
	}
	public Set<String> getWriteLock() {
		return write;
	}
	public void setWriteLock(String transactionName) {
		write.clear();
		write.add(transactionName);
	}
	public void setReadLock(String transactionName) {		
		read.add(transactionName);
	}
	public List<commandSet> getWaitingTransaction() {
		return waiting;
	}
	
	// add command into variable waiting list and return the previous transaction set that it is waiting for
	public Set<String> addWaitingTransaction(String[] op) {
		/*System.out.print("addWaitingTransaction: ");
		for(String o:op)
		System.out.print(o);*/
		if(waiting.isEmpty()) {
			//System.out.println("waiting is Empty");
			waiting.add(new commandSet(op,(write.isEmpty())?read:write));
		} else {
			// get last set in waiting
			//combine with previous (if it's read)
			// add new in waitinglist
			commandSet last = waiting.get(waiting.size()-1);
			if(last.isRead()) {
				last.addCommand(op);
			} else {
				waiting.add(new commandSet(op, last.getTransactions()));
			}
		}
		return waiting.get(waiting.size()-1).getWaitingSet();
	}
	
	public void addWaitingReadOnly(String transactionName) {
		this.waitingReadOnly.add(transactionName);
	}
	public void removeReadLock(String transactionName) {
		read.remove(transactionName);
	}
	
	public Set<String> getWaitingReadOnly() {
		return waitingReadOnly;
	}
	
	public void removeWriteLock() {
		write.clear();
	}
	public void clearWaitingReadOnly() {
		waitingReadOnly.clear();
	}
	// clean up waiting transaction if it is abort
	public void removeWaitingTransaction(String transactionName) {
		for(commandSet command:waiting) {
			if(command.transactions.contains(transactionName)) {
				command.transactions.remove(transactionName);
				if(command.transactions.isEmpty())
					waiting.remove(command);
				break;
			}
		}
	}
}
