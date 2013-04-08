climatic
========

A simple Groovy framework for creating task based CLIs

Example
-------

Put the following *Groovy* code in the file _tasks.groovy_:

    import org.climatic.TaskApp

    new TaskApp('tasks').configure {
        configureCliBuilder { cliBuilder ->
            cliBuilder.with {
                h longOpt: 'help', 'usage information'
            }
        }
        handleOptions { options, config ->
            if (options.h) {
                help('Please select a task')
            }
        }
        task('print') {
            description 'print a message'
                configureCliBuilder { cliBuilder ->
                    cliBuilder.with {
                        h longOpt: 'help', 'usage information'
                        m longOpt: 'message', args:1, argName: 'message', 'a message to print'
                    }
                }
            handleOptions { options, config ->
                if(options.h) {
                    help()
                }
                if (!options.m) {
                    help 'Please provide a message'
                }
                config.message = options.m
            }
            onExecute { task, config, args ->
                println config.message
            }
        }
        task('time') {
            description 'show the time'
            onExecute { task, config, args ->
                println "The time is ${new Date()}"
            }
        }
    }.run(args)


Running the script with climatic on the classpath

    groovy -cp <climatic jar> tasks.groovy

yields

    usage: tasks [<options>] <task>
     -h,--help   usage information
    tasks:
     print    print a message
     time     print the time

and

    groovy -cp <climatic jar> tasks.groovy print -m 'Hello climatic'

yields

    Hello climatic

Building
--------

    ./gradlew
This will run all tests and create the assemble the jar file

Installing the sample app
-------------------------

    ./gradlew clean :sample-todo-list:installApp

Running the sample app
----------------------

    samples-todo-list/build/install/todo-list/bin/todo-list