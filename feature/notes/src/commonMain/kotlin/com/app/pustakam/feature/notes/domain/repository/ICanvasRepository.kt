package com.app.pustakam.feature.notes.domain.repository

import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.richtext.master.model.CanvasDocument
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasRect
import com.app.pustakam.core.richtext.master.model.Viewport

interface ICanvasRepository {

    suspend fun load(noteId: String): Result<BaseResponse<CanvasDocument>, Error>

    suspend fun loadVisible(
        noteId: String,
        rect: CanvasRect
    ): Result<BaseResponse<CanvasDocument>, Error>

    suspend fun save(noteId: String, node: CanvasNode): Result<BaseResponse<CanvasNode>, Error>

    suspend fun saveAll(
        noteId: String,
        nodes: List<CanvasNode>
    ): Result<BaseResponse<List<CanvasNode>>, Error>

    suspend fun move(nodeId: String, x: Float, y: Float): Result<BaseResponse<Boolean>, Error>

    suspend fun resize(
        nodeId: String,
        width: Float,
        height: Float
    ): Result<BaseResponse<Boolean>, Error>

    suspend fun raise(nodeId: String, z: Int): Result<BaseResponse<Boolean>, Error>

    suspend fun rename(nodeId: String, name: String): Result<BaseResponse<Boolean>, Error>

    suspend fun remove(nodeId: String): Result<BaseResponse<Boolean>, Error>

    suspend fun removeAll(noteId: String): Result<BaseResponse<Boolean>, Error>

    suspend fun count(noteId: String): Result<BaseResponse<Int>, Error>

    suspend fun nodeForContent(contentId: String): Result<BaseResponse<CanvasNode>, Error>

    suspend fun pruneOrphans(): Result<BaseResponse<Boolean>, Error>

    suspend fun loadViewport(noteId: String): Result<BaseResponse<Viewport>, Error>

    suspend fun saveViewport(
        noteId: String,
        viewport: Viewport
    ): Result<BaseResponse<Viewport>, Error>
}
