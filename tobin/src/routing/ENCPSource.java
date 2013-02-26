
package routing;

import java.util.*;
import core.*;
/*
 * 
 * implement the encp
 * 
 */
public class ENCPSource implements RoutingDecisionEngine
{
	private  int L = new Settings("NCP").getInt("nrofCopies");
	private  int G = new Settings("NCP").getInt("nrofGenerations");
	private  int K = new Settings("NCP").getInt("nrofInjections");
	private  int P = new Settings("NCP").getInt("nrofPods");


	/*
	 * all the sources shares the same relays list so that a relay just take LC(L) 
	 * only from one source
	 */
	private static Map<String,Set<Integer>> podRelays;






	int pkt = 1;
	int pods = 0;
	int g=1;
	private Random rng;



	public ENCPSource(Settings s)
	{
		podRelays = new HashMap<String,Set<Integer>>();
		for(int i=0;i<P;i++)
			podRelays.put("P"+i, new HashSet<Integer>());


	}

	public ENCPSource(ENCPSource ENCP)
	{
		//podRelays = new HashMap<String,Set<Integer>>();

		//for(int i=0;i<P;i++)
			//podRelays.put("P"+i, new HashSet<Integer>());
		
		rng = new Random(1);



	}

	public RoutingDecisionEngine replicate()
	{
		return new ENCPSource(this);
	}

	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	public void doExchangeForNewConnection(Connection con, DTNHost peer){}

	public boolean isFinalDest(Message m, DTNHost from,DTNHost me)
	{	
		return m.getTo() == me;
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

			}


		m.setID("P"+pods+":"+getLC());
		m.addProperty("L", L);
		m.addProperty("Index",pkt);
		pkt++;
		return true;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return m.getTo() == hostReportingOld;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		String[] mId = m.getId().split(":");
		String mPodId = mId[0];
		podRelays.get(mPodId).add(otherHost.getAddress());

		return true;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return true;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		if(!(otherHost.getRouter().decider instanceof ENCPRelay))
			return false;
		
		String[] mId = m.getId().split(":");
		String mPodId = mId[0];
		

		if(podRelays.get(mPodId).contains(otherHost.getAddress())) 
			return false;
		
		return true;
	}



	public boolean shouldSortOldestMessages(){
		return false;
	}

	public int compareToSort(Message msg1, Message msg2){

		return 0;

	}

	/*
	 * generate K coefficients as a psudo source packet 
	 */

	String getLC(){
		String s="";

		for(int i=1;i<=G;i++){
			int alfa = rng.nextInt(256);
			s += alfa+","; 
		}

		return s;

	}

}