Environment
Field F   (400x200)
Area A1	  (100x200)
Area A2   (100x200)
Area A3	  (10x200)

Constraints
A1 union A2 > F
A1 intersect A2 = 0
A3 < A2

Nodes
G1 - static - WSN nodes in A2
G2 - static - WSN nodes in A1
G3 - moving - DTN protocol implementing nodes
G4 - static - sink nodes   (Not implementd in the simulator, emulated by G2)


>A1 and A2 at the beginning are linked, and they are a WSM.
>Then they desconnect and becomes two island
>Messages from A1 (that is not linked to the sinks in A3) must be carried to A2