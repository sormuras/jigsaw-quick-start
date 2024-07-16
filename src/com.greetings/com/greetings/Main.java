package com.greetings;

import org.astro.World;

public class Main {
  public static void main(String[] args) {
    System.out.printf("Greetings from %s in %s!%n", Main.class, Main.class.getModule());
    System.out.printf("Greetings to %s in %s!%n", World.class, World.class.getModule());
  }
}
