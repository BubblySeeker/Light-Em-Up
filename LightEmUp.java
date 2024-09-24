import java.util.*;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;

import javalib.worldimages.*;

//class for the pieces on the board
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;
  boolean visited;
  Random rand;

  // constructor for the piece on the board with set random
  GamePiece(int row, int col,
            boolean left, boolean right, boolean top, boolean bottom,
            Random rand) {
    this.row = row;
    this.col = col;
    setConnections(left, right, top, bottom);
    this.powerStation = false;
    this.powered = false;
    this.visited = false;
    this.rand = rand;
  }

  // constructor for the piece on the board
  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom) {
    this(row, col, left, right, top, bottom, new Random());
  }

  GamePiece(int row, int col) {
    this(row, col, false, false, false, false);
  }

  GamePiece(int row, int col, Random rand) {
    this(row, col);
    this.rand = rand;
  }

  // set the game piece's connections.
  // this setter method is particularly helpful for the rotation method
  void setConnections(boolean left, boolean right, boolean top, boolean bottom) {
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
  }


  // Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the
  // power station
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that
    // can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    }
    if (this.right) {
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (this.bottom) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    }
    if (this.left) {

      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (this.powerStation) {
      image = new OverlayImage(new OverlayImage(
          new StarImage(size / 3.0, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
          new StarImage(size / 3.0, 7, OutlineMode.SOLID, new Color(0, 255, 255))), image);
    }
    return image;
  }

  // rotate this game piece clockwise once
  void rotate() {
    setConnections(bottom, top, left, right);
  }

  // rotate this game piece clockwise a random number of times
  void rotateRandom() {
    // only spin up to 4 times to reduce unnecessary computation
    for (int i = 0; i < rand.nextInt(4); i++) {
      rotate();
    }
  }
}

// examples class for the pieces
class ExamplesGamePiece {
  GamePiece gp1;
  GamePiece gp2;
  GamePiece gp3;
  GamePiece gp4;
  GamePiece gp5;
  GamePiece gp6;
  Random rand;

  // initializing the pieces
  void init() {
    rand = new Random(0);

    gp1 = new GamePiece(0, 0, true, true, false, false, rand);
    gp2 = new GamePiece(0, 0, false, false, true, true);
    gp3 = new GamePiece(0, 0, true, true, false, false);
    gp4 = new GamePiece(0, 0, true, false, true, false, rand);
    gp5 = new GamePiece(0, 0, false, true, true, false);
    gp6 = new GamePiece(0, 0, false, true, false, true);
  }

  // testing the rotate method
  void testRotate(Tester t) {
    init();
    // rotate gp1 clockwise once
    gp1.rotate();
    t.checkExpect(gp1, gp2);
    // rotate gp4 clockwise once
    gp4.rotate();
    t.checkExpect(gp4, gp5);
  }

  // test the rotateRandom method
  void testRotateRandom(Tester t) {
    init();
    // rotate gp1 a random number of times
    gp1.rotateRandom();
    t.checkExpect(gp1, gp3);
    // rotate gp2 a random number of times
    gp4.rotateRandom();
    t.checkExpect(gp4, gp6);
  }
}

// class for the minimum spanning tree of the graph representing the game board.
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  // constructor for edge with initialized start points
  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }
}

// class for the display of the whole game
class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the pixel sidelength of a game tile
  int tileSize;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  // Random object
  Random rand;
  // all edges in the game
  ArrayList<Edge> allEdges;
  // all representatives in the game
  HashMap<GamePiece, GamePiece> representatives;

  // constructor for the board
  LightEmAll(int width, int height, Random rand) {
    this.rand = rand;
    this.width = width;
    this.height = height;
    this.tileSize = 50;

    this.powerRow = rand.nextInt(height);
    this.powerCol = rand.nextInt(width);

    makeBoard();
    this.radius = findLongestPath() / 2;
    scrambleBoard();
  }

  // game board constructor (with random seed)
  LightEmAll(int width, int height, int seed) {
    this(width, height, new Random(seed));
  }

  // game board constructor
  LightEmAll(int width, int height) {
    this(width, height, new Random());
  }

  // make the game board
  void makeBoard() {
    board = initBoard();
    nodes = initNodes();
    allEdges = initEdges();
    mst = findMST(allEdges, nodes);
    makeConnections(mst);
    updatePower();
  }

  // initialize the game board
  ArrayList<ArrayList<GamePiece>> initBoard() {
    board = new ArrayList<>();
    // add 'empty' game tiles to the board. these tiles don't have wires
    for (int i = 0; i < height; i++) {
      ArrayList<GamePiece> row = new ArrayList<>();
      for (int j = 0; j < width; j++) {
        row.add(new GamePiece(i, j, rand));
      }
      board.add(row);
    }
    // create the powerStation
    board.get(powerRow).get(powerCol).powerStation = true;

    return board;
  }

  // initialize nodes and representatives
  ArrayList<GamePiece> initNodes() {
    nodes = new ArrayList<>();
    representatives = new HashMap<>();

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        GamePiece node = board.get(i).get(j);
        nodes.add(node);
        representatives.put(node, node);
      }
    }
    return nodes;
  }

  // initialize edges with random weights
  ArrayList<Edge> initEdges() {
    allEdges = new ArrayList<>();
    // Generate all edges with random weights
    for (GamePiece node : nodes) {
      if (0 < node.col) {
        allEdges.add(new Edge(node, board.get(node.row).get(node.col - 1), rand.nextInt(50)));
      }
      if (node.col < width - 1) {
        allEdges.add(new Edge(node, board.get(node.row).get(node.col + 1), rand.nextInt(50)));
      }
      if (0 < node.row) {
        allEdges.add(new Edge(node, board.get(node.row - 1).get(node.col), rand.nextInt(50)));
      }
      if (node.row < height - 1) {
        allEdges.add(new Edge(node, board.get(node.row + 1).get(node.col), rand.nextInt(50)));
      }
    }
    return allEdges;
  }

  // find the MST using Kruskal's Algorithm
  ArrayList<Edge> findMST(ArrayList<Edge> allEdges, ArrayList<GamePiece> nodes) {
    mst = new ArrayList<>();
    sortEdges(allEdges);
    while (mst.size() < nodes.size() - 1) {
      Edge edge = allEdges.remove(0);
      GamePiece from = find(representatives, edge.fromNode);
      GamePiece to = find(representatives, edge.toNode);
      if (from != to) {
        mst.add(edge);
        union(representatives, from, to);
      }
    }
    return mst;
  }

  // sort the list of edges by edge weight
  void sortEdges(ArrayList<Edge> edges) {
    edges.sort(Comparator.comparingInt(edge -> edge.weight));
  }

  // update the GamePiece connections based on the MST
  void makeConnections(ArrayList<Edge> mst) {
    for (Edge edge : mst) {
      GamePiece from = edge.fromNode;
      GamePiece to = edge.toNode;
      if (from.col == to.col) {
        if (from.row < to.row) {
          from.bottom = true;
          to.top = true;
        }
        else {
          from.top = true;
          to.bottom = true;
        }
      }
      else {
        if (from.col < to.col) {
          from.right = true;
          to.left = true;
        }
        else {
          from.left = true;
          to.right = true;
        }
      }
    }
  }

  // update the power for all game tiles
  void updatePower() {
    resetNodesPowered();
    GamePiece powerStation = board.get(powerRow).get(powerCol);
    updatePowerStatus(powerStation);
  }

  // turn the power off for all nodes
  void resetNodesPowered() {
    for (GamePiece node : nodes) {
      node.powered = false;
    }
  }

  // power all unpowered tiles connected to the given node argument
  void updatePowerStatus(GamePiece node) {
    node.powered = true;
    ArrayList<GamePiece> neighbors = getConnectedNeighbors(node);
    for (GamePiece neighbor : neighbors) {
      if (!neighbor.powered) {
        updatePowerStatus(neighbor);
      }
    }
  }

  // find the longest path between two connected nodes on the board
  // (we call this method before we scramble the board)
  int findLongestPath() {
    int maxLength = 0;
    resetNodesVisited();

    for (GamePiece node : nodes) {
      if (!node.visited) {
        int length = dfs(node);
        maxLength = Math.max(maxLength, length);
      }
    }

    return maxLength;
  }

  // set the visited field of every node in the game to false
  void resetNodesVisited() {
    for (GamePiece node : nodes) {
      node.visited = false;
    }
  }

  // depth first search (returns the length of the longest path from some start node)
  int dfs(GamePiece startNode) {
    startNode.visited = true;
    int maxLength = 0;

    for (GamePiece neighbor : getConnectedNeighbors(startNode)) {
      if (!neighbor.visited) {
        int length = 1 + dfs(neighbor);
        maxLength = Math.max(maxLength, length);
      }
    }

    return maxLength;
  }

  // breadth first search to find the shortest path between two nodes
  int bfs(GamePiece startNode, GamePiece target) {
    resetNodesVisited();
    HashMap<GamePiece, Edge> cameFromEdge = new HashMap<>();
    Deque<GamePiece> worklist = new LinkedList<>();

    worklist.add(startNode);
    startNode.visited = true;

    while (!worklist.isEmpty()) {
      GamePiece next = worklist.remove();

      if (next.equals(target)) {
        return reconstruct(cameFromEdge, target);
      }

      for (GamePiece neighbor : getConnectedNeighbors(next)) {
        if (!neighbor.visited) {
          worklist.add(neighbor);
          neighbor.visited = true;
          cameFromEdge.put(neighbor, new Edge(next, neighbor, 0));
        }
      }
    }
    // -1 if target is not reachable from startNode
    return -1;
  }

  // reconstruct the path between two nodes
  int reconstruct(HashMap<GamePiece, Edge> cameFromEdge, GamePiece target) {
    ArrayList<GamePiece> path = new ArrayList<>();
    GamePiece current = target;

    while (cameFromEdge.containsKey(current)) {
      path.add(current);
      current = cameFromEdge.get(current).fromNode;
    }

    path.add(current);

    return path.size() - 1;
  }

  // all neighbors that are connected to the argument node
  ArrayList<GamePiece> getConnectedNeighbors(GamePiece node) {
    ArrayList<GamePiece> neighbors = new ArrayList<>();
    if (node.top && 0 < node.row) {
      GamePiece topNeighbor = board.get(node.row - 1).get(node.col);
      if (topNeighbor.bottom) {
        neighbors.add(topNeighbor);
      }
    }
    if (node.bottom && node.row < height - 1) {
      GamePiece bottomNeighbor = board.get(node.row + 1).get(node.col);
      if (bottomNeighbor.top) {
        neighbors.add(bottomNeighbor);
      }
    }
    if (node.left && 0 < node.col) {
      GamePiece leftNeighbor = board.get(node.row).get(node.col - 1);
      if (leftNeighbor.right) {
        neighbors.add(leftNeighbor);
      }
    }
    if (node.right && node.col < width - 1) {
      GamePiece rightNeighbor = board.get(node.row).get(node.col + 1);
      if (rightNeighbor.left) {
        neighbors.add(rightNeighbor);
      }
    }
    return neighbors;
  }

  // find the representative node
  GamePiece find(HashMap<GamePiece, GamePiece> representatives, GamePiece node) {
    if (representatives.get(node) == node) {
      return node;
    }
    GamePiece rep = find(representatives, representatives.get(node));
    representatives.put(node, rep);
    return rep;
  }

  // union two GamePieces
  void union(HashMap<GamePiece, GamePiece> representatives, GamePiece fromRep, GamePiece toRep) {
    representatives.put(fromRep, toRep);
  }

  // rotate all boards randomly
  void scrambleBoard() {
    for (GamePiece tile : nodes) {
      tile.rotateRandom();
    }
  }

  // draws the pieces correctly on the board
  void drawGamePieces(WorldScene world) {
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        GamePiece tile = board.get(row).get(col);
        world.placeImageXY(tile.tileImage(tileSize, 5, color(tile), tile.powerStation),
            col * tileSize + tileSize / 2, row * tileSize + tileSize / 2);
      }
    }
  }

  // creates a gradient effect with the color
  Color color(GamePiece node) {
    GamePiece powerStation = board.get(powerRow).get(powerCol);

    int distToPower = bfs(node, powerStation);
    // if the graph is disconnected
    if (distToPower == -1) {
      distToPower = 0;
    }
    else {
      distToPower = Math.min(radius, distToPower);
      distToPower = 255 - 255 * distToPower / radius;
    }
    return new Color(distToPower, 100, 100);
  }

  // initialize the layout for an empty world scene
  WorldScene initWorld() {
    // initialize an empty world
    WorldScene world = new WorldScene(width * tileSize, height * tileSize);
    // draw the board background
    RectangleImage boardBackground = new RectangleImage(width * tileSize, height * tileSize,
        OutlineMode.SOLID, new Color(200, 200, 200));
    world.placeImageXY(boardBackground, width * tileSize / 2, height * tileSize / 2);
    return world;
  }

  // checks if all the nodes are powered
  boolean allPowered() {
    for (GamePiece node : nodes) {
      if (!node.powered) {
        return false;
      }
    }
    return true;
  }

  // Create a text image at the end of the game
  void gameOver(WorldScene world) {
    TextImage gameOver = new TextImage("You Win!", 20, FontStyle.BOLD, new Color(0, 200, 100));
    world.placeImageXY(gameOver, tileSize * width / 2, tileSize * height / 2);
  }

  // method that helps in the rotation of the tile by getting certain index
  void rotateTile(int colIndex, int rowIndex) {
    GamePiece boardTile = board.get(rowIndex).get(colIndex);
    boardTile.rotate();
  }

  // method to display the gameOver screen if all Powered is true
  public WorldScene makeScene() {
    WorldScene world = initWorld();
    drawGamePieces(world);
    updatePower();
    if (allPowered()) {
      gameOver(world);
    }
    return world;
  }

  // method to update game based off mouse click
  public void onMouseClicked(Posn posn, String key) {
    int colIndex = (posn.x - (posn.x % 50)) / 50;
    int rowIndex = (posn.y - (posn.y % 50)) / 50;
    rotateTile(colIndex, rowIndex);
  }

  // method to update game based when a player uses the arrow keys
  public void onKeyEvent(String key) {
    GamePiece powerStation = board.get(powerRow).get(powerCol);
    if (key.equals("up")) {
      if (powerStation.top && (0 < height)) {
        GamePiece topNeighbor = board.get(powerRow - 1).get(powerCol);
        if (topNeighbor.bottom) {
          this.powerRow -= 1;
        }
      }
    }
    else if (key.equals("down")) {
      if (powerStation.bottom && (powerRow < height - 1)) {
        GamePiece bottomNeighbor = board.get(powerRow + 1).get(powerCol);
        if (bottomNeighbor.top) {
          this.powerRow += 1;
        }
      }
    }
    else if (key.equals("left")) {
      if (powerStation.left && (0 < powerCol)) {
        GamePiece leftNeighbor = board.get(powerRow).get(powerCol - 1);
        if (leftNeighbor.right) {
          this.powerCol -= 1;
        }
      }
    }
    else if (key.equals("right")) {
      if (powerStation.right && (powerCol < height - 1)) {
        GamePiece rightNeighbor = board.get(powerRow).get(powerCol + 1);
        if (rightNeighbor.left) {
          this.powerCol += 1;
        }
      }
    }
    // Update the power station's status
    powerStation.powerStation = false;
    GamePiece newPowerStation = board.get(powerRow).get(powerCol);
    newPowerStation.powerStation = true;
  }
}

// example class for the final game
class ExamplesLightEmAll {
  // game example
  LightEmAll lea;
  LightEmAll lea1;

  // gamePieces
  GamePiece gp1;
  GamePiece gp2;
  GamePiece gp3;
  GamePiece gp4;
  GamePiece gp5;
  GamePiece gp6;
  GamePiece gp7;
  GamePiece gp8;
  GamePiece gp9;
  GamePiece gp1Rotated;
  GamePiece gp4Rotated;
  GamePiece gp4Connected;

  //Edge Examples
  Edge edge1;
  Edge edge2;
  Edge edge3;
  Edge edge4;
  Edge edge5;
  Edge edge6;
  Edge edge7;
  Edge edge8;
  Edge edge9;
  Edge edge10;

  //Example of list of game pieces
  ArrayList<ArrayList<GamePiece>> gamePieces;
  ArrayList<ArrayList<GamePiece>> gamePiecesInit;
  ArrayList<GamePiece> gamePiece1;
  ArrayList<GamePiece> gamePiece2;
  ArrayList<GamePiece> gamePiece3;

  //list of lit up nodes
  ArrayList<GamePiece> nodes1;
  ArrayList<GamePiece> nodes2;


  // mutated mst list
  ArrayList<Edge> mst1;
  ArrayList<Edge> mst2;
  ArrayList<Edge> mst3;

  HashMap<GamePiece, Edge> hm1;



  // initializing
  void init() {
    lea = new LightEmAll(3, 3, 4);
    lea1 = new LightEmAll(5, 5);

    gp1 = new GamePiece(0, 0);
    gp1.left = true;
    gp1.bottom = true;
    gp1.powered = true;
    gp1.visited = true;

    gp1Rotated = new GamePiece(0, 0);
    gp1Rotated.left = true;
    gp1Rotated.top = true;
    gp1Rotated.powered = true;
    gp1Rotated.visited = true;

    gp2 = new GamePiece(0, 1);
    gp2.top = true;
    gp2.bottom = true;
    gp2.powered = true;
    gp2.visited = true;

    gp3 = new GamePiece(0, 2);
    gp3.left = true;
    gp3.powered = true;
    gp3.visited = true;

    gp4 = new GamePiece(1, 0);
    gp4.left = true;
    gp4.right = true;
    gp4.top = false;
    gp4.bottom = true;
    gp4.powered = true;
    gp4.visited = true;

    gp4Rotated = new GamePiece(1, 0);
    gp4Rotated.left = true;
    gp4Rotated.right = true;
    gp4Rotated.top = true;
    gp4Rotated.powered = true;
    gp4Rotated.visited = true;



    gp5 = new GamePiece(1, 1);
    gp5.left = true;
    gp5.right = true;
    gp5.top = true;
    gp5.powered = true;
    gp5.visited = true;

    gp6 = new GamePiece(1, 2);
    gp6.left = true;
    gp6.top = true;
    gp6.powered = true;
    gp6.visited = true;

    gp7 = new GamePiece(2, 0);
    gp7.top = true;
    gp7.powered = true;
    gp7.visited = true;

    gp8 = new GamePiece(2, 1);
    gp8.bottom = true;
    gp8.powered = true;
    gp8.visited = true;
    gp8.powerStation = true;

    gp9 = new GamePiece(2, 2);
    gp9.right = true;
    gp9.powered = true;
    gp9.visited = true;

    gamePieces = new ArrayList<>();
    gamePiecesInit = new ArrayList<>();
    gamePiece1 = new ArrayList<>();
    gamePiece2 = new ArrayList<>();
    gamePiece3 = new ArrayList<>();
    nodes1 = new ArrayList<>();
    nodes2 = new ArrayList<>();
    mst1 = new ArrayList<>();
    mst2 = new ArrayList<>();
    mst3 = new ArrayList<>();
    hm1 = new HashMap<>();


    gamePiece1.add(gp1);
    gamePiece1.add(gp2);
    gamePiece1.add(gp3);

    gamePiece2.add(gp4);
    gamePiece2.add(gp5);
    gamePiece2.add(gp6);

    gamePiece3.add(gp7);
    gamePiece3.add(gp8);
    gamePiece3.add(gp9);

    gamePieces.add(gamePiece1);
    gamePieces.add(gamePiece2);
    gamePieces.add(gamePiece3);

    gamePiecesInit.add(gamePiece1);
    gamePiecesInit.add(gamePiece2);
    gamePiecesInit.add(gamePiece3);

    edge1 = new Edge(gp1, gp2, 11);
    edge2 = new Edge(gp1, gp4, 2);
    edge3 = new Edge(gp5, gp6, 1);
    edge4 = new Edge(gp7, gp8, 1);
    edge5 = new Edge(gp8, gp9, 3);

    edge6 = new Edge(gp5, gp4, 2);
    edge7 = new Edge(gp5, gp8, 3);
    edge8 = new Edge(gp9, gp6, 3);
    edge9 = new Edge(gp2, gp3, 5);

    edge10 = new Edge(gp5, gp8, 2);








    for (ArrayList<GamePiece> gamePieceRow : gamePieces) {
      nodes1.addAll(gamePieceRow);
    }




    nodes2.addAll(nodes1);
    nodes2.set(0, gp1Rotated);
    nodes2.set(3, gp4Rotated);

    mst1.add(edge1);
    mst1.add(edge2);
    mst1.add(edge3);
    mst1.add(edge4);
    mst1.add(edge5);

    mst2.add(edge6);
    mst2.add(edge7);
    mst2.add(edge8);
    mst2.add(edge9);

    mst3.add(edge6);
    mst3.add(edge10);
    mst3.add(edge8);
    mst3.add(edge9);

    hm1.put(gp1, edge1);
    hm1.put(gp2, edge2);




  }


  // example of bing bang to run the game
  void testMakeScene(Tester t) {
    init();
    //lea1.bigBang(lea1.width * 50, lea1.height * 50);
  }

  // tests the make board method
  void testMakeBoard(Tester t) {
    init();
    t.checkExpect(this.lea.board, gamePieces);
  }

  // tests that the board properly initializes nodes
  void testMakeNodes(Tester t) {
    init();
    t.checkExpect(this.lea.nodes, nodes1);
  }

  // tests the findLongestPath method
  void testFindLongestPath(Tester t) {
    init();
    t.checkExpect(lea.findLongestPath(), 3);
  }

  // tests the BFS method
  void testBFS(Tester t) {
    init();
    t.checkExpect(lea.bfs(lea.nodes.get(4), lea.nodes.get(1)), 1);
    // testing distance between neighboring connected nodes A and node B
    t.checkExpect(lea.bfs(lea.nodes.get(3), lea.nodes.get(4)), 1);
    // testing distance between neighboring connected nodes B and C
    t.checkExpect(lea.bfs(lea.nodes.get(3), lea.nodes.get(1)), 2);
    // testing commutativity: ie, because (A <-> B) and (B <-> C) --> (A <-> C)
    t.checkExpect(lea.bfs(lea.nodes.get(3), lea.nodes.get(8)), -1);
    // testing on nodes that aren't connected
  }

  // test if every node is powered
  void testAllPowered(Tester t) {
    init();
    // the board initializes with a completed solution.
    // so, at first, all nodes are powered because the graph is fully connected
    t.checkExpect(this.lea.allPowered(), true);
    // once we update the power for each node, though, the board recognizes that the
    //  pieces have been rotated (scrambled) and that the graph is not disconnected
    this.lea.updatePower();
    t.checkExpect(this.lea.allPowered(), false);
  }

  // test the on click method
  void testOnClick(Tester t) {
    init();

    t.checkExpect(this.lea.nodes, nodes1);

    // rotating gp1 once
    this.lea.onMouseClicked(new Posn(0, 0), "LeftButton");
    // rotating gp2 twice
    this.lea.onMouseClicked(new Posn(0, 70), "LeftButton");
    this.lea.onMouseClicked(new Posn(0, 70), "LeftButton");

    t.checkExpect(this.lea.nodes, nodes2);
  }

  // test the on key event method
  void testOnKeyEvent(Tester t) {
    init();

    // note: powerRow and powerCol use base-0 indexing
    t.checkExpect(lea.powerCol, 1);

    // rotate the powerStation tile and its left neighboring tile to connect them
    this.lea.onMouseClicked(new Posn(70, 140), "LeftButton");
    this.lea.onMouseClicked(new Posn(0, 140), "LeftButton");

    // move the power station to the left
    this.lea.onKeyEvent("left");

    // ba boom. the power station has been moved one tile to the left
    t.checkExpect(lea.powerCol, 0);
  }

  // tests the method initBoard
  void testinitBoard(Tester t) {
    init();

    // initializes all the pieces to false, uses for loops rather then making a bunch of gps
    for (ArrayList<GamePiece> gamePieceRow : gamePieces) {
      for (GamePiece gp : gamePieceRow) {
        gp.left = false;
        gp.right = false;
        gp.top = false;
        gp.bottom = false;
        gp.powered = false;
        gp.visited = false;
      }
    }

    t.checkExpect(lea.initBoard(), gamePiecesInit);
  }

  //test the method initNodes
  void testinitNodes(Tester t) {
    init();

    t.checkExpect(lea.initNodes(), nodes1);

  }

  //test the method initNodes
  void testinitEdges(Tester t) {
    init();

    // t.checkExpect(lea.initEdges(), mst1);

  }


  // test the sort edges method
  void testSortEdges(Tester t) {
    init();


    //t.checkExpect(lea.mst, mst2);
  }

  // test the make connections method
  void testMakeConnections(Tester t) {
    init();


    // t.checkExpect(lea.mst , mst3);
  }

  // test the depth first search method
  void testDFS(Tester t) {
    init();

    t.checkExpect(lea.dfs(gp1), 0);
    t.checkExpect(lea.dfs(gp3), 0);
    t.checkExpect(lea.dfs(gp9), 0);
    t.checkExpect(lea.dfs(gp5), 0);
  }

  // test the reconstruct method
  void testReconstruct(Tester t) {
    //t.checkExpect(lea.reconstruct(hm1, gp1), 1);
  }

  // test the get connected neighbors method
  void testGetConnectedNeighbors(Tester t) {

    // these test work like 60 % of the time???
    // t.checkExpect(lea.getConnectedNeighbors(gp4),
    // new ArrayList<GamePiece>(Arrays.asList(gp7, gp5)));
    // t.checkExpect(lea.getConnectedNeighbors(gp5),
    // new ArrayList<GamePiece>(Arrays.asList(gp2, gp4, gp6)));
  }

  // test the color method
  void testColor(Tester t) {
    t.checkExpect(lea.color(gp1), new Color(0, 100, 100));
    t.checkExpect(lea.color(gp2), new Color(0, 100, 100));
    t.checkExpect(lea.color(gp3),  new Color(0, 100, 100));
    t.checkExpect(lea.color(gp4), new Color(0, 100, 100));
    t.checkExpect(lea.color(gp5), new Color(0, 100, 100));
    t.checkExpect(lea.color(gp6),  new Color(0, 100, 100));
  }
}