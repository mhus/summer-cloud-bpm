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
package de.mhus.app.reactive.examples.simple1.sub;

import de.mhus.app.reactive.examples.simple1.S1Lane1;
import de.mhus.app.reactive.examples.simple1.S1Pool;
import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.util.InactiveStartPoint;
import de.mhus.app.reactive.util.bpmn2.RStartPoint;

@ActivityDescription(
        outputs = @Output(activity = S1StepSetSpock.class),
        lane = S1Lane1.class,
        displayName = "Extra Start Point Setting text2 to spock",
        description = "Another not automatically executed startpoint")
public class S1StartSpock extends RStartPoint<S1Pool> implements InactiveStartPoint {}
