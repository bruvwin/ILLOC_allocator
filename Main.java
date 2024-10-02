import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {

    private static final String[] categories = {"LOAD", "STORE", "LOADI", "ADD", "SUB", "MULT", "LSHIFT", "RSHIFT", "OUTPUT", "NOP", "CONSTANT", "REGISTER", "COMMA", "INTO", "EOF", "EOL"};
    public static int lineNum = 0;
    public static int index;
    public static String line;
    public static BufferedReader reader;
    public static int flag = -1;
    public static int bCount;
    public static boolean failed;
    public static IR curr;
    public static void main(String[] args) throws Exception{
        String[] flags = {"-h", "-r", "-p", "-s"};

        
        String fileName = "";
        boolean flagRepeat = false;

        for(int i = 0; i < args.length; i++) {
            if (args[i].equals("-h")) {
                printHelp();
                return;
            } else if (args[i].equals("-r")) {
                if (flag > -1) 
                    flagRepeat = true;
                if (flag == -1)
                    flag = 1;
                flag = Math.min(flag, 1);
            } else if (args[i].equals("-p")) {
                if (flag > -1) 
                    flagRepeat = true;
                if (flag == -1)
                    flag = 2;
                flag = Math.min(flag, 2);
            } else if (args[i].equals("-s")) {
                if (flag > -1) 
                    flagRepeat = true;
                if (flag == -1)
                    flag = 3;
                flag = Math.min(flag, 3);
            } else {
                fileName = args[i];
                break;
            }
        }
        if (flag == -1){
            printError("Flag was not specified before file name");
            return ;
        }
        if (fileName.isEmpty()){
            printError("No target file name was specified.");
            return ;
        }
        if (flagRepeat)
            printError("Multiple flags were provided. Please only provide a single flag. Flag -"+ flags[flag] + " was chosen.");
        
        IR head;
        if (flag == 1) {
            head = new IR(0, -1, null);
            curr = head;
        }
        if (flag == 2) {
            bCount = 0;
        } 

        scan(fileName);   
        if (flag == 2)  {
            if (failed) {
                System.out.println("Parse found errors.");
            } else {
                System.out.println("Parse succeeded. Processed "+bCount+" operations.");
            }
        }
    }

    public static void scan(String fileName) {
        index = 0;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            failed = false;
            while ((line = reader.readLine()) != null) {
                line += "\n";
                lineNum++;
                if (line.isBlank() || line.isEmpty()) {
                    continue;
                }
                
                int[] upcode = getNextToken();
                if (upcode[0] == 15) {
                    index = 0;
                    continue;
                }   
                System.out.print(line);
                System.out.println("Line num "+lineNum+" upcode: "+upcode[0]);
                if (upcode[0] > -1 && upcode[0] < 2) {
                    finishMemop(upcode[0]);                    
                } else if (upcode[0] == 2){
                    finishLoadI();
                } else if (upcode[0] > 2 && upcode[0] < 8) {
                    finishArith(upcode[0]);
                } else if (upcode[0] == 8) {
                    finishOutput();
                } else if (upcode[0] == 9) {
                    finishNop();
                } else {
                    printError("Block does not start with an upcode.");
                }
                System.out.println();
                index = 0;
            }
        } catch(IOException e) {
            printError("There was an issue trying to read te specified file");
        }    
    }
    
    public static int[] getNextToken() {
        if (index >= line.length()) {
            printError("Mising arguments.");
        }
        char c = line.charAt(index++);
        while ((c == ' ' || c == '\t') && index < line.length()) {
            c = line.charAt(index++);
        }
        if (c == '/') { // check for comment
            c = line.charAt(index++);
            if (c == '/') {
                return new int[]{15, 1}; 
                }
        } else if (c == 'a'){ // check for add
            c = line.charAt(index++);
            if (c == 'd') {
                c = line.charAt(index++);
                if (c == 'd') { 
                    return new int[]{3, 0}; 
                } 
            }
        } else if (c == 'l') {
            c = line.charAt(index++);
            if (c == 'o') { // check for laodI or load
                c = line.charAt(index++);
                if (c == 'a') {
                    c = line.charAt(index++);
                    if (c == 'd') {
                        c = line.charAt(index++);
                        if (c == 'I')  {
                            c = line.charAt(index++);
                            if (c == ' ' || c == '\t'){
                                return new int[]{2, 0};
                            }
                        } else if (c == ' ' || c == '\t'){
                            return new int[]{0, 0};
                        }

                    }
                }
            } else if (c == 's') { // check for lshift
                c = line.charAt(index++);
                if (c == 'h') {
                    c = line.charAt(index++);
                    if (c == 'i') {
                        c = line.charAt(index++);
                        if (c == 'f') {
                            c = line.charAt(index++);
                            if (c == 't') {
                                c = line.charAt(index++);
                                if (c == ' ' || c == '\t')
                                    return new int[]{2, 3};
                            }
                        }
                    }
                }
            }
        } else if (c == 'm') {
            c = line.charAt(index++);
            if (c == 'u') {
                c = line.charAt(index++);
                if (c == 'l') {
                    c = line.charAt(index++);
                    if (c == 't') {
                        c = line.charAt(index++);
                        if (c == ' ' || c == '\t') {
                            return new int[]{5, 0};
                        }
                    }
                }
            }
        } else if (c == 'n') { // check for nop
            c = line.charAt(index++);
            if (c == 'o') {
                c = line.charAt(index++);
                if (c == 'p') {
                    c = line.charAt(index++);
                    if (c == ' ' || c == '\t') {
                        return new int[]{9, 0};
                    }
                }
            }
        } else if (c == 'o') { // check for output
            c = line.charAt(index++);
            if (c == 'u') {
                c = line.charAt(index++);
                if (c == 't') {
                    c = line.charAt(index++);
                    if (c == 'p') {
                        c = line.charAt(index++);
                        if (c =='u') {
                            c = line.charAt(index++);
                            if (c == 't') {
                                c = line.charAt(index++);
                                if (c == ' ' || c == '\t')  {
                                    return new int[]{8, 0};
                                }
                            }
                        }
                    }
                }
            }
        } else if (c == 'r') {
            c = line.charAt(index++);
            if ('0' <= c && c <= '9') {
                int register = c - 48;
                c = line.charAt(index++);
                while ('0' <= c && c <= '9') {
                    register *= 10;
                    register += (c - 48);
                    c = line.charAt(index++);
                }
                index--;
                return new int[]{11, register};
            } else if (c == 's') { // check for rshift
                c = line.charAt(index++);
                if (c == 'h') {
                    c = line.charAt(index++);
                    if (c == 'i') {
                        c = line.charAt(index++);
                        if (c == 'f') {
                            c = line.charAt(index++);
                            if (c == 't') {
                                c = line.charAt(index++);
                                if (c == ' ' || c == '\t')
                                    return new int[]{7, 0};
                            }
                        }
                    }
                }
            }
        } else if (c == 's') { // check sub and store
            c = line.charAt(index++);
            if (c == 'u') {
                c = line.charAt(index++);
                if (c == 'b') {
                    c = line.charAt(index++);
                    if (c == ' '|| c == '\t') {
                        return new int[]{4, 0};
                    }
                }
            } else if (c == 't') {
                c = line.charAt(index++);
                if (c == 'o') {
                    c = line.charAt(index++);
                    if (c == 'r') {
                        c = line.charAt(index++);
                        if (c == 'e') {
                            c = line.charAt(index++);
                            if (c == ' '|| c == '\t') {
                                return new int[]{1, 0};
                            }
                        }
                    }
                }
            }
        } else if ('0' <= c && c <= '9') {
            int register = c - 48;
            c = line.charAt(index++);
            while ('0' <= c && c <= '9') {
                register *= 10;
                register += (c - 48);
                c = line.charAt(index++);
            }
            index--;
            return new int[]{10, register};
        } else if (c == ',') {
            return new int[]{12, 0};
        } else if (c == '=') {
            c = line.charAt(index++);
            if (c == '>') {
                return new int[]{13, 0};
            }
        } else if (c == '\n') {
            return new int[]{15, 0};
        }

        return new int[]{-1,-1};
    }

    public static void finishArith(int operation) {
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: ARITHOP; Lexeme: " + categories[operation]);
        }
        int[] r1 = getNextToken();
        if (r1[0] != 11) {
            printError("Arithmetic operation must be followed by a register.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: REGISTER; Lexeme: r" + r1[1]);
        }

        if (getNextToken()[0] != 12) {
            printError("Registers 1 must be must be followed by a comma.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: COMMA; Lexeme: ,");
        }

        int[] r2 = getNextToken();
        if (r2[0] != 11) {
            printError("Comma must be followed by a second register.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: REGISTER; Lexeme: r" + r2[1]);
        }

        if (getNextToken()[0] != 13) {
            printError("Registers 2 must be must be followed by a '=>'' .");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: INTO; Lexeme: =>");
        }

        int[] r3 = getNextToken();
        if (r3[0] != 11) {
            printError("=> must be followed by a third register.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: REGISTER; Lexeme: r" + r3[1]);
        }

        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: EOL; Lexeme: \\n");
        }

        if (flag == 1 && failed == false) {
            Argument[] arguments = new Argument[3];
            Argument arg1 = new Argument(r1[1], 0, 0, 0);
            Argument arg2 = new Argument(r2[1], 0, 0, 0);
            Argument arg3 = new Argument(r3[1], 0, 0, 0);
            arguments[0] =arg1;
            arguments[1] =arg2;
            arguments[2] =arg3;
            System.out.println("Line: "+lineNum+"; Opcode: "+categories[operation]+"; SR1: "+r1[1]+", VR1: -"+", PR1: -"+", NU1: -"
            +"; SR2: "+r2[1]+", VR2: -"+", PR2: -"+", NU2: -"
            +"; SR3: "+r3[1]+", VR3: -"+", PR3: -"+", NU3: -; \n");
            curr.next = new IR(lineNum, operation, arguments);
            curr = curr.next;

        }

        if (flag == 2)
            bCount++;
        
        

    } 
    
    public static void finishMemop(int operation) {
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: MEMOP; Lexeme: " + categories[operation]);
        }
        int[] r1 = getNextToken();
        
        if (r1[0] != 11) {
            printError("Arithmetic operation must be followed by a register.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: REGISTER; Lexeme: r" + r1[1]);
        }

        if (getNextToken()[0] != 13) {
            printError("Register 1 must be must be followed by a '=>'' .");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: INTO; Lexeme: =>");
        }

        int[] r2 = getNextToken();
        if (r2[0] != 11) {
            printError("=> must be followed by a second register.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: REGISTER; Lexeme: r" + r2[1]);
        }

        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: EOL; Lexeme: \\n");
        }

        if (flag == 1 && failed == false) {
            Argument[] arguments = new Argument[3];
            Argument arg1 = new Argument(r1[1], 0, 0, 0);
            Argument arg2 = new Argument(r2[1], 0, 0, 0);
            arguments[0] =arg1;
            arguments[1] =arg2;
            System.out.println("Line: "+lineNum+"; Opcode: "+categories[operation]+"; SR1: "+r1[1]+", VR1: -"+", PR1: -"+", NU1: -"
            +"; SR2: "+r2[1]+", VR2: -"+", PR2: -"+", NU2: -; \n");
            curr.next = new IR(lineNum, operation, arguments);
            curr = curr.next;

        }

        if (flag == 2)
            bCount++;
    }
    
    public static void finishLoadI() {
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: MEMOP; Lexeme: " + categories[2]);
        }

        int[] r1 = getNextToken();
        if (r1[0] != 10) {
            printError("LoadI operation must be followed by a constant.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: CONSTANT; Lexeme: "+ r1[1]);
        }

        if (getNextToken()[0] != 13) {
            printError("Register 1 must be must be followed by a '=>'' .");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: INTO; Lexeme: =>");
        }

        int[] r2 = getNextToken();
        if (r2[0] != 11) {
            printError("=> must be followed by a register.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: REGISTER; Lexeme: r" + r2[1]);
        }

        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: EOL; Lexeme: \\n");
        }

        if (flag == 1 && failed == false) {
            Argument[] arguments = new Argument[3];
            Argument arg1 = new Argument(r1[1], 0, 0, 0);
            Argument arg2 = new Argument(r2[1], 0, 0, 0);
            arguments[0] =arg1;
            arguments[1] =arg2;
            System.out.println("Line: "+lineNum+"; Opcode: "+categories[2]+"; SR1: "+r1[1]+", VR1: -"+", PR1: -"+", NU1: -"
            +"; SR2: "+r2[1]+", VR2: -"+", PR2: -"+", NU2: -; \n");
            curr.next = new IR(lineNum, 2, arguments);
            curr = curr.next;
        }

        if (flag == 2)
            bCount++;
    }

    public static void finishOutput() {
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: OUTPUT; Lexeme: " + categories[8]);
        }
        
        int[] r1 = getNextToken();
        if (r1[0] != 10) {
            printError("Output operation must be followed by a constant.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: CONSTANT; Lexeme: "+ r1[1]);
        }

        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: EOL; Lexeme: \\n");
        }

        if (flag == 1 && failed == false) {
            Argument[] arguments = new Argument[3];
            Argument arg1 = new Argument(r1[1], 0, 0, 0);
            arguments[0] =arg1;
            System.out.println("Line: "+lineNum+"; Opcode: "+categories[8]+"; SR1: "+r1[1]+", VR1: -"+", PR1: -"+", NU1: -; \n");
            curr.next = new IR(lineNum, 2, arguments);
            curr = curr.next;
        }

        if (flag == 2)
            bCount++;
    }

    public static void finishNop() {
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: NOP; Lexeme: " + categories[9]);
        }

        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }
        if (flag == 3) {
            System.out.println("Line: " + lineNum + "; Type: EOL; Lexeme: \\n");
        }

        if (flag == 1 && failed == false) {
            Argument[] arguments = new Argument[3];
            System.out.println("Line: "+lineNum+"; Opcode: "+categories[8]+" \n");
            curr.next = new IR(lineNum, 2, arguments);
            curr = curr.next;
        }

        if (flag == 2)
            bCount++;
    }
    
    public static void printHelp() {
        System.out.println("412fe -s <name>  :");
        System.out.println("Reads the file specified by <name> and prints, to the standard output stream, a list of the tokens that the scanner found. Each token includes the line number, the token’s type (or syntactic category), and its spelling (or lexeme). ");
        System.out.println("412fe -p <name>  :");
        System.out.println("Reads the file specified by <name>, scans it and parses it, builds the intermediate representation, and reports either success or report all the errors that it finds in the input file. ");
        System.out.println("412fe -r <name>  :");
        System.out.println("Reads the file specified by <name>, scans it, parses it, builds the intermediate representation, and prints out the information in the intermediate representation.");
    }

    public static void printError(String msg) {
        System.err.println("ERROR "+lineNum+": " + msg+"\n");
        failed = true;
    }
}

class IR {
    public Argument[] arguments;
    public IR prev, next;
    public int lineNum, upcode;
    public IR(int lineNum, int upcode, Argument[] args) {
        this.lineNum = lineNum;
        this.upcode = upcode;
        this.arguments = args;
    }
} 

class Argument {
    private int SR;
    private int VR;
    private int PR;
    private int NU;
    public Argument(int sr, int vr, int pr, int nu) {
        this.SR = sr;
        this.VR = vr;
        this.PR = pr;
        this.NU = nu;
    }
    public int getSR() {
        return this.SR;
    }
    public int getVR() {
        return this.VR;
    }
    public int getPR() {
        return this.PR;
    }
    public int getNU() {
        return this.NU;
    }
}

