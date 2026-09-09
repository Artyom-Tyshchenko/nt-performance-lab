import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class Task2 {

    private static final double EPS = 1e-9;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Использование: java Task2 <файл_эллипса> <файл_точек>");
            System.exit(1);
        }

        try {
            double[] ellipse = readEllipse(args[0]);
            double cx = ellipse[0];
            double cy = ellipse[1];
            double rx = ellipse[2];
            double ry = ellipse[3];

            List<double[]> points = readPoints(args[1]);

            StringBuilder output = new StringBuilder();
            for (double[] point : points) {
                int result = classifyPoint(point[0], point[1], cx, cy, rx, ry);
                output.append(result).append(System.lineSeparator());
            }

            System.out.print(output);

        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
            System.exit(1);
        }
    }

    private static double[] readEllipse(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            double[] center = parseLine(reader.readLine());
            double[] radius = parseLine(reader.readLine());
            return new double[]{center[0], center[1], radius[0], radius[1]};
        }
    }

    private static List<double[]> readPoints(String filePath) throws IOException {
        List<double[]> points = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                points.add(parseLine(line));
            }
        }
        return points;
    }

    private static double[] parseLine(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line.trim());
        double x = Double.parseDouble(tokenizer.nextToken().replace(',', '.'));
        double y = Double.parseDouble(tokenizer.nextToken().replace(',', '.'));
        return new double[]{x, y};
    }

    private static int classifyPoint(double x, double y, double cx, double cy, double rx, double ry) {
        double dx = (x - cx) / rx;
        double dy = (y - cy) / ry;
        double value = dx * dx + dy * dy;

        if (Math.abs(value - 1.0) < EPS) {
            return 0;
        } else if (value < 1.0) {
            return 1;
        } else {
            return 2;
        }
    }
}