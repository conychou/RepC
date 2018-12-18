# RepC
Replicated Concurrency Control and Recovery database system

Project Description
Implement Replicated Concurrency Control and Recovery Database.
It has following properities
-	Distributed database design with replicated data
-	Available on copies algorithm
-	Multiversion read(for readonly transaction)
-	Two phase locking with Read and Write lock
-	Site recovery
-	Deadlock detection


Environment SetUp:
module load python-2.7 
python -c 'import reprozip' 
export LC_ALL="en_US.UTF-8"
export LC_CTYPE="en_US.UTF-8"

Steps of execution
1.	Unzip by reprounzip directory setup RepC.rpz demo
2.	Run default testcase by reprounzip directory run demo
The default testcase is test1.txt.
begin(T1)
begin(T2)
W(T1,x1,101) 
W(T2,x2,202)
W(T1,x2,102) 
W(T2,x1,201)
end(T1)
dump()     
The result should be “T2 should abort, T1 should not, because of kill youngest”.
You will see all the execution in printout. Also, after executing each command, it will show the cached data of each transaction.

3.	If you want to run in command line that accept standard input
reprounzip directory run demo --cmdline java -jar RepC.jar


 
Main Component 
Transaction Manager:
Process the input command to transactions
Manage and execute transactions command
Communicate with LockManager to acquire locks
Communicate with SiteManager for Data read/write
Main function
•	public void process(String type, String op) : process input command and call the related function
•	public void begin(String op, boolean readOnly): create transaction
•	public void readwrite(String op, String type): execute read/write command. Will check whether it’s a readonly command or r/w command.
•	public void updateVariable(String varName, int value, int writeTime): update committed variable to related SiteManager
•	public DataItem getData(String varName, int site, int readTime): getData from siteManager
•	public Set<String> getDeadLockSet(Transaction t): when transaction needs to wait, check whether there is deadlock
•	public void abortTransaction(Transaction transaction, String msg): abort transaction and cleanup its cache and locks
•	public void recovery(String ops): recover site and check waiting transactions
•	public void fail(String ops): set site fail and abort related transaction

Transaction:
Keep transaction data (status, startTime, transactionName, readOnly flag, cache data, waiting informatoin) we can set transactionName, time, readOnly in constructor
Main function
•	public void setStatus(Status status): set transaction status ( RUNNING, WAITING, STOP)
•	public void addData(DataItem dataItem): add read or write Data in cache
•	public void printCache(): Print transaction current cache data

Lock Manager:
Provide read/Write persistent(commited) Data 
Main function
•	public Set<String> getRelatedTransaction(int site): get all transactions related to this site (means has get lock or access this site before)
•	public void releaseUpdateLock(String transactionName): when transaction abort or commit, release its lock and check the waiting transactions by calling updateLock(String varName)
•	public void updateLock(String varName): check waiting transaction, process if there is processable waiting transaction
•	public int getLock(String varName, String transactionName, String rw, String value): check transaction can get lock or not.
if yes, return site, if no, add it into waiting list

Lock:
Provide variable locking information (read, write, waiting and waitingReadOnly) 
Main function
•	getReadLock() / getWriteLock()
•	removeReadLock(String transactionName) / removeWriteLock(String transactionName)

Site Manager:
Initialize site data
Provide read/Write persistent(commited) Data on site
Provide multiversion read (for readonly)
Main function
•	public DataItem getData(String varName, int time) : get data from site based on time
•	public boolean isReadable(String varName): provide information that whether variable can be read or not. If the site fail and doesn’t get first write to updated value, the variable is not readable. 
•	public void updateData(String varName, int value, int time): update data on the site (called from TransactionManager)

Data:
Variable Data (isReplicated) in TreeMap sorted by time
Main function
•	public void setData(String varName, int val, int time): store the data with given timestamp
•	public DataItem getLatestData() : get the latest data
•	public DataItem getDataItem(int time): get the data at given timestamp

DataItem:
Variable Data (varName, value, writeTime…) 
