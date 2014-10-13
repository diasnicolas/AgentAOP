package framework.aop;

public interface IAspect {
	
	public void before();
	public void after();
	public void around();
}
