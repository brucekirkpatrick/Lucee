package lucee.runtime.tag;

import lucee.runtime.PageSource;
import lucee.runtime.PageSourceImpl;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.tag.TagImpl;
import lucee.runtime.op.Caster;
import lucee.transformer.bytecode.BytecodeContext;
import lucee.transformer.bytecode.cast.CastOther;
import lucee.transformer.bytecode.literal.LitDoubleImpl;
import lucee.transformer.bytecode.literal.LitStringImpl;
import lucee.transformer.bytecode.statement.tag.Attribute;
import lucee.transformer.bytecode.statement.tag.Tag;
import lucee.transformer.expression.Expression;
import lucee.transformer.library.tag.TagLibTag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Define extends TagImpl {
	public static LinkedHashMap<String, CFMLTypeDefinition> cfmlTypeVariables=new LinkedHashMap<>();
	public static LinkedHashMap<String, CFMLTypeDefinition> cfmlTypeDefinitions=new LinkedHashMap<>();
	public class CFMLTypeDefinition{
		public Tag tag;
		public int id;
		public boolean required=false;
		public boolean dynamic=false;
		public String importValue="";
		public String name="";
		public String type="";
		public String arrayType="";
		public String component="";
		public String variable="";
		public CFMLTypeDefinition(Tag tag){
			this.tag=tag;
		}
		public CFMLTypeDefinition duplicate(){
			CFMLTypeDefinition tdNew=new CFMLTypeDefinition(this.tag);
			tdNew.required=this.required;
			tdNew.dynamic=this.dynamic;
			tdNew.importValue=this.importValue;
			tdNew.name=this.name;
			tdNew.type=this.type;
			tdNew.arrayType=this.arrayType;
			tdNew.component=this.component;
			tdNew.variable=this.variable;
			return tdNew;
		}
	}

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

	public static CFMLTypeDefinition checkVariableDefinition(LinkedHashMap<String, CFMLTypeDefinition> variables, String variableString){
		CFMLTypeDefinition def=variables.getOrDefault(variableString, null);
		if(def!=null){
			return def;
		}
		// search for prefix matches
		for(CFMLTypeDefinition typeDef:variables.values()){
			if(variableString.substring(0, typeDef.variable.length()).equalsIgnoreCase(variableString)){
				String[] parts=typeDef.variable.split(".");
				// transform initial statements.
			}
		}
		return def;
	}
	public static CFMLTypeDefinition checkNameDefinition(LinkedHashMap<String, CFMLTypeDefinition> names){
		CFMLTypeDefinition def=null;
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

		CFMLTypeDefinition typeDefine=new CFMLTypeDefinition(tag);

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
		boolean isApplicationCFC= ps.getFileName().equalsIgnoreCase("application.cfc");

		if(typeDefine.importValue.length()>0){
			// verify no other attributes were used
			if(typeDefine.variable.length()>0 && typeDefine.component.length()>0 && typeDefine.name.length()>0 && typeDefine.type.length()>0 && typeDefine.arrayType.length()>0 && requiredAttr!=null && dynamicAttr!=null) {
				throw new RuntimeException("cfdefine error - import attribute can't be combined with any other cfdefine attribute.");
			}
			// do import
			if(isApplicationCFC) {
				// TODO: compile/load the pagesource for typeDefine.importValue
				for(CFMLTypeDefinition typeDefinition:ps.cfmlTypeVariables.values()){
					CFMLTypeDefinition td=typeDefinition.duplicate();
					td.id=cfmlTypeVariables.size();
					cfmlTypeVariables.put(typeDefinition.variable, td);
				}
				for(CFMLTypeDefinition typeDefinition:ps.cfmlTypeDefinitions.values()){
					CFMLTypeDefinition td=typeDefinition.duplicate();
					td.id=cfmlTypeDefinitions.size();
					cfmlTypeDefinitions.put(typeDefinition.variable, td);
				}
			}else{
				for(CFMLTypeDefinition typeDefinition:ps.cfmlTypeVariables.values()){
					CFMLTypeDefinition td=typeDefinition.duplicate();
					td.id=ps.cfmlTypeVariables.size();
					ps.cfmlTypeVariables.put(typeDefinition.variable, td);
				}
				for(CFMLTypeDefinition typeDefinition:ps.cfmlTypeDefinitions.values()){
					CFMLTypeDefinition td=typeDefinition.duplicate();
					td.id=ps.cfmlTypeDefinitions.size();
					ps.cfmlTypeDefinitions.put(typeDefinition.variable, td);
				}
			}


			return;
		}

		if(typeDefine.variable.length()>0 && typeDefine.component.length()>0){
			// verify no other attributes were used
			if(typeDefine.importValue.length()>0 && typeDefine.name.length()>0 && typeDefine.type.length()>0 && typeDefine.arrayType.length()>0 && requiredAttr!=null) {
				throw new RuntimeException("cfdefine error - name, required, type, arrayType and import can't be combined with the variable attribute.");
			}

			if(isApplicationCFC) {
				typeDefine.id = cfmlTypeVariables.size();
				cfmlTypeVariables.put(typeDefine.variable, typeDefine);
			}else{
				typeDefine.id = ps.cfmlTypeVariables.size();
				ps.cfmlTypeVariables.put(typeDefine.variable, typeDefine);
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
			// verify no other attributes were used
			if(typeDefine.importValue.length()>0 && typeDefine.variable.length()>0 && typeDefine.component.length()>0 && typeDefine.arrayType.length()>0) {
				throw new RuntimeException("cfdefine error - variable, component and import can't be combined with the name attribute.");
			}

			if(isApplicationCFC) {
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
