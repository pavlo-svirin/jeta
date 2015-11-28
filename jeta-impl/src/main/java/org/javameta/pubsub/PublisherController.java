/*
 * Copyright 2015 Oleg Khalidov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.javameta.pubsub;

import org.javameta.MasterController;
import org.javameta.metasitory.Metasitory;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */
public class PublisherController<M> extends MasterController<M, PublisherMetacode<M>> {

    public PublisherController(Metasitory metasitory, M master) {
        super(metasitory, master);
    }

    public void createPublisher() {
        for (PublisherMetacode<M> observable : metacodes)
            observable.applyPublisher(master);
    }
}
