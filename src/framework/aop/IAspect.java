package framework.aop;

import framework.aop.structures.MethodExecution;

public interface IAspect {
	
	public void before(Object source, MethodExecution method);
	public void after(Object source, MethodExecution method);
	public Object around(Object source, MethodExecution method);
}
