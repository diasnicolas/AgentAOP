package framework.aop.structures;

import java.lang.reflect.Method;

public class MethodExecution{
	
	private String name;
	private Params params;
	
	public MethodExecution(String name,Params params)
	{
		this.name = name;
		this.params = params;
	}
	
	public Params getParams() {
		return params;
	}

	public String getName() {
		return name;
	}

	public Object proceed(Object source)
	{	
		try
		{	
			Method metodo = source.getClass().getMethod(this.name+"$Copy", params.getTypeArgs());
			return metodo.invoke(source, params.getArgs());
			
		}catch(Exception e)
		{
			e.getStackTrace();
		}
		return null;
	}
}
