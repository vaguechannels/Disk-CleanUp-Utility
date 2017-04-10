import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.marimba.intf.application.IApplication;
import com.marimba.intf.application.IApplicationContext;
import com.marimba.intf.castanet.IWorkspace;
import com.marimba.intf.util.IConfig;
 
public class DiskCleanUpUtility extends SimpleFileVisitor<Path> implements IApplication {

	protected static boolean DEBUG = false, is64bit;
	protected static long currentepoch = System.currentTimeMillis()/1000, TotalDiskSpaceSaved;
	protected static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MMM/yyyy h:mm:ss a z ");

	private IApplicationContext context;
	private IWorkspace workspace;
	private IConfig tunerConfig;	//To Access Tuner Properties
	private IConfig config;			//To Access Channel Parameters
	private IConfig chConfig;		//To Access Channel Properties
	
	@Override
	public void notify(Object target, int msg, Object arg) {
		 switch (msg) {
			
			case APP_INIT:
				context = (IApplicationContext) arg;
				config = context.getConfiguration();
				workspace = (IWorkspace) context.getFeature("workspace");
				tunerConfig = (IConfig) context.getFeature("config");
				chConfig = workspace.getChannelCreate(this.context.getChannelURL().toString());
				try {Thread.sleep(2000);} catch (InterruptedException e1) {	e1.printStackTrace();}//Pause for two seconds				
				break;

			case APP_START:
				String debugflag = tunerConfig.getProperty("dsg.marimbadiskcleanuputility.debug.enabled");
				if (debugflag!= null && debugflag.equals("true")) {	 DEBUG = true;	}
				if (DEBUG) System.out.println(DebugInfo() + "DebugInfo# " + "Debug flag enabled, So we will print all debug messages");
				
				String folders = "C:/users, C:/windows/temp";
				ArrayList<String> folderlist = null;
				folderlist = new ArrayList(Arrays.asList(folders.split("\\s*,\\s*")));
				long initialsize =0, finalsize = 0;
				Date Starttime = null, Endtime = null;
				
				try 
				{
					System.out.println(DebugInfo() + "Folders to be scanned & cleaned: " + folders);
					Starttime = simpleDateFormat.parse(new java.text.SimpleDateFormat("dd/MMM/yyyy HH:mm:ss a z ").format(new Date()).toString());
					System.out.println(DebugInfo() + "Search started at " + Starttime);
					initialsize = getDiskSize() / 1024 / 1024;
					
					for (int i = 0; i<folderlist.size(); i++) 
					{
						System.out.println(DebugInfo() + "Scrubbing Directory: " + folderlist.get(i).toString());
						Path directoryToClean = Paths.get(folderlist.get(i).toString());
						DiskCleanUpUtility clean = new DiskCleanUpUtility(); 
						Files.walkFileTree(directoryToClean, clean);
					}
					
					Endtime = simpleDateFormat.parse(new java.text.SimpleDateFormat("dd/MMM/yyyy HH:mm:ss a z ").format(new Date()).toString());
					System.out.println(DebugInfo() + "Search completed at " + folders + " on " + Endtime);
					finalsize = getDiskSize() / 1024 / 1024 ;

					if(DEBUG) System.out.println(DebugInfo() + "Initial Disk Size : " + initialsize + " MB");
					if(DEBUG) System.out.println(DebugInfo() + "Final Disk Size : " + finalsize + " MB");

					totaltimetaken(Starttime, Endtime);
					System.out.println(DebugInfo() + "Disk space saved today: " + (initialsize - finalsize) + " MB");
					
					if(getRegKey(RegistryMacro() + "Marimba\\DiskCleanupUtility", "DiskSpaceSaved") == null) TotalDiskSpaceSaved = (long) 0;
					else TotalDiskSpaceSaved = Long.valueOf(getRegKey(RegistryMacro() + "Marimba\\DiskCleanupUtility", "DiskSpaceSaved"));
					System.out.println(DebugInfo() + "Total Disk Space saved till now: " + TotalDiskSpaceSaved);
					setRegKey(RegistryMacro() + "Marimba\\DiskCleanupUtility", "DiskSpaceSaved", Long.toString (TotalDiskSpaceSaved + (initialsize - finalsize)) );
					System.out.println(DebugInfo() + "Registry entry DiskSpaceSaved updated with total disk space saved");
					
				}
				catch (ParseException | IOException e) {e.printStackTrace();}
				context.stop();
				break;
         
			case APP_ARGV:
                context.stop();
                break;
			
			case APP_STOP:
				System.out.println(DebugInfo() + "Notification received APP_STOP...");
				break;
		}
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
	    if(attributes.isRegularFile())
	    {
	    	if(file.getParent().toString().contains("AppData\\Local\\Temp") 
	    			|| file.getParent().toString().contains("AppData\\Local\\Microsoft\\Windows\\Temporary Internet Files")
	    				|| file.getParent().toString().contains("C:\\windows\\temp"))
	    	{
	    		try
		    	{
		    		String LastModifiedTime = new java.text.SimpleDateFormat("dd/MMM/yyyy HH:mm:ss a z ").format(new Date (Long.parseLong(Long.toString(attributes.lastModifiedTime().toMillis()).substring(0, Long.toString(attributes.lastModifiedTime().toMillis()).length()-3))*1000));
			    	Date LastModifiedDate = simpleDateFormat.parse(LastModifiedTime);
		        	
		        	String CurrentTime = new java.text.SimpleDateFormat("dd/MMM/yyyy HH:mm:ss a z ").format(new Date());
		        	Date CurrentDate = simpleDateFormat.parse(CurrentTime.toString());
		        	
		        	long days = TimeUnit.DAYS.convert(CurrentDate.getTime() - LastModifiedDate.getTime(), TimeUnit.MILLISECONDS);
		        	
			    	if(days > 7)
	            	{
		        		Files.delete(file);
		        		System.out.println(DebugInfo() + "Deleted File: " + file.toAbsolutePath() + " was last modified: " + days + " days ago, on " + LastModifiedDate);
	            	}		    	
			    	
		    	}
	    		catch (ParseException | IOException p) 
	    		{
	    			System.out.println(DebugInfo() + "Access denied. Unable to delete file: " + file.toAbsolutePath());
	    		}
	    	}
	    }
	    else if(attributes.isSymbolicLink())
	    {
	    	if(file.getParent().toString().contains("AppData\\Local\\Temp") 
	    			|| file.getParent().toString().contains("AppData\\Local\\Microsoft\\Windows\\Temporary Internet Files")
	    				|| file.getParent().toString().contains("C:\\windows\\temp"))
	    	{ 
	    		System.out.println(DebugInfo() + "File is SymbolicLink/Shortcut to another directory. Skipping subtree of " + file.toAbsolutePath()); 
	    	}
	    	return FileVisitResult.SKIP_SUBTREE;
	    }
	    return FileVisitResult.CONTINUE;
	}
	 
	@Override
    public FileVisitResult visitFileFailed(Path file, IOException ioe) 
	{
		//Iterating all files within below listed location
		if(file.getParent().toString().contains("AppData\\Local\\Temp") 
    			|| file.getParent().toString().contains("AppData\\Local\\Microsoft\\Windows\\Temporary Internet Files")
    				|| file.getParent().toString().contains("C:\\windows\\temp"))
		{ 
			System.out.println(DebugInfo() + "File is SymbolicLink/Shortcut to another directory. Skipping subtree of " + file.toAbsolutePath()); 
		}		
		return FileVisitResult.CONTINUE;
    }
	
	@Override
    public FileVisitResult postVisitDirectory(Path directory, IOException ioe) throws IOException 
	{
		//Iterating all directories within below listed location
		if(directory.getParent().toString().contains("AppData\\Local\\Temp") 
				|| directory.getParent().toString().contains("AppData\\Local\\Microsoft\\Windows\\Temporary Internet Files")
					|| directory.getParent().toString().contains("C:\\windows\\temp"))
		{
			try	
			{	
				Files.delete(directory);
				System.out.println(DebugInfo() + "Purged Directory: " + directory.toAbsolutePath());
			}
			catch(DirectoryNotEmptyException ne) 
			{	
				System.out.println(DebugInfo() + "Unable to purge non-empty directory: " + directory.toAbsolutePath());	
			}
			catch(FileSystemException fe) 
			{
				System.out.println(DebugInfo() + "Access denied. Unable to purge directory: " + directory.toAbsolutePath());	
			}
		}		
		return FileVisitResult.CONTINUE;
    }
	
	private static long getDiskSize() throws IOException
	{
		long size = 0;
		Path drive = Paths.get("C:");
		FileStore store = Files.getFileStore(drive);
		size = store.getTotalSpace() - store.getUsableSpace();
		return size;
	}
	
	public static void totaltimetaken(Date time1, Date time2)
	{
		long totaltime = time2.getTime() - time1.getTime();
		System.out.println(DebugInfo() + "Search Took: " + TimeUnit.MILLISECONDS.toMinutes(totaltime) + " Minutes " 
				+ (TimeUnit.MILLISECONDS.toSeconds(totaltime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totaltime)))+ " Seconds");		
	}
	
	//Update Registryentries
	protected static void setRegKey(String Node, String Key, String Value)
	{
		String[] addregkey = {"REG", "ADD", Node , "/v", Key, "/d", Value, "/f"};
		//Command To Update Registry Entries
		if(DEBUG)	
		{	
			System.out.print(DebugInfo() + "Registry Add Command: " );
			for (String element : addregkey) {System.out.print(element + " ");}
			System.out.println();
		}
		ProcessBuilder pb = new ProcessBuilder(addregkey);
		try	{pb.start();} 
		catch (IOException e)	{e.printStackTrace();}
		if(DEBUG) System.out.println(DebugInfo() + "Registry " + Node + " updated with " + Key + " along with " + Value);
	}
	
	protected static String getRegKey(String Node, String Key) 
	{
		String type = "", value = "", key = "", out = "";
		String[] checkregkey = {"REG", "QUERY", Node, "/v" , Key};
		ArrayList<String> regResult = null;
		if(DEBUG) System.out.println(DebugInfo() + "Operating System:" + System.getProperty("os.name"));
		
		if(DEBUG)	
		{	
			System.out.print(DebugInfo() + "Registry Query Command: " );
			for (String element : checkregkey) {System.out.print(element + " ");}
			System.out.println();
		}
		
		ProcessBuilder pb = new ProcessBuilder(checkregkey);
		Process process;
		try 
		{
			process = pb.start();
			BufferedReader in = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
	        while ( ( out = in.readLine() ) != null ) 
	        {
	        	if(DEBUG) System.out.println(DebugInfo() + "Input & Output Information readLine " + out);
	        	if (out.matches("(.*)\\s+REG_(.*)")) { break; }
	        }
	        in.close();
	        if(DEBUG) System.out.println(DebugInfo() + "Registry Query Output:" + out);
	        
	        if(DEBUG) System.out.println(DebugInfo() + "Splitting Registry Query Output using ArrayList");
	        
	        regResult = new ArrayList(Arrays.asList(out.split("\t")));
	        if(System.getProperty("os.name").equals("Windows 7"))	{	regResult = new ArrayList(Arrays.asList(out.split(" ")));	}
	        
	        for (int i = 0; i<regResult.size(); i++) 
			{
	        	if(DEBUG) System.out.println(DebugInfo() + "Registry Attribute(ArrayList) " + regResult.get(i));
	        	if(regResult.get(i).startsWith("REG_")) { type = regResult.get(i);}
			}
	        
	        //Trucate Registry type from Registry Query Output
	        String str[] = out.split(type);
	        
	        //Capture the Reg Query Output
	        if(DEBUG)	
			{	
	        	System.out.print(DebugInfo() + "Registry Query Output after trucating Registry Attribute Type: " );
				for (String element : str) {System.out.print(element + " ");}
				System.out.println();
			}
	        
	        if(DEBUG) System.out.println(DebugInfo() + "Registry Attribute Type: " + type);
	        
	        for (int a=0; a < str.length; a++) 
	        {
	        	switch (a) 
                {	
                	case 0: key = str[a].trim(); if(DEBUG) System.out.println(DebugInfo() + "Registry Attribute Name: " + key); break;
                	case 1: value = str[a].trim(); if(DEBUG) System.out.println(DebugInfo() + "Registry Attribute Value: " + value); break;
                }
	        }
	        return value;	        
		} 
		catch(NullPointerException | IOException e)
        {
			if(DEBUG) System.out.println(DebugInfo() + "Registry Key or Attribute doesn't exist.");
        	return null;
        }
    }
	
	//Registry Entries For Reporting
	protected static String RegistryMacro() 
	{
		// Windows OS Verification
		String Macro = null;
		if (System.getProperty("os.name").contains("Windows")) 
		{
			is64bit = (System.getenv("ProgramFiles(x86)") != null);
			if (is64bit == false) 
			{	
				if(DEBUG)System.out.println(DebugInfo() + "OS Architecture: 32-bit OS");
				Macro = "HKLM\\Software\\";
			} 
			else 
			{	
				if(DEBUG)System.out.println(DebugInfo() + "OS Architecture: 64-bit OS");
				Macro = "HKLM\\Software\\Wow6432Node\\";
			}
		}
		else 
		{
			is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
		}
		return Macro;
	}
	
	public static String DebugInfo() 
	{
		String currenttime = new java.text.SimpleDateFormat("[dd/MMM/yyyy HH:mm:ss Z] ").format(new Date()); 
		String logtimestamp = currenttime	+ "- Client Engineering Info - " + System.getProperty("user.name") + " ";
		return logtimestamp;
	}

}
