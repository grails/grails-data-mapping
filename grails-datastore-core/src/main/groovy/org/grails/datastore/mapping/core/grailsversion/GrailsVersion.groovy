package org.grails.datastore.mapping.core.grailsversion

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.reflect.ClassUtils


/**
 * A class to represent a version of Grails for comparison
 *
 * @author James Kleeh
 */
@CompileStatic
class GrailsVersion implements Comparable<GrailsVersion> {

    /**
     * The current version
     */
    private static GrailsVersion currentVersion = null

    /**
     * The major version
     */
    int major
    /**
     * The minor version
     */
    int minor
    /**
     * The patch version
     */
    int patch
    /**
     * Information about the snapshot status
     */
    Snapshot snapshot
    /**
     * The full version text
     */
    String versionText

    GrailsVersion(String version) {
        String[] parts = version.split("\\.")
        if (parts.length >= 3) {
            this.versionText = version
            if (parts.length > 3) {
                this.snapshot = new Snapshot(parts[3])
            }
            this.major = parts[0].toInteger()
            this.minor = parts[1].toInteger()
            this.patch = parts[2].toInteger()
        } else {
            throw new IllegalArgumentException("GrailsVersion only supports comparison of versions with 3 or 4 parts")
        }
    }

    /**
     * Check whether the version is at least the given version
     *
     * @param requiredVersion The required version
     * @return True if it is
     */
    static boolean isAtLeast(String requiredVersion) {
        GrailsVersion currentVersion = getCurrent()
        if (currentVersion != null) {
            // if the current version is greater than the required versiono
            if (currentVersion.compareTo(new GrailsVersion(requiredVersion)) == -1) {
                return true
            }
        }
        return false
    }

    /**
     * @return Obtains the current Grails version
     */
    static GrailsVersion getCurrent() {
        if(currentVersion != null) {
            return currentVersion
        }
        else if (ClassUtils.isPresent("grails.util.BuildSettings")) {
            currentVersion = new GrailsVersion(Class.forName("grails.util.BuildSettings").package.implementationVersion)
            return currentVersion
        } else {
            null
        }
    }

    boolean isSnapshot() {
        snapshot != null
    }

    @Override
    int compareTo(GrailsVersion o) {
        int majorCompare = this.major <=> o.major
        if (majorCompare != 0) {
            return majorCompare
        }

        int minorCompare = this.minor <=> o.minor
        if (minorCompare != 0) {
            return minorCompare
        }

        int patchCompare = this.patch <=> o.patch
        if (patchCompare != 0) {
            return patchCompare
        }

        if (this.isSnapshot() && !o.isSnapshot()) {
            return -1
        } else if (!this.isSnapshot() && o.isSnapshot()) {
            return 1
        } else if (this.isSnapshot() && o.isSnapshot()) {
            return this.getSnapshot() <=> o.getSnapshot()
        } else {
            return 0
        }
    }
}
