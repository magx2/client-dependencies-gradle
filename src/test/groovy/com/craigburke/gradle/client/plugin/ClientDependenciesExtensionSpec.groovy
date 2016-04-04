package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.registry.bower.BowerRegistry
import com.craigburke.gradle.client.registry.core.Registry
import com.craigburke.gradle.client.registry.npm.NpmRegistry
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ClientDependenciesExtensionSpec extends Specification {

    @Subject ClientDependenciesExtension extension
    @Rule TemporaryFolder sourceFolder = new TemporaryFolder()

    def setup() {
        Project project = [ getLogger: { Logging.getLogger('foo') } ] as Project
        extension = new ClientDependenciesExtension(project)
    }

    def "include with default file extensions are used by default"() {
        expect:
        defaultCopyResults.includes == extension.fileExtensions.collect { "**/*.${it}" }
    }

    def "source folder containing a release folder changes the default includes"() {
        setup:
        extension.releaseFolders = ['dist']
        new File("${sourceFolder.root.absolutePath}/dist").mkdir()

        expect:
        defaultCopyResults.includes == extension.fileExtensions.collect { "dist/**/*.${it}" }
    }

    def "default excludes are used by default"() {
        expect:
        defaultCopyResults.excludes == extension.copyExcludes
    }

    @Unroll
    def "include is generated correctly extensions: #fileExtensions, includes: #copyIncludes folders: #releaseFolder"() {
        setup:
        extension.fileExtensions = fileExtensions
        extension.copyIncludes = copyIncludes

        if (releaseFolder) {
            extension.releaseFolders = [releaseFolder]
            new File("${sourceFolder.root.absolutePath}/${releaseFolder}").mkdir()
        }

        expect:
        defaultCopyResults.includes == result

        where:
        fileExtensions | copyIncludes                  | releaseFolder | result
        ['foo']        | []                            | null | ['**/*.foo']
        []             | ['/foo/**']                   | null | ['/foo/**']
        ['foo']        | ['/bar/**']                   | null | ['**/*.foo', '/bar/**']
        ['foo', 'bar'] | ['/bar/**']                   | null | ['**/*.foo', '**/*.bar', '/bar/**']
        ['foo', 'bar'] | ['/bar/**', 'baz/**/*foobar'] | null | ['**/*.foo', '**/*.bar', '/bar/**', 'baz/**/*foobar']

        ['foo']        | []                            | 'dist' | ['dist/**/*.foo']
        []             | ['/foo/**']                   | 'dist' | ['/foo/**']
        ['foo']        | ['/bar/**']                   | 'dist' | ['dist/**/*.foo', '/bar/**']
        ['foo', 'bar'] | ['/bar/**']                   | 'dist' | ['dist/**/*.foo', 'dist/**/*.bar', '/bar/**']
        ['foo', 'bar'] | ['/bar/**', 'baz/**/*foobar'] | 'dist' | ['dist/**/*.foo', 'dist/**/*.bar', '/bar/**', 'baz/**/*foobar']
    }

    @Unroll
    def "exclude is generated correctly when set to #copyExcludes"() {
        setup:
        extension.copyExcludes = copyExcludes

        expect:
        defaultCopyResults.excludes == copyExcludes

        where:
        copyExcludes << [
                [],
                ['foo/**'],
                ['foo/**', 'bar/**'],
                ['foo/**', 'bar/**', '*.foo'],
        ]
    }

    def "default copy closure is returned when it's specified"() {
        setup:
        extension.defaultCopy = myDefaultCopy

        expect:
        extension.getDefaultCopyConfig(sourceFolder.root) == myDefaultCopy

        where:
        myDefaultCopy = {
            include '**'
            exclude '*.less'
        }
    }

    @Unroll
    def "can register custom registry #name"() {
        when:
        extension.registry(name: name, type: type, url: url)
        Registry registry = extension.registryMap[name]

        then:
        registry.url == url

        and:
        registry.getClass() == expectedClass

        where:
        name         | type    | url                            | expectedClass
        'npmLocal'   | 'npm'   | 'http://www.example.com/npm'   | NpmRegistry
        'bowerLocal' | 'bower' | 'http://www.example.com/bower' | BowerRegistry

    }

    Map getDefaultCopyResults() {
        Closure closure = extension.defaultCopyConfig
        Map result = [includes: [], excludes: []]
        Expando delegate = new Expando()

        delegate.metaClass.getProperty = { String name ->
            extension[name]
        }
        delegate.include = {
            result.includes.addAll(it)
        }
        delegate.exclude = {
            result.excludes.addAll(it)
        }
        delegate.eachFile = { }

        Closure clonedClosure = closure.rehydrate(delegate, delegate, delegate)
        clonedClosure.call()
        result
    }

}
