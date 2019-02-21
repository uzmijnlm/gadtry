/*
 * Copyright (C) 2018 The Harbby Authors
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

import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.github.harbby.gadtry.base.MoreObjects.checkState;
import static com.github.harbby.gadtry.base.Strings.isNotBlank;
import static java.util.Objects.requireNonNull;

public class JVMLaunchers
{
    private JVMLaunchers() {}

    public static class VmBuilder<T extends Serializable>
    {
        private VmCallable<T> callable;
        private boolean depThisJvm = true;
        private Consumer<String> consoleHandler;
        private final List<URL> tmpJars = new ArrayList<>();
        private final List<String> otherVmOps = new ArrayList<>();
        private final Map<String, String> environment = new HashMap<>(System.getenv());
        private ClassLoader classLoader;

        public VmBuilder<T> setCallable(VmCallable<T> callable)
        {
            this.callable = requireNonNull(callable, "callable is null");
            return this;
        }

        public VmBuilder<T> setClassLoader(ClassLoader classLoader)
        {
            this.classLoader = requireNonNull(classLoader, "classLoader is null");
            return this;
        }

        public VmBuilder<T> setConsole(Consumer<String> consoleHandler)
        {
            this.consoleHandler = requireNonNull(consoleHandler, "consoleHandler is null");
            return this;
        }

        public VmBuilder<T> notDepThisJvmClassPath()
        {
            depThisJvm = false;
            return this;
        }

        public VmBuilder<T> addUserURLClassLoader(URLClassLoader vmClassLoader)
        {
            ClassLoader classLoader = vmClassLoader;
            while (classLoader instanceof URLClassLoader) {
                Collections.addAll(tmpJars, ((URLClassLoader) classLoader).getURLs());
                classLoader = classLoader.getParent();
            }
            return this;
        }

        public VmBuilder<T> addUserjars(Collection<URL> jars)
        {
            tmpJars.addAll(jars);
            return this;
        }

        public VmBuilder<T> setXms(String xms)
        {
            otherVmOps.add("-Xms" + xms);
            return this;
        }

        public VmBuilder<T> setXmx(String xmx)
        {
            otherVmOps.add("-Xmx" + xmx);
            return this;
        }

        public VmBuilder<T> setEnvironment(Map<String, String> env)
        {
            this.environment.putAll(requireNonNull(env, "env is null"));
            return this;
        }

        public VmBuilder<T> setEnvironment(String key, String value)
        {
            checkState(isNotBlank(key), "key is null or Empty");
            checkState(isNotBlank(value), "value is null or Empty");
            this.environment.put(key, value);
            return this;
        }

        public JVMLauncher<T> build()
        {
            requireNonNull(consoleHandler, "setConsole(Consumer<String> consoleHandler) not setting");
            return new JVMLauncher<T>(callable, consoleHandler, tmpJars, depThisJvm, otherVmOps, environment, classLoader);
        }
    }

    public static <T extends Serializable> VmBuilder<T> newJvm()
    {
        return new VmBuilder<T>();
    }
}
