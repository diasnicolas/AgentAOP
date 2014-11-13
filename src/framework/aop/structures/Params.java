package framework.aop.structures;

public class Params{

	private Object[] args;
	private Class[] typeArgs;
	
	public Params(Object[] args, Class[] typeArgs)
	{
		this.args = args;
		this.typeArgs = typeArgs;
	}
	
	public Object[] getArgs() {
		return args;
	}
	
	public Class[] getTypeArgs() {
		return typeArgs;
	}
}
