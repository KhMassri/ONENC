for k in 15 20 30
do
for c in 0 1
do
echo "=====> coding is $c"" and K = $k"
cat "SCFadSourceG10K$k""B32kC$c""S"* | awk '
/nrof_encounters_at_encoding:/ { a+=$2 ;enc++}
/active_encounters_at_encoding:/ { b+=$2 }
/delivered:/ { d+=$2 ;del++}
/relayed:/ { e+=$2 } 
END { print "nrofEnc = "(a+0)/enc "\n actvEnc = "(b+0)/enc "\n delvrdPkts = "(d+0)/del "\n trans = "(e+0)/del "\n success = "(enc+0.0)/del }'
done
done
