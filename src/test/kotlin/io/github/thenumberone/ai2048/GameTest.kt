package io.github.thenumberone.ai2048

import org.junit.Test
import org.junit.Assert.*

inline fun <reified T : Throwable> assertThrows(code: () -> Any?) {
    try {
        code()
    } catch (e: Throwable) {
        if (e is T) {
            return
        }
    }
    fail("Didn't throw ${T::class}")
}

class TileTests {
    @Test
    fun tileMustBeGreaterThanOne() {
        assertThrows<IllegalArgumentException> { Tile.Num(0) }
        assertThrows<IllegalArgumentException> { Tile.Num(1) }
        assertThrows<IllegalArgumentException> { Tile.Num(-3) }
    }
    
    @Test
    fun tileMustBePowerOfTwo() {
        assertThrows<IllegalArgumentException> { Tile.Num(3) }
        assertThrows<IllegalArgumentException> { Tile.Num(7) }
        assertThrows<IllegalArgumentException> { Tile.Num(12) }
        assertThrows<IllegalArgumentException> { Tile.Num(2047) }
        
        Tile.Num(2)
        Tile.Num(4)
        Tile.Num(8)
        Tile.Num(2048)
    }
    
    @Test
    fun testEqual() {
        assertEquals(Tile.Num(2), Tile.Num(2))
        val four = Tile.Num(4)
        assertEquals(four, four)
        assertNotEquals(Tile.Num(2), Tile.Num(4))
        assertNotEquals(Tile.Num(2048), null)
        assertNotEquals(Tile.Num(2048), 2048)
    }
    
    @Test
    fun testMerge() {
        assertThrows<IllegalArgumentException> { Tile.Num(2).merge(Tile.Num(4)) }
        
        val four = Tile.Num(2).merge(Tile.Num(2))
        
        assertEquals(4, four.num)
    }
    
    @Test
    fun fromNumReturnsEmptyForZero() {
        assertEquals(Tile.Empty, Tile.fromNum(0))
    }
    
    @Test
    fun fromNumReturnsTilesForNonZero() {
        assertEquals(Tile.Num(2048), Tile.fromNum(2048))
        assertEquals(Tile.Num(2), Tile.fromNum(2))
    }
}


class GameTest {

    
    @Test
    fun gameMustHaveSquareParameters() {
        assertThrows<IllegalArgumentException> {
            Game(arrayOf(intArrayOf(1, 2)))
        }
        
        Game(arrayOf(
            intArrayOf(2, 2),
            intArrayOf(8, 0)))
        
        assertThrows<IllegalArgumentException> {
            Game(arrayOf(
                intArrayOf(1, 2),
                intArrayOf(3, 4),
                intArrayOf(5)))
        }
        
        Game(arrayOf())
        
        assertThrows<IllegalArgumentException> {
            Game(
                2, 4, 8,
                0,    2,
                4, 0, 128)
        }
        
        Game(
            2, 4, 8,
            0, 0, 2,
            4, 0, 128)
            
        Game(
            256, 512, 2048, 8192,
            64, 8, 4, 0,
            2, 4, 2, 0,
            8, 0, 0, 0)
            
        Game(
            0, 1024, 2048, 32768, 65536,
            0, 0, 2, 8, 4,
            0, 0, 0, 4, 8,
            0, 0, 0, 2, 2,
            0, 0, 2, 0, 0)
            
        Game(
            32, 512, 512, 2048, 4096, 16384, 256, 2,
            8, 32, 16, 4, 2, 0, 0, 0,
            16, 8, 4, 0, 0, 0, 0, 0,
            2, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            2, 0, 0, 0, 0, 0, 0, 0)
    }
    
    @Test
    fun slideWorks() {
        // 256, 512, 2048, 8192,
        // 64, 8, 4, 0,
        // 2, 4, 2, 0,
        // 8, 0, 0, 0
        var g = Game(
            256, 512, 2048, 8192,
            64, 8, 4, 0,
            2, 4, 2, 0,
            8, 0, 0, 0)
        
        g = g.slide(SlideDirection.RIGHT)
        var expected = Game(
            256, 512, 2048, 8192,
            0, 64, 8, 4,
            0, 2, 4, 2,
            0, 0, 0, 8)
        assertEquals(expected, g)
        
        g = g.placeTile(3, 1, Tile.Num(2))
        g = g.slide(SlideDirection.DOWN)
        expected = Game(
            0, 0, 0, 8192,
            0, 512, 2048, 4,
            0, 64, 8, 2,
            256, 4, 4, 8)
        assertEquals(expected, g)
        
        g = g.slide(SlideDirection.LEFT)
        expected = Game(
            8192, 0, 0, 0,
            512, 2048, 4, 0,
            64, 8, 2, 0,
            256, 8, 8, 0)
        assertEquals(expected, g)
            
        g = g.slide(SlideDirection.UP)
        expected = Game(
            8192, 2048, 4, 0,
            512, 16, 2, 0,
            64, 0, 8, 0,
            256, 0, 0, 0)
        assertEquals(expected, g)
    }

    @Test
    fun mergingWorks() {
        var g = Game(
            2, 2, 2, 2, 2,
            2, 2, 2, 2, 0,
            2, 2, 2, 0, 0, 
            4, 0, 2, 2, 0,
            4, 0, 8, 4, 0)
            
        g = g.slide(SlideDirection.RIGHT)
        
        var expected = Game(
            0, 0, 2, 4, 4,
            0, 0, 0, 4, 4,
            0, 0, 0, 2, 4,
            0, 0, 0, 4, 4,
            0, 0, 4, 8, 4)
            
        assertEquals(expected, g) 
    }
    
    @Test
    fun slideDoesntWorkIfNothingMoves() {
        var g = Game(
            2, 4,
            2, 4)
        
        assertThrows<IllegalArgumentException> { g.slide(SlideDirection.LEFT) }
    }
    
    @Test
    fun nextTurnIsSlideWorks() {
        var g = Game(
            0, 2,
            0, 0, nextTurnIsSlide = false)
            
        assertThrows<IllegalStateException> { g.slide(SlideDirection.LEFT) }
        
        g = g.copy(nextTurnIsSlide = true).slide(SlideDirection.LEFT)
        assertEquals(false, g.nextTurnIsSlide)
            
        assertThrows<IllegalStateException> { g.slide(SlideDirection.RIGHT) }
        
        g = g.placeTile(1, 1, 2)
        assertEquals(true, g.nextTurnIsSlide)
        g = g.slide(SlideDirection.LEFT)
        assertEquals(false, g.nextTurnIsSlide)
    }
    
    @Test
    fun canOnlyPlaceOnEmptyTiles() {
        var g = Game(
            0, 0,
            0, 0)
            
        g = g.placeTile(1, 1, 2)
        
        assertThrows<IllegalArgumentException> { g.placeTile(1, 1, 2) }
        
        g = g.slide(SlideDirection.UP)
        g = g.placeTile(1, 1, 2)
        
        assertThrows<IllegalArgumentException> { g.placeTile(0, 1, 2) }
    }
}