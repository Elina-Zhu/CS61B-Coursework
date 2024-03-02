package byow.TileEngine;

import java.io.Serializable;

public class TETileWrapper implements Serializable {
    private TETile tile;
    private boolean marked;
    private boolean isRoom;
    private int x;
    private int y;
    private int roomNum;
    private boolean isAround = false;

    public TETileWrapper(TETile tile, int x, int y) {
        this.tile = tile;
        this.x = x;
        this.y = y;
        this.marked = false;
        this.isRoom = false;
    }


    public void setTile(TETile tile) {
        this.tile = tile;
    }

    public TETile getTile() {
        return tile;
    }


    public void markTile(boolean markedValue) {
        marked = markedValue;
    }

    public boolean isMarked() {
        return marked;
    }


    public void markRoom() {
        isRoom = true;
    }

    public boolean isRoom() {
        return isRoom;
    }


    public void setX(Integer x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }


    public void setY(Integer y) {
        this.y = y;
    }

    public int getY() {
        return y;
    }


    public void setRoomNum(int roomNum) {
        this.roomNum = roomNum;
    }

    public int getRoomNum() {
        return roomNum;
    }


    public void setIsAround() {
        this.isAround = true;
    }

    public boolean getIsAround() {
        return isAround;
    }
}
