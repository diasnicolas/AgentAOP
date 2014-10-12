package framework.aop.structures;

public class MethodArgs{

	private Object[] args;
	private Object[] typeArgs;
	
	public MethodArgs(Object[] args, Object[] typeArgs)
	{
		this.args = args;
		this.typeArgs = typeArgs;
	}
	
	public Object[] getArgs() {
		return args;
	}
	
	public Object[] getTypeArgs() {
		return typeArgs;
	}
}
