
import numpy as np
import glob


#"### Delay  ActiveEncounters  deliverdPackets  TotalRelayes  DecodinProb =  "


#/nrof_encounters_at_encoding:/ { a+=$2 ;enc++}
#/active_encounters_at_encoding:/ { b+=$2 }
#/delivered:/ { d+=$2 ;del++}
#/relayed:/ { e+=$2 } 
#END { print (a+0)/enc "\t"(b+0)/enc "\t"(d+0)/del "\t"(e+0)/del "\t"(enc+0.0)/del }'
#full_delivery_time:
#last_delivery_time:


G=['20']
K=['20','30','40','50','60','70','80']
C=['0','1']
Seeds = list(range(40))

# out.write('Decoded     Latency      Delivered')

#  5:  5 10  15  20 30
# 10: 10 15  20  25 30 40 50
# 15: 15 20  30 40 50 60 70
# 20: 20  30  40 50 60 70 80 

for g in G:
    out = open('G'+g+'.txt','w')

    for c in C:
        for k in K:
            print 'G',g,'K',k,'C',c
            delivered =[]
            decoded = []
            latency = []
            
            for seed in Seeds:
                isDec = False
                for line in open('./finalResults77Node/SCFadSourceG'+g+'K'+k+'B32kC'+c+'S'+str(seed)+'_MessageStatsReport.txt').readlines():
                    record=line.split()
                    if record[0]=='delivered:':
                        delivered.append(int(record[1]))
                        if int(record[1]) >= int(g):
                            decoded.append(1)
                            isDec = True
                        else:
                            decoded.append(0)    
                            
                    elif record[0]=='full_delivery_time:'and int(c)==0 and isDec:
                        latency.append(float(record[1]))
                    elif record[0]=='last_delivery_time:'and int(c)==1 and isDec:
                        latency.append(float(record[1]))
                    #elif record[0]=='last_delivery_time:' and not isDec:
                      #  latency.append(float(record[1]))
            print decoded
            #print len(latency),sum(decoded)
            avgDel = sum(delivered)/(0.0+len(delivered))
            avgDec = sum(decoded)/(0.0+len(decoded))
            
            if len(latency)>0:
                avgLat = sum(latency)/(0.0+len(latency))
            else:
                avgLat = 341000.0
                                 
            out.write(str(avgDec)+' \t '+str(avgLat)+' \t '+str(avgDel)+'\n')
            



                    
                
                
    
#map(lambda x: int(x), ['1', '2', '3'])
#lst.sort(key=lambda x: x[4],reverse=True)
#matrix.sort(key=lambda x: x[0],reverse=True)
#con = zeros((10000,4), dtype=str)
