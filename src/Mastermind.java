import tester.*;                // The tester library
import javalib.worldimages.*;   // images, like RectangleImage or OverlayImages
import javalib.funworld.*;      // the abstract World class and the big-bang library

import java.awt.Color;          // general colors (as triples of red,green,blue values)
// and predefined colors (Color.RED, Color.GRAY, etc.)

import java.util.Random;

class MMGame extends World {

  // drawing variables
  static int CIRC_SIZE = 20;
  static int CIRC_SPACING = 50;

  // configurations
  boolean duplicatesAllowed;
  int sequenceLen;
  int maxGuesses;
  ILoColor possibleColors;

  // game variables
  ILoColor correct, current;
  ILoGuess past;

  Random rand;

  // for updating the game state
  MMGame(boolean duplicatesAllowed, int sequenceLen, int maxGuesses, ILoColor possibleColors, ILoColor correct, ILoColor current, ILoGuess past, Random rand) {
    this.duplicatesAllowed = duplicatesAllowed;
    this.sequenceLen = sequenceLen;
    this.maxGuesses = maxGuesses;
    this.possibleColors = possibleColors;

    // must be a way to assign a correct sequence
    this.correct = correct;
    this.current = current;
    this.past = past;

    this.rand = rand;
  }

  // tester constructor
  MMGame(boolean duplicatesAllowed, int sequenceLen, int maxGuesses, ILoColor possibleColors, ILoColor current, ILoGuess past, Random rand) {
    this(validateParams(duplicatesAllowed, sequenceLen, maxGuesses, possibleColors),
            sequenceLen,
            maxGuesses,
            possibleColors,
            makeSequence(duplicatesAllowed, sequenceLen, possibleColors, rand),
            current,
            past,
            rand);
  }

  // real game constructor
  MMGame(boolean duplicatesAllowed, int sequenceLen, int maxGuesses, ILoColor possibleColors) {
    this(duplicatesAllowed,
            sequenceLen,
            maxGuesses,
            possibleColors,
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
    return this.drawBoard()
            // places the hidden black rectangle
            .placeImageXY(new RectangleImage(
                    scale(this.sequenceLen),
                    scale(1),
                    OutlineMode.SOLID,
                    // cannot scale the x value using scale, because of number typing
                    Color.BLACK), ((1 + this.sequenceLen) * CIRC_SPACING / 2), scale(1));
  }

  // draws everything without the correct code
  public WorldScene drawBoard() {
    WorldScene bg = this.getEmptyScene();

    // starting y coord
    int bottomY = this.maxGuesses + 2;

    // avoids duplicate comp
    int guessedLen = this.past.length();

    // amount of blank rows
    int blankCount = guessedLen == this.maxGuesses ? 0 : this.maxGuesses - (guessedLen + 1);

    // y of blank rows
    int unguessedY = scale(this.maxGuesses - guessedLen);

    // drawing individual components
    WorldScene bgWithOptions = this.possibleColors.draw(bg, scale(1), scale(bottomY));
    WorldScene withGuesses = this.past.drawGuesses(bgWithOptions, scale(1), scale(this.sequenceLen + 1), scale(bottomY - 1));

    // don't alwas have to draw the current input, in case of loss
    if (this.maxGuesses == guessedLen) {
      return this.drawUnguessed(withGuesses, blankCount, scale(this.sequenceLen), unguessedY);
    } else {
      return this.drawUnguessed(this.drawCurrent(withGuesses, scale(1), scale(this.sequenceLen)),
              blankCount, scale(this.sequenceLen), unguessedY);
    }
  }

  // scales the Y value to the canvas size
  static int scale(int n) {
    return n * CIRC_SPACING;
  }

  // draw the current guess row
  public WorldScene drawCurrent(WorldScene bg, int leftX, int rightX) {
    // computes the y value
    int rowY = scale(this.maxGuesses - this.past.length() + 1);

    // recurs the blank circles from the right
    int blanks = this.sequenceLen - this.current.length();
    return this.current.draw(this.drawBlanks(bg, blanks, rightX, rowY), leftX, rowY);
  }


  // draws the unguessed rows
  public WorldScene drawUnguessed(WorldScene bg, int blankCount, int rightX, int y) {
    if (blankCount <= 0) {
      return bg;
    } else {
      WorldScene updatedBg = this.drawBlanks(bg, this.sequenceLen, rightX, y);
      // moves up the y val
      return this.drawUnguessed(updatedBg, blankCount - 1, rightX, y - scale(1));
    }
  }

  // draws the blanks from the right given the count
  public WorldScene drawBlanks(WorldScene bg, int blanks, int x, int y) {
    if (blanks <= 0) {
      return bg;
    } else {
      WorldScene updatedBg = bg.placeImageXY(new CircleImage(CIRC_SIZE, OutlineMode.OUTLINE, Color.BLACK), x, y);
      // moves across
      return this.drawBlanks(updatedBg, blanks - 1, x - CIRC_SPACING, y);
    }
  }

  // processes the key input
  public World onKeyEvent(String key) {
    boolean isFull = this.current.length() == sequenceLen;

    if ("123456789".contains(key) && Integer.parseInt(key) <= this.possibleColors.length() && !isFull) {
      return this.appendToCurrent(this.possibleColors.getIndex(Integer.parseInt(key) - 1));
    } else if (key.equals("backspace")) {
      return this.removeLastGuess();
    } else if (key.equals("enter") && isFull) {
      // checks if the game is over
      if (this.current.findExact(correct) == this.sequenceLen) {
        return this.endOfWorld("Victory!");
      } else if (this.past.length() + 1 == this.maxGuesses) {
        return this.endOfWorld("Lose!");
      } else {
        return this.processGuess();
      }
    } else {
      return this;
    }
  }

  // draws the final scene of the game based on win or loss
  public WorldScene lastScene(String msg) {
    Color txtColor = msg.equals("Lose!") ? Color.RED : Color.GREEN;
    // have to advance game state for drawing
    MMGame updatedGame = this.processGuess();

    int offset = 3 * CIRC_SPACING / 2;
    int textX = scale(updatedGame.sequenceLen) + offset;

    return updatedGame.correct.draw(updatedGame.drawBoard(), scale(1), scale(1))
            .placeImageXY(new TextImage(msg, CIRC_SIZE, txtColor), textX, scale(1));
  }

  // adds the given color to the current guess
  public MMGame appendToCurrent(Color c) {
    return this.replaceCurrentAndPlace(this.current.append(c), this.past);
  }

  // chops off the last color in the current list
  public MMGame removeLastGuess() {
    return this.replaceCurrentAndPlace(this.current.chop(), this.past);
  }

  // changes the current guess and the past guesses
  public MMGame replaceCurrentAndPlace(ILoColor newCurrent, ILoGuess newPast) {
    // must use the manuel assignment constructor!
    return new MMGame(this.duplicatesAllowed, this.sequenceLen, this.maxGuesses, this.possibleColors, this.correct, newCurrent, newPast, this.rand);
  }

  // turns the current input into a past guess with feedback
  public MMGame processGuess() {
    int exactMatches = this.current.findExact(correct);

    return this.replaceCurrentAndPlace(new MtLoColor(), this.past.append(new Guess(this.current,
            this.current.findInexact(correct) - exactMatches,
            exactMatches)));
  }

  // computes the width of the window
  public int width() {
    return scale(Math.max(this.possibleColors.length() + 1, this.sequenceLen + 3));
  }

  // computes the height of the window
  public int height() {
    return scale(3 + this.maxGuesses);
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
  ILoGuess append(Guess g);
}

class MtLoGuess implements ILoGuess {
  public WorldScene drawGuesses(WorldScene bg, int leftX, int rightX, int y) {
    return bg;
  }

  public int length() {
    return 0;
  }

  public ILoGuess append(Guess guess) {
    return new ConsLoGuess(guess, new MtLoGuess());
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
    return this.rest.drawGuesses(this.first.drawGuess(bg, leftX, rightX, y), leftX, rightX, y - MMGame.scale(1));
  }

  public int length() {
    return 1 + this.rest.length();
  }

  public ILoGuess append(Guess guess) {
    if (this.rest.length() == 0) {
      return new ConsLoGuess(this.first, new ConsLoGuess(guess, new MtLoGuess()));
    } else {
      return new ConsLoGuess(this.first, this.rest.append(guess));
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

  // gets the exact matches in linked list
  int findExact(ILoColor correct);

  // remove the exact matches in two lists using the pointer index
  ILoColor removeExact(ILoColor comp, int currentIndex);

  // count the inexact matches
  int findInexact(ILoColor correct);

  // is the given color inside this
  boolean isMember(Color c);
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
    return new ConsLoColor(c, this);
  }

  public ILoColor chop() {
    return this;
  }

  public int findExact(ILoColor correct) {
    return 0;
  }


  public ILoColor removeExact(ILoColor comp, int currentIndex) {
    return this;
  }

  public int findInexact(ILoColor correct) {
    return 0;
  }

  public boolean isMember(Color c) {
    return false;
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
    return new ConsLoColor(this.first, this.rest.append(c));
  }

  public ILoColor chop() {
    if (this.length() > 1) {
      return new ConsLoColor(this.first, this.rest.chop());
    } else {
      return this.rest;
    }
  }

  public int findExact(ILoColor correct) {
    return this.length() - this.removeExact(correct, 0).length();
  }

  public ILoColor removeExact(ILoColor comp, int currentIndex) {
    ILoColor next = this.rest.removeExact(comp, currentIndex + 1);

    if (this.first.equals(comp.getIndex(currentIndex))) {
      return next;
    } else {
      return new ConsLoColor(this.first, next);
    }
  }

  public int findInexact(ILoColor correct) {
    if (correct.isMember(this.first)) {
      return 1 + this.rest.findInexact(correct.remove(this.first));
    } else {
      return this.rest.findInexact(correct);
    }
  }

  public boolean isMember(Color c) {
    if (c.equals(this.first)) {
      return true;
    } else {
      return this.rest.isMember(c);
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
  MMGame testerGame2 = new MMGame(true, 4, 4, sixColors, randomSeq, BGRY,
          new ConsLoGuess(guessOfGBPR, new ConsLoGuess(guessOfGBPR, new ConsLoGuess(guessOfGBPR, new MtLoGuess()))), new Random(1));

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

  boolean testKeyInput(Tester tester) {
    return tester.checkExpect(testerGame1.onKeyEvent("1"), new MMGame(true, 4, 10, sixColors,
            randomSeq, new ConsLoColor(Color.RED, new ConsLoColor(Color.BLUE, new MtLoColor())), new ConsLoGuess(guessOfBRGY, new ConsLoGuess(guessOfGBPR, new MtLoGuess())), new Random(1)))
            && tester.checkExpect(testerGame1.onKeyEvent("backspace"), new MMGame(true, 4, 10, sixColors,
            randomSeq, new MtLoColor(), new ConsLoGuess(guessOfBRGY, new ConsLoGuess(guessOfGBPR, new MtLoGuess())), new Random(1)));
  }

  boolean testLen(Tester tester) {
    return tester.checkExpect(BGRY.length(), 4)
            && tester.checkExpect(new MtLoColor().length(), 0)
            && tester.checkExpect(sixColors.length(), 6);
  }

  boolean testAppend(Tester tester) {
    return tester.checkExpect(BGRY.append(Color.PINK),
            new ConsLoColor(Color.BLUE,
                    new ConsLoColor(Color.GREEN,
                            new ConsLoColor(Color.RED,
                                    new ConsLoColor(Color.YELLOW,
                                            new ConsLoColor(Color.PINK,
                                                    new MtLoColor()))))))
            && tester.checkExpect(new MtLoColor().append(Color.PINK), new ConsLoColor(Color.PINK, new MtLoColor()));
  }

  boolean testChop(Tester tester) {
    return tester.checkExpect(new MtLoColor().chop(), new MtLoColor())
            && tester.checkExpect(BGRY.chop(),
            new ConsLoColor(Color.BLUE,
                    new ConsLoColor(Color.GREEN,
                            new ConsLoColor(Color.RED, new MtLoColor()))));
  }

  boolean testFeedback(Tester tester) {
    return tester.checkExpect(GBPR.findExact(GBPR), 4)
            && tester.checkExpect(randomSeq.findExact(randomSeq), 4)
            && tester.checkExpect(GBPR.findExact(BGRY), 0)
            && tester.checkExpect(GBPR.findInexact(BGRY), 2);

  }

  boolean testRemove(Tester tester) {
    return tester.checkExpect(GBPR.remove(Color.GREEN), new ConsLoColor(Color.BLACK,
            new ConsLoColor(Color.PINK,
                    justRed)))
            && tester.checkExpect(GBPR.remove(Color.YELLOW), GBPR);
  }

  boolean testEndGame(Tester tester) {
    return tester.checkExpect(testerGame2.processGuess(),
            new MMGame(true, 4, 4, sixColors, randomSeq, new MtLoColor(),
                    new ConsLoGuess(guessOfGBPR,
                            new ConsLoGuess(guessOfGBPR,
                                    new ConsLoGuess(guessOfGBPR,
                                            new ConsLoGuess(new Guess(BGRY, 0, 2), new MtLoGuess())))), new Random(1)).endOfWorld("Lose!"));
  }

  boolean testRun(Tester tester) {
    MMGame game = new MMGame(true, 5, 10, sixColors);
    int w = game.width();
    int h = game.height();
    return game.bigBang(w, h);
  }
}