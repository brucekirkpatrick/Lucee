/**
 * Copyright (c) 2014, the Railo Company Ltd.
 * Copyright (c) 2015, Lucee Assosication Switzerland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package lucee.runtime.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import lucee.commons.digest.RSA;
import lucee.commons.io.IOUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.lang.StringUtil;
import lucee.loader.engine.CFMLEngine;
import lucee.runtime.PageSource;
import lucee.runtime.PageSourceImpl;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.config.Constants;
import lucee.runtime.exp.TemplateException;
import lucee.transformer.Factory;
import lucee.transformer.Position;
import lucee.transformer.TransformerException;
import lucee.transformer.bytecode.*;
import lucee.transformer.bytecode.statement.*;
import lucee.transformer.bytecode.statement.tag.Tag;
import lucee.transformer.bytecode.statement.tag.TagBase;
import lucee.transformer.bytecode.statement.tag.TagThread;
import lucee.transformer.bytecode.statement.udf.Function;
import lucee.transformer.bytecode.statement.udf.FunctionImpl;
import lucee.transformer.bytecode.util.ASMUtil;
import lucee.transformer.bytecode.util.ClassRenamer;
import lucee.transformer.cfml.tag.CFMLTransformer;
import lucee.transformer.library.function.FunctionLib;
import lucee.transformer.library.tag.TagLib;
import lucee.transformer.util.AlreadyClassException;
import lucee.transformer.util.PageSourceCode;
import lucee.transformer.util.SourceCode;
import org.objectweb.asm.MethodTooLargeException;

/**
 * CFML Compiler compiles CFML source templates
 */
public final class CFMLCompilerImpl implements CFMLCompiler {

    private CFMLTransformer cfmlTransformer;
    private ConcurrentLinkedQueue<WatchEntry> watched = new ConcurrentLinkedQueue<WatchEntry>();

    /**
     * Constructor of the compiler
     *
     */
    public CFMLCompilerImpl() {
	cfmlTransformer = new CFMLTransformer();
    }

    public Result compile(ConfigImpl config, PageSource ps, TagLib[] tld, FunctionLib[] fld, Resource classRootDir, boolean returnValue, boolean ignoreScopes)
	    throws TemplateException, IOException {
	return _compile(config, ps, null, null, tld, fld, classRootDir, returnValue, ignoreScopes);
    }

    public Result compile(ConfigImpl config, SourceCode sc, TagLib[] tld, FunctionLib[] fld, Resource classRootDir, String className, boolean returnValue, boolean ignoreScopes)
	    throws TemplateException, IOException {

	// just to be sure
	PageSource ps = (sc instanceof PageSourceCode) ? ((PageSourceCode) sc).getPageSource() : null;

	return _compile(config, ps, sc, className, tld, fld, classRootDir, returnValue, ignoreScopes);
    }

    /*
     * private byte[] _compiless(ConfigImpl config,PageSource ps,SourceCode sc,String className,
     * TagLib[] tld, FunctionLib[] fld, Resource classRootDir,TransfomerSettings settings) throws
     * TemplateException { Factory factory = BytecodeFactory.getInstance(config);
     * 
     * Page page=null;
     * 
     * TagLib[][] _tlibs=new TagLib[][]{null,new TagLib[0]}; _tlibs[CFMLTransformer.TAG_LIB_GLOBAL]=tld;
     * // reset page tlds if(_tlibs[CFMLTransformer.TAG_LIB_PAGE].length>0) {
     * _tlibs[CFMLTransformer.TAG_LIB_PAGE]=new TagLib[0]; }
     * 
     * CFMLScriptTransformer scriptTransformer = new CFMLScriptTransformer();
     * scriptTransformer.transform( BytecodeFactory.getInstance(config) , page , new EvaluatorPool() ,
     * _tlibs, fld , null , config.getCoreTagLib(ps.getDialect()).getScriptTags() , sc , settings);
     * 
     * //CFMLExprTransformer extr=new CFMLExprTransformer(); //extr.transform(factory, page, ep, tld,
     * fld, scriptTags, cfml, settings)
     * 
     * return null; }
     */

    private Result _compile(ConfigImpl config, PageSource ps, SourceCode sc, String className, TagLib[] tld, FunctionLib[] fld, Resource classRootDir, boolean returnValue,
	    boolean ignoreScopes) throws TemplateException, IOException {
	if (className == null) className = ps.getClassName();

	Result result = null;
	// byte[] barr = null;
	Page page = null;
	Factory factory = BytecodeFactory.getInstance(config);
	try {
	    page = sc == null ? cfmlTransformer.transform(factory, config, ps, tld, fld, returnValue, ignoreScopes)
		    : cfmlTransformer.transform(factory, config, sc, tld, fld, System.currentTimeMillis(),
			    config.getDotNotationUpperCase(), returnValue, ignoreScopes);
	    page.setSplitIfNecessary(false);
	    try {
		result = new Result(page, page.execute(className));
	    }catch (RuntimeException re) {
		String msg = StringUtil.emptyIfNull(re.getMessage());
		if (StringUtil.indexOfIgnoreCase(msg, "Method too large") != -1 || StringUtil.indexOfIgnoreCase(msg, "Method code too large!") != -1) {
		    page = sc == null ? cfmlTransformer.transform(factory, config, ps, tld, fld, returnValue, ignoreScopes)
			    : cfmlTransformer.transform(factory, config, sc, tld, fld, System.currentTimeMillis(),
				    config.getDotNotationUpperCase(), returnValue, ignoreScopes);

		    page.setSplitIfNecessary(true);
		    result = new Result(page, page.execute(className));
		}
		else throw re;
	    }catch (ClassFormatError cfe) {
		String msg = StringUtil.emptyIfNull(cfe.getMessage());
		if (StringUtil.indexOfIgnoreCase(msg, "Invalid method Code length") != -1) {
		    page = ps != null ? cfmlTransformer.transform(factory, config, ps, tld, fld, returnValue, ignoreScopes)
			    : cfmlTransformer.transform(factory, config, sc, tld, fld, System.currentTimeMillis(),
				    config.getDotNotationUpperCase(), returnValue, ignoreScopes);

		    page.setSplitIfNecessary(true);
		    result = new Result(page, page.execute(className));
		}
		else throw cfe;
	    }

	    // store
	    if (classRootDir != null) {
		Resource classFile = classRootDir.getRealResource(page.getClassName() + ".class");
		Resource classFileDirectory = classFile.getParentResource();
		if (!classFileDirectory.exists()) classFileDirectory.mkdirs();
		IOUtil.copy(new ByteArrayInputStream(result.barr), classFile, true);
	    }
		// TODO: this gives 500 on restart-lucee.cfc for some reason, need to fix
//		optimizeResult(result);

	    return result;
	}


	catch (AlreadyClassException ace) {

	    byte[] bytes = ace.getEncrypted() ? readEncrypted(ace) : readPlain(ace);

	    result = new Result(null, bytes);

	    String displayPath = ps != null ? "[" + ps.getDisplayPath() + "] " : "";
	    String srcName = ASMUtil.getClassName(result.barr);

	    int dialect = sc == null ? ps.getDialect() : sc.getDialect();
	    // source is cfm and target cfc
	    if (endsWith(srcName, Constants.getCFMLTemplateExtensions(), dialect) && className
		    .endsWith("_" + Constants.getCFMLComponentExtension() + (Constants.CFML_CLASS_SUFFIX))) {
		throw new TemplateException("source file " + displayPath + "contains the bytecode for a regular cfm template not for a component");
	    }
	    // source is cfc and target cfm
	    if (srcName.endsWith(
			    "_" + Constants.getCFMLComponentExtension() + (Constants.CFML_CLASS_SUFFIX))
		    && endsWith(className, Constants.getCFMLTemplateExtensions(), dialect))
		throw new TemplateException("source file " + displayPath + "contains a component not a regular cfm template");

	    // rename class name when needed
	    if (!srcName.equals(className)) result = new Result(result.page, ClassRenamer.rename(result.barr, className));
	    // store
	    if (classRootDir != null) {
		Resource classFile = classRootDir.getRealResource(className + ".class");
		Resource classFileDirectory = classFile.getParentResource();
		if (!classFileDirectory.exists()) classFileDirectory.mkdirs();
		result = new Result(result.page, Page.setSourceLastModified(result.barr, ps != null ? ps.getPhyscalFile().lastModified() : System.currentTimeMillis()));
		IOUtil.copy(new ByteArrayInputStream(result.barr), classFile, true);
	    }

		// TODO: this gives 500 on restart-lucee.cfc for some reason, need to fix
//		optimizeResult(result);

	    return result;
	}
	catch (TransformerException bce) {
	    Position pos = bce.getPosition();
	    int line = pos == null ? -1 : pos.line;
	    int col = pos == null ? -1 : pos.column;
	    if (ps != null) bce.addContext(ps, line, col, null);
	    throw bce;
	}
    }


	// TODO: add second pass optimizer transformations
    /*
     need to support these types of optimizations:
     defining the type and parent object of a variable
     transforming isDefined("request.key.key2") to structkeyexists(request, "key") and structkeyexists(request.key, "key2")


     change Variable,
      */

    // need to be able to recurse through statement expressions
	// might need to allow returning a new expression to replace it in the parent record.
	// might need to allow removing expression.
	private void optimizeExpression(){

		/*
		ExpressionInvoker; // has members I'd need to process
		 */
		/*
VariableString // variable that is definitely a string inside
ExpressionInvoker // a kind of cfml method call
Call // a kind of cfml method call
VariableRef // used in for(row in query) for the row part, and others
DynAssign // used for static assignments - not sure if that means CFC static or something else
Assign // left = right cfml assignment
Argument // any function argument, not the scope

EmptyArray // cfml empty array
EmptyStruct // cfml empty struct
Empty
EmptyArray

// these convert expression to the right type and also wrap them as unique typed Expression.  A literal value is stored internally
CastFloat
CastString
CastDouble
CastBoolean
CastFloat
CastString
CastOther
CastInt
Null

// literal values
LiteralStringArray
LitLongImpl
LitIntegerImpl
LitBooleanImpl
LitStringImpl
LitDoubleImpl
LitFloatImpl
NullConstant // check full null support and writes out the right null value

// may contain literal or cast expression
ExprDouble
ExprBoolean
ExprFloat
ExprInt

Variable
VariableImpl

// other
CollectionKeyArray // Array of Key objects

// operators - have member expression that may need to be updated
OpBool
OpDouble
OpDecision
OpString
OpBool
OpContional
OpElvis
OpDouble
OpUnary
OpNegateNumber
OpDecision
OpNegate
OpString
OpBigDecimal

// ignore for now
FailSafeExpression; // never used
FunctionAsExpression // used for closures - ignore for now
ClosureAsExpression // used for closures - ignore for now

// bytecode exceptions
RPCException
FunctionNotSupported
CasterException
AbortException
FunctionException
XMLException
ValueSupport // FusionDebug stuff
Value // FusionDebug stuff

// query of query stuff that can be ignored
Column
ColumnExpression
BracketExpression
Operation2
Operation1
OperationN
Operation3
Operation


		 */
	}

	class StatementContainer{
		public Function functionParent=null;
		public TagThread tagThreadParent=null;
		public ForEach forEachParent=null;
		public Statement statement=null;
		public int type=StatementTypes.FUNCTION;
	}
	private Result optimizeStatement(Result result, StatementContainer sc) {
		// how many types of statements are there?
		if(sc.statement instanceof Return){
			Return sReturn=(Return) sc.statement;
			// sReturn.expr // be able to loop over all types of Expressions
		}else if(sc.statement instanceof DoWhile){
			DoWhile sDoWhile=(DoWhile) sc.statement;
		}else if(sc.statement instanceof PrintOut){
			PrintOut sPrintOut=(PrintOut) sc.statement;
		}else if(sc.statement instanceof Function){
			Function sFunction=(Function) sc.statement;
		}else if(sc.statement instanceof Switch){
			Switch sSwitch=(Switch) sc.statement;
		}else if(sc.statement instanceof NativeSwitch){
			NativeSwitch sNativeSwitch=(NativeSwitch) sc.statement;
		}else if(sc.statement instanceof TryCatchFinally){
			TryCatchFinally sTryCatchFinally=(TryCatchFinally) sc.statement;
		}else if(sc.statement instanceof ExpressionAsStatement){
			ExpressionAsStatement sExpressionAsStatement=(ExpressionAsStatement) sc.statement;
		}else if(sc.statement instanceof While){
			While sWhile=(While) sc.statement;
		}else if(sc.statement instanceof ForEach){
			ForEach sForEach=(ForEach) sc.statement;

			StatementContainer scForEach=new StatementContainer();
			sc.forEachParent=sForEach;
			sc.type=StatementTypes.FOR_EACH;
			for(Statement s2 : sForEach.getBodyBase().statements) {
				result=optimizeStatement(result, scForEach);

			}
		}else if(sc.statement instanceof TagBase){
			TagBase sTagBase=(TagBase) sc.statement;
			sTagBase.getBody();
		}else if(sc.statement instanceof Abort){
			Abort sAbort=(Abort) sc.statement;
			// just creates newInstance
		}else if(sc.statement instanceof For){
			For sFor=(For) sc.statement;
			sFor.getBodyBase();
			// sFor.condition; // multiple kinds for the middle expr?
			// what is sFor.update - the right side?
			// what is sFor.init - the left side?
		}else if(sc.statement instanceof SystemOut){
			SystemOut sSystemOut=(SystemOut) sc.statement;
			// this is for System.out.println
		}else if(sc.statement instanceof Condition){
			Condition sCondition=(Condition) sc.statement;
			// multiple kinds
			for(Condition.Pair pair: sCondition.ifs) { // array of Pair
//					    	pair.body;
//					    	pair.condition;
			}
			// this one is just abstract - and means that the expression has children I guess.
//				    }else if(s instanceof StatementBaseNoFinal){
//						StatementBaseNoFinal sStatementBaseNoFinal=(StatementBaseNoFinal) s;
//					    sStatementBaseNoFinal.getFlowControlFinal()
		}else if(sc.statement instanceof Tag){
			Tag sTag=(Tag) sc.statement;
		}
		if(sc.type==0) {
			// sc.functionParent
		}else if(sc.type==1){
			// sc.tagThreadParent
		}
		return result;
	}
    private Result optimizeResult(Result result){
    	Result resultNew=result;

	    for(TagThread tt: result.page.threads){
		    StatementContainer sc=new StatementContainer();
		    sc.tagThreadParent=tt;
		    sc.type=StatementTypes.TAG_THREAD;
		    for(Statement s : tt.getBodyBase().statements) {
			    resultNew=optimizeStatement(result, sc);
		    }
		    tt.appendix.toString(); // string
		    tt.fullname.toString(); // string
		    tt.tagLibTag.getName();
		    tt.attributes.size(); // map
		    tt.missingAttributes.size(); // hashset
		    boolean tempBool=tt.scriptBase; // boolean

		    tt.metadata.size(); // map
	    }
	    for(IFunction f : result.page.functions){
		    if(f instanceof Function) {
			    Function func = (Function) f;
			    StatementContainer sc=new StatementContainer();
			    sc.functionParent=func;
			    sc.type=StatementTypes.FUNCTION;
			    for(Statement s : func.getBodyBase().statements) {
				    resultNew=optimizeStatement(result, sc);
			    }
		    }
	    }
    	return resultNew;
    }

    private byte[] readPlain(AlreadyClassException ace) throws IOException {
	return IOUtil.toBytes(ace.getInputStream(), true);
    }

    private byte[] readEncrypted(AlreadyClassException ace) throws IOException {

	String str = System.getenv("PUBLIC_KEY");
	if (str == null) str = System.getProperty("PUBLIC_KEY");
	if (str == null) throw new RuntimeException("to decrypt encrypted bytecode, you need to set PUBLIC_KEY as system property or or enviroment variable");

	byte[] bytes = IOUtil.toBytes(ace.getInputStream(), true);
	try {
	    PublicKey publicKey = RSA.toPublicKey(str);
	    // first 2 bytes are just a mask to detect encrypted code, so we need to set offset 2
	    bytes = RSA.decrypt(bytes, publicKey, 2);
	}
	catch (IOException ioe) {
	    throw ioe;
	}
	catch (Exception e) {
	    throw new RuntimeException(e);
	}

	return bytes;
    }

    private boolean endsWith(String name, String[] extensions, int dialect) {
	for (int i = 0; i < extensions.length; i++) {
	    if (name.endsWith("_" + extensions[i] + (Constants.CFML_CLASS_SUFFIX))) return true;
	}
	return false;
    }

    public Page transform(ConfigImpl config, PageSource source, TagLib[] tld, FunctionLib[] fld, boolean returnValue, boolean ignoreScopes) throws TemplateException, IOException {
	return cfmlTransformer.transform(BytecodeFactory.getInstance(config), config, source, tld, fld, returnValue, ignoreScopes);
    }

    public class Result {

	public Page page;
	public byte[] barr;

	public Result(Page page, byte[] barr) {
	    this.page = page;
	    this.barr = barr;
	}
    }

    public void watch(PageSource ps, long now) {
	watched.offer(new WatchEntry(ps, now, ps.getPhyscalFile().length(), ps.getPhyscalFile().lastModified()));
    }

    public void checkWatched() {
	WatchEntry we;
	long now = System.currentTimeMillis();
	Stack<WatchEntry> tmp = new Stack<WatchEntry>();
	while ((we = watched.poll()) != null) {
	    // to young
	    if (we.now + 1000 > now) {
		tmp.add(we);
		continue;
	    }

	    if (we.length != we.ps.getPhyscalFile().length() && we.ps.getPhyscalFile().length() > 0) { // TODO this is set to avoid that removed files are removed from pool, remove
												       // this line if a UDF still wprks fine when the page is gone
		((PageSourceImpl) we.ps).flush();
	    }
	}

	// add again entries that was to young for next round
	Iterator<WatchEntry> it = tmp.iterator();
	while (it.hasNext()) {
	    watched.add(we = it.next());
	}
    }

    private class WatchEntry {

	private final PageSource ps;
	private final long now;
	private final long length;
	private final long lastModified;

	public WatchEntry(PageSource ps, long now, long length, long lastModified) {
	    this.ps = ps;
	    this.now = now;
	    this.length = length;
	    this.lastModified = lastModified;
	}
    }
}