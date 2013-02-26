
package routing;

import java.util.*;

import core.*;
/*
 * 
 * implement the encp
 * 
 */
public class LCNCPSource implements RoutingDecisionEngine
{
	/*The 2 is canceled from the first division of the first relay*/
	
	private  int G = new Settings("NCP").getInt("nrofGenerations");
	private  int K = new Settings("NCP").getInt("nrofInjections");
	private  int P = new Settings("NCP").getInt("nrofPods");
	//private  int L = new Settings("NCP").getInt("nrofCopies");
	private  int L = (int)Math.ceil(77.0/K);
	


	Set<Integer> AllLC;
	Random rng;

	int pkt;
	int pods;
	int g=1;



	public LCNCPSource(Settings s)
	{
		
	}

	public LCNCPSource(LCNCPSource lcncp)
	{

		this.pkt=1;
		this.g = 1;
		this.pods=0;
		this.AllLC=new HashSet<Integer>();
		L = 2*L;
		this.rng = new Random(1);
		System.out.println(rng.toString());

	}

	public RoutingDecisionEngine replicate()
	{
		return new LCNCPSource(this);
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


		if(pkt > K) 
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
		m.addProperty("L",L);
		return true;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return m.getTo() == hostReportingOld;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		return true;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		if(!(otherHost.getRouter().decider instanceof LCNCPRelay))
			return false;
		
		/*
		 * each relay take one pod from any source
		 */
		if(m.getTo() == otherHost) return true;

		String[] mId = m.getId().split(":");
		String mPodId = mId[0];

		for(Message msg:otherHost.getRouter().getMessageCollection()){

			String[] msgId = msg.getId().split(":");
			String msgPodId = msgId[0];
			if(msgPodId.equals(mPodId))
			{
				return false;
			}

		}



		return true;
	}



	public boolean shouldSortOldestMessages(){
		return false;
	}

	public int compareToSort(Message msg1, Message msg2){
		return 0;
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