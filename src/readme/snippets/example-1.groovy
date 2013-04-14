import org.climatic.TaskApp

new TaskApp('tasks').configure {
    configureCliBuilder { cliBuilder ->
        cliBuilder.with {
            v longOpt: 'version', 'show version'
        }
    }
    handleOptions { options, config ->
        if (options.v) {
            terminate('Version 1.0')
        }
    }
    task('print') {
        description 'print a message'
            configureCliBuilder { cliBuilder ->
                cliBuilder.with {
                    m longOpt: 'message', args:1, argName: 'message', 'a message to print'
                }
            }
        handleOptions { options, config ->
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