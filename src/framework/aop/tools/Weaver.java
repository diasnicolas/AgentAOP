package framework.aop.tools;

import java.lang.annotation.Annotation;
import framework.aop.IAspect;
import framework.aop.annotations.Initialize;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/*
	# ADICIONAR INTERCEPTACAO AROUND EM MÉTODOS COM PARAMETROS
	# ADICIONAR RETORNO DE MÉTODO NO PROCEED
	# ADICIONAR INFORMAÇÕES COMO PARAMETROS DO BEFORE, AFTER, AROUND
	# ADICIONAR POSSIBILIDADE DE EXTENSÃO EXEMPLO: @NomeDoAspecto
*/

class Weaver {
	
	private CtBehavior target;
	private CtClass    aspect;
	private CtClass    classTarget;
	private Class<? extends IAspect>    lastAroundMethod;
	private AdviceType adviceType;
	public static String lastClassTarget="";
	public static String copyMethodName="";
	
	public Weaver(CtBehavior target, Class<? extends IAspect>  aspect, AdviceType adviceType) 
			throws NotFoundException
	{
		this.target      = target;
		this.aspect      = ClassPool.getDefault().get(aspect.getSimpleName());
		this.adviceType  = adviceType;
		this.classTarget = this.target.getDeclaringClass();
		
		if(!this.classTarget.getName().equals(lastClassTarget))
			Weaver.copyMethodName = "";
		
		Weaver.lastClassTarget = this.classTarget.getName();
	}
	
	public Weaver(CtBehavior target, Class<? extends IAspect>  aspect, AdviceType adviceType,Class<? extends IAspect> lastAroundMethod) 
			throws NotFoundException
	{
		this(target, aspect, adviceType);
		this.lastAroundMethod = lastAroundMethod;
	}
	
	public void combine()
			throws ClassNotFoundException, CannotCompileException, NotFoundException
	{	
		this.addFields();
		this.addAdviceMethod();
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
			throws CannotCompileException
	{
		this.classTarget.addMethod(adviceMethod);
		this.target.insertBefore(adviceMethod.getName()+"();");
	}
	
	private void addAfter(CtMethod adviceMethod)
			throws CannotCompileException
	{
		this.classTarget.addMethod(adviceMethod);
		this.target.insertAfter(adviceMethod.getName()+"();");
	}
	
	private void addAround(CtMethod adviceMethod)
			throws CannotCompileException
	{	
		String copyName = this.target.getName()+"$Copy";
		
		if(!Weaver.copyMethodName.equals(copyName))
		{	
			Weaver.copyMethodName = copyName;
			CtMethod copyMethod = CtNewMethod.copy((CtMethod)this.target, Weaver.copyMethodName, this.classTarget, null);
			this.classTarget.addMethod(copyMethod);
			
		}else
			Weaver.copyMethodName = this.lastAroundMethod.getSimpleName()+"$around$"+this.target.getName();
		
		adviceMethod.instrument(new ExprEditor() {
			
		     public void edit(MethodCall m) throws CannotCompileException {
		    	 
		    	 if (m.getMethodName().equals("proceed") &&
		        	 m.getClassName().equals("framework.aop.structures.CurrentMethod")) {
		    		 m.replace(Weaver.copyMethodName+"();");
		         }
		     }
		 });
		
		this.classTarget.addMethod(adviceMethod);
		this.target.setBody("{"+adviceMethod.getName()+"();}");
	}
	
	private CtMethod createMethod() 
			throws CannotCompileException, NotFoundException
	{	
		String advice = this.adviceType.toString().toLowerCase();
		String methodName = this.aspect.getSimpleName()+"$"+advice+"$"+this.target.getName();
		return CtNewMethod.copy(aspect.getDeclaredMethod(advice), 
											     methodName, this.classTarget, null);
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
}
