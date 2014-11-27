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

/*
	# MAIS DE UM AROUND EM UM MÉTODO
*/

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
		this.classTarget = this.target.getDeclaringClass();
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
		CtMethod adviceMethod = this.createMethod();
		
		switch(this.adviceType)
		{
			case Before: this.addBefore(adviceMethod);
				break;
			case After: this.addAfter(adviceMethod);
				break;
			case Around: this.addAround(adviceMethod);
				break;
		}
	}
	
	private void addBefore(CtMethod adviceMethod)
			throws CannotCompileException, 
			        NotFoundException
	{
		this.classTarget.addMethod(adviceMethod);
		this.target.addLocalVariable("methodExecution",
		ClassPool.getDefault().get("framework.aop.structures.MethodExecution"));
		this.target.insertBefore(
				this.createMethodInfo("methodExecution", this.target)
				+adviceMethod.getName()+"($0,methodExecution);");
	}
	
	private void addAfter(CtMethod adviceMethod)
			throws CannotCompileException, 
			        NotFoundException
	{
		this.classTarget.addMethod(adviceMethod);
		this.target.addLocalVariable("methodExecution",
		ClassPool.getDefault().get("framework.aop.structures.MethodExecution"));
		this.target.insertAfter(
				this.createMethodInfo("methodExecution", this.target)
				+adviceMethod.getName()+"($0,methodExecution);");
	}
	
	private void addAround(CtMethod adviceMethod)
			throws CannotCompileException, 
			        NotFoundException
	{	
		String copyName = this.target.getName()+"$Copy";

		CtMethod copyMethod = CtNewMethod.copy((CtMethod)this.target, 
												copyName, this.classTarget,
												null);
		this.classTarget.addMethod(copyMethod);
		
		this.classTarget.addMethod(adviceMethod);
		this.target.setBody("{framework.aop.structures.MethodExecution "+
							this.createMethodInfo("methodExecution", this.target)
			    			+"return ($r)"+adviceMethod.getName()+
			    			"($0,methodExecution);}");
	}
	
	private String createMethodInfo(String variableName,CtBehavior method)
	{
		return
		variableName+"= new framework.aop.structures.MethodExecution(\""
		+method.getName()+"\",new framework.aop.structures.Params($args,$sig));";
	}
	
	private CtMethod createMethod() 
			throws CannotCompileException, 
					NotFoundException
	{	
		String advice = this.adviceType.toString().toLowerCase();
		String methodName = this.aspect.getSimpleName()+"$"+advice+"$"+
							this.target.getName();
		return CtNewMethod.copy(aspect.getDeclaredMethod(advice), 
								methodName, this.classTarget, null);
	}
}
