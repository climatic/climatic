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

package org.climatic

public class TaskApp {

    private final static char NBSP = '\u00A0'

    private final appName

    private final tasks = [:]

    private final beforeTasks = []

    private writer

    public TaskApp(String appName) {
        this.appName = appName
        this.writer = new PrintWriter(new NonBreakingSpaceFilter(System.out))
    }

    public void setWriter(Writer writer) {
        this.writer = new PrintWriter(new NonBreakingSpaceFilter(writer))
    }

    public TaskApp configure(Closure... taskConfigurations) {
        new TaskGraphBuilder(tasks).configure(taskConfigurations)
        this
    }

    public TaskApp onTaskBegin(Closure taskBeginAction) {
        beforeTasks << taskBeginAction
        this
    }

    public void help(message = '') {
        if (message) {
            message = '\n' + message + '\n'
        }
        terminate(message, new Help())
    }

    public void terminate(message = '', throwable = new TerminateTaskApp()) {
        if (message) {
            writer << message + '\n'
        }
        writer.flush()
        throw throwable
    }

    public void run(ConfigObject initialConfig = new ConfigObject(), String... args) {
        try {
            runUnsafe(initialConfig, args)
        } catch (TerminateTaskApp t) {
            //NOP
        }
    }

    private void runUnsafe(ConfigObject initialConfig, String... args) {
        def taskName = ':'
        def task = tasks[taskName]
        //TODO use a category instead?
        initialConfig.metaClass {
            extend { config ->
                delegate.merge(config.merge(delegate))
            }
        }
        def (argz, config) = configTask(task, args, initialConfig)
        if (argz) {
            taskName += argz.head()
            def nextTask = tasks[taskName]
            while (nextTask) {
                task = nextTask
                (argz, config) = configTask(task, argz.tail(), config)
                if (argz) {
                    taskName += ':' + argz.head()
                    nextTask = tasks[taskName]
                } else {
                    nextTask = null
                }
            }
        }
        def tasks = task.getDependencies(tasks)
        runTasks(tasks, config, argz)
    }

    private configTask(task, args, config) {
        def cli = new CliBuilder()
        cli.writer = writer
        cli.setUsage(createUsage(task))
        // TODO make help optional/configurable
        cli.with {
            h longOpt: 'help', 'usage information'
        }
        task.cliConfig.each { configCli ->
            configCli(cli)
        }
        def options = cli.parse(args)
        try {
            if (!options) {
                throw new TerminateTaskApp()
            }
            if(options.h) {
                help()
            }
            task.cliHandle.each { cliHandler ->
                cliHandler.delegate = this
                cliHandler(options, config)
            }
        } catch (Help h) {
            taskHelp(task, cli)
        }
        config[task.qualifiedName].cli = cli
        [options.arguments(), config]
    }

    private createUsage(task) {
        def qName = task.qualifiedName
        def opts = ' [<options>] '
        def taskList = qName == ':' ? opts : "$opts${task.qualifiedName.split(':').tail().join(opts)}$opts"
        "$appName$taskList<task>"
    }

    private void taskHelp(task, cli) {
        def subTasks = task.subTasks
        if (subTasks) {
            def footer = 'tasks:\n' << ''
            def pad = subTasks*.name.inject(0) { size, name -> name.size() > size ? name.size() : size }
            subTasks.each {
                footer << "$TaskApp.NBSP${it.name.padRight(pad)}    $it.description\n"
            }
            cli.setFooter footer.toString()
        }
        cli.usage()
        throw new TerminateTaskApp()
    }

    private void runTasks(tasks, config, argz) {
        tasks.each { task ->
            beforeTasks.each {
                it(task, config, argz)
            }
            try {
                def taskRunners = task.runners
                if(!taskRunners) {
                    help()
                }
                taskRunners.each {
                    it.delegate = this
                    it(task, config, argz)
                }
            } catch (Help h) {
                taskHelp(task, config[task.qualifiedName].cli)
            }
        }
    }

    private static class NonBreakingSpaceFilter extends FilterWriter {

        public NonBreakingSpaceFilter(Writer writer) {
            super(writer)
        }

        public NonBreakingSpaceFilter(OutputStream os) {
            super(new OutputStreamWriter(os))
        }

        public void write(char[] cbuf, int off, int len) {
            super.write(cbuf.collect {c -> c == TaskApp.NBSP ? ' ' : c} as char[], off, len)
        }
        public void write(int c) {
            super.write(c == TaskApp.NBSP ? ' ' : c)

        }
        public void write(String str, int off, int len) {
            super.write(str.replace(TaskApp.NBSP, (char) ' '), off, len)
        }
    }

    private static class TaskGraphBuilder {

        private final tasks

        private currentTask

        public TaskGraphBuilder(tasks) {
            this.tasks = tasks
        }

        public void configure(Closure... taskConfigurations) {
            task(null, taskConfigurations)
        }

        public Task task(String name, Closure... taskConfigurations) {
            Task task = resolveTaskFromName(name)
            if (taskConfigurations) {
                def parentTask = currentTask
                currentTask = task
                taskConfigurations.each { taskConfiguration ->
                    taskConfiguration.delegate = this
                    taskConfiguration()
                }
                currentTask = parentTask
            }
            task
        }

        private resolveTaskFromName(name) {
            def task
            if (currentTask) {
                task = currentTask.subTask(name)
            } else {
                task = getRootTask() ?: Task.createRootTask()
            }
            tasks[task.qualifiedName] = task
            task
        }

        private getRootTask() {
            tasks[':']
        }

        public Task description(String description) {
            currentTask.description = description
            currentTask
        }

        public Task dependsOn(String... tasks) {
            currentTask.dependsOn.addAll tasks
            currentTask
        }

        public Task configureCliBuilder(cli) {
            currentTask.cliConfig << cli
            currentTask
        }

        public Task handleOptions(cli) {
            currentTask.cliHandle << cli
            currentTask
        }

        public Task onExecute(job) {
            currentTask.runners << job
            currentTask
        }

    }

    private static class Task {

        final name

        final qualifiedName

        final dependsOn = []

        final cliConfig = []

        final cliHandle = []

        final runners = []

        String description

        def subTasks = []

        private Task(name, qualifiedName) {
            this.name = name
            this.qualifiedName = qualifiedName
        }

        static Task createRootTask() {
            new Task(':', ':')
        }

        Task subTask(name) {
            def task = subTasks.find { it.name == name }
            if (!task) {
                task = new Task(name, fullyQualify(name))
                subTasks << task
            }
            task
        }

        private fullyQualify(taskName) {
            qualifiedName == ':' ? ':' + taskName : qualifiedName + ':' + taskName
        }

        def getDependencies(tasks, upstream = [this].toSet()) {
            dependsOn.unique().collect {
                def task = tasks[it]
                if(!task) {
                    throw new RuntimeException("Task $it does not exist")
                }
                task
            }.each {
                if (upstream.contains(it)) {
                    throw new RuntimeException("Cyclic dependencies found")
                }
                upstream << it
            }.collect {
                it.getDependencies(tasks, upstream)
            }.flatten().unique() + [this]
        }

    }

    private static class Help extends RuntimeException {}

    private static class TerminateTaskApp extends RuntimeException {}

}
