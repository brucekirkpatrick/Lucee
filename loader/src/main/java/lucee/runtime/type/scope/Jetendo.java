package lucee.runtime.type.scope;

import lucee.runtime.PageContext;

public interface Jetendo extends Scope {

    public abstract void reload();
    public abstract void reload(PageContext pc);
    public Boolean memberBoolFunc();
    public Object callCFMLFunction();
}