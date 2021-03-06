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
package com.github.harbby.gadtry.jvm;

import com.github.harbby.gadtry.collection.mutable.MutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JVMLaunchersTest
{
    @Test
    public void testForkJvmReturn1()
    {
        System.out.println("--- vm test ---");
        JVMLauncher<Integer> launcher = JVMLaunchers.<Integer>newJvm()
                .setCallable(() -> {
                    //TimeUnit.SECONDS.sleep(1000000);
                    System.out.println("************ job start ***************");
                    return 1;
                })
                .addUserjars(Collections.emptyList())
                .setXms("16m")
                .setXmx("16m")
                .setConsole((msg) -> System.out.println(msg))
                .build();

        try {
            VmFuture<Integer> out = launcher.startAsync();
            System.out.println(out.getPid());
            Assert.assertEquals(out.get().intValue(), 1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testForkJvmThrowRuntimeException123()
            throws InterruptedException, ExecutionException
    {
        String f = "testForkJvmThrowRuntimeException123";
        System.out.println("--- vm test ---");
        JVMLauncher<Integer> launcher = JVMLaunchers.<Integer>newJvm()
                .setCallable(() -> {
                    System.out.println("************ job start ***************");
                    throw new RuntimeException(f);
                })
                .addUserjars(Collections.emptyList())
                .addUserURLClassLoader((URLClassLoader) this.getClass().getClassLoader())
                .setClassLoader(this.getClass().getClassLoader())
                .setXms("16m")
                .setXmx("16m")
                .setConsole(System.out::println)
                .notDepThisJvmClassPath()
                .build();

        try {
            VmFuture<Integer> out = launcher.startAsync();
            out.get();
        }
        catch (JVMException e) {
            Assert.assertTrue(e.getMessage().contains(f));
            e.printStackTrace();
        }
    }

    @Test
    public void testForkJvmEnv()
            throws JVMException
    {
        String envValue = "value_007";
        JVMLauncher<String> launcher = JVMLaunchers.<String>newJvm()
                .setCallable(() -> {
                    //TimeUnit.SECONDS.sleep(1000000);
                    System.out.println("************ job start ***************");
                    String env = System.getenv("TestEnv");
                    Assert.assertEquals(env, envValue);
                    Assert.assertEquals(System.getenv("k1"), "v1");
                    return env;
                })
                .setEnvironment("TestEnv", envValue)
                .setEnvironment(MutableMap.of("k1", "v1"))
                .addUserjars(Collections.emptyList())
                .setXms("16m")
                .setXmx("16m")
                .setConsole((msg) -> System.out.println(msg))
                .build();

        String out = launcher.startAndGet();
        Assert.assertEquals(out, envValue);
    }

    @Test
    public void actorModelTest1()
            throws InterruptedException
    {
        String f = "testForkJvmThrowRuntimeException123";
        JVMLauncher<Integer> launcher = JVMLaunchers.<Integer>newJvm()
                .setCallable(() -> {
                    System.out.println("************ job start ***************");
                    throw new RuntimeException(f);
                })
                .addUserjars(Collections.emptyList())
                .setXms("16m")
                .setXmx("16m")
                .setConsole(System.out::println)
                .build();

        // 使用如下方式 对actor模型 进行测试
        final Object lock = new Object();
        synchronized (lock) {
            CompletableFuture.runAsync(launcher::startAndGet).whenComplete((value, error) -> {
                Assert.assertTrue(error.getMessage().contains(f));
                error.printStackTrace();
                synchronized (lock) {
                    lock.notify();   //唤醒主线程
                }
            });
            lock.wait(500_000); //开始睡眠并让出锁
        }
    }

    @Test
    public void testActorModelForkReturn2019()
            throws JVMException, InterruptedException
    {
        JVMLauncher<Integer> launcher = JVMLaunchers.<Integer>newJvm()
                .setCallable(() -> {
                    System.out.println("************ job start ***************");
                    return 2019;
                })
                .addUserjars(Collections.emptyList())
                .setXms("16m")
                .setXmx("16m")
                .setConsole(System.out::println)
                .build();

        // 使用如下方式 对actor模型 进行测试
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        lock.lock();

        CompletableFuture.supplyAsync(launcher::startAndGet)
                .whenComplete((value, error) -> {
                    Assert.assertEquals(2019, value.intValue());
                    System.out.println(value);
                    lock.lock();
                    condition.signal();  //唤醒睡眠的主线程
                    lock.unlock();
                });

        // LockSupport.class
        condition.await(600, TimeUnit.SECONDS); //睡眠进入等待池并让出锁
        lock.unlock();
    }

    @Test
    public void getLatestUserDefinedLoader()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Class<?> class1 = java.io.ObjectInputStream.class;
        Method method = class1.getDeclaredMethod("latestUserDefinedLoader");
        method.setAccessible(true);  //必须要加这个才能
        Object a1 = method.invoke(null);
        Assert.assertTrue(a1 instanceof ClassLoader);
    }

    @Test
    public void workDirTest()
            throws JVMException
    {
        File dir = new File("/tmp");
        JVMLauncher<String> launcher = JVMLaunchers.<String>newJvm()
                .addUserjars(Collections.emptyList())
                .setXms("16m")
                .setXmx("16m")
                .setWorkDirectory(dir)
                .setConsole(System.out::println)
                .build();

        String jvmWorkDir = launcher.startAndGet(() -> {
            return System.getProperty("user.dir");
        });

        Assert.assertArrayEquals(dir.list(), new File(jvmWorkDir).list());
    }
}
