package lucee.runtime;

import lucee.runtime.type.UDF;

// FUTURE add to Page and delete this class
public abstract class PageImpl extends Page implements PagePro, ImplementationUdfCall {

    public int getHash() {
	return 0;
    }

    public long getSourceLength() {
	return 0;
    }
	public Object udfCall(final PageContextImpl pageContext, final UDF udf, final int functionIndex) throws Throwable {
		return null;
	}
	public Object call(PageContextImpl pc) throws Throwable{
    	return null;
	}
}
