import kjson.*
import kliopt.*
import kommon.cityHash64
import kurl.*
import kommon.machineName
import kotlin.system.exitProcess

fun showUsage(message: String) {
    println(message)
    exitProcess(1)
}

fun main(args: Array<String>) {
    var server: String? = null
    var name: String = ""
    var command: String? = null
    var service: String? = null

    val options = listOf(
            OptionDescriptor(OptionType.STRING, "s", "server", "Server to connect", "http://localhost:8080"),
            OptionDescriptor(OptionType.STRING, "n", "name", "User name", "CLI user"),
            OptionDescriptor(OptionType.BOOLEAN, "h", "help", "Usage info"),
            OptionDescriptor(OptionType.STRING, "c", "command", "Command to issue"),
            OptionDescriptor(OptionType.STRING, "p", "password", "Password for admin access"),

            OptionDescriptor(OptionType.STRING, "f", "fcommand", "Finder command to issue"),
            OptionDescriptor(OptionType.STRING, "P", "parameter", "Parameter to pass, in form key=value")
    )
    var password = ""
    var parameterName = ""
    var parameterValue = ""
    parseOptions(options, args).forEach {
        when (it.descriptor?.longName) {
            "server" -> server = it.stringValue
            "name" -> name = it.stringValue
            "command" -> {
                service = "json"
                command = it.stringValue
            }
            "fcommand" -> {
                service = "finder"
                command = it.stringValue
            }
            "parameter" -> {
                it.stringValue.split("=").forEachIndexed { index, value ->
                    when (index) {
                        0 -> parameterName = value
                        1 -> parameterValue = value
                        else -> error("Incorrect format for $it, must be key=value")
                    }
                }
            }
            "password" -> password = it.stringValue
            "help" -> showUsage(makeUsage(options))
        }
    }
    val machine = machineName()
    val kurl = KUrl("cookies.txt")
    val url = "$server/$service/$command"
    val urlParams = mutableMapOf("name" to name, "client" to "cli", "machine" to machine, "password" to password)
    if (command == "proximity") {
        // Replace device name with the hash.   println("p=$parameterValue")
        parameterValue = parameterValue.split(',').map {
            val deviceStrength = it.split(':').toMutableList()
            deviceStrength[0] = deviceStrength[0].cityHash64().toString(16)
            deviceStrength.joinToString(":")
        }.joinToString(",")
    }
    if (parameterName.isNotEmpty()) urlParams[parameterName] = parameterValue
    println("Connecting to $url as $name from $machine")
    println("passing parameters: $urlParams")
    withUrl(kurl) { it.fetch(url, urlParams) {
        content -> withJson(content) {
                println("Got $it")
            }
        }
    }
}
