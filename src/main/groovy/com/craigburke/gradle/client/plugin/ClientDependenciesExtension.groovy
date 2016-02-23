/*
 * Copyright 2016 Craig Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.craigburke.gradle.client.plugin

import com.craigburke.gradle.client.dependency.DeclaredDependency
import com.craigburke.gradle.client.registry.Registry
import org.gradle.api.Project

class ClientDependenciesExtension {

    int threadPoolSize = 15

    Project project
    String installDir
    String cacheDir

    List<String> fileExtensions = ['css', 'js', 'eot', 'svg', 'ttf', 'woff', 'woff2', 'ts']
    List<String> releaseFolders = ['dist', 'release']
    List<String> copyIncludes = []
    List<String> copyExcludes = ['**/*.min.js', '**/*.min.css', '**/*.map', '**/Gruntfile.js', 'index.js', 'gulpfile.js', 'source/**']

    Closure defaultCopy

    ClientDependenciesExtension(Project project) {
        this.project = project
    }

    Map<String, Registry> registryMap = [:]
    List<DeclaredDependency> rootDependencies = []

    def methodMissing(String registryName, args) {
       if (args && args.last() instanceof Closure) {
           Registry registry = registryMap[registryName]
           DependencyBuilder dependencyBuilder = new DependencyBuilder(registry)
           Closure clonedClosure = args.last().rehydrate(dependencyBuilder, this, this)
           clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST
           clonedClosure()
           rootDependencies += dependencyBuilder.rootDependencies
       }
    }

    Closure getDefaultCopyConfig(File sourceFolder) {
        if (defaultCopy) {
            return defaultCopy
        }

        String pathPrefix = sourceFolder
                .listFiles()
                .find { it.directory && releaseFolders.contains(it.name)}?.name ?: ''

        List<String> includes = fileExtensions
                .collect { "${pathPrefix ? pathPrefix + '/' : ''}**/*.${it}"} + copyIncludes

        List<String> excludes = copyExcludes

        return {
            exclude excludes
            include includes
            if (pathPrefix) {
                eachFile { it.path -= "${pathPrefix}/" }
            }
        }
    }
}
