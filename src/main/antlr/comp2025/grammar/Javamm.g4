grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;

WHILE : 'while' ;

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9_]* ;

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

program
    : (importDecl)* classDecl+ EOF
    ;

importDecl
    : IMPORT name=ID ('.' ID)* SEMI_COLON
    ;

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
    : INT '...'                  #VarArgInt
    | BOOLEAN '...'               #VarArgBool
    | INT                         #IntType
    | BOOLEAN                     #BooleanType
    | name=ID                          #ClassType
    | INT LEFT_BRACKET RIGHT_BRACKET #IntArrayType
    ;


methodDecl
    : (PUBLIC)? type name=ID LEFT_PARENTHESES (paramDecl (',' paramDecl)*)? RIGHT_PARENTHESES
            LEFT_BRACE varDecl* stmt* RIGHT_BRACE
    | (PUBLIC)? 'static' 'void' name='main' LEFT_PARENTHESES ID LEFT_BRACKET RIGHT_BRACKET ID RIGHT_PARENTHESES
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
    | 'if' LEFT_PARENTHESES expr RIGHT_PARENTHESES stmt 'else' stmt  #IfStatement
    | LEFT_BRACE stmt* RIGHT_BRACE                                   #BlockStatement
    | name=ID EQUAL expr SEMI_COLON                                       #AssignStatement
    | name=ID LEFT_BRACKET expr RIGHT_BRACKET EQUAL expr SEMI_COLON       #ArrayAssignStatement
    | expr SEMI_COLON                                                #ExprStatement
    | RETURN expr SEMI_COLON                                         #ReturnStatement
    ;

expr
    : expr MULT expr                                                      #MultiplicationExpr
    | expr PLUS expr                                                      #AdditionExpr
    | expr SUB expr                                                       #SubtractionExpr
    | expr DIV expr                                                       #DivisionExpr
    | expr AND expr                                                       #AndExpr
    | expr LESS_THAN expr                                                 #LessThanExpr
    | expr '.' name=ID                                                         #FieldAccess
    | expr LEFT_BRACKET expr RIGHT_BRACKET                                #ArrayAccess
    | expr '.' 'length'                                                   #ArrayLength
    | LEFT_BRACKET (expr (',' expr)*)? RIGHT_BRACKET                      #ArrayInit
    | expr '.' name=ID LEFT_PARENTHESES (expr (',' expr)*)? RIGHT_PARENTHESES  #MethodCall
    | 'new' INT LEFT_BRACKET expr RIGHT_BRACKET                           #NewArray
    | 'new' name=ID LEFT_PARENTHESES RIGHT_PARENTHESES                         #NewObject
    | NOT expr                                                            #Negate
    | LEFT_PARENTHESES expr RIGHT_PARENTHESES                             #ParenthesesExpr
    | TRUE                                                                #BooleanLiteral
    | FALSE                                                               #BooleanLiteral
    | INTEGER                                                             #IntegerLiteral
    | name=ID                                                                  #VarRefExpr
    | 'this'                                                              #ThisExpr
    ;


