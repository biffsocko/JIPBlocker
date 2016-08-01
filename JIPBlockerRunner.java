import java.io.*;
import java.sql.*;

//NOTES:
//SQL:
//delete > 24 hours : delete from dblock_blocked where DATE(date) = CURDATE() - INTERVAL 1 DAY;
//
//
//EXIT STATUS:  Success = 0 
//            Fail = 1


public class JIPBlockerRunner {
	int DEBUG=0;
	Connection conn;
	
	
	//*************************************************************
	// constructor
	//*************************************************************
	JIPBlockerRunner(){
		this.DBConnect();
	}
	
	//*************************************************************
	//* Destructor
	//*************************************************************
    protected void finalize(){
    	this.DBClose();
    }
    
    private void delRule(String x){
        String s = null;
        String cmd="/usr/local/JIPBlocker/rmFWrule.sh "+x;
        
        try {

             
            if(DEBUG == 1){
            	System.out.println("delRule: "+cmd);
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
	            //System.out.println("Here is the standard error of the command (if any):\n");
	            while ((s = stdError.readLine()) != null) {
	                System.out.println(s);
	            }
            }
        }
        catch (IOException e) {
            System.out.println("exception: ");
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
            System.out.println("failed connecting to dblock / JIBlockerRunner:DBConnect\n"+e );
            System.exit(1);
    	}
    }
    
    public void insertHistory(String address, String date){
    	
    	try{
    		String SQLStatement="insert into dblock_history (ip,blocked,unblocked ) values ('"+address+"','"+date+"', NOW());";
    	
    		if(DEBUG == 1){
    			System.out.println(SQLStatement);
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

    public void removeFirewallRule(){
    	String SQLStatement="select ip,date from dblock_blocked where DATE(date) < CURDATE() - INTERVAL 1 DAY;";
       	try{
       		Statement stmt = conn.createStatement();
            ResultSet result = stmt.executeQuery(SQLStatement);
            
            while(result.next()){            		
            	String IP = result.getString("ip");
            	String date = result.getString("date");
            	// remove from the firwewall
            	
            	if(DEBUG == 1){
            		System.out.println("working on: "+IP);
            	}
            	
            	this.delRule(IP);
            	this.insertHistory(IP, date);
            }

            
            
            stmt.close();
	    	    
    	}catch(SQLException e){
    		System.out.println("Error in method removeFirewallRule   "+e);
    		System.exit(1);
    	}catch(Exception e){
    		System.out.println("exception in removeFirewallRule   "+e);
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

    public void unblock(){
       	try{
                
                //*************************************************************
                //* clean up the dblock_blocked table
                //*************************************************************
    	        String SQLStatement="delete from dblock_blocked where DATE(date) < CURDATE() - INTERVAL 1 DAY;";
       		if(DEBUG == 1){
       			System.out.println("unblock: "+SQLStatement);
       		}
       		Statement stmt = conn.createStatement();
       		
       		if(DEBUG==1){
       			System.out.println(SQLStatement);
       		}
       			
       		stmt.execute(SQLStatement);
                stmt.close();
	    	    
                //*************************************************************
                //* clean up the dblock_attack table
                //*************************************************************
                SQLStatement="delete from dblock_attack where DATE(date) < CURDATE() - INTERVAL 1 DAY;";
                if(DEBUG == 1){
                        System.out.println("unblock: "+SQLStatement);
                }
                stmt = conn.createStatement();

                if(DEBUG==1){
                        System.out.println(SQLStatement);
                }

                stmt.execute(SQLStatement);
                stmt.close();

    	}catch(SQLException e){
    		System.out.println("Error in method unblock "+e);
    		System.exit(1);
    	}catch(Exception e){
    		System.out.println("exception in unblock "+e);
    	}
    }
    
    
    
    
	public static void main(String[] args) {
		JIPBlockerRunner x = new JIPBlockerRunner();
		
		
		x.removeFirewallRule();
		
		// get rid of old entries
		x.unblock();
		
		

	}

}
