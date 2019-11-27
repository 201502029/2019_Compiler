package listener;

//import java.util.Hashtable;

import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.If_stmtContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;

//import listener.SymbolTable;
//import listener.SymbolTable.VarInfo;

public class BytecodeGenListenerHelper {
	
	// <boolean functions>
	
	static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
		return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
	}
	
	// type_spec IDENT '[' ']'
	static boolean isArrayParamDecl(ParamContext param) {
		return param.getChildCount() == 4;
	}
	
	// global vars
	static int initVal(Var_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	// var_decl	: type_spec IDENT '=' LITERAL ';
	static boolean isDeclWithInit(Var_declContext ctx) {
		return ctx.getChildCount() == 5;
	}
	// var_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Var_declContext ctx) {
		return ctx.getChildCount() == 6;
	}
	static boolean isDecl(Var_declContext ctx) {
		return ctx.getChildCount() == 3;
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
	
	static boolean isVoidF(Fun_declContext ctx) {
		// <Fill in>
		return ctx.getChildCount() == 0;
	}
	
	static boolean isIntReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() == 3;
	}

	static boolean isVoidReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() == 2;
	}
	
	// <information extraction>
	static String getStackSize(Fun_declContext ctx) {
		return "32";
	}
	
	static String getLocalVarSize(Fun_declContext ctx) {
		return "32";
	}
	
	static String getTypeText(Type_specContext ctx) {
		// <Fill in>
		return ctx.getText();
	}

	// params
	static String getParamName(ParamContext ctx) {
		// <Fill in>
		return ctx.IDENT().getText();
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
	
	static boolean noElse(If_stmtContext ctx) {
		return ctx.getChildCount() == 5;
	}
	
	static String getFunProlog() {
		// return ".class public Test .....
		// ...
		// invokenonvirtual java/lang/Object/<init>()
		// return
		// .end method"
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
