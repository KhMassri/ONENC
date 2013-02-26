package routing;

import java.util.*;


import core.*;

/**
 * This class overrides ActiveRouter in order to inject calls to a 
 * DecisionEngine object where needed add extract as much code from the update()
 * method as possible. 
 * 
 * <strong>Forwarding Logic:</strong> 
 * 
 * A DecisionEngineRouter maintains a List of Tuple<Message, Connection> in 
 * support of a call to ActiveRouter.tryMessagesForConnected() in 
 * DecisionEngineRouter.update(). Since update() is called so frequently, we'd 
 * like as little computation done in it as possible; hence the List that gets
 * updated when events happen. Four events cause the List to be updated: a new 
 * message from this host, a new received message, a connection goes up, or a 
 * connection goes down. On a new message (either from this host or received 
 * from a peer), the collection of open connections is examined to see if the
 * message should be forwarded along them. If so, a new Tuple is added to the
 * List. When a connection goes up, the collection of messages is examined to 
 * determine to determine if any should be sent to this new peer, adding a Tuple
 * to the list if so. When a connection goes down, any Tuple in the list
 * associated with that connection is removed from the List.
 * 
 * <strong>Decision Engines</strong>
 * 
 * Most (if not all) routing decision making is provided by a 
 * RoutingDecisionEngine object. The DecisionEngine Interface defines methods 
 * that enact computation and return decisions as follows:
 * 
 * <ul>
 *   <li>In createNewMessage(), a call to RoutingDecisionEngine.newMessage() is 
 * 	 made. A return value of true indicates that the message should be added to
 * 	 the message store for routing. A false value indicates the message should
 *   be discarded.
 *   </li>
 *   <li>changedConnection() indicates either a connection went up or down. The
 *   appropriate connectionUp() or connectionDown() method is called on the
 *   RoutingDecisionEngine object. Also, on connection up events, this first
 *   peer to call changedConnection() will also call
 *   RoutingDecisionEngine.doExchangeForNewConnection() so that the two 
 *   decision engine objects can simultaneously exchange information and update 
 *   their routing tables (without fear of this method being called a second
 *   time).
 *   </li>
 *   <li>Starting a Message transfer, a protocol first asks the neighboring peer
 *   if it's okay to send the Message. If the peer indicates that the Message is
 *   OLD or DELIVERED, call to RoutingDecisionEngine.shouldDeleteOldMessage() is
 *   made to determine if the Message should be removed from the message store.
 *   <em>Note: if tombstones are enabled or deleteDelivered is disabled, the 
 *   Message will be deleted and no call to this method will be made.</em>
 *   </li>
 *   <li>When a message is received (in messageTransferred), a call to 
 *   RoutingDecisionEngine.isFinalDest() to determine if the receiving (this) 
 *   host is an intended recipient of the Message. Next, a call to 
 *   RoutingDecisionEngine.shouldSaveReceivedMessage() is made to determine if
 *   the new message should be stored and attempts to forward it on should be
 *   made. If so, the set of Connections is examined for transfer opportunities
 *   as described above.
 *   </li>
 *   <li> When a message is sent (in transferDone()), a call to 
 *   RoutingDecisionEngine.shouldDeleteSentMessage() is made to ask if the 
 *   departed Message now residing on a peer should be removed from the message
 *   store.
 *   </li>
 * </ul>
 * 
 * <strong>Tombstones</strong>
 * 
 * The ONE has the the deleteDelivered option that lets a host delete a message
 * if it comes in contact with the message's destination. More aggressive 
 * approach lets a host remember that a given message was already delivered by
 * storing the message ID in a list of delivered messages (which is called the
 * tombstone list here). Whenever any node tries to send a message to a host 
 * that has a tombstone for the message, the sending node receives the 
 * tombstone.
 * 
 * @author PJ Dillon, University of Pittsburgh
 */

/*
 * From Khalil
 * I made the decider to be a field in the MessageRouter 
 * this field is constructed in SimScenario calss then setted to the MessageRouter super class
 * and then is replicated to each new object
 * By this I can make a defferent decider for each group and the type of this decider is configured in the configuration txt file
 * as a groupx.DecisionEngine
 */
public class DecisionEngineRouter extends ActiveRouter
{
	public static final String PUBSUB_NS = "DecisionEngineRouter";
	public static final String ENGINE_SETTING = "decisionEngine";
	public static final String TOMBSTONE_SETTING = "tombstones";
	public static final String CONNECTION_STATE_SETTING = "";
	
	
	protected boolean tombstoning; //this to make all nodes know about all delivered msgs at the same time
	
	//protected RoutingDecisionEngine decider;
	protected List<Tuple<Message, Connection>> outgoingMessages;
	
	protected Set<String> tombstones;
	
	/** 
	 * Used to save state machine when new connections are made. See comment in
	 * changedConnection() 
	 */
	protected Map<Connection, Integer> conStates;
	

	public DecisionEngineRouter(Settings s)
	{
		super(s);
		
		Settings routeSettings = new Settings(PUBSUB_NS);
		
	
		
		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		
//	decider = (RoutingDecisionEngine)routeSettings.createIntializedObject(
//				"routing." + routeSettings.getSetting(ENGINE_SETTING));
//		
		if(routeSettings.contains(TOMBSTONE_SETTING))
			tombstoning = routeSettings.getBoolean(TOMBSTONE_SETTING);
		else
			tombstoning = false;
		
		if(tombstoning)
			tombstones = new HashSet<String>(10);
		conStates = new HashMap<Connection, Integer>(4);
	}

	public DecisionEngineRouter(DecisionEngineRouter r)
	{
		super(r);

		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		
		tombstoning = r.tombstoning;
		
		if(this.tombstoning)
			tombstones = new HashSet<String>(10);
		conStates = new HashMap<Connection, Integer>(4);
	}

	@Override
	public MessageRouter replicate()
	{
		return new DecisionEngineRouter(this);
	}

	@Override
	public boolean createNewMessage(Message m)
	{
		if(decider.newMessage(m))
		{
			
			if(super.createNewMessage(m))
			{
				findConnectionsForNewMessage(m, getHost());
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void changedConnection(Connection con)
	{
		DTNHost myHost = getHost();
		DTNHost otherNode = con.getOtherNode(myHost);
		DecisionEngineRouter otherRouter = (DecisionEngineRouter)otherNode.getRouter();
		if(con.isUp())
		{
			decider.connectionUp(myHost, otherNode);
			
			/*
			 * This part is a little confusing because there's a problem we have to
			 * avoid. When a connection comes up, we're assuming here that the two 
			 * hosts who are now connected will exchange some routing information and
			 * update their own based on what the get from the peer. So host A updates
			 * its routing table with info from host B, and vice versa. In the real
			 * world, A would send its *old* routing information to B and compute new
			 * routing information later after receiving B's *old* routing information.
			 * In ONE, changedConnection() is called twice, once for each host A and
			 * B, in a serial fashion. If it's called for A first, A uses B's old info
			 * to compute its new info, but B later uses A's *new* info to compute its
			 * new info.... and this can lead to some nasty problems. 
			 * 
			 * To combat this, whichever host calls changedConnection() first calls
			 * doExchange() once. doExchange() interacts with the DecisionEngine to
			 * initiate the exchange of information, and it's assumed that this code
			 * will update the information on both peers simultaneously using the old
			 * information from both peers.
			 */
			if(shouldNotifyPeer(con))
			{
				this.doExchange(con, otherNode);
				otherRouter.didExchange(con);
			}
			
			/*
			 * Once we have new information computed for the peer, we figure out if
			 * there are any messages that should get sent to this peer.
			 */
			Collection<Message> msgs = getMessageCollection();
			for(Message m : msgs)
			{
				if(decider.shouldSendMessageToHost(m,getHost(), otherNode))
					outgoingMessages.add(new Tuple<Message,Connection>(m, con));
			}
		}
		else
		{
			decider.connectionDown(myHost, otherNode);
			
			conStates.remove(con);
			
			/*
			 * If we  were trying to send message to this peer, we need to remove them
			 * from the outgoing List.
			 */
			for(Iterator<Tuple<Message,Connection>> i = outgoingMessages.iterator(); 
					i.hasNext();)
			{
				Tuple<Message, Connection> t = i.next();
				if(t.getValue() == con)
					i.remove();
			}
		}
	}
	
	protected void doExchange(Connection con, DTNHost otherHost)
	{
		conStates.put(con, 1);
		decider.doExchangeForNewConnection(con, otherHost);
	}
	
	/**
	 * Called by a peer DecisionEngineRouter to indicated that it already 
	 * performed an information exchange for the given connection.
	 * 
	 * @param con Connection on which the exchange was performed
	 */
	protected void didExchange(Connection con)
	{
		conStates.put(con, 1);
	}
	
	@Override
	protected int startTransfer(Message m, Connection con)
	{
		int retVal;
		
		if (!con.isReadyForTransfer()) {
			return TRY_LATER_BUSY;
		}
		
		retVal = con.startTransfer(getHost(), m);
		
		if (retVal == RCV_OK) { // started transfer
			addToSendingConnections(con);
		}
		
		else if(tombstoning && retVal == DENIED_DELIVERED)
		{
			if(this.hasMessage(m.getId()) && !this.isSending(m.getId())){
			this.deleteMessage(m.getId(), false);
			tombstones.add(m.getId());}
		}
		else if (deleteDelivered && (retVal == DENIED_DELIVERED) ||
				(retVal == DENIED_OLD ) && decider.shouldDeleteOldMessage(m, con.getOtherNode(getHost())))
		{
			/* final recipient has already received the msg -> delete it */
			if(this.hasMessage(m.getId()) && !this.isSending(m.getId()))
			this.deleteMessage(m.getId(), false);
		}
		
		return retVal;
	}

	@Override
	public int receiveMessage(Message m, DTNHost from)
	{
		if(isDeliveredMessage(m) || (tombstoning && tombstones.contains(m.getId())))
			return DENIED_DELIVERED;
			
		return super.receiveMessage(m, from);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from)
	{
		Message incoming = removeFromIncomingBuffer(id, from);
	
		if (incoming == null) {
			throw new SimError("No message with ID " + id + " in the incoming "+
					"buffer of " + getHost());
		}
		
		incoming.setReceiveTime(SimClock.getTime());
		
		Message outgoing = incoming;
		for (Application app : getApplications(incoming.getAppID())) {
			// Note that the order of applications is significant
			// since the next one gets the output of the previous.
			outgoing = app.handle(outgoing, getHost());
			if (outgoing == null) break; // Some app wanted to drop the message
		}
		
		Message aMessage = (outgoing==null)?(incoming):(outgoing);
		
		boolean isFinalRecipient = decider.isFinalDest(aMessage,from, getHost());
		boolean isFirstDelivery =  isFinalRecipient && 
			!isDeliveredMessage(aMessage);
		
		if (outgoing!=null && decider.shouldSaveReceivedMessage(aMessage, getHost())) 
		{
			// not the final recipient and app doesn't want to drop the message
			// -> put to buffer
			addToMessages(aMessage, false);
			
			// Determine any other connections to which to forward a message
			findConnectionsForNewMessage(aMessage, from);
		}
		
		if (isFirstDelivery)
		{
			this.deliveredMessages.put(id, aMessage);
		}
		
		for (MessageListener ml : this.mListeners) {
			ml.messageTransferred(aMessage, from, getHost(),
					isFirstDelivery);
		}
		
		return aMessage;
	}

	@Override
	protected void transferDone(Connection con)

	{
		
		
		Message transferred = this.getMessage(con.getMessage().getId());
		
		if(transferred==null)
		{
			for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
			i.hasNext();)
			{
				Tuple<Message, Connection> t = i.next();
				if(t.getKey().getId().equals(con.getMessage().getId()))
				{
					i.remove();
					}
				}
			return;
		}
	
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator();i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(t.getKey().getId().equals(transferred.getId()) && t.getValue().equals(con))
			{
				i.remove();
				break;
			}
		}
		
		
		
		if(decider.shouldDeleteSentMessage(transferred, con.getOtherNode(getHost())))
			
			{
			
			this.deleteMessage(transferred.getId(), false);
			
			for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
			i.hasNext();)
			{
				Tuple<Message, Connection> t = i.next();
				if(t.getKey().getId().equals(transferred.getId()))
				{
					i.remove();
					}
				}
			}
		
		
		/*
		 * Rechecks outgoing messages after sending a message.
		 * so maybe the condition changed 
		 */
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator();i.hasNext();)
				{
					Tuple<Message, Connection> t = i.next();
					if(!decider.shouldSendMessageToHost(t.getKey(),getHost(), t.getValue().getOtherNode(getHost())))
					{
						i.remove();
						}
					}
		
	}

	@Override
	public void update()
	{
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		/*
		 * Rechecks the outgoing messages
		 */
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator();i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(!decider.shouldSendMessageToHost(t.getKey(),getHost(), t.getValue().getOtherNode(getHost())))
			{
				i.remove();
				}
			}
		
		/*
		 * Decide to Sort the message befor sending or not.
		 * 
		 */
		
		if(decider.shouldSortOldestMessages())
		{
			
			Collections.sort(outgoingMessages,new Comparator<Tuple<Message,Connection>>() {
				public int compare(Tuple<Message,Connection> t1, Tuple<Message,Connection> t2) {
					
					Message msg1 = t1.getKey();
					Message msg2 = t2.getKey();
					
					if(decider.compareToSort(msg1,msg2) == 0)
						return compareByQueueMode(msg1, msg2);
					
					return decider.compareToSort(msg1,msg2);
					}		
				} 
			); 
		}
		
		
		tryMessagesForConnected(outgoingMessages);
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
		i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(!this.hasMessage(t.getKey().getId()))
			{
				i.remove();
			}
		}
	}
	
	public RoutingDecisionEngine getDecisionEngine()
	{
		return this.decider;
	}

	protected boolean shouldNotifyPeer(Connection con)
	{
		Integer i = conStates.get(con);
		return i == null || i < 1;
	}
	
	protected void findConnectionsForNewMessage(Message m, DTNHost from)
	{
		for(Connection c : getConnections())
		{
			DTNHost other = c.getOtherNode(getHost());
			if(other != from && decider.shouldSendMessageToHost(m,getHost(), other))
			{
				outgoingMessages.add(new Tuple<Message, Connection>(m, c));
			}
		}
	}

	
	/*check for QM*/
	
	protected Message getOldestMessage(boolean excludeMsgBeingSent) {
		
		Collection<Message> messages = this.getMessageCollection();
		List<Message> validMessages = new ArrayList<Message>();
		Message oldest = null;
		
		if(!decider.shouldSortOldestMessages())
		{
			for (Message m : messages) {
			if (oldest == null ) {
				oldest = m;
			}
			else if (oldest.getReceiveTime() > m.getReceiveTime()) {
				oldest = m;
				}
			return oldest;
			}
		}
		
		else 
			{
	
			for (Message m : messages) {	
				if (excludeMsgBeingSent && isSending(m.getId())) {
					continue; // skip the message(s) that router is sending
				}
				validMessages.add(m);
			}
			
			Collections.sort(validMessages,new Comparator<Message>() {
				public int compare(Message msg1, Message msg2) {
					
					if(decider.compareToSort(msg1,msg2) == 0)
						return compareByQueueMode(msg1, msg2);
					
					return decider.compareToSort(msg1,msg2);
					}		
				} 
			); 
			
			oldest =  validMessages.get(validMessages.size()-1); // return last message
			}
		
		return oldest;
	}
		
	
	
	
	
	
	
	@Override
	public String hello() {
		// TODO Auto-generated method stub
		return null;
	}
}
