package org.grails.datastore.mapping.core.grailsversion


/**
 * A class to represent a version of Grails for comparison
 *
 * @author James Kleeh
 */
class GrailsVersion implements Comparable<GrailsVersion> {

    int major
    int minor
    int patch
    Snapshot snapshot
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
