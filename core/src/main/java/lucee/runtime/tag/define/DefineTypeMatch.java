package lucee.runtime.tag.define;

public class DefineTypeMatch {
	public DefineType defineType;
	public int matchOffset;
	public DefineTypeMatch(DefineType defineType, int matchOffset){
		this.defineType=defineType;
		this.matchOffset=matchOffset;
	}
}
