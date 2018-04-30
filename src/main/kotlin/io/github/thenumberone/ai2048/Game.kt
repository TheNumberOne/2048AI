package io.github.thenumberone.ai2048

import kotlin.math.*
import java.util.Objects
import java.util.Random

enum class SlideDirection {
    UP, DOWN, LEFT, RIGHT
}


private fun isPowerOfTwo(n: Int): Boolean {
    //a power of two is 1000....
    //a power of two minus one is 01111...
    //so n & (n - 1) = 0 for all powers of two
    //it also equals 0 for 0 and MIN_INT so we add n > 0
    return (n and (n - 1) == 0) && (n > 0)
}

private fun unflatten(nums: IntArray): Array<Array<Tile>> {
    val width = sqrt(nums.size.toDouble()).toInt()
    require(width * width == nums.size) { "$nums.size elements isn't square" }
    
    return Array(width) { i -> 
        Array(width) { j -> 
            Tile.fromNum(nums[i * width + j])
        }
    }
}

sealed class Tile {
    companion object {
        fun fromNum(num: Int) = if (num == 0) Empty else Num(num)
    }
    
    object Empty : Tile() {
        override fun toString(): String = "."
    }
    data class Num(val num: Int) : Tile() {
        init {
            require(num >= 2) { "$num is not greater or equal to two" }
            require(isPowerOfTwo(num)) { "$num is not a power of two" }
        }
        
        fun merge(other: Num): Num {
            require(other == this)
            
            return Num(this.num + other.num)
        }
        
        override fun toString(): String = "$num"
    }
}

data class Game constructor(
    val width: Int,
    val tiles: Array<Array<Tile>>,
    val nextTurnIsSlide: Boolean? = null)
{
    init {
        require(tiles.size == width) { "width doesn't match up. given width: $width. computed width: $tiles.size" }
        require(tiles.all { it.size == width }) { "width doesn't match up. expected width: $width. widths: ${tiles.map { it.size }}" }
    }
    
    constructor(width: Int, nextTurnIsSlide: Boolean? = null): this(
        width,
        Array(width) { Array<Tile>(width) { Tile.Empty } },
        nextTurnIsSlide)
    {
    }
    
    constructor(tiles: Array<IntArray>, nextTurnIsSlide: Boolean? = null): this(
        tiles.size,
        Array(tiles.size) { i -> 
            require(tiles[i].size == tiles.size) { "outside width: $tiles.size doesn't match inside width: ${tiles.map { it.size }}" }
            Array(tiles.size) { j -> 
                Tile.fromNum(tiles[i][j]) 
            }
        },
        nextTurnIsSlide)
    {
    }
    
    /**
     * Unflattens the nums into the array. Fills in left to right and then up to down.
     */
    constructor(vararg nums: Int, nextTurnIsSlide: Boolean? = null): this(
        sqrt(nums.size.toDouble()).toInt(),
        unflatten(nums),
        nextTurnIsSlide)
    {
    }
    
    operator fun get(row: Int, col: Int) : Tile {
        return tiles[row][col]
    }
    
    //Flips this game diagonally if slide direction is up or down.
    //Flips this game horizontally if slide direction is right or down.
    operator fun get(direction: SlideDirection, row: Int, col: Int) : Tile {
        return when (direction) {
            SlideDirection.DOWN -> tiles[width - 1 - col][row]
            SlideDirection.UP -> tiles[col][row]
            SlideDirection.LEFT -> tiles[row][col]
            SlideDirection.RIGHT -> tiles[row][width - 1 - col]
        }
    }
    
    fun canSlide(direction: SlideDirection): Boolean {
        for (i in 0 until width) {
            for (j in 1 until width) {
                val prev = this[direction, i, j - 1]
                val square = this[direction, i, j]
                
                if (square != Tile.Empty && (prev == Tile.Empty || prev == square)) {
                    //Squares can slide into squares equivalent to themselves or an empty square.
                    return true
                }
            }
        }
        
        return false
    }
    
    fun canSlideAtAll(): Boolean {
        return SlideDirection.values().any { canSlide(it) }
    }
    
    fun slide(direction: SlideDirection): Game {
        check(nextTurnIsSlide != false) { "Next turn can't be a slide" }
        require(canSlide(direction)) { "The grid can't slide $direction!" }
        
        //start scanning from the slide direction
        //we use this function so we can ignore slide direction
        
        operator fun Array<Array<Tile>>.set(i: Int, j: Int, tile: Tile) {
            when (direction) {
                SlideDirection.DOWN -> this[width - 1 - j][i] = tile
                SlideDirection.UP -> this[j][i] = tile
                SlideDirection.LEFT -> this[i][j] = tile
                SlideDirection.RIGHT -> this[i][width - 1 - j] = tile
            }
        }
        
        val newTiles = Array(width) { Array<Tile>(width) { Tile.Empty } }
        
        //note that we translate coordinates so we can assume we are sliding left
        //i is column
        for (i in 0 until width) {
            //Index of next square to be copied over
            var j = 0
            
            //Index of square after j
            var k: Int
            
            //Index to be put in newTiles
            var newJ = 0
            
            while (true) {
                //j is first tile
                while (j < width && this[direction, i, j] == Tile.Empty) {
                    j += 1
                }
                
                //If scanned to the end, go to next row
                if (j == width) break
                
                //k is next tile after j
                k = j + 1
                while (k < width && this[direction, i, k] == Tile.Empty) {
                    k += 1
                }
                
                //If scanned to end, place tile on j and then go to next row.
                if (k == width) {
                    newTiles[i, newJ] = this[direction, i, j]
                    break
                }
                
                //If found two tiles, check to see if they can be merged.
                var firstTile = this[direction, i, j] as Tile.Num
                var nextTile = this[direction, i, k] as Tile.Num
                if (firstTile == nextTile) {
                    newTiles[i, newJ] = firstTile.merge(nextTile)
                    //Skip merged tile
                    j = k + 1
                } else {
                    newTiles[i, newJ] = firstTile
                    //Next tile to be placed was already found
                    j = k
                }
                
                //We for sure placed a tile.
                newJ += 1
            }
        }
        
        return copy(tiles = newTiles, nextTurnIsSlide = nextTurnIsSlide?.not())
    }
    
    fun placeTile(row: Int, col: Int, tile: Tile.Num): Game {
        check(nextTurnIsSlide != true) { "Next turn is slide" }
        require(this[row, col] is Tile.Empty) { "You can only place on empty tiles" }
        require(tile.num == 2 || tile.num == 4) { "Tile must be a 2 or 4" }
        
        val newTiles = Array(width) { tiles[it].copyOf() }
        newTiles[row][col] = tile
        
        return copy(tiles = newTiles, nextTurnIsSlide = nextTurnIsSlide?.not())
    }
    
    fun placeTile(row: Int, col: Int, tile: Int) = placeTile(row, col, Tile.Num(tile))
    
    fun placeTile(random: Random): Game {
        var chosenI = 0
        var chosenJ = 0
        var seenSoFar = 0
        
        for (i in 0 until width) {
            for (j in 0 until width) {
                if (this[i, j] == Tile.Empty) {
                    seenSoFar += 1
                    if (random.nextInt(seenSoFar) == 0) {
                        chosenI = i
                        chosenJ = j
                    }
                }
            }
        }
        val tile = if (random.nextInt(10) == 0) 4 else 2
        
        return placeTile(chosenI, chosenJ, tile)
    }
    
    fun doMove(move: Move) = when(move) {
        is Slide -> slide(move.direction)
        is PlaceTile -> placeTile(move.row, move.col, move.tile) 
    }
    
    override fun toString(): String {
        return "Game(width = $width, tiles = ${tiles.contentDeepToString()}, nextTurnIsSlide = $nextTurnIsSlide)"
    }
    
    override fun equals(other: Any?): Boolean {
        return when(other) {
            null, !is Game -> false
            other === this -> true
            else -> {
                return width == other.width && tiles.contentDeepEquals(other.tiles) && nextTurnIsSlide == other.nextTurnIsSlide
            }
        }
    }
    
    override fun hashCode(): Int {
        return Objects.hash(width, tiles.contentDeepHashCode(), nextTurnIsSlide)
    }
}

sealed class Move
data class Slide(val direction: SlideDirection): Move()
data class PlaceTile(val row: Int, val col: Int, val tile: Tile.Num): Move()
{
    init {
        require(tile.num == 2 || tile.num == 4) { "Tile must be 2 or 4. Not $tile.num" }
    }
    
    constructor(row: Int, col: Int, tile: Int): this(row, col, Tile.Num(tile))
    {
    }
}