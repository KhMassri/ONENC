/*
 * @(#)DistributedBubbleRap.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package routing;

import java.util.*;
import core.*;

public class Test implements RoutingDecisionEngine
{
	
	 
	public Test(Settings s)
	{
		
	}
	
	/**
	 * Constructs a DistributedBubbleRap Decision Engine from the argument 
	 * prototype. 
	 * 
	 * @param proto Prototype DistributedBubbleRap upon which to base this object
	 */
	public Test(Test proto)
	{
		
	}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	/**
	 * Starts timing the duration of this new connection and informs the community
	 * detection object that a new connection was formed.
	 * 
	 * @see routing.RoutingDecisionEngine#doExchangeForNewConnection(core.Connection, core.DTNHost)
	 */
	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{
		DTNHost myHost = con.getOtherNode(peer);
		
	}
	
	public void connectionDown(DTNHost thisHost, DTNHost peer)
	{
		
	}

	public boolean newMessage(Message m)
	{
		return true; // Always keep and attempt to forward a created message
	}

	public boolean isFinalDest(Message m, DTNHost from,DTNHost me)
	{
		return m.getTo() == me; // Unicast Routing
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		//if(m.getTo() == otherHost) return true; // trivial to deliver to final dest
		
		/*
		 * Here is where we decide when to forward along a message. 
		 * 
		 *
		 */
		
		return true;
		
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		return false;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return true;
	}

	public Test replicate()
	{
		return new Test(this);
	}
	
	
	
	
	

	private Test getOtherDecisionEngine(DTNHost h)
	{
		MessageRouter otherRouter = h.getRouter();
		assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
		" with other routers of same type";
		
		return (Test) ((DecisionEngineRouter)otherRouter).getDecisionEngine();
	}

	

	public boolean shouldSortOldestMessages(){
		return false;
	}

	public int compareToSort(Message msg1, Message msg2, Connection con1, Connection con2, DTNHost me){
		
		return 0;
		
	}

	
	
}
