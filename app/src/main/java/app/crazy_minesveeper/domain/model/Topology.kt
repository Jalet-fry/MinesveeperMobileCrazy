package app.crazy_minesveeper.domain.model

data class Topology(
    val xWrap: Int = 0,
    val yWrap: Int = 0
) {
    fun wrapCoordinates(x: Int, y: Int, width: Int, height: Int): Pair<Int, Int>? {
        var nx = x
        var ny = y
        if (nx < 0 && xWrap > 0) {
            nx += width
            if (xWrap > 1) ny = height - 1 - ny
        } else if (nx >= width && xWrap > 0) {
            nx -= width
            if (xWrap > 1) ny = height - 1 - ny
        }
        if (ny < 0 && yWrap > 0) {
            ny += height
            if (yWrap > 1) nx = width - 1 - nx
        } else if (ny >= height && yWrap > 0) {
            ny -= height
            if (yWrap > 1) nx = width - 1 - nx
        }
        if (nx !in 0 until width || ny !in 0 until height) return null
        return nx to ny
    }

    fun arePositionsEqual(x1: Int, y1: Int, x2: Int, y2: Int, width: Int, height: Int): Boolean {
        val p1 = wrapCoordinates(x1, y1, width, height) ?: (x1 to y1)
        val p2 = wrapCoordinates(x2, y2, width, height) ?: (x2 to y2)
        return p1.first == p2.first && p1.second == p2.second
    }
}
