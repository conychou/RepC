/*
Author: YuLin Chou (yc3115)

Program Purpose: Advanced Database System Final Project

Program Description: Implement Replicated Concurrency Control and Recovery Database
It has following properities
-	Distributed database design with replicated data
-	Available on copies algorithm
-	Multiversion read(for readonly transaction)
-	Two phase locking with Read and Write lock
-	Site recovery
-	Deadlock detection

Compile and Execution steps:  
	1. javac *.java
	2. jar cfe RepC.jar advdb.RepC *.class
	3. java -jar RepC.jar (input file)		if no input file, it's in interactive mode and can read standard input

Date: 2018/12/07

*/
package advdb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RepC {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String fname = new String(), line;
		TransactionManager mgr;
		BufferedReader br = null;


		if (args.length > 1) {
			System.out.println("Usage: java nyu/advdb/repcrec/RepCRec or java nyu/advdb/repcrec/RepCRec filename");
			return;
		} else if(args.length == 1){			
			fname = args[0];
			System.out.println("File Name : "+fname+"\n");
		}
		
		mgr = new TransactionManager();
		// read files and prepare for processing
		
		try {
			
			if(!fname.isEmpty()) {
				br = new BufferedReader(new FileReader(fname));
			} else {
				br = new BufferedReader(new InputStreamReader(System.in));
			}
			
            while((line = br.readLine()) != null) {
	            	
            		if(line.contains("//"))
	        			line = line.substring(0,line.indexOf("//"));
	        		line = line.trim();
	        		if(line.isEmpty()) continue;
	        		
                String[] info = line.split("[()]");
                String type = info[0];
                String op =(info.length>1)?info[1]:new String();
                mgr.process(type, op);
            }			
		} catch (IOException e) {
			System.out.println("Error Occurred during execution");
		} finally {
            if(br != null) {
                try{
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }   
            }  
		}
		//System.out.println("=============================================");
		//mgr.dumpAll();
	}	
}
