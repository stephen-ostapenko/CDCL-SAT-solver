package mkn.mathlog.utils

import kotlinx.cinterop.memScoped
import platform.posix.EOF
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

class FileOutput(private val filePath: String) {
    fun writeAllLines(lines: List<String>, lineEnding: String = "\n") {
        val file = fopen(filePath, "w") ?:
            throw IllegalArgumentException("Cannot open output file $filePath")

        try {
            memScoped {
                lines.forEach {
                    if (fputs(it + lineEnding, file) == EOF) {
                        throw Error("File write error")
                    }
                }
            }
        } finally {
            fclose(file)
        }
    }
}