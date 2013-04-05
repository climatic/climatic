package org.climatic

public class TaskApp {

  private final tasks = [:]

  private final beforeTasks = []
    
  private static class TaskGraphBuilder {
    
    private final tasks
      
    private currentTask
    
    public TaskGraphBuilder(tasks) {
      this.tasks = tasks
    }
    
    public void configure(Closure... taskConfigurations) {
      task(null, taskConfigurations)
    }
    
    Task task(String name, Closure... taskConfigurations) {
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
        task = currentTask.createTask(name)
      } else {
        task = getRootTask() ?: Task.createRootTask()
      }
      tasks[task.qualifiedName] = task
      task
    }
    
    private getRootTask() {
      tasks[':']
    }

    public void dependsOn(String... tasks) {
      currentTask.dependsOn.addAll tasks
    }

    public void configureCli(cli) {
      currentTask.cliConfig << cli
    }

    public void handleCli(cli) {
      currentTask.cliHandle << cli
    }

    public void onExecute(job) {
      currentTask.runners << job
    }  
    
  }

  private static class Task {
    final name
    final qualifiedName
    final dependsOn = []
    final cliConfig = []
    final cliHandle = []
    final runners = []

    def subTasks = []

    private Task(name, qualifiedName) {
      this.name = name
      this.qualifiedName = qualifiedName
    }
    
    static Task createRootTask() {
      new Task(':', ':')
    }

    Task createTask(name) {
      def task = new Task(name, fullyQualify(name))
      subTasks << task
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

    def asMap() {
        def details = [name: name, qn: qualifiedName]
      if (dependsOn) {
        details.dependsOn = dependsOn.join(', ')
      }
      if (subTasks) {
        details.subTasks = subTasks*.asMap()
      }
      details
    }

  }

  TaskApp configure(Closure... taskConfigurations) {
    new TaskGraphBuilder(tasks).configure(taskConfigurations)
    this
  }

  TaskApp onTaskBegin(Closure taskBeginAction) {
    beforeTasks << taskBeginAction
    this
  }

  private static class Help extends RuntimeException {}

  public void help() {
    throw new Help()
  }

  def run(String... args) {
    def taskName = ':'
    def task = tasks[taskName]
    def (argz, config) = configTask(task, args)
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
    task
  }

  private configTask(task, args, config = new ConfigObject()) {
    def cli = new CliBuilder()
    task.cliConfig.each {
      cli.with it
    }
    def options = cli.parse(args)
    try {
      task.cliHandle.each { cliHandler ->
        cliHandler.delegate = this
        cliHandler(options, config)
      }
    } catch (Help h) {
      cli.usage()
    }
    config[task.qualifiedName].cli = cli
    [options.arguments(), config]
  }

  private runTasks(tasks, config, argz) {
    tasks.each { task ->
      beforeTasks.each {
        it(task, config, argz)
      }
      try {
        task.runners.each {
          it.delegate = this
          it(task, config, argz)
        }
      } catch (Help h) {
        config[task.qualifiedName].cli.usage()
      }
    }
  }

}
