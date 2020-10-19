import tester.*;                // The tester library
import javalib.worldimages.*;   // images, like RectangleImage or OverlayImages
import javalib.funworld.*;      // the abstract World class and the big-bang library
import java.awt.Color;          // general colors (as triples of red,green,blue values)
// and predefined colors (Color.RED, Color.GRAY, etc.)
import javalib.worldcanvas.*;  // for manually viewing images (delete if you're not using it)

import java.util.Random;

class MMGame extends World {

  // configurations
  boolean duplicatesAllowed;
  int sequenceLen;
  int maxGuesses;
  ILoColor possibleColors;

  // game variables
  ILoColor correct;
  ILoColor current;
  ILoGuess past;

  Random rand;

  MMGame(boolean duplicatesAllowed, int sequenceLen, int maxGuesses, ILoColor possibleColors, ILoColor correct, ILoColor current, ILoGuess past, Random rand) {
    this.duplicatesAllowed = validateParams(duplicatesAllowed, sequenceLen, maxGuesses, possibleColors);
    this.sequenceLen = sequenceLen;
    this.maxGuesses = maxGuesses;
    this.possibleColors = possibleColors;

    this.correct = makeSequence(duplicatesAllowed, sequenceLen, possibleColors, rand);
    this.current = current;
    this.past = past;

    this.rand = rand;
  }

  MMGame(boolean duplicatesAllowed, int sequenceLen, int maxGuesses, ILoColor possibleColors) {
    this(duplicatesAllowed,
            sequenceLen,
            maxGuesses,
            possibleColors,
            new MtLoColor(),
            new MtLoColor(),
            new MtLoGuess(),
            new Random());
  }

  // starts the accumulator to make a random sequence
  static ILoColor makeSequence(boolean duplicatesAllowed, int len, ILoColor possibleColors, Random gen) {
    return new MtLoColor().makeSequence(duplicatesAllowed, len, possibleColors, gen);
  }

  // checks if the configs are compatible
  static boolean validateParams(boolean dupes, int seqLen, int maxGuesses, ILoColor posCols) {
    // positive checks
    int posSeq = posCheck(seqLen, "values must be greater than zero");
    int posGuesses = posCheck(maxGuesses, "values must be greater than zero");
    int posColLen = posCheck(posCols.length(), "must be more than one color");

    if (!dupes && posColLen < posSeq) {
      throw new IllegalArgumentException("not enough possible colors");
    }
    return dupes;
  }

  static int posCheck(int tester, String msg) {
    if (tester < 1) {
      throw new IllegalArgumentException(msg);
    }
    return tester;
  }

  public WorldScene makeScene() {
    return null;
  }
}

class Guess {
  ILoColor sequence;
  int outOfPlace;
  int correct;

  Guess(ILoColor sequence, int outOfPlace, int correct) {
    this.sequence = sequence;
    this.outOfPlace = outOfPlace;
    this.correct = correct;
  }
}

interface ILoGuess {
}

class MtLoGuess implements ILoGuess {
}

class ConsLoAGuess implements ILoGuess {
  Guess first;
  ILoGuess rest;

  ConsLoAGuess(Guess first, ILoGuess rest) {
    this.first = first;
    this.rest = rest;
  }
}

interface ILoColor {
  // gets the length of a list
  int length();

  // makes a list with a given length and random colors given a list of possible
  ILoColor makeSequence(boolean duplicatesAllowed, int left, ILoColor possibleColors, Random gen);

  // finds the color at a given index
  Color getIndex(int index);

  // remove the given color
  ILoColor remove(Color toRemove);
}

class MtLoColor implements ILoColor {
  public int length() {
    return 0;
  }

  public ILoColor makeSequence(boolean duplicatesAllowed, int left, ILoColor possibleColors, Random gen) {
    // uses natural number recursion
    if (left == 0) {
      return this;
    } else {
      // gets the new color's index from possible and new possible
      Color newColor = possibleColors.getIndex(gen.nextInt(possibleColors.length()));
      ILoColor newPosColors = duplicatesAllowed ? possibleColors : possibleColors.remove(newColor);

      return new ConsLoColor(newColor, new MtLoColor()).makeSequence(duplicatesAllowed, left - 1, newPosColors, gen);
    }
  }

  public Color getIndex(int index) {
    throw new IllegalArgumentException("given index is not in the list");
  }

  public ILoColor remove(Color toRemove) {
    return this;
  }
}

class ConsLoColor implements ILoColor {
  Color first;
  ILoColor rest;

  ConsLoColor(Color first, ILoColor rest) {
    this.first = first;
    this.rest = rest;
  }

  public int length() {
    return 1 + this.rest.length();
  }

  public ILoColor makeSequence(boolean duplicatesAllowed, int left, ILoColor possibleColors, Random gen) {
    // uses natural number recursion
    if (left == 0) {
      return this;
    } else {
      // gets the new color's index from possible and new possible
      Color newColor = possibleColors.getIndex(gen.nextInt(possibleColors.length()));
      ILoColor newPosColors = duplicatesAllowed ? possibleColors : possibleColors.remove(newColor);

      return new ConsLoColor(newColor, this).makeSequence(duplicatesAllowed, left - 1, newPosColors, gen);
    }
  }

  public Color getIndex(int index) {
    if (index == 0) {
      return this.first;
    } else {
      return this.rest.getIndex(index - 1);
    }
  }

  public ILoColor remove(Color toRemove) {
    if (this.first.equals(toRemove)) {
      return this.rest;
    } else {
      return new ConsLoColor(this.first, this.rest.remove(toRemove));
    }
  }
}

class Examples {
  ILoColor sixColors = new ConsLoColor(Color.BLUE,
          new ConsLoColor(Color.GREEN,
                  new ConsLoColor(Color.RED,
                          new ConsLoColor(Color.YELLOW,
                                  new ConsLoColor(Color.PINK,
                                          new ConsLoColor(Color.BLACK, new MtLoColor()))))));

  boolean testConstructor(Tester tester) {
    return tester.checkConstructorException(new IllegalArgumentException("values must be greater than zero"), "MMGame", true, -1, -1, sixColors)
            && tester.checkConstructorException(new IllegalArgumentException("values must be greater than zero"), "MMGame", true, 10, -5, sixColors)
            && tester.checkConstructorException(new IllegalArgumentException("must be more than one color"), "MMGame", true, 10, 5, new MtLoColor())
            && tester.checkConstructorException(new IllegalArgumentException("not enough possible colors"), "MMGame", false, 10, 8, sixColors);
  }
}