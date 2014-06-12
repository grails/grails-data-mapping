includeTargets << new File(releasePluginDir, "scripts/_GrailsMaven.groovy")

target(default: "Generate a POM for a plugin project.") {
    depends(parseArguments, generatePom)
}
