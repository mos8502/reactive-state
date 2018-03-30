import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

open class Configuration(configuration: String, required: Boolean = true) {
    private val properties = Properties().apply {
        try {
            FileInputStream(configuration).use { load(it) }
        } catch (error: FileNotFoundException) {
            if (required) throw error
        }
    }

    protected fun int(name: String? = null, default: Int = 0): ReadOnlyProperty<Configuration, Int> =
            Property(default, String::toInt, name)

    protected fun string(name: String? = null, default: String = ""): ReadOnlyProperty<Configuration, String> =
            Property(default, { this }, name)

    private class Property<out T>(val default: T, val map: String.() -> T, val name: String?) : ReadOnlyProperty<Configuration, T> {
        override fun getValue(thisRef: Configuration, property: KProperty<*>): T {
            val name = name ?: property.name
            return if (thisRef.properties.stringPropertyNames().contains(name)) thisRef.properties.getProperty(name).map()
            else default
        }
    }

}