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
package com.github.harbby.gadtry.collection.tuple;

import org.junit.Assert;
import org.junit.Test;

public class Tuple3Test
{
    private final Tuple3<String, Integer, Boolean> tuple = Tuple3.of("k1", 1, true);

    @Test
    public void of()
    {
        Assert.assertEquals(tuple.f1(), "k1");
        Assert.assertEquals(tuple.f2().intValue(), 1);
        Assert.assertEquals(tuple.f3(), true);
    }

    @Test
    public void hashCodeTest()
    {
        tuple.hashCode();
    }

    @Test
    public void toString1()
    {
        Assert.assertEquals(tuple.toString(), "(k1, 1, true)");
    }

    @Test
    public void getArity()
    {
        Assert.assertEquals(tuple.getArity(), 3);
    }

    @Test
    public void getField()
    {
        Assert.assertEquals(tuple.getField(1), "k1");
        Assert.assertEquals(tuple.<Integer>getField(2).intValue(), 1);
        Assert.assertEquals(tuple.<Boolean>getField(3), true);

        try {
            Assert.assertEquals(tuple.getField(tuple.getArity() + 1), "1");
            Assert.fail();
        }
        catch (IndexOutOfBoundsException e) {
            Assert.assertEquals(e.getMessage(), tuple.getArity() + 1 + "");
        }
    }

    @Test
    public void copy()
    {
        Assert.assertEquals(tuple, tuple.copy());
    }
}
