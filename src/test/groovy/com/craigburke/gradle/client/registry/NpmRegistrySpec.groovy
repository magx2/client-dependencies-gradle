package com.craigburke.gradle.client.registry

import com.craigburke.gradle.client.dependency.Dependency
import com.craigburke.gradle.client.dependency.SimpleDependency
import spock.lang.Unroll

class NpmRegistrySpec extends AbstractRegistrySpec {

    def setup() {
        setupRegistry(NpmRegistry)

        responses = [
                '/foo'          : resource('npm/foo.json').text,
                '/bar'          : resource('npm/bar.json').text,
                '/baz'          : resource('npm/baz.json').text,
                '/foobar'       : resource('npm/foobar.json').text,
                '/foo-1.0.0.tgz': resource('npm/foo-1.0.0.tgz').bytes,
                '/bar-1.0.0.tgz': resource('npm/bar-1.0.0.tgz').bytes
        ]
    }

    @Unroll
    def "can get source for #name@#version"() {
        given:
        SimpleDependency simpleDependency = new SimpleDependency(name: name, versionExpression: version)
        Dependency dependency = registry.loadDependency(simpleDependency)

        when:
        File source = registry.getInstallSource(dependency)

        then:
        source.name == "package.tgz"

        and:
        getChecksum(source.bytes) == checksum

        where:
        name  | version | checksum
        'foo' | '1.0.0' | '117f23ed600939d08eb0cf258439c842c7feea2d'
        'bar' | '1.0.0' | 'f097c2eb1f6e27d34566ccabe689f25881a03558'
    }


}
