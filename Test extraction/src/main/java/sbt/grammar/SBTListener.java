// Generated from SBT.g4 by ANTLR 4.5.1

    package sbt.grammar;

    import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SBTParser}.
 */
public interface SBTListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SBTParser#node}.
	 * @param ctx the parse tree
	 */
	void enterNode(SBTParser.NodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SBTParser#node}.
	 * @param ctx the parse tree
	 */
	void exitNode(SBTParser.NodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SBTParser#nodes}.
	 * @param ctx the parse tree
	 */
	void enterNodes(SBTParser.NodesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SBTParser#nodes}.
	 * @param ctx the parse tree
	 */
	void exitNodes(SBTParser.NodesContext ctx);
}