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

Transaction:
Keep transaction data (status, startTime, transactionName, readOnly flag, cache data, waiting informatoin) we can set transactionName, time, readOnly in constructor

Lock Manager:
Provide read/Write persistent(commited) Data 

Lock:
Provide variable locking information (read, write, waiting and waitingReadOnly) 


Site Manager:
Initialize site data
Provide read/Write persistent(commited) Data on site
Provide multiversion read (for readonly)


Data:
Variable Data (isReplicated) in TreeMap sorted by time


DataItem:
Variable Data (varName, value, writeTime…) 
