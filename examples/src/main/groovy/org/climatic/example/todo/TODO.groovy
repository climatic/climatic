package org.climatic.example.todo
  
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
  
import org.climatic.TaskApp

public class TODO {

  private TODO(String... cliArgs) {
    
    def app = new TaskApp()
    app.configure {
      onExecute { task, config, args ->
        help()
      }
      configureCli {
        setUsage '''
          T O D O   L I S T
          '''
        h longOpt: 'help', 'usage information'
        t longOpt: 'todo-home', args:1, argName:'dir', 'TODO list dir'
      }
      handleCli { options, config ->
        if(options.h) {
          help()
        }
        if(!options.t) {
          println 'no todo list dir set'
          help()
        } else {
          config.todoList = new TodoList(new File(options.t))
        }
      }

      task('show') {
        configureCli {
          //NOP
        }
        handleCli { options, config ->
          //NOP
        }
        onExecute { task, config, args ->
          config.todoList.each {
            println it
          }
        }
      }
      
      task('add') { 
        onExecute { task, config, args ->
          config.todoList.add(args.head())
        }
      }
    }
    app.run(*cliArgs)
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

    @Override
    public Iterator iterator() {
      data.list.iterator()
    }
    
    public void add(String item) {
      data.list << item
      save()
    }
    
    private void save() {
      todoListDataFile.withWriter {
        new JsonBuilder(data).writeTo(it)
      }
    }  
    
  }
  

  public static void main(String... args) {
    new TODO(args)
  }

}
