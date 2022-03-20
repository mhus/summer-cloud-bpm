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
package de.mhus.app.reactive.examples.simple1;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.util.old.RServiceTaskInternal;

@ActivityDescription(outputs = @Output(activity = S1TheEnd.class), lane = S1Lane1.class)
public class S1Step2 extends RServiceTaskInternal<S1Pool> {

    @Override
    public Class<? extends AActivity<S1Pool>> doExecuteInternal() {
        return S1TheEnd.class;
    }

    //	@Override
    //	public HumanForm createForm() {
    //		return new HumanForm().add(
    //			new FmText(S1Pool::getText1, "Text1", ""),
    //			new FmText(S1Pool::getText1, "Text1", "",new FmColumns(2))
    //		).add(
    //			new FmText(S1Pool::getText1, "Text1", ""),
    //			new FmText(S1Pool::getText1, "Text1", ""),
    //			new FmText(S1Pool::getText1, "Text1", "")
    //		);
    //	}

}
