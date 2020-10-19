import tester.*;                // The tester library
import javalib.worldimages.*;   // images, like RectangleImage or OverlayImages
import javalib.funworld.*;      // the abstract World class and the big-bang library

import java.awt.Color;          // general colors (as triples of red,green,blue values)
// and predefined colors (Color.RED, Color.GRAY, etc.)
import javalib.worldcanvas.*;  // for manually viewing images (delete if you're not using it)

class MMGAME extends World {

  // configurations
  static boolean DUPLICATES_ALLOWED = true;
  static int CODE_LENGTH = 6;
  static int MAX_GUESSES = 10;
  static ILoColor COLOR_OPTIONS = new ConsLoColor(Color.BLUE,
          new ConsLoColor(Color.GREEN,
                  new ConsLoColor(Color.RED,
                          new ConsLoColor(Color.YELLOW,
                                  new ConsLoColor(Color.PINK,
                                          new ConsLoColor(Color.BLACK, new MtLoColor()))))));

  // game variables
  ILoColor correct;



  public WorldScene makeScene() {
    return null;
  }
}

abstract class AGuess {
  ILoColor sequence;

  AGuess(ILoColor sequence) {
    this.sequence = sequence;
  }
}

class Incomplete extends AGuess {
  Incomplete(ILoColor sequence) {
    super(sequence);
  }
}

class Complete extends AGuess {
  int outOfPlace;
  int correct;

  Complete(ILoColor sequence, int outOfPlace, int correct) {
    super(sequence);
    this.outOfPlace = outOfPlace;
    this.correct = correct;
  }
}

interface ILoAGuess {
}

class MtLoAGuess implements ILoAGuess {
}

class ConsLoAGuess implements ILoAGuess {
  AGuess first;
  ILoAGuess rest;

  ConsLoAGuess(AGuess first, ILoAGuess rest) {
    this.first = first;
    this.rest = rest;
  }
}

interface ILoColor {
}

class MtLoColor implements ILoColor {}

class ConsLoColor implements ILoColor {
  Color first;
  ILoColor rest;

  ConsLoColor(Color first, ILoColor rest) {
    this.first = first;
    this.rest = rest;
  }
}

class Examples {
}