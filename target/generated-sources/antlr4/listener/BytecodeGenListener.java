package listener;

import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ParamsContext;

import static listener.BytecodeGenListenerHelper.*;
import static listener.SymbolTable.*;

import java.util.LinkedList;
import java.util.Queue;

public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {

	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	SymbolTable symbolTable = new SymbolTable();

	int tab = 0;
	int label = 0;

	private int curS = 0; // 현재 스택 사이즈
	private int maxS = 0; // 최고 스택 사이즈

	Queue<Integer> maxRecord = new LinkedList<Integer>();
	
	private void comP() {
		curS++;
		if (curS > maxS)
			maxS = curS;
		System.out.print("curS: " + curS);
		System.out.println(" maxS: " + maxS);
	}
	
	private void comN() {
		curS--;
		if (curS > maxS)
			maxS = curS;
		System.out.print("curS: " + curS);
		System.out.println(" maxS: " + maxS);
	}

	private void clearP() {
		if (maxRecord.add(maxS))
			System.out.println("PushSuccess: " + maxS);;
		curS = 0;
		maxS = 0;
	}
	
	// program	: decl+
	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
		symbolTable.initFunDecl();

		curS = 0;
		maxS = 0;
		
		System.out.println("초기화!\n");

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

		whileStmt += expr					// 1
				+ "ifeq " + lEnd + "\n"		// 2
				+ lStart + ": \n"			// 3
				+ stmt						// 4
				+ "goto " + lStart + "\n"	// 5
				+ lEnd + ": \n";			// 6
		
							// 1 expr 부분에서 처리
		comN();				// 2
							// 3
							// 4 stmt 부분에서 처리
							// 5
							// 6
		System.out.println("ifeq!");
		newTexts.put(ctx, whileStmt);
	}

	// fun_decl	: type_spec IDENT '(' params ')' compound_stmt ;
	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
		// <(2) Fill here!>
		String fun = "";
		String compound = newTexts.get(ctx.compound_stmt());
		if (ctx.type_spec().getText().contains("void")) {
			clearP();
		}
		fun += funcHeader(ctx, ctx.IDENT().getText()) + compound;
		if (ctx.type_spec().getText().contains("void")) {
			fun += "return \n"
					+ ".end method \n\n";
		}
		newTexts.put(ctx, fun);
	}

	private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
		int localVarSize = symbolTable.localGetter().size();
		int max = 100;

		if (maxRecord.peek() != null) {
			System.out.println("---------------maxRecord: " + maxRecord);
			max = maxRecord.poll();
		}
		else
			max = 99;
		System.out.println("---------------maxRecord: " + maxRecord);
		return ".method public static " + symbolTable.getFunSpecStr(fname) + "\n"	
				+ ".limit stack " 	+ Integer.toString(max) + "\n"
				+ ".limit locals " 	+ Integer.toString(localVarSize) + "\n";
	}

	// var_decl	:  type_spec IDENT ';'	| type_spec IDENT '=' LITERAL ';'	| type_spec IDENT '[' LITERAL ']' ';'	;
	@Override
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		String varDecl = "";

		if (isDeclWithInit(ctx))
			varDecl += "putfield " + varName + "\n";  
		
		newTexts.put(ctx, varDecl);
	}

	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		String varDecl = "";

		if (isDeclWithInit(ctx)) {
			String vId = symbolTable.getVarId(ctx);
			varDecl += "ldc " + ctx.LITERAL().getText() + "\n"	// 1
					+ "istore_" + vId + "\n";					// 2
			
			comP();
			System.out.println("ldc "+ctx.LITERAL().getText()+"!");
			comN();
			System.out.println("istore_"+vId+"!");
		} else if (isDecl(ctx))
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);

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
			
			comN();
			System.out.println("ifeq!");
		} else {
			String elseStmt = newTexts.get(ctx.stmt(1)).trim();
			stmt += condExpr + "\n"
					+ "ifeq " + lelse + "\n"
					+ thenStmt + "\n"
					+ "goto " + lend + "\n"
					+ lelse + ": \n" 
					+ elseStmt + "\n"
					+ lend + ":"  + "\n";
			
			comN();
			System.out.println("ifeq!");
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
			
			comN();
			System.out.println("method return!");
			
			clearP();
		} else {
			stmt += "return \n"
					+ ".end method \n\n";
		
			clearP();
		}

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
				
					comP();
					System.out.println("iload_"+symbolTable.getVarId(idName)+"! exitExpr");
				}
			} else if (ctx.LITERAL() != null) {
				String literalStr = ctx.LITERAL().getText();
				expr += "ldc " + literalStr + " \n";
				
				comP();
				System.out.println("ldc "+literalStr+"!");
			}
		} else if (ctx.getChildCount() == 2) { // UnaryOperation
			expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);
			if (ctx.expr(0).getText().matches(id)) {
				expr += "istore_" + symbolTable.getVarId(ctx.expr(0).getText()) + "\n";
			
				comN();
				System.out.println("istore_"+symbolTable.getVarId(ctx.expr(0).getText())+"!");
			}
		}
		else if (ctx.getChildCount() == 3) {
			if(ctx.getChild(0).getText().equals("("))		// '(' expr ')'
				expr = newTexts.get(ctx.expr(0));

			else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
				expr = newTexts.get(ctx.expr(0))
						+ "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";
				
				comN();
				System.out.println("istore_"+symbolTable.getVarId(ctx.IDENT().getText())+"!");
			} else										// binary operation
				expr = handleBinExpr(ctx, expr);

		}
		// IDENT '(' args ')' |  IDENT '[' expr ']'
		else if(ctx.getChildCount() == 4) {
			if(ctx.args() != null)	// function calls
				expr = handleFunCall(ctx, expr);
			
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
			expr += "ineg \n";
			
			System.out.println("ineg!");
			break;
		case "--":
			expr += "ldc 1" + "\n"
					+ "isub" + "\n";
			
			comP();
			System.out.println("ldc 1!");
			comN();
			System.out.println("-!");
			break;
		case "++":
			expr += "ldc 1" + "\n"
					+ "iadd" + "\n";
			
			comP();
			System.out.println("ldc 1!");
			comN();
			System.out.println("+!");
			break;
		case "!":
			expr += "ifeq " + l2 + "\n"
					+ l1 + ": \n" 
					+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ l2 + ": \n" 
					+ "ldc 1" + "\n"
					+ lend + ": " + "\n";
			
			comN();
			System.out.println("ifeq!");
			comP();
			System.out.println("ldc 0 or 1!");
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
			
//			comP();
//			System.out.println("iload_"+symbolTable.getVarId(ctx.expr(0).getText())+"! handleBinExpr");
		}
		
		else {
			expr += "ldc " + ctx.expr(0).getText() + "\n";
		
//			comP();
//			System.out.println("ldc "+ctx.expr(0).getText()+"!");
		}
		
		if (ctx.expr(1).getText().matches(id)) {
			expr += "iload_" + symbolTable.getVarId(ctx.expr(1).getText()) + "\n";
		
//			comP();
//			System.out.println("iload_"+symbolTable.getVarId(ctx.expr(1).getText())+"! handBinExpr2");
		}
		
		else {
			expr += "ldc " + ctx.expr(1).getText() + "\n";
		
//			comP();
//			System.out.println("ldc "+ctx.expr(1).getText()+"!");
		}
		
		switch (ctx.getChild(1).getText()) {
		case "*":
			expr += "imul \n"; 
			
			comN();
			System.out.println("*!");
			break;
		case "/":
			expr += "idiv \n"; 
			
			comN(); 
			System.out.println("/!");
			break;
		case "%":
			expr += "irem \n"; 
			
			comN();
			System.out.println("%!");
			break;
		case "+":		// expr(0) expr(1) iadd
			expr += "iadd \n";
			
			comN();
			System.out.println("+!");
			break;
		case "-":
			expr += "isub \n";
			
			comN();
			System.out.println("-!");
			break;
		case "==":
			expr += "isub " + "\n"
					+ "ifeq " + lstart + "\n"
					+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ lstart + ": \n" 
					+ "ldc 1" + "\n"
					+ lend + ": " + "\n";
			
			comN();
			System.out.println("-!");
			comN();
			System.out.println("ifeq!");
			comP();
			System.out.println("ldc 0 or 1!");
			break;
		case "!=":
			expr += "isub " + "\n"
					+ "ifne " + lstart + "\n"
					+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ lstart + ": \n" 
					+ "ldc 1" + "\n"
					+ lend + ": " + "\n";
			
			comN();
			System.out.println("-!");
			comN();
			System.out.println("ifne!");
			comP();
			System.out.println("ldc 0 or 1!");
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

			comN();
			System.out.println("-!");
			comN();
			System.out.println("ifle!");
			comP();
			System.out.println("ldc 0 or 1!");
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

			comN();
			System.out.println("-!");
			comN();
			System.out.println("iflt!");
			comP();
			System.out.println("ldc 0 or 1!");
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
			
			comN();
			System.out.println("-!");
			comN();
			System.out.println("ifge!");
			comP();
			System.out.println("ldc 0 or 1!");
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
			
			comN();
			System.out.println("-!");
			comN();
			System.out.println("ifgt!");
			comP();
			System.out.println("ldc 0 or 1!");
			break;
		case "and":
			expr += "imul\n"
					+ "ifeq " + lstart + "\n"
					+ "ldc 1" + "\n"
					+ "goto " + lend + "\n" 
					+ lstart + ": " + "\n"
					+ "ldc 0\n"
					+ lend + ": " + "\n";
			
			comN();
			System.out.println("*!");
			comN();
			System.out.println("ifeq!");
			comP();
			System.out.println("ldc 0 or 1!");
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
			
			comN();
			System.out.println("+!");
			comP();
			System.out.println(temp1+"!");
			comN();
			System.out.println("+!");
			comN();
			System.out.println("ifeq!");
			comP();
			System.out.println("ldc 0 or 1!");
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

			comP();
			System.out.println("print method add!");
			comN();
			System.out.println("print method argument pop!");
			comN();
			System.out.println("print method method pop!");
		} else {	
			expr = newTexts.get(ctx.args()) 
					+ "invokestatic " + getCurrentClassName() + "/"
					+ symbolTable.getFunSpecStr(fname) 
					+ "\n";

			comP();
			System.out.println("method add!");
			
			String argType = symbolTable.getFunSpecStr(fname);
			int totLen = argType.length();
			argType = argType.substring(fname.length() + 1, totLen - 2);
			
			while (argType.length() != 0) {
				argType = argType.substring(1);
				comN();
				System.out.println("method arg pop!");
			}
			
			comN();
			System.out.println("method pop!");
			
			String rType = symbolTable.getFunSpecStr(fname);
			rType = rType.substring(rType.length() - 1);
			
			if (rType.equals("I")) {
				// return type => int
				comP();
				System.out.println("method return add!");
			} else {
				// return type => void
			}
			
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