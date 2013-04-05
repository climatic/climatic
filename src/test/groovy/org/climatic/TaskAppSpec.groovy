package org.climatic

import spock.lang.Specification

class TaskAppSpec extends Specification {
  
  def 'full test'() {
    def app = new TaskApp()
    app.configure {
      onExecute { task, config, args ->
        help()
      }
      configureCli {
        setUsage '''
          T A S K   A P P
          '''
        h longOpt: 'help', 'usage information'
        t longOpt: 'test.home', args: 1, argName: 'dir', 'Test Suite home'
      }
      handleCli { options, config ->
        if(options.h) {
          help()
          config.h = 'help'
        }
        if(!options.t) {
          println 'no test suite set'
            help()
          } else {
            config.testHome = options.t
          }
        config.root = 'root value'
      }
      task('suite') {
        onExecute { task, config, args ->
          println config.root
        }
        task('show-all')
      }
      task('test') {
        dependsOn ':group1'

        handleCli { options, config ->
        }
        onExecute { task, config, args ->
          println '    group options'
        }
        task('clean') {
          onExecute { task, config, args ->
            println "    executing task '$task.name'"
          }
        }
        task('run') {
          dependsOn(':test:clean', ':suite:show-all')
          onExecute { task, config, args ->
            println "    executing task '$task.name' - config '$config', args '$args'"
          }
        }
        task('run-ouput') {
          onExecute { task, config, args ->
            println "    executing task '$task.name' - config '$config', args '$args'"
          }
        }
      }
    }
    /*app.onTaskBegin { task, config, args ->
      println "  before executing $task.name $config"
    }*/
    app.run('-t /tmp test run'.split())
      expect:
    true
  }
}
