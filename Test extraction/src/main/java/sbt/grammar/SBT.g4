sbtGrammar SBT;

@src.parser::header
{
    package src.sbtGrammar;

    import com.github.javaparser.ast.Node;
    import application.utilities.ObjectCreation;
}

@lexer::header
{
    package src.sbtGrammar;
}

node returns [Object result]
    :   f=OPEN i=IDENT n=nodes CLOSE IDENT {
            $result = ObjectCreation.createObjectFromSBTString($i.text, $n.result);
        }
    ;

nodes returns [List<Object> result]
    @init  { List<Object> objects = new ArrayList<>(); }
    @after { $result = objects; }
    : (n=node{
        objects.add($n.result);
    })*
    ;

WS  :	(' ' | '\t' | '\n' | '\r') -> skip;

COMMENT : '/*' .*? '*/'  -> skip;

SINGLE_COMMENT : '//'  ~[\r\n]*  -> skip;

//IDENT:   ( '\\(' | '\\)' | ~('(' | ')'))+{_input.LA(1) != '\\'}?; //cannot be folloewd by \

//IDENT:   ( {_input.LA(-1) != '\\'}?'\\(' | {_input.LA(-1) != '\\'}?'\\)' | ~('(' | ')'))+; //This rule will consume it?

IDENT: ( '\\\\' | '\\(' | '\\)' | '\\_' | ~('(' | ')' | '\\'))+;

OPEN: '(';

CLOSE: ')';

//OPEN: { _input.LA(-2) == '\\' || _input.LA(-1) != '\\' }?'(';

//CLOSE: {_input.LA(-2) == '\\' || _input.LA(-1) != '\\'}?')';
