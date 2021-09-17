import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UILGrader extends JEMGrader
{
  private File answer;
  protected String testResult;

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

    if(output.exists())
    {
      // Read test results from output file
      String results = "";
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
          result = "F";
          if(lines.size() == answerLines.size())
          {
            result = "P";
            for(int i=0; i<lines.size(); i++)
            {
              if(!lines.get(i).equals(answerLines.get(i)))
              {
                result = "F";
                break;
              }//end if lines are not equal
            }//end for each line
          }//end if same number of lines
        }//end if there are any lines
      }//end try
      catch(Exception e)
      {
        e.printStackTrace();
      }

    }

    // update output csv file
    this.testResult = result;
  }

  public void afterCompileError(JavaRunner jr, File dir)
  {
    this.testResult = "C";
  }

  public void afterTimeoutError(JavaRunner jr, File dir)
  {
    this.testResult = "T";
  }

  public void afterEverything(File dir)
  {
    printResult(dir);
  }

  public static void main(String[] args)
  {
    UILGrader grader = new UILGrader();

    if(args.length >= 3)
    {
      grader.setPathToStudentFiles(args[0]);
      grader.setPathToAnswer(args[1]);
      grader.setFileToCompile(args[2]);
    }
    else
    {
      System.out.println("Syntax: java UILGrader path-to-student-files path-to-answer test-file-name [timeout]");
      return;
    }

    if(args.length >= 4)
    {
      grader.setTimeout(Integer.parseInt(args[3]));
    }
    else
    {
      grader.setTimeout(5000);
    }

    grader.go();
  }
}
