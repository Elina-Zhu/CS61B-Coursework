package byow.lab12;

import net.sf.saxon.functions.PositionAndLast;
import org.junit.Test;
import static org.junit.Assert.*;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.util.Random;

/**
 * Draws a world consisting of hexagonal regions.
 */
public class HexWorld {
    private static final int WIDTH = 50;
    private static final int HEIGHT = 50;

    private static final long SEED = 2873123;
    private static final Random RANDOM = new Random(SEED);

    /**
     * Draw a row of tiles to the board, starting from a given position.
     */
    public static void drawRow(TETile[][] tiles, Position p, TETile tile, int length) {
        for (int dx = 0; dx < length; dx++) {
            tiles[p.x + dx][p.y] = tile;
        }
    }

    public static void addHexagonHelper(TETile[][] tiles, Position p, TETile tile, int b, int t) {
        // Draw this row
        Position startOfRow = p.shift(b, 0);
        drawRow(tiles, startOfRow, tile, t);

        // Draw the remaining rows recursively
        if (b > 0) {
            Position nextP = p.shift(0, -1);
            addHexagonHelper(tiles, nextP, tile, b - 1, t + 2);
        }

        // Draw this row again for symmetry
        Position startOfReflectedRow = startOfRow.shift(0, -(2 * b + 1));
        drawRow(tiles, startOfReflectedRow, tile, t);
    }

    /**
     * Add a hexagon to the world at position P of size SIZE.
     */
    public static void addHexagon(TETile[][] tiles, Position p, TETile tile, int size) {
        if (size < 2) {
            return;
        }

        addHexagonHelper(tiles, p, tile, size - 1, size);
    }

    /**
     * Add a column of NUM hexagons.
     */
    public static void addHexColumn(TETile[][] tiles, Position p, int size, int num) {
        if (num < 1) {
            return;
        }

        // Draw this hexagon
        addHexagon(tiles, p, randomTile(), size);

        // Draw n - 1 hexagons below it
        if (num > 1) {
            Position bottomNeighbor = getBottomNeighbor(p, size);
            addHexColumn(tiles, bottomNeighbor, size, num - 1);
        }
    }


    /**
     * Fill the given 2D array of tiles with blank tiles.
     */
    public static void fillWithBlankTiles(TETile[][] tiles) {
        int height = tiles[0].length;
        int width = tiles.length;
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                tiles[x][y] = Tileset.NOTHING;
            }
        }
    }

    /**
     * Picks a RANDOM tile.
     */
    private static TETile randomTile() {
        int tileNum = RANDOM.nextInt(5);
        switch (tileNum) {
            case 0: return Tileset.MOUNTAIN;
            case 1: return Tileset.GRASS;
            case 2: return Tileset.TREE;
            case 3: return Tileset.SAND;
            case 4: return Tileset.FLOWER;
            default: return Tileset.NOTHING;
        }
    }

    public static Position getTopRightNeighbor(Position p, int n) {
        return p.shift(2 * n - 1, n);
    }

    public static Position getBottomRightNeighbor(Position p, int n) {
        return p.shift(2 * n - 1, -n);
    }

    public static Position getBottomNeighbor(Position p, int n) {
        return p.shift(0, -2 * n);
    }

    private static class Position {
        int x;
        int y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Position shift(int dx, int dy) {
            return new Position(this.x + dx, this.y + dy);
        }
    }

    /**
     * Draw the hexagonal world.
     */
    public static void drawWorld(TETile[][] tiles, Position p, int hexSize, int tessSize) {
        // Draw the first hexagon
        addHexColumn(tiles, p, hexSize, tessSize);

        // Expand up and to the right
        for (int i = 1; i < tessSize; i++) {
            p = getTopRightNeighbor(p, hexSize);
            addHexColumn(tiles, p, hexSize, tessSize + i);
        }

        // Expand down and to the right
        for (int i = tessSize - 2; i >= 0; i--) {
            p = getBottomRightNeighbor(p, hexSize);
            addHexColumn(tiles, p, hexSize, tessSize + i);
        }
    }

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        TETile[][] world = new TETile[WIDTH][HEIGHT];
        fillWithBlankTiles(world);
        Position anchor = new Position(10, 35); // Picked arbitrarily
        drawWorld(world, anchor, 3, 3);

        ter.renderFrame(world);
    }
}
