BR L_Prog_lab1

L_Function_fred:
POP -1(SP)
PUSH D2
MOV SP D2
ADD SP #3
POP 2(D2)
SUB SP #3
PUSH 2(D2)
MOV D2 3(D2)
RET
{End block}


L_Function_mary:
POP -1(SP)
PUSH D2
MOV SP D2
ADD SP #3
POP 2(D2)
SUB SP #3
PUSH 2(D2)
MOV D2 3(D2)
RET
{End block}


L_Procedure_mixit:
POP -3(SP)
POP -3(SP)
POP -3(SP)
PUSH D1
MOV SP D1
ADD SP #7
POP 4(D1)
SUB SP #7
MOV D1 7(D1)
RET
{End block}


L_Prog_lab1:
POP -5(SP)
PUSH D0
MOV SP D0
ADD SP #7
RD 1(D0)
RDF 3(D0)
WRT 
SUB SP #7
MOV D0 7(D0)
{End block}

HLT
