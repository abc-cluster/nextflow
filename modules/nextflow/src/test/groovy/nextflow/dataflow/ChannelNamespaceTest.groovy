/*
 * Copyright 2013-2026, Seqera Labs
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

package nextflow.dataflow

import java.util.concurrent.TimeUnit

import nextflow.dataflow.ChannelNamespace as channel
import nextflow.extension.CH
import spock.lang.Specification
import spock.lang.Timeout

import static test.ScriptHelper.runDataflow
/**
 *
 * @author Ben Sherman <bentshermann@gmail.com>
 */
@Timeout(value = 1, unit = TimeUnit.MINUTES)
class ChannelNamespaceTest extends Specification {

    def 'should create a channel of values' () {
        when:
        def result = runDataflow {
            channel.of('a')
        }
        then:
        result.val == 'a'
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of('a','b','c')
        }
        then:
        result.val == 'a'
        result.val == 'b'
        result.val == 'c'
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of([1,2,3])
        }
        then:
        result.val == [1,2,3]
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of([1,2], [3,4])
        }
        then:
        result.val == [1,2]
        result.val == [3,4]
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of([])
        }
        then:
        result.val == []
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of()
        }
        then:
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of([1,2,3].toArray())
        }
        then:
        result.val == 1
        result.val == 2
        result.val == 3
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of([].toArray())
        }
        then:
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of(null)
        }
        then:
        result.val == null
        result.val == CH.stop()
    }

    def 'should create a channel from a range' () {
        when:
        def result = runDataflow {
            channel.of(1..3)
        }
        then:
        result.val == 1
        result.val == 2
        result.val == 3
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of(1..3,'X','Y')
        }
        then:
        result.val == 1
        result.val == 2
        result.val == 3
        result.val == 'X'
        result.val == 'Y'
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.of(1..3,'X'..'Y')
        }
        then:
        result.val == 1
        result.val == 2
        result.val == 3
        result.val == 'X'
        result.val == 'Y'
        result.val == CH.stop()
    }

    def 'should create channel from a list'() {
        when:
        def result = runDataflow {
            channel.fromList(['alpha','delta'])
        }
        then:
        result.val == 'alpha'
        result.val == 'delta'
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.fromList([])
        }
        then:
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.fromList(null)
        }
        then:
        result.val == CH.stop()

        when:
        result = runDataflow {
            channel.fromList([1..3, 'X'..'Y'])
        }
        then:
        result.val == 1..3
        result.val == 'X'..'Y'
        result.val == CH.stop()
    }

}
