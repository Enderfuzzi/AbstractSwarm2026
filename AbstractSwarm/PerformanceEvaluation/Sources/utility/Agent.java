package utility;

import java.util.HashMap;

import org.jdom2.Element;

public class Agent {
	
	Integer id			= null;
	Integer count		= null;
	Double  size		= null;
	Integer necessity	= null;
	Integer frequency 	= null;
	Double  time      	= null;
	Double  speed     	= null;
	
	Integer perspective_id = null;	// Id of the perspective that contains the current station
	
	HashMap<Integer, Integer> connected_stations = new HashMap<Integer, Integer>();	// HashMap<station_id, station_count>
	
	Integer sum_connected_stations = null;	// Sum of all instances of the agents that are connected to the current station
	
	public Integer getId() {
		return this.id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public Integer getCount() {
		return this.count;
	}
	
	public void setCount(Integer count) {
		this.count = count;
	}
	
	public Double getSize() {
		return this.size;
	}

	public void setSize(Double size) {
		this.size = size;
	}

	public Integer getNecessity() {
		return necessity;
	}

	public void setNecessity(Integer necessity) {
		this.necessity = necessity;
	}

	public Integer getFrequency() {
		return this.frequency;
	}

	public void setFrequency(Integer frequency) {
		this.frequency = frequency;
	}
	
	public Double getTime() {
		return this.time;
	}

	public void setTime(Double time) {
		this.time = time;
	}

	public Double getSpeed() {
		return this.speed;
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
	}
	
	public Integer getPerspective_id() {
		return this.perspective_id;
	}

	public void setPerspective_id(Integer perspective_id) {
		this.perspective_id = perspective_id;
	}

	public HashMap<Integer, Integer> getConnected_stations() {
		return this.connected_stations;
	}
	
	public void setConnected_stations(HashMap<Integer, Integer> connected_stations) {
		this.connected_stations = connected_stations;
	}
	
	private void setSum_connected_stations()
	{
		
		this.sum_connected_stations = 0;
		
		for (int count : this.connected_stations.values()) 
		{
			
			this.sum_connected_stations += count;
		}
	}
	
	public Integer getSum_connected_stations()
	{
		
		this.setSum_connected_stations();
		
		return this.sum_connected_stations;
	}
	
	public void readFromXml(Element xml_agent, Integer curr_perspective_id)
	{
		
    	// Set agent id
    	this.setId(Integer.parseInt(xml_agent.getAttributeValue("id")));
    	
    	// Set amount of instances of that agent
    	this.setCount(Integer.parseInt(xml_agent.getAttributeValue("count")));
    	
    	// Set perspective_id for that agent (helps later to use the right total place value of the perspective)
    	this.setPerspective_id(curr_perspective_id);
    	
    	// Set size value for that agent
    	if (xml_agent.getChild("size") != null)
		{
    		
    		this.size = Double.parseDouble(xml_agent.getChild("size").getAttributeValue("value"));
		}
    	else
    	{
    		
    		// If the agent has no size attribute, it's size is not limited
    		this.size = Double.POSITIVE_INFINITY;
    	}
    
    	// Set necessity value for that agent
    	if (xml_agent.getChild("necessity") != null)
		{
    		
    		this.necessity = Integer.parseInt(xml_agent.getChild("necessity").getAttributeValue("value"));
		}
    	else
    	{
    		
    		this.necessity = 0;
    	}
    	
    	// Set frequency value for that agent
    	if (xml_agent.getChild("frequency") != null)
		{

    		this.frequency = Integer.parseInt(xml_agent.getChild("frequency").getAttributeValue("value"));
		}
    	else
    	{
    		
    		this.frequency = 0;
    	}
    	
    	// Set time value for that agent
    	if (xml_agent.getChild("time") != null)
		{

    		this.time = Double.parseDouble(xml_agent.getChild("time").getAttributeValue("value"));
		}
    	else
    	{
    		
    		// If the agent has no time attribute, the time it has to spent on a station is not limited
    		this.time = Double.POSITIVE_INFINITY;
    	}
    	
    	// Set speed value for that agent
    	if (xml_agent.getChild("speed") != null)
		{

    		this.speed = Double.parseDouble(xml_agent.getChild("speed").getAttributeValue("value"));
		}
    	else
    	{
    		
    		// If the agent has no speed attribute, it's speed is not limited
    		this.speed = Double.POSITIVE_INFINITY;
    	}
	}

	@Override
	public String toString() {
		
		return "Agent [id=" 			 + this.id 					   + ", "
			 + "count=" 				 + this.count 				   + ", "
			 + "size=" 				     + this.size 				   + ", "
			 + "necessity=" 			 + this.necessity 			   + ", "
			 + "frequency=" 			 + this.frequency			   + ", "
			 + "time=" 					 + this.time 				   + ", "
			 + "speed=" 				 + this.speed 				   + ", "
			 + "perspective_id=" 		 + this.perspective_id 		   + ", "
			 + "sum_connected_stations=" + this.sum_connected_stations + "]";
	}
}
