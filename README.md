# Light-Em-Up

Overview
Created a strategic puzzle game called "Light 'Em All," where the main objective is to connect a network of game pieces on a board in such a way that all pieces become powered. The game is built using Java and leverages a graphical library to handle the rendering and interaction within the game.

Key Components
- GamePiece: Represents individual tiles on the game board. Each piece can have connections to its adjacent pieces and may also house a power station.
- LightEmAll: The main class that orchestrates game logic, including board setup, gameplay mechanics, and rendering. It handles tasks like generating the game board, connecting pieces, updating the power distribution across the board, and responding to user interactions like mouse clicks and keyboard events.
ExamplesGamePiece & ExamplesLightEmAll: These classes contain methods for testing the functionality of the game pieces and the overall game logic, ensuring that all components work as expected.

Game Mechanics
- Board Initialization: The board is set up with a grid of game pieces, where connections between pieces are initially randomized.
- Power Distribution: A central aspect of the game is the distribution of power from a power station, which spreads to connected pieces.
- User Interaction: Players interact with the game by rotating pieces to form continuous connections between the power station and other pieces, aiming to light up the entire board.
- 
Visual Representation
The game visually represents the board and its pieces, showing connections and whether a piece is powered. The power station is also distinctly displayed.

Goal
The ultimate goal for the player is to manipulate the game pieces to ensure that power flows from the power station to all other pieces on the board, achieving a "fully powered" state to win the game.

