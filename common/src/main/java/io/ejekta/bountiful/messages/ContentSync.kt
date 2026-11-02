package io.ejekta.bountiful.messages

import io.ejekta.bountiful.Bountiful
import io.ejekta.bountiful.config.JsonFormats
import io.ejekta.bountiful.content.BountifulContent
import io.ejekta.bountiful.data.Decree
import io.ejekta.bountiful.data.Pool
import io.ejekta.bountiful.data.PoolEntry
import io.ejekta.kambrik.message.KambrikMsg
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.server.level.ServerPlayer

/**
 * Decrees and pools are loaded from server data only, so on a dedicated server the client has none of them.
 * Client screens that show them (analyzer, editor) need a copy: [ContentSync.sendTo] snapshots the server's
 * decrees and pools and sends them in chunks (a string in a packet is limited in length), and the client
 * rebuilds them once all chunks have arrived.
 *
 * When the client hosts the server itself (singleplayer), both sides share the same objects and nothing is replaced.
 */
@Serializable
class ContentSyncChunk(
    private val syncId: Int,
    private val index: Int,
    private val total: Int,
    private val data: String
) : KambrikMsg() {

    override fun onClientReceived() {
        ContentSync.receive(syncId, index, total, data)
    }
}

object ContentSync {

    private const val CHUNK_SIZE = 20_000

    @Serializable
    private class EntryDto(val id: String, val json: String)

    @Serializable
    private class PoolDto(val id: String, val weightMult: Double, val currency: Boolean, val entries: List<EntryDto>)

    @Serializable
    private class SnapshotDto(val decrees: List<Decree>, val pools: List<PoolDto>)

    private var nextSyncId = 0

    // Client side assembly state
    private var receivingId = -1
    private var receivedChunks = arrayOfNulls<String>(0)

    private fun snapshot(): String {
        val dto = SnapshotDto(
            BountifulContent.Decrees.toList(),
            BountifulContent.Pools.map { pool ->
                PoolDto(pool.id, pool.weightMult, pool.currency, pool.items.map { EntryDto(it.id, it.save()) })
            }
        )
        return JsonFormats.DataPack.encodeToString(SnapshotDto.serializer(), dto)
    }

    fun sendTo(player: ServerPlayer) {
        val text = snapshot()
        val chunks = text.chunked(CHUNK_SIZE)
        val syncId = nextSyncId++
        Bountiful.LOGGER.debug("Syncing ${BountifulContent.Decrees.size} decrees and ${BountifulContent.Pools.size} pools to ${player.plainTextName}: ${chunks.size} chunks")
        chunks.forEachIndexed { index, chunk ->
            ContentSyncChunk(syncId, index, chunks.size, chunk).sendToClient(player)
        }
    }

    internal fun receive(syncId: Int, index: Int, total: Int, data: String) {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            return // Shares the server's objects
        }
        if (syncId != receivingId || receivedChunks.size != total) {
            receivingId = syncId
            receivedChunks = arrayOfNulls(total)
        }
        receivedChunks[index] = data
        if (receivedChunks.any { it == null }) {
            return
        }

        val text = receivedChunks.joinToString("")
        receivedChunks = arrayOfNulls(0)
        receivingId = -1
        applySnapshot(text)
    }

    private fun applySnapshot(text: String) {
        val dto = JsonFormats.DataPack.decodeFromString(SnapshotDto.serializer(), text)

        val pools = dto.pools.map { poolDto ->
            Pool(poolDto.id, poolDto.weightMult, currency = poolDto.currency).also { pool ->
                for (entryDto in poolDto.entries) {
                    val entry = JsonFormats.Config.dynamicDecodeFromString(entryDto.json, PoolEntry.serializer())
                    entry.id = entryDto.id
                    pool.items.add(entry)
                }
            }
        }
        BountifulContent.populatePools(pools)
        BountifulContent.Decrees.clear()
        BountifulContent.Decrees.addAll(dto.decrees)
        Bountiful.LOGGER.info("Received ${dto.decrees.size} decrees and ${pools.size} pools from the server")
        (Minecraft.getInstance().gui.screen() as? io.ejekta.bountiful.client.AnalyzerScreen)?.refreshWidgets()
    }
}
