
package routing;

import core.*;
/*
 * 
 * implement the encp
 * 
 */
public class LCNCPRelay implements RoutingDecisionEngine
{	
	private  int P = new Settings("NCP").getInt("nrofPods");

	public LCNCPRelay(Settings s){}

	public LCNCPRelay(LCNCPRelay lcncp){}

	public RoutingDecisionEngine replicate()
	{
		return new LCNCPRelay(this);
	}

	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	public void doExchangeForNewConnection(Connection con, DTNHost peer){
		
		if(!(peer.getRouter().decider instanceof LCNCPDestination))
			return;
		
		LCNCPDestination dest =(LCNCPDestination)peer.getRouter().decider;
		DTNHost thisHost = con.getOtherNode(peer);
		/*
		 * count how many peer has a pod packet of podi
		 */
		for(int i=0;i<P;i++)
		{
			if(dest.encodedPods.contains("P"+i))
				continue;

			for(Message m:thisHost.getRouter().getMessageCollection())
			{
				String[] id = m.getId().split(":");
				int pod = Integer.parseInt(id[0].substring(1));
				if(pod==i)
				{
					dest.nrofPodCarrier[pod]++;
					break;
				}

			}	
		}
		
		
		
	}

	public boolean isFinalDest(Message m,DTNHost from, DTNHost me)
	{
		Integer nrofCopies = (Integer)m.getProperty("L");
		nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		m.updateProperty("L", nrofCopies);

		return false;
	}

	public boolean newMessage(Message m)
	{
		return false;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return false;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		int nrofCopies;

		if(m.getTo() == otherHost) return true;

		nrofCopies = (Integer)m.getProperty("L");

		if(nrofCopies > 1){
			nrofCopies = (int)Math.floor(nrofCopies/2.0);
			m.updateProperty("L", nrofCopies);
		}
			
				

		return false;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		
		if(m.getFrom() == otherHost || otherHost.getRouter().decider instanceof LCNCPSource) 
			return false;

		String[] mId = m.getId().split(":");
		String mPodId = mId[0];
		
		if(otherHost.getRouter().decider instanceof LCNCPDestination)
		{
			if(!(((LCNCPDestination)otherHost.getRouter().decider).getEncodedPods().contains(mPodId)))
				return true;
			
			
			return false;
		}
				

		for(Message msg:otherHost.getRouter().getMessageCollection()){

			String[] msgId = msg.getId().split(":");
			String msgPodId = msgId[0];
			if(msgPodId.equals(mPodId))
			{
				return false;
			}

		}

		int nrofCopies = (Integer)m.getProperty("L");
		if(nrofCopies > 1 ) return true;




		return false;
	}



	public boolean shouldSortOldestMessages(){
		return true;
	}

	public int compareToSort(Message msg1, Message msg2, Connection con1, Connection con2, DTNHost me){

		return (Integer)msg2.getProperty("L") - (Integer)msg1.getProperty("L");

	}
}