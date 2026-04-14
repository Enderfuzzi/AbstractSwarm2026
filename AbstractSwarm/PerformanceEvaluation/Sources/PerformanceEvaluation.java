import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import utility.Agent;
import utility.Station;
import utility.utils;

/**
 * A performance evaluation monitor for AbstractSwarm agents.
 * 
 * @author Daan Apeldoorn
 *
 */
public class PerformanceEvaluation 
{
	
	/** The initial width of the window. */
	private static final int PERFORMANCE_EVAL_WINDOW_WIDTH = 400;

	/** The initial height of the window. */
	private static final int PERFORMANCE_EVAL_WINDOW_HEIGHT = 300;
	
	/** The width of the generated image. */
	private static final int PERFORMANCE_EVAL_IMAGE_WIDTH = 800;
	
	/** The height of the generated image. */
	private static final int PERFORMANCE_EVAL_IMAGE_HEIGHT = 600;
	
	/** The update interval of the window in milliseconds. */
	private static final int PERFORMANCE_EVAL_WINDOW_UPDATE_INTERVAL = 1000;
	
	/** The default number of runs when no configuration is provided through a .conf file. */
	private static final int PERFORMANCE_EVAL_DEFAULT_RUNS = 100;
	
	/** The default number of repetitions when no configuration is provided through a .conf file. */
	private static final int PERFORMANCE_EVAL_DEFAULT_REPS = 1;

	/** The window object. */
	private static JFrame performanceEvalWindow = null;

	/** The timer implementing the update interval. */
	private static Timer performanceEvalUpdateTimer = null;
	
	/** The timestamp of the timetables.log file's last modification. */
	private static long timetablesLogLastModified = -1;
	
	/** Path to simulation directory created by sim_agenda tool*/ 
	private static String simulation_folder_path = "";
	
	/** Window for debugging output pop-ups */
	static JFrame jFrame = new JFrame();

	/**
	 * The applications entry point.
	 * 
	 * @param args  the command line arguments ("final" can be provided to signal a last call for this evaluation)
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception 
	{	

		// Check whether this is intended to be the final performance evaluation call
		boolean is_final_call = false;
		
		// Iterate over all arguments given
		for( int i = 0; i < args.length; i++ )
		{
			
			// If signal indicating last call is given, change corresponding boolean to "true" to change work mode of the evaluation update function (which will for example store data and finish evaluation)
			if( args[ i ].equals( "final" ) )
			{
				
				is_final_call = true;
			}
			
			// Extract the name of the folder for the current simulation and generate its absolute path (located in the Results folder in the PerformanceEvaluation directory)
			else if ( args [ i ].startsWith( "_" ) )
			{
				
				String folder_name = args[ i ];
				simulation_folder_path = utils.generate_absolute_simulation_folder_path( folder_name );
			}
		}
		
		// If we are not on the last call for this evaluation 
		if( !is_final_call )
		{
			// Update on start up
			update_evaluation( false );

			// Create and run timer
			ActionListener actionListener = new ActionListener() 
			{
				
				public void actionPerformed( ActionEvent evt ) 
				{
					
					try 
					{
						
						update_evaluation( false );
					} 
					catch (Exception e) 
					{
						
						e.printStackTrace();
					}
				}
			};
			
			performanceEvalUpdateTimer = new Timer( PERFORMANCE_EVAL_WINDOW_UPDATE_INTERVAL, actionListener );
			performanceEvalUpdateTimer.start();
		}
		else
		{
			
			update_evaluation( true );
		}
	}
	
	
	/**
	 * update_evaluation
	 * Implements the timed update of the window.
	 * 
	 * @param is_final_call: boolean, signals a last call for this evaluation (e.g., to write an image of the plot)
	 * @throws Exception 
	 */
	private static void update_evaluation( boolean is_final_call ) throws Exception
	{
		
		// If there is no existing monitor window, create it
		if( performanceEvalWindow == null )
		{
			
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			
			performanceEvalWindow = new JFrame( "Agents Performance Evaluation Monitor" );
			performanceEvalWindow.setBackground( Color.white );
			performanceEvalWindow.setLocation( toolkit.getScreenSize().width / 2 - PERFORMANCE_EVAL_WINDOW_WIDTH / 2, toolkit.getScreenSize().height / 2 - PERFORMANCE_EVAL_WINDOW_HEIGHT / 2 );
			performanceEvalWindow.setResizable( true );
			performanceEvalWindow.setSize( PERFORMANCE_EVAL_WINDOW_WIDTH, PERFORMANCE_EVAL_WINDOW_HEIGHT );
		}
			    
		// Initialize array that will be used for storing the results (which in turn will be displayed in the monitor window)
		List<Double> results = new ArrayList<Double>();
		
		/**
		 * 	Find the folder of the current simulation
		 * 
		 * 	If the PerfEval-Monitor is not called for the last time (arg = "final"),
		 * 	it uses the global variable "simulation_folder_path" which is the name of the folder that the sim_agenda.bat created
		 * 	to store the results and the simulation history (simulation_history_file.txt)
		 * 
		 * 	The simulation_history_file.txt contains all the information about the current and the past simulation orders.
		 * 	This is needed because going through the timetables.log, we need to know which entries have to be normalized
		 * 	by which graphs total waiting time.
		 * 
		 * 	Therefore, as a first step, this history file is read and in the following step, the timetables' entries will
		 * 	be processed according to that information.
		 */
		
		// Initialize arrays storing the simulation information in the correct order
		List<String> list_of_agents = new ArrayList<String>();
		List<String> list_of_graphs = new ArrayList<String>();
		List<String> list_of_runs   = new ArrayList<String>();
		List<String> list_of_reps   = new ArrayList<String>();
		List<Double> list_of_twt    = new ArrayList<Double>(); 	// twt = total waiting time
		
		
		// Initialize variables (which will be updated in case the information is provided in the simulation configuration)
		int     curr_runs       = PERFORMANCE_EVAL_DEFAULT_RUNS; 
		int     curr_reps 	    = PERFORMANCE_EVAL_DEFAULT_REPS;	
		String  curr_agent_name = "Current selected agent";
	    
		// Find the current simulation order using the argument passed to the update_evaluation function
		try 
		{	
			
			// Get the path of the simulation history file (simulation_history_file.txt) in the simulation folder
			String simulation_history_path = Paths.get(simulation_folder_path, "sim_history.txt").toString();
			
			// Read the file from the generated path
			File simulation_history_file = new File ( simulation_history_path );
			
			// Iterate over the content in the simulation history file and extract the information for each simulation iteration (and store it in the corresponding arrays)
			Scanner file_reader = new Scanner( simulation_history_file );
					    
			while ( file_reader.hasNextLine() ) 
			{
				
				/**
				 * 	Information in the simulation_history_file.txt is stored like
				 * 
				 * 	agent,graph,runs,reps
				 * 	agent,graph,runs,reps
				 * 	...
				 */
				
				String[] current_configs = file_reader.nextLine().split( "," );
				
				list_of_agents.add( current_configs[0] );
				list_of_graphs.add( current_configs[1] );
				  list_of_runs.add( current_configs[2] );
				  list_of_reps.add( current_configs[3] );

			}
			
			// Close the file reader
			file_reader.close();
		    
			// Iterate over each graph found (there is no simulation without a graph specified) and compute the estimated worst case total waiting time
			for ( String graph: list_of_graphs )
			{
				
				try
				{
				
					Double graph_twt = get_estimated_worst_case_total_waiting_time( graph );	
					list_of_twt.add( graph_twt );
				}
				// If the total waiting time could not be computed correctly, raise an error
				catch (Exception e)
				{
					
					// Pop-up window
		        	JOptionPane.showMessageDialog( jFrame, 
								        			String.format( "Error: Could not compute TWT correctly!\n" +
								        						   "Current graph: '%s'",
								        						   graph
								        					      )
							        			  );
		        	
		        	// Write to console
		        	System.out.println( String.format( "Error: Could not compute TWT correctly!\n" +
								        			   "Current graph: '%s'",
								        			    graph
								        			  )
		        					   );
		        	
		        	// Raise error
		        	String exception = String.format("Could not compute TWT correctly!\nCurrent graph: '%s'", graph);
		        	throw new IOException( exception );
				}
			}
			
			// Update current configuration information (curr_agent, curr_graph, curr_runs, curr_reps)
			if ( list_of_runs.size() > 0 )
			{
				
				curr_runs = Integer.parseInt( list_of_runs.get( list_of_runs.size() - 1 ) );
			}
			
			if ( list_of_reps.size() > 0 )
			{
				
				curr_reps = Integer.parseInt( list_of_reps.get( list_of_reps.size() - 1 ) );
			}
			
			if ( list_of_agents.size() > 0 )
			{
				
				curr_agent_name = list_of_agents.get( list_of_agents.size() - 1 );
			}	
		}
		catch( FileNotFoundException e ) 
		{
		    
			// DEBUGGING
			JOptionPane.showMessageDialog( jFrame, String.format( "Error evaluating agent performance (sim_history.txt not found at: %s\\%s).", simulation_folder_path, "sim_history.txt" ) );
			
			System.out.println( String.format( "Error evaluating agent performance (sim_history.txt not found at: %s\\%s).", simulation_folder_path, "sim_history.txt" ) );
		} 
		catch( Exception e ) 
		{
			
			// DEBUGGING
			JOptionPane.showMessageDialog( jFrame, "Error evaluating agent performance." );
			
			System.out.println( "Error evaluating agent performance." );
		}
		
		/**
		 * 	Next up, read all current entries in the timetables.log
		 * 	
		 * 	The .log file is reset after every batch (all graphs have been simulated for one agent)
		 * 
		 * 	If there are 10 graphs, 1 agent, 100 runs, 20 repetitions simulated, there are 10 * 1 * 100 * 20 entries in that timetables.log
		 * 	
		 * 	Those values have to be normalized, but not with just one normalization value.
		 * 	Each graph was run with 100 runs and 20 repetitions, so each graph corresponds to 100 * 20 values in the timetables.log
		 * 	Those values have to be normalized with the graph's total waiting time.
		 * 	
		 * 	Therefore, the first 2000 values have to be normalized with the first graph's total waiting time, the next 2000 values with
		 * 	the total waiting time of the second graph and so on.
		 * 
		 * 	The strategy is to iterate through all the entries in the timetables.log in batches of runs * reps.
		 * 	Each iteration, the corresponding total waiting time is calculated. The normalized values are stored in the results array.
		 * 	
		 * 	In the end, the results array values will be displayed in the monitor.
		 */
		
		// Find the timetables.log file
		try
		{
			
			// Search timetables (depending from where the performance evaluation is started)
			String log_path = utils.getLogPath();
			
			File log_file = new File( log_path );
			
			// Only update the performance evaluation if the file was modified
			if( log_file.lastModified() == timetablesLogLastModified )
			{
				
				return;
			}
			
			timetablesLogLastModified = log_file.lastModified();
			
			// Read file line by line
			BufferedReader fileReader  = new BufferedReader( new FileReader( log_file ) );			
			String         readLine    = fileReader.readLine();
		
			List<Double>   log_entries = new ArrayList<Double>();
			
			// As long as there is another value (row) in the timetables.log
			while( readLine != null )
			{	
				
				// If there is a value, convert to number
				if( !readLine.equals( "" ) )
				{
					
					double value = Double.parseDouble( readLine );
										
					log_entries.add( value );
				}
				
				// ...else the maximum value possible is assumed. To mark those entries for later procesing,
				// the value is set to -1. (-1) is a value that can not occur in a regular simulation therefore
				// it will be always detected as that special case
				else
				{
					
					log_entries.add( -1.0 );

				}
				
				readLine = fileReader.readLine();
			}
			
			// Close the file reader for the .log file
			fileReader.close();
					    
			/**
			 * 	Right now we have a list of all the entries in the timetables.log where "" entries are now marked as -1
			 * 	
			 * 	In the next step, we will transform/normalize those values according to the corresponding graph they were simulated with
			 */
			
			// Batch_size: Number of entries in log_entries list that belong to one graph in the simulation history
			// For each simulation (for one agent), the number of runs and reps must be the same (otherwise we would have volatile batch sizes and
			// the simulation results for each graph can not be averaged (100 runs for graph1, 50 runs for graph2 --> no average for every value possible)
			
			for ( int k = 0; k < list_of_runs.size(); k++ )
			{
				
				int curr_runs_entry = Integer.parseInt( list_of_runs.get( k ) );
				int curr_reps_entry = Integer.parseInt( list_of_reps.get( k ) );
				
				if ( ( curr_runs_entry != curr_runs ) || ( curr_reps_entry != curr_reps ) )
				{
				    
					// DEBUGGING
					JOptionPane.showMessageDialog( jFrame, "Different number of runs or reps among entries in simulation_history_file.txt" );
					
					throw new IOException( "Different number of runs or reps among entries in simulation_history_file.txt" );
				}
			}
			
			/**
			 * 	We made sure, that every simulation that was run had the same configuration for runs and reps.
			 * 	Also, we checked if there are enough values in the timetables.log for every graph in the simulation history
			 * 
			 * 	Next, we will normalize the values in the log_entries list.
			 */
								
			
			/**
			 * 	We are iterating over every value in the .log file
			 * 	
			 * sim_order_idx: The current position in die simulation_history_fileory (e.g., line 3, meaning 3rd simulation order)
			 * run_idx: Current run index assigned to a value in the .log file (e.g., run 3 for the third value,
			 * 																    or 3 again for the 13th value when 
			 * 																	there are 10 runs each repetition)
			 */
			
			// Current position in the simulation_history_file.txt
			int sim_order_idx = 0;
			
			// Current run index for value in .log
			int run_idx       = 0;
			
			/**
			 * 	The HashMap results_of_each_sim is used to store the results for each simulation order
			 * 	to be able to produce a separate results data file for each line.
			 * 
			 * 	It contains the index of the simulation (3rd simulation means index of 3) and a List of this
			 * 	simulations results.
			 */
			
			HashMap<Integer, List<Double>> results_of_each_sim = new HashMap<Integer, List<Double>>();
			
			// Initialize results_of_each_sim with all simulation indices done in this batch (until next "final" call)
			for ( int e = 0; e < list_of_graphs.size(); e++ )
			{
				
				results_of_each_sim.put(e, new ArrayList<Double>());
			}
			
			// calc_log: Safes the calculations of the result values to make sure/ allow for check if there was the correct total waiting time used
			String calc_log = "\nsimulations: " + String.valueOf( list_of_graphs.size()) + "\n";
	        
			// some more content for the calc_log header
	        for (String graph: list_of_graphs)
	        {
	        	int idx = list_of_graphs.indexOf( graph );
	        	
	        	calc_log += "graph: " + graph + ",twt: " + (double) Math.round(list_of_twt.get( idx )) + ",runs: " + list_of_runs.get( idx) + ",reps: " + list_of_reps.get( idx ) + "\n";
	        	
	        }
	        
	        calc_log += "\n-----------------------------------------\n\n";
	        
	        // Iterate over the whole .log file
			for ( int i = 0; i < log_entries.size(); i ++ ) 
			{
				
				// If one mini-batch (one simulation order) was read, increase sim_order_idx to jump to the next simulation
				// meaning, for the following entries a different total waiting time will be used
				// Also, increase or reset the current run index
				if ( run_idx == (curr_runs * curr_reps) )
				{
					
					sim_order_idx += 1;
					run_idx = 1;
				}
				else
				{
					
					run_idx += 1;
				}
				
				// Read the current total waiting time for the graph of the current simulation
				double current_twt  = list_of_twt.get( sim_order_idx );
								
				// Read the current value in the log_entries list
				double curr_value = log_entries.get( i );
				
				// Check if value is special case (agents could not find a solution in the simulation)
				// This value was marked previously as "-1"
				if ( curr_value < 0 )
				{
					
					// If the value is a special case, the maximum normalized total waiting time value is assumed (1.0)
					curr_value = 1.0;
				}
				else
				{
					
					// If it is not a special case, the value must be normalized with the current graph's total waiting time
					curr_value = curr_value / current_twt;
				}
				
				// Add current calculation to the calc_log
				calc_log +=     "current log entry:\t" 	+ String.valueOf( i + 1) 					+ "\n"
							 +	"current value:\t\t"	+ String.valueOf( log_entries.get( i ) ) 	+ "\n"
							 +	"current normalized:\t"	+ String.valueOf( curr_value ) 				+ "\n"
							 +	"sim_order_idx:\t\t"	+ String.valueOf( sim_order_idx ) 			+ "\n"
							 +	"curr twt:\t\t\t" 		+ String.valueOf( current_twt )			+ "\n"
							 +  "-------------------\n";
    				
				// After normalization, the value is added to the results list
				results.add( curr_value );	
				
				/*
				 * 	To store the current value not only in the results list which is later used to create the data series in the plot,
				 * 	but to also store the value in the results_of_each_sim list to assign it to the correct simulation and to allow for
				 * 	creating a simulation-specific data file, read the current list of values for that simulation and append the new value
				 * 	to it.
				 */
				
				// Read current list of values for current simulation
				List<Double> curr_list = results_of_each_sim.get( sim_order_idx );
				
				// Add value to temporary list
				curr_list.add( curr_value );
									
				// Append list in HashMap
				results_of_each_sim.put(sim_order_idx, curr_list );
			}	
			
			// If simulation encountered the end of a batch or the end of the agenda
			if (is_final_call)
			{
				
				// For every simulation stored, create a data file
				for ( Integer key: results_of_each_sim.keySet() )
				{			
					
					// Initialize data content memory
					String raw_log_content = "";
					
					// Read the data stored for the current simulation
					List<Double> current_data = results_of_each_sim.get( key );
					
					// Check if there is enough data for all runs (100 data, 10 runs -> % == 0
					//												99 data, 10 runs -> % != 0)
					if ( !( current_data.size() % curr_runs == 0 ) )
					{
						
						// DEBUGGING
						JOptionPane.showMessageDialog( jFrame, "Not enough data for current simulation's data file." );	
						
						throw new IOException("Not enough data for current simulation!");
					}
					
					double[] graph_data 	= new double[ curr_runs ]; 
					int[]    run_pairs 	= new int[ curr_runs ]; 
					int      curr_idx   = 0;
					double   curr_min_value = Double.MAX_VALUE;
					
					
					// Iterate over the values 
					// Iterate until index == curr_runs, because we want to average those values
					// For each repetition, run_1 will be averaged with all other run_1 of the other repetitions
					for ( int v = 0; v < current_data.size(); v++ )
					{
						
						double current_value = current_data.get( v );
						
						
						// Get the current minimal total waiting time found so far in this repetition
						if( curr_min_value > current_value )
						{
							
							curr_min_value = current_value;
						}
						
						// Add it to the series data and count values (will be averaged afterwards)
						graph_data[ curr_idx ] += curr_min_value;
						run_pairs[ curr_idx ]  += 1;

						// Increment index
						curr_idx += 1;
						
						if( curr_idx >= curr_runs )
						{
							
							curr_idx = 0;
							curr_min_value = Integer.MAX_VALUE;
						}
					}
					
					// Create averages
					
					Double area_graph = 0.0;
					
					for( int i = 0; i < graph_data.length; i++ )
					{
						
						graph_data[ i ] /= ((double) run_pairs[ i ]);
						area_graph += graph_data[ i ];
						
						// Save minimum to data file content
						raw_log_content += String.valueOf( graph_data [ i ] ) + "\n";
					}
						
					// Set the path for the data file for each simulation
					String raw_log_path = Paths.get( simulation_folder_path,
													 String.format( "data_Agent_%s__Graph_%s__Runs_%s_Reps_%s__Area_%s.log", 
															 		 list_of_agents.get( 0 ),
															 		 list_of_graphs.get( key ).split( ".xml" )[ 0 ],
																	 list_of_runs.get( key ),
																	 list_of_reps.get( key ),
																	 String.valueOf( area_graph ) 
																   )
													).toString();
					
					// Save data file for simulation
					try
					{
				        BufferedWriter calc_log_writer = new BufferedWriter( new FileWriter( raw_log_path, false ) );
	
				 
				        // Writing into the file
				        calc_log_writer.write( raw_log_content );
				        calc_log_writer.close();
					}
					catch (IOException e)
					{
						
						// DEBUGGING
						JOptionPane.showMessageDialog( jFrame, "Error saving current simulation's data file." );	
					
						System.out.print( e.getMessage() );
					}
					
				}
			
				int batch_idx = 1;
				
				String calc_log_file = Paths.get( simulation_folder_path, String.format( "calc_log_batch_%d.txt", batch_idx ) ).toString();
				
				File f = new File( calc_log_file );
				
				while (f.exists() && !f.isDirectory() )
				{
					
					batch_idx += 1;
					
					calc_log_file = Paths.get( simulation_folder_path, String.format( "calc_log_batch_%s.txt", batch_idx ) ).toString();
					f = new File( calc_log_file );
				}
				
				try 
				{
			        
			        BufferedWriter calc_log_writer = new BufferedWriter( new FileWriter( calc_log_file, false ) );

			 
			        // Writing into the file
			        calc_log_writer.write( calc_log );
			        
			        calc_log_writer.close();
				}
				catch (IOException e)
				{
					
					System.out.print( e.getMessage() );
				}
			}
		}
		catch( FileNotFoundException e ) 
		{
			
			// DEBUGGING
			JOptionPane.showMessageDialog( jFrame, "Error evaluating agent performance (timetables.log not found)." );
			
			System.out.println( "Error evaluating agent performance (timetables.log not found)." );
		} 
		catch( IOException e ) 
		{
			
			// DEBUGGING
			JOptionPane.showMessageDialog( jFrame, "Error evaluating agent performance." );
			
			System.out.println( "Error evaluating agent performance." );
		}
	    
		/**
		 * 	At this point, we got a results list with all the normalized values.
		 * 
		 * 	The final goal is to present those values in the PerformanceEvaluation monitor.
		 */
		
		// Transform the data to be plotted
		double[] seriesData 				= new double[ curr_runs ]; 
		int[]    seriesDataNumberOfValues 	= new int[ curr_runs ]; 
		int      currentIndex 				= 0;
		double   currentMinTotalWaitingTime = Double.MAX_VALUE;
		
		// Iterate over every entry in results list
		for( int i = 0; i < results.size(); i++ )
		{
			
			// Get the current minimal total waiting time found so far in this repetition
			if( currentMinTotalWaitingTime > results.get( i ) )
			{
				
				currentMinTotalWaitingTime = results.get( i );
			}
			
			// Add it to the series data and count values (will be averaged afterwards)
			seriesData[ currentIndex ] += currentMinTotalWaitingTime;
			seriesDataNumberOfValues[ currentIndex ] += 1;

			// Increment index
			currentIndex += 1;
			
			if( currentIndex >= curr_runs )
			{
				
				currentIndex = 0;
				currentMinTotalWaitingTime = Integer.MAX_VALUE;
			}
		}
		
		// Create averages
		for( int i = 0; i < seriesData.length; i++ )
		{
			
			seriesData[ i ] /= ((double) seriesDataNumberOfValues[ i ]);
		}
		
		// Create the data series to be plotted
		XYSeries series = new XYSeries( curr_agent_name );
		
		for( int i = 0; i < (int) Math.min( results.size() , seriesData.length ); i++ )
		{
			
			series.add( i + 1, seriesData[ i ] );
		}
			
		// Add the data series to the series collection representing the plot
		XYSeriesCollection seriesCollection = new XYSeriesCollection();
		seriesCollection.addSeries( series );
		
		// Create a chart rendering the plot and add it to the window
		JFreeChart chart = ChartFactory.createXYLineChart( "Avg. Best Total Waiting Time after i Runs\n( repetitions: " + curr_reps + " | runs: " + curr_runs + " )",
														   "Run i",
														   "Avg. best total waiting time", 
														   seriesCollection,
														   PlotOrientation.VERTICAL,
														   true,
														   true,
														   false );
		
		ChartPanel chartPanel = new ChartPanel( chart );
		chartPanel.setPreferredSize( new java.awt.Dimension( PERFORMANCE_EVAL_WINDOW_WIDTH , PERFORMANCE_EVAL_WINDOW_HEIGHT ) );
		performanceEvalWindow.setContentPane( chartPanel );
		
		// If not a final call for the evaluation...
		if( !is_final_call )
		{
			
			// Set the window visible (again)
			performanceEvalWindow.setVisible( true );
		}
		
		// ...else, if final call, calculate area, write image and data, etc.
		else
		{
			// Calculate area
			double area = 0;
			
			for( int i = 0; i < seriesData.length; i++ )
			{
				
				area += seriesData[ i ];
			}
			
			// Write chart to file
			File chartFile = new File(	 
										 simulation_folder_path      	+ "/Plot_"			+ curr_agent_name	+ "_" 
												 		+ "Repetitions_"	+ curr_reps		  	+ "_"
												 		+ "Runs_" 			+ curr_runs 	  	+ "_" 
												 		+ "Area_" 			+ area 		      	+ ".jpeg" 
									   
									  ); 

			try 
			{
				
				ChartUtilities.saveChartAsJPEG( chartFile, chart, PERFORMANCE_EVAL_IMAGE_WIDTH, PERFORMANCE_EVAL_IMAGE_HEIGHT );
			} 
			catch( IOException e )
			{
			    
				// DEBUGGING
				JOptionPane.showMessageDialog( jFrame, "Error writing chart file." );
				
				System.out.println( "Error writing chart file." );
			}		
			
			//
			// Write data to file
			//			
			File dataFile = new File(	
										simulation_folder_path 		 + "/Data_"			+ curr_agent_name 	+ "_" 
														 + "Repetitions_"   + curr_reps			+ "_"
														 + "Runs_" 			+ curr_runs 		+ "_" 
														 + "Area_" 			+ area 				+ ".txt" 
									  
									 ); 
		    
			BufferedWriter file = null;
		    
			try 
			{
			    
				file = new BufferedWriter( new FileWriter( dataFile, false ) );

				for( int i = 0; i < seriesData.length; i++ )
				{
					
					file.write( seriesData[ i ] + "\n" );
				}
			} 
			catch( IOException e ) 
			{
				
				// DEBUGGING
				JOptionPane.showMessageDialog( jFrame, "Error writing data file." );
				
				System.out.println( "Error writing data file." );
			} 
		    
			// Close the file
			if( file != null )
			{
				
				try 
				{
					
					file.close();
				} 
				catch( IOException e )
				{
					
					// DEBUGGING
					JOptionPane.showMessageDialog( jFrame, "Error closing data file." );
					
					System.out.println( "Error closing data file." );
				}
			}
			
			// Audio signal when done
			Toolkit.getDefaultToolkit().beep();
		}
	}	
	
	/**
	 * Function: get_estimated_worst_case_total_waiting_time
	 * 
	 * Returns the estimated worst case total waiting time for the provided scenario graph.
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 * @param graph_name  the scenario graph for which the estimated worst case total waiting time is returned
	 * @return           the estimated worst case total waiting time for the provided graph
	 */
	public static double get_estimated_worst_case_total_waiting_time( String graph_name ) throws IOException, JDOMException
	{
		
	    // Read the graph's name and compute it's path
	    String graph_path = utils.get_absolute_graph_path( graph_name );	   

	    // Prepare tools to read .xml file content
	    File 		graph_file 		= new File( graph_path );
	    SAXBuilder 	saxBuilder 		= new SAXBuilder();
	    Document 	document 		= saxBuilder.build( graph_file );
	    Element 	root_element 	= document.getRootElement(); // Root element of the XML file; required to find children objects that are of interest

	    // Get all perspectives and edges in the graph
	    List<Element> xml_perspectives	= root_element.getChildren( "perspective ");
	    List<Element> xml_place_edges 	= root_element.getChildren( "placeEdge" );
	    List<Element> xml_time_edges 	= root_element.getChildren( "timeEdge" );
	    List<Element> xml_visit_edges   = root_element.getChildren( "visitEdge" );
	    
	    // Create empty hashmap for all agents and stations in the graph to access them later / find them by "id"
	    HashMap<Integer, Agent>   obj_agents   = new HashMap<Integer, Agent>();
	    HashMap<Integer, Station> obj_stations = new HashMap<Integer, Station>();
	    
	    // Reserve memory for agents and stations ids to simplify accessing corresponding lists
	    Set<Integer> agent_ids   = obj_agents.keySet();
	    Set<Integer> station_ids = obj_stations.keySet();
	    	    
	    // Reserve memory for each perspective's max_travel(a, s)	   
	    HashMap<Integer, Integer> max_travel = new HashMap<Integer, Integer>();	// Hashmap<perspective_id, max_travel>
	    
	    /**
	     *  Iterate over every perspective in the graph
	     */
	    for (Element xml_curr_perspective: xml_perspectives)
	    {
	    	    	
	    	// Get current perspective id
	    	int curr_perspective_id = Integer.parseInt(xml_curr_perspective.getAttributeValue("id"));
	    	
	    	// Get all agents and stations the current perspective contains
		    List<Element> xml_curr_agents 	 = xml_curr_perspective.getChildren("agentType");
		    List<Element> xml_curr_stations  = xml_curr_perspective.getChildren("stationType");
		    
		    // Initialize max_travel for each perspective
		    max_travel.put(curr_perspective_id, 0);	// Perspectives with no place edges receive max_travel value of '0'
		   			    
		    /**
		     *  Iterate over every agent found in the current perspective
		     */
		    for (Element xml_curr_agent: xml_curr_agents) {
		    			    	
		    	// Create new agent object
		    	Agent obj_curr_agent = new Agent();
		    	
		    	// Read attribute values from XML into new agent
		    	obj_curr_agent.readFromXml(xml_curr_agent, curr_perspective_id);
		    			    	
		    	// Save current agent to agents list
		    	obj_agents.put(obj_curr_agent.getId(), obj_curr_agent);
		    }
		    
		    /**
		     *  Iterate over every station found in the current perspective
		     */
		    for (Element xml_curr_station: xml_curr_stations) {
		    	
		    	// Create new station object
		    	Station obj_curr_station = new Station();
		    	
		    	// Read attribute values from XML into new station
		    	obj_curr_station.readFromXml(xml_curr_station, curr_perspective_id);
		    	
		    	// Save current station to stations list
		    	obj_stations.put(obj_curr_station.getId(), obj_curr_station);
		    }
	    }
	    	    
	    /**
	     *  Iterate over every visit edge to find connections between agents and stations
	     */
	    for (Element xml_curr_visit_edge: xml_visit_edges)
	    {
	    	
	    	// Get the id of the current visit edge
	    	int curr_visit_edge_id = Integer.parseInt( xml_curr_visit_edge.getAttributeValue( "id" ) );
	    	
	    	// Each visit edge connects one agent to one station 
	    	int curr_connIdRef1 = Integer.parseInt( xml_curr_visit_edge.getAttributeValue( "connectedIdRef1" ) );	// This should be the id of the station connected
	    	int curr_connIdRef2 = Integer.parseInt( xml_curr_visit_edge.getAttributeValue( "connectedIdRef2" ) );	// This should be the id of the agent connected
	    	
	    	// Check if agent can be found using either RefId1 or RefId2
	    	if (obj_agents.get( curr_connIdRef1 ) == null && obj_agents.get( curr_connIdRef2 )== null )
	    	{
	    		
				// DEBUGGING
				JOptionPane.showMessageDialog( jFrame, "Error: Current time edge's agent could not be found.\n" +
														String.format( "Current visit edge id: %d\n", curr_visit_edge_id ) +
														"Agent id should match 'connectedIdRef1'.\n" +
														"Please check .xml file.");
				
				System.out.println( "Error: Current time edge's agent could not be found.\n" +
									String.format( "Current visit edge id:\t%d\n", curr_visit_edge_id ) +
									"Agent id should match 'connectedIdRef1'.\n" +
									"Please check .xml file.");
				
	        	// Raise error
	        	throw new IOException( "Could not find agent by either RefId1 (correct way) or RefId2.");
	    	}
	    	
	    	// Check if station can be found using either RefId1 or RefId2
	    	if (obj_stations.get( curr_connIdRef2 ) == null && obj_stations.get( curr_connIdRef1 ) == null)
	    	{
	    		
				// DEBUGGING
				JOptionPane.showMessageDialog( jFrame, "Error: Current time edge's station could not be found.\n" +
														String.format( "Current visit edge id: %d\n", curr_visit_edge_id ) +
														"Station id should match 'connectedIdRef2'.\n" +
														"Please check .xml file.");
	    		
				System.out.println( "Error: Current time edge's station could not be found.\n" +
									String.format( "Current visit edge id:\t%d\n", curr_visit_edge_id ) +
									"Station id should match 'connectedIdRef2'.\n" +
									"Please check .xml file.");
				
	        	// Raise error
	        	throw new IOException( "Could not find station by either RefId1 or RefId2 (correct way).");
	    	}
	    	
	    	/**
	    	 * If the algorithm reaches this point, that means, for the current visit edge, there is one agent and one station that can be found
	    	 */
	    	
	    	Integer curr_agent_id    = null;
	    	Integer curr_agent_count = null;
	    	
	    	Integer curr_station_id    = null;
	    	Integer curr_station_count = null;
	    	
	    	// If the agents id matches the IdRef1 (normal case)
	    	if (obj_agents.get( curr_connIdRef1 ) != null )
	    	{
	    		
	    		// Read the agents information
	    		curr_agent_id    = Integer.parseInt( xml_curr_visit_edge.getAttributeValue( "connectedIdRef1" ) );
	    		curr_agent_count = obj_agents.get( curr_connIdRef1 ).getCount();
	    		
	    		// Read the stations information
	    		curr_station_id    = Integer.parseInt( xml_curr_visit_edge.getAttributeValue( "connectedIdRef2" ) );
	    		curr_station_count = obj_stations.get( curr_connIdRef2 ).getCount();

	    	}
	    	// In case, agent id matches IdRef2 (station / agent id confused in xml file)
	    	else
	    	{
	    		
				JOptionPane.showMessageDialog( jFrame, "Warning: Current time edge's 'connectedIdRef1' should match the agent's id - not the stations id.\n" +
														String.format( "Current visit edge id:\t%d\n", curr_visit_edge_id ) +
														"Agent could be found using 'connectedIdRef2'.\n" +
														"You might want to consider modifying the .xml file.");
	    		
				System.out.println( "Warning: Current time edge's 'connectedIdRef1' should match the agent's id - not the stations id.\n" +
									String.format( "Current visit edge id: %d\n", curr_visit_edge_id ) +
									"Agent could be found using 'connectedIdRef2'.\n" +
									"You might want to consider modifying the .xml file.");
				
	    		// Read the agents information
	    		curr_agent_id    = Integer.parseInt( xml_curr_visit_edge.getAttributeValue( "connectedIdRef2" ) );
	    		curr_agent_count = obj_agents.get( curr_connIdRef2 ).getCount();
	    		
	    		// Read the stations information
	    		curr_station_id    = Integer.parseInt( xml_curr_visit_edge.getAttributeValue( "connectedIdRef1" ) );
	    		curr_station_count = obj_stations.get( curr_connIdRef1 ).getCount();
	    	}
	    
	    	/**
	    	 * Dealing with the connected stations for the current agent
	    	 */
	    	
	    	// Read already connected stations of the current agent
	    	HashMap<Integer, Integer> curr_connected_stations = obj_agents.get(curr_agent_id).getConnected_stations();
	    	
	    	// Add the current station_id and current station_count to the connected stations
	    	curr_connected_stations.put(curr_station_id, curr_station_count);
	    	
	    	// Update the currently connected stations of the agent
	    	obj_agents.get(curr_agent_id).setConnected_stations(curr_connected_stations);
	    	
	    	/**
	    	 * Dealing with the connected agents for the current station
	    	 */
	    	
	    	// Read already connected agents of the current station
	    	HashMap<Integer, Integer> curr_connected_agents = obj_stations.get(curr_station_id).getConnected_agents();
	    	
	    	// Add the current agent_id and current agent_count to the connected agents
	    	curr_connected_agents.put(curr_agent_id, curr_agent_count);
	    	
	    	// Update the currently connected agents of the station
	    	obj_stations.get(curr_station_id).setConnected_agents(curr_connected_agents);
	    }
	    
	    /**
	     *  Iterate over time edges to read their time values and find the stations connected to it
	     */
	    for (Element xml_curr_time_edge: xml_time_edges)
	    {
	    	
	    	// Read information about the time edge itself
	    	int curr_time_edge_id = Integer.parseInt(xml_curr_time_edge.getAttributeValue("id")); 		// Id of current time edge
	    	int curr_time         = Integer.parseInt(xml_curr_time_edge.getAttributeValue("value"));	// Time value of current time edge
	    	
	    	/*
	    	 * There can be either two stations or one station and one agent be connected by a time edge
	    	 * In any case, only the station types are of interest
	    	 * That is because the total waiting time formula uses the maximum number of time units an agent has to wait
	    	 * according to time edges before the station it tries to visit (highest weight of all time edges connected to that station)
	    	 * Therefore, we need to identify the objects first and act accordingly to their type
			 */
	    	int curr_obj_id1 = Integer.parseInt(xml_curr_time_edge.getAttributeValue("connectedIdRef1"));	// Id of first object connected by the time edge
	    	int curr_obj_id2 = Integer.parseInt(xml_curr_time_edge.getAttributeValue("connectedIdRef2"));	// Id of second object connected by the time edge
	    	
	    	// Reserve memory for the information about already connected time edges to one/both of the objects
	    	HashMap<Integer, Integer> curr_connected_time_edges = new HashMap<Integer, Integer>();
	    	
	    	// Check if type of first object is 'station'
	    	if (station_ids.contains(curr_obj_id1))
	    	{
	    		
	    		// Read already connected time edges of current station
	    		curr_connected_time_edges = obj_stations.get(curr_obj_id1).getConnectedTimeEdges();
	    		
	    		// Add new time edge to the currently connected time edges
	    		curr_connected_time_edges.put(curr_time_edge_id, curr_time);
	    		
	    		// Update the connected time edges attribute of the current station
	    		obj_stations.get(curr_obj_id1).setConnectedTimeEdges(curr_connected_time_edges);
	    	}
	    	
	    	// Check if type of second object is 'station'
	    	if (station_ids.contains(curr_obj_id2))
	    	{
	    		
	    		// Read already connected time edges of current station
	    		curr_connected_time_edges = obj_stations.get(curr_obj_id2).getConnectedTimeEdges();
	    		
	    		// Add new time edge to the currently connected time edges
	    		curr_connected_time_edges.put(curr_time_edge_id, curr_time);
	    		
	    		// Update the connected time edges attribute of the current station
	    		obj_stations.get(curr_obj_id2).setConnectedTimeEdges(curr_connected_time_edges);
	    	}
	    }
	    	    
	    /**
	     *  Iterate over every place edge to calculate max_travel for each perspective
	     */
	    for (Element xml_curr_place_edge: xml_place_edges) 
	    {
	    	
	    	/*
	    	 * Because the place edge itself does not contain information about the perspective that contains it,
	    	 * we need to find the current perspective id using the objects that are connected by that place edge.
	    	 * To accomplish that, we only need to use on of the two objects that are connected by that place edge.
	    	 */
	    	
	    	// Reserve memory for the current perspective id, set default to an invalid value (-1, helps debugging)  	
	    	int curr_perspective_id = -1;
	    	
	    	// Read the id of the first object connected by the current place edge
	    	int id_of_connected_obj = Integer.parseInt(xml_curr_place_edge.getAttributeValue("connectedIdRef1"));
	    	
	    	/* 
	    	 * Next up, we need to find the object id in either the agents or the stations ids.
	    	 * Therefore we check which of those lists contains that id.
	    	 */
	    	if (agent_ids.contains(id_of_connected_obj))	// The object is an agent
	    	{
	    		
	    		// Update the current perspective id
	    		curr_perspective_id = obj_agents.get(id_of_connected_obj).getPerspective_id();
	    	}
	    	else	// The object is a station
	    	{
	    		
	    		// Update the current perspective id
	    		curr_perspective_id = obj_stations.get(id_of_connected_obj).getPerspective_id();
	    	}
	    	
	    	// Read the actual place value of the place edge
	    	int curr_place_value    = Integer.parseInt(xml_curr_place_edge.getAttributeValue("value"));
	    	
	    	/*
	    	 * In a final step, we need to update the max_travel value of the current perspective.
	    	 * Each place edge contributes to the max_travel value.
	    	 * First, we read the current max_travel value and then add the current place value to it.
	    	 * Finally, we update the max_travel value for the current perspective.
	    	 */
	    	
	    	// Reserve memory for already existing information about max_travel value in the current perspective
	    	int curr_max_travel     = 0;
	    	
	    	// Check if there is already existing information about max_travel value
	    	if (max_travel.get(curr_perspective_id) != null)
	    	{
	    		
	    		// Read the already existing information about max_travel value
	    		curr_max_travel = max_travel.get(curr_perspective_id);
	    	}
	    	
	    	// Update the max_travel value for the current perspective
	    	max_travel.put(curr_perspective_id, curr_max_travel + curr_place_value);
	    }
	    	    
	    /**
	     *  Calculate the total waiting time (in three parts)
	     *  
	     *  - first part:
	     *  	sum_agents [ sum_stations [ (count( all_agents ) - space( curr_station ) * visits( curr_agent, curr_station ) / space( curr_station ) * max_time( curr_agent, curr_station ) ] ]
	     *  
	     *  - second part:
	     *  	sum_Agents [ sum_stations [ visits( curr_agent, curr_station ) * max_travel(curr_perspective) ] ]
	     *  
	     *  - third part:
	     *  	sum_agents [ sum_stations [ visits( curr_agent, curr_station ) * max_waiting_time( curr_agent( connected_stations ) ] ]
	     */
	    
	    // Reserve memory for total waiting time value
	    double total_waiting_time = 0.0;
	    
	    // Reserve memory for number of all agent instances in the current graph
	    int total_agent_count = 0;
	    
	    // Compute number of all agent instances in the current graph
	    for (Agent curr_agent: obj_agents.values())
	    {
	    	
	    	total_agent_count += curr_agent.getCount();
	    }

	    // DEBUGGING
	    double sum_twt_part_1 = 0.0;
	    double sum_twt_part_2 = 0.0;
	    double sum_twt_part_3 = 0.0;
	    //int    counter        = 0;
	    
	    // DEBUGGING
	    //String final_calculation_twt_part_1 = "final calculation of twt_part_1:\n\n";
	    
	    /**
	     * Actual calculation of the total waiting time
	     */
	    // Iterate over every agent object that was found in the graph
	    for (Agent curr_agent: obj_agents.values())
	    {	

	    	// Read attributes of the current agent required by the first part of the equation
	    	Integer curr_agent_count = curr_agent.getCount();
	    	Double  curr_agent_time  = curr_agent.getTime();  
	    	Double  curr_agent_size  = curr_agent.getSize();

	    	// Reserve memory for all the stations that are connected to the current agent
	    	List<Station> curr_conn_stations = new ArrayList<Station>();
	    	
	    	// Read all the currently connected stations
	    	for (Integer conn_station_id: curr_agent.getConnected_stations().keySet())
	    	{
	    		
	    		curr_conn_stations.add(obj_stations.get(conn_station_id));
	    	}
	    	
	    	// Get current perspective
	    	Integer curr_perspective_id = curr_agent.getPerspective_id();
	    	
	    	// Iterate over every agent instance of the current agent object
	    	for (int agent_inst_nr = 0; agent_inst_nr < curr_agent_count; agent_inst_nr++)
	    	{
	    		
	    		// Iterate over every station object that is connected to the current agent object
	    		for (Station curr_station: curr_conn_stations)
	    		{
	    			
			    	// Read attribute of the current station required by the first part of the equation
	    			Integer curr_station_count 			  = curr_station.getCount();
		    		Double  curr_station_space 			  = curr_station.getSpace();
	    			Double  curr_station_time			  = curr_station.getTime();
		    		Integer curr_station_max_waiting_time = curr_station.getMaxWaitingTime();
	    			
	    			/*
	    			 * Up next: Dealing with the different scenarios for curr_agent_size and curr_station_space
	    			 * 
	    			 * If the curr_station has no space attribute, that means that the current station is able
	    			 * to process any amount of agents at the same time. Thus, this station (and it's instances)
	    			 * do only contribute to the total waiting time in case the curr_agent does not posses a size attribute.
	    			 * In this case, the size of the curr_agent would be 'infinite'. 
	    			 * In case the curr_station has unlimited space and the agent is of infinite size, the assumption is made
	    			 * that the station can handle exactly one station at a time.
	    			 * 
	    			 * In case the curr_agent possesses a size attribute and the curr_station has unlimited space, the station (and it's instances)
	    			 * do not contribute to the total waiting time and the first part of the total waiting time equation should not be calculated.
	    			 * This is done by setting the sim_visits variable (required in the formula) is set to '-1.0', indicating that exception. 
	    			 */
	    			
	    			// DEBUGGING
	    			//String sim_visits_calculation = "";
	    			
	    			/*
	    			 * Default case:
	    			 * 	- curr_agent's size and curr_station's space are not limited
	    			 * 	- sim_visits = -1.0
	    			 */
	    			double sim_visits = -1.0;
	    				    			
	    			// In case the agent's size is not limited, a station (no matter it's space) can only handle one agent at a time
	    			if (Double.isInfinite(curr_agent_size))
	    			{
	    				
	    				sim_visits = 1.0;

	    				// DEBUGGING
	    				//sim_visits_calculation = String.format("calculation of sim_visits = %.2f\n", sim_visits);	
	    			}
	    			else if (!Double.isInfinite(curr_station_space))
	    			{
	    				
	    				sim_visits = curr_station_space / curr_agent_size;
	    				
						// DEBUGGING
						/*sim_visits_calculation = String.format("\ncalculation of sim_visits = %.2f / %.2f\n",
															   curr_station_space,
															   curr_agent_size);*/
	    			}    	
	    			
		    		// Read minimum of the time values of the agent and the station
		    		Double curr_time = Math.min(curr_agent_time, curr_station_time);
	    				    		
		    		// Amount of times one agent instance has to visit one instance of a connected station
		    		double curr_visits = utils.getVisits(graph_name, curr_agent, curr_station);
		    		
		    		// Iterate over every station instance of the current station object
		    		for (Integer station_inst_nr = 0; station_inst_nr < curr_station_count; station_inst_nr++)
		    		{
		    			
		    			/**
		    			 * First part of the total waiting time equation
		    			 */
		    			
		    			// Reserve memory for the calculation fo the first part of the total waiting time equation
		    			double twt_part_1 = 0.0;	    			
	    				
	    				/*
	    				 *  In case the curr_station and the curr_agent are contributing to the total waiting time,
	    				 *	calculate the first part of the total waiting time equation.
	    				 */
		    			if (sim_visits != -1.0)
		    			{
			    			
		    				// Calculate the first part of the total waiting time equation
			    			twt_part_1 = ( total_agent_count - sim_visits ) * (curr_visits / sim_visits) * curr_time;
			    			
			    			// DEBUGGING
				    		sum_twt_part_1 = sum_twt_part_1 + twt_part_1;
		    			}
		    			
		    			/**
		    			 * Second part of the total waiting time equation
		    			 */
		    			
		    			// Calculate the second part of the total waiting time equation
		    			double twt_part_2 = (curr_visits * max_travel.get(curr_perspective_id));
		    			
		    			// DEBUGGING
			    		sum_twt_part_2 = sum_twt_part_2 + twt_part_2;
		    			
		    			/**
		    			 * Second part of the total waiting time equation
		    			 */
			    		
			    		// Calculate the third part of the total waiting time equation
		    			double twt_part_3 = (curr_visits * curr_station_max_waiting_time);
		    			
		    			// DEBUGGING
			    		sum_twt_part_3 = sum_twt_part_3 + twt_part_3;
		    			
			    		// Add all parts to total waiting time
			    		total_waiting_time = total_waiting_time + twt_part_1 + twt_part_2 + twt_part_3;	
			    		
		    			/**
		    			 * DEBUGGING / LOGGING CALCULATION AND RESULTS (delete if not needed anymore, helpful to understand calculation of TWT)
		    			 * Remember to uncomment counter initialization around line 1216
		    			 */
			    		
		    			/*
		    			counter += 1;
		    			
		    			System.out.println("\n-------------------------------------");
		    			System.out.println(String.format("%s - twt update nr:\t%d\n", graph_name, counter));
		    			
		    			System.out.println(String.format("current twt:\t\t\t%.2f\n", total_waiting_time));

		    			System.out.println(String.format("current agent id:\t\t%d",         curr_agent.getId()));
		    			System.out.println(String.format("current agent instance:\t\t%d\n", agent_inst_nr + 1));

		    			System.out.println(String.format("current station id:\t\t%d",       curr_station.getId()));
		    			System.out.println(String.format("current station instance:\t%d\n", station_inst_nr + 1));
		    			
		    			System.out.println("  Part 1 ---------------\n");
		    			
		    			System.out.println(String.format("total_agent_count:\t\t%d",  	total_agent_count));
		    			System.out.println(String.format("curr_agent_size:\t\t%.2f",  	curr_agent_size));
		    			System.out.println(String.format("curr_station_space:\t\t%.2f", curr_station_space));
		    			System.out.println(String.format("sim_visits:\t\t\t%.2f", 	  	sim_visits));
		    			System.out.println(String.format("curr_visits:\t\t\t%.2f",    	curr_visits));
		    			System.out.println(String.format("curr_time:\t\t\t%.2f",      	curr_time));

		    			System.out.println(sim_visits_calculation);
		    			
		    			if (sim_visits != -1.0)
		    			{
			    			System.out.println("formula:\t\t\t(total_agent_count - sim_visits) * curr_visits / sim_visits * curr_time = twt_part_1");
			    			System.out.println(String.format("calculation:\t\t\t(%d - %.2f) * %.2f / %.2f * %.2f = %.2f\n", total_agent_count,
																			    										    sim_visits,
																			    										    curr_visits,
																			    										    sim_visits,
																			    										    curr_time, twt_part_1));
			    			
			    			final_calculation_twt_part_1 += String.format("\t\t(+%.2f) --> (%.2f)  \t||\t+ ", twt_part_1, sum_twt_part_1);
			    			
			    			final_calculation_twt_part_1 += String.format("[(%d - %.2f) * %.2f / %.2f * %.2f]\n", total_agent_count,
			    																								  sim_visits, 
			    																								  curr_visits, 
			    																								  sim_visits,
			    																								  curr_time);
			    		
		    			}
		    			else
		    			{
		    				
		    				final_calculation_twt_part_1 += String.format("\t\tCalculation skipped\t\t\t\\\\ (%.2f) --> (%.2f)\n", twt_part_1, sum_twt_part_1);
		    				System.out.println("calculation:\t\t\tCalculation skipped.\n");
		    			}
		    			
		    			System.out.println(String.format("current part_1 of twt:\t\t%.2f", twt_part_1));
		    			System.out.println(String.format("total part_1 of twt:\t\t%.2f\n", sum_twt_part_1));

		    			System.out.println("  Part 2 ---------------\n");
		    			
		    			System.out.println(String.format("curr_visits:\t\t\t%.2f",   curr_visits));
		    			System.out.println(String.format("curr_max_travel:\t\t%d\n", max_travel.get(curr_perspective_id)));
		    			
		    			System.out.println("formula:\t\t\tcurr_visits * curr_max_travel = twt_part_2");
		    			System.out.println(String.format("calculation:\t\t\t%.2f * %d = %.2f\n", curr_visits, max_travel.get(curr_perspective_id), twt_part_2));
		    			
		    			System.out.println(String.format("current part_2 of twt:\t\t%.2f", twt_part_2));
		    			System.out.println(String.format("total part_2 of twt:\t\t%.2f\n", sum_twt_part_2));

		    			System.out.println("  Part 3 ---------------\n");
		    			
		    			System.out.println(String.format("curr_visits:\t\t\t%.2f",         curr_visits));
		    			System.out.println(String.format("curr_max_waiting_time:\t\t%d\n", curr_station_max_waiting_time));
		    			
		    			System.out.println("formula:\t\t\tcurr_visits * curr_max_waiting_time = twt_part_3");
		    			System.out.println(String.format("calculation:\t\t\t%.2f * %d = %.2f\n", curr_visits, curr_station_max_waiting_time, twt_part_3));
		    			
		    			System.out.println(String.format("current part_3 of twt:\t\t%.2f", twt_part_3));
		    			System.out.println(String.format("total part_3 of twt:\t\t%.2f\n", sum_twt_part_3));
			    		
		    			System.out.println("  Result ---------------\n");
		    			
		    			System.out.println(String.format("updatet twt:\t\t\t%.2f", total_waiting_time));
		    			*/
		    		}	    		
	    		}
	    	}
	    }
	    /*
	    System.out.println("\n-------------------------------------\n");
	    
	    System.out.println(final_calculation_twt_part_1);
	    
	    System.out.println("\n-------------------------------------\n");
	   
	    System.out.print("total waiting time:\t");
	    System.out.println(total_waiting_time);
	    
	    System.out.println("\n-------------------------------------\n");
	    */
	    
	    // Return the total waiting time (sum of all three equation parts)
		return total_waiting_time;
	}
}