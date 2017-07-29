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
package com.takeseem.demo.aop.model;

/**
 * @author <a href="mailto:yh@takeseem.com">杨浩</a>
 */
public class AsmClassLoader extends ClassLoader {
	
	public AsmClassLoader() {
		super(Thread.currentThread().getContextClassLoader());
	}

	public synchronized Class<?> getClass(String name, byte[] code) {
		if (name == null) throw new IllegalArgumentException("name=" + name);
		return defineClass(name, code, 0, code.length);
	}
}
