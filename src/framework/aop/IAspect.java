package framework.aop;

import framework.aop.structures.MethodExecution;

public interface IAspect {
	
	public void before(Object source, MethodExecution method) throws Exception;
	public void after(Object source, MethodExecution method) throws Exception;
	public Object around(Object source, MethodExecution method);
}
