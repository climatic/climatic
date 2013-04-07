package org.climatic

import spock.lang.Specification

class TaskAppSpec extends Specification {

  private final writer = new StringWriter()

  private final taskConfiguration = {
      task('test') {
        configureCliBuilder { cliBuilder ->
          cliBuilder.with {
            e longOpt: 'echo', args: 1, argName: 'message', 'message'
          }
        }
        handleOptions { options, config ->
          if(!options.e) {
            help 'No message'
          }
          config.message = options.e
        }
        onExecute { task, config, args ->
          writer << config.message
        }
      }

    }

  def 'task executed'() {
    def app = new TaskApp('app')
    app.writer = writer
    app.configure taskConfiguration
    app.run(['test', '-e', 'testing testing'] as String[])

    expect:
        'testing testing' == writer.toString()
  }

  def 'task configured incorrectly'() {
    def app = new TaskApp('app')
    app.writer = writer
    app.configure taskConfiguration
    app.run(['test'] as String[])

    expect:
        writer.toString().startsWith('\nNo message')
  }
}
