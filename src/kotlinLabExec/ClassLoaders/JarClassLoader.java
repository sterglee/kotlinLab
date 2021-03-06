package kotlinLabExec.ClassLoaders;

import kotlinLabExec.kotlinLab.kotlinLabToolbox;
import kotlinLabGlobal.Interpreter.GlobalValues;

import kotlinLabExec.gui.AutoCompletion;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.jar.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a simple class loader  that can be used to load at runtime 
 * classes contained in a JAR file.
 *
 */
public class JarClassLoader extends ClassLoader
{
    public int  numLoadedClass;
    Hashtable  clTable;  // reference to the global hashtable of loaded classes
    TreeSet    sortedLoadedClasses;   // keeps sorted the loaded classes of the toolbox
    Vector avoidClassPatterns = new Vector();  // some classes can be problematic for autocompletion or for loading their methods. We can specify them
                            // in a file name "AvoidClassPatterns.txt" in order to avoid loading them
            
    
    /**
   * Creates a new JarClassLoader that will allow the loading
   * of classes stored in a jar file.
   *
   * @param jarFileName   the name of the jar file
   * @exception IOException   an error happened while reading
   * the contents of the jar file
   */
    
    public JarClassLoader() {
    }
    
     // returns true if the Java class javaName is not within the class names that we have to avoid
  private boolean checkAllowed(String javaName) {
    for (int k=0; k<avoidClassPatterns.size(); k++) {
        String currentClassToAvoid = (String)avoidClassPatterns.get(k);
        if (javaName.contains(currentClassToAvoid))
            return false;
      }
    return true;
    }
      


 
    // scan the jarFileName for patterns beginning with LibDir
  public  static Vector scanLib(String jarFileName, String LibDir)  
  {
    Vector scannedLibs = new Vector();  
    int libPathLen = LibDir.length();  // the length of the prefix
    int numLibAllClasses = 0;      // number of classes
    int numLibAllMethods=0;     // number of methods
    //  get reference to the global Hashtable that holds all loaded classes
  JarEntry je;
  JarInputStream jis=null;
        try {
            jis = new JarInputStream(new BufferedInputStream  (new FileInputStream(jarFileName)));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            while ((je = jis.getNextJarEntry()) != null)
                {   // while jar file has entries unprocessed
                 String nameOfEntry = je.toString();
                 
          // make sure we have only slashes, i.e. use Unix path conventions
                  String name ='/'+je.getName().replace('\\', '/');
          
                 int strLen = nameOfEntry.length();
                 if (strLen > libPathLen) {   // entry can be considered further
                     
                     nameOfEntry = nameOfEntry.substring(0, libPathLen);   // extract prefix

                     if (nameOfEntry.equalsIgnoreCase(LibDir))  { // scan only entries starting with LibDir subdirectory
                  
                      String javaName = name.replace('/', '.');
                      String lowerCaseJavaName = javaName.toLowerCase();  
                      if (lowerCaseJavaName.contains("test") == false && lowerCaseJavaName.contains("$") == false) {  // avoid test classes
                            
                            int idx = javaName.lastIndexOf(".class");
                            int javaNameLen = javaName.length();
                            boolean classStringIsWithinName = javaNameLen > ( idx+".class".length() );
                            if  (idx != -1 && !classStringIsWithinName) {  // a class file
                            javaName = javaName.substring(1, idx);    // remove the first '.'
                 
                  
                         Class  foundClass = null;  
                             try {
                     foundClass = GlobalValues.extensionClassLoader.loadClass(javaName);
                       }
                       catch (ClassNotFoundException e) {
                           foundClass = null;
                       }
                    if (foundClass!=null)
                        scannedLibs.add(foundClass);
                      
                       }   // a class file
                      }  // avoid test classes
                      
                   }   // scan only  LibDir  subdirectory
                 
              } // strLen > libPathLen
            } // while jar file has entries unprocessed
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            jis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
  
 return scannedLibs;  // return the Vector of methods
 }
 

  
  /**
the scanBuildInLibraryClasses(String libDir, String jarFileName) scans the jar library file jarFileName
 in order to retrieve information for classes that have prefix libDir
   **/
  public  int  scanBuiltInLibraryClasses(String libDir, String jarFileName)    throws IOException 
  {
    String  LibDir = libDir;
    int libPathLen = LibDir.length();  // the length of the prefix
    int numLibAllClasses = 0;    
    int numLibAllMethods=0;
    //  get reference to the global Hashtable that holds all loaded classes
  JarEntry je;
  JarInputStream jis = new JarInputStream(new BufferedInputStream  (new FileInputStream(jarFileName)));
  
  while ((je = jis.getNextJarEntry()) != null)
      {   // while jar file has entries unprocessed
       String nameOfEntry = je.toString();
       
       int strLen = nameOfEntry.length();
       if (strLen > libPathLen) {  // strLen > libPathLen
        nameOfEntry = nameOfEntry.substring(0, libPathLen);   
       if (nameOfEntry.equalsIgnoreCase(LibDir))  { // scan only  LibDir  subdirectory
// make sure we have only slashes, i.e. use Unix path conventions
	String name ='/'+je.getName().replace('\\', '/');
        String   remainingClassName = name.substring(libPathLen+1, name.length());
        
            String javaName = name.replace('/','.');
            int idx = javaName.lastIndexOf(".class");
            int javaNameLen = javaName.length();
            boolean classStringIsWithinName = javaNameLen > ( idx+".class".length() );
            if (idx != -1 && !classStringIsWithinName) {  // a class file
            javaName = javaName.substring(1, idx);    // remove the first '.'
       
        
               Class  foundClass = null;  
                   try {
           foundClass = GlobalValues.extensionClassLoader.loadClass(javaName);
             }
             catch (ClassNotFoundException e) {
                 foundClass = null;
             }

           if (javaName.indexOf("$") == -1) {
        String smallName = javaName.substring(javaName.lastIndexOf(".")+1, javaName.length());
        String nameToInsert = smallName+GlobalValues.smallNameFullPackageSeparator+javaName;
        AutoCompletion.scanMethods .add(nameToInsert);
        numLibAllClasses++;
           }

        if (foundClass != null)  {
            Method [] classMethods = foundClass.getDeclaredMethods();
            for (Method currentMethod: classMethods) {
                if (Modifier.isPublic(currentMethod.getModifiers() )) {
                    String methodName = currentMethod.getName()+GlobalValues.smallNameFullPackageSeparator+javaName;
                    if (AutoCompletion.scanMethods .indexOf(methodName)==-1)  {
                         AutoCompletion.scanMethods .add(methodName);
                         numLibAllMethods++;
                    }
                         
               }
          }
        }
            }   // a class file
            
         }   // scan only  LibDir  subdirectory
       
    } // strLen > libPathLen
  } // while jar file has entries unprocessed
  
 jis.close();
  
 return numLibAllClasses;  // return the number of classes
 }
 
  

  
  public static Vector  scanAll( String jarFileName)  {
    int numLibAllClasses = 0;    
    int numLibAllMethods=0;
    Vector scannedLibs = new Vector();  
    
    //  get reference to the global Hashtable that holds all loaded classes
  JarEntry je;
  JarInputStream jis=null;
        try {
            jis = new JarInputStream(new BufferedInputStream  (new FileInputStream(jarFileName)));
        } catch (IOException ex) {
            Logger.getLogger(JarClassLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
  
        try {
            while ((je = jis.getNextJarEntry()) != null)
            {   // while jar file has entries unprocessed
                String nameOfEntry = je.toString();
                
                int strLen = nameOfEntry.length();
                
// make sure we have only slashes, i.e. use Unix path conventions
                String name ='/'+je.getName().replace('\\', '/');
                
                String javaName = name.replace('/','.');
                int idx = javaName.lastIndexOf(".class");
                int javaNameLen = javaName.length();
                boolean classStringIsWithinName = javaNameLen > ( idx+".class".length() );
                if (idx != -1 && !classStringIsWithinName) {  // a class file
                    javaName = javaName.substring(1, idx);    // remove the first '.'
                    
                    
                    Class  foundClass = null;
                    try {
                        foundClass = GlobalValues.jshell.getClass().getClassLoader().loadClass(javaName);
                    }
                    catch (ClassNotFoundException e) {
                        foundClass = null;
                    }
                    
                    if (javaName.indexOf("$") == -1) {
                        String smallName = javaName.substring(javaName.lastIndexOf(".")+1, javaName.length());
                        String nameToInsert = smallName+GlobalValues.smallNameFullPackageSeparator+javaName;
                        AutoCompletion.scanMethods .add(nameToInsert);
                        numLibAllClasses++;
                    }
                    
                    if (foundClass != null)  {
                        scannedLibs.add(foundClass);
                        
                        Method [] classMethods = foundClass.getDeclaredMethods();
                        for (Method currentMethod: classMethods) {
                            if (Modifier.isPublic(currentMethod.getModifiers() )) {
                                String methodName = currentMethod.getName()+GlobalValues.smallNameFullPackageSeparator+javaName;
                                if (AutoCompletion.scanMethods .indexOf(methodName)==-1)  {
                                    AutoCompletion.scanMethods .add(methodName);
                                    numLibAllMethods++;
                                }
                            }
                        }
                    }   // foundClass != null
                } // a class file
            } // while jar file has entries unprocessed
        } catch (IOException ex) {
            Logger.getLogger(JarClassLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
  
        try {
            jis.close();
        } catch (IOException ex) {
            Logger.getLogger(JarClassLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
  
 return scannedLibs;
 }
 
  
  
  public  static Vector scanNRNumAlLibs(String jarFileName)  
  {
    Vector NRNumAlLibs = new Vector();  
    String  LibDir = "com";
    int libPathLen = LibDir.length();  // the length of the prefix
    int numLibAllClasses = 0;    
    int numLibAllMethods=0;
    //  get reference to the global Hashtable that holds all loaded classes
  JarEntry je;
  JarInputStream jis=null;
        try {
            jis = new JarInputStream(new BufferedInputStream  (new FileInputStream(jarFileName)));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            while ((je = jis.getNextJarEntry()) != null)
                {   // while jar file has entries unprocessed
                 String nameOfEntry = je.toString();
                 
                 int strLen = nameOfEntry.length();
                 if (strLen > libPathLen) {  // strLen > libPathLen
                  nameOfEntry = nameOfEntry.substring(0, libPathLen);   
                 if (nameOfEntry.equalsIgnoreCase(LibDir))  { // scan only  LibDir  subdirectory
          // make sure we have only slashes, i.e. use Unix path conventions
                  String name ='/'+je.getName().replace('\\', '/');
                  String   remainingClassName = name.substring(libPathLen+1, name.length());
                  
                      String javaName = name.replace('/','.');
                      if (javaName.contains("test") == false) {  // avoid test classes
                    
                      int idx = javaName.lastIndexOf(".class");
                      int javaNameLen = javaName.length();
                      boolean classStringIsWithinName = javaNameLen > ( idx+".class".length() );
                      if (idx != -1 && !classStringIsWithinName) {  // a class file
                      javaName = javaName.substring(1, idx);    // remove the first '.'
                 
                  
                         Class  foundClass = null;  
                             try {
                     foundClass = foundClass = GlobalValues.jshell.getClass().getClassLoader().loadClass(javaName);
                       }
                       catch (ClassNotFoundException e) {
                           foundClass = null;
                       }
                    if (foundClass!=null)
                        NRNumAlLibs.add(foundClass);
                      
                       }   // a class file
                      }  // avoid test classes
                      
                   }   // scan only  LibDir  subdirectory
                 
              } // strLen > libPathLen
            } // while jar file has entries unprocessed
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            jis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
  
 return NRNumAlLibs;  // return the Vector of methods
 }
  
    
  /**
     * scans the built-in   library classes from the .jar file
     * Groovy classes are housed at the directory /groovy .
     * Returns the number of the classes that successfully accessible and fills their information for autocompletion
     **/
  public  int  scanBuiltInClasses(String jarFileName)    throws IOException 
  {
    String  kotlinSciDir = "kotlinLabSci";   // matrix functions, plotting functions etc.
    int libPathLen = kotlinSciDir.length();  // the length of the smallest dir
    int numGroovyLoadedClasses = 0;    
    int numGroovyMethods = 0;
    //  get reference to the global Hashtable that holds all loaded classes
    
        JarEntry je;
  JarInputStream jis = new JarInputStream(new BufferedInputStream  (new FileInputStream(jarFileName)));
  
  
  while ((je = jis.getNextJarEntry()) != null)
      {   // while jar file has entries unprocessed
       String nameOfEntry = je.toString();
  
        int strLen = nameOfEntry.length();
       if (strLen > libPathLen) {  
           int idxSeparator = nameOfEntry.indexOf('/');
           if (idxSeparator != -1)  {
       nameOfEntry = nameOfEntry.substring(0, idxSeparator);
       if ( nameOfEntry.equalsIgnoreCase(kotlinSciDir))  { // scan only  subdirectories starting with kotlinSciDir
// make sure we have only slashes, i.e. use Unix path conventions
	String name ='/'+je.getName().replace('\\', '/');
        int pathLen=libPathLen;
        String   remainingClassName = name.substring(pathLen+1, name.length());
        
            String javaName = name.replace('/','.');
            int idx = javaName.lastIndexOf(".class");
            if (idx!=-1) {   // "class" exists in name
             int javaNameLen = javaName.length();
              boolean classStringIsWithinName = javaNameLen > ( idx+".class".length() );
              if (!classStringIsWithinName) {  // a class file
            javaName = javaName.substring(1, idx);    // remove the first '.'
       
  Class  foundClass = null;  
                   try {
           foundClass = foundClass = GlobalValues.jshell.getClass().getClassLoader().loadClass(javaName);
             }
             catch (ClassNotFoundException e) {
                 foundClass = null;
             }
            
           if (foundClass != null)  {
            Method [] classMethods = foundClass.getDeclaredMethods();
            for (Method currentMethod: classMethods) {
                if (Modifier.isPublic(currentMethod.getModifiers() )) {
                  String methodName = currentMethod.getName()+GlobalValues.smallNameFullPackageSeparator+javaName;
                    
                    if (AutoCompletion.scanMethods .indexOf(methodName)==-1) {
                    AutoCompletion.scanMethods .add(methodName);
                    numGroovyMethods++;
                  }
                 }
            }
         }  // foundClass != null
        }
  
      if (javaName.indexOf("$") == -1) {
        String smallName = javaName.substring(javaName.lastIndexOf(".")+1, javaName.length());
        String nameToInsert = smallName+GlobalValues.smallNameFullPackageSeparator+javaName;
        AutoCompletion.scanMethods .add(nameToInsert);
        numGroovyLoadedClasses++;
         }

      
                 }   // a class file
               }   // "class" exists in name
           }  // scan only  subdirectories starting with kotlinSciDir
          }  // idxSeparator != -1
            } // strLen > libPathLen
       

  
 jis.close();
 System.out.println("   LoadedClasses = "+numGroovyLoadedClasses+",   Methods = "+numGroovyMethods);
 System.out.println("Groovy Classes = "+numGroovyLoadedClasses);
 System.out.println("Groovy Methods = "+numGroovyMethods);
  
 return numGroovyLoadedClasses;
 }

  
  
 
  public String  readTextFromJar(String jarFileName, String s) {
       StringBuilder  initCode = new StringBuilder();
      
               try {
      JarFile jarFile = new JarFile(jarFileName);
     InputStream in = jarFile.getInputStream(jarFile.getEntry(s));
     Scanner inScanner = new Scanner(in);
     while (inScanner.hasNextLine())
         initCode.append(inScanner.nextLine()+"\n");
     String code = initCode.toString();
      }
    catch (IOException ioe)  
    { 
        ioe.printStackTrace();
    }

       String readedText = initCode.toString();
       GlobalValues.toolboxStartUpcode += readedText;    // append to the pending code for initialization of the toolbox
       return readedText;
   
  }

        
 
  // scan the jarFileName for classes without loading them for extracting information
  public  int  scanAllBuiltInClasses(String jarFileName)    throws IOException 
  {
      
    int numJarClasses = 0;    
    int numJarAutoCompletionItems=0;
    //  get reference to the global Hashtable that holds all loaded classes
         int numOfPatternsToAvoid = 0;
    //  get reference to the global Hashtable that holds all loaded classes
    String startupCode = null, patternsToAvoid = null;

        kotlinLabToolbox kotlinLabToolbox = new kotlinLabToolbox();
           
    HashMap <String, Boolean> specifiedToolboxes = GlobalValues.jartoolboxesLoadedFlag;  // the table of already loaded toolboxes
    boolean toolboxInited = specifiedToolboxes.containsKey(jarFileName);
    if (toolboxInited == false)  {  // load the specified jar toolbox
     
        GlobalValues.jartoolboxesLoadedFlag.put(jarFileName, true);
        
         int toolboxIndex = 0;   // the kotlinLabToolbox class keeps the name of the .jar file of each toolbox and a Vector that holds the bytecodes of its classes
           kotlinLabToolbox.toolboxName = jarFileName;
  
           
            LinkedList <String> pathsList = new LinkedList<String>();
            pathsList.add("." );   // current directory
            for (int k=0; k<GlobalValues.jartoolboxes.size(); k++)
               pathsList.add((String)GlobalValues.jartoolboxes.get(k));
        // append now the jLabClassPath to the CompilerConfiguration path, in order to make them   accessible 
            // append now the  ClassPath components
          
            pathsList.add(jarFileName);
            
           }
   
    
        JarEntry je;
  JarInputStream jis = new JarInputStream(new BufferedInputStream  (new FileInputStream(jarFileName)));
  

    //  scan first the jar for the file "AvoidClassPatterns.txt" and for the startup code
    while  ((je = jis.getNextJarEntry()) != null)  {
        String nameOfEntry = je.toString();
       
        if (nameOfEntry.contains("AvoidClassPatterns.txt"))  {
             patternsToAvoid = readTextFromJar(jarFileName, nameOfEntry);    // get the patterns to avoid from the .jar file
             StringTokenizer strTok = new StringTokenizer(patternsToAvoid, " \n,\t;");
             numOfPatternsToAvoid =strTok.countTokens();
             while (strTok.hasMoreElements())  
                 avoidClassPatterns.add(strTok.nextToken());
             break;
         }
        
    }

   
  jis.close();
  jis = new JarInputStream(new BufferedInputStream  (new FileInputStream(jarFileName)));
  while ((je = jis.getNextJarEntry()) != null)
      {   // while jar file has entries unprocessed
       String nameOfEntry = je.toString();
    
       if (nameOfEntry.contains("startup.gsci"))  {
             readTextFromJar(jarFileName, nameOfEntry);    // get the startup code from the .jar file
         }
       
// make sure we have only slashes, i.e. use Unix path conventions
	String name ='/'+je.getName().replace('\\', '/');
  
            String javaName = name.replace('/','.');
            int idx = javaName.lastIndexOf(".class");
            int javaNameLen = javaName.length();
            boolean classStringIsWithinName = javaNameLen > ( idx+".class".length() );
            if (idx != -1 && !classStringIsWithinName) {  // a class file
               javaName = javaName.substring(1, idx);    // remove the first '.'
       
if (checkAllowed(javaName) == true)   {   // not a class name that we should avoid to load  

               Class  foundClass = null;  
                   try {
           foundClass = GlobalValues.jshell.getClass().getClassLoader().loadClass(javaName);
             }
               catch (Exception  e) {
                   foundClass = null;
               }
             
            
           if (foundClass != null)  {
               int modifier = foundClass.getModifiers();
               if (Modifier.isAbstract(modifier)==false && Modifier.isInterface(modifier)== false &&  Modifier.isStatic(modifier)==false)    {  // class is not abstract or interface
                 if ( Modifier.isPublic(modifier) )  {  // class is public
                  
                   kotlinLabToolbox.toolboxClasses.add(foundClass);
               if (GlobalValues.retrieveAlsoMethods)  {
                   try {
            Method [] classMethods = foundClass.getDeclaredMethods();
            if (classMethods.length > 0)
             for (Method currentMethod: classMethods) {
                if (Modifier.isPublic(currentMethod.getModifiers() )) {
                    String methodName = currentMethod.getName()+GlobalValues.smallNameFullPackageSeparator+javaName;
                    AutoCompletion.scanMethods .add(methodName);
                  numJarAutoCompletionItems++;
                      }
                   }
                   }
               catch (Exception e) {}
                }
                           
if (javaName.indexOf("$") == -1) {
       
         String smallName = javaName.substring(javaName.lastIndexOf(".")+1, javaName.length());
        String nameToInsert = smallName+GlobalValues.smallNameFullPackageSeparator+javaName;
        String elemToInsert = nameToInsert;
        if (AutoCompletion.scanMethods .indexOf(elemToInsert) == -1)   {
           AutoCompletion.scanMethods .add(elemToInsert);
           numJarAutoCompletionItems++;
           numJarClasses++;
                            }
                         }
                      }
                  }     // class is public
               }  // class is not abstract or interface
           }    // foundClasss != null
        }   // a class file
    }  // while jar file has entries unprocessed
       
 // construct a new AutoCompletion Object (needed in order to sort the list of methods, and to permit the access of the new methods)
            GlobalValues.AutoCompletionJShell= new kotlinLabExec.gui.AutoCompletion();  // create the autocompletion object
               
 jis.close();
 System.out.println("number of Toolbox LoadedClasses = "+numJarClasses);
 System.out.println("number of Toolbox AutoCompletion Items = "+numJarAutoCompletionItems);
 System.out.println("Total items of Autocompletion = "+ GlobalValues.AutoCompletionJShell.scanMethods.size());
 
     
 return numJarAutoCompletionItems;
 
 }


private void   updateClassPathWithToolboxes() {
  
 if (GlobalValues.toolboxStartUpcode.isEmpty()==false )   {   // execute toolbox init code pending after loading the toolboxes
                // set the compiler configuration to have the list of the specified .jar files in its classpath
 
                LinkedList <String> pathsList = new LinkedList<String>();
            pathsList.add("." );   // current directory
            for (int k=0; k<GlobalValues.jartoolboxes.size(); k++) {
               String pathToAdd = (String)GlobalValues.jartoolboxes.get(k);
               if (pathsList.contains(pathToAdd)==false)
                  pathsList.add(pathToAdd);
            }
        

         }
 }
}

  
