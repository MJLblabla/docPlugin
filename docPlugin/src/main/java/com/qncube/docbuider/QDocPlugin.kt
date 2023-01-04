package com.qncube.docbuider

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

class QDocPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val docTask = project.tasks.create("buidQDoc")
        project.extensions.create("docSrcFiles", DocSrcFiles::class.java)
        docTask.doLast {
            val allPJ = project.rootProject.allprojects
            println("allPJ " + allPJ.size)
            val sources = ArrayList<String>()
            allPJ.forEach { proj ->
                println(proj.name)
                try {
                    val android = proj.extensions.getByType(BaseExtension::class.java)
                    //  project.rootProject.subprojects.forEach { proj ->
                    val mainSourceSet = android.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                    mainSourceSet.java.getSourceFiles().files.forEach { file ->
                        println("srcDirs file ${file.name}  ${file.absolutePath}")
                        sources.add(file.absolutePath)
                    }
                } catch (e: Exception) {
                    println("${proj.name} is not android")
                }
            }
            val srcFiles = project.extensions.getByType(DocSrcFiles::class.java).files
            println("docTask.doLast  ${srcFiles.size}")

            Doclet.links.clear()
            Doclet.links.putAll(srcFiles)
            Doclet.println(sources, project.buildDir.absolutePath)
        }
    }
}
