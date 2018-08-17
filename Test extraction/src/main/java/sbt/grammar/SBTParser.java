// Generated from SBT.g4 by ANTLR 4.5.1

    package sbt.grammar;

    import application.utilities.ObjectCreation;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
    import org.antlr.v4.runtime.tree.*;
import java.util.List;
    import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SBTParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, COMMENT=2, SINGLE_COMMENT=3, IDENT=4, OPEN=5, CLOSE=6;
	public static final int
		RULE_node = 0, RULE_nodes = 1;
	public static final String[] ruleNames = {
		"node", "nodes"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, null, null, null, "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "WS", "COMMENT", "SINGLE_COMMENT", "IDENT", "OPEN", "CLOSE"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "SBT.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SBTParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class NodeContext extends ParserRuleContext {
		public Object result;
		public Token f;
		public Token i;
		public NodesContext n;
		public TerminalNode CLOSE() { return getToken(SBTParser.CLOSE, 0); }
		public List<TerminalNode> IDENT() { return getTokens(SBTParser.IDENT); }
		public TerminalNode IDENT(int i) {
			return getToken(SBTParser.IDENT, i);
		}
		public TerminalNode OPEN() { return getToken(SBTParser.OPEN, 0); }
		public NodesContext nodes() {
			return getRuleContext(NodesContext.class,0);
		}
		public NodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_node; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SBTListener ) ((SBTListener)listener).enterNode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SBTListener ) ((SBTListener)listener).exitNode(this);
		}
	}

	public final NodeContext node() throws RecognitionException {
		NodeContext _localctx = new NodeContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_node);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(4);
			((NodeContext)_localctx).f = match(OPEN);
			setState(5);
			((NodeContext)_localctx).i = match(IDENT);
			setState(6);
			((NodeContext)_localctx).n = nodes();
			setState(7);
			match(CLOSE);
			setState(8);
			match(IDENT);

			            ((NodeContext)_localctx).result =  ObjectCreation.createObjectFromSBTString((((NodeContext)_localctx).i!=null?((NodeContext)_localctx).i.getText():null), ((NodeContext)_localctx).n.result);
			        
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NodesContext extends ParserRuleContext {
		public List<Object> result;
		public NodeContext n;
		public List<NodeContext> node() {
			return getRuleContexts(NodeContext.class);
		}
		public NodeContext node(int i) {
			return getRuleContext(NodeContext.class,i);
		}
		public NodesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodes; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SBTListener ) ((SBTListener)listener).enterNodes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SBTListener ) ((SBTListener)listener).exitNodes(this);
		}
	}

	public final NodesContext nodes() throws RecognitionException {
		NodesContext _localctx = new NodesContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_nodes);
		 List<Object> objects = new ArrayList<>(); 
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(16);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OPEN) {
				{
				{
				setState(11);
				((NodesContext)_localctx).n = node();

				        objects.add(((NodesContext)_localctx).n.result);
				    
				}
				}
				setState(18);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
			 ((NodesContext)_localctx).result =  objects; 
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\b\26\4\2\t\2\4\3"+
		"\t\3\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\7\3\21\n\3\f\3\16\3\24\13"+
		"\3\3\3\2\2\4\2\4\2\2\24\2\6\3\2\2\2\4\22\3\2\2\2\6\7\7\7\2\2\7\b\7\6\2"+
		"\2\b\t\5\4\3\2\t\n\7\b\2\2\n\13\7\6\2\2\13\f\b\2\1\2\f\3\3\2\2\2\r\16"+
		"\5\2\2\2\16\17\b\3\1\2\17\21\3\2\2\2\20\r\3\2\2\2\21\24\3\2\2\2\22\20"+
		"\3\2\2\2\22\23\3\2\2\2\23\5\3\2\2\2\24\22\3\2\2\2\3\22";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}