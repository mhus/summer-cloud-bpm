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

import de.mhus.app.reactive.engine.util.DefaultProcessLoader;
import de.mhus.app.reactive.engine.util.JavaPackageProcessProvider;
import de.mhus.app.reactive.examples.simple1.S1Process;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.engine.EProcess;
import de.mhus.app.reactive.util.designer.DesignerUtil;
import de.mhus.app.reactive.util.designer.XmlModel;

public class S1DesignerTest {

    @Test
    public void testCreateDesign() throws Exception {
        File f = new File("target/classes");
        System.out.println(f.getAbsolutePath());
        DefaultProcessLoader loader = new DefaultProcessLoader(new File[] {f});
        JavaPackageProcessProvider provider = new JavaPackageProcessProvider();
        provider.addProcess(loader, S1Process.class.getPackageName());

        XmlModel model = new XmlModel();
        EProcess process =
                provider.getProcess("de.mhus.app.reactive.examples.simple1.S1Process", "0.0.1");
        EPool pool = process.getPool("de.mhus.app.reactive.examples.simple1.S1Pool");
        model.merge(process, pool);

        model.dump();

        DesignerUtil.createDocument(model, new File("target/S1Pool-1.bpmn2"));
        DesignerUtil.createDocument(model, new File("target/S1Pool-3.bpmn2"));

        // try loading again
        model = new XmlModel();
        DesignerUtil.load(model, new File("target/S1Pool-1.bpmn2"));
        // save again
        DesignerUtil.createDocument(model, new File("target/S1Pool-2.bpmn2"));

        // merge in existing
        DesignerUtil.saveInto(model, new File("target/S1Pool-3.bpmn2"));

        // merge in existing
        DesignerUtil.saveInto(model, new File("target/S1Pool-4.bpmn2"));
    }
}
