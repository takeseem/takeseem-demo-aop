###
* 支持：JDK动态代理、ASM、CGLIB、BCEL、Javassist、AspectJ(暂不考虑)
* 测试基于：[梁飞：动态代理方案性能对比](http://javatar.iteye.com/blog/814426)
* Hibernate
 * org.hibernate.bytecode.javassist.BytecodeProviderImpl
 * org.hibernate.bytecode.cglib.BytecodeProviderImpl  

### 测试结果
* 2017-07-28
 * 创建代理速度：ASM > JDKProxy > Javassist > Javassist(bytecode) > cglib

```
Create No Proxy: 1 ms
Create JDK Proxy: 20 ms
Create CGLIB Proxy: 85 ms
Create JAVAASSIST Proxy: 35 ms
Create JAVAASSIST Bytecode Proxy: 55 ms
Create ASM Proxy: 1 ms
```
 * 调用速度：ASM | Javassist(bytecode) > cglib(基于接口的比基于类快10%) > JDKProxy > Javassist 
 
```
Run NO Proxy: 38 ms, 37,106,984 t/s
Run JDK Proxy: 60 ms, 23,501,090 t/s
Run CGLIB Proxy: 49 ms, 28,776,845 t/s
Run JAVAASSIST Proxy: 127 ms, 11,102,877 t/s
Run JAVAASSIST Bytecode Proxy: 41 ms, 34,391,839 t/s
Run ASM Bytecode Proxy: 41 ms, 34,391,839 t/s
```
* 2017-07-29：增加spring-aop的对比，spring不优化基本是最慢的，优化如果spring(jdk) : opaque=true，cglib: frozen=true
 * 创建代理速度：ASM > JDKProxy > Javassist > Javassist(bytecode) > cglib <===== spring比对应的实现确实慢点

```
Create No Proxy: 0 ms
Create JDK Proxy: 2 ms
Create Spring JDK Proxy: 1 ms
Create CGLIB Proxy: 57 ms
Create Spring interface Proxy: 4 ms class com.sun.proxy.$Proxy22
Create Spring class Proxy: 69 ms class com.takeseem.demo.aop.service.impl.CountServiceImpl$$EnhancerBySpringCGLIB$$8049f30d
Create JAVAASSIST Proxy: 23 ms
Create JAVAASSIST Bytecode Proxy: 34 ms
Create ASM Proxy: 0 ms
```
 * 调用速度：ASM | Javassist(bytecode) > Spring(cglib) > cglib(基于接口的比基于类快10%) > JDKProxy > Javassist > Spring(jdk)  <===== spring比对应的实现确实慢点，但是spring cglib却表现不错
 
```
Run NO Proxy: 38 ms, 37,106,984 t/s
Run JDK Proxy: 79 ms, 17,848,929 t/s
Run Spring JDK Proxy: 91 ms, 15,495,224 t/s
Run CGLIB Proxy: 52 ms, 27,116,642 t/s
Run Spring interface Proxy: 217 ms, 6,497,997 t/s
Run Spring class Proxy: 41 ms, 34,391,839 t/s
Run JAVAASSIST Proxy: 121 ms, 11,653,433 t/s
Run JAVAASSIST Bytecode Proxy: 40 ms, 35,251,635 t/s
Run ASM Bytecode Proxy: 40 ms, 35,251,635 t/s
```