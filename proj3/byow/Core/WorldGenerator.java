package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.TETileWrapper;
import byow.TileEngine.Tileset;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class WorldGenerator implements Serializable {
    private static final int ROOMNUM = 35;
    private final int N;
    private Long seed;
    private Random random;


    // Set the worldWrappers
    private TETileWrapper[][] worldWrappers;
    private final int width;
    private final int height;

    // Use the A* method to find the shortest path
    private int source;
    private int target;
    private final int[] edgeTo;
    private final int[] distTo;
    private boolean targetFound;

    private boolean isFirst;
    private boolean alwaysCenterTarget; // Always use center as target

    private TETileWrapper avatar;
    private LinkedList<Room> rooms = new LinkedList<>();
    private boolean turn = true; // true -> turn on; false -> turn off
    private LinkedList<Room> randomRooms = new LinkedList<>();

    public WorldGenerator(Long seed, TETile[][] world, boolean alwaysCenterTarget) {
        this.seed = seed;
        this.random = new Random(seed);

        this.width = world.length;
        this.height = world[0].length;
        this.N = Math.max(width, height);
        this.edgeTo = new int[V()];
        this.distTo = new int[V()];
        this.targetFound = false;

        // Initialise the world
        this.worldWrappers = new TETileWrapper[width][height];
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                worldWrappers[x][y] = new TETileWrapper(Tileset.NOTHING, x, y);
            }
        }
        reset();

        this.source = 0;
        setTarget(width / 2, height / 2);
        this.isFirst = true;
        this.alwaysCenterTarget = alwaysCenterTarget;
    }

    public TETile[][] generateWorld() {
        connectRooms();
        fillSomeWalls();
        randomAvatar();
        createRandomLightInRooms();
        return getWorldByWorldWrappers();
    }

    public TETile[][] moveAvatarThenGenerateWorld(String direction) {
        moveAvatar(direction);
        return getWorldByWorldWrappers();
    }

    private TETile[][] getWorldByWorldWrappers() {
        TETile[][] world = new TETile[width][height];
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                world[x][y] = worldWrappers[x][y].getTile();
            }
        }
        return world;
    }

    private void reset() {
        for (int i = 0; i < V(); i += 1) {
            edgeTo[i] = Integer.MAX_VALUE;
            distTo[i] = Integer.MAX_VALUE;
        }
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                if (!worldWrappers[x][y].isRoom()) {
                    worldWrappers[x][y].markTile(false);
                }
            }
        }
    }

    private void connectRooms() {
        for (int i = 0; i < ROOMNUM; i += 1) {
            Room room = new Room(worldWrappers, i, seed);
            rooms.add(room);
            room.makeRoom();
            reset();
            connectRoomToTarget(room);
        }
    }

    private void connectRoomToTarget(Room room) {
        if (alwaysCenterTarget) {
            connectRoomToTargetByCenterTarget(room);
        } else {
            connectRoomToTargetByRandomTarget(room);
        }
    }

    private void connectRoomToTargetByCenterTarget(Room room) {
        targetFound = false;
        TETileWrapper randomExit = room.getRandomExitByDoorsInRoom();
        setSource(randomExit.getX(), randomExit.getY());
        astar();
        buildHallwayByShortestPath(target);
    }

    private void connectRoomToTargetByRandomTarget(Room room) {
        if (isFirst) {
            setFirstTargetAndSource(room);
            astar();
        } else {
            targetFound = false;
            setRandomTargetAndSource(room);
            astar();
        }
        buildHallwayByShortestPath(target);
    }

    private void setFirstTargetAndSource(Room room) {
        // Set first target
        int randomNum = random.nextInt((width - 2) * (height - 2));
        int num = 0;
        for (int x = 1; x < width - 1; x += 1) {
            for (int y = 1; y < height - 1; y += 1) {
                if (num == randomNum) {
                    setTarget(x, y);
                    worldWrappers[x][y].setTile(Tileset.FLOOR);
                    isFirst = false;
                    // Set first source
                    TETileWrapper randomExit = room.getRandomExitByDoorsInRoom();
                    setSource(randomExit.getX(), randomExit.getY());
                    return;
                }
                num += 1;
            }
        }
    }

    private void setRandomTargetAndSource(Room room) {
        LinkedList<TETileWrapper> notRoomButFloors = notRoomButFloors();
        LinkedList<TETileWrapper> exits = room.getExitsInRoom();
        int randomNum1, randomNum2;
        // Set random target
        randomNum1 = random.nextInt(notRoomButFloors.size());
        TETileWrapper tileWrapper = notRoomButFloors.get(randomNum1);
        setTarget(tileWrapper.getX(), tileWrapper.getY());
        // Set ran = dom source
        randomNum2 = random.nextInt(exits.size());
        TETileWrapper randomExit = exits.get(randomNum2);
        int x = randomExit.getX();
        int y = randomExit.getY();
        setSource(x, y);
        room.setDoorAsFloorByExitInRoom(x, y);
    }

    private LinkedList<TETileWrapper> notRoomButFloors() {
        LinkedList<TETileWrapper> notRoomButFloors = new LinkedList<>();
        for (int x = 1; x < width - 1; x += 1) {
            for (int y = 1; y < height - 1; y += 1) {
                if (!worldWrappers[x][y].isRoom() && worldWrappers[x][y].getTile().equals(Tileset.FLOOR)) {
                    notRoomButFloors.add(worldWrappers[x][y]);
                }
            }
        }
        return notRoomButFloors;
    }

    /** Estimates the distance from v to the target. */
    private int h(int v) {
        return Math.abs(toX(v) - toX(target)) + Math.abs(toY(v) - toY(target));
    }

    /** Returns the vertex estimated to be closest to the target. */
    private int findMinimumUnmarked(Queue<Integer> queue) {
        int minimumVertex = queue.peek();
        int minimumPath = distTo[minimumVertex] + h(minimumVertex);
        for (int vertex : queue) {
            if (distTo[vertex] + h(vertex) < minimumPath) {
                minimumVertex = vertex;
            }
        }
        return minimumVertex;
    }

    private void astar() {
        Queue<Integer> fringe = new ArrayDeque<>();
        fringe.add(source);
        setMarkInWorldWrappers(source, true);
        while (!fringe.isEmpty()) {
            int v = findMinimumUnmarked(fringe);
            fringe.remove(v);
            for (TETileWrapper tileWrapper : tileNeighbors(v)) {
                if (!tileWrapper.isMarked()) {
                    int w = xyTo1D(tileWrapper.getX(), tileWrapper.getY());
                    fringe.add(w);
                    setMarkInWorldWrappers(w, true);
                    edgeTo[w] = v;
                    distTo[w] = distTo[v] + 1;
                    if (w == target) {
                        targetFound = true;
                    }
                    if (targetFound) {
                        return;
                    }
                }
            }
        }
    }

    private LinkedList<TETileWrapper> tileNeighbors(int v) {
        LinkedList<TETileWrapper> neighbors = new LinkedList<>();
        int x = toX(v);
        int y = toY(v);
        // North: (x, y + 1)
        if (isNeighbor(x, y + 1)) {
            neighbors.add(worldWrappers[x][y + 1]);
        }
        // South: (x, y - 1)
        if (isNeighbor(x, y - 1)) {
            neighbors.add(worldWrappers[x][y - 1]);
        }
        // West: (x - 1, y)
        if (isNeighbor(x - 1, y)) {
            neighbors.add(worldWrappers[x - 1][y]);
        }
        // East: (x + 1, y)
        if (isNeighbor(x + 1, y)) {
            neighbors.add(worldWrappers[x + 1][y]);
        }
        return neighbors;
    }

    private boolean isNeighbor(int x, int y) {
        return x < width - 1 && x > 0
                && y < height - 1 && y > 0
                && !worldWrappers[x][y].isRoom();
    }

    private void buildHallwayByShortestPath(int v) {
        int x = toX(v);
        int y = toY(v);
        worldWrappers[x][y].setTile(Tileset.FLOOR);
        // Set the tiles in four directions as wall
        // North: (x, y + 1)
        if (isHallwayWall(x, y + 1)) {
            worldWrappers[x][y + 1].setTile(Tileset.WALL);
        }
        // South: (x, y - 1)
        if (isHallwayWall(x, y - 1)) {
            worldWrappers[x][y - 1].setTile(Tileset.WALL);
        }
        // West: (x - 1, y)
        if (isHallwayWall(x - 1, y)) {
            worldWrappers[x - 1][y].setTile(Tileset.WALL);
        }
        // East: (x + 1, y)
        if (isHallwayWall(x + 1, y)) {
            worldWrappers[x + 1][y].setTile(Tileset.WALL);
        }
        if (v == source) {
            return;
        }
        buildHallwayByShortestPath(edgeTo[v]);
    }

    private boolean isHallwayWall(int x, int y) {
        return x < width && x >= 0 && y < height && y >= 0
                && !worldWrappers[x][y].isRoom()
                && !worldWrappers[x][y].getTile().equals(Tileset.FLOOR);
    }

    private void setSource(int x, int y) {
        this.source = xyTo1D(x, y);
    }

    private void setTarget(int x, int y) {
        this.target = xyTo1D(x, y);
    }

    private void fillSomeWalls() {
        for (int x = 0; x <= width - 3; x += 1) {
            for (int y = 2; y <= height - 1; y += 1) {
                if (!worldWrappers[x][y].isRoom()
                        && worldWrappers[x][y].getTile().equals(Tileset.FLOOR)) {
                    int floorCount = 0;
                    for (int i = x; i <= x + 2; i += 1) {
                        for (int j = y; j >= y - 2; j -= 1) {
                            if (worldWrappers[i][j].getTile().equals(Tileset.FLOOR)) {
                                floorCount += 1;
                            }
                        }
                    }
                    if (floorCount == 9) {
                        worldWrappers[x + 1][y - 1].setTile(Tileset.WALL);
                    }
                }
            }
        }
    }

    // Choose a random tile and set it as avatar
    private void randomAvatar() {
        LinkedList<TETileWrapper> floors = new LinkedList<>();
        for (int x = 0; x <= width - 1; x += 1) {
            for (int y = 0; y <= height - 1; y += 1) {
                if (worldWrappers[x][y].getTile().equals(Tileset.FLOOR)) {
                    floors.add(worldWrappers[x][y]);
                }
            }
        }
        int randomNum = random.nextInt(floors.size());
        TETileWrapper avatarTemp = floors.get(randomNum);
        worldWrappers[avatarTemp.getX()][avatarTemp.getY()].setTile(Tileset.AVATAR);
        this.avatar = worldWrappers[avatarTemp.getX()][avatarTemp.getY()];
    }

    private void moveAvatar(String direction) {
        // W -> up
        if (direction.equals("W") && validDirection("W")) {
            moveTo("W");
        }
        // S -> down
        if (direction.equals("S") && validDirection("S")) {
            moveTo("S");
        }
        // A -> left
        if (direction.equals("A") && validDirection("A")) {
            moveTo("A");
        }
        // D -> right
        if (direction.equals("D") && validDirection("D")) {
            moveTo("D");
        }
        keepLightingWithAvatarInRoom();
    }

    private boolean validDirection(String direction) {
        int x = avatar.getX();
        int y = avatar.getY();
        TETileWrapper tileWrapper = switch (direction) {
            case "W" -> worldWrappers[x][y + 1];
            case "S" -> worldWrappers[x][y - 1];
            case "A" -> worldWrappers[x - 1][y];
            case "D" -> worldWrappers[x + 1][y];
            default -> null;
        };
        return tileWrapper.getTile().equals(Tileset.FLOOR)
                || tileWrapper.getTile().description().equals(Tileset.LIGHTS[0].description());
    }

    private void moveTo(String direction) {
        int x = avatar.getX();
        int y = avatar.getY();
        switch (direction) {
            case "W":
                worldWrappers[x][y].setTile(Tileset.FLOOR);
                worldWrappers[x][y + 1].setTile(Tileset.AVATAR);
                this.avatar = worldWrappers[x][y + 1];
                break;
            case "S":
                worldWrappers[x][y].setTile(Tileset.FLOOR);
                worldWrappers[x][y - 1].setTile(Tileset.AVATAR);
                this.avatar = worldWrappers[x][y - 1];
                break;
            case "A":
                worldWrappers[x][y].setTile(Tileset.FLOOR);
                worldWrappers[x - 1][y].setTile(Tileset.AVATAR);
                this.avatar = worldWrappers[x - 1][y];
                break;
            case "D":
                worldWrappers[x][y].setTile(Tileset.FLOOR);
                worldWrappers[x + 1][y].setTile(Tileset.AVATAR);
                this.avatar = worldWrappers[x + 1][y];
                break;
        }
    }

    private void createRandomLightInRooms() {
        for (Room room : rooms) {
            int x = room.getX();
            int y = room.getY();
            int wight = room.getWidth();
            int height = room.getHeight();
            int randomNum = random.nextInt((wight - 2) * (height - 2));
            int count = 0;
            for (int i = x + 1; i <= x + wight - 2; i += 1) {
                for (int j = y - 1; j >= y - height + 2; j -= 1) {
                    if (count == randomNum) {
                        room.setXWithLight(i);
                        room.setYWithLight(j);
                        turnOnOrOffLightInRoom(i, j, room.getRoomNum(), room.getTurnOn());
                    }
                    count += 1;
                }
            }
        }
    }

    private void keepLightingWithAvatarInRoom() {
        if (avatar.isRoom()) {
            Room room = rooms.get(avatar.getRoomNum());
            turnOnOrOffLightInRoom(room.getXWithLight(), room.getYWithLight(), room.getRoomNum(), room.getTurnOn());
        }
    }

    public TETile[][] turnOnOrOffLightInRooms() {
        turn = !turn;
        if (!turn) {
            resetRandomRooms();
        }
        for (Room room : randomRooms) {
            room.setTurnOn(turn);
            turnOnOrOffLightInRoom(room.getXWithLight(), room.getYWithLight(), room.getRoomNum(), room.getTurnOn());
        }
        return getWorldByWorldWrappers();
    }

    private void resetRandomRooms() {
        int turnTotal = random.nextInt(ROOMNUM + 1);
        while (turnTotal == 0) {
            turnTotal = random.nextInt(ROOMNUM + 1);
        }
        int randomRoomNum = random.nextInt(turnTotal);
        LinkedList<Integer> turnRoomNumbers = new LinkedList<>();
        while (turnRoomNumbers.size() != turnTotal) {
            if (!turnRoomNumbers.contains(randomRoomNum)) {
                turnRoomNumbers.add(randomRoomNum);
            }
            randomRoomNum = random.nextInt(turnTotal);
        }
        randomRooms.clear();
        for (int roomNum : turnRoomNumbers) {
            randomRooms.add(rooms.get(roomNum));
        }
    }

    private void turnOnOrOffLightInRoom(int xWithLight, int yWithLight, int roomNum, boolean turn) {
        if (turn) {
            Tileset.generateLightWithBlue();
        } else {
            Tileset.generateLightWithoutBackground();
        }
        for (int x = xWithLight, y = yWithLight, levelWithLight = 0, sideLength = 1;
             y < yWithLight + Tileset.levelWithLights;
             x -= 1, y += 1, levelWithLight += 1, sideLength += 2) {
            TETileWrapper tileWrapper;
            // The bottom of the room
            int level = sideLength - 1;
            for (int i = x; i < x + sideLength; i += 1) {
                setFloorToLightInRoom(i, y - level, roomNum, levelWithLight);
            }
            // The middle of the room
            level -= 1;
            while (level > 0) {
                for (int i = x; i < x + sideLength; i += 1) {
                    if (i == x || i == x + sideLength - 1) {
                        setFloorToLightInRoom(i, y - level, roomNum, levelWithLight);
                    }
                }
                level -= 1;
            }
            // The top of the room
            for (int i = x; i < x + sideLength; i += 1) {
                setFloorToLightInRoom(i, y - level, roomNum, levelWithLight);
            }
        }
    }

    private void setFloorToLightInRoom(int x, int y, int roomNum, int levelWithLight) {
        if (validTileInWorld(x, y)) {
            TETileWrapper tileWrapper = worldWrappers[x][y];
            if ((tileWrapper.getTile().equals(Tileset.FLOOR)
                    || (tileWrapper.getTile().description().equals("light")))
                    && !tileWrapper.getTile().equals(Tileset.AVATAR)
                    && tileWrapper.isRoom()
                    && !tileWrapper.getIsAround()
                    && tileWrapper.getRoomNum() == roomNum) {
                tileWrapper.setTile(Tileset.LIGHTS[levelWithLight]);
            }
        }
    }

    private boolean validTileInWorld(int x, int y) {
        return  x >= 0 && x < width && y >= 0 && y < height;
    }

    private void setMarkInWorldWrappers(int v, boolean markedValue) {
        worldWrappers[toX(v)][toY(v)].markTile(markedValue);
    }

    /**
     * Returns the x coordinate for a given vertex.
     * For example, if N = 10, and V = 12, returns 2.
     */
    private int toX(int v) {
        return v % N + 1;
    }

    /**
     * Returns the y coordinate for a given vertex.
     * For example, if N = 10, and V = 12, returns 1.
     */
    private int toY(int v) {
        return v / N + 1;
    }

    /**
     * Returns a one-dimensional coordinate for the vertex in position (x, y).
     */
    private int xyTo1D(int x, int y) {
        return (y - 1) * N + (x - 1);
    }

    /**
     * Returns the number of spaces in the maze.
     */
    private int V() {
        return N * N;
    }
}
