import javafx.geometry.Point3D;
import javafx.scene.transform.Rotate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CubeState {
    private final Map<String, LogicalCubie> cubies = new HashMap<>();

    public CubeState() {
        buildCube();
    }

    public void buildCube() {
        cubies.clear();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    LogicalCubie cubie = new LogicalCubie(x, y, z);
                    cubies.put(getHash(x, y, z), cubie);
                }
            }
        }
    }

    private String getHash(int x, int y, int z) { return x + "," + y + "," + z; }

    private LogicalCubie getCubieAt(int x, int y, int z) {
        for (LogicalCubie c : cubies.values()) {
            if (c.lx == x && c.ly == y && c.lz == z) {
                return c;
            }
        }
        return null;
    }

    public void doTurn(String move) {
        if(move == null || move.trim().isEmpty()) return;

        boolean cw = !move.contains("'");
        String axisChar = move.replace("'", "").replace("2", "");
        boolean doubleMove = move.contains("2");

        // 1. Define Logic (Standard Rubik's definitions)
        String rotAxis = "Y";
        double logicalDirection = 0; // 1 for CW, -1 for CCW

        switch (axisChar) {
            case "U": rotAxis="Y"; logicalDirection = 1;  break;
            case "D": rotAxis="Y"; logicalDirection = -1; break;
            case "E": rotAxis="Y"; logicalDirection = -1; break;

            case "L": rotAxis="X"; logicalDirection = 1;  break;
            case "R": rotAxis="X"; logicalDirection = -1; break;
            case "M": rotAxis="X"; logicalDirection = 1;  break;

            case "F": rotAxis="Z"; logicalDirection = 1;  break;
            case "B": rotAxis="Z"; logicalDirection = -1; break;
        }

        if (!cw) logicalDirection *= -1;
        double angleMagnitude = doubleMove ? 180 : 90;

        final double logicalAngle = logicalDirection * angleMagnitude;

        // 4. Select Cubies
        int layer = 0;
        if (axisChar.equals("U") || axisChar.equals("L") || axisChar.equals("F")) layer = -1;
        if (axisChar.equals("D") || axisChar.equals("R") || axisChar.equals("B")) layer = 1;

        List<LogicalCubie> targetCubies = new ArrayList<>();
        for (LogicalCubie c : cubies.values()) {
            if (rotAxis.equals("X") && c.lx == layer) targetCubies.add(c);
            if (rotAxis.equals("Y") && c.ly == layer) targetCubies.add(c);
            if (rotAxis.equals("Z") && c.lz == layer) targetCubies.add(c);
        }

        Point3D axis = Rotate.Y_AXIS;
        if (rotAxis.equals("X")) axis = Rotate.X_AXIS;
        else if (rotAxis.equals("Z")) axis = Rotate.Z_AXIS;

        for(LogicalCubie c : targetCubies) {
            // A. Update Logical Grid
            c.updateCoordinates(rotAxis, logicalAngle);
            c.updateFaceColors(rotAxis, logicalAngle);

            if(doubleMove) {
                c.updateCoordinates(rotAxis, logicalAngle);
                c.updateFaceColors(rotAxis, logicalAngle);
            }

            // C. Update Orientation
            c.addTransform(new Rotate(logicalAngle, axis));
        }
    }

    public String generateString() {
        StringBuilder sb = new StringBuilder();

        // 1. UP Face (y = -1). Read Z: Back->Front, X: Left->Right
        for (int z = 1; z >= -1; z--) {
            for (int x = -1; x <= 1; x++) {
                LogicalCubie c = getCubieAt(x, -1, z);
                sb.append(c.getVisibleFaceColor(new Point3D(0, -1, 0)));
            }
        }

        // 2. RIGHT Face (x = 1). Read Y: Top->Bottom, Z: Front->Back
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                LogicalCubie c = getCubieAt(1, y, z);
                sb.append(c.getVisibleFaceColor(new Point3D(1, 0, 0)));
            }
        }

        // 3. FRONT Face (z = -1). Read Y: Top->Bottom, X: Left->Right
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                LogicalCubie c = getCubieAt(x, y, -1);
                sb.append(c.getVisibleFaceColor(new Point3D(0, 0, -1)));
            }
        }

        // 4. DOWN Face (y = 1). Read Z: Front->Back, X: Left->Right
        for (int z = -1; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) {
                LogicalCubie c = getCubieAt(x, 1, z);
                sb.append(c.getVisibleFaceColor(new Point3D(0, 1, 0)));
            }
        }

        // 5. LEFT Face (x = -1). Read Y: Top->Bottom, Z: Back->Front
        for (int y = -1; y <= 1; y++) {
            for (int z = 1; z >= -1; z--) {
                LogicalCubie c = getCubieAt(-1, y, z);
                sb.append(c.getVisibleFaceColor(new Point3D(-1, 0, 0)));
            }
        }

        // 6. BACK Face (z = 1). Read Y: Top->Bottom, X: Right->Left
        for (int y = -1; y <= 1; y++) {
            for (int x = 1; x >= -1; x--) {
                LogicalCubie c = getCubieAt(x, y, 1);
                sb.append(c.getVisibleFaceColor(new Point3D(0, 0, 1)));
            }
        }

        return sb.toString();
    }
}
