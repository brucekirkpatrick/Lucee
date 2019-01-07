package lucee.runtime.tag.define;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;
import lucee.runtime.*;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.tag.TagImpl;
import lucee.runtime.functions.other.CreatePageContext;
import lucee.runtime.op.Caster;
import lucee.runtime.tag.define.DefineType;
import lucee.runtime.type.scope.ScopeFactory;
import lucee.transformer.bytecode.BytecodeContext;
import lucee.transformer.bytecode.cast.CastOther;
import lucee.transformer.bytecode.expression.var.UDF;
import lucee.transformer.bytecode.literal.LitDoubleImpl;
import lucee.transformer.bytecode.literal.LitStringImpl;
import lucee.transformer.bytecode.statement.tag.Attribute;
import lucee.transformer.bytecode.statement.tag.Tag;
import lucee.transformer.expression.Expression;
import lucee.transformer.expression.var.DataMember;
import lucee.transformer.expression.var.Member;
import lucee.transformer.expression.var.Variable;
import lucee.transformer.library.tag.TagLibTag;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

public class Define extends TagImpl {
	public static LinkedHashMap<String, DefineType> cfmlTypeVariables=new LinkedHashMap<>();
	public static LinkedHashMap<String, DefineType> cfmlTypeDefinitions=new LinkedHashMap<>();


	// TODO: use get/set function to set that tag attributes

	@Override
	public void release() {
		super.release();
	}


	@Override
	public int doStartTag() throws PageException {
//		try {
//			pageContext.write("DefinedCalled");
//		} catch (IOException e) {
//			throw new RuntimeException("define startTag write failed", e);
//		}
		return SKIP_BODY;
	}

	@Override
	public int doEndTag() {
		return EVAL_PAGE;
	}

//
	public static DefineTypeMatch checkVariableDefinition(BytecodeContext bc, Member member){
		PageSourceImpl ps=(PageSourceImpl) bc.getPageSource();
//		return null;
//		ps.initializeDefine();
		boolean inApplicationCFC= ps.getFileName().equalsIgnoreCase("application.cfc");
		DefineTypeMatch match;
		if(inApplicationCFC){
			match=checkVariableDefinition(bc, member, cfmlTypeVariables); // , true
		}else {
			match=checkVariableDefinition(bc, member, ps.cfmlTypeVariables); // ,  false
		}
		if(match!=null){
//			match.defineType
//			match.matchOffset
		}
		return match;
	}

	public static DefineTypeMatch checkVariableDefinition(BytecodeContext bc, Member member, LinkedHashMap<String, DefineType> variables){ // , boolean inApplicationCFC
//		PageSourceImpl ps=(PageSourceImpl) bc.getPageSource();
//		return null;
//		ps.initializeDefine();

		// loop each member, check scope + current fields, until one matches.
		Variable parent=member.getParent();
		String scope=ScopeFactory.toStringScope(parent.getScope(), "undefined");
		List<Member> members=parent.getMembers();
		List<String> memberStrings=new ArrayList<>();
		String currentMembers=scope;

		// build all combinations first
		for(int i=0;i<members.size();i++) {
			Member m=members.get(i);
			String name;
			if(m instanceof UDF) {
				UDF udfMember = (UDF) m;
				name=udfMember.getName().toString();
			}else if(m instanceof DataMember) {
				DataMember dataMember = (DataMember) m;
				name=dataMember.getName().toString();
			}else{
				continue; // ignore certain types
			}
			currentMembers += "." + name;

			memberStrings.add(currentMembers);
		}

		DefineTypeMatch defineTypeMatch;
		DefineType def = variables.getOrDefault(currentMembers, null);
		if (def != null) {
			defineTypeMatch=new DefineTypeMatch(def, memberStrings.size());
			return defineTypeMatch;
		}

		Object[] keys = variables.entrySet().toArray();

		// we loop in reverse to check the most deep (or at least the last) keys first, in case there are overlapping cfdefine variables.
		for(int i=memberStrings.size()-1;i>=0;i--) {
			// in reverse order, check for a matching define which helps us find the define closest to the current code, which may override others
			for (int g = keys.length - 1; g >= 0; g--) {
				String key=keys[g].toString();
				if (currentMembers.equalsIgnoreCase(key)) {
					def = variables.getOrDefault(key, null);
					if(def!=null) {
						return new DefineTypeMatch(def, g);
					}
//					break;
				}
			}

//			if (def != null) {
//				// TODO: must transform all the statements from 0 to i to be a direct reference instead of the key name.
//				// actually, we just need to get the arrayIndex in application.cfc where the cfc was stored
//
//				break;
//			}
		}
		return null;

	}

	public static void storeBytecodeFields(ClassWriter cw){
		// + Opcodes.ACC_FINAL
		FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC, "componentVariables", "[Llucee/runtime/ComponentImpl;", null, null);
		fv.visitEnd();
	}

	public static void storeBytecodeData(GeneratorAdapter constrAdapter, PageSourceImpl ps, String className){
		// load "this"
		constrAdapter.visitVarInsn(Opcodes.ALOAD, 0);
		int count=ps.cfmlTypeVariables.size(); // TODO: the variables are not there yet...
		Type componentImplType=Type.getType(ComponentImpl.class);
		constrAdapter.push(count);
		constrAdapter.newArray(componentImplType);
		int index = 0;
		Set<Map.Entry<String, DefineType>> typeKeys = ps.cfmlTypeVariables.entrySet();
//				String key=keys[g].toString();
		// cfmlTypeVariables
		Iterator<Map.Entry<String, DefineType>> typeKeysIterator=typeKeys.iterator();
		while(typeKeysIterator.hasNext()){
			Map.Entry<String, DefineType> typeEntry=typeKeysIterator.next();
//			    typeEntry.getValue();
			constrAdapter.dup();
			constrAdapter.push(index++);
			constrAdapter.visitInsn(Opcodes.ACONST_NULL);
			constrAdapter.visitInsn(Opcodes.AASTORE);
		}
		String componentImplClassName = componentImplType.toString();
		constrAdapter.visitFieldInsn(Opcodes.PUTFIELD, className, "componentVariables", "["+componentImplClassName);

	}

	public static DefineType checkNameDefinition(LinkedHashMap<String, DefineType> names){
		DefineType def=null;
		return def;
	}

	public void registerType(BytecodeContext bc, Tag tag, TagLibTag tlt) throws PageException {
		// A) import only works by itself
		Attribute importValueAttr=tag.getAttribute("import");
		// B), variable and component are required together
		Attribute variableAttr=tag.getAttribute("variable");
		Attribute componentAttr=tag.getAttribute("component");
		// or C), name and type are required together
		Attribute nameAttr=tag.getAttribute("name");
		Attribute typeAttr=tag.getAttribute("type");
		// only if type is array
		Attribute arrayTypeAttr=tag.getAttribute("arrayType");
		// works with C
		Attribute requiredAttr=tag.getAttribute("required");
		// works with B or C
		Attribute dynamicAttr=tag.getAttribute("dynamic");

		Attribute[] attributes=new Attribute[]{importValueAttr, variableAttr, componentAttr, nameAttr, typeAttr, arrayTypeAttr, requiredAttr, dynamicAttr};
		String[] attributeTypes=new String[]{"string", "string", "string", "string", "string", "string", "boolean", "boolean"};

		DefineType typeDefine=new DefineType(tag);

		// we must prevent any runtime expressions from being used in the cfdefine attributes because we are providing compiler information, not runtime information.
		for(int i=0;i<attributes.length;i++){
			Attribute attribute=attributes[i];
			String attributeType=attributeTypes[i];
			if(attribute!=null){
				Expression value=attribute.getValue();
				if (value instanceof CastOther) {
					value=((CastOther) value).getExpr();
				}
				if (value instanceof LitDoubleImpl) {
					if(attributeType.equalsIgnoreCase("boolean")) {
						if(attribute.getName().equalsIgnoreCase("required")) {
							typeDefine.required = ((LitDoubleImpl) value).getBooleanValue();
						}else if(attribute.getName().equalsIgnoreCase("dynamic")) {
							typeDefine.dynamic = ((LitDoubleImpl) value).getBooleanValue();
						}
					}
				}else if(value instanceof LitStringImpl){
					String stringValue=((LitStringImpl) value).str;
					switch(attribute.getName()){
						case "import":
							typeDefine.importValue=stringValue;
							break;
						case "required":
							typeDefine.required = Caster.toBooleanValue(stringValue);
							break;
						case "dynamic":
							typeDefine.dynamic = Caster.toBooleanValue(stringValue);
							break;
						case "variable":
							typeDefine.variable=stringValue;
							break;
						case "component":
							typeDefine.component=stringValue;
							break;
						case "name":
							typeDefine.name=stringValue;
							break;
						case "type":
							typeDefine.type=stringValue;
							break;
						case "arrayType":
							typeDefine.arrayType=stringValue;
							break;
					}
				}else{
					throw new RuntimeException("cfdefine error - "+attribute.getName()+" attribute must be a string literal boolean or string boolean because cfdefine attributes can't be evaluated at runtime.");
				}
			}
		}
		PageSourceImpl ps=(PageSourceImpl) bc.getPageSource();

//		bc.getConfig()
//		ps.initializeDefine();
		boolean inApplicationCFC= ps.getFileName().equalsIgnoreCase("application.cfc");

		typeDefine.inApplicationCFC=inApplicationCFC;

		if(typeDefine.importValue.length()>0){
			throw new RuntimeException("cfdefine error - import attribute is not supported yet.");
			// verify no other attributes were used
//			if(typeDefine.variable.length()>0 && typeDefine.component.length()>0 && typeDefine.name.length()>0 && typeDefine.type.length()>0 && typeDefine.arrayType.length()>0 && requiredAttr!=null && dynamicAttr!=null) {
//				throw new RuntimeException("cfdefine error - import attribute can't be combined with any other cfdefine attribute.");
//			}
//			// TODO: need to still compile/load the pagesource for typeDefine.importValue here like the one that runs _compile() in PageSourceImpl
//			PageContextImpl pc=PageContextImpl.createPageContext();
//			ComponentImpl importComponent=(ComponentImpl) pc.loadComponent(typeDefine.importValue);
//			PageSourceImpl importPS=(PageSourceImpl) importComponent.getPageSource();
////				importPS.
//			// do import
//			if(inApplicationCFC) {
//				for(DefineType typeDefinition:importPS.cfmlTypeVariables.values()){
//					DefineType td=typeDefinition.duplicate();
//					td.id=cfmlTypeVariables.size();
//					cfmlTypeVariables.put(typeDefinition.variable, td);
//				}
//				for(DefineType typeDefinition:importPS.cfmlTypeDefinitions.values()){
//					DefineType td=typeDefinition.duplicate();
//					td.id=cfmlTypeDefinitions.size();
//					cfmlTypeDefinitions.put(typeDefinition.variable, td);
//				}
//			}else{
//				for(DefineType typeDefinition:importPS.cfmlTypeVariables.values()){
//					DefineType td=typeDefinition.duplicate();
//					td.id=ps.cfmlTypeVariables.size();
//					ps.cfmlTypeVariables.put(typeDefinition.variable, td);
//				}
//				for(DefineType typeDefinition:importPS.cfmlTypeDefinitions.values()){
//					DefineType td=typeDefinition.duplicate();
//					td.id=ps.cfmlTypeDefinitions.size();
//					ps.cfmlTypeDefinitions.put(typeDefinition.variable, td);
//				}
//			}
//			return;
		}

		if(typeDefine.variable.length()>0 && typeDefine.component.length()>0){
			// verify no other attributes were used
			if(typeDefine.name.length()>0 || typeDefine.type.length()>0 || typeDefine.arrayType.length()>0 || requiredAttr!=null) {
				throw new RuntimeException("cfdefine error - name, required, type, arrayType and import can't be combined with the variable attribute.");
			}

			if(inApplicationCFC) {
				typeDefine.id = cfmlTypeVariables.size();
				cfmlTypeVariables.put(typeDefine.variable, typeDefine);
				// TODO: write bytecode to store in the application.cfc component Page near top.
			}else{
				typeDefine.id = ps.cfmlTypeVariables.size();
				ps.cfmlTypeVariables.put(typeDefine.variable, typeDefine);

				// TODO: write bytecode to store in the current component Page near top.
			}
			return;
		}else {
			if (typeDefine.variable.length()>0) {
				if (typeDefine.component.length()==0) {
					throw new RuntimeException("cfdefine error - component attribute is required when variable attribute is specified.");
				}
			} else if (typeDefine.component.length()>0) {
				if (typeDefine.variable.length()==0) {
					throw new RuntimeException("cfdefine error - variable attribute is required when component attribute is specified.");
				}
			}
		}

		if(typeDefine.name.length()>0 && typeDefine.type.length()>0){

			if(inApplicationCFC) {
				typeDefine.id = cfmlTypeDefinitions.size();
				cfmlTypeDefinitions.put(typeDefine.name, typeDefine);
			}else{
				typeDefine.id = ps.cfmlTypeDefinitions.size();
				ps.cfmlTypeDefinitions.put(typeDefine.name, typeDefine);
			}
			return;
		}else {
			if (typeDefine.name.length()>0) {
				if (typeDefine.type.length()==0) {
					throw new RuntimeException("cfdefine error - type attribute is required when name attribute is specified.");
				}
			} else if (typeDefine.type.length()>0) {
				if (typeDefine.name.length()==0) {
					throw new RuntimeException("cfdefine error - name attribute is required when type attribute is specified.");
				}
			}
		}
	}

	public void setImport(String s){

	}
	public void setVariable(String s){

	}
	public void setType(String s){

	}
	public void setArrayType(String s){

	}
	public void setName(String s){

	}
	public void setRequired(Boolean s){

	}
	public void setComponent(String s){

	}
	public void setDynamic(Boolean b){

	}
}
