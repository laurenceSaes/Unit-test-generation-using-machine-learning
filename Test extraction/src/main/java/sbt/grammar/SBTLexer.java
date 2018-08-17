// Generated from SBT.g4 by ANTLR 4.5.1

    package sbt.grammar;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SBTLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, COMMENT=2, SINGLE_COMMENT=3, IDENT=4, OPEN=5, CLOSE=6;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"WS", "COMMENT", "SINGLE_COMMENT", "IDENT", "OPEN", "CLOSE"
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


	public SBTLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "SBT.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\b=\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3"+
		"\7\3\30\n\3\f\3\16\3\33\13\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\7\4&"+
		"\n\4\f\4\16\4)\13\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\6\5\66"+
		"\n\5\r\5\16\5\67\3\6\3\6\3\7\3\7\3\31\2\b\3\3\5\4\7\5\t\6\13\7\r\b\3\2"+
		"\5\5\2\13\f\17\17\"\"\4\2\f\f\17\17\4\2*+^^C\2\3\3\2\2\2\2\5\3\2\2\2\2"+
		"\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\3\17\3\2\2\2\5\23\3\2"+
		"\2\2\7!\3\2\2\2\t\65\3\2\2\2\139\3\2\2\2\r;\3\2\2\2\17\20\t\2\2\2\20\21"+
		"\3\2\2\2\21\22\b\2\2\2\22\4\3\2\2\2\23\24\7\61\2\2\24\25\7,\2\2\25\31"+
		"\3\2\2\2\26\30\13\2\2\2\27\26\3\2\2\2\30\33\3\2\2\2\31\32\3\2\2\2\31\27"+
		"\3\2\2\2\32\34\3\2\2\2\33\31\3\2\2\2\34\35\7,\2\2\35\36\7\61\2\2\36\37"+
		"\3\2\2\2\37 \b\3\2\2 \6\3\2\2\2!\"\7\61\2\2\"#\7\61\2\2#\'\3\2\2\2$&\n"+
		"\3\2\2%$\3\2\2\2&)\3\2\2\2\'%\3\2\2\2\'(\3\2\2\2(*\3\2\2\2)\'\3\2\2\2"+
		"*+\b\4\2\2+\b\3\2\2\2,-\7^\2\2-\66\7^\2\2./\7^\2\2/\66\7*\2\2\60\61\7"+
		"^\2\2\61\66\7+\2\2\62\63\7^\2\2\63\66\7a\2\2\64\66\n\4\2\2\65,\3\2\2\2"+
		"\65.\3\2\2\2\65\60\3\2\2\2\65\62\3\2\2\2\65\64\3\2\2\2\66\67\3\2\2\2\67"+
		"\65\3\2\2\2\678\3\2\2\28\n\3\2\2\29:\7*\2\2:\f\3\2\2\2;<\7+\2\2<\16\3"+
		"\2\2\2\7\2\31\'\65\67\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}