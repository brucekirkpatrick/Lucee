package lucee.runtime;

import lucee.runtime.type.UDF;

public interface ImplementationUdfCall {
	public Object udfCall(final PageContextImpl pageContext, final UDF udf, final int functionIndex) throws Throwable;
}
