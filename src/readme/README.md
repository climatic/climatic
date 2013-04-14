climatic ![Travis CI status](https://api.travis-ci.org/climatic/climatic.png)
========

A simple Groovy framework for creating task based CLIs

Example
-------

Put the following *Groovy* code in the file _tasks.groovy_:

${snippetsSrc 'example-1'}

Running the script with climatic on the classpath

    groovy -cp <climatic jar> tasks.groovy

yields

${snippetsResult 'example-1', []}

and

    groovy -cp <climatic jar> tasks.groovy print -m 'Hello climatic'

yields

${snippetsResult 'example-1', ['print', '-m', 'Hello climatic']}

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
