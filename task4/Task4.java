import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Task4 {

    private static final int MAX_MOVES = 20;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Использование: java Task4 <файл_с_числами>");
            System.exit(1);
        }

        try {
            List<Integer> nums = readNumbers(args[0]);

            if (nums.isEmpty()) {
                System.err.println("Файл не содержит чисел");
                System.exit(1);
            }

            long minMoves = calculateMinMoves(nums);

            if (minMoves > MAX_MOVES) {
                System.out.println(MAX_MOVES + " ходов недостаточно для приведения всех элементов массива к одному числу.");
            } else {
                System.out.println(minMoves);
            }

        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
            System.exit(1);
        } catch (NumberFormatException e) {
            System.err.println("Файл содержит некорректное число: " + e.getMessage());
            System.exit(1);
        }
    }

    private static List<Integer> readNumbers(String filePath) throws IOException {
        List<Integer> numbers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    numbers.add(Integer.parseInt(line));
                }
            }
        }
        return numbers;
    }

    private static long calculateMinMoves(List<Integer> nums) {
        List<Integer> sorted = new ArrayList<>(nums);
        Collections.sort(sorted);

        int median = sorted.get(sorted.size() / 2);

        long moves = 0;
        for (int num : sorted) {
            moves += Math.abs(num - median);
        }

        return moves;
    }
}