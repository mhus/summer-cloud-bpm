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
package de.mhus.app.reactive.model.util;

import java.util.UUID;

import org.summerclouds.common.core.M;
import org.summerclouds.common.core.cfg.CfgLong;
import org.summerclouds.common.core.concurrent.Lock;
import org.summerclouds.common.core.concurrent.LockManager;
import org.summerclouds.common.core.error.TimeoutException;
import org.summerclouds.common.core.tool.MPeriod;

import de.mhus.app.reactive.model.engine.CaseLockProvider;

public class LocalCaseLockProvider implements CaseLockProvider {

    private static CfgLong CFG_TIMEOUT =
            new CfgLong(
                    LocalCaseLockProvider.class, "timeout", MPeriod.MINUTE_IN_MILLISECONDS * 5);

    @Override
    public boolean isCaseLocked(UUID caseId) {
        return M.l(LockManager.class)
                .getLock(getClass().getCanonicalName() + "_" + caseId)
                .isLocked();
    }

    @Override
    public Lock lock(UUID caseId) throws TimeoutException {
        return M.l(LockManager.class)
                .getLock(getClass().getCanonicalName() + "_" + caseId)
                .lockWithException(CFG_TIMEOUT.value());
    }

    @Override
    public Lock acquireCleanupMaster() {
        return new DummyLock();
    }

    @Override
    public Lock acquirePrepareMaster() {
        return new DummyLock();
    }

    @Override
    public Lock lockOrNull(UUID caseId) {
        try {
            return M.l(LockManager.class)
                    .getLock(getClass().getCanonicalName() + "_" + caseId)
                    .lockWithException(10);
        } catch (TimeoutException e) {
        }
        return null;
    }

    @Override
    public Lock acquireEngineMaster() {
        return M.l(LockManager.class).getLock(getClass().getCanonicalName() + ":engine").lock();
    }

    //    @Override
    //    public void releaseEngineMaster() {
    //        M.l(LockManager.class).getLock(getClass().getCanonicalName() +
    // ":engine").unlockHard();
    //    }

    @Override
    public boolean isReady() {
        return true;
    }

    private class DummyLock implements Lock {

        @Override
        public Lock lock() {
            return this;
        }

        @Override
        public boolean lock(long timeout) {
            return true;
        }

        @Override
        public boolean unlock() {
            return true;
        }

        @Override
        public void unlockHard() {}

        @Override
        public boolean isLocked() {
            return true;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getOwner() {
            return null;
        }

        @Override
        public long getLockTime() {
            return 0;
        }

        @Override
        public boolean refresh() {
            return true;
        }

        @Override
        public long getCnt() {
            return 0;
        }

        @Override
        public String getStartStackTrace() {
            return null;
        }
    }
}
