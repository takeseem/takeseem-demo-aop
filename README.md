###
* 支持：JDK动态代理、ASM、CGLIB、BCEL、Javassist、AspectJ(暂不考虑)
* 测试基于：[梁飞：动态代理方案性能对比](http://javatar.iteye.com/blog/814426)
* Hibernate
 * org.hibernate.bytecode.javassist.BytecodeProviderImpl
 * org.hibernate.bytecode.cglib.BytecodeProviderImpl  

### 测试结果
#### 结论
* jdk32bit-server细微快于其他
* jdk32bit-client时非字节码增强模式性能极差
* 其他的jdk组合性能基本相同

### 测试case
* 2017-07-30：增加32和64位jvm的比较，32位-client模式的非字节码增强性能急剧下降

```
64bit -server: 
* lambda和原生相差不大
* 字节码增强最快：(ASM | Javassist(bytecode)) > Spring(cglib) > cglib
* 代理模式：jdk自带的性能也不错，jdk-proxy > Javassist(proxy) > Spring(jdk-proxy)慢太多
Run NO Proxy: 37 ms, 38,109,875 t/s
Run lambda(static): 38 ms, 37,106,984 t/s
Run lambda(this): 37 ms, 38,109,875 t/s
Run JDK Proxy: 80 ms, 17,625,817 t/s
Run CGLIB Proxy: 52 ms, 27,116,642 t/s
Run Spring interface Proxy: 212 ms, 6,651,251 t/s
Run Spring class Proxy: 41 ms, 34,391,839 t/s
Run JAVAASSIST Proxy: 102 ms, 13,824,170 t/s
Run JAVAASSIST Bytecode Proxy: 40 ms, 35,251,635 t/s
Run ASM Bytecode Proxy: 40 ms, 35,251,635 t/s

64bit -client：与-server无差别
Run NO Proxy: 37 ms, 38,109,875 t/s
Run lambda(static): 37 ms, 38,109,875 t/s
Run lambda(this): 38 ms, 37,106,984 t/s
Run JDK Proxy: 79 ms, 17,848,929 t/s
Run CGLIB Proxy: 54 ms, 26,112,322 t/s
Run Spring interface Proxy: 217 ms, 6,497,997 t/s
Run Spring class Proxy: 40 ms, 35,251,635 t/s
Run JAVAASSIST Proxy: 110 ms, 12,818,776 t/s
Run JAVAASSIST Bytecode Proxy: 40 ms, 35,251,635 t/s
Run ASM Bytecode Proxy: 39 ms, 36,155,523 t/s


32bit -server：与64bit差别不大，快得不明显
Run NO Proxy: 34 ms, 41,472,512 t/s
Run lambda(static): 37 ms, 38,109,875 t/s
Run lambda(this): 38 ms, 37,106,984 t/s
Run JDK Proxy: 75 ms, 18,800,872 t/s
Run CGLIB Proxy: 50 ms, 28,201,308 t/s
Run Spring interface Proxy: 214 ms, 6,589,090 t/s
Run Spring class Proxy: 38 ms, 37,106,984 t/s
Run JAVAASSIST Proxy: 122 ms, 11,557,913 t/s
Run JAVAASSIST Bytecode Proxy: 37 ms, 38,109,875 t/s
Run ASM Bytecode Proxy: 37 ms, 38,109,875 t/s

32bit -client：
* 总体来说都要慢，特别是非字节码增强的
* jdk-proxy是其他的6倍，cglib是原来的3倍，Spring
Run NO Proxy: 38 ms, 37,106,984 t/s
Run lambda(static): 37 ms, 38,109,875 t/s
Run lambda(this): 41 ms, 34,391,839 t/s
Run JDK Proxy: 505 ms, 2,792,208 t/s
Run CGLIB Proxy: 147 ms, 9,592,281 t/s
Run Spring interface Proxy: 1367 ms, 1,031,503 t/s
Run Spring class Proxy: 84 ms, 16,786,492 t/s
Run JAVAASSIST Proxy: 553 ms, 2,549,847 t/s
Run JAVAASSIST Bytecode Proxy: 55 ms, 25,637,552 t/s
Run ASM Bytecode Proxy: 53 ms, 26,605,007 t/s
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