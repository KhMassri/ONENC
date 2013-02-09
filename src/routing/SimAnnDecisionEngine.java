
package routing;

import java.util.*;

import routing.maxprop.MeetingProbabilitySet;

import core.*;


public class SimAnnDecisionEngine implements RoutingDecisionEngine
{
	/** Router's setting namespace ({@value})*/
	public static final String SimulatedAnnealing_NS = "SimAnn";
	/**  */
	public static final String ALPHA = "alpha";
	private double alpha;

	private MeetingProbabilitySet probs;


	protected Map<DTNHost, Double> recentEncounters;
	protected static final double DEFAULT_TIMEDIFF = 300;
	protected static final double defaultTransitivityThreshold = 1.0;

	double cdc;
	private double lastNUpdate;
	private Set<Integer> Nt;
	private Set<Integer> Nt_1;
	
	private String FTCStr="FTCValue";

	public SimAnnDecisionEngine(Settings s)
	{

		Settings SimulatedAnnealingSettings = new Settings(SimulatedAnnealing_NS);
		if(SimulatedAnnealingSettings.contains(ALPHA))
			alpha= SimulatedAnnealingSettings.getDouble(ALPHA);
		else
			alpha = 1;

	}

	public SimAnnDecisionEngine(SimAnnDecisionEngine r)
	{
		this.alpha=r.alpha;
		this.probs = new MeetingProbabilitySet(400, alpha);

		/*recent of encounters to store the time of meetings*/
		recentEncounters = new HashMap<DTNHost, Double>();
		
		/*to computer change degree of connections*/
		Nt=new HashSet<Integer>();
		Nt_1=new HashSet<Integer>();
		lastNUpdate= SimClock.getTime();

	}

	public RoutingDecisionEngine replicate()
	{
		return new SimAnnDecisionEngine(this);
	}

	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{
		SimAnnDecisionEngine de = this.getOtherSimAnnDecisionEngine(peer);
		DTNHost myHost = con.getOtherNode(peer);

		/*
		 * Update meeting probability
		 * */
		probs.updateMeetingProbFor(peer.getAddress());
		de.probs.updateMeetingProbFor(myHost.getAddress());
		
		/*update the neighbers set*/
		Nt.add(peer.getAddress());
		de.Nt.add(myHost.getAddress());
		
		updateCdc();
		de.updateCdc();
		
		

		/*
		 *Heating process 
		 * */
		for(Message m:myHost.getMessageCollection())
			m.incTemp();
				
		for(Message m:peer.getMessageCollection())
			m.incTemp();
				
//		List<String> msgs = new ArrayList<String> (this.getOtherLMMessages(peer));
//		
//		for(Message m:myHost.getMessageCollection())
//			if(m.getState().equals("LM") && msgs.contains(m.getId()))
//			{
//				m.setTemp(20);
//				m.setState("DOWN");
//			}
//	
		/*
		 * for SnF Utility
		 */
	
		double distBwt = myHost.getLocation().distance(peer.getLocation());
		double mySpeed = myHost.getPath() == null ? 0 : myHost.getPath().getSpeed(),
				peerSpeed = peer.getPath() == null ? 0 : peer.getPath().getSpeed(),
						myTimediff, peerTimediff;
	
		if(mySpeed == 0.0)
			myTimediff = DEFAULT_TIMEDIFF;
		else
			myTimediff = distBwt/mySpeed;
	
		if(peerSpeed == 0.0)
			peerTimediff = DEFAULT_TIMEDIFF;
		else
			peerTimediff = distBwt/peerSpeed;
	
	
		//Add references to other tables
		recentEncounters.put(peer, SimClock.getTime());
		de.recentEncounters.put(myHost, SimClock.getTime());
	
		Set<DTNHost> hostSet = new HashSet<DTNHost>(this.recentEncounters.size() 
				+ de.recentEncounters.size());
		hostSet.addAll(this.recentEncounters.keySet());
		hostSet.addAll(de.recentEncounters.keySet());
	
		for(DTNHost h : hostSet)
		{
			double myTime, peerTime;
	
			if(this.recentEncounters.containsKey(h)) 
				myTime = this.recentEncounters.get(h);
			else
				myTime = -1.0;
			if(de.recentEncounters.containsKey(h))
				peerTime = de.recentEncounters.get(h);
			else
				peerTime = -1.0;
	
			//update my table for host h
			if(myTime < 0.0 || myTime + myTimediff < peerTime)
				recentEncounters.put(h, peerTime - myTimediff);
	
			//update peer table for host h
			if(peerTime < 0.0 || peerTime + peerTimediff < myTime)
				de.recentEncounters.put(h, myTime - peerTimediff);
		}

	}

	public boolean isFinalDest(Message m, DTNHost from,DTNHost me)
	{

	//	System.out.println("state  "+m.getState()+"  T  "+ m.getTemp());

		/*checking the state of the msg*/
		
		if(m.getState().equals("UP"))
			m.setTemp(0);
		else if(m.getState().equals("DOWN"))
			m.decTemp();
		
		else if(m.getState().equals("MAX")){
			m.setState("DOWN");
			m.updateProperty(FTCStr,(Integer)m.getProperty(FTCStr)+1 );

		}
			
		
		return m.getTo() == me;
	}

	public boolean newMessage(Message m)
	{
		m.addProperty(FTCStr, new Integer(1));

		return true;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{

		return m.getTo() == hostReportingOld;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		if(!(m.getState().equals("MAX"))) 
			return true;

		m.setState("LM");
		m.updateProperty(FTCStr,(Integer)m.getProperty(FTCStr)+1 );
		return false;


	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m, DTNHost me,DTNHost otherHost)
	{

		DTNHost dest = m.getTo();
		SimAnnDecisionEngine de = this.getOtherSimAnnDecisionEngine(otherHost);

		if(m.getTo() == otherHost) return true;

		/*
		 * According to age of encounter utility
		 */



		if(de.recentEncounters.containsKey(dest) && this.recentEncounters.containsKey(dest))
			if(de.recentEncounters.get(dest) > this.recentEncounters.get(dest))
			{
				return true;
			}


		if(m.getState().equals("DOWN") || m.getState().equals("MAX"))
		{
			double ua=0,ub=0;

			if (m.getHops().contains(otherHost))
				return false;

			if(de.recentEncounters.containsKey(dest))
				ub = de.recentEncounters.get(dest);
			if(this.recentEncounters.containsKey(dest))
				ua = this.recentEncounters.get(dest);

			boolean send = Math.exp((ub - ua)/ (m.getTemp())) >= Math.random();

			//System.out.println("Max-Down: Other ub-ua T send?"+ "  "+otherHost.getAddress()+"  "+(ub-ua)+"  "+ m.getTemp()+"  "+ send);

			if(send) 
				return true;

		}

		/*
		 * 
		 * According to meeting probability utility
		 */
		
//				double ua=0,ub=0;
//				
//				ua = (this.probs.getProbFor(dest.getAddress()) + 2*cdc)/3;
//				ub = (de.probs.getProbFor(dest.getAddress()) + 2*de.cdc)/3;
//
//
//				if(ua < ub)
//				{
//					return true;
//				}
//				
//				
//		
//				if(m.getState().equals("DOWN") || m.getState().equals("MAX"))
//				{
//					if (m.getHops().contains(otherHost))
//						return false;
//					
//		
//					boolean send = Math.exp((ub - ua)/ (0.1*m.getTemp())) >= Math.random();
//					
//		
//					if(send) 
//						return true;
//		
//				}


		return false;
	}

	private SimAnnDecisionEngine getOtherSimAnnDecisionEngine(DTNHost h)
	{
		MessageRouter otherRouter = h.getRouter();
		assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
		" with other routers of same type";

		return (SimAnnDecisionEngine) ((DecisionEngineRouter)otherRouter).getDecisionEngine();
	}


	private List<String> getOtherLMMessages(DTNHost h){
		List<String> msgs = new ArrayList<String>();

		MessageRouter otherRouter = h.getRouter();
		assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
		" with other routers of same type";


		for(Message m:otherRouter.getMessageCollection())
			if(m.getState().equals("LM"))
				msgs.add(m.getId());

				return msgs;



	}
	
private void updateCdc() {
		
		
		
		if(SimClock.getTime()>=lastNUpdate+1000){
			double curCdc;
			Set<Integer> union = new HashSet<Integer>();
			union.addAll(Nt_1);union.addAll(Nt);
			curCdc=union.size()-(Nt.size()+Nt_1.size()-union.size());
			if(curCdc!=0)
				curCdc=curCdc/union.size();
			
			//cdc = (1 - 0.85)*cdc + 0.85*curCdc;
			cdc = curCdc;

			
			Nt_1.removeAll(Nt_1);
			Nt_1.addAll(Nt);
			Nt.removeAll(Nt);
			lastNUpdate=SimClock.getTime();	
			
			
				
			
			
		}
			
	}
	
	
public boolean shouldSortOldestMessages(){
	return false;
}

public int compareToSort(Message msg1, Message msg2){
	
	return 0;
}




}