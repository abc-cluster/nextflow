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

package nextflow.script

import java.nio.file.Files

import nextflow.extension.OpCall
import test.Dsl2Spec

import static test.ScriptHelper.*

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class WorkflowBindingTest extends Dsl2Spec {

    def setupSpec() {
        WorkflowBinding.init()
    }

    def 'should lookup variables' () {
        given:
        def obj1 = new Object()
        def obj2 = new Object()
        def binding = new WorkflowBinding()

        when:
        binding.foo = obj1
        binding.bar = obj2
        then:
        WorkflowBinding.lookup(obj1) == 'foo'
        WorkflowBinding.lookup(obj2) == 'bar'
    }

    def 'should invoke an extension method' () {

        given:
        def FOO = Mock(ComponentDef)
        def ARGS = ['alpha','beta'] as Object[]
        def binding = Spy(WorkflowBinding)
        binding.@meta = Mock(ScriptMeta)

        // should invoke an extension component
        when:
        def result = binding.invokeMethod('foo', ARGS)
        then:
        1 * binding.getComponent0('foo') >> FOO
        1 * FOO.invoke_a(ARGS) >> 'Hello'
        result == 'Hello'

        // should invoke an extension operator
        when:
        result = binding.invokeMethod('map', ARGS)
        then:
        1 * binding.getComponent0('map') >> null
        result instanceof OpCall
        (result as OpCall).methodName == 'map'
        (result as OpCall).args == ARGS

        // should throw missing method exception
        when:
        binding.invokeMethod('foo', ARGS)
        then:
        1 * binding.getComponent0('foo') >> null
        thrown(MissingMethodException)

    }

    def 'should get an extension method as variable' () {

        given:
        def FOO = Mock(ComponentDef)
        def binding = Spy(WorkflowBinding)
        binding.setVariable('alpha', 'Hello')
        binding.@meta = Mock(ScriptMeta)

        // should return an existing variable
        when:
        def result = binding.getVariable('alpha')
        then:
        0 * binding.getComponent0('alpha') >> null
        result == 'Hello'

        when:
        result = binding.getVariable('foo')
        then:
        1 * binding.getComponent0('foo') >> FOO
        result == FOO

        when:
        result = binding.getVariable('map')
        then:
        1 * binding.getComponent0('map') >> null
        result instanceof OpCall
        (result as OpCall).methodName == 'map'
        (result as OpCall).args == [] as Object[]

    }

    def 'should allow typed workflow to call legacy process/workflow' () {
        given:
        def folder = Files.createTempDirectory('test')

        folder.resolve('main.nf').text = '''
            nextflow.preview.types = true

            include { foo ; bar } from './module.nf'

            workflow {
                ch = channel.of(1, 2, 3)
                bar(foo(ch))
            }
            '''

        folder.resolve('module.nf').text = '''
            process foo {
                input:
                val x

                output:
                val y

                exec:
                y = x * 2
            }

            workflow bar {
                take:
                ys

                emit:
                ys.map { y -> y % 3 }
            }
            '''

        when:
        runScript(folder.resolve('main.nf'))
        then:
        noExceptionThrown()

        cleanup:
        folder?.deleteDir()
    }

    def 'should allow legacy workflow to call typed process/workflow' () {
        given:
        def folder = Files.createTempDirectory('test')

        folder.resolve('main.nf').text = '''
            include { foo ; bar } from './module.nf'

            workflow {
                ch = channel.of(1, 2, 3)
                bar(foo(ch))
            }
            '''

        folder.resolve('module.nf').text = '''
            nextflow.preview.types = true

            process foo {
                input:
                x: Integer

                output:
                y

                exec:
                y = x * 2
            }

            workflow bar {
                take:
                ys: Channel<Integer>

                emit:
                ys.map { y -> y * 2 }
            }
            '''

        when:
        runScript(folder.resolve('main.nf'))
        then:
        noExceptionThrown()

        cleanup:
        folder?.deleteDir()
    }

}
