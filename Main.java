import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Math;
import java.util.Stack;
public class Main {

    private static final String[] categories = {"LOAD", "STORE", "LOADI", "ADD", "SUB", "MULT", "LSHIFT", "RSHIFT", "OUTPUT", "NOP", "CONSTANT", "REGISTER", "COMMA", "INTO", "EOF", "EOL"};
    public static int lineNum = 0;
    public static int index;
    public static String line;
    public static int flag = -1;
    public static int bCount;
    public static boolean failed;
    public static IR curr;
    public static int maxR;
    public static int k;
    public static Stack<Integer> rStack;
    public static int[] vrToPr;
    public static int[] prToVr;
    public static int[] prNu;
    public static int[] vrToSpill;
    public static int memoryLoc;
    public static int mark;
    public static void main(String[] args) throws Exception{
        //String[] flags = {"-h", "-x", "k"}; 
        String fileName = "";


        // for(int i = 0; i < args.length; i++) {
        //     if (args[i].equals("-h")) {
        //         printHelp();
        //         return;
        //     } else if (args[i].equals("-x")) {
        //         if (flag != -1) 
        //             flagRepeat = true;
        //         else
        //             flag = 1;
        //     } else {
        //         fileName = args[i];
        //         break;
        //     }
        // }
        // if (flag == -1){
        //     printError("Flag was not specified before file name");
        //     return ;
        // }
        // if (fileName.isEmpty()){
        //     printError("No target file name was specified.");
        //     return ;
        // }
        // if (flagRepeat)
        //     printError("Multiple flags were provided. Please only provide a single flag. Flag -"+ flags[flag] + " was chosen.");
        if (args[0].equals("-h")){
            printHelp();
            return;
        } else if (args.length < 2){
            printError("Missing arguments. Bad");
            return;
        } else if (args[0].equals("-x")) {
            flag = 1;
        } else {
            flag = 2;
            try {
                k = Integer.parseInt(args[0]);
            } catch (Exception e) {
                printError("Invalid arguments.");
            }
        }
        fileName = args[1];
        IR head = new IR(0, -1, null);
        curr = head;
        bCount = 0;
        maxR = -1;
        scan(fileName);
        if (failed)
            return;
        rename();
        
        if (flag == 2) {
            curr = head.next;
            renameAlloc();
        }
        curr = head;    
        printRenamed();
    }

    public static void renameAlloc() {
        //IR currOp = curr;
        vrToPr = new int[maxR + 1];
        prToVr = new int[k - 1];
        prNu = new int[k - 1];
        vrToSpill = new int[maxR + 1];
        memoryLoc = 32768;
        rStack = new Stack<Integer>();

        for (int i = 0; i <= maxR; i++) {
            vrToPr[i] = -1;
        }
        for (int i = 0; i < k - 1; i++) {
            prToVr[i] = -1;
            prNu[i] = -1;
            rStack.push(i);
        }
        
        while (curr != null) {
            mark = -1;
            if (curr.upcode == 0) { // load
                Argument U = curr.arguments[0];
                int pr = vrToPr[U.getVR()];
                if (pr == -1) {
                    U.setPR(getAPR(U.getVR(), U.getNU()));
                    restore(U.getVR(), U.getPR());
                } else {
                    U.setPR(pr);
                }
                if (U.getNU() == -1 && prToVr[U.getPR()] != -1) {
                    freeAPR(U.getPR());
                }
                mark = -1;
                Argument D = curr.arguments[2];
                D.setPR(getAPR(D.getVR(), D.getNU()));

            } else if (curr.upcode == 1) {
                Argument U1 = curr.arguments[1];
                Argument U2 = curr.arguments[0];
                int pr1 = vrToPr[U1.getVR()];
                int pr2 = vrToPr[U2.getVR()];

                if (pr1 == -1) {
                    U1.setPR(getAPR(U1.getVR(), U1.getNU()));
                    restore(U1.getVR(), U1.getPR());
                } else {
                    U1.setPR(pr1);
                }
                if (U1.getNU() == -1 && prToVr[U1.getPR()] != -1) {
                    freeAPR(U1.getPR());
                }
                mark = pr1;
                if (pr2 == -1) {
                    U2.setPR(getAPR(U2.getVR(), U2.getNU()));
                    restore(U2.getVR(), U2.getPR());
                } else {
                    U2.setPR(pr2);
                }
                if (U2.getNU() == -1 && prToVr[U2.getPR()] != -1) {
                    freeAPR(U2.getPR());
                }
            } else if(curr.upcode == 2) {
                Argument D = curr.arguments[2];
                D.setPR(getAPR(D.getVR(), D.getNU()));
            } else if(curr.upcode > 2 && curr.upcode < 8 ) {
                
                Argument U1 = curr.arguments[0];
                Argument U2 = curr.arguments[1];
                int pr1 = vrToPr[U1.getVR()];
                if (pr1 == -1) {
                    U1.setPR(getAPR(U1.getVR(), U1.getNU()));
                    restore(U1.getVR(), U1.getPR());
                } else {
                    U1.setPR(pr1);
                }
                
                mark = U1.getPR();
                
                int pr2 = vrToPr[U2.getVR()];
                if (pr2 == -1) {
                    U2.setPR(getAPR(U2.getVR(), U2.getNU()));
                    restore(U2.getVR(), U2.getPR());
                } else {
                    U2.setPR(pr2);
                }
                if (U1.getNU() == -1 && prToVr[U1.getPR()] != -1) {
                    freeAPR(U1.getPR());
                }
                if (U2.getNU() == -1 && prToVr[U2.getPR()] != -1) {
                    freeAPR(U2.getPR());
                }
                
                mark = -1;
                Argument D = curr.arguments[2];
                D.setPR(getAPR(D.getVR(), D.getNU()));
                
            }
            curr = curr.next;
        }

    }

    public static void restore(int vr, int pr) {
        int currMem = vrToSpill[vr];
        vrToSpill[vr] = -1;

        Argument l1 = new Argument(currMem, currMem, currMem, curr.lineNum);
        Argument l2 = new Argument(k - 1, k - 1, k - 1, curr.lineNum);
        Argument[] loadIargs = new Argument[]{l1, null, l2};
        IR loadI = new IR(curr.lineNum, 2, loadIargs);

        Argument lo1 = new Argument(k - 1, k - 1, k - 1, curr.lineNum);
        Argument lo2 = new Argument(pr, pr, pr, curr.lineNum);
        Argument[] loargs = new Argument[]{lo1, null, lo2};
        IR load = new IR(curr.lineNum, 0, loargs);

        loadI.prev = curr.prev;
        loadI.next = load;
        load.prev = loadI;
        load.next = curr;

        curr.prev.next = loadI;
        curr.prev = load;

    }

    public static int getAPR(int vr, int nu) {
        int x;
        if (rStack.size() > 0) {
            x = rStack.pop();
        }
            
        else {
            x = -2;
            int latest = -2;
            for (int i = 0; i < k - 1; i++) {
                if (prNu[i] > latest && i != mark) {
                    x = i;
                    latest = prNu[i];
                }
            }
            spill(x);
        }
        vrToPr[vr] = x;
        prToVr[x] = vr;
        prNu[x] = nu;
        return x;
    }

    public static void spill(int x) {
        vrToSpill[prToVr[x]] = memoryLoc;
        vrToPr[prToVr[x]] = -1;
        
        Argument l1 = new Argument(memoryLoc, memoryLoc, memoryLoc, curr.lineNum);
        Argument l2 = new Argument(k - 1, k - 1, k - 1, curr.lineNum);
        Argument[] loadIargs = new Argument[]{l1, null, l2};
        IR loadIr = new IR(curr.lineNum, 2, loadIargs);

        Argument s1 = new Argument(x, x, x, curr.lineNum);
        Argument s2 = new Argument(k - 1, k - 1, k - 1, curr.lineNum);
        Argument[] storeArgs = new Argument[]{s1, s2, null};
        IR store = new IR(curr.lineNum, 1, storeArgs);

        loadIr.prev = curr.prev;
        loadIr.next = store;
        store.prev = loadIr;
        store.next = curr;

        curr.prev.next = loadIr;
        curr.prev = store;

        memoryLoc += 4;
    }

    public static void freeAPR(int pr) {
        vrToPr[prToVr[pr]] = -1;
        prToVr[pr] = -1;
        prNu[pr] = -1;
        rStack.push(pr);
    }

    public static void printRenamed() {
        curr = curr.next;
        while (curr != null) {
            if (curr.upcode == 0) {
                System.out.println(categories[curr.upcode].toLowerCase() + " r" + curr.arguments[0].getPR() + " => r"+ curr.arguments[2].getPR());
            } else if (curr.upcode == 1) {
                System.out.println(categories[curr.upcode].toLowerCase() + " r" + curr.arguments[0].getPR() + " => r"+ curr.arguments[1].getPR());
            }
            else if (curr.upcode == 2) {
                System.out.println("loadI " + curr.arguments[0].getPR() + " => r"+ curr.arguments[2].getPR());
            } else if (curr.upcode > 2 && curr.upcode < 8) {
                System.out.println(categories[curr.upcode].toLowerCase() + " r" + curr.arguments[0].getPR() + ", r" + curr.arguments[1].getPR() +" => r"+ curr.arguments[2].getPR());
            } else if (curr.upcode == 8) {
                System.out.println("output " + curr.arguments[0].getPR());
            } else if (curr.upcode == 9) {
                System.out.println("nop");
            }
            curr = curr.next;
        }
    }

    public static void rename() {
        int virtualName = 0;
        int[] lu = new int[maxR + 1];
        int[] converter = new int[maxR + 1];
        for (int i =  0; i <= maxR; i++) {
            lu[i] = -1;
            converter[i] = -1;
        }
        while (curr.lineNum > 0) {
            // Each definition
            if(curr.upcode > -1 && curr.upcode < 8 && curr.upcode != 1) {
                if (converter[curr.arguments[2].getSR()] == -1)
                    converter[curr.arguments[2].getSR()] = virtualName++;
                curr.arguments[2].setVR(converter[curr.arguments[2].getSR()]);
                curr.arguments[2].setPR(converter[curr.arguments[2].getSR()]);
                curr.arguments[2].setNU(lu[curr.arguments[2].getSR()]);

                // Kill live range
                converter[curr.arguments[2].getSR()] = -1;
                lu[curr.arguments[2].getSR()] = -1;
            }

            // Each use 
            if (curr.upcode == 0) {
                if (converter[curr.arguments[0].getSR()] == -1)
                    converter[curr.arguments[0].getSR()] = virtualName++;
                curr.arguments[0].setVR(converter[curr.arguments[0].getSR()]);
                curr.arguments[0].setPR(converter[curr.arguments[0].getSR()]);
                curr.arguments[0].setNU(lu[curr.arguments[0].getSR()]);
                lu[curr.arguments[0].getSR()] = curr.lineNum;
            }
            if (curr.upcode == 1) {
                if (converter[curr.arguments[1].getSR()] == -1)
                        converter[curr.arguments[1].getSR()] = virtualName++;
                curr.arguments[1].setVR(converter[curr.arguments[1].getSR()]);
                curr.arguments[1].setPR(converter[curr.arguments[1].getSR()]);
                curr.arguments[1].setNU(lu[curr.arguments[1].getSR()]);
                lu[curr.arguments[1].getSR()] = curr.lineNum;
                if (converter[curr.arguments[0].getSR()] == -1)
                    converter[curr.arguments[0].getSR()] = virtualName++;
                curr.arguments[0].setVR(converter[curr.arguments[0].getSR()]);
                curr.arguments[0].setPR(converter[curr.arguments[0].getSR()]);
                curr.arguments[0].setNU(lu[curr.arguments[0].getSR()]);
                lu[curr.arguments[0].getSR()] = curr.lineNum;
            }
            if(curr.upcode > 2 && curr.upcode < 8 ) {
                if (converter[curr.arguments[0].getSR()] == -1)
                    converter[curr.arguments[0].getSR()] = virtualName++;
                curr.arguments[0].setVR(converter[curr.arguments[0].getSR()]);
                curr.arguments[0].setPR(converter[curr.arguments[0].getSR()]);
                curr.arguments[0].setNU(lu[curr.arguments[0].getSR()]);
                lu[curr.arguments[0].getSR()] = curr.lineNum;

                if (curr.upcode > 2) {
                    if (converter[curr.arguments[1].getSR()] == -1)
                        converter[curr.arguments[1].getSR()] = virtualName++;
                    curr.arguments[1].setVR(converter[curr.arguments[1].getSR()]);
                    curr.arguments[1].setPR(converter[curr.arguments[1].getSR()]);
                    curr.arguments[1].setNU(lu[curr.arguments[1].getSR()]);
                    lu[curr.arguments[1].getSR()] = curr.lineNum;
                }
            }
            curr = curr.prev;
            
        }
        maxR = virtualName - 1;

    }

    public static void scan(String fileName) {
        index = 0;
        try {
            
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
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
                //System.out.println("Line num "+lineNum+" upcode: "+upcode[0]);
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
                index = 0;
            }
            reader.close();
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
                                    return new int[]{6, 0};
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

        int[] r1 = getNextToken();
        if (r1[0] != 11) {
            printError("Arithmetic operation must be followed by a register.");
            return ;
        }
        maxR = Math.max(maxR, r1[1]);

        if (getNextToken()[0] != 12) {
            printError("Registers 1 must be must be followed by a comma.");
            return ;
        }

        int[] r2 = getNextToken();
        if (r2[0] != 11) {
            printError("Comma must be followed by a second register.");
            return ;
        }
        maxR = Math.max(maxR, r2[1]);

        if (getNextToken()[0] != 13) {
            printError("Registers 2 must be must be followed by a '=>'' .");
            return ;
        }

        int[] r3 = getNextToken();
        if (r3[0] != 11) {
            printError("=> must be followed by a third register.");
            return ;
        }
        maxR = Math.max(maxR, r3[1]);

        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }

        if (failed == false) {
            Argument[] arguments = new Argument[3];
            Argument arg1 = new Argument(r1[1], r1[1], r1[1], 0);
            Argument arg2 = new Argument(r2[1], r2[1], r2[1], 0);
            Argument arg3 = new Argument(r3[1], r3[1], r3[1], 0);
            arguments[0] =arg1;
            arguments[1] =arg2;
            arguments[2] =arg3;
            curr.next = new IR(lineNum, operation, arguments);
            curr.next.prev = curr;
            curr = curr.next;
            bCount++;
        }

            
        
        

    } 
    
    public static void finishMemop(int operation) {
        int[] r1 = getNextToken();
        if (r1[0] != 11) {
            printError("Arithmetic operation must be followed by a register.");
            return ;
        }
        maxR = Math.max(maxR, r1[1]);

        if (getNextToken()[0] != 13) {
            printError("Register 1 must be must be followed by a '=>'' .");
            return ;
        }

        int[] r2 = getNextToken();
        if (r2[0] != 11) {
            printError("=> must be followed by a second register.");
            return ;
        }
        maxR = Math.max(maxR, r2[1]);

        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }

        if (failed == false) {
            Argument[] arguments = new Argument[3];
            Argument arg1 = new Argument(r1[1], r1[1], r1[1], 0);
            Argument arg2 = new Argument(r2[1], r2[1], r2[1], 0);
            arguments[0] =arg1;
            if (operation == 0)
                arguments[2] =arg2;
            else
                arguments[1] =arg2;
            curr.next = new IR(lineNum, operation, arguments);
            curr.next.prev = curr;
            curr = curr.next;
            bCount++;
        }

            
    }
    
    public static void finishLoadI() {
        int[] r1 = getNextToken();
        if (r1[0] != 10) {
            printError("LoadI operation must be followed by a constant.");
            return ;
        }
        if (getNextToken()[0] != 13) {
            printError("Register 1 must be must be followed by a '=>'' .");
            return ;
        }

        int[] r2 = getNextToken();
        if (r2[0] != 11) {
            printError("=> must be followed by a register.");
            return ;
        }
        maxR = Math.max(maxR, r2[1]);

        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }

        if (failed == false) {
            Argument[] arguments = new Argument[3];
            Argument arg1 = new Argument(r1[1], r1[1], r1[1], 0);
            Argument arg2 = new Argument(r2[1], r2[1], r2[1], 0);
            arguments[0] =arg1;
            arguments[2] =arg2;
            curr.next = new IR(lineNum, 2, arguments);
            curr.next.prev = curr;
            curr = curr.next;
            bCount++;
        }

            
    }

    public static void finishOutput() {
        int[] r1 = getNextToken();
        if (r1[0] != 10) {
            printError("Output operation must be followed by a constant.");
            return ;
        }

        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }

        if (failed == false) {
            Argument[] arguments = new Argument[3];
            Argument arg1 = new Argument(r1[1], r1[1], r1[1], 0);
            arguments[0] =arg1;
            curr.next = new IR(lineNum, 8, arguments);
            curr.next.prev = curr;
            curr = curr.next;
            bCount++;
        }
            
    }

    public static void finishNop() {
        if (getNextToken()[0] != 15) {
            printError("Too many tokens given for operation.");
            return ;
        }
        
        if (failed == false) {
            curr.next = new IR(lineNum, 9, null);
            curr.next.prev = curr;
            curr = curr.next;
            bCount++;
        }

            
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
        prev = null;
        next = null;
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
    public void setVR(int newVr) {
        this.VR = newVr;
    }
    public void setNU(int newNU) {
        this.NU = newNU;
    }
    public void setPR(int newPR) {
        this.PR = newPR;
    }
}

