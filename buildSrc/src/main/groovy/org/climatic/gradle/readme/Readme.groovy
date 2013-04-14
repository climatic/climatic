package org.climatic.gradle.readme

import java.net.URLClassLoader

import groovy.text.SimpleTemplateEngine
import groovy.ui.SystemOutputInterceptor

// TODO extract plugin
public class Readme {

    private final readmeDir = new File('src/readme')

    private final snippetsDir = new File(readmeDir, 'snippets')
    
    private classLoader

    public void generate(List<File> jarFiles) {
        classLoader  = new URLClassLoader(jarFiles.collect { it.toURL() } as URL[])
        def text = new File(readmeDir, 'README.md').text
        def binding = [
            snippetsSrc: { generateSrc it },
            snippetsResult: { code, args -> generateRes code, args }
        ]
        def engine = new groovy.text.SimpleTemplateEngine()
        def template = engine.createTemplate(text).make(binding)
          new File('README.md').withWriter { it << template }
    }

    def generateSrc(code) {
        new File(snippetsDir, code + '.groovy').readLines().inject('' << '') {memo, line ->
            memo << formatLine(line) }.toString()

    }
    
    private static formatLine(line) {
      (line.trim() ? '    ' << line : '') << '\n'
    }

    def generateRes(code, args = []) {
        def output = '' << ''
        def interceptor = new SystemOutputInterceptor({ it.eachLine { line -> output << formatLine(line) }; false})
        def shell = new GroovyShell(classLoader)
        interceptor.start()
        shell.run(new File(snippetsDir, code + '.groovy'), args)
        interceptor.stop()
        output.toString()
    }
}
