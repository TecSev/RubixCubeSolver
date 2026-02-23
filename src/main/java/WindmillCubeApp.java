import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;
import cs.min2phase.Search;

public class WindmillCubeApp extends Application {

    private Group cubeGroup;
    private final Map<String, Cubie> cubies = new HashMap<>();
    private boolean isAnimating = false;
    private final double CUBE_SIZE = 100;
    private final double GAP = 2;
    private TextArea consoleLog;
    
    // UI Controls
    private ComboBox<String> cubeTypeSelector;
    private TextArea algorithmDisplay;
    private final Queue<String> moveQueue = new ArrayDeque<>();
    private boolean isPaintMode = false;
    private Color currentPaintColor = Color.WHITE;
    private double animationSpeed = 250.0; // Default: 250ms per turn
    @Override
    public void start(Stage stage) {
	new Thread(() -> {
            try {
                cs.min2phase.Search.init();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Initialization Error");
                    alert.setHeaderText("Solver Initialization Failed");
                    alert.setContentText("The solver engine failed to initialize. Solving functionality will be unavailable.\n\nError: " + e.getMessage());
                    alert.showAndWait();
                    if (consoleLog != null) {
                        log("CRITICAL ERROR: Solver initialization failed: " + e.getMessage());
                    }
                });
            }
        }).start();
        // 1. Setup 3D Scene
        cubeGroup = buildCube();
        SubScene subScene = create3DSubScene(cubeGroup);

        // 2. Setup UI Overlay
        BorderPane root = new BorderPane();
        root.setCenter(subScene);
        root.setRight(createControlPanel());

        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("Java Windmill Cube Solver");
        stage.setScene(scene);
        stage.show();
    }

    // --- 3D Construction ---
    private Group buildCube() {
        Group group = new Group();
        cubies.clear(); // Ensure map is clean on rebuild
        
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Cubie cubie = new Cubie(CUBE_SIZE, x, y, z);
                    cubies.put(getHash(x, y, z), cubie);
                    group.getChildren().add(cubie);
                }
            }
        }
        group.getChildren().add(new javafx.scene.AmbientLight(Color.WHITE));
        return group;
    }
    private final Color[] PALETTE = {
            Color.WHITE, Color.YELLOW, Color.ORANGE, 
            Color.RED, Color.GREEN, Color.BLUE
     };

    private SubScene create3DSubScene(Group group) {
        // ... (Keep your existing camera and setup code) ...
        SubScene subScene = new SubScene(group, 750, 700, true, SceneAntialiasing.BALANCED);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFarClip(5000);
        camera.setTranslateZ(-1400);
        camera.setTranslateY(-350);
        camera.setRotationAxis(Rotate.X_AXIS);
        camera.setRotate(-15);
        subScene.setCamera(camera);

        Rotate xRotate = new Rotate(0, Rotate.X_AXIS);
        Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);
        group.getTransforms().addAll(xRotate, yRotate);

        // --- MOUSE DRAG (Keep existing) ---
        final double[] anchor = new double[2];
        subScene.setOnMousePressed(event -> {
            anchor[0] = event.getSceneX();
            anchor[1] = event.getSceneY();
        });
        subScene.setOnMouseDragged(event -> {
            yRotate.setAngle(yRotate.getAngle() + (event.getSceneX() - anchor[0]) * 0.3);
            xRotate.setAngle(xRotate.getAngle() - (event.getSceneY() - anchor[1]) * 0.3);
            anchor[0] = event.getSceneX();
            anchor[1] = event.getSceneY();
        });

        // --- MOUSE CLICK (NEW: This handles the painting) ---
        subScene.setOnMouseClicked(event -> {
            if (!isPaintMode) return; 

            javafx.scene.input.PickResult res = event.getPickResult();
            Node node = res.getIntersectedNode();

            if (node instanceof Box) {
                Box face = (Box) node;
                
                // 1. Visually update the face color
                face.setMaterial(new PhongMaterial(currentPaintColor));
                
                // 2. Logically update the internal memory (THIS WAS MISSING)
                boolean found = false;
                for (Cubie c : cubies.values()) {
                    for (int i = 0; i < 6; i++) {
                        // We look for which Cubie owns this specific 3D Box
                        if (c.faceNodes[i] == face) {
                            c.faceColors[i] = currentPaintColor; // Update the memory
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                
                log("Painted a face " + getColorName(currentPaintColor));
            }
        });

        return subScene;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: #333; -fx-padding: 20;");
        
        Label title = new Label("Solver Settings");
        title.setTextFill(Color.WHITE);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        cubeTypeSelector = new ComboBox<>();
        cubeTypeSelector.getItems().addAll("Standard 3x3", "Windmill 3x3");
        cubeTypeSelector.setValue("Windmill 3x3");

        // --- PAINT CONTROLS ---
        TitledPane paintPane = new TitledPane();
        paintPane.setText("Color Editor");
        paintPane.setCollapsible(false);
        
        VBox paintContent = new VBox(10);
        ToggleButton paintToggle = new ToggleButton("Enable Paint Mode");
        paintToggle.setMaxWidth(Double.MAX_VALUE);
        paintToggle.setOnAction(e -> {
            isPaintMode = paintToggle.isSelected();
            log(isPaintMode ? "Paint Mode ENABLED." : "Paint Mode DISABLED.");
        });

        FlowPane paletteBox = new FlowPane();
        paletteBox.setHgap(5); paletteBox.setVgap(5);
        for (Color c : PALETTE) {
            Button colorBtn = new Button();
            colorBtn.setPrefSize(30, 30);
            String hex = String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
            colorBtn.setStyle("-fx-background-color: " + hex + "; -fx-border-color: #888;");
            colorBtn.setOnAction(e -> currentPaintColor = c);
            paletteBox.getChildren().add(colorBtn);
        }
        paintContent.getChildren().addAll(paintToggle, new Label("Select Color:"), paletteBox);
        paintPane.setContent(paintContent);
        
        // --- ACTION BUTTONS ---
        HBox buttonBox = new HBox(10); 
        Button scrambleBtn = new Button("Scramble");
        scrambleBtn.setOnAction(e -> scrambleCube());
        Button resetBtn = new Button("Reset"); 
        resetBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        resetBtn.setOnAction(e -> resetCube());
        Button solveBtn = new Button("Solve");
        solveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        solveBtn.setOnAction(e -> startSolve());
        buttonBox.getChildren().addAll(scrambleBtn, resetBtn, solveBtn);

        // --- SPEED CONTROL (NEW) ---
        VBox speedBox = new VBox(5);
        Label speedLabel = new Label("Animation Speed (ms):");
        speedLabel.setTextFill(Color.WHITE);
        
        // Slider from 50ms (Fast) to 1000ms (Slow), defaulting to 250ms
        Slider speedSlider = new Slider(50, 1000, 250);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(false);
        
        // Listener to update the variable
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            animationSpeed = newVal.doubleValue();
        });
        
        speedBox.getChildren().addAll(speedLabel, speedSlider);

        // --- OUTPUTS ---
        algorithmDisplay = new TextArea();
        algorithmDisplay.setEditable(false);
        algorithmDisplay.setWrapText(true);
        algorithmDisplay.setPrefHeight(60);

        consoleLog = new TextArea();
        consoleLog.setEditable(false);
        consoleLog.setWrapText(true);
        consoleLog.setStyle("-fx-font-family: 'Consolas', monospace;");
        consoleLog.setPrefHeight(150);

        panel.getChildren().addAll(
            title, cubeTypeSelector, paintPane, 
            buttonBox, speedBox, // Added speedBox here
            new Label("Solution:"), algorithmDisplay, 
            new Label("Console:"), consoleLog
        );
        return panel;
    }
    
    private String getColorName(Color c) {
        if(c.equals(Color.WHITE)) return "White";
        if(c.equals(Color.YELLOW)) return "Yellow";
        if(c.equals(Color.ORANGE)) return "Orange";
        if(c.equals(Color.RED)) return "Red";
        if(c.equals(Color.GREEN)) return "Green";
        if(c.equals(Color.BLUE)) return "Blue";
        return "Custom";
    }
    private void log(String message) {
        consoleLog.appendText("> " + message + "\n");
        consoleLog.setScrollTop(Double.MAX_VALUE);
    }

    private void resetCube() {
        if (isAnimating) {
            log("Cannot reset while animating!");
            return;
        }

        log("Resetting Cube state...");
        
        // 1. Clear Data
        moveQueue.clear();
        cubies.clear();
        cubeGroup.getChildren().clear(); // Empty the existing bucket
        algorithmDisplay.clear();

        // 2. Refill the EXISTING bucket (Do not create a new Group)
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Cubie cubie = new Cubie(CUBE_SIZE, x, y, z);
                    cubies.put(getHash(x, y, z), cubie);
                    cubeGroup.getChildren().add(cubie); // Add to the main group
                }
            }
        }
        
        // 3. Add the light back (it was cleared in step 1)
        cubeGroup.getChildren().add(new javafx.scene.AmbientLight(Color.WHITE));
        
        log("Cube Reset Complete.");
    }
    private Cubie getCubieAt(int x, int y, int z) {
        for (Cubie c : cubies.values()) {
            if (c.lx == x && c.ly == y && c.lz == z) {
                return c;
            }
        }
        return null;
    }
 // 0:U, 1:R, 2:F, 3:D, 4:L, 5:B
    private char getVisibleFaceColor(Cubie c, Point3D scanDirection) {
        if (c == null) return '?';
        
        // Transform the global scan direction into the Cubie's local coordinate system
        Point3D localDir = scanDirection;
        try {
            // Iterate transforms in reverse order to apply inverse logic correctly
            // (Last applied transform must be undone first)
            for (int i = c.getTransforms().size() - 1; i >= 0; i--) {
                Transform t = c.getTransforms().get(i);
                localDir = t.inverseDeltaTransform(localDir);
            }
        } catch (javafx.scene.transform.NonInvertibleTransformException e) {
            e.printStackTrace();
            return 'X'; // Should not happen for Rotations/Translations
        }

        // Determine which face index matches the LOCAL scan direction
        // We find the axis with the largest component magnitude
        int faceIndex = -1;
        
        double absX = Math.abs(localDir.getX());
        double absY = Math.abs(localDir.getY());
        double absZ = Math.abs(localDir.getZ());

        // Note: JavaFX Y is Down (-1 is Up). Z is Back (-1 is Front).
        if (absY > absX && absY > absZ) {
            if (localDir.getY() < 0) faceIndex = 0; // Up (-Y)
            else faceIndex = 3;                     // Down (+Y)
        } else if (absX > absY && absX > absZ) {
            if (localDir.getX() > 0) faceIndex = 1; // Right (+X)
            else faceIndex = 4;                     // Left (-X)
        } else {
            if (localDir.getZ() < 0) faceIndex = 2; // Front (-Z)
            else faceIndex = 5;                     // Back (+Z)
        }
        
        if (faceIndex >= 0) {
            return colorToChar(c.faceColors[faceIndex]);
        }
        return '?';
    }

    private char colorToChar(Color c) {
        // Map Colors to standard Face characters
        if (c.equals(Color.YELLOW)) return 'U'; // Up
        if (c.equals(Color.RED))    return 'R'; // Right (Standard)
        if (c.equals(Color.BLUE))   return 'F'; // Front (Standard)
        if (c.equals(Color.WHITE))  return 'D'; // Down
        if (c.equals(Color.ORANGE)) return 'L'; // Left (Standard)
        if (c.equals(Color.GREEN))  return 'B'; // Back (Standard)
        return 'X'; // Error
    }
    private String generateString() {
        StringBuilder sb = new StringBuilder();

        // 1. UP Face (y = -1). Read Z: Back->Front, X: Left->Right
        // Standard U order: Top-Left (Back-Left) to Bottom-Right (Front-Right)
        for (int z = 1; z >= -1; z--) {
            for (int x = -1; x <= 1; x++) {
                Cubie c = getCubieAt(x, -1, z);
                // Scan Direction: UP (Negative Y in JavaFX)
                sb.append(getVisibleFaceColor(c, new Point3D(0, -1, 0)));
            }
        }

        // 2. RIGHT Face (x = 1). Read Y: Top->Bottom, Z: Front->Back
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                Cubie c = getCubieAt(1, y, z);
                // Scan Direction: RIGHT (Positive X)
                sb.append(getVisibleFaceColor(c, new Point3D(1, 0, 0)));
            }
        }

        // 3. FRONT Face (z = -1). Read Y: Top->Bottom, X: Left->Right
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                Cubie c = getCubieAt(x, y, -1);
                // Scan Direction: FRONT (Negative Z)
                sb.append(getVisibleFaceColor(c, new Point3D(0, 0, -1)));
            }
        }

        // 4. DOWN Face (y = 1). Read Z: Front->Back, X: Left->Right
        for (int z = -1; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) {
                Cubie c = getCubieAt(x, 1, z);
                // Scan Direction: DOWN (Positive Y)
                sb.append(getVisibleFaceColor(c, new Point3D(0, 1, 0)));
            }
        }

        // 5. LEFT Face (x = -1). Read Y: Top->Bottom, Z: Back->Front
        for (int y = -1; y <= 1; y++) {
            for (int z = 1; z >= -1; z--) {
                Cubie c = getCubieAt(-1, y, z);
                // Scan Direction: LEFT (Negative X)
                sb.append(getVisibleFaceColor(c, new Point3D(-1, 0, 0)));
            }
        }

        // 6. BACK Face (z = 1). Read Y: Top->Bottom, X: Right->Left
        for (int y = -1; y <= 1; y++) {
            for (int x = 1; x >= -1; x--) {
                Cubie c = getCubieAt(x, y, 1);
                // Scan Direction: BACK (Positive Z)
                sb.append(getVisibleFaceColor(c, new Point3D(0, 0, 1)));
            }
        }

        return sb.toString();
    }
    // --- LOGIC CLASS ---
    private class Cubie extends Group {
        Translate t = new Translate();
        int lx, ly, lz; 
        final int startX, startY, startZ;
        
        // 0:U, 1:R, 2:F, 3:D, 4:L, 5:B
        Color[] faceColors = new Color[6]; 
        Box[] faceNodes = new Box[6]; // Keep track of physical boxes to paint them

        public Cubie(double size, int x, int y, int z) {
            this.lx = x; this.ly = y; this.lz = z;
            this.startX = x; this.startY = y; this.startZ = z;
            
            t.setX(x * (size + GAP));
            t.setY(y * (size + GAP));
            t.setZ(z * (size + GAP));
            getTransforms().add(t);

            double s = size / 2;
            
            // --- FIX: Initialize all faces to BLACK (Internal Plastic) ---
            Arrays.fill(faceColors, Color.BLACK);

            // --- Only Paint the Outer Faces based on Position ---
            // U=Yellow, D=White, F=Blue, B=Green, R=Red, L=Orange
            
            if (y == -1) faceColors[0] = Color.YELLOW; // Top Layer -> Paint Up Face
            if (x == 1)  faceColors[1] = Color.RED;    // Right Layer -> Paint Right Face
            if (z == -1) faceColors[2] = Color.BLUE;   // Front Layer -> Paint Front Face
            if (y == 1)  faceColors[3] = Color.WHITE;  // Bottom Layer -> Paint Down Face
            if (x == -1) faceColors[4] = Color.ORANGE; // Left Layer -> Paint Left Face
            if (z == 1)  faceColors[5] = Color.GREEN;  // Back Layer -> Paint Back Face

            // Create Physical Faces
            faceNodes[0] = createFace(faceColors[0], 0, -s, 0, Rotate.X_AXIS, 90);   // U
            faceNodes[1] = createFace(faceColors[1], s, 0, 0, Rotate.Y_AXIS, 90);    // R
            faceNodes[2] = createFace(faceColors[2], 0, 0, -s, Rotate.Y_AXIS, 0);    // F
            faceNodes[3] = createFace(faceColors[3], 0, s, 0, Rotate.X_AXIS, 90);    // D
            faceNodes[4] = createFace(faceColors[4], -s, 0, 0, Rotate.Y_AXIS, 90);   // L
            faceNodes[5] = createFace(faceColors[5], 0, 0, s, Rotate.Y_AXIS, 0);     // B
            
            getChildren().addAll(faceNodes);
        }
        
        public void updateCoordinates(String axis, double angle) {
            int dir = (angle > 0) ? 1 : -1;
            int oldX = lx; int oldY = ly; int oldZ = lz;

            // Update only the LOGICAL position. 
            // The Visual position is handled by the Transforms chain.
            switch (axis) {
                case "X": 
                    ly = (dir==1)? -oldZ : oldZ; 
                    lz = (dir==1)? oldY : -oldY; 
                    break;
                case "Y": 
                    lx = (dir==1)? oldZ : -oldZ; 
                    lz = (dir==1)? -oldX : oldX; 
                    break;
                case "Z": 
                    lx = (dir==1)? -oldY : oldY; 
                    ly = (dir==1)? oldX : -oldX; 
                    break;
            }
        }

        public void updateFaceColors(String axis, double angle) {
             int dir = (angle > 0) ? 1 : -1;
             // 0:U, 1:R, 2:F, 3:D, 4:L, 5:B
             if (axis.equals("X")) {
                 if (dir == 1) cycleFaces(0, 2, 3, 5); // U->F->D->B
                 else cycleFaces(0, 5, 3, 2);          // U->B->D->F
             } else if (axis.equals("Y")) {
                 if (dir == 1) cycleFaces(1, 2, 4, 5); // R->F->L->B
                 else cycleFaces(1, 5, 4, 2);          // R->B->L->F
             } else if (axis.equals("Z")) {
                 if (dir == 1) cycleFaces(0, 1, 3, 4); // U->R->D->L
                 else cycleFaces(0, 4, 3, 1);          // U->L->D->R
             }
        }

        private void cycleFaces(int a, int b, int c, int d) {
            Color tColor = faceColors[d];
            Box tBox = faceNodes[d];

            faceColors[d] = faceColors[c];
            faceNodes[d] = faceNodes[c];

            faceColors[c] = faceColors[b];
            faceNodes[c] = faceNodes[b];

            faceColors[b] = faceColors[a];
            faceNodes[b] = faceNodes[a];

            faceColors[a] = tColor;
            faceNodes[a] = tBox;
        }
        
        // Helper just to silence compiler, logic is inline above
      

        private Box createFace(Color c, double tx, double ty, double tz, Point3D axis, double angle) {
            Box face = new Box(CUBE_SIZE, CUBE_SIZE, 1);
            face.setMaterial(new PhongMaterial(c));
            face.setTranslateX(tx); face.setTranslateY(ty); face.setTranslateZ(tz);
            face.setRotationAxis(axis); face.setRotate(angle);
            return face;
        }
    }

    private String getHash(int x, int y, int z) { return x + "," + y + "," + z; }

    // --- ANIMATION ---
    private void scrambleCube() {
        if(isAnimating) return;
        log("Generating Scramble...");
        String[] moves = {"R", "L", "U", "D", "F", "B"}; // simplified scramble for stability
        StringBuilder scramble = new StringBuilder();
        Random rand = new Random();
        for(int i=0; i<15; i++) {
            scramble.append(moves[rand.nextInt(moves.length)]).append(" ");
        }
        log("Scramble: " + scramble);
        animateSequence(scramble.toString());
    }

    private void startSolve() {
        if(isAnimating) return;
        
        // 1. Get current state (This reads the PAINTED colors correctly)
        String cubeString = generateString();
        log("Scanned State: " + cubeString);

        // --- FIX START ---
        // OLD: if (checkSolved()) { ... } 
        // PROBLEM: This only checked if blocks moved, not if colors changed.
        
        // NEW: Check if the COLOR string matches a solved cube.
        String solvedState = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";
        if (cubeString.equals(solvedState)) {
            log("Cube is already solved!");
            return;
        }
        // --- FIX END ---
        
        // 2. Validation check (Min2Phase is strict!)
        if (cubeString.length() != 54 || cubeString.contains("?")) {
            log("ERROR: Invalid scan. Please check all faces are painted.");
            return;
        }

        log("Computing solution...");
        isAnimating = true;

        final String currentCubeType = cubeTypeSelector.getValue();

        Task<String> solverTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                // 3. Run the Min2Phase Solver
                // solution(state, maxDepth, timeOut, 0, 0)
                return new Search().solution(cubeString, 21, 100000000, 0, 0);
            }
        };

        solverTask.setOnSucceeded(e -> {
            String solution = solverTask.getValue();

            // 4. Check for Solver Errors
            if (solution.startsWith("Error")) {
                log("SOLVER ERROR: " + solution);
                log("Check that you have exactly 9 of each color.");
                isAnimating = false;
                return;
            }

            // 5. Build Final Algorithm (Standard + Windmill Fixes)
            StringBuilder finalAlgorithm = new StringBuilder(solution);

            if (currentCubeType.equals("Windmill 3x3")) {
                log("Mode: Windmill 3x3. Checking centers...");
                // We append a comment for the user to see in the text box
                finalAlgorithm.append(" [Check Centers]");
            }

            log("Solution Found: " + solution);
            algorithmDisplay.setText(finalAlgorithm.toString());
            animateSequence(finalAlgorithm.toString());
        });

        solverTask.setOnFailed(e -> {
            log("Solver failed: " + solverTask.getException().getMessage());
            isAnimating = false;
        });

        new Thread(solverTask).start();
    }

    private void animateSequence(String seq) {
        moveQueue.clear();
        moveQueue.addAll(Arrays.asList(seq.split("\\s+")));
        playNextMove();
    }

    private void playNextMove() {
        if (moveQueue.isEmpty()) {
            isAnimating = false;
            if(checkSolved()) log("Cube Solved!");
            return;
        }
        isAnimating = true;
        doTurn(moveQueue.poll());
    }

    private void doTurn(String move) {
        if(move.trim().isEmpty()) { playNextMove(); return; }
        
        boolean cw = !move.contains("'");
        String axisChar = move.replace("'", "").replace("2", "");
        boolean doubleMove = move.contains("2");

        // 1. Define Logic (Standard Rubik's definitions)
        // We determine the Rotation Axis and the 'Direction' relative to that axis.
        // Base Direction 1 means "Clockwise" relative to the standard face.
        String rotAxis = "Y";
        double logicalDirection = 0; // 1 for CW, -1 for CCW

        switch (axisChar) {
            case "U": rotAxis="Y"; logicalDirection = 1;  break; // U  (CW around Y)
            case "D": rotAxis="Y"; logicalDirection = -1; break; // D  (Opposite of U)
            case "E": rotAxis="Y"; logicalDirection = -1; break; // E  (Matches D)
            
            case "L": rotAxis="X"; logicalDirection = 1;  break; // L  (CW around X)
            case "R": rotAxis="X"; logicalDirection = -1; break; // R  (Opposite of L)
            case "M": rotAxis="X"; logicalDirection = 1;  break; // M  (Matches L)
            
            case "F": rotAxis="Z"; logicalDirection = 1;  break; // F  (CW around Z)
            case "B": rotAxis="Z"; logicalDirection = -1; break; // B  (Opposite of F)
        }

        // 2. Adjust for Prime (') and Double (2) moves
        if (!cw) logicalDirection *= -1; // Reverse direction for Prime
        double angleMagnitude = doubleMove ? 180 : 90;
        
        // This is the angle we pass to the Logic Class
        final double logicalAngle = logicalDirection * angleMagnitude; 

        // 3. Define Visuals (JavaFX Axis Quirks)
        // JavaFX axes are: Y-Down, Z-Away. Standard math is Y-Up, Z-Towards.
        // This means Y and Z visual rotations might need to be flipped to match the logic.
        double visualAngle = logicalAngle;
        
        // 4. Select Cubies
        // Determine layer: -1 (Top/Left/Front), 1 (Bottom/Right/Back), 0 (Middle)
        int layer = 0;
        if (axisChar.equals("U") || axisChar.equals("L") || axisChar.equals("F")) layer = -1;
        if (axisChar.equals("D") || axisChar.equals("R") || axisChar.equals("B")) layer = 1;

        List<Cubie> targetCubies = new ArrayList<>();
        for (Cubie c : cubies.values()) {
            if (rotAxis.equals("X") && c.lx == layer) targetCubies.add(c);
            if (rotAxis.equals("Y") && c.ly == layer) targetCubies.add(c);
            if (rotAxis.equals("Z") && c.lz == layer) targetCubies.add(c);
        }

        if(targetCubies.isEmpty()) {
            playNextMove(); return;
        }

        // 5. Animate
        Group rotationGroup = new Group();
        cubeGroup.getChildren().removeAll(targetCubies);
        rotationGroup.getChildren().addAll(targetCubies);
        cubeGroup.getChildren().add(rotationGroup);

        RotateTransition rt = new RotateTransition(Duration.millis(animationSpeed), rotationGroup);
        if (rotAxis.equals("X")) rt.setAxis(Rotate.X_AXIS);
        else if (rotAxis.equals("Y")) rt.setAxis(Rotate.Y_AXIS);
        else rt.setAxis(Rotate.Z_AXIS);
        
        rt.setByAngle(visualAngle);

        final double angleForLogic = logicalAngle;
        final String axisForLogic = rotAxis;
        final boolean isDouble = doubleMove;
        
        // --- FIX: Create a final copy of visualAngle ---
        final double finalVisualAngle = visualAngle; 

        rt.setOnFinished(e -> {
            for(Cubie c : targetCubies) {
                // A. Update Logical Grid (Use strict Logical Angle)
                c.updateCoordinates(axisForLogic, angleForLogic);
                c.updateFaceColors(axisForLogic, angleForLogic);

                if(isDouble) {
                    c.updateCoordinates(axisForLogic, angleForLogic);
                    c.updateFaceColors(axisForLogic, angleForLogic);
                }

                // --- DELETE SECTION B (Update Physical Position) ---
                // The accumulated Rotation below handles the physical move automatically.
                // Do NOT update c.t.setX/Y/Z here.

                // C. Update Orientation
                // Use 'finalVisualAngle' here instead of 'visualAngle'
                c.getTransforms().add(new Rotate(finalVisualAngle, rt.getAxis()));
            }
            
            cubeGroup.getChildren().remove(rotationGroup);
            rotationGroup.getChildren().removeAll(targetCubies);
            cubeGroup.getChildren().addAll(targetCubies);

            Platform.runLater(this::playNextMove);
        });

        rt.play();
    }

    private boolean checkSolved() {
        for (Cubie c : cubies.values()) {
            if (c.lx != c.startX || c.ly != c.startY || c.lz != c.startZ) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        launch(args);
    }
}