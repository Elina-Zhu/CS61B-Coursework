package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import static byow.Core.OtherUtils.*;

import java.io.*;

public class Engine implements Serializable {
    TERenderer ter = new TERenderer();
    /* Feel free to change the width and height. */
    public static final int WIDTH = 90;
    public static final int HEIGHT = 50;
    private Long seed;
    private Menu menu = new Menu(40, 60);
    private boolean gameInit = true;
    private TETile[][] world = new TETile[WIDTH][HEIGHT];
    private WorldGenerator worldGenerator;

    // The current working directory
    public static final File CWD = new File(System.getProperty("user.dir"));
    // The .save directory
    public static final File SAVE_DIR = join(CWD, ".save");

    public Engine() {
    }

    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {
        if (gameInit) {
            menu.drawMenu();
        }
        String inputString = "";
        char typedKey;

        while (true) {
            typedKey = OtherUtils.getNextKey();
            if (isNumber(typedKey) || isValidChar(typedKey)) {
                inputString += typedKey;
            }

            // When in the menu interface
            if (gameInit && typedKey == 'S') {
                int stepIndex = inputString.indexOf("S");
                inputString = inputString.substring(1, stepIndex);
                break;
            }
            if (gameInit) {
                if (typedKey == 'Q') {
                    System.exit(0);
                }
                if (typedKey == 'N') {
                    continue;
                }
                if (typedKey == 'L') {
                    load();
                    break;
                }
            }

            // When the game is in progress
            if (!gameInit && inputString.equals(":Q")) {
                saveAndQuit();
            }
            if (!gameInit && inputString.length() == 1) {
                if (inputString.equals("W") || inputString.equals("S")
                        || inputString.equals("A") || inputString.equals("D")
                        || inputString.equals("P")) {
                    break;
                }
            }
        }
        renderWorld(inputString);
        interactWithKeyboard();
    }

    /**
     * Method used for autograding and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww". The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     *
     * Recall that strings ending in ":q" should cause the game to quite save. For example,
     * if we do interactWithInputString("n123sss:q"), we expect the game to run the first
     * 7 commands (n123sss) and then quit and save. If we then do
     * interactWithInputString("l"), we should be back in the exact same state.
     *
     * In other words, both of these calls:
     *   - interactWithInputString("n123sss:q")
     *   - interactWithInputString("lww")
     *
     * should yield the exact same world state as:
     *   - interactWithInputString("n123sssww")
     *
     * @param input the input string to feed to your program
     * @return the 2D TETile[][] representing the state of the world
     */
    public TETile[][] interactWithInputString(String input) {
        // TODO: Fill out this method so that it run the engine using the input
        // passed in as an argument, and return a 2D tile representation of the
        // world that would have been drawn if the same inputs had been given
        // to interactWithKeyboard().
        //
        // See proj3.byow.InputDemo for a demo of how you can make a nice clean interface
        // that works for many different input types.
        switch (input) {
            case "W":
                return worldGenerator.moveAvatarThenGenerateWorld("W");
            case "S":
                return worldGenerator.moveAvatarThenGenerateWorld("S");
            case "A":
                return worldGenerator.moveAvatarThenGenerateWorld("A");
            case "D":
                return worldGenerator.moveAvatarThenGenerateWorld("D");
            case "P":
                return worldGenerator.turnOnOrOffLightInRooms();
        }

        // Create a new world
        if (seed == null) {
            seed = Long.parseLong(input);
            worldGenerator = new WorldGenerator(seed, world, false);
            return worldGenerator.generateWorld();
        }
        // Load the previous world
        return world;
    }

    private void load() {
        Engine loadEngine = readObject(join(SAVE_DIR, "saveEngine.txt"), Engine.class);
        ter = loadEngine.getTer();
        seed = loadEngine.getSeed();
        world = loadEngine.getWorld();
        worldGenerator = loadEngine.getWorldGenerator();
    }

    private void saveAndQuit() {
        if (!SAVE_DIR.exists()) {
            SAVE_DIR.mkdir();
        }
        writeObject(join(SAVE_DIR, "saveEngine.txt"), this);
        System.exit(0);
    }

    private void renderWorld(String inputString) {
        world = interactWithInputString(inputString);
        if (gameInit) {
            gameInit = false;
            renderWorldWithBeginning(world);
        } else {
            renderWorldWithMoving(world);
        }
    }

    public void renderWorldWithBeginning(TETile[][] world) {
        ter.initialize(WIDTH, HEIGHT);
        ter.renderFrame(world);
    }

    public void renderWorldWithMoving(TETile[][] world) {
        ter.renderFrame(world);
    }

    public TERenderer getTer() {
        return ter;
    }

    public Long getSeed() {
        return seed;
    }

    public TETile[][] getWorld() {
        return world;
    }

    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
}
