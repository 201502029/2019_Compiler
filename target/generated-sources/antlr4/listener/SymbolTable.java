package listener;

import java.util.HashMap;
import java.util.Map;
//import java.util.Hashtable;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.Var_declContext;
//import generated.MiniCParser.ParamsContext;
//import generated.MiniCParser.Type_specContext;

//import listener.SymbolTable.Type;

import static listener.BytecodeGenListenerHelper.*;

public class SymbolTable {
	enum Type {
		INT, INTARRAY, VOID, ERROR
	}

	static public class VarInfo {
		Type type; 
		int id;
		int initVal;

		public VarInfo(Type type, int id, int initVal) {
			this.type = type;
			this.id = id;
			this.initVal = initVal;
		}
		
		public VarInfo(Type type,  int id) {
			this.type = type;
			this.id = id;
			this.initVal = 0;
		}
	}

	static public class FInfo {
		public String sigStr;
	}

	private Map<String, VarInfo> _lsymtable = new HashMap<>();	// local v.
	private Map<String, VarInfo> _gsymtable = new HashMap<>();	// global v.
	private Map<String, FInfo> _fsymtable = new HashMap<>();	// function 

	private int _globalVarID = 0;
	private int _localVarID = 0;
	private int _labelID = 0;
	private int _tempVarID = 0;

	SymbolTable(){
		initFunDecl();
		initFunTable();
	}

	void initFunDecl() {		// at each func decl
		_lsymtable.clear();
		_localVarID = 0;
		_labelID = 0;
		_tempVarID = 32;		
	}

	void putLocalVar(String varname, Type type){
		//<Fill here>
		if (_lsymtable.containsKey(varname)) {
			VarInfo info = _lsymtable.get(varname);
			VarInfo realInfo = new VarInfo(type, info.id);
			_lsymtable.remove(varname);
			_lsymtable.put(varname, realInfo);
		} else {
			VarInfo info = new VarInfo(type, _localVarID);
			_lsymtable.put(varname, info);
			_localVarID++;
		}		
	}

	void putGlobalVar(String varname, Type type){
		//<Fill here>
		VarInfo info = new VarInfo(type, _globalVarID);
		_gsymtable.put(varname, info);
		_globalVarID++;
	}

	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here>
		if (_lsymtable.containsKey(varname)) {
			VarInfo info = _lsymtable.get(varname);
			VarInfo realInfo = new VarInfo(type, info.id, initVar);
			_lsymtable.remove(varname);
			_lsymtable.put(varname, realInfo);
		} else {
			VarInfo info = new VarInfo(type, _localVarID, initVar);
			_lsymtable.put(varname, info);
			_localVarID++;
		}
	}
	
	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here>
		VarInfo info = new VarInfo(type, _globalVarID, initVar);
		_gsymtable.put(varname, info);
		_globalVarID++;
	}

	void putParams(MiniCParser.ParamsContext params) {
		for(int i = 0; i < params.param().size(); i++) {
			//<Fill here>
			String name = params.param(i).IDENT().getText();
			String typeStr = params.param(i).type_spec().getText();
			Type type;
			
			if (typeStr.contains("int"))
				type = Type.INT;
			
			else if (typeStr.contains("void"))
				type = Type.VOID;
			
			else if (typeStr.contains("int[]"))
				type = Type.INTARRAY;
			
			else
				type = Type.ERROR;
			
			putLocalVar(name, type);
		}
	}

	private void initFunTable() {
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "java/io/PrintStream/println(I)V";

		FInfo maininfo = new FInfo();
		maininfo.sigStr = "main([Ljava/lang/String;)V";
		
		_fsymtable.put("_print", printlninfo);
		_fsymtable.put("main", maininfo);
	}

	public String getFunSpecStr(String fname) {		
		// <Fill here>
		FInfo fun = _fsymtable.get(fname);
		if (fun != null)
			return fun.sigStr;
		
		return null;
	}

	public String getFunSpecStr(Fun_declContext ctx) {
		// <Fill here>
		String fname = ctx.IDENT().getText();
		FInfo info = _fsymtable.get(fname);
		return info.sigStr;
	}

	public String putFunSpecStr(Fun_declContext ctx) {
		String fname = getFunName(ctx);
		String argtype = "";	
		String rtype = "";
		String res = "";
		
		// <Fill here>
		String temp = getParamTypesText(ctx.params());
		argtype = temp.replace("int", "I");
		
		temp = ctx.type_spec().getText();
		
		if (temp.contains("int"))
			rtype = temp.replace("int", "I");
		if (temp.contains("void"))
			rtype = temp.replace("void", "V");

		res = fname + "(" + argtype + ")" + rtype;

		FInfo finfo = new FInfo();
		finfo.sigStr = res;
		_fsymtable.put(fname, finfo);

		return res;
	}

	String getVarId(String name){
		// <Fill here>
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if (lvar != null)
			return Integer.toString(lvar.id);

		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		if (gvar != null)
			return Integer.toString(gvar.id);

		return null;
	}

	Type getVarType(String name){
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if (lvar != null) {
			return lvar.type;
		}

		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		if (gvar != null) {
			return gvar.type;
		}

		return Type.ERROR;	
	}
	
	String newLabel() {
		return "label" + _labelID++;
	}

	String newTempVar() {
		String id = "";
		return id + _tempVarID--;
	}

	// global
	public String getVarId(Var_declContext ctx) {
		// <Fill here>
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}

	// local
	public String getVarId(Local_declContext ctx) {
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}
}
