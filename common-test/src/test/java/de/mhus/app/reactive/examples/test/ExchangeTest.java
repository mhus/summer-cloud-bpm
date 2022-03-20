/**
 * Copyright (C) 2018 Mike Hummel (mh@mhus.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mhus.app.reactive.examples.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.summerclouds.common.core.console.Console;
import org.summerclouds.common.core.console.Console.COLOR;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.tool.MDate;
import org.summerclouds.common.core.tool.MFile;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.engine.Engine;
import de.mhus.app.reactive.engine.EngineConfiguration;
import de.mhus.app.reactive.engine.EngineContext;
import de.mhus.app.reactive.engine.util.DefaultProcessLoader;
import de.mhus.app.reactive.engine.util.EngineListenerUtil;
import de.mhus.app.reactive.engine.util.ExchangeUtil;
import de.mhus.app.reactive.engine.util.JavaPackageProcessProvider;
import de.mhus.app.reactive.examples.simple1.S1Pool;
import de.mhus.app.reactive.examples.simple1.S1Process;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.util.engine.MemoryStorage;

public class ExchangeTest {

    private EngineConfiguration config;
    private Engine engine;
    private Console console;

    @Test
    public void testExchange() throws Exception {

        createEngine();

        String uri =
                "bpm://de.mhus.app.reactive.examples.simple1.S1Process:0.0.1/de.mhus.app.reactive.examples.simple1.S1Pool;customId=554466?text1=area&testDate=1.2.1997&testEnum=ON&testInt=5&testInteger=17";
        System.out.println("URI: " + uri);
        System.out.println(
                "------------------------------------------------------------------------");
        UUID caseId = engine.start(uri);

        {
            // from properties test
            PCase caze = engine.getCaseWithoutLock(caseId);
            EngineContext context = engine.createContext(caze);
            S1Pool pool = (S1Pool) context.getPool();

            assertNotNull(pool);

            System.out.println("Date: " + pool.getTestDate());
            System.out.println("Enum: " + pool.getTestEnum());
            System.out.println("Int : " + pool.getTestInt());
            System.out.println("Integer: " + pool.getTestInteger());

            assertEquals(MDate.toDate("1.2.1997", null), pool.getTestDate());
            assertEquals(S1Pool.TEST_ENUM.ON, pool.getTestEnum());
            assertEquals(5, pool.getTestInt());
            assertEquals(Integer.valueOf(17), pool.getTestInteger());
            assertEquals("554466", caze.getCustomId());
        }

        // export
        System.out.println(">>> Export");
        File dir = new File("target/test/exchange");
        MFile.deleteDir(dir);
        dir.mkdirs();
        ExchangeUtil.exportData(engine, dir);

        // reset engine
        engine = null;
        config = null;
        createEngine();

        // import
        System.out.println(">>> Import");
        ExchangeUtil.importData(engine, dir);

        // test again
        System.out.println(">>> Test");
        {
            PCase caze = engine.getCaseWithoutLock(caseId);
            EngineContext context = engine.createContext(caze);
            S1Pool pool = (S1Pool) context.getPool();

            assertNotNull(pool);

            System.out.println("Date: " + pool.getTestDate());
            System.out.println("Enum: " + pool.getTestEnum());
            System.out.println("Int : " + pool.getTestInt());
            System.out.println("Integer: " + pool.getTestInteger());

            assertEquals(MDate.toDate("1.2.1997", null), pool.getTestDate());
            assertEquals(S1Pool.TEST_ENUM.ON, pool.getTestEnum());
            assertEquals(5, pool.getTestInt());
            assertEquals(Integer.valueOf(17), pool.getTestInteger());
            assertEquals("554466", caze.getCustomId());
        }
    }

    private void createEngine() throws MException, IOException {

        console = Console.get();
        console.setBold(true);
        console.setColor(COLOR.RED, null);
        System.out.println(
                "========================================================================================");
        System.out.println(MSystem.findCallingMethod(3));
        System.out.println(
                "========================================================================================");
        console.cleanup();
        File f = new File("target/classes");
        System.out.println(f.getAbsolutePath());
        DefaultProcessLoader loader = new DefaultProcessLoader(new File[] {f});
        JavaPackageProcessProvider provider = new JavaPackageProcessProvider();
        provider.addProcess(loader, S1Process.class.getPackageName());

        config = new EngineConfiguration();
        config.storage = new MemoryStorage();
        config.archive = new MemoryStorage();
        config.aaa = new SimpleAaaProvider();
        config.parameters = new HashMap<>();
        config.parameters.put(
                "process:de.mhus.app.reactive.examples.simple1.S1Process:versions", "0.0.1");
        config.executeParallel = false;

        config.listener.add(EngineListenerUtil.createAnsiListener());

        config.processProvider = provider;

        engine = new Engine(config);
    }
}
