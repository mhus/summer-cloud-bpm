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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.summerclouds.common.core.console.Console;
import org.summerclouds.common.core.console.Console.COLOR;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.engine.Engine;
import de.mhus.app.reactive.engine.EngineConfiguration;
import de.mhus.app.reactive.engine.util.DefaultProcessLoader;
import de.mhus.app.reactive.engine.util.EngineListenerUtil;
import de.mhus.app.reactive.engine.util.JavaPackageProcessProvider;
import de.mhus.app.reactive.examples.simple1.S1Process;
import de.mhus.app.reactive.examples.simple1.area.S2DoSomething;
import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.engine.PCaseInfo;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.util.engine.MemoryStorage;

public class LockTest {

    private EngineConfiguration config;
    private Engine engine;
    long sleep = 10;
    private Console console;

    @Test
    public void testLock() throws Exception {
        createEngine();

        String uri =
                "bpm://de.mhus.app.reactive.examples.simple1.S1Process:0.0.1/de.mhus.app.reactive.examples.simple1.S1Pool?text1=area";
        System.out.println("URI: " + uri);

        for (int i = 0; i < 20; i++) engine.start(uri);

        while (true) {
            S2DoSomething.sleepTime = 200;
            engine.step();
            int cntActiveCases = 0;
            for (@SuppressWarnings("unused")
            PCaseInfo info : engine.storageGetCases(STATE_CASE.RUNNING)) cntActiveCases++;
            if (cntActiveCases == 0) break;
            int cntDoSomething = 0;
            int cntActiveNodes = 0;
            console.setColor(COLOR.BLUE, null);
            for (PNodeInfo info : engine.storageGetFlowNodes(null, null)) {
                if (info.getState() != STATE_NODE.CLOSED) {
                    if (info.getCanonicalName()
                                    .equals(
                                            "de.mhus.app.reactive.examples.simple1.lock.S2DoSomething")
                            && info.getState() == STATE_NODE.RUNNING) cntDoSomething++;
                    console.println("### " + info);
                    cntActiveNodes++;
                }
            }
            console.println("# Active Cases: " + cntActiveCases);
            console.println("# Active Nodes: " + cntActiveNodes);
            console.println("# DoSomething : " + cntDoSomething);
            console.println("# Rounds: " + engine.getStatisticRounds());
            console.println();
            console.cleanup();
            // Thread.sleep(2000);

            assertFalse(S2DoSomething.failed);
            assertFalse(cntDoSomething > 1);
            assertTrue(engine.getStatisticRounds() < 65);
        }
    }

    private void createEngine() throws MException, IOException {

        console = Console.get();
        console.setColor(COLOR.RED, null);
        System.out.println(
                "========================================================================================");
        System.out.println(MSystem.findCallerMethod(3));
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
        config.executeParallel = true;
        config.sleepBetweenProgress = 0;
        config.listener.add(EngineListenerUtil.createAnsiListener());

        config.processProvider = provider;

        engine = new Engine(config);
    }
}
