package org.readium.r2.streamer.Fetcher

import org.readium.r2.shared.Publication
import org.readium.r2.streamer.Containers.Container
import java.io.InputStream

class Fetcher(var publication: Publication, var container: Container, private val userPropertiesPath: String?) {
    var rootFileDirectory: String
    var contentFilters: ContentFilters?

    init {
    val rootFilePath = publication.internalData["rootfile"] ?: throw Exception("Missing root file")
        if (rootFilePath.isNotEmpty() && rootFilePath.contains('/')) {
            rootFileDirectory = rootFilePath.replaceAfterLast("/", "", rootFilePath)
            rootFileDirectory = rootFileDirectory.dropLast(1)
        } else {
            rootFileDirectory = ""
        }
        contentFilters = getContentFilters(container.rootFile.mimetype)
    }

    fun data(path: String): ByteArray? {
        publication.resource(path) ?: throw Exception("Missing file")
        var data: ByteArray? = container.data(path)
        if (data != null)
            data = contentFilters?.apply(data, publication, container, path)
        return data
    }

    fun dataStream(path: String): InputStream {
        publication.resource("/" + path) ?: throw Exception("Missing file")
        var inputStream = container.dataInputStream(path)
        inputStream = contentFilters?.apply(inputStream, publication, container, path) ?: inputStream
        return inputStream
    }

    fun dataLength(path: String): Long {
        val relativePath = rootFileDirectory.plus(path)

        publication.resource(path) ?: throw Exception("Missing file")
        return container.dataLength(relativePath)
    }

    private fun getContentFilters(mimeType: String?): ContentFilters {
        return when (mimeType) {
            "application/epub+zip", "application/oebps-package+xml" -> ContentFiltersEpub(userPropertiesPath)
            "application/vnd.comicbook+zip", "application/x-cbr" -> ContentFiltersCbz()
            else -> throw Exception("Missing container or MIMEtype")
        }
    }
}