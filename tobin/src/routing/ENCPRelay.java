
package routing;

import java.util.*;

import Jama.Matrix;

import core.*;
/*
 * 
 * implement the encp
 * 
 */
public class ENCPRelay implements RoutingDecisionEngine
{
	/*
	 * reading ENCP setting parameters
	 */

	private  int G = new Settings("NCP").getInt("nrofGenerations");
	private  int P = new Settings("NCP").getInt("nrofPods");
	private  GaloisField gf =GaloisField.getInstance();

	Map<String,LinkedList<SprayListElement>> sprayList;
	private Random rng;

	public ENCPRelay(Settings s)
	{

	}

	public ENCPRelay(ENCPRelay ENCP)
	{


		/*
		 * spray list for each <pod,<i,L>> 
		 */
		sprayList = new HashMap<String,LinkedList<SprayListElement>>();
		for(int i=0;i<P;i++)
			sprayList.put("P"+i,new LinkedList<SprayListElement>());
		rng = new Random();


	}

	public RoutingDecisionEngine replicate()
	{
		return new ENCPRelay(this);
	}

	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{
		RoutingDecisionEngine otherDecider = peer.getRouter().decider;
		DTNHost me = con.getOtherNode(peer);
		
		if(((ActiveRouter)me.getRouter()).isTransferring())
			return;


		Message lc = null;

		if( otherDecider instanceof ENCPRelay)
		{
			for(int pod = 0;pod<P;pod++)

			{
				if(this.sprayList.get("P"+pod).size()==0)
					continue;

				for(Iterator<SprayListElement> i = this.sprayList.get("P"+pod).iterator();i.hasNext();)
				{
					SprayListElement e = i.next();

					if (!(((ENCPRelay)otherDecider).isSprayListContains("P"+pod,e.getIndex())) && e.getL() > 1)

					{
						lc = getLCOf(me,"P"+pod);
						if(lc == null)
							continue;
						lc.updateProperty("Index", e.getIndex());
						double l = e.getL()/2.0;
						lc.updateProperty("L",(int)Math.floor(l));
						
						if(((DecisionEngineRouter)me.getRouter()).startTransfer(lc, con)==MessageRouter.RCV_OK)
						{	lc.updateProperty("L",(int)Math.ceil(l));
							me.getRouter().addToMessages(lc, true);

							return;
						}				
					}
				}

			}


			/********************** IF no msgs here check from other***********************/

			if(lc == null)

				for(int pod = 0;pod<P;pod++)

				{
					if(((ENCPRelay) otherDecider).getSprayList().get("P"+pod).size()==0)
						continue;

					for(Iterator<SprayListElement> i = ((ENCPRelay) otherDecider).getSprayList().get("P"+pod).iterator();i.hasNext();)
					{
						SprayListElement e = i.next();

						if (!(((ENCPRelay)this).isSprayListContains("P"+pod,e.getIndex())) && e.getL() > 1)

						{
							lc = ((ENCPRelay)peer.getRouter().decider).getLCOf(peer,"P"+pod);
							if(lc == null)
								continue;
							lc.updateProperty("Index", e.getIndex());
							double l = e.getL()/2.0;
							lc.updateProperty("L",(int)Math.floor(l));
							

							if(((DecisionEngineRouter)peer.getRouter()).startTransfer(lc, con)==MessageRouter.RCV_OK)
							{
								lc.updateProperty("L",(int)Math.ceil(l));
								peer.getRouter().addToMessages(lc, true);
								return;
							}				
						}
					}

				}

		}


		/**************To Send One packet to destination**************************************************************/

		else if( otherDecider instanceof ENCPDestination)
		{
			for(int pod = 0;pod<P;pod++)

			{
				if(this.sprayList.get("P"+pod).size()==0)
					continue;


				lc = getLCOf(me,"P"+pod);
				
				if(lc == null || ((ENCPDestination)otherDecider).getEncodedPods().contains("P"+pod))
					continue;
				
				if(((DecisionEngineRouter)me.getRouter()).startTransfer(lc, con)==MessageRouter.RCV_OK)
				{
					lc.updateProperty("L",1);
					me.getRouter().addToMessages(lc, true);
					return;
				}				



			}

		}

	}

	public boolean isFinalDest(Message m,DTNHost from, DTNHost me)
	{
		String[] mId = m.getId().split(":");
		String mPodId = mId[0];

		int l = (Integer)m.getProperty("L");
		int index = (Integer)m.getProperty("Index");

		// make sure that no index is already exist
		sprayList.get(mPodId).add(new SprayListElement(index,l));
		return false;
		
	}


	public boolean newMessage(Message m)
	{
		return false;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return m.getTo() == hostReportingOld;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		
		
		String[] mId = m.getId().split(":");
		String mPodId = mId[0];

		int l = (Integer)m.getProperty("L");
		int index = (Integer)m.getProperty("Index");
		
		for(Iterator<SprayListElement> i = this.sprayList.get(mPodId).iterator();i.hasNext();)
		{
			SprayListElement e = i.next();
			if(e.getIndex() == index)
				e.setL(l);
		}

		// make sure that no index is already exist

		return true;

		
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		/*to send one packet to dest*/
		//if(m.getTo() == otherHost) return true;

		return false;
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

	Message getLCOf(DTNHost h, String pod){

		List<Message> msgs = new ArrayList<Message>();
		msgs.addAll(h.getMessageCollection());
		List<String[]> co =new ArrayList<String[]>();

		String s="";
		Message Ms=null;

		for(Message m:msgs){
			String[] mId = m.getId().split(":");
			if(!pod.equals(mId[0]))
				continue;
			co.add(mId[1].split(","));
			Ms = m;
		}

		int[][] result = new int [co.size()][G];
		int i=0,j;

		for(String[] m:co){

			int beta = rng.nextInt(256);
			j=0;
			for(String e:m)
				result[i][j++] = gf.multiply(Integer.parseInt(e),beta);	
			i++;	
		}

		for(j=0;j<G;j++)
		{
			int sum = 0;

			for(i=0;i<result.length;i++)
				sum=gf.add(sum, result[i][j]);

			s+= sum+",";
		}

		if(Ms == null)
			return null;

		Message ret = Ms.replicate();
		ret.setID(pod+":"+s);
		return ret;

	}


	public Map<String, LinkedList<SprayListElement>> getSprayList() {
		return sprayList;
	}

	public boolean isSprayListContains(String pod,int index){

		LinkedList<SprayListElement> list = this.sprayList.get(pod);
		for(SprayListElement e:list)
			if(index == e.getIndex())
				return true;


		return false;
	}


	class SprayListElement{

		int index,l;
		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
		public int getL() {
			return l;
		}
		public void setL(int l) {
			this.l = l;
		}
		SprayListElement(int index,int l){
			this.index=index;
			this.l=l;

		}

	}

}