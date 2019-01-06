package lucee.runtime.tag.define;

import lucee.transformer.bytecode.statement.tag.Tag;

public class DefineType {
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
	public DefineType(Tag tag){
		this.tag=tag;
	}
	public DefineType duplicate(){
		DefineType tdNew=new DefineType(this.tag);
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
