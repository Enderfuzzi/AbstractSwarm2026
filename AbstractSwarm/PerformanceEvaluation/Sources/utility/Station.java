package utility;

import java.util.Collections;
import java.util.HashMap;

import org.jdom2.Element;

public class Station {

	Integer id 			= null;
	Integer count 		= null;
	Double  space 		= null;
	Integer necessity 	= null;
	Integer frequency 	= null;
	Double  time      	= null;
	
	Integer perspective_id = null;	// Id of the perspective that contains the current station
	
	HashMap<Integer, Integer> connected_agents = new HashMap<Integer, Integer>();	// HashMap<agent_id, agent_count>
	
	Integer sum_connected_agents = 0;	// Sum of all instances of the agents that are connected to the current station
	
	HashMap<Integer, Integer> connectedTimeEdges = new HashMap<Integer, Integer>();	// HashMap<timeEdge_id, time_value>
	
	Integer max_waiting_time = 0;	// Maximum value of all time edges conntected to the current station
	
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
	
	public Double getSpace() {
		return this.space;
	}
	
	public void setSpace(Double space) {
		this.space = space;
	}
	
	public Integer getNecessity() {
		return this.necessity;
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
	
	public Integer getPerspective_id() {
		return this.perspective_id;
	}

	public void setPerspective_id(Integer perspective_id) {
		this.perspective_id = perspective_id;
	}

	public HashMap<Integer, Integer> getConnected_agents() {
		return this.connected_agents;
	}

	public void setConnected_agents(HashMap<Integer, Integer> connected_agents) {
		this.connected_agents = connected_agents;
	}
	
	private void setSum_connected_agents()
	{
		
		this.sum_connected_agents = 0;
		
		for (Integer count : this.connected_agents.values()) 
		{
			
			this.sum_connected_agents += count;
		}
	}
	
	public Integer getSum_connected_agents()
	{
		
		this.setSum_connected_agents();
		
		return this.sum_connected_agents;
	}
	
	public HashMap<Integer, Integer> getConnectedTimeEdges() {
		return connectedTimeEdges;
	}

	public void setConnectedTimeEdges(HashMap<Integer, Integer> connectedTimeEdges) {
		this.connectedTimeEdges = connectedTimeEdges;
	}
	
	private void setMaxWaitingTime()
	{
		
		if (this.getConnectedTimeEdges().size() > 0)
		{
			
			this.max_waiting_time = Collections.max(this.connectedTimeEdges.values());
		}
		else
		{
			
			this.max_waiting_time = 0;
		}
	}
	
	public Integer getMaxWaitingTime()
	{
		
		this.setMaxWaitingTime();
		return this.max_waiting_time;
	}
	
	public void readFromXml(Element xml_station, Integer curr_perspective_id)
	{
		// Set station id
    	this.setId(Integer.parseInt(xml_station.getAttributeValue("id")));
    	
    	// Set station count
    	this.setCount(Integer.parseInt(xml_station.getAttributeValue("count")));
    	
    	// Set perspective_id for that station (helps later to use the right total place value of the perspective)
    	this.setPerspective_id(curr_perspective_id);
    			    	
    	// Set space value for that agent
    	if (xml_station.getChild("space") != null)
		{
    		
    		this.space = Double.parseDouble(xml_station.getChild("space").getAttributeValue("value"));
		}
    	else
    	{
    		
    		// If the station has no space attribute, it's space is not limited
    		this.space = Double.POSITIVE_INFINITY;
    	}
    	
    	// Set necessity value for that station
    	if (xml_station.getChild("necessity") != null)
		{
    		
    		this.necessity = Integer.parseInt(xml_station.getChild("necessity").getAttributeValue("value"));
		}
    	else
    	{
    		
    		this.necessity = 0;
    	}
    	
    	// Set frequency value for that station
    	if (xml_station.getChild("frequency") != null)
		{

    		this.frequency = Integer.parseInt(xml_station.getChild("frequency").getAttributeValue("value"));
		}
    	else
    	{
    		
    		this.frequency = 0;
    	}
    	
    	// Set time value for that station
    	if (xml_station.getChild("time") != null)
		{

    		this.time = Double.parseDouble(xml_station.getChild("time").getAttributeValue("value"));
		}
    	else
    	{
    		
    		// If the station has no time attribute, the time an agent has to spent on this station is not limited
    		//this.time = Double.POSITIVE_INFINITY;
    		this.time = 999.0;
    	}
	}
	
	@Override
	public String toString() {
		
		return           "Station [id=" + this.id                 + ", "
				+              "count=" + this.count              + ", "
				+              "space=" + this.space              + ", "
				+          "necessity=" + this.necessity          + ", "
				+          "frequency=" + this.frequency          + ", "
				+               "time=" + this.time               + ", "
				+     "perspective_id=" + this.perspective_id     + ", "
				+   "connected_agents=" + this.connected_agents   + ", "
				+ "connectedTimeEdges=" + this.connectedTimeEdges + ", "
				+   "max_waiting_time=" + this.max_waiting_time   + "]";
	}	
}
