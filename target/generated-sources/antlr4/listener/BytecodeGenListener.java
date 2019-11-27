package listener;

//import java.util.Hashtable;
//
//import org.antlr.v4.runtime.ParserRuleContext;
//import org.antlr.v4.runtime.tree.ErrorNode;
//import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ParamsContext;
//import generated.MiniCParser.ExprContext;
//import generated.MiniCParser.Fun_declContext;
//import generated.MiniCParser.Local_declContext;
//import generated.MiniCParser.ProgramContext;
//import generated.MiniCParser.StmtContext;
//import generated.MiniCParser.Type_specContext;
//import generated.MiniCParser.Var_declContext;

import static listener.BytecodeGenListenerHelper.*;
import static listener.SymbolTable.*;

public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {

	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	SymbolTable symbolTable = new SymbolTable();

	int tab = 0;
	int label = 0;

	// program	: decl+
	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
		symbolTable.initFunDecl();

		String fname = getFunName(ctx);
		ParamsContext params;

		if (fname.equals("main"))
			symbolTable.putLocalVar("args", Type.INTARRAY);

		else {
			symbolTable.putFunSpecStr(ctx);
			params = (MiniCParser.ParamsContext) ctx.getChild(3);
			symbolTable.putParams(params);
		}		
	}

	// var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';' | type_spec IDENT '[' LITERAL ']' ';'
	@Override
	public void enterVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();

		if (isArrayDecl(ctx))
			symbolTable.putGlobalVar(varName, Type.INTARRAY);

		else if (isDecl(ctx))
			symbolTable.putGlobalVar(varName, Type.INT);

		else
			symbolTable.putGlobalVarWithInitVal(varName, Type.INT, initVal(ctx));
	}

	@Override
	public void enterLocal_decl(MiniCParser.Local_declContext ctx) {			
		if (isArrayDecl(ctx))
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INTARRAY);

		else if (isDecl(ctx))
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);

		else 
			symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), Type.INT, initVal(ctx));	

	}

	@Override
	public void exitProgram(MiniCParser.ProgramContext ctx) {
		String classProlog = getFunProlog();

		String fun_decl = "", var_decl = "";

		for(int i = 0; i < ctx.getChildCount(); i++) {
			if(isFunDecl(ctx, i))
				fun_decl += newTexts.get(ctx.decl(i));
			else
				var_decl += newTexts.get(ctx.decl(i));
		}

		newTexts.put(ctx, classProlog + var_decl + fun_decl);

		System.out.println(newTexts.get(ctx));
	}

	// decl	: var_decl | fun_decl
	@Override
	public void exitDecl(MiniCParser.DeclContext ctx) {
		String decl = "";
		if(ctx.getChildCount() == 1) {
			if(ctx.var_decl() != null)				//var_decl
				decl += newTexts.get(ctx.var_decl());

			else							//fun_decl
				decl += newTexts.get(ctx.fun_decl());
		}
		newTexts.put(ctx, decl);
	}

	// stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
	@Override
	public void exitStmt(MiniCParser.StmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() > 0)	{
			if (ctx.expr_stmt() != null)				// expr_stmt
				stmt += newTexts.get(ctx.expr_stmt());

			else if (ctx.compound_stmt() != null)	// compound_stmt
				stmt += newTexts.get(ctx.compound_stmt());

			// <(0) Fill here>
			else if (ctx.if_stmt() != null)
				stmt += newTexts.get(ctx.if_stmt());

			else if (ctx.while_stmt() != null)
				stmt += newTexts.get(ctx.while_stmt());

			else
				stmt += newTexts.get(ctx.return_stmt());
		}
		newTexts.put(ctx, stmt);
	}

	// expr_stmt	: expr ';'
	@Override
	public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
		String stmt = "";

		if (ctx.getChildCount() == 2)
			stmt += newTexts.get(ctx.expr());	// expr

		newTexts.put(ctx, stmt);
	}

	// while_stmt	: WHILE '(' expr ')' stmt
	@Override
	public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
		// <(1) Fill here!>
		String whileStmt = "";
		String expr = newTexts.get(ctx.expr());
		String stmt = newTexts.get(ctx.stmt());
		
		String lStart = symbolTable.newLabel();
		String lEnd = symbolTable.newLabel();
		
		whileStmt += expr
				+ "ifeq " + lEnd + "\n"
				+ lStart + ": \n"
				+ stmt
				+ "goto " + lStart + "\n"
				+ lEnd + ": \n";
		newTexts.put(ctx, whileStmt);
	}

	// fun_decl	: type_spec IDENT '(' params ')' compound_stmt ;
	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
		// <(2) Fill here!>
		String fun = "";
		String compound = newTexts.get(ctx.compound_stmt());
		fun += funcHeader(ctx, ctx.IDENT().getText()) + compound;
		if (ctx.type_spec().getText().contains("void"))
			fun += "return \n.end method\n\n";
		newTexts.put(ctx, fun);
	}

	private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
		return ".method public static " + symbolTable.getFunSpecStr(fname) + "\n"	
				+ ".limit stack " 	+ getStackSize(ctx) + "\n"
				+ ".limit locals " 	+ getLocalVarSize(ctx) + "\n";
	}

	// var_decl	:  type_spec IDENT ';'	| type_spec IDENT '=' LITERAL ';'	| type_spec IDENT '[' LITERAL ']' ';'	;
	@Override
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		String varDecl = "";

		if (isDeclWithInit(ctx)) {
			varDecl += "putfield " + varName + "\n";  
			// v. initialization => Later! skip now..: 
		}
		newTexts.put(ctx, varDecl);
	}

	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		String varDecl = "";

		if (isDeclWithInit(ctx)) {
			String vId = symbolTable.getVarId(ctx);
			varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
					+ "istore_" + vId + "\n";
		} else if (isDecl(ctx)){
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);
//			String vId = symbolTable.getVarId(ctx);
//			varDecl += "istore_" +vId + "\n";
		}

		newTexts.put(ctx, varDecl);
	}

	// compound_stmt	: '{' local_decl* stmt* '}'
	@Override
	public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
		// <(3) Fill here>
		String compound = "";

		for (int i = 0; i < ctx.local_decl().size(); i++)
			compound += newTexts.get(ctx.local_decl(i)); 

		for (int i = 0; i < ctx.stmt().size(); i++)
			compound += newTexts.get(ctx.stmt(i));
		newTexts.put(ctx, compound);
	}

	// if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
	@Override
	public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
		String stmt = "";
		String condExpr = newTexts.get(ctx.expr()).trim();
		String thenStmt = newTexts.get(ctx.stmt(0)).trim();
		
		String lend = symbolTable.newLabel();
		String lelse = symbolTable.newLabel();

		if (noElse(ctx)) {		
			stmt += condExpr + "\n"
					+ "ifeq " + lend + "\n"
					+ thenStmt + "\n"
					+ lend + ":"  + "\n";
		} else {
			String elseStmt = newTexts.get(ctx.stmt(1)).trim();
			stmt += condExpr + "\n"
					+ "ifeq " + lelse + "\n"
					+ thenStmt + "\n"
					+ "goto " + lend + "\n"
					+ lelse + ": \n" 
					+ elseStmt + "\n"
					+ lend + ":"  + "\n";
		}
		
		newTexts.put(ctx, stmt);
	}

	// return_stmt	: RETURN ';' | RETURN expr ';'
	@Override
	public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
		// <(4) Fill here>
		String stmt = "";

		if (ctx.getChildCount() == 3) {
			String expr = newTexts.get(ctx.expr());
			stmt += expr + "ireturn \n"
					+ ".end method \n\n";
		} else
			stmt += "return \n"
					+ ".end method \n\n";

		newTexts.put(ctx, stmt);
	}


	@Override
	public void exitExpr(MiniCParser.ExprContext ctx) {
		String expr = "";
		String id = "^[a-zA-Z]*$";

		if (ctx.getChildCount() <= 0) {
			newTexts.put(ctx, ""); 
			return;
		}

		if (ctx.getChildCount() == 1) { // IDENT | LITERAL
			if (ctx.IDENT() != null) {
				String idName = ctx.IDENT().getText();
				if (symbolTable.getVarType(idName) == Type.INT) {
					expr += "iload_" + symbolTable.getVarId(idName) + " \n";
				}
				//else	// Type int array => Later! skip now..
				//	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
			} else if (ctx.LITERAL() != null) {
				String literalStr = ctx.LITERAL().getText();
				expr += "ldc " + literalStr + " \n";
			}
		} else if (ctx.getChildCount() == 2) { // UnaryOperation
			expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);
			if (ctx.expr(0).getText().matches(id))
				expr += "istore_" + symbolTable.getVarId(ctx.expr(0).getText()) + "\n";
		}
		else if (ctx.getChildCount() == 3) {
			if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
				expr = newTexts.get(ctx.expr(0));

			} else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
				expr = newTexts.get(ctx.expr(0))
						+ "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";
				} else { 											// binary operation
				expr = handleBinExpr(ctx, expr);
			}
		}
		// IDENT '(' args ')' |  IDENT '[' expr ']'
		else if(ctx.getChildCount() == 4) {
			if(ctx.args() != null){		// function calls
				expr = handleFunCall(ctx, expr);
			} else { // expr
				// Arrays: TODO  
			}
		}
		// IDENT '[' expr ']' '=' expr
		else { // Arrays: TODO			*/
		}
		newTexts.put(ctx, expr);
	}


	private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
		String l1 = symbolTable.newLabel();
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		
		expr = "";
		expr += newTexts.get(ctx.expr(0));
		switch(ctx.getChild(0).getText().trim()) {
		case "-":
			expr += "ineg \n"; break;
		case "--":
			expr += "ldc 1" + "\n"
					+ "isub" + "\n";
			break;
		case "++":
			expr += "ldc 1" + "\n"
					+ "iadd" + "\n";
			break;
		case "!":
			expr += "ifeq " + l2 + "\n"
					+ l1 + ": \n" 
					+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ l2 + ": \n" 
					+ "ldc 1" + "\n"
					+ lend + ": " + "\n";
			break;
		}
		return expr;
	}


	private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
		String lstart = symbolTable.newLabel();
		String lend = symbolTable.newLabel();

		String id = "^[a-zA-Z]*$";
		String temp1 = "";
		
		if (ctx.expr(0).getText().matches(id)) {
			expr += "iload_" + symbolTable.getVarId(ctx.expr(0).getText()) + "\n";
			temp1 = "iload_" + symbolTable.getVarId(ctx.expr(0).getText());
		}
		else
			expr += "ldc " + ctx.expr(0).getText() + "\n";
		if (ctx.expr(1).getText().matches(id)) {
			expr += "iload_" + symbolTable.getVarId(ctx.expr(1).getText()) + "\n";
		}
		else
			expr += "ldc " + ctx.expr(1).getText() + "\n";
		
		switch (ctx.getChild(1).getText()) {
		case "*":
			expr += "imul \n"; break;
		case "/":
			expr += "idiv \n"; break;
		case "%":
			expr += "irem \n"; break;
		case "+":		// expr(0) expr(1) iadd
			expr += "iadd \n"; break;
		case "-":
			expr += "isub \n"; break;

		case "==":
			expr += "isub " + "\n"
					+ "ifeq " + lstart + "\n"
					+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ lstart + ": \n" 
					+ "ldc 1" + "\n"
					+ lend + ": " + "\n";
			break;
		case "!=":
			expr += "isub " + "\n"
					+ "ifne " + lstart + "\n"
					+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ lstart + ": \n" 
					+ "ldc 1" + "\n"
					+ lend + ": " + "\n";
			break;
		case "<=":
			// <(5) Fill here>
			expr += "isub \n"
					+ "ifle " + lstart + "\n"
					+ "ldc 0 \n"
					+ "goto " + lend + "\n"
					+ lstart + ": \n" 
					+ "ldc 1 \n"
					+ lend + ": \n";

			break;
		case "<":
			// <(6) Fill here>
			expr += "isub \n"
					+ "iflt " + lstart + "\n"
					+ "ldc 0 \n"
					+ "goto " + lend + "\n"
					+ lstart + ": \n" 
					+ "ldc 1 \n"
					+ lend + ": \n";

			break;

		case ">=":
			// <(7) Fill here>
			expr += "isub \n"
					+ "ifge " + lstart + "\n"
					+ "ldc 0 \n"
					+ "goto " + lend + "\n"
					+ lstart + ": \n" 
					+ "ldc 1 \n"
					+ lend + ": \n";
			break;

		case ">":
			// <(8) Fill here>
			expr += "isub \n"
					+ "ifgt " + lstart + "\n"
					+ "ldc 0 \n"
					+ "goto " + lend + "\n"
					+ lstart + ": \n" 
					+ "ldc 1 \n"
					+ lend + ": \n";
			break;

		case "and":
			expr += "imul\n"
					+ "ifeq " + lstart + "\n"
					+ "ldc 1" + "\n"
					+ "goto " + lend + "\n" 
					+ lstart + ": " + "\n"
					+ "ldc 0\n"
					+ lend + ": " + "\n";
					break;
		case "or":
			// <(9) Fill here>
			expr += "iadd\n"
					+ temp1 + "\n"
					+ "iadd\n"
					+ "ifeq " + lstart + "\n"
					+ "ldc 1\n"
					+ "goto " + lend + "\n"
					+ lstart +": \n"
					+ "ldc 0\n"
					+ lend + ": \n";
			break;

		}

		return expr;
	}
	private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
		String fname = getFunName(ctx);		

		if (fname.equals("_print")) {		// System.out.println	
			expr = "getstatic java/lang/System/out Ljava/io/PrintStream; " + "\n"
					+ newTexts.get(ctx.args()).trim() + "\n"
					+ "invokevirtual " + symbolTable.getFunSpecStr("_print") + "\n";
		} else {	
			expr = newTexts.get(ctx.args()) 
					+ "invokestatic " + getCurrentClassName() + "/"
					+ symbolTable.getFunSpecStr(fname) 
					+ "\n";
		}
		return expr;
	}

	// args	: expr (',' expr)* | ;
	@Override
	public void exitArgs(MiniCParser.ArgsContext ctx) {

		String argsStr = "\n";

		for (int i=0; i < ctx.expr().size() ; i++)
			argsStr += newTexts.get(ctx.expr(i)) ; 

		newTexts.put(ctx, argsStr);
	}

}
