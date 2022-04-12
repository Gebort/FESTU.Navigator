package com.example.festunavigator.domain.tree

data class WrongEntryException(
    val availableEntries: Set<String>
): Exception() {
    override val message = "Wrong entry number. Available: $availableEntries"
}