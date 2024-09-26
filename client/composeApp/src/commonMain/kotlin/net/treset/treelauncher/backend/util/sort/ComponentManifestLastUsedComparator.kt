package net.treset.treelauncher.backend.util.sort

import net.treset.treelauncher.backend.data.manifest.Component
import net.treset.treelauncher.localization.strings

class ComponentManifestLastUsedComparator : Comparator<Component> {
    override fun compare(o1: Component, o2: Component): Int {
        if (o1.lastUsed == o2.lastUsed) {
            return 0
        }
        o1.lastUsed.let {  o1Used ->
            o2.lastUsed.let { o2Used ->
                if(o1Used == null) {
                    return 1
                }
                if(o2Used == null) {
                    return -1
                }
                return o2Used.compareTo(o1Used)
            }
        }
    }

    override fun toString(): String = strings().sortBox.sort.lastUsed()
}
