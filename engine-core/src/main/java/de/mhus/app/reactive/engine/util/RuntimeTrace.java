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
package de.mhus.app.reactive.engine.util;

import java.io.PrintStream;
import java.util.UUID;

import org.summerclouds.common.core.console.ANSIConsole;
import org.summerclouds.common.core.console.Console;
import org.summerclouds.common.core.console.Console.COLOR;

import de.mhus.app.reactive.model.engine.EngineMessage;
import de.mhus.app.reactive.model.engine.RuntimeNode;

public class RuntimeTrace {

    private RuntimeNode runtime;
    private boolean ansi;
    private int width = 40;

    public RuntimeTrace(RuntimeNode runtime) {
        this.runtime = runtime;
        this.ansi = Console.get().isAnsi();
        trace();
    }

    private void trace() {
        // TODO create model
    }

    public void dumpMessages(PrintStream out) {
        for (EngineMessage msg : runtime.getMessages()) {
            switch (msg.getType()) {
                case CONNECT:
                    if (ansi) out.print(ANSIConsole.ansiForeground(COLOR.BLUE));
                    out.print("+++ ");
                    break;
                case ERROR:
                    if (ansi) out.print(ANSIConsole.ansiForeground(COLOR.RED));
                    out.print("*** ");
                    break;
                case DEBUG:
                    if (ansi) out.print(ANSIConsole.ansiForeground(COLOR.YELLOW));
                    out.print("--- ");
                    break;
                case START:
                    if (ansi) out.print(ANSIConsole.ansiForeground(COLOR.BLUE));
                    out.print("+++ ");
                    break;
                case FLOW:
                    out.print("--- ");
                    break;
                case OTHER:
                default:
                    out.print("    ");
                    break;
            }
            out.println(msg);
            if (ansi) out.print(ANSIConsole.ansiCleanup());
        }
    }

    public void dumpTrace(PrintStream out, UUID nodeId) {
        for (EngineMessage msg : runtime.getMessages()) {
            if (nodeId.equals(msg.getFromNode()) || nodeId.equals(msg.getToNode()))
                out.println(msg);
        }
    }

    public boolean isAnsi() {
        return ansi;
    }

    public void setAnsi(boolean ansi) {
        this.ansi = ansi;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
