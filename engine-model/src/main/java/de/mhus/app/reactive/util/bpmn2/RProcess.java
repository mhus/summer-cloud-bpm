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
package de.mhus.app.reactive.util.bpmn2;

import java.util.Date;
import java.util.jar.Manifest;

import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.tool.MCast;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.activity.AProcess;

public class RProcess extends MLog implements AProcess {

    @Override
    public String getVersionInformation() {
        try {
            Manifest manifest = MSystem.getManifest(getClass());
            String bundleVersion = manifest.getMainAttributes().getValue("Bundle-Version");
            String bundleSymbolicName =
                    manifest.getMainAttributes().getValue("Bundle-SymbolicName");
            long bndLastModified =
                    MCast.tolong(manifest.getMainAttributes().getValue("Bnd-LastModified"), 0);
            return bundleSymbolicName
                    + ":"
                    + bundleVersion
                    + (bndLastModified <= 0 ? "" : " (" + new Date(bndLastModified) + ")");
        } catch (NotFoundException e) {
        }
        return null;
    }

    @Override
    public long getBuildTime() {
        try {
            Manifest manifest = MSystem.getManifest(getClass());
            long bndLastModified =
                    MCast.tolong(manifest.getMainAttributes().getValue("Bnd-LastModified"), 0);
            return bndLastModified;
        } catch (NotFoundException e) {
        }
        return 0;
    }
}
