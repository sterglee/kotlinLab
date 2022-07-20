package kotlinLabEdit;

import kotlin.io.LineReader;
import kotlin.script.experimental.api.ResultWithDiagnostics;
import kotlinLabGlobal.Interpreter.GlobalValues;
import kotlinLabExec.gui.AutoCompletionFrame;
import kotlinLabExec.gui.DetailHelpFrame;

import java.awt.Container;
import java.awt.Point;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.script.ScriptException;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import org.antlr.v4.runtime.misc.ObjectEqualityComparator;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper;

public class EditorKeyMouseHandler extends MouseAdapter implements KeyListener
{
    IntStream x;
        int caretPos = 0;      // track the cursor position 
        int prevCaretPos = 0;
        int  textLen = 0;  // the text lenngth
        int fromLoc = 0;
        int toLoc = 0;
       
        public RSyntaxTextArea  editorPane=null;    // the component that keeps and handles the editing text
        public RSyntaxDocument  docVar=null; 
        public RSyntaxDocument syntaxDocument=null;
    private ResultWithDiagnostics<?> result;

    public EditorKeyMouseHandler()
	{
	}


// update fields denoting the document in the editor, necessary when a new document is edited
  public  RSyntaxDocument updateDocument()  {
         
          docVar = (RSyntaxDocument) editorPane.getDocument();
          syntaxDocument = docVar;
          
          return syntaxDocument;
  }
               
     
  
   public  String  getCurrentLine() {
       if (docVar==null)
           updateDocument();
           
       RSyntaxDocument  myDoc = syntaxDocument;
       
       int caretpos = editorPane.getCaretPosition();
       int startpos = editorPane.getCaretOffsetFromLineStart();
       int scanpos = caretpos-startpos;
       String s = "";
       try {
            char ch = myDoc.charAt(scanpos);
       while (ch!='\n') {
                s += myDoc.charAt(scanpos);
            
           scanpos += 1;
           ch = myDoc.charAt(scanpos);
       }
       } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
       
       return s;
   }
       
     
   public  String   getSelectedTextOrCurrentLine() {
       String selectedTextOrCurrentLine = editorPane.getSelectedText();
       if (selectedTextOrCurrentLine==null)
           selectedTextOrCurrentLine = getCurrentLine();
       
       return selectedTextOrCurrentLine;
   }
     

        
         
    public void keyTyped(KeyEvent e){
       /* int  keyValue = e.getKeyChar();
       
        if (keyValue == KeyEvent.VK_F10);
                 display_help();      */
   }


        
        private static void  processListSelection(JList clList) { 
                String selected = (String) clList.getSelectedValue();
              
                GlobalValues.globalEditorPane.setSelectionStart(GlobalValues.selectionStart);
                GlobalValues.globalEditorPane.setSelectionEnd(GlobalValues.selectionEnd);
                
                int leftParenthesisIndex = selected.indexOf('(');
                if (leftParenthesisIndex != -1) 
                    selected = selected.substring(0, leftParenthesisIndex+1);
                    
                if (GlobalValues.methodNameSpecified==false)
                    selected = "." + selected;  // append a dot
                
                GlobalValues.globalEditorPane.replaceSelection( selected);
                  GlobalValues.selectionEnd = GlobalValues.selectionStart+selected.length();
        }
        
              
        
        private static String detectWordAtCursor() {
                
         RSyntaxTextArea  editor = GlobalValues.globalEditorPane;   // get RSyntaxArea based editor instance
         
         int  pos = editor.getCaretPosition()-1;      // position of the caret
         Document  doc = editor.getDocument();  // the document being edited
       
         GlobalValues.methodNameSpecified = false;
         GlobalValues.selectionStart = -1;
         
       boolean  exited = false;
        // take word part before cursor position
       String  wb = "";  // constructs the word before the cursor
       int  offset = pos;
       while (offset >= 0 && exited==false) {
         char  ch = 0;
            try {
                ch = doc.getText(offset, 1).charAt(0);
                if (ch == '.') {  // a method specified
                    GlobalValues.methodNameSpecified = true;
                    GlobalValues.selectionStart = offset+1;   // mark the start of the method
                }
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
         boolean  isalphaNumeric = ( ch >= 'a' && ch <='z')  || (ch >= 'A' && ch <='Z') || (ch >= '0' && ch <='9') || ch=='.' || ch=='_';
         if (!isalphaNumeric)  exited = true;
          else {
           wb = wb + ch;
           offset--;
            }
          }
         
       if (GlobalValues.selectionStart == -1)  // a method name is not specified, thus set selection start to the beginning of the word
      GlobalValues.selectionStart = pos+1;
    
          // take word part after cursor position
       String  wa = "";
       int  docLen = doc.getLength();
       offset = pos+1;
       exited = false;
       while (offset < docLen && exited==false) {
         char  ch = 0;
            try {
                ch = doc.getText(offset, 1).charAt(0);
                if (ch == '.')  GlobalValues.methodNameSpecified = true;
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
         boolean  isalphaNumeric = ( ch >= 'a' && ch <='z')  || (ch >= 'A' && ch <='Z') || (ch >= '0' && ch <='9') || ch=='.' ||  ch=='_';
         if (!isalphaNumeric)  exited = true;
           else {
         wa = wa + ch;
         offset++;
           }
         }
       GlobalValues.selectionEnd = offset;
  
      //   form total word that is under caret position
         // reverse string at the left of cursor
       StringBuffer sb = new StringBuffer(wb);
       StringBuffer rsb = new StringBuffer(wb);
       int p = 0;
       for (int k= wb.length()-1; k>=0; k--)
           rsb.setCharAt(p++, sb.charAt(k));
        
       // concatenate to form the whole word
       String  wordAtCursor = rsb.toString()+wa;

       wordAtCursor = wordAtCursor.trim();  // trim any spaces

       GlobalValues.textForCompletion = wordAtCursor; 
       
        return wordAtCursor;
        }
        
        public void buildjshellTool(){
            
        }
        


        public void clickComplete() {
            String currentTextForEval = getSelectedTextOrCurrentLine().trim();
           //   GlobalValues.kshell.getCompleter().complete
        }

        public void clickExecuteScriptEngine() {
            Runnable execCode = ()-> {
                String currentTextForEval = getSelectedTextOrCurrentLine().trim();

                try {
                    if (GlobalValues.kshell == null) {
                        return;
                    }

                    GlobalValues.globalEditorPane.setCursor(GlobalValues.waitCursor);

          Object evalResult =   GlobalValues.kshell.eval(currentTextForEval);


          // for assignment statements, display the value assigned
        String[] leftRight = currentTextForEval.split("=");
              String varname = leftRight[0];
                    if (varname != null) {

                        if (varname.contains("(") == false) { // not a function
                  varname = varname.replace("var", "");
                  varname = varname.replace("val", "");
                  varname = varname.trim();
                  System.out.println(varname);

                      Object ores = GlobalValues.kshell.eval(varname);
                      if (ores != null) {
                          String res = ores.toString();
                          System.out.println(res);
                      }
                      else
                          System.out.println(evalResult);
                  }
              } // not a function
          //}
              GlobalValues.globalEditorPane.setCursor(GlobalValues.defaultCursor);

                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    GlobalValues.globalEditorPane.setCursor(GlobalValues.defaultCursor);
                 }
                } ;

                GlobalValues.execService.execute(execCode);



        }


    public void clickExecuteKShell() {
        Runnable execCode = ()-> {
            String currentTextForEval = getSelectedTextOrCurrentLine().trim();
            GlobalValues.globalEditorPane.setCursor(GlobalValues.waitCursor);

            ResultWrapper evalResult = GlobalValues.kshell.eval(currentTextForEval);
            var result = evalResult.getResult();

            if (evalResult!=null)
                    System.out.println(evalResult.getResult());
            GlobalValues.globalEditorPane.setCursor(GlobalValues.defaultCursor);

        };
        try {
            GlobalValues.execService.execute(execCode);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            GlobalValues.globalEditorPane.setCursor(GlobalValues.defaultCursor);
         }
        }

    public void clickExecuteCode() {
            
                Runnable  execCode = () -> {
          
                    String  currentTextForEval = getSelectedTextOrCurrentLine().trim();
          
          buildjshellTool();
          
          GlobalValues.globalEditorPane.setCursor(GlobalValues.waitCursor);
       
          final String copyOfCurrentTextForEval = currentTextForEval;
         
           if (currentTextForEval != null) {
       SourceCodeAnalysis sca = GlobalValues.jshell.sourceCodeAnalysis();
       
       List<String> snippets = new ArrayList<>();
       do {
           SourceCodeAnalysis.CompletionInfo info = sca.analyzeCompletion(currentTextForEval);
           snippets.add(info.source());
           currentTextForEval = info.remaining();
       } while (currentTextForEval.length() > 0);

        //SysUtils.ConsoleWindow.enable = false;
        List<SnippetEvent> grResultSnippets = 
                snippets.stream().map(GlobalValues.jshell::eval).
                        flatMap(List::stream).collect(Collectors.toList());       

          GlobalValues.globalEditorPane.setCursor(GlobalValues.defaultCursor);
       
        //SysUtils.ConsoleWindow.enable = true;
        if (grResultSnippets != null) {
            String rmSuccess = grResultSnippets.toString().replace("Success", "");    
   
            for (SnippetEvent snippetEvent: grResultSnippets) {
                Snippet currentSnippet = snippetEvent.snippet();
                Snippet.Kind  Kind = currentSnippet.kind();
   /*             if (Kind==Snippet.Kind.VAR) {
                    
                var vvalue = snippetEvent.value();
                if (vvalue!=null) {
                int MxLen = kotlinLabGlobal.Interpreter.GlobalValues.mxLenToDisplay;
                if (vvalue.length()> MxLen)
                     vvalue = vvalue.substring(0,MxLen-1);
                System.out.println(vvalue);
                 }
                }
                else 
                */
                if (Kind==Snippet.Kind.METHOD) {
                    String  vvalue = snippetEvent.snippet().toString();
                    System.out.println("defining method ");
                    System.out.println(vvalue);
                }
              else if (Kind == Snippet.Kind.IMPORT) {
                    String vvalue = snippetEvent.snippet().source();
                    System.out.println("defining import");
                    System.out.println(vvalue);
                }

                else if (Kind ==Snippet.Kind.ERRONEOUS) {
                    System.out.println("error: "+snippetEvent.value());
                    java.util.stream.Stream<jdk.jshell.Diag> diags = GlobalValues.jshell.diagnostics(currentSnippet);
                    diags.forEach(System.out::println);
        
                }
            }
        }
         GlobalValues.consoleOutputWindow.output.setCaretPosition(GlobalValues.consoleOutputWindow.output.getText().length());
        GlobalValues.globalEditorPane.setCursor(java.awt.Cursor.getDefaultCursor());
             
    System.out.flush();
        
   }
        
           };
                try {
                    GlobalValues.execService.execute(execCode);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    GlobalValues.globalEditorPane.setCursor(java.awt.Cursor.getDefaultCursor());

                }
        }
        
        
	/**Interpret key presses*/
    public void keyPressed(final KeyEvent e)
    {
        kotlinLabEditor.documentEditsPendable = true;
        int keyValue = e.getKeyCode();
        editorPane  = (RSyntaxTextArea)e.getSource();  // editor's keystrokes have as source the inputTextComponent JTextComponent
        prevCaretPos = caretPos;   
        
        switch (keyValue) {
                        

            case   KeyEvent.VK_ENTER:
                caretPos = editorPane.getCaretPosition();
                String text = editorPane.getText();
                int newLineCnt = 0;
                int idx = 0;
                while (idx<caretPos)   {
                    if (text.charAt(idx) == '\n') 
                       newLineCnt++;
                    idx++;
                    
                }
                break;

              
             
            case KeyEvent.VK_F3:
                        
          String    currentText = detectWordAtCursor();

         // GlobalValues.kcompleter.complete(currentText);
                
           if (currentText != null) {
       SourceCodeAnalysis sca = GlobalValues.jshell.sourceCodeAnalysis();
           
       System.out.println(currentText);
       int [] sl = new int[20];
       List<String> collectSuggestions = new ArrayList<>();
       List<SourceCodeAnalysis.Suggestion> suggestions = 
                     sca.completionSuggestions(currentText, currentText.length(), sl);
       suggestions.forEach((SourceCodeAnalysis.Suggestion sugg) ->  {
           collectSuggestions.add(sugg.continuation());
        //   System.out.println(sugg.continuation());
                   });
                    
       DefaultListModel dcl = new DefaultListModel();  // the model for the completion list
dcl.addAll(collectSuggestions);
       final JList clList = new JList(dcl);   // the completion's list

         
     // add a KeyListener to the JList in order to destroy itself with either ESC or ENTER
clList.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
              int keyValue = e.getKeyCode();
        switch (keyValue) {
                      
            case   KeyEvent.VK_ENTER:
                processListSelection(clList);
  
                break;
                
            case   KeyEvent.VK_ESCAPE:      // disposes the completion's list frame
        GlobalValues.completionFrame.dispose();
                break;
            default: 
                break;
              }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                //throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void keyReleased(KeyEvent e) {
               // throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    
        




     
     
clList.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    processListSelection(clList);
             }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
      

     // register our specialized list cell renderer that displays static members in bold
     //clList.setCellRenderer(new FontCellRenderer());
     
             JScrollPane  completionPane = new JScrollPane(clList);
   
    GlobalValues.completionFrame  = new JFrame("Completion List for "+currentText);
    Point pp  = GlobalValues.globalEditorPane.getCaret().getMagicCaretPosition();
     if (pp != null) {
            SwingUtilities.convertPointToScreen(pp, GlobalValues.globalEditorPane);
            pp.x = pp.x + 2;
            pp.y = pp.y + 20;
            GlobalValues.completionFrame.setLocation(pp.x, pp.y);

     }        
    
     GlobalValues.completionFrame.add(completionPane);
   int numOfComponentsForCompletion =  dcl.getSize();
    if (numOfComponentsForCompletion > GlobalValues.maxItemsToDisplayAtCompletions)
        numOfComponentsForCompletion = GlobalValues.maxItemsToDisplayAtCompletions;
   if (numOfComponentsForCompletion < 5) // some minimum size
        numOfComponentsForCompletion = 5;
   
    
    int approxPixelsPerItem = 20;    // approx. how many pixels to take vertically for a list item
                                                           // in order to approximate the vertical size of the completion list
    GlobalValues.completionFrame.setSize(600,  numOfComponentsForCompletion*approxPixelsPerItem);
     
    GlobalValues.completionFrame.setVisible(true);
    

           }
           
           
       break;
       

                
            case KeyEvent.VK_F9:
                clickExecuteKShell();
                e.consume();
                break;
               
                // use JShell API directly
            case KeyEvent.VK_F7:
                clickExecuteCode();
                e.consume();
                break;
        
                
          // Execute code with Kotlin
            case KeyEvent.VK_F6:
                clickExecuteScriptEngine();
                e.consume();
                break;

            case KeyEvent.VK_F10:
                clickComplete();
                e.consume();
                break;

     case KeyEvent.VK_F5:
         GlobalValues.consoleOutputWindow.resetText( " ");
         e.consume();
         break;
        
            case KeyEvent.VK_F2:
     String etext =  editorPane.getText();
     int currentTextLen = etext.length();
     if  (currentTextLen != textLen)   // text altered at the time between F2 clicks
      {
         fromLoc = 0;    // reset
     }
    
     int cursorLoc = editorPane.getCaretPosition();
     if (cursorLoc < toLoc)  {
     // reset if cursor is within the already executed part
         fromLoc = 0;
     }
     toLoc = cursorLoc;
     String textToExec = etext.substring(fromLoc, toLoc);
     
     editorPane.setSelectionStart(fromLoc);
     editorPane.setSelectionEnd(toLoc);
     editorPane.setSelectedTextColor(java.awt.Color.RED);
     textToExec = textToExec.substring(0, textToExec.lastIndexOf("\n"));
     fromLoc += textToExec.length();
     
      String grResult = ""; 
     GlobalValues.consoleOutputWindow.output.append("\n"+grResult);
     GlobalValues.consoleOutputWindow.output.setCaretPosition(GlobalValues.consoleOutputWindow.output.getText().length());

      e.consume();
    break;

            
            case KeyEvent.VK_F1:
            //case KeyEvent.VK_F3:    
                    e.consume();  // consume this event so it will not be processed in the default manner by the source that originated it
                  		//get the text on the current line
    
                    String inputString  = editorPane.getSelectedText();
                    if (inputString != null)   {   // some text is selected
               String [] matches = null;
               if (keyValue==KeyEvent.VK_F1) 
                       matches = GlobalValues.AutoCompletionJShell.getMatched(inputString);
               else 
                       matches = GlobalValues.AutoCompletionJShell.getMatchedRegEx(inputString);
                     
                    
                     final JList  resultsList = new JList(matches);
                     autocompleteListHandler  detailHelpAdapter = new autocompleteListHandler();
                     resultsList.addKeyListener(detailHelpAdapter); 
                     
                     
                     resultsList.addListSelectionListener(new ListSelectionListener() {
                         public void valueChanged(ListSelectionEvent e) {
                             String  selValue = resultsList.getSelectedValue().toString();
                             GlobalValues.selectedStringForAutoCompletion = selValue;
                        
                         }
                     }
                            );
                            
                GlobalValues.autoCompletionFrame = new AutoCompletionFrame("kotlinLab editor autocompletion, Press F1  for detailed help on the selected entry");
                GlobalValues.autoCompletionFrame.displayMatches(resultsList);
               }    // some text is selected   
                    e.consume(); 
                    break;
                     
                    
            default:
                caretPos = editorPane.getCaretPosition();
                break;            
          }
    }
    
         
    public void mouseClicked(MouseEvent me)
        { 
            
   if (me.getClickCount()>=2)  {  //only on ndouble-clicks
       RSyntaxTextArea    editor = (RSyntaxTextArea) me.getSource();
       Point  pt = new Point(me.getX(), me.getY());
       int  pos = editor.viewToModel(pt);
       javax.swing.text.Document  doc = editor.getDocument();
       
       boolean  exited = false;
       String  wb = "";
       int  offset = pos;
         // extract the part of the word before the mouse click position
       while (offset >= 0 && exited==false) {
         char  ch=' ';
                try {
                    ch = doc.getText(offset, 1).charAt(0);
                } catch (BadLocationException ex) {
                    System.out.println("Bad Location exception");
                    ex.printStackTrace();
                }
         boolean  isalphaNumeric = ( ch >= 'a' && ch <='z')  || (ch >= 'A' && ch <='Z') || (ch >= '0' && ch <='9');
         if (!isalphaNumeric)  exited=true;
          else {
           wb = wb + ch;
           offset -= 1;
           }
          }
       
       String  wa = "";
       int  docLen = doc.getLength();
       offset = pos+1;
       exited = false;
         // extract the part of the word after the mouse click position
       while (offset < docLen && exited==false) {
         char ch=' ';
                try {
                    ch = doc.getText(offset, 1).charAt(0);
                } catch (BadLocationException ex) {
                     System.out.println("Bad Location exception");
                     ex.printStackTrace();
               }
         boolean  isalphaNumeric = ( ch >= 'a' && ch <='z')  || (ch >= 'A' && ch <='Z') || (ch >= '0' && ch <='9');
         if (!isalphaNumeric)  exited=true;
           else {
         wa = wa + ch;
         offset += 1;
           }
         }
         
         StringBuffer wbreverse = new StringBuffer();
         for (int k=wb.length()-1; k>=0; k--)
             wbreverse.append(wb.charAt(k));
         
         String  wordAtCursor = wbreverse.toString()+wa;       
          
      
       }
      }
              
    
    
    void display_help() {
        JFrame helpFrame = new JFrame("Glab help");
        helpFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        helpFrame.setSize(400, 400);
        Container container = helpFrame.getContentPane();
        JTextArea helpText = new JTextArea();

        int classCnt = 0;
        Hashtable  clTable= new Hashtable(); 
        Enumeration enumer = clTable.elements();
        TreeSet  sortedClasses =  new TreeSet();
        while(enumer.hasMoreElements())
		{
		    Object next = (Object)enumer.nextElement();
		    Class currentClass = (Class)next;
                    String className = currentClass.getCanonicalName();
                    sortedClasses.add(className);
                    classCnt++;
        }

          Iterator iter = sortedClasses.iterator();
          while (iter.hasNext()) {
                    String className = (String)iter.next();
                    helpText.append(className+"\n");
            }
          JScrollPane  helpPane = new JScrollPane(helpText);
        
        container.add(helpPane);
        helpFrame.setVisible(true);  
                
      }
    
        
    
    public void keyReleased(KeyEvent e)
    {
    	        
    }

    class autocompleteListHandler extends KeyAdapter {
        public void keyPressed(KeyEvent ktev) {
            int  keyCode = ktev.getKeyCode();
            if (keyCode == KeyEvent.VK_F1) {
                display_detailed_help(GlobalValues.selectedStringForAutoCompletion);
            }
            if (keyCode == KeyEvent.VK_SPACE) {
                ktev.consume();
                GlobalValues.autoCompletionFrame.dispose();
            }
            
        }
        
   
    
        
}
    
    
    
    // displays detailed help for the selected item
    public static void display_detailed_help(String selectedItem) {
GlobalValues.detailHelpStringSelected = selectedItem;
DetailHelpFrame detailFrame = new DetailHelpFrame();
detailFrame.setVisible(true);
        
      }

       
 
}
