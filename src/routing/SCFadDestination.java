package routing;

import java.util.*;

import Jama.Matrix;

import core.*;
/*
 * 
 * implement the encp
 * 
 */
public class SCFadDestination implements RoutingDecisionEngine
{

	static private  int G;
	static private  int K ;
	static private  int P;


	Matrix[] decodingMatrices;
	Set<String> encodedPods;
	int[] rows;
	int[] nrofPodCarrier;



	public SCFadDestination(Settings s)
	{
		Settings scfadSettings = new Settings("SCFad");
		 G = scfadSettings.getInt("nrofGenerations");
		 K = scfadSettings.getInt("nrofInjections");
		 P = scfadSettings.getInt("nrofPods");
	}

	public SCFadDestination(SCFadDestination lcncp)
	{

		decodingMatrices = new Matrix[P];
		rows = new int[P];
		nrofPodCarrier = new int[P];
		encodedPods = new HashSet<String>();
		for(int i=0;i<decodingMatrices.length;i++)
			decodingMatrices[i]=new Matrix(5*K,G);


	}

	public RoutingDecisionEngine replicate()
	{
		return new SCFadDestination(this);
	}

	public void connectionDown(DTNHost thisHost, DTNHost peer){

		
	}

	public void connectionUp(DTNHost thisHost, DTNHost peer){
		
		
		/*
		 * count how many peer has a pod packet of podi
		 */
		
		for(int i=0;i<P;i++)
		{
			if(encodedPods.contains("P"+i))
				continue;

			for(Message m:peer.getRouter().getMessageCollection())
			{
				String[] id = m.getId().split(":");
				int pod = Integer.parseInt(id[0].substring(1));
				if(pod==i)
				{
					this.nrofPodCarrier[pod]++;
					break;
				}

			}	
		}


	}

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{

	}

	public boolean isFinalDest(Message m, DTNHost from,DTNHost me)
	{
		/*Inform as delivered just if it increases the rank*/
		String[] id = m.getId().split(":");
		String lc = id[1];
		int pod = Integer.parseInt(id[0].substring(1));
		
		
		int curRank = decodingMatrices[pod].rank();
		insertRowInto(pod,lc);		

		if(decodingMatrices[pod].rank()>=G && !encodedPods.contains("P"+pod))
		{
			encodedPods.add("P"+pod);
			return true;
		}

		else if(decodingMatrices[pod].rank()>curRank)
			return true;
			
		/*put it in the delivered without telling the message listeners so that we have delivered = rank in the final report*/
		me.getRouter().deliveredMessages.put(m.getId(), m);
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

		return false;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return false;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		return false;

	}



	public boolean shouldSortOldestMessages(){
		return false;
	}

	public int compareToSort(Message msg1, Message msg2, Connection con1, Connection con2, DTNHost me){

		return 0;

	}

	void insertRowInto(int pod,String s){

		if(s.length() > decodingMatrices[pod].getColumnDimension())
			return;

		int i=0;
		for(byte b:s.getBytes())
			decodingMatrices[pod].set(rows[pod],i++, (double)(b-48));


				rows[pod]++;


	}


	public Set<String> getEncodedPods() {
		return encodedPods;
	}

	

}
