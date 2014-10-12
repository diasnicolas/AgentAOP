package framework.aop.tools;

import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import framework.aop.IAspect;

class Agent {

	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new AOPClassFileTransformer());
	}
}

class AOPClassFileTransformer implements ClassFileTransformer {

	private String[] ignore = new String[] { "sun/", "java/", "javax/" };

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {

		for (int i = 0; i < ignore.length; i++) {
			if (className.startsWith(ignore[i])) {
				return classfileBuffer;
			}
		}
		return processClass(className, classBeingRedefined, classfileBuffer);
	}

	private byte[] processClass(String clazzName, Class<?> clazz, byte[] b) {
		ClassPool pool = ClassPool.getDefault();
		CtClass cl = null;
		try {
			cl = pool.makeClass(new java.io.ByteArrayInputStream(b));
			
			for(CtBehavior behavior : cl.getDeclaredBehaviors())
			{
				for(Object a : behavior.getAnnotations())
				{
					this.addAdviceIfAnnotated((Annotation)a, behavior);
				}
			}
			b = cl.toBytecode();

		} catch (Exception e) {
			System.err.println("Não foi possível modificar a classe " + clazzName
					+ ",  erro : " + e.toString());
		} finally {
			 if (cl != null) cl.detach();
		}
		return b;
	}
	
	private void addAdviceIfAnnotated(Annotation annotation, CtBehavior behavior) throws NotFoundException
	{
		String typeAnnotation = annotation.annotationType().getSimpleName();
		
		if(typeAnnotation.equals("Before") ||
		   typeAnnotation.equals("After")  ||
		   typeAnnotation.equals("Around"))
		{	
			this.addAdvice(annotation, behavior);
		}
	}
	
	private void addAdvice(Annotation annotation, CtBehavior behavior) 
	{	
		String typeAnnotation = annotation.annotationType().getSimpleName();
		AdviceType adviceType = AdviceType.valueOf(typeAnnotation);
		
		Class<? extends IAspect> lastAround=null;
		
		
		
		for(Class<? extends IAspect> aspect : this.getListOfAspects(annotation))
		{	
			try
			{	Weaver weaver;
				if(adviceType.equals(AdviceType.Around) && lastAround != null)
					weaver = new Weaver(behavior,aspect,adviceType,lastAround);
				else
					weaver = new Weaver(behavior,aspect,adviceType);
				weaver.combine();
				
			}catch(Exception e)
			{
				System.err.println(e.getMessage());
			}
			lastAround = aspect;
		}
	}
	
	@SuppressWarnings("unchecked")
	private Class<? extends IAspect>[] getListOfAspects(Annotation annotation)
	{	
		Class<? extends IAspect>[] list=new Class[0];
		try
		{
			Method method = annotation.getClass().getMethod("value");
			Object result = method.invoke(annotation);
			list =  (Class<? extends IAspect>[])result;
		
		}catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
		return list;
	}
}
