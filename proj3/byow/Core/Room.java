package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.TETileWrapper;
import byow.TileEngine.Tileset;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Random;

public class Room implements Serializable {
    private static final int SMALLESTSIDE = 3;

    private TETileWrapper[][] worldWrappers;
    private Random RANDOM;
    private int roomNum;
    private int width;
    private int height;
    private int x;
    private int y;

    private int xWithLight;
    private int yWithLight;
    private boolean turn = true; // true => turn on; false => turn off

    public Room(TETileWrapper[][] worldWrappers, int roomNum, Long seed) {
        this.worldWrappers = worldWrappers;
        this.RANDOM = new Random(seed);
        this.roomNum = roomNum;
        this.width = randomRoomSide();
        this.height = randomRoomSide();
        this.x = randomRoomPosition(true);
        this.y = randomRoomPosition(false);
    }

    public TETileWrapper[][] makeRoom() {
        correctRoom();
        fillAllTilesInRoom();
        return worldWrappers;
    }

    private void fillAllTilesInRoom() {
        // The bottom of the room
        int level = height - 1;
        for (int i = x; i < x + width; i += 1) {
            fillOneTileInRoom(i, y - level, Tileset.WALL);
        }
        // The middle of the room
        level -= 1;
        while (level > 0) {
            for (int i = x; i < x + width; i += 1) {
                if (i == x || i == x + width - 1) {
                    fillOneTileInRoom(i, y - level, Tileset.WALL);
                } else {
                    fillOneTileInRoom(i, y - level, Tileset.FLOOR);
                }
            }
            level -= 1;
        }
        // The top of the room
        for (int i = x; i < x + width; i += 1) {
            fillOneTileInRoom(i, y, Tileset.WALL);
        }
    }

    private void fillOneTileInRoom(int x, int y, TETile tileType) {
        worldWrappers[x][y].setTile(tileType);
        if (tileType.equals(Tileset.WALL)) {
            worldWrappers[x][y].setIsAround();
        }
        worldWrappers[x][y].markRoom();
        worldWrappers[x][y].markTile(true);
        worldWrappers[x][y].setRoomNum(roomNum);
    }

    private void correctRoom() {
        while (!validRoom()) {
            width = randomRoomSide();
            height = randomRoomSide();
            x = randomRoomPosition(true);
            y = randomRoomPosition(false);
        }
    }

    private boolean validRoom() {
        int worldWidth = worldWrappers.length;
        // Choose left and up corner as (x,y)
        if (x + width >= worldWidth || y - height - 1 < 0) {
            return false;
        }
        // Check all tiles
        for (int i = x; i < x + width; i += 1) {
            for (int j = y; j > y - height; j -= 1) {
                if (!worldWrappers[i][j].getTile().equals(Tileset.NOTHING)) {
                    return false;
                }
            }
        }
        return true;
    }

    private int randomRoomSide() {
        // Choose a random number between 3 and 12 for the side length
        int side = RANDOM.nextInt(13);
        while (side < SMALLESTSIDE) {
            side = RANDOM.nextInt(13);
        }
        return side;
    }

    private int randomRoomPosition(boolean isXPosition) {
        int lengthLimit = worldWrappers.length;
        if (!isXPosition) {
            lengthLimit = worldWrappers[0].length;
        }
        return RANDOM.nextInt(lengthLimit);
    }

    private LinkedList<TETileWrapper> getDoorsInRoom() {
        LinkedList<TETileWrapper> doors = new LinkedList<>();
        // The left side
        for (int y = this.y - 1; y > this.y - height + 1; y -= 1) {
            doors.add(worldWrappers[x][y]);
        }
        // The right side
        for (int y = this.y - 1; y > this.y - height + 1; y -= 1) {
            doors.add(worldWrappers[x + width - 1][y]);
        }
        // The top side
        for (int x = this.x + 1; x < this.x + width - 1; x += 1) {
            doors.add(worldWrappers[x][y]);
        }
        // The bottom side
        for (int x = this.x + 1; x < this.x + width - 1; x += 1) {
            doors.add(worldWrappers[x][y - height + 1]);
        }
        return doors;
    }

    public LinkedList<TETileWrapper> getExitsInRoom() {
        LinkedList<TETileWrapper> doors = getDoorsInRoom();
        LinkedList<TETileWrapper> exits = new LinkedList<>();
        for (TETileWrapper door : doors) {
            TETileWrapper exit = getExitOfDoor(door.getX(), door.getY());
            if (exit != null) {
                exits.add(exit);
            }
        }
        return exits;
    }

    public TETileWrapper getRandomExitByDoorsInRoom() {
        LinkedList<TETileWrapper> doors = getDoorsInRoom();
        LinkedList<TETileWrapper> exits = new LinkedList<>();
        for (TETileWrapper door : doors) {
            TETileWrapper exit = getExitOfDoor(door.getX(), door.getY());
            if (exit != null) {
                exits.add(exit);
            }
        }
        int randomNum = RANDOM.nextInt(exits.size());
        TETileWrapper randomExit = exits.get(randomNum);
        int x = randomExit.getX();
        int y = randomExit.getY();
        // Set the exit as floor
        worldWrappers[x][y].setTile(Tileset.FLOOR);
        // Set the door as floor by exit
        setDoorAsFloorByExitInRoom(x, y);
        return worldWrappers[x][y];
    }

    public void setDoorAsFloorByExitInRoom(int x, int y) {
        // North: (x, y + 1)
        if (worldWrappers[x][y + 1].isRoom()) {
            worldWrappers[x][y + 1].setTile(Tileset.FLOOR);
        }
        // South: (x, y - 1)
        if (worldWrappers[x][y - 1].isRoom()) {
            worldWrappers[x][y - 1].setTile(Tileset.FLOOR);
        }
        // West: (x - 1, y)
        if (worldWrappers[x - 1][y].isRoom()) {
            worldWrappers[x - 1][y].setTile(Tileset.FLOOR);
        }
        // East: (x + 1, y)
        if (worldWrappers[x + 1][y].isRoom()) {
            worldWrappers[x + 1][y].setTile(Tileset.FLOOR);
        }
    }

    private TETileWrapper getExitOfDoor(int x, int y) {
        // North: (x, y + 1)
        if (isExitInRoom(x, y + 1)) {
            return worldWrappers[x][y + 1];
        }
        // South: (x, y - 1)
        if (isExitInRoom(x, y - 1)) {
            return worldWrappers[x][y - 1];
        }
        // West: (x - 1, y)
        if (isExitInRoom(x - 1, y)) {
            return worldWrappers[x - 1][y];
        }
        // East: (x + 1, y)
        if (isExitInRoom(x + 1, y)) {
            return worldWrappers[x + 1][y];
        }
        return null;
    }

    private boolean isExitInRoom(int x, int y) {
        int worldWidth = worldWrappers.length;
        int worldHeight = worldWrappers[0].length;
        return x < worldWidth - 1 && x > 0
                && y < worldHeight - 1 && y > 0
                && !worldWrappers[x][y].isRoom();
    }

    public void randomWallInWorld() {
        int worldWidth = worldWrappers.length;
        int worldHeight = worldWrappers[0].length;
        for (int x = 0; x < worldWidth; x += 1) {
            for (int y = 0; y < worldHeight; y += 1) {
                if (worldWrappers[x][y].getTile().equals(Tileset.WALL)) {
                    worldWrappers[x][y].setTile(randomTile());
                }
            }
        }
    }

    private TETile randomTile() {
        int tileNum = RANDOM.nextInt(4);
        switch (tileNum) {
            case 0: return Tileset.WALL;
            case 1: return Tileset.FLOWER;
            case 2: return Tileset.GRASS;
            case 3: return Tileset.SAND;
            default: return Tileset.NOTHING;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setXWithLight(int xWithLight) {
        this.xWithLight = xWithLight;
    }

    public void setYWithLight(int yWithLight) {
        this.yWithLight = yWithLight;
    }

    public int getXWithLight() {
        return xWithLight;
    }

    public int getYWithLight() {
        return yWithLight;
    }

    public int getRoomNum() {
        return roomNum;
    }

    public void setTurnOn(boolean turn) {
        this.turn = turn;
    }

    public boolean getTurnOn() {
        return turn;
    }
}
