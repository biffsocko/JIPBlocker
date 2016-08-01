//******************************************************************************************************************************
//  BiffSocko
// JIPBlocker
//
// captures multiple failed attempts and guessing the ssh password.  After threshold,
// a rule is inserted into the firewall to block access from the IP address.  The rule will be removed
// after 24 hours.
// 
// NOTES:
//
// IPTABLES:
//  =================================================================================
//  ADD Rule    :  iptables -t filter -I INPUT -s 221.229.166.254 -j DROP
//  DELETE Rule :  iptables -D INPUT -s 221.229.166.254 -p all  -j DROP
//
// FIREWALLD
//  =================================================================================
//  ADD Rule    :  firewall-cmd --zone=public --add-rich-rule='rule family="ipv4" source address="11.22.33.44" reject'
//  DELETE Rule :  firewall-cmd --zone=public --remove-rich-rule="rule family=ipv4 source address="11.22.33.44" reject"
//
// SQL:
// delete > 24 hours : delete from dblock_blocked where DATE(date) = CURDATE() - INTERVAL 1 DAY;
//
//
// EXIT STATUS:  Success = 0 
//               Fail = 1
//
//******************************************************************************************************************************


import java.io.*;
import java.lang.Thread;
import java.util.regex.*;
import java.sql.*;


public class JIPBlocker {
	int threshold=10;
	int DEBUG=0;
	Connection conn;

    
	//*************************************************************
	//* Constructor
	//*************************************************************
    JIPBlocker(){
    	this.DBConnect();
    	this.openInputLog();
    }
    
    
	//*************************************************************
	//* Destructor
	//*************************************************************
    protected void finalize(){
    	this.DBClose();
    }
    
    
    //*************************************************************
    // parse a string and extract the IP address
    //*************************************************************
    private String getIP(String x){
    	String IPADDRESS_PATTERN =  "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
    	Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
    	Matcher matcher = pattern.matcher(x);
    	        if (matcher.find()) {
    	            return matcher.group();
    	        }
    	        else{
    	            return "0.0.0.0";
    	        }
    }
    
    
    private void addFirewallRule(String x){
        String s = null;
        String cmd="/usr/local/JIPBlocker/addFWrule.sh "+x;
        
        try {
             
            if(DEBUG == 1){
            	System.out.println("addFirewallRule: "+cmd);
            }
            Process p = Runtime.getRuntime().exec(cmd);
             
            BufferedReader stdInput = new BufferedReader(new
                 InputStreamReader(p.getInputStream()));
 
            BufferedReader stdError = new BufferedReader(new
                 InputStreamReader(p.getErrorStream()));
 
            if(DEBUG == 1){
	            // read the output from the command
	            while ((s = stdInput.readLine()) != null) {
	                System.out.println(s);
	            }
	             
	            // read any errors from the attempted command
	            System.out.println("Here is the standard error of the command (if any):\n");
	            while ((s = stdError.readLine()) != null) {
	                System.out.println(s);
	            }
            }
        }
        catch (IOException e) {
            System.out.println("exception happened - here's what I know: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    //*************************************************************
    // connect to the database
    //*************************************************************
    private void DBConnect(){
    	try{
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/dblock", "dbadmin", "d4ta123");
            // set up to handle as a transaction
            conn.setAutoCommit(true);   // not necessary to use transactions
    	}catch(Exception e){
            System.out.println("failed in insertDB\n"+e );
            System.exit(1);
    	}
    }

    //*************************************************************
    // just close up the database
    //*************************************************************
    private void DBClose(){
    	try{
    	    conn.close();
    	}catch(SQLException e){
    		System.out.println("FAIL closing DB connection");
    		System.exit(1);
    	}
    }

    
    private boolean overThreshold(String x){
    	
    	int numrows = 0; 
    	
    	try{
    	    String SQLStatement="select COUNT(*) as rowcount from dblock_attack where ip='"+x+"';";
            Statement stmt = conn.createStatement ();
            //stmt.executeQuery(SQLStatement);
            ResultSet result=stmt.executeQuery(SQLStatement);
         
            while(result.next()){
            	if(DEBUG == 1){
            		System.out.println("overThreshold :"+SQLStatement);
            	}
            		
            	numrows = result.getInt("rowcount");
            	if(DEBUG==1){
            		System.out.println("result = "+numrows);
            	}
        
            }
            
            stmt.close();
	    	    
    	}catch(SQLException e){
    		System.out.println("Error in method overThreshold");
    		System.exit(1);
    	}catch(Exception e){
    		System.out.println("exception in overThreshold");
    	}
    	
        if(numrows <= threshold){
        	return false;
        }else{
        	return true;
        }
    }
    
    private void insertDBforBlocking(String x){ 
    	try{
    		String SQLStatement="insert into dblock_blocked (ip, date) value ('"+x+"', NOW());";
    		Statement stmt = conn.createStatement ();
 
    
    		if(DEBUG == 1){    
        		System.out.println("insertDB - 2: "+SQLStatement);
            }	
    	 	this.addFirewallRule(x);
            stmt = conn.createStatement ();
            stmt.executeUpdate(SQLStatement);
            stmt.close();
        }catch(SQLException e){
        	System.out.println("failed in insertDB\n"+e );
        	System.exit(1);
        }catch(Exception e){
            System.out.println("failed in insertDB\n"+e );
            System.exit(1);
        }
    	
   
    	
    }
    
    //*************************************************************
    // check to see if IP is already being blocked
    //*************************************************************
    private boolean isBlocked(String x){
    	int numrows=0;
    	try{
            
        	//*******************************************************
        	// check to see if it's on the blocked list already
        	//*******************************************************
        	String SQLStatement="select COUNT(ip) as blockcount from dblock_blocked where ip='"+x+"';";
        	Statement stmt = conn.createStatement ();
        	

        	ResultSet result=stmt.executeQuery(SQLStatement);

        	
            while(result.next()){            		
            	numrows = result.getInt("blockcount");
        
            }

        	stmt.close();
        	
        	if(DEBUG == 1){
        		System.out.println("isBlocked - 1: "+SQLStatement);
        	}
        	
        }catch(SQLException e){
        	System.out.println("failed in insertDB\n"+e );
        	System.exit(1);
        }catch(Exception e){
            System.out.println("failed in insertDB\n"+e );
            System.exit(1);
        }
    	
    	if(numrows > 0){
    		return true;
    	}else{
    		return false;
    	}
    	
    }
    
    //*************************************************************
    // insert a row into the database
    //*************************************************************
    private void insertDB(String x){ 
    	try{ 
    		//*******************************************************
        	// insert the IP address to the dblock_attack database
        	//*******************************************************
        	String SQLStatement="insert into dblock_attack (ip, date) value ('"+x+"', NOW());";
            
        	if(DEBUG == 1){    
        		System.out.println("insertDB - 2: "+SQLStatement);
            }	
        	Statement stmt = conn.createStatement ();
            stmt.executeUpdate(SQLStatement);
            stmt.close();
        }catch(SQLException e){
        	System.out.println("failed in insertDB\n"+e );
        	System.exit(1);
        }catch(Exception e){
            System.out.println("failed in insertDB\n"+e );
            System.exit(1);
        }

    }
      
    //*************************************************************
    // parse the INPUT log
    //*************************************************************
    private void openInputLog(){
    	String seclog="/var/log/secure"; 
        BufferedReader tail;
        String IP;
        
        try{
        	tail = new BufferedReader(new FileReader(seclog));
            String line = null;

            while (true){
            	if(tail.ready()){
            	
                    line=tail.readLine();
                    if(line.contains("Failed password for")){         
                	    IP=this.getIP(line);
                 
                	    if(! this.isBlocked(IP)){
                	    	this.insertDB(IP);   
                	    	if(this.overThreshold(IP)){
                	    		this.insertDBforBlocking(IP);
                	    		if(DEBUG==1){
                	    			System.out.println("Blocking this IP address: "+IP);
                	    		}
                	    	}
                	    }
                    }                    
            	}else{
            		Thread.sleep(500);
            	}           
            }
        }catch(IOException e){
            System.out.println("error reading from"+seclog);
            System.exit(1);
        }catch(Exception e){
        	System.out.println(e);
        }
    }
    
    
  //*************************************************************
  // main()
  //*************************************************************  
	public static void main(String[] args) {
		JIPBlocker x = new JIPBlocker();
		x.finalize();
	}
}
