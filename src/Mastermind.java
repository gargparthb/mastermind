import tester.*;                // The tester library
import javalib.worldimages.*;   // images, like RectangleImage or OverlayImages
import javalib.funworld.*;      // the abstract World class and the big-bang library

import java.awt.Color;          // general colors (as triples of red,green,blue values)
// and predefined colors (Color.RED, Color.GRAY, etc.)
import javalib.worldcanvas.*;  // for manually viewing images (delete if you're not using it)

import java.util.Random;

class MMGame extends World {

  // drawing variables
  static int SIZE = 800;
  static int CIRC_SIZE = 20;
  static int CIRC_SPACING = 50;

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
    // allows for testing of random
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
    // starts accumulator
    return new MtLoColor().makeSequence(duplicatesAllowed, len, possibleColors, gen);
  }

  // checks if the configs are compatible
  static boolean validateParams(boolean dupes, int seqLen, int maxGuesses, ILoColor posCols) {
    // positive checks
    int posSeq = posCheck(seqLen, "values must be greater than zero");
    int posGuesses = posCheck(maxGuesses, "values must be greater than zero");
    int posColLen = posCheck(posCols.length(), "must be more than one color");

    // impossible input check
    if (!dupes && posColLen < posSeq) {
      throw new IllegalArgumentException("not enough possible colors");
    }
    return dupes;
  }

  // if the number is positive return otherwise return error
  static int posCheck(int tester, String msg) {
    if (tester < 1) {
      throw new IllegalArgumentException(msg);
    }
    return tester;
  }

  // to-draw
  public WorldScene makeScene() {
    WorldScene bg = this.getEmptyScene();

    int leftX = (int) (SIZE/2) - (this.possibleColors.length() / 2) * CIRC_SPACING;
    int rightX = leftX + (this.sequenceLen * CIRC_SPACING);

    // drawing individual components
    WorldScene bgWithOptions = this.possibleColors.draw(bg, leftX, SIZE * 15 / 16);
    WorldScene withGuesses = this.past.drawGuesses(bgWithOptions, leftX, rightX, SIZE * 14 / 16);

    return withGuesses;
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

  // draws the colors and feedback
  public WorldScene drawGuess(WorldScene bg, int leftX, int rightX, int y) {
    WorldImage outOfPlaceText = new TextImage(Integer.toString(this.outOfPlace), MMGame.CIRC_SIZE, Color.ORANGE);
    WorldImage correctText = new TextImage(Integer.toString(this.correct), MMGame.CIRC_SIZE, Color.GREEN);

    return this.sequence.draw(bg, leftX, y)
            .placeImageXY(outOfPlaceText, rightX, y)
            .placeImageXY(correctText, rightX + MMGame.CIRC_SPACING, y);
  }
}

interface ILoGuess {
  // draw the past guesses with the feedback
  WorldScene drawGuesses(WorldScene bg, int leftX, int rightX, int y);
}

class MtLoGuess implements ILoGuess {
  public WorldScene drawGuesses(WorldScene bg, int leftX, int rightX, int y) {
    return bg;
  }
}

class ConsLoGuess implements ILoGuess {
  Guess first;
  ILoGuess rest;

  ConsLoGuess(Guess first, ILoGuess rest) {
    this.first = first;
    this.rest = rest;
  }

  public WorldScene drawGuesses(WorldScene bg, int leftX, int rightX, int y) {
    return this.rest.drawGuesses(this.first.drawGuess(bg, leftX, rightX, y), leftX, rightX, y - (MMGame.SIZE / 16));
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

  // draws with the current x
  WorldScene draw(WorldScene bg, int x, int y);
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

  public WorldScene draw(WorldScene bg, int y) {
    return bg;
  }

  public WorldScene draw(WorldScene bg, int x, int y) {
    return bg;
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
    // simple recursion
    return 1 + this.rest.length();
  }

  public ILoColor makeSequence(boolean duplicatesAllowed, int left, ILoColor possibleColors, Random gen) {
    // is there a way to abstract this
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

  public WorldScene draw(WorldScene bg, int x, int y) {
    return this.rest
            .draw(bg.placeImageXY(new CircleImage(MMGame.CIRC_SIZE, OutlineMode.SOLID, this.first), x, y),
                    x + MMGame.CIRC_SPACING, y);
  }
}

class Examples {
  ILoColor sixColors = new ConsLoColor(Color.BLUE,
          new ConsLoColor(Color.GREEN,
                  new ConsLoColor(Color.RED,
                          new ConsLoColor(Color.YELLOW,
                                  new ConsLoColor(Color.PINK,
                                          new ConsLoColor(Color.BLACK, new MtLoColor()))))));

  // yellow, green, pink, yellow
  ILoColor randomSeq = MMGame.makeSequence(true, 4, sixColors, new Random(1));

  // some tester [list-of Color]
  ILoColor justRed = new ConsLoColor(Color.RED, new MtLoColor());
  ILoColor GBPR = new ConsLoColor(Color.GREEN,
          new ConsLoColor(Color.BLACK,
                  new ConsLoColor(Color.PINK,
                          justRed)));
  ILoColor BGRY = new ConsLoColor(Color.BLUE,
          new ConsLoColor(Color.GREEN,
                  new ConsLoColor(Color.RED,
                          new ConsLoColor(Color.YELLOW, new MtLoColor()))));

  // guesses
  Guess guessOfGBPR = new Guess(GBPR, 1, 1);
  Guess guessOfBRGY = new Guess(BGRY, 0, 2);

  // games
  MMGame testerGame = new MMGame(true, 4, 10, sixColors,
          randomSeq, justRed, new MtLoGuess(), new Random(1));
  MMGame testerGame1 = new MMGame(true, 4, 10, sixColors,
          randomSeq, justRed, new ConsLoGuess(guessOfBRGY, new ConsLoGuess(guessOfGBPR, new MtLoGuess())), new Random(1));

  boolean testConstructor(Tester tester) {
    return tester.checkConstructorException(new IllegalArgumentException("values must be greater than zero"), "MMGame", true, -1, -1, sixColors)
            && tester.checkConstructorException(new IllegalArgumentException("values must be greater than zero"), "MMGame", true, 10, -5, sixColors)
            && tester.checkConstructorException(new IllegalArgumentException("must be more than one color"), "MMGame", true, 10, 5, new MtLoColor())
            && tester.checkConstructorException(new IllegalArgumentException("not enough possible colors"), "MMGame", false, 10, 8, sixColors);
  }

  boolean testIndex(Tester tester) {
    return tester.checkExpect(justRed.getIndex(0), Color.RED)
            && tester.checkExpect(GBPR.getIndex(3), Color.RED)
            && tester.checkException(new IllegalArgumentException("given index is not in the list"), GBPR, "getIndex", 10);
  }

  boolean testRemove(Tester tester) {
    return tester.checkExpect(GBPR.remove(Color.GREEN), new ConsLoColor(Color.BLACK,
            new ConsLoColor(Color.PINK,
                    justRed)))
            && tester.checkExpect(GBPR.remove(Color.YELLOW), GBPR);
  }

  boolean testRun(Tester tester) {
    return testerGame1.bigBang(MMGame.SIZE, MMGame.SIZE);
  }
}