/*
 * Copyright (C) 2018 The GadTry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.harbby.gadtry.aop.mock;

import com.github.harbby.gadtry.aop.JoinPoint;
import com.github.harbby.gadtry.aop.aopgo.Pointcut;
import com.github.harbby.gadtry.collection.tuple.Tuple2;
import com.github.harbby.gadtry.function.exception.Consumer;
import com.github.harbby.gadtry.function.exception.Function;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.harbby.gadtry.aop.mock.MockGo.LAST_MOCK_BY_WHEN_METHOD;
import static com.github.harbby.gadtry.base.JavaTypes.getClassInitValue;
import static com.github.harbby.gadtry.base.Throwables.throwsThrowable;
import static java.util.Objects.requireNonNull;

/**
 * AopGo 代理核心，mockMethods选择Method 时间复杂度为 O(1) + log(m)
 * 性能将比AopFactory List(Method Select) O(n) 高效很多。 在方法较多时性能差异将非常显著
 */
public class AopInvocationHandler
        implements InvocationHandler, Externalizable
{
    private final InvocationHandler defaultHandler;
    private InvocationHandler handler;
    private Class<?> proxyClass;   //需要序列化时会用到

    /**
     * 因为 mockMethods对象中Method 不可序列化 导致不能使用常规Serializable方式进行序列化
     */
    private final Map<Method, Function<JoinPoint, Object, Throwable>> mockMethods = new IdentityHashMap<>();

    @Override
    public void writeExternal(ObjectOutput out)
            throws IOException
    {
        if (proxyClass == null) {
            throw new MockGoException("Gadtry aopGo proxy object not be serializable. proxyClass is null");
        }
        out.writeObject(handler);
        out.writeObject(proxyClass);
        //-------------------------------
        List<Consumer<Map<Method, Function<JoinPoint, Object, Throwable>>, Exception>> mockMethodLoader = new ArrayList<>(mockMethods.size());
        for (Map.Entry<Method, Function<JoinPoint, Object, Throwable>> entry : mockMethods.entrySet()) {
            Method method = entry.getKey();
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?> methodClass = method.getDeclaringClass();

            Function<JoinPoint, Object, Throwable> value = entry.getValue();
            mockMethodLoader.add((mockMethods) -> {
                Pointcut pointcut = Collections::emptyList;
                Map<Method, Method> methods = pointcut.filter(this.proxyClass).stream()
                        .collect(Collectors.toMap(k -> k, v -> v));
                Method findMethod = methodClass.getDeclaredMethod(methodName, parameterTypes);
                mockMethods.put(methods.get(findMethod), value);
            });
        }
        out.writeObject(mockMethodLoader);
    }

    @Override
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException
    {
        this.handler = (InvocationHandler) in.readObject();
        this.proxyClass = (Class<?>) in.readObject();
        if (proxyClass == null) {
            throw new MockGoException("Gadtry aopGo proxy object serializable failed. proxyClass is null");
        }
        //--------------------------------
        List<Consumer<Map<Method, Function<JoinPoint, Object, Throwable>>, Exception>> mockMethodLoader = (List<Consumer<Map<Method, Function<JoinPoint, Object, Throwable>>, Exception>>) in.readObject();
        for (Consumer<Map<Method, Function<JoinPoint, Object, Throwable>>, Exception> consumer : mockMethodLoader) {
            try {
                consumer.apply(mockMethods);
            }
            catch (Exception e) {
                throwsThrowable(e);
            }
        }
    }

    public AopInvocationHandler(Object target)
    {
        requireNonNull(target, "instance is null");
        this.defaultHandler = (InvocationHandler & Serializable) (proxy, method, args) -> {
            boolean v2 = method.getDeclaringClass() == proxy.getClass();
            Object instance = v2 ? proxy : target;

            Function<JoinPoint, Object, Throwable> userCode = mockMethods.get(method);
            if (userCode != null) {
                JoinPoint joinPoint = JoinPoint.of(instance, method, args);
                return userCode.apply(joinPoint);
            }
            else {
                return method.invoke(instance, args);
            }
        };
        this.initHandler();
    }

    public AopInvocationHandler()
    {
        this.defaultHandler = (InvocationHandler & Serializable) (proxy, method, args) -> {
            Function<JoinPoint, Object, Throwable> userCode = mockMethods.get(method);
            if (userCode != null) {
                return userCode.apply(JoinPoint.of(method, args));
            }
            else {
                return getClassInitValue(method.getReturnType());
            }
        };
        this.initHandler();
    }

    public void setProxyClass(Class<?> proxyClass)
    {
        //如果需要序列化代理对象，则必须传入proxyClass代理类
        this.proxyClass = proxyClass;
    }

    public void setHandler(InvocationHandler handler)
    {
        this.handler = handler;
    }

    public void initHandler()
    {
        this.handler = requireNonNull(defaultHandler, "defaultHandler is null");
    }

    /**
     * WhenThen register
     */
    public void register(Method method, Function<JoinPoint, Object, Throwable> advice)
    {
        mockMethods.put(method, advice);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
    {
        LAST_MOCK_BY_WHEN_METHOD.set(Tuple2.of(proxy, method));
        try {
            //method.setAccessible(true);  //todo: 如果未来需要默认开启所有访问权限时。设置此处为 true 即可
            return this.handler.invoke(proxy, method, args);
        }
        catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
