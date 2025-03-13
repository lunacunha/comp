grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

WHILE : 'while' ;

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z0-9_]* ;

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

importDecl
    : 'import' ID ('.' ID)* SEMI_COLON
    ;

program
    : (importDecl)* classDecl EOF
    ;


classDecl
    : CLASS name=ID ( 'extends' ID )? LEFT_BRACE
        varDecl*
        methodDecl*
      RIGHT_BRACE
    ;


varDecl
    : type ID SEMI_COLON
    ;

type
    : INT
    | BOOLEAN
    | ID
    | INT '...'
    | BOOLEAN '...'
    | INT LEFT_BRACKET RIGHT_BRACKET
    ;

methodDecl
    : (PUBLIC)? type ID LEFT_PARENTHESES (param (',' param)*)? RIGHT_PARENTHESES
      LEFT_BRACE varDecl* stmt* RETURN expr SEMI_COLON RIGHT_BRACE
    | (PUBLIC)? 'static' 'void' 'main' LEFT_PARENTHESES ID LEFT_BRACKET RIGHT_BRACKET ID RIGHT_PARENTHESES
      LEFT_BRACE varDecl* stmt* RIGHT_BRACE
    ;

param
    : type ID
    ;

stmt
    : WHILE LEFT_PARENTHESES expr RIGHT_PARENTHESES stmt             #WhileStatement
    | 'if' LEFT_PARENTHESES expr RIGHT_PARENTHESES stmt 'else' stmt  #IfStatement
    | LEFT_BRACE stmt* RIGHT_BRACE                                   #BlockStatement
    | ID EQUAL expr SEMI_COLON                                       #AssingStatement
    | ID LEFT_BRACKET expr RIGHT_BRACKET EQUAL expr SEMI_COLON       #ArrayAssignStatement
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
    | expr '.' ID                                                         #FieldAccess
    | expr LEFT_BRACKET expr RIGHT_BRACKET                                #ArrayAccess
    | expr '.' 'length'                                                   #ArrayLength
    | LEFT_BRACKET (expr (',' expr)*)? RIGHT_BRACKET                      #ArrayInit
    | expr '.' ID LEFT_PARENTHESES (expr (',' expr)*)? RIGHT_PARENTHESES  #MethodCall
    | 'new' INT LEFT_BRACKET expr RIGHT_BRACKET                           #NewArray
    | 'new' ID LEFT_PARENTHESES RIGHT_PARENTHESES                         #NewObject
    | NOT expr                                                            #Negate
    | LEFT_PARENTHESES expr RIGHT_PARENTHESES                             #ParenthesesExpr
    | TRUE                                                                #BooleanLiteral
    | FALSE                                                               #BooleanLiteral
    | INTEGER                                                             #IntegerLiteral
    | ID                                                                  #VarRefExpr
    | 'this'                                                              #ThisExpr
    ;


