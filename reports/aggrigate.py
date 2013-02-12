
from numpy import *
from decimal import Decimal
import glob


#"### Delay  ActiveEncounters  deliverdPackets  TotalRelayes  DecodinProb =  "


#/nrof_encounters_at_encoding:/ { a+=$2 ;enc++}
#/active_encounters_at_encoding:/ { b+=$2 }
#/delivered:/ { d+=$2 ;del++}
#/relayed:/ { e+=$2 } 
#END { print (a+0)/enc "\t"(b+0)/enc "\t"(d+0)/del "\t"(e+0)/del "\t"(enc+0.0)/del }'
#full_delivery_time:
#last_delivery_time:

C=['0','1']
G=['10']
K=['10','20']

result  = open("results.txt","w")
for g in G:
    for k in K:
        for c in C:
            print 'G',g,'K',k,'C',c
            delivered =[]
            decoded = []
            latency = []
            isDec = False
            
            for f in glob.iglob('./10/SCFadSourceG'+g+'K'+k+'B32kC'+c+'S?_Message*.txt'):
                for line in open(f).readlines():
                    record=line.split()
                    if record[0]=='delivered:':
                        delivered.append(int(record[1]))
                        if int(record[1]) >= int(g):
                            decoded.append(1)
                            isDec = True
                        else:
                            decoded.append(0)    
                            
                    elif record[0]=='full_delivery_time:'and int(c)==0 and isDec:
                        latency.append(record[1])
                    elif record[0]=='last_delivery_time:'and int(c)==1 and isDec:
                        latency.append(record[1])
                    elif record[0]=='last_delivery_time:' and not isDec:
                        latency.append(341042)
            
            print delivered
            print decoded
            print latency
            #print sum(delivered)/(0.0+len(delivered)),(sum(decoded)/(0.0+len(decoded)))
                        
                    
                
                
    
'''''
    
    hcnts=0.0
    re=0
    # each list is:  
    # device,recvTime,MsgSeq,crtTime,hpcnt
    for line in logfile:
        lst=line.split(',')
        if not (int(lst[1])==int(lst[3])):
            re =re+1
            hcnts = hcnts+int(lst[4])

    avghpcnt.append(hcnts/re)
    remsgs.append(re)

#remsgs.sort()
#avghpcnt.sort()
print remsgs
print ['%0.2f' %i for i in avghpcnt]


sinked = []
TAGS = tags+emptags
perNodeSinked = zeros(len(TAGS), int)        # Create empty array ready to receive result
for i in range(0, len(TAGS)):
    perNodeSinked[i] = 0
shpcnt = 0.0
delay = 0.0
s = 0
lst=[]
for tag in sinks:
    logfile = open("../data/LOG-"+tag+".CSV").readlines()
    for line in logfile:
        lst.append(line.split(','))

lst.sort(key=lambda x: x[4],reverse=True)
for l in lst:
    if not(l[2] in sinked):
        sinked.append(l[2])
        delay=delay+(int(l[1])-int(l[3]))
        s=s+1
        shpcnt=shpcnt+int(l[4])
        if '35F8' not in (l[2])[0:4]:
            perNodeSinked[TAGS.index((l[2])[0:4])] = perNodeSinked[TAGS.index((l[2])[0:4])] +1  
    
print 'Total sinked = %.2f ' %(len(sinked))
#print 'Total sinked = %.2f ' %(len(sinked)/(20*4500/30.0))
print 'sinked HopCount Avg = %.2f ' %(shpcnt/s+1)
print 'sinked delay Avg = %.2f ' %(delay/s)
print perNodeSinked 


#matrix.sort(key=lambda x: x[0],reverse=True)
#print matrix
#str(lst[2])[0:4]==emptags[0] or str(lst[2])[0:4]==emptags[1] or str(lst[2])[0:4]==emptags[2] or str(lst[2])[0:4]==emptags[3]
#con.view('i8,i8,i8,i8').sort(order=['f0'], axis=0)
#con = zeros((10000,4), dtype=str)
#out.write(line[0]+line[1]+line[2]+'\n')'''''
