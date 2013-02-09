
package routing;

import java.util.*;

import core.*;
/*
 * 
 * Notes
 * K codewords are generated for each pod P of size G
 * this is a source router, as long as there is a free space in 
 * the buffer of a relay node it will send packets to it
 * sent packets are deleted
 * 
 */
public class SCFadSource implements RoutingDecisionEngine
{
	/*The 2 is canceled from the first division of the first relay*/
	
	static private  int G;
	static private  int K ;
	static private  int P;
	static private  int codding;
	
	static private String ftStr="FaultToleranceValue";
	
	
	


	Set<Integer> AllLC;
	Random rng;

	int pkt;
	int pods;
	int g=1;
	private int deleted;



	public SCFadSource(Settings s)
	{
		Settings scfadSettings = new Settings("SCFad");
		 G = scfadSettings.getInt("nrofGenerations");
		 K = scfadSettings.getInt("nrofInjections");
		 P = scfadSettings.getInt("nrofPods");
		codding = scfadSettings.getInt("codding");
	}

	public SCFadSource(SCFadSource lcncp)
	{

		this.pkt=1;
		this.g = 1;
		this.pods=0;
		this.AllLC=new HashSet<Integer>();
		this.rng = new Random(1);
		this.deleted = K-G;

	}

	public RoutingDecisionEngine replicate()
	{
		return new SCFadSource(this);
	}

	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{}

	public boolean isFinalDest(Message m, DTNHost from,DTNHost me)
	{

		return false;
	}

	public boolean newMessage(Message m)
	{
		/*if(SimClock.getIntTime()<50000)
			return false;
		*/


		if((codding == 1 && pkt > K)||(codding == 0 && pkt > G)) 
			if (pods >= P-1)
				return false;
			else
			{
				pkt = 1;
				pods++;

				g=1;
				AllLC.clear();

			}


		m.setID("P"+pods+":"+this.getE());
		pkt++;
		m.addProperty("FaultToleranceValue", new Double(0));
		return true;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return m.getTo() == hostReportingOld;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		//seen.add(otherHost.getAddress());
		if(deleted <=0 || codding == 1)
			return true;
		
		deleted--;
		return false;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		if(!(otherHost.getRouter().decider instanceof SCFadRelay))
			return false;
		
		//if(seen.contains(otherHost.getAddress()))
		if(otherHost.getRouter().getFreeBufferSize() > m.getSize())
			return true;
		
		for(Message msg:otherHost.getRouter().getMessageCollection()){
			// if no space just replace if ftvalue is less
			if((Double)m.getProperty(ftStr)<(Double)msg.getProperty(ftStr))
				return true;
		}
		
		
		return false;
		
		

		/*To send one msg for each pod*/
		//String[] mId = m.getId().split(":");
		//String mPodId = mId[0];

		/*for(Message msg:otherHost.getRouter().getMessageCollection()){

			String[] msgId = msg.getId().split(":");
			String msgPodId = msgId[0];
			if(msgPodId.equals(mPodId))
			{
				return false;
			}

		}*/



	}



	public boolean shouldSortOldestMessages(){
		return true;
	}

	public int compareToSort(Message msg1, Message msg2){
		if (rng.nextFloat()>0.5)
			return -1;
		else return 1;
		
	}



	String getRandomLC()
	{
		int r;
		do {

			r= 1 + rng.nextInt((int)Math.pow(2,G) - 1);
			
		}
		while(AllLC.contains(r));

		String s = Integer.toBinaryString(r);
		while(s.length()<G)s="0"+s;
		AllLC.add(r);					
		return s;	

	}


	String getE(){


		if(this.g > (int)Math.pow(2,this.G-1))
			return getRandomLC();
		

		String s = Integer.toBinaryString(g);
		while(s.length()<G)s="0"+s;
		AllLC.add(g);
		g=g*2;


		return s;

	}	

}