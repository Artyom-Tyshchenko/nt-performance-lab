import java.util.HashSet;
import java.util.Set;

public class Task1 {

  public static String circularPath(int n, int m) {
    StringBuilder path = new StringBuilder();
    int start = 0;
    Set<Integer> visitedStarts = new HashSet<>();

    while (true) {
      path.append(start + 1);

      int end = (start + m - 1) % n;

      if (end == 0) {
        break;
      }

      start = end;

      if (visitedStarts.contains(start)) {
        break;
      }
      visitedStarts.add(start);
    }

    return path.toString();
  }

  public static void main(String[] args) {
    if (args.length != 4) {
      System.err.println("Использование: java Task1 n1 m1 n2 m2");
      System.exit(1);
    }

    int n1 = Integer.parseInt(args[0]);
    int m1 = Integer.parseInt(args[1]);
    int n2 = Integer.parseInt(args[2]);
    int m2 = Integer.parseInt(args[3]);

    String path1 = circularPath(n1, m1);
    String path2 = circularPath(n2, m2);

    System.out.println(path1 + path2);
  }
}