package framework.aop.tools;

import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import framework.aop.IAspect;
import framework.aop.annotations.AdviceType;
import framework.aop.annotations.Extension;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

class Agent {

	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new AOPClassFileTransformer());
	}
}

class AOPClassFileTransformer implements ClassFileTransformer {

	private String[] ignoredPackages = new String[] { "sun/", "java/", "javax/" };

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {

		for (int i = 0; i < ignoredPackages.length; i++) {
			if (className.startsWith(ignoredPackages[i])) {
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
		String typeAnnotation = annotation.annotationType().getName();

		if(typeAnnotation.equals("framework.aop.annotations.Before") ||
		   typeAnnotation.equals("framework.aop.annotations.After")  ||
		   typeAnnotation.equals("framework.aop.annotations.Around"))
		{	
			this.addAdvice(annotation, behavior);	
		}else
		{	
			Annotation a = annotation.annotationType().getAnnotation(Extension.class);
			
			if(a!=null)
				this.addExtension((Extension)a, behavior);
		}
	}
	
	private void addAdvice(Annotation annotation, CtBehavior behavior) 
	{	
		String typeAnnotation = annotation.annotationType().getSimpleName();
		AdviceType adviceType = AdviceType.valueOf(typeAnnotation);
		
		for(Class<? extends IAspect> aspect : this.getListOfAspects(annotation))
		{	
			try
			{	
				Weaver weaver = new Weaver(behavior,aspect,adviceType);
				weaver.combine();
				
			}catch(Exception e)
			{
				System.err.println(e.getMessage());
			}
		}
	}
	
	private void addExtension(Extension extension, CtBehavior behavior)
	{
		try
		{	
			Weaver weaver = new Weaver(behavior,extension.aspect(),extension.adviceType());
			weaver.combine();
		}catch(Exception e)
		{
			System.err.println(e.getMessage());
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
