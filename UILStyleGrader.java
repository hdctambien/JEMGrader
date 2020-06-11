import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UILStyleGrader extends UILGrader
{
  private String styleResult;
  private File styleLog;

  public void printResultHeader()
  {
    System.out.println("Student, Style, Test Result");
  }

  public void printResult(File dir)
  {
    System.out.printf("%s, %s, %s%n", dir.getName().replaceAll("_" , " "), styleResult, testResult);
  }

  public void beforeCompile(JavaRunner jr, File dir)
  {
    super.beforeCompile(jr, dir);

    //Run Style checker & set styleResult instance variable
    styleResult = "?";
    styleLog = new File(jr.pathTo("style.log"));
    try
    {
      Files.deleteIfExists(styleLog.toPath());

      // Run checkstyle
      String fileToCheck = jr.getFileToCompile().toString();
      String cmd = "java -jar checkstyle-8.33-all.jar -c style_checks.xml " +fileToCheck + ".java";

      Runtime run = Runtime.getRuntime();
      Process proc = run.exec(cmd);
      JavaRunner.writeLines(styleLog.getPath(), proc.getInputStream());

      proc.waitFor();

      //Read test results and set styleResult value
      if(styleLog.exists())
      {
        String results = "";
        try
        {
          List<String> lines = Files.readAllLines(styleLog.toPath());
          for(String line : lines)
          {
            results += line;
          }
          results = results.trim();
        }
        catch(Exception e)
        {
          e.printStackTrace();
        }

        if(results.equals("Starting audit...Audit done."))
        {
          styleResult = "P";
        }
        else
        {
          styleResult = "F";
        }
      }

      // Copy style.log to student folder
      if(styleLog.exists())
      {
        File styleLogDest = new File(dir.toString() + File.separator + "style.log");
        Files.deleteIfExists(styleLogDest.toPath());

        Files.copy(styleLog.toPath(), styleLogDest.toPath());
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  public static void main(String[] args)
  {
    UILStyleGrader grader = new UILStyleGrader();

    if(args.length >= 3)
    {
      grader.setPathToStudentFiles(args[0]);
      grader.setPathToAnswer(args[1]);
      grader.setFileToCompile(args[2]);
    }
    else
    {
      System.out.println("Syntax: java UILStyleGrader path-to-student-files path-to-answer test-file-name [timeout]");
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
