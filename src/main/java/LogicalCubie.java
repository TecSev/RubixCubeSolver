import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogicalCubie {
    public int lx, ly, lz;
    public final int startX, startY, startZ;

    // 0:U, 1:R, 2:F, 3:D, 4:L, 5:B
    public Color[] faceColors = new Color[6];
    public List<Transform> transforms = new ArrayList<>();

    public LogicalCubie(int x, int y, int z) {
        this.lx = x; this.ly = y; this.lz = z;
        this.startX = x; this.startY = y; this.startZ = z;

        // --- Initialize all faces to BLACK (Internal Plastic) ---
        Arrays.fill(faceColors, Color.BLACK);

        // --- Only Paint the Outer Faces based on Position ---
        if (y == -1) faceColors[0] = Color.YELLOW; // Top Layer -> Paint Up Face
        if (x == 1)  faceColors[1] = Color.RED;    // Right Layer -> Paint Right Face
        if (z == -1) faceColors[2] = Color.BLUE;   // Front Layer -> Paint Front Face
        if (y == 1)  faceColors[3] = Color.WHITE;  // Bottom Layer -> Paint Down Face
        if (x == -1) faceColors[4] = Color.ORANGE; // Left Layer -> Paint Left Face
        if (z == 1)  faceColors[5] = Color.GREEN;  // Back Layer -> Paint Back Face
    }

    public void updateCoordinates(String axis, double angle) {
        int dir = (angle > 0) ? 1 : -1;
        int oldX = lx; int oldY = ly; int oldZ = lz;

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

    // Logic fix: Do not permute faceColors.
    // The visual orientation is handled by the transforms.
    public void updateFaceColors(String axis, double angle) {
        // No-op
    }

    public void addTransform(Transform t) {
        transforms.add(t);
    }

    public char getVisibleFaceColor(Point3D scanDirection) {
        // Transform the global scan direction into the Cubie's local coordinate system
        Point3D localDir = scanDirection;
        try {
            // Iterate transforms in reverse order to apply inverse logic correctly
            for (int i = transforms.size() - 1; i >= 0; i--) {
                Transform t = transforms.get(i);
                localDir = t.inverseDeltaTransform(localDir);
            }
        } catch (javafx.scene.transform.NonInvertibleTransformException e) {
            e.printStackTrace();
            return 'X';
        }

        // Determine which face index matches the LOCAL scan direction
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
            return colorToChar(faceColors[faceIndex]);
        }
        return '?';
    }

    private char colorToChar(Color c) {
        if (c.equals(Color.YELLOW)) return 'U';
        if (c.equals(Color.RED))    return 'R';
        if (c.equals(Color.BLUE))   return 'F';
        if (c.equals(Color.WHITE))  return 'D';
        if (c.equals(Color.ORANGE)) return 'L';
        if (c.equals(Color.GREEN))  return 'B';
        return 'X';
    }
}
