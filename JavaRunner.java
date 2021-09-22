/* https://www.journaldev.com/937/compile-run-java-program-another-java-program
 *
 *  This program will compile and run a .java file
 */
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class JavaRunner implements Runnable
{
  // **************
  // STATIC METHDOS
  // **************
  public static void writeLines(String filename, InputStream ins) throws Exception
  {
    Path path = Paths.get(filename);
    Files.copy(ins, path);
  }

  public static void writeLines(String filename, String data) throws Exception
  {
    Path path = Paths.get(filename);
    Files.write(path, data.getBytes());
  }

  // ****************
  // CLASS DEFINITION
  // ****************
  private String path;
  private String filename;
  private Process proc;
  private Timer timer;
  private int limit;
  private List<String> classpathFiles;

  private File compileLog;
  private File errorLog;
  private File outputLog;
  private boolean timedout;

  private boolean threadLock;
  private boolean threadRunning;

  /** Constructor
   *  @param File path The folder of code to be compiled
   *  @param String filename The name of the file to be compiled/run (do not include extension)
   *  @param int timeout The number of milliseconds to wait for this program to run before timing out. Use 0 for no timeout.
   */
  public JavaRunner(File path, String filename, int timeout)
  {
    this(path.getPath(), filename, timeout);
  }

  /** Constructor
   *  @param String path The path to the code to be compiled
   *  @param String filename The name of the file to be compiled/run (do not include extension)
   *  @param int timeout The number of milliseconds to wait for this program to run before timing out. Use 0 for no timeout.
   */
  public JavaRunner(String path, String filename, int timeout)
  {
    this.path = path + File.separator;
    this.filename = filename;
    this.timer = new Timer(this, timeout);
    this.limit = timeout;
    this.classpathFiles = new ArrayList<>();

    this.compileLog = new File(pathTo("compile.log"));
    this.outputLog = new File(pathTo("output.log"));
    this.errorLog = new File(pathTo("error.log"));
    this.timedout = false;
  }

  /** Change the file that will be compiled/run
   *  @param String filename the new file name
   */
  public void setFilename(String filename)
  {
    this.filename = filename;
  }

  public File getCompileLog()
  {
    return compileLog;
  }

  public File getErrorLog()
  {
    return errorLog;
  }

  public File getOutputLog()
  {
    return outputLog;
  }

  public boolean timedOut()
  {
    return timedout;
  }

  public void addClassPath(File cp)
  {
    classpathFiles.add(cp.toString());
  }

  public void addClassPath(String cpFilename)
  {
    classpathFiles.add(cpFilename);
  }

  public void addLocalClassPath(String cpFilename)
  {
    classpathFiles.add(pathTo(cpFilename));
  }

  public File getFileToCompile()
  {
    return new File(pathTo(filename));
  }

  /** Compile and run the specified program.
   *
   *  This method only blocks while compiling. The target program will run non-blockingly.
   */
  public boolean compileAndRun()
  {
    this.timedout = true;
    this.compileLog = null;
    this.errorLog = null;
    this.outputLog = null;

    if(compile())
    {
      execute();
      return true;
    }
    return false;
  }

  /** Execute the specified program. This method is non-blocking */
  public void execute()
  {
    Thread t = new Thread(this);
    threadLock = true;
    t.start();
  }

  /** Execute the specified program.
   *  @param boolean blocking if true, this method will block until specified program has completed.
   *                 Othewise, this method will be non-blocking
   */
  public void execute(boolean blocking)
  {
    execute();
    if(blocking)
    {
      while(threadLock)
      {
        try
        {
          Thread.sleep(1);
        }
        catch(Exception e) {}
      }
    }
  }

  /** Executes the specified program. Do not call this method directly.
   *
   *  1. Deletes output.log and error.log
   *  2. Executes the specified program
   *  3. Writes System.out to output.log & System.err to error.log
   */
  public void run()
  {
    // don't allow external access to this method
    // it can only be called from the javac method
    if(!threadLock || threadRunning)
      return;
    threadRunning = true;

    try
    {
      Files.deleteIfExists(outputLog.toPath());
      Files.deleteIfExists(errorLog.toPath());

      String cp = path;
      for(String cpFilename : classpathFiles)
      {
        cp += File.pathSeparator + cpFilename;
      }

      Runtime run = Runtime.getRuntime();

      timer.start();

      String cmd = "java -cp " + cp + " " + filename;
      proc = run.exec(cmd);

      writeLines(outputLog.getPath(), proc.getInputStream());
      writeLines(errorLog.getPath(), proc.getErrorStream());

      proc.waitFor();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      timer.terminate();
      threadRunning = false;
      threadLock = false;
    }
  }

  /** Compiles the specified program
   *
   *  1. Deletes compile.log & all .class files
   *  2. Compile the program using javac
   *  3. Write output to compile.log
   *
   *  @return boolean true if no compile errors, otherwise false.
   */
  public boolean compile()
  {
    try
    {
      Files.deleteIfExists(compileLog.toPath());

      //TODO: make this delete all .class files!
      Files.deleteIfExists(Paths.get(pathTo(filename, "class")));
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }

    String cp = path;
    for(String cpFilename : classpathFiles)
    {
      cp += File.pathSeparator + cpFilename;
    }

    try
    {
      Runtime run = Runtime.getRuntime();
      String cmd = "javac -cp " + cp + " " + pathTo(filename, "java");
      Process proc = run.exec(cmd);

      writeLines(compileLog.getPath(), proc.getErrorStream());

      proc.waitFor();

      return proc.exitValue() == 0;
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }

    return false;
  }

  /** Concatenate the file name with the path
   *
   *  @param String file the file name to append to the path
   *
   *  @return String path/filename
   */
  public String pathTo(String file)
  {
    return path + file;
  }

  /** Concatenate the  file name with the path and the extension to the file name
   *
   *  @param String file the file name to append to the path
   *  @param String ext the extension to append to the file name
   *
   *  @return String path/filename.ext
   */
  public String pathTo(String file, String ext)
  {
    if(null == ext) return path + file;
    return path + file + "." + ext;
  }

  /** Notify the thread when a running program has timed out */
  protected void timeout()
  {
    this.timedout = true;
    if(null != proc)
    {
      proc.destroy();
      try
      {
        Files.deleteIfExists(Paths.get(pathTo("/timeout.log")));
        writeLines(pathTo("timeout.log"), "Exceeded time limit ("+limit+")");
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}

/** Helper class to stop a running program if it exceeds its timeout limit */
class Timer extends Thread
{
  private JavaRunner runner;
  private boolean running;
  private long start;
  private long limit;

  public Timer(JavaRunner runner, int limit)
  {
    this.runner = runner;
    this.running = false;

    int buffer = 500; // a few extra miliseconds to compansate for *this* code

    this.limit = limit + buffer;
  }

  public void run()
  {
    // only run if the limit is a positive value
    if(limit <= 0)
      return;

    long now, diff;

    running = true;
    start = System.currentTimeMillis();
    while(running)
    {
      Thread.yield();
      now = System.currentTimeMillis();
      diff = now - start;

      if(running && diff >= limit)
      {
        runner.timeout();
        this.terminate();
      }
    }
  }

  public void terminate()
  {
    running = false;
  }
}
