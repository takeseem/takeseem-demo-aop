/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.takeseem.demo.aop;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.takeseem.demo.aop.model.AsmClassLoader;
import com.takeseem.demo.aop.model.CglibMethodInterceptor;
import com.takeseem.demo.aop.model.JavassistMethodHandler;
import com.takeseem.demo.aop.model.JdkProxyInvocationHandler;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.util.proxy.ProxyFactory;
import net.sf.cglib.proxy.Enhancer;

/**
 * @author <a href="mailto:yh@takeseem.com">杨浩</a>
 */
public class UtilProxy {

	@SuppressWarnings("unchecked")
	public static <T, O extends T> T createJdkProxy(O target, Class<?>... interfaces) {
		return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, new JdkProxyInvocationHandler(target));
	}

	@SuppressWarnings("unchecked")
	public static <T> T createCglibProxy(T target) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(target.getClass());
//		enhancer.setInterfaces(target.getClass().getInterfaces());
		enhancer.setCallback(new CglibMethodInterceptor(target));
		return (T) enhancer.create();
	}

	@SuppressWarnings("unchecked")
	public static <T, O extends T> T createJavassistProxy(O target) {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setInterfaces(target.getClass().getInterfaces());
		Class<?> proxyClass = proxyFactory.createClass();
		javassist.util.proxy.Proxy proxy;
		try {
			proxy = (javassist.util.proxy.Proxy) proxyClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
		proxy.setHandler(new JavassistMethodHandler(target));
		return (T) proxy;
	}

	@SuppressWarnings("unchecked")
	public static <T> T createJavassistBytecodeProxy(T target) {
		ClassPool pool = new ClassPool(true);
		CtClass ctClass = pool.makeClass(target.getClass().getName() + "JavaassistProxy");
		try {
			for (Class<?> clazz : target.getClass().getInterfaces()) {
				ctClass.addInterface(pool.get(clazz.getName()));
			}
		} catch (NotFoundException e) {
			throw new IllegalArgumentException(e);
		}

		Class<?> proxyClass;
		try {
			ctClass.addConstructor(CtNewConstructor.defaultConstructor(ctClass));
			ctClass.addField(CtField.make("public " + target.getClass().getName() + " target;", ctClass));
			ctClass.addMethod(CtNewMethod.make("public void count() { target.count(); }", ctClass));
			proxyClass = ctClass.toClass();
		} catch (CannotCompileException e) {
			throw new IllegalArgumentException(e);
		}

		try {
			T proxy = (T) proxyClass.newInstance();
			proxy.getClass().getField("target").set(proxy, target);
			return proxy;
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static <T> T createAsmProxy(T target) {
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		String targetClassPath = target.getClass().getName().replace('.', '/');
		String className = target.getClass().getName() + "AsmProxy";
		String classPath = className.replace('.', '/');
		
		List<String> interfacePaths = new ArrayList<>();
		for (Class<?> clazz : target.getClass().getInterfaces()) {
			interfacePaths.add(clazz.getName().replace('.', '/'));
		}
		classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, classPath, null, "java/lang/Object", interfacePaths.toArray(new String[0]));

		MethodVisitor initVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		initVisitor.visitCode();
		initVisitor.visitVarInsn(Opcodes.ALOAD, 0);
		initVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		initVisitor.visitInsn(Opcodes.RETURN);
		initVisitor.visitMaxs(0, 0);
		initVisitor.visitEnd();

		FieldVisitor fieldVisitor = classWriter.visitField(Opcodes.ACC_PUBLIC, "target", "L" + targetClassPath + ";", null, null);
		fieldVisitor.visitEnd();

		MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "count", "()V", null, null);
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
		methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classPath, "target", "L" + targetClassPath + ";");
		methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, targetClassPath, "count", "()V");
		methodVisitor.visitInsn(Opcodes.RETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();

		classWriter.visitEnd();
		byte[] code = classWriter.toByteArray();
		try {
			T proxy = (T) new AsmClassLoader().getClass(className, code).newInstance();
			proxy.getClass().getField("target").set(proxy, target);
			return proxy;
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
