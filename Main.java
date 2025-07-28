import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String[] files = { "testcase1.json", "testcase2.json" };
        for (String file : files) {
            try {
                BigInteger secret = computeSecretFromFile(file);
                System.out.println("Secret from " + file + ": " + secret);
                System.out.println();
            } catch (Exception e) {
                System.err.println("Error processing " + file + ": " + e.getMessage());
                System.out.println();
            }
        }
    }

    public static BigInteger computeSecretFromFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        String json = sb.toString();

        int k = 0, n = 0;
        int keysStart = json.indexOf("\"keys\"");
        if (keysStart != -1) {
            int braceStart = json.indexOf("{", keysStart);
            int braceEnd = json.indexOf("}", braceStart);
            String keysContent = json.substring(braceStart + 1, braceEnd).replaceAll("[\"\\s]", "");
            String[] keyParts = keysContent.split(",");
            for (String part : keyParts) {
                String[] kv = part.split(":");
                if (kv[0].equals("k")) k = Integer.parseInt(kv[1]);
                if (kv[0].equals("n")) n = Integer.parseInt(kv[1]);
            }
        }

        List<BigInteger> xList = new ArrayList<>();
        List<BigInteger> yList = new ArrayList<>();

        for (int i = 1; i <= n; i++) {
            String key = "\"" + i + "\"";
            int keyIndex = json.indexOf(key);
            if (keyIndex == -1) continue;

            int braceStart = json.indexOf("{", keyIndex);
            int braceEnd = json.indexOf("}", braceStart);
            if (braceStart == -1 || braceEnd == -1) continue;

            String content = json.substring(braceStart + 1, braceEnd).replaceAll("[\"\\s]", "");
            String[] parts = content.split(",");
            String baseStr = "", valueStr = "";

            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv[0].equals("base")) baseStr = kv[1];
                else if (kv[0].equals("value")) valueStr = kv[1];
            }

            try {
                int base = Integer.parseInt(baseStr);
                BigInteger x = BigInteger.valueOf(i);
                BigInteger y = new BigInteger(valueStr, base);
                xList.add(x);
                yList.add(y);
            } catch (Exception ignored) {}
        }

        if (xList.size() < k)
            throw new IllegalArgumentException("Not enough valid points. Required: " + k + ", Found: " + xList.size());

        Map<BigInteger, Integer> freq = new HashMap<>();
        Map<BigInteger, List<List<Integer>>> combinationsMap = new HashMap<>();
        List<List<Integer>> allIndices = generateCombinations(xList.size(), k);

        for (List<Integer> indices : allIndices) {
            BigInteger[] xs = new BigInteger[k];
            BigInteger[] ys = new BigInteger[k];
            for (int i = 0; i < k; i++) {
                xs[i] = xList.get(indices.get(i));
                ys[i] = yList.get(indices.get(i));
            }

            BigInteger secret = lagrangeInterpolationAtZero(xs, ys);
            freq.put(secret, freq.getOrDefault(secret, 0) + 1);
            combinationsMap.computeIfAbsent(secret, v -> new ArrayList<>()).add(indices);
        }

        BigInteger finalSecret = null;
        int maxCount = 0;
        for (Map.Entry<BigInteger, Integer> entry : freq.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                finalSecret = entry.getKey();
            }
        }

        List<List<Integer>> validCombos = combinationsMap.get(finalSecret);
        Set<Integer> validIndices = new HashSet<>();
        for (List<Integer> combo : validCombos) {
            validIndices.addAll(combo);
        }

        List<BigInteger> badPoints = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            if (!validIndices.contains(i)) {
                badPoints.add(xList.get(i));
            }
        }

        System.out.println("Valid combinations agree on secret: " + finalSecret);
        if (!badPoints.isEmpty()) {
            System.out.print("Discarded points: ");
            for (BigInteger bad : badPoints) {
                System.out.print(bad + " ");
            }
            System.out.println();
        }

        // Recover polynomial from best combination
        List<Integer> bestCombo = validCombos.get(0); // take any valid combo
        BigInteger[] xs = new BigInteger[k];
        BigInteger[] ys = new BigInteger[k];
        for (int i = 0; i < k; i++) {
            xs[i] = xList.get(bestCombo.get(i));
            ys[i] = yList.get(bestCombo.get(i));
        }

        List<BigInteger> coeffs = recoverPolynomialCoefficients(xs, ys);
        System.out.println("Polynomial coefficients (lowest to highest degree): " + coeffs);

        return finalSecret;
    }

    public static BigInteger lagrangeInterpolationAtZero(BigInteger[] x, BigInteger[] y) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < x.length; i++) {
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;
            for (int j = 0; j < x.length; j++) {
                if (i != j) {
                    num = num.multiply(x[j].negate());
                    den = den.multiply(x[i].subtract(x[j]));
                }
            }
            result = result.add(y[i].multiply(num).divide(den));
        }
        return result;
    }

    public static List<List<Integer>> generateCombinations(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        combineHelper(0, n, k, new ArrayList<>(), result);
        return result;
    }

    private static void combineHelper(int start, int n, int k, List<Integer> path, List<List<Integer>> result) {
        if (path.size() == k) {
            result.add(new ArrayList<>(path));
            return;
        }
        for (int i = start; i < n; i++) {
            path.add(i);
            combineHelper(i + 1, n, k, path, result);
            path.remove(path.size() - 1);
        }
    }

    public static List<BigInteger> recoverPolynomialCoefficients(BigInteger[] x, BigInteger[] y) {
        int k = x.length;
        BigInteger[][] matrix = new BigInteger[k][k];
        BigInteger[] rhs = new BigInteger[k];

        // Construct Vandermonde matrix
        for (int i = 0; i < k; i++) {
            BigInteger xi = BigInteger.ONE;
            for (int j = 0; j < k; j++) {
                matrix[i][j] = xi;
                xi = xi.multiply(x[i]);
            }
            rhs[i] = y[i];
        }

        // Solve matrix using Gauss-Jordan elimination
        return gaussJordan(matrix, rhs);
    }

    public static List<BigInteger> gaussJordan(BigInteger[][] a, BigInteger[] b) {
        int n = b.length;
        for (int i = 0; i < n; i++) {
            // Make diagonal 1
            BigInteger factor = a[i][i];
            for (int j = 0; j < n; j++) {
                a[i][j] = a[i][j].divide(factor);
            }
            b[i] = b[i].divide(factor);

            // Eliminate other rows
            for (int r = 0; r < n; r++) {
                if (r != i) {
                    BigInteger mult = a[r][i];
                    for (int c = 0; c < n; c++) {
                        a[r][c] = a[r][c].subtract(a[i][c].multiply(mult));
                    }
                    b[r] = b[r].subtract(b[i].multiply(mult));
                }
            }
        }

        return Arrays.asList(b);
    }
}
