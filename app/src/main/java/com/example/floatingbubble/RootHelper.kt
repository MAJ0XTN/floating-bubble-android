package com.example.floatingbubble

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Runs commands as root. Ctrl/Alt/Tab/Shift key injection is only possible
 * with root on a non-rooted device, so those actions go through here.
 */
object RootHelper {
    var hasRoot = false
        private set

    fun checkRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root_ok"))
            val line = BufferedReader(InputStreamReader(process.inputStream)).readLine()
            process.waitFor()
            val ok = line?.contains("root_ok") == true
            hasRoot = ok
            ok
        } catch (e: Exception) {
            hasRoot = false
            false
        }
    }

    fun runAsRoot(vararg commands: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            commands.forEach { os.writeBytes("$it\n") }
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    /** Inject one or more Android keycodes, e.g. "KEYCODE_CTRL_LEFT KEYCODE_A". */
    fun sendKey(vararg keycodes: String) {
        val cmd = "input keyevent " + keycodes.joinToString(" ")
        runAsRoot(cmd)
    }
}
