package it.casaricci.hass.plugin.services

import com.intellij.openapi.util.IconLoader
import it.casaricci.hass.plugin.MyBundle
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.IOException
import javax.swing.Icon

@Serializable
data class IconObject(val name: String, val aliases: List<String>)

// FIXME icons are black by default, ignoring IDE theme
object MdiIconsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Load all icons from the metadata definition file.
     * @return a map with alias (including the name itself) as keys and name as value
     */
    @Throws(IOException::class)
    @OptIn(ExperimentalSerializationApi::class)
    fun loadIcons(): Map<String, String> {
        val dataset = this.javaClass.getResourceAsStream("/icons/mdi/meta.json")
            ?: throw IOException(MyBundle.message("hass.completion.icons.loadError"))

        return json.decodeFromStream<List<IconObject>>(dataset).flatMap {
            buildList {
                add(it.name to it.name)
                addAll(it.aliases.map({ alias ->
                    alias to it.name
                }))
            }
        }.toMap()
    }

    fun getIcon(name: String): Icon? {
        return IconLoader.findIcon("/icons/mdi/svg/${name}.svg", javaClass)
    }

}
