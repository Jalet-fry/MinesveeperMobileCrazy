package app.crazy_minesveeper.domain.model

data class Offset(
    val dx: Int, 
    val dy: Int, 
    val forcedColor: Any? = null
)

data class NeighborShape(
    val small: List<Offset>,
    val large: List<Offset>,
    val dogCount: List<Offset>? = null,
    val dogEffect: List<Offset>? = null
) {
    val dogCountActual: List<Offset> get() = dogCount ?: small
    val dogEffectActual: List<Offset> get() = dogEffect ?: large

    companion object {
        val DEFAULT = NeighborShape(
            small = listOf(
                Offset(-1, -1), Offset(-1, 0), Offset(-1, 1),
                Offset(0, -1), Offset(0, 1),
                Offset(1, -1), Offset(1, 0), Offset(1, 1)
            ),
            large = listOf(
                Offset(-2, -2), Offset(-2, -1), Offset(-2, 0), Offset(-2, 1), Offset(-2, 2),
                Offset(-1, -2), Offset(-1, -1), Offset(-1, 0), Offset(-1, 1), Offset(-1, 2),
                Offset(0, -2), Offset(0, -1), Offset(0, 1), Offset(0, 2),
                Offset(1, -2), Offset(1, -1), Offset(1, 0), Offset(1, 1), Offset(1, 2),
                Offset(2, -2), Offset(2, -1), Offset(2, 0), Offset(2, 1), Offset(2, 2)
            )
        )

        val KNIGHT = NeighborShape(
            small = listOf(
                Offset(-2, -1), Offset(-2, 1), Offset(-1, -2), Offset(-1, 2),
                Offset(1, -2), Offset(1, 2), Offset(2, -1), Offset(2, 1)
            ),
            large = DEFAULT.large
        )

        val ANTI_KNIGHT = NeighborShape(
            small = DEFAULT.small,
            large = KNIGHT.small.map { Offset(it.dx, it.dy, "g") }
        )
        
        val MAGNETS = NeighborShape(
            small = listOf(
                Offset(-1, 2, "r"), Offset(0, 2, "r"), Offset(1, 2, "r"),
                Offset(-1, 1, "r"), Offset(0, 1, "r"), Offset(1, 1, "r"),
                Offset(-1, -1, "b"), Offset(0, -1, "b"), Offset(1, -1, "b"),
                Offset(-1, -2, "b"), Offset(0, -2, "b"), Offset(1, -2, "b")
            ),
            large = DEFAULT.large
        )

        val NUCLEAR = NeighborShape(
            small = DEFAULT.small,
            large = ((-3..3).flatMap { dx ->
                (-3..3).map { dy -> Offset(dx, dy) }
            }.filterNot { it.dx == 0 && it.dy == 0 })
        )

        val REDSHIFT = NeighborShape(
            small = listOf(
                Offset(0, 1, "b"), Offset(0, -1, "b"), Offset(1, 0, "b"), Offset(-1, 0, "b"),
                Offset(-1, -1, "g"), Offset(-1, 1, "g"), Offset(1, -1, "g"), Offset(1, 1, "g"),
                Offset(0, 2, "r"), Offset(0, -2, "r"), Offset(2, 0, "r"), Offset(-2, 0, "r")
            ),
            large = DEFAULT.large
        )
    }
}
