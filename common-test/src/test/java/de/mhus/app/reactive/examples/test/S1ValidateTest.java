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

import java.io.File;

import org.junit.jupiter.api.Test;
import org.summerclouds.common.core.error.MException;

import de.mhus.app.reactive.engine.util.DefaultProcessLoader;
import de.mhus.app.reactive.engine.util.JavaPackageProcessProvider;
import de.mhus.app.reactive.engine.util.PoolValidator;
import de.mhus.app.reactive.engine.util.ProcessTrace;
import de.mhus.app.reactive.examples.simple1.S1Process;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.engine.EProcess;

public class S1ValidateTest {

    @Test
    public void testValidate() throws MException {
        File f = new File("target/classes");
        System.out.println(f.getAbsolutePath());
        DefaultProcessLoader loader = new DefaultProcessLoader(new File[] {f});
        JavaPackageProcessProvider provider = new JavaPackageProcessProvider();
        provider.addProcess(loader, S1Process.class.getPackageName());

        for (String processName : provider.getProcessNames()) {
            System.out.println(">>> Process: " + processName);
            EProcess process = provider.getProcess(processName);
            for (String poolName : process.getPoolNames()) {
                System.out.println("   >>> Pool: " + poolName);
                EPool pool = process.getPool(poolName);
                PoolValidator validator = new PoolValidator(pool);
                validator.validate();
                for (PoolValidator.Finding finding : validator.getFindings()) {
                    System.out.println("   *** " + finding);
                }
            }
            ProcessTrace dump = new ProcessTrace(process);
            dump.dump(System.out);
        }
    }
}
