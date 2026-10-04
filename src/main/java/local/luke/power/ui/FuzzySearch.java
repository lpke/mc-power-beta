package local.luke.power.ui;

import java.text.Normalizer;
import java.util.Locale;

/** Ranked words, abbreviations and small typos. Runs only when the result list changes. */
public final class FuzzySearch {
  public static final int NONE = 1_000_000;

  private FuzzySearch() {}

  private static String normalize(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  public static int score(String query, String title, String context) {
    query = normalize(query).strip();
    if (query.isEmpty()) return 0;
    title = normalize(title);
    context = normalize(context);
    if (title.contains(query)) return title.startsWith(query) ? 0 : 1;
    if (!query.contains(" ") && query.length() >= 3) {
      String compactTitle = title.replaceAll("[^\\p{L}\\p{N}]", "");
      if (compactTitle.contains(query)) return 3;
      int at = 0;
      for (int i = 0; i < compactTitle.length() && at < query.length(); i++)
        if (compactTitle.charAt(i) == query.charAt(at)) at++;
      if (at == query.length() && compactTitle.length() <= query.length() * 2 + 2)
        return 9 + compactTitle.length() - query.length();
    }
    int total = 0;
    for (String term : query.split("\\s+")) {
      int best = Math.min(words(term, title), add(words(term, context), 30));
      if (best == NONE) return NONE;
      total += best;
    }
    return total;
  }

  private static int add(int score, int penalty) {
    return score == NONE ? NONE : score + penalty;
  }

  private static int words(String term, String value) {
    if (value.contains(term)) return 2;
    int best = NONE;
    for (String word : value.split("[^\\p{L}\\p{N}]+")) {
      if (term.length() >= 3 && word.length() >= term.length()) {
        int at = 0;
        for (int i = 0; i < word.length() && at < term.length(); i++)
          if (word.charAt(i) == term.charAt(at)) at++;
        if (at == term.length() && word.length() <= term.length() * 2 + 2)
          best = Math.min(best, 8 + word.length() - term.length());
      }
      int allowed = term.length() >= 8 ? 2 : term.length() >= 4 ? 1 : 0;
      if (allowed > 0 && Math.abs(term.length() - word.length()) <= allowed) {
        int distance = distance(term, word);
        if (distance <= allowed) best = Math.min(best, 12 + distance);
      }
    }
    // Initial letters let "rd" find "render distance" without matching arbitrary description gaps.
    if (term.length() >= 2) {
      StringBuilder initials = new StringBuilder();
      for (String word : value.split("[^\\p{L}\\p{N}]+"))
        if (!word.isEmpty()) initials.append(word.charAt(0));
      if (initials.toString().equals(term)) best = Math.min(best, 6);
    }
    return best;
  }

  private static int distance(String a, String b) {
    int[][] d = new int[a.length() + 1][b.length() + 1];
    for (int i = 0; i <= a.length(); i++) d[i][0] = i;
    for (int j = 0; j <= b.length(); j++) d[0][j] = j;
    for (int i = 1; i <= a.length(); i++)
      for (int j = 1; j <= b.length(); j++) {
        d[i][j] =
            Math.min(
                Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                d[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
        if (i > 1
            && j > 1
            && a.charAt(i - 1) == b.charAt(j - 2)
            && a.charAt(i - 2) == b.charAt(j - 1)) d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
      }
    return d[a.length()][b.length()];
  }
}
