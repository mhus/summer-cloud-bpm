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
import java.util.HashSet;
import java.util.List;

import org.summerclouds.common.core.console.ANSIConsole;
import org.summerclouds.common.core.console.Console;
import org.summerclouds.common.core.console.Console.COLOR;
import org.summerclouds.common.core.tool.MString;

import de.mhus.app.reactive.model.activity.AGateway;
import de.mhus.app.reactive.model.activity.APoint;
import de.mhus.app.reactive.model.activity.AStartPoint;
import de.mhus.app.reactive.model.activity.ATask;
import de.mhus.app.reactive.model.annotations.Trigger;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.engine.EProcess;
import de.mhus.app.reactive.model.util.InactiveStartPoint;

public class ProcessTrace {

    private EProcess process;
    private int width = 40;
    private boolean ansi;
    HashSet<String> done = null;
    HashSet<String> needed = null;

    public ProcessTrace(EProcess process) {
        this.process = process;
        this.ansi = Console.get().isAnsi();
    }

    public void dump(PrintStream out) {
        for (String poolName : process.getPoolNames()) {
            EPool pool = process.getPool(poolName);
            dump(out, pool);
        }
    }

    public void dump(PrintStream out, EPool pool, EElement cur) {

        if (done == null) done = new HashSet<>();
        if (needed == null) needed = new HashSet<>();

        printElement(out, cur);
        out.println("");
        printElementInfo(out, cur, done, needed);

        List<EElement> outputs = pool.getOutputElements(cur);
        while (true) {
            if (outputs.size() == 0) break;
            out.println("                       ||");
            out.println("                       \\/");
            EElement n = null;
            EElement f = null;
            for (EElement o : outputs) {
                out.print("  ");
                printElement(out, o);
                if (o != null && n == null && !done.contains(o.getCanonicalName())) n = o;
                else if (!done.contains(o.getCanonicalName())) needed.add(o.getCanonicalName());
                if (f == null) f = o;
                done.add(o.getCanonicalName());
            }
            out.println();
            printElementInfo(out, f, done, needed);
            if (n == null) break;
            outputs = pool.getOutputElements(n);
        }
    }

    public void dump(PrintStream out, EPool pool) {
        done = new HashSet<>();
        needed = new HashSet<>();
        out.println(MString.rep('=', width));
        out.println("Pool: " + pool.getCanonicalName());
        for (EElement startPoint : pool.getStartPoints(false)) {
            done.add(startPoint.getCanonicalName());
            needed.add(startPoint.getCanonicalName());
            while (needed.size() > 0) {
                String current = needed.iterator().next();
                needed.remove(current);
                EElement cur = pool.getElement(current);

                out.println("--------------------------------------------------------");
                dump(out, pool, cur);
            }
        }
    }

    private void printElement(PrintStream out, EElement cur) {
        if (ansi) out.print(ANSIConsole.ansiForeground(COLOR.RED));
        if (cur.is(APoint.class)) out.print('(');
        if (cur.is(AStartPoint.class) && cur.isInterface(InactiveStartPoint.class)) out.print("!");
        if (cur.is(ATask.class)) out.print('[');
        if (cur.is(AGateway.class)) out.print('<');
        out.print(cur.getCanonicalName());

        if (cur.is(APoint.class)) out.print(')');
        if (cur.is(ATask.class)) out.print(']');
        if (cur.is(AGateway.class)) out.print('>');
        if (ansi) out.print(ANSIConsole.ansiCleanup());
    }

    private void printElementInfo(
            PrintStream out, EElement cur, HashSet<String> done, HashSet<String> needed) {
        out.println("   Lane: " + cur.getSwimlane().getCanonicalName());
        for (Trigger trigger : cur.getTriggers()) {
            out.println(
                    "   Trigger: "
                            + trigger.type()
                            + ": "
                            + trigger.event()
                            + " "
                            + trigger.name()
                            + " -> "
                            + trigger.activity().getCanonicalName());
            if (!done.contains(trigger.activity().getCanonicalName()))
                needed.add(trigger.activity().getCanonicalName());
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
