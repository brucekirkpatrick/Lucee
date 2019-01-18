package lucee.runtime.functions.other;

import lucee.runtime.PageContext;
import lucee.runtime.exp.FunctionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.op.Caster;

public class Throw extends BIF {
	public static String call(PageContext pc, Object message, String type, String detail) throws PageException {
		throw new RuntimeException(message.toString()+" Type:"+type+" Detail:"+detail);
	}
		public static String call(PageContext pc, Object message, String type) throws PageException {
			throw new RuntimeException(message.toString()+" Type:"+type);
		}
		public static String call(PageContext pc, Object message) throws PageException {
			throw new RuntimeException(message.toString());
		}

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		if (args.length == 3){
			call(pc, args[0], Caster.toString(args[1]), Caster.toString(args[2]));
			return null;
		}else if (args.length == 2){
			call(pc, args[0], Caster.toString(args[1]));
			return null;
		}else if (args.length == 1){
			call(pc, args[0]);
			return null;
		}
		throw new FunctionException(pc, "Throw", 0, 2, args.length);
	}
}