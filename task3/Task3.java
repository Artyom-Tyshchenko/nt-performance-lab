import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Task3 {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Использование: java Task3 <values.json> <tests.json> <report.json>");
            System.exit(1);
        }

        String valuesPath = args[0];
        String testsPath = args[1];
        String reportPath = args[2];

        try {
            String valuesContent = readFile(valuesPath);
            String testsContent = readFile(testsPath);

            Object valuesRoot = new JsonParser(valuesContent).parse();
            Object testsRoot = new JsonParser(testsContent).parse();

            Map<Long, Object> valueById = buildValueMap(valuesRoot);

            fillValues(testsRoot, valueById);

            String output = JsonWriter.write(testsRoot);

            try (FileWriter writer = new FileWriter(reportPath)) {
                writer.write(output);
            }

            System.out.println("Отчёт успешно записан в " + reportPath);

        } catch (IOException e) {
            System.err.println("Ошибка чтения/записи файла: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("Ошибка обработки JSON: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), java.nio.charset.StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Object> buildValueMap(Object valuesRoot) {
        Map<Long, Object> result = new LinkedHashMap<>();
        if (!(valuesRoot instanceof Map)) {
            throw new RuntimeException("values.json должен содержать объект верхнего уровня");
        }
        Map<String, Object> rootMap = (Map<String, Object>) valuesRoot;
        Object valuesArrObj = rootMap.get("values");
        if (!(valuesArrObj instanceof List)) {
            throw new RuntimeException("Поле 'values' должно быть массивом");
        }
        List<Object> valuesArr = (List<Object>) valuesArrObj;
        for (Object item : valuesArr) {
            if (item instanceof Map) {
                Map<String, Object> obj = (Map<String, Object>) item;
                Object idObj = obj.get("id");
                Object valObj = obj.get("value");
                long id = toLong(idObj);
                result.put(id, valObj);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void fillValues(Object node, Map<Long, Object> valueById) {
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;

            if (map.containsKey("id") && map.containsKey("value")) {
                long id = toLong(map.get("id"));
                if (valueById.containsKey(id)) {
                    map.put("value", valueById.get(id));
                }
            }

            if (map.containsKey("tests")) {
                fillValues(map.get("tests"), valueById);
            }
            if (map.containsKey("values")) {
                fillValues(map.get("values"), valueById);
            }

        } else if (node instanceof List) {
            List<Object> list = (List<Object>) node;
            for (Object item : list) {
                fillValues(item, valueById);
            }
        }
    }

    private static long toLong(Object numObj) {
        if (numObj instanceof Long) return (Long) numObj;
        if (numObj instanceof Double) return ((Double) numObj).longValue();
        if (numObj instanceof Number) return ((Number) numObj).longValue();
        throw new RuntimeException("Ожидалось числовое значение id, получено: " + numObj);
    }

    static class JsonParser {
        private final String s;
        private int pos = 0;

        JsonParser(String s) {
            this.s = s;
        }

        Object parse() {
            skipWhitespace();
            Object result = parseValue();
            skipWhitespace();
            return result;
        }

        private Object parseValue() {
            skipWhitespace();
            char c = peek();
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't':
                case 'f': return parseBoolean();
                case 'n': return parseNull();
                default: return parseNumber();
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char c = next();
                if (c == '}') break;
                if (c != ',') throw new RuntimeException("Ожидалась ',' или '}' в позиции " + pos);
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                char c = next();
                if (c == ']') break;
                if (c != ',') throw new RuntimeException("Ожидалась ',' или ']' в позиции " + pos);
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            String hex = s.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default: throw new RuntimeException("Неизвестный escape-символ: \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object parseNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            boolean isDouble = false;
            if (pos < s.length() && s.charAt(pos) == '.') {
                isDouble = true;
                pos++;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            }
            if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                isDouble = true;
                pos++;
                if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            }
            String numStr = s.substring(start, pos);
            if (numStr.isEmpty() || numStr.equals("-")) {
                throw new RuntimeException("Некорректное число в позиции " + start);
            }
            if (isDouble) {
                return Double.parseDouble(numStr);
            } else {
                return Long.parseLong(numStr);
            }
        }

        private Boolean parseBoolean() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new RuntimeException("Ожидалось true/false в позиции " + pos);
        }

        private Object parseNull() {
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new RuntimeException("Ожидалось null в позиции " + pos);
        }

        private void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        private char peek() {
            if (pos >= s.length()) throw new RuntimeException("Неожиданный конец JSON");
            return s.charAt(pos);
        }

        private char next() {
            if (pos >= s.length()) throw new RuntimeException("Неожиданный конец JSON");
            return s.charAt(pos++);
        }

        private void expect(char c) {
            skipWhitespace();
            char actual = next();
            if (actual != c) throw new RuntimeException("Ожидался символ '" + c + "', найден '" + actual + "' в позиции " + (pos - 1));
        }
    }

    static class JsonWriter {

        static String write(Object node) {
            StringBuilder sb = new StringBuilder();
            writeValue(node, sb, 0);
            return sb.toString();
        }

        @SuppressWarnings("unchecked")
        private static void writeValue(Object node, StringBuilder sb, int indent) {
            if (node == null) {
                sb.append("null");
            } else if (node instanceof Map) {
                writeObject((Map<String, Object>) node, sb, indent);
            } else if (node instanceof List) {
                writeArray((List<Object>) node, sb, indent);
            } else if (node instanceof String) {
                writeString((String) node, sb);
            } else if (node instanceof Boolean) {
                sb.append(node.toString());
            } else if (node instanceof Long) {
                sb.append(node.toString());
            } else if (node instanceof Double) {
                double d = (Double) node;
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    sb.append((long) d);
                } else {
                    sb.append(d);
                }
            } else {
                sb.append(node.toString());
            }
        }

        private static void writeObject(Map<String, Object> map, StringBuilder sb, int indent) {
            if (map.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            int i = 0;
            int size = map.size();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                writeIndent(sb, indent + 1);
                writeString(entry.getKey(), sb);
                sb.append(": ");
                writeValue(entry.getValue(), sb, indent + 1);
                if (++i < size) sb.append(",");
                sb.append("\n");
            }
            writeIndent(sb, indent);
            sb.append("}");
        }

        private static void writeArray(List<Object> list, StringBuilder sb, int indent) {
            if (list.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            int size = list.size();
            for (int i = 0; i < size; i++) {
                writeIndent(sb, indent + 1);
                writeValue(list.get(i), sb, indent + 1);
                if (i < size - 1) sb.append(",");
                sb.append("\n");
            }
            writeIndent(sb, indent);
            sb.append("]");
        }

        private static void writeString(String str, StringBuilder sb) {
            sb.append('"');
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\t': sb.append("\\t"); break;
                    case '\r': sb.append("\\r"); break;
                    default: sb.append(c);
                }
            }
            sb.append('"');
        }

        private static void writeIndent(StringBuilder sb, int level) {
            for (int i = 0; i < level; i++) sb.append("  ");
        }
    }
}