package utility;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class utils {
	
	// DEBUGGING
	static JFrame error_popup_window = new JFrame();
	
	public static String getLogPath()
	{
		
		// Get current working directory path
		Path current_working_directory = Paths.get(System.getProperty("user.dir"));
		
		if (!current_working_directory.toString().contains("AbstractSwarm"))
		{
			
        	JOptionPane.showMessageDialog( error_popup_window, 
										   String.format( "Error: Current path '%s' does not contain \"AbstractSwarm\".!\n" +
												   	      "Current dir: '%s'",
												   	      current_working_directory,
								                          System.getProperty( "user.dir" )
								                         )	
        								   );
        	
			System.out.println("Error: Current path does not contain \"AbstractSwarm\".");
		}
		
		while (!current_working_directory.toString().endsWith("AbstractSwarm"))
		{
			
			current_working_directory = current_working_directory.getParent();
		}
		
		// Change path to the graphs subfolder inside the AbstractSwarm folder		
		Path log_path = Paths.get( current_working_directory.toString(), "timetables.log" );
		
		File log_file = new File( log_path.toString() );
		
		if (  !( log_file.exists() && !( log_file.isDirectory() ) ) )
		{
			
        	JOptionPane.showMessageDialog( error_popup_window, 
        								   String.format( "Error: Log file \"timetables.log\" does not exist in ..\\AbstractSwarm\\.!\n" +
        										   	      "Current dir: '%s'",
        						                          System.getProperty( "user.dir" )
        						                         )	
				                          );
        	
			// If not, print error message and return given name of the directory
			System.out.println( "Error:\t.log file \"timetables.log\" does not exist in ..\\AbstractSwarm\\.!" );
			
			return "";
		}
		else
		{
			
			return log_path.toString();	
		}		
	}
	
	/**
	 * Function: generate_absolute_simulation_folder_path
	 * 
	 * This function takes the name of a simulation folder and returns the correct absolute path.
	 * 
	 * The name of the folder for the current simulation (where its results will be stored) is passed to the PerformanceEvaluation algorithm as an argument.
	 * This name does not contain the full absolute path, it is only the name of the folder.
	 * 
	 * To create the absolute path (to access the folder later), the current working directory is used. We know where the simulation folder lies.
	 * We just have to move through the folder structure to the correct parent folder.
	 * 
	 * The problem is that the current working directory is different for different types of usage of the PerformanceEvaluation/AbstractSwarm program.
	 * The user can either control the program via the graphical user interface or use the batch files (sim_agenda.txt / sim_agenda.bat) to start the simulation.
	 * 
	 * The working directory is different for the two options.
	 * 
	 * In order to assure that the correct absolute path is generated (and thus, the folder of the current simulation can be found), we need to adapt to this.
	 * 
	 * The process is simple as we just have to move up the folder structure until we reach the "PerformanceEvaluation" folder. The number of directory changes differs for the
	 * types of usage as described above.
	 * 
	 * @param folder_name: String, the name of the folder (just the name, not the absolute path)
	 * @return String, the absolute path of the folder (given it exists, else an error is thrown)
	 * @throws IOException 
	 */
	public static String generate_absolute_simulation_folder_path(String folder_name) throws IOException
	{

		// Get current working directory path
		Path curr_directory_path = Paths.get(System.getProperty("user.dir"));
		
		// Make sure we are inside the AbstractSwarm folder structure
		if (!curr_directory_path.toString().contains("AbstractSwarm"))
		{
			
        	JOptionPane.showMessageDialog( error_popup_window, 
										   String.format( "Error: Current working directory path does not contain \"AbstractSwarm\".!\n" +
												   	      "Current working directory path: '%s'",
												   	      curr_directory_path
								                         )	
        								  );
        	
			System.out.println("Error: Current working directory path does not contain \"AbstractSwarm\".");
			
			throw new IOException("Current working directory path does not contain \"AbstractSwarm\".");
		}
		
		// Move up in the folder structure (starting from current working directory) until we reach the PerformanceEvaluation folder
		while (!curr_directory_path.toString().endsWith("PerformanceEvaluation"))
		{
			
			curr_directory_path = curr_directory_path.getParent();
		}
		
		// Get the results folder path (containing the simulation folders)
		Path results_folder_path = Paths.get(curr_directory_path.toString(), "Results");
		
		// Get the path of the current simulation folder (according to the given folder name)
		Path current_simulation_folder_path = Paths.get(results_folder_path.toString(), folder_name);
		
		// Make sure the folder with the given name exists (and is a directory, not a file)
		File current_simulation_folder = new File( current_simulation_folder_path.toString() );
		
		// If the folder does not exists, throw an error
		if (  !( current_simulation_folder.exists() && current_simulation_folder.isDirectory() ) )
		{
			
			// Show pop-up window
        	JOptionPane.showMessageDialog( error_popup_window, 
										   String.format( "Error: Directory %s does not exist in ..\\Results!\n",
												   		  "Current path: %s",
												   		  folder_name,
												   		  current_simulation_folder
								                         )	
										  );
        	
			// Also, print to the console
			System.out.println(String.format( "Error:\tDirectory %s does not exist in ..\\Results!", current_simulation_folder ) );
			
			throw new IOException("Simulation folder does not exists in ..\\Results folder.");
		}
		// If the folder does exists, return its absolute path
		else
		{
			
			// Return the path to the folder of the current simulation
			return current_simulation_folder_path.toString();	
		}		
	}
	
	/**
	 * get_absolute_graph_path
	 * 
	 * This function takes the name of a graph and returns the correct absolute path.
	 * 
	 * To create the absolute path (to access the graph's file), the current working directory is used. We know where the graph file lies.
	 * We just have to move through the folder structure to the correct parent folder.
	 * 
	 * The problem is that the current working directory is different for different types of usage of the PerformanceEvaluation/AbstractSwarm program.
	 * The user can either control the program via the graphical user interface or use the batch files (sim_agenda.txt / sim_agenda.bat) to start the simulation.
	 * 
	 * The working directory is different for the two options.
	 * 
	 * In order to assure that the correct absolute path is generated (and thus, the file of the graph can be found), we need to adapt to this.
	 * 
	 * The process is simple as we just have to move up the folder structure until we reach the "AbstractSwarm" folder. The number of directory changes differs for the
	 * types of usage as described above.
	 * 
	 * @param graph_name: String, the name of the graph
	 * @return String, absolute path of the graph
	 * @throws IOException 
	 */
	public static String get_absolute_graph_path(String graph_name) throws IOException
	{
				
		// Get current working directory path
		Path current_working_directory = Paths.get(System.getProperty("user.dir"));

		// Make sure we are located in the AbstractSwarm folder structure (else throw an error)
		if (!current_working_directory.toString().contains("AbstractSwarm"))
		{
			
			// Pop-up window
        	JOptionPane.showMessageDialog( error_popup_window, 
										   String.format( "Error: Current working directory path does not contain \"AbstractSwarm\".!\n" +
												   	      "Current working directory: '%s'",
												   	      current_working_directory
								                         )	
										  );
        	
        	// Write to console
			System.out.println( "Error: Current working directory path does not contain \"AbstractSwarm\"." );
			
			// Throw error
			String exception = String.format("Current working directory does not contain \"AbstractSwarm\".\nCurrent working directory: '%s'", current_working_directory );
			throw new IOException( exception );
		}
				
		// Move up the folder structure until you reach the AbstractSwarm folder
		while (!current_working_directory.toString().endsWith("AbstractSwarm"))
		{
			
			current_working_directory = current_working_directory.getParent();
		}
		
		// Change path to the graphs subfolder inside the AbstractSwarm folder		
		Path graphs_path = Paths.get(current_working_directory.toString(), "Graphs", graph_name);
		
		// Check if graph exist in directory
		File graph_file = new File(graphs_path.toString());
		
		if (!((graph_file.exists() && !graph_file.isDirectory())))
		{
        	JOptionPane.showMessageDialog( error_popup_window, 
						        			String.format( "Error: Graph '%s' could not be found in ..\\Graphs!\n" +
						        						   "Current dir: '%s'",
						        						   graph_name,
						        						   System.getProperty( "user.dir" )
						        					      )
					        			  );
        	
			// If not, print error message and return given name of the graph
			System.out.println("Error:\tGraph does not exist in ..\\Graphs");
			System.out.print("Graphs name:\t");
			System.out.println(graph_name);
			
			return graph_name;
		}
				
		// Return the string of the graphs path
		return graphs_path.toString();
	}
	
	public static double getVisits(String graph_name, Agent curr_agent, Station curr_station)
	{
		/**
		 * Assumptions:
		 * 
		 * 	- If an agent is connected to a station, only one of them has either the frequency or the necessity attribute
		 * 	- This function returns the amount of times one single agent instance has to visit a particular instance of a station object connected to it
		 * 
		 * Different scenarios:
		 * 
		 * 	- Agent has the frequency attribute
		 * 		
		 *  	- visits( curr_agent, curr_station ) = frequency_value( curr_agent )  / sum{ count( connected_stations( curr_agent )
		 * 
		 * 	- Agent has the necessity attribute
		 * 
		 *  	- visits( curr_agent, curr_station ) = necessity_value( curr_agent )
		 * 
		 * 	- Station has the frequency attribute
		 * 
    	 *		- visits( curr_agent, curr_station ) = frequency_value( curr_station )  / sum{ count( connected_agents( curr_station )
		 * 
		 * 	- Station has the necessity attribute
		 * 
		 *  	- visits( curr_agent, curr_station ) = necessity_value( curr_station )
		 */
		
		// Reserve memory for the attribute values and their corresponding object
		HashMap<String, Integer> obj_attr_val_dict = new HashMap<String, Integer>();	// HashMap<objectType_attributType, attributeValue>
		
		// Next, read the attributes (frequency, necessity) for both, the current agent and the current station
		obj_attr_val_dict.put("agent_frequency",   curr_agent.getFrequency());
		obj_attr_val_dict.put("agent_necessity",   curr_agent.getNecessity());
		obj_attr_val_dict.put("station_frequency", curr_station.getFrequency());
		obj_attr_val_dict.put("station_necessity", curr_station.getNecessity());

        // Generate iterator for the obj_attr_val_dict
        Iterator < String > dict_iterator = obj_attr_val_dict.keySet().iterator();
        		
        // Iterate over obj_attr_val_dict
        while (dict_iterator.hasNext()) 
        {

        	// Read the current key
            String curr_key = dict_iterator.next();
            
            // Check if the value of the current key equals '0'
            if (obj_attr_val_dict.get(curr_key) == 0)
            {
            	
                // If that is true, remove that key from dict
            	dict_iterator.remove();
            }
        }
        		
		/*
		 *  Check whether or not two entries remain in the collection
		 *  In that case, something is wrong, because the assumption in this function is violated
		 */
        if (!(obj_attr_val_dict.size() == 1))
        {
        	
        	JOptionPane.showMessageDialog( error_popup_window, "Error: Cannot compute visit events for current pair of agent and station.\n\n" 			+
													String.format( "Current graph name: %s\n", graph_name ) 									+
													String.format( "Current agent id: %d\n", curr_agent.getId() ) 							+
													String.format( "Current station id: %d\n\n", curr_station.getId() ) 						+
        											"Neither agent nor station has a frequency or a necessity atribute.\n" 					+
        											"Unfortunately, this is required to calculate the total waiting time of the graph.\n" 	+
        											"At the moment, the handling of this case is not yet implemented,\n"					+
        											"thus, some graphs might not be supported by the Evaluation-Tool.\n" 					+
        											"We are currently working on this. For now, program will exit.");
        	
        	// Error message
        	System.out.println("Error: Cannot compute visit events for current pair of agent and station.\n" 			+
								String.format( "Current agent id:\t%d\n", curr_agent.getId() ) 							+
								String.format( "Current station id:\t%d\n", curr_station.getId() ) 						+
								"Neither agent nor station has a frequency or a necessity atribute.\n" 					+
								"Unfortunately, this is required to calculate the total waiting time of the graph.\n" 	+
								"At the moment, the handling of this case is not yet implemented,\n"					+
								"thus, some graphs might not be supported by the Evaluation-Tool.\n" 					+
								"We are currently working on this. For now, program will exit.");
        	
        	System.exit(-1);
        }
        
        // Get first entry of obj_attr_val_dict and split information about object and attribute
        String   obj_attr_key  = (String) obj_attr_val_dict.keySet().toArray()[0];
    	String[] obj_att_split = obj_attr_key.split("_");
    	
    	// Read information about the current object type and the current attribute type
    	String curr_obj  = obj_att_split[0];
    	String curr_attr = obj_att_split[1];
    	
    	// Read the attribute value for that attribute
    	int curr_attr_val = obj_attr_val_dict.get(obj_attr_key);
		
    	/*
    	 * First, check whether the object is an agent or a station.
    	 * Next, check whether the attribute is frequency or necessity.
    	 * Last, act accordingly to the configuration.
    	 */
    	
    	// Initialize visits(curr_agent, curr_station)
    	double visits = 0.0;
    	
    	/**
    	 *	Object type is "agent"
    	 */
    	if (curr_obj.equals("agent")) 
    	{	
    		// Check if attribute type is "frequency"
    		if (curr_attr.equals("frequency"))
    		{
    		
    			/**
    			 *  Calculate the visits according to the configuration
    			 *  
    			 *  	- visits( curr_agent, curr_station ) = frequency_value( curr_agent )  / sum{ count( connected_stations( curr_agent )
    			 */
    			
    			int curr_freq = curr_attr_val;
    			
    			visits = (double) curr_freq / (double) curr_agent.getSum_connected_stations();
    		}
    		else	// Attribute type is "necessity"
    		{
    			
    			/**
    			 *  Calculate the visits according to the configuration
    			 *  
    			 *  	- visits( curr_agent, curr_station ) = necessity_value( curr_agent )
    			 */
    			
    			int curr_nec = curr_attr_val;
    			    			
    			visits = (double) curr_nec;
    		}
    	}
    	
    	/**
    	 *	Object type is "station"
    	 */
    	else	
    	{
    		
       		// Check if attribute type is "frequency"
    		if (curr_attr.equals("frequency"))
    		{
    			
    			/**
    			 *  Calculate the visits according to the configuration
    			 *  
    			 *		- visits( curr_agent, curr_station ) = frequency_value( curr_station )  / sum{ count( connected_agents( curr_station )
    			 */
    			
    			int curr_freq = curr_attr_val;
    			
    			visits = (double) curr_freq  / (double) curr_station.getSum_connected_agents();
    		}
    		else	// Attribute type is "necessity"
    		{
    			
    			/**
    			 *  Calculate the visits according to the configuration
    			 *  
    			 *  	- visits( curr_agent, curr_station ) = necessity_value( curr_station )
    			 */
    			
    			int curr_nec = curr_attr_val;
    			
    			visits = (double) curr_nec;
    			
    		}
    	}

		return visits;
	}
}