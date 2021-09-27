import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.*;

public class JUnitGrader extends JEMGrader implements Callable<Integer>
{
  protected String passed;
  protected int failed;
  protected int percent;

  @Override
  @Command(name = "JUnitGrader", mixinStandardHelpOptions = true, version = "JUnitGrader 1.0", description = "Grades assignments using JUnit tests.")
  public JavaRunner getJavaRunner(File dir)
  {
    JavaRunner jr = super.getJavaRunner(dir);
    jr.addLocalClassPath("junit-4.13.jar");
    jr.addLocalClassPath("hamcrest-core-1.3.jar");
    return jr;
  }

  public void printResultHeader()
  {
    System.out.println("Student, #Pass, #Fail, %Pass");
  }

  public void printResult(File dir)
  {
    System.out.printf("%s, %s, %d, %d%n", dir.getName().replaceAll("_" , " "), passed, failed, percent);
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
    // delete old test log
    try
    {
      File dest = new File(dir.getPath() + File.separator + "test.log");
      Files.deleteIfExists(dest.toPath());
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }

  }

  public void beforeExecute(JavaRunner jr, File dir)
  {
    // Reset the JavaRunner object so that it runs the JUnit program
    jr.setFilename("org.junit.runner.JUnitCore " + getFileToCompile());
  }

  public void afterExecute(JavaRunner jr, File dir)
  {
    int numSuccess = 0;
    int numFail = 0;
    int passPercent = 0;

    File output = jr.getOutputLog();

    if(output.exists())
    {
      // Read test results from output file
      String results = "";
      try
      {
        List<String> lines = Files.readAllLines(output.toPath());
        if(lines.size() > 1)
        {
          results = lines.get(1);
        }
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }

      //Calculate number passed & failed tests from results
      int total = 0;
      for(int i=0; i<results.length(); i++)
      {
        String letter = results.substring(i, i+1);
        if(".".equals(letter))
        {
          total++;
        }
        else
        {
          numFail++;
        }
      }
      numSuccess = total-numFail;

      passPercent = (int)(numSuccess * 1.0 / total * 100);
    }

    // update output csv file
    // studentDirName, numPass, numFail
    this.passed = ""+numSuccess;
    this.failed = numFail;
    this.percent = passPercent;
  }

  public void afterCompileError(JavaRunner jr, File dir)
  {
    this.passed = "C";
    this.failed = -1;
    this.percent = -1;
  }

  public void afterTimeoutError(JavaRunner jr, File dir)
  {
    this.passed = "T";
    this.failed = -1;
    this.percent = -1;
  }

  public void afterEverything(File dir)
  {
    printResult(dir);
  }

  @Override
  public Integer call() throws Exception {
    return go();
  }

  public static void main(String[] args) {
    // https://picocli.info/
    int exitCode = new CommandLine(new JUnitGrader()).execute(args);
    System.exit(exitCode);
  }
}
