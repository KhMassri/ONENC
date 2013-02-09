package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;


public class SCFadRelay implements RoutingDecisionEngine
{	
	
	static private double alpha;
	static private double gamma;
	static private double threshold =(2-2*alpha)/(2-alpha);
	static private String ftStr="FaultToleranceValue";
	static private int secondsForTimeOut;
	
	private double delProb;
	private double lastUpdate;
	
	

	public SCFadRelay(Settings s){

		Settings scfadSettings	=	new Settings("SCFad");
		alpha= scfadSettings.getDouble("alpha");
		gamma= scfadSettings.getDouble("gamma");
		secondsForTimeOut = scfadSettings.getInt("secondsForTimeOut");
	
	}


	public SCFadRelay(SCFadRelay r){


		this.lastUpdate = SimClock.getTime();
		this.delProb=0;

	}

	public RoutingDecisionEngine replicate()
	{
		return new SCFadRelay(this);
	}



	public void connectionDown(DTNHost thisHost, DTNHost peer){
	
		
		timeOutUpdate(thisHost);
	}

	public void connectionUp(DTNHost thisHost, DTNHost peer){
		
		if(peer.getRouter().decider instanceof SCFadDestination)
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
		if(!(from.getRouter().decider instanceof SCFadRelay))
			return false;

		double curFt=(Double)m.getProperty(ftStr);
		m.updateProperty(ftStr, 1-(1-curFt)*(1- (((SCFadRelay)(from.getRouter().decider)).getDelProb())));

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


		if((m.getTo() == otherHost) || ((otherHost.getRouter().decider instanceof SCFadDestination)))
			return true;


		if(otherHost.getRouter().decider instanceof SCFadRelay)
		{
			double curFt=(Double)m.getProperty(ftStr);

			m.updateProperty(ftStr, 1-(1-curFt)*(1- (((SCFadRelay)(otherHost.getRouter().decider)).getDelProb())));
			delProb=(1-alpha)*delProb + alpha*(((SCFadRelay)(otherHost.getRouter().decider)).getDelProb());
			this.lastUpdate = SimClock.getTime();

		}

		return false;
	}

	
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{

		if(m.getFrom() == otherHost || otherHost.getRouter().decider instanceof SCFadSource) 
			return false;

		String[] mId = m.getId().split(":");
		String mPodId = mId[0];

		//destination case

		if(otherHost.getRouter().decider instanceof SCFadDestination)
		{
			if(!(((SCFadDestination)otherHost.getRouter().decider).getEncodedPods().contains(mPodId)))
				return true;


			return false;
		}

		// Relay case

		/*If my delProb is larger dont send*/
		if(this.getDelProb() >= threshold *(((SCFadRelay)(otherHost.getRouter().decider)).getDelProb()))
			return false;

		/*in case the othernode buffer is full. m will be sent just if there is
		 * a msg has higher ft value than m*/
		
		if(otherHost.getRouter().getFreeBufferSize()<m.getSize())
		{
			for(Message msg:otherHost.getRouter().getMessageCollection()){
				/* if no space just replace if ftvalue is less*/
				if((Double)m.getProperty(ftStr)<(Double)msg.getProperty(ftStr))
					return true;

			//String[] msgId = msg.getId().split(":");
			//String msgPodId = msgId[0];
			
			/*EACH node carry one message*/
			//if(msgPodId.equals(mPodId))
			}
			
		return false;	
		}


		return true;
	}


	public boolean shouldSortOldestMessages(){
		return true;
	}

	/*to be sorted ascending according to ft value*/
	public int compareToSort(Message msg1, Message msg2){

		if((Double)msg1.getProperty(ftStr) < (Double)msg2.getProperty(ftStr))
			return -1;

		else if((Double)msg1.getProperty(ftStr) > (Double)msg2.getProperty(ftStr))
			return 1;

		else return 0;


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