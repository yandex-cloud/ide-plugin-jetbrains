package yandex.cloud.toolkit.util

enum class SourceFolderPolicy(val displayName: String) {

    KEEP("Upload Folders Directly"),
    EXTRACT("Extract Top Level Folders"),
    FLATTEN("Flatten File Tree");

    val id: String get() = name.lowercase()

    companion object {

        fun byId(id: String?): SourceFolderPolicy = values().find { it.id == id } ?: KEEP
    }
}