BR L_Prog_lab1

L_Function_it2:
POP -1(SP)
PUSH D2
MOV SP D2
ADD SP #3
WRT 
POP 2(D2)
SUB SP #3
PUSH 2(D2)
MOV D2 3(D2)
RET
{End block}


L_Function_it2b:
POP -1(SP)
PUSH D2
MOV SP D2
ADD SP #3
WRT 
POP 2(D2)
SUB SP #3
PUSH 2(D2)
MOV D2 3(D2)
RET
{End block}


L_Procedure_it1:
POP -2(SP)
POP -2(SP)
PUSH D1
MOV SP D1
ADD SP #5
WRT 
WRT 
WRT 
SUB SP #5
MOV D1 5(D1)
RET
{End block}


L_Prog_lab1:
POP -2(SP)
PUSH D0
MOV SP D0
ADD SP #4
RD 1(D0)
RDF 2(D0)
WRT 
SUB SP #4
MOV D0 4(D0)
{End block}

HLT
