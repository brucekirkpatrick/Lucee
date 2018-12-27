package lucee.runtime;

import lucee.runtime.type.UDF;

public interface ImplementationUdfCall {
	public Object udfCall(final PageContextImpl pageContext, final UDF udf, final int functionIndex) throws Throwable;
	public Object udfDefaultValue(final PageContextImpl pc, final int functionIndex, final int argumentIndex, final Object defaultValue);
	public Object call(PageContextImpl pc) throws Throwable;
	public void threadCall(final PageContextImpl pageContext, final int threadIndex) throws Throwable;
}
