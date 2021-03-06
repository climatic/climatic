/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.climatic.todo

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import org.climatic.TaskApp

public class TodoApp {

    private TodoApp(String... cliArgs) {
        def initialConfig = new ConfigObject()
        initialConfig.version = '1.0'
        new TaskApp('todo-list').configure {

            configureCliBuilder { cliBuilder ->
                cliBuilder.with {
                    v longOpt: 'version', 'show version'
                    t longOpt: 'todo-home', args: 1, argName: 'dir', 'todo list directory'
                }
            }

            handleOptions { options, config ->
                if (options.v) {
                    terminate("version $config.version")
                }
                if(!options.t) {
                    help 'No todo list directory provided'
                }
                config.todoList = new TodoList(new File(options.t))
            }

            def listPrinter = { item, index ->
                println "$index ${item.done ? 'X':' '} '$item.item'"
            }

            task('list') {
                description 'List all todos in descending priority'
                configureCliBuilder { cliBuilder ->
                    cliBuilder.with {
                        c longOpt: 'completed', 'list completed todos'
                        p longOpt: 'pending', 'list pending todos'
                    }
                }
                handleOptions { options, config ->
                    config.list.ignoreComplete = options.c ? true : false
                    config.list.ignorePending = options.p ? true : false
                }
                onExecute { task, config, args ->
                    config.todoList.list(config.list.ignoreComplete, config.list.ignorePending, listPrinter)
                }
            }

            task('add') {
                description 'Add a todo'
                onExecute { task, config, args ->
                    config.todoList.add(args.head())
                    config.todoList.list listPrinter
                }
            }

            task('complete') {
                description 'Mark a todo as complete'
                onExecute { task, config, args ->
                    def index = Integer.parseInt(args[0])
                    config.todoList.done(index - 1)
                    config.todoList.list listPrinter
                }
            }

            task('remove') {
                description 'Remove a todo from the list'
                onExecute { task, config, args ->
                    def index = Integer.parseInt(args[0])
                    config.todoList.remove(index - 1)
                }
            }

            task('remove-completed') {
                description 'Remove all completed todos'
                onExecute { task, config, args ->
                    config.todoList.removeChecked()
                    config.todoList.list listPrinter
                }
            }
        }.run(initialConfig, cliArgs)
    }

    private static class TodoList {

        private final todoListDataFile

        private data

        public TodoList(File todoDir) {
            if (!todoDir.directory) {
                throw new RuntimeException("$todoDir is not a directory")
            }
            todoListDataFile = new File(todoDir, 'todo.json')
            if (!todoListDataFile.file) {
                data = [list:[]]
            } else {
                todoListDataFile.withReader { data = new JsonSlurper().parse(it) }
            }
        }

        public void list(ignoreComplete = false, ignorePending = false, forEach) {
            def filter = { true }
            if (ignoreComplete ^ ignorePending) {
                filter = ignoreComplete ? { item -> !item.done } : { item -> item.done }
            }
            data.list
                    .collect { item -> [item: item] }
                    .eachWithIndex { indexedItem, index -> indexedItem.index = index }
                    .findAll { filter(it.item) }
                    .each {
                        def item = it.item
                        def index = it.index + 1
                        forEach(item, index)
                    }
        }

        public void add(String item) {
            withList { list ->
                list << [item: item, done: false]
            }
        }

        private void withList(doit) {
            try {
                doit(data.list)
            } finally {
                save()
            }
        }

        private void save() {
            todoListDataFile.withWriter {
                new JsonBuilder(data).writeTo(it)
            }
        }

        public void done(int index) {
            withList { list ->
                list[index].done = true
            }
        }

        public void remove(int index) {
            withList { list ->
                data.list.remove(index)
            }
        }

        public void removeChecked() {
            withList { list ->
                list.removeAll {
                    it.done
                }
            }
        }

    }

    public static void main(String... args) {
        new TodoApp(args)
    }

}
