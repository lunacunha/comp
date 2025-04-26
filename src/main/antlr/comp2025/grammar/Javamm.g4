grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
STATIC : 'static' ;
RETURN : 'return' ;
IMPORT : 'import' ;

WHILE : 'while' ;

INTEGER : [0-9]+ ;

COMMENT : '//' ~[\r\n]* -> skip ;
MULTI_LINE_COMMENT : '/*' .*? '*/' -> skip ;

PLUS : '+' ;
SUB : '-' ;
MULT : '*' ;
DIV : '/' ;
EQUAL : '=' ;
SEMI_COLON : ';' ;
LEFT_PARENTHESES : '(' ;
RIGHT_PARENTHESES : ')' ;
LEFT_BRACKET : '[' ;
RIGHT_BRACKET : ']' ;
LEFT_BRACE : '{' ;
RIGHT_BRACE : '}' ;

AND : '&&' ;
LESS_THAN : '<' ;
NOT : '!' ;

TRUE : 'true' ;
FALSE : 'false' ;
BOOLEAN : 'boolean' ;

WS : [ \t\n\r\f]+ -> skip ;
ID : [a-zA-Z_$][a-zA-Z0-9_$]* ;


program
    : (importDecl)* classDecl+ EOF
    ;

importDecl
    : IMPORT importPart SEMI_COLON
    ;
importPart:
    name+=ID ('.' name+=ID)*;

classDecl
    : CLASS name=ID ( 'extends' superClass=ID )? LEFT_BRACE
        varDecl*
        methodDecl*
      RIGHT_BRACE
    ;

varDecl
    : type name=ID SEMI_COLON
    ;

type
    : INT '...'                         #VarArgInt
    | BOOLEAN '...'                     #VarArgBool
    | INT                               #IntType
    | BOOLEAN                           #BooleanType
    | name=ID                                #ClassType
    | name=ID LEFT_BRACKET RIGHT_BRACKET     #ClassArrayType
    | INT LEFT_BRACKET RIGHT_BRACKET    #IntArrayType
    | 'void'                            #VoidType
    ;


methodDecl
    : (pub=PUBLIC)? (stat=STATIC)? type  name=ID LEFT_PARENTHESES (paramDecl (',' paramDecl)*)? RIGHT_PARENTHESES
        LEFT_BRACE varDecl* stmt* RIGHT_BRACE
    ;

paramDecl
    : type name=ID #NormalParam
    | INT '...' name=ID #VarargParam
    ;

paramList
    : paramDecl (',' paramDecl)*
    ;

stmt
    : WHILE LEFT_PARENTHESES expr RIGHT_PARENTHESES stmt             #WhileStatement
    | 'if' LEFT_PARENTHESES expr RIGHT_PARENTHESES stmt ('else' stmt)?  #IfStatement
    | LEFT_BRACE stmt* RIGHT_BRACE                                   #BlockStatement
    | name=ID EQUAL expr SEMI_COLON                                       #AssignStatement
    | name=ID LEFT_BRACKET expr RIGHT_BRACKET EQUAL expr SEMI_COLON       #ArrayAssignStatement
    | expr SEMI_COLON                                                #ExprStatement
    | RETURN expr SEMI_COLON                                         #ReturnStatement
    ;

expr
    : expr '.' name=ID LEFT_PARENTHESES (expr (',' expr)*)? RIGHT_PARENTHESES  #MethodCall
    | expr '.' name=ID                                                         #FieldAccess
    | expr LEFT_BRACKET expr RIGHT_BRACKET                                     #ArrayAccess
    | expr MULT expr                                                           #MultiplicationExpr
    | expr PLUS expr                                                           #AdditionExpr
    | expr SUB expr                                                            #SubtractionExpr
    | expr DIV expr                                                            #DivisionExpr
    | expr AND expr                                                            #AndExpr
    | expr LESS_THAN expr                                                      #LessThanExpr
    | LEFT_BRACKET (expr (',' expr)*)? RIGHT_BRACKET                           #ArrayInit
    | 'new' INT LEFT_BRACKET expr RIGHT_BRACKET                                #NewArray
    | 'new' name=ID LEFT_PARENTHESES RIGHT_PARENTHESES                         #NewObject
    | NOT expr                                                                 #NegationExpr
    | LEFT_PARENTHESES expr RIGHT_PARENTHESES                                  #ParenthesesExpr
    | TRUE                                                                     #BooleanLiteral
    | FALSE                                                                    #BooleanLiteral
    | value=INTEGER                                                            #IntegerLiteral
    | name=ID                                                                  #VarRefExpr
    | 'this'                                                                   #ThisExpr
    ;



