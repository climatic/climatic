package org.climatic.example.todo

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import org.climatic.TaskApp

public class TODO {

  private TODO(String... cliArgs) {

    new TaskApp().configure {

      configureCliBuilder { cliBuilder ->
        cliBuilder.with {
          setUsage 'todo [options] task'
          h longOpt: 'help', 'usage information'
          t longOpt: 'todo-home', args: 1, argName: 'dir', 'TODO dir'
        }
      }

      handleOptions { options, config ->
        if(options.h) {
          help()
        }
        if(!options.t) {
          help 'No TODO dir set'
        }
        config.todoList = new TodoList(new File(options.t))
      }

      task('list') {
        description 'List all todos in descending priority'
        onExecute { task, config, args ->
          config.todoList.eachWithIndex { item, i ->
            println "${i + 1} ${item.done ? 'X':' '} '$item.item'"
          }
        }
      }

      task('add') {
        description 'Add a todo'
        onExecute { task, config, args ->
          config.todoList.add(args.head())
        }
      }

      task('complete') {
        description 'Mark a todo as complete'
        onExecute { task, config, args ->
          def index = Integer.parseInt(args[0])
          config.todoList.done(index - 1)
        }
      }

      task('remove') {
        description 'Remove a todo from list'
        onExecute { task, config, args ->
          def index = Integer.parseInt(args[0])
          config.todoList.remove(index - 1)
        }
      }

      task('remove-completed') {
        description 'Remove all completed todos'
        onExecute { task, config, args ->
          config.todoList.removeChecked()
        }
      }
    }.run(cliArgs)
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
      withList { list ->
        list << [item: item, done: false]
      }
      save()
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
    new TODO(args)
  }

}
