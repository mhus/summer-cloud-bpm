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
package de.mhus.app.reactive.model.engine;

import java.util.UUID;

import org.summerclouds.common.core.concurrent.Lock;
import org.summerclouds.common.core.error.TimeoutException;

public interface CaseLockProvider {

    boolean isCaseLocked(UUID caseId);

    /**
     * Return the lock if already locked. This must be an atomic operation.
     *
     * @param caseId
     * @return The acquired lock.
     * @throws TimeoutException Thrown if it was not possible to acquire the lock.
     */
    Lock lock(UUID caseId) throws TimeoutException;

    /**
     * return true if the clean up master was acquired until the date in ms.
     *
     * @return The lock or null
     */
    Lock acquireCleanupMaster();

    /**
     * return true if the prepare master was acquired until the date in ms.
     *
     * @return The lock or null
     */
    Lock acquirePrepareMaster();

    /**
     * acquired the engine lock
     *
     * @return The lock or null
     */
    Lock acquireEngineMaster();

    /**
     * Return the lock or null if already locked. This must be an atomic operation.
     *
     * @param caseId
     * @return The acquired lock.
     */
    Lock lockOrNull(UUID caseId);

    /**
     * Return true if the lock engine is ready to lock. If not the engine will wait until it's
     * ready.
     *
     * @return true if locking is possible
     */
    boolean isReady();
}
