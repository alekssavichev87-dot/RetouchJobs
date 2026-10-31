package com.example.retouchjobs.data

import java.util.concurrent.TimeUnit

/**
 * Шифрование секретов средствами Windows (DPAPI, область «текущий пользователь»):
 * расшифровать данные может только тот же пользователь Windows на этом же компьютере.
 * Это тот же механизм, на котором построен Диспетчер учётных данных Windows.
 * Вызывается через встроенный PowerShell, дополнительные библиотеки не нужны.
 * Секрет передаётся через стандартный ввод, а не в командной строке.
 * При любой ошибке (и не на Windows) возвращается null.
 */
object WindowsDpapi {
    private val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    private const val PROTECT =
        "Add-Type -AssemblyName System.Security; " +
            "\$i = [Console]::In.ReadToEnd().Trim(); " +
            "[Convert]::ToBase64String([System.Security.Cryptography.ProtectedData]::Protect(" +
            "[Text.Encoding]::UTF8.GetBytes(\$i), \$null, 'CurrentUser'))"

    private const val UNPROTECT =
        "Add-Type -AssemblyName System.Security; " +
            "\$i = [Console]::In.ReadToEnd().Trim(); " +
            "[Text.Encoding]::UTF8.GetString([System.Security.Cryptography.ProtectedData]::Unprotect(" +
            "[Convert]::FromBase64String(\$i), \$null, 'CurrentUser'))"

    fun protect(plain: String): String? = run(PROTECT, plain)

    fun unprotect(encoded: String): String? = run(UNPROTECT, encoded)

    private fun run(script: String, input: String): String? {
        if (!isWindows) return null
        return try {
            val process = ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-Command", script,
            ).redirectError(ProcessBuilder.Redirect.DISCARD).start()
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(input) }
            val out = process.inputStream.bufferedReader(Charsets.UTF_8).readText().trim()
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return null
            }
            if (process.exitValue() != 0 || out.isEmpty()) null else out
        } catch (e: Exception) {
            null
        }
    }
}
