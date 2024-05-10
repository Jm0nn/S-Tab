package com.ssafy.stab.data.note

import androidx.compose.runtime.snapshots.SnapshotStateList

data class PathData(
    val paths: List<PathInfo>
)

data class PathInfo(
    val penType: PenType,
    val strokeWidth: Float,
    val color: String,
    var coordinates: SnapshotStateList<Coordinate>,
)

data class Coordinate(
    val x: Float,
    val y: Float
)

data class UserPagePathInfo(
    val userName: String,
    var page: Int,
    val pathInfo: PathInfo
)
