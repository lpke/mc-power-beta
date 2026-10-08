package local.luke.power.worldedit.core;

import java.util.*;

/** Bounded arithmetic expressions, with no scripting or world access. */
public final class Calculator {
  private final String input;
  private int at, depth;
  private Calculator(String input) { this.input = input; }
  public static double evaluate(String input) {
    if (input.isBlank() || input.length() > 2048) throw new IllegalArgumentException("Use //calc expression, up to 2048 characters.");
    Calculator p = new Calculator(input);
    double result = p.sum(); p.space();
    if (p.at != input.length() || !Double.isFinite(result)) throw p.error();
    return result;
  }
  private IllegalArgumentException error() { return new IllegalArgumentException("Invalid calculation near character " + (at + 1) + "."); }
  private void space() { while (at < input.length() && Character.isWhitespace(input.charAt(at))) at++; }
  private boolean take(char c) { space(); if (at < input.length() && input.charAt(at) == c) { at++; return true; } return false; }
  private double sum() {
    double v = product();
    while (true) { if (take('+')) v += product(); else if (take('-')) v -= product(); else return v; }
  }
  private double product() {
    double v = unary();
    while (true) { if (take('*')) v *= unary(); else if (take('/')) v /= unary(); else if (take('%')) v %= unary(); else return v; }
  }
  private double unary() {
    if (++depth > 64) throw error();
    try {
      if (take('+')) return unary();
      if (take('-')) return -unary();
      double v = atom();
      return take('^') ? Math.pow(v, unary()) : v;
    } finally { depth--; }
  }
  private double atom() {
    if (take('(')) { double v = sum(); if (!take(')')) throw error(); return v; }
    space(); int start = at;
    if (at < input.length() && Character.isLetter(input.charAt(at))) {
      while (at < input.length() && Character.isLetterOrDigit(input.charAt(at))) at++;
      String name = input.substring(start, at).toLowerCase(Locale.ROOT);
      if (name.equals("pi")) return Math.PI;
      if (name.equals("e")) return Math.E;
      if (!take('(')) throw error();
      List<Double> args = new ArrayList<>(); args.add(sum());
      while (take(',')) { if (args.size() >= 2) throw error(); args.add(sum()); }
      if (!take(')')) throw error();
      if (args.size() == 2) return switch (name) {
        case "min" -> Math.min(args.get(0), args.get(1)); case "max" -> Math.max(args.get(0), args.get(1));
        case "pow" -> Math.pow(args.get(0), args.get(1)); case "atan2" -> Math.atan2(args.get(0), args.get(1)); default -> throw error();
      };
      double x = args.get(0);
      return switch (name) {
        case "sqrt" -> Math.sqrt(x); case "abs" -> Math.abs(x); case "floor" -> Math.floor(x); case "ceil" -> Math.ceil(x);
        case "round" -> (double)Math.round(x); case "sin" -> Math.sin(x); case "cos" -> Math.cos(x); case "tan" -> Math.tan(x);
        case "asin" -> Math.asin(x); case "acos" -> Math.acos(x); case "atan" -> Math.atan(x);
        case "ln", "log" -> Math.log(x); case "log10" -> Math.log10(x); case "exp" -> Math.exp(x); default -> throw error();
      };
    }
    while (at < input.length() && (Character.isDigit(input.charAt(at)) || input.charAt(at) == '.')) at++;
    if (at < input.length() && (input.charAt(at) == 'e' || input.charAt(at) == 'E')) {
      at++; if (at < input.length() && (input.charAt(at) == '+' || input.charAt(at) == '-')) at++;
      while (at < input.length() && Character.isDigit(input.charAt(at))) at++;
    }
    try { return Double.parseDouble(input.substring(start, at)); } catch (NumberFormatException e) { throw error(); }
  }
}
