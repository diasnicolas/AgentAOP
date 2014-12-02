package framework.aop.tools;

import java.lang.annotation.Annotation;
import framework.aop.IAspect;
import framework.aop.annotations.AdviceType;
import framework.aop.annotations.Initialize;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

class Weaver {
	
	private CtBehavior target;
	private CtClass    aspect;
	private CtClass    classTarget;
	private AdviceType adviceType;
	
	public Weaver(CtBehavior target, Class<? extends IAspect>  aspect, AdviceType adviceType) 
			throws NotFoundException
	{
		this.target      = target;
		this.aspect      = ClassPool.getDefault().get(aspect.getName());
		this.adviceType  = adviceType;
		this.classTarget = this.target.getDeclaringClass();;
	}
	
	public void combine()
			throws ClassNotFoundException, 
			        CannotCompileException, 
			        NotFoundException
	{	
		this.addFields();
		this.addAdviceMethod();
	}
	
	private void addFields() 
			throws ClassNotFoundException, CannotCompileException
	{
		for(CtField field : this.aspect.getDeclaredFields())
		{	
			try
			{	
				this.classTarget.getDeclaredField(field.getName());
				
			}catch(Exception e)
			{
				this.addField(field);
			}
		}
	}
	
	private void addField(CtField field) 
			throws ClassNotFoundException, CannotCompileException
	{
		String initialValue = this.getInitialValueField(field);
		
		if(initialValue.equals(""))
			this.classTarget.addField(new CtField(field,this.classTarget));
		else
			this.classTarget.addField(new CtField(field,this.classTarget),initialValue);
	}
	
	private String getInitialValueField(CtField field)
			throws ClassNotFoundException
	{
		String value = "";
		Object[] annotationsField = field.getAnnotations();

		for (Object annotationField : annotationsField) 
		{
			Annotation f = (Annotation) annotationField;
			if (f.annotationType().getSimpleName().equals("Initialize")) {
				value = ((Initialize)f).value();
			}
		}
		return value;
	}
	
	private void addAdviceMethod()
			throws CannotCompileException, NotFoundException
	{
		String adviceMethodName = this.createMethod();
		
		switch(this.adviceType)
		{
			case Before: this.addBefore(adviceMethodName);
				break;
			case After: this.addAfter(adviceMethodName);
				break;
			case Around: this.addAround(adviceMethodName);
				break;
		}
	}
	
	private void addBefore(String adviceMethodName)
			throws CannotCompileException, 
			        NotFoundException
	{
		this.target.addLocalVariable("methodExecution",
		ClassPool.getDefault().
		get("framework.aop.structures.MethodExecution"));
		
		this.target.insertBefore(
				this.createMethodInfo("methodExecution", this.target)
				+adviceMethodName+"($0,methodExecution);");
	}
	
	private void addAfter(String adviceMethodName)
			throws CannotCompileException, 
			        NotFoundException
	{
		this.target.addLocalVariable("methodExecution",
		ClassPool.getDefault().
		get("framework.aop.structures.MethodExecution"));
		
		this.target.insertAfter(
				this.createMethodInfo("methodExecution", this.target)
				+adviceMethodName+"($0,methodExecution);");
	}
	
	private void addAround(String adviceMethodName)
			throws CannotCompileException, 
			        NotFoundException
	{	
		String copyName = this.target.getName()+"$Copy";
		
		CtMethod copyMethod = CtNewMethod.copy((CtMethod)this.target, 
												copyName, this.classTarget,
												null);
		this.classTarget.addMethod(copyMethod);
		
		this.target.setBody("{framework.aop.structures.MethodExecution "+
							this.createMethodInfo("methodExecution", this.target)
			    			+"return ($r)"+adviceMethodName+
			    			"($0,methodExecution);\"\";}");
	}
	
	private String createMethodInfo(String variableName,CtBehavior method)
	{
		return
		variableName+"= new framework.aop.structures.MethodExecution(\""
		+method.getName()+"\",new framework.aop.structures.Params($args,$sig));";
	}
	
	private String createMethod() 
			throws CannotCompileException, 
					NotFoundException
	{	
		String advice = this.adviceType.toString().toLowerCase();
		String methodName = this.aspect.getSimpleName().toLowerCase()+"_"+advice;
		
		try
		{
			this.classTarget.getDeclaredMethod(methodName);
			
		}catch(NotFoundException e)
		{	
			CtMethod adviceMethod = CtNewMethod.copy(aspect.getDeclaredMethod(advice), 
					methodName, this.classTarget, null);
			
			this.addTryCatch(adviceMethod);
			this.classTarget.addMethod(adviceMethod);
		}
		return methodName;
	}
	
	private void addTryCatch(CtMethod method) 
			throws NotFoundException, 
			        CannotCompileException
	{
		if(this.adviceType.equals(AdviceType.Before) ||
				   this.adviceType.equals(AdviceType.After))
		{
				CtClass etype = ClassPool.getDefault().get("java.lang.Exception");
				method.addCatch("{ System.out.println(($e).getMessage());return; }", etype);
		}
	}
}