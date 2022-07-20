package kotlinLabGlobal.Interpreter

import org.jetbrains.kotlinx.ki.shell.KotlinShell.configuration
import org.jetbrains.kotlinx.ki.shell.configuration.CachedInstance
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfigurationBase
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm

class  KShell {

    companion object {
        @JvmStatic
        fun createShell(): org.jetbrains.kotlinx.ki.shell.Shell {
            val repl = org.jetbrains.kotlinx.ki.shell.Shell(
                configuration(),
                defaultJvmScriptingHostConfiguration,
                ScriptCompilationConfiguration {
                    jvm {
                        dependenciesFromClassloader(
                            classLoader = KShell::class.java.classLoader,
                            wholeClasspath = true
                        )
                    }
                },
                ScriptEvaluationConfiguration {
                    jvm {
                        baseClassLoader(KShell::class.java.classLoader)
                    }
                }
            )

            Runtime.getRuntime().addShutdownHook(Thread {
                println("\nBye!")
                repl.cleanUp()
            })

            return repl;
        }
    }

fun configuration(): ReplConfiguration {
    val instance = CachedInstance<ReplConfiguration>()
    val klassName: String? = System.getProperty("config.class")

    return if (klassName != null) {
        instance.load(klassName, ReplConfiguration::class)
    } else {
        instance.get { object : ReplConfigurationBase() {}  }
    }
}
}