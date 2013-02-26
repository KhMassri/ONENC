package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import core.ConnectionListener;
import core.DTNHost;
import core.UpdateListener;

public class EncodingReport extends Report implements ConnectionListener,
	UpdateListener {

	private int[] encounters;
	ArrayList<Integer> dest;
	ArrayList<Integer> pod ;
	ArrayList<Integer> doneTime ;
	ArrayList<Integer> nrofCarriers ;
	ArrayList<Integer> enc ;
	
	public EncodingReport() {
		init();
		
	}
	
	protected void init() {
		super.init();
		
		 dest = new ArrayList<Integer>();
		 pod = new ArrayList<Integer>();
		 doneTime = new ArrayList<Integer>();
		 nrofCarriers = new ArrayList<Integer>();
		 enc = new ArrayList<Integer>();
		
	}

	
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (encounters == null) {
			return;
		}
		encounters[host1.getAddress()]++;
		encounters[host2.getAddress()]++;
		
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {}

	public void updated(List<DTNHost> hosts) {
		if (encounters == null) {
			encounters = new int[hosts.size()];
		}
	}

	public void encodingDone(DTNHost dest, int pod,int nrofCarrier){
		
		this.dest.add(dest.getAddress());
		this.pod.add(pod);
		doneTime.add((int) this.getSimTime());
		nrofCarriers.add(nrofCarrier);
		enc.add(encounters[dest.getAddress()]);
		
	
	}
	
	@Override
	public void done() {
		if(pod.size()>0)
			
		for(int i=0;i<pod.size();i++){
			write("time_at_encoding: "+doneTime.get(i));
			write("active_encounters_at_encoding: "+this.nrofCarriers.get(i));
			write("nrof_encounters_at_encoding: "+enc.get(i));
			write("total_encounters: "+encounters[dest.get(i)]);
			
			
		}
		super.done();
	}

	public int[] getEncounters() {
		return encounters;
	}

	public void setEncounters(int[] encounters) {
		this.encounters = encounters;
	}
	
}
