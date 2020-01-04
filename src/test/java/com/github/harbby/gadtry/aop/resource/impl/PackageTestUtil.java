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
package com.github.harbby.gadtry.aop.resource.impl;

import com.github.harbby.gadtry.aop.resource.PackageTestName;

public class PackageTestUtil
{
    private PackageTestUtil() {}

    public static int getAge(PackageTestName instance)
    {
        if (instance instanceof PackageTestNameAge) {
            return ((PackageTestNameAge) instance).getAge();
        }
        throw new RuntimeException("instance not instanceof PackageTestNameAge");
    }

    public static PackageTestName getInstance()
    {
        return new PackageTestImpl();
    }
}
