package listener;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import generated.*;

public class MiniCPrintListener extends MiniCBaseListener {

	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	private StringBuffer nested = new StringBuffer();

	@Override public void enterProgram(MiniCParser.ProgramContext ctx) {

	}

	@Override public void exitProgram(MiniCParser.ProgramContext ctx) {

		//지금까지 입력된 nexTexts를 출력
		for (int i = 0; i < ctx.getChildCount(); i++)
			System.out.println(newTexts.get(ctx.decl(i)));
		
		
	}

	@Override public void enterDecl(MiniCParser.DeclContext ctx) {

	}

	@Override public void exitDecl(MiniCParser.DeclContext ctx) {

		//decl의 자식을 newTexts에 넣어 끝낸다.
		newTexts.put(ctx, newTexts.get(ctx.getChild(0)));
	}

	@Override public void enterVar_decl(MiniCParser.Var_declContext ctx) {

	}

	@Override public void exitVar_decl(MiniCParser.Var_declContext ctx) {

		String type = ctx.type_spec().getText();
		String id = ctx.IDENT().getText();

		//선언만 할 때
		if (ctx.getChildCount() == 3)
			newTexts.put(ctx, type + " " + id + ";");

		else {
			String literal = ctx.LITERAL().getText();

			//정의까지 할 때
			if (ctx.getChildCount() == 5)
				newTexts.put(ctx, type + " " + id + " = " + literal + ";");

			//배열을 선언 할 때
			else
				newTexts.put(ctx, type + " " + id + "[" + literal + "];");
		}
	}

	@Override public void enterType_spec(MiniCParser.Type_specContext ctx) {

	}

	@Override public void exitType_spec(MiniCParser.Type_specContext ctx) {

	}

	@Override public void enterFun_decl(MiniCParser.Fun_declContext ctx) {

	}

	@Override public void exitFun_decl(MiniCParser.Fun_declContext ctx) {

		//기능 선언에 대한 필요한 변수들을 정의한다.
		String type = ctx.type_spec().getText();
		String name = ctx.IDENT().getText();
		String params = newTexts.get(ctx.params());
		String compound_stmt = newTexts.get(ctx.compound_stmt());

		//값을 알맞게 출력하기 위해 정리해서 넣는다.
		newTexts.put(ctx, type + " " + name + "(" + params + ")\n" + compound_stmt);
	}

	@Override public void enterParams(MiniCParser.ParamsContext ctx) {

	}

	@Override public void exitParams(MiniCParser.ParamsContext ctx) {

		//중첩된 구조를 위해 StringBuffer를 이용
		StringBuffer nested_degree = new StringBuffer();
		if (ctx.getChildCount() > 0)
			nested_degree.append(newTexts.get(ctx.getChild(0)));
		
		for (int i = 1; i < ctx.param().size(); i++)
			nested_degree.append(", " + newTexts.get(ctx.param(i)));
		
		newTexts.put(ctx, nested_degree.toString());
	}

	@Override public void enterParam(MiniCParser.ParamContext ctx) {

	}

	@Override public void exitParam(MiniCParser.ParamContext ctx) {

		String type_spec = ctx.type_spec().getText();
		String id = ctx.IDENT().getText();

		//일반 파라미터
		if (ctx.getChildCount() == 2)
			newTexts.put(ctx, type_spec + " " + id);

		//배열 라파미터
		else
			newTexts.put(ctx, type_spec + " " + id + "[]");

	}

	@Override public void enterStmt(MiniCParser.StmtContext ctx) {

	}

	@Override public void exitStmt(MiniCParser.StmtContext ctx) {
		
		//stmt의 자식을 저장한다.
		newTexts.put(ctx, newTexts.get(ctx.getChild(0)));
	}

	@Override public void enterExpr_stmt(MiniCParser.Expr_stmtContext ctx) { 

	}

	@Override public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {

		//expr 가져와서
		String expr = newTexts.get(ctx.expr());

		//양식에 맞게 저장
		newTexts.put(ctx, expr + ";");
	}

	@Override public void enterWhile_stmt(MiniCParser.While_stmtContext ctx) { 
		
	}

	@Override public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) { 

		//expr과 stmt를 가져온다
		String expr = newTexts.get(ctx.expr());
		String stmt = newTexts.get(ctx.stmt());

		//stmt의 첫번째 자식이 중첩된 stmt가 아닌 경우
		if (!(ctx.stmt().getChild(0) instanceof MiniCParser.Compound_stmtContext)) {
			stmt = nested + "...." + stmt + "\n";
			newTexts.put(ctx, "while (" + expr + ")\n" + stmt);
		}
		
		//중첩된 경우
		else
			newTexts.put(ctx, "while (" + expr + ")\n" + stmt);
	}

	@Override public void enterCompound_stmt(MiniCParser.Compound_stmtContext ctx) {

		//중첩된 stmt에 들어가기전에 값을 저장해둔다.
		nested.append("....");
	}

	@Override public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) { 

		//중첩된 정도를 넣기위한 StringBuffer
		StringBuffer nested_degree = new StringBuffer();

		for (int i = 1; i < ctx.getChildCount() - 1; i++)
			nested_degree.append(nested + newTexts.get(ctx.getChild(i)) + "\n");

		nested.delete(0, 4);

		newTexts.put(ctx, nested + "{\n" + nested_degree + nested + "}");
	}

	@Override public void enterLocal_decl(MiniCParser.Local_declContext ctx) { 
		
	}

	@Override public void exitLocal_decl(MiniCParser.Local_declContext ctx) { 

		String type = ctx.type_spec().getText();
		String id = ctx.IDENT().getText();

		if (ctx.getChildCount() == 3)
			newTexts.put(ctx, type + " " + id + ";");

		else {
			String literal = ctx.LITERAL().getText();

			if (ctx.getChildCount() == 5)
				newTexts.put(ctx, type + " " + id + " = " + literal + ";");

			else
				newTexts.put(ctx, type + " " + id + "[" + literal + "];");
		}
	}

	@Override public void enterIf_stmt(MiniCParser.If_stmtContext ctx) { 

	}

	@Override public void exitIf_stmt(MiniCParser.If_stmtContext ctx) { 

		//if문에 필요한 변수들을 불러온다.
		String expr = newTexts.get(ctx.expr());
		String stmt = newTexts.get(ctx.stmt(0));
		
		//stmt의 첫번째 자식이 중첩된 stmt인 경우가 아닌경우
		if (!(ctx.stmt(0).getChild(0) instanceof MiniCParser.Compound_stmtContext)) {
			stmt = nested + "...." + stmt;
			newTexts.put(ctx, "if (" + expr + ")\n" + stmt);
		}
		
		//중첩된 경우
		else
			newTexts.put(ctx, "if (" + expr + ")\n" + stmt);
		
		//else 문
		if (ctx.getChildCount() == 7) {
			String stmt2 = newTexts.get(ctx.stmt(1));
			newTexts.put(ctx, "else\n" + stmt2);
		}
	}

	@Override public void enterReturn_stmt(MiniCParser.Return_stmtContext ctx) {

	}

	@Override public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {

	}

	@Override public void enterExpr(MiniCParser.ExprContext ctx) { 

	}

	@Override public void exitExpr(MiniCParser.ExprContext ctx) {

		if (isBinaryOperation(ctx)) {
			String expr1 = newTexts.get(ctx.expr(0));
			String expr2 = newTexts.get(ctx.expr(1));
			String op = ctx.getChild(1).getText();
			newTexts.put(ctx, expr1 + " " + op + " " + expr2);
		}
		
		else if (ctx.getChildCount() == 6) {
			String id = ctx.IDENT().getText();
			String expr1 = newTexts.get(ctx.expr(0));
			String expr2 = newTexts.get(ctx.expr(1));
			newTexts.put(ctx, id + "[" + expr1 + "] = " + expr2);
		}
		
		else if (ctx.getChildCount() == 4) {
			String id = ctx.IDENT().getText();
			String temp = null;
			
			if (ctx.getChild(1).getText() == "[") {
				temp = newTexts.get(ctx.getChild(2));
				newTexts.put(ctx, id + "[" + temp + "]");
			}
			
			else {
				temp = ctx.args().getText();
				newTexts.put(ctx, id + "(" + temp + ")");
			}
		}
		
		else if (ctx.getChildCount() == 3) {
			String expr = newTexts.get(ctx.expr(0));
			if (ctx.getChild(0).getText() == "(")
				newTexts.put(ctx, "(" + expr + ")");
			
			else {
				String id = ctx.getChild(0).getText();
				newTexts.put(ctx, id + " = " + expr);
			}
		}
		
		else if (ctx.getChildCount() == 2) {
			String op = ctx.getChild(0).getText();
			String expr = newTexts.get(ctx.expr(0));
			
			newTexts.put(ctx, op + expr);
		} 
		
		else if (ctx.getChildCount() == 1)
			newTexts.put(ctx, ctx.getChild(0).getText());
	}

	boolean isBinaryOperation(MiniCParser.ExprContext ctx) {
		return ctx.getChildCount() == 3 && ctx.getChild(1) != ctx.expr() && ctx.getChild(0) == ctx.expr(0);
	}

	@Override public void enterArgs(MiniCParser.ArgsContext ctx) {

	}

	@Override public void exitArgs(MiniCParser.ArgsContext ctx) { 
		
	}

	@Override public void enterEveryRule(ParserRuleContext ctx) { 

	}

	@Override public void exitEveryRule(ParserRuleContext ctx) { 

	}

	@Override public void visitTerminal(TerminalNode node) {

	}

	@Override public void visitErrorNode(ErrorNode node) {

	}
}
