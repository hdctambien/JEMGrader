import java.io.*;
import java.nio.file.*;
import java.util.*;

import jdk.dynalink.linker.support.SimpleLinkRequest;

public class UILGrader extends JEMGrader
{
  private static final int IGNORE = -1;

  public static final String PASS = "P";
  public static final String FAIL = "F";
  public static final String COMPILE_ERROR = "C";
  public static final String ERROR = "E";
  public static final String TIMEOUT = "T";

  private File answer;
  protected String testResult;
  private int similarityThreshold = IGNORE;
  private int maxTypos = IGNORE;

  public boolean usingLevenshtein()
  {
    return maxTypos != IGNORE || similarityThreshold != IGNORE;
  }

  public void setSimilarityThreshold(int threshold)
  {
    similarityThreshold = Math.max(0, Math.min(100, threshold));
    maxTypos = IGNORE;
  }

  public void setMaximumTypos(int num)
  {
    maxTypos = Math.max(0, num);
    similarityThreshold = IGNORE;
  }

  public void setPathToAnswer(String path)
  {
    answer = new File(path);
  }

  public void printResultHeader()
  {
    System.out.println("Student, Test Result");
  }

  public void printResult(File dir)
  {
    System.out.printf("%s, %s%n", dir.getName().replaceAll("_" , " "), testResult);
  }

  public void setup()
  {
    printResultHeader();
  }

  public void cleanup()
  {

  }

  public void beforeCompile(JavaRunner jr, File dir)
  {
    // noop
  }

  public void beforeExecute(JavaRunner jr, File dir)
  {
    // noop
  }

  public void afterExecute(JavaRunner jr, File dir)
  {
    String result = "F";

    File output = jr.getOutputLog();
    File err = jr.getErrorLog();

    if (err.exists()) {
      try
      {
        List<String> errLines = Files.readAllLines(Paths.get(err.getPath()));
        if (errLines.size() > 0) {
          for(String line : errLines)
          {
            if (!line.trim().equals("")) {
              result = ERROR;
              break;
            }//end if line is not empty
          }//emd for
        }//end if length>0
      }
      catch(IOException e)
      {
        e.printStackTrace();
      }
    }//end if err exists

    int distance = -1;
    if(!result.equals("E") && output.exists())
    {
      // Read test results from output file
      try
      {        
        List<String> lines = Files.readAllLines(Paths.get(output.getPath()));
        List<String> answerLines = Files.readAllLines(Paths.get(answer.getPath()));

        //check for empty files
        if(lines.size() > 0 && answerLines.size() > 0)
        {
          // remove empty last lines
          if("".equals(lines.get(lines.size()-1).trim()))
          {
            lines.remove(lines.size()-1);
          }

          if("".equals(answerLines.get(answerLines.size()-1).trim()))
          {
            answerLines.remove(answerLines.size()-1);
          }

          // check that they have the same number of lines
          if(lines.size() == answerLines.size())
          {
            result = PASS;
            distance = 100;
            for(int i=0; i<lines.size(); i++)
            {
              if(!lines.get(i).equals(answerLines.get(i)))
              {
                result = FAIL;
                break;
              }//end if lines are not equal
            }//end for each line
          }//end if same number of lines
        }//end if there are any lines

        // If result did not pass because it is 100% correct,
        // check if it meets the similarity threshold or max typos
        if (!result.equals(PASS) && usingLevenshtein()) {
          String fullOutput = String.join("\n", lines);
          String fullAnswer = String.join("\n", answerLines);

          distance = calculateLevenshteinDistance(fullOutput, fullAnswer);

          // Check if this soltion is below the maxTypos allowed or similarity threshold
          if (maxTypos != IGNORE) {
            // Check if this has less than the max allowed typos
            if (distance <= maxTypos) {
              result = PASS;
            }
          }//end if check for typos
          else if (similarityThreshold != IGNORE) {
            // check if this meets the similarity threshold
            distance = 100 - (int)Math.round(1.0 * distance / fullAnswer.length() * 100);
            if (distance >= similarityThreshold) {
              result = PASS;
            }//end if distance greater than similarity threshold
          }//end if check for similarity
        }//end if result is not already passing and using levenshtein
      }//end try
      catch(Exception e)
      {
        e.printStackTrace();
      }//end catch
    }//end if output exists

    // update output csv file
    // only include the (levenshtein) distance if it was used to calculate a passing grade
    if (usingLevenshtein()) 
    {
      this.testResult = result + ", " + distance;
    }
    else
    {
      this.testResult = result;
    }
  }

  public void afterCompileError(JavaRunner jr, File dir)
  {
    this.testResult = COMPILE_ERROR;
    if (usingLevenshtein()) 
    {
      this.testResult += ", " + IGNORE;
    }
  }

  public void afterTimeoutError(JavaRunner jr, File dir)
  {
    this.testResult = TIMEOUT;
    if (usingLevenshtein()) 
    {
      this.testResult += ", " + IGNORE;
    }
  }

  public void afterEverything(File dir)
  {
    printResult(dir);
  }

  // https://www.baeldung.com/java-levenshtein-distance
  public static int calculateLevenshteinDistance(String x, String y) {
    int[][] dp = new int[x.length() + 1][y.length() + 1];

    for (int i = 0; i <= x.length(); i++) {
        for (int j = 0; j <= y.length(); j++) {
            if (i == 0) {
                dp[i][j] = j;
            }
            else if (j == 0) {
                dp[i][j] = i;
            }
            else {
                dp[i][j] = min(dp[i - 1][j - 1] 
                 + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)), 
                  dp[i - 1][j] + 1, 
                  dp[i][j - 1] + 1);
            }
        }
    }

    return dp[x.length()][y.length()];
  }

  protected static int costOfSubstitution(char a, char b) {
    return a == b ? 0 : 1;
  }

  private static int min(int... numbers) {
    return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
  }

  public static void main(String[] args)
  {
    UILGrader grader = new UILGrader();

    String format = "Syntax: java UILGrader path/to/student/files path/to/answer/file.out test-filename [timeout] [x% Similarity Threshold|x allowed typos]";

    if(args.length >= 3)
    {
      grader.setPathToStudentFiles(args[0]);
      grader.setPathToAnswer(args[1]);
      grader.setFileToCompile(args[2]);
    }
    else
    {
      System.out.println(format);
      return;
    }

    if(args.length >= 4) 
    {
      try 
      {
        grader.setTimeout(Integer.parseInt(args[3]));
      }
      catch (NumberFormatException e) 
      {
        System.out.println(format);
        System.out.println("Timeout must be an int");
        return;
      }
    }
    else 
    {
      grader.setTimeout(5000);
    }

    if(args.length >= 5) 
    {
      try 
      {
        String arg = args[4];
        if (arg.endsWith("%")) 
        {
          arg = arg.substring(0, arg.length()-1);
          grader.setSimilarityThreshold(Integer.parseInt(arg)); 
        }
        else 
        {
          grader.setMaximumTypos(Integer.parseInt(arg));
        }
      }
      catch (NumberFormatException e) 
      {
        return;
      }
    }

    grader.go();
  }
}
