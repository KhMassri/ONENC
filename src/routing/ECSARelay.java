package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;


public class ECSARelay implements RoutingDecisionEngine
{	
	
	static private double alpha;
	static private double threshold =(2-2*alpha)/(2-alpha);
	static private int secondsForTimeOut;
	
	private double delProb;
	private double lastUpdate;
	
	

	public ECSARelay(Settings s){

		Settings scfadSettings	=	new Settings("ECSA");
		alpha= scfadSettings.getDouble("alpha");
		secondsForTimeOut = scfadSettings.getInt("secondsForTimeOut");
	
	}


	public ECSARelay(ECSARelay r){


		this.lastUpdate = SimClock.getTime();
		this.delProb=0;

	}

	public RoutingDecisionEngine replicate()
	{
		return new ECSARelay(this);
	}



	public void connectionDown(DTNHost thisHost, DTNHost peer){
	
		
		timeOutUpdate(thisHost);
	}

	public void connectionUp(DTNHost thisHost, DTNHost peer){
		
		if(peer.getRouter().decider instanceof ECSADestination)
		{
			
			delProb=(1-alpha)*delProb + alpha;
			this.lastUpdate = SimClock.getTime();
			
						
		}
		
		timeOutUpdate(thisHost);


	}


	public void doExchangeForNewConnection(Connection con, DTNHost peer){



	}

	public boolean isFinalDest(Message m,DTNHost from, DTNHost me)
	{
		/*ft initially not changed*/
		if(!(from.getRouter().decider instanceof ECSARelay))
			return false;

		
		//replace it with msgs temp dec
		//double curFt=(Double)m.getProperty(ftStr);
		//m.updateProperty(ftStr, 1-(1-curFt)*(1- (((ECSARelay)(from.getRouter().decider)).getDelProb())));

		return false;
	}

	public boolean newMessage(Message m)
	{
		return false;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return m.getTo()==hostReportingOld;
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

		if(m.getFrom() == otherHost || otherHost.getRouter().decider instanceof ECSASource) 
			return false;

		String[] mId = m.getId().split(":");
		String mPodId = mId[0];
		String lc = mId[1];

		//destination case

		if(otherHost.getRouter().decider instanceof ECSADestination)
		{
			ECSADestination other = ((ECSADestination)otherHost.getRouter().decider);
			if(!(other.getEncodedPods().contains(mPodId)))
				return true;


			return false;
		}

		// Relay case

		// each relay can carry one msg
		
		for(Message msg:otherHost.getRouter().getMessageCollection()){
			String[] msgId = msg.getId().split(":");
			String msgPodId = msgId[0];
			/*EACH node carry one message*/
			if(msgPodId.equals(mPodId))
				return false;
		}
		
		/*If my delProb is larger dont send*/
		if(this.getDelProb() < 1 *(((ECSARelay)(otherHost.getRouter().decider)).getDelProb()))
			return true;
		
		

		if(Math.random()>0.5)
			return true;
				
		
			
			
		


		return false;
	}


	public boolean shouldSortOldestMessages(){
		return true;
	}

	/*to be sorted ascending according to ft value*/
	public int compareToSort(Message msg1, Message msg2, Connection con1, Connection con2, DTNHost me){

		double diff;
		if(con1==null || con2 == null)
		{
			/*called by get oldest msg*/
			diff = msg1.getReceiveTime() - msg2.getReceiveTime();
			if (diff == 0)
				return 0;
			return (diff < 0 ? -1 : 1);
		}
		
		if(con1.getOtherNode(me).getRouter().decider instanceof ECSADestination)
			return -1;
		if(con2.getOtherNode(me).getRouter().decider instanceof ECSADestination)
			return 1;
		
		
		diff = (((ECSARelay)(con1.getOtherNode(me).getRouter().decider)).getDelProb()) - (((ECSARelay)(con2.getOtherNode(me).getRouter().decider)).getDelProb());
		if (diff == 0)
			return 0;
		return (diff > 0 ? -1 : 1);


	}



	public double getDelProb(){return delProb;}

	private void timeOutUpdate(DTNHost me) {


		if (SimClock.getTime() - this.lastUpdate < secondsForTimeOut)
			return;

		delProb=(1-alpha)*delProb;
		this.lastUpdate = SimClock.getTime();
		
		
	}



	/*Inner classes*/
/*
	private class NeihbComparator implements Comparator<DTNHost> {

		public int compare(DTNHost h1,DTNHost h2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((FadToSink)h1.getRouter()).getDelProb();
			// -"- tuple2...
			double p2 = ((FadToSink)h2.getRouter()).getDelProb();

			if (p1>p2) {
				return -1;
			}
			else if(p1<p2){
				return 1;
			}
			else return 0;
		}
	}*/



}