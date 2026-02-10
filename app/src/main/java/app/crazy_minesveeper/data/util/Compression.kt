package app.crazy_minesveeper.data.util

object Compression {
    private const val B64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val b64Lookup = B64_CHARS.withIndex().associate { it.value to it.index }

    private sealed class Node {
        data class Branch(val left: Node, val right: Node) : Node()
        data class Repeat(val dist: Int, val len: Int) : Node()
        data class Number(val value: Int) : Node()
    }

    fun decompress(str: String, x: Int, y: Int): Array<IntArray> {
        val bits = mutableListOf<Int>()
        for (c in str) {
            val n = b64Lookup[c] ?: 0
            var b = 32
            while (b >= 1) {
                bits.add(if ((b and n) != 0) 1 else 0)
                b /= 2
            }
        }

        var pos = 0
        fun get(): Int {
            if (pos >= bits.size) return 0
            return bits[pos++]
        }

        fun parseByte(): Int {
            return get() * 128 + get() * 64 + get() * 32 + get() * 16 + get() * 8 + get() * 4 + get() * 2 + get()
        }

        fun parseTree(): Node {
            return if (get() == 1) {
                Node.Branch(parseTree(), parseTree())
            } else if (get() == 1) {
                Node.Repeat(parseByte() + 1, parseByte() + 3)
            } else {
                Node.Number(parseByte())
            }
        }

        val tree = try { parseTree() } catch(e: Exception) { Node.Number(0) }
        val boardLinear = mutableListOf<Int>()
        val totalCells = x * y

        while (boardLinear.size < totalCells && pos < bits.size) {
            var currentNode = tree
            while (currentNode is Node.Branch) {
                currentNode = if (get() == 0) currentNode.left else currentNode.right
            }
            
            when (currentNode) {
                is Node.Number -> boardLinear.add(currentNode.value)
                is Node.Repeat -> {
                    for (j in 0 until currentNode.len) {
                        if (boardLinear.size < totalCells) {
                            val targetIdx = boardLinear.size - currentNode.dist
                            if (targetIdx >= 0) {
                                boardLinear.add(boardLinear[targetIdx])
                            } else {
                                boardLinear.add(0)
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        // ВАЖНО: JS заполняет массив так: for(x...) for(y...) rawData[x][y] = nextValue
        val board = Array(x) { IntArray(y) }
        var linearIdx = 0
        for (i in 0 until x) {
            for (j in 0 until y) {
                if (linearIdx < boardLinear.size) {
                    board[i][j] = boardLinear[linearIdx++]
                } else {
                    board[i][j] = 0
                }
            }
        }
        return board
    }
}
