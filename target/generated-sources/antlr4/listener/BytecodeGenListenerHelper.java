package listener;

import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.If_stmtContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;

public class BytecodeGenListenerHelper {
	
	// <boolean functions>
	
	static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
		return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
	}

	// var_decl	: type_spec IDENT '=' LITERAL ';
	static boolean isDeclWithInit(Var_declContext ctx) {
		return ctx.getChildCount() == 5;
	}

	// <local vars>
	// local_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static int initVal(Local_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	static boolean isArrayDecl(Local_declContext ctx) {
		return ctx.getChildCount() == 6 
				&& ctx.getChild(2).getText() == "["
				&& ctx.getChild(4).getText() == "]";
	}
	
	static boolean isDeclWithInit(Local_declContext ctx) {
		return ctx.getChildCount() == 5;
	}
	
	static boolean isDecl(Local_declContext ctx) {
		return ctx.getChildCount() == 3;
	}
	
	static boolean noElse(If_stmtContext ctx) {
		return ctx.getChildCount() == 5;
	}
	
	static boolean isBinaryOperation(MiniCParser.ExprContext ctx) {
		return ctx.getChildCount() == 3 && ctx.getChild(1) != ctx.expr() && ctx.getChild(0) == ctx.expr(0);
	}
	
	// <information extraction>
	static String getStackSize(Fun_declContext ctx) {
		return "32";
	}
	
	static String getTypeText(Type_specContext ctx) {
		// <Fill in>
		return ctx.getText();
	}
	
	static String getParamTypesText(ParamsContext ctx) {
		String typeText = "";
		
		for(int i = 0; i < ctx.param().size(); i++) {
			MiniCParser.Type_specContext typespec = (MiniCParser.Type_specContext) ctx.param(i).getChild(0);
			typeText += getTypeText(typespec); // + ";";
		}
		return typeText;
	}
	
	static String getLocalVarName(Local_declContext ctx) {
		// <Fill in>
		return ctx.IDENT().getText();
	}
	
	static String getFunName(Fun_declContext ctx) {
		// <Fill in>
		return ctx.IDENT().getText();
	}
	
	static String getFunName(ExprContext ctx) {
		// <Fill in>
		return ctx.IDENT().getText();
	}
	
	static String getFunProlog() {
		return ".class public Test \n"
				+ ".super java/lang/Object \n"
				+ "; standard initializer \n\n"
				+ ".method public <init>()V \n"
				+ "aload_0 \n"
				+ "invokenonvirtual java/lang/Object/<init>()V \n"
				+ "return \n"
				+ ".end method \n\n";
	}
	
	static String getCurrentClassName() {
		return "Test";
	}
}
