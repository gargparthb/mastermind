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

  static int BOTTOM_Y = 15;

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
    return this.drawBoard().placeImageXY(new RectangleImage(
            (sequenceLen * CIRC_SPACING),
            SIZE / 16,
            OutlineMode.SOLID,
            Color.BLACK), (SIZE - CIRC_SPACING) / 2, scaleY(BOTTOM_Y - (this.maxGuesses)));
  }

  // draws everything without the correct code
  public WorldScene drawBoard() {
    WorldScene bg = this.getEmptyScene();

    int leftX = (SIZE / 2) - (this.sequenceLen / 2) * CIRC_SPACING;
    int rightX = leftX + (this.sequenceLen * CIRC_SPACING);

    int guessedLen = past.length();
    int blankCount = this.maxGuesses - (guessedLen + 1);

    int unguessedY = scaleY(BOTTOM_Y - (1 + this.past.length()));

    // drawing individual components
    WorldScene bgWithOptions = this.possibleColors.draw(bg, leftX, scaleY(BOTTOM_Y));
    WorldScene withGuesses = this.past.drawGuesses(bgWithOptions, leftX, rightX, scaleY(BOTTOM_Y - 1));
    WorldScene withCurrent = this.drawCurrent(withGuesses, leftX, rightX - CIRC_SPACING);
    return this.drawUnguessed(withCurrent, blankCount, rightX - CIRC_SPACING, unguessedY);
  }

  // scales the Y value to the canvas size
  static int scaleY(int y) {
    return (SIZE * y) / 16;
  }

  // draw the current guess row
  public WorldScene drawCurrent(WorldScene bg, int leftX, int rightX) {
    int rowY = scaleY(BOTTOM_Y - (1 + this.past.length()));

    int blanks = this.sequenceLen - this.current.length();
    return this.current.draw(this.drawBlanks(bg, blanks, rightX, rowY), leftX, rowY);
  }

  // draws the unguessed rows
  public WorldScene drawUnguessed(WorldScene bg, int blankCount, int rightX, int y) {
    if (blankCount == 0) {
      return bg;
    } else {
      WorldScene updatedBg = this.drawBlanks(bg, this.sequenceLen, rightX, y);
      return this.drawUnguessed(updatedBg, blankCount - 1, rightX, y - scaleY(1));
    }
  }

  // draws the blanks from the right given the count
  public WorldScene drawBlanks(WorldScene bg, int blanks, int x, int y) {
    if (blanks == 0) {
      return bg;
    } else {
      WorldScene updatedBg = bg.placeImageXY(new CircleImage(CIRC_SIZE, OutlineMode.OUTLINE, Color.BLACK), x, y);
      return this.drawBlanks(updatedBg, blanks - 1, x - CIRC_SPACING, y);
    }
  }

  // processes the key input
  public World onKeyEvent(String key) {
    boolean isFull = this.current.length() == sequenceLen;

    if ("1234567890".contains(key) && Integer.parseInt(key) <= this.possibleColors.length() && !isFull) {
      return this.appendToCurrent(this.possibleColors.getIndex(Integer.parseInt(key) - 1));
    } else if (key.equals("backspace")) {
      return this.removeLastGuess();
    } else if (key.equals("enter") && isFull) {
      return this.processGuess();
    } else {
      return this;
    }
  }

  // adds the given color to the current guess
  public MMGame appendToCurrent(Color c) {
    ILoColor updatedCurrent = this.current.append(c);
    return this.replaceCurrentandPast(updatedCurrent, this.past);
  }

  // chops off the last color in the current list
  public MMGame removeLastGuess() {
    return this.replaceCurrentandPast(this.current.chop(), this.past);
  }

  // changes the current guess and the past guesses
  public MMGame replaceCurrentandPast(ILoColor newCurrent, ILoGuess newPast) {
    return new MMGame(this.duplicatesAllowed, this.sequenceLen, this.maxGuesses, this.possibleColors, this.correct, newCurrent, newPast, this.rand);
  }

  // turns the current input into a past guess with feedback
  public MMGame processGuess() {
    return this.replaceCurrentandPast(new MtLoColor(), this.past.appendWithFeedback(this.correct, this.current));
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

  // length
  int length();

  // adds the list of colors to the list with the feedback numbers
  ILoGuess appendWithFeedback(ILoColor correct, ILoColor guess);
}

class MtLoGuess implements ILoGuess {
  public WorldScene drawGuesses(WorldScene bg, int leftX, int rightX, int y) {
    return bg;
  }

  public int length() {
    return 0;
  }

  public ILoGuess appendWithFeedback(ILoColor correct, ILoColor guess) {
    return new ConsLoGuess(guess.makeGuess(correct), new MtLoGuess());
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

  public int length() {
    return 1 + this.rest.length();
  }

  public ILoGuess appendWithFeedback(ILoColor correct, ILoColor guess) {
    if (this.rest.length() == 0) {
      return new ConsLoGuess(this.first, new ConsLoGuess(guess.makeGuess(correct), new MtLoGuess()));
    } else {
      return new ConsLoGuess(this.first, this.rest.appendWithFeedback(correct, guess));
    }
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

  // appends a color to the end
  ILoColor append(Color c);

  // removes last item
  ILoColor chop();

  // calculates the feedback and makes the guess given the correct sequence
  Guess makeGuess(ILoColor correct);
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

  public WorldScene draw(WorldScene bg, int x, int y) {
    return bg;
  }

  public ILoColor append(Color c) {
    return new ConsLoColor(c, new MtLoColor());
  }

  public ILoColor chop() {
    return this;
  }

  public Guess makeGuess(ILoColor correct) {
    // vacuous case
    return new Guess(new MtLoColor(), 0, 0);
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

  public ILoColor append(Color c) {
    if (this.rest.length() == 0) {
      return new ConsLoColor(this.first, new ConsLoColor(c, new MtLoColor()));
    } else {
      return new ConsLoColor(this.first, this.rest.append(c));
    }
  }

  public ILoColor chop() {
    if (this.length() == 1) {
      return new MtLoColor();
    } else {
      return new ConsLoColor(this.first, this.rest.chop());
    }
  }

  public Guess makeGuess(ILoColor) {

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
  MMGame testerGame1 = new MMGame(true, 4, 13, sixColors,
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